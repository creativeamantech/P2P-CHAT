package com.example.p2pchat.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.p2pchat.core.storage.entity.IdentityEntity

@Composable
fun IdentityListRoute(
    onNavigateToCreate: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: IdentityListViewModel = hiltViewModel()
) {
    val identities by viewModel.identities.collectAsState()
    val activeId by viewModel.activeIdentityId.collectAsState()

    IdentityListScreen(
        identities = identities,
        activeId = activeId,
        onSetActive = viewModel::setActiveIdentity,
        onBurn = viewModel::burnIdentity,
        onNavigateToCreate = onNavigateToCreate,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityListScreen(
    identities: List<IdentityEntity>,
    activeId: String?,
    onSetActive: (String) -> Unit,
    onBurn: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Identities") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Default.Add, contentDescription = "Create Identity")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (identities.isEmpty()) {
                Text("No identities found.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(identities) { identity ->
                        IdentityCard(
                            identity = identity,
                            isActive = identity.id == activeId,
                            onClick = { onSetActive(identity.id) },
                            onBurn = { onBurn(identity.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IdentityCard(
    identity: IdentityEntity,
    isActive: Boolean,
    onClick: () -> Unit,
    onBurn: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = !identity.isBurned, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = identity.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (identity.isBurned) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Type: ${identity.type}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (isActive) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (identity.isBurned) {
                    Text(
                        text = "BURNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (!identity.isBurned && identity.type == "BURNER") {
                IconButton(onClick = onBurn) {
                    Icon(Icons.Default.Delete, contentDescription = "Burn", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
