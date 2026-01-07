package com.example.catetduls.data

import java.lang.IllegalArgumentException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository untuk operasi Buku Disesuaikan untuk mendukung mekanisme sinkronisasi offline-first.
 */
class BookRepository
@Inject
constructor(
        private val bookDao: BookDao,
        private val walletDao: WalletDao,
        private val categoryDao: CategoryDao
) : SyncRepository<Book> {

        // ===================================

        // ===================================

        fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBooks()

        suspend fun getAllBooksSync(): List<Book> {
                return bookDao.getAllBooks().first()
        }

        fun getBookById(bookId: Int): Flow<Book?> = bookDao.getBookById(bookId)

        suspend fun getBookByIdSync(bookId: Int): Book? = bookDao.getBookByIdSync(bookId)

        fun getActiveBook(): Flow<Book?> = bookDao.getActiveBook()

        suspend fun getActiveBookSync(): Book? = bookDao.getActiveBookSync()

        suspend fun getBookCount(): Int = bookDao.getBookCount()

        suspend fun insertBookFromBackup(book: Book) {
                bookDao.insert(
                        book.copy(
                                isSynced = false,
                                syncAction = null,
                                lastSyncAt = System.currentTimeMillis()
                        )
                )
        }

        // ===================================

        // ===================================

        suspend fun insert(book: Book): Long {
                if (!book.isValid()) {
                        throw IllegalArgumentException("Nama buku tidak boleh kosong")
                }

                val bookToInsert =
                        book.copy(
                                isSynced = false,
                                isDeleted = false,
                                syncAction = "CREATE",
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                        )

                val newBookId = bookDao.insert(bookToInsert)

                // Create default wallets for the new book
                createDefaultWalletsForBook(newBookId.toInt())

                // Create default categories for the new book
                createDefaultCategoriesForBook(newBookId.toInt())

                return newBookId
        }

        private suspend fun createDefaultWalletsForBook(bookId: Int) {
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
                walletDao.insertAll(defaultWallets)
        }

        private suspend fun createDefaultCategoriesForBook(bookId: Int) {
                val defaultCategories =
                        listOf(
                                // Income categories
                                Category(
                                        bookId = bookId,
                                        name = "Gaji",
                                        type = TransactionType.PEMASUKAN,
                                        icon = "💰",
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Bonus",
                                        type = TransactionType.PEMASUKAN,
                                        icon = "🎁",
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                // Expense categories
                                Category(
                                        bookId = bookId,
                                        name = "Makanan",
                                        type = TransactionType.PENGELUARAN,
                                        icon = "🍔",
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Transport",
                                        type = TransactionType.PENGELUARAN,
                                        icon = "🚗",
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Belanja",
                                        type = TransactionType.PENGELUARAN,
                                        icon = "🛒",
                                        isDefault = true,
                                        lastSyncAt = 0L
                                )
                        )
                categoryDao.insertAll(defaultCategories)
        }

        suspend fun insertAll(books: List<Book>) {
                val booksToInsert =
                        books.map { book ->
                                if (!book.isValid()) {
                                        throw IllegalArgumentException(
                                                "Nama buku tidak boleh kosong"
                                        )
                                }
                                book.copy(
                                        isSynced = false,
                                        isDeleted = false,
                                        syncAction = "CREATE",
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                )
                        }
                bookDao.insertAll(booksToInsert)
        }

        // ===================================

        // ===================================

        suspend fun update(book: Book) {
                if (!book.isValid()) {
                        throw IllegalArgumentException("Nama buku tidak boleh kosong")
                }

                val bookToUpdate =
                        book.copy(
                                isSynced = false,
                                isDeleted = false,
                                syncAction = "UPDATE",
                                updatedAt = System.currentTimeMillis()
                        )

                bookDao.update(bookToUpdate)
        }

        suspend fun switchActiveBook(newActiveBookId: Int) {

                bookDao.switchActiveBook(newActiveBookId)
        }

        suspend fun updateBookCurrency(bookId: Int, currencyCode: String, currencySymbol: String) {
                val book = bookDao.getBookByIdSync(bookId)
                if (book != null) {
                        val updatedBook =
                                book.copy(
                                        currencyCode = currencyCode,
                                        currencySymbol = currencySymbol,
                                        isSynced = false,
                                        updatedAt = System.currentTimeMillis(),
                                        syncAction =
                                                if (book.syncAction == "CREATE") "CREATE"
                                                else "UPDATE"
                                )
                        bookDao.update(updatedBook)
                }
        }

        // ===================================

        // ===================================

        suspend fun delete(book: Book) {
                if (book.serverId == null) {
                        bookDao.delete(book)
                } else {

                        val bookToDelete =
                                book.copy(
                                        isSynced = false,
                                        isDeleted = true,
                                        syncAction = "DELETE",
                                        updatedAt = System.currentTimeMillis()
                                )
                        bookDao.update(bookToDelete)
                }
        }

        suspend fun deleteByIdPermanently(bookId: Int) {
                bookDao.deleteById(bookId)
        }

        override suspend fun deleteByIdPermanently(id: Long) {
                deleteByIdPermanently(id.toInt())
        }

        // ===================================

        // ===================================

        suspend fun createDefaultBook(
                currencyCode: String = "IDR",
                currencySymbol: String = "Rp"
        ): Long {
                val defaultBook =
                        Book(
                                name = "Buku Baru",
                                description = "Buku keuangan baru",
                                icon = "📖",
                                isActive = false,
                                lastSyncAt = 0L,
                                currencyCode = currencyCode,
                                currencySymbol = currencySymbol
                        )

                return insert(defaultBook)
        }

        private suspend fun createDefaultWallets(bookId: Int) {
                val defaultWallets =
                        listOf(
                                Wallet(
                                        bookId = bookId,
                                        name = "Tunai",
                                        type = WalletType.CASH,
                                        icon = "💵",
                                        color = "#4CAF50",
                                        initialBalance = 0.0,
                                        isActive = true,
                                        lastSyncAt = 0L
                                ),
                                Wallet(
                                        bookId = bookId,
                                        name = "Bank",
                                        type = WalletType.BANK,
                                        icon = "🏦",
                                        color = "#2196F3",
                                        initialBalance = 0.0,
                                        isActive = true,
                                        lastSyncAt = 0L
                                ),
                                Wallet(
                                        bookId = bookId,
                                        name = "E-Wallet",
                                        type = WalletType.E_WALLET,
                                        icon = "📱",
                                        color = "#FF9800",
                                        initialBalance = 0.0,
                                        isActive = true,
                                        lastSyncAt = 0L
                                )
                        )
                walletDao.insertAll(defaultWallets)
        }

        private suspend fun createDefaultCategories(bookId: Int) {
                val defaultCategories =
                        listOf(
                                // Kategori Pengeluaran
                                Category(
                                        bookId = bookId,
                                        name = "Makanan & Minuman",
                                        icon = "🍔",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Transport",
                                        icon = "🚌",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Belanja",
                                        icon = "🛒",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Hiburan",
                                        icon = "🎮",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Kesehatan",
                                        icon = "💊",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Pendidikan",
                                        icon = "📚",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Tagihan",
                                        icon = "💡",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Rumah Tangga",
                                        icon = "🏠",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Olahraga",
                                        icon = "⚽",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Kecantikan",
                                        icon = "💄",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),

                                // Kategori Pemasukan
                                Category(
                                        bookId = bookId,
                                        name = "Gaji",
                                        icon = "💼",
                                        type = TransactionType.PEMASUKAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Bonus",
                                        icon = "💰",
                                        type = TransactionType.PEMASUKAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Investasi",
                                        icon = "📈",
                                        type = TransactionType.PEMASUKAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Hadiah",
                                        icon = "🎁",
                                        type = TransactionType.PEMASUKAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Freelance",
                                        icon = "💻",
                                        type = TransactionType.PEMASUKAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),

                                // Kategori Lainnya
                                Category(
                                        bookId = bookId,
                                        name = "Lainnya (Pemasukan)",
                                        icon = "⚙️",
                                        type = TransactionType.PEMASUKAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Lainnya (Pengeluaran)",
                                        icon = "⚙️",
                                        type = TransactionType.PENGELUARAN,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                ),
                                Category(
                                        bookId = bookId,
                                        name = "Transfer",
                                        icon = "🔄️",
                                        type = TransactionType.TRANSFER,
                                        isDefault = true,
                                        lastSyncAt = 0L
                                )
                        )
                categoryDao.insertAll(defaultCategories)
        }

        // ===================================
        // SYNC METHODS (Dipanggil oleh Sync Worker)
        // ===================================

        /** Mengambil semua buku yang belum tersinkronisasi atau ditandai DELETE */
        override suspend fun getAllUnsynced(): List<Book> {
                return bookDao.getUnsyncedBooks()
        }

        /**
         * Memperbarui status sinkronisasi setelah operasi server berhasil (CREATE/UPDATE/DELETE).
         */
        override suspend fun updateSyncStatus(id: Long, serverId: String, syncedAt: Long) {
                bookDao.updateSyncStatus(id.toInt(), serverId, syncedAt)
        }

        suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long) {
                bookDao.updateSyncStatus(localId, serverId, lastSyncAt)
        }

        /** Menyimpan data buku yang diterima dari server (untuk operasi PULL/READ dari server) */
        override suspend fun saveFromRemote(entity: Book) {
                bookDao.insert(
                        entity.copy(
                                isSynced = true,
                                isDeleted = false,
                                syncAction = null,
                                lastSyncAt = System.currentTimeMillis()
                        )
                )
        }

        override suspend fun getByServerId(serverId: String): Book? {
                return bookDao.getByServerId(serverId)
        }

        override suspend fun markAsUnsynced(id: Long, action: String) {
                bookDao.markAsUnsynced(id.toInt(), action, System.currentTimeMillis())
        }
}
