package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * Repository adalah perantara antara ViewModel dan DAO.
 * Ini adalah "best practice" arsitektur MVVM.
 * ViewModel HANYA boleh tahu tentang Repository, tidak boleh tahu tentang DAO.
 *
 * Repository bertanggung jawab untuk:
 * - Menyediakan abstraksi data dari berbagai sumber (database, API, dll)
 * - Melakukan operasi bisnis logic jika diperlukan
 * - Menyediakan data dalam bentuk yang siap digunakan oleh ViewModel
 */
class TransactionRepository(
    private val transactionDao: TransactionDao
) {

    // ========================================
    // CRUD Operations - Basic
    // ========================================

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    fun getTransactionById(id: Int): Flow<Transaction?> =
        transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
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
        // --- DIPERBAIKI ---
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

    suspend fun insertAll(transactions: List<Transaction>) {
        transactionDao.insertAll(transactions)
    }

    // ========================================
    // Halaman Transaksi - Filter & Search
    // ========================================

    /**
     * Filter transaksi berdasarkan jenis (Pemasukan/Pengeluaran)
     */

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

    fun getTransactionsByCategory(categoryId: Int): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(categoryId)

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    /**
     * Search transaksi berdasarkan catatan
     * Catatan: Ini HANYA akan berfungsi jika DAO Anda adalah:
     * @Query("... WHERE notes LIKE :query ...")
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> {
        // Menambahkan wildcard % di sini
        return transactionDao.searchTransactions("%$query%")
    }

    /**
     * Filter kombinasi: jenis + kategori + rentang tanggal
     */

    fun getFilteredTransactions(
        type: TransactionType? = null, // <-- Diperbaiki
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
        type: TransactionType, // <-- Diperbaiki
        startDate: Long,
        endDate: Long
    ): Flow<Double?> =
        transactionDao.getTotalByTypeAndDateRange(type, startDate, endDate)



    fun getMonthlyDailySummary(startDate: Long, endDate: Long): Flow<List<DailySummary>> {
        // ASUMSI: transactionDao memiliki fungsi getDailySummaries(startDate, endDate)
        // yang mengembalikan List<DailySummary>
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