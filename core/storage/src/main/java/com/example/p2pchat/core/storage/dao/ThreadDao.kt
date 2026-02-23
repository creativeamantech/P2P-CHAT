package com.example.p2pchat.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.p2pchat.core.storage.entity.ThreadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {
    @Query("SELECT * FROM threads ORDER BY last_activity DESC")
    fun getAllThreads(): Flow<List<ThreadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ThreadEntity)
}
