package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) untuk Transaction Interface ini berisi semua query database untuk tabel
 * transactions
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

    @Query("DELETE FROM transactions WHERE book_id = :bookId")
    suspend fun deleteTransactionsByBook(bookId: Int)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    // FILTER UI WAJIB PAKAI book_id (Sudah Benar)
    @Query("SELECT * FROM transactions WHERE book_id = :bookId ORDER BY date DESC")
    fun getAllTransactions(bookId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

    @Query("SELECT COUNT(*) FROM transactions WHERE book_id = :bookId")
    suspend fun getTransactionCount(bookId: Int): Int

    // ========================================
    // SYNC OPERATIONS (Background Worker)
    // Worker tidak peduli book_id, dia sync semua data pending. (Sudah Benar)
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
    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long)

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
    // Dashboard Queries (UI)
    // ========================================

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'PEMASUKAN' THEN amount ELSE -amount END), 0) 
        FROM transactions
        WHERE is_deleted = 0 AND book_id = :bookId
    """)
    fun getTotalBalance(bookId: Int): Flow<Double?>

    @Query("""
        SELECT * FROM transactions 
        WHERE is_deleted = 0 AND book_id = :bookId
        ORDER BY date DESC 
        LIMIT :limit
    """)
    fun getRecentTransactions(bookId: Int, limit: Int = 5): Flow<List<Transaction>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) 
        FROM transactions 
        WHERE type = :type 
        AND date BETWEEN :startDate AND :endDate
        AND is_deleted = 0 AND book_id = :bookId
    """)
    fun getTotalByTypeAndDateRange(
        bookId: Int,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    // ========================================
    // Filter Queries (Halaman Transaksi)
    // ========================================

    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
        AND is_deleted = 0 AND book_id = :bookId
        ORDER BY date DESC
    """)
    fun getTransactionsByType(bookId: Int, type: TransactionType): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE categoryId = :categoryId 
        AND is_deleted = 0 AND book_id = :bookId
        ORDER BY date DESC
    """)
    fun getTransactionsByCategory(bookId: Int, categoryId: Int): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate 
        AND is_deleted = 0 AND book_id = :bookId
        ORDER BY date DESC
    """)
    fun getTransactionsByDateRange(
        bookId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>

    @Query("""
    SELECT * FROM transactions 
    WHERE categoryId = :categoryId 
    AND date BETWEEN :startDate AND :endDate 
    AND is_deleted = 0 AND book_id = :bookId
    ORDER BY date DESC
