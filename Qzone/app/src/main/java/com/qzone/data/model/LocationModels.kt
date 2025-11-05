package com.qzone.data.model

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Parcelize
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    @SuppressLint("DefaultLocale")
    fun toDisplayString(): String {
        return address ?: String.format("%.4f, %.4f", latitude, longitude)
    }
    
    /**
     * Calculate distance to another location in meters using Haversine formula
     */
    fun distanceTo(otherLat: Double, otherLng: Double): Int {
        val earthRadiusKm = 6371.0
        
        val dLat = Math.toRadians(otherLat - latitude)
        val dLon = Math.toRadians(otherLng - longitude)
        
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(otherLat)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return (earthRadiusKm * c * 1000).toInt() // Convert to meters
    }
}

sealed class LocationResult {
    data class Success(val location: UserLocation) : LocationResult()
    data class Error(val message: String) : LocationResult()
    data object PermissionDenied : LocationResult()
    data object LocationDisabled : LocationResult()
}
