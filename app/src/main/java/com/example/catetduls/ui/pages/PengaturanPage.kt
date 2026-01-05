package com.example.catetduls.ui.pages

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Import Hilt ktx
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.catetduls.R
import com.example.catetduls.data.AppDatabase
import com.example.catetduls.data.getBookRepository // Import Extension
import com.example.catetduls.viewmodel.PengaturanViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
    private lateinit var btnLaporan: MaterialButton
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
    private lateinit var tvActiveBookName: TextView
    private lateinit var tvActiveCurrency: TextView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var loadingText: TextView

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

    private val createPdfFileLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {
                    uri: Uri? ->
                uri?.let { fileUri ->
                    viewLifecycleOwner.lifecycleScope.launch { handlePdfExportAndSave(fileUri) }
                }
                        ?: run {
                            Toast.makeText(
                                            requireContext(),
                                            "Export PDF dibatalkan",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
            }
    
    private val createSqlFileLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("application/sql")) {
                    uri: Uri? ->
                uri?.let { fileUri ->
                    viewLifecycleOwner.lifecycleScope.launch { handleSqlExportAndSave(fileUri) }
                }
                        ?: run {
                            Toast.makeText(
                                            requireContext(),
                                            "Export SQL dibatalkan",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
            }

    private val restoreFileLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                uri?.let { fileUri ->
                    viewLifecycleOwner.lifecycleScope.launch { handleRestoreFromFile(fileUri) }
                }
                        ?: run {
                            Toast.makeText(
                                            requireContext(),
                                            "Restore dibatalkan",
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
        setupButtons(view)
        setupCardAnimations(view)

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
        btnLaporan = view.findViewById(R.id.btn_laporan)
        btnResetData = view.findViewById(R.id.btn_reset_data)
        btnResetKategori = view.findViewById(R.id.btn_reset_kategori)
        tvAppVersion = view.findViewById(R.id.tv_app_version)
        tvTotalTransaksi = view.findViewById(R.id.tv_total_transaksi)
        tvTotalKategori = view.findViewById(R.id.tv_total_kategori)

        // Init View Baru
        btnAuthAction = view.findViewById(R.id.btn_auth_action)
        tvUserStatus = view.findViewById(R.id.tv_user_status)
        btnSyncNow = view.findViewById(R.id.btn_sync_now)
        tvActiveBookName = view.findViewById(R.id.tv_active_book_name)
        tvActiveCurrency = view.findViewById(R.id.tv_active_currency)

        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
        loadingText = view.findViewById(R.id.loading_text)
    }

    private fun setupCardAnimations(view: View) {
        val cards =
                listOf(
                        view.findViewById<MaterialCardView>(R.id.card_kelola_buku),
                        view.findViewById<MaterialCardView>(R.id.card_kelola_kategori),
                        view.findViewById<MaterialCardView>(R.id.card_kelola_wallet),
                        view.findViewById<MaterialCardView>(R.id.card_mata_uang_buku)
                )

        cards.forEachIndexed { index, card: MaterialCardView ->
            card.alpha = 0f
            card.translationY = 50f

            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay((index * 100).toLong())
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
        }
    }

    private fun animateClick(view: View, action: () -> Unit) {
        val scaleDown =
                android.animation.ObjectAnimator.ofPropertyValuesHolder(
                                view,
                                android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.95f),
                                android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.95f)
                        )
                        .apply { duration = 100 }

        val scaleUp =
                android.animation.ObjectAnimator.ofPropertyValuesHolder(
                                view,
                                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f),
                                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f)
                        )
                        .apply { duration = 100 }

        scaleDown.addListener(
                object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        scaleUp.start()
                        action()
                    }
                }
        )

        scaleDown.start()
    }

    private fun setupButtons(view: View) {
        // Kelola Buku
        cardKelolaBuku.setOnClickListener {
            animateClick(it) {
                val kelolaFragment = KelolaBukuPage()
                parentFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_container, kelolaFragment)
                        .addToBackStack(null)
                        .commit()
            }
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

        // Mata Uang Buku
        val cardMataUang = view.findViewById<MaterialCardView>(R.id.card_mata_uang_buku)
        cardMataUang.setOnClickListener { showCurrencySelectionDialog() }

        // Show current currency validation
        viewLifecycleOwner.lifecycleScope.launch {
            val bookRepository = requireContext().getBookRepository()
            bookRepository.getActiveBook().collect { book ->
                if (book != null) {
                    tvActiveBookName.text = book.name
                    tvActiveCurrency.text = "${book.currencyCode} (${book.currencySymbol})"
                }
            }
        }

        // Backup
        btnBackup.setOnClickListener {
            showBackupDialog()
        }

        // Restore
        btnRestore.setOnClickListener {
            showRestoreDialog()
        }

        // Laporan
        btnLaporan.setOnClickListener { animateClick(it) { showLaporanDialog() } }

        // Reset Data
        btnResetData.setOnClickListener {
            showCustomDialog(
                    icon = R.drawable.ic_delete_24,
                    iconTint = R.color.danger,
                    title = "Reset Semua Data",
                    message =
                            "Apakah Anda yakin ingin menghapus SEMUA transaksi? Tindakan ini tidak dapat dibatalkan!",
                    confirmText = "Ya, Hapus",
                    confirmColor = R.color.danger,
                    onConfirm = { viewModel.resetAllData() }
            )
        }

        // Reset Kategori
        btnResetKategori.setOnClickListener {
            showCustomDialog(
                    icon = R.drawable.ic_category_24,
                    iconTint = R.color.warning,
                    title = "Reset Kategori",
                    message = "Apakah Anda yakin ingin mereset kategori ke default?",
                    confirmText = "Ya, Reset",
                    confirmColor = R.color.primary,
                    onConfirm = { viewModel.resetCategoriesToDefault() }
            )
        }

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

        btnSyncNow.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                viewModel.forceSync()
            } else {
                Toast.makeText(
                                requireContext(),
                                "Harap login terlebih dahulu untuk menggunakan fitur sinkronisasi",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        }
    }

    // ===============================================
    // EXPORT HANDLERS
    // ===============================================

    private suspend fun handleCsvExportAndSave(fileUri: Uri) {
        // Use pending transactions from laporan if available, otherwise export all
        val transactions =
                if (pendingCsvTransactions.isNotEmpty()) {
                    pendingCsvTransactions
                } else {
                    val database = AppDatabase.getDatabase(requireContext())
                    val prefs =
                            requireContext()
                                    .getSharedPreferences(
                                            "app_settings",
                                            android.content.Context.MODE_PRIVATE
                                    )
                    val activeBookId = prefs.getInt("active_book_id", 1)
                    kotlinx.coroutines.withContext<List<com.example.catetduls.data.Transaction>>(
                            kotlinx.coroutines.Dispatchers.IO
                    ) { database.transactionDao().getAllTransactions(activeBookId).first() }
                }

        val csvData = generateCSV(transactions)
        if (csvData != null) {
            try {
                requireContext().contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(csvData.toByteArray())
                    showSnackbar("CSV berhasil tersimpan!")
                }
                // Clear pending
                pendingCsvTransactions = emptyList()
            } catch (e: Exception) {
                showSnackbar("Gagal menyimpan CSV: ${e.message}", true)
            }
        }
    }

    private fun generateCSV(transactions: List<com.example.catetduls.data.Transaction>): String {
        val sb = StringBuilder()
        sb.append("Tanggal,Kategori,Dompet,Tipe,Jumlah,Catatan\n")

        transactions.forEach { transaction ->
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("id", "ID"))
            val date = sdf.format(java.util.Date(transaction.date))
            val type =
                    if (transaction.type == com.example.catetduls.data.TransactionType.PEMASUKAN)
                            "Pemasukan"
                    else "Pengeluaran"

            sb.append("$date,")
            sb.append("${transaction.categoryId},") // Could fetch name but keeping it simple
            sb.append("${transaction.walletId},")
            sb.append("$type,")
            sb.append("${transaction.amount},")
            sb.append("\"${transaction.notes}\"\n")
        }

        return sb.toString()
    }

    private suspend fun handleJsonExportAndSave(fileUri: Uri) {
        val jsonData = viewModel.exportToJson(null)
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

    private suspend fun handleRestoreFromFile(fileUri: Uri) {
        try {
            loadingOverlay.visibility = View.VISIBLE
            loadingText.text = "Memulihkan backup..."

            // Read file content
            val fileContent =
                    requireContext().contentResolver.openInputStream(fileUri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }
                            ?: run {
                                showSnackbar("Gagal membaca file backup", true)
                                loadingOverlay.visibility = View.GONE
                                return
                            }

            // Detect file format based on content
            val isJson = fileContent.trim().startsWith("{")
            val isSql = fileContent.trim().uppercase().contains("INSERT INTO")
            
            val success = when {
                isJson -> {
                    // Validate JSON format
                    try {
                        val gson = com.google.gson.Gson()
                        gson.fromJson(fileContent, com.google.gson.JsonObject::class.java)
                        viewModel.restoreFromJson(fileContent)
                    } catch (e: Exception) {
                        showSnackbar("Format JSON tidak valid", true)
                        loadingOverlay.visibility = View.GONE
                        return
                    }
                }
                isSql -> {
                    viewModel.restoreFromSql(fileContent)
                }
                else -> {
                    showSnackbar("Format file tidak dikenali. Gunakan file JSON atau SQL", true)
                    loadingOverlay.visibility = View.GONE
                    return
                }
            }

            loadingOverlay.visibility = View.GONE

            if (success) {
                showSnackbar("Backup berhasil dipulihkan!")
                // Reload statistics
                loadStatistics()
            } else {
                showSnackbar("Gagal memulihkan backup", true)
            }
        } catch (e: Exception) {
            loadingOverlay.visibility = View.GONE
            showSnackbar("Error: ${e.message}", true)
            e.printStackTrace()
        }
    }

    // ===============================================
    // DIALOGS
    // ===============================================    }

    private fun showCustomDialog(
            icon: Int,
            iconTint: Int,
            title: String,
            message: String,
            confirmText: String,
            confirmColor: Int,
            onConfirm: () -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)
        val dialog =
                MaterialAlertDialogBuilder(requireContext())
                        .setView(dialogView)
                        .setCancelable(true)
                        .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val iconView = dialogView.findViewById<ImageView>(R.id.dialog_icon)
        val titleView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val messageView = dialogView.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_confirm)

        iconView.setImageResource(icon)
        iconView.setColorFilter(resources.getColor(iconTint, null))
        titleView.text = title
        messageView.text = message
        btnConfirm.text = confirmText
        btnConfirm.setBackgroundColor(resources.getColor(confirmColor, null))

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            animateClick(it) {
                onConfirm()
                dialog.dismiss()
            }
        }

        dialog.show()

        dialogView.alpha = 0f
        dialogView.scaleX = 0.8f
        dialogView.scaleY = 0.8f
        dialogView
                .animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .start()
    }

    private fun showSnackbar(message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view

        snackbarView.setBackgroundColor(
                resources.getColor(if (isError) R.color.danger else R.color.success, null)
        )

        snackbar.setTextColor(resources.getColor(R.color.white, null))
        snackbar.show()
    }

    private fun showResetDataDialog() {
        showCustomDialog(
                icon = R.drawable.ic_delete_24,
                iconTint = R.color.danger,
                title = "Reset Semua Data",
                message =
                        "Apakah Anda yakin ingin menghapus SEMUA transaksi? Tindakan ini tidak dapat dibatalkan!",
                confirmText = "Ya, Hapus",
                confirmColor = R.color.danger,
                onConfirm = { viewModel.resetAllData() }
        )
    }

    private fun showResetKategoriDialog() {
        showCustomDialog(
                icon = R.drawable.ic_category_24,
                iconTint = R.color.warning,
                title = "Reset Kategori",
                message = "Apakah Anda yakin ingin mereset kategori ke default?",
                confirmText = "Ya, Reset",
                confirmColor = R.color.primary,
                onConfirm = { viewModel.resetCategoriesToDefault() }
        )
    }

    private fun showLogoutDialog() {
        showCustomDialog(
                icon = R.drawable.ic_warning_24,
                iconTint = R.color.warning,
                title = "Keluar Akun",
                message =
                        "Apakah Anda yakin ingin keluar? Data lokal yang belum disinkronkan mungkin hilang.",
                confirmText = "Keluar",
                confirmColor = R.color.danger,
                onConfirm = { viewModel.logout() }
        )
    }

    private fun showCurrencySelectionDialog() {
        val currencies = com.example.catetduls.utils.CurrencyHelper.getAvailableCurrencies()
        val items = currencies.map { it.toString() }.toTypedArray()

        // Fetch current active code effectively using a one-shot fetch or observing activeBook in
        // VM
        // Since we are in Fragment, we can launch a coroutine to get it before showing dialog,
        // OR just show dialog without pre-selection if lazy.
        // But user asked for "kasih mark selected".

        viewLifecycleOwner.lifecycleScope.launch {
            val bookRepository = requireContext().getBookRepository()
            val activeBook = bookRepository.getBookByIdSync(viewModel.getActiveBookId())
            val currentCode = activeBook?.currencyCode ?: "IDR"

            // Find index
            val checkedItem = currencies.indexOfFirst { it.code == currentCode }

            AlertDialog.Builder(requireContext())
                    .setTitle("Pilih Mata Uang Buku Aktif")
                    .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                        val selected = currencies[which]
                        viewModel.updateActiveBookCurrency(selected.code, selected.symbol)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
        }
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

        viewModel.isLoading.asLiveData().observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingOverlay.visibility = View.VISIBLE
            } else {
                loadingOverlay.visibility = View.GONE
                loadStatistics()
            }
        }

        // Observe Username
        viewModel.userName.asLiveData().observe(viewLifecycleOwner) { name ->
            tvUserStatus.text = "Halo, $name"
        }

        // Observe Success Message
        viewModel.successMessage.asLiveData().observe(viewLifecycleOwner) { message ->
            message?.let {
                showSnackbar(it, false)
                loadStatistics()
                viewModel.clearMessages()
            }
        }

        // Observe Error Message
        viewModel.errorMessage.asLiveData().observe(viewLifecycleOwner) { error ->
            error?.let {
                showSnackbar(it, true)
                viewModel.clearMessages()
            }
        }
    }

    private fun loadStatistics() {
        tvAppVersion.text = "Versi ${viewModel.getAppVersion()}"
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(requireContext())
                val prefs =
                        requireContext()
                                .getSharedPreferences(
                                        "app_settings",
                                        android.content.Context.MODE_PRIVATE
                                )
                val activeBookId = prefs.getInt("active_book_id", 1)

                val transactionCount =
                        kotlinx.coroutines.withContext<Int>(kotlinx.coroutines.Dispatchers.IO) {
                            database.transactionDao().getAllTransactions(activeBookId).first().size
                        }

                val categoryCount =
                        kotlinx.coroutines.withContext<Int>(kotlinx.coroutines.Dispatchers.IO) {
                            database.categoryDao().getAllCategoriesSync(activeBookId).size
                        }

                tvTotalTransaksi.text = "Total Transaksi: $transactionCount"
                tvTotalKategori.text = "Total Kategori: $categoryCount"
            } catch (e: Exception) {
                tvTotalTransaksi.text = "Total Transaksi: -"
                tvTotalKategori.text = "Total Kategori: -"
            }
        }
    }

    private fun showLaporanDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_laporan, null)
        val dialog =
                MaterialAlertDialogBuilder(requireContext())
                        .setView(dialogView)
                        .setCancelable(true)
                        .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)

        // Views
        val chipGroup =
                dialogView.findViewById<com.google.android.material.chip.ChipGroup>(
                        R.id.chip_group_filter
                )
        val chip7days =
                dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_7days)
        val chip30days =
                dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_30days)
        val chip1year =
                dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_1year)
        val chipAll = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chip_all)

        val etStartDate =
                dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
                        R.id.et_start_date
                )
        val etEndDate =
                dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
                        R.id.et_end_date
                )

        val rgFormat = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_format)
        val rbCsv =
                dialogView.findViewById<
                        com.google.android.material.radiobutton.MaterialRadioButton>(R.id.rb_csv)
        val rbPdf =
                dialogView.findViewById<
                        com.google.android.material.radiobutton.MaterialRadioButton>(R.id.rb_pdf)

        val tvPeriodInfo = dialogView.findViewById<TextView>(R.id.tv_period_info)
        val tvTransactionCount = dialogView.findViewById<TextView>(R.id.tv_transaction_count)

        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnGenerate = dialogView.findViewById<MaterialButton>(R.id.btn_generate)

        // Date selection state
        var startDate: Long? = null
        var endDate: Long? = null

        fun updateDateFields() {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
            if (startDate != null && endDate != null) {
                etStartDate.setText(sdf.format(java.util.Date(startDate!!)))
                etEndDate.setText(sdf.format(java.util.Date(endDate!!)))
                tvPeriodInfo.text =
                        "Periode: ${sdf.format(java.util.Date(startDate!!))} - ${sdf.format(java.util.Date(endDate!!))}"
            } else {
                etStartDate.setText("")
                etEndDate.setText("")
                tvPeriodInfo.text = "Periode: Semua waktu"
            }
        }

        // Quick filter logic
        chip7days.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            endDate = cal.timeInMillis
            cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
            startDate = cal.timeInMillis
            updateDateFields()
        }

        chip30days.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            endDate = cal.timeInMillis
            cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
            startDate = cal.timeInMillis
            updateDateFields()
        }

        chip1year.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            endDate = cal.timeInMillis
            cal.add(java.util.Calendar.YEAR, -1)
            startDate = cal.timeInMillis
            updateDateFields()
        }

        chipAll.setOnClickListener {
            startDate = null
            endDate = null
            updateDateFields()
        }

        // Date pickers
        etStartDate.setOnClickListener {
            val datePicker =
                    com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Pilih Tanggal Mulai")
                            .setSelection(startDate ?: System.currentTimeMillis())
                            .build()

            datePicker.addOnPositiveButtonClickListener {
                startDate = it
                chipGroup.clearCheck()
                updateDateFields()
            }
            datePicker.show(parentFragmentManager, "START_DATE_PICKER")
        }

        etEndDate.setOnClickListener {
            val datePicker =
                    com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Pilih Tanggal Akhir")
                            .setSelection(endDate ?: System.currentTimeMillis())
                            .build()

            datePicker.addOnPositiveButtonClickListener {
                endDate = it
                chipGroup.clearCheck()
                updateDateFields()
            }
            datePicker.show(parentFragmentManager, "END_DATE_PICKER")
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnGenerate.setOnClickListener {
            val format = if (rbCsv.isChecked) "csv" else "pdf"

            lifecycleScope.launch {
                try {
                    loadingOverlay.visibility = View.VISIBLE
                    loadingText.text = "Membuat laporan..."

                    // Get filtered transactions
                    val database = AppDatabase.getDatabase(requireContext())
                    val prefs =
                            requireContext()
                                    .getSharedPreferences(
                                            "app_settings",
                                            android.content.Context.MODE_PRIVATE
                                    )
                    val activeBookId = prefs.getInt("active_book_id", 1)

                    val allTransactions =
                            kotlinx.coroutines.withContext<
                                    List<com.example.catetduls.data.Transaction>>(
                                    kotlinx.coroutines.Dispatchers.IO
                            ) { database.transactionDao().getAllTransactions(activeBookId).first() }

                    val filteredTransactions =
                            if (startDate != null && endDate != null) {
                                allTransactions.filter {
                                    it.date >= startDate!! && it.date <= endDate!!
                                }
                            } else {
                                allTransactions
                            }

                    loadingOverlay.visibility = View.GONE

                    if (filteredTransactions.isEmpty()) {
                        showSnackbar("Tidak ada transaksi pada periode ini", true)
                        return@launch
                    }

                    if (format == "csv") {
                        exportToCSV(filteredTransactions)
                    } else {
                        exportToPDF(filteredTransactions, startDate, endDate)
                    }

                    dialog.dismiss()
                } catch (e: Exception) {
                    loadingOverlay.visibility = View.GONE
                    showSnackbar("Gagal membuat laporan: ${e.message}", true)
                }
            }
        }

        dialog.show()
    }

    private fun exportToCSV(transactions: List<com.example.catetduls.data.Transaction>) {
        val fileName = "Laporan_${System.currentTimeMillis()}.csv"
        createCsvFileLauncher.launch(fileName)

        // Store untuk dipakai di file created callback
        pendingCsvTransactions = transactions
    }

    private var pendingCsvTransactions: List<com.example.catetduls.data.Transaction> = emptyList()
    private var pendingPdfTransactions: List<com.example.catetduls.data.Transaction> = emptyList()
    private var pendingPdfStartDate: Long? = null
    private var pendingPdfEndDate: Long? = null

    private fun exportToPDF(
            transactions: List<com.example.catetduls.data.Transaction>,
            startDate: Long?,
            endDate: Long?
    ) {
        val fileName = "Laporan_${System.currentTimeMillis()}.pdf"
        pendingPdfTransactions = transactions
        pendingPdfStartDate = startDate
        pendingPdfEndDate = endDate
        createPdfFileLauncher.launch(fileName)
    }

    private suspend fun handlePdfExportAndSave(fileUri: Uri) {
        val transactions = pendingPdfTransactions
        val startDate = pendingPdfStartDate
        val endDate = pendingPdfEndDate

        if (transactions.isEmpty()) return

        lifecycleScope.launch {
            try {
                loadingOverlay.visibility = View.VISIBLE
                loadingText.text = "Membuat PDF..."

                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageInfo =
                        android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDocument.startPage(pageInfo)

                val canvas = page.canvas
                val paint = android.graphics.Paint()

                // Title
                paint.textSize = 20f
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                canvas.drawText("LAPORAN TRANSAKSI", 50f, 50f, paint)

                // Period
                paint.textSize = 12f
                paint.typeface = android.graphics.Typeface.DEFAULT
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
                val period =
                        if (startDate != null && endDate != null) {
                            "${sdf.format(java.util.Date(startDate))} - ${sdf.format(java.util.Date(endDate))}"
                        } else {
                            "Semua Waktu"
                        }
                canvas.drawText("Periode: $period", 50f, 80f, paint)
                canvas.drawText("Total: ${transactions.size} transaksi", 50f, 100f, paint)

                // Summary
                val totalIncome =
                        transactions
                                .filter {
                                    it.type == com.example.catetduls.data.TransactionType.PEMASUKAN
                                }
                                .sumOf { it.amount }
                val totalExpense =
                        transactions
                                .filter {
                                    it.type ==
                                            com.example.catetduls.data.TransactionType.PENGELUARAN
                                }
                                .sumOf { it.amount }

                canvas.drawText(
                        "Total Pemasukan: Rp ${String.format("%,.0f", totalIncome)}",
                        50f,
                        130f,
                        paint
                )
                canvas.drawText(
                        "Total Pengeluaran: Rp ${String.format("%,.0f", totalExpense)}",
                        50f,
                        150f,
                        paint
                )
                canvas.drawText(
                        "Selisih: Rp ${String.format("%,.0f", totalIncome - totalExpense)}",
                        50f,
                        170f,
                        paint
                )

                pdfDocument.finishPage(page)

                requireContext().contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()

                loadingOverlay.visibility = View.GONE
                showSnackbar("PDF berhasil tersimpan!")

                // Clear pending
                pendingPdfTransactions = emptyList()
                pendingPdfStartDate = null
                pendingPdfEndDate = null
            } catch (e: Exception) {
                loadingOverlay.visibility = View.GONE
                showSnackbar("Gagal menyimpan PDF: ${e.message}", true)
                e.printStackTrace()
            }
        }
    }
    
    // ========================================
    // BACKUP/RESTORE DIALOGS
    // ========================================
    
    private fun showBackupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_backup, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Initialize views
        val actvBookSelection = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.actv_book_selection)
        val rgBackupFormat = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_backup_format)
        val tvBackupPreview = dialogView.findViewById<android.widget.TextView>(R.id.tv_backup_preview)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        val btnGenerate = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_generate)
        
        // Load books for selection
        viewLifecycleOwner.lifecycleScope.launch {
            val books = viewModel.getAllBooksForSelection()
            val activeBookId = viewModel.getActiveBookId()
            val bookNames = mutableListOf("Semua Buku")
            bookNames.addAll(books.map { book -> 
                if (book.id == activeBookId) "${book.name} ✓ Aktif" else book.name 
            })
            
            val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, bookNames)
            actvBookSelection.setAdapter(adapter)
            actvBookSelection.setText("Semua Buku", false)
            
            // Update preview
            updateBackupPreview(null, tvBackupPreview)
            
            // Book selection listener
            actvBookSelection.setOnItemClickListener { _, _, position, _ ->
                val selectedBookId = if (position == 0) null else books[position - 1].id
                viewLifecycleOwner.lifecycleScope.launch {
                    updateBackupPreview(selectedBookId, tvBackupPreview)
                }
            }
        }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnGenerate.setOnClickListener {
            val selectedFormat = when (rgBackupFormat.checkedRadioButtonId) {
                R.id.rb_json -> "json"
                R.id.rb_sql -> "sql"
                else -> "json"
            }
            
            val selectedBookText = actvBookSelection.text.toString()
            val selectedBookId = if (selectedBookText == "Semua Buku") null else {
                viewLifecycleOwner.lifecycleScope.launch {
                    val books = viewModel.getAllBooksForSelection()
                    books.find { it.name == selectedBookText }?.id
                }
                null // Simplified for now
            }
            
            dialog.dismiss()
            
            // Launch file picker
            val fileName = "backup_${System.currentTimeMillis()}.$selectedFormat"
            if (selectedFormat == "json") {
                createJsonFileLauncher.launch(fileName)
            } else {
                createSqlFileLauncher.launch(fileName)
            }
        }
        
        dialog.show()
    }
    
    private suspend fun updateBackupPreview(bookId: Int?, textView: android.widget.TextView) {
        val preview = viewModel.getBackupPreview(bookId)
        textView.text = "${preview.bookName}\n" +
                "Transaksi: ${preview.transactionCount}\n" +
                "Kategori: ${preview.categoryCount}\n" +
                "Dompet: ${preview.walletCount}"
    }
    
    private fun showRestoreDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_restore, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        val btnChooseFile = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_choose_file)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnChooseFile.setOnClickListener {
            dialog.dismiss()
            restoreFileLauncher.launch(arrayOf("application/json", "application/sql"))
        }
        
        dialog.show()
    }
    
    private suspend fun handleSqlExportAndSave(fileUri: Uri) {
        val sqlData = viewModel.exportToSql(null)
        if (sqlData != null) {
            try {
                requireContext().contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(sqlData.toByteArray())
                    Toast.makeText(
                                    requireContext(),
                                    "Export SQL berhasil disimpan!",
                                    Toast.LENGTH_LONG
                            )
                            .show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                                requireContext(),
                                "Gagal menyimpan file SQL: ${e.message}",
                                Toast.LENGTH_LONG
                        )
                        .show()
                e.printStackTrace()
            }
        }
    }
}
