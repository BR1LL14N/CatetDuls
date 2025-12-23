package com.example.catetduls.data

/**
 * Repository untuk operasi Kategori Disesuaikan untuk mendukung mekanisme sinkronisasi
 * offline-first.
 */
import java.lang.IllegalArgumentException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Repository untuk operasi Kategori Disesuaikan untuk mendukung mekanisme sinkronisasi
 * offline-first.
 */
class CategoryRepository
@Inject
constructor(private val categoryDao: CategoryDao, private val bookRepository: BookRepository) :
        SyncRepository<Category> {

    private suspend fun getActiveBookId(): Int {
        return bookRepository.getActiveBookSync()?.id ?: 1
    }

    // ===================================
    // READ
    // ===================================

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllCategories(): Flow<List<Category>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                categoryDao.getAllCategories(book?.id ?: 1)
            }

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
        return categoryDao.getAllCategoriesSync(getActiveBookId())
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

        val categoryToInsert =
                category.copy(
                        isSynced = false,
                        isDeleted = false,
                        syncAction = "CREATE",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                )
        // Ensure bookId is set
        val currentBookId =
                if (categoryToInsert.bookId == 0) getActiveBookId() else categoryToInsert.bookId
        val finalCategory = categoryToInsert.copy(bookId = currentBookId)
        categoryDao.insertCategory(finalCategory)
    }

    suspend fun insertAll(categories: List<Category>) {
        val categoriesToInsert =
                categories.map { category ->
                    if (!category.isValid()) {
                        throw IllegalArgumentException(
                                "Nama kategori dan ID buku tidak boleh kosong"
                        )
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

    suspend fun getCategoryByIdSync(id: Int): Category? {
        return categoryDao.getCategoryByIdSync(id) // Anda perlu buat query ini di DAO
    }

    // ===================================
    // UPDATE
    // ===================================

    suspend fun updateCategory(category: Category) {
        if (!category.isValid()) {
            throw IllegalArgumentException("Nama kategori dan ID buku tidak boleh kosong")
        }

        val categoryToUpdate =
                category.copy(
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
     * Menandai kategori sebagai terhapus (soft delete) untuk disinkronkan ke server. Jika kategori
     * belum pernah disinkronkan (server_id null), maka hapus permanen lokal. Catatan: Operasi ini
     * menggantikan fungsi deleteCategory lama.
     */
    suspend fun deleteCategory(category: Category) {
        if (category.isDefault) {

            throw IllegalStateException("Kategori default tidak dapat dihapus.")
        }

        if (category.serverId == null) {

            categoryDao.deleteCategory(category)
        } else {

            val categoryToDelete =
                    category.copy(
                            isSynced = false,
                            isDeleted = true,
                            syncAction = "DELETE",
                            updatedAt = System.currentTimeMillis()
                    )
            categoryDao.updateCategory(categoryToDelete)
        }
    }

    /**
     * Menghapus kategori secara permanen berdasarkan ID (Biasanya dipanggil hanya setelah sync
     * DELETE berhasil)
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

    /** Mengambil semua kategori yang belum tersinkronisasi atau ditandai DELETE */
    override suspend fun getAllUnsynced(): List<Category> {
        return categoryDao.getUnsyncedCategories()
    }

    /** Memperbarui status sinkronisasi setelah operasi server berhasil. */
    override suspend fun updateSyncStatus(id: Long, serverId: String, syncedAt: Long) {
        categoryDao.updateSyncStatus(id.toInt(), serverId, syncedAt)
    }

    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long) {
        categoryDao.updateSyncStatus(localId, serverId, lastSyncAt)
    }

    /** Menyimpan data kategori yang diterima dari server (untuk operasi PULL/READ dari server) */
    override suspend fun saveFromRemote(entity: Category) {
        categoryDao.insertCategory(
                entity.copy(
                        isSynced = true,
                        isDeleted = false,
                        syncAction = null,
                        lastSyncAt = System.currentTimeMillis()
                )
        )
    }

    override suspend fun getByServerId(serverId: String): Category? {
        return categoryDao.getByServerId(serverId)
    }

    override suspend fun markAsUnsynced(id: Long, action: String) {
        categoryDao.markAsUnsynced(id.toInt(), action, System.currentTimeMillis())
    }

    suspend fun getCategoryIdByType(type: TransactionType, bookId: Int): Int? {
        return categoryDao.getCategoryIdByType(type, bookId)
    }
}
