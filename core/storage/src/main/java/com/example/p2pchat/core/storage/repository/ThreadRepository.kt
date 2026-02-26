package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.model.Thread
import com.example.p2pchat.core.storage.dao.ThreadDao
import com.example.p2pchat.core.storage.dao.ThreadParticipantDao
import com.example.p2pchat.core.storage.entity.ThreadEntity
import com.example.p2pchat.core.storage.entity.ThreadParticipantEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import java.util.UUID
import javax.inject.Inject

class ThreadRepository @Inject constructor(
    private val threadDao: ThreadDao,
    private val threadParticipantDao: ThreadParticipantDao
) {
    fun getAllThreads(): Flow<List<Thread>> = threadDao.getAllThreads().map { entities ->
        entities.map { entity ->
            Thread(
                id = entity.id,
                name = entity.name,
                participants = emptyList(), // Ideally fetch count or avatars
                createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
                lastActivity = Instant.fromEpochMilliseconds(entity.lastActivity),
                isPinned = entity.isPinned,
                type = entity.type
            )
        }
    }

    suspend fun createThread(id: String, name: String, type: String = "ONE_TO_ONE") {
        val now = System.currentTimeMillis()
        threadDao.insertThread(
            ThreadEntity(
                id = id,
                name = name,
                createdAt = now,
                lastActivity = now,
                isPinned = false,
                type = type
            )
        )
    }

    suspend fun createGroupThread(name: String, memberIds: List<String>): String {
        val groupId = UUID.randomUUID().toString()
        createThread(groupId, name, "GROUP")
        val now = System.currentTimeMillis()
        val participants = memberIds.map { peerId ->
            ThreadParticipantEntity(
                threadId = groupId,
                peerId = peerId,
                joinedAt = now,
                role = "MEMBER"
            )
        }
        threadParticipantDao.insertParticipants(participants)
        return groupId
    }

    suspend fun addParticipant(threadId: String, peerId: String, role: String = "MEMBER") {
        threadParticipantDao.insertParticipant(
            ThreadParticipantEntity(
                threadId = threadId,
                peerId = peerId,
                joinedAt = System.currentTimeMillis(),
                role = role
            )
        )
    }

    suspend fun getParticipants(threadId: String): List<ThreadParticipantEntity> {
        return threadParticipantDao.getParticipantsList(threadId)
    }

    fun observeParticipants(threadId: String): Flow<List<ThreadParticipantEntity>> {
        return threadParticipantDao.getParticipantsForThread(threadId)
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
