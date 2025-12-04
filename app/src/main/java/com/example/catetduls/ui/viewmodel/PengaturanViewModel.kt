package com.example.catetduls.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*



/**
 * ViewModel untuk PengaturanPage
 *
 * Mengelola:
 * - Backup & Restore data
 * - Export data ke JSON/CSV
 * - Kelola kategori
 * - Theme settings
 * - Reset data
 */
class PengaturanViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository,
    private val activeBookId: Int,
    private val context: Context

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
    // Theme Settings
    // ========================================

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        loadThemePreference()
    }

    /**
     * Load theme preference dari SharedPreferences
     */
    private fun loadThemePreference() {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        _isDarkMode.value = prefs.getBoolean("dark_mode", false)
    }

    /**
     * Toggle dark mode
     */
    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue

        // Simpan ke SharedPreferences
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dark_mode", newValue).apply()
    }

    // ========================================
    // Backup & Restore
    // ========================================

    /**
     * Export data ke JSON
     */
    suspend fun exportToJson(): String? {
        return try {
            _isLoading.value = true

            val transactions = transactionRepository.getAllTransactions().first()
            val categories = categoryRepository.getAllCategories().first()

            // TODO: Implement actual data fetching
            // Untuk sementara return dummy data
            val json = JSONObject().apply {
                put("version", "1.0")
                put("exportDate", System.currentTimeMillis())
                put("transactions", BackupHelper.transactionsToJson(transactions))
                put("categories", BackupHelper.categoriesToJson(categories))
            }

            _successMessage.value = "Data berhasil di-export"
            _isLoading.value = false
            json.toString()

        } catch (e: Exception) {
            _errorMessage.value = "Gagal export data: ${e.message}"
            _isLoading.value = false
            null
        }
    }

    /**
     * Export data ke CSV
     */
    suspend fun exportToCsv(): String? {
        return try {
            _isLoading.value = true

            // Ambil data secara synchronous (gunakan first())
            val transactions = transactionRepository.getAllTransactions().first()
            val categories = categoryRepository.getAllCategories().first()
            val wallets = walletRepository.getWalletsByBook(activeBookId).first() // <--- Ambil Wallet

            val csv = BackupHelper.transactionsToCsv(
                transactions = transactions,
                categories = categories,
                wallets = wallets // <--- Pass Wallet
            )

            _successMessage.value = "Data berhasil di-export ke CSV"
            _isLoading.value = false
            csv
        } catch (e: Exception) {
            _errorMessage.value = "Gagal export CSV: ${e.message}"
            _isLoading.value = false
            e.printStackTrace()
            null
        }
    }

    /**
     * Simpan backup ke file
     */
    fun saveBackupToFile(data: String): File? {
        return try {
            val isCsv = false
            val extension = if (isCsv) "csv" else "json"
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val fileName = "finnote_backup_$timestamp.json"

            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val file = File(backupDir, fileName)
            file.writeText(data)

            _successMessage.value = "Backup berhasil disimpan: $fileName"
            file

        } catch (e: Exception) {
            _errorMessage.value = "Gagal menyimpan backup: ${e.message}"
            null
        }
    }

    /**
     * Import/Restore data dari JSON
     */
    fun importFromJson(jsonString: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val json = JSONObject(jsonString)

                val categories = BackupHelper.jsonToCategories(json.getJSONArray("categories"))
                val transactions = BackupHelper.jsonToTransactions(json.getJSONArray("transactions"))
                categoryRepository.insertAll(categories)
                transactionRepository.insertAll(transactions)

                _successMessage.value = "Data berhasil di-import"
                _isLoading.value = false

            } catch (e: Exception) {
                _errorMessage.value = "Gagal import data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // ========================================
    // Data Management
    // ========================================

    /**
     * Reset semua data (hapus semua transaksi)
     */
    fun resetAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                transactionRepository.deleteAllTransactions()
                _successMessage.value = "Semua data berhasil dihapus"
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Gagal reset data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Reset kategori ke default
     */
    fun resetCategoriesToDefault() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val defaultCategories = listOf(
                    Category(bookId = activeBookId,name = "Makanan & Minuman", icon = "🍔", type = TransactionType.PENGELUARAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Transport", icon = "🚌", type = TransactionType.PENGELUARAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Belanja", icon = "🛒", type = TransactionType.PENGELUARAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Hiburan", icon = "🎮", type = TransactionType.PENGELUARAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Kesehatan", icon = "💊", type = TransactionType.PENGELUARAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Pendidikan", icon = "📚", type = TransactionType.PENGELUARAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Tagihan", icon = "💡", type = TransactionType.PENGELUARAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Rumah Tangga", icon = "🏠", type = TransactionType.PENGELUARAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Olahraga", icon = "⚽", type = TransactionType.PENGELUARAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Kecantikan", icon = "💄", type = TransactionType.PENGELUARAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Gaji", icon = "💼", type = TransactionType.PEMASUKAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Bonus", icon = "💰", type = TransactionType.PEMASUKAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Investasi", icon = "📈", type = TransactionType.PEMASUKAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Hadiah", icon = "🎁", type = TransactionType.PEMASUKAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Freelance", icon = "💻", type = TransactionType.PEMASUKAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Lainnya (Pemasukan)", icon = "⚙️", type = TransactionType.PEMASUKAN, isDefault = true),
                    Category(bookId = activeBookId,name = "Lainnya (Pengeluaran)", icon = "⚙️", type = TransactionType.PENGELUARAN, isDefault = true)
                )


                categoryRepository.insertAll(defaultCategories)

                _successMessage.value = "Kategori berhasil direset"
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Gagal reset kategori: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // ========================================
    // Statistics Info
    // ========================================

    /**
     * Get statistik aplikasi untuk ditampilkan di pengaturan
     */
    fun getAppStatistics(): Map<String, String> {
        // TODO: Implement actual statistics
        return mapOf(
            "Total Transaksi" to "0",
            "Total Kategori" to "0",
            "Database Size" to "0 KB"
        )
    }

    // ========================================
    // Utility Functions
    // ========================================

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    /**
     * Format date untuk backup filename
     */
    fun getBackupFileName(extension: String = "json"): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        // Menggunakan extension yang diberikan
        return "finnote_backup_$timestamp.$extension"
    }

    /**
     * Get app version
     */
    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}

