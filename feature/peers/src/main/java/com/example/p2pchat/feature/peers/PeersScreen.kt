package com.example.p2pchat.feature.peers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.p2pchat.core.model.Peer
import com.example.p2pchat.core.network.ConnectionState
import com.example.p2pchat.core.network.PeerDescriptor

@Composable
fun PeersRoute(
    viewModel: PeersViewModel = hiltViewModel(),
    initialLink: String? = null,
    onNavigateToScan: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialLink) {
        if (initialLink != null) {
            viewModel.importPeer(initialLink)
        }
    }

    PeersScreen(
        uiState = uiState,
        onConnect = viewModel::connectToPeer,
        onDisconnect = viewModel::disconnect,
        onScanQr = onNavigateToScan,
        onPeerClick = onNavigateToDetails
    )
}

@Composable
fun PeersScreen(
    uiState: PeersUiState,
    onConnect: (PeerDescriptor) -> Unit,
    onDisconnect: () -> Unit,
    onScanQr: () -> Unit,
    onPeerClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            PeersUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is PeersUiState.Empty, is PeersUiState.Success -> {
                val successState = uiState as? PeersUiState.Success
                val savedPeers = successState?.savedPeers ?: emptyList()
                val discoveredPeers = successState?.discoveredPeers ?: emptyList()
                val connectionState = successState?.connectionState ?: ConnectionState.Disconnected

                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    item {
                        // Actions
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                            Button(onClick = onScanQr) {
                                Text("Scan QR")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Connection Status
                        Text(
                            text = "Connection Status: ${connectionState.javaClass.simpleName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (connectionState is ConnectionState.Connected) {
                            Button(onClick = onDisconnect) {
                                Text("Disconnect")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                         Text(
                            text = "Saved Peers:",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    if (savedPeers.isEmpty()) {
                        item { Text("No saved peers.") }
                    } else {
                        items(savedPeers) { peer ->
                            SavedPeerItem(peer = peer, onClick = { onPeerClick(peer.id) })
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Discovered Peers:",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    if (discoveredPeers.isEmpty()) {
                        item { Text("No peers found nearby.") }
                    } else {
                        items(discoveredPeers) { peer ->
                            PeerItem(peer = peer, onConnect = onConnect)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeerItem(
    peer: PeerDescriptor,
    onConnect: (PeerDescriptor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onConnect(peer) }
            .padding(16.dp)
    ) {
        Text(text = peer.name, style = MaterialTheme.typography.bodyLarge)
        Text(text = peer.peerId, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SavedPeerItem(
    peer: Peer,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(text = peer.displayName, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (peer.isVerified) "Verified" else "Unverified",
            style = MaterialTheme.typography.bodySmall,
            color = if (peer.isVerified) Color.Green else Color.Red
        )
    }
}
