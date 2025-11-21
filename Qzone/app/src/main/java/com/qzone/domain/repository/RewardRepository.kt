package com.qzone.domain.repository

import com.qzone.data.model.Reward
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    val availableRewards: Flow<List<Reward>>
    suspend fun getReward(id: String): Reward?
    suspend fun redeemReward(reward: Reward): Boolean
}
