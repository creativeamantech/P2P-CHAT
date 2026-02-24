package com.example.p2pchat.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface P2PTransport {
    val peerId: String
    val connectionState: StateFlow<ConnectionState>

    // Discovery
    fun discoverPeers(): Flow<List<PeerDescriptor>>

    // Connection
    suspend fun connect(peerDescriptor: PeerDescriptor): Result<Unit>
    suspend fun send(payload: EncryptedPayload): Result<Unit>
    suspend fun sendHandshake(message: TransportMessage.Handshake): Result<Unit>
    suspend fun sendAttachment(message: TransportMessage.AttachmentChunk): Result<Unit> // Added
    fun receive(): Flow<EncryptedPayload>
    suspend fun disconnect()
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val cause: Throwable) : ConnectionState()
}

data class PeerDescriptor(
    val peerId: String,
    val name: String,
    val address: String? = null, // IP, MAC, onion, etc.
    val identityKey: ByteArray? = null, // Ed25519 Public Key
    val exchangeKey: ByteArray? = null, // X25519 Public Key
    val relay: String? = null, // STUN/TURN server or relay URL
    val signature: ByteArray? = null // Signature of the descriptor data
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PeerDescriptor

        if (peerId != other.peerId) return false
        if (name != other.name) return false
        if (address != other.address) return false
        if (identityKey != null) {
            if (other.identityKey == null) return false
            if (!identityKey.contentEquals(other.identityKey)) return false
        } else if (other.identityKey != null) return false
        if (exchangeKey != null) {
            if (other.exchangeKey == null) return false
            if (!exchangeKey.contentEquals(other.exchangeKey)) return false
        } else if (other.exchangeKey != null) return false
        if (relay != other.relay) return false
        if (signature != null) {
            if (other.signature == null) return false
            if (!signature.contentEquals(other.signature)) return false
        } else if (other.signature != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = peerId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (address?.hashCode() ?: 0)
        result = 31 * result + (identityKey?.contentHashCode() ?: 0)
        result = 31 * result + (exchangeKey?.contentHashCode() ?: 0)
        result = 31 * result + (relay?.hashCode() ?: 0)
        result = 31 * result + (signature?.contentHashCode() ?: 0)
        return result
    }
}

data class EncryptedPayload(
    val data: ByteArray,
    val senderId: String? = null,
    val isHandshake: Boolean = false,
    val isAttachment: Boolean = false // Added
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedPayload
        if (!data.contentEquals(other.data)) return false
        if (senderId != other.senderId) return false
        if (isHandshake != other.isHandshake) return false
        if (isAttachment != other.isAttachment) return false
        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + (senderId?.hashCode() ?: 0)
        result = 31 * result + isHandshake.hashCode()
        result = 31 * result + isAttachment.hashCode()
        return result
    }
}
