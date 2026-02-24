package com.example.p2pchat.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.p2pchat.core.storage.entity.MessageTopicCrossRef
import com.example.p2pchat.core.storage.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY last_used DESC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTopic(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessageTopicCrossRef(crossRef: MessageTopicCrossRef)

    @Query("SELECT topic_name FROM message_topics WHERE message_id = :messageId")
    fun getTopicsForMessage(messageId: String): Flow<List<String>>

    @Transaction
    suspend fun tagMessage(messageId: String, topicName: String, timestamp: Long) {
        // 1. Ensure topic exists
        insertTopic(TopicEntity(
            name = topicName,
            displayName = topicName, // Simple logic: display name = name
            color = 0xFF000000.toInt(), // Default color
            createdAt = timestamp,
            lastUsed = timestamp
        ))

        // 2. Link message
        insertMessageTopicCrossRef(MessageTopicCrossRef(
            messageId = messageId,
            topicName = topicName,
            taggedAt = timestamp
        ))

        // 3. Update last used
        updateTopicLastUsed(topicName, timestamp)
    }

    @Query("UPDATE topics SET last_used = :timestamp WHERE name = :topicName")
    suspend fun updateTopicLastUsed(topicName: String, timestamp: Long)
}
