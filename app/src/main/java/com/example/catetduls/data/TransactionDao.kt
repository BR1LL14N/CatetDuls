package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) untuk Transaction
 * Interface ini berisi semua query database untuk tabel transactions
 */
@Dao
interface TransactionDao {

    // ========================================
    // CRUD Operations - Basic
    // ========================================

    /**
     * Insert transaksi baru
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    /**
     * Insert multiple transaksi sekaligus (untuk import data)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    /**
     * Update transaksi yang sudah ada
     */
    @Update
    suspend fun updateTransaction(transaction: Transaction)

    /**
     * Delete transaksi
     */
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    /**
     * Delete transaksi berdasarkan ID
     */
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Int)

    /**
     * Delete semua transaksi
     */
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    /**
     * Get semua transaksi, diurutkan dari terbaru
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    /**
     * Get transaksi berdasarkan ID
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

    /**
     * Get jumlah total transaksi
     */
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    // ========================================
    // Dashboard Queries
    // ========================================

    /**
     * Menghitung total saldo (pemasukan - pengeluaran)
     */
    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'PEMASUKAN' THEN amount ELSE -amount END), 0) 
        FROM transactions
    """)
    fun getTotalBalance(): Flow<Double?>

    /**
     * Get transaksi terbaru (untuk dashboard)
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 5): Flow<List<Transaction>>

    /**
     * Total berdasarkan tipe dan rentang tanggal
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) 
        FROM transactions 
        WHERE type = :type 
        AND date BETWEEN :startDate AND :endDate
    """)
    fun getTotalByTypeAndDateRange(
        type: TransactionType, // <-- DIPERBAIKI
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    // ========================================
    // Filter Queries (Halaman Transaksi)
    // ========================================

    /**
     * Filter berdasarkan tipe (Pemasukan/Pengeluaran)
     */
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> // <-- DIPERBAIKI

    /**
     * Filter berdasarkan kategori
     */
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Int): Flow<List<Transaction>>

    /**
     * Filter berdasarkan rentang tanggal
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    /**
     * Search transaksi berdasarkan notes
     */
    @Query("SELECT * FROM transactions WHERE notes LIKE :query ORDER BY date DESC")
    fun searchTransactions(query: String): Flow<List<Transaction>>

    /**
     * Filter kombinasi: type + kategori
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type AND categoryId = :categoryId 
        ORDER BY date DESC
    """)
    fun getTransactionsByTypeAndCategory(
        type: TransactionType, // <-- DIPERBAIKI
        categoryId: Int
    ): Flow<List<Transaction>>

    /**
     * Filter kombinasi: type + rentang tanggal
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
        AND date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    fun getTransactionsByTypeAndDateRange(
        type: TransactionType, // <-- DIPERBAIKI
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>

    /**
     * Filter kombinasi: type + kategori + rentang tanggal
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
        AND categoryId = :categoryId 
        AND date BETWEEN :startDate AND :endDate 
        ORDER BY date DESC
    """)
    fun getTransactionsByTypeAndCategoryAndDateRange(
        type: TransactionType, // <-- DIPERBAIKI
        categoryId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>

    // ========================================
    // Statistik Queries
    // ========================================

    /**
     * Total pengeluaran per kategori (untuk Pie Chart)
     * Join dengan tabel categories untuk mendapat nama kategori
     */
    @Query("""
        SELECT 
            t.categoryId,
            c.name as categoryName,
            SUM(t.amount) as total
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'PENGELUARAN' 
        GROUP BY t.categoryId, c.name
        ORDER BY total DESC
    """) // <-- DIPERBAIKI
    fun getTotalExpenseByCategory(): Flow<List<CategoryExpense>>

    /**
     * Kategori dengan pengeluaran terbesar
     */
    @Query("""
        SELECT 
            t.categoryId,
            c.name as categoryName,
            SUM(t.amount) as total
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'PENGELUARAN' 
        GROUP BY t.categoryId, c.name
        ORDER BY total DESC
        LIMIT 1
    """) // <-- DIPERBAIKI
    fun getTopExpenseCategory(): Flow<CategoryExpense?>

    /**
     * Total pemasukan dan pengeluaran per bulan dalam setahun
     * Untuk Bar Chart bulanan
     */
    @Query("""
        SELECT 
            CAST(strftime('%m', date/1000, 'unixepoch') AS INTEGER) as month,
            CAST(strftime('%Y', date/1000, 'unixepoch') AS INTEGER) as year,
            COALESCE(SUM(CASE WHEN type = 'PEMASUKAN' THEN amount ELSE 0 END), 0) as income,
            COALESCE(SUM(CASE WHEN type = 'PENGELUARAN' THEN amount ELSE 0 END), 0) as expense,
            COALESCE(SUM(CASE WHEN type = 'PEMASUKAN' THEN amount ELSE -amount END), 0) as balance
        FROM transactions
        WHERE CAST(strftime('%Y', date/1000, 'unixepoch') AS INTEGER) = :year
        GROUP BY month, year
        ORDER BY month ASC
    """) // <-- DIPERBAIKI
    fun getMonthlyTotals(year: Int): Flow<List<MonthlyTotal>>

    /**
     * Total transaksi per hari dalam sebulan (untuk detail statistik)
     */
    @Query("""
        SELECT 
            date,
            SUM(CASE WHEN type = 'PEMASUKAN' THEN amount ELSE 0 END) as income,
            SUM(CASE WHEN type = 'PENGELUARAN' THEN amount ELSE 0 END) as expense
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date ASC
    """) // <-- DIPERBAIKI
    fun getDailyTotals(startDate: Long, endDate: Long): Flow<List<DailyTotal>>

    /**
     * Statistik kategori detail (jumlah transaksi + total)
     */
    @Query("""
        SELECT 
            t.categoryId,
            c.name as categoryName,
            COUNT(*) as transactionCount,
            SUM(t.amount) as total,
            AVG(t.amount) as average
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = :type
        GROUP BY t.categoryId, c.name
        ORDER BY total DESC
    """)
    fun getCategoryStats(type: TransactionType): Flow<List<CategoryStats>> // <-- DIPERBAIKI

    // ========================================
    // Utility Queries
    // ========================================

    /**
     * Cek apakah ada transaksi dalam rentang tanggal
     */
    @Query("""
        SELECT COUNT(*) > 0 
        FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun hasTransactionsInDateRange(startDate: Long, endDate: Long): Boolean

    /**
     * Get tanggal transaksi pertama (untuk menentukan range data)
     */
    @Query("SELECT MIN(date) FROM transactions")
    suspend fun getFirstTransactionDate(): Long?

    /**
     * Get tanggal transaksi terakhir
     */
    @Query("SELECT MAX(date) FROM transactions")
    suspend fun getLastTransactionDate(): Long?

    /**
     * Get total amount berdasarkan kategori dalam rentang waktu
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE categoryId = :categoryId
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalByCategoryAndDateRange(
        categoryId: Int,
        startDate: Long,
        endDate: Long
    ): Double
}

// ========================================
// Data Classes untuk Query Results
// ========================================