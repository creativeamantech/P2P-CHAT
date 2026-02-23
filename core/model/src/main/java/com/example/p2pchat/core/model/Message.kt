package com.example.p2pchat.core.model

import kotlinx.datetime.Instant

data class Message(
    val id: String,
    val threadId: String,
    val parentMessageId: String?,
    val senderId: String,
    val encryptedContent: ByteArray,
    val iv: ByteArray,
    val clearTextCache: String?,   // transient, never persisted
    val topics: Set<String>,
    val attachments: List<Attachment>,
    val sentAt: Instant,
    val deliveryState: DeliveryState,
    val reactions: Map<String, String> // peerId -> emoji
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (id != other.id) return false
        if (threadId != other.threadId) return false
        if (parentMessageId != other.parentMessageId) return false
        if (senderId != other.senderId) return false
        if (!encryptedContent.contentEquals(other.encryptedContent)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (clearTextCache != other.clearTextCache) return false
        if (topics != other.topics) return false
        if (attachments != other.attachments) return false
        if (sentAt != other.sentAt) return false
        if (deliveryState != other.deliveryState) return false
        if (reactions != other.reactions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + threadId.hashCode()
        result = 31 * result + (parentMessageId?.hashCode() ?: 0)
        result = 31 * result + senderId.hashCode()
        result = 31 * result + encryptedContent.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + (clearTextCache?.hashCode() ?: 0)
        result = 31 * result + topics.hashCode()
        result = 31 * result + attachments.hashCode()
        result = 31 * result + sentAt.hashCode()
        result = 31 * result + deliveryState.hashCode()
        result = 31 * result + reactions.hashCode()
        return result
    }
}
