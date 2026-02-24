package com.example.p2pchat.feature.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.p2pchat.core.model.Message

@Composable
fun ChatScreen(
    viewModel: MessagingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    ChatContent(
        uiState = uiState,
        onSendMessage = { text, parentId -> viewModel.sendMessage(text, parentId) },
        onTagMessage = { id, topic -> viewModel.tagMessage(id, topic) }
    )
}

@Composable
fun ChatContent(
    uiState: MessagingUiState,
    onSendMessage: (String, String?) -> Unit,
    onTagMessage: (String, String) -> Unit
) {
    var replyingToMessageId by remember { mutableStateOf<String?>(null) }
    var taggingMessageId by remember { mutableStateOf<String?>(null) }

    if (taggingMessageId != null) {
        TagDialog(
            onDismiss = { taggingMessageId = null },
            onTag = { topic ->
                onTagMessage(taggingMessageId!!, topic)
                taggingMessageId = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (uiState) {
                MessagingUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MessagingUiState.Error -> Text("Error: ${uiState.message}", modifier = Modifier.align(Alignment.Center))
                is MessagingUiState.Success -> MessageList(
                    messages = uiState.messages,
                    onReply = { replyingToMessageId = it.id },
                    onTag = { taggingMessageId = it.id }
                )
            }
        }

        if (replyingToMessageId != null) {
            Text(
                text = "Replying to message...",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        MessageInput(
            onSendMessage = { text ->
                onSendMessage(text, replyingToMessageId)
                replyingToMessageId = null
            }
        )
    }
}

@Composable
fun MessageList(
    messages: List<Message>,
    onReply: (Message) -> Unit,
    onTag: (Message) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(messages) { message ->
            MessageItem(message = message, onReply = onReply, onTag = onTag)
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    onReply: (Message) -> Unit,
    onTag: (Message) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (message.senderId == "local_peer") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
            .clickable { /* Show options */ }
            .padding(8.dp)
    ) {
        if (message.parentMessageId != null) {
            Text(
                text = "Replying to...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = message.clearTextCache ?: "[Encrypted]",
            style = MaterialTheme.typography.bodyMedium
        )
        Row {
            Button(onClick = { onReply(message) }, modifier = Modifier.padding(end = 8.dp)) {
                Text("Reply", style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = { onTag(message) }) {
                Text("Tag", style = MaterialTheme.typography.labelSmall)
            }
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

@Composable
fun TagDialog(
    onDismiss: () -> Unit,
    onTag: (String) -> Unit
) {
    var topic by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Topic") },
        text = {
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic (e.g. #work)") }
            )
        },
        confirmButton = {
            Button(onClick = { onTag(topic) }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
