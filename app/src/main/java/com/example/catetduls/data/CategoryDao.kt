package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    /**
     * Insert kategori baru
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    /**
     * Insert multiple categories (untuk data awal)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    /**
     * Update kategori
     */
    @Update
    suspend fun updateCategory(category: Category)

    /**
     * Delete kategori
     */
    @Delete
    suspend fun deleteCategory(category: Category)

    /**
     * Delete berdasarkan ID
     */
    @Query("DELETE FROM categories WHERE id = :categoryId AND isDefault = 0")
    suspend fun deleteCategoryById(categoryId: Int)

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
    fun getCategoriesByBookIdAndType(bookId: Int, type: TransactionType): Flow<List<Category>> // <-- FUNGSI YANG DIPANGGIL VIEwMODEL
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
}

