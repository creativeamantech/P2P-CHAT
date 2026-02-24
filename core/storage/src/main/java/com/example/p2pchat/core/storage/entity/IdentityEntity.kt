package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identities")
data class IdentityEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "type") val type: String, // "PERMANENT", "BURNER", "CONTEXTUAL"
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long?,
    @ColumnInfo(name = "is_burned") val isBurned: Boolean = false,
    @ColumnInfo(name = "ed25519_alias") val ed25519Alias: String,
    @ColumnInfo(name = "x25519_alias") val x25519Alias: String
)
