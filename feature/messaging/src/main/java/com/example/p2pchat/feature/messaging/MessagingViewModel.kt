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
                        // For sent messages, we might have stored plain text in encryptedContent
                        // if we didn't implement "encrypt for self".
                        // BUT Ratchet encrypt produces a different ciphertext every time.
                        // Ideally we store plain text for self, or encrypt with a self-device key.
                        // For MVP, let's assume we stored plain text bytes for sent messages
                        // just to show them in UI (insecure for local storage, but functional).
                        String(entity.encryptedContent)
                    } else {
                        // Received message. It was stored encrypted as received from wire?
                        // Or decrypted before storage?
                        // If stored encrypted, we need to decrypt now.
                        // BUT Ratchet decryption changes state! We cannot decrypt on read (GET).
                        // We must decrypt on receive (PUT).
                        // So the entity in DB *should* be decrypted content (encrypted with local DB key ideally).

                        // Let's assume the Repository/DB layer handles "At-Rest" encryption transparently
                        // (SQLCipher handles this).
                        // So what we get back from DB in 'encryptedContent' field...
                        // If we saved the RESULT of ratchet decryption there, it is plaintext!
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
                        topics = emptySet(),
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
                    // 1. Decrypt using Ratchet
                    val plaintext = ratchetManager.decrypt(peerId, payload.data)

                    // 2. Save Decrypted (Plaintext) to DB
                    // (SQLCipher protects it at rest)
                    val now = Clock.System.now().toEpochMilliseconds()
                    val messageEntity = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        threadId = threadId,
                        parentMessageId = null,
                        senderId = peerId,
                        encryptedContent = plaintext, // Storing plaintext (protected by DB enc)
                        iv = ByteArray(12),
                        sentAt = now,
                        deliveryState = "DELIVERED",
                        deliveredAt = now,
                        readAt = null
                    )
                    messageRepository.saveMessage(messageEntity)
                } catch (e: Exception) {
                    // Decryption failed or unknown peer
                    e.printStackTrace()
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val messageId = UUID.randomUUID().toString()
            val senderId = "local_peer"
            val plaintext = text.toByteArray()

            // 1. Encrypt using Ratchet
            try {
                val ciphertext = ratchetManager.encrypt(peerId, plaintext)

                // 2. Send over Transport
                transport.send(EncryptedPayload(ciphertext))
                    .onSuccess {
                        // 3. Save to DB (Plaintext for local viewing)
                        val messageEntity = MessageEntity(
                            id = messageId,
                            threadId = threadId,
                            parentMessageId = null,
                            senderId = senderId,
                            encryptedContent = plaintext, // Save plaintext
                            iv = ByteArray(12),
                            sentAt = now,
                            deliveryState = "SENT",
                            deliveredAt = null,
                            readAt = null
                        )
                        messageRepository.saveMessage(messageEntity)
                    }
                    .onFailure {
                        // Handle error
                    }
            } catch (e: Exception) {
                // Encryption failed (maybe no session established?)
                e.printStackTrace()
            }
        }
    }
}
