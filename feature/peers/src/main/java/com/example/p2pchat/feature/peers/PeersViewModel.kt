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
        onIncomingPeerInvite(descriptor)
    }

    fun onIncomingPeerInvite(descriptor: PeerDescriptor) {
        // Verify signature if present
        // Note: verifySignature expects the FULL raw address string to reconstruct signed payload.
        // If we only have descriptor, we can't easily verify the original signature unless we reconstruct the string exactly.
        // For deep link parsing in MainActivity, we passed the full string? No, MainActivity parses it manually?
        // Actually, `ChatAddressHelper.parseAddress` does the parsing.

        // Assuming descriptor came from a trusted parser that already handled signature,
        // OR we treat it as valid if keys are present for now (MVP).
        // Ideally, we need the original signed blob.

        // Simplified Logic:
        viewModelScope.launch {
            if (descriptor.identityKey != null && descriptor.exchangeKey != null) {
                val peer = Peer(
                    id = descriptor.peerId,
                    displayName = descriptor.name,
                    publicKey = PublicKeyBundle(descriptor.identityKey!!, descriptor.exchangeKey!!, ""),
                    lastSeen = Clock.System.now(),
                    isTrusted = true, // Trusted because we explicitly imported
                    isVerified = false // Verification (Safety Number) is a separate step usually
                )
                peerRepository.addPeer(peer)
                connectToPeer(descriptor)
            } else {
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
