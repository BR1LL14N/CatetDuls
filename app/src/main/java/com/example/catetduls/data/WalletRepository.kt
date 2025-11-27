package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository untuk operasi Dompet
 */
class WalletRepository(private val walletDao: WalletDao) {

    // READ
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

    // CREATE
    suspend fun insert(wallet: Wallet): Long {
        if (!wallet.isValid()) {
            throw IllegalArgumentException("Data dompet tidak valid")
        }
        return walletDao.insert(wallet)
    }

    suspend fun insertAll(wallets: List<Wallet>) {
        wallets.forEach { wallet ->
            if (!wallet.isValid()) {
                throw IllegalArgumentException("Data dompet tidak valid")
            }
        }
        walletDao.insertAll(wallets)
    }

    // UPDATE
    suspend fun update(wallet: Wallet) {
        if (!wallet.isValid()) {
            throw IllegalArgumentException("Data dompet tidak valid")
        }
        walletDao.update(wallet.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateBalance(walletId: Int, balance: Double) {
        if (balance < 0) {
            throw IllegalArgumentException("Saldo tidak boleh negatif")
        }
        walletDao.updateBalance(walletId, balance)
    }

    // DELETE
    suspend fun delete(wallet: Wallet) {
        walletDao.delete(wallet)
    }

    suspend fun deleteById(walletId: Int) {
        walletDao.deleteById(walletId)
    }

    suspend fun deleteByBookId(bookId: Int) {
        walletDao.deleteByBookId(bookId)
    }

    // HELPER
    suspend fun createDefaultWalletsForBook(bookId: Int) {
        val defaultWallets = listOf(
            Wallet(
                bookId = bookId,
                name = "Tunai",
                type = WalletType.CASH,
                icon = "💵",
                color = "#4CAF50",
                initialBalance = 0.0
            ),
            Wallet(
                bookId = bookId,
                name = "Bank",
                type = WalletType.BANK,
                icon = "🏦",
                color = "#2196F3",
                initialBalance = 0.0
            )
        )
        insertAll(defaultWallets)
    }
}