package com.qzone.feature.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.Survey
import com.qzone.data.model.SurveyQuestion
import com.qzone.data.model.SurveyResponseDetail
import com.qzone.data.model.SurveyStatus
import com.qzone.data.network.QzoneApiClient
import com.qzone.data.network.model.SubmitAnswerItem
import com.qzone.data.repository.LocalSurveyRepository
import com.qzone.domain.repository.SurveyRepository
import com.qzone.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.qzone.util.QLog


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
    private val attemptedResponseRestores = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            userRepository.currentUser.collect { profile ->
                lastKnownUserPoints = profile.totalPoints
            }
        }
        viewModelScope.launch {
            runCatching { localSurveyRepository.getSurveyById(surveyId) }
                .onFailure { throwable ->
                    QLog.w(TAG) { "Failed to load local survey cache: ${throwable.message}" }
                }
                .getOrNull()
                ?.let { cached ->
                    QLog.d(TAG) { "Loaded local survey cache id=${cached.id} answers=${cached.answers.size}" }
                    applySurveyState(cached)
                }
        }
        viewModelScope.launch {
            val survey = repository.getSurveyById(surveyId)
            val resolvedSurvey = survey?.let { applySurveyState(it) }
            if (survey == null) {
                _uiState.update { it.copy(survey = null, answers = emptyMap()) }
            }
            resolvedSurvey?.let {
                runCatching {
                    localSurveyRepository.saveSurvey(it)
                }.onFailure { throwable ->
                    QLog.w(TAG) { "Failed to cache survey locally: ${throwable.message}" }
                }
            }
        }
    }

    fun onAnswerChanged(questionId: String, value: String, toggle: Boolean = false) {
        QLog.d(TAG) { "onAnswerChanged question=$questionId value=$value toggle=$toggle" }
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
        QLog.d(TAG) { "submit() invoked for surveyId=$surveyId" }
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
            val items = buildSubmitItems(survey, answersMap)
            QLog.d(TAG) { "Submitting answers: surveyId=$surveyId items=${items.size}" }
            QLog.d(TAG) { "Submit payload JSON: ${items.toDebugJson()}" }
            val response = try {
                QzoneApiClient.service.submitResponses(items)
            } catch (t: Throwable) {
                QLog.e(TAG, t) { "Submit responses failed" }
                _uiState.update { it.copy(isSubmitting = false) }
                return@launch
            }
            QLog.d(TAG) { "Submit response success=${response.success} code=${response.code} msg=${response.msg}" }
            if (response.success) {
                val completionResponseId = response.data?.responseId
                val serverEarnedPoints = response.data?.earnedPoints
                val earnedDelta = when {
                    serverEarnedPoints != null -> {
                        applyEarnedPointsDelta(serverEarnedPoints)
                        serverEarnedPoints
                    }
                    else -> refreshUserPoints()
                }
                repository.markSurveyCompleted(surveyId, completionResponseId)
                runCatching { localSurveyRepository.markSurveyCompleted(surveyId) }
                    .onFailure { throwable -> QLog.w(TAG) { "Failed to update local survey completion: ${throwable.message}" } }
                val resolvedPoints = earnedDelta ?: survey.points
                val completedSurvey = survey.copy(
                    points = resolvedPoints,
                    isCompleted = true,
                    status = SurveyStatus.COMPLETE,
                    responseId = completionResponseId ?: survey.responseId
                )
                userRepository.recordSurveyCompletion(completedSurvey)
                _uiState.update {
                    it.copy(
                        survey = completedSurvey,
                        isSubmitting = false,
                        isComplete = true,
                        validationError = null,
                        earnedPoints = resolvedPoints
                    )
                }
            } else {
                _uiState.update { it.copy(isSubmitting = false) }
                QLog.w(TAG) { "Submit response unsuccessful: ${response.msg}" }
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
            QLog.d(TAG) { "cacheProgress surveyId=$surveyId answers=${answersMap.size}" }
            val items = buildSubmitItems(survey, answersMap)
            try {
                val response = QzoneApiClient.service.submitResponses(items)
                if (response.success) {
                    val responseId = response.data?.responseId ?: survey.responseId
                    val questionCount = if (survey.questionCount > 0) survey.questionCount else survey.questions.size
                    val updated = survey.copy(
                        answers = answersMap,
                        status = SurveyStatus.IN_PROGRESS,
                        isCompleted = false,
                        questionCount = questionCount,
                        responseId = responseId
                    )
                    try {
                        repository.saveSurveyProgress(updated)
                    } catch (cacheError: Throwable) {
                        QLog.w(TAG) { "Failed to update local survey progress cache: ${cacheError.message}" }
                    }
                } else {
                    QLog.w(TAG) { "Cache progress API failed: ${response.msg}" }
                }
            } catch (t: Throwable) {
                QLog.w(TAG) { "Failed to cache survey progress: ${t.message}" }
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
                QLog.w(TAG) { "Unable to refresh user points after survey completion: ${throwable.message}" }
            }
            .getOrNull()
            ?.let { result ->
                if (result.success && result.data != null) {
                    val newPoints = result.data.currentPoints
                    val delta = (newPoints - previousPoints).coerceAtLeast(0)
                    lastKnownUserPoints = newPoints
                    runCatching { userRepository.updatePoints(newPoints) }
                        .onFailure { QLog.w(TAG) { "Failed to update local user points cache: ${it.message}" } }
                    delta
                } else {
                    QLog.w(TAG) { "User profile fetch failed while refreshing points: ${result.msg}" }
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

    private suspend fun applyEarnedPointsDelta(delta: Int) {
        if (delta <= 0) return
        val updatedTotal = (lastKnownUserPoints + delta).coerceAtLeast(0)
        lastKnownUserPoints = updatedTotal
        runCatching { userRepository.updatePoints(updatedTotal) }
            .onFailure { throwable ->
                QLog.w(TAG) { "Failed to apply earned points delta: ${throwable.message}" }
            }
    }

    private fun applySurveyState(incoming: Survey): Survey {
        val existingAnswers = _uiState.value.answers
        val restoredAnswers = when {
            incoming.answers.isNotEmpty() -> incoming.answers
            existingAnswers.isNotEmpty() -> existingAnswers
            else -> emptyMap()
        }
        val questionCount = incoming.questions.size.takeIf { it > 0 } ?: incoming.questionCount
        val resolvedIndex = computeResumeIndex(incoming, restoredAnswers, questionCount)
        val resolvedSurvey = incoming.copy(
            answers = restoredAnswers,
            currentQuestionIndex = resolvedIndex
        )
        _uiState.update {
            it.copy(
                survey = resolvedSurvey,
                answers = restoredAnswers,
                currentQuestionIndex = resolvedIndex
            )
        }
        if (restoredAnswers.isEmpty()) {
            maybeRestoreAnswersFromResponse(resolvedSurvey)
        }
        return resolvedSurvey
    }

    private fun computeResumeIndex(
        survey: Survey,
        answers: Map<String, List<String>>,
        questionCount: Int
    ): Int {
        if (questionCount <= 0) return 0
        if (answers.isEmpty()) {
            return survey.currentQuestionIndex.coerceIn(0, questionCount - 1)
        }
        val firstUnanswered = survey.questions.indexOfFirst { question ->
            !question.isAnswered(answers[question.id])
        }
        return if (firstUnanswered >= 0) firstUnanswered else {
            (survey.questions.lastIndex).coerceAtLeast(0)
        }
    }

    private fun maybeRestoreAnswersFromResponse(survey: Survey) {
        val responseId = survey.responseId?.takeIf { it.isNotBlank() } ?: return
        if (survey.questions.isEmpty()) {
            return
        }
        if (!attemptedResponseRestores.add(responseId)) {
            return
        }
        viewModelScope.launch {
            val detail = runCatching { repository.getResponseDetail(responseId) }
                .onFailure { throwable ->
                    QLog.w(TAG) { "Failed to fetch response detail for $responseId: ${throwable.message}" }
                }
                .getOrNull()
            if (detail == null) {
                attemptedResponseRestores.remove(responseId)
                return@launch
            }
            val currentSurvey = _uiState.value.survey ?: survey
            val restoredAnswers = mapDetailAnswers(detail, currentSurvey)
            if (restoredAnswers.isEmpty()) {
                attemptedResponseRestores.remove(responseId)
                return@launch
            }
            applySurveyState(currentSurvey.copy(answers = restoredAnswers))
            try {
                localSurveyRepository.saveSurvey(currentSurvey.copy(answers = restoredAnswers))
            } catch (throwable: Throwable) {
                QLog.w(TAG) { "Failed to persist restored answers: ${throwable.message}" }
            }
        }
    }

    private fun mapDetailAnswers(
        detail: SurveyResponseDetail,
        survey: Survey
    ): Map<String, List<String>> {
        if (survey.questions.isEmpty()) return emptyMap()
        val questionMap = survey.questions.associateBy { it.id }
        val restored = mutableMapOf<String, List<String>>()
        detail.questionAnswers.forEach { answer ->
            val question = questionMap[answer.questionId] ?: return@forEach
            when (question.type.lowercase()) {
                "text" -> {
                    answer.textAnswer
                        ?.takeIf { it.isNotBlank() }
                        ?.let { restored[question.id] = listOf(it) }
                }
                "multiple" -> {
                    val selections = answer.selectedOptions.mapNotNull { selected ->
                        matchOptionValue(question, selected)
                    }
                    if (selections.isNotEmpty()) {
                        restored[question.id] = selections
                    }
                }
                else -> {
                    val selection = answer.selectedOptions.firstOrNull()?.let { selected ->
                        matchOptionValue(question, selected)
                    }
                    selection?.let { restored[question.id] = listOf(it) }
                }
            }
        }
        return restored
    }

    private fun matchOptionValue(question: SurveyQuestion, rawValue: String?): String? {
        val value = rawValue?.takeIf { it.isNotBlank() } ?: return null
        val options = question.options.orEmpty()
        val matched = options.firstOrNull {
            it.label.equals(value, ignoreCase = true) || it.content.equals(value, ignoreCase = true)
        }
        return matched?.content ?: value
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
