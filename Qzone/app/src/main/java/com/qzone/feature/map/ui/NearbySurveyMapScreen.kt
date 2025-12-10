package com.qzone.feature.map.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.qzone.R
import com.qzone.feature.map.NearbyMapUiState

@Composable
fun NearbySurveyMapScreen(
    state: NearbyMapUiState,
    onRefresh: () -> Unit,
    onLocationPermissionGranted: () -> Unit,
    onSurveySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultLocation = LatLng(37.4221, -122.0841)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 5f)
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                onLocationPermissionGranted()
            }
        }
    )
    val enableMyLocation = state.hasLocationPermission && state.currentLocation != null
    val mapProperties = MapProperties(isMyLocationEnabled = enableMyLocation)
    val uiSettings = MapUiSettings(zoomControlsEnabled = false)

    LaunchedEffect(state.currentLocation) {
        state.currentLocation?.let { location ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    14f
                )
            )
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.map_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(
                    id = R.string.map_status_summary,
                    state.completedCount,
                    state.activeCount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.map_location_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.currentLocation?.toDisplayString()
                            ?: stringResource(id = R.string.map_fetching_location),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.isLoading
                ) {
                    Text(text = stringResource(id = R.string.map_refresh))
                }
            }
            if (!state.hasLocationPermission) {
                OutlinedButton(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text(text = stringResource(id = R.string.map_enable_location))
                }
            }
            state.locationError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Box(Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        uiSettings = uiSettings
                    ) {
                        state.nearbyLocations.forEach { location ->
                            val markerPosition = LatLng(location.latitude, location.longitude)
                            val snippet = buildString {
                                location.description?.takeIf { it.isNotBlank() }?.let {
                                    append(it)
                                }
                                state.currentLocation?.let { current ->
                                    val distanceMeters = current.distanceTo(location.latitude, location.longitude)
                                    if (distanceMeters > 0) {
                                        if (isNotEmpty()) append(" • ")
                                        append(formatDistance(distanceMeters))
                                    }
                                }
                            }.ifEmpty { null }
                            Marker(
                                state = MarkerState(position = markerPosition),
                                title = location.title,
                                snippet = snippet,
                                onClick = {
                                    onSurveySelected(location.documentId)
                                    true
                                }
                            )
                        }
                    }
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDistance(distanceMeters: Int): String {
    return if (distanceMeters >= 1000) {
        val km = distanceMeters / 1000.0
        String.format("%.1f km", km)
    } else {
        "$distanceMeters m"
    }
}
