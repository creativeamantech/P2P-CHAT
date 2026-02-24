package com.example.p2pchat.feature.messaging

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.crypto.ratchet.RatchetManager
import com.example.p2pchat.core.model.DeliveryState
import com.example.p2pchat.core.model.Message
import com.example.p2pchat.core.network.EncryptedPayload
import com.example.p2pchat.core.network.P2PTransport
import com.example.p2pchat.core.storage.entity.MessageEntity
import com.example.p2pchat.core.storage.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.UUID
import javax.inject.Inject

sealed interface MessagingUiState {
    object Loading : MessagingUiState
    data class Success(val messages: List<Message>) : MessagingUiState
    data class Error(val message: String) : MessagingUiState
}

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val ratchetManager: RatchetManager,
    private val transport: P2PTransport,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val threadId: String = checkNotNull(savedStateHandle["threadId"])
    private val peerId: String = threadId // Assuming threadId == peerId for 1:1 chat MVP

    val uiState: StateFlow<MessagingUiState> = messageRepository.observeThread(threadId)
        .map { entities ->
            val messages = entities.mapNotNull { entity ->
                try {
                    val clearText = if (entity.senderId == "local_peer") {
                        String(entity.encryptedContent)
                    } else {
                        String(entity.encryptedContent)
                    }

                    Message(
                        id = entity.id,
                        threadId = entity.threadId,
                        parentMessageId = entity.parentMessageId,
                        senderId = entity.senderId,
                        encryptedContent = entity.encryptedContent,
                        iv = entity.iv,
                        clearTextCache = clearText,
                        topics = emptySet(), // TODO: Fetch topics
                        attachments = emptyList(),
                        sentAt = kotlinx.datetime.Instant.fromEpochMilliseconds(entity.sentAt),
                        deliveryState = DeliveryState.Pending, // TODO
                        reactions = emptyMap()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            MessagingUiState.Success(messages)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MessagingUiState.Loading
        )

    init {
        viewModelScope.launch {
            transport.receive().collect { payload ->
                try {
                    val plaintext = ratchetManager.decrypt(peerId, payload.data)
                    val now = Clock.System.now().toEpochMilliseconds()
                    val messageEntity = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        threadId = threadId,
                        parentMessageId = null,
                        senderId = peerId,
                        encryptedContent = plaintext,
                        iv = ByteArray(12),
                        sentAt = now,
                        deliveryState = "DELIVERED",
                        deliveredAt = now,
                        readAt = null
                    )
                    messageRepository.saveMessage(messageEntity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun sendMessage(text: String, parentId: String? = null) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val messageId = UUID.randomUUID().toString()
            val senderId = "local_peer"
            val plaintext = text.toByteArray()

            try {
                val ciphertext = ratchetManager.encrypt(peerId, plaintext)

                transport.send(EncryptedPayload(ciphertext))
                    .onSuccess {
                        val messageEntity = MessageEntity(
                            id = messageId,
                            threadId = threadId,
                            parentMessageId = parentId,
                            senderId = senderId,
                            encryptedContent = plaintext,
                            iv = ByteArray(12),
                            sentAt = now,
                            deliveryState = "SENT",
                            deliveredAt = null,
                            readAt = null
                        )
                        messageRepository.saveMessage(messageEntity)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun tagMessage(messageId: String, topic: String) {
        viewModelScope.launch {
            messageRepository.tagMessage(messageId, topic)
        }
    }
}
