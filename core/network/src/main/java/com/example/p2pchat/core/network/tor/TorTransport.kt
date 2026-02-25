package com.example.p2pchat.core.network.tor

import android.util.Log
import com.example.p2pchat.core.network.ConnectionState
import com.example.p2pchat.core.network.EncryptedPayload
import com.example.p2pchat.core.network.P2PTransport
import com.example.p2pchat.core.network.PeerDescriptor
import com.example.p2pchat.core.network.TransportMessage
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorTransport @Inject constructor(
    private val torManager: TorManager
) : P2PTransport {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    override val peerId: String = "tor_transport"

    private var serverSocket: ServerSocket? = null

    // Map PeerID -> Socket/Stream
    private val activeSockets = mutableMapOf<String, Socket>()
    private val activeOutputStreams = mutableMapOf<String, DataOutputStream>()

    // Shared channel for all incoming messages from all Tor connections
    private val incomingMessages = Channel<EncryptedPayload>(Channel.BUFFERED)

    override fun discoverPeers(): Flow<List<PeerDescriptor>> = flow {
        emit(emptyList())
    }

    override suspend fun connect(peerDescriptor: PeerDescriptor): Result<Unit> {
        val onionAddress = peerDescriptor.address
        if (onionAddress == null || !onionAddress.endsWith(".onion")) {
            return Result.failure(IllegalArgumentException("Invalid onion address"))
        }

        return withContext(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.Connecting

                if (!torManager.isTorReady.value) {
                    torManager.startTor()
                    var attempts = 0
                    while(!torManager.isTorReady.value && attempts < 20) {
                        Thread.sleep(1000)
                        attempts++
                    }
                    if (!torManager.isTorReady.value) throw IllegalStateException("Tor failed to start")
                }

                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(torManager.getSocksProxyHost(), torManager.getSocksProxyPort()))
                val socket = Socket(proxy)

                socket.connect(InetSocketAddress.createUnresolved(onionAddress, 47890))

                handleConnection(socket, peerDescriptor.peerId, outgoing = true)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("TorTransport", "Connect failed", e)
                _connectionState.value = ConnectionState.Error(e)
                Result.failure(e)
            }
        }
    }

    private fun handleConnection(socket: Socket, remotePeerId: String?, outgoing: Boolean) {
        scope.launch {
            try {
                val input = DataInputStream(socket.getInputStream())
                val output = DataOutputStream(socket.getOutputStream())

                if (remotePeerId != null) {
                    synchronized(activeSockets) {
                        activeSockets[remotePeerId] = socket
                        activeOutputStreams[remotePeerId] = output
                    }
                    _connectionState.value = ConnectionState.Connected
                }

                // Read Loop
                while (isActive && socket.isConnected) {
                    val type = input.readByte().toInt()

                    if (type == 1) { // Handshake
                        val idLen = input.readInt()
                        val idKey = ByteArray(idLen)
                        input.readFully(idKey)

                        val exLen = input.readInt()
                        val exKey = ByteArray(exLen)
                        input.readFully(exKey)

                        val ephLen = input.readInt()
                        val ephKey = ByteArray(ephLen)
                        input.readFully(ephKey)

                        val handshake = TransportMessage.Handshake(idKey, exKey, ephKey)
                        incomingMessages.send(EncryptedPayload(handshake.toBytes(), remotePeerId, true))

                    } else if (type == 2) { // Chat
                        val len = input.readInt()
                        val payload = ByteArray(len)
                        input.readFully(payload)

                        incomingMessages.send(EncryptedPayload(payload, remotePeerId, false))

                    } else if (type == 3) { // Attachment
                        val transferId = input.readUTF()
                        val index = input.readInt()
                        val total = input.readInt()
                        val len = input.readInt()
                        val data = ByteArray(len)
                        input.readFully(data)

                        val chunk = TransportMessage.AttachmentChunk(transferId, index, total, data)
                        incomingMessages.send(EncryptedPayload(chunk.toBytes(), remotePeerId, false, true))
                    } else if (type == 4) { // Signaling
                        val sigType = input.readUTF()
                        val sigPayload = input.readUTF()
                        // TODO: Handle Signaling
                    } else {
                        // Unknown type or EOF
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("TorTransport", "Connection error", e)
                _connectionState.value = ConnectionState.Disconnected // Only if this was the last one?
                if (remotePeerId != null) {
                    synchronized(activeSockets) {
                        activeSockets.remove(remotePeerId)
                        activeOutputStreams.remove(remotePeerId)
                    }
                }
                socket.close()
            }
        }
    }

    fun startListening() {
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(torManager.getLocalServerPort())
                while (isActive) {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null) {
                        handleConnection(clientSocket, null, outgoing = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("TorTransport", "Server socket failed", e)
            }
        }
    }

    override suspend fun send(payload: EncryptedPayload): Result<Unit> {
        return Result.failure(Exception("TorTransport requires peerId for routing. Use send(payload, peerId)"))
    }

    override suspend fun send(payload: EncryptedPayload, peerId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        if (peerId == null) return@withContext Result.failure(Exception("PeerID required for Tor transport"))

        try {
            val output = synchronized(activeSockets) { activeOutputStreams[peerId] }
                ?: return@withContext Result.failure(Exception("No connection to $peerId"))

            val msg = TransportMessage.Chat(payload.data)
            val bytes = msg.toBytes()

            synchronized(output) {
                output.write(bytes)
                output.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendHandshake(message: TransportMessage.Handshake, peerId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        if (peerId == null) return@withContext Result.failure(Exception("PeerID required for Tor transport"))
        try {
             val output = synchronized(activeSockets) { activeOutputStreams[peerId] }
                ?: return@withContext Result.failure(Exception("No connection to $peerId"))
            val bytes = message.toBytes()
            synchronized(output) {
                output.write(bytes)
                output.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendAttachment(message: TransportMessage.AttachmentChunk, peerId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        if (peerId == null) return@withContext Result.failure(Exception("PeerID required for Tor transport"))
         try {
             val output = synchronized(activeSockets) { activeOutputStreams[peerId] }
                ?: return@withContext Result.failure(Exception("No connection to $peerId"))
            val bytes = message.toBytes()
            synchronized(output) {
                output.write(bytes)
                output.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        serverSocket?.close()
        synchronized(activeSockets) {
            activeSockets.values.forEach { it.close() }
            activeSockets.clear()
            activeOutputStreams.clear()
        }
        torManager.stopTor()
        _connectionState.value = ConnectionState.Disconnected
    }

    override fun receive(): Flow<EncryptedPayload> = incomingMessages.consumeAsFlow()
}
