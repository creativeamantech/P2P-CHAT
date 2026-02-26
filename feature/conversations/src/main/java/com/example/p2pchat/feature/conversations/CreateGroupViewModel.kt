package com.example.p2pchat.feature.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.model.Peer
import com.example.p2pchat.core.network.group.GroupManager
import com.example.p2pchat.core.storage.repository.PeerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val peerRepository: PeerRepository,
    private val groupManager: GroupManager
) : ViewModel() {

    val peers: StateFlow<List<Peer>> = peerRepository.getAllPeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<CreateGroupUiState>(CreateGroupUiState.Idle)
    val uiState: StateFlow<CreateGroupUiState> = _uiState

    fun createGroup(name: String, selectedPeerIds: List<String>) {
        if (name.isBlank() || selectedPeerIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = CreateGroupUiState.Loading
            try {
                // Include myself implicitly in logic, but here we just pass selected peers
                // GroupManager handles adding me.
                // Wait, GroupManager expects memberIds. Should I include myself?
                // GroupManager logic:
                // val myId = cryptoManager.getMyIdentity()?.userId
                // threadRepository.addParticipant(groupId, myId, "ADMIN")
                // So I don't need to pass myself in memberIds, or I can.
                // GroupManager.createGroup logic:
                // memberIds.forEach { ... if (peerId == myId) return@forEach ... }
                // So passing myself is safe/ignored for distribution.
                // But threadRepository.createGroupThread(name, memberIds) uses memberIds to insert participants.
                // And then GroupManager adds me as ADMIN explicitly.
                // So I should pass OTHER members.

                val groupId = groupManager.createGroup(name, selectedPeerIds)
                _uiState.value = CreateGroupUiState.Success(groupId)
            } catch (e: Exception) {
                _uiState.value = CreateGroupUiState.Error(e.message ?: "Failed to create group")
            }
        }
    }
}

sealed class CreateGroupUiState {
    object Idle : CreateGroupUiState()
    object Loading : CreateGroupUiState()
    data class Success(val groupId: String) : CreateGroupUiState()
    data class Error(val message: String) : CreateGroupUiState()
}
