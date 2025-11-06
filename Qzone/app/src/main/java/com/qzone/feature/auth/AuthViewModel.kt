package com.qzone.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.AuthResult
import com.qzone.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationComplete: Boolean = false
)

class AuthViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRegisterUsernameChanged(value: String) {
        _registerState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onRegisterEmailChanged(value: String) {
        _registerState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onRegisterPasswordChanged(value: String) {
        _registerState.update { it.copy(password = value, errorMessage = null) }
    }

    fun signIn(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = uiState.value
            if (state.email.isBlank() || state.password.isBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "请输入邮箱和密码") }
                return@launch
            }
            val result: AuthResult = repository.signIn(state.email, state.password)
            if (result.success) {
                onSuccess()
                _uiState.update { it.copy(isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.errorMessage) }
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = registerState.value
            if (state.username.isBlank() || state.email.isBlank() || state.password.length < 6) {
                _registerState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "请填写用户名、邮箱，并保证密码至少 6 位"
                    )
                }
                return@launch
            }
            val result = repository.register(state.username, state.email, state.password)
            if (result.success) {
                _registerState.update { it.copy(isLoading = false, registrationComplete = true) }
                onSuccess()
            } else {
                _registerState.update { it.copy(isLoading = false, errorMessage = result.errorMessage) }
            }
        }
    }

    companion object {
        fun factory(repository: UserRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(repository) as T
            }
        }
    }
}
