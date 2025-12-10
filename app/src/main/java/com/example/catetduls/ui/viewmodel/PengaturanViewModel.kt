package com.example.catetduls.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.*
import com.example.catetduls.data.local.TokenManager
//import com.example.catetduls.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import javax.inject.Inject

@HiltViewModel
class PengaturanViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ========================================
    // Auth State
    // ========================================
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow("Tamu")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val activeBookId: Int by lazy {
        try {
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getInt("active_book_id", 1)
        } catch (e: Exception) {
            1
        }
    }

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
        // ✅ PERBAIKAN: Panggil loadThemePreference() LANGSUNG (bukan di launch)
        loadThemePreference()

        // ✅ Fungsi async tetap di launch
        viewModelScope.launch {
            checkLoginStatus()
        }
    }

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
                try {
                    userRepository.logoutRemote()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                TokenManager.clearToken(context)
                userRepository.logout()

                _isLoggedIn.value = false
                _userName.value = "Tamu"
                _successMessage.value = "Berhasil keluar"

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
            // TODO: Implement your export logic
            null
        } catch (e: Exception) {
            _errorMessage.value = "Gagal export JSON: ${e.message}"
            null
        }
    }

    suspend fun exportToCsv(): String? {
        return try {
            // TODO: Implement your export logic
            null
        } catch (e: Exception) {
            _errorMessage.value = "Gagal export CSV: ${e.message}"
            null
        }
    }

    fun saveBackupToFile(data: String): File? {
        return try {
            // TODO: Implement your backup logic
            null
        } catch (e: Exception) {
            _errorMessage.value = "Gagal simpan backup: ${e.message}"
            null
        }
    }

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
                // TODO: Implement reset categories
                _successMessage.value = "Kategori berhasil direset"
            } catch (e: Exception) {
                _errorMessage.value = "Gagal reset kategori: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getAppVersion(): String = "1.0.0"

    fun getAppStatistics(): Map<String, String> {
        return try {
            mapOf(
                "Total Transaksi" to "0",
                "Total Kategori" to "0"
            )
        } catch (e: Exception) {
            mapOf(
                "Total Transaksi" to "-",
                "Total Kategori" to "-"
            )
        }
    }

    fun getBackupFileName(ext: String): String {
        val timestamp = System.currentTimeMillis()
        return "backup_$timestamp.$ext"
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}