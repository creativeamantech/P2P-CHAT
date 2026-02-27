package com.example.p2pchat.feature.messaging

import com.example.p2pchat.core.model.Message
import com.example.p2pchat.core.model.ThreadNode

object ThreadBuilder {
    fun buildThreadTree(messages: List<Message>): List<ThreadNode> {
        val nodeMap = messages.associateBy { it.id }
        val roots = mutableListOf<ThreadNode>()
        val childMap = mutableMapOf<String, MutableList<Message>>()

        messages.forEach { msg ->
            if (msg.parentMessageId == null || !nodeMap.containsKey(msg.parentMessageId)) {
                // Treated as root if parent not found in current list (Paging artifact?)
                roots.add(ThreadNode(msg, mutableListOf()))
            } else {
                childMap.getOrPut(msg.parentMessageId!!) { mutableListOf() }.add(msg)
            }
        }

        fun buildNode(msg: Message): ThreadNode {
            val children = (childMap[msg.id] ?: emptyList())
                .sortedBy { it.sentAt } // Chronological replies
                .map { buildNode(it) }
                .toMutableList()
            return ThreadNode(msg, children)
        }

        // Roots usually sorted reverse chronological for chat view, but if we build tree,
        // we might want chronological roots if we display oldest at top?
        // Chat is reverse layout usually.
        return roots.map { buildNode(it.message) }.sortedByDescending { it.message.sentAt }
    }
}
