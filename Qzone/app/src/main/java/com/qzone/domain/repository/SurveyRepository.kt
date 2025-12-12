package com.qzone.domain.repository

import com.qzone.data.model.Survey
import com.qzone.data.model.UserLocation
import com.qzone.data.model.UserSurveyHistoryItem
import kotlinx.coroutines.flow.Flow

interface SurveyRepository {
    val nearbySurveys: Flow<List<Survey>>
    suspend fun refreshNearby(userLocation: UserLocation? = null, radiusMeters: Int = 5000)
    suspend fun getSurveyById(id: String): Survey?
    suspend fun markSurveyCompleted(id: String, responseId: String? = null)
    
    fun getCompletedSurveys(): Flow<List<Survey>>
    fun getUncompletedSurveys(): Flow<List<Survey>>
    fun getUserSurveyHistory(): Flow<List<UserSurveyHistoryItem>>
    suspend fun saveSurveyProgress(survey: Survey)
    suspend fun refreshSurveyHistory()
    suspend fun clearCachedSurveys()
    suspend fun getResponseDetail(responseId: String): com.qzone.data.model.SurveyResponseDetail?
}
