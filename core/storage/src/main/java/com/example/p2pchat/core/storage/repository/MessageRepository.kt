package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.storage.dao.AttachmentDao
import com.example.p2pchat.core.storage.dao.MessageDao
import com.example.p2pchat.core.storage.dao.MessageDaoWithAttachments
import com.example.p2pchat.core.storage.dao.TopicDao
import com.example.p2pchat.core.storage.entity.AttachmentEntity
import com.example.p2pchat.core.storage.entity.MessageEntity
import com.example.p2pchat.core.storage.entity.MessageWithAttachments
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val topicDao: TopicDao,
    private val attachmentDao: AttachmentDao,
    private val messageDaoWithAttachments: MessageDaoWithAttachments
) {
    fun observeThread(threadId: String): Flow<List<MessageEntity>> {
        return messageDao.observeThread(threadId)
    }

    fun observeThreadWithAttachments(threadId: String): Flow<List<MessageWithAttachments>> {
        return messageDaoWithAttachments.observeThreadWithAttachments(threadId)
    }

    suspend fun saveMessage(entity: MessageEntity) {
        val content = String(entity.encryptedContent)
        messageDao.insertMessageWithFts(entity, content)
    }

    suspend fun saveAttachment(entity: AttachmentEntity) {
        attachmentDao.insert(entity)
    }

    suspend fun getAttachmentsForMessage(messageId: String): List<AttachmentEntity> {
        return attachmentDao.getAttachmentsForMessage(messageId)
    }

    suspend fun tagMessage(messageId: String, topicName: String) {
        topicDao.tagMessage(messageId, topicName, System.currentTimeMillis())
    }

    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        return messageDao.searchMessages("**")
    }
}
