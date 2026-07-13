package com.sarrows.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarrows.app.data.models.*
import com.sarrows.app.data.repository.SarrowsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val series: Series? = null,
    val reviews: List<Review> = emptyList(),
    val inWatchlist: Boolean = false,
    val userReview: Review? = null,
    val error: String? = null,
    val reviewSubmitting: Boolean = false,
    val reviewError: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: SarrowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    fun loadMovie(id: String, userId: String?) {
        currentUserId = userId
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            val movieDef    = async { repository.getMovieById(id) }
            val reviewsDef  = async { repository.getReviews("Movie", id) }
            val watchlistDef = async { repository.getWatchlistStatus("Movie", id) }

            val movie = (movieDef.await() as? ApiResult.Success)?.data
            val reviews = (reviewsDef.await() as? ApiResult.Success)?.data ?: emptyList()
            val inWatchlist = (watchlistDef.await() as? ApiResult.Success)?.data ?: false
            val userReview = reviews.find { it.user.id == userId }

            _uiState.value = DetailUiState(
                isLoading = false,
                movie = movie,
                reviews = reviews,
                inWatchlist = inWatchlist,
                userReview = userReview,
                error = if (movie == null) "Content not found" else null
            )
        }
    }

    fun loadSeries(series: Series, userId: String?) {
        currentUserId = userId
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            val reviewsDef   = async { repository.getReviews("Series", series.id) }
            val watchlistDef = async { repository.getWatchlistStatus("Series", series.id) }

            val reviews = (reviewsDef.await() as? ApiResult.Success)?.data ?: emptyList()
            val inWatchlist = (watchlistDef.await() as? ApiResult.Success)?.data ?: false
            val userReview = reviews.find { it.user.id == userId }

            _uiState.value = DetailUiState(
                isLoading = false,
                series = series,
                reviews = reviews,
                inWatchlist = inWatchlist,
                userReview = userReview
            )
        }
    }

    fun toggleWatchlist(targetType: String, targetId: String) {
        viewModelScope.launch {
            when (val result = repository.toggleWatchlist(targetType, targetId)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(inWatchlist = result.data)
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    fun submitReview(targetType: String, targetId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(reviewSubmitting = true, reviewError = null)
            when (val result = repository.submitReview(targetType, targetId, rating, comment)) {
                is ApiResult.Success -> {
                    val reviews = _uiState.value.reviews.toMutableList()
                    val idx = reviews.indexOfFirst { it.id == result.data.id }
                    if (idx >= 0) reviews[idx] = result.data else reviews.add(0, result.data)
                    _uiState.value = _uiState.value.copy(
                        reviewSubmitting = false,
                        reviews = reviews,
                        userReview = result.data
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    reviewSubmitting = false, reviewError = result.message
                )
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            when (repository.deleteReview(reviewId)) {
                is ApiResult.Success -> {
                    val reviews = _uiState.value.reviews.filter { it.id != reviewId }
                    _uiState.value = _uiState.value.copy(reviews = reviews, userReview = null)
                }
                is ApiResult.Error -> { /* ignore */ }
            }
        }
    }

    fun recordView(targetType: String, targetId: String) {
        viewModelScope.launch { repository.recordView(targetType, targetId) }
    }
}
