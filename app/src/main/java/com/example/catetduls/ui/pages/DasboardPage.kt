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
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.ui.adapter.TransactionAdapter
import com.example.catetduls.viewmodel.DashboardViewModel
import com.example.catetduls.viewmodel.DashboardViewModelFactory
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.first
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = requireContext().getTransactionRepository()
        val factory = DashboardViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        initViews(view)

        loadCategoriesMap()

        setupRecyclerView()

        observeData()

        setupClickListeners()
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
     * Memuat semua kategori dari database dan menyimpannya dalam Map.
     * Ini membuat pencarian nama dan ikon menjadi sinkron dan cepat.
     */
    private fun loadCategoriesMap() {
        val categoryRepository = requireContext().getCategoryRepository()

        viewLifecycleOwner.lifecycleScope.launch {
            // Ambil data kategori
            val categories = categoryRepository.getAllCategories().first()


            categoryMap = categories.associateBy { it.id }


            viewModel.recentTransactions.value?.let {
                transactionAdapter.submitList(it)
            }
        }
    }

    private fun setupRecyclerView() {

        transactionAdapter = TransactionAdapter(
            onItemClick = { transaction ->

            },

            getCategoryName = { categoryId ->
                // Jika categoryId ditemukan, ambil namanya. Jika tidak, gunakan "Unknown".
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
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottom_navigation
            )?.selectedItemId = R.id.nav_transaksi
        }
    }

    private fun observeData() {
        // Observe total saldo
        viewModel.totalBalance.observe(viewLifecycleOwner) { balance ->
            tvTotalSaldo.text = viewModel.formatCurrency(balance)

            // Set warna saldo
            val color = viewModel.getBalanceColor(balance)
            tvTotalSaldo.setTextColor(color)
        }

        // Observe pemasukan bulan ini
        viewModel.totalIncomeThisMonth.observe(viewLifecycleOwner) { income ->
            tvTotalPemasukan.text = viewModel.formatCurrency(income)
        }

        // Observe pengeluaran bulan ini
        viewModel.totalExpenseThisMonth.observe(viewLifecycleOwner) { expense ->
            tvTotalPengeluaran.text = viewModel.formatCurrency(expense)
        }

        // Observe transaksi terakhir
        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
        }
    }
}