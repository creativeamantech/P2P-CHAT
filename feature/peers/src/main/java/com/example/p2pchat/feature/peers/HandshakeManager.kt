package com.example.p2pchat.feature.peers

import com.example.p2pchat.core.crypto.CryptoManager
import com.example.p2pchat.core.crypto.handshake.X3DH
import com.example.p2pchat.core.crypto.ratchet.RatchetManager
import com.example.p2pchat.core.crypto.ratchet.RatchetSessionState
import com.example.p2pchat.core.network.P2PTransport
import com.example.p2pchat.core.network.TransportMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandshakeManager @Inject constructor(
    private val transport: P2PTransport,
    private val cryptoManager: CryptoManager,
    private val ratchetManager: RatchetManager
) {

    private val pendingHandshakes = mutableMapOf<String, RatchetSessionState.KeyPair>()

    suspend fun initiateHandshake(peerId: String) {
        val myEph = X3DH.generateEphemeralKeyPair()
        val myIdentity = cryptoManager.getMyIdentity() ?: return

        val handshakeMsg = TransportMessage.Handshake(
            identityKey = myIdentity.x25519PublicKey,
            ephemeralKey = myEph.public.encoded
        )

        if (transport is com.example.p2pchat.core.network.WifiDirectTransport) {
            transport.sendHandshake(handshakeMsg)
        }

        pendingHandshakes[peerId] = myEph
    }

    suspend fun onHandshakeReceived(peerId: String, msg: TransportMessage.Handshake) {
        val myEph = pendingHandshakes.remove(peerId)
        val myIdentity = cryptoManager.getMyIdentity() ?: return

        // This is a placeholder logic for MVP Phase 3.
        // Real X3DH requires accessing private keys which are inside CryptoManager (EncryptedSharedPrefs).
        // To properly implement this, we need to expose a method in CryptoManager that takes
        // the peer's keys and returns the calculated SharedSecret.

        // For MVP Demo, we will simulate Shared Secret derivation to allow the app to compile and "work"
        // in a mock encryption mode if keys aren't perfect.
        // Real security requires the `CryptoManager.calculateSharedSecret` refactor.

        val sharedSecret = ByteArray(32) { 0 } // Dummy secret for compilation/demo

        if (myEph != null) {
            // Alice (Initiator)
            // Received Bob's keys.
            // Derive Shared Secret.
            // Initialize Ratchet as Alice.
            ratchetManager.initializeSessionAsAlice(peerId, sharedSecret, msg.ephemeralKey)
        } else {
            // Bob (Responder)
            // Received Alice's keys.
            val myEphB = X3DH.generateEphemeralKeyPair()

            // Send Reply
            val replyMsg = TransportMessage.Handshake(
                myIdentity.x25519PublicKey,
                myEphB.public.encoded
            )

            if (transport is com.example.p2pchat.core.network.WifiDirectTransport) {
                transport.sendHandshake(replyMsg)
            }

            // Derive Shared Secret.
            // Initialize Ratchet as Bob.
            ratchetManager.initializeSessionAsBob(peerId, sharedSecret, msg.ephemeralKey)
        }
    }
}
