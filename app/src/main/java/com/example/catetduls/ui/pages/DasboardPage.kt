package com.example.catetduls.ui.pages

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.viewmodel.DashboardViewModel
import com.example.catetduls.viewmodel.DashboardViewModelFactory
import com.google.android.material.card.MaterialCardView

/**
 * DashboardPage - Halaman utama aplikasi
 *
 * Menampilkan:
 * - Total Saldo
 * - Total Pemasukan Bulan Ini
 * - Total Pengeluaran Bulan Ini
 * - 5 Transaksi Terakhir
 */
class DashboardPage : Fragment() {

    private lateinit var viewModel: DashboardViewModel

    // Views
    private lateinit var tvTotalSaldo: TextView
    private lateinit var tvTotalPemasukan: TextView
    private lateinit var tvTotalPengeluaran: TextView
    private lateinit var rvRecentTransactions: RecyclerView
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

        // Initialize ViewModel
        val repository = requireContext().getTransactionRepository()
        val factory = DashboardViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        // Initialize Views
        initViews(view)

        // Setup RecyclerView
        setupRecyclerView()

        // Observe data
        observeData()
    }

    private fun initViews(view: View) {
        tvTotalSaldo = view.findViewById(R.id.tv_total_saldo)
        tvTotalPemasukan = view.findViewById(R.id.tv_total_pemasukan)
        tvTotalPengeluaran = view.findViewById(R.id.tv_total_pengeluaran)
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions)
        cardSaldo = view.findViewById(R.id.card_saldo)
    }

    private fun setupRecyclerView() {
        rvRecentTransactions.layoutManager = LinearLayoutManager(requireContext())
        // Adapter akan dibuat nanti
    }

    private fun observeData() {
        // Observe total saldo
        viewModel.totalBalance.observe(viewLifecycleOwner) { balance ->
            tvTotalSaldo.text = viewModel.formatCurrency(balance)

            // Set warna card berdasarkan saldo
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
            // Setup adapter dengan data transactions
            // TODO: Implement RecyclerView Adapter
        }
    }
}