package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.model.Thread
import com.example.p2pchat.core.storage.dao.ThreadDao
import com.example.p2pchat.core.storage.entity.ThreadEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

class ThreadRepository @Inject constructor(
    private val threadDao: ThreadDao
) {
    fun getAllThreads(): Flow<List<Thread>> = threadDao.getAllThreads().map { entities ->
        entities.map { entity ->
            Thread(
                id = entity.id,
                name = entity.name,
                participants = emptyList(), // Need to fetch from join table if needed
                createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
                lastActivity = Instant.fromEpochMilliseconds(entity.lastActivity),
                isPinned = entity.isPinned
            )
        }
    }

    suspend fun createThread(id: String, name: String) {
        val now = System.currentTimeMillis()
        threadDao.insertThread(
            ThreadEntity(
                id = id,
                name = name,
                createdAt = now,
                lastActivity = now,
                isPinned = false
            )
        )
    }

    suspend fun getThreadEntity(threadId: String): ThreadEntity? {
        return threadDao.getThreadById(threadId)
    }

    fun observeThreadEntity(threadId: String): Flow<ThreadEntity?> {
        return threadDao.observeThreadById(threadId)
    }

    suspend fun updateThreadExpiration(threadId: String, seconds: Int?) {
        threadDao.updateThreadExpiration(threadId, seconds)
    }
}
