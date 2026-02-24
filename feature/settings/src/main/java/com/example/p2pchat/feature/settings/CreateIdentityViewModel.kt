package com.example.p2pchat.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.crypto.CryptoManager
import com.example.p2pchat.core.crypto.IdentityManager
import com.example.p2pchat.core.storage.entity.IdentityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateIdentityViewModel @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val identityManager: IdentityManager
) : ViewModel() {

    fun createIdentity(displayName: String) {
        viewModelScope.launch {
            // Legacy support
            cryptoManager.generateNewIdentity(displayName)

            // New multi-identity support
            identityManager.createIdentity(displayName, "PERMANENT")
        }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val identityManager: IdentityManager
) : ViewModel() {

    val identities: StateFlow<List<IdentityEntity>> = identityManager.getAllActiveIdentities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createBurnerIdentity() {
        viewModelScope.launch {
            identityManager.createIdentity("Anonymous", "BURNER", System.currentTimeMillis() + 86400000)
        }
    }

    fun burnIdentity(id: String) {
        viewModelScope.launch {
            identityManager.burnIdentity(id)
        }
    }
}
