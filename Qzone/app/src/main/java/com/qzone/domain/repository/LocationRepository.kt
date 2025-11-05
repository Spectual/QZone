package com.qzone.domain.repository

import com.qzone.data.model.LocationResult
import com.qzone.data.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    /**
     * Get the last known location of the device
     */
    suspend fun getLastLocation(): LocationResult
    
    /**
     * Request a fresh location update with high accuracy
     */
    suspend fun getCurrentLocation(): LocationResult
    
    /**
     * Stream of location updates (for continuous tracking)
     */
    val locationUpdates: Flow<UserLocation>
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean
    
    /**
     * Check if location services are enabled on the device
     */
    fun isLocationEnabled(): Boolean
}
