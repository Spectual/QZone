package com.qzone.data.repository

import android.util.Log
import com.qzone.data.model.Survey
import com.qzone.data.model.UserLocation
import com.qzone.data.placeholder.MockSurveyPayload
import com.qzone.data.placeholder.PlaceholderDataSource
import com.qzone.domain.repository.SurveyRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

import com.qzone.data.model.SurveyStatus

class PlaceholderSurveyRepository : SurveyRepository {

    private val completedIds = mutableSetOf<String>()
    private val surveysFlow = MutableStateFlow(applyCompletion(PlaceholderDataSource.sampleSurveys()))

    init {
        logMockSurveyJson()
    }

    override val nearbySurveys: Flow<List<Survey>> = surveysFlow.asStateFlow()

    override suspend fun refreshNearby(userLocation: UserLocation?, radiusMeters: Int) {
        delay(400)

        val allSurveys = PlaceholderDataSource.sampleSurveys()
        surveysFlow.value = applyCompletion(allSurveys)
    }

    override suspend fun getSurveyById(id: String): Survey? {
        return surveysFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun markSurveyCompleted(id: String) {
        completedIds.add(id)
        surveysFlow.value = surveysFlow.value.map { survey ->
            if (survey.id == id) survey.copy(isCompleted = true, status = SurveyStatus.COMPLETE) else survey
        }
    }

    override fun getCompletedSurveys(): Flow<List<Survey>> {
        return surveysFlow.map { list -> list.filter { it.isCompleted } }
    }

    override fun getUncompletedSurveys(): Flow<List<Survey>> {
        return surveysFlow.map { list -> list.filter { !it.isCompleted } }
    }

    override suspend fun saveSurveyProgress(survey: Survey) {
        surveysFlow.value = surveysFlow.value.map {
            if (it.id == survey.id) {
                // If answers are present but not complete, mark as PARTIAL
                val newStatus = if (survey.answers.isNotEmpty() && !survey.isCompleted) {
                    SurveyStatus.PARTIAL
                } else {
                    survey.status
                }
                survey.copy(status = newStatus)
            } else it
        }
    }

    override suspend fun refreshSurveyHistory() {
        // No-op for placeholder
        delay(200)
    }

    override suspend fun clearCachedSurveys() {
        completedIds.clear()
        surveysFlow.value = applyCompletion(PlaceholderDataSource.sampleSurveys())
    }

    private fun applyCompletion(source: List<Survey>): List<Survey> {
        if (completedIds.isEmpty()) return source
        return source.map { survey ->
            if (completedIds.contains(survey.id)) {
                survey.copy(isCompleted = true, status = SurveyStatus.COMPLETE)
            } else survey
        }
    }

    private fun logMockSurveyJson() {
        val payload: MockSurveyPayload = PlaceholderDataSource.mockSurveyPayload()
        val adapter = moshi.adapter(MockSurveyPayload::class.java)
        Log.d(TAG, "Mock survey payload: ${adapter.toJson(payload)}")
    }

    companion object {
        private const val TAG = "PlaceholderSurveyRepo"
        private val moshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
}
