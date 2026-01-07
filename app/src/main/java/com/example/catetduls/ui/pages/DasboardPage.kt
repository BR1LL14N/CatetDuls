package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Category
import com.example.catetduls.data.getBookRepository
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.ui.adapter.TransactionAdapter
import com.example.catetduls.viewmodel.DashboardViewModel
import com.example.catetduls.viewmodel.DashboardViewModelFactory
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/**
 * DashboardPage
 *
 * Menampilkan:
 * - Total Saldo
 * - Total Pemasukan Bulan Ini
 * - Total Pengeluaran Bulan Ini
 * - 5 Transaksi Terakhir
 */
class DashboardPage : Fragment() {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var transactionAdapter: TransactionAdapter

    private var categoryMap: Map<Int, Category> = emptyMap()

    private lateinit var tvTotalSaldo: TextView
    private lateinit var tvTotalPemasukan: TextView
    private lateinit var tvTotalPengeluaran: TextView
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var tvViewAll: TextView
    private lateinit var cardSaldo: MaterialCardView

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    // State Global
    private var currentCurrencySymbol: String = "Rp"
    private var currentCurrencyCode: String = "IDR"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = requireContext().getTransactionRepository()
        val factory = DashboardViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        initViews(view)

        loadCategoriesMap()

        setupRecyclerView()

        setupCurrencyObserver()

        observeData()

        setupClickListeners()
    }

    private fun setupCurrencyObserver() {
        val bookRepository = requireContext().getBookRepository()
        viewLifecycleOwner.lifecycleScope.launch {
            // Mengamati perubahan buku aktif secara real-time
            bookRepository.getActiveBook().collect { book ->
                if (book != null) {
                    // ✅ PERBAIKAN: Tambahkan Elvis Operator (?:)
                    // Jika null, gunakan default "Rp" dan "IDR"
                    currentCurrencySymbol = book.currencySymbol ?: "Rp"
                    currentCurrencyCode = book.currencyCode ?: "IDR"

                    // Update adapter currency
                    transactionAdapter.setCurrency(currentCurrencyCode, currentCurrencySymbol)

                    // Force update summary texts manually with new currency
                    updateDashboardTexts()
                }
            }
        }
    }

    // We need to store latest values to re-update format
    private var lastBalance: Double? = null
    private var lastIncome: Double? = null
    private var lastExpense: Double? = null

    private fun updateDashboardTexts() {
        if (lastBalance != null) {
            val converted =
                    com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                            lastBalance!!,
                            currentCurrencyCode
                    )
            tvTotalSaldo.text = viewModel.formatCurrency(converted, currentCurrencySymbol)
            val color = viewModel.getBalanceColor(lastBalance)
            tvTotalSaldo.setTextColor(color)
        }
        if (lastIncome != null) {
            val converted =
                    com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                            lastIncome!!,
                            currentCurrencyCode
                    )
            tvTotalPemasukan.text = viewModel.formatCurrency(converted, currentCurrencySymbol)
        }
        if (lastExpense != null) {
            val converted =
                    com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                            lastExpense!!,
                            currentCurrencyCode
                    )
            tvTotalPengeluaran.text = viewModel.formatCurrency(converted, currentCurrencySymbol)
        }
    }

    // ...

    private fun observeData() {
        // Observe total saldo
        viewModel.totalBalance.observe(viewLifecycleOwner) { balance ->
            lastBalance = balance
            val converted =
                    com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                            balance ?: 0.0,
                            currentCurrencyCode
                    )
            tvTotalSaldo.text = viewModel.formatCurrency(converted, currentCurrencySymbol)
            val color = viewModel.getBalanceColor(balance)
            tvTotalSaldo.setTextColor(color)
        }

        // Observe pemasukan bulan ini
        viewModel.totalIncomeThisMonth.observe(viewLifecycleOwner) { income ->
            lastIncome = income
            val converted =
                    com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                            income ?: 0.0,
                            currentCurrencyCode
                    )
            tvTotalPemasukan.text = viewModel.formatCurrency(converted, currentCurrencySymbol)
        }

        // Observe pengeluaran bulan ini
        viewModel.totalExpenseThisMonth.observe(viewLifecycleOwner) { expense ->
            lastExpense = expense
            val converted =
                    com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                            expense ?: 0.0,
                            currentCurrencyCode
                    )
            tvTotalPengeluaran.text = viewModel.formatCurrency(converted, currentCurrencySymbol)
        }

        // Observe transaksi terakhir
        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->

            // PERBAIKAN: Konversi Transaction -> TransactionListItem
            // Karena Dashboard hanya menampilkan list simpel, kita bungkus semua jadi
            // TransactionItem
            val listItems =
                    transactions.map { transaction ->
                        com.example.catetduls.ui.adapter.TransactionListItem.TransactionItem(
                                transaction
                        )
                    }

            transactionAdapter.submitList(listItems)
        }
    }

    private fun initViews(view: View) {
        tvTotalSaldo = view.findViewById(R.id.tv_total_saldo)
        tvTotalPemasukan = view.findViewById(R.id.tv_total_pemasukan)
        tvTotalPengeluaran = view.findViewById(R.id.tv_total_pengeluaran)
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions)
        tvViewAll = view.findViewById(R.id.tv_view_all)
        cardSaldo = view.findViewById(R.id.card_saldo)
    }

    /**
     * Memuat semua kategori dari database dan menyimpannya dalam Map. Ini membuat pencarian nama
     * dan ikon menjadi sinkron dan cepat.
     */
    private fun loadCategoriesMap() {
        val categoryRepository = requireContext().getCategoryRepository()

        viewLifecycleOwner.lifecycleScope.launch {
            // PERBAIKAN: Gunakan .collect(), bukan .first()
            // Agar Dashboard selalu update real-time jika ada perubahan kategori
            categoryRepository.getAllCategories().collect { categories ->

                // 1. Update Map Kategori terbaru
                categoryMap = categories.associateBy { it.id }

                // 2. Refresh Adapter Transaksi
                // Kita perlu memaksa adapter untuk me-refresh tampilan
                // agar nama kategori yang baru (dari Map) segera muncul.
                if (transactionAdapter.currentList.isNotEmpty()) {
                    transactionAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupRecyclerView() {

        transactionAdapter =
                TransactionAdapter(
                        onItemClick = { transaction -> },
                        getCategoryName = { categoryId ->
                            // Jika categoryId ditemukan, ambil namanya. Jika tidak, gunakan
                            // "Unknown".
                            categoryMap[categoryId]?.name ?: "Unknown"
                        },
                        getCategoryIcon = { categoryId ->
                            // Jika categoryId ditemukan, ambil ikonnya. Jika tidak, gunakan "⚙️".
                            categoryMap[categoryId]?.icon ?: "⚙️"
                        }
                )

        rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        tvViewAll.setOnClickListener {
            // Pindah ke tab Transaksi
            requireActivity()
                    .findViewById<
                            com.google.android.material.bottomnavigation.BottomNavigationView>(
                            R.id.bottom_navigation
                    )
                    ?.selectedItemId = R.id.nav_transaksi
        }
    }
}
