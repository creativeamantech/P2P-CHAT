package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["thread_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("thread_id", "sent_at"),
        Index("parent_message_id"),
        Index("sender_id")
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "thread_id") val threadId: String,
    @ColumnInfo(name = "parent_message_id") val parentMessageId: String?,
    @ColumnInfo(name = "sender_id") val senderId: String,
    @ColumnInfo(name = "encrypted_content") val encryptedContent: ByteArray,
    val iv: ByteArray,
    @ColumnInfo(name = "sent_at") val sentAt: Long,
    @ColumnInfo(name = "delivery_state") val deliveryState: String, // Serialize state
    @ColumnInfo(name = "delivered_at") val deliveredAt: Long?,
    @ColumnInfo(name = "read_at") val readAt: Long?,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "expires_at") val expiresAt: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageEntity

        if (id != other.id) return false
        if (threadId != other.threadId) return false
        if (parentMessageId != other.parentMessageId) return false
        if (senderId != other.senderId) return false
        if (!encryptedContent.contentEquals(other.encryptedContent)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (sentAt != other.sentAt) return false
        if (deliveryState != other.deliveryState) return false
        if (deliveredAt != other.deliveredAt) return false
        if (readAt != other.readAt) return false
        if (isDeleted != other.isDeleted) return false
        if (expiresAt != other.expiresAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + threadId.hashCode()
        result = 31 * result + (parentMessageId?.hashCode() ?: 0)
        result = 31 * result + senderId.hashCode()
        result = 31 * result + encryptedContent.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + sentAt.hashCode()
        result = 31 * result + deliveryState.hashCode()
        result = 31 * result + (deliveredAt?.hashCode() ?: 0)
        result = 31 * result + (readAt?.hashCode() ?: 0)
        result = 31 * result + isDeleted.hashCode()
        result = 31 * result + (expiresAt?.hashCode() ?: 0)
        return result
    }
}
