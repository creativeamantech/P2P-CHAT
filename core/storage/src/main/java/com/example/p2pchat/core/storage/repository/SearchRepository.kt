package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.storage.dao.MessageDao
import com.example.p2pchat.core.storage.dao.PeerDao
import com.example.p2pchat.core.storage.dao.TopicDao
import com.example.p2pchat.core.storage.entity.MessageEntity
import com.example.p2pchat.core.storage.entity.PeerEntity
import com.example.p2pchat.core.storage.entity.TopicEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val topicDao: TopicDao,
    private val peerDao: PeerDao
) {
    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        val ftsQuery = "*$query*"
        return messageDao.searchMessagesFts(ftsQuery)
    }

    fun searchTopics(query: String): Flow<List<TopicEntity>> {
        return topicDao.searchTopics("%$query%")
    }

    fun searchPeers(query: String): Flow<List<PeerEntity>> {
        return peerDao.searchPeers("%$query%")
    }
}
