package com.example.p2pchat.core.network

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

sealed class TransportMessage {
    abstract fun toBytes(): ByteArray

    data class Handshake(
        val identityKey: ByteArray, // Ed25519 Public Key
        val ephemeralKey: ByteArray // X25519 Public Key (Initial Ratchet Key)
    ) : TransportMessage() {
        override fun toBytes(): ByteArray {
            val bos = ByteArrayOutputStream()
            val dos = DataOutputStream(bos)
            dos.writeByte(TYPE_HANDSHAKE)

            dos.writeInt(identityKey.size)
            dos.write(identityKey)

            dos.writeInt(ephemeralKey.size)
            dos.write(ephemeralKey)

            dos.flush()
            return bos.toByteArray()
        }
    }

    data class Chat(
        val payload: ByteArray // Encrypted Ratchet Message
    ) : TransportMessage() {
        override fun toBytes(): ByteArray {
            val bos = ByteArrayOutputStream()
            val dos = DataOutputStream(bos)
            dos.writeByte(TYPE_CHAT)

            dos.writeInt(payload.size)
            dos.write(payload)

            dos.flush()
            return bos.toByteArray()
        }
    }

    companion object {
        private const val TYPE_HANDSHAKE = 1
        private const val TYPE_CHAT = 2

        fun fromBytes(bytes: ByteArray): TransportMessage {
            val bis = ByteArrayInputStream(bytes)
            val dis = DataInputStream(bis)
            val type = dis.readByte().toInt()

            return when (type) {
                TYPE_HANDSHAKE -> {
                    val idLen = dis.readInt()
                    val idKey = ByteArray(idLen)
                    dis.readFully(idKey)

                    val ephLen = dis.readInt()
                    val ephKey = ByteArray(ephLen)
                    dis.readFully(ephKey)

                    Handshake(idKey, ephKey)
                }
                TYPE_CHAT -> {
                    val len = dis.readInt()
                    val payload = ByteArray(len)
                    dis.readFully(payload)
                    Chat(payload)
                }
                else -> throw IllegalArgumentException("Unknown message type: ")
            }
        }
    }
}
