package com.sarrows.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarrows.app.data.remote.PlaybackResult
import com.sarrows.app.data.repository.SarrowsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlayerUiState {
    object Idle : PlayerUiState()
    object Loading : PlayerUiState()
    data class StreamReady(val url: String, val cookieHeader: String) : PlayerUiState()
    data class EmbedReady(val url: String) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
    object Unauthenticated : PlayerUiState()
    object RateLimited : PlayerUiState()
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: SarrowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: kotlinx.coroutines.Job? = null

    fun loadMovie(id: String) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            _uiState.value = when (val result = repository.resolveMoviePlayback(id)) {
                is PlaybackResult.Stream -> PlayerUiState.StreamReady(result.url, result.cookieHeader)
                is PlaybackResult.Embed  -> PlayerUiState.EmbedReady(result.url)
                is PlaybackResult.Error  -> PlayerUiState.Error(result.message)
                is PlaybackResult.Unauthenticated -> PlayerUiState.Unauthenticated
                is PlaybackResult.RateLimited -> PlayerUiState.RateLimited
            }
        }
    }

    fun loadEpisode(id: String) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            _uiState.value = when (val result = repository.resolveEpisodePlayback(id)) {
                is PlaybackResult.Stream -> PlayerUiState.StreamReady(result.url, result.cookieHeader)
                is PlaybackResult.Embed  -> PlayerUiState.EmbedReady(result.url)
                is PlaybackResult.Error  -> PlayerUiState.Error(result.message)
                is PlaybackResult.Unauthenticated -> PlayerUiState.Unauthenticated
                is PlaybackResult.RateLimited -> PlayerUiState.RateLimited
            }
        }
    }

    fun saveProgress(targetType: String, targetId: String, progressSeconds: Int) {
        viewModelScope.launch {
            repository.saveProgress(targetType, targetId, progressSeconds)
        }
    }

    fun recordView(targetType: String, targetId: String) {
        viewModelScope.launch { repository.recordView(targetType, targetId) }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}
