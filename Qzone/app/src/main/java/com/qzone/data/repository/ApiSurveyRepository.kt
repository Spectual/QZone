package com.qzone.data.repository

import android.util.Log
import com.qzone.data.model.Survey
import com.qzone.data.model.UserLocation
import com.qzone.data.network.QzoneApiClient
import com.qzone.data.network.model.NetworkSurveyOption
import com.qzone.data.network.model.NetworkSurveyQuestion
import com.qzone.data.model.SurveyResponseDetail
import com.qzone.data.model.QuestionAnswerResponse
import com.qzone.data.model.UserSurveyHistoryItem
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

import com.qzone.data.model.SurveyStatus
import com.qzone.data.network.model.UserSurveyHistoryRequest
import com.qzone.data.network.model.UserSurveyRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import retrofit2.HttpException

class ApiSurveyRepository : SurveyRepository {
    private val surveysFlow = MutableStateFlow<List<Survey>>(emptyList())
    override val nearbySurveys: Flow<List<Survey>> = surveysFlow.asStateFlow()
    private val userHistoryFlow = MutableStateFlow<List<UserSurveyHistoryItem>>(emptyList())
    private val surveyStatusCache = mutableMapOf<String, SurveyProgressSnapshot>()
    private var lastHistorySyncMs = 0L

