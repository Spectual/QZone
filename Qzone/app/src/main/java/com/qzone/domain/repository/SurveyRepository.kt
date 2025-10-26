package com.qzone.domain.repository

import com.qzone.data.model.Survey
import kotlinx.coroutines.flow.Flow

interface SurveyRepository {
    val nearbySurveys: Flow<List<Survey>>
    suspend fun refreshNearby()
    suspend fun getSurveyById(id: String): Survey?
    suspend fun markSurveyCompleted(id: String)
}
