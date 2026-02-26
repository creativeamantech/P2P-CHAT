package com.example.p2pchat.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.p2pchat.core.storage.entity.ThreadParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadParticipantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ThreadParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ThreadParticipantEntity>)

    @Query("SELECT * FROM thread_participants WHERE thread_id = :threadId")
    fun getParticipantsForThread(threadId: String): Flow<List<ThreadParticipantEntity>>

    @Query("SELECT * FROM thread_participants WHERE thread_id = :threadId")
    suspend fun getParticipantsList(threadId: String): List<ThreadParticipantEntity>

    @Query("DELETE FROM thread_participants WHERE thread_id = :threadId AND peer_id = :peerId")
    suspend fun removeParticipant(threadId: String, peerId: String)
}
