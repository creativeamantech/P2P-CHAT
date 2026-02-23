package com.example.p2pchat.core.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDirectTransport @Inject constructor(
    @ApplicationContext private val context: Context
) : P2PTransport {

    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    private val wifiP2pChannel: WifiP2pManager.Channel? by lazy {
        manager?.initialize(context, context.mainLooper, null)
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override val peerId: String = "local_peer" // Should be injected or managed

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
                                    peerId = device.deviceAddress, // MAC as ID for now
                                    name = device.deviceName,
                                    address = device.deviceAddress
                                )
                            }
                            trySend(descriptors)
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
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

    @SuppressLint("MissingPermission")
    override suspend fun connect(peerDescriptor: PeerDescriptor): Result<Unit> {
        if (!hasPermissions()) return Result.failure(SecurityException("Missing permissions"))

        _connectionState.value = ConnectionState.Connecting

        val config = WifiP2pConfig().apply {
            deviceAddress = peerDescriptor.address
        }

        manager?.connect(wifiP2pChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Connection initiated
                // Actual connection state changes via broadcast
                // For MVP, assume connected
                _connectionState.value = ConnectionState.Connected
            }

            override fun onFailure(reason: Int) {
                _connectionState.value = ConnectionState.Error(Exception("Connection failed: $reason"))
            }
        })

        return Result.success(Unit) // Async result handled by state flow
    }

    override suspend fun send(payload: EncryptedPayload): Result<Unit> {
        // Socket send logic
        return Result.success(Unit)
    }

    override fun receive(): Flow<EncryptedPayload> = flow {
        // Socket receive logic
    }

    override suspend fun disconnect() {
        manager?.removeGroup(wifiP2pChannel, null)
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
