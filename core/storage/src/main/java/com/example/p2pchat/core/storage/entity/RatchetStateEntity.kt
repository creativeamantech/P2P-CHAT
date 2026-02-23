package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ratchet_states")
data class RatchetStateEntity(
    @PrimaryKey @ColumnInfo(name = "peer_id") val peerId: String,
    @ColumnInfo(name = "root_key") val rootKey: ByteArray,
    @ColumnInfo(name = "send_chain_key") val sendChainKey: ByteArray,
    @ColumnInfo(name = "receive_chain_key") val receiveChainKey: ByteArray,
    @ColumnInfo(name = "send_message_num") val sendMessageNum: Int,
    @ColumnInfo(name = "recv_message_num") val recvMessageNum: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RatchetStateEntity

        if (peerId != other.peerId) return false
        if (!rootKey.contentEquals(other.rootKey)) return false
        if (!sendChainKey.contentEquals(other.sendChainKey)) return false
        if (!receiveChainKey.contentEquals(other.receiveChainKey)) return false
        if (sendMessageNum != other.sendMessageNum) return false
        if (recvMessageNum != other.recvMessageNum) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = peerId.hashCode()
        result = 31 * result + rootKey.contentHashCode()
        result = 31 * result + sendChainKey.contentHashCode()
        result = 31 * result + receiveChainKey.contentHashCode()
        result = 31 * result + sendMessageNum
        result = 31 * result + recvMessageNum
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
