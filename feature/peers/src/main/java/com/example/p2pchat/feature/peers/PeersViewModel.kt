package com.example.p2pchat.feature.peers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.model.Peer
import com.example.p2pchat.core.storage.repository.PeerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface PeersUiState {
    object Loading : PeersUiState
    data class Success(val peers: List<Peer>) : PeersUiState
    object Empty : PeersUiState
}

@HiltViewModel
class PeersViewModel @Inject constructor(
    private val peerRepository: PeerRepository
) : ViewModel() {

    val uiState: StateFlow<PeersUiState> = peerRepository.getAllPeers()
        .map { peers ->
            if (peers.isEmpty()) {
                PeersUiState.Empty
            } else {
                PeersUiState.Success(peers)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PeersUiState.Loading
        )
}
