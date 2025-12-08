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


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)


    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Int)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>


    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    // ========================================
    // SYNC OPERATIONS - PERBAIKAN DAN TAMBAHAN
    // ========================================

    @Query("SELECT * FROM transactions WHERE is_synced = 0")
    suspend fun getPendingSyncTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE server_id = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): Transaction?

    @Query("""
        UPDATE transactions 
        SET server_id = :serverId, 
            is_synced = 1, 
            last_sync_at = :lastSyncAt,
            sync_action = NULL
        WHERE id = :localId
    """)
    suspend fun updateSyncStatus(
        localId: Int,
        serverId: String,
        lastSyncAt: Long
    )

    @Query("DELETE FROM transactions WHERE is_deleted = 1 AND is_synced = 1")
    suspend fun cleanupSyncedDeletes(): Unit

    @Query("""
        UPDATE transactions 
        SET is_synced = 0, 
            sync_action = :action, 
            updated_at = :updatedAt 
        WHERE id = :id
    """)
    suspend fun markAsUnsynced(id: Int, action: String, updatedAt: Long)

    @Query("SELECT * FROM transactions WHERE is_deleted = 1 AND is_synced = 0")
    suspend fun getDeletedTransactions(): List<Transaction>


    @Query("SELECT * FROM transactions WHERE is_synced = 0")
    suspend fun getAllUnsyncedTransactions(): List<Transaction>

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
        WHERE is_deleted = 0
    """)
    fun getTotalBalance(): Flow<Double?>

    /**
     * Get transaksi terbaru (untuk dashboard)
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0
        ORDER BY date DESC 
        LIMIT :limit
    """)
    fun getRecentTransactions(limit: Int = 5): Flow<List<Transaction>>

    /**
     * Total berdasarkan tipe dan rentang tanggal
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) 
        FROM transactions 
        WHERE type = :type 
        AND date BETWEEN :startDate AND :endDate
        AND is_deleted = 0
    """)
    fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    // ========================================
    // Filter Queries (Halaman Transaksi)
    // ========================================

    /**
     * Filter berdasarkan tipe (Pemasukan/Pengeluaran)
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
        AND is_deleted = 0
        ORDER BY date DESC
    """)
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    /**
     * Filter berdasarkan kategori
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE categoryId = :categoryId 
        AND is_deleted = 0
        ORDER BY date DESC
    """)
    fun getTransactionsByCategory(categoryId: Int): Flow<List<Transaction>>

    /**
     * Filter berdasarkan rentang tanggal
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate 
        AND is_deleted = 0
        ORDER BY date DESC
    """)
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    /**
     * Filter kombinasi: categoryId + rentang tanggal
     */
    @Query("""
    SELECT * FROM transactions 
    WHERE categoryId = :categoryId 
    AND date BETWEEN :startDate AND :endDate 
    AND is_deleted = 0
    ORDER BY date DESC
""")
    fun getTransactionsByCategoryAndDateRange(
        categoryId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>

    /**
     * Search transaksi berdasarkan notes
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE notes LIKE :query 
        AND is_deleted = 0
        ORDER BY date DESC
    """)
    fun searchTransactions(query: String): Flow<List<Transaction>>

    /**
     * Filter kombinasi: type + kategori
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
        AND categoryId = :categoryId 
        AND is_deleted = 0
        ORDER BY date DESC
    """)
    fun getTransactionsByTypeAndCategory(
        type: TransactionType,
        categoryId: Int
    ): Flow<List<Transaction>>

    /**
     * Filter kombinasi: type + rentang tanggal
     */
    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
        AND date BETWEEN :startDate AND :endDate 
        AND is_deleted = 0
        ORDER BY date DESC
    """)
    fun getTransactionsByTypeAndDateRange(
        type: TransactionType,
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
        AND is_deleted = 0
        ORDER BY date DESC
    """)
    fun getTransactionsByTypeAndCategoryAndDateRange(
        type: TransactionType,
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
        AND t.is_deleted = 0
        GROUP BY t.categoryId, c.name
        ORDER BY total DESC
    """)
    fun getTotalExpenseByCategory(): Flow<List<CategoryExpense>>

    @Query("""
        SELECT 
            CAST(strftime('%d', datetime(date/1000, 'unixepoch', 'localtime')) AS INTEGER) AS dayOfMonth,
            SUM(CASE WHEN type = 'PEMASUKAN' THEN amount ELSE 0 END) AS totalIncome,
            SUM(CASE WHEN type = 'PENGELUARAN' THEN amount ELSE 0 END) AS totalExpense
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        AND is_deleted = 0
        GROUP BY dayOfMonth
    """)
    fun getDailySummaries(startDate: Long, endDate: Long): Flow<List<DailySummary>>

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
        AND t.is_deleted = 0
        GROUP BY t.categoryId, c.name
        ORDER BY total DESC
        LIMIT 1
    """)
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
        AND is_deleted = 0
        GROUP BY month, year
        ORDER BY month ASC
    """)
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
        AND is_deleted = 0
        GROUP BY date
        ORDER BY date ASC
    """)
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
        AND t.is_deleted = 0
        GROUP BY t.categoryId, c.name
        ORDER BY total DESC
    """)
    fun getCategoryStats(type: TransactionType): Flow<List<CategoryStats>>

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
        AND is_deleted = 0
    """)
    suspend fun hasTransactionsInDateRange(startDate: Long, endDate: Long): Boolean

    /**
     * Get tanggal transaksi pertama (untuk menentukan range data)
     */
    @Query("""
        SELECT MIN(date) 
        FROM transactions
        WHERE is_deleted = 0
    """)
    suspend fun getFirstTransactionDate(): Long?

    /**
     * Get tanggal transaksi terakhir
     */
    @Query("""
        SELECT MAX(date) 
        FROM transactions
        WHERE is_deleted = 0
    """)
    suspend fun getLastTransactionDate(): Long?

    /**
     * Get total amount berdasarkan kategori dalam rentang waktu
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE categoryId = :categoryId
        AND date BETWEEN :startDate AND :endDate
        AND is_deleted = 0
    """)
    suspend fun getTotalByCategoryAndDateRange(
        categoryId: Int,
        startDate: Long,
        endDate: Long
    ): Double
}