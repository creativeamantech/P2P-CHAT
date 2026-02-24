package com.example.p2pchat.core.crypto

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentCipher @Inject constructor() {

    data class EncryptionResult(
        val key: ByteArray,
        val iv: ByteArray,
        val outputFile: File
    )

    fun encryptFile(inputFile: File, outputFile: File): EncryptionResult {
        val key = ByteArray(32)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(key)
        SecureRandom().nextBytes(iv)

        val spec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, spec, gcmSpec)

        FileInputStream(inputFile).use { input ->
            CipherOutputStream(FileOutputStream(outputFile), cipher).use { output ->
                input.copyTo(output)
            }
        }

        return EncryptionResult(key, iv, outputFile)
    }

    fun decryptFile(inputFile: File, outputFile: File, key: ByteArray, iv: ByteArray) {
        val spec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, spec, gcmSpec)

        FileInputStream(inputFile).use { input ->
            CipherInputStream(input, cipher).use { cipherIn ->
                FileOutputStream(outputFile).use { output ->
                    cipherIn.copyTo(output)
                }
            }
        }
    }
}
