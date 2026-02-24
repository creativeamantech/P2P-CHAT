package com.example.p2pchat.feature.conversations.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.p2pchat.core.model.Message

@Composable
fun SearchRoute(
    viewModel: SearchViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    SearchScreen(
        query = query,
        onQueryChanged = viewModel::onQueryChanged,
        uiState = uiState,
        onNavigateToChat = onNavigateToChat
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChanged: (String) -> Unit,
    uiState: SearchUiState,
    onNavigateToChat: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChanged,
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search messages...") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            // No content in active view for now
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                SearchUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                SearchUiState.Empty -> Text("No results found", modifier = Modifier.align(Alignment.Center))
                SearchUiState.Idle -> Text("Type to search", modifier = Modifier.align(Alignment.Center))
                is SearchUiState.Success -> {
                    LazyColumn {
                        items(uiState.results) { message ->
                            Text(
                                text = message.clearTextCache ?: "",
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
