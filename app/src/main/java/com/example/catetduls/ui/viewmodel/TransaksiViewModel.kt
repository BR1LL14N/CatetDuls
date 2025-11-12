package com.example.catetduls.viewmodel

import androidx.lifecycle.*
import com.example.catetduls.data.Transaction
import com.example.catetduls.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel untuk TransaksiPage
 *
 * Mengelola:
 * - Daftar transaksi dengan filter
 * - Search transaksi
 * - Delete transaksi
 * - Filter berdasarkan tipe, kategori, tanggal
 */
class TransaksiViewModel(
    private val repository: TransactionRepository
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
    // Filter States
    // ========================================

    private val _selectedType = MutableStateFlow<String?>(null) // "Pemasukan", "Pengeluaran", atau null (semua)
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ========================================
    // Data
    // ========================================

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    /**
     * Total dari transaksi yang sedang ditampilkan
     */
    val displayedTotalIncome: LiveData<Double> = transactions.map { list ->
        list.filter { it.type == "Pemasukan" }.sumOf { it.amount }
    }

    val displayedTotalExpense: LiveData<Double> = transactions.map { list ->
        list.filter { it.type == "Pengeluaran" }.sumOf { it.amount }
    }

    // ========================================
    // Initialization
    // ========================================

    init {
        loadTransactions()
    }

    // ========================================
    // Actions
    // ========================================

    /**
     * Load transaksi berdasarkan filter yang aktif
     */
    fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val query = _searchQuery.value

                // Jika ada search query, gunakan search
                if (query.isNotBlank()) {
                    repository.searchTransactions(query)
                        .collect { _transactions.value = it }
                } else {
                    // Gunakan filter
                    repository.getFilteredTransactions(
                        type = _selectedType.value,
                        categoryId = _selectedCategoryId.value,
                        startDate = _startDate.value,
                        endDate = _endDate.value
                    ).collect { _transactions.value = it }
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Gagal memuat transaksi: ${e.message}"
            }
        }
    }

    /**
     * Set filter tipe (Pemasukan/Pengeluaran)
     */
    fun setTypeFilter(type: String?) {
        _selectedType.value = type
        loadTransactions()
    }

    /**
     * Set filter kategori
     */
    fun setCategoryFilter(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
        loadTransactions()
    }

    /**
     * Set filter rentang tanggal
     */
    fun setDateRangeFilter(startDate: Long?, endDate: Long?) {
        _startDate.value = startDate
        _endDate.value = endDate
        loadTransactions()
    }

    /**
     * Search transaksi berdasarkan notes
     */
    fun searchTransactions(query: String) {
        _searchQuery.value = query
        loadTransactions()
    }

    /**
     * Clear semua filter
     */
    fun clearFilters() {
        _selectedType.value = null
        _selectedCategoryId.value = null
        _startDate.value = null
        _endDate.value = null
        _searchQuery.value = ""
        loadTransactions()
    }

    /**
     * Delete transaksi
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                _successMessage.value = "Transaksi berhasil dihapus"
                loadTransactions()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal menghapus transaksi: ${e.message}"
            }
        }
    }

    /**
     * Get transaksi berdasarkan ID (untuk detail/edit)
     */
    fun getTransactionById(id: Int): LiveData<Transaction?> {
        return repository.getTransactionById(id).asLiveData()
    }

    // ========================================
    // Helper Functions
    // ========================================

    /**
     * Apakah ada filter aktif?
     */
    fun hasActiveFilters(): Boolean {
        return _selectedType.value != null ||
                _selectedCategoryId.value != null ||
                _startDate.value != null ||
                _endDate.value != null ||
                _searchQuery.value.isNotBlank()
    }

    /**
     * Format tanggal untuk display
     */
    fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        return "$day/$month/$year"
    }

    /**
     * Format currency
     */
    fun formatCurrency(amount: Double): String {
        return "Rp ${String.format("%,.0f", amount)}"
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // ========================================
    // Quick Filters (Helper)
    // ========================================

    /**
     * Filter transaksi bulan ini
     */
    fun filterThisMonth() {
        val (start, end) = repository.getThisMonthDateRange()
        setDateRangeFilter(start, end)
    }

    /**
     * Filter transaksi minggu ini
     */
    fun filterThisWeek() {
        val (start, end) = repository.getThisWeekDateRange()
        setDateRangeFilter(start, end)
    }

    /**
     * Filter hari ini
     */
    fun filterToday() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis

        setDateRangeFilter(start, end)
    }
}

/**
 * Factory untuk TransaksiViewModel
 */
class TransaksiViewModelFactory(
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransaksiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransaksiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}