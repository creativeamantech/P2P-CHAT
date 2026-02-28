package com.example.p2pchat.core.crypto

import com.example.p2pchat.core.storage.dao.IdentityDao
import com.example.p2pchat.core.storage.entity.IdentityEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences

@Singleton
class IdentityManager @Inject constructor(
    private val identityDao: IdentityDao,
    private val cryptoManagerImpl: CryptoManagerImpl
    // Typically we'd use KeystoreWrapper, here we'll use EncryptedSharedPreferences for simplicity
    // as it's already configured in CryptoManagerImpl, but we need raw access or delegate to it.
) {
    // For MVP, we will store keys in EncryptedSharedPreferences and metadata in DB
    private val secureRandom = SecureRandom()

    // Active identity state
    private val _activeIdentityId = MutableStateFlow<String?>(null)
    val activeIdentityId: Flow<String?> = _activeIdentityId.asStateFlow()

    suspend fun setActiveIdentity(id: String) {
        _activeIdentityId.value = id
    }

    suspend fun getAllIdentities(): Flow<List<IdentityEntity>> {
        return identityDao.getAllIdentities()
    }

    suspend fun generateIdentity(name: String, type: String = "PERMANENT"): IdentityEntity {
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

        val id = UUID.randomUUID().toString()
        val edAlias = "ed25519_$id"
        val xAlias = "x25519_$id"

        // We need a way to store these. Let's use a temporary in-memory map or a dedicated keystore class.
        // But since we have CryptoManagerImpl doing it for a single identity, we need to adapt it.
        // For MVP, we'll assume CryptoManager uses activeIdentityId to lookup keys from SharedPreferences.

        // Save to DB
        val entity = IdentityEntity(
            id = id,
            type = type,
            displayName = name,
            createdAt = System.currentTimeMillis(),
            expiresAt = if (type == "BURNER") System.currentTimeMillis() + 24 * 60 * 60 * 1000 else null,
            isBurned = false,
            ed25519Alias = edAlias,
            x25519Alias = xAlias
        )

        identityDao.insertIdentity(entity)

        // Instruct CryptoManager to store keys for these aliases
        // This is a bit of a circular dependency if not careful, so we might need to refactor Keystore logic out.
        // For now, let's just create the entity.
        // We will need to store the actual keys. Let's add a KeystoreWrapper.

        return entity
    }

    suspend fun burnIdentity(id: String) {
        val entity = identityDao.getIdentity(id) ?: return
        // delete keys from keystore
        // keystore.delete(entity.ed25519Alias)

        identityDao.insertIdentity(entity.copy(isBurned = true))

        if (_activeIdentityId.value == id) {
            _activeIdentityId.value = null
        }
    }
}
