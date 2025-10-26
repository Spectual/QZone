package com.qzone.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String?,
    val levelLabel: String,
    val totalPoints: Int,
    val tierPointsGoal: Int,
    val location: String,
    val countryRegion: String,
    val history: List<SurveyHistoryItem>,
    val redemptions: List<RewardRedemption>
) : Parcelable {
    val progressFraction: Float
        get() = if (tierPointsGoal > 0) totalPoints.toFloat() / tierPointsGoal else 0f
}

@Parcelize
data class EditableProfile(
    val displayName: String,
    val email: String,
    val passwordMasked: String,
    val countryRegion: String
) : Parcelable

/**
 * Placeholder structure for authentication work that will later be delegated to the Java backend impl.
 */
data class AuthResult(
    val success: Boolean,
    val errorMessage: String? = null
)
