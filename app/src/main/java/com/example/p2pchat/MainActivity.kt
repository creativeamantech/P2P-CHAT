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
import com.example.p2pchat.feature.peers.scan.ScanQrScreen
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
                PeersRoute(
                    initialLink = initialLink,
                    onNavigateToScan = {
                        navController.navigate("scan")
                    }
                )
            }
            composable("scan") {
                ScanQrScreen(
                    onQrScanned = { link ->
                        // Navigate back to peers with the link to import
                        // Since we can't pass args to existing screen easily via popBackStack with result in this simplified setup,
                        // we'll navigate to peers with argument or use a shared viewmodel?
                        // For MVP, navigate to peers with link as argument.
                        // But current PeersRoute uses 'initialLink' which is from Intent.
                        // Let's modify PeersRoute to accept link from nav arg too?
                        // Or simpler: handle import in ScanQrScreen?
                        // ScanQrScreen shouldn't have business logic.

                        // Let's just navigate to Peers with "link={link}" (need to update route)
                        // Or for MVP, since PeersRoute handles 'initialLink' which is passed from MainActivity param,
                        // we can't easily change it here.

                        // Workaround: We'll just handle the import call here via a SideEffect if we had a shared VM.
                        // Or we update PeersRoute to use SavedStateHandle?

                        // Simplest:
                        // launch Intent(ACTION_VIEW, Uri.parse(link))
                        // This will trigger MainActivity.onNewIntent -> setIntent -> Recompose with new initialLink.
                        val intent = Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                        intent.setPackage(navController.context.packageName) // Keep inside app
                        navController.context.startActivity(intent)
                        // And pop this scan screen
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    P2PChatAppContent()
}
