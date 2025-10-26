package com.qzone.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.Survey
import com.qzone.domain.repository.SurveyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val surveys: List<Survey> = emptyList(),
    val completedCount: Int = 0,
    val isRefreshing: Boolean = false
)

class FeedViewModel(private val repository: SurveyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.nearbySurveys.collect { surveys ->
                val active = surveys.filterNot { it.isCompleted }
                val completed = surveys.count { it.isCompleted }
                _uiState.update { it.copy(surveys = active, completedCount = completed) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repository.refreshNearby()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    companion object {
        fun factory(repository: SurveyRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FeedViewModel(repository) as T
            }
        }
    }
}
