package com.qzone.feature.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyQuestion
import com.qzone.domain.repository.SurveyRepository
import com.qzone.domain.repository.UserRepository
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
            repository.markSurveyCompleted(surveyId)
            userRepository.recordSurveyCompletion(survey)
            _uiState.update { it.copy(isSubmitting = false, isComplete = true) }
        }
    }

    companion object {
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
