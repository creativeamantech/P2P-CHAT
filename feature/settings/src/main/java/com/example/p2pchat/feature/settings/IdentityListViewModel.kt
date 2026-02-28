package com.example.p2pchat.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.crypto.IdentityManager
import com.example.p2pchat.core.storage.entity.IdentityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdentityListViewModel @Inject constructor(
    private val identityManager: IdentityManager
) : ViewModel() {

    // Usually we would observe DB. Assuming IdentityManager.getAllIdentities() returns Flow
    val identities = kotlinx.coroutines.flow.flow {
        identityManager.getAllIdentities().collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeIdentityId = identityManager.activeIdentityId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setActiveIdentity(id: String) {
        viewModelScope.launch {
            identityManager.setActiveIdentity(id)
        }
    }

    fun burnIdentity(id: String) {
        viewModelScope.launch {
            identityManager.burnIdentity(id)
        }
    }
}
