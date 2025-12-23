package com.example.catetduls.data

import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Repository adalah perantara antara ViewModel dan DAO. Disesuaikan untuk mendukung mekanisme
 * sinkronisasi offline-first.
 */
class TransactionRepository
@Inject
constructor(
        private val transactionDao: TransactionDao,
        private val bookRepository: BookRepository
) : SyncRepository<Transaction> {

    // Helper untuk suspend functions
    private suspend fun getActiveBookId(): Int {
        return bookRepository.getActiveBookSync()?.id ?: 1
    }

    // ========================================
    // READ Operations
    // ========================================

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllTransactions(): Flow<List<Transaction>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                transactionDao.getAllTransactions(book?.id ?: 1)
            }

    fun getTransactionById(id: Int): Flow<Transaction?> = transactionDao.getTransactionById(id)

    suspend fun getSingleTransactionById(id: Int): Transaction? {
        // Menggunakan first() atau singleOrNull() untuk mendapatkan nilai pertama/tunggal dari Flow
        return transactionDao.getTransactionById(id).firstOrNull()
    }

    suspend fun getTransactionCountByBookId(bookId: Int): Int {
        return transactionDao.getTransactionCountByBookId(bookId)
    }

    // ========================================
    // CREATE (Penandaan Sync: CREATE)
    // ========================================

    suspend fun insertTransaction(transaction: Transaction) {
        if (!transaction.isValid()) {
            throw IllegalArgumentException(
                    "Transaksi tidak valid: Jumlah, kategori, atau dompet kosong."
            )
        }

        // OTOMATIS ISI bookId DARI STATE APLIKASI
        val currentBookId = if (transaction.bookId == 0) getActiveBookId() else transaction.bookId
        val transactionToInsert =
                transaction.copy(
                        bookId = currentBookId, // <--- TAMBAHAN PENTING
                        isSynced = false,
                        isDeleted = false,
                        syncAction = "CREATE",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        imagePath = transaction.imagePath
                )
        transactionDao.insertTransaction(transactionToInsert)
    }

    suspend fun insertAll(transactions: List<Transaction>) {
        val transactionsToInsert =
                transactions.map { transaction ->
                    if (!transaction.isValid()) {
                        throw IllegalArgumentException(
                                "Transaksi tidak valid: Jumlah, kategori, atau dompet kosong."
                        )
                    }
                    val currentBookId =
                            if (transaction.bookId == 0) getActiveBookId() else transaction.bookId
                    transaction.copy(
                            bookId = currentBookId, // <--- TAMBAHAN PENTING
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
            throw IllegalArgumentException(
                    "Transaksi tidak valid: Jumlah, kategori, atau dompet kosong."
            )
        }

        // Menentukan status sinkronisasi untuk UPDATE
        val currentBookId = if (transaction.bookId == 0) getActiveBookId() else transaction.bookId
        val transactionToUpdate =
                transaction.copy(
                        bookId = currentBookId, // <--- Jaga-jaga jika 0
                        isSynced = false,
                        isDeleted = false,
                        syncAction = "UPDATE",
                        updatedAt = System.currentTimeMillis(),
                        imagePath = transaction.imagePath
                )
        transactionDao.updateTransaction(transactionToUpdate)
    }

    // ========================================
    // DELETE (Penandaan Sync: DELETE / Hapus Permanen)
    // ========================================

    /**
     * Menandai transaksi sebagai terhapus (soft delete) untuk disinkronkan ke server. Jika
     * transaksi belum pernah disinkronkan (server_id null), maka hapus permanen lokal.
     */
    suspend fun deleteTransaction(transaction: Transaction) {
        if (transaction.serverId == null) {
            transactionDao.deleteTransaction(transaction)
        } else {
            val transactionToDelete =
                    transaction.copy(
                            isSynced = false,
                            isDeleted = true,
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
        // Hapus transaksi HANYA untuk buku yang sedang aktif
        transactionDao.deleteTransactionsByBook(getActiveBookId())
    }

    // ========================================
    // SYNC METHODS (Dipanggil oleh Sync Worker)
    // ========================================

    /** Mengambil semua transaksi yang perlu disinkronkan (CREATE, UPDATE, DELETE). */
    override suspend fun getAllUnsynced(): List<Transaction> {
        return transactionDao.getAllUnsyncedTransactions()
    }

    /** Memperbarui status sinkronisasi setelah operasi server berhasil (CREATE/UPDATE). */
    override suspend fun updateSyncStatus(id: Long, serverId: String, syncedAt: Long) {
        transactionDao.updateSyncStatus(id.toInt(), serverId, syncedAt)
    }

    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long) {
        transactionDao.updateSyncStatus(localId, serverId, lastSyncAt)
    }

    /** Menyimpan data transaksi yang diterima dari server (untuk operasi PULL/READ dari server) */
    override suspend fun saveFromRemote(entity: Transaction) {
        // Cek data lama berdasarkan serverId
        val existingTransaction =
                if (entity.serverId != null) {
                    transactionDao.getByServerId(entity.serverId)
                } else {
                    null
                }

        val existingPath = existingTransaction?.imagePath

        // Simpan ke DB Lokal
        val transactionToSave =
                entity.copy(
                        isSynced = true,
                        isDeleted = false,
                        syncAction = null,
                        lastSyncAt = System.currentTimeMillis(),
                        imagePath = existingPath ?: entity.imagePath,
                        // Handle Nullability 'notes' agar tidak crash
                        notes = entity.notes ?: ""
                )

        transactionDao.insertTransaction(transactionToSave)
    }

    override suspend fun getByServerId(serverId: String): Transaction? {
        return transactionDao.getByServerId(serverId)
    }

    override suspend fun markAsUnsynced(id: Long, action: String) {
        transactionDao.markAsUnsynced(id.toInt(), action, System.currentTimeMillis())
    }

    /** Membersihkan transaksi yang sudah berhasil di-sync delete ke server */
    suspend fun cleanupSyncedDeletes() {
        transactionDao.cleanupSyncedDeletes()
    }

    // ========================================
    // Dashboard - Ringkasan Keuangan
    // ========================================

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTotalBalance(): Flow<Double?> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                transactionDao.getTotalBalance(book?.id ?: 1)
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getRecentTransactions(limit: Int = 5): Flow<List<Transaction>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                transactionDao.getRecentTransactions(book?.id ?: 1, limit)
            }

    /** Total pemasukan bulan ini */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTotalIncomeThisMonth(): Flow<Double?> {
        val (startOfMonth, endOfMonth) = getThisMonthDateRange()
        return bookRepository.getActiveBook().flatMapLatest { book ->
            transactionDao.getTotalByTypeAndDateRange(
                    book?.id ?: 1,
                    TransactionType.PEMASUKAN,
                    startOfMonth,
                    endOfMonth
            )
        }
    }

    /** Total pengeluaran bulan ini */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTotalExpenseThisMonth(): Flow<Double?> {
        val (startOfMonth, endOfMonth) = getThisMonthDateRange()
        return bookRepository.getActiveBook().flatMapLatest { book ->
            transactionDao.getTotalByTypeAndDateRange(
                    book?.id ?: 1,
                    TransactionType.PENGELUARAN,
                    startOfMonth,
                    endOfMonth
            )
        }
    }

    // ========================================
    // Halaman Transaksi - Filter & Search
    // ========================================

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                transactionDao.getTransactionsByType(book?.id ?: 1, type)
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTransactionsByCategory(categoryId: Int): Flow<List<Transaction>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                transactionDao.getTransactionsByCategory(book?.id ?: 1, categoryId)
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                transactionDao.getTransactionsByDateRange(book?.id ?: 1, startDate, endDate)
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTransactionsByCategoryAndDateRange(
            categoryId: Int,
            startDate: Long,
            endDate: Long
    ): Flow<List<Transaction>> {
        return bookRepository.getActiveBook().flatMapLatest { book ->
            transactionDao.getTransactionsByCategoryAndDateRange(
                    book?.id ?: 1,
                    categoryId,
                    startDate,
                    endDate
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTransactionsByTypeAndDateRange(
            type: TransactionType,
            startDate: Long,
            endDate: Long
    ): Flow<List<Transaction>> {
        return bookRepository.getActiveBook().flatMapLatest { book ->
            transactionDao.getTransactionsByTypeAndDateRange(
                    book?.id ?: 1,
                    type,
                    startDate,
                    endDate
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return bookRepository.getActiveBook().flatMapLatest { book ->
            transactionDao.searchTransactions(book?.id ?: 1, "%$query%")
        }
    }

    /** Filter kombinasi: jenis + kategori + rentang tanggal */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFilteredTransactions(
            type: TransactionType? = null,
            categoryId: Int? = null,
            startDate: Long? = null,
            endDate: Long? = null
    ): Flow<List<Transaction>> {
        return bookRepository.getActiveBook().flatMapLatest { book ->
            val bookId = book?.id ?: 1
            when {
                type != null && categoryId != null && startDate != null && endDate != null -> {
                    transactionDao.getTransactionsByTypeAndCategoryAndDateRange(
                            bookId,
                            type,
                            categoryId,
                            startDate,
                            endDate
                    )
                }
                type != null && startDate != null && endDate != null -> {
                    transactionDao.getTransactionsByTypeAndDateRange(
                            bookId,
                            type,
                            startDate,
                            endDate
                    )
                }
                type == null && categoryId != null && startDate != null && endDate != null -> {
                    transactionDao.getTransactionsByCategoryAndDateRange(
                            bookId,
                            categoryId,
                            startDate,
                            endDate
                    )
                }
                type != null && categoryId != null -> {
                    transactionDao.getTransactionsByTypeAndCategory(bookId, type, categoryId)
                }
                type != null -> transactionDao.getTransactionsByType(bookId, type)
                categoryId != null -> transactionDao.getTransactionsByCategory(bookId, categoryId)
                startDate != null && endDate != null ->
                        transactionDao.getTransactionsByDateRange(bookId, startDate, endDate)
                else -> transactionDao.getAllTransactions(bookId)
            }
        }
    }

    // ========================================
    // Halaman Statistik - Analisis Data
    // ========================================

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTotalExpenseByCategory(): Flow<List<CategoryExpense>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                transactionDao.getTotalExpenseByCategory(book?.id ?: 1)
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMonthlyTotals(year: Int): Flow<List<MonthlyTotal>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                transactionDao.getMonthlyTotals(book?.id ?: 1, year)
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTopExpenseCategory(): Flow<CategoryExpense?> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                transactionDao.getTopExpenseCategory(book?.id ?: 1)
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTotalByTypeAndDateRange(
            type: TransactionType,
            startDate: Long,
            endDate: Long
    ): Flow<Double?> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                transactionDao.getTotalByTypeAndDateRange(book?.id ?: 1, type, startDate, endDate)
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMonthlyDailySummary(startDate: Long, endDate: Long): Flow<List<DailySummary>> {
        return bookRepository.getActiveBook().flatMapLatest { book ->
            transactionDao.getDailySummaries(book?.id ?: 1, startDate, endDate)
        }
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
