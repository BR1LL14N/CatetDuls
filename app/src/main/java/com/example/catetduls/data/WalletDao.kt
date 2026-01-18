package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(wallet: Wallet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(wallets: List<Wallet>)

    // READ
    @Query("SELECT * FROM wallets WHERE bookId = :bookId ORDER BY created_at DESC")
    fun getWalletsByBook(bookId: Int): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets WHERE bookId = :bookId AND isActive = 1 ORDER BY created_at DESC")
    fun getActiveWalletsByBook(bookId: Int): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets WHERE id = :walletId")
    fun getWalletById(walletId: Int): Flow<Wallet?>

    @Query("SELECT * FROM wallets WHERE id = :walletId LIMIT 1")
    suspend fun getSingleWalletById(walletId: Int): Wallet?

    @Query("SELECT * FROM wallets WHERE id = :walletId")
    suspend fun getWalletByIdSync(walletId: Int): Wallet?

    @Query("SELECT * FROM wallets WHERE bookId = :bookId AND type = :type")
    fun getWalletsByType(bookId: Int, type: WalletType): Flow<List<Wallet>>

    @Query("SELECT COUNT(*) FROM wallets WHERE bookId = :bookId")
    suspend fun getWalletCount(bookId: Int): Int

    @Query("SELECT * FROM wallets WHERE bookId = :bookId ORDER BY created_at DESC")
    suspend fun getWalletsSync(bookId: Int): List<Wallet>

    // UPDATE
    @Update suspend fun update(wallet: Wallet)

    @Query(
            "UPDATE wallets SET currentBalance = :balance, updated_at = :timestamp WHERE id = :walletId"
    )
    suspend fun updateBalance(
            walletId: Int,
            balance: Double,
            timestamp: Long = System.currentTimeMillis()
    )

    // DELETE
    @Delete suspend fun delete(wallet: Wallet)

    @Query("DELETE FROM wallets WHERE id = :walletId") suspend fun deleteById(walletId: Int)

    @Query("DELETE FROM wallets WHERE bookId = :bookId") suspend fun deleteByBookId(bookId: Int)

    // STATISTICS
    @Query(
            """
        SELECT w.*, 
        COALESCE(SUM(CASE WHEN t.type = 'PEMASUKAN' THEN t.amount ELSE 0 END), 0) as totalIncome,
        COALESCE(SUM(CASE WHEN t.type = 'PENGELUARAN' THEN t.amount ELSE 0 END), 0) as totalExpense
        FROM wallets w
        LEFT JOIN transactions t ON w.id = t.walletId
        WHERE w.bookId = :bookId
        GROUP BY w.id
        ORDER BY w.created_at DESC
    """
    )
    fun getWalletsWithStats(bookId: Int): Flow<List<WalletWithStats>>

    @Query(
            """
        SELECT SUM(currentBalance) as total
        FROM wallets
        WHERE bookId = :bookId AND isActive = 1
    """
    )
    fun getTotalBalance(bookId: Int): Flow<Double?>

    @Query("SELECT * FROM wallets WHERE is_synced = 0")
    suspend fun getUnsyncedWallets(): List<Wallet>

    @Query("SELECT * FROM wallets WHERE server_id = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): Wallet?

    @Query(
            """
        UPDATE wallets 
        SET server_id = :serverId, 
            is_synced = 1, 
            last_sync_at = :lastSyncAt,
            sync_action = NULL
        WHERE id = :localId
    """
    )
    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long)

    @Query(
            "UPDATE wallets SET is_synced = 0, sync_action = :action, updated_at = :updatedAt WHERE id = :id"
    )
    suspend fun markAsUnsynced(id: Int, action: String, updatedAt: Long)
}

// Data class untuk wallet dengan statistik
data class WalletWithStats(
        @Embedded val wallet: Wallet,
        val totalIncome: Double,
        val totalExpense: Double
) {
    val netBalance: Double
        get() = wallet.initialBalance + totalIncome - totalExpense
}
