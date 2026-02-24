package com.example.p2pchat.core.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDirectTransport @Inject constructor(
    @ApplicationContext private val context: Context
) : P2PTransport {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    private val wifiP2pChannel: WifiP2pManager.Channel? by lazy {
        manager?.initialize(context, context.mainLooper, null)
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override val peerId: String = "local_peer" // TODO: Inject Identity

    private var socket: Socket? = null
    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null

    private val incomingMessages = Channel<EncryptedPayload>(Channel.BUFFERED)

    @SuppressLint("MissingPermission")
    override fun discoverPeers(): Flow<List<PeerDescriptor>> = callbackFlow {
        if (!hasPermissions()) {
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        manager?.requestPeers(wifiP2pChannel) { peers ->
                            val descriptors = peers.deviceList.map { device ->
                                PeerDescriptor(
                                    peerId = device.deviceAddress,
                                    name = device.deviceName,
                                    address = device.deviceAddress
                                )
                            }
                            trySend(descriptors)
                        }
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        val networkInfo = intent.getParcelableExtra<android.net.NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                        if (networkInfo?.isConnected == true) {
                            manager?.requestConnectionInfo(wifiP2pChannel) { info ->
                                handleConnectionInfo(info)
                            }
                        } else {
                            _connectionState.value = ConnectionState.Disconnected
                            closeSocket()
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, intentFilter)

        manager?.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {}
        })

        awaitClose {
            context.unregisterReceiver(receiver)
            manager?.stopPeerDiscovery(wifiP2pChannel, null)
        }
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        if (_connectionState.value is ConnectionState.Connected) return // Already connected logic

        scope.launch {
            try {
                if (info.groupFormed && info.isGroupOwner) {
                    _connectionState.value = ConnectionState.Connecting
                    // Start Server
                    val serverSocket = ServerSocket(47890)
                    socket = serverSocket.accept() // Block until client connects
                    _connectionState.value = ConnectionState.Connected
                    setupStreams()
                } else if (info.groupFormed) {
                    _connectionState.value = ConnectionState.Connecting
                    // Client: Connect to Group Owner
                    val host = info.groupOwnerAddress.hostAddress
                    socket = Socket()
                    socket?.connect(InetSocketAddress(host, 47890), 5000)
                    _connectionState.value = ConnectionState.Connected
                    setupStreams()
                }
            } catch (e: Exception) {
                Log.e("P2P", "Connection error", e)
                _connectionState.value = ConnectionState.Error(e)
                closeSocket()
            }
        }
    }

    private fun setupStreams() {
        val s = socket ?: return
        inputStream = DataInputStream(s.getInputStream())
        outputStream = DataOutputStream(s.getOutputStream())

        // Start listening loop
        scope.launch {
            try {
                while (true) {
                    val length = inputStream?.readInt() ?: break
                    val bytes = ByteArray(length)
                    inputStream?.readFully(bytes)

                    // Parse TransportMessage
                    // In real app, we handle handshake packets here internally
                    // and only expose Payload to the Flow once handshake is done.
                    // For MVP Phase 2, we just pass bytes.
                    // But now we defined TransportMessage.

                    try {
                        val message = TransportMessage.fromBytes(bytes)
                        when (message) {
                            is TransportMessage.Handshake -> {
                                // TODO: Handle Handshake (Delegate to ConnectionManager or higher level)
                                // For now, we expose it wrapped as EncryptedPayload just to pass data up
                                // This assumes upper layer handles handshake
                                incomingMessages.send(EncryptedPayload(bytes))
                            }
                            is TransportMessage.Chat -> {
                                incomingMessages.send(EncryptedPayload(message.payload))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("P2P", "Parse error", e)
                    }
                }
            } catch (e: IOException) {
                Log.e("P2P", "Receive error", e)
                _connectionState.value = ConnectionState.Disconnected
                closeSocket()
            }
        }
    }

    private fun closeSocket() {
        try {
            socket?.close()
            socket = null
            inputStream = null
            outputStream = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(peerDescriptor: PeerDescriptor): Result<Unit> {
        if (!hasPermissions()) return Result.failure(SecurityException("Missing permissions"))

        _connectionState.value = ConnectionState.Connecting

        val config = WifiP2pConfig().apply {
            deviceAddress = peerDescriptor.address
        }

        return withContext(Dispatchers.Main) { // connect needs main thread or looper usually
             try {
                 manager?.connect(wifiP2pChannel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        // Wait for CONNECTION_CHANGED_ACTION
                    }

                    override fun onFailure(reason: Int) {
                        _connectionState.value = ConnectionState.Error(Exception("Connection initiation failed: "))
                    }
                })
                Result.success(Unit)
             } catch (e: Exception) {
                 Result.failure(e)
             }
        }
    }

    override suspend fun send(payload: EncryptedPayload): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val out = outputStream ?: return@withContext Result.failure(Exception("Not connected"))

                // Wrap in TransportMessage.Chat
                val msg = TransportMessage.Chat(payload.data)
                val bytes = msg.toBytes()

                synchronized(out) {
                    out.writeInt(bytes.size)
                    out.write(bytes)
                    out.flush()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Special method to send raw handshake bytes (TransportMessage.Handshake)
    suspend fun sendHandshake(message: TransportMessage.Handshake): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val out = outputStream ?: return@withContext Result.failure(Exception("Not connected"))
                val bytes = message.toBytes()
                synchronized(out) {
                    out.writeInt(bytes.size)
                    out.write(bytes)
                    out.flush()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun receive(): Flow<EncryptedPayload> = incomingMessages.consumeAsFlow()

    override suspend fun disconnect() {
        manager?.removeGroup(wifiP2pChannel, null)
        closeSocket()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}
