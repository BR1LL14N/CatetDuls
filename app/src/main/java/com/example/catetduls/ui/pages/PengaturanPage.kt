package com.example.catetduls.ui.pages

// Hapus import PengaturanViewModelFactory (sudah tidak ada)
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Import Hilt ktx
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.catetduls.R
import com.example.catetduls.viewmodel.PengaturanViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint // WAJIB: Anotasi agar Hilt bekerja di Fragment ini
class PengaturanPage : Fragment() {

    // CARA BARU: Init ViewModel dengan Hilt (Otomatis)
    private val viewModel: PengaturanViewModel by viewModels()

    // Views
    private lateinit var cardKelolaBuku: MaterialCardView
    private lateinit var cardKelolaKategori: MaterialCardView
    private lateinit var cardKelolaWallet: MaterialCardView
    private lateinit var btnBackup: MaterialButton
    private lateinit var btnRestore: MaterialButton
    private lateinit var btnExportCsv: MaterialButton
    private lateinit var btnExportJson: MaterialButton
    private lateinit var btnResetData: MaterialButton
    private lateinit var btnResetKategori: MaterialButton
    private lateinit var tvAppVersion: TextView
    private lateinit var tvTotalTransaksi: TextView
    private lateinit var tvTotalKategori: TextView

    private lateinit var btnEditProfile: MaterialButton

    // View Baru (Login/Logout Button)
    private lateinit var btnAuthAction: MaterialButton
    private lateinit var tvUserStatus: TextView
    private lateinit var btnSyncNow: MaterialButton

    // ===============================================
    // LAUNCHER UNTUK EXPORT FILE
    // ===============================================

    private val createCsvFileLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
                    uri: Uri? ->
                uri?.let { fileUri ->
                    viewLifecycleOwner.lifecycleScope.launch { handleCsvExportAndSave(fileUri) }
                }
                        ?: run {
                            Toast.makeText(
                                            requireContext(),
                                            "Export CSV dibatalkan",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
            }

    private val createJsonFileLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
                    uri: Uri? ->
                uri?.let { fileUri ->
                    viewLifecycleOwner.lifecycleScope.launch { handleJsonExportAndSave(fileUri) }
                }
                        ?: run {
                            Toast.makeText(
                                            requireContext(),
                                            "Export JSON dibatalkan",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
            }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pengaturan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // =================================================================
        // HAPUS SEMUA KODE INISIALISASI MANUAL (Repository, Factory, dll)
        // Hilt sudah menanganinya lewat 'by viewModels()' di atas.
        // =================================================================

        // Initialize Views
        initViews(view)

        // Setup
        setupButtons()

        // Observe data
        observeData()

        // Load statistics & Check Login
        loadStatistics()

        // Panggil fungsi ini (Pastikan di ViewModel sudah PUBLIC, tidak private)
        viewModel.checkLoginStatus()
    }

    private fun initViews(view: View) {
        cardKelolaBuku = view.findViewById(R.id.card_kelola_buku)
        cardKelolaKategori = view.findViewById(R.id.card_kelola_kategori)
        cardKelolaWallet = view.findViewById(R.id.card_kelola_wallet)
        btnBackup = view.findViewById(R.id.btn_backup)
        btnRestore = view.findViewById(R.id.btn_restore)
        btnExportCsv = view.findViewById(R.id.btn_export_csv)
        btnExportJson = view.findViewById(R.id.btn_export_json)
        btnResetData = view.findViewById(R.id.btn_reset_data)
        btnResetKategori = view.findViewById(R.id.btn_reset_kategori)
        tvAppVersion = view.findViewById(R.id.tv_app_version)
        tvTotalTransaksi = view.findViewById(R.id.tv_total_transaksi)
        tvTotalKategori = view.findViewById(R.id.tv_total_kategori)

        // Init View Baru
        btnAuthAction = view.findViewById(R.id.btn_auth_action)
        tvUserStatus = view.findViewById(R.id.tv_user_status)
        btnSyncNow = view.findViewById(R.id.btn_sync_now)

        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
    }

    private fun setupButtons() {
        // Kelola Buku
        cardKelolaBuku.setOnClickListener {
            val kelolaFragment = KelolaBukuPage()
            parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, kelolaFragment)
                    .addToBackStack(null)
                    .commit()
        }

        // Kelola Kategori
        cardKelolaKategori.setOnClickListener {
            val kelolaFragment = KelolaKategoriPage()
            parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, kelolaFragment)
                    .addToBackStack(null)
                    .commit()
        }

