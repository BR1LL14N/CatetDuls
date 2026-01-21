package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos WHERE book_id = :bookId AND is_deleted = 0 ORDER BY date DESC")
    fun getAllMemosByBook(bookId: Int): Flow<List<Memo>>

    @Query(
            "SELECT * FROM memos WHERE book_id = :bookId AND tags LIKE '%' || :tag || '%' AND is_deleted = 0 ORDER BY date DESC"
    )
    fun getMemosByTag(bookId: Int, tag: String): Flow<List<Memo>>

    // Soft delete
    @Query("SELECT * FROM memos WHERE id = :id AND is_deleted = 0 LIMIT 1")
    suspend fun getMemoById(id: Int): Memo?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(memo: Memo): Long

    @Update suspend fun update(memo: Memo)

    // Soft delete
    @Query(
            "UPDATE memos SET is_deleted = 1, sync_action = 'DELETE', updated_at = :timestamp WHERE id = :memoId"
    )
    suspend fun softDelete(memoId: Int, timestamp: Long = System.currentTimeMillis())

    // --- SYNC METHODS ---
    @Query("SELECT * FROM memos WHERE is_synced = 0") suspend fun getAllUnsynced(): List<Memo>

    @Query("SELECT * FROM memos WHERE server_id = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): Memo?

    @Query(
            "UPDATE memos SET server_id = :serverId, is_synced = 1, last_sync_at = :syncedAt, sync_action = NULL WHERE id = :id"
    )
    suspend fun updateSyncStatus(id: Int, serverId: String, syncedAt: Long)

    @Query(
            "UPDATE memos SET is_synced = 0, sync_action = :action, updated_at = :timestamp WHERE id = :id"
    )
    suspend fun markAsUnsynced(
            id: Int,
            action: String,
            timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM memos WHERE id = :id") suspend fun deleteByIdPermanently(id: Int)
}
