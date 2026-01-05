package com.example.catetduls.viewmodel

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
                        _userName.value = user?.name ?: "Pengguna"
                    } else {
                        _userName.value = "Tamu"
                    }
                } catch (e: Exception) {
                    _userName.value = "Tamu"
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            _isLoggedIn.value = false
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
                    "INSERT INTO books (id, name, description, icon, color, is_active, currency_code, currency_symbol) VALUES "
            )
            sb.append(
                    "(${book.id}, '${escapeSql(book.name)}', '${escapeSql(book.description)}', '${escapeSql(book.icon)}', '${escapeSql(book.color)}', ${if (book.isActive) 1 else 0}, '${book.currencyCode}', '${book.currencySymbol}');\n"
            )
        }

        // Wallets
        sb.append("\n-- Wallets\n")
        wallets.forEach { wallet ->
            sb.append(
                    "INSERT INTO wallets (id, book_id, name, type, icon, color, initial_balance, is_active) VALUES "
            )
            sb.append(
                    "(${wallet.id}, ${wallet.bookId}, '${escapeSql(wallet.name)}', '${wallet.type}', '${escapeSql(wallet.icon)}', '${escapeSql(wallet.color)}', ${wallet.initialBalance}, ${if (wallet.isActive) 1 else 0});\n"
            )
        }

        // Categories
        sb.append("\n-- Categories\n")
        categories.forEach { category ->
            sb.append("INSERT INTO categories (id, book_id, name, icon, type, is_default) VALUES ")
            sb.append(
                    "(${category.id}, ${category.bookId}, '${escapeSql(category.name)}', '${escapeSql(category.icon)}', '${category.type}', ${if (category.isDefault) 1 else 0});\n"
            )
        }

        // Transactions
        sb.append("\n-- Transactions\n")
        transactions.forEach { transaction ->
            sb.append(
                    "INSERT INTO transactions (id, wallet_id, category_id, amount, type, notes, date) VALUES "
            )
            sb.append(
                    "(${transaction.id}, ${transaction.walletId}, ${transaction.categoryId}, ${transaction.amount}, '${transaction.type}', '${escapeSql(transaction.notes)}', ${transaction.date});\n"
            )
        }
    }

    private fun escapeSql(str: String): String {
        return str.replace("'", "''")
    }

    suspend fun restoreFromJson(jsonString: String): Boolean {
        return try {
            _isLoading.value = true

            val gson = com.google.gson.Gson()
            val backupData = gson.fromJson(jsonString, BackupData::class.java)

            // Insert books
            backupData.books.forEach { book -> bookRepository.insertBookFromBackup(book) }

            // Insert wallets
            backupData.wallets.forEach { wallet -> walletRepository.insertWalletFromBackup(wallet) }

            // Insert categories
            backupData.categories.forEach { category ->
                categoryRepository.insertCategoryFromBackup(category)
            }

            // Insert transactions
            backupData.transactions.forEach { transaction ->
                transactionRepository.insertTransactionFromBackup(transaction)
            }

            _successMessage.value = "Backup berhasil dipulihkan"
            _isLoading.value = false
            true
        } catch (e: Exception) {
            _errorMessage.value = "Gagal memulihkan backup: ${e.message}"
            _isLoading.value = false
            false
        }
    }

    suspend fun restoreFromSql(sqlContent: String): Boolean {
        return try {
            _isLoading.value = true
            
            // Get database instance
            val db = com.example.catetduls.data.AppDatabase.getDatabase(context)
            
            // Parse SQL statements
            val statements = sqlContent.split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("--") && it.uppercase().startsWith("INSERT") }
            
            if (statements.isEmpty()) {
                _errorMessage.value = "File SQL tidak mengandung statement INSERT yang valid"
                _isLoading.value = false
                return false
            }
            
            // Execute SQL statements in a transaction
            db.runInTransaction {
                statements.forEach { statement ->
                    try {
                        db.openHelper.writableDatabase.execSQL(statement)
                    } catch (e: Exception) {
                        // Log error but continue with other statements
                        android.util.Log.e("PengaturanViewModel", "Error executing SQL: $statement", e)
                    }
                }
            }
            
            _successMessage.value = "Backup SQL berhasil dipulihkan (${statements.size} statements)"
            _isLoading.value = false
            true
            
        } catch (e: Exception) {
            _errorMessage.value = "Gagal memulihkan dari SQL: ${e.message}"
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
