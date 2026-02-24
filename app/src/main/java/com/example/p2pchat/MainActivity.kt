package com.example.p2pchat

import android.content.Intent
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.p2pchat.feature.conversations.ConversationsRoute
import com.example.p2pchat.feature.messaging.ChatScreen
import com.example.p2pchat.feature.peers.PeerDetailsScreen
import com.example.p2pchat.feature.peers.PeersRoute
import com.example.p2pchat.feature.peers.scan.ScanQrScreen
import com.example.p2pchat.feature.settings.CreateIdentityRoute
import dagger.hilt.android.AndroidEntryPoint
import com.example.p2pchat.feature.peers.ChatAddressHelper
import com.example.p2pchat.feature.peers.PeersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val initialLink = intent?.data?.toString()
            P2PChatAppContent(initialLink = initialLink)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
                    },
                    onNavigateToPeers = {
                        navController.navigate("peers")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            composable("chat/{threadId}") { backStackEntry ->
                ChatScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("peers") {
                PeersRoute(
                    initialLink = initialLink,
                    onNavigateToScan = {
                        navController.navigate("scan")
                    },
                    onNavigateToDetails = { peerId ->
                        navController.navigate("peer_details/$peerId")
                    }
                )
            }
            composable("scan") {
                ScanQrScreen(
                    onQrScanned = { link ->
                        val intent = Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                        intent.setPackage(navController.context.packageName)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        navController.context.startActivity(intent)
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = "peer_details/{peerId}",
                arguments = listOf(navArgument("peerId") { type = NavType.StringType })
            ) {
                PeerDetailsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("settings") {
                com.example.p2pchat.feature.settings.qr.MyAddressRoute(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
