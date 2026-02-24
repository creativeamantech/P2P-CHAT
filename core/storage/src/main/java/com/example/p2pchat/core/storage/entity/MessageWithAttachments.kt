package com.example.p2pchat.core.storage.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MessageWithAttachments(
    @Embedded val message: MessageEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "message_id"
    )
    val attachments: List<AttachmentEntity>
)
