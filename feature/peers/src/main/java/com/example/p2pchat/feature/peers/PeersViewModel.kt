package com.example.p2pchat.feature.peers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.model.Peer
import com.example.p2pchat.core.model.PublicKeyBundle
import com.example.p2pchat.core.network.ConnectionManager
import com.example.p2pchat.core.crypto.IdentityManager
import com.example.p2pchat.core.network.ConnectionState
import com.example.p2pchat.core.network.HandshakeManager
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
        val savedPeers: List<Peer>,
        val connectedPeerId: String?,
        val connectionState: ConnectionState
    ) : PeersUiState
    object Empty : PeersUiState
}

@HiltViewModel
class PeersViewModel @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val peerRepository: PeerRepository,
    private val handshakeManager: HandshakeManager,
    private val identityManager: IdentityManager
) : ViewModel() {

    // Discover peers from ConnectionManager (Aggregated)
    private val discoveredPeers = connectionManager.discoverPeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Saved peers from DB
    private val savedPeers = peerRepository.getAllPeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Connection State from ConnectionManager
    private val connectionState = connectionManager.connectionState

    val uiState: StateFlow<PeersUiState> = combine(
        discoveredPeers,
        savedPeers,
        connectionState
    ) { discovered, saved, connState ->
        if (discovered.isEmpty() && saved.isEmpty() && connState is ConnectionState.Disconnected) {
            PeersUiState.Empty
        } else {
            PeersUiState.Success(
                discoveredPeers = discovered,
                savedPeers = saved,
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
            val result = connectionManager.connect(peer)
            if (result.isSuccess) {
                savePeerPlaceholder(peer)
                // Initiate handshake
                handshakeManager.initiateHandshake(peer.peerId)
            }
        }
    }

    fun importPeer(address: String) {
        val descriptor = ChatAddressHelper.parseAddress(address) ?: return

        // Verify signature if present
        val isSignatureValid = if (descriptor.signature != null && descriptor.identityKey != null) {
            ChatAddressHelper.verifySignature(address) { data, sig, key ->
                identityManager.verify(data, sig, key)
            }
        } else {
            false
        }

        viewModelScope.launch {
            if (descriptor.identityKey != null && descriptor.exchangeKey != null && isSignatureValid) {
                // We have their keys and signature is valid!
                val peer = Peer(
                    id = descriptor.peerId,
                    displayName = descriptor.name,
                    publicKey = PublicKeyBundle(descriptor.identityKey!!, descriptor.exchangeKey!!, ""),
                    lastSeen = Clock.System.now(),
                    isTrusted = true,
                    isVerified = true // Verified because signature matches identity key (Self-authenticating)
                )
                peerRepository.addPeer(peer)

                // Also update descriptor in ConnectionManager if needed?
                // ConnectionManager uses PeerRepository mostly.

                // Try to connect immediately?
                connectToPeer(descriptor)
            } else if (descriptor.identityKey != null) {
                 // Keys present but signature invalid/missing
                 // Treat as unverified
                 val peer = Peer(
                    id = descriptor.peerId,
                    displayName = descriptor.name,
                    publicKey = PublicKeyBundle(descriptor.identityKey!!, descriptor.exchangeKey ?: ByteArray(32), ""),
                    lastSeen = Clock.System.now(),
                    isTrusted = false,
                    isVerified = false
                )
                peerRepository.addPeer(peer)
            } else {
                // Just a name/ID, save placeholder
                savePeerPlaceholder(descriptor)
            }
        }
    }

    private suspend fun savePeerPlaceholder(peer: PeerDescriptor) {
        // Check if exists
        if (peerRepository.getPeer(peer.peerId) != null) return

        val placeholderPeer = Peer(
            id = peer.peerId,
            displayName = peer.name,
            publicKey = PublicKeyBundle(ByteArray(32), ByteArray(32), ""),
            lastSeen = Clock.System.now(),
            isTrusted = false,
            isVerified = false
        )
        peerRepository.addPeer(placeholderPeer)
    }

    fun disconnect() {
        viewModelScope.launch {
            connectionManager.disconnect()
        }
    }
}
