package com.example.p2pchat.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.crypto.CryptoManager
import com.example.p2pchat.core.crypto.IdentityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateIdentityViewModel @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val identityManager: IdentityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateIdentityUiState>(CreateIdentityUiState.Idle)
    val uiState: StateFlow<CreateIdentityUiState> = _uiState

    fun onCreateIdentity(displayName: String, type: String = "PERMANENT") {
        viewModelScope.launch {
            _uiState.value = CreateIdentityUiState.Loading
            try {
                // Legacy logic fallback for backwards compatibility where needed
                if (type == "PERMANENT") {
                    cryptoManager.generateNewIdentity(displayName)
                }

                val identity = identityManager.generateIdentity(displayName, type)
                identityManager.setActiveIdentity(identity.id)

                _uiState.value = CreateIdentityUiState.Success
            } catch (e: Exception) {
                _uiState.value = CreateIdentityUiState.Error(e.message ?: "Failed to create identity")
            }
        }
    }
}

sealed class CreateIdentityUiState {
    object Idle : CreateIdentityUiState()
    object Loading : CreateIdentityUiState()
    object Success : CreateIdentityUiState()
    data class Error(val message: String) : CreateIdentityUiState()
}
