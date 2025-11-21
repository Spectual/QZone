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

data class RewardsUiState(
    val rewards: List<Reward> = emptyList()
)

class RewardsViewModel(private val rewardRepository: RewardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            rewardRepository.availableRewards.collect { list ->
                _uiState.update { it.copy(rewards = list) }
            }
        }
    }

    fun redeemReward(reward: Reward, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = rewardRepository.redeemReward(reward)
            onResult(success)
        }
    }

    companion object {
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
            _uiState.update { it.copy(reward = repository.getReward(rewardId)) }
        }
    }

    companion object {
        fun factory(repository: RewardRepository, rewardId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RewardDetailViewModel(repository, rewardId) as T
                }
            }
    }
}
