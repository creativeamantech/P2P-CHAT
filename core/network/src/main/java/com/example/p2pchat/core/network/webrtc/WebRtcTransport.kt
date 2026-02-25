package com.example.p2pchat.core.network.webrtc

import android.content.Context
import android.util.Log
import com.example.p2pchat.core.network.ConnectionState
import com.example.p2pchat.core.network.EncryptedPayload
import com.example.p2pchat.core.network.P2PTransport
import com.example.p2pchat.core.network.PeerDescriptor
import com.example.p2pchat.core.network.TransportMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcTransport @Inject constructor(
    @ApplicationContext private val context: Context
) : P2PTransport {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    override val peerId: String = "webrtc_transport"

    private lateinit var factory: PeerConnectionFactory
    private val activeConnections = mutableMapOf<String, PeerConnection>()
    private val activeDataChannels = mutableMapOf<String, DataChannel>()

    private val incomingMessages = Channel<EncryptedPayload>(Channel.BUFFERED)

    // Callback to send signaling messages via another transport
    var signalingSender: ((peerId: String, message: TransportMessage.Signaling) -> Unit)? = null

    init {
        initializeFactory()
    }

    private fun initializeFactory() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    override fun discoverPeers(): Flow<List<PeerDescriptor>> = flow {
        emit(emptyList())
    }

    override suspend fun connect(peerDescriptor: PeerDescriptor): Result<Unit> {
        val peerId = peerDescriptor.peerId
        if (activeConnections.containsKey(peerId)) return Result.success(Unit)

        val pc = createPeerConnection(peerId)
        if (pc == null) return Result.failure(Exception("Failed to create PeerConnection"))

        // Create Data Channel (Initiator must create it)
        val dcInit = DataChannel.Init()
        dcInit.ordered = true
        val dc = pc.createDataChannel("chat", dcInit)
        if (dc != null) {
            setupDataChannel(peerId, dc)
            activeDataChannels[peerId] = dc
        }

        // Create Offer
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    pc.setLocalDescription(this, it)
                    signalingSender?.invoke(peerId, TransportMessage.Signaling("OFFER", it.description))
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String?) {
                _connectionState.value = ConnectionState.Error(Exception("Create Offer Failed: $s"))
            }
            override fun onSetFailure(s: String?) {}
        }, MediaConstraints())

        _connectionState.value = ConnectionState.Connecting
        return Result.success(Unit)
    }

    private fun createPeerConnection(peerId: String): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        )

        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    _connectionState.value = ConnectionState.Connected
                } else if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                           state == PeerConnection.IceConnectionState.FAILED ||
                           state == PeerConnection.IceConnectionState.CLOSED) {
                    // Only disconnect if it's the last connection? Or track per peer?
                    // P2PTransport interface has global state.
                    // If one peer disconnects, we shouldn't necessarily set global to Disconnected if others exist.
                    // But for MVP/Simplicity, we can leave it.
                    activeConnections.remove(peerId)
                    activeDataChannels.remove(peerId)
                    if (activeConnections.isEmpty()) {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }
            }
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    val payload = "${it.sdpMid}|${it.sdpMLineIndex}|${it.sdp}"
                    signalingSender?.invoke(peerId, TransportMessage.Signaling("ICE", payload))
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) {
                dc?.let {
                    setupDataChannel(peerId, it)
                    activeDataChannels[peerId] = it
                }
            }
            override fun onRenegotiationNeeded() {}
        }

        val pc = factory.createPeerConnection(rtcConfig, observer)
        if (pc != null) {
            activeConnections[peerId] = pc
        }
        return pc
    }

    private fun setupDataChannel(peerId: String, dc: DataChannel) {
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}
            override fun onStateChange() {
                Log.d("WebRTC", "DataChannel state: ${dc.state()}")
            }
            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.let {
                    val data = ByteArray(it.data.remaining())
                    it.data.get(data)

                    try {
                        // DataChannel messages are frame-based. No length prefix needed.
                        // First byte is Type.
                        val message = TransportMessage.fromBytes(data)
                        when (message) {
                            is TransportMessage.Chat -> {
                                incomingMessages.trySend(EncryptedPayload(message.payload, peerId, false))
                            }
                            is TransportMessage.Handshake -> {
                                incomingMessages.trySend(EncryptedPayload(message.toBytes(), peerId, true))
                            }
                            is TransportMessage.AttachmentChunk -> {
                                incomingMessages.trySend(EncryptedPayload(message.toBytes(), peerId, false, true))
                            }
                            is TransportMessage.Signaling -> {
                                // Signaling over DataChannel? Possible, but usually out-of-band.
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WebRTC", "Failed to parse message", e)
                    }
                }
            }
        })
    }

    // Handle incoming signaling messages from other transports
    fun onSignalingMessage(peerId: String, msg: TransportMessage.Signaling) {
        val pc = activeConnections[peerId] ?: createPeerConnection(peerId) ?: return

        when (msg.type) {
            "OFFER" -> {
                pc.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {}
                    override fun onSetSuccess() {
                        pc.createAnswer(object : SdpObserver {
                            override fun onCreateSuccess(desc: SessionDescription?) {
                                desc?.let {
                                    pc.setLocalDescription(this, it)
                                    signalingSender?.invoke(peerId, TransportMessage.Signaling("ANSWER", it.description))
                                }
                            }
                            override fun onSetSuccess() {}
                            override fun onCreateFailure(s: String?) {}
                            override fun onSetFailure(s: String?) {}
                        }, MediaConstraints())
                    }
                    override fun onCreateFailure(s: String?) {}
                    override fun onSetFailure(s: String?) {}
                }, SessionDescription(SessionDescription.Type.OFFER, msg.payload))
            }
            "ANSWER" -> {
                pc.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {}
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(s: String?) {}
                    override fun onSetFailure(s: String?) {}
                }, SessionDescription(SessionDescription.Type.ANSWER, msg.payload))
            }
            "ICE" -> {
                val parts = msg.payload.split("|", limit = 3)
                if (parts.size == 3) {
                    val candidate = IceCandidate(parts[0], parts[1].toInt(), parts[2])
                    pc.addIceCandidate(candidate)
                }
            }
        }
    }

    override suspend fun send(payload: EncryptedPayload): Result<Unit> {
        return Result.failure(Exception("Use send(payload, peerId)"))
    }

    override suspend fun send(payload: EncryptedPayload, peerId: String?): Result<Unit> {
        if (peerId == null) return Result.failure(Exception("PeerID required"))
        val dc = activeDataChannels[peerId] ?: return Result.failure(Exception("No DataChannel"))

        val msg = TransportMessage.Chat(payload.data)
        val bytes = msg.toBytes()
        val buffer = ByteBuffer.wrap(bytes)

        val sent = dc.send(DataChannel.Buffer(buffer, true))
        return if (sent) Result.success(Unit) else Result.failure(Exception("Send failed"))
    }

    override suspend fun sendHandshake(message: TransportMessage.Handshake, peerId: String?): Result<Unit> {
        if (peerId == null) return Result.failure(Exception("PeerID required"))
        val dc = activeDataChannels[peerId] ?: return Result.failure(Exception("No DataChannel"))

        val bytes = message.toBytes()
        val buffer = ByteBuffer.wrap(bytes)

        val sent = dc.send(DataChannel.Buffer(buffer, true))
        return if (sent) Result.success(Unit) else Result.failure(Exception("Send failed"))
    }

    override suspend fun sendAttachment(message: TransportMessage.AttachmentChunk, peerId: String?): Result<Unit> {
        if (peerId == null) return Result.failure(Exception("PeerID required"))
        val dc = activeDataChannels[peerId] ?: return Result.failure(Exception("No DataChannel"))

        val bytes = message.toBytes()
        val buffer = ByteBuffer.wrap(bytes)

        val sent = dc.send(DataChannel.Buffer(buffer, true))
        return if (sent) Result.success(Unit) else Result.failure(Exception("Send failed"))
    }

    override fun receive(): Flow<EncryptedPayload> = incomingMessages.consumeAsFlow()

    override suspend fun disconnect() {
        activeConnections.values.forEach { it.close() }
        activeConnections.clear()
        activeDataChannels.clear()
        factory.dispose()
        _connectionState.value = ConnectionState.Disconnected
    }
}
