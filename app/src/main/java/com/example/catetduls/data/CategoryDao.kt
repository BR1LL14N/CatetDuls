package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // ===================================
    // CREATE
    // ===================================

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    // ===================================
    // READ
    // ===================================

    @Query("SELECT * FROM categories WHERE bookId = :bookId ORDER BY name ASC")
    fun getAllCategories(bookId: Int): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE bookId = :bookId")
    suspend fun getAllCategoriesSync(bookId: Int): List<Category>

    @Query("SELECT * FROM categories WHERE bookId = :bookId ORDER BY name ASC")
    fun getAllCategoriesByBook(bookId: Int): Flow<List<Category>>

    @Query(
            """
        SELECT * FROM categories 
        WHERE bookId = :bookId 
        AND (name LIKE :query OR icon LIKE :query)
        AND (:type IS NULL OR type = :type)
        ORDER BY name ASC
    """
    )
    fun searchCategories(bookId: Int, query: String, type: TransactionType?): Flow<List<Category>>

    @Query(
            """
        SELECT * FROM categories 
        WHERE bookId = :bookId AND type = :type 
        ORDER BY name ASC
    """
    )
    fun getCategoriesByBookIdAndType(bookId: Int, type: TransactionType): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id") fun getCategoryById(id: Int): Flow<Category?>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryByIdSync(id: Int): Category?

    // ===================================
    // UPDATE
    // ===================================

    @Update suspend fun updateCategory(category: Category)

    @Query(
            "UPDATE categories SET is_synced = 0, sync_action = :action, updated_at = :updatedAt WHERE id = :id"
    )
    suspend fun markAsUnsynced(id: Int, action: String, updatedAt: Long)

    // ===================================
    // DELETE
    // ===================================

    @Delete suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :categoryId AND isDefault = 0")
    suspend fun deleteCategoryById(categoryId: Int)

    // ===================================
    // SYNC OPERATIONS
    // ===================================

    @Query("SELECT * FROM categories WHERE is_synced = 0 OR is_deleted = 1")
    suspend fun getUnsyncedCategories(): List<Category>

    @Query(
            """
        UPDATE categories 
        SET server_id = :serverId, 
            is_synced = 1, 
            last_sync_at = :lastSyncAt,
            sync_action = NULL
        WHERE id = :localId
    """
    )
    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long)

    @Query("SELECT * FROM categories WHERE server_id = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): Category?

    // ===================================
    // UTILITY/STATS
    // ===================================

    @Query("SELECT COUNT(*) > 0 FROM categories WHERE name = :name")
    suspend fun isCategoryNameExists(name: String): Boolean

    @Query("SELECT COUNT(*) FROM categories") suspend fun getCategoryCount(): Int

    @Query(
            """
        SELECT c.*, COUNT(t.id) as transactionCount
        FROM categories c
        LEFT JOIN transactions t ON c.id = t.categoryId
        WHERE c.bookId = :bookId
        GROUP BY c.id, c.name, c.icon, c.type, c.isDefault, c.id /* Group by all columns of c */
        ORDER BY transactionCount DESC
    """
    )
    fun getCategoriesWithTransactionCount(bookId: Int): Flow<List<CategoryWithCount>>

    @Query("SELECT id FROM categories WHERE type = :type AND bookId = :bookId LIMIT 1")
    suspend fun getCategoryIdByType(type: TransactionType, bookId: Int): Int?
}
