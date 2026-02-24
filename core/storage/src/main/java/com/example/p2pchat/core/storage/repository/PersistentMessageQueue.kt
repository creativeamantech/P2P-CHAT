package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.storage.dao.OutboxDao
import com.example.p2pchat.core.storage.entity.OutboxEntity
import javax.inject.Inject

class PersistentMessageQueue @Inject constructor(
    private val outboxDao: OutboxDao
) {
    suspend fun enqueue(peerId: String, payload: ByteArray) {
        outboxDao.insert(
            OutboxEntity(
                peerId = peerId,
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getPendingForPeer(peerId: String): List<OutboxEntity> {
        return outboxDao.getPendingMessages(peerId)
    }

    suspend fun remove(id: Long) {
        outboxDao.deleteById(id)
    }
}
