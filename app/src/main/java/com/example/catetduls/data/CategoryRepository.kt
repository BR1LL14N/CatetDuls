package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow
import java.lang.IllegalArgumentException
import javax.inject.Inject

/**
 * Repository untuk operasi Kategori
 * Disesuaikan untuk mendukung mekanisme sinkronisasi offline-first.
 */
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) : SyncRepository<Category> {

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

    override suspend fun deleteByIdPermanently(id: Long) {
        deleteCategoryByIdPermanently(id.toInt())
    }

    // ===================================
    // SYNC METHODS (Dipanggil oleh Sync Worker)
    // ===================================

    /**
     * Mengambil semua kategori yang belum tersinkronisasi atau ditandai DELETE
     */
    override suspend fun getAllUnsynced(): List<Category> {
        return categoryDao.getUnsyncedCategories()
    }

    /**
     * Memperbarui status sinkronisasi setelah operasi server berhasil.
     */
    override suspend fun updateSyncStatus(localId: Long, serverId: String, lastSyncAt: Long) {
        categoryDao.updateSyncStatus(localId.toInt(), serverId, lastSyncAt)
    }

    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long) {
        categoryDao.updateSyncStatus(localId, serverId, lastSyncAt)
    }

    /**
     * Menyimpan data kategori yang diterima dari server (untuk operasi PULL/READ dari server)
     */
    override suspend fun saveFromRemote(category: Category) {
        categoryDao.insertCategory(category.copy(
            isSynced = true,
            isDeleted = false,
            syncAction = null,
            lastSyncAt = System.currentTimeMillis()
        ))
    }

    override suspend fun getByServerId(serverId: String): Category? {
        return categoryDao.getByServerId(serverId)
    }

    suspend fun getCategoryIdByType(type: TransactionType, bookId: Int): Int? {
        return categoryDao.getCategoryIdByType(type, bookId)
    }

}