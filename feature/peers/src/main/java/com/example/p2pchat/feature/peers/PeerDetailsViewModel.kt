package com.example.p2pchat.feature.peers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.crypto.CryptoManager
import com.example.p2pchat.core.crypto.FingerprintGenerator
import com.example.p2pchat.core.model.Peer
import com.example.p2pchat.core.storage.repository.PeerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PeerDetailsUiState(
    val peer: Peer? = null,
    val safetyNumber: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class PeerDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val peerRepository: PeerRepository,
    private val cryptoManager: CryptoManager,
    private val fingerprintGenerator: FingerprintGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeerDetailsUiState())
    val uiState: StateFlow<PeerDetailsUiState> = _uiState.asStateFlow()

    private val peerId: String = checkNotNull(savedStateHandle["peerId"])

    init {
        loadPeerDetails()
    }

    private fun loadPeerDetails() {
        viewModelScope.launch {
            val peer = peerRepository.getPeer(peerId) // using getPeer instead of getPeerById if not available, but I added it.
            if (peer != null) {
                val myIdentity = cryptoManager.getMyIdentity()
                val safetyNumber = if (myIdentity != null) {
                    fingerprintGenerator.generateSafetyNumber(
                        myIdentity.ed25519PublicKey,
                        peer.publicKey.identityKey
                    )
                } else {
                    null
                }

                _uiState.value = PeerDetailsUiState(
                    peer = peer,
                    safetyNumber = safetyNumber,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleVerification() {
        viewModelScope.launch {
            val currentPeer = _uiState.value.peer ?: return@launch
            val newStatus = !currentPeer.isVerified
            peerRepository.updateVerificationStatus(peerId, newStatus)
            // Reload to reflect changes
            val updatedPeer = peerRepository.getPeer(peerId)
            _uiState.value = _uiState.value.copy(peer = updatedPeer)
        }
    }
}
