package com.example.catetduls.ui.viewmodel

// import com.example.catetduls.data.repository.UserRepository
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.*
import com.example.catetduls.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PengaturanViewModel
@Inject
constructor(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val walletRepository: WalletRepository,
        private val bookRepository: BookRepository,
        private val userRepository: UserRepository,
        @ApplicationContext private val context: Context
) : ViewModel() {

    // ========================================
    // State Management
    // ========================================
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // ========================================
    // Auth State
    // ========================================
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow("Tamu")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // ========================================
    // Theme Settings
    // ========================================
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun getActiveBookId(): Int {
        return try {
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    .getInt("active_book_id", 1)
        } catch (e: Exception) {
            1
        }
    }

    init {
        loadThemePreference()
        // Check login status on init
        viewModelScope.launch { checkLoginStatus() }
    }

    // ========================================
    // Currency & Book
    // ========================================
    fun updateActiveBookCurrency(currencyCode: String, currencySymbol: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val bookId = getActiveBookId()
                // Hanya update metadata buku, nilai transaksi TIDAK diubah (Rupiah di database)
                bookRepository.updateBookCurrency(bookId, currencyCode, currencySymbol)
                _successMessage.value =
                        "Mata uang diperbarui: $currencyCode (Nilai transaksi tetap IDR di database)"
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memperbarui mata uang: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================
    // Theme Functions
    // ========================================
    private fun loadThemePreference() {
        try {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            _isDarkMode.value = prefs.getBoolean("dark_mode", false)
        } catch (e: Exception) {
            _isDarkMode.value = false
            e.printStackTrace()
        }
    }

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        try {
            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("dark_mode", newValue).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========================================
    // Auth Functions
    // ========================================
    fun checkLoginStatus() {
        try {
            val token = TokenManager.getToken(context)
            _isLoggedIn.value = token != null

            viewModelScope.launch {
                try {
                    if (token != null) {
                        val user = userRepository.getCurrentUser()
                        _currentUser.value = user
                        _userName.value = user?.name ?: "Pengguna"
                    } else {
                        _currentUser.value = null
                        _userName.value = "Tamu"
                    }
                } catch (e: Exception) {
                    _currentUser.value = null
                    _userName.value = "Tamu"
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            _isLoggedIn.value = false
            _currentUser.value = null
            _userName.value = "Tamu"
            e.printStackTrace()
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = userRepository.logout()
                if (result.isSuccess) {
                    _isLoggedIn.value = false
                    _currentUser.value = null
                    _userName.value = "Tamu"
                    _successMessage.value = "Berhasil keluar"
                } else {
                    throw result.exceptionOrNull() ?: Exception("Gagal logout")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal logout: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================
    // Backup & Export Functions
    // ========================================

    data class BackupPreview(
            val bookName: String,
            val transactionCount: Int,
            val categoryCount: Int,
            val walletCount: Int
    )

    data class BackupData(
            val version: Int = 1,
            val exportDate: Long = System.currentTimeMillis(),
            val books: List<Book>,
            val wallets: List<Wallet>,
            val categories: List<Category>,
            val transactions: List<Transaction>
    )

    suspend fun getAllBooksForSelection(): List<Book> {
        return try {
            bookRepository.getAllBooksSync()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getBackupPreview(bookId: Int?): BackupPreview {
        return try {
            if (bookId == null) {
                // All books
                val allBooks = bookRepository.getAllBooksSync()
                val allTransactions = transactionRepository.getAllTransactionsSync()
                val allCategories = categoryRepository.getAllCategoriesSync()
                val allWallets = walletRepository.getAllWalletsSync()

                BackupPreview(
                        bookName = "Semua Buku (${allBooks.size} buku)",
                        transactionCount = allTransactions.size,
                        categoryCount = allCategories.size,
                        walletCount = allWallets.size
                )
            } else {
                // Specific book
                val book = bookRepository.getBookByIdSync(bookId)
                val transactions = transactionRepository.getTransactionsByBookIdSync(bookId)
                val categories = categoryRepository.getCategoriesByBookIdSync(bookId)
                val wallets = walletRepository.getWalletsByBookIdSync(bookId)

                BackupPreview(
                        bookName = book?.name ?: "Unknown",
                        transactionCount = transactions.size,
                        categoryCount = categories.size,
                        walletCount = wallets.size
                )
            }
        } catch (e: Exception) {
            BackupPreview("Error", 0, 0, 0)
        }
    }

    suspend fun exportToJson(bookId: Int?): String? {
        return try {
            val backupData =
                    if (bookId == null) {
                        // Export all books
                        BackupData(
                                books = bookRepository.getAllBooksSync(),
                                wallets = walletRepository.getAllWalletsSync(),
                                categories = categoryRepository.getAllCategoriesSync(),
                                transactions = transactionRepository.getAllTransactionsSync()
                        )
                    } else {
                        // Export specific book
                        val book = bookRepository.getBookByIdSync(bookId)
                        if (book != null) {
                            BackupData(
                                    books = listOf(book),
                                    wallets = walletRepository.getWalletsByBookIdSync(bookId),
                                    categories =
                                            categoryRepository.getCategoriesByBookIdSync(bookId),
                                    transactions =
                                            transactionRepository.getTransactionsByBookIdSync(
                                                    bookId
                                            )
                            )
                        } else {
                            return null
                        }
                    }

            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            gson.toJson(backupData)
        } catch (e: Exception) {
            _errorMessage.value = "Gagal export JSON: ${e.message}"
            null
        }
    }

    suspend fun exportToSql(bookId: Int?): String? {
        return try {
            // For SQL export, we'll create SQL INSERT statements
            val sb = StringBuilder()
            sb.append("-- CatetDuls Backup SQL\n")
            sb.append(
                    "-- Export Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}\n\n"
            )

            if (bookId == null) {
                // Export all books
                val books = bookRepository.getAllBooksSync()
                val wallets = walletRepository.getAllWalletsSync()
                val categories = categoryRepository.getAllCategoriesSync()
                val transactions = transactionRepository.getAllTransactionsSync()

                appendSqlStatements(sb, books, wallets, categories, transactions)
            } else {
                // Export specific book
                val book = bookRepository.getBookByIdSync(bookId)
                if (book != null) {
                    val wallets = walletRepository.getWalletsByBookIdSync(bookId)
                    val categories = categoryRepository.getCategoriesByBookIdSync(bookId)
                    val transactions = transactionRepository.getTransactionsByBookIdSync(bookId)

                    appendSqlStatements(sb, listOf(book), wallets, categories, transactions)
                } else {
                    return null
                }
            }

            sb.toString()
        } catch (e: Exception) {
            _errorMessage.value = "Gagal export SQL: ${e.message}"
            null
        }
    }

    private fun appendSqlStatements(
            sb: StringBuilder,
            books: List<Book>,
            wallets: List<Wallet>,
            categories: List<Category>,
            transactions: List<Transaction>
    ) {
        // Books
        sb.append("-- Books\n")
        books.forEach { book ->
            sb.append(
                    "INSERT OR REPLACE INTO books (id, name, description, icon, color, isActive, currency_code, currency_symbol) VALUES "
            )
            sb.append(
                    "(${book.id}, '${escapeSql(book.name)}', '${escapeSql(book.description)}', '${escapeSql(book.icon)}', '${escapeSql(book.color)}', ${if (book.isActive) 1 else 0}, '${book.currencyCode}', '${book.currencySymbol}');\n"
            )
        }

        // Wallets
        sb.append("\n-- Wallets\n")
        wallets.forEach { wallet ->
            sb.append(
                    "INSERT OR REPLACE INTO wallets (id, bookId, name, type, icon, color, initialBalance, currentBalance, isActive, created_at, updated_at, server_id, is_synced, is_deleted, last_sync_at, sync_action) VALUES "
            )
            val serverId = wallet.serverId?.let { "'${escapeSql(it)}'" } ?: "NULL"
            val syncAction = wallet.syncAction?.let { "'${escapeSql(it)}'" } ?: "NULL"
            sb.append(
                    "(${wallet.id}, ${wallet.bookId}, '${escapeSql(wallet.name)}', '${wallet.type}', '${escapeSql(wallet.icon)}', '${escapeSql(wallet.color)}', ${wallet.initialBalance}, ${wallet.currentBalance}, ${if (wallet.isActive) 1 else 0}, ${wallet.createdAt}, ${wallet.updatedAt}, $serverId, ${if (wallet.isSynced) 1 else 0}, ${if (wallet.isDeleted) 1 else 0}, ${wallet.lastSyncAt}, $syncAction);\n"
            )
        }

        // Categories
        sb.append("\n-- Categories\n")
        categories.forEach { category ->
            sb.append(
                    "INSERT OR REPLACE INTO categories (id, bookId, name, icon, type, isDefault, created_at, updated_at, server_id, is_synced, is_deleted, last_sync_at, sync_action) VALUES "
            )
            val serverId = category.serverId?.let { "'${escapeSql(it)}'" } ?: "NULL"
            val syncAction = category.syncAction?.let { "'${escapeSql(it)}'" } ?: "NULL"
            sb.append(
                    "(${category.id}, ${category.bookId}, '${escapeSql(category.name)}', '${escapeSql(category.icon)}', '${category.type}', ${if (category.isDefault) 1 else 0}, ${category.createdAt}, ${category.updatedAt}, $serverId, ${if (category.isSynced) 1 else 0}, ${if (category.isDeleted) 1 else 0}, ${category.lastSyncAt}, $syncAction);\n"
            )
        }

        // Transactions
        sb.append("\n-- Transactions\n")
        transactions.forEach { transaction ->
            sb.append(
                    "INSERT OR REPLACE INTO transactions (id, book_id, walletId, categoryId, amount, type, notes, date, image_path, created_at, updated_at, server_id, is_synced, is_deleted, last_sync_at, sync_action) VALUES "
            )
            val imagePath = transaction.imagePath?.let { "'${escapeSql(it)}'" } ?: "NULL"
            val serverId = transaction.serverId?.let { "'${escapeSql(it)}'" } ?: "NULL"
            val lastSyncAt = transaction.lastSyncAt
            val syncAction = transaction.syncAction?.let { "'${escapeSql(it)}'" } ?: "NULL"

            sb.append(
                    "(${transaction.id}, ${transaction.bookId}, ${transaction.walletId}, ${transaction.categoryId}, ${transaction.amount}, '${transaction.type}', '${escapeSql(transaction.notes)}', ${transaction.date}, $imagePath, ${transaction.createdAt}, ${transaction.updatedAt}, $serverId, ${if (transaction.isSynced) 1 else 0}, ${if (transaction.isDeleted) 1 else 0}, $lastSyncAt, $syncAction);\n"
            )
        }
    }

    private fun escapeSql(str: String): String {
        return str.replace("'", "''")
    }

    suspend fun restoreFromJson(jsonString: String): Boolean {
        return try {
            _isLoading.value = true

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val gson = com.google.gson.Gson()
                val backupData = gson.fromJson(jsonString, BackupData::class.java)

                android.util.Log.d(
                        "PengaturanViewModel",
                        "Restoring backup: ${backupData.books.size} books, ${backupData.wallets.size} wallets, ${backupData.categories.size} categories, ${backupData.transactions.size} transactions"
                )

                // Insert books
                backupData.books.forEach { book ->
                    try {
                        bookRepository.insertBookFromBackup(book)
                        android.util.Log.d("PengaturanViewModel", "Inserted book: ${book.name}")
                    } catch (e: Exception) {
                        android.util.Log.e(
                                "PengaturanViewModel",
                                "Failed to insert book: ${book.name}",
                                e
                        )
                        throw Exception("Gagal insert buku '${book.name}': ${e.message}")
                    }
                }

                // Insert wallets
                backupData.wallets.forEach { wallet ->
                    try {
                        walletRepository.insertWalletFromBackup(wallet)
                        android.util.Log.d("PengaturanViewModel", "Inserted wallet: ${wallet.name}")
                    } catch (e: Exception) {
                        android.util.Log.e(
                                "PengaturanViewModel",
                                "Failed to insert wallet: ${wallet.name}",
                                e
                        )
                        throw Exception("Gagal insert wallet '${wallet.name}': ${e.message}")
                    }
                }

                // Insert categories
                backupData.categories.forEach { category ->
                    try {
                        categoryRepository.insertCategoryFromBackup(category)
                        android.util.Log.d(
                                "PengaturanViewModel",
                                "Inserted category: ${category.name}"
                        )
                    } catch (e: Exception) {
                        android.util.Log.e(
                                "PengaturanViewModel",
                                "Failed to insert category: ${category.name}",
                                e
                        )
                        throw Exception("Gagal insert kategori '${category.name}': ${e.message}")
                    }
                }

                // Insert transactions
                backupData.transactions.forEach { transaction ->
                    try {
                        transactionRepository.insertTransactionFromBackup(transaction)
                    } catch (e: Exception) {
                        android.util.Log.e(
                                "PengaturanViewModel",
                                "Failed to insert transaction ID: ${transaction.id}",
                                e
                        )
                        throw Exception("Gagal insert transaksi: ${e.message}")
                    }
                }
            }

            _successMessage.value = "Backup berhasil dipulihkan"
            _isLoading.value = false
            true
        } catch (e: Exception) {
            android.util.Log.e("PengaturanViewModel", "Restore failed", e)
            _errorMessage.value = "Gagal memulihkan backup: ${e.message}"
            _isLoading.value = false
            false
        }
    }

    suspend fun restoreFromSql(sqlContent: String): Boolean {
        return try {
            _isLoading.value = true

            // Execute database operations on IO thread
            val result =
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        // Get database instance
                        val db = com.example.catetduls.data.AppDatabase.getDatabase(context)

                        // Parse SQL statements - split by semicolon and filter
                        val statements =
                                sqlContent.split(";").map { it.trim() }.filter {
                                    it.isNotEmpty() &&
                                            !it.startsWith("--") &&
                                            (it.uppercase().startsWith("INSERT"))
                                }

                        android.util.Log.d(
                                "PengaturanViewModel",
                                "Total SQL statements found: ${statements.size}"
                        )

                        if (statements.isEmpty()) {
                            android.util.Log.e(
                                    "PengaturanViewModel",
                                    "No valid INSERT statements found"
                            )
                            return@withContext Pair(
                                    false,
                                    "File SQL tidak mengandung statement INSERT yang valid"
                            )
                        }

                        var successCount = 0
                        var errorCount = 0
                        val errors = mutableListOf<String>()

                        // Sort statements by table dependency order
                        val sortedStatements =
                                statements.sortedBy { statement ->
                                    val upperStatement = statement.uppercase()
                                    when {
                                        upperStatement.contains("INTO BOOKS") -> 1
                                        upperStatement.contains("INTO WALLETS") -> 2
                                        upperStatement.contains("INTO CATEGORIES") -> 3
                                        upperStatement.contains("INTO TRANSACTIONS") -> 4
                                        else -> 5
                                    }
                                }

                        android.util.Log.d(
                                "PengaturanViewModel",
                                "Sorted ${sortedStatements.size} statements by dependency order"
                        )

                        // Counters for each table
                        var booksInserted = 0
                        var walletsInserted = 0
                        var categoriesInserted = 0
                        var transactionsInserted = 0

                        // Execute SQL statements in a transaction
                        db.runInTransaction {
                            sortedStatements.forEach { statement ->
                                try {
                                    // Add semicolon back for execution
                                    db.openHelper.writableDatabase.execSQL("$statement;")
                                    successCount++

                                    // Track per-table success
                                    val upperStatement = statement.uppercase()
                                    when {
                                        upperStatement.contains("INTO BOOKS") -> booksInserted++
                                        upperStatement.contains("INTO WALLETS") -> walletsInserted++
                                        upperStatement.contains("INTO CATEGORIES") ->
                                                categoriesInserted++
                                        upperStatement.contains("INTO TRANSACTIONS") ->
                                                transactionsInserted++
                                    }
                                } catch (e: Exception) {
                                    errorCount++
                                    val errorMsg = "Error: ${e.message?.take(50)}"
                                    errors.add(errorMsg)
                                    android.util.Log.e(
                                            "PengaturanViewModel",
                                            "SQL Error: $statement",
                                            e
                                    )
                                }
                            }
                        }

                        android.util.Log.d(
                                "PengaturanViewModel",
                                "Restore completed: $successCount success, $errorCount errors"
                        )
                        android.util.Log.d(
                                "PengaturanViewModel",
                                "Per-table: Books=$booksInserted, Wallets=$walletsInserted, Categories=$categoriesInserted, Transactions=$transactionsInserted"
                        )

                        // Return result
                        if (successCount > 0) {
                            Pair(true, "Backup SQL berhasil dipulihkan ($successCount statements)")
                        } else {
                            Pair(false, "Gagal restore: ${errors.firstOrNull() ?: "Unknown error"}")
                        }
                    }

            _isLoading.value = false

            if (result.first) {
                _successMessage.value = result.second
            } else {
                _errorMessage.value = result.second
            }

            result.first
        } catch (e: Exception) {
            _errorMessage.value = "Gagal memulihkan dari SQL: ${e.message}"
            android.util.Log.e("PengaturanViewModel", "Restore failed", e)
            _isLoading.value = false
            false
        }
    }

    // ========================================
    // Reset Data
    // ========================================
    fun resetAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                transactionRepository.deleteAllTransactions()
                _successMessage.value = "Semua data berhasil dihapus"
            } catch (e: Exception) {
                _errorMessage.value = "Gagal reset data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetCategoriesToDefault() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: Implement reset categories logic
                _successMessage.value = "Kategori berhasil direset"
            } catch (e: Exception) {
                _errorMessage.value = "Gagal reset kategori: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ========================================
    // Info & Util Functions
    // ========================================
    fun getAppVersion(): String = "1.0.0"

    fun getAppStatistics(): Map<String, String> {
        return try {
            mapOf("Total Transaksi" to "0", "Total Kategori" to "0")
        } catch (e: Exception) {
            mapOf("Total Transaksi" to "-", "Total Kategori" to "-")
        }
    }

    fun forceSync() {
        com.example.catetduls.data.sync.SyncManager.forceOneTimeSync(context)
        _successMessage.value = "Sinkronisasi dimulai..."
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
