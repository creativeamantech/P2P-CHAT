package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.model.Peer
import com.example.p2pchat.core.model.PublicKeyBundle
import com.example.p2pchat.core.storage.dao.PeerDao
import com.example.p2pchat.core.storage.entity.PeerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

class PeerRepository @Inject constructor(
    private val peerDao: PeerDao
) {
    fun getAllPeers(): Flow<List<Peer>> = peerDao.getAllPeers().map { entities ->
        entities.map { entity ->
            Peer(
                id = entity.id,
                displayName = entity.displayName,
                publicKey = PublicKeyBundle(
                    identityKey = entity.identityKey,
                    exchangeKey = entity.exchangeKey,
                    certPem = entity.certPem
                ),
                lastSeen = Instant.fromEpochMilliseconds(entity.lastSeen),
                isTrusted = entity.isTrusted,
                isVerified = entity.isVerified,
                connectionHistory = emptyList()
            )
        }
    }

    suspend fun addPeer(peer: Peer) {
        peerDao.insertPeer(
            PeerEntity(
                id = peer.id,
                displayName = peer.displayName,
                identityKey = peer.publicKey.identityKey,
                exchangeKey = peer.publicKey.exchangeKey,
                certPem = peer.publicKey.certPem,
                lastSeen = peer.lastSeen.toEpochMilliseconds(),
                isTrusted = peer.isTrusted,
                isVerified = peer.isVerified,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getPeer(peerId: String): Peer? {
        val entity = peerDao.getPeerById(peerId) ?: return null
        return Peer(
            id = entity.id,
            displayName = entity.displayName,
            publicKey = PublicKeyBundle(
                identityKey = entity.identityKey,
                exchangeKey = entity.exchangeKey,
                certPem = entity.certPem
            ),
            lastSeen = Instant.fromEpochMilliseconds(entity.lastSeen),
            isTrusted = entity.isTrusted,
            isVerified = entity.isVerified,
            connectionHistory = emptyList()
        )
    }

    suspend fun updateVerificationStatus(peerId: String, isVerified: Boolean) {
        peerDao.updateVerification(peerId, isVerified)
    }

    // Keep the old name for compatibility if needed, but updateVerificationStatus is better
    suspend fun setPeerVerified(peerId: String, isVerified: Boolean) {
        peerDao.updateVerification(peerId, isVerified)
    }

    suspend fun getPeerById(peerId: String): Peer? {
        return getPeer(peerId)
    }
}
