package com.example.p2pchat.core.network

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.p2pchat.core.crypto.ratchet.RatchetManager
import com.example.p2pchat.core.storage.entity.AttachmentEntity
import com.example.p2pchat.core.storage.entity.MessageEntity
import com.example.p2pchat.core.storage.repository.MessageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageProcessor @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val handshakeManager: HandshakeManager,
    private val ratchetManager: RatchetManager,
    private val messageRepository: MessageRepository,
    private val fileTransferManager: FileTransferManager,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        connectionManager.incomingMessages.onEach { payload ->
            processMessage(payload)
        }.launchIn(scope)
    }

    private suspend fun processMessage(payload: EncryptedPayload) {
        try {
            if (payload.isHandshake) {
                try {
                    val message = TransportMessage.fromBytes(payload.data)
                    if (message is TransportMessage.Handshake) {
                        val peerId = payload.senderId ?: Base64.encodeToString(message.identityKey, Base64.NO_WRAP)
                        handshakeManager.onHandshakeReceived(peerId, message)
                    }
                } catch (e: Exception) {
                    Log.e("MessageProcessor", "Failed to parse handshake", e)
                }
            } else if (payload.isAttachment) {
                try {
                    val message = TransportMessage.fromBytes(payload.data)
                    if (message is TransportMessage.AttachmentChunk) {
                        val completedFile = fileTransferManager.receiveChunk(message)
                        if (completedFile != null) {
                            // File received!
                            // Move to attachments dir
                            val attachmentDir = File(context.filesDir, "attachments").apply { mkdirs() }
                            val destFile = File(attachmentDir, "${UUID.randomUUID()}.jpg") // Assume jpg for now
                            completedFile.copyTo(destFile, overwrite = true)
                            completedFile.delete()

                            // We need to link this to a message.
                            // Currently the chunk doesn't have messageID.
                            // We expect a text message with "ATTACHMENT_POINTER:transferId" to arrive via Ratchet.
                            // OR we save it as "Orphaned" and link when message arrives.

                            // For MVP simplicity: Just log it.
                            Log.d("MessageProcessor", "File received: ${destFile.absolutePath}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MessageProcessor", "Failed to parse attachment", e)
                }
            } else {
                // Chat message
                val peerId = payload.senderId
                if (peerId != null) {
                    val decrypted = ratchetManager.decrypt(peerId, payload.data)
                    if (decrypted != null) {
                        val content = String(decrypted)

                        // Check for Attachment Pointer
                        // Format: "ATTACHMENT_POINTER:transferId:filename"
                        if (content.startsWith("ATTACHMENT_POINTER:")) {
                            // Handle logic to link previously received file
                            // For MVP, just save as text for now
                        }

                        messageRepository.saveMessage(
                            MessageEntity(
                                id = UUID.randomUUID().toString(),
                                threadId = peerId,
                                parentMessageId = null,
                                senderId = peerId,
                                encryptedContent = decrypted,
                                iv = ByteArray(0),
                                sentAt = System.currentTimeMillis(),
                                deliveryState = "READ",
                                deliveredAt = System.currentTimeMillis(),
                                readAt = System.currentTimeMillis()
                            )
                        )
                    }
                } else {
                    Log.w("MessageProcessor", "Received chat message without senderId")
                }
            }
        } catch (e: Exception) {
            Log.e("MessageProcessor", "Error processing message", e)
        }
    }
}
