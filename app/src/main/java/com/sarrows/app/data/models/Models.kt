package com.sarrows.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Genre ────────────────────────────────────────────────────────────────────

@Serializable
data class Genre(
    @SerialName("_id") val id: String,
    val name: String
)

// ─── Cast Member ─────────────────────────────────────────────────────────────

@Serializable
data class CastMember(
    @SerialName("_id") val id: String,
    val name: String,
    val character: String? = null,
    val image: String? = null,
    val order: Int = 0
)

// ─── Movie ────────────────────────────────────────────────────────────────────

@Serializable
data class Movie(
    @SerialName("_id") val id: String,
    val title: String,
    val slug: String = "",
    val description: String = "",
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val trailerUrl: String? = null,
    val externalId: String? = null,
    val duration: Int? = null,         // minutes
    val releaseYear: Int? = null,
    val genres: List<Genre> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val views: Long = 0,
    val status: String = "published",
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ─── Series ───────────────────────────────────────────────────────────────────

@Serializable
data class Series(
    @SerialName("_id") val id: String,
    val title: String,
    val slug: String = "",
    val description: String = "",
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val externalId: String? = null,
    val totalSeasons: Int? = null,
    val releaseYear: Int? = null,
    val genres: List<Genre> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val status: String = "completed",  // "ongoing" | "completed"
    val type: String = "anime",        // "anime" | "series"
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val views: Long = 0,
    val publishStatus: String = "published",
    val episodes: List<Episode> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ─── Episode ─────────────────────────────────────────────────────────────────

@Serializable
data class Episode(
    @SerialName("_id") val id: String,
    val series: String,               // parent Series id
    val season: Int,
    val episodeNumber: Int,
    val title: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ─── User (session) ───────────────────────────────────────────────────────────

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val image: String? = null,
    val role: String = "user"
)

@Serializable
data class SessionResponse(
    val user: User? = null,
    val expires: String? = null
)

// ─── Review ───────────────────────────────────────────────────────────────────

@Serializable
data class ReviewUser(
    @SerialName("_id") val id: String,
    val nickname: String,
    val image: String? = null
)

@Serializable
data class Review(
    @SerialName("_id") val id: String,
    val user: ReviewUser,
    val targetType: String,
    val targetId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ─── Content Request ─────────────────────────────────────────────────────────

@Serializable
data class ContentRequest(
    @SerialName("_id") val id: String,
    val user: String,
    val title: String,
    val type: String,
    val note: String? = null,
    val status: String = "pending",
    val adminNote: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ─── API Response wrappers ────────────────────────────────────────────────────

@Serializable
data class MoviesResponse(
    val movies: List<Movie>,
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1
)

@Serializable
data class SeriesResponse(
    val series: List<Series>,
    val total: Int = 0,
    val page: Int = 1,
    val totalPages: Int = 1
)

@Serializable
data class MovieDetailResponse(val movie: Movie)

@Serializable
data class EpisodeDetailResponse(val episode: Episode)

@Serializable
data class ReviewsResponse(val reviews: List<Review>)

@Serializable
data class ReviewResponse(val review: Review)

@Serializable
data class RequestsResponse(val requests: List<ContentRequest>)

@Serializable
data class RequestResponse(val request: ContentRequest)

@Serializable
data class SearchResponse(
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList()
)

@Serializable
data class WatchlistStatusResponse(val inWatchlist: Boolean)

@Serializable
data class ViewsResponse(
    val ok: Boolean,
    val counted: Boolean = false
)

@Serializable
data class WatchHistoryResponse(val ok: Boolean)

@Serializable
data class EmbedUrlResponse(val url: String)

@Serializable
data class CsrfResponse(val csrfToken: String)

@Serializable
data class SignupRequest(
    val nickname: String,
    val email: String,
    val password: String
)

@Serializable
data class ReviewRequest(
    val targetType: String,
    val targetId: String,
    val rating: Int,
    val comment: String? = null
)

@Serializable
data class WatchlistToggleRequest(
    val targetType: String,
    val targetId: String
)

@Serializable
data class WatchHistoryRequest(
    val targetType: String,
    val targetId: String,
    val progressSeconds: Int
)

@Serializable
data class ViewRequest(
    val targetType: String,
    val targetId: String
)

@Serializable
data class ContentRequestBody(
    val title: String,
    val type: String,
    val note: String? = null
)

@Serializable
data class ApiError(
    val error: String = "",
    val fieldErrors: Map<String, String>? = null
)

// ─── Sealed result ────────────────────────────────────────────────────────────

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0, val fieldErrors: Map<String, String>? = null) : ApiResult<Nothing>()
}
