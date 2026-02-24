package com.example.p2pchat.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.p2pchat.core.storage.entity.IdentityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: IdentityEntity)

    @Query("SELECT * FROM identities WHERE id = :id")
    suspend fun getById(id: String): IdentityEntity?

    @Query("SELECT * FROM identities WHERE is_burned = 0 ORDER BY created_at DESC")
    fun getAllActive(): Flow<List<IdentityEntity>>

    @Query("UPDATE identities SET is_burned = 1 WHERE id = :id")
    suspend fun markBurned(id: String)

    @Query("DELETE FROM identities WHERE id = :id")
    suspend fun delete(id: String)
}
