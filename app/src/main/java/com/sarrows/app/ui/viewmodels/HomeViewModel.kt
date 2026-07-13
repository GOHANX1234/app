package com.sarrows.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarrows.app.data.models.*
import com.sarrows.app.data.repository.SarrowsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val trendingMovies: List<Movie> = emptyList(),
    val latestMovies: List<Movie> = emptyList(),
    val topRatedMovies: List<Movie> = emptyList(),
    val trendingAnime: List<Series> = emptyList(),
    val ongoingAnime: List<Series> = emptyList(),
    val trendingSeries: List<Series> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SarrowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            try {
                val trending  = async { repository.getMovies(sort = "views") }
                val latest    = async { repository.getMovies(sort = "latest") }
                val topRated  = async { repository.getMovies(sort = "rating") }
                val anime     = async { repository.getAnime(sort = "views") }
                val ongoing   = async { repository.getAnime(sort = "latest", status = "ongoing") }
                val series    = async { repository.getSeries(sort = "views") }

                _uiState.value = HomeUiState(
                    isLoading     = false,
                    trendingMovies = (trending.await() as? ApiResult.Success)?.data?.movies ?: emptyList(),
                    latestMovies   = (latest.await()   as? ApiResult.Success)?.data?.movies ?: emptyList(),
                    topRatedMovies = (topRated.await() as? ApiResult.Success)?.data?.movies ?: emptyList(),
                    trendingAnime  = (anime.await()    as? ApiResult.Success)?.data?.series ?: emptyList(),
                    ongoingAnime   = (ongoing.await()  as? ApiResult.Success)?.data?.series ?: emptyList(),
                    trendingSeries = (series.await()   as? ApiResult.Success)?.data?.series ?: emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState(isLoading = false, error = e.message)
            }
        }
    }
}
