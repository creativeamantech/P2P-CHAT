package com.example.p2pchat.feature.conversations

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
fun ConversationsRoute(
    viewModel: ConversationsViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    ConversationsScreen(uiState = uiState, onNavigateToChat = onNavigateToChat)
}

@Composable
fun ConversationsScreen(
    uiState: ConversationsUiState,
    onNavigateToChat: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                                .padding(16.dp)
                                .padding(vertical = 8.dp)
                        )
                        // Add click listener
                    }
                }
            }
        }
    }
}
