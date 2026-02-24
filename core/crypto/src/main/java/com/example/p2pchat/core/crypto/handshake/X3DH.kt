package com.example.p2pchat.core.crypto.handshake

import com.example.p2pchat.core.crypto.ratchet.RatchetSessionState
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom

/**
 * Simplified X3DH (Extended Triple Diffie-Hellman) implementation.
 *
 * Protocol:
 * Alice (Initiator) wants to send to Bob (Responder).
 *
 * 1. Alice fetches Bob's PreKey Bundle:
 *    - IKB (Bob's Identity Key)
 *    - SPKB (Bob's Signed PreKey)
 *    - OPKB (Bob's One-Time PreKey) -- Optional, omitting for MVP simplicity
 *
 * 2. Alice performs DH:
 *    - DH1 = DH(IKA, SPKB)
 *    - DH2 = DH(EKA, IKB)
 *    - DH3 = DH(EKA, SPKB)
 *    - SK = KDF(DH1 || DH2 || DH3)
 *
 * 3. Alice sends to Bob:
 *    - IKA (Alice's Identity Key)
 *    - EKA (Alice's Ephemeral Key)
 *
 * 4. Bob receives and performs same DH calculations to derive SK.
 */
object X3DH {
    private val secureRandom = SecureRandom()

    data class PreKeyBundle(
        val identityKey: ByteArray,
        val signedPreKey: ByteArray,
        val preKeySignature: ByteArray
    )

    data class InitialMessage(
        val identityKey: ByteArray, // Alice's Identity
        val ephemeralKey: ByteArray // Alice's Ephemeral
    )

    fun generateEphemeralKeyPair(): RatchetSessionState.KeyPair {
        val gen = X25519KeyPairGenerator()
        gen.init(X25519KeyGenerationParameters(secureRandom))
        val pair = gen.generateKeyPair()
        return RatchetSessionState.KeyPair(
            pair.private as X25519PrivateKeyParameters,
            pair.public as X25519PublicKeyParameters
        )
    }

    // Alice calculates Shared Secret
    fun calculateSenderSecret(
        myIdentityPrivateKey: ByteArray,
        myEphemeralPrivateKey: ByteArray,
        bobIdentityKey: ByteArray,
        bobSignedPreKey: ByteArray
    ): ByteArray {
        val ikA = X25519PrivateKeyParameters(myIdentityPrivateKey, 0)
        val ekA = X25519PrivateKeyParameters(myEphemeralPrivateKey, 0)
        val ikB = X25519PublicKeyParameters(bobIdentityKey, 0)
        val spkB = X25519PublicKeyParameters(bobSignedPreKey, 0)

        // DH1 = DH(IKA, SPKB) -- Wait, X3DH usually is DH(IKA, SPKB) + DH(EKA, IKB) + DH(EKA, SPKB)
        // Signal spec:
        // DH1 = DH(IKA, SPKB)
        // DH2 = DH(EKA, IKB)
        // DH3 = DH(EKA, SPKB)

        val dh1 = computeDH(ikA, spkB)
        val dh2 = computeDH(ekA, ikB)
        val dh3 = computeDH(ekA, spkB)

        return kdf(dh1 + dh2 + dh3)
    }

    // Bob calculates Shared Secret
    fun calculateReceiverSecret(
        myIdentityPrivateKey: ByteArray,
        mySignedPreKeyPrivateKey: ByteArray,
        aliceIdentityKey: ByteArray,
        aliceEphemeralKey: ByteArray
    ): ByteArray {
        val ikB = X25519PrivateKeyParameters(myIdentityPrivateKey, 0)
        val spkB = X25519PrivateKeyParameters(mySignedPreKeyPrivateKey, 0)
        val ikA = X25519PublicKeyParameters(aliceIdentityKey, 0)
        val ekA = X25519PublicKeyParameters(aliceEphemeralKey, 0)

        // DH1 = DH(SPKB, IKA)
        // DH2 = DH(IKB, EKA)
        // DH3 = DH(SPKB, EKA)

        val dh1 = computeDH(spkB, ikA)
        val dh2 = computeDH(ikB, ekA)
        val dh3 = computeDH(spkB, ekA)

        return kdf(dh1 + dh2 + dh3)
    }

    private fun computeDH(priv: X25519PrivateKeyParameters, pub: X25519PublicKeyParameters): ByteArray {
        val secret = ByteArray(32)
        priv.generateSecret(pub, secret, 0)
        return secret
    }

    private fun kdf(input: ByteArray): ByteArray {
        // Simple SHA-256 for MVP.
        // Real impl should use HKDF.
        val digest = org.bouncycastle.crypto.digests.SHA256Digest()
        val output = ByteArray(digest.digestSize)
        digest.update(input, 0, input.size)
        digest.doFinal(output, 0)
        return output
    }
}
