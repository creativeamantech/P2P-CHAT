package com.example.p2pchat.core.model

data class UserIdentity(
    val userId: String,           // UUID v4
    val displayName: String,
    val ed25519PublicKey: ByteArray,   // For signing
    val x25519PublicKey: ByteArray,    // For key exchange
    val selfSignedCert: Any // Replaced X509Certificate with Any for now as it requires Android or heavy dep
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserIdentity

        if (userId != other.userId) return false
        if (displayName != other.displayName) return false
        if (!ed25519PublicKey.contentEquals(other.ed25519PublicKey)) return false
        if (!x25519PublicKey.contentEquals(other.x25519PublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + ed25519PublicKey.contentHashCode()
        result = 31 * result + x25519PublicKey.contentHashCode()
        return result
    }
}
