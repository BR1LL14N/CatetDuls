package com.example.catetduls.ui.pages

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.ui.adapter.TransactionAdapter
import com.example.catetduls.utils.Formatters
import com.example.catetduls.ui.viewmodel.DetailStatistikViewModel
import com.example.catetduls.ui.viewmodel.DetailStatistikViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter


class DetailStatistikPage : Fragment() {


    private lateinit var viewModel: DetailStatistikViewModel
    private lateinit var transactionAdapter: TransactionAdapter

    // Views
    private lateinit var lineChart: LineChart
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvCategoryIcon: TextView
    private lateinit var tvCategoryName: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvCurrentMonth: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail_statistik, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Init ViewModel
        val transactionRepo = requireContext().getTransactionRepository()
        val categoryRepo = requireContext().getCategoryRepository()
        val factory = DetailStatistikViewModelFactory(transactionRepo, categoryRepo)
        viewModel = ViewModelProvider(this, factory)[DetailStatistikViewModel::class.java]

        initViews(view)
        setupLineChart()
        setupRecyclerView()
        setupListeners()

        // 2. Ambil Argument (ID Kategori)
        val categoryId = arguments?.getInt("ARG_CATEGORY_ID", -1) ?: -1
        if (categoryId != -1) {
            viewModel.setCategoryId(categoryId)
            viewModel.setMonthYear(System.currentTimeMillis()) // Default bulan ini
        } else {
            Toast.makeText(requireContext(), "Kategori tidak valid", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        observeData()
    }

    private fun initViews(view: View) {
        lineChart = view.findViewById(R.id.line_chart)
        rvHistory = view.findViewById(R.id.rv_transaction_history)
        tvCategoryIcon = view.findViewById(R.id.tv_category_icon)
        tvCategoryName = view.findViewById(R.id.tv_category_name)
        tvTotalAmount = view.findViewById(R.id.tv_total_amount)
        tvCurrentMonth = view.findViewById(R.id.tv_current_month)
        btnBack = view.findViewById(R.id.btn_back)
        btnPrevMonth = view.findViewById(R.id.btn_prev_month)
        btnNextMonth = view.findViewById(R.id.btn_next_month)
    }

    private fun setupRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(requireContext())

        // Kita REUSE TransactionAdapter yang sudah canggih (Header + Item)
        transactionAdapter = TransactionAdapter(
            onItemClick = {
                // Opsional: Buka detail/edit transaksi jika diklik
                Toast.makeText(requireContext(), "Edit: ${it.notes}", Toast.LENGTH_SHORT).show()
            },
            // Karena ini halaman detail kategori spesifik, nama/ikon sudah jelas,
            // tapi adapter tetap butuh fungsi ini.
            getCategoryName = { viewModel.categoryInfo.value?.name ?: "" },
            getCategoryIcon = { viewModel.categoryInfo.value?.icon ?: "" },
            getWalletName = { "Wallet" } // TODO: Sebaiknya ambil dari Map wallet jika mau sempurna
        )
        rvHistory.adapter = transactionAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnPrevMonth.setOnClickListener { viewModel.navigateMonth(-1) }
        btnNextMonth.setOnClickListener { viewModel.navigateMonth(1) }
    }

    private fun setupLineChart() {
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false // Tidak perlu legenda untuk 1 garis
            setPinchZoom(false)
            setTouchEnabled(false) // Statis saja sesuai gambar
            setDrawGridBackground(false)

            // Konfigurasi Sumbu X (Bulan)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.DKGRAY
                textSize = 10f
                valueFormatter = IndexAxisValueFormatter(
                    listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                )
                // Pastikan menampilkan semua bulan
                axisMinimum = 0f
                axisMaximum = 11f
            }

            // Konfigurasi Sumbu Y (Kiri)
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                textColor = Color.GRAY
                axisMinimum = 0f // Mulai dari 0
            }

            axisRight.isEnabled = false // Matikan sumbu kanan
        }
    }

    private fun updateChartData(monthlyData: List<Float>) {
        val entries = monthlyData.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "Tren").apply {
            color = ContextCompat.getColor(requireContext(), R.color.danger) // Garis Merah
            lineWidth = 2f

            // Konfigurasi Titik (Dot)
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.danger))
            circleRadius = 4f
            setDrawCircleHole(false) // Lingkaran penuh

            // Mode Garis
            mode = LineDataSet.Mode.LINEAR // Garis lurus antar titik

            // Sembunyikan angka di atas titik (biar bersih)
            setDrawValues(false)
        }

        lineChart.data = LineData(dataSet)
        lineChart.invalidate() // Refresh
        lineChart.animateX(1000)
    }

    private fun observeData() {
        // 1. Info Kategori
        viewModel.categoryInfo.observe(viewLifecycleOwner) { category ->
            if (category != null) {
                tvCategoryName.text = category.name
                tvCategoryIcon.text = category.icon
            }
        }

        // 2. Bulan Header (Mengambil dari _selectedMonthYear di VM)
        // Note: Kita perlu expose livedata untuk ini di VM jika ingin dinamis,
        // tapi sementara kita bisa format manual di sini atau tambahkan di VM.
        // Asumsi: ViewModel punya helper 'formatMonthYear'
        // (Akan saya update di instruksi tambahan jika VM belum punya)

        // 3. Total Bulanan
        viewModel.currentMonthTotal.observe(viewLifecycleOwner) { total ->
            tvTotalAmount.text = Formatters.toRupiah(total)
        }

        // 4. List Transaksi
        viewModel.transactionHistory.observe(viewLifecycleOwner) { list ->
            transactionAdapter.submitList(list)

            // Update Header Tanggal (Manual triggger karena observe combine)
            // Di VM: _selectedMonthYear berubah -> transactionHistory berubah.
            // Kita bisa ambil timestamp dari item pertama list (jika ada) atau simpan state di VM.
        }

        // 5. Data Chart
        viewModel.lineChartData.observe(viewLifecycleOwner) { data ->
            updateChartData(data)
        }
    }

    companion object {
        fun newInstance(categoryId: Int): DetailStatistikPage {
            val fragment = DetailStatistikPage()
            val args = Bundle()
            args.putInt("ARG_CATEGORY_ID", categoryId)
            fragment.arguments = args
            return fragment
        }
    }
}