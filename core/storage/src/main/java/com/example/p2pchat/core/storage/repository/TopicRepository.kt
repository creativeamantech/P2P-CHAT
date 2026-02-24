package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.model.Topic
import com.example.p2pchat.core.storage.dao.TopicDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

class TopicRepository @Inject constructor(
    private val topicDao: TopicDao
) {
    fun getAllTopics(): Flow<List<Topic>> = topicDao.getAllTopics().map { entities ->
        entities.map { entity ->
            Topic(
                name = entity.name,
                displayName = entity.displayName,
                color = entity.color,
                messageCount = 0, // TODO: Calculate message count
                lastUsed = Instant.fromEpochMilliseconds(entity.lastUsed)
            )
        }
    }

    suspend fun tagMessage(messageId: String, topicName: String) {
        topicDao.tagMessage(messageId, topicName, System.currentTimeMillis())
    }
}
