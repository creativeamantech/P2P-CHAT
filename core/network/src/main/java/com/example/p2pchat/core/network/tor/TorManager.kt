package com.example.p2pchat.core.network.tor

import android.content.Context
import android.util.Log
import com.example.p2pchat.core.crypto.IdentityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.torproject.android.binary.TorResourceInstaller
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class TorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val identityManager: IdentityManager
) {
    private val _isTorReady = MutableStateFlow(false)
    val isTorReady: StateFlow<Boolean> = _isTorReady.asStateFlow()

    private val _onionAddress = MutableStateFlow<String?>(null)
    val onionAddress: StateFlow<String?> = _onionAddress.asStateFlow()

    private var torProcess: Process? = null
    private val isStarting = AtomicBoolean(false)

    // Config
    private val SOCKS_PORT = 9050
    private val HIDDEN_SERVICE_PORT = 47890 // External port peers connect to
    private val LOCAL_PORT = 47890 // Internal port Tor forwards to (our server socket)

    suspend fun startTor() = withContext(Dispatchers.IO) {
        if (_isTorReady.value || isStarting.getAndSet(true)) return@withContext

        try {
            val installResult = installTorResources()
            if (!installResult) {
                Log.e("TorManager", "Failed to install Tor resources")
                isStarting.set(false)
                return@withContext
            }

            val torConfig = createTorConfig()
            val torBinary = File(getTorInstallDir(), "tor")

            // Make executable
            torBinary.setExecutable(true)

            val cmd = listOf(
                torBinary.absolutePath,
                "-f", torConfig.absolutePath
            )

            val pb = ProcessBuilder(cmd)
            val env = pb.environment()
            env["HOME"] = getTorInstallDir().absolutePath // Tor needs HOME

            torProcess = pb.start()

            // Monitor startup (simplified: wait and check/read logs in real app)
            // Ideally use jtorctl/TorControlConnection to wait for bootstrap.
            // For MVP, we assume success after a delay and if process is alive.
            // In real world, we connect to Control Port 9051 and listen for events.

            // Wait for bootstrap (mock wait for now as implementing full JTorCtl listener is complex in one file)
            // But we need the onion address.

            // Wait for hostname file creation
            val hostnameFile = File(getHiddenServiceDir(), "hostname")
            var attempts = 0
            while (!hostnameFile.exists() && attempts < 20) {
                Thread.sleep(1000)
                attempts++
            }

            if (hostnameFile.exists()) {
                val address = hostnameFile.readText().trim()
                _onionAddress.value = address
                Log.d("TorManager", "Onion Address: $address")
                _isTorReady.value = true
            } else {
                Log.e("TorManager", "Hidden service hostname not created")
            }

        } catch (e: Exception) {
            Log.e("TorManager", "Error starting Tor", e)
        } finally {
            isStarting.set(false)
        }
    }

    suspend fun stopTor() = withContext(Dispatchers.IO) {
        torProcess?.destroy()
        torProcess = null
        _isTorReady.value = false
        _onionAddress.value = null
    }

    private fun installTorResources(): Boolean {
        val installDir = getTorInstallDir()
        return try {
            val installer = TorResourceInstaller(context, installDir)
            installer.installResources()
        } catch (e: Exception) {
            Log.e("TorManager", "Install failed", e)
            false
        }
    }

    private fun getTorInstallDir(): File {
        return context.getDir("tor", Context.MODE_PRIVATE)
    }

    private fun getHiddenServiceDir(): File {
        val dir = File(getTorInstallDir(), "hs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun createTorConfig(): File {
        val torrc = File(getTorInstallDir(), "torrc")
        val dataDir = File(getTorInstallDir(), "data")
        if (!dataDir.exists()) dataDir.mkdirs()

        val hsDir = getHiddenServiceDir()

        val config = """
            DataDirectory ${dataDir.absolutePath}
            SocksPort $SOCKS_PORT
            ControlPort 9051

            # Hidden Service
            HiddenServiceDir ${hsDir.absolutePath}
            HiddenServicePort $HIDDEN_SERVICE_PORT 127.0.0.1:$LOCAL_PORT

            # Logging
            Log notice stdout
        """.trimIndent()

        torrc.writeText(config)
        return torrc
    }

    fun getSocksProxyHost(): String = "127.0.0.1"
    fun getSocksProxyPort(): Int = SOCKS_PORT
    fun getLocalServerPort(): Int = LOCAL_PORT
}
