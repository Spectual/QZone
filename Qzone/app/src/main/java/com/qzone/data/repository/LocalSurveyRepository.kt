package com.qzone.data.repository

import com.qzone.util.QLog
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

    suspend fun saveSurvey(survey: Survey) {
        QLog.d(TAG) { "DB saveSurvey id=${survey.id} questions=${survey.questions.size}" }
        try {
            val surveyEntity = SurveyEntity.fromSurvey(survey)
            surveyDao.insertSurvey(surveyEntity)

            survey.questions.forEachIndexed { index, question ->
                val questionEntity = SurveyQuestionEntity.fromSurveyQuestion(survey.id, question, index)
                questionDao.insertQuestion(questionEntity)

                question.options?.forEach { option ->
                    val optionEntity = SurveyOptionEntity.fromSurveyOption(question.id, option)
                    optionDao.insertOption(optionEntity)
                }
            }

            QLog.d(TAG) { "Survey ${survey.id} saved successfully" }
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error saving survey: ${e.message}" }
        }
    }

    suspend fun saveSurveys(surveys: List<Survey>) {
        QLog.d(TAG) { "DB saveSurveys count=${surveys.size}" }
        surveys.forEach { survey ->
            saveSurvey(survey)
        }
    }

    suspend fun getSurveyById(id: String): Survey? {
        QLog.d(TAG) { "DB getSurveyById id=$id" }
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
            QLog.e(TAG, e) { "Error getting survey by id: ${e.message}" }
            null
        }
    }

    fun getAllSurveys(): Flow<List<Survey>> {
        QLog.d(TAG) { "DB observing all surveys" }
        return surveyDao.getAllSurveys().map { surveys ->
            surveys.map { survey ->
                survey.toSurvey(questions = emptyList())
            }
        }
    }

    fun getUncompletedSurveys(): Flow<List<Survey>> {
        QLog.d(TAG) { "DB observing uncompleted surveys" }
        return surveyDao.getUncompletedSurveys().map { surveys ->
            surveys.map { survey ->
                survey.toSurvey(questions = emptyList())
            }
        }
    }

    suspend fun markSurveyCompleted(id: String) {
        try {
            surveyDao.markSurveyAsCompleted(id)
            QLog.d(TAG) { "Survey $id marked as completed" }
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error marking survey as completed: ${e.message}" }
        }
    }

    suspend fun deleteSurvey(id: String) {
        QLog.d(TAG) { "DB deleteSurvey id=$id" }
        try {
            val survey = surveyDao.getSurveyById(id)
            if (survey != null) {
                surveyDao.deleteSurvey(survey)
                QLog.d(TAG) { "Survey $id deleted" }
            }
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error deleting survey: ${e.message}" }
        }
    }

    suspend fun deleteAllSurveys() {
        QLog.d(TAG) { "DB deleteAllSurveys" }
        try {
            surveyDao.deleteAllSurveys()
            QLog.d(TAG) { "All surveys deleted" }
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error deleting all surveys: ${e.message}" }
        }
    }

    suspend fun getSurveysByLocation(userLocation: UserLocation, radiusMeters: Int = 5000): List<Survey> {
        QLog.d(TAG) { "DB getSurveysByLocation lat=${userLocation.latitude}, lng=${userLocation.longitude}, radius=$radiusMeters" }
        return try {
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
            QLog.e(TAG, e) { "Error getting surveys by location: ${e.message}" }
            emptyList()
        }
    }

    suspend fun saveNearbyLocation(location: NearbyLocation) {
        QLog.d(TAG) { "DB saveNearbyLocation id=${location.documentId}" }
        try {
            val entity = NearbyLocationEntity.fromNearbyLocation(location)
            locationDao.insertLocation(entity)
            QLog.d(TAG) { "Nearby location ${location.documentId} saved" }
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error saving nearby location: ${e.message}" }
        }
    }

    suspend fun saveNearbyLocations(locations: List<NearbyLocation>) {
        QLog.d(TAG) { "DB saveNearbyLocations count=${locations.size}" }
        locations.forEach { location ->
            saveNearbyLocation(location)
        }
    }

    suspend fun getNearbyLocationById(documentId: String): NearbyLocation? {
        QLog.d(TAG) { "DB getNearbyLocationById id=$documentId" }
        return try {
            locationDao.getLocationById(documentId)?.toNearbyLocation()
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error getting nearby location: ${e.message}" }
            null
        }
    }

    fun getAllNearbyLocations(): Flow<List<NearbyLocation>> {
        QLog.d(TAG) { "DB observing all nearby locations" }
        return locationDao.getAllLocations().map { locations ->
            locations.map { it.toNearbyLocation() }
        }
    }

    suspend fun getNearbyLocationsByBounds(userLocation: UserLocation, radiusMeters: Int = 5000): List<NearbyLocation> {
        QLog.d(TAG) { "DB getNearbyLocationsByBounds lat=${userLocation.latitude}, lng=${userLocation.longitude}, radius=$radiusMeters" }
        return try {
            val radiusDegrees = radiusMeters / 111000.0

            val minLat = userLocation.latitude - radiusDegrees
            val maxLat = userLocation.latitude + radiusDegrees
            val minLng = userLocation.longitude - radiusDegrees
            val maxLng = userLocation.longitude + radiusDegrees

            locationDao.getLocationsByBounds(minLat, maxLat, minLng, maxLng)
                .map { it.toNearbyLocation() }
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error getting nearby locations by bounds: ${e.message}" }
            emptyList()
        }
    }

    suspend fun deleteNearbyLocation(documentId: String) {
        QLog.d(TAG) { "DB deleteNearbyLocation id=$documentId" }
        try {
            val location = locationDao.getLocationById(documentId)
            if (location != null) {
                locationDao.deleteLocation(location)
                QLog.d(TAG) { "Nearby location $documentId deleted" }
            }
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error deleting nearby location: ${e.message}" }
        }
    }

    suspend fun deleteAllNearbyLocations() {
        QLog.d(TAG) { "DB deleteAllNearbyLocations" }
        try {
            locationDao.deleteAllLocations()
            QLog.d(TAG) { "All nearby locations deleted" }
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error deleting all nearby locations: ${e.message}" }
        }
    }

    suspend fun getSurveyCount(): Int {
        QLog.d(TAG) { "DB getSurveyCount" }
        return try {
            surveyDao.getSurveyCount()
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error getting survey count: ${e.message}" }
            0
        }
    }

    suspend fun getNearbyLocationCount(): Int {
        QLog.d(TAG) { "DB getNearbyLocationCount" }
        return try {
            locationDao.getLocationCount()
        } catch (e: Exception) {
            QLog.e(TAG, e) { "Error getting location count: ${e.message}" }
            0
        }
    }
}
