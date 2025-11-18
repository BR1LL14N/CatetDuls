package com.example.catetduls.viewmodel

import androidx.lifecycle.*
import com.example.catetduls.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

/**
 * ViewModel untuk StatistikPage
 */
class StatistikViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    // ========================================
    // State Management
    // ========================================

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ========================================
    // Filter State
    // ========================================

    private val _selectedPeriod = MutableStateFlow("Bulan Ini")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    // ========================================
    // Statistics Data
    // ========================================

    /**
     * Total pengeluaran per kategori (untuk Pie Chart)
     */
    val expenseByCategory: LiveData<List<CategoryExpense>> =
        repository.getTotalExpenseByCategory().asLiveData()

    /**
     * Kategori dengan pengeluaran terbesar
     */
    val topExpenseCategory: LiveData<CategoryExpense?> =
        repository.getTopExpenseCategory().asLiveData()

    /**
     * Total pemasukan & pengeluaran per bulan (untuk Bar Chart)
     */
    val monthlyTotals: LiveData<List<MonthlyTotal>> = _selectedYear.asLiveData().switchMap { year ->
        repository.getMonthlyTotals(year).asLiveData()
    }

    // ========================================
    // Period Totals
    // ========================================

    private val _periodIncome = MutableStateFlow<Double?>(null)
    val periodIncome: StateFlow<Double?> = _periodIncome.asStateFlow()

    private val _periodExpense = MutableStateFlow<Double?>(null)
    val periodExpense: StateFlow<Double?> = _periodExpense.asStateFlow()

    private val _periodBalance = MutableStateFlow<Double?>(null)
    val periodBalance: StateFlow<Double?> = _periodBalance.asStateFlow()

    // ========================================
    // Initialization
    // ========================================

    init {
        loadPeriodData()
    }

    // ========================================
    // Actions
    // ========================================

    /**
     * Set periode filter
     */
    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        loadPeriodData()
    }

    /**
     * Set tahun untuk Bar Chart
     */
    fun setYear(year: Int) {
        _selectedYear.value = year
    }

    /**
     * Load data berdasarkan periode yang dipilih
     */
    private fun loadPeriodData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (startDate, endDate) = getDateRange(_selectedPeriod.value)

                // Load total pemasukan
                repository.getTotalByTypeAndDateRange(TransactionType.PEMASUKAN, startDate, endDate)
                    .collect { income ->
                        _periodIncome.value = income ?: 0.0
                    }

                // Load total pengeluaran
                repository.getTotalByTypeAndDateRange(TransactionType.PENGELUARAN, startDate, endDate)
                    .collect { expense ->
                        _periodExpense.value = expense ?: 0.0

                        // Hitung balance
                        val income = _periodIncome.value ?: 0.0
                        _periodBalance.value = income - (expense ?: 0.0)
                    }

                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Gagal memuat statistik: ${e.message}"
            }
        }
    }

    /**
     * Refresh semua data statistik
     */
    fun refreshStatistics() {
        loadPeriodData()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // ========================================
    // Helper Functions
    // ========================================

    /**
     * Get rentang tanggal berdasarkan periode
     */
    private fun getDateRange(period: String): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        return when (period) {
            "Hari Ini" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            "Minggu Ini" -> repository.getThisWeekDateRange()
            "Bulan Ini" -> repository.getThisMonthDateRange()
            "Tahun Ini" -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY); calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.MONTH, Calendar.DECEMBER); calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            else -> repository.getThisMonthDateRange()
        }
    }

    /**
     * Format currency
     */
    fun formatCurrency(amount: Double?): String {
        if (amount == null) return "Rp 0"
        return "Rp ${String.format("%,.0f", amount)}"
    }

    /**
     * Hitung persentase dari total
     */
    fun calculatePercentage(part: Double, total: Double): Float {
        if (total == 0.0) return 0f
        return ((part / total) * 100).toFloat()
    }

    /**
     * Get nama bulan
     */
    fun getMonthName(monthNumber: Int): String {
        val months = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
            "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
        )
        return if (monthNumber in 1..12) months[monthNumber - 1] else ""
    }

    /**
     * Get warna untuk kategori (untuk Pie Chart)
     */
    fun getCategoryColor(index: Int): Int {
        val colors = listOf(
            android.graphics.Color.parseColor("#FF6384"), // Pink
            android.graphics.Color.parseColor("#36A2EB"), // Biru
            android.graphics.Color.parseColor("#FFCE56"), // Kuning
            android.graphics.Color.parseColor("#4BC0C0"), // Cyan
            android.graphics.Color.parseColor("#9966FF"), // Ungu
            android.graphics.Color.parseColor("#FF9F40"), // Oranye
            android.graphics.Color.parseColor("#33FFCC"), // Hijau Muda
            android.graphics.Color.parseColor("#C9CBCF")  // Abu-abu
        )
        return colors[index % colors.size]
    }

    /**
     * Data untuk Pie Chart dengan persentase (Fungsi ini mungkin tidak perlu jika menggunakan PieEntry langsung)
     */
    fun getPieChartData(): LiveData<List<CategoryExpenseWithPercentage>> {
        return expenseByCategory.map { categories ->
            val total = categories.sumOf { it.total }
            categories.map { category ->
                CategoryExpenseWithPercentage(
                    categoryId = category.categoryId,
                    categoryName = category.categoryName,
                    total = category.total,
                    percentage = calculatePercentage(category.total, total)
                )
            }
        }
    }

    /**
     * Get summary text untuk periode
     */
    fun getPeriodSummaryText(): String {
        val income = _periodIncome.value ?: 0.0
        val expense = _periodExpense.value ?: 0.0
        val balance = income - expense

        // Menggunakan formatCurrency untuk angka, tapi tanpa "Rp"
        val formattedBalance = String.format("%,.0f", abs(balance))

        return when {
            balance > 0 -> "Anda surplus bulan ini: $formattedBalance"
            balance < 0 -> "Anda defisit bulan ini: $formattedBalance"
            else -> "Seimbang bulan ini."
        }
    }
}

/**
 * Data class untuk Pie Chart dengan persentase
 */
data class CategoryExpenseWithPercentage(
    val categoryId: Int,
    val categoryName: String,
    val total: Double,
    val percentage: Float
)

/**
 * Factory untuk StatistikViewModel
 */
class StatistikViewModelFactory(
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatistikViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatistikViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}