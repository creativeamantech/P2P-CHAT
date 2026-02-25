package com.example.p2pchat.core.crypto

import java.security.MessageDigest
import javax.inject.Inject

class FingerprintGenerator @Inject constructor() {

    fun generateSafetyNumber(localKey: ByteArray, remoteKey: ByteArray): String {
        // Concatenate keys in sorted order to ensure consistency regardless of who initiates
        val (key1, key2) = if (compareKeys(localKey, remoteKey) < 0) {
            localKey to remoteKey
        } else {
            remoteKey to localKey
        }

        val combined = key1 + key2
        val hash = MessageDigest.getInstance("SHA-512").digest(combined)

        // Doc says: "Format as 12 groups of 5 digits".
        // Using 30 bytes? No, 30 bytes / 5 bytes-per-chunk = 6 chunks.
        // We need 60 bytes to get 12 chunks.
        // Or if doc implies 30 bytes total, then chunked(2.5)? No.
        // Let's stick to 60 bytes logic for 12 groups of 5 digits.
        // If hash is 64 bytes (SHA-512), we have enough.

        val bytesToUse = hash.take(60).toByteArray()

        return bytesToUse.toList()
            .chunked(5)
            .map { bytes ->
                var acc = 0L
                for (b in bytes) {
                    acc = (acc shl 8) or (b.toInt() and 0xFF).toLong()
                }
                val digits = acc % 100000L
                digits.toString().padStart(5, '0')
            }
            .joinToString(" ")
    }

    private fun compareKeys(a: ByteArray, b: ByteArray): Int {
        if (a.size != b.size) return a.size - b.size
        for (i in a.indices) {
            if (a[i] != b[i]) {
                return (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            }
        }
        return 0
    }
}
