package com.qzone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qzone.data.model.Survey

@Composable
fun SurveyCard(
    survey: Survey,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.2f
    val gradientColors = if (isDark) {
        listOf(Color(0xFF020203), Color(0xFF16171F), Color(0xFF2C2E37))
    } else {
        listOf(Color(0xFFFFFFFF), Color(0xFFE9EBF1), Color(0xFFD4D7DF))
    }
    val shape = MaterialTheme.shapes.extraLarge

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(gradientColors))
            .border(1.dp, colorScheme.outline.copy(alpha = 0.05f), shape)
            .clickable { onClick() }
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: Title and Points
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = survey.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (survey.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = survey.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (survey.points > 0) {
                QzoneTag(
                    text = "+${survey.points} pts",
                    emphasize = true
                )
            }
        }

        // Location
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Location based survey",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }

        // Footer: Status / Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (survey.isCompleted) {
                QzoneTag(
                    text = "Completed",
                    containerColor = colorScheme.secondaryContainer,
                    contentColor = colorScheme.onSecondaryContainer
                )
            } else {
                // Calculate progress if started
                val total = survey.questions.size
                val current = survey.currentQuestionIndex
                if (current > 0 && total > 0) {
                     val progress = (current.toFloat() / total) * 100
                     Text(
                         text = "${progress.toInt()}% completed",
                         style = MaterialTheme.typography.labelMedium,
                         color = colorScheme.primary
                     )
                } else {
                    QzoneTag(
                        text = "${survey.questions.size} questions",
                        containerColor = colorScheme.surfaceVariant,
                        contentColor = colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Start",
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Progress Bar for incomplete but started surveys
        if (!survey.isCompleted && survey.currentQuestionIndex > 0 && survey.questions.isNotEmpty()) {
             LinearProgressIndicator(
                 progress = survey.currentQuestionIndex.toFloat() / survey.questions.size,
                 modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                 color = colorScheme.primary,
                 trackColor = colorScheme.surfaceVariant
             )
        }
    }
}
