package com.example.catetduls.ui.viewmodel

import androidx.lifecycle.*
import com.example.catetduls.data.*
import com.example.catetduls.ui.adapter.TransactionListItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class DetailStatistikViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // ===================================
    // INPUT STATE (Dari Fragment)
    // ===================================
    private val _categoryId = MutableStateFlow<Int>(0)
    private val _selectedMonthYear = MutableStateFlow(System.currentTimeMillis())

    // ===================================
    // OUTPUT DATA (Ke Fragment)
    // ===================================

    // 1. Info Kategori (Nama & Ikon)
    val categoryInfo: LiveData<Category?> = _categoryId.flatMapLatest { id ->
        categoryRepository.getCategoryById(id)
    }.asLiveData()

    // 2. Total Pengeluaran Bulan Ini (Yang tampil besar di atas)
    val currentMonthTotal = MutableLiveData<Double>(0.0)

    // 3. Data Grafik Garis (12 Bulan di Tahun terpilih)
    val lineChartData = MutableLiveData<List<Float>>() // List 12 float (Jan-Des)

    // 4. Riwayat Transaksi (Grouped by Date)
    val transactionHistory: LiveData<List<TransactionListItem>> = combine(
        _categoryId,
        _selectedMonthYear
    ) { catId, timestamp ->
        Pair(catId, timestamp)
    }.flatMapLatest { (catId, timestamp) ->

        // Hitung range tanggal (Awal bulan s/d Akhir bulan)
        val (start, end) = getMonthRange(timestamp)

        // Ambil transaksi khusus kategori ini di bulan ini
        transactionRepository.getTransactionsByCategoryAndDateRange(catId, start, end)
            .map { list ->
                // Hitung total bulan ini sekalian
                val total = list.sumOf { it.amount }
                currentMonthTotal.postValue(total) // Update UI Total Besar

                // Grouping untuk RecyclerView (Sama seperti di TransaksiPage)
                val result = mutableListOf<TransactionListItem>()
                val grouped = list.groupBy { trans ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = trans.date
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }

                grouped.forEach { (date, dailyList) ->
                    val income = dailyList.filter { it.type == TransactionType.PEMASUKAN }.sumOf { it.amount }
                    val expense = dailyList.filter { it.type == TransactionType.PENGELUARAN }.sumOf { it.amount }

                    // Header Tanggal
                    result.add(TransactionListItem.DateHeader(date, income, expense))
                    // Item Transaksi
                    dailyList.forEach { result.add(TransactionListItem.TransactionItem(it)) }
                }
                result
            }
    }.asLiveData()

    // ===================================
    // ACTIONS
    // ===================================

    fun setCategoryId(id: Int) {
        _categoryId.value = id
        loadYearlyTrend(id) // Load data grafik saat kategori di-set
    }

    fun setMonthYear(timestamp: Long) {
        _selectedMonthYear.value = timestamp
        // Jika bulan berubah, kita perlu reload data grafik juga (karena tahun mungkin berubah)
        loadYearlyTrend(_categoryId.value, timestamp)
    }

    /**
     * Navigasi Bulan (< Des 2025 >)
     */
    fun navigateMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = _selectedMonthYear.value
        cal.add(Calendar.MONTH, offset)
        setMonthYear(cal.timeInMillis)
    }

    /**
     * Mengambil data tren 1 tahun (Jan - Des) untuk Line Chart
     */
    private fun loadYearlyTrend(catId: Int, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            val year = cal.get(Calendar.YEAR)

            // Siapkan range 1 tahun (1 Jan - 31 Des)
            cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
            val startYear = cal.timeInMillis

            cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
            val endYear = cal.timeInMillis

            // Ambil semua transaksi kategori ini dalam 1 tahun
            // Note: Kita pakai 'getTotalByCategoryAndDateRange' di repository tapi kita butuh breakdown per bulan.
            // Cara manual: Ambil semua transaksi setahun, lalu group by month di Kotlin.

            val transactions = transactionRepository.getTransactionsByCategoryAndDateRange(catId, startYear, endYear).first()

            val monthlyData = FloatArray(12) { 0f } // Array 12 slot (0-11)

            transactions.forEach { trans ->
                val c = Calendar.getInstance()
                c.timeInMillis = trans.date
                val monthIndex = c.get(Calendar.MONTH) // 0 = Jan, 11 = Des
                monthlyData[monthIndex] += trans.amount.toFloat()
            }

            lineChartData.value = monthlyData.toList()
        }
    }

    // ===================================
    // HELPERS
    // ===================================

    private fun getMonthRange(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun formatMonthYear(timestamp: Long): String {
        val format = java.text.SimpleDateFormat("MMM yyyy", Locale("id", "ID"))
        return format.format(Date(timestamp))
    }
}

class DetailStatistikViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailStatistikViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailStatistikViewModel(transactionRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}