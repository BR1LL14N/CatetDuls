package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // ===================================
    // CREATE
    // ===================================

    /**
     * Insert kategori baru atau mengganti jika ada konflik
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    /**
     * Insert multiple categories (untuk data awal atau saat sync pull)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    // ===================================
    // READ
    // ===================================

    /**
     * Get semua kategori
     */
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<Category>

    /**
     * Get semua kategori berdasarkan ID Buku
     */
    @Query("SELECT * FROM categories WHERE bookId = :bookId ORDER BY name ASC")
    fun getAllCategoriesByBook(bookId: Int): Flow<List<Category>>

    /**
     * Search dan filter kategori berdasarkan ID Buku, Query, dan Tipe (Opsional)
     */
    @Query("""
        SELECT * FROM categories 
        WHERE bookId = :bookId 
        AND (name LIKE :query OR icon LIKE :query)
        AND (:type IS NULL OR type = :type)
        ORDER BY name ASC
    """)
    fun searchCategories(bookId: Int, query: String, type: TransactionType?): Flow<List<Category>>

    @Query("""
        SELECT * FROM categories 
        WHERE bookId = :bookId AND type = :type 
        ORDER BY name ASC
    """)
    fun getCategoriesByBookIdAndType(bookId: Int, type: TransactionType): Flow<List<Category>>

    /**
     * Get kategori berdasarkan ID
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Int): Flow<Category?>

    /**
     * Get kategori berdasarkan ID (suspend)
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryByIdSync(id: Int): Category?

    // ===================================
    // UPDATE
    // ===================================

    /**
     * Update kategori
     */
    @Update
    suspend fun updateCategory(category: Category)

    /**
     * Menandai kategori sebagai belum tersinkronisasi
     */
    @Query("UPDATE categories SET is_synced = 0, sync_action = :action, updated_at = :updatedAt WHERE id = :id")
    suspend fun markAsUnsynced(id: Int, action: String, updatedAt: Long)

    // ===================================
    // DELETE
    // ===================================

    /**
     * Delete kategori (Dipanggil oleh Repo hanya jika belum pernah sync)
     */
    @Delete
    suspend fun deleteCategory(category: Category)

    /**
     * Delete berdasarkan ID (Dipanggil oleh Repo setelah sync DELETE berhasil)
     */
    @Query("DELETE FROM categories WHERE id = :categoryId AND isDefault = 0")
    suspend fun deleteCategoryById(categoryId: Int)

    // ===================================
    // SYNC OPERATIONS
    // ===================================

    /**
     * Mengambil semua kategori yang perlu disinkronkan ke server.
     * Termasuk yang baru dibuat/diubah (is_synced = 0) dan yang ditandai untuk dihapus (is_deleted = 1).
     */
    @Query("SELECT * FROM categories WHERE is_synced = 0 OR is_deleted = 1")
    suspend fun getUnsyncedCategories(): List<Category>

    /**
     * Memperbarui status sinkronisasi setelah server merespons (sukses CREATE/UPDATE).
     */
    @Query("""
        UPDATE categories 
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

    // ===================================
    // UTILITY/STATS
    // ===================================

    /**
     * Cek apakah nama kategori sudah ada
     */
    @Query("SELECT COUNT(*) > 0 FROM categories WHERE name = :name")
    suspend fun isCategoryNameExists(name: String): Boolean

    /**
     * Get jumlah kategori
     */
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    /**
     * Get kategori dengan jumlah transaksi
     */
    @Query("""
        SELECT c.*, COUNT(t.id) as transactionCount
        FROM categories c
        LEFT JOIN transactions t ON c.id = t.categoryId
        GROUP BY c.id, c.name, c.icon, c.type, c.isDefault, c.id /* Group by all columns of c */
        ORDER BY transactionCount DESC
    """)
    fun getCategoriesWithTransactionCount(): Flow<List<CategoryWithCount>>

    @Query("SELECT id FROM categories WHERE type = :type AND bookId = :bookId LIMIT 1")
    suspend fun getCategoryIdByType(type: TransactionType, bookId: Int): Int?
}