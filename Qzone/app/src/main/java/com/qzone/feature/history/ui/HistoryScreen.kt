package com.qzone.feature.history.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qzone.data.model.UserSurveyHistoryItem
import com.qzone.feature.history.HistoryUiState
import com.qzone.ui.components.QzoneElevatedSurface
import com.qzone.ui.components.qzoneScreenBackground
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HistoryScreen(
    state: StateFlow<HistoryUiState>,
    onQueryChanged: (String) -> Unit,
    onInProgressSurveyClick: (UserSurveyHistoryItem) -> Unit,
    onCompletedSurveyClick: (UserSurveyHistoryItem) -> Unit
) {
    val uiState by state.collectAsState()
    val topPadding = 0.dp
    var selectedTab by remember { mutableIntStateOf(0) }
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

        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search by title or description") },
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

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

        when {
            uiState.isLoading && items.isEmpty() -> {
                HistoryPlaceholder(
                    title = "Loading history",
                    body = "Fetching your recent responses...",
                    showProgress = true
                )
            }
            uiState.errorMessage != null && items.isEmpty() -> {
                HistoryPlaceholder(
                    title = "Unable to load history",
                    body = uiState.errorMessage ?: "Please try again later."
                )
            }
            items.isEmpty() -> {
                val emptyText = when {
                    uiState.query.isNotBlank() -> "No records match your search."
                    selectedTab == 0 -> "You have no surveys in progress."
                    else -> "You have not completed any surveys yet."
                }
                HistoryPlaceholder(
                    title = "Nothing to show",
                    body = emptyText
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(
                        items,
                        key = { record -> record.responseId.ifBlank { record.surveyId } }
                    ) { record ->
                        HistoryRecordCard(
                            record = record,
                            onClick = {
                                if (selectedTab == 0) {
                                    onInProgressSurveyClick(record)
                                } else {
                                    onCompletedSurveyClick(record)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPlaceholder(
    title: String,
    body: String,
    showProgress: Boolean = false
) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: UserSurveyHistoryItem,
    onClick: () -> Unit
) {
    QzoneElevatedSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = record.surveyTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${record.completionRate.toInt()}% complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = record.responseTime ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
