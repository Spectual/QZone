package com.qzone.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.LocationResult
import com.qzone.data.model.Survey
import com.qzone.data.model.UserLocation
import com.qzone.domain.repository.LocationRepository
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val surveys: List<Survey> = emptyList(),
    val completedCount: Int = 0,
    val isRefreshing: Boolean = false,
    val currentLocation: UserLocation? = null,
    val locationError: String? = null,
    val hasLocationPermission: Boolean = false
)

class FeedViewModel(
    private val surveyRepository: SurveyRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        checkLocationPermission()
        viewModelScope.launch {
            surveyRepository.nearbySurveys.collect { surveys ->
                val active = surveys.filterNot { it.isCompleted }
                val completed = surveys.count { it.isCompleted }
                _uiState.update { it.copy(surveys = active, completedCount = completed) }
            }
        }
        // Try to get location and refresh surveys on init
        refreshWithLocation()
    }

    private fun checkLocationPermission() {
        val hasPermission = locationRepository.hasLocationPermission()
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }
    }

    fun refreshWithLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, locationError = null) }
            
            // Get current location
            when (val result = locationRepository.getCurrentLocation()) {
                is LocationResult.Success -> {
                    _uiState.update { it.copy(currentLocation = result.location) }
                    // Refresh surveys with location filter (5km radius)
                    surveyRepository.refreshNearby(result.location, radiusMeters = 5000)
                }
                is LocationResult.PermissionDenied -> {
                    _uiState.update { it.copy(locationError = "Location permission denied") }
                    // Refresh without location filter
                    surveyRepository.refreshNearby(null)
                }
                is LocationResult.LocationDisabled -> {
                    _uiState.update { it.copy(locationError = "Please enable location services") }
                    surveyRepository.refreshNearby(null)
                }
                is LocationResult.Error -> {
                    _uiState.update { it.copy(locationError = result.message) }
                    surveyRepository.refreshNearby(null)
                }
            }
            
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun refresh() {
        refreshWithLocation()
    }

    fun onLocationPermissionGranted() {
        checkLocationPermission()
        refreshWithLocation()
    }

    companion object {
        fun factory(
            surveyRepository: SurveyRepository,
            locationRepository: LocationRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FeedViewModel(surveyRepository, locationRepository) as T
            }
        }
    }
}
