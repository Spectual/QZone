package com.qzone.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.LocationResult
import com.qzone.data.model.NearbyLocation
import com.qzone.data.model.Survey
import com.qzone.data.model.UserLocation
import com.qzone.data.network.AuthTokenProvider
import com.qzone.data.repository.LocalSurveyRepository
import com.qzone.domain.repository.LocationRepository
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.qzone.util.QLog

data class FeedUiState(
    val surveys: List<Survey> = emptyList(),
    val completedCount: Int = 0,
    val isRefreshing: Boolean = false,
    val currentLocation: UserLocation? = null,
    val locationError: String? = null,
    val hasLocationPermission: Boolean = false,
    val nearbyLocations: List<NearbyLocation> = emptyList(),
    val isLoadingNearby: Boolean = false,
    val nearbyError: String? = null
)

class FeedViewModel(
    private val surveyRepository: SurveyRepository,
    private val locationRepository: LocationRepository,
    private val localSurveyRepository: LocalSurveyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    
    private var hasLoadedInitially = false
    private var hasLoadedFromNetwork = false
    private val prefetchingDetails = mutableSetOf<String>()

    init {
        checkLocationPermission()
        viewModelScope.launch {
            localSurveyRepository.getAllSurveys().collect { localSurveys ->
                QLog.d(TAG) { "Local survey flow emitted size=${localSurveys.size}" }
                val active = localSurveys.filterNot { it.isCompleted }
                val completed = localSurveys.count { it.isCompleted }
                _uiState.update { state ->
                    if (!hasLoadedFromNetwork || state.surveys.isEmpty()) {
                        state.copy(surveys = active, completedCount = completed)
                    } else {
                        state
                    }
                }
            }
        }
        viewModelScope.launch {
            surveyRepository.nearbySurveys.collect { surveys ->
                QLog.d(TAG) { "Remote survey flow emitted size=${surveys.size}" }
                val active = surveys.filterNot { it.isCompleted }
                val completed = surveys.count { it.isCompleted }
                if (surveys.isNotEmpty()) {
                    hasLoadedFromNetwork = true
                }
                _uiState.update { it.copy(surveys = active, completedCount = completed) }
                
                viewModelScope.launch {
                    runCatching {
                        localSurveyRepository.saveSurveys(surveys)
                    }.onFailure { e ->
                        QLog.w(TAG) { "Failed to save surveys to local database: ${e.message}" }
                    }
                }

                surveys.filter { it.questionCount == 0 || it.description.isBlank() }.forEach { survey ->
                    if (prefetchingDetails.add(survey.id)) {
                        viewModelScope.launch {
                            try {
                                surveyRepository.getSurveyById(survey.id)
                            } finally {
                                prefetchingDetails.remove(survey.id)
                            }
                        }
                    }
                }
            }
        }
        if (!hasLoadedInitially) {
            hasLoadedInitially = true
            refreshWithLocation()
        }
    }

    private fun checkLocationPermission() {
        val hasPermission = locationRepository.hasLocationPermission()
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }
    }

    
    fun refreshWithLocation() {
        QLog.d(TAG) { "refreshWithLocation() invoked" }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, locationError = null) }

            if (!hasValidSession()) {
                _uiState.update { it.copy(isRefreshing = false) }
                return@launch
            }

            when (val result = locationRepository.getCurrentLocation()) {
                is LocationResult.Success -> {
                    QLog.d(TAG) { "Location success lat=${result.location.latitude}, lng=${result.location.longitude}" }
                    _uiState.update { it.copy(currentLocation = result.location) }
                    try {
                        surveyRepository.refreshNearby(result.location, radiusMeters = 5000)
                        refreshHistorySnapshot()
                    } catch (t: Throwable) {
                        QLog.e(TAG, t) { "Failed to refresh nearby surveys" }
                        _uiState.update { it.copy(locationError = t.message ?: "Failed to refresh nearby surveys") }
                    }
                    loadNearbyLocationsWithCoordinates(result.location.latitude, result.location.longitude)
                }
                is LocationResult.PermissionDenied -> {
                    QLog.w(TAG) { "Location permission denied when refreshing feed" }
                    _uiState.update { it.copy(locationError = "Location permission denied") }
                    surveyRepository.refreshNearby(null)
                    refreshHistorySnapshot()
                }
                is LocationResult.LocationDisabled -> {
                    QLog.w(TAG) { "Location services disabled" }
                    _uiState.update { it.copy(locationError = "Please enable location services") }
                    surveyRepository.refreshNearby(null)
                    refreshHistorySnapshot()
                }
                is LocationResult.Error -> {
                    QLog.e(TAG) { "Location error: ${result.message}" }
                    _uiState.update { it.copy(locationError = result.message) }
                    surveyRepository.refreshNearby(null)
                    refreshHistorySnapshot()
                }
            }
            
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun loadNearbyLocations() {
        QLog.d(TAG) { "loadNearbyLocations() invoked" }
        viewModelScope.launch {
            when (val result = locationRepository.getCurrentLocation()) {
                is LocationResult.Success -> {
                    loadNearbyLocationsWithCoordinates(result.location.latitude, result.location.longitude)
                }
                else -> {
                    loadNearbyLocationsWithCoordinates(42.3505, -71.1054)
                }
            }
        }
    }

    private fun loadNearbyLocationsWithCoordinates(latitude: Double, longitude: Double) {
        QLog.d(TAG) { "loadNearbyLocationsWithCoordinates lat=$latitude lng=$longitude" }
        viewModelScope.launch {
            if (!hasValidSession()) {
                return@launch
            }
            _uiState.update { it.copy(isLoadingNearby = true, nearbyError = null) }
            try {
                val radiusKm = 5.0
                val result = com.qzone.data.network.QzoneApiClient.service.getNearbyLocations(
                    userLat = latitude,
                    userLng = longitude,
                    radiusKm = radiusKm
                )
                if (result.success && result.data != null) {
                    QLog.d(TAG) { "Nearby location API returned ${result.data.size} entries" }
                    _uiState.update { it.copy(nearbyLocations = result.data, isLoadingNearby = false) }
                    runCatching {
                        localSurveyRepository.saveNearbyLocations(result.data)
                    }.onFailure { e ->
                        QLog.w(TAG) { "Failed to save nearby locations to local database: ${e.message}" }
                    }
                } else {
                    QLog.w(TAG) { "Nearby location API failure: ${result.msg}" }
                    _uiState.update { 
                        it.copy(
                            nearbyError = result.msg ?: "Failed to load nearby locations",
                            isLoadingNearby = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                QLog.e(TAG, e) { "Nearby location request failed" }
                _uiState.update { 
                    it.copy(
                        nearbyError = e.message ?: "Failed to load nearby locations",
                        isLoadingNearby = false
                    ) 
                }
            }
        }
    }

    private fun hasValidSession(): Boolean {
        val tokenPresent = !AuthTokenProvider.accessToken.isNullOrBlank()
        if (!tokenPresent) {
            QLog.w(TAG) { "No auth token available; blocking network call" }
            _uiState.update { state ->
                state.copy(
                    locationError = "Please sign in to load nearby surveys",
                    nearbyError = state.nearbyError ?: "Please sign in to load nearby surveys"
                )
            }
        }
        return tokenPresent
    }

    private suspend fun refreshHistorySnapshot() {
        runCatching { surveyRepository.refreshSurveyHistory() }
            .onFailure { throwable ->
                QLog.w(TAG) { "Failed to refresh survey history: ${throwable.message}" }
            }
    }

    fun refresh() {
        refreshWithLocation()
        loadNearbyLocations()
    }

    fun onLocationPermissionGranted() {
        checkLocationPermission()
        refreshWithLocation()
    }

    companion object {
        private const val TAG = "FeedViewModel"
        fun factory(
            surveyRepository: SurveyRepository,
            locationRepository: LocationRepository,
            localSurveyRepository: LocalSurveyRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FeedViewModel(surveyRepository, locationRepository, localSurveyRepository) as T
            }
        }
    }
}
