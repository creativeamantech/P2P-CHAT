package com.example.p2pchat.core.crypto.privacy

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagePadding @Inject constructor() {
    companion object {
        private const val BLOCK_SIZE = 512
    }

    private val random = SecureRandom()

    fun pad(plaintext: ByteArray): ByteArray {
        val targetSize = ((plaintext.size / BLOCK_SIZE) + 1) * BLOCK_SIZE
        // We need at least 2 bytes for the length marker
        val padded = ByteArray(targetSize)

        // Copy plaintext
        System.arraycopy(plaintext, 0, padded, 0, plaintext.size)

        // Fill padding with random bytes (or zeros if simpler, but random is better for entropy)
        // Wait, standard PKCS7 padding fills with value equal to padding length.
        // Here we store length explicitly at the end as per spec in Section 23.5.
        // "Store real length in last 2 bytes"

        val paddingLength = targetSize - plaintext.size
        // Fill the gap with random noise to maximize entropy
        val noise = ByteArray(paddingLength - 2) // -2 for length bytes
        random.nextBytes(noise)
        System.arraycopy(noise, 0, padded, plaintext.size, noise.size)

        // Write length at the end (Big Endian)
        padded[targetSize - 2] = (plaintext.size shr 8).toByte()
        padded[targetSize - 1] = (plaintext.size and 0xFF).toByte()

        return padded
    }

    fun unpad(padded: ByteArray): ByteArray {
        if (padded.size < 2) return padded // Should not happen if protocol followed

        val realSize = ((padded[padded.size - 2].toInt() and 0xFF) shl 8) or
                        (padded[padded.size - 1].toInt() and 0xFF)

        if (realSize < 0 || realSize > padded.size - 2) {
            // Invalid padding length, maybe not padded? Return as is or throw?
            // For resilience, return as is or empty?
            // If we enforce padding, this is an attack or error.
            return padded
        }

        return padded.copyOfRange(0, realSize)
    }
}
