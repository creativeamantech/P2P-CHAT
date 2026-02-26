package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "sender_keys",
    primaryKeys = ["group_id", "sender_id"]
)
data class SenderKeyEntity(
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "sender_id") val senderId: String,
    @ColumnInfo(name = "chain_key") val chainKey: ByteArray,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SenderKeyEntity

        if (groupId != other.groupId) return false
        if (senderId != other.senderId) return false
        if (!chainKey.contentEquals(other.chainKey)) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + senderId.hashCode()
        result = 31 * result + chainKey.contentHashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
