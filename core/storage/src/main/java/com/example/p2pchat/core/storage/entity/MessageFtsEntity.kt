package com.example.p2pchat.core.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

import androidx.room.PrimaryKey

@Entity(tableName = "messages_fts")
@Fts4
data class MessageFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Int? = null,
    @ColumnInfo(name = "message_id") val messageId: String,
    @ColumnInfo(name = "decrypted_content") val decryptedContent: String,
    @ColumnInfo(name = "thread_id") val threadId: String
)
