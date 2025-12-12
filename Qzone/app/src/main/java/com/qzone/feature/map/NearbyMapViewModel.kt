package com.qzone.feature.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.LocationResult
import com.qzone.data.model.NearbyLocation
import com.qzone.data.model.UserLocation
import com.qzone.data.network.AuthTokenProvider
import com.qzone.data.network.QzoneApiClient
import com.qzone.data.repository.LocalSurveyRepository
import com.qzone.domain.repository.LocationRepository
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NearbyMapUiState(
    val isLoading: Boolean = false,
    val currentLocation: UserLocation? = null,
    val hasLocationPermission: Boolean = false,
    val nearbyLocations: List<NearbyLocation> = emptyList(),
    val errorMessage: String? = null,
    val locationError: String? = null,
    val completedCount: Int = 0,
    val activeCount: Int = 0
)

class NearbyMapViewModel(
    private val locationRepository: LocationRepository,
    private val localSurveyRepository: LocalSurveyRepository,
    private val surveyRepository: SurveyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyMapUiState())
    val uiState: StateFlow<NearbyMapUiState> = _uiState.asStateFlow()
    private val surveyCompletionState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private var cachedLocations: List<NearbyLocation> = emptyList()

    init {
        updatePermissionState()
        observeCachedLocations()
        observeSurveyCompletion()
        refresh()
    }

    private fun observeCachedLocations() {
        viewModelScope.launch {
            localSurveyRepository.getAllNearbyLocations().collectLatest { cached ->
                updateNearbyLocations(cached)
            }
        }
    }

    private fun observeSurveyCompletion() {
        viewModelScope.launch {
            surveyRepository.nearbySurveys.collectLatest { surveys ->
                val completionMap = surveys.associate { it.id to it.isCompleted }
                surveyCompletionState.value = completionMap
                refreshVisibleLocations()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadLocationAndNearby()
        }
    }

    fun onLocationPermissionGranted() {
        updatePermissionState()
        refresh()
    }

    private suspend fun loadLocationAndNearby() {
        if (!hasValidSession()) {
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, locationError = null) }
        when (val result = locationRepository.getCurrentLocation()) {
            is LocationResult.Success -> {
                fetchWithLocation(result.location)
            }
            LocationResult.PermissionDenied -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasLocationPermission = false,
                        locationError = "Location permission denied"
                    )
                }
            }
            LocationResult.LocationDisabled -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        locationError = "Please enable location services"
                    )
                }
            }
            is LocationResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        locationError = result.message
                    )
                }
            }
        }
    }

    private suspend fun fetchWithLocation(location: UserLocation) {
        _uiState.update {
            it.copy(
                currentLocation = location,
                hasLocationPermission = true,
                locationError = null
            )
        }
        try {
            val radiusKm = 5.0
            val response = QzoneApiClient.service.getNearbyLocations(
                userLat = location.latitude,
                userLng = location.longitude,
                radiusKm = radiusKm
            )
            if (response.success && response.data != null) {
                try {
                    localSurveyRepository.deleteAllNearbyLocations()
                    localSurveyRepository.saveNearbyLocations(response.data)
                } catch (dbError: Exception) {
                    Log.w(TAG, "Failed to cache nearby locations", dbError)
                }
                updateNearbyLocations(response.data)
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.msg ?: "Failed to load nearby surveys"
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load nearby surveys", t)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "Failed to load nearby surveys"
                )
            }
        }
    }

    private fun updatePermissionState() {
        val hasPermission = locationRepository.hasLocationPermission()
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }
    }

    private fun hasValidSession(): Boolean {
        val tokenPresent = !AuthTokenProvider.accessToken.isNullOrBlank()
        if (!tokenPresent) {
            _uiState.update {
                it.copy(errorMessage = "Please sign in to load nearby surveys", isLoading = false)
            }
        }
        return tokenPresent
    }

    private fun updateNearbyLocations(locations: List<NearbyLocation>) {
        cachedLocations = locations
        refreshVisibleLocations()
    }

    private fun refreshVisibleLocations() {
        val locations = cachedLocations
        val completionMap = surveyCompletionState.value
        val (completed, active) = locations.partition { completionMap[it.documentId] == true }
        _uiState.update {
            it.copy(
                nearbyLocations = active,
                completedCount = completed.size,
                activeCount = active.size
            )
        }
    }

    companion object {
        private const val TAG = "NearbyMapViewModel"

        fun factory(
            locationRepository: LocationRepository,
            localSurveyRepository: LocalSurveyRepository,
            surveyRepository: SurveyRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NearbyMapViewModel(locationRepository, localSurveyRepository, surveyRepository) as T
            }
        }
    }
}
