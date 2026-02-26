package com.example.p2pchat.core.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.p2pchat.core.storage.entity.SenderKeyEntity

@Dao
interface SenderKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSenderKey(senderKey: SenderKeyEntity)

    @Query("SELECT * FROM sender_keys WHERE group_id = :groupId AND sender_id = :senderId")
    suspend fun getSenderKey(groupId: String, senderId: String): SenderKeyEntity?

    @Query("DELETE FROM sender_keys WHERE group_id = :groupId AND sender_id = :senderId")
    suspend fun deleteSenderKey(groupId: String, senderId: String)

    @Query("DELETE FROM sender_keys WHERE group_id = :groupId")
    suspend fun deleteAllSenderKeysForGroup(groupId: String)
}
