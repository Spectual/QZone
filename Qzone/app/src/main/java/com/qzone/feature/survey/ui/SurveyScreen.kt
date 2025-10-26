package com.qzone.feature.survey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed
import com.qzone.data.model.SurveyQuestionType
import com.qzone.feature.survey.SurveyUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SurveyScreen(
    state: StateFlow<SurveyUiState>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
    onAnswerChanged: (questionId: String, answer: String, toggle: Boolean) -> Unit
) {
    val uiState by state.collectAsState()
    val survey = uiState.survey
    val question = uiState.currentQuestion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close survey")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "${uiState.currentQuestionIndex + 1}/${survey?.questions?.size ?: 1}")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = survey?.title ?: "", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = survey?.subtitle ?: "", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        if (question != null) {
            Text(text = question.prompt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            QuestionContent(
                questionId = question.id,
                type = question.type,
                options = question.options,
                helper = question.helperText,
                selectedOptions = uiState.answers[question.id].orEmpty(),
                onAnswerChanged = onAnswerChanged
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onPrevious, enabled = uiState.currentQuestionIndex > 0) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Previous")
            }
            Button(
                onClick = {
                    if ((survey?.questions?.lastIndex ?: 0) == uiState.currentQuestionIndex) {
                        onSubmit()
                    } else {
                        onNext()
                    }
                },
                enabled = !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if ((survey?.questions?.lastIndex ?: 0) == uiState.currentQuestionIndex) "Submit" else "Next")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.isComplete) {
            Text(
                text = "Thanks for completing the survey!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuestionContent(
    questionId: String,
    type: SurveyQuestionType,
    options: List<String>,
    helper: String?,
    selectedOptions: List<String>,
    onAnswerChanged: (questionId: String, answer: String, toggle: Boolean) -> Unit
) {
    when (type) {
        SurveyQuestionType.SINGLE_CHOICE, SurveyQuestionType.RATING -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    SingleSelectOption(
                        text = option,
                        selected = selectedOptions.contains(option),
                        onClick = { onAnswerChanged(questionId, option, false) }
                    )
                }
            }
        }
        SurveyQuestionType.MULTI_CHOICE -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    SingleSelectOption(
                        text = option,
                        selected = selectedOptions.contains(option),
                        onClick = { onAnswerChanged(questionId, option, true) }
                    )
                }
            }
        }
        SurveyQuestionType.SHORT_TEXT -> {
            OutlinedTextField(
                value = selectedOptions.firstOrNull().orEmpty(),
                onValueChange = { value -> onAnswerChanged(questionId, value, false) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = helper?.let { { Text(it) } }
            )
        }
    }
}

@Composable
private fun SingleSelectOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = shape
            )
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .clickableNoRipple { onClick() }
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    clickable(
        indication = null,
        interactionSource = interactionSource,
        onClick = onClick
    )
}
