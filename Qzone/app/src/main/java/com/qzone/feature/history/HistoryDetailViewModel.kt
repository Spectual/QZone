package com.qzone.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.SurveyResponseDetail
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.qzone.util.QLog
import com.qzone.data.model.QuestionAnswerOption

data class HistoryDetailUiState(
    val isLoading: Boolean = true,
    val detail: SurveyResponseDetail? = null,
    val errorMessage: String? = null
)

class HistoryDetailViewModel(
    private val repository: SurveyRepository,
    private val responseId: String,
    private val fallbackSurveyId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = HistoryDetailUiState(isLoading = true)
            try {
                val detail = repository.getResponseDetail(responseId)?.let { ensureQuestionContent(it) }
                if (detail != null) {
                    _uiState.value = HistoryDetailUiState(isLoading = false, detail = detail)
                } else {
                    _uiState.value = HistoryDetailUiState(
                        isLoading = false,
                        errorMessage = "Unable to load survey history."
                    )
                }
            } catch (t: Throwable) {
                QLog.e(TAG, t) { "Failed to load response detail" }
                _uiState.value = HistoryDetailUiState(
                    isLoading = false,
                    errorMessage = t.message ?: "Unable to load survey history."
                )
            }
        }
    }

    private suspend fun ensureQuestionContent(detail: SurveyResponseDetail): SurveyResponseDetail {
        val missingQuestionText = detail.questionAnswers.any { it.questionContent.isBlank() || it.questionContent == DEFAULT_QUESTION_PLACEHOLDER }
        if (!missingQuestionText) {
            return detail
        }
        val resolvedSurveyId = detail.surveyId.ifBlank { fallbackSurveyId.orEmpty() }
        if (resolvedSurveyId.isBlank()) {
            return detail
        }
        val survey = runCatching { repository.getSurveyById(resolvedSurveyId) }.getOrNull()
        val questionMap = survey?.questions?.associateBy { it.id } ?: return detail
        val enrichedAnswers = detail.questionAnswers.map { answer ->
            val question = questionMap[answer.questionId]
            if (question != null) {
                val optionStates = question.options.orEmpty().map { option ->
                    val selected = answer.selectedOptions.any { selection ->
                        selection.equals(option.label, ignoreCase = true) || selection.equals(option.content, ignoreCase = true)
                    }
                    QuestionAnswerOption(
                        label = option.label,
                        content = option.content,
                        isSelected = selected
                    )
                }
                answer.copy(
                    questionContent = if (question.content.isNotBlank()) question.content else answer.questionContent,
                    options = optionStates
                )
            } else {
                answer
            }
        }
        return detail.copy(questionAnswers = enrichedAnswers)
    }

    companion object {
        private const val TAG = "HistoryDetailViewModel"
        private const val DEFAULT_QUESTION_PLACEHOLDER = "Question"

        fun factory(
            repository: SurveyRepository,
            responseId: String,
            surveyId: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryDetailViewModel(repository, responseId, surveyId) as T
            }
        }
    }
}
