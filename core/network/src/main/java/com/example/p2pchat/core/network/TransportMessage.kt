package com.example.p2pchat.core.network

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

sealed class TransportMessage {
    abstract fun toBytes(): ByteArray

    data class Handshake(
        val identityKey: ByteArray, // Ed25519 Public Key
        val exchangeKey: ByteArray, // X25519 Identity Key
        val ephemeralKey: ByteArray // X25519 Ephemeral Key
    ) : TransportMessage() {
        override fun toBytes(): ByteArray {
            val bos = ByteArrayOutputStream()
            val dos = DataOutputStream(bos)
            dos.writeByte(TYPE_HANDSHAKE)

            dos.writeInt(identityKey.size)
            dos.write(identityKey)

            dos.writeInt(exchangeKey.size)
            dos.write(exchangeKey)

            dos.writeInt(ephemeralKey.size)
            dos.write(ephemeralKey)

            dos.flush()
            return bos.toByteArray()
        }
    }

    data class Chat(
        val payload: ByteArray, // Encrypted Ratchet Message
        val expiresInSeconds: Int = 0 // 0 means no expiration
    ) : TransportMessage() {
        override fun toBytes(): ByteArray {
            val bos = ByteArrayOutputStream()
            val dos = DataOutputStream(bos)
            dos.writeByte(TYPE_CHAT)

            dos.writeInt(payload.size)
            dos.write(payload)
            dos.writeInt(expiresInSeconds) // Added field

            dos.flush()
            return bos.toByteArray()
        }
    }

    data class AttachmentChunk(
        val transferId: String,
        val chunkIndex: Int,
        val totalChunks: Int,
        val data: ByteArray
    ) : TransportMessage() {
        override fun toBytes(): ByteArray {
            val bos = ByteArrayOutputStream()
            val dos = DataOutputStream(bos)
            dos.writeByte(TYPE_ATTACHMENT)

            dos.writeUTF(transferId)
            dos.writeInt(chunkIndex)
            dos.writeInt(totalChunks)
            dos.writeInt(data.size)
            dos.write(data)

            dos.flush()
            return bos.toByteArray()
        }
    }

    data class Signaling(
        val type: String, // "OFFER", "ANSWER", "ICE"
        val payload: String // SDP or ICE candidate JSON
    ) : TransportMessage() {
        override fun toBytes(): ByteArray {
            val bos = ByteArrayOutputStream()
            val dos = DataOutputStream(bos)
            dos.writeByte(TYPE_SIGNALING)
            dos.writeUTF(type)
            dos.writeUTF(payload)
            dos.flush()
            return bos.toByteArray()
        }
    }

    companion object {
        private const val TYPE_HANDSHAKE = 1
        private const val TYPE_CHAT = 2
        const val TYPE_ATTACHMENT = 3
        private const val TYPE_SIGNALING = 4

        fun fromBytes(bytes: ByteArray): TransportMessage {
            val bis = ByteArrayInputStream(bytes)
            val dis = DataInputStream(bis)
            val type = dis.readByte().toInt()

            return when (type) {
                TYPE_HANDSHAKE -> {
                    val idLen = dis.readInt()
                    val idKey = ByteArray(idLen)
                    dis.readFully(idKey)

                    val exLen = dis.readInt()
                    val exKey = ByteArray(exLen)
                    dis.readFully(exKey)

                    val ephLen = dis.readInt()
                    val ephKey = ByteArray(ephLen)
                    dis.readFully(ephKey)

                    Handshake(idKey, exKey, ephKey)
                }
                TYPE_CHAT -> {
                    val len = dis.readInt()
                    val payload = ByteArray(len)
                    dis.readFully(payload)

                    val expiresIn = try {
                        if (dis.available() > 0) dis.readInt() else 0
                    } catch (e: Exception) { 0 }

                    Chat(payload, expiresIn)
                }
                TYPE_ATTACHMENT -> {
                    val transferId = dis.readUTF()
                    val index = dis.readInt()
                    val total = dis.readInt()
                    val len = dis.readInt()
                    val data = ByteArray(len)
                    dis.readFully(data)
                    AttachmentChunk(transferId, index, total, data)
                }
                TYPE_SIGNALING -> {
                    val sigType = dis.readUTF()
                    val payload = dis.readUTF()
                    Signaling(sigType, payload)
                }
                else -> throw IllegalArgumentException("Unknown message type: $type")
            }
        }
    }
}
