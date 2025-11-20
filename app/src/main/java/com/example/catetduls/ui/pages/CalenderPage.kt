package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.DailySummary
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.ui.adapter.CalendarAdapter
import com.example.catetduls.ui.adapter.CalendarDayCell
import com.example.catetduls.viewmodel.CalendarViewModel
import com.example.catetduls.viewmodel.CalendarViewModelFactory
import java.util.*
import kotlin.collections.ArrayList

class CalendarPage : Fragment() {

    private lateinit var viewModel: CalendarViewModel

    // Views
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView
    private lateinit var tvCurrentMonth: TextView
    private lateinit var tvMonthlyIncome: TextView
    private lateinit var tvMonthlyExpense: TextView
    private lateinit var tvMonthlyTotal: TextView
    private lateinit var rvCalendarGrid: RecyclerView

    // Adapter
    private var calendarAdapter: CalendarAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_kalender_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi ViewModel
        val repository = requireContext().getTransactionRepository()
        val factory = CalendarViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CalendarViewModel::class.java]

        initViews(view)
        setupCalendarGrid()
        setupListeners()
        observeData()
    }

    private fun initViews(view: View) {
        btnPrevMonth = view.findViewById(R.id.btn_prev_month)
        btnNextMonth = view.findViewById(R.id.btn_next_month)
        tvCurrentMonth = view.findViewById(R.id.tv_current_month)
        tvMonthlyIncome = view.findViewById(R.id.tv_monthly_income)
        tvMonthlyExpense = view.findViewById(R.id.tv_monthly_expense)
        tvMonthlyTotal = view.findViewById(R.id.tv_monthly_total)
        rvCalendarGrid = view.findViewById(R.id.rv_calendar_grid)
    }

    private fun setupListeners() {
        btnPrevMonth.setOnClickListener { viewModel.prevMonth() }
        btnNextMonth.setOnClickListener { viewModel.nextMonth() }
    }

    private fun setupCalendarGrid() {
        calendarAdapter = CalendarAdapter(emptyList())
        rvCalendarGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        rvCalendarGrid.adapter = calendarAdapter
    }

    private fun observeData() {
        // Observe Bulan Aktif untuk Header
        viewModel.currentMonth.asLiveData().observe(viewLifecycleOwner) { timestamp ->
            tvCurrentMonth.text = viewModel.formatMonthYear(timestamp)
        }

        // Observe Ringkasan Bulanan (Summary Bar)
        viewModel.monthlyIncome.observe(viewLifecycleOwner) { income ->
            tvMonthlyIncome.text = viewModel.formatCurrency(income ?: 0.0)
        }
        viewModel.monthlyExpense.observe(viewLifecycleOwner) { expense ->
            tvMonthlyExpense.text = viewModel.formatCurrency(expense ?: 0.0)
        }
        viewModel.monthlyTotal.observe(viewLifecycleOwner) { total ->
            tvMonthlyTotal.text = viewModel.formatCurrency(total ?: 0.0)
        }

        // Observe Data Kalender untuk Grid
        viewModel.calendarData.observe(viewLifecycleOwner) { dailySummaryMap ->

            val currentMonthTimestamp = viewModel.currentMonth.value

            // Dapatkan hari dalam seminggu dari hari pertama bulan (1=Minggu, 7=Sabtu)
            val firstDayOfWeek = viewModel.getFirstDayOfWeek(currentMonthTimestamp)

            val calendar = Calendar.getInstance().apply { timeInMillis = currentMonthTimestamp }
            val maxDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            val calendarDays = ArrayList<CalendarDayCell?>()

            // Hitung padding awal (Jika Minggu=1, padding=0. Jika Senin=2, padding=1, dst.)
            // Kita harus menyesuaikan firstDayOfWeek dengan standar 0-indexed untuk padding
            val paddingStart = firstDayOfWeek - 1

            // Tambahkan padding (null cells)
            for (i in 0 until paddingStart) {
                calendarDays.add(null)
            }

            // Tambahkan hari-hari di bulan ini
            for (day in 1..maxDayOfMonth) {
                val summary = dailySummaryMap[day]
                calendarDays.add(CalendarDayCell(day, summary))
            }

            // Update adapter
            calendarAdapter?.submitList(calendarDays)
        }

        // Observe Total Weeks untuk menyesuaikan tinggi RecyclerView
        viewModel.totalWeeks.observe(viewLifecycleOwner) { weeks ->
            // Mengatur tinggi RecyclerView secara dinamis untuk menghindari scrolling di dalam ScrollView utama
            val singleItemHeight = 70 // ASUMSI: Tinggi satu item baris (misalnya 70dp)
            val heightInPixels = (weeks * singleItemHeight) * resources.displayMetrics.density

            val layoutParams = rvCalendarGrid.layoutParams
            layoutParams.height = heightInPixels.toInt()
            rvCalendarGrid.layoutParams = layoutParams
        }
    }
}