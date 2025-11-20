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
import com.example.catetduls.data.TransactionType
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
import android.widget.ImageView // Import ImageView
import android.widget.Toast

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

    // View BARU
    private lateinit var btnToCalendar: ImageView // Tombol Kalender di Header

    // TODO: Definisikan adapter Anda di sini
    // private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = requireContext().getTransactionRepository()
        val factory = TransaksiViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TransaksiViewModel::class.java]

        initViews(view)

        setupRecyclerView()
        setupChips()
        setupSearchView()
        setupQuickFilters()
        setupHeaderButtons() // Setup Tombol Kalender

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
            ?: throw IllegalStateException("btn_filter_hari_ini not found in layout")
        btnFilterMingguIni = view.findViewById(R.id.btn_filter_minggu_ini)
            ?: throw IllegalStateException("btn_filter_minggu_ini not found in layout")
        btnFilterBulanIni = view.findViewById(R.id.btn_filter_bulan_ini)
            ?: throw IllegalStateException("btn_filter_bulan_ini not found in layout")

        // btnClearFilter sekarang ada di dalam Summary Card
        btnClearFilter = view.findViewById(R.id.btn_clear_filter)
            ?: throw IllegalStateException("btn_clear_filter not found in layout")

        // Inisialisasi Tombol Kalender
        btnToCalendar = view.findViewById(R.id.btn_to_calendar)
            ?: throw IllegalStateException("btn_to_calendar not found in layout")
    }

    private fun setupHeaderButtons() {
        btnToCalendar.setOnClickListener {

            // Periksa apakah Activity mengimplementasikan NavigationCallback
            if (activity is NavigationCallback) {
                // Panggil navigateTo dan berikan instance CalendarPage
                (activity as NavigationCallback).navigateTo(CalendarPage())
            } else {
                // Fallback jika interface tidak diimplementasikan
                Toast.makeText(requireContext(), "Error: Navigasi Kalender tidak tersedia.", Toast.LENGTH_LONG).show()
            }

            // Hapus Toast lama karena navigasi sekarang berfungsi
            // Toast.makeText(requireContext(), "Pindah ke Tampilan Kalender", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupRecyclerView() {
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        // TODO: Inisialisasi dan set adapter Anda di sini
    }

    private fun setupChips() {
        chipSemua.setOnClickListener {
            viewModel.setTypeFilter(null)
        }

        chipPemasukan.setOnClickListener {
            viewModel.setTypeFilter(TransactionType.PEMASUKAN)
        }

        chipPengeluaran.setOnClickListener {
            viewModel.setTypeFilter(TransactionType.PENGELUARAN)
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
        }
    }

    private fun observeData() {

        // 1. OBSERVE UNTUK LIVE DATA
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            // TODO: Update RecyclerView adapter
        }

        viewModel.displayedTotalIncome.observe(viewLifecycleOwner) { total ->
            tvTotalPemasukan.text = viewModel.formatCurrency(total)
        }

        viewModel.displayedTotalExpense.observe(viewLifecycleOwner) { total ->
            tvTotalPengeluaran.text = viewModel.formatCurrency(total)
        }

        // 2. OBSERVE UNTUK STATEFLOW
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }

                launch {
                    viewModel.selectedType.collect { type ->
                        when (type) {
                            TransactionType.PEMASUKAN -> chipPemasukan.isChecked = true
                            TransactionType.PENGELUARAN -> chipPengeluaran.isChecked = true
                            null -> chipSemua.isChecked = true
                        }
                    }
                }

                launch {
                    viewModel.searchQuery.collect { query ->
                        // Ini akan mencegah loop tak terhingga jika ViewModel mereset query ke ""
                        if (query.isEmpty() && searchView.query.isNotEmpty()) {
                            searchView.setQuery("", false)
                        }
                    }
                }
            }
        }
    }
}