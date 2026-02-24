package com.example.p2pchat.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.p2pchat.core.storage.entity.IdentityEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityListScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val identities by viewModel.identities.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identities") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.createBurnerIdentity() }) {
                Icon(Icons.Default.Add, contentDescription = "Create Burner")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            items(identities) { identity ->
                IdentityItem(
                    identity = identity,
                    onBurn = { viewModel.burnIdentity(identity.id) }
                )
            }
        }
    }
}

@Composable
fun IdentityItem(
    identity: IdentityEntity,
    onBurn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = identity.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = identity.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (identity.type == "BURNER") Color.Red else Color.Unspecified
                )
            }
            if (identity.type == "BURNER") {
                IconButton(onClick = onBurn) {
                    Icon(Icons.Default.Delete, contentDescription = "Burn", tint = Color.Red)
                }
            }
        }
    }
}
