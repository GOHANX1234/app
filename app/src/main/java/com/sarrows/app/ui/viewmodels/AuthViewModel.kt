package com.sarrows.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarrows.app.data.models.ApiResult
import com.sarrows.app.data.models.User
import com.sarrows.app.data.repository.SarrowsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User? = null) : AuthUiState()
    data class Error(val message: String, val fieldErrors: Map<String, String>? = null) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: SarrowsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            when (val result = repository.getSession()) {
                is ApiResult.Success -> {
                    _currentUser.value = result.data.user
                    if (result.data.user != null) _uiState.value = AuthUiState.Success(result.data.user)
                }
                is ApiResult.Error -> { /* stay idle */ }
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.login(email.trim(), password)) {
                is ApiResult.Success -> {
                    // Fetch session to get user info
                    when (val session = repository.getSession()) {
                        is ApiResult.Success -> {
                            _currentUser.value = session.data.user
                            _uiState.value = AuthUiState.Success(session.data.user)
                        }
                        is ApiResult.Error -> _uiState.value = AuthUiState.Success()
                    }
                }
                is ApiResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }

    fun signUp(nickname: String, email: String, password: String, confirmPassword: String) {
        when {
            nickname.isBlank() || email.isBlank() || password.isBlank() ->
                _uiState.value = AuthUiState.Error("Please fill in all fields")
            password != confirmPassword ->
                _uiState.value = AuthUiState.Error("Passwords do not match")
            password.length < 8 ->
                _uiState.value = AuthUiState.Error("Password must be at least 8 characters")
            else -> viewModelScope.launch {
                _uiState.value = AuthUiState.Loading
                when (val result = repository.signUp(nickname.trim(), email.trim(), password)) {
                    is ApiResult.Success -> _uiState.value = AuthUiState.Success()
                    is ApiResult.Error   -> _uiState.value = AuthUiState.Error(result.message, result.fieldErrors)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.logout()
            _currentUser.value = null
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() { _uiState.value = AuthUiState.Idle }
}
