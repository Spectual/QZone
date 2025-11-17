package com.qzone.data.repository

import com.qzone.data.model.Survey
import com.qzone.data.model.UserLocation
import com.qzone.data.network.QzoneApiClient
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
                    isCompleted = false
                )
            }
        } else {
            surveysFlow.value = emptyList()
        }
    }

    override suspend fun getSurveyById(id: String): Survey? {
        return surveysFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun markSurveyCompleted(id: String) {
        // Optionally call API to mark as completed
        // For now, just update local state
        surveysFlow.value = surveysFlow.value.map {
            if (it.id == id) it.copy(isCompleted = true) else it
        }
    }
}
