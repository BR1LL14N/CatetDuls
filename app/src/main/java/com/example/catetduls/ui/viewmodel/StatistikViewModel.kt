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

    private val _selectedPeriod = MutableStateFlow("Bulan Ini")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    // State untuk Tipe Chart (Default: Pengeluaran)
    private val _chartType = MutableStateFlow(TransactionType.PENGELUARAN)
    val chartType: StateFlow<TransactionType> = _chartType.asStateFlow()

    // ========================================
    // Statistics Data
    // ========================================

    /**
     * LOGIC BARU: Pie Chart Dinamis
     * Menggabungkan 3 Flow: Periode, Tipe Chart, dan Semua Kategori.
     */

    val expenseByCategory: LiveData<List<CategoryExpense>> = combine(
        _selectedPeriod,
        _chartType,
        categoryRepository.getAllCategories()

    ) { period: String, type: TransactionType, allCategories: List<Category> ->

        Triple(period, type, allCategories)
    }

        .flatMapLatest { triple: Triple<String, TransactionType, List<Category>> ->
            val (period, type, allCategories) = triple

            val (startDate, endDate) = getDateRange(period)

            transactionRepository.getTransactionsByTypeAndDateRange(type, startDate, endDate)
                .map { transactions: List<Transaction> ->

                    val groupedMap = transactions.groupBy { it.categoryId }

                    val resultList = groupedMap.map { (catId, transList) ->
                        val totalAmount = transList.sumOf { it.amount }

                        val catName = allCategories.find { it.id == catId }?.name ?: "Unknown"

                        CategoryExpense(
                            categoryId = catId,
                            categoryName = catName,
                            total = totalAmount
                        )
                    }

                    resultList.sortedByDescending { it.total }
                }
        }
        .asLiveData()

    /**
     * Kategori dengan pengeluaran terbesar
     */
    val topExpenseCategory: LiveData<CategoryExpense?> = expenseByCategory.map { list ->
        list.firstOrNull()
    }

    /**
     * Total pemasukan & pengeluaran per bulan (untuk Bar Chart)
     */
    val monthlyTotals: LiveData<List<MonthlyTotal>> = _selectedYear.asLiveData().switchMap { year ->
        transactionRepository.getMonthlyTotals(year).asLiveData()
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
     * Action Baru: Set Tipe Chart
     */
    fun setChartType(type: TransactionType) {
        _chartType.value = type
        loadPeriodData()
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        loadPeriodData()
    }

    fun setYear(year: Int) {
        _selectedYear.value = year
    }

    private fun loadPeriodData() {
        // Ambil range tanggal dulu
        val (startDate, endDate) = getDateRange(_selectedPeriod.value)

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Menggunakan flow.collect untuk memperbarui StateFlow

                // Pemasukan
                launch {
                    transactionRepository.getTotalByTypeAndDateRange(TransactionType.PEMASUKAN, startDate, endDate)
                        .collect { income ->
                            _periodIncome.value = income ?: 0.0
                            calculateBalance()
                        }
                }

                // Pengeluaran
                launch {
                    transactionRepository.getTotalByTypeAndDateRange(TransactionType.PENGELUARAN, startDate, endDate)
                        .collect { expense ->
                            _periodExpense.value = expense ?: 0.0
                            calculateBalance()
                        }
                }

                // Catatan: isLoading=false harus diatur saat data pertama kali dimuat.
                // Karena 'collect' adalah operasi berkelanjutan, kita tidak bisa menganggap
                // data selesai dimuat di sini. Lebih baik menggunakan State khusus di UI.
                // Namun, untuk sementara, kita biarkan di sini.
                _isLoading.value = false

            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Gagal memuat statistik: ${e.message}"
            }
        }
    }

    // Helper kecil untuk menghitung saldo (agar tidak duplikat kode)
    private fun calculateBalance() {
        val income = _periodIncome.value ?: 0.0
        val expense = _periodExpense.value ?: 0.0
        _periodBalance.value = income - expense
    }

    fun refreshStatistics() {
        loadPeriodData()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ========================================
    // Helper Functions
    // ========================================

    private fun getDateRange(period: String): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        // Reset ke waktu sekarang agar kalkulasi akurat
        calendar.timeInMillis = System.currentTimeMillis()

        return when (period) {
            "Hari Ini" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            "Minggu Ini" -> transactionRepository.getThisWeekDateRange()
            "Bulan Ini" -> transactionRepository.getThisMonthDateRange()
            "Tahun Ini" -> {
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

    fun calculatePercentage(part: Double, total: Double): Float {
        if (total == 0.0) return 0f
        return ((part / total) * 100).toFloat()
    }

    fun getMonthName(monthNumber: Int): String {
        val months = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
            "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
        )
        return if (monthNumber in 1..12) months[monthNumber - 1] else ""
    }

    fun getCategoryColor(index: Int): Int {
        val colors = listOf(
            android.graphics.Color.parseColor("#FF6384"),
            android.graphics.Color.parseColor("#36A2EB"),
            android.graphics.Color.parseColor("#FFCE56"),
            android.graphics.Color.parseColor("#4BC0C0"),
            android.graphics.Color.parseColor("#9966FF"),
            android.graphics.Color.parseColor("#FF9F40"),
            android.graphics.Color.parseColor("#33FFCC"),
            android.graphics.Color.parseColor("#C9CBCF")
        )
        return colors[index % colors.size]
    }

    // Fungsi Data Class Helper (opsional, jika tidak dipakai bisa dihapus)
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

    fun getPeriodSummaryText(): String {
        val income = _periodIncome.value ?: 0.0
        val expense = _periodExpense.value ?: 0.0
        val balance = income - expense
        val formattedBalance = String.format("%,.0f", abs(balance))

        return when {
            balance > 0 -> "Anda surplus bulan ini: ${formatCurrency(balance)}"
            balance < 0 -> "Anda defisit bulan ini: ${formatCurrency(abs(balance))}"
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