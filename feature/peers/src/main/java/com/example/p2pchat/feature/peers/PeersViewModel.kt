package com.example.p2pchat.feature.peers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.model.Peer
import com.example.p2pchat.core.model.PublicKeyBundle
import com.example.p2pchat.core.network.ConnectionState
import com.example.p2pchat.core.network.P2PTransport
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
    private val transport: P2PTransport,
    private val peerRepository: PeerRepository
) : ViewModel() {

    // Discover peers from Transport (Network)
    private val discoveredPeers = transport.discoverPeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Connection State from Transport
    private val connectionState = transport.connectionState

    val uiState: StateFlow<PeersUiState> = combine(
        discoveredPeers,
        connectionState
    ) { peers, connState ->
        if (peers.isEmpty() && connState is ConnectionState.Disconnected) {
            PeersUiState.Empty
        } else {
            PeersUiState.Success(
                discoveredPeers = peers,
                connectedPeerId = if (connState is ConnectionState.Connected) "Connected" else null, // Simplified
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
            transport.connect(peer)
                .onSuccess {
                    // On success, we might want to save this peer to DB as a "known peer"
                    // But for now, we just rely on connection state
                    // In a real app, we'd exchange keys here.
                    savePeerPlaceholder(peer)
                }
                .onFailure {
                    // Handle error
                }
        }
    }

    private suspend fun savePeerPlaceholder(peer: PeerDescriptor) {
        // We don't have their keys yet until we exchange messages or handshake.
        // For MVP, we'll create a dummy peer entry so we can start a thread.
        val placeholderPeer = Peer(
            id = peer.peerId,
            displayName = peer.name,
            publicKey = PublicKeyBundle(ByteArray(32), ByteArray(32), ""), // Dummy keys
            lastSeen = Clock.System.now(),
            isTrusted = false
        )
        peerRepository.addPeer(placeholderPeer)
    }

    fun disconnect() {
        viewModelScope.launch {
            transport.disconnect()
        }
    }
}
