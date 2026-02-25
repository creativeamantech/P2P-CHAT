package com.example.p2pchat.feature.messaging

import com.example.p2pchat.core.model.Message
import com.example.p2pchat.core.model.ThreadNode

object ThreadBuilder {
    fun buildThreadTree(messages: List<Message>): List<ThreadNode> {
        val nodeMap = messages.associateBy { it.id }
        val roots = mutableListOf<ThreadNode>()
        val childMap = mutableMapOf<String, MutableList<Message>>()

        // Sort by timestamp to ensure deterministic order before building tree
        val sortedMessages = messages.sortedBy { it.sentAt }

        sortedMessages.forEach { msg ->
            if (msg.parentMessageId == null) {
                // Potential root, but we defer creation until we process all children
                // Actually, in a recursive build, we need to know children first?
                // No, we can build the map first.
            } else {
                childMap.getOrPut(msg.parentMessageId!!) { mutableListOf() }.add(msg)
            }
        }

        // Recursive function
        fun buildNode(msg: Message): ThreadNode {
            val children = childMap[msg.id]?.map { buildNode(it) } ?: emptyList()
            return ThreadNode(msg, children)
        }

        // Find actual roots (null parent OR parent not in list)
        sortedMessages.forEach { msg ->
            if (msg.parentMessageId == null || !nodeMap.containsKey(msg.parentMessageId)) {
                roots.add(buildNode(msg))
            }
        }

        return roots
    }
}
