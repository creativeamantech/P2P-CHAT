package com.example.p2pchat.feature.conversations.domain

import com.example.p2pchat.core.storage.entity.MessageEntity
import com.example.p2pchat.core.storage.entity.PeerEntity
import com.example.p2pchat.core.storage.entity.TopicEntity
import com.example.p2pchat.core.storage.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class SearchResults(
    val messages: List<MessageEntity>,
    val topics: List<TopicEntity>,
    val peers: List<PeerEntity>
)

class SearchUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    operator fun invoke(query: String): Flow<SearchResults> {
        val messages = searchRepository.searchMessages(query)
        val topics = searchRepository.searchTopics(query)
        val peers = searchRepository.searchPeers(query)

        return combine(messages, topics, peers) { m, t, p ->
            SearchResults(m, t, p)
        }
    }
}
