package com.example.p2pchat.feature.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.model.Message
import com.example.p2pchat.core.model.Thread
import com.example.p2pchat.core.storage.entity.MessageEntity
import com.example.p2pchat.core.storage.repository.MessageRepository
import com.example.p2pchat.core.storage.repository.ThreadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ConversationsUiState {
    object Loading : ConversationsUiState
    data class Success(
        val conversations: List<Thread>,
        val searchResults: List<MessageEntity> = emptyList(),
        val isSearching: Boolean = false
    ) : ConversationsUiState
    object Empty : ConversationsUiState
}

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchResults = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(emptyList())
        } else {
            messageRepository.searchMessages(query)
        }
    }

    private val conversations = threadRepository.getAllThreads()

    val uiState: StateFlow<ConversationsUiState> = combine(
        conversations,
        searchResults,
        searchQuery
    ) { threads, results, query ->
        if (threads.isEmpty() && query.isBlank()) {
            ConversationsUiState.Empty
        } else {
            ConversationsUiState.Success(
                conversations = threads,
                searchResults = results,
                isSearching = query.isNotBlank()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConversationsUiState.Loading
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }
}
