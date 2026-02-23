package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "thread_participants",
    primaryKeys = ["thread_id", "peer_id"],
    foreignKeys = [
        ForeignKey(
            entity = ThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["thread_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PeerEntity::class,
            parentColumns = ["id"],
            childColumns = ["peer_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ThreadParticipantEntity(
    @ColumnInfo(name = "thread_id") val threadId: String,
    @ColumnInfo(name = "peer_id") val peerId: String,
    @ColumnInfo(name = "joined_at") val joinedAt: Long
)
