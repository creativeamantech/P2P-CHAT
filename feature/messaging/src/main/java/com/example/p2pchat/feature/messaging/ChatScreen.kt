package com.example.p2pchat.feature.messaging

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import coil.compose.AsyncImage
import com.example.p2pchat.core.model.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MessagingViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val threadInfo by viewModel.threadInfo.collectAsState()
    var showTimerDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendImage(uri)
        }
    }

    if (showTimerDialog) {
        TimerDialog(
            currentTimer = threadInfo?.defaultExpiration ?: 0,
            onDismiss = { showTimerDialog = false },
            onSetTimer = { seconds ->
                viewModel.setDisappearingTimer(seconds)
                showTimerDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chat")
                        if ((threadInfo?.defaultExpiration ?: 0) > 0) {
                            Text(
                                "Disappearing: ${formatTimer(threadInfo?.defaultExpiration ?: 0)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTimerDialog = true }) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Set Timer",
                            tint = if ((threadInfo?.defaultExpiration ?: 0) > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        ChatContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onSendMessage = { text, parentId -> viewModel.sendMessage(text, parentId) },
            onTagMessage = { id, topic -> viewModel.tagMessage(id, topic) },
            onPickImage = { imagePickerLauncher.launch("image/*") }
        )
    }
}

@Composable
fun ChatContent(
    modifier: Modifier = Modifier,
    uiState: MessagingUiState,
    onSendMessage: (String, String?) -> Unit,
    onTagMessage: (String, String) -> Unit,
    onPickImage: () -> Unit
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

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (uiState) {
                MessagingUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MessagingUiState.Error -> Text("Error: ${uiState.message}", modifier = Modifier.align(Alignment.Center))
                is MessagingUiState.Success -> {
                    val messages = uiState.messages.collectAsLazyPagingItems()
                    MessageList(
                        messages = messages,
                        onReply = { replyingToMessageId = it.id },
                        onTag = { taggingMessageId = it.id }
                    )
                }
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
            },
            onPickImage = onPickImage
        )
    }
}

@Composable
fun MessageList(
    messages: LazyPagingItems<Message>,
    onReply: (Message) -> Unit,
    onTag: (Message) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(messages) { message ->
            if (message != null) {
                MessageItem(message = message, onReply = onReply, onTag = onTag)
            }
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

        // Image Attachments
        if (message.attachments.isNotEmpty()) {
            message.attachments.forEach { attachment ->
                if (attachment.type.startsWith("image/")) {
                    AsyncImage(
                        model = attachment.uri,
                        contentDescription = "Image attachment",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
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
fun MessageInput(
    onSendMessage: (String) -> Unit,
    onPickImage: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPickImage) {
            Icon(Icons.Default.Add, contentDescription = "Add Attachment")
        }
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

@Composable
fun TimerDialog(
    currentTimer: Int,
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    val options = listOf(
        0 to "Off",
        30 to "30 Seconds",
        300 to "5 Minutes",
        3600 to "1 Hour",
        86400 to "1 Day"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disappearing Messages") },
        text = {
            Column {
                options.forEach { (seconds, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetTimer(seconds) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentTimer == seconds) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatTimer(seconds: Int): String {
    return when {
        seconds < 60 -> "$seconds s"
        seconds < 3600 -> "${seconds / 60} m"
        seconds < 86400 -> "${seconds / 3600} h"
        else -> "${seconds / 86400} d"
    }
}
