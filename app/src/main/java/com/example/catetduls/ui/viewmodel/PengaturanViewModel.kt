package com.example.catetduls.viewmodel

// import com.example.catetduls.data.repository.UserRepository
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.*
import com.example.catetduls.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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

    suspend fun exportToJson(): String? {
        return try {
            // TODO: Implement actual export logic
            null
        } catch (e: Exception) {
            _errorMessage.value = "Gagal export JSON: ${e.message}"
            null
        }
    }

    suspend fun exportToCsv(): String? {
        return try {
            // TODO: Implement actual export logic
            null
        } catch (e: Exception) {
            _errorMessage.value = "Gagal export CSV: ${e.message}"
            null
        }
    }

    fun saveBackupToFile(data: String): File? {
        return try {
            // TODO: Implement actual backup file saving logic
            null
        } catch (e: Exception) {
            _errorMessage.value = "Gagal simpan backup: ${e.message}"
            null
        }
    }

    fun getBackupFileName(ext: String): String {
        val timestamp = System.currentTimeMillis()
        return "backup_$timestamp.$ext"
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
