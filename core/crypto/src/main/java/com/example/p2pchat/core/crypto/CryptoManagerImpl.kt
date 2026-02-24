package com.example.p2pchat.core.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.p2pchat.core.model.UserIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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

    private val secureRandom = SecureRandom()

    override suspend fun getMyIdentity(): UserIdentity? {
        val userId = sharedPreferences.getString("user_id", null) ?: return null
        val displayName = sharedPreferences.getString("display_name", "Unknown") ?: "Unknown"
        val ed25519PubBase64 = sharedPreferences.getString("ed25519_pub", null)
        val x25519PubBase64 = sharedPreferences.getString("x25519_pub", null)

        if (ed25519PubBase64 == null || x25519PubBase64 == null) return null

        return UserIdentity(
            userId = userId,
            displayName = displayName,
            ed25519PublicKey = Base64.decode(ed25519PubBase64, Base64.DEFAULT),
            x25519PublicKey = Base64.decode(x25519PubBase64, Base64.DEFAULT),
            selfSignedCert = Any() // Dummy
        )
    }

    override suspend fun generateNewIdentity(displayName: String): UserIdentity {
        // Generate Ed25519 Identity Key
        val edGen = Ed25519KeyPairGenerator()
        edGen.init(Ed25519KeyGenerationParameters(secureRandom))
        val edPair = edGen.generateKeyPair()
        val edPriv = edPair.private as Ed25519PrivateKeyParameters
        val edPub = edPair.public as Ed25519PublicKeyParameters

        // Generate X25519 Pre-Key
        val xGen = X25519KeyPairGenerator()
        xGen.init(X25519KeyGenerationParameters(secureRandom))
        val xPair = xGen.generateKeyPair()
        val xPriv = xPair.private as X25519PrivateKeyParameters
        val xPub = xPair.public as X25519PublicKeyParameters

        val userId = UUID.randomUUID().toString()

        // Store Keys
        sharedPreferences.edit()
            .putString("user_id", userId)
            .putString("display_name", displayName)
            .putString("ed25519_priv", Base64.encodeToString(edPriv.encoded, Base64.DEFAULT))
            .putString("ed25519_pub", Base64.encodeToString(edPub.encoded, Base64.DEFAULT))
            .putString("x25519_priv", Base64.encodeToString(xPriv.encoded, Base64.DEFAULT))
            .putString("x25519_pub", Base64.encodeToString(xPub.encoded, Base64.DEFAULT))
            .apply()

        return UserIdentity(
            userId = userId,
            displayName = displayName,
            ed25519PublicKey = edPub.encoded,
            x25519PublicKey = xPub.encoded,
            selfSignedCert = Any() // Dummy
        )
    }

    override fun encrypt(plaintext: ByteArray, recipientPublicKey: ByteArray): ByteArray {
        val sharedSecret = calculateSharedSecret(recipientPublicKey)

        // Derive AES Key (using simple SecretKeySpec for MVP, in real app use HKDF)
        val aesKey = SecretKeySpec(sharedSecret, "AES")

        // Encrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // Prepend IV
        return iv + ciphertext
    }

    override fun decrypt(ciphertext: ByteArray, senderPublicKey: ByteArray): ByteArray {
        val sharedSecret = calculateSharedSecret(senderPublicKey)
        val aesKey = SecretKeySpec(sharedSecret, "AES")

        // Decrypt
        if (ciphertext.size < 12) return ByteArray(0)
        val iv = ciphertext.copyOfRange(0, 12)
        val actualCiphertext = ciphertext.copyOfRange(12, ciphertext.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(actualCiphertext)
    }

    override fun calculateSharedSecret(remotePublicKey: ByteArray): ByteArray {
        val myPrivBase64 = sharedPreferences.getString("x25519_priv", null) ?: throw IllegalStateException("No private key initialized")
        val myPrivBytes = Base64.decode(myPrivBase64, Base64.DEFAULT)
        val myPrivKey = X25519PrivateKeyParameters(myPrivBytes, 0)

        val theirPubKey = X25519PublicKeyParameters(remotePublicKey, 0)
        val sharedSecret = ByteArray(32)
        myPrivKey.generateSecret(theirPubKey, sharedSecret, 0)

        return sharedSecret
    }

    override fun getDatabasePassphrase(): ByteArray {
        val key = "db_passphrase"
        val existing = sharedPreferences.getString(key, null)
        return if (existing != null) {
            Base64.decode(existing, Base64.DEFAULT)
        } else {
            val passphrase = ByteArray(32)
            secureRandom.nextBytes(passphrase)
            val encoded = Base64.encodeToString(passphrase, Base64.DEFAULT)
            sharedPreferences.edit().putString(key, encoded).apply()
            passphrase
        }
    }
}
