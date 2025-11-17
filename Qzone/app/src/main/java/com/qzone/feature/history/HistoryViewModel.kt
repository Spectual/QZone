package com.qzone.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.SurveyHistoryItem
import com.qzone.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val query: String = "",
    val completedEntries: List<SurveyHistoryItem> = emptyList(),
    val inProgressEntries: List<SurveyHistoryItem> = emptyList(),
    val filteredCompleted: List<SurveyHistoryItem> = emptyList(),
    val filteredInProgress: List<SurveyHistoryItem> = emptyList()
)

class HistoryViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                _uiState.update {
                    val (completed, inProgress) = user.history.partition { it.completedAt.isNotBlank() }
                    it.copy(
                        completedEntries = completed,
                        inProgressEntries = inProgress,
                        filteredCompleted = filter(completed, it.query),
                        filteredInProgress = filter(inProgress, it.query)
                    )
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            val filteredCompleted = filter(state.completedEntries, query)
            val filteredInProgress = filter(state.inProgressEntries, query)
            state.copy(
                query = query,
                filteredCompleted = filteredCompleted,
                filteredInProgress = filteredInProgress
            )
        }
    }

    private fun filter(list: List<SurveyHistoryItem>, query: String): List<SurveyHistoryItem> {
        if (query.isBlank()) return list
        val lower = query.lowercase()
        return list.filter { item ->
            item.title.lowercase().contains(lower) || item.locationLabel.lowercase().contains(lower)
        }
    }

    companion object {
        fun factory(userRepository: UserRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(userRepository) as T
            }
        }
    }
}
