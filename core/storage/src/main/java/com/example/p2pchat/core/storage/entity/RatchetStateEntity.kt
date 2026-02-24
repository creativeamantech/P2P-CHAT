package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ratchet_states")
data class RatchetStateEntity(
    @PrimaryKey @ColumnInfo(name = "peer_id") val peerId: String,

    // DH Ratchet
    @ColumnInfo(name = "root_key") val rootKey: ByteArray,
    @ColumnInfo(name = "dh_priv") val dhPriv: ByteArray,
    @ColumnInfo(name = "dh_pub") val dhPub: ByteArray,
    @ColumnInfo(name = "dh_remote_pub") val dhRemotePub: ByteArray?,

    // Symmetric Ratchet
    @ColumnInfo(name = "chain_key_send") val chainKeySend: ByteArray?,
    @ColumnInfo(name = "chain_key_recv") val chainKeyRecv: ByteArray?,

    // Message Numbers
    @ColumnInfo(name = "send_message_num") val sendMessageNum: Int,
    @ColumnInfo(name = "recv_message_num") val recvMessageNum: Int,
    @ColumnInfo(name = "prev_chain_num") val prevChainNum: Int,

    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RatchetStateEntity

        if (peerId != other.peerId) return false
        if (!rootKey.contentEquals(other.rootKey)) return false
        if (!dhPriv.contentEquals(other.dhPriv)) return false
        if (!dhPub.contentEquals(other.dhPub)) return false
        if (dhRemotePub != null) {
            if (other.dhRemotePub == null) return false
            if (!dhRemotePub.contentEquals(other.dhRemotePub)) return false
        } else if (other.dhRemotePub != null) return false
        if (chainKeySend != null) {
            if (other.chainKeySend == null) return false
            if (!chainKeySend.contentEquals(other.chainKeySend)) return false
        } else if (other.chainKeySend != null) return false
        if (chainKeyRecv != null) {
            if (other.chainKeyRecv == null) return false
            if (!chainKeyRecv.contentEquals(other.chainKeyRecv)) return false
        } else if (other.chainKeyRecv != null) return false
        if (sendMessageNum != other.sendMessageNum) return false
        if (recvMessageNum != other.recvMessageNum) return false
        if (prevChainNum != other.prevChainNum) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = peerId.hashCode()
        result = 31 * result + rootKey.contentHashCode()
        result = 31 * result + dhPriv.contentHashCode()
        result = 31 * result + dhPub.contentHashCode()
        result = 31 * result + (dhRemotePub?.contentHashCode() ?: 0)
        result = 31 * result + (chainKeySend?.contentHashCode() ?: 0)
        result = 31 * result + (chainKeyRecv?.contentHashCode() ?: 0)
        result = 31 * result + sendMessageNum
        result = 31 * result + recvMessageNum
        result = 31 * result + prevChainNum
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
