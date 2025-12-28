package com.example.catetduls.data

/**
 * Repository untuk operasi Dompet Disesuaikan untuk mendukung mekanisme sinkronisasi offline-first.
 */
import java.lang.IllegalArgumentException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class WalletRepository
@Inject
constructor(private val walletDao: WalletDao, private val bookRepository: BookRepository) :
        SyncRepository<Wallet> {

    private suspend fun getActiveBookId(): Int {
        return bookRepository.getActiveBookSync()?.id ?: 1
    }

    // ===================================

    // ===================================

    fun getWalletsByBook(bookId: Int): Flow<List<Wallet>> = walletDao.getWalletsByBook(bookId)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllWallets(): Flow<List<Wallet>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                walletDao.getWalletsByBook(book?.id ?: 1)
            }

    fun getActiveWalletsByBook(bookId: Int): Flow<List<Wallet>> =
            walletDao.getActiveWalletsByBook(bookId)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllActiveWallets(): Flow<List<Wallet>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                walletDao.getActiveWalletsByBook(book?.id ?: 1)
            }

    fun getWalletById(walletId: Int): Flow<Wallet?> = walletDao.getWalletById(walletId)

    suspend fun getWalletByIdSync(walletId: Int): Wallet? = walletDao.getWalletByIdSync(walletId)

    fun getWalletsByType(bookId: Int, type: WalletType): Flow<List<Wallet>> =
            walletDao.getWalletsByType(bookId, type)

    suspend fun getWalletCount(bookId: Int): Int = walletDao.getWalletCount(bookId)

    fun getWalletsWithStats(bookId: Int): Flow<List<WalletWithStats>> =
            walletDao.getWalletsWithStats(bookId)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllWalletsWithStats(): Flow<List<WalletWithStats>> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                walletDao.getWalletsWithStats(book?.id ?: 1)
            }

    fun getTotalBalance(bookId: Int): Flow<Double?> = walletDao.getTotalBalance(bookId)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTotalBalance(): Flow<Double?> =
            bookRepository.getActiveBook().flatMapLatest { book ->
                walletDao.getTotalBalance(book?.id ?: 1)
            }


    suspend fun getSingleWalletById(walletId: Int): Wallet? {
        return walletDao.getSingleWalletById(walletId)
    }

    // ===================================

    // ===================================

    suspend fun insert(wallet: Wallet): Long {
        if (!wallet.isValid()) {
            throw IllegalArgumentException("Data dompet tidak valid")
        }

        val currentBookId = if (wallet.bookId == 0) getActiveBookId() else wallet.bookId
        val walletToInsert =
                wallet.copy(
                        bookId = currentBookId,
                        isSynced = false,
                        isDeleted = false,
                        syncAction = "CREATE",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                )
        return walletDao.insert(walletToInsert)
    }

    suspend fun insertAll(wallets: List<Wallet>) {
        val walletsToInsert =
                wallets.map { wallet ->
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

    // ===================================

    suspend fun update(wallet: Wallet) {
        if (!wallet.isValid()) {
            throw IllegalArgumentException("Data dompet tidak valid")
        }

        val walletToUpdate =
                wallet.copy(
                        isSynced = false,
                        isDeleted = false,
                        syncAction = "UPDATE",
                        updatedAt = System.currentTimeMillis()
                )
        walletDao.update(walletToUpdate)
    }

    suspend fun updateBalance(walletId: Int, balance: Double) {
        if (balance < 0) {
            throw IllegalArgumentException("Saldo tidak boleh negatif")
        }

        walletDao.updateBalance(walletId, balance, System.currentTimeMillis())
        walletDao.markAsUnsynced(walletId, "UPDATE", System.currentTimeMillis())
    }

    // ===================================

    // ===================================


    suspend fun delete(wallet: Wallet) {
        if (wallet.serverId == null) {
            walletDao.delete(wallet)
        } else {

            val walletToDelete =
                    wallet.copy(
                            isSynced = false,
                            isDeleted = true,
                            syncAction = "DELETE",
                            updatedAt = System.currentTimeMillis()
                    )
            walletDao.update(walletToDelete)
        }
    }


    suspend fun deleteByIdPermanently(walletId: Int) {
        walletDao.deleteById(walletId)
    }

    override suspend fun deleteByIdPermanently(id: Long) {
        deleteByIdPermanently(id.toInt())
    }

    suspend fun deleteByBookId(bookId: Int) {
        walletDao.deleteByBookId(bookId)
    }

    // ===================================

    // ===================================

    override suspend fun getAllUnsynced(): List<Wallet> {
        return walletDao.getUnsyncedWallets()
    }


    override suspend fun updateSyncStatus(id: Long, serverId: String, syncedAt: Long) {
        walletDao.updateSyncStatus(id.toInt(), serverId, syncedAt)
    }

    suspend fun updateSyncStatus(id: Int, serverId: String, syncedAt: Long) {
        walletDao.updateSyncStatus(id, serverId, syncedAt)
    }


    override suspend fun saveFromRemote(entity: Wallet) {
        walletDao.insert(
                entity.copy(
                        isSynced = true,
                        isDeleted = false,
                        syncAction = null,
                        lastSyncAt = System.currentTimeMillis()
                )
        )
    }

    override suspend fun getByServerId(serverId: String): Wallet? {
        return walletDao.getByServerId(serverId)
    }

    override suspend fun markAsUnsynced(id: Long, action: String) {
        walletDao.markAsUnsynced(id.toInt(), action, System.currentTimeMillis())
    }

    // ===================================

    // ===================================

    suspend fun createDefaultWalletsForBook(bookId: Int) {
        val defaultWallets =
                listOf(
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
        insertAll(defaultWallets)
    }
}
