package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE is_deleted = 0 ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE is_deleted = 0") suspend fun getAllTagsList(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(tag: TagEntity): Long

    @Update suspend fun update(tag: TagEntity)

    @Delete suspend fun delete(tag: TagEntity)

    // Soft delete
    @Query(
            "UPDATE tags SET is_deleted = 1, sync_action = 'DELETE', updated_at = :timestamp WHERE id = :id"
    )
    suspend fun softDelete(id: Int, timestamp: Long = System.currentTimeMillis())

    // --- SYNC METHODS ---
    @Query("SELECT * FROM tags WHERE is_synced = 0") suspend fun getAllUnsynced(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE server_id = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): TagEntity?

    @Query(
            "UPDATE tags SET server_id = :serverId, is_synced = 1, last_sync_at = :syncedAt, sync_action = NULL WHERE id = :id"
    )
    suspend fun updateSyncStatus(id: Int, serverId: String, syncedAt: Long)

    @Query(
            "UPDATE tags SET is_synced = 0, sync_action = :action, updated_at = :timestamp WHERE id = :id"
    )
    suspend fun markAsUnsynced(
            id: Int,
            action: String,
            timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM tags WHERE id = :id") suspend fun deleteByIdPermanently(id: Int)
}
