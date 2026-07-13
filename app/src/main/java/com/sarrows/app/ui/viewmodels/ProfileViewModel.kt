package com.sarrows.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarrows.app.data.models.*
import com.sarrows.app.data.repository.SarrowsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: SarrowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState(isLoading = true)
            when (val result = repository.getSession()) {
                is ApiResult.Success -> _uiState.value = ProfileUiState(
                    isLoading = false, user = result.data.user
                )
                is ApiResult.Error -> _uiState.value = ProfileUiState(
                    isLoading = false, error = result.message
                )
            }
        }
    }
}
