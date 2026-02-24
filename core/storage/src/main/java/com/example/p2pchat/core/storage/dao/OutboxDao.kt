package com.example.p2pchat.core.storage.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.p2pchat.core.storage.entity.OutboxEntity

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox WHERE peer_id = :peerId ORDER BY created_at ASC")
    suspend fun getPendingMessages(peerId: String): List<OutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OutboxEntity): Long

    @Delete
    suspend fun delete(entity: OutboxEntity)

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT DISTINCT peer_id FROM outbox")
    suspend fun getPeersWithPendingMessages(): List<String>
}
