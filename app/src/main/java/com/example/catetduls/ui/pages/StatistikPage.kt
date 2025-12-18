package com.example.catetduls.ui.pages

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.CategoryExpense
import com.example.catetduls.data.TransactionType
import com.example.catetduls.data.getBookRepository
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.ui.adapter.CategoryStatAdapter
import com.example.catetduls.viewmodel.StatistikViewModel
import com.example.catetduls.viewmodel.StatistikViewModelFactory
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatistikPage : Fragment() {

    private lateinit var viewModel: StatistikViewModel
    private lateinit var categoryAdapter: CategoryStatAdapter

    // Views
    private lateinit var spinnerPeriod: Spinner
    private lateinit var tabLayout: TabLayout
    private lateinit var pieChart: PieChart
    private lateinit var rvCategoryDetails: RecyclerView
    private lateinit var tvCurrentMonth: TextView
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView

    // Warna Chart & List (Pastikan urutan sama dengan di Adapter)
    private val chartColors =
            listOf(
                    "#FF6384",
                    "#36A2EB",
                    "#FFCE56",
                    "#4BC0C0",
                    "#9966FF",
                    "#FF9F40",
                    "#33FFCC",
                    "#C9CBCF"
            )

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistik, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Init ViewModel
        // 1. Init ViewModel
        val transactionRepo = requireContext().getTransactionRepository()
        val categoryRepo = requireContext().getCategoryRepository()
        val bookRepo = requireContext().getBookRepository()
        val factory = StatistikViewModelFactory(transactionRepo, categoryRepo, bookRepo)
        viewModel = ViewModelProvider(this, factory)[StatistikViewModel::class.java]

        // 2. Init Views
        initViews(view)

        // 3. Setup Components
        setupSpinner()
        setupTabLayout()
        setupChart()
        setupRecyclerView()
        setupHeaderDate() // Set teks bulan saat ini

        // 4. Observe Data
        observeData()
    }

    private fun initViews(view: View) {
        spinnerPeriod = view.findViewById(R.id.spinner_period)
        tabLayout = view.findViewById(R.id.tab_layout_type)
        pieChart = view.findViewById(R.id.pie_chart)
        rvCategoryDetails = view.findViewById(R.id.rv_category_details)

        tvCurrentMonth = view.findViewById(R.id.tv_current_month)
        btnPrevMonth = view.findViewById(R.id.btn_prev_month)
        btnNextMonth = view.findViewById(R.id.btn_next_month)
    }

    private fun setupHeaderDate() {
        // Menampilkan Bulan & Tahun saat ini (Hanya visual header)
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("MMM yyyy", Locale("id", "ID"))
        tvCurrentMonth.text = format.format(calendar.time)

        // TODO: Jika ingin fitur navigasi bulan (< >) berfungsi mengubah data,
        // ViewModel harus diupdate untuk mendukung pergeseran bulan (addMonth).
        // Untuk saat ini, kita biarkan statis sesuai periode "Bulan Ini" di ViewModel.
        btnPrevMonth.alpha = 0.3f // Dimmed agar user tahu belum aktif
        btnNextMonth.alpha = 0.3f
    }

    private fun setupSpinner() {
        // Opsi Periode
        val periods = listOf("Harian", "Mingguan", "Bulanan", "Tahunan")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriod.adapter = adapter

        // Set default ke "Bulanan" (index 2)
        spinnerPeriod.setSelection(2)

        spinnerPeriod.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        val selected = periods[position]
                        // Mapping string UI ke string yang dimengerti ViewModel
                        val periodKey =
                                when (selected) {
                                    "Harian" -> "Hari Ini"
                                    "Mingguan" -> "Minggu Ini"
                                    "Bulanan" -> "Bulan Ini"
                                    "Tahunan" -> "Tahun Ini"
                                    else -> "Bulan Ini"
                                }
                        viewModel.setPeriod(periodKey)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
    }

    private fun setupTabLayout() {
        // Tab 0: Pendapatan, Tab 1: Pengeluaran (Default)
        val tabPengeluaran = tabLayout.getTabAt(1)
        tabPengeluaran?.select()

        tabLayout.addOnTabSelectedListener(
                object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        when (tab?.position) {
                            0 -> viewModel.setChartType(TransactionType.PEMASUKAN)
                            1 -> viewModel.setChartType(TransactionType.PENGELUARAN)
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                }
        )
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryStatAdapter(onItemClick = { categoryExpense ->
            val detailFragment = DetailStatistikPage.newInstance(categoryExpense.categoryId)

            parentFragmentManager
                    .beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right
                    )
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
        })

        // 2. BAGIAN PENTING YANG HILANG: Pasang ke RecyclerView UI
        rvCategoryDetails.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter // <--- INI WAJIB ADA
            isNestedScrollingEnabled = false // Agar scrolling lancar di dalam ScrollView
        }
    }

    private fun setupChart() {
        pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false // Legenda kita buat sendiri di RecyclerView

            // Konfigurasi Donut Chart
            isDrawHoleEnabled = false
            // --- KONFIGURASI LABEL & ROTASI ---
            setUsePercentValues(true) // Gunakan nilai persen
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(11f)

            // Offset ekstra agar label di luar tidak terpotong layar
            setExtraOffsets(45f, 20f, 45f, 20f)
            animateY(1000)
        }
    }

    private fun observeData() {
        // Observe Data Utama (Chart & List)
        viewModel.categoryStats.observe(viewLifecycleOwner) { categories ->

            // 1. Update Chart
            updateChartData(categories)

            // 2. Update RecyclerView
            categoryAdapter.submitList(categories)

            // 3. Update Center Text (Total di tengah Donut)
            val total = categories.sumOf { it.total }
            // FIX: Gunakan CurrencyHelper.format langsung karena 'total' sudah dalam mata uang asing (hasil convert di ViewModel)
            // Jangan pakai viewModel.formatCurrency() lagi karena itu akan mengkonversi ulang.
            pieChart.centerText = "Total\n${com.example.catetduls.utils.CurrencyHelper.format(total, viewModel.currencySymbol.value)}"
            pieChart.setCenterTextSize(14f)
            pieChart.setCenterTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_primary)
            )
        }

        // Observe Header Total (Opsional, untuk update judul Tab)
        viewModel.periodIncome.asLiveData().observe(viewLifecycleOwner) { income ->
            tabLayout.getTabAt(0)?.text = "Pendapatan\n${viewModel.formatCurrency(income)}"
        }

        viewModel.periodExpense.asLiveData().observe(viewLifecycleOwner) { expense ->
            tabLayout.getTabAt(1)?.text = "Pengeluaran\n${viewModel.formatCurrency(expense)}"
        }

        // 4. Currency (New)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.currencySymbol.collect { symbol ->
                categoryAdapter.setCurrency(symbol)
                // Refresh list if needed (though submitList triggers update, but bind uses symbol)
                // Adapter.notifyDataSetChanged should be called in setCurrency
            }
        }
    }

    private fun updateChartData(categories: List<CategoryExpense>) {
        if (categories.isEmpty()) {
            pieChart.data = null
            pieChart.setNoDataText("Belum ada data")
            pieChart.invalidate()
            return
        }

        val totalAmount = categories.sumOf { it.total }

        val entries =
                categories.map {
                    val percent = (it.total / totalAmount) * 100
                    // Format Label: "Ikon Nama" (Baris 1)
                    // Persen akan otomatis dihandle formatter
                    PieEntry(it.total.toFloat(), "${it.icon} ${it.categoryName}")
                }

        val dataSet =
                PieDataSet(entries, "").apply {
                    sliceSpace = 2f
                    selectionShift = 5f
                    colors = chartColors.map { Color.parseColor(it) }

                    // --- KONFIGURASI VALUE LINES (GARIS PETUNJUK) ---

                    // 1. Posisi Label di Luar Chart
                    yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                    xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

                    // 2. Gaya Garis
                    valueLinePart1OffsetPercentage = 99f // Jarak awal garis dari pusat
                    valueLinePart1Length = 1.1f // Panjang garis bagian dalam
                    valueLinePart2Length = 0.2f // Panjang garis bagian luar (horizontal)
                    valueLineColor = Color.DKGRAY // Warna garis
                    valueLineWidth = 1.3f

                    // 3. Warna Teks Label
                    valueTextColor = Color.BLACK
                    valueTextSize = 11f
                }

        val data =
                PieData(dataSet).apply {
                    // Formatter untuk menampilkan "XX.X %"
                    setValueFormatter(PercentFormatter(pieChart))
                }

        pieChart.data = data
        pieChart.highlightValues(null) // Hapus highlight saat init
        pieChart.invalidate() // Refresh chart
        //        pieChart.animateY(800)
    }
}
