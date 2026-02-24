package com.example.p2pchat.feature.settings.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.network.ConnectionManager
import com.example.p2pchat.core.network.config.PrivacyLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _currentLevel = MutableStateFlow(PrivacyLevel.STANDARD)
    val currentLevel: StateFlow<PrivacyLevel> = _currentLevel.asStateFlow()

    fun setPrivacyLevel(level: PrivacyLevel) {
        _currentLevel.value = level
        // Ideally persist this in preferences
        viewModelScope.launch {
            connectionManager.setPrivacyLevel(level)
        }
    }
}
