@file:Suppress("UNCHECKED_CAST", "RedundantSuppression")
package com.qzone.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.qzone.data.model.Survey

/**
 * Wrapper class for loading a survey with all its associated questions and their options.
 * This is NOT an @Entity - it's used for Room relations.
 * 
 * Note: This class is used with @Transaction in DAO queries to automatically
 * load surveys with their associated questions and options through Room's Relation system.
 */
data class SurveyWithQuestions(
    @Embedded
    val survey: SurveyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "surveyId"
    )
    val questions: List<SurveyQuestionWithOptions> = emptyList()
) {
    fun toSurvey(): Survey {
        return survey.toSurvey(
            questions = questions.map { it.toSurveyQuestion() }
        )
    }
}
