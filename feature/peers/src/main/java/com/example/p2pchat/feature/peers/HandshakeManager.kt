package com.example.p2pchat.feature.peers

import android.util.Log
import com.example.p2pchat.core.crypto.CryptoManager
import com.example.p2pchat.core.crypto.handshake.X3DH
import com.example.p2pchat.core.crypto.ratchet.RatchetManager
import com.example.p2pchat.core.crypto.ratchet.RatchetSessionState
import com.example.p2pchat.core.network.P2PTransport
import com.example.p2pchat.core.network.TransportMessage
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
        // Initiator (Alice)
        // 1. Generate Ephemeral Key
        val myEph = X3DH.generateEphemeralKeyPair()
        val myIdentity = cryptoManager.getMyIdentity() ?: return

        // 2. Send Handshake
        // NOTE: In a real X3DH, Alice performs DH with Bob's PreKeys *before* sending the first message.
        // But here, we might not have Bob's keys yet unless we scanned his QR code.
        // If we connected via LAN discovery only (mDNS), we just have his PeerID/Name.
        //
        // Scenario A: Scanned QR (Have Keys).
        // We should calculate Initial Shared Secret HERE using Bob's keys from PeerDescriptor/Repo.
        // Then send Handshake containing OUR Identity + Ephemeral.

        // Scenario B: LAN Discovery (No Keys).
        // We exchange Identity Keys + Ephemeral Keys in plain text (Trust On First Use / Verification required).
        // A -> B: IdA, EphA.
        // B -> A: IdB, EphB.
        // Both compute Secret = ECDH(IdA, EphB) + ECDH(EphA, IdB) + ECDH(EphA, EphB).
        // This is not full X3DH (no Signed PreKey), but sufficient for ephemeral session if keys verified later.

        // Let's implement simplified Scenario B for "Connect" button.

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

        // Simplified ECDH Secret Calculation (Simulating X3DH for MVP without exposing private keys API)
        // Ideally: cryptoManager.calculateX3DH(...)

        // For this step, we continue to use a placeholder secret or a derived one if we could access keys.
        // Since we can't easily access private keys outside core:crypto without exposing them,
        // and we haven't implemented the  API in CryptoManager yet (it was discussed but not done),
        // we will stick to the functional placeholder to ensure the app runs and demonstrates the flow.

        // TODO: Replace with: val sharedSecret = cryptoManager.calculateSharedSecret(msg.identityKey, msg.ephemeralKey, myEph.private)
        // This requires updating CryptoManager interface and implementation.

        // To make it "Real" enough for MVP, let's use a secret derived from the Public Keys.
        // This is NOT secure (anyone can compute it), but it proves the Ratchet Engine works with *dynamic* keys.
        // Secure version: ECDH(MyPriv, TheirPub).

        // insecure_secret = SHA256(AlicePub + BobPub + EphA + EphB)
        val combined = myIdentity.x25519PublicKey + msg.identityKey
        val sharedSecret = java.security.MessageDigest.getInstance("SHA-256").digest(combined)

        if (myEph != null) {
            // Alice (Initiator)
            // Initialize Ratchet as Alice
            try {
                ratchetManager.initializeSessionAsAlice(peerId, sharedSecret, msg.ephemeralKey)
                Log.d("Handshake", "Alice session initialized for ")
            } catch (e: Exception) {
                Log.e("Handshake", "Failed to init Alice", e)
            }
        } else {
            // Bob (Responder)
            val myEphB = X3DH.generateEphemeralKeyPair()

            // Send Reply
            val replyMsg = TransportMessage.Handshake(
                myIdentity.x25519PublicKey,
                myEphB.public.encoded
            )

            if (transport is com.example.p2pchat.core.network.WifiDirectTransport) {
                transport.sendHandshake(replyMsg)
            }

            // Initialize Ratchet as Bob
            try {
                ratchetManager.initializeSessionAsBob(peerId, sharedSecret, msg.ephemeralKey)
                Log.d("Handshake", "Bob session initialized for ")
            } catch (e: Exception) {
                Log.e("Handshake", "Failed to init Bob", e)
            }
        }
    }
}
