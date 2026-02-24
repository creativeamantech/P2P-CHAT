package com.example.p2pchat.core.network

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.p2pchat.core.storage.repository.PersistentMessageQueue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MessageFlushWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val connectionManager: ConnectionManager,
    private val messageQueue: PersistentMessageQueue
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val peers = messageQueue.getPeersWithPendingMessages()
            Log.d("MessageFlushWorker", "Found ${peers.size} peers with pending messages")

            for (peerId in peers) {
                // If we know how to connect (e.g. have a descriptor cached or can discovery), try connect.
                // Currently ConnectionManager.connect requires PeerDescriptor.
                // We don't have PeerDescriptor easily here if they are not nearby.
                // But if they ARE nearby (discovered), we can connect.

                // For MVP, we'll ask ConnectionManager to "flushQueue(peerId)".
                // ConnectionManager needs to be updated to handle connection initiation if possible,
                // or just flush if already connected.

                // If connected, this will flush.
                // If not connected, we need to trigger connection?
                // ConnectionManager stores active transports.

                connectionManager.flushQueue(peerId)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("MessageFlushWorker", "Error flushing messages", e)
            Result.retry()
        }
    }
}
