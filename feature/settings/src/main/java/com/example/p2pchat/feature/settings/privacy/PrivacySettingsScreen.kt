package com.example.p2pchat.feature.settings.privacy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.p2pchat.core.network.config.PrivacyLevel

@Composable
fun PrivacySettingsRoute(
    onBackClick: () -> Unit,
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val currentLevel by viewModel.currentLevel.collectAsState()

    PrivacySettingsScreen(
        currentLevel = currentLevel,
        onLevelSelected = viewModel::setPrivacyLevel,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    currentLevel: PrivacyLevel,
    onLevelSelected: (PrivacyLevel) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item {
                Text(
                    text = "Connection Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            item {
                PrivacyOption(
                    level = PrivacyLevel.BASIC,
                    selected = currentLevel == PrivacyLevel.BASIC,
                    title = "Basic",
                    description = "Direct connection. Fastest. Peer sees your IP.",
                    onSelect = onLevelSelected
                )
                PrivacyOption(
                    level = PrivacyLevel.STANDARD,
                    selected = currentLevel == PrivacyLevel.STANDARD,
                    title = "Standard (Recommended)",
                    description = "Padding enables size masking. ~300ms latency.",
                    onSelect = onLevelSelected
                )
                PrivacyOption(
                    level = PrivacyLevel.HIGH,
                    selected = currentLevel == PrivacyLevel.HIGH,
                    title = "High",
                    description = "Mix Network hides timing. ~1-3s latency.",
                    onSelect = onLevelSelected
                )
                PrivacyOption(
                    level = PrivacyLevel.MAXIMUM,
                    selected = currentLevel == PrivacyLevel.MAXIMUM,
                    title = "Maximum",
                    description = "Cover Traffic hides patterns. High battery usage.",
                    onSelect = onLevelSelected
                )
            }
        }
    }
}

@Composable
fun PrivacyOption(
    level: PrivacyLevel,
    selected: Boolean,
    title: String,
    description: String,
    onSelect: (PrivacyLevel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = { onSelect(level) }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = { onSelect(level) }
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
