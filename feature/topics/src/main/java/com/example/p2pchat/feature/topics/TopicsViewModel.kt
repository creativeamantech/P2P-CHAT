package com.example.p2pchat.feature.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.model.Topic
import com.example.p2pchat.core.storage.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface TopicsUiState {
    object Loading : TopicsUiState
    data class Success(val topics: List<Topic>) : TopicsUiState
    object Empty : TopicsUiState
}

@HiltViewModel
class TopicsViewModel @Inject constructor(
    private val topicRepository: TopicRepository
) : ViewModel() {

    val uiState: StateFlow<TopicsUiState> = topicRepository.getAllTopics()
        .map { topics ->
            if (topics.isEmpty()) {
                TopicsUiState.Empty
            } else {
                TopicsUiState.Success(topics)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TopicsUiState.Loading
        )
}
