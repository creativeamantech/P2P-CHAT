package com.example.p2pchat.core.model

import kotlinx.datetime.Instant

data class Topic(
    val name: String,              // normalized: lowercase, no spaces
    val displayName: String,       // user-facing label
    val color: Int,                // ARGB
    val messageCount: Int,
    val lastUsed: Instant
)
