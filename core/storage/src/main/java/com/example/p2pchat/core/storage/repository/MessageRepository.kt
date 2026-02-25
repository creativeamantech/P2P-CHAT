package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.storage.dao.AttachmentDao
import com.example.p2pchat.core.storage.dao.MessageDao
import com.example.p2pchat.core.storage.dao.MessageDaoWithAttachments
import com.example.p2pchat.core.storage.dao.TopicDao
import com.example.p2pchat.core.storage.entity.AttachmentEntity
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
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
    fun observeThread(threadId: String): Flow<PagingData<MessageEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { messageDao.observeThread(threadId) }
        ).flow
    }

    // Legacy support or new pager for attachments?
    // messageDaoWithAttachments.observeThreadWithAttachments(threadId) is also a Flow<List>.
    // If we want paging for ChatScreen, we should use observeThread with Paging.
    // Attachments loading strategy: Load on demand or join?
    // Paging with Relation is possible.
    // But MessageDaoWithAttachments needs to return PagingSource too.

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
