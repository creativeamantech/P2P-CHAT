package com.example.p2pchat.core.network

import com.example.p2pchat.core.storage.repository.PersistentMessageQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionManager @Inject constructor(
    private val wifiDirectTransport: WifiDirectTransport,
    private val bluetoothTransport: BluetoothTransport,
    private val messageQueue: PersistentMessageQueue
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Aggregated Connection State
    // Simplified: logical OR of transports.
    // Ideally map: PeerId -> Transport
    private val activeTransports = ConcurrentHashMap<String, P2PTransport>()

    val connectionState: Flow<ConnectionState> = combine(
        wifiDirectTransport.connectionState,
        bluetoothTransport.connectionState
    ) { wifiState, btState ->
        if (wifiState is ConnectionState.Connected) {
            wifiState
        } else if (btState is ConnectionState.Connected) {
            btState
        } else if (wifiState is ConnectionState.Connecting || btState is ConnectionState.Connecting) {
            ConnectionState.Connecting
        } else {
            ConnectionState.Disconnected
        }
    }

    val incomingMessages: Flow<EncryptedPayload> = merge(
        wifiDirectTransport.receive(),
        bluetoothTransport.receive()
    )

    init {
        // Monitor connections to flush queue
        scope.launch {
            connectionState.collect { state ->
                if (state is ConnectionState.Connected) {
                    // Flush queue for connected peer
                    // Current P2PTransport impl doesn't expose WHO is connected easily in state.
                    // Assuming 1:1 connection for MVP.
                    flushQueue("peer_id_placeholder")
                }
            }
        }
    }

    suspend fun connect(peerDescriptor: PeerDescriptor): Result<Unit> {
        // Priority: WiFi > Bluetooth
        // Try WiFi first
        val wifiResult = wifiDirectTransport.connect(peerDescriptor)
        if (wifiResult.isSuccess) {
            activeTransports[peerDescriptor.peerId] = wifiDirectTransport
            return wifiResult
        }

        // Fallback to Bluetooth
        val btResult = bluetoothTransport.connect(peerDescriptor)
        if (btResult.isSuccess) {
            activeTransports[peerDescriptor.peerId] = bluetoothTransport
            return btResult
        }

        return Result.failure(Exception("All transports failed"))
    }

    suspend fun disconnect() {
        wifiDirectTransport.disconnect()
        bluetoothTransport.disconnect()
        activeTransports.clear()
    }

    suspend fun sendMessage(peerId: String, payload: EncryptedPayload) {
        // 1. Enqueue
        messageQueue.enqueue(peerId, payload.data)

        // 2. Try to send immediately
        flushQueue(peerId)
    }

    private suspend fun flushQueue(peerId: String) {
        val transport = getActiveTransport(peerId) ?: return // Not connected

        val pending = messageQueue.getPendingForPeer(peerId)
        for (msg in pending) {
            val result = transport.send(EncryptedPayload(msg.payload))
            if (result.isSuccess) {
                messageQueue.remove(msg.id)
            } else {
                break // Stop on error, retry later
            }
        }
    }

    private fun getActiveTransport(peerId: String): P2PTransport? {
        // For MVP, if we are connected via any transport, we assume it's to the active peer.
        // Real implementation requires transport to report connected Peer ID.
        if (wifiDirectTransport.connectionState.value is ConnectionState.Connected) return wifiDirectTransport
        if (bluetoothTransport.connectionState.value is ConnectionState.Connected) return bluetoothTransport
        return null
    }

    fun discoverPeers(): Flow<List<PeerDescriptor>> {
        // Aggregate discovery
        return combine(
            wifiDirectTransport.discoverPeers(),
            bluetoothTransport.discoverPeers()
        ) { wifiPeers, btPeers ->
            // Deduplicate by ID?
            (wifiPeers + btPeers).distinctBy { it.peerId }
        }
    }
}
