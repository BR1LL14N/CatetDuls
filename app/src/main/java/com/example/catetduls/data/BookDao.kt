package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<Book>)

    // READ
    @Query("SELECT * FROM books ORDER BY created_at DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookById(bookId: Int): Flow<Book?>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookByIdSync(bookId: Int): Book?

    @Query("SELECT * FROM books WHERE isActive = 1 LIMIT 1")
    fun getActiveBook(): Flow<Book?>

    @Query("SELECT * FROM books WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveBookSync(): Book?

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    // UPDATE
    @Update
    suspend fun update(book: Book)

    @Query("UPDATE books SET isActive = 0")
    suspend fun deactivateAllBooks()

    @Query("UPDATE books SET isActive = 1 WHERE id = :bookId")
    suspend fun setActiveBook(bookId: Int)

    // DELETE
    @Delete
    suspend fun delete(book: Book)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteById(bookId: Int)

    // TRANSACTION untuk switch buku aktif
    @androidx.room.Transaction
    suspend fun switchActiveBook(newActiveBookId: Int) {
        deactivateAllBooks()
        setActiveBook(newActiveBookId)
    }

    @Query("SELECT * FROM books WHERE is_synced = 0")
    suspend fun getUnsyncedBooks(): List<Book>

    @Query("SELECT * FROM books WHERE server_id = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): Book?

    @Query("""
        UPDATE books 
        SET server_id = :serverId, 
            is_synced = 1, 
            last_sync_at = :lastSyncAt,
            sync_action = NULL
        WHERE id = :localId
    """)
    suspend fun updateSyncStatus(
        localId: Int,
        serverId: String,
        lastSyncAt: Long
    )

    @Query("UPDATE books SET is_synced = 0, sync_action = :action, updated_at = :updatedAt WHERE id = :id")
    suspend fun markAsUnsynced(id: Int, action: String, updatedAt: Long)
}