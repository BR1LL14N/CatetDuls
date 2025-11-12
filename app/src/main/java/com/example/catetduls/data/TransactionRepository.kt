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

    /**
     * Mengambil semua transaksi terurut dari terbaru
     */
    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    /**
     * Mengambil transaksi berdasarkan ID
     */
    fun getTransactionById(id: Int): Flow<Transaction?> =
        transactionDao.getTransactionById(id)

    /**
     * Menambah transaksi baru
     */
    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    /**
     * Update transaksi yang sudah ada
     */
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    /**
     * Hapus transaksi
     */
    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    /**
     * Hapus semua transaksi (untuk fitur reset data)
     */
    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    // ========================================
    // Dashboard - Ringkasan Keuangan
    // ========================================

    /**
     * Menghitung total saldo (pemasukan - pengeluaran)
     */
    fun getTotalBalance(): Flow<Double?> =
        transactionDao.getTotalBalance()

    /**
     * Mengambil 5 transaksi terakhir untuk ditampilkan di Dashboard
     */
    fun getRecentTransactions(limit: Int = 5): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit)

    /**
     * Total pemasukan bulan ini
     */
    fun getTotalIncomeThisMonth(): Flow<Double?> {
        val calendar = Calendar.getInstance()
        val startOfMonth = calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfMonth = calendar.apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        return transactionDao.getTotalByTypeAndDateRange("Pemasukan", startOfMonth, endOfMonth)
    }

    /**
     * Total pengeluaran bulan ini
     */
    fun getTotalExpenseThisMonth(): Flow<Double?> {
        val calendar = Calendar.getInstance()
        val startOfMonth = calendar.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfMonth = calendar.apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        return transactionDao.getTotalByTypeAndDateRange("Pengeluaran", startOfMonth, endOfMonth)
    }

    // ========================================
    // Halaman Transaksi - Filter & Search
    // ========================================

    /**
     * Filter transaksi berdasarkan jenis (Pemasukan/Pengeluaran)
     */
    fun getTransactionsByType(type: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

    /**
     * Filter transaksi berdasarkan kategori
     */
    fun getTransactionsByCategory(categoryId: Int): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(categoryId)

    /**
     * Filter transaksi berdasarkan rentang tanggal
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    /**
     * Search transaksi berdasarkan catatan
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactions("%$query%")

    /**
     * Filter kombinasi: jenis + kategori + rentang tanggal
     */
    fun getFilteredTransactions(
        type: String? = null,
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

    /**
     * Total pengeluaran berdasarkan kategori (untuk Pie Chart)
     */
    fun getTotalExpenseByCategory(): Flow<List<CategoryExpense>> =
        transactionDao.getTotalExpenseByCategory()

    /**
     * Total pemasukan & pengeluaran per bulan (untuk Bar Chart)
     */
    fun getMonthlyTotals(year: Int): Flow<List<MonthlyTotal>> =
        transactionDao.getMonthlyTotals(year)

    /**
     * Kategori dengan pengeluaran terbesar
     */
    fun getTopExpenseCategory(): Flow<CategoryExpense?> =
        transactionDao.getTopExpenseCategory()

    /**
     * Total transaksi dalam periode tertentu
     */
    fun getTotalByTypeAndDateRange(
        type: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?> =
        transactionDao.getTotalByTypeAndDateRange(type, startDate, endDate)

    // ========================================
    // Helper Functions untuk Business Logic
    // ========================================

    /**
     * Mendapatkan rentang tanggal minggu ini
     */
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

    /**
     * Mendapatkan rentang tanggal bulan ini
     */
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

    /**
     * Validasi transaksi sebelum disimpan
     */
    fun validateTransaction(transaction: Transaction): ValidationResult {
        return when {
            transaction.amount <= 0 -> {
                ValidationResult.Error("Jumlah harus lebih dari 0")
            }
            transaction.type.isBlank() -> {
                ValidationResult.Error("Jenis transaksi harus dipilih")
            }
            transaction.categoryId == 0 -> {
                ValidationResult.Error("Kategori harus dipilih")
            }
            else -> ValidationResult.Success
        }
    }
}

// ========================================
// Data Classes untuk Statistik
// ========================================

/**
 * Data class untuk total pengeluaran per kategori
 * Digunakan untuk Pie Chart di halaman Statistik
 */
data class CategoryExpense(
    val categoryId: Int,
    val categoryName: String,
    val total: Double,
    val percentage: Float? = null
)

/**
 * Data class untuk total bulanan
 * Digunakan untuk Bar Chart di halaman Statistik
 */
data class MonthlyTotal(
    val month: Int,
    val year: Int,
    val income: Double,
    val expense: Double,
    val balance: Double
)

/**
 * Sealed class untuk hasil validasi
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}