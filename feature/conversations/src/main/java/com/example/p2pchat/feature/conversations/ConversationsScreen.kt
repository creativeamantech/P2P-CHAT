package com.example.p2pchat.feature.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.material.icons.filled.List

@Composable
fun ConversationsRoute(
    viewModel: ConversationsViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit,
    onNavigateToPeers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTopics: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    ConversationsScreen(
        uiState = uiState,
        onNavigateToChat = onNavigateToChat,
        onNavigateToPeers = onNavigateToPeers,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToTopics = onNavigateToTopics
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    uiState: ConversationsUiState,
    onNavigateToChat: (String) -> Unit,
    onNavigateToPeers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTopics: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (!active) {
                TopAppBar(
                    title = { Text("P2P Chat") },
                    actions = {
                        IconButton(onClick = { active = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onNavigateToTopics) {
                            Icon(Icons.Default.List, contentDescription = "Topics")
                        }
                        IconButton(onClick = onNavigateToPeers) {
                            Icon(Icons.Default.Person, contentDescription = "Peers")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            } else {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { /* Handle search */ },
                    active = active,
                    onActiveChange = { active = it },
                    placeholder = { Text("Search messages...") }
                ) {
                    // Search results would go here.
                    // For MVP, we need ViewModel support for search.
                    // Assuming ViewModel handles it if we pass query?
                    // Skipping detailed implementation for now as ViewModel update is needed.
                    Text("Search functionality coming soon", modifier = Modifier.padding(16.dp))
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState) {
                ConversationsUiState.Loading -> {
                    Text("Loading...", modifier = Modifier.align(Alignment.Center))
                }
                ConversationsUiState.Empty -> {
                    Text("No conversations yet", modifier = Modifier.align(Alignment.Center))
                }
                is ConversationsUiState.Success -> {
                    LazyColumn {
                        items(uiState.conversations) { thread ->
                            Text(
                                text = thread.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onNavigateToChat(thread.id) }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
