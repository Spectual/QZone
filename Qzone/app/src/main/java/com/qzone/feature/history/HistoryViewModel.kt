package com.qzone.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.UserSurveyHistoryItem
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.qzone.util.QLog

import com.qzone.data.model.SurveyStatus

data class HistoryUiState(
    val query: String = "",
    val completedRecords: List<UserSurveyHistoryItem> = emptyList(),
    val inProgressRecords: List<UserSurveyHistoryItem> = emptyList(),
    val filteredCompleted: List<UserSurveyHistoryItem> = emptyList(),
    val filteredInProgress: List<UserSurveyHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HistoryViewModel(
    private val surveyRepository: SurveyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refreshHistory()
        observeHistory()
    }

    private fun observeHistory() {
        viewModelScope.launch {
            surveyRepository.getUserSurveyHistory().collect { records ->
                val (completed, inProgress) = records.partition { it.isComplete || it.status == SurveyStatus.COMPLETE }
                QLog.d(TAG) { "History flow update completed=${completed.size} inProgress=${inProgress.size}" }
                _uiState.update { state ->
                    val filteredCompleted = filter(completed, state.query)
                    val filteredInProgress = filter(inProgress, state.query)
                    state.copy(
                        completedRecords = completed,
                        inProgressRecords = inProgress,
                        filteredCompleted = filteredCompleted,
                        filteredInProgress = filteredInProgress,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            QLog.d(TAG) { "refreshHistory() invoked" }
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                surveyRepository.refreshSurveyHistory()
            } catch (e: Exception) {
                QLog.w(TAG) { "Failed to refresh survey history: ${e.message}" }
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onQueryChange(query: String) {
        QLog.d(TAG) { "History query changed -> $query" }
        _uiState.update { state ->
            val filteredCompleted = filter(state.completedRecords, query)
            val filteredInProgress = filter(state.inProgressRecords, query)
            state.copy(
                query = query,
                filteredCompleted = filteredCompleted,
                filteredInProgress = filteredInProgress
            )
        }
    }

    private fun filter(list: List<UserSurveyHistoryItem>, query: String): List<UserSurveyHistoryItem> {
        if (query.isBlank()) return list
        val lower = query.lowercase()
        return list.filter { item ->
            item.surveyTitle.lowercase().contains(lower) || item.surveyDescription.lowercase().contains(lower)
        }
    }

    companion object {
        private const val TAG = "HistoryViewModel"
        fun factory(surveyRepository: SurveyRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(surveyRepository) as T
            }
        }
    }
}
