package com.example.catetduls.ui.pages

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.catetduls.R
import com.example.catetduls.data.AppDatabase
import com.example.catetduls.data.getBookRepository // Import Extension
import com.example.catetduls.ui.viewmodel.PengaturanViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private lateinit var ivProfileSetting: ImageView
    private lateinit var cardProfileImage: androidx.cardview.widget.CardView
    private lateinit var tvUserEmail: TextView

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                when (requestCode) {
                    REQUEST_CODE_CREATE_CSV -> {
                        lifecycleScope.launch { handleCsvExportAndSave(uri) }
                    }
                    REQUEST_CODE_CREATE_PDF -> {
                        lifecycleScope.launch { handlePdfExportAndSave(uri) }
                    }
                    else -> {
                        // Ignore other request codes
                    }
                }
            }
        }
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

        // Profile Views
        ivProfileSetting = view.findViewById(R.id.iv_profile_setting)
        cardProfileImage = view.findViewById(R.id.card_profile_image)
        tvUserEmail = view.findViewById(R.id.tv_user_email)
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
        btnBackup.setOnClickListener { showBackupDialog() }

        // Restore
        btnRestore.setOnClickListener { showRestoreDialog() }

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
    // HELPER
    // ===============================================

    private fun loadUserPhoto(photoUrl: String?) {
        if (photoUrl.isNullOrEmpty()) {
            ivProfileSetting.setImageResource(R.drawable.ic_person_24)
            return
        }

        // Determine load source: Local File or Remote URL
        val loadModel: Any =
                when {
                    // 1. Local File Check (Absolute path & file exists)
                    !photoUrl.startsWith("http") && java.io.File(photoUrl).exists() -> {
                        java.io.File(photoUrl)
                    }
                    // 2. Remote URL construction
                    photoUrl.startsWith("http") -> photoUrl
                    photoUrl.startsWith("/api/") -> "http://10.0.2.2:8000$photoUrl"
                    photoUrl.startsWith("/storage/") -> "http://10.0.2.2:8000$photoUrl"
                    else -> "http://10.0.2.2:8000/api/photos/$photoUrl"
                }

        // Hapus tint default
        ivProfileSetting.imageTintList = null

        Glide.with(this)
                .load(loadModel)
                .apply(com.bumptech.glide.request.RequestOptions().override(400, 400))
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.ic_person_24)
                .error(R.drawable.ic_person_24)
                .circleCrop()
                .into(ivProfileSetting)
    }

    // ===============================================
    // EXPORT HANDLERS
    // ===============================================

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
            val isSql =
                    fileContent.trim().uppercase().let {
                        it.contains("INSERT INTO") || it.contains("INSERT OR REPLACE INTO")
                    }

            val success =
                    when {
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
                            showSnackbar(
                                    "Format file tidak dikenali. Gunakan file JSON atau SQL",
                                    true
                            )
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

        // Observe Username & Photo
        viewModel.currentUser.asLiveData().observe(viewLifecycleOwner) { user ->
            if (user != null) {
                tvUserStatus.text = "Halo, ${user.name}"
                tvUserEmail.text = user.email
                tvUserEmail.visibility = View.VISIBLE
                cardProfileImage.visibility = View.VISIBLE
                btnAuthAction.text = "Keluar Akun"

                loadUserPhoto(user.photo_url)
            } else {
                tvUserStatus.text = "Halo, Tamu"
                tvUserEmail.visibility = View.GONE
                cardProfileImage.visibility = View.GONE
                btnAuthAction.text = "Masuk Akun"
            }
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

    private fun exportToPDF(
            transactions: List<com.example.catetduls.data.Transaction>,
            startDate: Long?,
            endDate: Long?
    ) {
        val fileName = "Laporan_CatetDuls_${System.currentTimeMillis()}.pdf"

        // Use file picker to let user choose where to save
        val intent =
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }

        // Store data for callback
        pendingPdfTransactions = transactions
        pendingPdfStartDate = startDate
        pendingPdfEndDate = endDate

        // Launch file picker
        try {
            startActivityForResult(intent, REQUEST_CODE_CREATE_PDF)
        } catch (e: Exception) {
            showSnackbar("Gagal membuka file picker: ${e.message}", true)
        }
    }

    private fun exportToCSV(transactions: List<com.example.catetduls.data.Transaction>) {
        val fileName = "Laporan_CatetDuls_${System.currentTimeMillis()}.csv"

        // Use file picker to let user choose where to save
        val intent =
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/csv"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }

        // Store transactions for callback
        pendingCsvTransactions = transactions

        // Launch file picker (you'll need to add this launcher in onCreate/onViewCreated)
        try {
            startActivityForResult(intent, REQUEST_CODE_CREATE_CSV)
        } catch (e: Exception) {
            showSnackbar("Gagal membuka file picker: ${e.message}", true)
        }
    }

    private fun handleCsvExportAndSave(fileUri: Uri) {
        val transactions = pendingCsvTransactions
        if (transactions.isEmpty()) return

        lifecycleScope.launch {
            try {
                loadingOverlay.visibility = View.VISIBLE
                loadingText.text = "Membuat CSV..."

                withContext(Dispatchers.IO) {
                    val csvData = buildString {
                        // ===== HEADER SECTION (as comments) =====
                        appendLine("# LAPORAN TRANSAKSI KEUANGAN")
                        appendLine("# CatetDuls - Aplikasi Pencatatan Keuangan")

                        // Generation date
                        val now =
                                java.text.SimpleDateFormat(
                                                "dd MMMM yyyy, HH:mm",
                                                java.util.Locale("id", "ID")
                                        )
                                        .format(java.util.Date())
                        appendLine("# Tanggal Generate: $now")
                        appendLine("#")

                        // Summary calculations
                        val totalIncome =
                                transactions
                                        .filter {
                                            it.type ==
                                                    com.example.catetduls.data.TransactionType
                                                            .PEMASUKAN
                                        }
                                        .sumOf { it.amount }
                        val totalExpense =
                                transactions
                                        .filter {
                                            it.type ==
                                                    com.example.catetduls.data.TransactionType
                                                            .PENGELUARAN
                                        }
                                        .sumOf { it.amount }
                        val balance = totalIncome - totalExpense

                        appendLine("# Total Transaksi: ${transactions.size}")
                        appendLine("# Total Pemasukan: Rp ${String.format("%,.0f", totalIncome)}")
                        appendLine(
                                "# Total Pengeluaran: Rp ${String.format("%,.0f", totalExpense)}"
                        )
                        appendLine("# Saldo: Rp ${String.format("%,.0f", balance)}")
                        appendLine("#")
                        appendLine("")

                        // ===== COLUMN HEADERS =====
                        appendLine("No,Tanggal,Waktu,Kategori,Tipe,Keterangan,Jumlah")

                        // ===== DATA ROWS =====
                        val dateFormat =
                                java.text.SimpleDateFormat(
                                        "dd/MM/yyyy",
                                        java.util.Locale("id", "ID")
                                )
                        val timeFormat =
                                java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID"))

                        transactions.forEachIndexed { index, transaction ->
                            val date = dateFormat.format(java.util.Date(transaction.date))
                            val time = timeFormat.format(java.util.Date(transaction.date))
                            val category =
                                    transaction.categoryId
                                            .toString() // You may want to fetch actual category
                            // name
                            val type =
                                    when (transaction.type) {
                                        com.example.catetduls.data.TransactionType.PEMASUKAN ->
                                                "Pemasukan"
                                        com.example.catetduls.data.TransactionType.PENGELUARAN ->
                                                "Pengeluaran"
                                        else -> "Transfer"
                                    }

                            // Escape notes for CSV (handle commas and quotes)
                            val escapedNotes =
                                    transaction.notes.replace("\"", "\"\"") // Escape quotes
                                            .let {
                                                if (it.contains(",") ||
                                                                it.contains("\"") ||
                                                                it.contains("\n")
                                                )
                                                        "\"$it\""
                                                else it
                                            }

                            // Format amount as number (no Rp prefix for Excel compatibility)
                            val amount = transaction.amount.toLong()

                            appendLine(
                                    "${index + 1},$date,$time,$category,$type,$escapedNotes,$amount"
                            )
                        }

                        // ===== SUMMARY SECTION (as comments at end) =====
                        appendLine("")
                        appendLine("#")
                        appendLine("# RINGKASAN")
                        appendLine("# Total Pemasukan,$totalIncome")
                        appendLine("# Total Pengeluaran,$totalExpense")
                        appendLine("# Saldo,$balance")
                    }

                    // Write to file
                    requireContext().contentResolver.openOutputStream(fileUri)?.use { outputStream
                        ->
                        outputStream.write(csvData.toByteArray())
                    }
                }

                loadingOverlay.visibility = View.GONE
                showSnackbar("CSV berhasil tersimpan!")

                // Clear pending
                pendingCsvTransactions = emptyList()
            } catch (e: Exception) {
                loadingOverlay.visibility = View.GONE
                showSnackbar("Gagal menyimpan CSV: ${e.message}", true)
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_CREATE_CSV = 1001
        private const val REQUEST_CODE_CREATE_PDF = 1002
    }

    private var pendingCsvTransactions: List<com.example.catetduls.data.Transaction> = emptyList()
    private var pendingPdfTransactions: List<com.example.catetduls.data.Transaction> = emptyList()
    private var pendingPdfStartDate: Long? = null
    private var pendingPdfEndDate: Long? = null

    // ========================================
    // BACKUP/RESTORE DIALOGS
    // ========================================

    private suspend fun handlePdfExportAndSave(fileUri: Uri) {
        val transactions = pendingPdfTransactions
        val startDate = pendingPdfStartDate
        val endDate = pendingPdfEndDate

        if (transactions.isEmpty()) return

        lifecycleScope.launch {
            try {
                loadingOverlay.visibility = View.VISIBLE
                loadingText.text = "Membuat PDF..."

                withContext(Dispatchers.IO) {
                    val pdfDocument = android.graphics.pdf.PdfDocument()

                    // PDF Constants
                    val PAGE_WIDTH = 595f
                    val PAGE_HEIGHT = 842f
                    val MARGIN = 40f
                    val USABLE_WIDTH = PAGE_WIDTH - (2 * MARGIN)

                    // Colors
                    val COLOR_PRIMARY = android.graphics.Color.rgb(33, 150, 243)
                    val COLOR_INCOME = android.graphics.Color.rgb(76, 175, 80)
                    val COLOR_EXPENSE = android.graphics.Color.rgb(244, 67, 54)
                    val COLOR_GRAY = android.graphics.Color.rgb(158, 158, 158)
                    val COLOR_LIGHT_GRAY = android.graphics.Color.rgb(245, 245, 245)

                    var pageNumber = 1
                    var yPosition = MARGIN

                    // Create first page
                    val pageInfo =
                            android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                            PAGE_WIDTH.toInt(),
                                            PAGE_HEIGHT.toInt(),
                                            pageNumber
                                    )
                                    .create()
                    var page = pdfDocument.startPage(pageInfo)
                    var canvas = page.canvas
                    val paint = android.graphics.Paint()

                    // ===== HEADER SECTION =====

                    // Draw Logo
                    val d =
                            androidx.core.content.ContextCompat.getDrawable(
                                    requireContext(),
                                    R.mipmap.ic_launcher
                            )
                    val bitmap =
                            if (d is android.graphics.drawable.BitmapDrawable) {
                                d.bitmap
                            } else {
                                val bmp =
                                        android.graphics.Bitmap.createBitmap(
                                                d?.intrinsicWidth ?: 100,
                                                d?.intrinsicHeight ?: 100,
                                                android.graphics.Bitmap.Config.ARGB_8888
                                        )
                                val c = android.graphics.Canvas(bmp)
                                d?.setBounds(0, 0, c.width, c.height)
                                d?.draw(c)
                                bmp
                            }
                    val scaledBitmap =
                            android.graphics.Bitmap.createScaledBitmap(bitmap, 50, 50, false)
                    canvas.drawBitmap(scaledBitmap, MARGIN, yPosition, paint)

                    // App Name (Right of Logo)
                    paint.color = COLOR_PRIMARY
                    paint.textSize = 24f
                    paint.typeface =
                            android.graphics.Typeface.create(
                                    android.graphics.Typeface.DEFAULT,
                                    android.graphics.Typeface.BOLD
                            )
                    // Geser teks ke kanan logo (margin + lebar logo + padding)
                    canvas.drawText("CatetDuls", MARGIN + 60f, yPosition + 20f, paint)

                    paint.textSize = 12f
                    paint.color = COLOR_GRAY
                    paint.typeface = android.graphics.Typeface.DEFAULT
                    canvas.drawText(
                            "Pencatatan Keuangan Pribadi",
                            MARGIN + 60f,
                            yPosition + 40f,
                            paint
                    )

                    // Move Y position down below logo
                    yPosition += 70f

                    // Report Title
                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 18f
                    paint.typeface =
                            android.graphics.Typeface.create(
                                    android.graphics.Typeface.DEFAULT,
                                    android.graphics.Typeface.BOLD
                            )
                    // Center align title
                    val titleText = "LAPORAN TRANSAKSI KEUANGAN"
                    val titleWidth = paint.measureText(titleText)
                    val titleX = (PAGE_WIDTH - titleWidth) / 2
                    canvas.drawText(titleText, titleX, yPosition, paint)
                    yPosition += 15f

                    // Separator Line
                    paint.strokeWidth = 2f
                    canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, paint)
                    yPosition += 20f

                    // ===== METADATA SECTION =====
                    paint.textSize = 11f
                    paint.typeface = android.graphics.Typeface.DEFAULT

                    // Generation Date
                    val now =
                            java.text.SimpleDateFormat(
                                            "dd MMMM yyyy, HH:mm",
                                            java.util.Locale("id", "ID")
                                    )
                                    .format(java.util.Date())
                    canvas.drawText("Tanggal Cetak: $now", MARGIN, yPosition, paint)
                    yPosition += 18f

                    // Period
                    val sdf =
                            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
                    val period =
                            if (startDate != null && endDate != null) {
                                "${sdf.format(java.util.Date(startDate))} - ${sdf.format(java.util.Date(endDate))}"
                            } else {
                                "Semua Waktu"
                            }
                    canvas.drawText("Periode: $period", MARGIN, yPosition, paint)
                    yPosition += 18f

                    // Total Transactions
                    canvas.drawText(
                            "Total Transaksi: ${transactions.size}",
                            MARGIN,
                            yPosition,
                            paint
                    )
                    yPosition += 25f

                    // ===== SUMMARY BOX =====
                    val totalIncome =
                            transactions
                                    .filter {
                                        it.type ==
                                                com.example.catetduls.data.TransactionType.PEMASUKAN
                                    }
                                    .sumOf { it.amount }
                    val totalExpense =
                            transactions
                                    .filter {
                                        it.type ==
                                                com.example.catetduls.data.TransactionType
                                                        .PENGELUARAN
                                    }
                                    .sumOf { it.amount }
                    val balance = totalIncome - totalExpense

                    // Draw summary box background
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = COLOR_LIGHT_GRAY
                    canvas.drawRect(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition + 70f, paint)

                    // Draw summary box border
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.color = COLOR_GRAY
                    paint.strokeWidth = 1f
                    canvas.drawRect(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition + 70f, paint)

                    yPosition += 20f
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.textSize = 10f

                    // Income
                    paint.color = COLOR_INCOME
                    canvas.drawText("Total Pemasukan:", MARGIN + 10f, yPosition, paint)
                    paint.typeface =
                            android.graphics.Typeface.create(
                                    android.graphics.Typeface.DEFAULT,
                                    android.graphics.Typeface.BOLD
                            )
                    canvas.drawText(
                            "Rp ${String.format("%,.0f", totalIncome)}",
                            MARGIN + 150f,
                            yPosition,
                            paint
                    )
                    yPosition += 18f

                    // Expense
                    paint.typeface = android.graphics.Typeface.DEFAULT
                    paint.color = COLOR_EXPENSE
                    canvas.drawText("Total Pengeluaran:", MARGIN + 10f, yPosition, paint)
                    paint.typeface =
                            android.graphics.Typeface.create(
                                    android.graphics.Typeface.DEFAULT,
                                    android.graphics.Typeface.BOLD
                            )
                    canvas.drawText(
                            "Rp ${String.format("%,.0f", totalExpense)}",
                            MARGIN + 150f,
                            yPosition,
                            paint
                    )
                    yPosition += 18f

                    // Balance
                    paint.typeface = android.graphics.Typeface.DEFAULT
                    paint.color = android.graphics.Color.BLACK
                    canvas.drawText("Saldo:", MARGIN + 10f, yPosition, paint)
                    paint.typeface =
                            android.graphics.Typeface.create(
                                    android.graphics.Typeface.DEFAULT,
                                    android.graphics.Typeface.BOLD
                            )
                    paint.color = if (balance >= 0) COLOR_INCOME else COLOR_EXPENSE
                    canvas.drawText(
                            "Rp ${String.format("%,.0f", balance)}",
                            MARGIN + 150f,
                            yPosition,
                            paint
                    )

                    yPosition += 30f

                    // ===== TRANSACTION TABLE =====
                    paint.color = android.graphics.Color.BLACK
                    paint.typeface =
                            android.graphics.Typeface.create(
                                    android.graphics.Typeface.DEFAULT,
                                    android.graphics.Typeface.BOLD
                            )
                    paint.textSize = 12f
                    canvas.drawText("Rincian Transaksi", MARGIN, yPosition, paint)
                    yPosition += 20f

                    // Table Header
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = COLOR_PRIMARY
                    canvas.drawRect(
                            MARGIN,
                            yPosition - 15f,
                            PAGE_WIDTH - MARGIN,
                            yPosition + 5f,
                            paint
                    )

                    paint.color = android.graphics.Color.WHITE
                    paint.textSize = 9f
                    paint.typeface =
                            android.graphics.Typeface.create(
                                    android.graphics.Typeface.DEFAULT,
                                    android.graphics.Typeface.BOLD
                            )

                    val colNo = MARGIN + 5f
                    val colDate = MARGIN + 30f
                    val colCategory = MARGIN + 100f
                    val colNotes = MARGIN + 200f
                    val colIncome = MARGIN + 340f
                    val colExpense = MARGIN + 440f

                    canvas.drawText("No", colNo, yPosition, paint)
                    canvas.drawText("Tanggal", colDate, yPosition, paint)
                    canvas.drawText("Kategori", colCategory, yPosition, paint)
                    canvas.drawText("Keterangan", colNotes, yPosition, paint)
                    canvas.drawText("Pemasukan", colIncome, yPosition, paint)
                    canvas.drawText("Pengeluaran", colExpense, yPosition, paint)

                    yPosition += 20f

                    // Table Rows
                    paint.typeface = android.graphics.Typeface.DEFAULT
                    paint.textSize = 8f
                    val dateFormat =
                            java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale("id", "ID"))

                    transactions.forEachIndexed { index, transaction ->
                        // Check if need new page
                        if (yPosition > PAGE_HEIGHT - 100f) {
                            // Footer for current page
                            paint.color = COLOR_GRAY
                            paint.textSize = 8f
                            canvas.drawText(
                                    "Halaman $pageNumber",
                                    PAGE_WIDTH / 2 - 20f,
                                    PAGE_HEIGHT - 20f,
                                    paint
                            )

                            pdfDocument.finishPage(page)
                            pageNumber++

                            // Start new page
                            val newPageInfo =
                                    android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                                    PAGE_WIDTH.toInt(),
                                                    PAGE_HEIGHT.toInt(),
                                                    pageNumber
                                            )
                                            .create()
                            page = pdfDocument.startPage(newPageInfo)
                            canvas = page.canvas
                            yPosition = MARGIN

                            // Repeat table header on new page
                            paint.style = android.graphics.Paint.Style.FILL
                            paint.color = COLOR_PRIMARY
                            canvas.drawRect(
                                    MARGIN,
                                    yPosition - 15f,
                                    PAGE_WIDTH - MARGIN,
                                    yPosition + 5f,
                                    paint
                            )

                            paint.color = android.graphics.Color.WHITE
                            paint.textSize = 9f
                            paint.typeface =
                                    android.graphics.Typeface.create(
                                            android.graphics.Typeface.DEFAULT,
                                            android.graphics.Typeface.BOLD
                                    )
                            canvas.drawText("No", colNo, yPosition, paint)
                            canvas.drawText("Tanggal", colDate, yPosition, paint)
                            canvas.drawText("Kategori", colCategory, yPosition, paint)
                            canvas.drawText("Keterangan", colNotes, yPosition, paint)
                            canvas.drawText("Pemasukan", colIncome, yPosition, paint)
                            canvas.drawText("Pengeluaran", colExpense, yPosition, paint)

                            yPosition += 20f
                            paint.typeface = android.graphics.Typeface.DEFAULT
                            paint.textSize = 8f
                        }

                        // Alternating row background
                        if (index % 2 == 0) {
                            paint.style = android.graphics.Paint.Style.FILL
                            paint.color = COLOR_LIGHT_GRAY
                            canvas.drawRect(
                                    MARGIN,
                                    yPosition - 12f,
                                    PAGE_WIDTH - MARGIN,
                                    yPosition + 3f,
                                    paint
                            )
                        }

                        // Row data
                        paint.color = android.graphics.Color.BLACK
                        canvas.drawText("${index + 1}", colNo, yPosition, paint)
                        canvas.drawText(
                                dateFormat.format(java.util.Date(transaction.date)),
                                colDate,
                                yPosition,
                                paint
                        )

                        // Get category name (truncate if too long)
                        val categoryName =
                                transaction.categoryId
                                        .toString() // You may want to fetch actual category name
                        val truncatedCategory =
                                if (categoryName.length > 12) categoryName.substring(0, 12) + "..."
                                else categoryName
                        canvas.drawText(truncatedCategory, colCategory, yPosition, paint)

                        // Notes (truncate if too long)
                        val truncatedNotes =
                                if (transaction.notes.length > 18)
                                        transaction.notes.substring(0, 18) + "..."
                                else transaction.notes
                        canvas.drawText(truncatedNotes, colNotes, yPosition, paint)

                        // Amount
                        if (transaction.type == com.example.catetduls.data.TransactionType.PEMASUKAN
                        ) {
                            paint.color = COLOR_INCOME
                            canvas.drawText(
                                    String.format("%,.0f", transaction.amount),
                                    colIncome,
                                    yPosition,
                                    paint
                            )
                        } else {
                            paint.color = COLOR_EXPENSE
                            canvas.drawText(
                                    String.format("%,.0f", transaction.amount),
                                    colExpense,
                                    yPosition,
                                    paint
                            )
                        }

                        yPosition += 15f
                    }

                    // Footer for last page
                    paint.color = COLOR_GRAY
                    paint.textSize = 8f
                    canvas.drawText(
                            "Halaman $pageNumber",
                            PAGE_WIDTH / 2 - 20f,
                            PAGE_HEIGHT - 20f,
                            paint
                    )
                    canvas.drawText("Dibuat oleh CatetDuls v1.0", MARGIN, PAGE_HEIGHT - 20f, paint)

                    pdfDocument.finishPage(page)

                    // Write to file
                    requireContext().contentResolver.openOutputStream(fileUri)?.use { outputStream
                        ->
                        pdfDocument.writeTo(outputStream)
                    }
                    pdfDocument.close()
                }

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
        val dialog =
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Initialize views
        val actvBookSelection =
                dialogView.findViewById<android.widget.AutoCompleteTextView>(
                        R.id.actv_book_selection
                )
        val rgBackupFormat =
                dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_backup_format)
        val tvBackupPreview =
                dialogView.findViewById<android.widget.TextView>(R.id.tv_backup_preview)
        val btnCancel =
                dialogView.findViewById<com.google.android.material.button.MaterialButton>(
                        R.id.btn_cancel
                )
        val btnGenerate =
                dialogView.findViewById<com.google.android.material.button.MaterialButton>(
                        R.id.btn_generate
                )

        // Load books for selection
        viewLifecycleOwner.lifecycleScope.launch {
            val books = viewModel.getAllBooksForSelection()
            val activeBookId = viewModel.getActiveBookId()
            val bookNames = mutableListOf("Semua Buku")
            bookNames.addAll(
                    books.map { book ->
                        if (book.id == activeBookId) "${book.name} ✓ Aktif" else book.name
                    }
            )

            val adapter =
                    android.widget.ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            bookNames
                    )
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

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnGenerate.setOnClickListener {
            val selectedFormat =
                    when (rgBackupFormat.checkedRadioButtonId) {
                        R.id.rb_json -> "json"
                        R.id.rb_sql -> "sql"
                        else -> "json"
                    }

            val selectedBookText = actvBookSelection.text.toString()
            val selectedBookId =
                    if (selectedBookText == "Semua Buku") null
                    else {
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
        textView.text =
                "${preview.bookName}\n" +
                        "Transaksi: ${preview.transactionCount}\n" +
                        "Kategori: ${preview.categoryCount}\n" +
                        "Dompet: ${preview.walletCount}"
    }

    private fun showRestoreDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_restore, null)
        val dialog =
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnCancel =
                dialogView.findViewById<com.google.android.material.button.MaterialButton>(
                        R.id.btn_cancel
                )
        val btnChooseFile =
                dialogView.findViewById<com.google.android.material.button.MaterialButton>(
                        R.id.btn_choose_file
                )

        btnCancel.setOnClickListener { dialog.dismiss() }

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
