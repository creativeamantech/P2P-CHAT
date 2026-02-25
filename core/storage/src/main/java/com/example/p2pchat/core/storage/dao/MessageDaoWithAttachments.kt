package com.example.p2pchat.core.storage.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.p2pchat.core.storage.entity.MessageWithAttachments
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDaoWithAttachments {
    @Transaction
    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY sent_at ASC")
    fun observeThreadWithAttachments(threadId: String): Flow<List<MessageWithAttachments>>

    @Transaction
    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY sent_at ASC")
    fun observeThreadWithAttachmentsPaging(threadId: String): PagingSource<Int, MessageWithAttachments>
}
