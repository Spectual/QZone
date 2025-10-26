package com.qzone.data.repository

import com.qzone.data.model.Survey
import com.qzone.data.placeholder.PlaceholderDataSource
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaceholderSurveyRepository : SurveyRepository {

    private val completedIds = mutableSetOf<String>()
    private val surveysFlow = MutableStateFlow(applyCompletion(PlaceholderDataSource.sampleSurveys()))

    override val nearbySurveys: Flow<List<Survey>> = surveysFlow.asStateFlow()

    override suspend fun refreshNearby() {
        // Simulate network delay for UI preview/testing purposes.
        delay(400)
        surveysFlow.value = applyCompletion(PlaceholderDataSource.sampleSurveys())
    }

    override suspend fun getSurveyById(id: String): Survey? {
        return surveysFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun markSurveyCompleted(id: String) {
        completedIds.add(id)
        surveysFlow.value = surveysFlow.value.map { survey ->
            if (survey.id == id) survey.copy(isCompleted = true) else survey
        }
    }

    private fun applyCompletion(source: List<Survey>): List<Survey> {
        if (completedIds.isEmpty()) return source
        return source.map { survey ->
            if (completedIds.contains(survey.id)) survey.copy(isCompleted = true) else survey
        }
    }
}
