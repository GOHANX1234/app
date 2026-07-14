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
            try {
                val movieDef     = async { repository.getMovieById(id) }
                val reviewsDef   = async { repository.getReviews("Movie", id) }
                val watchlistDef = async { repository.getWatchlistStatus("Movie", id) }

                val movie       = (movieDef.await()     as? ApiResult.Success)?.data
                val reviews     = (reviewsDef.await()   as? ApiResult.Success)?.data ?: emptyList()
                val inWatchlist = (watchlistDef.await() as? ApiResult.Success)?.data ?: false
                val userReview  = reviews.find { it.user.id == userId }

                _uiState.value = DetailUiState(
                    isLoading   = false,
                    movie       = movie,
                    reviews     = reviews,
                    inWatchlist = inWatchlist,
                    userReview  = userReview,
                    error       = if (movie == null) "Content not found" else null
                )
            } catch (e: Exception) {
                _uiState.value = DetailUiState(isLoading = false, error = e.message ?: "Failed to load content")
            }
        }
    }

    /**
     * Entry point when navigating to a series/anime detail via route (only type + id are
     * available). Looks up the full Series object from the API and delegates to loadSeries().
     */
    fun loadSeriesById(id: String, userId: String?) {
        currentUserId = userId
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            try {
                val series = repository.findSeriesById(id)
                if (series != null) {
                    loadSeries(series, userId)
                } else {
                    _uiState.value = DetailUiState(isLoading = false, error = "Content not found")
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load content"
                )
            }
        }
    }

    fun loadSeries(series: Series, userId: String?) {
        currentUserId = userId
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            try {
                val reviewsDef   = async { repository.getReviews("Series", series.id) }
                val watchlistDef = async { repository.getWatchlistStatus("Series", series.id) }

                val reviews     = (reviewsDef.await()   as? ApiResult.Success)?.data ?: emptyList()
                val inWatchlist = (watchlistDef.await() as? ApiResult.Success)?.data ?: false
                val userReview  = reviews.find { it.user.id == userId }

                _uiState.value = DetailUiState(
                    isLoading   = false,
                    series      = series,
                    reviews     = reviews,
                    inWatchlist = inWatchlist,
                    userReview  = userReview
                )
            } catch (e: Exception) {
                _uiState.value = DetailUiState(isLoading = false, error = e.message ?: "Failed to load content")
            }
        }
    }

    fun toggleWatchlist(targetType: String, targetId: String) {
        viewModelScope.launch {
            try {
                when (val result = repository.toggleWatchlist(targetType, targetId)) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(inWatchlist = result.data)
                    is ApiResult.Error   -> { /* silently fail */ }
                }
            } catch (_: Exception) { /* silently fail */ }
        }
    }

    fun submitReview(targetType: String, targetId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(reviewSubmitting = true, reviewError = null)
            try {
                when (val result = repository.submitReview(targetType, targetId, rating, comment)) {
                    is ApiResult.Success -> {
                        val reviews = _uiState.value.reviews.toMutableList()
                        val idx = reviews.indexOfFirst { it.id == result.data.id }
                        if (idx >= 0) reviews[idx] = result.data else reviews.add(0, result.data)
                        _uiState.value = _uiState.value.copy(
                            reviewSubmitting = false,
                            reviews          = reviews,
                            userReview       = result.data
                        )
                    }
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                        reviewSubmitting = false, reviewError = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    reviewSubmitting = false, reviewError = e.message ?: "Failed to submit review"
                )
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            try {
                when (repository.deleteReview(reviewId)) {
                    is ApiResult.Success -> {
                        val reviews = _uiState.value.reviews.filter { it.id != reviewId }
                        _uiState.value = _uiState.value.copy(reviews = reviews, userReview = null)
                    }
                    is ApiResult.Error -> { /* ignore */ }
                }
            } catch (_: Exception) { /* ignore */ }
        }
    }

    fun recordView(targetType: String, targetId: String) {
        viewModelScope.launch {
            try { repository.recordView(targetType, targetId) } catch (_: Exception) { }
        }
    }
}
