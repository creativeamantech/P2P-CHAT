package com.example.p2pchat.core.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.p2pchat.core.model.UserIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import android.util.Base64

class CryptoManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CryptoManager {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun getMyIdentity(): UserIdentity {
        // Return dummy identity
        return UserIdentity(
            userId = "dummy_id",
            displayName = "Me",
            ed25519PublicKey = ByteArray(32),
            x25519PublicKey = ByteArray(32),
            selfSignedCert = Any()
        )
    }

    override suspend fun generateNewIdentity(displayName: String): UserIdentity {
        return UserIdentity(
            userId = "new_id",
            displayName = displayName,
            ed25519PublicKey = ByteArray(32),
            x25519PublicKey = ByteArray(32),
            selfSignedCert = Any()
        )
    }

    override fun encrypt(plaintext: ByteArray, recipientPublicKey: ByteArray): ByteArray {
        return plaintext // Dummy encryption
    }

    override fun decrypt(ciphertext: ByteArray, senderPublicKey: ByteArray): ByteArray {
        return ciphertext // Dummy decryption
    }

    override fun getDatabasePassphrase(): ByteArray {
        val key = "db_passphrase"
        val existing = sharedPreferences.getString(key, null)
        return if (existing != null) {
            Base64.decode(existing, Base64.DEFAULT)
        } else {
            val passphrase = ByteArray(32)
            SecureRandom().nextBytes(passphrase)
            val encoded = Base64.encodeToString(passphrase, Base64.DEFAULT)
            sharedPreferences.edit().putString(key, encoded).apply()
            passphrase
        }
    }
}
