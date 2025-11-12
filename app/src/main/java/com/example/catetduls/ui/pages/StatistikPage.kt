package com.example.catetduls.ui.pages

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
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.collect
import com.example.catetduls.data.CategoryWithCount

/**
 * StatistikPage - Halaman grafik dan analisis
 *
 * Fitur:
 * - Pie Chart (pengeluaran per kategori)
 * - Bar Chart (pemasukan vs pengeluaran per bulan)
 * - Filter periode (Hari Ini, Minggu Ini, Bulan Ini, Tahun Ini)
 * - Top kategori pengeluaran
 * - Summary (surplus/defisit)
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
    // TODO: Add chart views (PieChart, BarChart)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistik, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        val repository = requireContext().getTransactionRepository()
        val factory = StatistikViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StatistikViewModel::class.java]

        // Initialize Views
        initViews(view)

        // Setup
        setupChips()

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
    }

    private fun setupChips() {
        chipHariIni.setOnClickListener {
            viewModel.setPeriod("Hari Ini")
        }

        chipMingguIni.setOnClickListener {
            viewModel.setPeriod("Minggu Ini")
        }

        chipBulanIni.setOnClickListener {
            viewModel.setPeriod("Bulan Ini")
        }

        chipTahunIni.setOnClickListener {
            viewModel.setPeriod("Tahun Ini")
        }

        // Set default
        chipBulanIni.isChecked = true
    }

    private fun observeData() {

        // 1. OBSERVE UNTUK STATEFLOW (Sudah Benar)
        // ===========================================
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe total pemasukan periode
                launch {
                    viewModel.periodIncome.collect { income: Double? ->
                        tvPeriodIncome.text = "Pemasukan: ${viewModel.formatCurrency(income)}"
                    }
                }

                // Observe total pengeluaran periode
                launch {
                    viewModel.periodExpense.collect { expense: Double? ->
                        tvPeriodExpense.text = "Pengeluaran: ${viewModel.formatCurrency(expense)}"
                    }
                }

                // Observe balance periode
                launch {
                    viewModel.periodBalance.collect { balance: Double? ->
                        tvPeriodBalance.text = viewModel.formatCurrency(balance)

                        val color = if (balance != null && balance >= 0) {
                            android.graphics.Color.parseColor("#4CAF50") // Hijau
                        } else {
                            android.graphics.Color.parseColor("#F44336") // Merah
                        }
                        tvPeriodBalance.setTextColor(color)

                        tvSummary.text = viewModel.getPeriodSummaryText()
                    }
                }
            }
        }

        // 2. OBSERVE UNTUK LIVE DATA (Ini Perbaikannya)
        // ===========================================

        // Observe top kategori
        // Ganti .collect menjadi .observe dan perbaiki tipe datanya
        viewModel.topExpenseCategory.observe(viewLifecycleOwner) { category -> // Tipe datanya adalah CategoryExpense?
            if (category != null) {
                // Asumsi: data class CategoryExpense punya properti 'categoryName' dan 'total'
                // (Berdasarkan file StatistikViewModel.kt Anda)
                tvTopCategory.text = "Top Pengeluaran: ${category.categoryName} (${viewModel.formatCurrency(category.total)})"
            } else {
                tvTopCategory.text = "Belum ada data"
            }
        }

        // Observe data untuk Pie Chart
        // Ganti .collect menjadi .observe dan perbaiki tipe datanya
        viewModel.expenseByCategory.observe(viewLifecycleOwner) { categories -> // Tipe datanya adalah List<CategoryExpense>
            // TODO: Update Pie Chart dengan data 'categories'
            // 'categories' adalah List<CategoryExpense>
        }

        // Observe data untuk Bar Chart
        // Ganti .collect menjadi .observe dan perbaiki tipe datanya
        viewModel.monthlyTotals.observe(viewLifecycleOwner) { monthlyData -> // Tipe datanya adalah List<MonthlyTotal>
            // TODO: Update Bar Chart dengan data 'monthlyData'
        }
    }
}