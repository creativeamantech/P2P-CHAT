package com.example.p2pchat.core.crypto.ratchet

import com.example.p2pchat.core.model.UserIdentity
import com.example.p2pchat.core.storage.entity.RatchetStateEntity
import com.example.p2pchat.core.storage.repository.RatchetRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatchetManager @Inject constructor(
    private val ratchetRepository: RatchetRepository,
    // private val identityManager: IdentityManager // If we had one for long-term keys
) {
    private val ratchetEngine = RatchetEngine()
    private val mutex = Mutex() // Lock to prevent race conditions on ratchet state

    // Initialize session as Alice (Initiator)
    suspend fun initializeSessionAsAlice(
        peerId: String,
        sharedSecret: ByteArray,
        bobPublicKey: ByteArray
    ) {
        mutex.withLock {
            val bobPubParams = X25519PublicKeyParameters(bobPublicKey, 0)
            val sessionState = ratchetEngine.initializeAlice(sharedSecret, bobPubParams)
            saveState(peerId, sessionState)
        }
    }

    // Initialize session as Bob (Responder)
    suspend fun initializeSessionAsBob(
        peerId: String,
        sharedSecret: ByteArray,
        alicePublicKey: ByteArray // Currently unused in engine init for Bob, but good to have
    ) {
        mutex.withLock {
            val alicePubParams = X25519PublicKeyParameters(alicePublicKey, 0)
            val sessionState = ratchetEngine.initializeBob(sharedSecret, alicePubParams)
            saveState(peerId, sessionState)
        }
    }

    suspend fun encrypt(peerId: String, plaintext: ByteArray): ByteArray {
        mutex.withLock {
            val currentState = loadState(peerId) ?: throw IllegalStateException("No session for peer ")

            val (newState, payload) = ratchetEngine.encrypt(currentState, plaintext)

            saveState(peerId, newState)
            return payload
        }
    }

    suspend fun decrypt(peerId: String, payload: ByteArray): ByteArray {
        mutex.withLock {
            val currentState = loadState(peerId) ?: throw IllegalStateException("No session for peer ")

            val (newState, plaintext) = ratchetEngine.decrypt(currentState, payload)

            saveState(peerId, newState)
            return plaintext
        }
    }

    private suspend fun loadState(peerId: String): RatchetSessionState? {
        val entity = ratchetRepository.getRatchetState(peerId) ?: return null

        val dhPriv = X25519PrivateKeyParameters(entity.dhPriv, 0)
        val dhPub = X25519PublicKeyParameters(entity.dhPub, 0)
        val dhPair = RatchetSessionState.KeyPair(dhPriv, dhPub)

        val dhRemotePub = entity.dhRemotePub?.let { X25519PublicKeyParameters(it, 0) }

        return RatchetSessionState(
            dhPair = dhPair,
            dhRemotePublic = dhRemotePub,
            rootKey = entity.rootKey,
            chainKeySend = entity.chainKeySend,
            chainKeyRecv = entity.chainKeyRecv,
            ns = entity.sendMessageNum,
            nr = entity.recvMessageNum,
            pn = entity.prevChainNum
        )
    }

    private suspend fun saveState(peerId: String, state: RatchetSessionState) {
        val entity = RatchetStateEntity(
            peerId = peerId,
            rootKey = state.rootKey,
            dhPriv = state.dhPair.private.encoded,
            dhPub = state.dhPair.public.encoded,
            dhRemotePub = state.dhRemotePublic?.encoded,
            chainKeySend = state.chainKeySend,
            chainKeyRecv = state.chainKeyRecv,
            sendMessageNum = state.ns,
            recvMessageNum = state.nr,
            prevChainNum = state.pn,
            updatedAt = System.currentTimeMillis()
        )
        ratchetRepository.saveRatchetState(entity)
    }
}
