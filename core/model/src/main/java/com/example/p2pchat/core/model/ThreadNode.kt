package com.example.p2pchat.core.model

data class ThreadNode(
    val message: Message,
    val children: List<ThreadNode>
)
