package com.example.p2pchat.core.network

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.p2pchat.core.crypto.AttachmentCipher
import com.example.p2pchat.core.crypto.ratchet.RatchetManager
import com.example.p2pchat.core.network.webrtc.WebRtcTransport
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageProcessor @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val handshakeManager: HandshakeManager,
    private val ratchetManager: RatchetManager,
    private val messageRepository: MessageRepository,
    private val fileTransferManager: FileTransferManager,
    private val webRtcTransport: WebRtcTransport,
    private val attachmentCipher: AttachmentCipher,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        startExpirationCleaner()
    }

    private fun startExpirationCleaner() {
        scope.launch {
             while(isActive) {
                 kotlinx.coroutines.delay(30_000) // 30 seconds check
                 try {
                     messageRepository.deleteExpiredMessages(System.currentTimeMillis())
                 } catch (e: Exception) {
                     Log.e("MessageProcessor", "Expiration cleanup failed", e)
                 }
             }
        }
    }

    private data class PendingAttachmentInfo(
        val key: ByteArray,
        val iv: ByteArray,
        val messageId: String,
        val filename: String
    )

    private val pendingKeys = ConcurrentHashMap<String, PendingAttachmentInfo>()

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
                            // File received (Encrypted)!
                            val transferId = message.transferId

                            // Check if we have pending keys
                            val info = pendingKeys.remove(transferId)
                            if (info != null) {
                                // Decrypt immediately
                                val attachmentDir = File(context.filesDir, "attachments").apply { mkdirs() }
                                val destFile = File(attachmentDir, "${UUID.randomUUID()}.jpg")
                                attachmentCipher.decryptFile(completedFile, destFile, info.key, info.iv)
                                completedFile.delete()

                                // Save Attachment Entity
                                val attachmentEntity = AttachmentEntity(
                                    id = UUID.randomUUID().toString(),
                                    messageId = info.messageId,
                                    type = "image/jpeg",
                                    size = destFile.length(),
                                    filename = info.filename,
                                    uri = destFile.absolutePath
                                )
                                messageRepository.saveAttachment(attachmentEntity)

                                // Update Message content to show attachment is ready?
                                // Ideally yes, but tricky to update specific message content dynamically here without knowing logic.
                                // But at least the attachment is linked now.
                            } else {
                                // Save as pending encrypted file
                                val tempDir = File(context.filesDir, "temp_transfers").apply { mkdirs() }
                                val encFile = File(tempDir, "${transferId}.enc")
                                completedFile.copyTo(encFile, overwrite = true)
                                completedFile.delete()
                            }
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
                        try {
                            val msg = TransportMessage.fromBytes(decrypted)
                            when (msg) {
                                is TransportMessage.Chat -> {
                                    val content = String(msg.payload)
                                    val messageId = UUID.randomUUID().toString()
                                    var displayContent = msg.payload
                                    val now = System.currentTimeMillis()
                                    val expiresAt = if (msg.expiresInSeconds > 0) now + (msg.expiresInSeconds * 1000L) else null

                                    // Check for Attachment Pointer
                                    if (content.startsWith("ATTACHMENT_POINTER:")) {
                                        // content: ATTACHMENT_POINTER:transferId:filename:key:iv
                                        val parts = content.split(":")
                                        if (parts.size >= 5) {
                                            val transferId = parts[1]
                                            val filename = parts[2]
                                            val key = Base64.decode(parts[3], Base64.DEFAULT)
                                            val iv = Base64.decode(parts[4], Base64.DEFAULT)

                                            // Check for file
                                            val tempDir = File(context.filesDir, "temp_transfers")
                                            val encFile = File(tempDir, "${transferId}.enc")

                                            if (encFile.exists()) {
                                                // Decrypt
                                                val attachmentDir = File(context.filesDir, "attachments").apply { mkdirs() }
                                                val destFile = File(attachmentDir, "${UUID.randomUUID()}.jpg")
                                                try {
                                                    attachmentCipher.decryptFile(encFile, destFile, key, iv)
                                                    encFile.delete()

                                                    // Save Attachment Entity
                                                    val attachmentEntity = AttachmentEntity(
                                                        id = UUID.randomUUID().toString(),
                                                        messageId = messageId,
                                                        type = "image/jpeg",
                                                        size = destFile.length(),
                                                        filename = filename,
                                                        uri = destFile.absolutePath
                                                    )
                                                    messageRepository.saveAttachment(attachmentEntity)
                                                    displayContent = "[Image Attachment]".toByteArray()
                                                } catch (e: Exception) {
                                                    Log.e("MessageProcessor", "Decryption failed", e)
                                                    displayContent = "[Decryption Failed]".toByteArray()
                                                }
                                            } else {
                                                // File not arrived yet. Store keys.
                                                pendingKeys[transferId] = PendingAttachmentInfo(key, iv, messageId, filename)
                                                displayContent = "[Downloading Image...]".toByteArray()
                                            }
                                        }
                                    }

                                    messageRepository.saveMessage(
                                        MessageEntity(
                                            id = messageId,
                                            threadId = peerId,
                                            parentMessageId = null,
                                            senderId = peerId,
                                            encryptedContent = displayContent,
                                            iv = ByteArray(0),
                                            sentAt = now,
                                            deliveryState = "READ",
                                            deliveredAt = now,
                                            readAt = now,
                                            expiresAt = expiresAt
                                        )
                                    )
                                }
                                is TransportMessage.Signaling -> {
                                    webRtcTransport.onSignalingMessage(peerId, msg)
                                }
                                else -> {
                                    Log.w("MessageProcessor", "Unexpected message type inside encryption")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MessageProcessor", "Failed to parse decrypted message", e)
                        }
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
