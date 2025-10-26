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
    val entries: List<SurveyHistoryItem> = emptyList(),
    val filteredEntries: List<SurveyHistoryItem> = emptyList()
)

class HistoryViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                _uiState.update {
                    val list = user.history
                    it.copy(entries = list, filteredEntries = filter(list, it.query))
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = filter(state.entries, query)
            state.copy(query = query, filteredEntries = filtered)
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
