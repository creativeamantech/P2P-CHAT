package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

import androidx.room.PrimaryKey

@Entity(tableName = "messages_fts")
@Fts4
data class MessageFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val id: Int? = null, // FTS rowid mapping? Or we map 'rowid' to message_id?
    // FTS tables usually have an implicit rowid.
    // We want to map it to our string message ID.
    // But FTS rowid is integer.
    // We can store message_id as a column.

    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "thread_id") val threadId: String
)
