package com.example.p2pchat.feature.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.model.Thread
import com.example.p2pchat.core.storage.repository.ThreadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ConversationsUiState {
    object Loading : ConversationsUiState
    data class Success(val conversations: List<Thread>) : ConversationsUiState
    object Empty : ConversationsUiState
}

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val threadRepository: ThreadRepository
) : ViewModel() {

    val uiState: StateFlow<ConversationsUiState> = threadRepository.getAllThreads()
        .map { threads ->
            if (threads.isEmpty()) {
                ConversationsUiState.Empty
            } else {
                ConversationsUiState.Success(threads)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConversationsUiState.Loading
        )
}