/**
 * Factory untuk PengaturanViewModel
 */
class PengaturanViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository,
    private val activeBookId: Int,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PengaturanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PengaturanViewModel(
                transactionRepository,
                categoryRepository,
                walletRepository,
                activeBookId,
                context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ========================================
// Utility Classes for Backup/Restore
// ========================================

/**
 * Helper class untuk export/import data
 */
object BackupHelper {

    /**
     * Convert Transaction list to JSON
     */
    fun transactionsToJson(transactions: List<Transaction>): JSONArray {
        val jsonArray = JSONArray()
        transactions.forEach { transaction ->
            val jsonObject = JSONObject().apply {
                put("id", transaction.id)
                put("type", transaction.type)
                put("amount", transaction.amount)
                put("categoryId", transaction.categoryId)
                put("date", transaction.date)
                put("notes", transaction.notes)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray
    }

    /**
     * Convert JSON to Transaction list
     */
    fun jsonToTransactions(jsonArray: JSONArray): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            transactions.add(
                Transaction(
                    id = json.getInt("id"),
                    walletId = json.getInt("walletId"),
                    type = TransactionType.valueOf(json.getString("type")),
                    // ---------------------------------------------------
                    amount = json.getDouble("amount"),
                    categoryId = json.getInt("categoryId"),
                    date = json.getLong("date"),
                    notes = json.optString("notes", "")
                )
            )
        }
        return transactions
    }

    /**
     * Convert Category list to JSON
     */
    fun categoriesToJson(categories: List<Category>): JSONArray {
        val jsonArray = JSONArray()
        categories.forEach { category ->
            val jsonObject = JSONObject().apply {
                put("id", category.id)
                put("name", category.name)
                put("icon", category.icon)
                put("type", category.type)
                put("isDefault", category.isDefault)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray
    }

    /**
     * Convert JSON to Category list
     */
    fun jsonToCategories(jsonArray: JSONArray): List<Category> {
        val categories = mutableListOf<Category>()
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            categories.add(
                Category(
                    id = json.getInt("id"),
                    bookId = json.getInt("bookId"),
                    name = json.getString("name"),
                    icon = json.getString("icon"),

                    type = TransactionType.valueOf(json.getString("type")),
                    // ---------------------------------------------------
                    isDefault = json.getBoolean("isDefault")
                )
            )
        }
        return categories
    }

    fun transactionsToCsv(
        transactions: List<Transaction>,
        categories: List<Category>,
        wallets: List<Wallet> // <--- TAMBAHAN: List Wallet
    ): String {
        val csv = StringBuilder()
        val fullDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val exportDateString = fullDateFormat.format(Date())

        // Buat Map untuk Lookup Cepat
        val categoryNameMap: Map<Int, String> = categories.associate { it.id to it.name }
        val walletNameMap: Map<Int, String> = wallets.associate { it.id to it.name } // <--- Map Wallet

        csv.append("Backup Data - $exportDateString\n")
        // Header diperbarui (Gunakan Nama Dompet)
        csv.append("ID,Tanggal,Tipe,Kategori,Dompet,Jumlah,Catatan\n")

        transactions.forEach { transaction ->
            val transactionDate = fullDateFormat.format(Date(transaction.date))

            // Lookup Nama
            val categoryName = categoryNameMap[transaction.categoryId] ?: "Kategori Terhapus"
            val walletName = walletNameMap[transaction.walletId] ?: "Dompet Terhapus" // <--- Lookup Wallet

            val safeNotes = "\"${transaction.notes.replace("\"", "\"\"")}\"" // Escape quote

            csv.append("${transaction.id},")
            csv.append("$transactionDate,")
            csv.append("${transaction.type},")
            csv.append("\"$categoryName\",") // Pakai quote biar aman jika ada koma
            csv.append("\"$walletName\",")   // Pakai quote
            csv.append("${transaction.amount},")
            csv.append(safeNotes)
            csv.append("\n")
        }

        return csv.toString()
    }
}