package com.qzone.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.util.Log
import com.qzone.data.model.UserLocation
import com.qzone.domain.repository.LocationRepository
import com.qzone.util.CoordinateConverter
import com.qzone.util.QLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.coroutines.resume

class LocationRepositoryImpl(
    private val context: Context
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder? = if (Geocoder.isPresent()) {
        // Force English for address results so UI shows English location text
        Geocoder(context, Locale.ENGLISH)
    } else null

    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    override suspend fun getLastLocation(): com.qzone.data.model.LocationResult {
        if (!hasLocationPermission()) {
            QLog.w(TAG) { "getLastLocation aborted: permission denied" }
            return com.qzone.data.model.LocationResult.PermissionDenied
        }

        if (!isLocationEnabled()) {
            QLog.w(TAG) { "getLastLocation aborted: location disabled" }
            return com.qzone.data.model.LocationResult.LocationDisabled
        }

        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                // Debug: log raw location values returned by FusedLocationProvider
                QLog.d(TAG) { "getLastLocation RAW lat=${location.latitude} lng=${location.longitude} time=${location.time} acc=${location.accuracy}" }

                // Validate cached lastLocation: prefer current high-accuracy reading if lastLocation is old or coarse
                val ageMs = System.currentTimeMillis() - location.time
                val accuracyMeters = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE

                val maxAgeMs = 60_000L // 1 minute
                val maxAcceptableAccuracy = 50f // meters

                if (ageMs <= maxAgeMs && accuracyMeters <= maxAcceptableAccuracy) {
                    // Convert coordinates if in China (WGS-84 -> GCJ-02)
                    val (convertedLat, convertedLng) = if (CoordinateConverter.isInChina(location.latitude, location.longitude)) {
                        val converted = CoordinateConverter.wgs84ToGcj02(location.latitude, location.longitude)
                        QLog.d(TAG) { "getLastLocation converted GCJ-02 lat=${converted.first} lng=${converted.second}" }
                        converted
                    } else {
                        Pair(location.latitude, location.longitude)
                    }
                    
                    val address = getAddressFromLocation(convertedLat, convertedLng)
                    com.qzone.data.model.LocationResult.Success(
                        UserLocation(
                            latitude = convertedLat,
                            longitude = convertedLng,
                            address = address,
                            timestamp = location.time
                        )
                    )
                } else {
                    // Cached value is too old or inaccurate; request an up-to-date high-accuracy location
                    QLog.d(TAG) { "getLastLocation stale cache age=${ageMs}ms acc=${accuracyMeters}m -> requesting fresh reading" }
                    getCurrentLocation()
                }
            } else {
                // If last location is null, try to get current location
                getCurrentLocation()
            }
        } catch (e: SecurityException) {
            com.qzone.data.model.LocationResult.PermissionDenied
        } catch (e: Exception) {
            com.qzone.data.model.LocationResult.Error(
                e.message ?: "Failed to get location"
            )
        }
    }

    override suspend fun getCurrentLocation(): com.qzone.data.model.LocationResult {
        if (!hasLocationPermission()) {
            QLog.w(TAG) { "getCurrentLocation aborted: permission denied" }
            return com.qzone.data.model.LocationResult.PermissionDenied
        }

        if (!isLocationEnabled()) {
            QLog.w(TAG) { "getCurrentLocation aborted: location disabled" }
            return com.qzone.data.model.LocationResult.LocationDisabled
        }

        return try {
            // Prefer the Task-based getCurrentLocation which usually returns a high-accuracy instant location
            try {
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                if (location != null) {
                    // Log raw GPS coordinates (WGS-84)
                    QLog.d(TAG) { "getCurrentLocation RAW lat=${location.latitude} lng=${location.longitude} acc=${location.accuracy}" }
                    
                    // Convert coordinates if in China (WGS-84 -> GCJ-02)
                    val (convertedLat, convertedLng) = if (CoordinateConverter.isInChina(location.latitude, location.longitude)) {
                        val converted = CoordinateConverter.wgs84ToGcj02(location.latitude, location.longitude)
                        QLog.d(TAG) { "getCurrentLocation converted GCJ-02 lat=${converted.first} lng=${converted.second}" }
                        converted
                    } else {
                        QLog.d(TAG) { "getCurrentLocation outside China -> using WGS-84" }
                        Pair(location.latitude, location.longitude)
                    }
                    
                    val address = getAddressFromLocationSync(convertedLat, convertedLng)
                    return com.qzone.data.model.LocationResult.Success(
                        UserLocation(
                            latitude = convertedLat,
                            longitude = convertedLng,
                            address = address,
                            timestamp = location.time
                        )
                    )
                }
            } catch (ignored: Exception) {
                // Fall through to requestLocationUpdates approach if getCurrentLocation fails for any reason
                QLog.d(TAG) { "getCurrentLocation direct call failed -> ${ignored.message}" }
            }

            // Fallback: request location updates and wait for the next available result
            suspendCancellableCoroutine { continuation ->
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    10_000L // 10 seconds
                ).build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        super.onLocationResult(result)
                        result.lastLocation?.let { location ->
                            // Log raw GPS coordinates (WGS-84)
                            QLog.d(TAG) { "getCurrentLocation callback RAW lat=${location.latitude} lng=${location.longitude} acc=${location.accuracy}" }
                            
                            // Convert coordinates if in China (WGS-84 -> GCJ-02)
                            val (convertedLat, convertedLng) = if (CoordinateConverter.isInChina(location.latitude, location.longitude)) {
                                val converted = CoordinateConverter.wgs84ToGcj02(location.latitude, location.longitude)
                                QLog.d(TAG) { "getCurrentLocation callback converted GCJ-02 lat=${converted.first} lng=${converted.second}" }
                                converted
                            } else {
                                Pair(location.latitude, location.longitude)
                            }
                            
                            val address = getAddressFromLocationSync(convertedLat, convertedLng)
                            continuation.resume(
                                com.qzone.data.model.LocationResult.Success(
                                    UserLocation(
                                        latitude = convertedLat,
                                        longitude = convertedLng,
                                        address = address,
                                        timestamp = location.time
                                    )
                                )
                            )
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }
                }

                try {
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        null
                    )
                } catch (e: SecurityException) {
                    continuation.resume(com.qzone.data.model.LocationResult.PermissionDenied)
                }

                continuation.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }
        } catch (e: SecurityException) {
            com.qzone.data.model.LocationResult.PermissionDenied
        } catch (e: Exception) {
            com.qzone.data.model.LocationResult.Error(
                e.message ?: "Failed to get current location"
            )
        }
    }

    override val locationUpdates: Flow<UserLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            QLog.w(TAG) { "locationUpdates aborted: permission denied" }
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000L // 30 seconds
        ).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.lastLocation?.let { location ->
                    val address = getAddressFromLocationSync(
                        location.latitude,
                        location.longitude
                    )
                    trySend(
                        UserLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            address = address,
                            timestamp = location.time
                        )
                    )
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        return getAddressFromLocation(latitude, longitude)
    }

    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            if (geocoder == null) return null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()?.let { addr ->
                            buildString {
                                addr.locality?.let { append("$it, ") }
                                addr.adminArea?.let { append(it) }
                            }.ifEmpty { null }
                        }
                        continuation.resume(address)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { addr ->
                    buildString {
                        addr.locality?.let { append("$it, ") }
                        addr.adminArea?.let { append(it) }
                    }.ifEmpty { null }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getAddressFromLocationSync(latitude: Double, longitude: Double): String? {
        return try {
            if (geocoder == null) return null

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { addr ->
                buildString {
                    addr.locality?.let { append("$it, ") }
                    addr.adminArea?.let { append(it) }
                }.ifEmpty { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "LocationRepositoryImpl"
    }
}
