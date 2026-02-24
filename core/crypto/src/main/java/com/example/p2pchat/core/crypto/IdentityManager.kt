package com.example.p2pchat.core.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.p2pchat.core.storage.dao.IdentityDao
import com.example.p2pchat.core.storage.entity.IdentityEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val identityDao: IdentityDao
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "identity_keystore",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val secureRandom = SecureRandom()

    suspend fun createIdentity(displayName: String, type: String, expiresAt: Long? = null): IdentityEntity {
        val id = UUID.randomUUID().toString()
        val edAlias = "${id}_ed25519"
        val xAlias = "${id}_x25519"

        // Generate Keys
        val edGen = Ed25519KeyPairGenerator()
        edGen.init(Ed25519KeyGenerationParameters(secureRandom))
        val edPair = edGen.generateKeyPair()
        val edPriv = edPair.private as Ed25519PrivateKeyParameters
        val edPub = edPair.public as Ed25519PublicKeyParameters

        val xGen = X25519KeyPairGenerator()
        xGen.init(X25519KeyGenerationParameters(secureRandom))
        val xPair = xGen.generateKeyPair()
        val xPriv = xPair.private as X25519PrivateKeyParameters
        val xPub = xPair.public as X25519PublicKeyParameters

        // Store Keys securely mapped to alias (using EncryptedSharedPreferences for now, similar to CryptoManagerImpl)
        sharedPreferences.edit()
            .putString("${edAlias}_priv", Base64.encodeToString(edPriv.encoded, Base64.DEFAULT))
            .putString("${edAlias}_pub", Base64.encodeToString(edPub.encoded, Base64.DEFAULT))
            .putString("${xAlias}_priv", Base64.encodeToString(xPriv.encoded, Base64.DEFAULT))
            .putString("${xAlias}_pub", Base64.encodeToString(xPub.encoded, Base64.DEFAULT))
            .apply()

        val entity = IdentityEntity(
            id = id,
            type = type,
            displayName = displayName,
            createdAt = System.currentTimeMillis(),
            expiresAt = expiresAt,
            isBurned = false,
            ed25519Alias = edAlias,
            x25519Alias = xAlias
        )

        identityDao.insert(entity)
        return entity
    }

    suspend fun burnIdentity(id: String) {
        val entity = identityDao.getById(id) ?: return

        // Delete keys
        sharedPreferences.edit()
            .remove("${entity.ed25519Alias}_priv")
            .remove("${entity.ed25519Alias}_pub")
            .remove("${entity.x25519Alias}_priv")
            .remove("${entity.x25519Alias}_pub")
            .apply()

        identityDao.markBurned(id)
    }

    fun getAllActiveIdentities(): Flow<List<IdentityEntity>> {
        return identityDao.getAllActive()
    }

    // Helper to get keys for an identity (Internal use)
    fun getIdentityKeys(id: String): IdentityKeys? {
        // Need to fetch entity? Or construct alias from ID?
        // Alias is deterministic based on createIdentity: "${id}_..."
        // But better to verify existence.
        // Since this is sync, maybe just try load?
        val edAlias = "${id}_ed25519"
        val xAlias = "${id}_x25519"

        val edPrivStr = sharedPreferences.getString("${edAlias}_priv", null) ?: return null
        val edPubStr = sharedPreferences.getString("${edAlias}_pub", null) ?: return null
        val xPrivStr = sharedPreferences.getString("${xAlias}_priv", null) ?: return null
        val xPubStr = sharedPreferences.getString("${xAlias}_pub", null) ?: return null

        return IdentityKeys(
            ed25519PrivateKey = Base64.decode(edPrivStr, Base64.DEFAULT),
            ed25519PublicKey = Base64.decode(edPubStr, Base64.DEFAULT),
            x25519PrivateKey = Base64.decode(xPrivStr, Base64.DEFAULT),
            x25519PublicKey = Base64.decode(xPubStr, Base64.DEFAULT)
        )
    }

    data class IdentityKeys(
        val ed25519PrivateKey: ByteArray,
        val ed25519PublicKey: ByteArray,
        val x25519PrivateKey: ByteArray,
        val x25519PublicKey: ByteArray
    )
}
