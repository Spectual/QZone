package com.qzone.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.Survey
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.qzone.data.model.SurveyStatus

data class HistoryUiState(
    val query: String = "",
    val completedSurveys: List<Survey> = emptyList(),
    val inProgressSurveys: List<Survey> = emptyList(),
    val filteredCompleted: List<Survey> = emptyList(),
    val filteredInProgress: List<Survey> = emptyList()
)

class HistoryViewModel(
    private val surveyRepository: SurveyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        refreshHistory()
        viewModelScope.launch {
            combine(
                surveyRepository.getCompletedSurveys(),
                surveyRepository.getUncompletedSurveys()
            ) { completed, uncompleted ->
                // Filter uncompleted surveys to only include those with PARTIAL status
                val actualInProgress = uncompleted.filter { it.status == SurveyStatus.PARTIAL }
                completed to actualInProgress
            }.collect { (completed, inProgress) ->
                _uiState.update {
                    it.copy(
                        completedSurveys = completed,
                        inProgressSurveys = inProgress,
                        filteredCompleted = filter(completed, it.query),
                        filteredInProgress = filter(inProgress, it.query)
                    )
                }
            }
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            try {
                surveyRepository.refreshSurveyHistory()
            } catch (e: Exception) {
                // Handle error if needed, currently just logging in repository
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            val filteredCompleted = filter(state.completedSurveys, query)
            val filteredInProgress = filter(state.inProgressSurveys, query)
            state.copy(
                query = query,
                filteredCompleted = filteredCompleted,
                filteredInProgress = filteredInProgress
            )
        }
    }

    private fun filter(list: List<Survey>, query: String): List<Survey> {
        if (query.isBlank()) return list
        val lower = query.lowercase()
        return list.filter { item ->
            item.title.lowercase().contains(lower) || item.description.lowercase().contains(lower)
        }
    }

    companion object {
        fun factory(surveyRepository: SurveyRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(surveyRepository) as T
            }
        }
    }
}
