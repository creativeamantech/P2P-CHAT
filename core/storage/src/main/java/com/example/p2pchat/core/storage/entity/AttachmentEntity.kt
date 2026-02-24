package com.example.p2pchat.core.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("message_id")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    @androidx.room.ColumnInfo(name = "message_id") val messageId: String,
    val type: String,
    val size: Long,
    val filename: String,
    val uri: String
)
