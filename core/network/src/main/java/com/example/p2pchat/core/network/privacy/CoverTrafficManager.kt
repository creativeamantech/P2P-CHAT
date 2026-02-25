package com.example.p2pchat.core.network.privacy

import com.example.p2pchat.core.crypto.ratchet.RatchetManager
import com.example.p2pchat.core.network.EncryptedPayload
import com.example.p2pchat.core.network.P2PTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class CoverTrafficManager @Inject constructor(
    private val ratchetManager: RatchetManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private val activeJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    // Dummy marker: 4 bytes 0xDE
    private val DUMMY_MARKER = ByteArray(4) { 0xDE.toByte() }

    fun startCoverTraffic(peerId: String, transport: P2PTransport) {
        if (activeJobs.containsKey(peerId)) return

        val job = scope.launch {
            while (isActive) {
                try {
                    // Generate random data for dummy message
                    val dummyContent = ByteArray(512)
                    Random.nextBytes(dummyContent)

                    // Prepend marker
                    val plaintext = DUMMY_MARKER + dummyContent

                    // Encrypt using Ratchet (advances state, making it valid traffic)
                    val encryptedData = ratchetManager.encrypt(peerId, plaintext)

                    // Send
                    transport.send(EncryptedPayload(encryptedData, null), peerId)
                } catch (e: Exception) {
                    // Ignore errors (e.g. no session yet)
                }

                // Random delay
                delay(Random.nextLong(2000, 10000))
            }
        }
        activeJobs[peerId] = job
    }

    fun stopCoverTraffic(peerId: String) {
        activeJobs[peerId]?.cancel()
        activeJobs.remove(peerId)
    }
}
