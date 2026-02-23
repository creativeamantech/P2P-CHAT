package com.example.p2pchat.core.model

import kotlinx.datetime.Instant

data class Peer(
    val id: String,
    val displayName: String,
    val publicKey: PublicKeyBundle,
    val lastSeen: Instant,
    val isTrusted: Boolean,
    val connectionHistory: List<ConnectionRecord> = emptyList()
)

data class PublicKeyBundle(
    val identityKey: ByteArray,   // Ed25519
    val exchangeKey: ByteArray,   // X25519
    val certPem: String           // Self-signed X.509
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PublicKeyBundle

        if (!identityKey.contentEquals(other.identityKey)) return false
        if (!exchangeKey.contentEquals(other.exchangeKey)) return false
        if (certPem != other.certPem) return false

        return true
    }

    override fun hashCode(): Int {
        var result = identityKey.contentHashCode()
        result = 31 * result + exchangeKey.contentHashCode()
        result = 31 * result + certPem.hashCode()
        return result
    }
}

data class ConnectionRecord(
    val connectedAt: Instant,
    val disconnectedAt: Instant?
)
