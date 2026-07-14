package com.sarrows.app.data.repository

import com.sarrows.app.data.models.*
import com.sarrows.app.data.ndk.NativeSecurity
import com.sarrows.app.data.remote.PlaybackResult
import com.sarrows.app.data.remote.SarrowsApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SarrowsRepository @Inject constructor(
    private val api: SarrowsApiClient,
    private val nativeSecurity: NativeSecurity
) {
    // â”€â”€ Auth â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun signUp(nickname: String, email: String, password: String) =
        api.signUp(nickname, email, password)

    suspend fun login(email: String, password: String) =
        api.login(email, password)

    suspend fun getSession() = api.getSession()

    suspend fun logout() = api.logout()

    fun hasSession() = nativeSecurity.nativeHasSession()

    // â”€â”€ Movies â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getMovies(page: Int = 1, sort: String = "latest",
                          genre: String? = null, year: Int? = null) =
        api.getMovies(page = page, sort = sort, genre = genre, year = year)

    suspend fun getMovieById(id: String) = api.getMovieById(id)

    // â”€â”€ Anime / Series â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getAnime(page: Int = 1, sort: String = "latest",
                         genre: String? = null, status: String? = null) =
        api.getSeries(type = "anime", page = page, sort = sort, genre = genre, status = status)

    suspend fun getSeries(page: Int = 1, sort: String = "latest",
                          genre: String? = null, status: String? = null) =
        api.getSeries(type = "series", page = page, sort = sort, genre = genre, status = status)

    suspend fun getEpisodeById(id: String) = api.getEpisodeById(id)

    // â”€â”€ Search â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun search(query: String) = api.search(query)

    // â”€â”€ Streaming â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun resolveMoviePlayback(id: String): PlaybackResult = api.resolveMoviePlayback(id)
    suspend fun resolveEpisodePlayback(id: String): PlaybackResult = api.resolveEpisodePlayback(id)

    // â”€â”€ Reviews â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getReviews(targetType: String, targetId: String) =
        api.getReviews(targetType, targetId)

    suspend fun submitReview(targetType: String, targetId: String, rating: Int, comment: String?) =
        api.submitReview(targetType, targetId, rating, comment)

    suspend fun deleteReview(reviewId: String) = api.deleteReview(reviewId)

    // â”€â”€ Watchlist â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun getWatchlistStatus(targetType: String, targetId: String) =
        api.getWatchlistStatus(targetType, targetId)

    suspend fun toggleWatchlist(targetType: String, targetId: String) =
        api.toggleWatchlist(targetType, targetId)

    // â”€â”€ History & Views â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun saveProgress(targetType: String, targetId: String, progressSeconds: Int) =
        api.saveProgress(targetType, targetId, progressSeconds)

    suspend fun recordView(targetType: String, targetId: String) =
        api.recordView(targetType, targetId)

    // â”€â”€ Content Requests â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    suspend fun submitRequest(title: String, type: String, note: String?) =
        api.submitRequest(title, type, note)

    suspend fun getMyRequests() = api.getMyRequests()

    suspend fun cancelRequest(requestId: String) = api.cancelRequest(requestId)
}
