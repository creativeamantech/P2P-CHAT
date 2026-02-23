package com.example.p2pchat.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface P2PTransport {
    val peerId: String
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(peerDescriptor: PeerDescriptor): Result<Unit>
    suspend fun send(payload: EncryptedPayload): Result<Unit>
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
    val address: String? = null // IP, MAC, etc.
)

data class EncryptedPayload(
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedPayload
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}
