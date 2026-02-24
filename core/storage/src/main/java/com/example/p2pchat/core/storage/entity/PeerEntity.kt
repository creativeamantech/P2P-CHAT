package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "identity_key") val identityKey: ByteArray,
    @ColumnInfo(name = "exchange_key") val exchangeKey: ByteArray,
    @ColumnInfo(name = "cert_pem") val certPem: String,
    @ColumnInfo(name = "last_seen") val lastSeen: Long,
    @ColumnInfo(name = "is_trusted") val isTrusted: Boolean,
    @ColumnInfo(name = "is_verified") val isVerified: Boolean = false, // Added
    @ColumnInfo(name = "created_at") val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PeerEntity

        if (id != other.id) return false
        if (displayName != other.displayName) return false
        if (!identityKey.contentEquals(other.identityKey)) return false
        if (!exchangeKey.contentEquals(other.exchangeKey)) return false
        if (certPem != other.certPem) return false
        if (lastSeen != other.lastSeen) return false
        if (isTrusted != other.isTrusted) return false
        if (isVerified != other.isVerified) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + identityKey.contentHashCode()
        result = 31 * result + exchangeKey.contentHashCode()
        result = 31 * result + certPem.hashCode()
        result = 31 * result + lastSeen.hashCode()
        result = 31 * result + isTrusted.hashCode()
        result = 31 * result + isVerified.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
