package com.qzone.domain.repository

import com.qzone.data.model.Reward
import com.qzone.data.model.UserCoupon
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    val availableRewards: Flow<List<Reward>>
    suspend fun getReward(id: String): Reward?
    suspend fun redeemReward(reward: Reward): Boolean
    suspend fun getUserCoupons(page: Int = 1, pageSize: Int = 20): List<UserCoupon>

    class InsufficientPointsException(message: String) : Exception(message)
}
