package com.qzone.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize

@Parcelize
data class SurveyOption(
    val content: String,
    val label: String
) : Parcelable

@Parcelize
data class SurveyQuestion(
    val id: String,
    val type: String,
    val content: String,
    val required: Boolean,
    val options: List<SurveyOption>? = null
) : Parcelable

@Parcelize
data class Survey(
    val id: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val points: Int = 0,
    val questions: List<SurveyQuestion> = emptyList(),
    val questionCount: Int = 0,
    val isCompleted: Boolean = false,
    val status: SurveyStatus = SurveyStatus.EMPTY,
    val currentQuestionIndex: Int = 0,
    val answers: Map<String, List<String>> = emptyMap()
) : Parcelable

enum class SurveyStatus {
    EMPTY,
    IN_PROGRESS,
    PARTIAL,
    COMPLETE
}

@Parcelize
data class SurveyHistoryItem(
    val id: String,
    val surveyId: String,
    val title: String,
    val completedAt: String,
    val pointsEarned: Int,
    val locationLabel: String
) : Parcelable

@Parcelize
data class NearbyLocation(
    @Json(name = "documentId") val documentId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "distanceMile") val distance: Double? = null,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "questionNumber") val questionNumber: Int? = null,
    @Json(name = "points") val points: Int? = null,
    @Json(name = "status") val status: String? = null
) : Parcelable
