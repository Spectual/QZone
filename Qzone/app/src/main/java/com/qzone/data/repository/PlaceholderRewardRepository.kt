package com.qzone.data.repository

import com.qzone.data.model.Reward
import com.qzone.data.placeholder.PlaceholderDataSource
import com.qzone.domain.repository.RewardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaceholderRewardRepository(
    private val userRepository: com.qzone.domain.repository.UserRepository
) : RewardRepository {

    private val rewardsFlow = MutableStateFlow(PlaceholderDataSource.sampleRewards())

    override val availableRewards: Flow<List<Reward>> = rewardsFlow.asStateFlow()

    override suspend fun getReward(id: String): Reward? {
        return rewardsFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun redeemReward(reward: Reward): Boolean {
        // In a real app, this would be an API call that handles both points deduction and reward issuance.
        // Here we coordinate the local updates.
        return try {
            userRepository.deductPoints(reward.pointsCost)
            userRepository.recordRedemption(reward)
            true
        } catch (e: Exception) {
            false
        }
    }
}
