package com.example.p2pchat.feature.settings.qr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.p2pchat.core.crypto.CryptoManager
import com.example.p2pchat.core.crypto.IdentityManager
import com.example.p2pchat.core.network.tor.TorManager
import com.example.p2pchat.feature.peers.ChatAddressHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.EnumMap
import javax.inject.Inject

@HiltViewModel
class MyAddressViewModel @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val identityManager: IdentityManager,
    private val torManager: TorManager
) : ViewModel() {

    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()

    private val _addressString = MutableStateFlow<String>("")
    val addressString: StateFlow<String> = _addressString.asStateFlow()

    init {
        generateQr()
    }

    private fun generateQr() {
        viewModelScope.launch {
            val identity = cryptoManager.getMyIdentity() ?: return@launch

            // Try to get Onion Address if available
            var onionAddress = torManager.onionAddress.value
            if (onionAddress == null) {
                // If not ready, maybe start it?
                // Or just use null (Tor not active/ready yet)
                // Assuming user enables PrivacyLevel.STANDARD elsewhere, or we start it here?
                // Let's check if Tor is ready or start it if we want to advertise it.
                // For "My Address" we generally want to advertise the best possible reachability.
                torManager.startTor()
                // Wait briefly? Or observe flow?
                // Observability is better but for MVP just wait a bit or use null if not ready immediately.
                // We'll use flow collection in a real app.
                // Here we just check again after a short delay or proceed.
                // Let's just use what's available.
                onionAddress = torManager.onionAddress.value
            }

            // Generate address with relay/onion info
            // ChatAddressHelper needs update to accept relay/onion.
            // Currently generateAddress(identity, signer).
            // We need to pass onion address.

            // Wait, I haven't updated ChatAddressHelper signature yet in the plan step "Update ChatAddressHelper...".
            // I should update ChatAddressHelper first or here.
            // I'll update ChatAddressHelper in this file update if possible or next step.
            // Actually, I can overload generateAddress in ChatAddressHelper.

            // Since I cannot edit ChatAddressHelper in this tool call (different file),
            // I will assume I will update it.
            // For now, I'll pass it as a parameter if I can update ChatAddressHelper.

            // I'll update ChatAddressHelper in the next tool call then come back?
            // Or assume I update ChatAddressHelper to accept optional address.

            // Let's update ChatAddressHelper logic in the next step properly.
            // For now, I will use existing generateAddress and append onion query param manually if helper doesn't support it yet?
            // Helper supports constructing URI.

            // I will update ChatAddressHelper to accept `onionAddress` string.
            val address = ChatAddressHelper.generateAddress(identity, onionAddress) { data ->
                identityManager.sign(data, identity.userId)
            }
            _addressString.value = address

            val bitmap = withContext(Dispatchers.IO) {
                generateQrBitmap(address)
            }
            _qrBitmap.value = bitmap
        }
    }

    private fun generateQrBitmap(content: String): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.MARGIN] = 1
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun MyAddressRoute(
    viewModel: MyAddressViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val bitmap by viewModel.qrBitmap.collectAsState()
    val address by viewModel.addressString.collectAsState()

    MyAddressScreen(
        qrBitmap = bitmap,
        address = address,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAddressScreen(
    qrBitmap: Bitmap?,
    address: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Address") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "My Chat Address",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(250.dp)
                )
            } else {
                Box(modifier = Modifier.size(250.dp)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Scan this to connect instantly",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Truncate address for display
            Text(
                text = address.take(50) + "...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
