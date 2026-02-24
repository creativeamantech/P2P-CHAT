package com.example.p2pchat.core.storage.repository

import com.example.p2pchat.core.storage.dao.RatchetStateDao
import com.example.p2pchat.core.storage.entity.RatchetStateEntity
import javax.inject.Inject

class RatchetRepository @Inject constructor(
    private val ratchetStateDao: RatchetStateDao
) {
    suspend fun getRatchetState(peerId: String): RatchetStateEntity? {
        return ratchetStateDao.getRatchetState(peerId)
    }

    suspend fun saveRatchetState(state: RatchetStateEntity) {
        ratchetStateDao.insertRatchetState(state)
    }
}
