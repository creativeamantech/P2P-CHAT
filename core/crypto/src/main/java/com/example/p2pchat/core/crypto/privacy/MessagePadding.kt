package com.example.p2pchat.core.crypto.privacy

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagePadding @Inject constructor() {

    private val BLOCK_SIZE = 512

    fun pad(plaintext: ByteArray): ByteArray {
        val paddedLen = ((plaintext.size / BLOCK_SIZE) + 1) * BLOCK_SIZE
        val padded = ByteArray(paddedLen)

        // Copy plaintext
        plaintext.copyInto(padded)

        // Fill padding with zeros (implicit in new ByteArray)
        // Store length in last 4 bytes of the PADDED buffer
        val len = plaintext.size
        padded[paddedLen - 4] = (len shr 24).toByte()
        padded[paddedLen - 3] = (len shr 16).toByte()
        padded[paddedLen - 2] = (len shr 8).toByte()
        padded[paddedLen - 1] = len.toByte()

        return padded
    }

    fun unpad(padded: ByteArray): ByteArray {
        if (padded.size < 4) return padded

        val len = ((padded[padded.size - 4].toInt() and 0xFF) shl 24) or
                  ((padded[padded.size - 3].toInt() and 0xFF) shl 16) or
                  ((padded[padded.size - 2].toInt() and 0xFF) shl 8) or
                  (padded[padded.size - 1].toInt() and 0xFF)

        if (len > padded.size || len < 0) {
            return padded // Fallback or throw?
        }

        return padded.copyOf(len)
    }
}
