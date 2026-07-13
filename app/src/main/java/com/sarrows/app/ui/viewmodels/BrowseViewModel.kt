package com.sarrows.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarrows.app.data.models.*
import com.sarrows.app.data.repository.SarrowsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowseUiState(
    val isLoading: Boolean = false,
    val movies: List<Movie> = emptyList(),
    val anime: List<Series> = emptyList(),
    val series: List<Series> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val sortBy: String = "latest",
    val genreFilter: String? = null,
    val error: String? = null
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: SarrowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    fun loadMovies(page: Int = 1, sort: String = "latest", genre: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getMovies(page, sort, genre)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movies = if (page == 1) result.data.movies
                    else _uiState.value.movies + result.data.movies,
                    currentPage = result.data.page,
                    totalPages = result.data.totalPages,
                    sortBy = sort,
                    genreFilter = genre
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun loadAnime(page: Int = 1, sort: String = "latest", genre: String? = null, status: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getAnime(page, sort, genre, status)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    anime = if (page == 1) result.data.series
                    else _uiState.value.anime + result.data.series,
                    currentPage = result.data.page,
                    totalPages = result.data.totalPages
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun loadSeries(page: Int = 1, sort: String = "latest", genre: String? = null, status: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getSeries(page, sort, genre, status)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    series = if (page == 1) result.data.series
                    else _uiState.value.series + result.data.series,
                    currentPage = result.data.page,
                    totalPages = result.data.totalPages
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }
}
