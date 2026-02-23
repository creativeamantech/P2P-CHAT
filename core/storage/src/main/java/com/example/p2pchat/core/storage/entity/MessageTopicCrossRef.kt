package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "message_topics",
    primaryKeys = ["message_id", "topic_name"],
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["name"],
            childColumns = ["topic_name"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("topic_name", "tagged_at")
    ]
)
data class MessageTopicCrossRef(
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "topic_name") val topicName: String,
    @ColumnInfo(name = "tagged_at") val taggedAt: Long
)
