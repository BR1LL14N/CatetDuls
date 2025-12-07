package com.example.catetduls.viewmodel

import androidx.lifecycle.*
import com.example.catetduls.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

/**
 * ViewModel untuk StatistikPage
 */
class StatistikViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
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

    // Periode Filter (Hari Ini, Minggu Ini, Bulan Ini, Tahun Ini)
    private val _selectedPeriod = MutableStateFlow("Bulan Ini")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    // Tahun Filter (Khusus untuk grafik bulanan/tahunan jika dikembangkan nanti)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    // Tipe Chart (Pemasukan / Pengeluaran)
    private val _chartType = MutableStateFlow(TransactionType.PENGELUARAN)
    val chartType: StateFlow<TransactionType> = _chartType.asStateFlow()

    // ========================================
    // Statistics Data
    // ========================================

    /**
     * Data Utama untuk Pie Chart & List Detail.
     * Menggabungkan filter Periode, Tipe Transaksi, dan Data Kategori.
     */
    val categoryStats: LiveData<List<CategoryExpense>> = combine(
        _selectedPeriod,
        _chartType,
        categoryRepository.getAllCategories()
    ) { period, type, allCategories ->
        Triple(period, type, allCategories)
    }.flatMapLatest { (period, type, allCategories) ->

        val (startDate, endDate) = getDateRange(period)

        // Ambil transaksi mentah berdasarkan tipe dan tanggal
        transactionRepository.getTransactionsByTypeAndDateRange(type, startDate, endDate)
            .map { transactions ->
                // 1. Grouping transaksi berdasarkan categoryId
                val groupedMap = transactions.groupBy { it.categoryId }

                // 2. Map ke object CategoryExpense (termasuk Ikon)
                val resultList = groupedMap.map { (catId, transList) ->
                    val totalAmount = transList.sumOf { it.amount }

                    // Cari detail kategori untuk nama & ikon
                    val category = allCategories.find { it.id == catId }
                    val catName = category?.name ?: "Lainnya"
                    val catIcon = category?.icon ?: "🏷️" // Default icon jika null

                    CategoryExpense(
                        categoryId = catId,
                        categoryName = catName,
                        icon = catIcon, // Field ini penting untuk UI baru
                        total = totalAmount
                    )
                }

                // 3. Sort dari nominal terbesar
                resultList.sortedByDescending { it.total }
            }
    }.asLiveData()

    /**
     * Kategori Terbesar (Otomatis diambil dari item pertama list di atas)
     */
    val topExpenseCategory: LiveData<CategoryExpense?> = categoryStats.map { list ->
        list.firstOrNull()
    }

    // ========================================
    // Period Totals (Header Data)
    // ========================================

    private val _periodIncome = MutableStateFlow<Double?>(null)
    val periodIncome: StateFlow<Double?> = _periodIncome.asStateFlow()

    private val _periodExpense = MutableStateFlow<Double?>(null)
    val periodExpense: StateFlow<Double?> = _periodExpense.asStateFlow()

    private val _periodBalance = MutableStateFlow<Double?>(null)
    val periodBalance: StateFlow<Double?> = _periodBalance.asStateFlow()

    // ========================================
    // Initialization & Actions
    // ========================================

    init {
        loadPeriodData()
    }

    fun setChartType(type: TransactionType) {
        _chartType.value = type
        // Tidak perlu loadPeriodData() manual karena Flow akan otomatis bereaksi
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        loadPeriodData() // Refresh data header
    }

    fun setYear(year: Int) {
        _selectedYear.value = year
    }

    private fun loadPeriodData() {
        val (startDate, endDate) = getDateRange(_selectedPeriod.value)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Launch paralel untuk Pemasukan & Pengeluaran agar tidak saling tunggu
                launch {
                    transactionRepository.getTotalByTypeAndDateRange(TransactionType.PEMASUKAN, startDate, endDate)
                        .collect { income ->
                            _periodIncome.value = income ?: 0.0
                            calculateBalance()
                        }
                }

                launch {
                    transactionRepository.getTotalByTypeAndDateRange(TransactionType.PENGELUARAN, startDate, endDate)
                        .collect { expense ->
                            _periodExpense.value = expense ?: 0.0
                            calculateBalance()
                        }
                }

                // Loading false bisa diatur di sini atau menggunakan logic terpisah
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Gagal memuat statistik: ${e.message}"
            }
        }
    }

    private fun calculateBalance() {
        val income = _periodIncome.value ?: 0.0
        val expense = _periodExpense.value ?: 0.0
        _periodBalance.value = income - expense
    }

    // ========================================
    // Helper Functions
    // ========================================

    private fun getDateRange(period: String): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis() // Reset ke Now

        return when (period) {
            "Harian", "Hari Ini" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            "Mingguan", "Minggu Ini" -> transactionRepository.getThisWeekDateRange()
            "Bulanan", "Bulan Ini" -> transactionRepository.getThisMonthDateRange()
            "Tahunan", "Tahun Ini" -> {
                val currentYear = calendar.get(Calendar.YEAR)
                calendar.set(Calendar.YEAR, currentYear)
                calendar.set(Calendar.MONTH, Calendar.JANUARY); calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.set(Calendar.YEAR, currentYear)
                calendar.set(Calendar.MONTH, Calendar.DECEMBER); calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            else -> transactionRepository.getThisMonthDateRange()
        }
    }

    fun formatCurrency(amount: Double?): String {
        if (amount == null) return "Rp 0"
        return "Rp ${String.format("%,.0f", amount)}"
    }

    fun getCategoryColor(index: Int): Int {
        val colors = listOf(
            android.graphics.Color.parseColor("#FF6384"), // Pink
            android.graphics.Color.parseColor("#36A2EB"), // Biru
            android.graphics.Color.parseColor("#FFCE56"), // Kuning
            android.graphics.Color.parseColor("#4BC0C0"), // Teal
            android.graphics.Color.parseColor("#9966FF"), // Ungu
            android.graphics.Color.parseColor("#FF9F40"), // Oranye
            android.graphics.Color.parseColor("#33FFCC"), // Hijau Muda
            android.graphics.Color.parseColor("#C9CBCF")  // Abu-abu
        )
        return colors[index % colors.size]
    }
}

/**
 * Factory untuk StatistikViewModel
 */
class StatistikViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatistikViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatistikViewModel(transactionRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}