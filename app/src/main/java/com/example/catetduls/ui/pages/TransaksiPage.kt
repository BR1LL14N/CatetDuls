package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.util.Locale
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import kotlinx.coroutines.flow.first
import com.example.catetduls.data.TransactionType
import com.example.catetduls.data.getWalletRepository
import com.example.catetduls.data.Category
import com.example.catetduls.data.Wallet
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
import android.widget.LinearLayout
import android.widget.Toast
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.ui.adapter.TransactionAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import java.util.Calendar

class TransaksiPage : Fragment() {

    private lateinit var viewModel: TransaksiViewModel
    private lateinit var transactionAdapter: TransactionAdapter

    private var categoryMap: Map<Int, Category> = emptyMap()
    private var walletMap: Map<Int, Wallet> = emptyMap()
    // Views
    private lateinit var rvTransactions: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tabLayout: TabLayout

    // Header Views
    private lateinit var tvCurrentDate: TextView
    private lateinit var btnPrevDate: ImageView
    private lateinit var btnNextDate: ImageView
    private lateinit var btnSearchToggle: ImageView
    private lateinit var btnFilterToggle: ImageView // (Opsional, logika filter lanjut)

    // Summary Views
    private lateinit var tvTotalPemasukan: TextView
    private lateinit var tvTotalPengeluaran: TextView
    private lateinit var tvGrandTotal: TextView

    // FAB
    private lateinit var fabAdd: FloatingActionButton

    // State Lokal untuk Navigasi Tanggal
    private var currentCalendar: Calendar = Calendar.getInstance()
    private var currentTabMode = 2 // Default: 2 = Bulanan (Index Tab)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        loadReferenceData()
        setupListeners()

        // Default: Set Tab "Bulanan" (Index 2)
        tabLayout.getTabAt(2)?.select()
        updateDateFilter() // Trigger load awal

