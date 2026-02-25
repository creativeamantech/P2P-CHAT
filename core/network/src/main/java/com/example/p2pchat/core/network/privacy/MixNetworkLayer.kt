package com.example.p2pchat.core.network.privacy

import com.example.p2pchat.core.network.EncryptedPayload
import com.example.p2pchat.core.network.P2PTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MixNetworkLayer @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mixPool = ConcurrentLinkedDeque<QueuedMessage>()
    private val poolMutex = Mutex()
    private var isRunning = false

    data class QueuedMessage(
        val payload: EncryptedPayload,
        val peerId: String,
        val transport: P2PTransport
    )

    fun start() {
        if (isRunning) return
        isRunning = true
        startMixFlush()
    }

    fun stop() {
        isRunning = false
    }

    suspend fun sendViaMix(
        payload: EncryptedPayload,
        peerId: String,
        transport: P2PTransport
    ) {
        if (!isRunning) start()

        poolMutex.withLock {
            mixPool.add(QueuedMessage(payload, peerId, transport))
        }
    }

    private fun startMixFlush() = scope.launch {
        while (isActive && isRunning) {
            // Random flush interval
            val interval = Random.nextLong(1000, 5000)
            delay(interval)

            val batch = mutableListOf<QueuedMessage>()
            poolMutex.withLock {
                // Take all messages? Or a subset?
                // Taking all and shuffling is simple mixing.
                while (!mixPool.isEmpty()) {
                    val msg = mixPool.poll()
                    if (msg != null) batch.add(msg)
                }
            }

            if (batch.isNotEmpty()) {
                // Shuffle the batch to break ordering
                batch.shuffle()

                batch.forEach { msg ->
                    try {
                        msg.transport.send(msg.payload, msg.peerId)
                    } catch (e: Exception) {
                        // Log error
                    }
                    // Small jitter between sends
                    delay(Random.nextLong(0, 500))
                }
            }
        }
    }
}
