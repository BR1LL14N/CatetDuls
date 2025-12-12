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

import com.example.catetduls.ui.adapter.CalendarDayCell

class CalendarViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    // State bulan yang sedang dilihat (hanya perlu timestamp di dalam bulan tersebut)
    private val _currentMonth = MutableStateFlow(System.currentTimeMillis())
    val currentMonth: StateFlow<Long> = _currentMonth.asStateFlow()

    // Data untuk Grid (42 Hari)
    private val _gridSummaryData = MutableLiveData<List<DailySummary>>()
    
    // Data untuk Totals (1 Bulan Penuh)
    private val _monthSummaryData = MutableLiveData<List<DailySummary>>()

    // Summary Bulanan (dari data strict bulan ini)
    val monthlyIncome: LiveData<Double> = _monthSummaryData.map { list -> list.sumOf { it.totalIncome } }
    val monthlyExpense: LiveData<Double> = _monthSummaryData.map { list -> list.sumOf { it.totalExpense } }
    val monthlyTotal: LiveData<Double> = monthlyIncome.map { it - (monthlyExpense.value ?: 0.0) }

    // Data Kalender yang akan di binding ke RecyclerView (List of Cells)
    private val _calendarGridCells = MutableLiveData<List<CalendarDayCell>>()
    val calendarGridCells: LiveData<List<CalendarDayCell>> = _calendarGridCells

    init {
        loadMonthlyData()
    }

    /**
     * Memuat data ringkasan harian untuk bulan yang sedang aktif
     */
    fun loadMonthlyData() {
        viewModelScope.launch {
            val currentTimestamp = _currentMonth.value
            val calendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
            
            // ==========================================
            // 1. Calculate Strict Month Range (For Totals)
            // ==========================================
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis
            
            val currentMonthIndex = calendar.get(Calendar.MONTH)

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val monthEnd = calendar.timeInMillis
            
            // Launch coroutine for totals
            launch {
                repository.getMonthlyDailySummary(monthStart, monthEnd).collect { summaryList ->
                    _monthSummaryData.value = summaryList
                }
            }

            // ==========================================
            // 2. Calculate Grid Range (42 Days)
            // ==========================================
            val gridCal = Calendar.getInstance()
            gridCal.timeInMillis = monthStart // Start from 1st
            
            val firstDayOfWeek = gridCal.get(Calendar.DAY_OF_WEEK) // 1=Sun
            val daysBefore = firstDayOfWeek - 1
            
            val gridStartCalendar = gridCal.clone() as Calendar
            gridStartCalendar.add(Calendar.DAY_OF_YEAR, -daysBefore)
            val gridStartTime = gridStartCalendar.timeInMillis
            
            val totalCells = 42
            val gridEndCalendar = gridStartCalendar.clone() as Calendar
            gridEndCalendar.add(Calendar.DAY_OF_YEAR, totalCells - 1)
            gridEndCalendar.set(Calendar.HOUR_OF_DAY, 23)
            gridEndCalendar.set(Calendar.MINUTE, 59)
            gridEndCalendar.set(Calendar.SECOND, 59)
            val gridEndTime = gridEndCalendar.timeInMillis

            // Launch coroutine for grid
            launch {
                 repository.getMonthlyDailySummary(gridStartTime, gridEndTime).collect { summaryList ->
                    _gridSummaryData.value = summaryList
                    
                    // Map to Cells
                    val mappedCells = ArrayList<CalendarDayCell>()
                    val iteratorCal = gridStartCalendar.clone() as Calendar

                    for (i in 0 until totalCells) {
                        val cellTimestamp = iteratorCal.timeInMillis
                        val dayOfMonth = iteratorCal.get(Calendar.DAY_OF_MONTH)
                        val monthIndex = iteratorCal.get(Calendar.MONTH)
                        val isCurrentMonth = (monthIndex == currentMonthIndex)
                        val isToday = isSameDay(cellTimestamp, System.currentTimeMillis())

                        // Logika Pencocokan Summary
                        // Karena DailySummary mungkin ambigu (hanya dayOfMonth), kita perlu hati-hati.
                        // Namun karena range kita (42 hari) maksimal mencakup bagian dari 3 bulan yang berbeda,
                        // dan tanggal 1-10 di bulan AWAL vs 1-10 di bulan AKHIR bisa bertabrakan jika hanya cek dayOfMonth.
                        // Idealnya DailySummary punya timestamp. Jika tidak, kita best-effort.
                        
                        // Strat: Cari summary dengan dayOfMonth yang cocok.
                        // Jika isCurrentMonth=true, cari yang match.
                        // Jika isCurrentMonth=false, kita juga cari, tapi jika Repo mengembalikan semua yg match dalam range,
                        // kita mungkin mendapatkan data 'tanggal 1' milik next month saat merender 'tanggal 1' milik current month?
                        // Tidak, karena iterator berjalan sekuensial.
                        // MASALAH: summaryList adalah Flat List. Kita tidak tahu mana yg milik timestamp ini jika tidak ada info bulan.
                        // SOLUSI SEMENTARA: Filter berdasarkan isCurrentMonth.
                        // Jika isCurrentMonth, kita cari di summaryList (yang overlap dengan current month).
                        // Jika Tidak, kita kosongkan dulu untuk keamanan (kecuali kita yakin).
                        // Mengingat user meminta "kosong tapi ada cell", ini aman.
                        
                        val matchedSummary = if (isCurrentMonth) {
                             summaryList.find { it.dayOfMonth == dayOfMonth }
                        } else {
                             // Coba cari juga untuk prev/next month jika repo handle dengan benar
                             // Asumsi: Repo returning correct date range.
                             // Risiko: Tabrakan tanggal.
                             // Safe: null
                             null 
                        }

                        mappedCells.add(CalendarDayCell(
                            dayOfMonth = dayOfMonth,
                            summary = matchedSummary,
                            isCurrentMonth = isCurrentMonth,
                            isToday = isToday,
                            timestamp = cellTimestamp
                        ))
                        
                        iteratorCal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    _calendarGridCells.postValue(mappedCells)
                }
            }
        }
    }

    private fun isDateInCurrentMonth(dayOfMonth: Int): Boolean {
        // Now redundant as we use separate fetch, but keeping for safety if needed
        return true 
    }
    
    // Better `isSameDay` utility
    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
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