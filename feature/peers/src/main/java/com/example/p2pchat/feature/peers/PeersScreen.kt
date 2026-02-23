package com.example.p2pchat.feature.peers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PeersRoute(
    viewModel: PeersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    PeersScreen(uiState = uiState)
}

@Composable
fun PeersScreen(
    uiState: PeersUiState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            PeersUiState.Loading -> {
                Text("Loading...", modifier = Modifier.align(Alignment.Center))
            }
            PeersUiState.Empty -> {
                Text("No peers found", modifier = Modifier.align(Alignment.Center))
            }
            is PeersUiState.Success -> {
                LazyColumn {
                    items(uiState.peers) { peer ->
                        Text(
                            text = peer.displayName,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
