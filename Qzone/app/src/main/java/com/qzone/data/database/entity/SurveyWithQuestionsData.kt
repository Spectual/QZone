package com.qzone.data.database.entity

import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyQuestion

/**
 * Data holder class for survey with questions.
 * Used when manually combining results from separate DAO queries.
 * This approach avoids Room's relation annotation issues.
 */
data class SurveyWithQuestionsData(
    val survey: SurveyEntity,
    val questions: List<SurveyQuestionEntity> = emptyList(),
    val options: Map<String, List<SurveyOptionEntity>> = emptyMap()
) {
    fun toSurvey(): Survey {
        val surveyQuestions = questions.mapIndexed { index, questionEntity ->
            val questionOptions = options[questionEntity.id] ?: emptyList()
            questionEntity.toSurveyQuestion(
                options = questionOptions.map { it.toSurveyOption() }
            )
        }
        return survey.toSurvey(questions = surveyQuestions)
    }
}
