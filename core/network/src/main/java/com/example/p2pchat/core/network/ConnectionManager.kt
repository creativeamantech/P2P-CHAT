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

import com.example.p2pchat.core.network.tor.TorTransport
import com.example.p2pchat.core.network.webrtc.WebRtcTransport
import com.example.p2pchat.core.crypto.ratchet.RatchetManager

@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiDirectTransport: WifiDirectTransport,
    private val bluetoothTransport: BluetoothTransport,
    private val torTransport: TorTransport,
    private val webRtcTransport: WebRtcTransport,
    private val messageQueue: PersistentMessageQueue,
    private val mixNetworkLayer: MixNetworkLayer,
    private val coverTrafficManager: CoverTrafficManager,
    private val ratchetManager: RatchetManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var privacyLevel = PrivacyLevel.STANDARD // Default

    // Aggregated Connection State
    private val activeTransports = ConcurrentHashMap<String, P2PTransport>()

    val connectionState: Flow<ConnectionState> = combine(
        wifiDirectTransport.connectionState,
        bluetoothTransport.connectionState,
        torTransport.connectionState,
        webRtcTransport.connectionState
    ) { wifiState, btState, torState, rtcState ->
        if (rtcState is ConnectionState.Connected) rtcState
        else if (wifiState is ConnectionState.Connected) wifiState
        else if (btState is ConnectionState.Connected) btState
        else if (torState is ConnectionState.Connected) torState
        else if (wifiState is ConnectionState.Connecting || btState is ConnectionState.Connecting || torState is ConnectionState.Connecting || rtcState is ConnectionState.Connecting) {
            ConnectionState.Connecting
        } else {
            ConnectionState.Disconnected
        }
    }

    val incomingMessages: Flow<EncryptedPayload> = merge(
        wifiDirectTransport.receive(),
        bluetoothTransport.receive(),
        torTransport.receive(),
        webRtcTransport.receive()
    )

    init {
        // Setup Signaling Callback for WebRTC
        webRtcTransport.signalingSender = { peerId, message ->
            // Send this signaling message via whatever transport is currently active for this peer
            scope.launch {
                val transport = getActiveTransport(peerId)
                if (transport != null) {
                    // Wrap signaling message in EncryptedPayload logic?
                    // No, transports usually send EncryptedPayload.
                    // But signaling messages are effectively cleartext (or encrypted by session if possible).
                    // If we use the transport's `send` which expects EncryptedPayload (Ratchet Encrypted),
                    // we are encrypting SDP. This is GOOD.
                    // BUT, `TransportMessage.Signaling` is a type.
                    // We need to wrap it into `EncryptedPayload`.
                    // BUT `send` usually takes `EncryptedPayload` and sends it as `Chat`.
                    // We need a way to send `Signaling` TYPE.

                    // Actually, `TorTransport.send(EncryptedPayload)` wraps it in `Chat`.
                    // We need a raw send or update `EncryptedPayload` to support `Signaling` flag.
                    // OR we send it as `Chat` content, but with a prefix?

                    // Ideally, we assume we are inside the E2E tunnel.
                    // So we send `TransportMessage.Signaling` serialized as bytes, encrypted by Ratchet.
                    // The receiver decrypts it, sees it's a `Signaling` message (how?), and routes it to `WebRtcTransport`.

                    // Problem: `RatchetEngine` decrypts to `ByteArray`.
                    // `MessageProcessor` parses `TransportMessage`.
                    // If `TransportMessage` has `Signaling` type, `MessageProcessor` should handle it.

                    // So:
                    // 1. Serialize `TransportMessage.Signaling`
                    // 2. Encrypt it (sendMessage logic)
                    // 3. Send via transport

                    // But `TransportMessage` is what is INSIDE the encryption?
                    // In `TorTransport`: `deserializePayload` reads `TransportMessage`.
                    // Wait, `TorTransport` sends `EncryptedPayload` wrapped in `TransportMessage.Chat`.
                    // So `EncryptedPayload` = [Header + Ciphertext].
                    // Ciphertext = Encrypted(TransportMessage).

                    // IF `MessageProcessor` decrypts `EncryptedPayload`, it gets `TransportMessage` bytes?
                    // `MessageProcessor` calls `ratchetManager.decrypt`. Result is `plaintext`.
                    // `plaintext` should be `TransportMessage` (Chat or Signaling).

                    // So we just need to send it like a normal message!
                    // But we don't want to save it to DB as Chat.
                    // `MessageProcessor` needs to distinguish.

                    // For now, let's create a special helper or just use `sendMessage`?
                    // But `sendMessage` enqueues. Signaling should be ephemeral/fast?
                    // And we construct `Signaling` object.

                    // I will create `sendSignalingMessage` method.
                    sendSignalingMessage(peerId, message)
                }
            }
        }

        // Schedule WorkManager
        scheduleMessageFlush()

        // Monitor connections to flush queue
        scope.launch {
            connectionState.collect { state ->
                if (state is ConnectionState.Connected) {
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
        // Check for Onion Address (Tor)
        if (peerDescriptor.address?.endsWith(".onion") == true) {
            val torResult = torTransport.connect(peerDescriptor)
            if (torResult.isSuccess) {
                activeTransports[peerDescriptor.peerId] = torTransport
                return torResult
            }
            // If Tor fails, and we have no other transports, fail.
            // Assuming no multi-homing fallback between Onion and Wifi Direct unless addresses match.
            return torResult
        }

        // Priority: WiFi > Bluetooth (Local)
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
        torTransport.disconnect()
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

    suspend fun sendSignalingMessage(peerId: String, message: TransportMessage.Signaling) {
        try {
            // Encrypt using Ratchet session (Signaling inside E2E tunnel)
            val plaintext = message.toBytes()
            val encryptedBytes = ratchetManager.encrypt(peerId, plaintext)

            // Wrap in EncryptedPayload
            // Note: Receiver MessageProcessor will decrypt -> parse TransportMessage -> handle Signaling
            val payload = EncryptedPayload(encryptedBytes)

            // Send directly (bypass queue for lower latency, or use queue if reliability needed)
            // WebRTC signaling needs reliability, but strict ordering?
            // Ratchet enforces ordering. If we drop one, chain breaks (in this simple impl).
            // So we MUST ensure delivery.
            sendMessage(peerId, payload)
        } catch (e: Exception) {
            // Log error
        }
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
                // Use routed send if supported (Tor) or default send (Wifi/BT)
                val result = transport.send(payload, peerId)
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
