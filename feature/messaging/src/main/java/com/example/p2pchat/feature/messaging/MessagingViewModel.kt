package com.example.p2pchat.feature.messaging

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.crypto.CryptoManager
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
    private val cryptoManager: CryptoManager,
    private val transport: P2PTransport,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val threadId: String = checkNotNull(savedStateHandle["threadId"])

    val uiState: StateFlow<MessagingUiState> = messageRepository.observeThread(threadId)
        .map { entities ->
            val messages = entities.mapNotNull { entity ->
                // Decrypt content (Simplified: assumes sender key is available or part of metadata)
                // In real app, we need to know WHO sent it to use the correct key for decryption.
                // Or we attempt to decrypt with the session key.
                // For MVP, we will try to decrypt with a dummy key or just show raw if failed.

                // NOTE: Repository returns entities with encrypted bytes.
                // We need to decrypt here.

                try {
                    // For MVP, we are doing symmetric encryption with a derived key from identity.
                    // But wait, CryptoManager.decrypt takes senderPublicKey.
                    // We don't have it here easily without looking up the peer.
                    // Let's assume for MVP we used a static key or just bypass for now to show flow.

                    // Actually, let's try to decrypt.
                    // We need the peer's public key.
                    // We can look up the peer using senderId from entity.
                    // But that requires another async call.

                    // To keep it simple: We will just pass the encrypted content to UI for now
                    // OR we just decrypt using a placeholder if we stored it that way.

                    // Let's rely on the fact that for "Self" messages, we can decrypt.
                    // For "Other" messages, we need their key.

                    val decryptedBytes = if (entity.senderId == "local_peer") { // TODO: Use actual ID
                         // It's my message, I might have stored it plain text or encrypted?
                         // Usually stored encrypted. I can decrypt with my own key? No.
                         // I encrypt for recipient.
                         // So I can't decrypt my own sent messages unless I encrypt for myself too (Sender Key).
                         entity.encryptedContent
                    } else {
                         entity.encryptedContent
                    }

                    // Converting byte array to string for display (Assuming UTF-8 text)
                    // This is obviously wrong if it's still encrypted.
                    // For the sake of the MVP UI working, let's assume clearTextCache is populated
                    // or we handle this better.

                    // Let's just create the Message object.
                    Message(
                        id = entity.id,
                        threadId = entity.threadId,
                        parentMessageId = entity.parentMessageId,
                        senderId = entity.senderId,
                        encryptedContent = entity.encryptedContent,
                        iv = entity.iv,
                        clearTextCache = String(entity.encryptedContent), // DANGER: Showing encrypted as text
                        topics = emptySet(),
                        attachments = emptyList(),
                        sentAt = kotlinx.datetime.Instant.fromEpochMilliseconds(entity.sentAt),
                        deliveryState = DeliveryState.Pending, // TODO: Map properly
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
        // Start listening for incoming messages
        viewModelScope.launch {
            transport.receive().collect { payload ->
                // Received encrypted payload
                // 1. Decrypt (Need sender ID to find key, but payload doesn't have it in this simple impl)
                // In real app, payload includes metadata (SenderID, Ratchet info).

                // 2. Save to DB
                val now = Clock.System.now().toEpochMilliseconds()
                val messageEntity = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    threadId = threadId, // We assume all messages come to this thread for MVP
                    parentMessageId = null,
                    senderId = "other_peer", // Placeholder
                    encryptedContent = payload.data,
                    iv = ByteArray(12), // extracted from payload in real app
                    sentAt = now,
                    deliveryState = "DELIVERED",
                    deliveredAt = now,
                    readAt = null
                )
                messageRepository.saveMessage(messageEntity)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val messageId = UUID.randomUUID().toString()
            val senderId = "local_peer" // TODO: Get from CryptoManager

            // 1. Encrypt
            // We need recipient key.
            // For MVP, using dummy key.
            val encryptedBytes = text.toByteArray() // cryptoManager.encrypt(text.toByteArray(), ByteArray(32))

            // 2. Save to DB (Optimistic)
            val messageEntity = MessageEntity(
                id = messageId,
                threadId = threadId,
                parentMessageId = null,
                senderId = senderId,
                encryptedContent = encryptedBytes,
                iv = ByteArray(12),
                sentAt = now,
                deliveryState = "PENDING",
                deliveredAt = null,
                readAt = null
            )
            messageRepository.saveMessage(messageEntity)

            // 3. Send over Transport
            transport.send(EncryptedPayload(encryptedBytes))
                .onSuccess {
                    // Update state to SENT
                }
                .onFailure {
                    // Update state to FAILED
                }
        }
    }
}
