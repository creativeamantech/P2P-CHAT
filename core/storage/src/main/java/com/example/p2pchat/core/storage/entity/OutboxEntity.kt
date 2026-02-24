package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "peer_id") val peerId: String,
    @ColumnInfo(name = "payload") val payload: ByteArray,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OutboxEntity

        if (id != other.id) return false
        if (peerId != other.peerId) return false
        if (!payload.contentEquals(other.payload)) return false
        if (createdAt != other.createdAt) return false
        if (attemptCount != other.attemptCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + attemptCount
        return result
    }
}
