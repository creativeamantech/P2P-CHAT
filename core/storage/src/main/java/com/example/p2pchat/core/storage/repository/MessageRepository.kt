package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.storage.dao.MessageDao
import com.example.p2pchat.core.storage.dao.TopicDao
import com.example.p2pchat.core.storage.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val topicDao: TopicDao
) {
    fun observeThread(threadId: String): Flow<List<MessageEntity>> {
        return messageDao.observeThread(threadId)
    }

    // Updated to accept content for FTS indexing
    suspend fun saveMessage(entity: MessageEntity) {
        // We assume entity.encryptedContent contains the bytes.
        // For MVP we decided "encryptedContent" might store plaintext for local display.
        // We extract string from it for indexing.
        val content = String(entity.encryptedContent)
        messageDao.insertMessageWithFts(entity, content)
    }

    suspend fun tagMessage(messageId: String, topicName: String) {
        topicDao.tagMessage(messageId, topicName, System.currentTimeMillis())
    }

    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        // FTS MATCH query syntax: "query*"
        return messageDao.searchMessages("**")
    }
}
