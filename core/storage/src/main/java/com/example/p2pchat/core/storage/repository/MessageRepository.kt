package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.storage.dao.MessageDao
import com.example.p2pchat.core.storage.dao.TopicDao
import com.example.p2pchat.core.storage.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val topicDao: TopicDao
) {
    // We return a wrapper that includes the entity and its tags.
    // Ideally we map to Domain Message here, but for now we'll stick to Entity + Tags
    // or let ViewModel do the mapping (as current ViewModel expects Entities but maps them).

    // To support tags in the UI, we need to fetch them.
    // Doing N+1 queries in Flow is bad.
    // Ideally we use a Relation POJO in DAO.

    // For MVP, let's just stick to fetching messages.
    // Tags can be fetched on demand or we ignore them for the main list if performance is issue.
    // But architecture says "Topic tagging ... is distinctive feature".

    // Let's modify observeThread to return MessageEntity.
    // ViewModel will have to fetch tags or we assume they are not critical for the *list* view initially,
    // or we fetch them.

    fun observeThread(threadId: String): Flow<List<MessageEntity>> {
        return messageDao.observeThread(threadId)
    }

    suspend fun saveMessage(entity: MessageEntity) {
        messageDao.insertMessage(entity)
    }

    suspend fun tagMessage(messageId: String, topicName: String) {
        topicDao.tagMessage(messageId, topicName, System.currentTimeMillis())
    }
}
