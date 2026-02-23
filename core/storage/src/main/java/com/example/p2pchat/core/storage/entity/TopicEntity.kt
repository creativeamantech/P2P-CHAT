package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val name: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val color: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_used") val lastUsed: Long
)
