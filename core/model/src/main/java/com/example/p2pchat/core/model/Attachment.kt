package com.example.p2pchat.core.model

data class Attachment(
    val id: String,
    val type: String, // MIME type
    val size: Long,
    val filename: String,
    val uri: String // Local URI
)
