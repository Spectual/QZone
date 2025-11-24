package com.qzone.feature.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyQuestion
import com.qzone.data.model.SurveyStatus
import com.qzone.data.repository.LocalSurveyRepository
import com.qzone.domain.repository.SurveyRepository
import com.qzone.domain.repository.UserRepository
import com.qzone.data.network.QzoneApiClient
import com.qzone.data.network.model.SubmitAnswerItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class SurveyUiState(
    val survey: Survey? = null,
    val currentQuestionIndex: Int = 0,
    val answers: Map<String, List<String>> = emptyMap(),
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false,
    val validationError: String? = null,
    val earnedPoints: Int? = null
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
    private val surveyId: String,
    private val localSurveyRepository: LocalSurveyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SurveyUiState())
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()
    private var lastKnownUserPoints: Int = 0

    init {
        viewModelScope.launch {
            userRepository.currentUser.collect { profile ->
                lastKnownUserPoints = profile.totalPoints
            }
        }
        viewModelScope.launch {
            val survey = repository.getSurveyById(surveyId)
            _uiState.update { it.copy(survey = survey) }
            survey?.let {
                runCatching {
                    localSurveyRepository.saveSurvey(it)
                }.onFailure { throwable ->
                    Log.w(TAG, "Failed to cache survey locally", throwable)
                }
            }
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
            state.copy(answers = updatedAnswers, validationError = null)
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
            val currentState = _uiState.value
            val survey = currentState.survey ?: return@launch
            val answersMap = currentState.answers
            val hasIncompleteRequired = survey.questions.any { question ->
                question.required && !question.isAnswered(answersMap[question.id])
            }
            if (hasIncompleteRequired) {
                _uiState.update { it.copy(validationError = REQUIRED_QUESTION_ERROR) }
                return@launch
            }
            _uiState.update { it.copy(isSubmitting = true, validationError = null) }
            // Build request body from answers
            val items = buildSubmitItems(survey, answersMap)
            Log.d(TAG, "Submitting answers: surveyId=" + surveyId + ", items=" + items.size)
            Log.d(TAG, "Submit payload JSON: ${items.toDebugJson()}")
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
                val earnedPoints = refreshUserPoints()
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isComplete = true,
                        validationError = null,
                        earnedPoints = earnedPoints ?: survey.points
                    )
                }
            } else {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun cacheProgress(onFinished: (() -> Unit)? = null) {
        val currentState = _uiState.value
        val survey = currentState.survey
        if (survey == null) {
            onFinished?.invoke()
            return
        }
        val answersMap = currentState.answers
        if (answersMap.isEmpty()) {
            onFinished?.invoke()
            return
        }
        viewModelScope.launch {
            val items = buildSubmitItems(survey, answersMap)
            Log.d(TAG, "Caching survey progress: surveyId=" + surveyId + ", items=" + items.size)
            try {
                val response = QzoneApiClient.service.submitResponses(items)
                if (response.success) {
                    val questionCount = if (survey.questionCount > 0) survey.questionCount else survey.questions.size
                    val updated = survey.copy(
                        answers = answersMap,
                        status = SurveyStatus.IN_PROGRESS,
                        isCompleted = false,
                        questionCount = questionCount
                    )
                    try {
                        repository.saveSurveyProgress(updated)
                    } catch (cacheError: Throwable) {
                        Log.w(TAG, "Failed to update local survey progress cache", cacheError)
                    }
                } else {
                    Log.w(TAG, "Cache progress API failed: ${response.msg}")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to cache survey progress", t)
            } finally {
                onFinished?.invoke()
            }
        }
    }

    private fun buildSubmitItems(
        survey: Survey,
        answersMap: Map<String, List<String>>
    ): List<SubmitAnswerItem> {
        return survey.questions.map { q ->
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
    }

    private suspend fun refreshUserPoints(): Int? {
        val previousPoints = lastKnownUserPoints
        return runCatching { QzoneApiClient.service.getCurrentUserProfile() }
            .onFailure { throwable ->
                Log.w(TAG, "Unable to refresh user points after survey completion", throwable)
            }
            .getOrNull()
            ?.let { result ->
                if (result.success && result.data != null) {
                    val newPoints = result.data.currentPoints
                    val delta = (newPoints - previousPoints).coerceAtLeast(0)
                    lastKnownUserPoints = newPoints
                    runCatching { userRepository.updatePoints(newPoints) }
                        .onFailure { Log.w(TAG, "Failed to update local user points cache", it) }
                    delta
                } else {
                    Log.w(TAG, "User profile fetch failed while refreshing points: ${result.msg}")
                    null
                }
            }
    }

    companion object {
        private const val TAG = "SurveyViewModel"
        private const val REQUIRED_QUESTION_ERROR = "Please complete all required questions."
        fun factory(
            repository: SurveyRepository,
            userRepository: UserRepository,
            surveyId: String,
            localSurveyRepository: LocalSurveyRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SurveyViewModel(repository, userRepository, surveyId, localSurveyRepository) as T
                }
            }
    }
}

private fun List<SubmitAnswerItem>.toDebugJson(): String {
    return joinToString(prefix = "[", postfix = "]") { item ->
        val selectedPart = item.selected?.let { "\"$it\"" } ?: "null"
        val contentEscaped = item.content?.replace("\"", "\\\"")
        val contentPart = contentEscaped?.let { "\"$it\"" } ?: "null"
        "{\"questionId\":\"${item.questionId}\",\"selected\":$selectedPart,\"content\":$contentPart}"
    }
}

private fun SurveyQuestion.isAnswered(selectedValues: List<String>?): Boolean {
    val answers = selectedValues.orEmpty()
    return when (type.lowercase()) {
        "text" -> answers.firstOrNull()?.isNullOrBlank() == false
        else -> answers.any { it.isNotBlank() }
    }
}
