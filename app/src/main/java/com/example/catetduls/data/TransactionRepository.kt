package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import javax.inject.Inject

/**
 * Repository adalah perantara antara ViewModel dan DAO.
 * Disesuaikan untuk mendukung mekanisme sinkronisasi offline-first.
 */
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) : SyncRepository<Transaction> {

    // ========================================
    // READ Operations
    // ========================================

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    fun getTransactionById(id: Int): Flow<Transaction?> =
        transactionDao.getTransactionById(id)

    suspend fun getSingleTransactionById(id: Int): Transaction? {
        // Menggunakan first() atau singleOrNull() untuk mendapatkan nilai pertama/tunggal dari Flow
        return transactionDao.getTransactionById(id).firstOrNull()
    }

    // ========================================
    // CREATE (Penandaan Sync: CREATE)
    // ========================================

    suspend fun insertTransaction(transaction: Transaction) {
        if (!transaction.isValid()) {
            throw IllegalArgumentException("Transaksi tidak valid: Jumlah, kategori, atau dompet kosong.")
        }

        // Menentukan status sinkronisasi untuk CREATE
        val transactionToInsert = transaction.copy(
            isSynced = false,
            isDeleted = false,
            syncAction = "CREATE", // Aksi yang perlu dilakukan server
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            imagePath = transaction.imagePath
        )
        transactionDao.insertTransaction(transactionToInsert)
    }

    suspend fun insertAll(transactions: List<Transaction>) {
        val transactionsToInsert = transactions.map { transaction ->
            if (!transaction.isValid()) {
                throw IllegalArgumentException("Transaksi tidak valid: Jumlah, kategori, atau dompet kosong.")
            }
            transaction.copy(
                isSynced = false,
                isDeleted = false,
                syncAction = "CREATE",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                imagePath = transaction.imagePath
            )
        }
        transactionDao.insertAll(transactionsToInsert)
    }

    // ========================================
    // UPDATE (Penandaan Sync: UPDATE)
    // ========================================

    suspend fun updateTransaction(transaction: Transaction) {
        if (!transaction.isValid()) {
            throw IllegalArgumentException("Transaksi tidak valid: Jumlah, kategori, atau dompet kosong.")
        }

        // Menentukan status sinkronisasi untuk UPDATE
        val transactionToUpdate = transaction.copy(
            isSynced = false,
            isDeleted = false,
            syncAction = "UPDATE", // Aksi yang perlu dilakukan server
            updatedAt = System.currentTimeMillis(),
            imagePath = transaction.imagePath
        )
        transactionDao.updateTransaction(transactionToUpdate)
    }

    // ========================================
    // DELETE (Penandaan Sync: DELETE / Hapus Permanen)
    // ========================================

    /**
     * Menandai transaksi sebagai terhapus (soft delete) untuk disinkronkan ke server.
     * Jika transaksi belum pernah disinkronkan (server_id null), maka hapus permanen lokal.
     */
    suspend fun deleteTransaction(transaction: Transaction) {
        if (transaction.serverId == null) {
            // Jika belum pernah disinkronkan, hapus permanen dari lokal
            transactionDao.deleteTransaction(transaction)
        } else {
            // Jika sudah ada di server, tandai sebagai deleted dan update
            val transactionToDelete = transaction.copy(
                isSynced = false,
                isDeleted = true, // Tanda bahwa ini harus dihapus di server
                syncAction = "DELETE",
                updatedAt = System.currentTimeMillis()
            )
            transactionDao.updateTransaction(transactionToDelete)
        }
    }

    /**
     * Menghapus transaksi secara permanen berdasarkan ID (Dipanggil setelah sync DELETE berhasil)
     */
    suspend fun deleteByIdPermanently(transactionId: Int) {
        transactionDao.deleteById(transactionId)
    }

    override suspend fun deleteByIdPermanently(id: Long) {
        deleteByIdPermanently(id.toInt())
    }

    suspend fun deleteAllTransactions() {
        // Jika menggunakan sync, ini harus dilakukan dengan hati-hati.
        // Opsi 1: Menghapus semua yang belum sync, dan menandai yang sudah sync sebagai delete.
        // Opsi 2: Hapus permanen lokal (untuk reset database).
        // Kita gunakan versi DAO yang ada (Hard Delete All)
        transactionDao.deleteAllTransactions()
    }

    // ========================================
    // SYNC METHODS (Dipanggil oleh Sync Worker)
    // ========================================

    /**
     * Mengambil semua transaksi yang perlu disinkronkan (CREATE, UPDATE, DELETE).
     */
    override suspend fun getAllUnsynced(): List<Transaction> {
        return transactionDao.getAllUnsyncedTransactions()
    }

    /**
     * Memperbarui status sinkronisasi setelah operasi server berhasil (CREATE/UPDATE).
     */
    override suspend fun updateSyncStatus(localId: Long, serverId: String, lastSyncAt: Long) {
        transactionDao.updateSyncStatus(localId.toInt(), serverId, lastSyncAt)
    }

    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long) {
        transactionDao.updateSyncStatus(localId, serverId, lastSyncAt)
    }

    /**
     * Menyimpan data transaksi yang diterima dari server (untuk operasi PULL/READ dari server)
     */
    override suspend fun saveFromRemote(transaction: Transaction) {
        val existingPath = transactionDao.getByServerId(transaction.serverId ?: "")?.imagePath
        transactionDao.insertTransaction(transaction.copy(
            isSynced = true,
            isDeleted = false,
            syncAction = null,
            lastSyncAt = System.currentTimeMillis(),
            imagePath = existingPath ?: transaction.imagePath
        ))
    }

    override suspend fun getByServerId(serverId: String): Transaction? {
        return transactionDao.getByServerId(serverId)
    }

    /**
     * Membersihkan transaksi yang sudah berhasil di-sync delete ke server
     */
    suspend fun cleanupSyncedDeletes() {
        transactionDao.cleanupSyncedDeletes()
    }

    // ========================================
    // Dashboard - Ringkasan Keuangan
    // ========================================

    fun getTotalBalance(): Flow<Double?> =
        transactionDao.getTotalBalance()

    fun getRecentTransactions(limit: Int = 5): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit)

    /**
     * Total pemasukan bulan ini
     */
    fun getTotalIncomeThisMonth(): Flow<Double?> {
        val (startOfMonth, endOfMonth) = getThisMonthDateRange()
        return transactionDao.getTotalByTypeAndDateRange(
            TransactionType.PEMASUKAN,
            startOfMonth,
            endOfMonth
        )
    }

    /**
     * Total pengeluaran bulan ini
     */
    fun getTotalExpenseThisMonth(): Flow<Double?> {
        val (startOfMonth, endOfMonth) = getThisMonthDateRange()
        return transactionDao.getTotalByTypeAndDateRange(
            TransactionType.PENGELUARAN,
            startOfMonth,
            endOfMonth
        )
    }

    // ========================================
    // Halaman Transaksi - Filter & Search
    // ========================================

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

    fun getTransactionsByCategory(categoryId: Int): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(categoryId)

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    fun getTransactionsByCategoryAndDateRange(
        categoryId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategoryAndDateRange(categoryId, startDate, endDate)
    }

    fun getTransactionsByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByTypeAndDateRange(type, startDate, endDate)
    }

    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions("%$query%")
    }

    /**
     * Filter kombinasi: jenis + kategori + rentang tanggal
     */
    fun getFilteredTransactions(
        type: TransactionType? = null,
        categoryId: Int? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<Transaction>> {
        return when {

            type != null && categoryId != null && startDate != null && endDate != null -> {
                transactionDao.getTransactionsByTypeAndCategoryAndDateRange(
                    type, categoryId, startDate, endDate
                )
            }

            type != null && startDate != null && endDate != null -> {
                transactionDao.getTransactionsByTypeAndDateRange(type, startDate, endDate)
            }

            type == null && categoryId != null && startDate != null && endDate != null -> {
                getTransactionsByCategoryAndDateRange(categoryId, startDate, endDate)
            }

            type != null && categoryId != null -> {
                transactionDao.getTransactionsByTypeAndCategory(type, categoryId)
            }

            type != null -> getTransactionsByType(type)

            categoryId != null -> getTransactionsByCategory(categoryId)

            startDate != null && endDate != null -> getTransactionsByDateRange(startDate, endDate)

            else -> getAllTransactions()
        }
    }

    // ========================================
    // Halaman Statistik - Analisis Data
    // ========================================

    fun getTotalExpenseByCategory(): Flow<List<CategoryExpense>> =
        transactionDao.getTotalExpenseByCategory()

    fun getMonthlyTotals(year: Int): Flow<List<MonthlyTotal>> =
        transactionDao.getMonthlyTotals(year)

    fun getTopExpenseCategory(): Flow<CategoryExpense?> =
        transactionDao.getTopExpenseCategory()

    fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<Double?> =
        transactionDao.getTotalByTypeAndDateRange(type, startDate, endDate)

    fun getMonthlyDailySummary(startDate: Long, endDate: Long): Flow<List<DailySummary>> {
        return transactionDao.getDailySummaries(startDate, endDate)
    }

    // ========================================
    // Helper Functions untuk Business Logic
    // ========================================

    fun getThisWeekDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfWeek = calendar.timeInMillis

        return Pair(startOfWeek, endOfWeek)
    }

    fun getThisMonthDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = calendar.timeInMillis

        return Pair(startOfMonth, endOfMonth)
    }

    fun validateTransaction(transaction: Transaction): ValidationResult {
        return when {
            transaction.amount <= 0 -> {
                ValidationResult.Error("Jumlah harus lebih dari 0")
            }
            transaction.categoryId == 0 -> {
                ValidationResult.Error("Kategori harus dipilih")
            }
            else -> ValidationResult.Success
        }

    }
}

// Catatan: Anda perlu memastikan `ValidationResult`, `DailySummary`,
// `CategoryExpense`, dan `MonthlyTotal` didefinisikan di tempat lain dalam proyek Anda.