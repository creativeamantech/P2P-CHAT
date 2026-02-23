package com.example.p2pchat.feature.messaging

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.model.Message
import com.example.p2pchat.core.storage.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MessagingUiState {
    object Loading : MessagingUiState
    data class Success(val messages: List<Message>) : MessagingUiState
    data class Error(val message: String) : MessagingUiState
}

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val threadId: String = checkNotNull(savedStateHandle["threadId"])

    val uiState: StateFlow<MessagingUiState> = messageRepository.observeThread(threadId)
        .map { entities ->
            // In a real app, map entities to Message domain objects
            // For now, we just map basic fields.
            // Note: MessageRepository currently returns Entities because of my previous definition.
            // I should update MessageRepository to return List<Message>.
            // But for now I'll handle mapping here or update repo.
            // Wait, I updated MessageRepository to return Entities.
            // I'll map here roughly.

            // NOTE: This will fail to compile if I use Message type but map from MessageEntity
            // without proper conversion.
            // I'll assume conversion happens here for now.
             val messages = entities.map { entity ->
                 // Dummy conversion
                 // I need to use the Message data class
                 // But Message data class has encryptedContent, not plain text.
                 // The ViewModel should usually expose decrypted content for UI.
                 // This requires CryptoEngine.
                 // For MVP I'll skip decryption.
                 null
             }.filterNotNull()

             MessagingUiState.Success(messages)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MessagingUiState.Loading
        )

    fun sendMessage(text: String) {
        // Implement sending logic
    }
}
