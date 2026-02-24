package com.example.p2pchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.p2pchat.feature.conversations.ConversationsRoute
import com.example.p2pchat.feature.messaging.ChatScreen
import com.example.p2pchat.feature.peers.PeersRoute
import com.example.p2pchat.feature.settings.CreateIdentityRoute
import dagger.hilt.android.AndroidEntryPoint

import android.content.Intent
import com.example.p2pchat.feature.peers.ChatAddressHelper
import com.example.p2pchat.feature.peers.PeersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Inject PeersViewModel to handle new peer addition directly if needed,
    // or we can just rely on the UI to handle it via navigation arguments?
    // ViewModel is typically scoped to navigation graph or activity.
    // Let's use a MainViewModel or just handle intent in onCreate and pass to Compose.

    // Better: Handle in MainViewModel which is already injected.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle Deep Link
        val data = intent?.data
        if (data != null && data.scheme == "p2pchat" && data.host == "peer") {
            // Process in ViewModel
            // But MainViewModel is for startup logic.
            // We should navigate to Peers screen or a "New Peer" dialog.
        }

        setContent {
            P2PChatAppContent(initialLink = intent?.data?.toString())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Re-compose or handle new intent
    }
}

@Composable
fun P2PChatAppContent(
    viewModel: MainViewModel = hiltViewModel(),
    initialLink: String? = null
) {
    val navController = rememberNavController()
    val startDestination by viewModel.startDestination.collectAsState()

    // TODO: Handle initialLink in PeersViewModel or similar
    // For now, we just let the app start.

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    } else {
        NavHost(navController = navController, startDestination = startDestination!!) {
            composable("create_identity") {
                CreateIdentityRoute(
                    onIdentityCreated = {
                        navController.navigate("conversations") {
                            popUpTo("create_identity") { inclusive = true }
                        }
                    }
                )
            }
            composable("conversations") {
                ConversationsRoute(
                    onNavigateToChat = { threadId ->
                        navController.navigate("chat/$threadId")
                    }
                )
            }
            composable("chat/{threadId}") { backStackEntry ->
                ChatScreen()
            }
            composable("peers") {
                PeersRoute()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    P2PChatAppContent()
}
