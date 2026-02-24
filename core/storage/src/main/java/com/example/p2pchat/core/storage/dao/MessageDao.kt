package com.example.p2pchat.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.p2pchat.core.storage.entity.MessageEntity
import com.example.p2pchat.core.storage.entity.MessageFtsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE thread_id = :threadId ORDER BY sent_at ASC")
    fun observeThread(threadId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    // FTS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageFts(fts: MessageFtsEntity)

    @Transaction
    suspend fun insertMessageWithFts(message: MessageEntity, content: String) {
        insertMessage(message)
        insertMessageFts(MessageFtsEntity(messageId = message.id, content = content, threadId = message.threadId))
    }

    @Query("""
        SELECT m.* FROM messages m
        JOIN messages_fts fts ON m.id = fts.message_id
        WHERE fts.content MATCH :query
    """)
    fun searchMessages(query: String): Flow<List<MessageEntity>>
}
