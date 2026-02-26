package com.example.p2pchat.core.crypto.group

import com.example.p2pchat.core.crypto.ratchet.HKDF
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import java.security.SecureRandom

@Singleton
class GroupCipher @Inject constructor(
    private val senderKeyManager: SenderKeyManager
) {
    // Encrypt message using MY sender key
    suspend fun encrypt(groupId: String, myPeerId: String, plaintext: ByteArray): ByteArray {
        // 1. Get or Create my sender key
        var key = senderKeyManager.getSenderKey(groupId, myPeerId)
        if (key == null) {
            key = senderKeyManager.createMySenderKey(groupId, myPeerId)
        }

        // 2. Ratchet Step: ChainKey -> (NextChainKey, MessageKey)
        val (nextChainKey, messageKey) = HKDF.chainStep(key.chainKey)

        // 3. Encrypt with Message Key
        val (ciphertext, iv) = aesEncrypt(messageKey, plaintext)

        // 4. Update State with Next Chain Key
        senderKeyManager.updateSenderKey(key.copy(chainKey = nextChainKey))

        // 5. Return Payload (IV + Ciphertext)
        return iv + ciphertext
    }

    // Decrypt message from sender
    suspend fun decrypt(groupId: String, senderId: String, payload: ByteArray): ByteArray {
        // 1. Get sender key
        val key = senderKeyManager.getSenderKey(groupId, senderId)
            ?: throw IllegalStateException("No sender key for  in group ")

        // 2. Ratchet Step
        val (nextChainKey, messageKey) = HKDF.chainStep(key.chainKey)

        // 3. Extract IV and Ciphertext
        if (payload.size < 12) throw IllegalArgumentException("Payload too short")
        val iv = payload.copyOfRange(0, 12)
        val ciphertext = payload.copyOfRange(12, payload.size)

        // 4. Decrypt
        val plaintext = aesDecrypt(messageKey, iv, ciphertext)

        // 5. Update State
        senderKeyManager.updateSenderKey(key.copy(chainKey = nextChainKey))

        return plaintext
    }

    private fun aesEncrypt(key: ByteArray, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        return Pair(cipher.doFinal(plaintext), iv)
    }

    private fun aesDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val spec = GCMParameterSpec(128, iv)
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(ciphertext)
    }
}
