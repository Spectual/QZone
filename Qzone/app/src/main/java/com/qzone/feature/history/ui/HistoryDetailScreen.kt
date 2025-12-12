package com.qzone.feature.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qzone.data.model.SurveyResponseDetail
import com.qzone.data.model.QuestionAnswerOption
import com.qzone.feature.history.HistoryDetailUiState
import com.qzone.ui.components.QzoneElevatedSurface
import com.qzone.ui.components.qzoneScreenBackground
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HistoryDetailScreen(
    state: StateFlow<HistoryDetailUiState>,
    onBack: () -> Unit
) {
    val uiState by state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .qzoneScreenBackground()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Survey history",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        val detail = uiState.detail
        when {
            uiState.isLoading -> {
                Text(
                    text = "Loading responses...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            detail != null -> {
                HistoryResponses(detail = detail)
            }
            else -> {
                Text(
                    text = uiState.errorMessage ?: "Unable to load survey history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun HistoryResponses(detail: SurveyResponseDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        QzoneElevatedSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Completion rate: ${detail.completionRate.toInt()}%",
                    style = MaterialTheme.typography.titleMedium
                )
                detail.responseTime?.let {
                    Text(
                        text = "Submitted at $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(detail.questionAnswers, key = { it.questionId }) { answer ->
                QzoneElevatedSurface {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = answer.questionContent,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (answer.options.isNotEmpty()) {
                            AnswerOptionsList(answer.options)
                        } else {
                            val responseText = when {
                                !answer.textAnswer.isNullOrBlank() -> answer.textAnswer
                                answer.selectedOptions.isNotEmpty() -> answer.selectedOptions.joinToString(", ")
                                else -> "No response"
                            }
                            Text(
                                text = responseText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerOptionsList(options: List<QuestionAnswerOption>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val indicatorColor = if (option.isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
                Spacer(
                    modifier = Modifier
                        .size(12.dp)
                        .background(indicatorColor, CircleShape)
                )
                Text(
                    text = "${option.label}. ${option.content}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (option.isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (option.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
