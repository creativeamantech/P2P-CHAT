package com.example.p2pchat.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.crypto.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateIdentityViewModel @Inject constructor(
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateIdentityUiState>(CreateIdentityUiState.Idle)
    val uiState: StateFlow<CreateIdentityUiState> = _uiState.asStateFlow()

    fun onCreateIdentity(displayName: String) {
        if (displayName.isBlank()) return

        viewModelScope.launch {
            _uiState.value = CreateIdentityUiState.Loading
            try {
                cryptoManager.generateNewIdentity(displayName)
                _uiState.value = CreateIdentityUiState.Success
            } catch (e: Exception) {
                _uiState.value = CreateIdentityUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface CreateIdentityUiState {
    object Idle : CreateIdentityUiState
    object Loading : CreateIdentityUiState
    object Success : CreateIdentityUiState
    data class Error(val message: String) : CreateIdentityUiState
}