        observeData()
    }

    private fun initViews(view: View) {
        rvTransactions = view.findViewById(R.id.rv_transactions)
        searchView = view.findViewById(R.id.search_view)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        tabLayout = view.findViewById(R.id.tab_layout)

        tvCurrentDate = view.findViewById(R.id.tv_current_date)
        btnPrevDate = view.findViewById(R.id.btn_prev_date)
        btnNextDate = view.findViewById(R.id.btn_next_date)
        btnSearchToggle = view.findViewById(R.id.btn_search_toggle)
        btnFilterToggle = view.findViewById(R.id.btn_filter_toggle)

        tvTotalPemasukan = view.findViewById(R.id.tv_total_pemasukan)
        tvTotalPengeluaran = view.findViewById(R.id.tv_total_pengeluaran)
        tvGrandTotal = view.findViewById(R.id.tv_grand_total)

        fabAdd = view.findViewById(R.id.fab_add_transaction)
    }

    private fun setupRecyclerView() {
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())


        // SETUP ADAPTER DENGAN MAP LOOKUP
        transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                // Handle klik item

                val editFragment = TambahTransaksiPage.newInstance(transaction.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, editFragment) // Pastikan ID container sesuai Activity utama Anda
                    .addToBackStack(null) // Agar bisa di-back
                    .commit()

                Toast.makeText(requireContext(), "Catatan: ${transaction.notes}", Toast.LENGTH_SHORT).show()
            },

            // Ambil Nama Kategori dari Map
            getCategoryName = { id ->
                categoryMap[id]?.name ?: "Loading..."
            },

            // Ambil Ikon Kategori dari Map
            getCategoryIcon = { id ->
                categoryMap[id]?.icon ?: "⏳"
            },

            // Ambil Nama Dompet dari Map
            getWalletName = { id ->
                walletMap[id]?.name ?: "Dompet"
            }
        )
        rvTransactions.adapter = transactionAdapter
    }

    private fun setupListeners() {
        // 1. Tab Layout Listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Harian
                        currentTabMode = 0
                        updateDateFilter()
                    }
                    1 -> { // Kalender
                        if (activity is NavigationCallback) {
                            (activity as NavigationCallback).navigateTo(CalendarPage())
                        }
                        // Kembalikan seleksi ke tab sebelumnya agar visual tetap konsisten
                        // atau biarkan, tergantung flow
                    }
                    2 -> { // Bulanan
                        currentTabMode = 2
                        updateDateFilter()
                    }
                    3 -> Toast.makeText(requireContext(), "Tutup Buku: Segera Hadir", Toast.LENGTH_SHORT).show()
                    4 -> Toast.makeText(requireContext(), "Memo: Segera Hadir", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 2. Navigasi Tanggal (< >)
        btnPrevDate.setOnClickListener { navigateDate(-1) }
        btnNextDate.setOnClickListener { navigateDate(1) }

        // 3. Search Toggle
        btnSearchToggle.setOnClickListener {
            if (searchView.visibility == View.VISIBLE) {
                searchView.visibility = View.GONE
                viewModel.searchTransactions("") // Clear search
            } else {
                searchView.visibility = View.VISIBLE
                searchView.isIconified = false
            }
        }

        // 4. Search View Logic
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchTransactions(query ?: "")
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchTransactions(newText ?: "")
                return true
            }
        })

        // 5. FAB (Tambah Transaksi)
        fabAdd.setOnClickListener {
            if (activity is NavigationCallback) {
                (activity as NavigationCallback).navigateTo(TambahTransaksiPage())
            }
        }
    }

    private fun loadReferenceData() {
        val categoryRepo = requireContext().getCategoryRepository()
        val walletRepo = requireContext().getWalletRepository()

        // Ambil ID buku aktif (default 1)
        val activeBookId = requireContext()
            .getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
            .getInt("active_book_id", 1)

        viewLifecycleOwner.lifecycleScope.launch {
            // 1. Load Categories
            launch {
                categoryRepo.getAllCategories().collect { categories ->
                    // Ubah List menjadi Map (Key: ID, Value: Object Category)
                    categoryMap = categories.associateBy { it.id }

                    // Refresh tampilan jika data transaksi sudah ada
                    if (transactionAdapter.currentList.isNotEmpty()) {
                        transactionAdapter.notifyDataSetChanged()
                    }
                }
            }

            // 2. Load Wallets
            launch {
                walletRepo.getWalletsByBook(activeBookId).collect { wallets ->
                    walletMap = wallets.associateBy { it.id }

                    // Refresh tampilan
                    if (transactionAdapter.currentList.isNotEmpty()) {
                        transactionAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    /**
     * Mengubah tanggal state (+1 atau -1)
     */
    private fun navigateDate(offset: Int) {
        if (currentTabMode == 0) {
            // Mode Harian: Geser Hari
            currentCalendar.add(Calendar.DAY_OF_YEAR, offset)
        } else if (currentTabMode == 2) {
            // Mode Bulanan: Geser Bulan
            currentCalendar.add(Calendar.MONTH, offset)
        }
        updateDateFilter()
    }

    /**
     * Menerapkan filter ke ViewModel berdasarkan Tab & Tanggal aktif
     */
    private fun updateDateFilter() {
        // Reset waktu ke awal/akhir
        val start: Long
        val end: Long

        if (currentTabMode == 0) { // HARIAN
            // Header Text: "28 Nov 2025"
            val dayFormat = java.text.SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            tvCurrentDate.text = dayFormat.format(currentCalendar.time)

            // Logic Filter: 00:00 - 23:59
            val temp = currentCalendar.clone() as Calendar
            temp.set(Calendar.HOUR_OF_DAY, 0); temp.set(Calendar.MINUTE, 0); temp.set(Calendar.SECOND, 0)
            start = temp.timeInMillis

            temp.set(Calendar.HOUR_OF_DAY, 23); temp.set(Calendar.MINUTE, 59); temp.set(Calendar.SECOND, 59)
            end = temp.timeInMillis

        } else { // BULANAN (Default)
            // Header Text: "Nov 2025"
            val monthFormat = java.text.SimpleDateFormat("MMM yyyy", Locale("id", "ID"))
            tvCurrentDate.text = monthFormat.format(currentCalendar.time)

            // Logic Filter: Tgl 1 - Akhir Bulan
            val temp = currentCalendar.clone() as Calendar
            temp.set(Calendar.DAY_OF_MONTH, 1)
            temp.set(Calendar.HOUR_OF_DAY, 0); temp.set(Calendar.MINUTE, 0); temp.set(Calendar.SECOND, 0)
            start = temp.timeInMillis

            temp.set(Calendar.DAY_OF_MONTH, temp.getActualMaximum(Calendar.DAY_OF_MONTH))
            temp.set(Calendar.HOUR_OF_DAY, 23); temp.set(Calendar.MINUTE, 59); temp.set(Calendar.SECOND, 59)
            end = temp.timeInMillis
        }

        viewModel.setDateRangeFilter(start, end)
    }

    private fun observeData() {
        // PERBAIKAN: Gunakan groupedTransactions, bukan transactions biasa
        viewModel.groupedTransactions.observe(viewLifecycleOwner) { listItems ->

            // Kirim data yang sudah ada Headernya ke adapter
            transactionAdapter.submitList(listItems)

            // Handle Empty State (Cek apakah list kosong)
            if (listItems.isEmpty()) {
                layoutEmptyState.visibility = View.VISIBLE
                rvTransactions.visibility = View.GONE
            } else {
                layoutEmptyState.visibility = View.GONE
                rvTransactions.visibility = View.VISIBLE
            }
        }

        // Summary Data (Tetap sama)
        viewModel.displayedTotalIncome.observe(viewLifecycleOwner) { income ->
            tvTotalPemasukan.text = String.format("%,.0f", income)
            calculateGrandTotal()
        }

        viewModel.displayedTotalExpense.observe(viewLifecycleOwner) { expense ->
            tvTotalPengeluaran.text = String.format("%,.0f", expense)
            calculateGrandTotal()
        }
    }

    private fun calculateGrandTotal() {
        val incomeStr = tvTotalPemasukan.text.toString().replace(",", "").replace(".", "")
        val expenseStr = tvTotalPengeluaran.text.toString().replace(",", "").replace(".", "")

        val income = incomeStr.toDoubleOrNull() ?: 0.0
        val expense = expenseStr.toDoubleOrNull() ?: 0.0
        val total = income - expense

        tvGrandTotal.text = String.format("%,.0f", total)
    }
}