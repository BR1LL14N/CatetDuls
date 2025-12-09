package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow
import java.lang.IllegalArgumentException

/**
 * Repository untuk operasi Dompet
 * Disesuaikan untuk mendukung mekanisme sinkronisasi offline-first.
 */
class WalletRepository(private val walletDao: WalletDao) {

    // ===================================
    // READ
    // ===================================

    fun getWalletsByBook(bookId: Int): Flow<List<Wallet>> =
        walletDao.getWalletsByBook(bookId)

    fun getActiveWalletsByBook(bookId: Int): Flow<List<Wallet>> =
        walletDao.getActiveWalletsByBook(bookId)

    fun getWalletById(walletId: Int): Flow<Wallet?> =
        walletDao.getWalletById(walletId)

    suspend fun getWalletByIdSync(walletId: Int): Wallet? =
        walletDao.getWalletByIdSync(walletId)

    fun getWalletsByType(bookId: Int, type: WalletType): Flow<List<Wallet>> =
        walletDao.getWalletsByType(bookId, type)

    suspend fun getWalletCount(bookId: Int): Int =
        walletDao.getWalletCount(bookId)

    fun getWalletsWithStats(bookId: Int): Flow<List<WalletWithStats>> =
        walletDao.getWalletsWithStats(bookId)

    fun getTotalBalance(bookId: Int): Flow<Double?> =
        walletDao.getTotalBalance(bookId)

    suspend fun getSingleWalletById(walletId: Int): Wallet? {
        return walletDao.getSingleWalletById(walletId)
    }

    // ===================================
    // CREATE
    // ===================================

    suspend fun insert(wallet: Wallet): Long {
        if (!wallet.isValid()) {
            throw IllegalArgumentException("Data dompet tidak valid")
        }

        val walletToInsert = wallet.copy(
            isSynced = false,
            isDeleted = false,
            syncAction = "CREATE",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return walletDao.insert(walletToInsert)
    }

    suspend fun insertAll(wallets: List<Wallet>) {
        val walletsToInsert = wallets.map { wallet ->
            if (!wallet.isValid()) {
                throw IllegalArgumentException("Data dompet tidak valid")
            }
            wallet.copy(
                isSynced = false,
                isDeleted = false,
                syncAction = "CREATE",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        walletDao.insertAll(walletsToInsert)
    }

    // ===================================
    // UPDATE
    // ===================================

    suspend fun update(wallet: Wallet) {
        if (!wallet.isValid()) {
            throw IllegalArgumentException("Data dompet tidak valid")
        }

        val walletToUpdate = wallet.copy(
            isSynced = false,
            isDeleted = false,
            syncAction = "UPDATE",
            updatedAt = System.currentTimeMillis()
        )
        walletDao.update(walletToUpdate)
    }

    suspend fun updateBalance(walletId: Int, balance: Double) {
        // Logika updateBalance ini biasanya dipanggil setelah transaksi
        // Karena ini update balance secara langsung, kita harus menandainya sebagai unsynced.

        // Catatan: Jika updateBalance ini dipanggil internal setelah insert/update transaksi,
        // pastikan logic di sync worker menangani ini agar tidak terjadi konflik sync.

        if (balance < 0) {
            throw IllegalArgumentException("Saldo tidak boleh negatif")
        }

        // Kita tidak bisa langsung memanggil walletDao.updateBalance
        // karena kita perlu mendapatkan action dan updatedAt untuk markAsUnsynced
        walletDao.updateBalance(walletId, balance, System.currentTimeMillis())
        walletDao.markAsUnsynced(walletId, "UPDATE", System.currentTimeMillis())
    }

    // ===================================
    // DELETE
    // ===================================

    /**
     * Menandai dompet sebagai terhapus (soft delete) untuk disinkronkan ke server.
     * Jika dompet belum pernah disinkronkan (server_id null), maka hapus permanen lokal.
     */
    suspend fun delete(wallet: Wallet) {
        if (wallet.serverId == null) {
            walletDao.delete(wallet)
        } else {

            val walletToDelete = wallet.copy(
                isSynced = false,
                isDeleted = true,
                syncAction = "DELETE",
                updatedAt = System.currentTimeMillis()
            )
            walletDao.update(walletToDelete)
        }
    }

    /**
     * Menghapus dompet secara permanen berdasarkan ID (Dipanggil setelah sync DELETE berhasil)
     */
    suspend fun deleteByIdPermanently(walletId: Int) {
        walletDao.deleteById(walletId)
    }

    suspend fun deleteByBookId(bookId: Int) {
        // Ini adalah hard delete massal (biasanya setelah buku dihapus)
        // Pastikan Buku sudah disinkronkan/dihapus di server terlebih dahulu
        walletDao.deleteByBookId(bookId)
    }

    // ===================================
    // SYNC METHODS
    // ===================================

    /**
     * Mengambil semua dompet yang perlu disinkronisasi (CREATE, UPDATE, DELETE).
     * Termasuk yang baru dibuat/diubah (is_synced = 0) dan yang ditandai untuk dihapus (is_deleted = 1).
     */
    suspend fun getAllUnsynced(): List<Wallet> {
        // Menggabungkan logika unsynced (is_synced = 0) dan deleted (is_deleted = 1 & is_synced = 0)
        return walletDao.getUnsyncedWallets() // Asumsi DAO ini mengambil semua yang is_synced = 0
    }

    /**
     * Memperbarui status sinkronisasi setelah operasi server berhasil (CREATE/UPDATE).
     */
    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long) {
        walletDao.updateSyncStatus(localId, serverId, lastSyncAt)
    }

    /**
     * Menyimpan data dompet yang diterima dari server (untuk operasi PULL/READ dari server)
     */
    suspend fun saveFromRemote(wallet: Wallet) {
        walletDao.insert(wallet.copy(
            isSynced = true,
            isDeleted = false,
            syncAction = null,
            lastSyncAt = System.currentTimeMillis()
        ))
    }

    // ===================================
    // HELPER
    // ===================================

    suspend fun createDefaultWalletsForBook(bookId: Int) {
        val defaultWallets = listOf(
            Wallet(
                bookId = bookId,
                name = "Tunai",
                type = WalletType.CASH,
                icon = "💵",
                color = "#4CAF50",
                initialBalance = 0.0,
                lastSyncAt = 0L
            ),
            Wallet(
                bookId = bookId,
                name = "Bank",
                type = WalletType.BANK,
                icon = "🏦",
                color = "#2196F3",
                initialBalance = 0.0,
                lastSyncAt = 0L
            )
        )
        // Memanggil fungsi insertAll() di repository yang sudah menangani logic sync
        insertAll(defaultWallets)
    }
}