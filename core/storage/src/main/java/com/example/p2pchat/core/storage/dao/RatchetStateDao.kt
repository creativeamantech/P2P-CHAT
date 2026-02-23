package com.example.p2pchat.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.p2pchat.core.storage.entity.RatchetStateEntity

@Dao
interface RatchetStateDao {
    @Query("SELECT * FROM ratchet_states WHERE peer_id = :peerId")
    suspend fun getRatchetState(peerId: String): RatchetStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRatchetState(state: RatchetStateEntity)
}
