package com.qzone.data.model

import android.os.Parcelable
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
    val documentId: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Double? = null
) : Parcelable
