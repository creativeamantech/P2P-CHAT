package com.example.p2pchat.core.crypto.ratchet

import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters

data class RatchetSessionState(
    // DH Ratchet
    val dhPair: KeyPair,              // My current DH key pair
    val dhRemotePublic: X25519PublicKeyParameters?, // Remote's current public key

    // Symmetric Ratchet
    val rootKey: ByteArray,           // 32 bytes

    // Chain Keys
    val chainKeySend: ByteArray?,     // Current sending chain key
    val chainKeyRecv: ByteArray?,     // Current receiving chain key

    // Message Numbers
    val ns: Int = 0,                  // Send message number
    val nr: Int = 0,                  // Received message number (last)
    val pn: Int = 0                   // Previous chain length (for header)
) {
    data class KeyPair(
        val private: X25519PrivateKeyParameters,
        val public: X25519PublicKeyParameters
    )
}

data class RatchetHeader(
    val dhPublic: ByteArray, // 32 bytes (My current public key)
    val pn: Int,             // Previous chain length
    val n: Int               // Message number
)
