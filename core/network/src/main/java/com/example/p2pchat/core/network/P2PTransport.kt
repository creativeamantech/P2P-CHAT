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
    suspend fun sendHandshake(message: TransportMessage.Handshake): Result<Unit> // Added
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
    val address: String? = null // IP, MAC, etc.
)

data class EncryptedPayload(
    val data: ByteArray,
    val senderId: String? = null,
    val isHandshake: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedPayload
        if (!data.contentEquals(other.data)) return false
        if (senderId != other.senderId) return false
        if (isHandshake != other.isHandshake) return false
        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + (senderId?.hashCode() ?: 0)
        result = 31 * result + isHandshake.hashCode()
        return result
    }
}
