package com.qzone.feature.survey.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyOption
import com.qzone.data.model.SurveyQuestion
import com.qzone.feature.survey.SurveyUiState
import com.qzone.ui.components.QzoneElevatedSurface
import com.qzone.ui.components.QzoneTag
import com.qzone.ui.components.qzoneScreenBackground
import com.qzone.R
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SurveyScreen(
    state: StateFlow<SurveyUiState>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
    onCompletionAcknowledged: () -> Unit,
    onAnswerChanged: (questionId: String, answer: String, toggle: Boolean) -> Unit
) {
    val uiState by state.collectAsState()
    val survey = uiState.survey
    val question = uiState.currentQuestion
    val totalQuestions = survey?.questions?.size ?: 0
    val currentIndex = (uiState.currentQuestionIndex + 1).coerceAtLeast(1)
    val progress = remember(totalQuestions, currentIndex) {
        if (totalQuestions == 0) 0f else currentIndex.toFloat() / totalQuestions.toFloat()
    }

    val topInsets = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BackHandler(enabled = !uiState.isComplete) {
        onClose()
    }

    if (uiState.isComplete) {
        SurveyCompletionDialog(
            points = uiState.earnedPoints ?: survey?.points ?: 0,
            onDismiss = onCompletionAcknowledged
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .qzoneScreenBackground()
            .padding(horizontal = 24.dp)
            .padding(top = topInsets + 20.dp, bottom = 36.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close survey")
            }
            Spacer(modifier = Modifier.weight(1f))
            if (totalQuestions > 0) {
                QzoneTag(
                    text = "$currentIndex / $totalQuestions",
                    emphasize = true
                )
            }
        }

        survey?.let {
            SurveyHeroCard(
                survey = it,
                progress = progress,
                currentIndex = currentIndex,
                totalQuestions = totalQuestions
            )
        }

        if (question != null) {
            SurveyQuestionCard(
                question = question,
                index = currentIndex,
                totalQuestions = totalQuestions,
                selectedOptions = uiState.answers[question.id].orEmpty(),
                onAnswerChanged = onAnswerChanged
            )
        } else {
            QzoneElevatedSurface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Preparing your next prompt…",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "We are loading the survey details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        uiState.validationError?.let { validationMessage ->
            Text(
                text = validationMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = uiState.currentQuestionIndex > 0,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Back")
            }

            val isLastQuestion = (totalQuestions - 1) == uiState.currentQuestionIndex
            Button(
                onClick = {
                    if (isLastQuestion) {
                        onSubmit()
                    } else {
                        onNext()
                    }
                },
                enabled = !uiState.isSubmitting,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isLastQuestion) "Submit" else "Continue")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }

    }
}

@Composable
private fun SurveyHeroCard(
    survey: Survey,
    progress: Float,
    currentIndex: Int,
    totalQuestions: Int
) {
    QzoneElevatedSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (survey.points > 0) {
                    QzoneTag(
                        text = "+${survey.points} pts",
                        emphasize = true
                    )
                }
                QzoneTag(
                    text = "Question $currentIndex of $totalQuestions",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = survey.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (survey.description.isNotBlank()) {
                    Text(
                        text = survey.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(MaterialTheme.shapes.large),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SurveyCompletionDialog(
    points: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        QzoneElevatedSurface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                Text(
                    text = "Thank you for taking part in the survey!",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "You earned $points pts",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun SurveyQuestionCard(
    question: SurveyQuestion,
    index: Int,
    totalQuestions: Int,
    selectedOptions: List<String>,
    onAnswerChanged: (questionId: String, answer: String, toggle: Boolean) -> Unit
) {
    QzoneElevatedSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Question $index",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = question.content,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QzoneTag(
                    text = questionTypeLabel(question.type),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
                if (question.required) {
                    QzoneTag(
                        text = "Required",
                        emphasize = true
                    )
                } else {
                    QzoneTag(
                        text = "Optional",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            val helperText = when (question.type.lowercase()) {
                "single" -> "Choose the option that best fits."
                "multiple" -> "Select every option that applies."
                "text" -> "Share any details or context you feel comfortable sharing."
                else -> null
            }
            helperText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            QuestionContent(
                question = question,
                selectedOptions = selectedOptions,
                onAnswerChanged = onAnswerChanged
            )
        }
    }
}

@Composable
private fun QuestionContent(
    question: SurveyQuestion,
    selectedOptions: List<String>,
    onAnswerChanged: (questionId: String, answer: String, toggle: Boolean) -> Unit
) {
    when (question.type.lowercase()) {
        "single" -> {
            SurveyOptionList(
                options = question.options.orEmpty(),
                selectedOptions = selectedOptions,
                onOptionSelected = { answer ->
                    onAnswerChanged(question.id, answer, false)
                }
            )
        }

        "multiple" -> {
            SurveyOptionList(
                options = question.options.orEmpty(),
                selectedOptions = selectedOptions,
                allowMultiple = true,
                onOptionSelected = { answer ->
                    onAnswerChanged(question.id, answer, true)
                }
            )
        }

        "text" -> {
            val value = selectedOptions.firstOrNull().orEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { updated ->
                        onAnswerChanged(question.id, updated, false)
                    },
                    placeholder = { Text("Type your response") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    supportingText = {
                        Text(
                            text = "${value.length} characters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                )
                Text(
                    text = "Thoughtful, descriptive answers help us tailor better surveys for you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> {
            Text(
                text = "Unsupported question type: ${question.type}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SurveyOptionList(
    options: List<SurveyOption>,
    selectedOptions: List<String>,
    allowMultiple: Boolean = false,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        options.forEachIndexed { index, option ->
            val selected = selectedOptions.contains(option.content)
            SurveyOptionCard(
                index = index,
                label = option.label,
                text = option.content,
                selected = selected,
                allowMultiple = allowMultiple,
                onClick = { onOptionSelected(option.content) }
            )
        }
    }
}

@Composable
private fun SurveyOptionCard(
    index: Int,
    label: String?,
    text: String,
    selected: Boolean,
    allowMultiple: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        }
    )

    QzoneElevatedSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoRipple { onClick() },
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (selected) 10.dp else 4.dp,
        borderColor = borderColor
    ) {
        Row(
            modifier = Modifier
                .background(backgroundColor, MaterialTheme.shapes.large)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OptionSelectionIndicator(selected = selected, label = index + 1, isMulti = allowMultiple)
            Column(modifier = Modifier.weight(1f)) {
                label?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun OptionSelectionIndicator(
    selected: Boolean,
    label: Int,
    isMulti: Boolean
) {
    val shape = MaterialTheme.shapes.medium
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    )
    Column(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(indicatorColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = label.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun questionTypeLabel(raw: String): String = when (raw.lowercase()) {
    "single" -> "Single choice"
    "multiple" -> "Multi-select"
    "text" -> "Open response"
    else -> raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    clickable(
        indication = null,
        interactionSource = interactionSource,
        onClick = onClick
    )
}
