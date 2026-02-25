package com.example.p2pchat.core.storage.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.p2pchat.core.storage.entity.AttachmentEntity
import com.example.p2pchat.core.storage.entity.MessageEntity

data class MessageWithAttachments(
    @Embedded val message: MessageEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "message_id"
    )
    val attachments: List<AttachmentEntity>
)
