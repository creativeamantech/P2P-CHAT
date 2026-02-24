package com.example.p2pchat.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.p2pchat.core.storage.entity.AttachmentEntity
import com.example.p2pchat.core.storage.entity.MessageWithAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Insert
    suspend fun insert(entity: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE message_id = :messageId")
    suspend fun getAttachmentsForMessage(messageId: String): List<AttachmentEntity>
}

@Dao
interface MessageDaoWithAttachments {
    @Transaction
    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY sent_at ASC")
    fun observeThreadWithAttachments(threadId: String): Flow<List<MessageWithAttachments>>
}
