package com.qzone.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Reward(
    val id: String,
    val brandName: String,
    val description: String,
    val pointsCost: Int,
    val expiryDate: String,
    val terms: String,
    val qrCodePlaceholder: String = "",
    val imageUrl: String? = null
) : Parcelable

@Parcelize
data class RewardRedemption(
    val rewardId: String,
    val redeemedAt: String,
    val status: RedemptionStatus
) : Parcelable

enum class RedemptionStatus {
    AVAILABLE,
    REDEEMED,
    EXPIRED
}
