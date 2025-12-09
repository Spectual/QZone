package com.qzone.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.util.QLog
import com.qzone.data.model.AuthResult
import com.qzone.domain.repository.UserRepository
import com.qzone.domain.repository.SurveyRepository
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

class AuthViewModel(
    private val userRepository: UserRepository,
    private val surveyRepository: SurveyRepository
) : ViewModel() {

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
            QLog.d(TAG) { "signIn requested email=${state.email}" }
            if (state.email.isBlank() || state.password.isBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Please enter email and password") }
                return@launch
            }
            val result: AuthResult = userRepository.signIn(state.email, state.password)
            if (result.success) {
                refreshUserProfile()
                refreshUserResponses()
                onSuccess()
                _uiState.update { it.copy(isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.errorMessage) }
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onFailure: (String?) -> Unit) {
        QLog.d(TAG) { "signInWithGoogle tokenLength=${idToken.length}" }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = userRepository.signInWithGoogle(idToken)
            QLog.d(TAG) { "signInWithGoogle result success=${result.success} error=${result.errorMessage}" }
            if (result.success) {
                refreshUserProfile()
                refreshUserResponses()
                onSuccess()
                _uiState.update { it.copy(isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.errorMessage) }
                onFailure(result.errorMessage)
            }
        }
    }

    fun finalizeFirebaseLogin(isThirdParty: Boolean = true, onSuccess: () -> Unit, onFailure: (String?) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = userRepository.finalizeFirebaseLogin(isThirdParty)
            if (result.success) {
                refreshUserProfile()
                refreshUserResponses()
                onSuccess()
                _uiState.update { it.copy(isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.errorMessage) }
                onFailure(result.errorMessage)
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }
            val state = registerState.value
            QLog.d(TAG) { "register requested username=${state.username} email=${state.email}" }
            if (state.username.isBlank() || state.email.isBlank() || state.password.length < 6) {
                _registerState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Please fill in username, email, and ensure password is at least 6 characters"
                    )
                }
                return@launch
            }
            val result = userRepository.register(state.username, state.email, state.password)
            if (result.success) {
                refreshUserProfile()
                refreshUserResponses()
                _registerState.update { it.copy(isLoading = false, registrationComplete = true) }
                onSuccess()
            } else {
                _registerState.update { it.copy(isLoading = false, errorMessage = result.errorMessage) }
            }
        }
    }

    private suspend fun refreshUserResponses() {
        runCatching { surveyRepository.refreshSurveyHistory() }
            .onFailure { throwable -> QLog.w(TAG) { "Failed to sync survey history after login: ${throwable.message}" } }
    }

    private suspend fun refreshUserProfile() {
        runCatching { userRepository.refreshUserProfile() }
            .onFailure { throwable -> QLog.w(TAG) { "Failed to refresh user profile after login: ${throwable.message}" } }
    }

    companion object {
        private const val TAG = "AuthViewModel"
        fun factory(
            userRepository: UserRepository,
            surveyRepository: SurveyRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(userRepository, surveyRepository) as T
            }
        }
    }
}
