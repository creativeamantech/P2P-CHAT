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
import android.util.Base64
import com.example.p2pchat.core.crypto.AttachmentCipher
import com.example.p2pchat.core.network.FileTransferManager
import com.example.p2pchat.core.network.TransportMessage
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.p2pchat.core.storage.entity.AttachmentEntity
import com.example.p2pchat.core.storage.entity.MessageEntity
import com.example.p2pchat.core.storage.repository.AttachmentRepository
import com.example.p2pchat.core.storage.repository.MessageRepository
import com.example.p2pchat.core.storage.repository.ThreadRepository
import com.example.p2pchat.core.storage.relation.MessageWithAttachments
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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
    data class Success(val messages: Flow<PagingData<Message>>) : MessagingUiState
    data class Error(val message: String) : MessagingUiState
}

@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository,
    private val attachmentRepository: AttachmentRepository,
    private val ratchetManager: RatchetManager,
    private val connectionManager: ConnectionManager,
    private val fileTransferManager: FileTransferManager,
    private val attachmentCipher: AttachmentCipher,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val threadId: String = checkNotNull(savedStateHandle["threadId"])
    private val peerId: String = threadId

    val threadInfo = threadRepository.observeThreadEntity(threadId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<MessagingUiState> = kotlinx.coroutines.flow.flow {
        try {
            val pagingData = messageRepository.observeThread(threadId)
                .map { pagingData ->
                    pagingData.map { relation ->
                        val entity = relation.message
                        val attachments = relation.attachments.map { att ->
                            Attachment(
                                id = att.id,
                                type = att.type,
                                url = att.uri,
                                size = att.size,
                                filename = att.filename
                            )
                        }

                        val clearText = try {
                            String(entity.encryptedContent)
                        } catch (e: Exception) {
                            "[Error]"
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
                    }
                }
                .cachedIn(viewModelScope)

            emit(MessagingUiState.Success(pagingData))
        } catch (e: Exception) {
            emit(MessagingUiState.Error(e.message ?: "Unknown Error"))
        }
    }.stateIn(
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

            // Get Thread Expiration
            val thread = threadRepository.getThreadEntity(threadId)
            val expiresIn = thread?.defaultExpiration ?: 0
            val expiresAt = if (expiresIn > 0) now + (expiresIn * 1000) else null

            try {
                // Wrap in TransportMessage.Chat before encryption
                val chatMessage = TransportMessage.Chat(plaintext, expiresIn)
                val chatBytes = chatMessage.toBytes()

                val ciphertext = ratchetManager.encrypt(peerId, chatBytes)

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
                    readAt = null,
                    expiresAt = expiresAt
                )
                messageRepository.saveMessage(messageEntity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            // 1. Save attachment locally (original plaintext)
            val file = attachmentRepository.saveAttachment(uri) ?: return@launch

            // 2. Create Attachment Entity (for local display)
            val attachmentId = UUID.randomUUID().toString()
            val attachmentEntity = AttachmentEntity(
                id = attachmentId,
                messageId = "", // Will update later
                type = "image/jpeg", // Simplified
                size = file.length(),
                filename = file.name,
                uri = file.absolutePath
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

            // 5. Encrypt file for transfer
            val encryptedFile = java.io.File(file.parentFile, "${file.name}.enc")
            val encryptionResult = attachmentCipher.encryptFile(file, encryptedFile)
            val keyBase64 = Base64.encodeToString(encryptionResult.key, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(encryptionResult.iv, Base64.NO_WRAP)

            // 6. Send Transfer (Encrypted File)
            val transferId = UUID.randomUUID().toString()
            val transport = connectionManager.getActiveTransport(peerId)

            if (transport != null) {
                fileTransferManager.sendFile(peerId, encryptedFile, transport, transferId)

                // 7. Send Metadata Message (with Key/IV)
                // Format: ATTACHMENT_POINTER:transferId:filename:key:iv
                val meta = "ATTACHMENT_POINTER:$transferId:${file.name}:$keyBase64:$ivBase64"

                // Wrap metadata in Chat message (treated as text command)
                val metaMessage = TransportMessage.Chat(meta.toByteArray())
                val metaBytes = metaMessage.toBytes()

                val ciphertext = ratchetManager.encrypt(peerId, metaBytes)
                connectionManager.sendMessage(peerId, EncryptedPayload(ciphertext))

                // Cleanup temp encrypted file? Maybe keep for retry?
                // encryptedFile.delete() // Don't delete immediately if async send
            }
        }
    }

    fun tagMessage(messageId: String, topic: String) {
        viewModelScope.launch {
            messageRepository.tagMessage(messageId, topic)
        }
    }

    fun setDisappearingTimer(seconds: Int) {
        viewModelScope.launch {
            threadRepository.updateThreadExpiration(threadId, if (seconds > 0) seconds else null)
        }
    }
}
