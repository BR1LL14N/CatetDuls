package com.example.catetduls.viewmodel

import androidx.lifecycle.*
import com.example.catetduls.data.*
import java.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** ViewModel untuk StatistikPage */
class StatistikViewModel(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val bookRepository: BookRepository
) : ViewModel() {

    // ========================================
    // State Management
    // ========================================

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Currency State
    private val _currencyCode = MutableStateFlow("IDR")
    private val _currencySymbol = MutableStateFlow("Rp")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()
    val currencyCode: StateFlow<String> = _currencyCode.asStateFlow()

    // Helper data class for combining 5 streams
    data class StatsParams(
        val period: String,
        val type: TransactionType,
        val categories: List<Category>,
        val currencyCode: String,
        val selectedDate: Long
    )

    // ========================================
    // Filter State
    // ========================================

    // Periode Filter (Hari Ini, Minggu Ini, Bulan Ini, Tahun Ini)
    private val _selectedPeriod = MutableStateFlow("Bulan Ini")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    // Tahun Filter (Khusus untuk grafik bulanan/tahunan jika dikembangkan nanti)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    // Tanggal yang dipilih (Default: Hari ini)
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    // Tipe Chart (Pemasukan / Pengeluaran)
    private val _chartType = MutableStateFlow(TransactionType.PENGELUARAN)
    val chartType: StateFlow<TransactionType> = _chartType.asStateFlow()

    // ========================================
    // Statistics Data
    // ========================================

    /**
     * Data Utama untuk Pie Chart & List Detail. Menggabungkan filter Periode, Tipe Transaksi, Data
     * Kategori, Mata Uang, DAN Tanggal yang dipilih.
     */
    val categoryStats: LiveData<List<CategoryExpense>> =
            combine(
                            _selectedPeriod,
                            _chartType,
                            categoryRepository.getAllCategories(),
                            _currencyCode,
                            _selectedDate // Tambahkan ini agar flow ke-trigger saat tanggal berubah
                    ) { period, type, allCategories, currencyCode, selectedDate ->
                        StatsParams(period, type, allCategories, currencyCode, selectedDate)
                    }
                    .flatMapLatest { (period, type, allCategories, currencyCode, _) -> // selectedDate tidak perlu dipakai langsung di sini, tapi mentrigger update via getDateRange yang pakai _selectedDate.value (atau lebih baik via param biar pure)
                        // Note: getDateRange internal pakai _selectedDate.value, tapi karena kita mentrigger ulang saat _selectedDate berubah, hasil getDateRange akan baru.
                        val (startDate, endDate) = getDateRange(period)

                        // Ambil transaksi mentah berdasarkan tipe dan tanggal
                        transactionRepository.getTransactionsByTypeAndDateRange(
                                        type,
                                        startDate,
                                        endDate
                                )
                                .map { transactions ->
                                    // 1. Grouping transaksi berdasarkan categoryId
                                    val groupedMap = transactions.groupBy { it.categoryId }

                                    // 2. Map ke object CategoryExpense (termasuk Ikon)
                                    val resultList =
                                            groupedMap.map { (catId, transList) ->
                                                val totalAmountIdr = transList.sumOf { it.amount }
                                                val totalAmountConverted =
                                                        com.example.catetduls.utils.CurrencyHelper
                                                                .convertIdrTo(
                                                                        totalAmountIdr,
                                                                        currencyCode
                                                                )

                                                // Cari detail kategori untuk nama & ikon
                                                val category = allCategories.find { it.id == catId }
                                                val catName = category?.name ?: "Lainnya"
                                                val catIcon =
                                                        category?.icon
                                                                ?: "🏷️" // Default icon jika null

                                                CategoryExpense(
                                                        categoryId = catId,
                                                        categoryName = catName,
                                                        icon = catIcon, // Field ini penting untuk
                                                        // UI baru
                                                        total = totalAmountConverted
                                                )
                                            }

                                    // 3. Sort dari nominal terbesar
                                    resultList.sortedByDescending { it.total }
                                }
                    }
                    .asLiveData()

    /** Kategori Terbesar (Otomatis diambil dari item pertama list di atas) */
    val topExpenseCategory: LiveData<CategoryExpense?> =
            categoryStats.map { list -> list.firstOrNull() }

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
        observeActiveBook()
    }

    private fun observeActiveBook() {
        viewModelScope.launch {
            bookRepository.getActiveBook().collect { book ->
                if (book != null) {
                    _currencySymbol.value = book.currencySymbol
                    _currencyCode.value = book.currencyCode // Update StateFlow
                    // Reload data if currency changes to ensure charts update
                    loadPeriodData()
                }
            }
        }
    }

    fun formatCurrency(amount: Double?): String {
        // Amount is assumed IDR. Convert it locally to display
        val amountIdr = amount ?: 0.0
        val converted =
                com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                        amountIdr,
                        _currencyCode.value
                )
        return com.example.catetduls.utils.CurrencyHelper.format(converted, _currencySymbol.value)
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
                    transactionRepository.getTotalByTypeAndDateRange(
                                    TransactionType.PEMASUKAN,
                                    startDate,
                                    endDate
                            )
                            .collect { income ->
                                _periodIncome.value = income ?: 0.0
                                calculateBalance()
                            }
                }

                launch {
                    transactionRepository.getTotalByTypeAndDateRange(
                                    TransactionType.PENGELUARAN,
                                    startDate,
                                    endDate
                            )
                            .collect { expense ->
                                _periodExpense.value = expense ?: 0.0
                                calculateBalance()
                            }
                }

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



    fun nextPeriod() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _selectedDate.value

        when (_selectedPeriod.value) {
            "Harian", "Hari Ini" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "Mingguan", "Minggu Ini" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "Bulanan", "Bulan Ini" -> calendar.add(Calendar.MONTH, 1)
            "Tahunan", "Tahun Ini" -> calendar.add(Calendar.YEAR, 1)
        }

        _selectedDate.value = calendar.timeInMillis
        loadPeriodData()
    }

    fun prevPeriod() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _selectedDate.value

        when (_selectedPeriod.value) {
            "Harian", "Hari Ini" -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            "Mingguan", "Minggu Ini" -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
            "Bulanan", "Bulan Ini" -> calendar.add(Calendar.MONTH, -1)
            "Tahunan", "Tahun Ini" -> calendar.add(Calendar.YEAR, -1)
        }

        _selectedDate.value = calendar.timeInMillis
        loadPeriodData()
    }

    private fun getDateRange(period: String): Pair<Long, Long> {
        // Gunakan _selectedDate sebagai basis, bukan System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _selectedDate.value

        return when (period) {
            "Harian", "Hari Ini" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            "Mingguan", "Minggu Ini" -> {
                // Set ke awal minggu (Senin)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                // Set ke akhir minggu (Minggu)
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            "Bulanan", "Bulan Ini" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.set(
                        Calendar.DAY_OF_MONTH,
                        calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                )
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            "Tahunan", "Tahun Ini" -> {
                val currentYear = calendar.get(Calendar.YEAR)
                calendar.set(Calendar.YEAR, currentYear)
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis

                calendar.set(Calendar.YEAR, currentYear)
                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                Pair(start, end)
            }
            else -> {
                // Default fallback
                val now = Calendar.getInstance()
                now.set(Calendar.DAY_OF_MONTH, 1)
                now.set(Calendar.HOUR_OF_DAY, 0)
                now.set(Calendar.MINUTE, 0)
                now.set(Calendar.SECOND, 0)
                now.set(Calendar.MILLISECOND, 0)
                val start = now.timeInMillis
                now.set(Calendar.DAY_OF_MONTH, now.getActualMaximum(Calendar.DAY_OF_MONTH))
                now.set(Calendar.HOUR_OF_DAY, 23)
                now.set(Calendar.MINUTE, 59)
                now.set(Calendar.SECOND, 59)
                now.set(Calendar.MILLISECOND, 999)
                val end = now.timeInMillis
                Pair(start, end)
            }
        }
    }

    fun getCategoryColor(index: Int): Int {
        val colors =
                listOf(
                        android.graphics.Color.parseColor("#FF6384"), // Pink
                        android.graphics.Color.parseColor("#36A2EB"), // Biru
                        android.graphics.Color.parseColor("#FFCE56"), // Kuning
                        android.graphics.Color.parseColor("#4BC0C0"), // Teal
                        android.graphics.Color.parseColor("#9966FF"), // Ungu
                        android.graphics.Color.parseColor("#FF9F40"), // Oranye
                        android.graphics.Color.parseColor("#33FFCC"), // Hijau Muda
                        android.graphics.Color.parseColor("#C9CBCF") // Abu-abu
                )
        return colors[index % colors.size]
    }
}

/** Factory untuk StatistikViewModel */
class StatistikViewModelFactory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val bookRepository: BookRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatistikViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatistikViewModel(transactionRepository, categoryRepository, bookRepository) as
                    T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
