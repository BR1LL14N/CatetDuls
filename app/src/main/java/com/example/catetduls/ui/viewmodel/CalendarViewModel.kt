package com.example.catetduls.viewmodel

import androidx.lifecycle.*
import com.example.catetduls.data.DailySummary
import com.example.catetduls.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class CalendarViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    // State bulan yang sedang dilihat (hanya perlu timestamp di dalam bulan tersebut)
    private val _currentMonth = MutableStateFlow(System.currentTimeMillis())
    val currentMonth: StateFlow<Long> = _currentMonth.asStateFlow()

    // Data ringkasan transaksi harian untuk bulan ini
    private val _dailySummaryData = MutableLiveData<List<DailySummary>>()

    // Summary Bulanan (dari data harian)
    val monthlyIncome: LiveData<Double> = _dailySummaryData.map { list -> list.sumOf { it.totalIncome } }
    val monthlyExpense: LiveData<Double> = _dailySummaryData.map { list -> list.sumOf { it.totalExpense } }
    val monthlyTotal: LiveData<Double> = monthlyIncome.map { it - (monthlyExpense.value ?: 0.0) }

    // Data Kalender yang akan di binding ke RecyclerView (Map<Tanggal Hari, Summary>)
    val calendarData: LiveData<Map<Int, DailySummary>> = _dailySummaryData.map { list ->
        list.associateBy { it.dayOfMonth }
    }

    // Jumlah minggu yang akan ditampilkan di grid
    val totalWeeks: LiveData<Int> = currentMonth.asLiveData().map { timestamp ->
        getWeeksInMonth(timestamp)
    }


    init {
        loadMonthlyData()
    }

    /**
     * Memuat data ringkasan harian untuk bulan yang sedang aktif
     */
    fun loadMonthlyData() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance().apply { timeInMillis = _currentMonth.value }

            // Dapatkan rentang tanggal untuk bulan yang sedang dilihat
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startDate = calendar.timeInMillis

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endDate = calendar.timeInMillis

            // Ambil data dari Repository
            repository.getMonthlyDailySummary(startDate, endDate).collect { summaryList ->
                _dailySummaryData.value = summaryList
            }
        }
    }

    /**
     * Navigasi ke bulan sebelumnya
     */
    fun prevMonth() {
        navigateMonth(-1)
    }

    /**
     * Navigasi ke bulan berikutnya
     */
    fun nextMonth() {
        navigateMonth(1)
    }

    private fun navigateMonth(offset: Int) {
        val calendar = Calendar.getInstance().apply { timeInMillis = _currentMonth.value }
        calendar.add(Calendar.MONTH, offset)
        _currentMonth.value = calendar.timeInMillis
        loadMonthlyData()
    }

    // ========================================
    // Helper & Formatting
    // ========================================

    fun formatMonthYear(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id", "ID"))
        val year = calendar.get(Calendar.YEAR)
        return "$month $year"
    }

    fun formatCurrency(amount: Double): String {
        val format = java.text.NumberFormat.getNumberInstance(Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(abs(amount))
    }

    /**
     * Menghitung jumlah minggu yang dibutuhkan untuk grid.
     */
    fun getWeeksInMonth(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        // PENTING: Atur agar minggu dimulai dari MINGGU (standar Android)
        val tempCalendar = Calendar.getInstance()
        tempCalendar.timeInMillis = timestamp
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)
        val maxDays = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val totalDays = maxDays + (firstDayOfWeek - 1)
        return (totalDays / 7.0).roundToInt().coerceIn(5, 6)
    }

    /**
     * Mengembalikan hari dalam seminggu (1=Minggu, 7=Sabtu) dari hari pertama bulan
     */
    fun getFirstDayOfWeek(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.get(Calendar.DAY_OF_WEEK)
    }
}


class CalendarViewModelFactory(
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}