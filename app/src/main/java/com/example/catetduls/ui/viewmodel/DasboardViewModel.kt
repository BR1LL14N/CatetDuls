package com.example.catetduls.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel untuk DashboardPage
 *
 * Mengelola data:
 * - Total saldo
 * - Total pemasukan bulan ini
 * - Total pengeluaran bulan ini
 * - Transaksi terbaru
 * - Loading state
 */
class DashboardViewModel(private val repository: TransactionRepository) : ViewModel() {

    // ========================================
    // State Management
    // ========================================

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ========================================
    // Data Flows
    // ========================================

    /** Total saldo (pemasukan - pengeluaran) */
    val totalBalance = repository.getTotalBalance().asLiveData()

    /** Total pemasukan bulan ini */
    val totalIncomeThisMonth = repository.getTotalIncomeThisMonth().asLiveData()

    /** Total pengeluaran bulan ini */
    val totalExpenseThisMonth = repository.getTotalExpenseThisMonth().asLiveData()

    /** 5 Transaksi terakhir */
    val recentTransactions = repository.getRecentTransactions(5).asLiveData()

    // ========================================
    // Computed Properties
    // ========================================

    /** Format saldo ke Rupiah */
    /** Format saldo ke Rupiah */
    fun formatCurrency(amount: Double?, symbol: String = "Rp"): String {
        if (amount == null) return com.example.catetduls.utils.CurrencyHelper.format(0.0, symbol)
        return com.example.catetduls.utils.CurrencyHelper.format(amount, symbol)
    }

    /** Warna untuk saldo (hijau jika positif, merah jika negatif) */
    fun getBalanceColor(balance: Double?): Int {
        return if (balance != null && balance >= 0) {
            android.graphics.Color.parseColor("#4CAF50") // Hijau
        } else {
            android.graphics.Color.parseColor("#F44336") // Merah
        }
    }

    /** Persentase pengeluaran dari pemasukan */
    fun getExpensePercentage(income: Double?, expense: Double?): Float {
        if (income == null || expense == null || income == 0.0) return 0f
        return ((expense / income) * 100).toFloat()
    }

    // ========================================
    // Actions
    // ========================================

    /** Refresh data dashboard */
    fun refreshDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Data akan ter-update otomatis melalui Flow
                _isLoading.value = false
                _errorMessage.value = null
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Gagal memuat data: ${e.message}"
            }
        }
    }

    /** Clear error message */
    fun clearError() {
        _errorMessage.value = null
    }

    init {
        _isLoading.value = false
    }
}

/** Factory untuk membuat DashboardViewModel dengan repository */
class DashboardViewModelFactory(private val repository: TransactionRepository) :
        ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
