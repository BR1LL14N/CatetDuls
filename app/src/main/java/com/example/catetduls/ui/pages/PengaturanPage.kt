package com.example.catetduls.ui.pages

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.catetduls.R
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.viewmodel.PengaturanViewModel
import com.example.catetduls.viewmodel.PengaturanViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.launch


/**
 * PengaturanPage - Halaman pengaturan aplikasi
 *
 * Fitur:
 * - Dark Mode toggle
 * - Kelola Kategori
 * - Backup & Restore
 * - Export CSV/JSON
 * - Reset Data
 * - Info Aplikasi
 */
class PengaturanPage : Fragment() {

    private lateinit var viewModel: PengaturanViewModel

    // Views
    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var cardKelolaKategori: MaterialCardView
    private lateinit var btnBackup: MaterialButton
    private lateinit var btnRestore: MaterialButton
    private lateinit var btnExportCsv: MaterialButton
    private lateinit var btnExportJson: MaterialButton
    private lateinit var btnResetData: MaterialButton
    private lateinit var btnResetKategori: MaterialButton
    private lateinit var tvAppVersion: TextView
    private lateinit var tvTotalTransaksi: TextView
    private lateinit var tvTotalKategori: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pengaturan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        val transactionRepo = requireContext().getTransactionRepository()
        val categoryRepo = requireContext().getCategoryRepository()
        val factory = PengaturanViewModelFactory(transactionRepo, categoryRepo, requireContext())
        viewModel = ViewModelProvider(this, factory)[PengaturanViewModel::class.java]

        // Initialize Views
        initViews(view)

        // Setup
        setupDarkMode()
        setupButtons()

        // Observe data
        observeData()

        // Load statistics
        loadStatistics()
    }

    private fun initViews(view: View) {
        switchDarkMode = view.findViewById(R.id.switch_dark_mode)
        cardKelolaKategori = view.findViewById(R.id.card_kelola_kategori)
        btnBackup = view.findViewById(R.id.btn_backup)
        btnRestore = view.findViewById(R.id.btn_restore)
        btnExportCsv = view.findViewById(R.id.btn_export_csv)
        btnExportJson = view.findViewById(R.id.btn_export_json)
        btnResetData = view.findViewById(R.id.btn_reset_data)
        btnResetKategori = view.findViewById(R.id.btn_reset_kategori)
        tvAppVersion = view.findViewById(R.id.tv_app_version)
        tvTotalTransaksi = view.findViewById(R.id.tv_total_transaksi)
        tvTotalKategori = view.findViewById(R.id.tv_total_kategori)
    }

    private fun setupDarkMode() {
        // Set initial state langsung dari SharedPreferences
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val currentDarkMode = prefs.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = currentDarkMode

        // Listener untuk toggle
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleDarkMode()
            Toast.makeText(requireContext(), "Dark Mode akan aktif setelah restart", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtons() {
        // Kelola Kategori
        cardKelolaKategori.setOnClickListener {
            Toast.makeText(requireContext(), "Kelola Kategori - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Backup
        btnBackup.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val jsonData = viewModel.exportToJson()
                if (jsonData != null) {
                    val file = viewModel.saveBackupToFile(jsonData)
                    if (file != null) {
                        Toast.makeText(requireContext(), "Backup disimpan: ${file.name}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Restore
        btnRestore.setOnClickListener {
            Toast.makeText(requireContext(), "Restore - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // Export CSV
        btnExportCsv.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val csvData = viewModel.exportToCsv()
                if (csvData != null) {
                    Toast.makeText(requireContext(), "Export CSV berhasil", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Export JSON
        btnExportJson.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val jsonData = viewModel.exportToJson()
                if (jsonData != null) {
                    Toast.makeText(requireContext(), "Export JSON berhasil", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Reset Data
        btnResetData.setOnClickListener {
            showResetDataDialog()
        }

        // Reset Kategori
        btnResetKategori.setOnClickListener {
            showResetKategoriDialog()
        }
    }

    private fun showResetDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Semua Data")
            .setMessage("Apakah Anda yakin ingin menghapus SEMUA transaksi? Tindakan ini tidak dapat dibatalkan!")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                viewModel.resetAllData()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showResetKategoriDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Kategori")
            .setMessage("Apakah Anda yakin ingin mereset kategori ke default?")
            .setPositiveButton("Ya") { _, _ ->
                viewModel.resetCategoriesToDefault()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun observeData() {
        // Observe success messages
        viewModel.successMessage.asLiveData().observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        // Observe error messages
        viewModel.errorMessage.asLiveData().observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun loadStatistics() {
        // Set app version
        tvAppVersion.text = "Versi ${viewModel.getAppVersion()}"

        // Get app statistics
        val stats = viewModel.getAppStatistics()
        tvTotalTransaksi.text = "Total Transaksi: ${stats["Total Transaksi"]}"
        tvTotalKategori.text = "Total Kategori: ${stats["Total Kategori"]}"
    }
}