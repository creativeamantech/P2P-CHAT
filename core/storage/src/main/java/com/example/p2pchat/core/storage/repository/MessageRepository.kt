package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.storage.dao.MessageDao
import com.example.p2pchat.core.storage.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    fun observeThread(threadId: String): Flow<List<MessageEntity>> {
        return messageDao.observeThread(threadId)
    }

    suspend fun saveMessage(entity: MessageEntity) {
        messageDao.insertMessage(entity)
    }
}
