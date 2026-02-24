package com.example.p2pchat.core.crypto

import com.example.p2pchat.core.model.UserIdentity

interface CryptoManager {
    suspend fun getMyIdentity(): UserIdentity?
    suspend fun generateNewIdentity(displayName: String): UserIdentity

    // Encrypt/Decrypt
    fun encrypt(plaintext: ByteArray, recipientPublicKey: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray, senderPublicKey: ByteArray): ByteArray

    // Key Agreement
    fun calculateSharedSecret(remotePublicKey: ByteArray): ByteArray

    // Database Security
    fun getDatabasePassphrase(): ByteArray
}
