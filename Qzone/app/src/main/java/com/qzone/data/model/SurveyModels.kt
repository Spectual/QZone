package com.qzone.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class SurveyCategory {
    EXPERIENCE,
    TRANSPORT,
    FOOD,
    EVENT,
    LIFESTYLE,
    OTHER
}

enum class SurveyQuestionType {
    SINGLE_CHOICE,
    MULTI_CHOICE,
    SHORT_TEXT,
    RATING
}

@Parcelize
data class SurveyQuestion(
    val id: String,
    val prompt: String,
    val type: SurveyQuestionType,
    val options: List<String> = emptyList(),
    val helperText: String? = null
) : Parcelable

@Parcelize
data class Survey(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: SurveyCategory,
    val locationLabel: String,
    val latitude: Double,
    val longitude: Double,
    val points: Int,
    val distanceMeters: Int? = null,
    val questions: List<SurveyQuestion> = emptyList(),
    val estimatedMinutes: Int = 5,
    val isCompleted: Boolean = false
) : Parcelable

@Parcelize
data class SurveyHistoryItem(
    val id: String,
    val surveyId: String,
    val title: String,
    val completedAt: String,
    val pointsEarned: Int,
    val locationLabel: String
) : Parcelable
