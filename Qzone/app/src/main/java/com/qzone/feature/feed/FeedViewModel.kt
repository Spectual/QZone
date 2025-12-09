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
import android.util.Log

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
        // Observe local cache first to render offline data
        viewModelScope.launch {
            localSurveyRepository.getAllSurveys().collect { localSurveys ->
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
                val active = surveys.filterNot { it.isCompleted }
                val completed = surveys.count { it.isCompleted }
                if (surveys.isNotEmpty()) {
                    hasLoadedFromNetwork = true
                }
                _uiState.update { it.copy(surveys = active, completedCount = completed) }
                
                // Save surveys to local database
                try {
                    // Call suspend function with launch
                    viewModelScope.launch {
                        localSurveyRepository.saveSurveys(surveys)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Prefetch question counts for surveys missing details
                surveys.filter { it.questionCount == 0 }.forEach { survey ->
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
        // Trigger initial load with location when FeedViewModel is created
        // At this point, the user should be logged in and token should be available
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
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, locationError = null) }

            if (!hasValidSession()) {
                _uiState.update { it.copy(isRefreshing = false) }
                return@launch
            }

            // Get current location
            when (val result = locationRepository.getCurrentLocation()) {
                is LocationResult.Success -> {
                    val success = result as LocationResult.Success
                    _uiState.update { it.copy(currentLocation = success.location) }
                    try {
                        surveyRepository.refreshNearby(success.location, radiusMeters = 5000)
                        refreshHistorySnapshot()
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to refresh nearby surveys", t)
                        _uiState.update { it.copy(locationError = t.message ?: "刷新附近问卷失败") }
                    }
                    loadNearbyLocationsWithCoordinates(success.location.latitude, success.location.longitude)
                }
                is LocationResult.PermissionDenied -> {
                    _uiState.update { it.copy(locationError = "Location permission denied") }
                    surveyRepository.refreshNearby(null)
                    refreshHistorySnapshot()
                }
                is LocationResult.LocationDisabled -> {
                    _uiState.update { it.copy(locationError = "Please enable location services") }
                    surveyRepository.refreshNearby(null)
                    refreshHistorySnapshot()
                }
                is LocationResult.Error -> {
                    val error = result as LocationResult.Error
                    _uiState.update { it.copy(locationError = error.message) }
                    surveyRepository.refreshNearby(null)
                    refreshHistorySnapshot()
                }
            }
            
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun loadNearbyLocations() {
        viewModelScope.launch {
            // Try to get current location and load nearby surveys
            when (val result = locationRepository.getCurrentLocation()) {
                is LocationResult.Success -> {
                    loadNearbyLocationsWithCoordinates(result.location.latitude, result.location.longitude)
                }
                else -> {
                    // If no location available, use default coordinates
                    loadNearbyLocationsWithCoordinates(42.3505, -71.1054)
                }
            }
        }
    }

    private fun loadNearbyLocationsWithCoordinates(latitude: Double, longitude: Double) {
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
                    _uiState.update { it.copy(nearbyLocations = result.data, isLoadingNearby = false) }
                    // Save nearby locations to local database
                    try {
                        localSurveyRepository.saveNearbyLocations(result.data)
                    } catch (dbError: Exception) {
                        dbError.printStackTrace()
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            nearbyError = result.msg ?: "Failed to load nearby locations",
                            isLoadingNearby = false
                        ) 
                    }
                }
            } catch (e: Exception) {
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
            _uiState.update { state ->
                state.copy(
                    locationError = "请登录后再尝试加载附近问卷",
                    nearbyError = state.nearbyError ?: "请登录后再尝试加载附近问卷"
                )
            }
        }
        return tokenPresent
    }

    private suspend fun refreshHistorySnapshot() {
        runCatching { surveyRepository.refreshSurveyHistory() }
            .onFailure { Log.w(TAG, "Failed to refresh survey history", it) }
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
