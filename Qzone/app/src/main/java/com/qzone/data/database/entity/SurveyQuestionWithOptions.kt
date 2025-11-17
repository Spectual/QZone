@file:Suppress("UNCHECKED_CAST", "RedundantSuppression")
package com.qzone.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Wrapper class for loading a question with all its associated options.
 * This is NOT an @Entity - it's used for Room relations.
 * 
 * Note: This class is used with @Transaction in DAO queries to automatically
 * load questions with their associated options through Room's Relation system.
 */
data class SurveyQuestionWithOptions(
    @Embedded
    val question: SurveyQuestionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val options: List<SurveyOptionEntity> = emptyList()
) {
    fun toSurveyQuestion() = question.toSurveyQuestion(
        options = options.map { it.toSurveyOption() }
    )
}
