package com.qzone.data.repository

import com.qzone.data.model.Reward
import com.qzone.data.placeholder.PlaceholderDataSource
import com.qzone.domain.repository.RewardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaceholderRewardRepository : RewardRepository {

    private val rewardsFlow = MutableStateFlow(PlaceholderDataSource.sampleRewards())

    override val availableRewards: Flow<List<Reward>> = rewardsFlow.asStateFlow()

    override suspend fun getReward(id: String): Reward? {
        return rewardsFlow.value.firstOrNull { it.id == id }
    }
}
