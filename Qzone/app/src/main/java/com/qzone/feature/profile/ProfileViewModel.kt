package com.qzone.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.EditableProfile
import com.qzone.data.model.UserProfile
import com.qzone.data.repository.LocalSurveyRepository
import com.qzone.domain.repository.RewardRepository
import com.qzone.domain.repository.SurveyRepository
import com.qzone.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.qzone.util.QLog

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val nextRewardCost: Int? = null,
    val recentRedemptions: List<RedemptionDisplayItem> = emptyList()
)

data class RedemptionDisplayItem(
    val rewardId: String,
    val rewardName: String,
    val pointsCost: Int,
    val redeemedAt: String
)

data class EditProfileUiState(
    val name: String = "",
    val isSaving: Boolean = false,
    val message: String? = null
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val rewardRepository: RewardRepository,
    private val localSurveyRepository: LocalSurveyRepository,
    private val surveyRepository: SurveyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow(EditProfileUiState())
    val editState: StateFlow<EditProfileUiState> = _editState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(userRepository.currentUser, rewardRepository.availableRewards) { user, rewards ->
                val nextRewardCost = rewards.minByOrNull { it.pointsCost }?.pointsCost
                val redemptions = user.redemptions.mapNotNull { redemption ->
                    rewards.find { it.id == redemption.rewardId }?.let { reward ->
                        RedemptionDisplayItem(
                            rewardId = reward.id,
                            rewardName = reward.brandName,
                            pointsCost = reward.pointsCost,
                            redeemedAt = redemption.redeemedAt
                        )
                    }
                }
                Triple(user, nextRewardCost, redemptions)
            }.collect { (user, nextRewardCost, redemptions) ->
                QLog.d(TAG) { "Profile flow update points=${user.totalPoints} rewards=${redemptions.size}" }
                _uiState.update { 
                    ProfileUiState(
                        profile = user, 
                        isLoading = false, 
                        nextRewardCost = nextRewardCost,
                        recentRedemptions = redemptions.sortedByDescending { it.redeemedAt }
                    ) 
                }
                _editState.update {
                    it.copy(
                        name = user.displayName
                    )
                }
            }
        }
    }

    fun onNameChanged(value: String) {
        _editState.update { it.copy(name = value) }
    }

    fun saveEdits() {
        viewModelScope.launch {
            val snapshot = editState.value
            if (snapshot.name.isBlank()) {
                _editState.update { it.copy(message = "Display name cannot be empty") }
                return@launch
            }
            QLog.d(TAG) { "saveEdits displayName=${snapshot.name}" }
            _editState.update { it.copy(isSaving = true, message = null) }
            try {
                userRepository.updateProfile(
                    EditableProfile(
                        displayName = snapshot.name,
                        email = "", // Not used anymore
                        passwordMasked = "", // Not used anymore
                        countryRegion = "" // Not used anymore
                    )
                )
                _editState.update { it.copy(isSaving = false, message = "Username updated successfully") }
                QLog.d(TAG) { "Username updated successfully" }
            } catch (e: Exception) {
                QLog.e(TAG, e) { "Failed to update username" }
                _editState.update { 
                    it.copy(
                        isSaving = false, 
                        message = e.message ?: "Failed to update username"
                    ) 
                }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            QLog.i(TAG) { "signOut requested" }
            userRepository.signOut()
            // Delete all local survey data when user signs out
            localSurveyRepository.deleteAllSurveys()
            surveyRepository.clearCachedSurveys()
            onSignedOut()
        }
    }

    fun linkPhoneNumber(phone: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val trimmed = phone.trim()
            if (trimmed.isEmpty()) {
                onResult(false, "Please enter a valid phone number.")
                return@launch
            }
            val result = userRepository.linkPhoneNumber(trimmed)
            onResult(result.success, result.errorMessage)
        }
    }

    fun uploadAvatar(imageBytes: ByteArray, contentType: String, filename: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                QLog.d(TAG) { "Uploading avatar filename=$filename size=${imageBytes.size}" }
                val success = userRepository.uploadAvatar(imageBytes, contentType, filename)
                if (success) {
                    QLog.d(TAG) { "Avatar upload succeeded" }
                    onResult(true, "Avatar updated")
                } else {
                    QLog.w(TAG) { "Avatar upload failed via repository" }
                    onResult(false, "Failed to update avatar")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                QLog.e(TAG, t) { "Avatar upload threw exception" }
                onResult(false, t.message ?: "Failed to update avatar")
            }
        }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
        fun factory(
            userRepository: UserRepository,
            rewardRepository: RewardRepository,
            localSurveyRepository: LocalSurveyRepository,
            surveyRepository: SurveyRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(userRepository, rewardRepository, localSurveyRepository, surveyRepository) as T
                }
            }
    }
}
