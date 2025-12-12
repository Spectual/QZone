package com.qzone.data.repository

import android.util.Log
import com.qzone.data.model.Reward
import com.qzone.data.model.UserCoupon
import com.qzone.data.network.QzoneApiClient
import com.qzone.data.network.model.RedeemCouponRequest
import com.qzone.data.network.model.CouponListRequest
import com.qzone.data.network.model.NetworkCouponRecord
import com.qzone.data.placeholder.PlaceholderDataSource
import com.qzone.domain.repository.RewardRepository
import com.qzone.domain.repository.RewardRepository.InsufficientPointsException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaceholderRewardRepository(
    private val userRepository: com.qzone.domain.repository.UserRepository,
    private val apiService: com.qzone.data.network.QzoneApiService = QzoneApiClient.service
) : RewardRepository {

    private val rewardsFlow = MutableStateFlow(PlaceholderDataSource.sampleRewards())

    override val availableRewards: Flow<List<Reward>> = rewardsFlow.asStateFlow()

    override suspend fun getReward(id: String): Reward? {
        return rewardsFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun redeemReward(reward: Reward): Boolean {
        return try {
            val response = runCatching {
                val couponName = reward.brandName.ifBlank { reward.description ?: reward.id }
                apiService.redeemCoupon(
                    RedeemCouponRequest(
                        requiredPoints = reward.pointsCost,
                        couponName = couponName
                    )
                )
            }.getOrElse { throwable ->
                Log.e(TAG, "Redeem API call failed", throwable)
                throw throwable
            }
            if (!response.success || response.data == null) {
                Log.w(TAG, "Redeem API returned failure: ${response.msg}")
                throw InsufficientPointsException(response.msg ?: "Redemption failed")
            }
            userRepository.updatePoints(response.data)
            userRepository.recordRedemption(reward)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Redeem reward failed", t)
            throw t
        }
    }

    override suspend fun getUserCoupons(page: Int, pageSize: Int): List<UserCoupon> {
        return try {
            val response = apiService.getUserCoupons(
                CouponListRequest(page = page, pageSize = pageSize)
            )
            if (response.success && response.data != null) {
                response.data.records.map { it.toDomain() }
            } else {
                Log.w(TAG, "Coupon list request failed: ${response.msg}")
                emptyList()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load coupon history", t)
            emptyList()
        }
    }

    private fun NetworkCouponRecord.toDomain(): UserCoupon =
        UserCoupon(
            id = documentId,
            couponName = couponName,
            deductedPoints = deductedPoints,
            createdAt = createTime,
            updatedAt = updateTime
        )

    companion object {
        private const val TAG = "RewardRepository"
    }
}
