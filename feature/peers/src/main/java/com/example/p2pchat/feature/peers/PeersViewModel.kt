package com.example.p2pchat.feature.peers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.model.Peer
import com.example.p2pchat.core.model.PublicKeyBundle
import com.example.p2pchat.core.network.ConnectionManager
import com.example.p2pchat.core.network.ConnectionState
import com.example.p2pchat.core.network.PeerDescriptor
import com.example.p2pchat.core.storage.repository.PeerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

sealed interface PeersUiState {
    object Loading : PeersUiState
    data class Success(
        val discoveredPeers: List<PeerDescriptor>,
        val connectedPeerId: String?,
        val connectionState: ConnectionState
    ) : PeersUiState
    object Empty : PeersUiState
}

@HiltViewModel
class PeersViewModel @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val peerRepository: PeerRepository,
    private val handshakeManager: HandshakeManager
) : ViewModel() {

    // Discover peers from ConnectionManager (Aggregated)
    private val discoveredPeers = connectionManager.discoverPeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Connection State from ConnectionManager
    private val connectionState = connectionManager.connectionState

    val uiState: StateFlow<PeersUiState> = combine(
        discoveredPeers,
        connectionState
    ) { peers, connState ->
        if (peers.isEmpty() && connState is ConnectionState.Disconnected) {
            PeersUiState.Empty
        } else {
            PeersUiState.Success(
                discoveredPeers = peers,
                connectedPeerId = if (connState is ConnectionState.Connected) "Connected" else null,
                connectionState = connState
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PeersUiState.Loading
    )

    fun connectToPeer(peer: PeerDescriptor) {
        viewModelScope.launch {
            connectionManager.connect(peer)
                .onSuccess {
                    savePeerPlaceholder(peer)
                    // Initiate handshake
                    handshakeManager.initiateHandshake(peer.peerId)
                }
                .onFailure {
                    // Handle error
                }
        }
    }

    fun importPeer(address: String) {
        val descriptor = ChatAddressHelper.parseAddress(address) ?: return
        val keys = ChatAddressHelper.extractKeys(address)

        viewModelScope.launch {
            if (keys != null) {
                // We have their keys!
                val (ik, ek) = keys
                val peer = Peer(
                    id = descriptor.peerId,
                    displayName = descriptor.name,
                    publicKey = PublicKeyBundle(ik, ek, ""),
                    lastSeen = Clock.System.now(),
                    isTrusted = true // Imported via QR/Link implies some trust
                )
                peerRepository.addPeer(peer)
            } else {
                // Just a name/ID, save placeholder
                savePeerPlaceholder(descriptor)
            }
        }
    }

    private suspend fun savePeerPlaceholder(peer: PeerDescriptor) {
        val placeholderPeer = Peer(
            id = peer.peerId,
            displayName = peer.name,
            publicKey = PublicKeyBundle(ByteArray(32), ByteArray(32), ""),
            lastSeen = Clock.System.now(),
            isTrusted = false
        )
        peerRepository.addPeer(placeholderPeer)
    }

    fun disconnect() {
        viewModelScope.launch {
            connectionManager.disconnect()
        }
    }
}
