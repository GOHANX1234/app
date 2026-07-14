package com.sarrows.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarrows.app.data.models.*
import com.sarrows.app.data.repository.SarrowsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequestsUiState(
    val isLoading: Boolean = true,
    val requests: List<ContentRequest> = emptyList(),
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null,
    val submitError: String? = null
)

@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val repository: SarrowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestsUiState())
    val uiState: StateFlow<RequestsUiState> = _uiState.asStateFlow()

    init { loadRequests() }

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                when (val result = repository.getMyRequests()) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                        isLoading = false, requests = result.data
                    )
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false, error = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = e.message ?: "Failed to load requests"
                )
            }
        }
    }

    fun submitRequest(title: String, type: String, note: String?) {
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(submitError = "Please enter a title")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            try {
                when (val result = repository.submitRequest(title.trim(), type, note?.takeIf { it.isNotBlank() })) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                        isSubmitting  = false,
                        submitSuccess = true,
                        requests      = listOf(result.data) + _uiState.value.requests
                    )
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                        isSubmitting = false, submitError = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false, submitError = e.message ?: "Failed to submit request"
                )
            }
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            try {
                when (repository.cancelRequest(requestId)) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                        requests = _uiState.value.requests.filter { it.id != requestId }
                    )
                    is ApiResult.Error -> { /* ignore */ }
                }
            } catch (_: Exception) { /* ignore */ }
        }
    }

    fun resetSubmitState() {
        _uiState.value = _uiState.value.copy(submitSuccess = false, submitError = null)
    }
}
