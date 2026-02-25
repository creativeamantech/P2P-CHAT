package com.example.p2pchat.core.network

import android.util.Log
import com.example.p2pchat.core.crypto.CryptoManager
import com.example.p2pchat.core.crypto.handshake.X3DH
import com.example.p2pchat.core.crypto.ratchet.RatchetManager
import com.example.p2pchat.core.crypto.ratchet.RatchetSessionState
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import javax.inject.Inject
import javax.inject.Singleton

// Forward declaration to break circular dependency if any.
// ConnectionManager is used to find the transport.
// But ConnectionManager depends on HandshakeManager?
// Let's check ConnectionManager.
// Yes, ConnectionManager injects HandshakeManager via MessageProcessor?
// No, MessageProcessor injects HandshakeManager.
// ConnectionManager injects MessageProcessor?
// Let's check imports. HandshakeManager needs a way to send message.
// ConnectionManager has `getActiveTransport(peerId)`.
// We can inject `Provider<ConnectionManager>` or refactor.
// Or just inject `ConnectionManager` if no cycle.
// Cycle: ConnectionManager -> MessageFlushWorker -> ...
// Cycle: MessageProcessor -> ConnectionManager.
// Cycle: HandshakeManager -> ConnectionManager.
// MessageProcessor -> HandshakeManager.
// So: MP -> HM -> CM -> MP (Cycle!).

// Solution: HandshakeManager should not depend on ConnectionManager directly if CM depends on MP which depends on HM.
// Instead, HandshakeManager should return the handshake message to the caller?
// Or we use a callback or an interface `TransportProvider`.
// OR we break the cycle by injecting `Lazy<ConnectionManager>` or `Provider`.
// Let's use `javax.inject.Provider`.

import javax.inject.Provider

@Singleton
class HandshakeManager @Inject constructor(
    private val connectionManagerProvider: Provider<ConnectionManager>,
    private val cryptoManager: CryptoManager,
    private val ratchetManager: RatchetManager
) {

    private val pendingHandshakes = mutableMapOf<String, RatchetSessionState.KeyPair>()

    suspend fun initiateHandshake(peerId: String) {
        // Initiator (Alice)
        // 1. Generate Ephemeral Key
        val myEph = X3DH.generateEphemeralKeyPair()
        val myIdentity = cryptoManager.getMyIdentity() ?: return

        // 2. Send Handshake
        val handshakeMsg = TransportMessage.Handshake(
            identityKey = myIdentity.ed25519PublicKey, // Ed25519
            exchangeKey = myIdentity.x25519PublicKey, // X25519
            ephemeralKey = myEph.public.encoded
        )

        pendingHandshakes[peerId] = myEph

        val transport = connectionManagerProvider.get().getActiveTransport(peerId)
        if (transport != null) {
            transport.sendHandshake(handshakeMsg, peerId)
        } else {
            Log.e("Handshake", "No active transport for $peerId")
        }
    }

    suspend fun onHandshakeReceived(peerId: String, msg: TransportMessage.Handshake) {
        val myPendingEph = pendingHandshakes.remove(peerId)
        val myIdentity = cryptoManager.getMyIdentity() ?: return

        val theirIdentityPub = msg.identityKey // Ed25519
        val theirExchangePub = msg.exchangeKey // X25519
        val theirEphPub = msg.ephemeralKey // X25519

        if (myPendingEph != null) {
            // WE are ALICE (Initiator), receiving reply from BOB (Responder)

            // Calculate 3-DH Secret
            // 1. DH(EphA_Priv, EphB_Pub)
            val dh1 = X3DH.computeDH(myPendingEph.private, X25519PublicKeyParameters(theirEphPub, 0))

            // 2. DH(IdentityA_Priv, EphB_Pub) -> CryptoManager handles this (using local X25519 Identity Priv)
            val dh2 = cryptoManager.calculateSharedSecret(theirEphPub)

            // 3. DH(EphA_Priv, IdentityB_Pub) -> Use EphA_Priv and Bob's X25519 Identity Pub
            val dh3 = X3DH.computeDH(myPendingEph.private, X25519PublicKeyParameters(theirExchangePub, 0))

            // Concatenate DHs
            val combined = ByteArray(dh1.size + dh2.size + dh3.size)
            System.arraycopy(dh1, 0, combined, 0, dh1.size)
            System.arraycopy(dh2, 0, combined, dh1.size, dh2.size)
            System.arraycopy(dh3, 0, combined, dh1.size + dh2.size, dh3.size)

            val sharedSecret = X3DH.kdf(combined)

            // Initialize Ratchet as Alice
            try {
                ratchetManager.initializeSessionAsAlice(peerId, sharedSecret, theirEphPub)
                Log.d("Handshake", "Alice session initialized for $peerId")
            } catch (e: Exception) {
                Log.e("Handshake", "Failed to init Alice", e)
            }

        } else {
            // WE are BOB (Responder), receiving initial handshake from ALICE (Initiator)

            // 1. Generate Ephemeral Key EphB
            val myEphB = X3DH.generateEphemeralKeyPair()

            // Calculate 3-DH Secret
            // 1. DH(EphB_Priv, EphA_Pub)
            val dh1 = X3DH.computeDH(myEphB.private, X25519PublicKeyParameters(theirEphPub, 0))

            // 2. DH(EphB_Priv, IdentityA_Pub) -> Use EphB_Priv and Alice's X25519 Identity Pub
            val dh2 = X3DH.computeDH(myEphB.private, X25519PublicKeyParameters(theirExchangePub, 0))

            // 3. DH(IdentityB_Priv, EphA_Pub) -> CryptoManager handles this
            val dh3 = cryptoManager.calculateSharedSecret(theirEphPub)

            // Concatenate DHs
            val combined = ByteArray(dh1.size + dh2.size + dh3.size)
            System.arraycopy(dh1, 0, combined, 0, dh1.size)
            System.arraycopy(dh2, 0, combined, dh1.size, dh2.size)
            System.arraycopy(dh3, 0, combined, dh1.size + dh2.size, dh3.size)

            val sharedSecret = X3DH.kdf(combined)

            // Initialize Ratchet as Bob
            try {
                ratchetManager.initializeSessionAsBob(peerId, sharedSecret, theirEphPub, myEphB)
                Log.d("Handshake", "Bob session initialized for $peerId")
            } catch (e: Exception) {
                Log.e("Handshake", "Failed to init Bob", e)
            }

            // Send Reply
            val replyMsg = TransportMessage.Handshake(
                identityKey = myIdentity.ed25519PublicKey,
                exchangeKey = myIdentity.x25519PublicKey,
                ephemeralKey = myEphB.public.encoded
            )

            val transport = connectionManagerProvider.get().getActiveTransport(peerId)
            if (transport != null) {
                transport.sendHandshake(replyMsg, peerId)
            } else {
                Log.e("Handshake", "No active transport to reply to $peerId")
            }
        }
    }
}
