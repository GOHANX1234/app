package com.sarrows.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarrows.app.data.models.*
import com.sarrows.app.data.repository.SarrowsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SarrowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _uiState.value = _uiState.value.copy(query = q)
        searchJob?.cancel()
        if (q.trim().length < 2) {
            _uiState.value = _uiState.value.copy(movies = emptyList(), series = emptyList(), isLoading = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.search(q.trim())) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movies = result.data.movies,
                    series = result.data.series
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, error = result.message
                )
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = SearchUiState()
    }
}
