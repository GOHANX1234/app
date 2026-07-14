package com.sarrows.app.data.remote

import com.sarrows.app.data.models.*
import com.sarrows.app.data.ndk.NativeSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SarrowsApiClient @Inject constructor(
    private val nativeSecurity: NativeSecurity,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val BASE = "https://sarrows.vercel.app"
        private val JSON_MEDIA   = "application/json; charset=utf-8".toMediaType()
        private val FORM_MEDIA   = "application/x-www-form-urlencoded".toMediaType()
    }

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // â”€â”€ Internal request helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun buildRequest(url: String, method: String = "GET", body: RequestBody? = null): Request {
        val headers = nativeSecurity.buildAuthHeadersMap()
        val cookieHeader = nativeSecurity.nativeBuildCookieHeader()

        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> builder.header(k, v) }
        if (cookieHeader.isNotEmpty()) builder.header("Cookie", cookieHeader)

        return when (method) {
            "POST"   -> builder.post(body ?: ByteArray(0).toRequestBody())
            "DELETE" -> builder.delete(body)
            else     -> builder.get()
        }.build()
    }

    private fun parseSetCookies(response: Response) {
        val cookies = response.headers("Set-Cookie")
        if (cookies.isNotEmpty()) {
            nativeSecurity.nativeParseCookies(cookies.joinToString("\n"))
        }
    }

    private suspend fun execute(request: Request): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            okHttpClient.newCall(request).execute().use { resp ->
                parseSetCookies(resp)
                resp.code to (resp.body?.string() ?: "")
            }
        } catch (e: Exception) {
            // Network failure (no connectivity, timeout, SSL error, etc.)
            -1 to (e.message ?: "Network error")
        }
    }

    private inline fun <reified T> parseBody(body: String): T =
        json.decodeFromString(body)

    /** Returns null instead of throwing if body is not valid JSON for T. */
    private inline fun <reified T> safeParseBody(body: String): T? = try {
        json.decodeFromString(body)
    } catch (_: Exception) {
        null
    }

    private fun errorFrom(code: Int, body: String): ApiResult.Error {
        return try {
            val err = json.decodeFromString<ApiError>(body)
            ApiResult.Error(err.error.ifEmpty { "Unknown error" }, code, err.fieldErrors)
        } catch (_: Exception) {
            ApiResult.Error("HTTP $code", code)
        }
    }

    // â”€â”€ Auth â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getCsrfToken(): ApiResult<String> {
        val req = buildRequest("$BASE/api/auth/csrf")
        val (code, body) = execute(req)
        return if (code == 200) {
            val parsed = safeParseBody<CsrfResponse>(body)
            if (parsed != null) {
                nativeSecurity.nativeStoreCsrfToken(parsed.csrfToken)
                ApiResult.Success(parsed.csrfToken)
            } else {
                // Server returned a non-JSON or null body â€” likely a transient error; surface it clearly.
                ApiResult.Error("Could not retrieve login token â€” please try again", code)
            }
        } else errorFrom(code, body)
    }

    suspend fun signUp(nickname: String, email: String, password: String): ApiResult<Unit> {
        val payload = json.encodeToString(
            SignupRequest.serializer(),
            SignupRequest(nickname, email, password)
        )
        val req = buildRequest("$BASE/api/auth/signup", "POST", payload.toRequestBody(JSON_MEDIA))
        val (code, body) = execute(req)
        return if (code == 201) ApiResult.Success(Unit) else errorFrom(code, body)
    }

    suspend fun login(email: String, password: String): ApiResult<Unit> {
        // Step 1: get CSRF
        val csrfResult = getCsrfToken()
        if (csrfResult is ApiResult.Error) return csrfResult
        val csrf = (csrfResult as ApiResult.Success).data

        // Step 2: post credentials (form-encoded, built in C++)
        val formBody = nativeSecurity.nativeBuildLoginBody(email, password, csrf)
        val req = Request.Builder()
            .url("$BASE/api/auth/callback/credentials")
            .post(formBody.toRequestBody(FORM_MEDIA))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Origin", BASE)
            .header("Referer", "$BASE/login")
            .header("User-Agent", "SarrowsAndroid/1.0")
            .also { builder ->
                val cookie = nativeSecurity.nativeBuildCookieHeader()
                if (cookie.isNotEmpty()) builder.header("Cookie", cookie)
            }
            .build()
        val (code, body) = execute(req)

        // NextAuth returns 200 for both success and failure
        return if (code == 200 && !body.contains("error=")) {
            ApiResult.Success(Unit)
        } else {
            ApiResult.Error("Invalid email or password", code)
        }
    }

    suspend fun getSession(): ApiResult<SessionResponse> {
        val req = buildRequest("$BASE/api/auth/session")
        val (code, body) = execute(req)
        return if (code == 200) {
            // Unauthenticated returns "{}" which is valid; "null" or HTML must not crash.
            val session = safeParseBody<SessionResponse>(body) ?: SessionResponse(user = null)
            ApiResult.Success(session)
        } else errorFrom(code, body)
    }

    suspend fun logout(): ApiResult<Unit> {
        val csrfResult = getCsrfToken()
        if (csrfResult is ApiResult.Error) return csrfResult
        val csrf = (csrfResult as ApiResult.Success).data
        val formBody = nativeSecurity.nativeBuildSignoutBody(csrf)
        val req = Request.Builder()
            .url("$BASE/api/auth/signout")
            .post(formBody.toRequestBody(FORM_MEDIA))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Origin", BASE)
            .header("User-Agent", "SarrowsAndroid/1.0")
            .also { builder ->
                val cookie = nativeSecurity.nativeBuildCookieHeader()
                if (cookie.isNotEmpty()) builder.header("Cookie", cookie)
            }
            .build()
        val (code, _) = execute(req)
        nativeSecurity.nativeClearCookies()
        return if (code in 200..302) ApiResult.Success(Unit) else ApiResult.Error("Logout failed", code)
    }

    // â”€â”€ Movies â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getMovies(page: Int = 1, limit: Int = 24, sort: String = "latest",
                          genre: String? = null, year: Int? = null): ApiResult<MoviesResponse> {
        val url = buildUrl("$BASE/api/movies", mapOf(
            "page" to page.toString(), "limit" to limit.toString(),
            "sort" to sort,
            "genre" to (genre ?: ""),
            "year" to (year?.toString() ?: "")
        ))
        val (code, body) = execute(buildRequest(url))
        return if (code == 200) ApiResult.Success(parseBody(body)) else errorFrom(code, body)
    }

    suspend fun getMovieById(id: String): ApiResult<Movie> {
        val (code, body) = execute(buildRequest("$BASE/api/movies/$id"))
        return if (code == 200) ApiResult.Success(parseBody<MovieDetailResponse>(body).movie)
        else errorFrom(code, body)
    }

    // â”€â”€ Anime / Series â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getSeries(type: String = "anime", page: Int = 1, limit: Int = 24,
                          sort: String = "latest", genre: String? = null,
                          status: String? = null): ApiResult<SeriesResponse> {
        val url = buildUrl("$BASE/api/anime", mapOf(
            "type" to type, "page" to page.toString(), "limit" to limit.toString(),
            "sort" to sort,
            "genre" to (genre ?: ""),
            "status" to (status ?: "")
        ))
        val (code, body) = execute(buildRequest(url))
        return if (code == 200) ApiResult.Success(parseBody(body)) else errorFrom(code, body)
    }

    // â”€â”€ Episodes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getEpisodeById(id: String): ApiResult<Episode> {
        val (code, body) = execute(buildRequest("$BASE/api/episodes/$id"))
        return if (code == 200) ApiResult.Success(parseBody<EpisodeDetailResponse>(body).episode)
        else errorFrom(code, body)
    }

    // â”€â”€ Search â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun search(query: String): ApiResult<SearchResponse> {
        val url = "$BASE/api/search?q=${query.trim().take(100).encodeUrl()}"
        val (code, body) = execute(buildRequest(url))
        return if (code == 200) ApiResult.Success(parseBody(body)) else errorFrom(code, body)
    }

    // â”€â”€ Streaming â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Probes the stream endpoint with HEAD (no body downloaded) to determine
     * whether content is a direct/HLS stream or an embed iframe.
     *
     * API strategy (Â§9.6):
     *   HEAD /api/stream/movie/:id  â†’ 200 = stream, 400 = embed type, 401 = unauthed, 429 = rate limited
     *   If embed â†’ GET /api/stream/movie/:id/embed â†’ { url }
     */
    private fun buildHeadRequest(url: String): Request {
        val builder = Request.Builder().url(url).head()
        nativeSecurity.buildAuthHeadersMap().forEach { (k, v) -> builder.header(k, v) }
        val cookie = nativeSecurity.nativeBuildCookieHeader()
        if (cookie.isNotEmpty()) builder.header("Cookie", cookie)
        return builder.build()
    }

    suspend fun resolveMoviePlayback(id: String): PlaybackResult {
        if (!nativeSecurity.nativeCheckStreamRateLimit()) return PlaybackResult.RateLimited
        val streamUrl = nativeSecurity.nativeMovieStreamUrl(id)
        val (code, _) = execute(buildHeadRequest(streamUrl))
        nativeSecurity.nativeRecordStreamRequest()
        return when (code) {
            200, 206 -> PlaybackResult.Stream(streamUrl, nativeSecurity.nativeBuildCookieHeader())
            400 -> {
                val embedUrl = nativeSecurity.nativeMovieEmbedUrl(id)
                val (ec, eb) = execute(buildRequest(embedUrl))
                val parsed = safeParseBody<EmbedUrlResponse>(eb)
                if (ec == 200 && parsed != null) PlaybackResult.Embed(parsed.url)
                else PlaybackResult.Error("Could not resolve embed", ec)
            }
            401      -> PlaybackResult.Unauthenticated
            429      -> PlaybackResult.RateLimited
            else     -> PlaybackResult.Error("Stream probe failed ($code)", code)
        }
    }

    suspend fun resolveEpisodePlayback(id: String): PlaybackResult {
        if (!nativeSecurity.nativeCheckStreamRateLimit()) return PlaybackResult.RateLimited
        val streamUrl = nativeSecurity.nativeEpisodeStreamUrl(id)
        val (code, _) = execute(buildHeadRequest(streamUrl))
        nativeSecurity.nativeRecordStreamRequest()
        return when (code) {
            200, 206 -> PlaybackResult.Stream(streamUrl, nativeSecurity.nativeBuildCookieHeader())
            400 -> {
                val embedUrl = nativeSecurity.nativeEpisodeEmbedUrl(id)
                val (ec, eb) = execute(buildRequest(embedUrl))
                val parsed = safeParseBody<EmbedUrlResponse>(eb)
                if (ec == 200 && parsed != null) PlaybackResult.Embed(parsed.url)
                else PlaybackResult.Error("Could not resolve embed", ec)
            }
            401      -> PlaybackResult.Unauthenticated
            429      -> PlaybackResult.RateLimited
            else     -> PlaybackResult.Error("Stream probe failed ($code)", code)
        }
    }

    // â”€â”€ Reviews â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getReviews(targetType: String, targetId: String): ApiResult<List<Review>> {
        val url = "$BASE/api/reviews?targetType=$targetType&targetId=$targetId"
        val (code, body) = execute(buildRequest(url))
        return if (code == 200) ApiResult.Success(parseBody<ReviewsResponse>(body).reviews)
        else errorFrom(code, body)
    }

    suspend fun submitReview(targetType: String, targetId: String,
                             rating: Int, comment: String?): ApiResult<Review> {
        val payload = json.encodeToString(ReviewRequest.serializer(),
            ReviewRequest(targetType, targetId, rating, comment))
        val req = buildRequest("$BASE/api/reviews", "POST", payload.toRequestBody(JSON_MEDIA))
        val (code, body) = execute(req)
        return if (code == 200) ApiResult.Success(parseBody<ReviewResponse>(body).review)
        else errorFrom(code, body)
    }

    suspend fun deleteReview(reviewId: String): ApiResult<Unit> {
        val req = buildRequest("$BASE/api/reviews/$reviewId", "DELETE")
        val (code, body) = execute(req)
        return if (code == 200) ApiResult.Success(Unit) else errorFrom(code, body)
    }

    // â”€â”€ Watchlist â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getWatchlistStatus(targetType: String, targetId: String): ApiResult<Boolean> {
        val url = "$BASE/api/watchlist?targetType=$targetType&targetId=$targetId"
        val (code, body) = execute(buildRequest(url))
        return if (code == 200) ApiResult.Success(parseBody<WatchlistStatusResponse>(body).inWatchlist)
        else errorFrom(code, body)
    }

    suspend fun toggleWatchlist(targetType: String, targetId: String): ApiResult<Boolean> {
        val payload = json.encodeToString(WatchlistToggleRequest.serializer(),
            WatchlistToggleRequest(targetType, targetId))
        val req = buildRequest("$BASE/api/watchlist/toggle", "POST", payload.toRequestBody(JSON_MEDIA))
        val (code, body) = execute(req)
        return if (code == 200) ApiResult.Success(parseBody<WatchlistStatusResponse>(body).inWatchlist)
        else errorFrom(code, body)
    }

    // â”€â”€ Watch history â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun saveProgress(targetType: String, targetId: String, progressSeconds: Int): ApiResult<Unit> {
        val payload = json.encodeToString(WatchHistoryRequest.serializer(),
            WatchHistoryRequest(targetType, targetId, progressSeconds))
        val req = buildRequest("$BASE/api/watch-history", "POST", payload.toRequestBody(JSON_MEDIA))
        val (code, _) = execute(req)
        return if (code == 200) ApiResult.Success(Unit) else ApiResult.Error("Failed to save progress", code)
    }

    // â”€â”€ Views â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun recordView(targetType: String, targetId: String): ApiResult<Unit> {
        val payload = json.encodeToString(ViewRequest.serializer(), ViewRequest(targetType, targetId))
        val req = buildRequest("$BASE/api/views", "POST", payload.toRequestBody(JSON_MEDIA))
        val (code, _) = execute(req)
        return if (code == 200) ApiResult.Success(Unit) else ApiResult.Error("View record failed", code)
    }

    // â”€â”€ Content Requests â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun submitRequest(title: String, type: String, note: String?): ApiResult<ContentRequest> {
        val payload = json.encodeToString(ContentRequestBody.serializer(),
            ContentRequestBody(title, type, note))
        val req = buildRequest("$BASE/api/requests", "POST", payload.toRequestBody(JSON_MEDIA))
        val (code, body) = execute(req)
        return if (code == 201) ApiResult.Success(parseBody<RequestResponse>(body).request)
        else errorFrom(code, body)
    }

    suspend fun getMyRequests(): ApiResult<List<ContentRequest>> {
        val (code, body) = execute(buildRequest("$BASE/api/requests"))
        return if (code == 200) ApiResult.Success(parseBody<RequestsResponse>(body).requests)
        else errorFrom(code, body)
    }

    suspend fun cancelRequest(requestId: String): ApiResult<Unit> {
        val req = buildRequest("$BASE/api/requests/$requestId", "DELETE")
        val (code, body) = execute(req)
        return if (code == 200) ApiResult.Success(Unit) else errorFrom(code, body)
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun buildUrl(base: String, params: Map<String, String>): String {
        val query = params.filter { it.value.isNotEmpty() }
            .entries.joinToString("&") { (k, v) -> "$k=${v.encodeUrl()}" }
        return if (query.isEmpty()) base else "$base?$query"
    }

    private fun String.encodeUrl() = java.net.URLEncoder.encode(this, "UTF-8")
}

sealed class PlaybackResult {
    data class Stream(val url: String, val cookieHeader: String) : PlaybackResult()
    data class Embed(val url: String) : PlaybackResult()
    data class Error(val message: String, val code: Int) : PlaybackResult()
    object Unauthenticated : PlaybackResult()
    object RateLimited : PlaybackResult()
}
