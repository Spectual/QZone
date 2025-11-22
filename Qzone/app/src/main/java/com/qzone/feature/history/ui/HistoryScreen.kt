package com.qzone.feature.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.qzone.data.model.SurveyHistoryItem
import com.qzone.domain.repository.LocationRepository
import com.qzone.feature.history.HistoryUiState
import com.qzone.ui.components.QzoneElevatedSurface
import com.qzone.ui.components.QzoneTag
import com.qzone.ui.components.SurveyCard
import com.qzone.ui.components.qzoneScreenBackground
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HistoryScreen(
    state: StateFlow<HistoryUiState>,
    onQueryChanged: (String) -> Unit,
    onSurveyClick: (String) -> Unit,
    locationRepository: LocationRepository? = null
) {
    val uiState by state.collectAsState()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var selectedTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val tabs = listOf("In Progress", "Completed")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .qzoneScreenBackground()
            .padding(horizontal = 24.dp)
            .padding(top = topPadding + 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        QzoneElevatedSurface {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search by title or location") },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        val items = if (selectedTab == 0) uiState.filteredInProgress else uiState.filteredCompleted

        if (items.isEmpty()) {
            QzoneElevatedSurface(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (selectedTab == 0) "No surveys in progress" else "No completed surveys",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Surveys will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(items, key = { it.id }) { survey ->
                    SurveyCard(
                        survey = survey,
                        onClick = { onSurveyClick(survey.id) },
                        locationRepository = locationRepository
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTimelineRow(
    item: SurveyHistoryItem,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxIndicator(color = MaterialTheme.colorScheme.primary)
            if (!isLast) {
                Spacer(modifier = Modifier.height(4.dp))
                Spacer(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }
        }

        QzoneElevatedSurface(
            modifier = Modifier
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    QzoneTag(
                        text = "+${item.pointsEarned} pts",
                        emphasize = true
                    )
                }
                Text(
                    text = item.locationLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.completedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun BoxIndicator(color: Color, size: Dp = 14.dp) {
    Spacer(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
