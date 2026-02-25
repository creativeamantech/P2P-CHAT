package com.example.p2pchat

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        requestPermissionLauncher.launch(permissions.toTypedArray())

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

import androidx.compose.runtime.LaunchedEffect

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
        // Handle deep link navigation once graph is ready
        LaunchedEffect(initialLink) {
            if (initialLink != null) {
                // Ensure we don't navigate if already there or if graph issues?
                // Compose navigation handles repeated navigation safely usually.
                navController.navigate("peers")
            }
        }

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
            composable("peers") { entry ->
                // Check for QR scan result
                val qrResult = entry.savedStateHandle.get<String>("qr_result")
                if (qrResult != null) {
                    entry.savedStateHandle.remove<String>("qr_result")
                }

                PeersRoute(
                    initialLink = qrResult ?: initialLink,
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
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("qr_result", link)
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
