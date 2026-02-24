package com.example.p2pchat.core.network

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothTransport @Inject constructor(
    @ApplicationContext private val context: Context
) : P2PTransport {

    private val P2P_UUID = UUID.fromString("a3b4c5d6-1234-5678-abcd-ef0123456789")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override val peerId: String = adapter?.address ?: "local_bt" // Address is usually unavailable on modern Android

    private var socket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null

    private val incomingMessages = Channel<EncryptedPayload>(Channel.BUFFERED)

    init {
        startServer()
    }

    private fun startServer() {
        if (!hasPermissions() || adapter == null) return

        scope.launch {
            try {
                @SuppressLint("MissingPermission")
                val server = adapter.listenUsingInsecureRfcommWithServiceRecord("P2PChat", P2P_UUID)
                serverSocket = server
                while (true) {
                    val client = server.accept()
                    if (socket == null) { // Only handle one connection for MVP
                        socket = client
                        _connectionState.value = ConnectionState.Connected
                        setupStreams()
                        server.close()
                        break
                    } else {
                        client.close()
                    }
                }
            } catch (e: IOException) {
                Log.e("BT", "Server error", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun discoverPeers(): Flow<List<PeerDescriptor>> = callbackFlow {
        if (!hasPermissions() || adapter == null) {
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null) {
                            val descriptor = PeerDescriptor(
                                peerId = device.address,
                                name = device.name ?: "Unknown",
                                address = device.address
                            )
                            // Ideally we aggregate lists
                            trySend(listOf(descriptor))
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, intentFilter)

        adapter.startDiscovery()

        awaitClose {
            adapter.cancelDiscovery()
            context.unregisterReceiver(receiver)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(peerDescriptor: PeerDescriptor): Result<Unit> {
        if (!hasPermissions() || adapter == null) return Result.failure(SecurityException("Missing permissions"))

        adapter.cancelDiscovery()
        _connectionState.value = ConnectionState.Connecting

        return withContext(Dispatchers.IO) {
            try {
                val device = adapter.getRemoteDevice(peerDescriptor.address)
                val clientSocket = device.createInsecureRfcommSocketToServiceRecord(P2P_UUID)
                clientSocket.connect()
                socket = clientSocket
                _connectionState.value = ConnectionState.Connected
                setupStreams()
                Result.success(Unit)
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.Error(e)
                Result.failure(e)
            }
        }
    }

    private fun setupStreams() {
        val s = socket ?: return
        inputStream = DataInputStream(s.inputStream)
        outputStream = DataOutputStream(s.outputStream)

        scope.launch {
            try {
                while (true) {
                    val length = inputStream?.readInt() ?: break
                    val bytes = ByteArray(length)
                    inputStream?.readFully(bytes)
                    incomingMessages.send(EncryptedPayload(bytes))
                }
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.Disconnected
                closeSocket()
            }
        }
    }

    override suspend fun send(payload: EncryptedPayload): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val out = outputStream ?: return@withContext Result.failure(Exception("Not connected"))
                synchronized(out) {
                    out.writeInt(payload.data.size)
                    out.write(payload.data)
                    out.flush()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun sendHandshake(message: TransportMessage.Handshake): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val out = outputStream ?: return@withContext Result.failure(Exception("Not connected"))
                val bytes = message.toBytes()
                synchronized(out) {
                    out.write(bytes)
                    out.flush()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun sendAttachment(message: TransportMessage.AttachmentChunk): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val out = outputStream ?: return@withContext Result.failure(Exception("Not connected"))
                val bytes = message.toBytes()
                synchronized(out) {
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
        closeSocket()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun closeSocket() {
        try {
            socket?.close()
            socket = null
            inputStream = null
            outputStream = null
        } catch (e: Exception) {}
    }

    private fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return false
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return false
        } else {
            // Legacy
        }
        return true
    }
}
