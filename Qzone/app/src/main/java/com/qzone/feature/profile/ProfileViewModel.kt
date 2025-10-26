package com.qzone.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.EditableProfile
import com.qzone.data.model.UserProfile
import com.qzone.domain.repository.RewardRepository
import com.qzone.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val nextRewardCost: Int? = null
)

data class EditProfileUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "******",
    val country: String = "",
    val isSaving: Boolean = false,
    val message: String? = null
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val rewardRepository: RewardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow(EditProfileUiState())
    val editState: StateFlow<EditProfileUiState> = _editState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(userRepository.currentUser, rewardRepository.availableRewards) { user, rewards ->
                val nextRewardCost = rewards.minByOrNull { it.pointsCost }?.pointsCost
                user to nextRewardCost
            }.collect { (user, nextRewardCost) ->
                _uiState.update { ProfileUiState(profile = user, isLoading = false, nextRewardCost = nextRewardCost) }
                _editState.update {
                    it.copy(
                        name = user.displayName,
                        email = user.email,
                        country = user.countryRegion
                    )
                }
            }
        }
    }

    fun onNameChanged(value: String) {
        _editState.update { it.copy(name = value) }
    }

    fun onEmailChanged(value: String) {
        _editState.update { it.copy(email = value) }
    }

    fun onPasswordChanged(value: String) {
        _editState.update { it.copy(password = value.ifEmpty { "" }) }
    }

    fun onCountryChanged(value: String) {
        _editState.update { it.copy(country = value) }
    }

    fun saveEdits() {
        viewModelScope.launch {
            val snapshot = editState.value
            _editState.update { it.copy(isSaving = true, message = null) }
            userRepository.updateProfile(
                EditableProfile(
                    displayName = snapshot.name,
                    email = snapshot.email,
                    passwordMasked = snapshot.password,
                    countryRegion = snapshot.country
                )
            )
            _editState.update { it.copy(isSaving = false, message = "Profile updated") }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            userRepository.signOut()
            onSignedOut()
        }
    }

    companion object {
        fun factory(userRepository: UserRepository, rewardRepository: RewardRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(userRepository, rewardRepository) as T
                }
            }
    }
}
