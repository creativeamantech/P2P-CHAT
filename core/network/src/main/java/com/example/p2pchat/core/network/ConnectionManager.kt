package com.example.p2pchat.core.network

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.p2pchat.core.network.config.PrivacyLevel
import com.example.p2pchat.core.network.privacy.CoverTrafficManager
import com.example.p2pchat.core.network.privacy.MixNetworkLayer
import com.example.p2pchat.core.storage.repository.PersistentMessageQueue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiDirectTransport: WifiDirectTransport,
    private val bluetoothTransport: BluetoothTransport,
    private val messageQueue: PersistentMessageQueue,
    private val mixNetworkLayer: MixNetworkLayer,
    private val coverTrafficManager: CoverTrafficManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var privacyLevel = PrivacyLevel.STANDARD // Default

    // Aggregated Connection State
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
        // Schedule WorkManager
        scheduleMessageFlush()

        // Monitor connections to flush queue
        scope.launch {
            connectionState.collect { state ->
                if (state is ConnectionState.Connected) {
                    // Try to flush for connected peers
                    // Since we don't have exact peer ID in state, we rely on activeTransports map
                    // or just iterate known peers with pending messages.
                    val pendingPeers = messageQueue.getPeersWithPendingMessages()
                    for (peerId in pendingPeers) {
                        flushQueue(peerId)
                    }
                }
            }
        }
    }

    private fun scheduleMessageFlush() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // For WebRTC later, or generally connectivity
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MessageFlushWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "MessageFlush",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
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

    fun setPrivacyLevel(level: PrivacyLevel) {
        this.privacyLevel = level
        if (level == PrivacyLevel.MAXIMUM) {
            // Start cover traffic for all active peers?
            // For MVP, we start when connected.
        } else {
            // Stop cover traffic
            // We need peerId to stop specific ones.
            // Ideally iterate activeTransports.
            activeTransports.keys.forEach { coverTrafficManager.stopCoverTraffic(it) }
        }
    }

    suspend fun sendMessage(peerId: String, payload: EncryptedPayload) {
        // 1. Enqueue
        messageQueue.enqueue(peerId, payload.data)

        // 2. Try to send immediately
        flushQueue(peerId)
    }

    suspend fun flushQueue(peerId: String) {
        val transport = getActiveTransport(peerId) ?: return // Not connected

        // Ensure privacy features are active if needed
        if (privacyLevel == PrivacyLevel.MAXIMUM) {
             coverTrafficManager.startCoverTraffic(peerId, transport)
        }

        val pending = messageQueue.getPendingForPeer(peerId)
        for (msg in pending) {
            val payload = EncryptedPayload(msg.payload, senderId = null)

            if (privacyLevel >= PrivacyLevel.HIGH) {
                // Use Mix Network
                mixNetworkLayer.sendViaMix(payload, peerId, transport)
                messageQueue.remove(msg.id)
            } else {
                // Direct Send
                val result = transport.send(payload)
                if (result.isSuccess) {
                    messageQueue.remove(msg.id)
                } else {
                    break // Stop on error, retry later
                }
            }
        }
    }

    fun getActiveTransport(peerId: String): P2PTransport? {
        // Check active map
        if (activeTransports.containsKey(peerId)) return activeTransports[peerId]

        // Fallback checks (e.g. if we connected but map update lagged or simplified logic)
        // For MVP assuming 1:1, return any connected transport
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
            (wifiPeers + btPeers).distinctBy { it.peerId }
        }
    }
}
