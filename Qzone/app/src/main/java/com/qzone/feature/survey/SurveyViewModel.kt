package com.qzone.feature.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyQuestion
import com.qzone.domain.repository.SurveyRepository
import com.qzone.domain.repository.UserRepository
import com.qzone.data.network.QzoneApiClient
import com.qzone.data.network.model.SubmitAnswerItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class SurveyUiState(
    val survey: Survey? = null,
    val currentQuestionIndex: Int = 0,
    val answers: Map<String, List<String>> = emptyMap(),
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false
) {
    val currentQuestion: SurveyQuestion?
        get() = survey?.questions?.getOrNull(currentQuestionIndex)

    val progressFraction: Float
        get() {
            val total = survey?.questions?.size ?: return 0f
            if (total == 0) return 0f
            return (currentQuestionIndex + 1f) / total.toFloat()
        }
}

class SurveyViewModel(
    private val repository: SurveyRepository,
    private val userRepository: UserRepository,
    private val surveyId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(SurveyUiState())
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val survey = repository.getSurveyById(surveyId)
            _uiState.update { it.copy(survey = survey) }
        }
    }

    fun onAnswerChanged(questionId: String, value: String, toggle: Boolean = false) {
        Log.d(TAG, "onAnswerChanged: questionId=" + questionId + ", value=" + value + ", toggle=" + toggle)
        val survey = _uiState.value.survey ?: return
        val question = survey.questions.firstOrNull { it.id == questionId } ?: return
        _uiState.update { state ->
            val updatedAnswers = when (question.type.lowercase()) {
                "multiple" -> {
                    val current = state.answers[questionId]?.toMutableSet() ?: mutableSetOf()
                    if (toggle) {
                        if (current.contains(value)) current.remove(value) else current.add(value)
                    } else {
                        current.clear(); current.add(value)
                    }
                    state.answers + (questionId to current.toList())
                }
                else -> state.answers + (questionId to listOf(value))
            }
            state.copy(answers = updatedAnswers)
        }
    }

    fun onNext() {
        val survey = _uiState.value.survey ?: return
        val newIndex = (_uiState.value.currentQuestionIndex + 1).coerceAtMost(survey.questions.lastIndex)
        _uiState.update { it.copy(currentQuestionIndex = newIndex) }
    }

    fun onPrevious() {
        val newIndex = (_uiState.value.currentQuestionIndex - 1).coerceAtLeast(0)
        _uiState.update { it.copy(currentQuestionIndex = newIndex) }
    }

    fun submit() {
        viewModelScope.launch {
            val survey = _uiState.value.survey ?: return@launch
            _uiState.update { it.copy(isSubmitting = true) }
            // Build request body from answers
            val answersMap = _uiState.value.answers
            val items = survey.questions.map { q ->
                val selectedValues = answersMap[q.id].orEmpty()
                val qType = q.type.lowercase()
                when (qType) {
                    "text" -> {
                        SubmitAnswerItem(
                            questionId = q.id,
                            selected = null,
                            content = selectedValues.firstOrNull()
                        )
                    }
                    "multiple" -> {
                        // Map selected option contents to their labels (e.g., A, B)
                        val labels = selectedValues.mapNotNull { ans ->
                            q.options?.firstOrNull { it.content == ans }?.label ?: ans
                        }
                        SubmitAnswerItem(
                            questionId = q.id,
                            selected = labels.joinToString(",").ifBlank { null },
                            content = null
                        )
                    }
                    else -> {
                        // single choice or other types
                        val label = selectedValues.firstOrNull()?.let { ans ->
                            q.options?.firstOrNull { it.content == ans }?.label ?: ans
                        }
                        SubmitAnswerItem(
                            questionId = q.id,
                            selected = label,
                            content = null
                        )
                    }
                }
            }
            Log.d(TAG, "Submitting answers: surveyId=" + surveyId + ", items=" + items.size)
            val response = try {
                QzoneApiClient.service.submitResponses(items)
            } catch (t: Throwable) {
                Log.e(TAG, "Submit responses failed", t)
                _uiState.update { it.copy(isSubmitting = false) }
                return@launch
            }
            Log.d(TAG, "Submit response -> success=" + response.success + ", code=" + response.code + ", msg=" + (response.msg ?: "") + ", data=" + (response.data ?: ""))
            if (response.success) {
                repository.markSurveyCompleted(surveyId)
                userRepository.recordSurveyCompletion(survey)
                _uiState.update { it.copy(isSubmitting = false, isComplete = true) }
            } else {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    companion object {
        private const val TAG = "SurveyViewModel"
        fun factory(
            repository: SurveyRepository,
            userRepository: UserRepository,
            surveyId: String
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SurveyViewModel(repository, userRepository, surveyId) as T
                }
            }
    }
}
