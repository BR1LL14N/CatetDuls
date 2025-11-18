package com.example.catetduls.ui.pages

import android.graphics.Color // Import untuk warna chart
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.catetduls.R
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.viewmodel.StatistikViewModel
import com.example.catetduls.viewmodel.StatistikViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import com.example.catetduls.data.CategoryExpense
import com.example.catetduls.data.MonthlyTotal // Import data class

// Import MPAndroidChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis

/**
 * StatistikPage - Halaman grafik dan analisis
 */
class StatistikPage : Fragment() {

    private lateinit var viewModel: StatistikViewModel

    // Views
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipHariIni: Chip
    private lateinit var chipMingguIni: Chip
    private lateinit var chipBulanIni: Chip
    private lateinit var chipTahunIni: Chip
    private lateinit var tvPeriodIncome: TextView
    private lateinit var tvPeriodExpense: TextView
    private lateinit var tvPeriodBalance: TextView
    private lateinit var tvTopCategory: TextView
    private lateinit var tvSummary: TextView
    private lateinit var pieChartExpense: PieChart // <-- Pie Chart View
    private lateinit var barChartMonthly: BarChart // <-- Bar Chart View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistik, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel (Menggunakan Factory yang sudah Anda definisikan)
        val repository = requireContext().getTransactionRepository()
        val factory = StatistikViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StatistikViewModel::class.java]

        // Initialize Views
        initViews(view)

        // Setup Charts dan Chips
        setupChips()
        setupPieChart()
        setupBarChart()

        // Observe data
        observeData()
    }

    private fun initViews(view: View) {
        chipGroup = view.findViewById(R.id.chip_group_period)
        chipHariIni = view.findViewById(R.id.chip_hari_ini)
        chipMingguIni = view.findViewById(R.id.chip_minggu_ini)
        chipBulanIni = view.findViewById(R.id.chip_bulan_ini)
        chipTahunIni = view.findViewById(R.id.chip_tahun_ini)
        tvPeriodIncome = view.findViewById(R.id.tv_period_income)
        tvPeriodExpense = view.findViewById(R.id.tv_period_expense)
        tvPeriodBalance = view.findViewById(R.id.tv_period_balance)
        tvTopCategory = view.findViewById(R.id.tv_top_category)
        tvSummary = view.findViewById(R.id.tv_summary)
        pieChartExpense = view.findViewById(R.id.pie_chart_expense) // <-- Hubungkan Pie Chart
        barChartMonthly = view.findViewById(R.id.bar_chart_monthly) // <-- Hubungkan Bar Chart
    }

    private fun setupChips() {
        chipHariIni.setOnClickListener { viewModel.setPeriod("Hari Ini") }
        chipMingguIni.setOnClickListener { viewModel.setPeriod("Minggu Ini") }
        chipBulanIni.setOnClickListener { viewModel.setPeriod("Bulan Ini") }
        chipTahunIni.setOnClickListener { viewModel.setPeriod("Tahun Ini") }

        // Set default
        chipBulanIni.isChecked = true
    }

    // ========================================
    // PIE CHART LOGIC
    // ========================================
    private fun setupPieChart() {
        pieChartExpense.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            transparentCircleRadius = 58f

            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 5f
            }
        }
    }

    private fun updatePieChart(categories: List<CategoryExpense>) {
        if (categories.isEmpty()) {
            pieChartExpense.data = null
            pieChartExpense.setNoDataText("Belum ada data pengeluaran untuk periode ini.")
            pieChartExpense.invalidate()
            return
        }

        val entries = categories.map { category ->
            PieEntry(category.total.toFloat(), category.categoryName)
        }

        val dataSet = PieDataSet(entries, "Kategori Pengeluaran").apply {
            sliceSpace = 2f
            selectionShift = 5f

            val colors = ArrayList<Int>()
            for (i in categories.indices) {
                colors.add(viewModel.getCategoryColor(i))
            }
            this.colors = colors
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChartExpense))
            setValueTextSize(11f)
            setValueTextColor(Color.BLACK)
        }

        pieChartExpense.data = data
        pieChartExpense.highlightValues(null)
        pieChartExpense.animateY(1000)
        pieChartExpense.invalidate()
    }

    // ========================================
    // BAR CHART LOGIC
    // ========================================
    private fun setupBarChart() {
        barChartMonthly.apply {
            description.isEnabled = false
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawValueAboveBar(true)

            // X-Axis (Bulan)
            xAxis.apply {
                granularity = 1f
                isGranularityEnabled = true
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                labelCount = 12
            }

            // Y-Axis Kiri
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }

            // Y-Axis Kanan (dinonaktifkan)
            axisRight.isEnabled = false

            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
        }
    }

    private fun updateBarChart(monthlyData: List<MonthlyTotal>) {
        // Data transaksi seringkali tidak lengkap (tidak ada data untuk bulan yang kosong)
        // Kita perlu menyiapkan data untuk 12 bulan

        val incomeEntries = ArrayList<BarEntry>()
        val expenseEntries = ArrayList<BarEntry>()
        val monthLabels = ArrayList<String>()

        for (i in 1..12) {
            monthLabels.add(viewModel.getMonthName(i))
            val data = monthlyData.find { it.month == i }

            // Masukkan data untuk 12 bulan. Jika bulan kosong, nilai 0.0
            incomeEntries.add(BarEntry(i.toFloat(), (data?.income ?: 0.0).toFloat()))
            expenseEntries.add(BarEntry(i.toFloat(), (data?.expense ?: 0.0).toFloat()))
        }

        if (incomeEntries.all { it.y == 0f } && expenseEntries.all { it.y == 0f }) {
            barChartMonthly.data = null
            barChartMonthly.setNoDataText("Belum ada data transaksi tahun ini.")
            barChartMonthly.invalidate()
            return
        }

        // 1. Buat BarDataSet
        val incomeSet = BarDataSet(incomeEntries, "Pemasukan").apply {
            color = Color.parseColor("#4CAF50")
            valueTextSize = 10f
        }
        val expenseSet = BarDataSet(expenseEntries, "Pengeluaran").apply {
            color = Color.parseColor("#F44336")
            valueTextSize = 10f
        }

        val data = BarData(incomeSet, expenseSet)

        // 2. Kelompokkan bar (Grouping)
        val groupSpace = 0.3f
        val barSpace = 0.025f
        val barWidth = 0.325f // Total = 1.0 (0.325 * 2 + 0.025 * 2 + 0.3)
        data.barWidth = barWidth

        // 3. Masukkan data ke chart dan atur X-Axis
        barChartMonthly.data = data
        barChartMonthly.xAxis.valueFormatter = IndexAxisValueFormatter(monthLabels)
        barChartMonthly.xAxis.axisMinimum = 1f - data.barWidth / 2f
        barChartMonthly.xAxis.axisMaximum = 12f + data.barWidth / 2f

        // Mulai grouping dari posisi 1f (bulan 1)
        barChartMonthly.groupBars(1f, groupSpace, barSpace)
        barChartMonthly.animateY(1000)
        barChartMonthly.invalidate()
    }


    // ========================================
    // OBSERVATION
    // ========================================
    private fun observeData() {

        // 1. OBSERVE UNTUK STATEFLOW (Ringkasan Periode)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.periodIncome.collect { income: Double? ->
                        tvPeriodIncome.text = "Pemasukan: ${viewModel.formatCurrency(income)}"
                    }
                }
                launch {
                    viewModel.periodExpense.collect { expense: Double? ->
                        tvPeriodExpense.text = "Pengeluaran: ${viewModel.formatCurrency(expense)}"
                    }
                }
                launch {
                    viewModel.periodBalance.collect { balance: Double? ->
                        tvPeriodBalance.text = viewModel.formatCurrency(balance)

                        val color = if (balance != null && balance >= 0) {
                            Color.parseColor("#4CAF50") // Hijau
                        } else {
                            Color.parseColor("#F44336") // Merah
                        }
                        tvPeriodBalance.setTextColor(color)

                        tvSummary.text = viewModel.getPeriodSummaryText()
                    }
                }
            }
        }

        // 2. OBSERVE UNTUK LIVE DATA (Charts & Top Kategori)

        // Observe top kategori
        viewModel.topExpenseCategory.observe(viewLifecycleOwner) { category ->
            if (category != null) {
                tvTopCategory.text = "Top Pengeluaran: ${category.categoryName} (${viewModel.formatCurrency(category.total)})"
            } else {
                tvTopCategory.text = "Belum ada data"
            }
        }

        // Observe data untuk Pie Chart
        viewModel.expenseByCategory.observe(viewLifecycleOwner) { categories ->
            updatePieChart(categories) // <-- UPDATE PIE CHART
        }

        // Observe data untuk Bar Chart
        viewModel.monthlyTotals.observe(viewLifecycleOwner) { monthlyData ->
            updateBarChart(monthlyData) // <-- UPDATE BAR CHART
        }
    }
}