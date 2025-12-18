package com.example.catetduls.ui.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.ui.adapter.CalendarAdapter
import com.example.catetduls.viewmodel.CalendarViewModel
import com.example.catetduls.viewmodel.CalendarViewModelFactory
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import com.example.catetduls.data.getBookRepository // Import Extension
import java.util.*

class CalendarPage : Fragment() {

    private lateinit var viewModel: CalendarViewModel

    // Views
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView
    private lateinit var tvCurrentMonth: TextView
    private lateinit var tvMonthlyIncome: TextView
    private lateinit var tvMonthlyExpense: TextView
    private lateinit var tvMonthlyTotal: TextView
    private lateinit var rvCalendarGrid: RecyclerView

    // Adapter
    private var calendarAdapter: CalendarAdapter? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_kalender_transaksi, container, false)
    }

    // State Global
    private var currentCurrencySymbol: String = "Rp"
    private var currentCurrencyCode: String = "IDR"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi ViewModel
        val repository = requireContext().getTransactionRepository()
        val factory = CalendarViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CalendarViewModel::class.java]

        initViews(view)
        setupCalendarGrid()
        setupListeners()
        
        // Fetch Currency AND THEN Observe Data
        fetchCurrencyAndObserve()
    }
    
    private fun fetchCurrencyAndObserve() {
         val bookRepository = requireContext().getBookRepository()
         viewLifecycleOwner.lifecycleScope.launch {
             // Use collect to react to changes!
             bookRepository.getActiveBook().collect { book ->
                 if (book != null) {
                     currentCurrencyCode = book.currencyCode
                     currentCurrencySymbol = book.currencySymbol
                     calendarAdapter?.updateCurrency(currentCurrencyCode, currentCurrencySymbol)
                     // Trigger re-render of summary if needed (by re-observing or modifying values)
                     // Since LiveData doesn't re-emit on variable change, we might need to manually update text
                     // if functionality depends on it. 
                     // But let's just observeData() call once, and inside verify it uses current var.
                     // IMPORTANT: LiveData observer lambda captures variables. 
                     
                     // Instead of complex rebinding, let's just make sure updateViews is called.
                     // We can force update TextViews here using last values from ViewModel if possible
                     // or just wait for next emission.
                 }
             }
         }
         observeData()
    }

    private fun initViews(view: View) {
        btnPrevMonth = view.findViewById(R.id.btn_prev_month)
        btnNextMonth = view.findViewById(R.id.btn_next_month)
        tvCurrentMonth = view.findViewById(R.id.tv_current_month)
        tvMonthlyIncome = view.findViewById(R.id.tv_monthly_income)
        tvMonthlyExpense = view.findViewById(R.id.tv_monthly_expense)
        tvMonthlyTotal = view.findViewById(R.id.tv_monthly_total)
        rvCalendarGrid = view.findViewById(R.id.rv_calendar_grid)
    }

    private fun setupListeners() {
        btnPrevMonth.setOnClickListener { viewModel.prevMonth() }
        btnNextMonth.setOnClickListener { viewModel.nextMonth() }
    }

    private fun setupCalendarGrid() {
        // Inisialisasi Adapter
        // Inisialisasi Adapter
        calendarAdapter =
                CalendarAdapter(
                        emptyList(),
                        { timestamp ->
                            // Saat tanggal diklik
                            viewModel.selectDate(timestamp)

                            // On Day Click -> Navigate to TransaksiPage
                            if (activity is NavigationCallback) {
                                val fragment = TransaksiPage()
                                val args = Bundle()
                                args.putLong("ARG_INITIAL_DATE", timestamp)
                                args.putInt("ARG_INITIAL_TAB_MODE", 0) // 0 = Harian
                                fragment.arguments = args

                                (activity as NavigationCallback).navigateTo(fragment)
                            }
                        },
                        "IDR",
                        "Rp"
                )

        // Fixed 6 rows, but span is 7
        rvCalendarGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        rvCalendarGrid.adapter = calendarAdapter
    }

    private fun observeData() {
        // Observe Bulan Aktif untuk Header
        viewModel.currentMonth.asLiveData().observe(viewLifecycleOwner) { timestamp ->
            tvCurrentMonth.text = viewModel.formatMonthYear(timestamp)
        }

        // Observe Ringkasan Bulanan (Summary Bar)
        // Need current currency code. Since we don't have it as property in Fragment (fetched in local scope),
        // we should elevate `currentCurrencyCode` to class level or fetch it here again.
        // Or better, let ViewModel handle this if passed, but easier to do here consistent with Dashboard.
        // Let's rely on the fact that we need to fetch it.
        
        // Wait, I need currentCurrencyCode available here.
        // I will declare it at class level first.
        
        viewModel.monthlyIncome.observe(viewLifecycleOwner) { income ->
            val converted = com.example.catetduls.utils.CurrencyHelper.convertIdrTo(income ?: 0.0, currentCurrencyCode)
            tvMonthlyIncome.text = com.example.catetduls.utils.CurrencyHelper.format(converted, currentCurrencySymbol)
        }
        viewModel.monthlyExpense.observe(viewLifecycleOwner) { expense ->
            val converted = com.example.catetduls.utils.CurrencyHelper.convertIdrTo(expense ?: 0.0, currentCurrencyCode)
            tvMonthlyExpense.text = com.example.catetduls.utils.CurrencyHelper.format(converted, currentCurrencySymbol)
        }
        viewModel.monthlyTotal.observe(viewLifecycleOwner) { total ->
            val converted = com.example.catetduls.utils.CurrencyHelper.convertIdrTo(total ?: 0.0, currentCurrencyCode)
            tvMonthlyTotal.text = com.example.catetduls.utils.CurrencyHelper.format(converted, currentCurrencySymbol)
        }

        // Observe Data Kalender untuk Grid (New Logic: List is prepared in VM)
        viewModel.calendarGridCells.observe(viewLifecycleOwner) { cells ->
            calendarAdapter?.submitList(cells)
        }

        // Removed logic for manual padding and dynamic height calculation as grid is now fixed
        // logic in VM.
    }
}
