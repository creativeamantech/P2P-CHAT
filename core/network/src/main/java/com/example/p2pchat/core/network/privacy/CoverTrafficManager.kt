package com.example.p2pchat.core.network.privacy

import com.example.p2pchat.core.crypto.ratchet.RatchetManager
import com.example.p2pchat.core.network.ConnectionManager
import com.example.p2pchat.core.network.EncryptedPayload
import com.example.p2pchat.core.network.TransportMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class CoverTrafficManager @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val ratchetManager: RatchetManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = mutableMapOf<String, Job>()

    // Dummy message marker - recipients should discard if decrypted content matches/starts with this or via type
    // But Ratchet encrypts opaque bytes. We need a TransportMessage type for DUMMY?
    // Or just random bytes that fail to parse?
    // Section 23.6 says: "indistinguishable from real message".
    // So it should be a valid TransportMessage.Chat with dummy content?
    // Or a dedicated TYPE_DUMMY.
    // If we use TYPE_DUMMY, message length is revealed after decryption? No, padding hides length.
    // So let's use a Chat message with ignored content.

    private var isEnabled = false

    fun startCoverTraffic(peerId: String) {
        if (!isEnabled || activeJobs.containsKey(peerId)) return

        activeJobs[peerId] = scope.launch {
            while (isActive && isEnabled) {
                sendDummyMessage(peerId)
                // Random interval 2s - 10s
                delay(Random.nextLong(2000, 10000))
            }
        }
    }

    fun stopCoverTraffic(peerId: String) {
        activeJobs.remove(peerId)?.cancel()
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            activeJobs.values.forEach { it.cancel() }
            activeJobs.clear()
        }
    }

    private suspend fun sendDummyMessage(peerId: String) {
        try {
            // Create a dummy payload (e.g. 512 bytes of noise)
            val dummyContent = ByteArray(32) // Small real content
            Random.nextBytes(dummyContent)
            // Ideally we mark it as DUMMY so receiver discards it
            // Maybe prefix? "DUMMY:"
            val prefix = "DUMMY:".toByteArray()
            val payload = prefix + dummyContent

            val message = TransportMessage.Chat(payload, 0)
            val bytes = message.toBytes()

            // Encrypt (Ratchet or Padding layer will pad it to 512+)
            // Note: If we use RatchetManager directly, does it use MessagePadding?
            // We need to inject MessagePadding into RatchetManager or apply it here.
            // Assuming RatchetManager handles encryption.

            val ciphertext = ratchetManager.encrypt(peerId, bytes)
            connectionManager.sendMessage(peerId, EncryptedPayload(ciphertext))
        } catch (e: Exception) {
            // Ignore failures in cover traffic
        }
    }
}
