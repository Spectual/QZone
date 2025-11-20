package com.qzone.feature.feed.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qzone.data.model.Survey
import com.qzone.feature.feed.FeedUiState
import com.qzone.ui.components.QzoneElevatedSurface
import com.qzone.ui.components.QzoneTag
import com.qzone.ui.components.SurveyCard
import com.qzone.ui.components.qzoneScreenBackground
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
    state: StateFlow<FeedUiState>,
    onRefresh: () -> Unit,
    onSurveySelected: (String) -> Unit,
    onLocationPermissionGranted: () -> Unit = {}
) {
    val uiState by state.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onLocationPermissionGranted()
        }
    }

    // Removed auto permission request: user must tap 'Enable Location' to grant.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .qzoneScreenBackground()
            .padding(horizontal = 24.dp)
            .padding(top = topPadding + 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(text = "Nearby Surveys", style = MaterialTheme.typography.headlineSmall)
        if (!uiState.hasLocationPermission) {
            Spacer(modifier = Modifier.height(12.dp))
            AssistChip(
                onClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                label = { Text("Enable Location") },
                leadingIcon = { Icon(imageVector = Icons.Default.MyLocation, contentDescription = null) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Location access is disabled. Tap to enable for nearby surveys.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        // Show location info only after we actually have a location
        if (uiState.hasLocationPermission && uiState.currentLocation != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = uiState.currentLocation!!.toDisplayString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            uiState.completedCount.takeIf { it > 0 }?.let { count ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$count completed nearby",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (uiState.surveys.isEmpty()) {
            QzoneElevatedSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No active surveys nearby",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "We’re scouting new missions in your area. Try refreshing in a little while.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(uiState.surveys, key = { it.id }) { survey ->
                    SurveyCard(
                        survey = survey,
                        onClick = { onSurveySelected(survey.id) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                label = { Text("Top") },
                leadingIcon = { Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            AssistChip(
                onClick = onRefresh,
                label = { Text("Refresh") },
                leadingIcon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = null) }
            )
        }
    }
}


