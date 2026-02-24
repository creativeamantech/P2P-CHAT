package com.example.p2pchat.feature.conversations.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.model.Message
import com.example.p2pchat.core.storage.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<Message>) : SearchUiState
    object Empty : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .filter { it.isNotBlank() }
                .collectLatest { query ->
                    _uiState.value = SearchUiState.Loading
                    messageRepository.searchMessages(query)
                        .map { entities ->
                            entities.map { entity ->
                                // Simplified mapping - assuming encryptedContent holds display text for MVP FTS
                                Message(
                                    id = entity.id,
                                    threadId = entity.threadId,
                                    parentMessageId = entity.parentMessageId,
                                    senderId = entity.senderId,
                                    encryptedContent = entity.encryptedContent,
                                    iv = entity.iv,
                                    clearTextCache = String(entity.encryptedContent),
                                    topics = emptySet(),
                                    attachments = emptyList(),
                                    sentAt = kotlinx.datetime.Instant.fromEpochMilliseconds(entity.sentAt),
                                    deliveryState = com.example.p2pchat.core.model.DeliveryState.Pending,
                                    reactions = emptyMap()
                                )
                            }
                        }
                        .collect { messages ->
                            _uiState.value = if (messages.isEmpty()) SearchUiState.Empty else SearchUiState.Success(messages)
                        }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.value = SearchUiState.Idle
        }
    }
}
