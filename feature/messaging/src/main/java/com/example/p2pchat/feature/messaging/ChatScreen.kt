package com.example.p2pchat.feature.messaging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.p2pchat.core.model.Message

@Composable
fun ChatScreen(
    viewModel: MessagingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    ChatContent(uiState = uiState, onSendMessage = viewModel::sendMessage)
}

@Composable
fun ChatContent(
    uiState: MessagingUiState,
    onSendMessage: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (uiState) {
                MessagingUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MessagingUiState.Error -> Text("Error: ${uiState.message}", modifier = Modifier.align(Alignment.Center))
                is MessagingUiState.Success -> MessageList(messages = uiState.messages)
            }
        }

        MessageInput(onSendMessage = onSendMessage)
    }
}

@Composable
fun MessageList(messages: List<Message>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(messages) { message ->
            Text(
                text = message.clearTextCache ?: "[Encrypted]",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MessageInput(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = {
                onSendMessage(text)
                text = ""
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Send")
        }
    }
}
