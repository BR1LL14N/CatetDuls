package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.viewmodel.TransaksiViewModel
import com.example.catetduls.viewmodel.TransaksiViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.Lifecycle
import com.google.android.material.snackbar.Snackbar

/**
 * TransaksiPage - Halaman daftar semua transaksi
 *
 * Fitur:
 * - Daftar transaksi
 * - Filter (Semua, Pemasukan, Pengeluaran)
 * - Search
 * - Quick filters (Hari Ini, Minggu Ini, Bulan Ini)
 * - Swipe to delete
 */
class TransaksiPage : Fragment() {

    private lateinit var viewModel: TransaksiViewModel

    // Views
    private lateinit var rvTransactions: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipSemua: Chip
    private lateinit var chipPemasukan: Chip
    private lateinit var chipPengeluaran: Chip
    private lateinit var tvTotalPemasukan: TextView
    private lateinit var tvTotalPengeluaran: TextView
    private lateinit var btnFilterHariIni: MaterialButton
    private lateinit var btnFilterMingguIni: MaterialButton
    private lateinit var btnFilterBulanIni: MaterialButton
    private lateinit var btnClearFilter: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        val repository = requireContext().getTransactionRepository()
        val factory = TransaksiViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TransaksiViewModel::class.java]

        // Initialize Views
        initViews(view)

        // Setup
        setupRecyclerView()
        setupChips()
        setupSearchView()
        setupQuickFilters()

        // Observe data
        observeData()
    }

    private fun initViews(view: View) {
        rvTransactions = view.findViewById(R.id.rv_transactions)
        searchView = view.findViewById(R.id.search_view)
        chipGroup = view.findViewById(R.id.chip_group_type)
        chipSemua = view.findViewById(R.id.chip_semua)
        chipPemasukan = view.findViewById(R.id.chip_pemasukan)
        chipPengeluaran = view.findViewById(R.id.chip_pengeluaran)
        tvTotalPemasukan = view.findViewById(R.id.tv_total_pemasukan)
        tvTotalPengeluaran = view.findViewById(R.id.tv_total_pengeluaran)
        btnFilterHariIni = view.findViewById(R.id.btn_filter_hari_ini)
        btnFilterMingguIni = view.findViewById(R.id.btn_filter_minggu_ini)
        btnFilterBulanIni = view.findViewById(R.id.btn_filter_bulan_ini)
        btnClearFilter = view.findViewById(R.id.btn_clear_filter)
    }

    private fun setupRecyclerView() {
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Setup adapter dengan swipe to delete
    }

    private fun setupChips() {
        chipSemua.setOnClickListener {
            viewModel.setTypeFilter(null)
        }

        chipPemasukan.setOnClickListener {
            viewModel.setTypeFilter("Pemasukan")
        }

        chipPengeluaran.setOnClickListener {
            viewModel.setTypeFilter("Pengeluaran")
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchTransactions(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    viewModel.searchTransactions("")
                }
                return true
            }
        })
    }

    private fun setupQuickFilters() {
        btnFilterHariIni.setOnClickListener {
            viewModel.filterToday()
        }

        btnFilterMingguIni.setOnClickListener {
            viewModel.filterThisWeek()
        }

        btnFilterBulanIni.setOnClickListener {
            viewModel.filterThisMonth()
        }

        btnClearFilter.setOnClickListener {
            viewModel.clearFilters()
            searchView.setQuery("", false)
            chipSemua.isChecked = true
        }
    }

    private fun observeData() {

        // 1. OBSERVE UNTUK LIVE DATA (Ini sudah benar)
        // =============================================

        // Observe list transaksi
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            // TODO: Update RecyclerView adapter
        }

        // Observe total pemasukan yang ditampilkan
        viewModel.displayedTotalIncome.observe(viewLifecycleOwner) { total ->
            tvTotalPemasukan.text = viewModel.formatCurrency(total)
        }

        // Observe total pengeluaran yang ditampilkan
        viewModel.displayedTotalExpense.observe(viewLifecycleOwner) { total ->
            tvTotalPengeluaran.text = viewModel.formatCurrency(total)
        }

        // 2. OBSERVE UNTUK STATEFLOW (Ini perbaikannya)
        // ===============================================

        // Jalankan di dalam lifecycleScope
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe error messages
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
                        // viewModel.clearMessages() // Aktifkan jika ada
                    }
                }

                // Anda mungkin punya StateFlow lain di sini, seperti:
                // launch { viewModel.isLoading.collect { ... } }
            }
        }
    }
}