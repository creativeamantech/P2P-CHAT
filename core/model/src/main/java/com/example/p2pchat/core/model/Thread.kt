package com.example.p2pchat.core.model

import kotlinx.datetime.Instant

data class Thread(
    val id: String,
    val name: String,
    val participants: List<String>, // peer IDs
    val createdAt: Instant,
    val lastActivity: Instant,
    val isPinned: Boolean
)