        cardKelolaWallet.setOnClickListener {
            val kelolaFragment = KelolaWalletPage()
            parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, kelolaFragment)
                    .addToBackStack(null)
                    .commit()
        }

        // Backup
        btnBackup.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val jsonData = viewModel.exportToJson()
                if (jsonData != null) {
                    val file = viewModel.saveBackupToFile(jsonData)
                    if (file != null) {
                        Toast.makeText(
                                        requireContext(),
                                        "Backup disimpan: ${file.name}",
                                        Toast.LENGTH_LONG
                                )
                                .show()
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
            val defaultFileName = viewModel.getBackupFileName("csv")
            createCsvFileLauncher.launch(defaultFileName)
        }

        // Export JSON
        btnExportJson.setOnClickListener {
            val defaultFileName = viewModel.getBackupFileName("json")
            createJsonFileLauncher.launch(defaultFileName)
        }

        // Reset Data
        btnResetData.setOnClickListener { showResetDataDialog() }

        // Reset Kategori
        btnResetKategori.setOnClickListener { showResetKategoriDialog() }

        // LOGIN / LOGOUT Button Listener
        btnAuthAction.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                // Jika Login -> Tampilkan konfirmasi Logout
                showLogoutDialog()
            } else {
                // Jika Guest -> Buka Halaman Login
                val loginFragment = LoginPage()
                parentFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_container, loginFragment)
                        .addToBackStack(null)
                        .commit()
            }
        }

        btnEditProfile.setOnClickListener {
            val editFragment = EditProfilePage()
            parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit()
        }

        btnSyncNow.setOnClickListener { viewModel.forceSync() }
    }

    // ===============================================
    // EXPORT HANDLERS
    // ===============================================

    private suspend fun handleCsvExportAndSave(fileUri: Uri) {
        val csvData = viewModel.exportToCsv()
        if (csvData != null) {
            try {
                requireContext().contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(csvData.toByteArray())
                    Toast.makeText(
                                    requireContext(),
                                    "Export CSV berhasil disimpan!",
                                    Toast.LENGTH_LONG
                            )
                            .show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                                requireContext(),
                                "Gagal menyimpan file CSV: ${e.message}",
                                Toast.LENGTH_LONG
                        )
                        .show()
                e.printStackTrace()
            }
        }
    }

    private suspend fun handleJsonExportAndSave(fileUri: Uri) {
        val jsonData = viewModel.exportToJson()
        if (jsonData != null) {
            try {
                requireContext().contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(jsonData.toByteArray())
                    Toast.makeText(
                                    requireContext(),
                                    "Export JSON berhasil disimpan!",
                                    Toast.LENGTH_LONG
                            )
                            .show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                                requireContext(),
                                "Gagal menyimpan file JSON: ${e.message}",
                                Toast.LENGTH_LONG
                        )
                        .show()
                e.printStackTrace()
            }
        }
    }

    // ===============================================
    // DIALOGS
    // ===============================================

    private fun showResetDataDialog() {
        AlertDialog.Builder(requireContext())
                .setTitle("Reset Semua Data")
                .setMessage(
                        "Apakah Anda yakin ingin menghapus SEMUA transaksi? Tindakan ini tidak dapat dibatalkan!"
                )
                .setPositiveButton("Ya, Hapus") { _, _ -> viewModel.resetAllData() }
                .setNegativeButton("Batal", null)
                .show()
    }

    private fun showResetKategoriDialog() {
        AlertDialog.Builder(requireContext())
                .setTitle("Reset Kategori")
                .setMessage("Apakah Anda yakin ingin mereset kategori ke default?")
                .setPositiveButton("Ya") { _, _ -> viewModel.resetCategoriesToDefault() }
                .setNegativeButton("Batal", null)
                .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
                .setTitle("Keluar Akun")
                .setMessage(
                        "Apakah Anda yakin ingin keluar? Data lokal yang belum disinkronkan mungkin hilang."
                )
                .setPositiveButton("Keluar") { _, _ -> viewModel.logout() }
                .setNegativeButton("Batal", null)
                .show()
    }

    // ===============================================
    // OBSERVERS
    // ===============================================

    private fun observeData() {
        // Observe Login Status (Untuk ubah tampilan tombol)
        viewModel.isLoggedIn.asLiveData().observe(viewLifecycleOwner) { isLoggedIn ->
            if (isLoggedIn) {
                btnAuthAction.text = "Keluar Akun"
                btnAuthAction.setTextColor(resources.getColor(R.color.danger, null))
                btnAuthAction.setIconTintResource(R.color.danger)
                btnEditProfile.visibility = View.VISIBLE
            } else {
                btnAuthAction.text = "Masuk Akun"
                btnAuthAction.setTextColor(resources.getColor(R.color.primary, null))
                btnAuthAction.setIconTintResource(R.color.primary)
                btnEditProfile.visibility = View.GONE
            }
        }

        // Observe Username
        viewModel.userName.asLiveData().observe(viewLifecycleOwner) { name ->
            tvUserStatus.text = "Halo, $name"
        }

        // Observe Success Message
        viewModel.successMessage.asLiveData().observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                loadStatistics()
                viewModel.clearMessages()
            }
        }

        // Observe Error Message
        viewModel.errorMessage.asLiveData().observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }
    }

    private fun loadStatistics() {
        tvAppVersion.text = "Versi ${viewModel.getAppVersion()}"
        val stats = viewModel.getAppStatistics()
        tvTotalTransaksi.text = "Total Transaksi: ${stats["Total Transaksi"]}"
        tvTotalKategori.text = "Total Kategori: ${stats["Total Kategori"]}"
    }
}
