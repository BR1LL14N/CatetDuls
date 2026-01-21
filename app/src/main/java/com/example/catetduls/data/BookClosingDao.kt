package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookClosingDao {
        @Query(
                "SELECT * FROM book_closings WHERE book_id = :bookId AND is_deleted = 0 ORDER BY period_end DESC"
        )
        fun getAllClosingsByBook(bookId: Int): Flow<List<BookClosing>>

        @Query(
                "SELECT * FROM book_closings WHERE book_id = :bookId AND period_start <= :timestamp AND period_end >= :timestamp AND is_deleted = 0 LIMIT 1"
        )
        suspend fun getClosingForDate(bookId: Int, timestamp: Long): BookClosing?

        @Query("SELECT * FROM book_closings WHERE id = :id")
        suspend fun getClosingById(id: Int): BookClosing?

        @Insert suspend fun insert(closing: BookClosing): Long

        @Update suspend fun update(closing: BookClosing)

        @Delete suspend fun delete(closing: BookClosing)

        // Cek apakah tanggal tertentu ada di periode tertutup
        @Query(
                "SELECT COUNT(*) FROM book_closings WHERE book_id = :bookId AND period_start <= :timestamp AND period_end >= :timestamp AND is_deleted = 0"
        )
        suspend fun isDateClosed(bookId: Int, timestamp: Long): Int

        // Soft delete untuk reopen
        @Query(
                "UPDATE book_closings SET is_deleted = 1, sync_action = 'DELETE', updated_at = :timestamp WHERE id = :closingId"
        )
        suspend fun softDelete(closingId: Int, timestamp: Long = System.currentTimeMillis())

        // --- SYNC METHODS ---
        @Query("SELECT * FROM book_closings WHERE is_synced = 0")
        suspend fun getAllUnsynced(): List<BookClosing>

        @Query("SELECT * FROM book_closings WHERE server_id = :serverId LIMIT 1")
        suspend fun getByServerId(serverId: String): BookClosing?

        @Query(
                "UPDATE book_closings SET server_id = :serverId, is_synced = 1, last_sync_at = :syncedAt, sync_action = NULL WHERE id = :id"
        )
        suspend fun updateSyncStatus(id: Int, serverId: String, syncedAt: Long)

        @Query(
                "UPDATE book_closings SET is_synced = 0, sync_action = :action, updated_at = :timestamp WHERE id = :id"
        )
        suspend fun markAsUnsynced(
                id: Int,
                action: String,
                timestamp: Long = System.currentTimeMillis()
        )

        @Query("DELETE FROM book_closings WHERE id = :id")
        suspend fun deleteByIdPermanently(id: Int)
}
