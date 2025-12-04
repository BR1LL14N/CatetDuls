package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow
import java.lang.IllegalArgumentException

/**
 * Repository untuk operasi Kategori
 * Disesuaikan untuk mendukung mekanisme sinkronisasi offline-first.
 */
class CategoryRepository(
    private val categoryDao: CategoryDao
) {

    // ===================================
    // READ
    // ===================================

    fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories()

    fun getCategoriesByBookIdAndType(bookId: Int, type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByBookIdAndType(bookId, type)
    }

    fun getAllCategoriesByBook(bookId: Int): Flow<List<Category>> =
        categoryDao.getAllCategoriesByBook(bookId)

    fun searchCategories(bookId: Int, query: String, type: TransactionType?): Flow<List<Category>> {
        val searchQuery = "%$query%"
        return categoryDao.searchCategories(bookId, searchQuery, type)
    }

    suspend fun getAllCategoriesSync(): List<Category> {
        return categoryDao.getAllCategoriesSync()
    }

    fun getCategoryById(id: Int): Flow<Category?> {
        return categoryDao.getCategoryById(id)
    }

    // ===================================
    // CREATE
    // ===================================

    suspend fun insertCategory(category: Category) {
        if (!category.isValid()) {
            throw IllegalArgumentException("Nama kategori dan ID buku tidak boleh kosong")
        }


        val categoryToInsert = category.copy(
            isSynced = false,
            isDeleted = false,
            syncAction = "CREATE",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        categoryDao.insertCategory(categoryToInsert)
    }

    suspend fun insertAll(categories: List<Category>) {
        val categoriesToInsert = categories.map { category ->
            if (!category.isValid()) {
                throw IllegalArgumentException("Nama kategori dan ID buku tidak boleh kosong")
            }
            category.copy(
                isSynced = false,
                isDeleted = false,
                syncAction = "CREATE",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        categoryDao.insertAll(categoriesToInsert)
    }

    // ===================================
    // UPDATE
    // ===================================

    suspend fun updateCategory(category: Category) {
        if (!category.isValid()) {
            throw IllegalArgumentException("Nama kategori dan ID buku tidak boleh kosong")
        }


        val categoryToUpdate = category.copy(
            isSynced = false,
            isDeleted = false,
            syncAction = "UPDATE",
            updatedAt = System.currentTimeMillis()
        )

        categoryDao.updateCategory(categoryToUpdate)
    }

    // ===================================
    // DELETE
    // ===================================

    /**
     * Menandai kategori sebagai terhapus (soft delete) untuk disinkronkan ke server.
     * Jika kategori belum pernah disinkronkan (server_id null), maka hapus permanen lokal.
     * Catatan: Operasi ini menggantikan fungsi deleteCategory lama.
     */
    suspend fun deleteCategory(category: Category) {
        if (category.isDefault) {

            throw IllegalStateException("Kategori default tidak dapat dihapus.")
        }

        if (category.serverId == null) {

            categoryDao.deleteCategory(category)
        } else {

            val categoryToDelete = category.copy(
                isSynced = false,
                isDeleted = true,
                syncAction = "DELETE",
                updatedAt = System.currentTimeMillis()
            )
            categoryDao.updateCategory(categoryToDelete)
        }
    }

    /**
     * Menghapus kategori secara permanen berdasarkan ID
     * (Biasanya dipanggil hanya setelah sync DELETE berhasil)
     */
    suspend fun deleteCategoryByIdPermanently(categoryId: Int) {
        categoryDao.deleteCategoryById(categoryId)
    }

    // ===================================
    // SYNC METHODS (Dipanggil oleh Sync Worker)
    // ===================================

    /**
     * Mengambil semua kategori yang belum tersinkronisasi atau ditandai DELETE
     */
    suspend fun getUnsyncedCategories(): List<Category> {
        // Asumsi CategoryDao memiliki fungsi ini (mirip dengan BookDao)
        // Jika belum ada, tambahkan di CategoryDao: @Query("SELECT * FROM categories WHERE is_synced = 0 OR is_deleted = 1")
        // return categoryDao.getUnsyncedCategories()
        TODO("Implementasikan getUnsyncedCategories di CategoryDao dan Repository")
    }

    /**
     * Memperbarui status sinkronisasi setelah operasi server berhasil.
     */
    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long) {
        // Asumsi CategoryDao memiliki fungsi ini (mirip dengan BookDao)
        // categoryDao.updateSyncStatus(localId, serverId, lastSyncAt)
        TODO("Implementasikan updateSyncStatus di CategoryDao dan Repository")
    }

    /**
     * Menyimpan data kategori yang diterima dari server (untuk operasi PULL/READ dari server)
     */
    suspend fun saveFromRemote(category: Category) {
        categoryDao.insertCategory(category.copy(
            isSynced = true,
            isDeleted = false,
            syncAction = null,
            lastSyncAt = System.currentTimeMillis()
        ))
    }
}