package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView // Import ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Category
import com.example.catetduls.data.Wallet
import com.example.catetduls.data.getBookRepository
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.data.getWalletRepository
import com.example.catetduls.ui.adapter.TransactionAdapter
import com.example.catetduls.viewmodel.TransaksiViewModel
import com.example.catetduls.viewmodel.TransaksiViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import java.util.Calendar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaksi, container, false)
    }

    // State Global
    private var currentCurrencySymbol: String = "Rp"
    private var currentCurrencyCode: String = "IDR"
    private var currentIncome: Double = 0.0
    private var currentExpense: Double = 0.0

    // Child Fragments
    private var tutupBukuFragment: TutupBukuPage? = null
    private var memoFragment: MemoPage? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = requireContext().getTransactionRepository()
        val factory = TransaksiViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TransaksiViewModel::class.java]

        initViews(view)
        setupRecyclerView()
        loadReferenceData()
        setupListeners()
        setupCurrencyObserver()

        // Init child fragments but hide them initially
        initChildFragments()

        // Cek Arguments (dari Calendar Page atau yang lain)
        val args = arguments
        if (args != null && args.containsKey("ARG_INITIAL_DATE")) {
            val initialDate = args.getLong("ARG_INITIAL_DATE")
            val initialTab = args.getInt("ARG_INITIAL_TAB_MODE", 0)

            currentCalendar.timeInMillis = initialDate
            currentTabMode = initialTab
        }

        // Force update initial filter to ensure consistency
        updateDateFilter()

        observeData()
    }

    private fun setupCurrencyObserver() {
        val bookRepository = requireContext().getBookRepository()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bookRepository.getActiveBook().collect { book ->
                    if (book != null) {
                        // ✅ PERBAIKAN: Gunakan Elvis Operator (?:) untuk handle null
                        currentCurrencySymbol = book.currencySymbol ?: "Rp"
                        currentCurrencyCode = book.currencyCode ?: "IDR"

                        // Gunakan variabel lokal yang sudah aman (currentCurrencyCode)
                        // Jangan pakai book.currencyCode langsung di sini
                        transactionAdapter.setCurrency(currentCurrencyCode, currentCurrencySymbol)

                        updateSummaryDisplay()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        // 1. Tab Layout Listener
        tabLayout.addOnTabSelectedListener(
                object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        when (tab?.position) {
                            0 -> { // Harian
                                currentTabMode = 0
                                updateViewVisibility()
                                updateDateFilter()
                            }
                            1 -> { // Kalender
                                // Navigate to CalendarPage (Full Screen replacement)
                                if (activity is NavigationCallback) {
                                    (activity as NavigationCallback).navigateTo(
                                            com.example.catetduls.ui.pages.CalendarPage()
                                    )
                                }
                                // Reset tab to monthly or daily if returning? 
                                // Ideally CalendarPage should handle return, but for now this is fine.
                            }
                            2 -> { // Bulanan
                                currentTabMode = 2
                                updateViewVisibility()
                                updateDateFilter()
                            }
                            3 -> { // Tutup Buku
                                currentTabMode = 3
                                updateViewVisibility()
                                updateTutupBukuDate()
                                updateDateText()
                            }
                            4 -> { // Memo
                                currentTabMode = 4
                                updateViewVisibility()
                            }
                        }
                    }
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                }
        )

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
        searchView.setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        viewModel.searchTransactions(query ?: "")
                        return true
                    }
                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.searchTransactions(newText ?: "")
                        return true
                    }
                }
        )

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
        val activeBookId =
                requireContext()
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

    /** Mengubah tanggal state (+1 atau -1) */
    private fun navigateDate(offset: Int) {
        if (currentTabMode == 0) {
            currentCalendar.add(Calendar.DAY_OF_YEAR, offset)
        } else if (currentTabMode == 2 || currentTabMode == 3) {
            // Bulanan OR Tutup Buku
            currentCalendar.add(Calendar.MONTH, offset)
        }
        
        updateDateText()

        if (currentTabMode == 3) {
            updateTutupBukuDate()
        } else if (currentTabMode < 3) {
            updateDateFilter()
        }
    }
    
    private fun updateDateText() {
        if (currentTabMode == 0) {
             val dayFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
             tvCurrentDate.text = dayFormat.format(currentCalendar.time)
        } else {
             val monthFormat = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale("id", "ID"))
             tvCurrentDate.text = monthFormat.format(currentCalendar.time)
        }
    }

    private fun initChildFragments() {
        val fragmentManager = childFragmentManager
        
        tutupBukuFragment = fragmentManager.findFragmentByTag("TUTUP_BUKU") as? TutupBukuPage
        if (tutupBukuFragment == null) {
            tutupBukuFragment = TutupBukuPage()
            fragmentManager.beginTransaction()
                .add(R.id.child_fragment_container, tutupBukuFragment!!, "TUTUP_BUKU")
                .hide(tutupBukuFragment!!)
                .commit()
        }
        
        memoFragment = fragmentManager.findFragmentByTag("MEMO") as? MemoPage
        if (memoFragment == null) {
            memoFragment = MemoPage()
            fragmentManager.beginTransaction()
                .add(R.id.child_fragment_container, memoFragment!!, "MEMO")
                .hide(memoFragment!!)
                .commit()
        }
    }

    private fun updateViewVisibility() {
        val showTransactions = currentTabMode < 3
        val showTutupBuku = currentTabMode == 3
        val showMemo = currentTabMode == 4
        
        // Views
        val transactionContainer = view?.findViewById<View>(R.id.rv_transactions)
        val emptyState = view?.findViewById<View>(R.id.layout_empty_state)
        val childContainer = view?.findViewById<View>(R.id.child_fragment_container)
        val fab = view?.findViewById<FloatingActionButton>(R.id.fab_add_transaction)
        
        if (showTransactions) {
            transactionContainer?.visibility = View.VISIBLE
            // Empty state handled by observer
            if (transactionAdapter.itemCount == 0) {
                 emptyState?.visibility = View.VISIBLE
                 transactionContainer?.visibility = View.GONE
            } else {
                 emptyState?.visibility = View.GONE
            }
            
            childContainer?.visibility = View.GONE
            tutupBukuFragment?.let { if (it.isAdded) childFragmentManager.beginTransaction().hide(it).commit() }
            memoFragment?.let { if (it.isAdded) childFragmentManager.beginTransaction().hide(it).commit() }
            fab?.show()
            
            btnSearchToggle.visibility = View.VISIBLE
        } else {
            transactionContainer?.visibility = View.GONE
            emptyState?.visibility = View.GONE
            childContainer?.visibility = View.VISIBLE
            fab?.hide()
            
            if (showTutupBuku) {
                 tutupBukuFragment?.let { 
                     childFragmentManager.beginTransaction().show(it).commit()
                     memoFragment?.let { m -> if (m.isAdded) childFragmentManager.beginTransaction().hide(m).commit() }
                 }
                 updateTutupBukuDate()
                 btnSearchToggle.visibility = View.GONE
            } else if (showMemo) {
                 memoFragment?.let {
                     childFragmentManager.beginTransaction().show(it).commit()
                     tutupBukuFragment?.let { tb -> if (tb.isAdded) childFragmentManager.beginTransaction().hide(tb).commit() }
                 }
                 btnSearchToggle.visibility = View.GONE
            }
        }
    }
    
    private fun updateTutupBukuDate() {
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH) + 1
        tutupBukuFragment?.updateDate(year, month)
    }

    /** Menerapkan filter ke ViewModel berdasarkan Tab & Tanggal aktif */
    private fun updateDateFilter() {
        // Reset waktu ke awal/akhir
        val start: Long
        val end: Long

        if (currentTabMode == 0) { // HARIAN
            // Header Text: "28 Nov 2025"
            val dayFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
            tvCurrentDate.text = dayFormat.format(currentCalendar.time)

            // Logic Filter: 00:00 - 23:59
            val temp = currentCalendar.clone() as Calendar
            temp.set(Calendar.HOUR_OF_DAY, 0)
            temp.set(Calendar.MINUTE, 0)
            temp.set(Calendar.SECOND, 0)
            start = temp.timeInMillis

            temp.set(Calendar.HOUR_OF_DAY, 23)
            temp.set(Calendar.MINUTE, 59)
            temp.set(Calendar.SECOND, 59)
            end = temp.timeInMillis
        } else { // BULANAN (Default)
            // Header Text: "Nov 2025"
            val monthFormat = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale("id", "ID"))
            tvCurrentDate.text = monthFormat.format(currentCalendar.time)

            // Logic Filter: Tgl 1 - Akhir Bulan
            val temp = currentCalendar.clone() as Calendar
            temp.set(Calendar.DAY_OF_MONTH, 1)
            temp.set(Calendar.HOUR_OF_DAY, 0)
            temp.set(Calendar.MINUTE, 0)
            temp.set(Calendar.SECOND, 0)
            start = temp.timeInMillis

            temp.set(Calendar.DAY_OF_MONTH, temp.getActualMaximum(Calendar.DAY_OF_MONTH))
            temp.set(Calendar.HOUR_OF_DAY, 23)
            temp.set(Calendar.MINUTE, 59)
            temp.set(Calendar.SECOND, 59)
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
            currentIncome = income
            updateSummaryDisplay()
        }

        viewModel.displayedTotalExpense.observe(viewLifecycleOwner) { expense ->
            currentExpense = expense
            updateSummaryDisplay()
        }
    }

    private fun initViews(view: View) {
        rvTransactions = view.findViewById(R.id.rv_transactions)
        searchView = view.findViewById(R.id.search_view)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        tabLayout = view.findViewById(R.id.tab_layout)

        // Header Views
        tvCurrentDate = view.findViewById(R.id.tv_current_date)
        btnPrevDate = view.findViewById(R.id.btn_prev_date)
        btnNextDate = view.findViewById(R.id.btn_next_date)
        btnSearchToggle = view.findViewById(R.id.btn_search_toggle)
        btnFilterToggle = view.findViewById(R.id.btn_filter_toggle)

        // Summary Views
        tvTotalPemasukan = view.findViewById(R.id.tv_total_pemasukan)
        tvTotalPengeluaran = view.findViewById(R.id.tv_total_pengeluaran)
        tvGrandTotal = view.findViewById(R.id.tv_grand_total)

        // FAB
        fabAdd = view.findViewById(R.id.fab_add_transaction)
    }

    private fun setupRecyclerView() {
        transactionAdapter =
                TransactionAdapter(
                        onItemClick = { transaction ->
                            if (activity is NavigationCallback) {
                                val page = TambahTransaksiPage()
                                val bundle = Bundle()
                                bundle.putInt("ARG_TRANSACTION_ID", transaction.id)
                                page.arguments = bundle
                                (activity as NavigationCallback).navigateTo(page)
                            }
                        },
                        getCategoryName = { id -> categoryMap[id]?.name ?: "Unknown" },
                        getCategoryIcon = { id -> categoryMap[id]?.icon ?: "⚙️" }
                )

        rvTransactions.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun updateSummaryDisplay() {
        val convertedIncome =
                com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                        currentIncome,
                        currentCurrencyCode
                )
        val convertedExpense =
                com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                        currentExpense,
                        currentCurrencyCode
                )
        val convertedTotal =
                com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                        currentIncome - currentExpense,
                        currentCurrencyCode
                )

        tvTotalPemasukan.text =
                com.example.catetduls.utils.CurrencyHelper.format(
                        convertedIncome,
                        currentCurrencySymbol
                )
        tvTotalPengeluaran.text =
                com.example.catetduls.utils.CurrencyHelper.format(
                        convertedExpense,
                        currentCurrencySymbol
                )

        tvGrandTotal.text =
                com.example.catetduls.utils.CurrencyHelper.format(
                        convertedTotal,
                        currentCurrencySymbol
                )
    }
}