""")
    fun getTransactionsByCategoryAndDateRange(
        bookId: Int,
        categoryId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE notes LIKE :query 
        AND is_deleted = 0 AND book_id = :bookId
        ORDER BY date DESC
    """)
    fun searchTransactions(bookId: Int, query: String): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
        AND categoryId = :categoryId 
        AND is_deleted = 0 AND book_id = :bookId
        ORDER BY date DESC
    """)
    fun getTransactionsByTypeAndCategory(
        bookId: Int,
        type: TransactionType,
        categoryId: Int
    ): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
        AND date BETWEEN :startDate AND :endDate 
        AND is_deleted = 0 AND book_id = :bookId
        ORDER BY date DESC
    """)
    fun getTransactionsByTypeAndDateRange(
        bookId: Int,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
        AND categoryId = :categoryId 
        AND date BETWEEN :startDate AND :endDate 
        AND is_deleted = 0 AND book_id = :bookId
        ORDER BY date DESC
    """)
    fun getTransactionsByTypeAndCategoryAndDateRange(
        bookId: Int,
        type: TransactionType,
        categoryId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>>

    // ========================================
    // Statistik Queries
    // ========================================

    @Query("""
        SELECT 
            t.categoryId,
            c.name as categoryName,
            c.icon,
            SUM(t.amount) as total
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'PENGELUARAN' 
        AND t.is_deleted = 0 AND t.book_id = :bookId
        GROUP BY t.categoryId, c.name
        ORDER BY total DESC
    """)
    fun getTotalExpenseByCategory(bookId: Int): Flow<List<CategoryExpense>>

    // Sudah benar pakai 'localtime'
    @Query("""
        SELECT 
            CAST(strftime('%d', datetime(date/1000, 'unixepoch', 'localtime')) AS INTEGER) AS dayOfMonth,
            SUM(CASE WHEN type = 'PEMASUKAN' THEN amount ELSE 0 END) AS totalIncome,
            SUM(CASE WHEN type = 'PENGELUARAN' THEN amount ELSE 0 END) AS totalExpense
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        AND is_deleted = 0 AND book_id = :bookId
        GROUP BY dayOfMonth
    """)
    fun getDailySummaries(bookId: Int, startDate: Long, endDate: Long): Flow<List<DailySummary>>

    @Query("""
        SELECT 
            t.categoryId,
            c.name as categoryName,
            c.icon,
            SUM(t.amount) as total
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'PENGELUARAN' 
        AND t.is_deleted = 0 AND t.book_id = :bookId
        GROUP BY t.categoryId, c.name
        ORDER BY total DESC
        LIMIT 1
    """)
    fun getTopExpenseCategory(bookId: Int): Flow<CategoryExpense?>

    // PERBAIKAN: Menambahkan 'localtime' agar konsisten dengan getDailySummaries
    @Query("""
        SELECT 
            CAST(strftime('%m', datetime(date/1000, 'unixepoch', 'localtime')) as INTEGER) as month,
            CAST(strftime('%Y', datetime(date/1000, 'unixepoch', 'localtime')) as INTEGER) as year,
            COALESCE(SUM(CASE WHEN type = 'PEMASUKAN' THEN amount ELSE 0 END), 0) as income,
            COALESCE(SUM(CASE WHEN type = 'PENGELUARAN' THEN amount ELSE 0 END), 0) as expense,
            COALESCE(SUM(CASE WHEN type = 'PEMASUKAN' THEN amount ELSE -amount END), 0) as balance
        FROM transactions
        WHERE CAST(strftime('%Y', datetime(date/1000, 'unixepoch', 'localtime')) AS INTEGER) = :year
        AND is_deleted = 0 AND book_id = :bookId
        GROUP BY month, year
        ORDER BY month ASC
    """)
    fun getMonthlyTotals(bookId: Int, year: Int): Flow<List<MonthlyTotal>>

    @Query("""
        SELECT 
            date,
            SUM(CASE WHEN type = 'PEMASUKAN' THEN amount ELSE 0 END) as income,
            SUM(CASE WHEN type = 'PENGELUARAN' THEN amount ELSE 0 END) as expense
        FROM transactions
        WHERE date BETWEEN :startDate AND :endDate
        AND is_deleted = 0 AND book_id = :bookId
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getDailyTotals(bookId: Int, startDate: Long, endDate: Long): Flow<List<DailyTotal>>

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
        AND t.is_deleted = 0 AND t.book_id = :bookId
        GROUP BY t.categoryId, c.name
        ORDER BY total DESC
    """)
    fun getCategoryStats(bookId: Int, type: TransactionType): Flow<List<CategoryStats>>

    // ========================================
    // Utility Queries
    // ========================================

    // PERBAIKAN: Menambahkan 'suspend' karena return type bukan Flow/LiveData
    @Query("""
        SELECT COUNT(*) > 0 
        FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate
        AND is_deleted = 0 AND book_id = :bookId
    """)
    suspend fun hasTransactionsInDateRange(bookId: Int, startDate: Long, endDate: Long): Boolean

    @Query("""
        SELECT MIN(date) 
        FROM transactions
        WHERE is_deleted = 0 AND book_id = :bookId
    """)
    suspend fun getFirstTransactionDate(bookId: Int): Long?

    @Query("""
        SELECT MAX(date) 
        FROM transactions
        WHERE is_deleted = 0 AND book_id = :bookId
    """)
    suspend fun getLastTransactionDate(bookId: Int): Long?

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE categoryId = :categoryId
        AND date BETWEEN :startDate AND :endDate
        AND is_deleted = 0 AND book_id = :bookId
    """)
    suspend fun getTotalByCategoryAndDateRange(
        bookId: Int,
        categoryId: Int,
        startDate: Long,
        endDate: Long
    ): Double
}
