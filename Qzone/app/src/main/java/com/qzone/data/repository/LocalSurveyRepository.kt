package com.qzone.data.repository

import android.util.Log
import com.qzone.data.database.QzoneDatabase
import com.qzone.data.database.entity.NearbyLocationEntity
import com.qzone.data.database.entity.SurveyEntity
import com.qzone.data.database.entity.SurveyOptionEntity
import com.qzone.data.database.entity.SurveyQuestionEntity
import com.qzone.data.model.NearbyLocation
import com.qzone.data.model.Survey
import com.qzone.data.model.UserLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalSurveyRepository(private val database: QzoneDatabase) {
    
    private val surveyDao = database.surveyDao()
    private val questionDao = database.surveyQuestionDao()
    private val optionDao = database.surveyOptionDao()
    private val locationDao = database.nearbyLocationDao()

    companion object {
        private const val TAG = "LocalSurveyRepository"
    }

    // ===== Survey Operations =====

    suspend fun saveSurvey(survey: Survey) {
        try {
            val surveyEntity = SurveyEntity.fromSurvey(survey)
            surveyDao.insertSurvey(surveyEntity)

            // Save questions and options
            survey.questions.forEachIndexed { index, question ->
                val questionEntity = SurveyQuestionEntity.fromSurveyQuestion(survey.id, question, index)
                questionDao.insertQuestion(questionEntity)

                // Save options
                question.options?.forEach { option ->
                    val optionEntity = SurveyOptionEntity.fromSurveyOption(question.id, option)
                    optionDao.insertOption(optionEntity)
                }
            }

            Log.d(TAG, "Survey ${survey.id} saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving survey: ${e.message}", e)
        }
    }

    suspend fun saveSurveys(surveys: List<Survey>) {
        surveys.forEach { survey ->
            saveSurvey(survey)
        }
    }

    suspend fun getSurveyById(id: String): Survey? {
        return try {
            val survey = surveyDao.getSurveyById(id)
            if (survey != null) {
                val questions = questionDao.getQuestionsBySurveyId(id)
                val surveyQuestions = questions.mapIndexed { _, questionEntity ->
                    val options = optionDao.getOptionsByQuestionId(questionEntity.id)
                    questionEntity.toSurveyQuestion(
                        options = options.map { it.toSurveyOption() }
                    )
                }
                survey.toSurvey(questions = surveyQuestions)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting survey by id: ${e.message}", e)
            null
        }
    }

    fun getAllSurveys(): Flow<List<Survey>> {
        return surveyDao.getAllSurveys().map { surveys ->
            surveys.map { survey ->
                // For getAllSurveys, we return surveys without their detailed questions
                // This is for performance reasons - use getSurveyById for full details
                survey.toSurvey(questions = emptyList())
            }
        }
    }

    fun getUncompletedSurveys(): Flow<List<Survey>> {
        return surveyDao.getUncompletedSurveys().map { surveys ->
            surveys.map { survey ->
                // For getUncompletedSurveys, we return surveys without their detailed questions
                // This is for performance reasons - use getSurveyById for full details
                survey.toSurvey(questions = emptyList())
            }
        }
    }

    suspend fun markSurveyCompleted(id: String) {
        try {
            surveyDao.markSurveyAsCompleted(id)
            Log.d(TAG, "Survey $id marked as completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking survey as completed: ${e.message}", e)
        }
    }

    suspend fun deleteSurvey(id: String) {
        try {
            val survey = surveyDao.getSurveyById(id)
            if (survey != null) {
                surveyDao.deleteSurvey(survey)
                Log.d(TAG, "Survey $id deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting survey: ${e.message}", e)
        }
    }

    suspend fun deleteAllSurveys() {
        try {
            surveyDao.deleteAllSurveys()
            Log.d(TAG, "All surveys deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all surveys: ${e.message}", e)
        }
    }

    suspend fun getSurveysByLocation(userLocation: UserLocation, radiusMeters: Int = 5000): List<Survey> {
        return try {
            // Convert radius from meters to degrees (rough approximation)
            val radiusDegrees = radiusMeters / 111000.0

            val minLat = userLocation.latitude - radiusDegrees
            val maxLat = userLocation.latitude + radiusDegrees
            val minLng = userLocation.longitude - radiusDegrees
            val maxLng = userLocation.longitude + radiusDegrees

            val surveys = surveyDao.getSurveysByLocation(minLat, maxLat, minLng, maxLng)
            surveys.mapNotNull { surveyEntity ->
                val questions = questionDao.getQuestionsBySurveyId(surveyEntity.id)
                    .map { questionEntity ->
                        val options = optionDao.getOptionsByQuestionId(questionEntity.id)
                            .map { it.toSurveyOption() }
                        questionEntity.toSurveyQuestion(options)
                    }
                surveyEntity.toSurvey(questions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting surveys by location: ${e.message}", e)
            emptyList()
        }
    }

    // ===== NearbyLocation Operations =====

    suspend fun saveNearbyLocation(location: NearbyLocation) {
        try {
            val entity = NearbyLocationEntity.fromNearbyLocation(location)
            locationDao.insertLocation(entity)
            Log.d(TAG, "Nearby location ${location.documentId} saved")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving nearby location: ${e.message}", e)
        }
    }

    suspend fun saveNearbyLocations(locations: List<NearbyLocation>) {
        locations.forEach { location ->
            saveNearbyLocation(location)
        }
    }

    suspend fun getNearbyLocationById(documentId: String): NearbyLocation? {
        return try {
            locationDao.getLocationById(documentId)?.toNearbyLocation()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting nearby location: ${e.message}", e)
            null
        }
    }

    fun getAllNearbyLocations(): Flow<List<NearbyLocation>> {
        return locationDao.getAllLocations().map { locations ->
            locations.map { it.toNearbyLocation() }
        }
    }

    suspend fun getNearbyLocationsByBounds(userLocation: UserLocation, radiusMeters: Int = 5000): List<NearbyLocation> {
        return try {
            val radiusDegrees = radiusMeters / 111000.0

            val minLat = userLocation.latitude - radiusDegrees
            val maxLat = userLocation.latitude + radiusDegrees
            val minLng = userLocation.longitude - radiusDegrees
            val maxLng = userLocation.longitude + radiusDegrees

            locationDao.getLocationsByBounds(minLat, maxLat, minLng, maxLng)
                .map { it.toNearbyLocation() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting nearby locations by bounds: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun deleteNearbyLocation(documentId: String) {
        try {
            val location = locationDao.getLocationById(documentId)
            if (location != null) {
                locationDao.deleteLocation(location)
                Log.d(TAG, "Nearby location $documentId deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting nearby location: ${e.message}", e)
        }
    }

    suspend fun deleteAllNearbyLocations() {
        try {
            locationDao.deleteAllLocations()
            Log.d(TAG, "All nearby locations deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all nearby locations: ${e.message}", e)
        }
    }

    // ===== Database Info =====

    suspend fun getSurveyCount(): Int {
        return try {
            surveyDao.getSurveyCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting survey count: ${e.message}", e)
            0
        }
    }

    suspend fun getNearbyLocationCount(): Int {
        return try {
            locationDao.getLocationCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location count: ${e.message}", e)
            0
        }
    }
}
