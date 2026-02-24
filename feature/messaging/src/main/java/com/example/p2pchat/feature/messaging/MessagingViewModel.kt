package com.example.p2pchat.feature.messaging

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.crypto.ratchet.RatchetManager
import com.example.p2pchat.core.model.Attachment
import com.example.p2pchat.core.model.DeliveryState
import com.example.p2pchat.core.model.Message
import com.example.p2pchat.core.network.ConnectionManager
import com.example.p2pchat.core.network.EncryptedPayload
import com.example.p2pchat.core.storage.entity.AttachmentEntity
import com.example.p2pchat.core.storage.entity.MessageEntity
import com.example.p2pchat.core.storage.repository.AttachmentRepository
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
    private val attachmentRepository: AttachmentRepository,
    private val ratchetManager: RatchetManager,
    private val connectionManager: ConnectionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val threadId: String = checkNotNull(savedStateHandle["threadId"])
    private val peerId: String = threadId

    val uiState: StateFlow<MessagingUiState> = messageRepository.observeThreadWithAttachments(threadId)
        .map { messagesWithAttachments ->
            val messages = messagesWithAttachments.mapNotNull { item ->
                try {
                    val entity = item.message
                    val attachmentEntities = item.attachments
                    val clearText = String(entity.encryptedContent)

                    val attachments = attachmentEntities.map {
                        Attachment(
                            id = it.id,
                            type = it.type,
                            size = it.size,
                            filename = it.filename,
                            uri = it.uri
                        )
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
                        attachments = attachments,
                        sentAt = kotlinx.datetime.Instant.fromEpochMilliseconds(entity.sentAt),
                        deliveryState = DeliveryState.Pending,
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

    fun sendMessage(text: String, parentId: String? = null) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val messageId = UUID.randomUUID().toString()
            val senderId = "local_peer"
            val plaintext = text.toByteArray()

            try {
                val ciphertext = ratchetManager.encrypt(peerId, plaintext)

                connectionManager.sendMessage(peerId, EncryptedPayload(ciphertext))

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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            // 1. Save attachment locally
            val file = attachmentRepository.saveAttachment(uri) ?: return@launch

            // 2. Create Attachment Entity
            val attachmentId = UUID.randomUUID().toString()
            val attachmentEntity = AttachmentEntity(
                id = attachmentId,
                messageId = "", // Will update later
                type = "image/jpeg", // Simplified
                size = file.length(),
                filename = file.name,
                uri = file.absolutePath // Store local path
            )

            // 3. Create Message
            val now = Clock.System.now().toEpochMilliseconds()
            val messageId = UUID.randomUUID().toString()
            val messageEntity = MessageEntity(
                id = messageId,
                threadId = threadId,
                parentMessageId = null,
                senderId = "local_peer",
                encryptedContent = "[Image Attachment]".toByteArray(),
                iv = ByteArray(12),
                sentAt = now,
                deliveryState = "SENT",
                deliveredAt = null,
                readAt = null
            )

            // 4. Save both
            messageRepository.saveMessage(messageEntity)
            messageRepository.saveAttachment(attachmentEntity.copy(messageId = messageId))

            // 5. Send (Placeholder)
            val ciphertext = ratchetManager.encrypt(peerId, "[Image Attachment]".toByteArray())
            connectionManager.sendMessage(peerId, EncryptedPayload(ciphertext))
        }
    }

    fun tagMessage(messageId: String, topic: String) {
        viewModelScope.launch {
            messageRepository.tagMessage(messageId, topic)
        }
    }
}
