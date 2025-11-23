package com.qzone.data.repository

import android.util.Log
import com.qzone.data.model.Survey
import com.qzone.data.model.UserLocation
import com.qzone.data.network.QzoneApiClient
import com.qzone.data.network.model.NetworkSurveyOption
import com.qzone.data.network.model.NetworkSurveyQuestion
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

import com.qzone.data.model.SurveyStatus
import com.qzone.data.network.model.UserSurveyHistoryRequest

class ApiSurveyRepository : SurveyRepository {
    private val surveysFlow = MutableStateFlow<List<Survey>>(emptyList())
    override val nearbySurveys: Flow<List<Survey>> = surveysFlow.asStateFlow()

    override suspend fun refreshNearby(userLocation: UserLocation?, radiusMeters: Int) {
        val lat = userLocation?.latitude ?: 0.0
        val lng = userLocation?.longitude ?: 0.0
        val radiusKm = radiusMeters / 1000.0
        val maxResults = 200
        val includeDistance = true
        val sortByDistance = true

        val precisionFallbacks = listOf(9, 8, 7, 6)
        var foundData: List<com.qzone.data.model.NearbyLocation>? = null

        for (p in precisionFallbacks) {
            val body = com.qzone.data.network.model.NearbyLocationRequest(
                userLat = lat,
                userLng = lng,
                radiusKm = radiusKm,
                precision = p,
                maxResults = maxResults,
                includeDistance = includeDistance,
                sortByDistance = sortByDistance
            )
            val result = com.qzone.data.network.QzoneApiClient.service.getNearbyLocations(body)
            if (result.success && !result.data.isNullOrEmpty()) {
                foundData = result.data
                break
            }
        }

        if (foundData != null) {
            surveysFlow.value = foundData.map { loc ->
                Survey(
                    id = loc.documentId,
                    title = loc.title,
                    description = loc.description,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    points = 0,
                    questions = emptyList(),
                    questionCount = 0,
                    isCompleted = false,
                    status = SurveyStatus.EMPTY
                )
            }
        } else {
            surveysFlow.value = emptyList()
        }
    }

    override suspend fun getSurveyById(id: String): Survey? {
        val cached = surveysFlow.value.firstOrNull { it.id == id }
        Log.d(TAG, "Requesting survey detail for $id (cachedQuestions=${cached?.questions?.size ?: 0})")
        return try {
            val detailResponse = QzoneApiClient.service.getSurveyDetail(id)
            Log.d(TAG, "GET /api/survey/$id response: success=${detailResponse.success}, data=${detailResponse.data}")
            val questionResponse = QzoneApiClient.service.getSurveyQuestions(id)
            Log.d(TAG, "GET /api/survey/$id/questions response: success=${questionResponse.success}, data=${questionResponse.data}")

            val baseSurvey = if (detailResponse.success && detailResponse.data != null) {
                detailResponse.data.toSurvey(cached)
            } else {
                Log.w(TAG, "Survey detail request failed for $id code=${detailResponse.code} msg=${detailResponse.msg}")
                cached
            }

            val questions = if (questionResponse.success && !questionResponse.data.isNullOrEmpty()) {
                questionResponse.data.map { question ->
                    val options = fetchQuestionOptions(question)
                    question.toSurveyQuestion(options)
                }
            } else {
                Log.w(TAG, "Survey questions request failed for $id code=${questionResponse.code} msg=${questionResponse.msg}")
                baseSurvey?.questions ?: cached?.questions ?: emptyList()
            }

            val detailed = baseSurvey?.copy(questions = questions) ?: cached
            if (detailed != null) {
                updateCachedSurvey(detailed)
                Log.d(TAG, "Survey detail loaded for $id (questions=${detailed.questions.size})")
            }
            detailed
        } catch (t: Throwable) {
            Log.e(TAG, "Survey detail request error for $id", t)
            cached
        }
    }

    override suspend fun markSurveyCompleted(id: String) {
        // Optionally call API to mark as completed
        // For now, just update local state
        surveysFlow.value = surveysFlow.value.map {
            if (it.id == id) it.copy(isCompleted = true, status = SurveyStatus.COMPLETE) else it
        }
    }

    override fun getCompletedSurveys(): Flow<List<Survey>> {
        return surveysFlow.map { list -> list.filter { it.isCompleted } }
    }

    override fun getUncompletedSurveys(): Flow<List<Survey>> {
        return surveysFlow.map { list -> list.filter { !it.isCompleted } }
    }

    override suspend fun saveSurveyProgress(survey: Survey) {
        updateCachedSurvey(survey)
    }

    override suspend fun refreshSurveyHistory() {
        try {
            val response = QzoneApiClient.service.getUserSurveyHistory(
                UserSurveyHistoryRequest(page = 1, pageSize = 100)
            )
            if (response.success && response.data != null) {
                val historyRecords = response.data.records
                val currentSurveys = surveysFlow.value.toMutableList()

                historyRecords.forEach { record ->
                    val existingIndex = currentSurveys.indexOfFirst { it.id == record.surveyId }
                    val status = try {
                        SurveyStatus.valueOf(record.status)
                    } catch (e: Exception) {
                        if (record.isComplete) SurveyStatus.COMPLETE else SurveyStatus.PARTIAL
                    }

                    if (existingIndex >= 0) {
                        // Update existing survey status
                        val existing = currentSurveys[existingIndex]
                        currentSurveys[existingIndex] = existing.copy(
                            isCompleted = record.isComplete,
                            status = status,
                            questionCount = record.totalQuestions
                        )
                    } else {
                        // Add survey if missing (basic info from history)
                        currentSurveys.add(
                            Survey(
                                id = record.surveyId,
                                title = record.surveyTitle,
                                description = record.surveyDescription,
                                latitude = 0.0, // Missing from history
                                longitude = 0.0, // Missing from history
                                points = 0, // Missing from history
                                isCompleted = record.isComplete,
                                status = status,
                                questionCount = record.totalQuestions
                            )
                        )
                    }
                }
                surveysFlow.value = currentSurveys
            } else {
                Log.w(TAG, "Failed to fetch survey history: ${response.msg}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching survey history", e)
        }
    }

    private fun updateCachedSurvey(updated: Survey) {
        val current = surveysFlow.value
        val index = current.indexOfFirst { it.id == updated.id }
        surveysFlow.value = if (index >= 0) {
            current.map { if (it.id == updated.id) updated else it }
        } else {
            current + updated
        }
    }

    private suspend fun fetchQuestionOptions(question: NetworkSurveyQuestion): List<NetworkSurveyOption>? {
        if (question.type.equals("text", ignoreCase = true)) {
            return emptyList()
        }
        val questionId = question.resolvedId()
        return try {
            val response = QzoneApiClient.service.getQuestionOptions(questionId)
            Log.d(TAG, "GET /api/survey/question/$questionId/options response: success=${response.success}, data=${response.data}")
            if (response.success && !response.data.isNullOrEmpty()) {
                response.data
            } else {
                Log.w(TAG, "Question options request failed for $questionId code=${response.code} msg=${response.msg}")
                emptyList()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Question options request error for $questionId", t)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "ApiSurveyRepository"
    }
}