    override suspend fun refreshNearby(userLocation: UserLocation?, radiusMeters: Int) {
        val lat = userLocation?.latitude ?: 0.0
        val lng = userLocation?.longitude ?: 0.0
        val radiusKm = radiusMeters / 1000.0
        val result = try {
            com.qzone.data.network.QzoneApiClient.service.getNearbyLocations(
                userLat = lat,
                userLng = lng,
                radiusKm = radiusKm.coerceAtMost(50.0)
            )
        } catch (t: Throwable) {
            if (t is HttpException && t.code() == 401) {
                Log.w(TAG, "Unauthorized while fetching nearby surveys; aborting refresh")
            } else {
                Log.e(TAG, "Failed to fetch nearby surveys", t)
            }
            null
        }

        val foundData = result?.takeIf { it.success }?.data

        if (foundData != null) {
            ensureHistorySynced()
            val existingList = surveysFlow.value
            val currentMap = existingList.associateBy { it.id }.toMutableMap()
            val orderedIds = mutableListOf<String>()
            foundData.forEach { loc ->
                val existing = currentMap[loc.documentId]
                val snapshot = surveyStatusCache[loc.documentId] ?: existing?.toProgressSnapshot()
                val merged = Survey(
                    id = loc.documentId,
                    title = loc.title,
                    description = loc.description.orEmpty(),
                    imageUrl = loc.imageUrl ?: existing?.imageUrl,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    points = loc.points ?: existing?.points ?: 0,
                    questions = existing?.questions ?: emptyList(),
                    questionCount = snapshot?.questionCount ?: existing?.questionCount ?: 0,
                    isCompleted = snapshot?.isCompleted ?: false,
                    status = snapshot?.status ?: SurveyStatus.EMPTY,
                    responseId = existing?.responseId
                )
                currentMap[loc.documentId] = merged
                orderedIds.add(loc.documentId)
            }
            val orderedList = orderedIds.mapNotNull { currentMap[it] }
            val remaining = existingList.filter { it.id !in orderedIds }.mapNotNull { currentMap[it.id] }
            surveysFlow.value = orderedList + remaining
        } else {
            Log.w(TAG, "Unable to load nearby surveys, keeping existing cache")
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
                detailResponse.data.toSurvey(existingSurvey = cached)
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

    override suspend fun markSurveyCompleted(id: String, responseId: String?) {
        rememberProgress(id, true, SurveyStatus.COMPLETE, questionCount = surveysFlow.value.firstOrNull { it.id == id }?.questionCount ?: 0)
        var completedSurvey: Survey? = null
        surveysFlow.value = surveysFlow.value.map { survey ->
            if (survey.id == id) {
                val updated = survey.copy(
                    isCompleted = true,
                    status = SurveyStatus.COMPLETE,
                    responseId = responseId ?: survey.responseId
                )
                completedSurvey = updated
                updated
            } else survey
        }
        completedSurvey?.let { updateHistoryEntry(it, markComplete = true) }
    }

    override fun getCompletedSurveys(): Flow<List<Survey>> {
        return surveysFlow.map { list -> list.filter { it.isCompleted } }
    }

    override fun getUncompletedSurveys(): Flow<List<Survey>> {
        return surveysFlow.map { list -> list.filter { !it.isCompleted } }
    }

    override fun getUserSurveyHistory(): Flow<List<UserSurveyHistoryItem>> = userHistoryFlow.asStateFlow()

    override suspend fun saveSurveyProgress(survey: Survey) {
        updateCachedSurvey(survey)
        updateHistoryEntry(survey, markComplete = false)
    }

    override suspend fun refreshSurveyHistory() {
        if (syncSurveyHistory()) {
            lastHistorySyncMs = System.currentTimeMillis()
        }
    }

    override suspend fun clearCachedSurveys() {
        surveyStatusCache.clear()
        lastHistorySyncMs = 0L
        surveysFlow.value = emptyList()
    }

    override suspend fun getResponseDetail(responseId: String): SurveyResponseDetail? {
        return try {
            val response = QzoneApiClient.service.getResponseDetail(responseId)
            Log.d(TAG, "GET /api/response/detail/$responseId response: success=${response.success}, data=${response.data}")
            if (response.success && response.data != null) {
                response.data.toDomain()
            } else {
                Log.w(TAG, "Response detail fetch failed for $responseId: ${response.msg}")
                null
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Response detail request error for $responseId", t)
            null
        }
    }

    private fun updateCachedSurvey(updated: Survey) {
        val current = surveysFlow.value
        val index = current.indexOfFirst { it.id == updated.id }
        rememberProgress(updated.id, updated.isCompleted, updated.status, updated.questionCount)
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

    private fun rememberProgress(
        id: String,
        isCompleted: Boolean,
        status: SurveyStatus,
        questionCount: Int
    ) {
        surveyStatusCache[id] = SurveyProgressSnapshot(isCompleted, status, questionCount)
    }

    private suspend fun ensureHistorySynced() {
        val now = System.currentTimeMillis()
        val needsSync = surveyStatusCache.isEmpty() || now - lastHistorySyncMs > HISTORY_SYNC_WINDOW_MS
        if (needsSync && syncSurveyHistory()) {
            lastHistorySyncMs = now
        }
    }

private suspend fun syncSurveyHistory(): Boolean {
        return try {
            val response = QzoneApiClient.service.getUserSurveyHistory(
                UserSurveyHistoryRequest(page = 1, pageSize = 100)
            )
            if (response.success && response.data != null) {
                val historyRecords = response.data.records
                userHistoryFlow.value = historyRecords.map { it.toHistoryItem() }
                val currentSurveys = surveysFlow.value.toMutableList()

                historyRecords.forEach { record ->
                    val existingIndex = currentSurveys.indexOfFirst { it.id == record.surveyId }
                    val status = mapStatus(record)

                    if (existingIndex >= 0) {
                        val existing = currentSurveys[existingIndex]
                        currentSurveys[existingIndex] = existing.copy(
                            isCompleted = record.isComplete,
                            status = status,
                            questionCount = record.totalQuestions,
                            responseId = record.responseId ?: existing.responseId
                        )
                        rememberProgress(record.surveyId, record.isComplete, status, record.totalQuestions)
                    } else {
                        rememberProgress(record.surveyId, record.isComplete, status, record.totalQuestions)
                        currentSurveys.add(
                            Survey(
                                id = record.surveyId,
                                title = record.surveyTitle,
                                description = record.surveyDescription.orEmpty(),
                                imageUrl = record.surveyImageUrl,
                                latitude = 0.0,
                                longitude = 0.0,
                                points = 0,
                                isCompleted = record.isComplete,
                                status = status,
                                questionCount = record.totalQuestions,
                                responseId = record.responseId
                            )
                        )
                    }
                }
                surveysFlow.value = currentSurveys
                true
            } else {
                Log.w(TAG, "Failed to fetch survey history: ${response.msg}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching survey history", e)
            false
        }
    }

    private fun updateHistoryEntry(survey: Survey, markComplete: Boolean) {
        val totalQuestions = if (survey.questionCount > 0) survey.questionCount else survey.questions.size
        val answeredQuestions = if (markComplete) {
            totalQuestions
        } else {
            survey.answers.values.count { answers -> answers.isNotEmpty() }
        }
        val completionRate = if (totalQuestions > 0) {
            (answeredQuestions.toDouble() / totalQuestions.toDouble()) * 100.0
        } else {
            0.0
        }
        val derivedStatus = when {
            markComplete -> SurveyStatus.COMPLETE
            survey.status == SurveyStatus.PARTIAL || survey.status == SurveyStatus.IN_PROGRESS -> survey.status
            answeredQuestions > 0 -> SurveyStatus.IN_PROGRESS
            else -> SurveyStatus.EMPTY
        }

        val newItem = UserSurveyHistoryItem(
            responseId = survey.responseId.orEmpty(),
            surveyId = survey.id,
            surveyTitle = survey.title,
            surveyDescription = survey.description,
            surveyImageUrl = survey.imageUrl,
            answeredQuestions = answeredQuestions,
            totalQuestions = totalQuestions,
            completionRate = completionRate,
            responseTime = formatTimestamp(),
            status = derivedStatus,
            isComplete = markComplete
        )
        val current = userHistoryFlow.value
        val updated = if (current.isEmpty()) {
            listOf(newItem)
        } else {
            val existingIndex = current.indexOfFirst { it.surveyId == survey.id }
            if (existingIndex >= 0) {
                current.mapIndexed { index, item ->
                    if (index == existingIndex) newItem else item
                }
            } else {
                listOf(newItem) + current
            }
        }
        userHistoryFlow.value = updated
    }

    private fun formatTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    companion object {
        private const val TAG = "ApiSurveyRepository"
        private val HISTORY_SYNC_WINDOW_MS = TimeUnit.MINUTES.toMillis(5)
    }
}

private fun UserSurveyRecord.toHistoryItem(): UserSurveyHistoryItem {
    val surveyStatus = mapStatus(this)
    return UserSurveyHistoryItem(
        responseId = responseId.orEmpty(),
        surveyId = surveyId,
        surveyTitle = surveyTitle,
        surveyDescription = surveyDescription.orEmpty(),
        surveyImageUrl = surveyImageUrl,
        answeredQuestions = answeredQuestions,
        totalQuestions = totalQuestions,
        completionRate = completionRate,
        responseTime = responseTime,
        status = surveyStatus,
        isComplete = isComplete
    )
}

private fun mapStatus(record: UserSurveyRecord): SurveyStatus {
    val normalized = record.status
        ?.replace("-", "", ignoreCase = true)
        ?.replace("_", "", ignoreCase = true)
        ?.uppercase()
        .orEmpty()
    return when {
        normalized == "INPROGRESS" || normalized == "INGROGRESS" -> SurveyStatus.IN_PROGRESS
        normalized == "COMPLETE" || normalized == "COMPLETED" -> SurveyStatus.COMPLETE
        normalized == "PARTIAL" -> SurveyStatus.PARTIAL
        normalized == "EMPTY" -> SurveyStatus.EMPTY
        record.isComplete -> SurveyStatus.COMPLETE
        record.answeredQuestions > 0 -> SurveyStatus.IN_PROGRESS
        else -> SurveyStatus.EMPTY
    }
}

private data class SurveyProgressSnapshot(
    val isCompleted: Boolean,
    val status: SurveyStatus,
    val questionCount: Int
)

private fun Survey.toProgressSnapshot(): SurveyProgressSnapshot =
    SurveyProgressSnapshot(isCompleted = isCompleted, status = status, questionCount = questionCount)

private fun com.qzone.data.network.model.NetworkResponseDetail.toDomain(): SurveyResponseDetail {
    return SurveyResponseDetail(
        responseId = responseId.orEmpty(),
        surveyId = surveyId.orEmpty(),
        status = status.orEmpty(),
        answeredQuestions = answeredQuestions ?: 0,
        totalQuestions = totalQuestions ?: 0,
        completionRate = completionRate ?: 0.0,
        responseTime = responseTime,
        questionAnswers = questionAnswers.orEmpty().map { it.toDomain() }
    )
}

private fun com.qzone.data.network.model.NetworkQuestionAnswer.toDomain(): QuestionAnswerResponse {
    val resolvedType = (type ?: questionType)?.ifBlank { null } ?: "unknown"
    val resolvedContent = (content ?: questionContent)?.ifBlank { null } ?: "Question"
    return QuestionAnswerResponse(
        questionId = documentId,
        questionContent = resolvedContent,
        type = resolvedType,
        selectedOptions = selectedOptions.orEmpty(),
        textAnswer = textContent,
        options = emptyList()
    )
}
