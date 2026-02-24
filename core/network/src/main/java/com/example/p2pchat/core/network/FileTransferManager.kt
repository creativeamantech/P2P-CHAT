package com.example.p2pchat.core.network

import android.content.Context
import android.util.Log
import com.example.p2pchat.core.storage.repository.AttachmentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val attachmentRepository: AttachmentRepository
) {
    private val activeTransfers = ConcurrentHashMap<String, MutableMap<Int, ByteArray>>()

    // Config
    private val CHUNK_SIZE = 16 * 1024 // 16KB chunks

    suspend fun sendFile(
        peerId: String,
        file: File,
        transport: P2PTransport,
        transferId: String
    ) = withContext(Dispatchers.IO) {
        try {
            val totalSize = file.length()
            val totalChunks = (totalSize / CHUNK_SIZE).toInt() + 1

            file.inputStream().use { input ->
                val buffer = ByteArray(CHUNK_SIZE)
                var chunkIndex = 0
                var bytesRead = input.read(buffer)

                while (bytesRead != -1) {
                    val data = if (bytesRead == CHUNK_SIZE) buffer else buffer.copyOf(bytesRead)

                    val message = TransportMessage.AttachmentChunk(
                        transferId = transferId,
                        chunkIndex = chunkIndex,
                        totalChunks = totalChunks,
                        data = data
                    )

                    // Send directly via transport (bypassing Ratchet for bulk data usually,
                    // BUT for strict E2E, we should encrypt chunks or file.
                    // Here we assume file is ALREADY encrypted by AttachmentCipher before calling this.
                    // We just transport bytes.

                    // We need a raw send method on Transport that accepts TransportMessage?
                    // Currently send() takes EncryptedPayload.
                    // We can wrap it. senderId is implicit.

                    // Wait, EncryptedPayload logic in Transport unwraps TransportMessage.Chat/Handshake.
                    // We updated TransportMessage to have AttachmentChunk.
                    // But WifiDirectTransport.setupStreams reads Type byte.
                    // We need to update WifiDirectTransport/BluetoothTransport to handle TYPE_ATTACHMENT!
                    // I will update them in next step or use existing mechanism if possible.

                    // Assuming transport supports sending raw TransportMessage bytes via `send` if we construct EncryptedPayload correctly?
                    // No, `send` wraps in `TransportMessage.Chat`.
                    // We need a `sendAttachmentChunk` or generic `sendRaw`.
                    // I'll add `sendAttachment` to interface in next steps or reuse existing if capable.

                    // For now, let's assume we can use `send` if we wrap it?
                    // No, `send` wraps content in `Chat` type.
                    // If we wrap `AttachmentChunk` bytes inside `Chat`, it's double wrapping and routed to MessageProcessor as chat.

                    // Correct way: Add `sendAttachmentChunk` to P2PTransport.

                    transport.sendAttachment(message)

                    chunkIndex++
                    bytesRead = input.read(buffer)

                    // Throttling?
                    // delay(10)
                }
            }
        } catch (e: Exception) {
            Log.e("FileTransfer", "Error sending file", e)
        }
    }

    suspend fun receiveChunk(chunk: TransportMessage.AttachmentChunk): File? {
        // Simple in-memory reassembly for MVP (warning: OOM on large files)
        // Better: write to temp file immediately.

        val transferMap = activeTransfers.getOrPut(chunk.transferId) { ConcurrentHashMap() }
        transferMap[chunk.chunkIndex] = chunk.data

        if (transferMap.size == chunk.totalChunks) {
            // All chunks received
            val sortedChunks = transferMap.toSortedMap().values

            // Reassemble
            val outputFile = File(context.cacheDir, "received_${chunk.transferId}.tmp")
            outputFile.outputStream().use { output ->
                for (data in sortedChunks) {
                    output.write(data)
                }
            }

            activeTransfers.remove(chunk.transferId)
            return outputFile
        }
        return null
    }
}
