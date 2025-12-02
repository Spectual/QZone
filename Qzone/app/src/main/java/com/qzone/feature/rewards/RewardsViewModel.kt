package com.qzone.feature.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.Reward
import com.qzone.domain.repository.RewardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.qzone.util.QLog

data class RewardsUiState(
    val rewards: List<Reward> = emptyList()
)

class RewardsViewModel(private val rewardRepository: RewardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            rewardRepository.availableRewards.collect { list ->
                QLog.d(TAG) { "Rewards flow update count=${list.size}" }
                _uiState.update { it.copy(rewards = list) }
            }
        }
    }

    fun redeemReward(reward: Reward, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                QLog.d(TAG) { "redeemReward requested id=${reward.id} cost=${reward.pointsCost}" }
                val success = rewardRepository.redeemReward(reward)
                if (success) {
                    QLog.i(TAG) { "Reward ${reward.id} redeemed successfully" }
                    onResult(true, "Redemption successful")
                } else {
                    QLog.w(TAG) { "Reward ${reward.id} redeem returned false" }
                    onResult(false, "Redemption failed, please try again later")
                }
            } catch (e: com.qzone.domain.repository.RewardRepository.InsufficientPointsException) {
                QLog.w(TAG) { "Insufficient points for reward ${reward.id}: ${e.message}" }
                onResult(false, e.message ?: "Insufficient points to redeem")
            } catch (t: Throwable) {
                QLog.e(TAG, t) { "redeemReward exception for reward=${reward.id}" }
                onResult(false, t.message ?: "Redemption failed, please try again later")
            }
        }
    }

    companion object {
        private const val TAG = "RewardsViewModel"
        fun factory(repository: RewardRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RewardsViewModel(repository) as T
            }
        }
    }
}


data class RewardDetailUiState(
    val reward: Reward? = null
)

class RewardDetailViewModel(
    private val repository: RewardRepository,
    private val rewardId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardDetailUiState())
    val uiState: StateFlow<RewardDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val reward = repository.getReward(rewardId)
            QLog.d(TAG_DETAIL) { "Loaded reward detail id=$rewardId found=${reward != null}" }
            _uiState.update { it.copy(reward = reward) }
        }
    }

    companion object {
        private const val TAG_DETAIL = "RewardDetailVM"
        fun factory(repository: RewardRepository, rewardId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RewardDetailViewModel(repository, rewardId) as T
                }
            }
    }
}
