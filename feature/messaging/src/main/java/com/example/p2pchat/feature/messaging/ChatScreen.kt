package com.example.p2pchat.feature.messaging

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ChatScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Chat Screen", modifier = Modifier.align(Alignment.Center))
    }
}
