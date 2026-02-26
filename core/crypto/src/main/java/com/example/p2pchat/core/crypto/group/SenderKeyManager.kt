package com.example.p2pchat.core.crypto.group

import com.example.p2pchat.core.storage.dao.SenderKeyDao
import com.example.p2pchat.core.storage.entity.SenderKeyEntity
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SenderKeyManager @Inject constructor(
    private val senderKeyDao: SenderKeyDao
) {
    private val secureRandom = SecureRandom()

    suspend fun getSenderKey(groupId: String, senderId: String): SenderKeyEntity? {
        return senderKeyDao.getSenderKey(groupId, senderId)
    }

    suspend fun createMySenderKey(groupId: String, myPeerId: String): SenderKeyEntity {
        // Generate new random Chain Key (32 bytes)
        val chainKey = ByteArray(32)
        secureRandom.nextBytes(chainKey)

        val entity = SenderKeyEntity(
            groupId = groupId,
            senderId = myPeerId,
            chainKey = chainKey,
            updatedAt = System.currentTimeMillis()
        )
        senderKeyDao.insertSenderKey(entity)
        return entity
    }

    suspend fun updateSenderKey(entity: SenderKeyEntity) {
        senderKeyDao.insertSenderKey(entity) // REPLACE strategy
    }

    suspend fun saveReceivedSenderKey(groupId: String, senderId: String, chainKey: ByteArray) {
         val entity = SenderKeyEntity(
            groupId = groupId,
            senderId = senderId,
            chainKey = chainKey,
            updatedAt = System.currentTimeMillis()
        )
        senderKeyDao.insertSenderKey(entity)
    }
}
