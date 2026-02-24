package com.example.p2pchat.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.p2pchat.core.storage.entity.PeerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: PeerEntity)

    @Query("SELECT * FROM peers WHERE id = :peerId")
    suspend fun getPeerById(peerId: String): PeerEntity?

    @Query("UPDATE peers SET is_verified = :isVerified WHERE id = :peerId")
    suspend fun updateVerification(peerId: String, isVerified: Boolean)
}
