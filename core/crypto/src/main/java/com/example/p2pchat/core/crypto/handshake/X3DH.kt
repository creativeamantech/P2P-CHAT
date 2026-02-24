package com.example.p2pchat.core.crypto.handshake

import com.example.p2pchat.core.crypto.ratchet.RatchetSessionState
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom

/**
 * Simplified X3DH (Extended Triple Diffie-Hellman) implementation.
 */
object X3DH {
    private val secureRandom = SecureRandom()

    fun generateEphemeralKeyPair(): RatchetSessionState.KeyPair {
        val gen = X25519KeyPairGenerator()
        gen.init(X25519KeyGenerationParameters(secureRandom))
        val pair = gen.generateKeyPair()
        return RatchetSessionState.KeyPair(
            pair.private as X25519PrivateKeyParameters,
            pair.public as X25519PublicKeyParameters
        )
    }

    // Expose DH computation for HandshakeManager
    fun computeDH(priv: X25519PrivateKeyParameters, pub: X25519PublicKeyParameters): ByteArray {
        val secret = ByteArray(32)
        priv.generateSecret(pub, secret, 0)
        return secret
    }

    // Also helper for byte array inputs if needed
    fun computeDH(privBytes: ByteArray, pubBytes: ByteArray): ByteArray {
        val priv = X25519PrivateKeyParameters(privBytes, 0)
        val pub = X25519PublicKeyParameters(pubBytes, 0)
        return computeDH(priv, pub)
    }

    fun kdf(input: ByteArray): ByteArray {
        // Simple SHA-256 for MVP.
        // Real impl should use HKDF.
        val digest = org.bouncycastle.crypto.digests.SHA256Digest()
        val output = ByteArray(digest.digestSize)
        digest.update(input, 0, input.size)
        digest.doFinal(output, 0)
        return output
    }
}
