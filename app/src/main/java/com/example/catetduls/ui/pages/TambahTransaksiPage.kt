package com.example.catetduls.ui.pages

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.catetduls.R
import com.example.catetduls.data.Category
import com.example.catetduls.data.TransactionType
import com.example.catetduls.data.Wallet
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.data.getWalletRepository
import com.example.catetduls.viewmodel.TambahViewModel
import com.example.catetduls.viewmodel.TambahViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class TambahTransaksiPage : Fragment() {

    private lateinit var viewModel: TambahViewModel

    // Views
    private lateinit var radioGroupType: RadioGroup
    private lateinit var radioPemasukan: RadioButton
    private lateinit var radioPengeluaran: RadioButton
    private lateinit var tvLabelPemasukan: TextView
    private lateinit var tvLabelPengeluaran: TextView
    private lateinit var tvIconPemasukan: TextView
    private lateinit var tvIconPengeluaran: TextView
    private lateinit var cardPemasukan: MaterialCardView
    private lateinit var cardPengeluaran: MaterialCardView
    private lateinit var spinnerKategori: Spinner
    private lateinit var spinnerWallet: Spinner // ✅ Spinner Wallet
    private lateinit var etAmount: TextInputEditText
    private lateinit var tvDate: TextView
    private lateinit var cardSelectDate: MaterialCardView
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnSimpan: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var progressBar: ProgressBar

    // Views BARU untuk Foto dan Lanjut
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var ivProofImage: ImageView
    private lateinit var btnLanjut: MaterialButton

    private lateinit var tvHeaderTitle: TextView

    private var categoriesList: List<Category> = emptyList()
    private var walletsList: List<Wallet> = emptyList() // ✅ List Dompet
    private var imageUri: Uri? = null // Menyimpan URI gambar

    // Launcher untuk Galeri
    private val galleryLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                setImageProof(it)
            }
        }

    // Launcher untuk Kamera
    private val cameraLauncher: ActivityResultLauncher<Uri> =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                imageUri?.let { setImageProof(it) }
            } else {
                imageUri = null
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Izin diberikan, langsung buka kamera
                startCameraProcess()
            } else {
                Toast.makeText(requireContext(), "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tambah_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionRepo = requireContext().getTransactionRepository()
        val categoryRepo = requireContext().getCategoryRepository()
        val walletRepo = requireContext().getWalletRepository()

        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val activeWalletId: Int = try {
            prefs.getInt("active_wallet_id", 1)
        } catch (e: Exception) {
            1
        }

        val activeBookId: Int = try {
            prefs.getInt("active_book_id", 1)
        } catch (e: Exception) {
            1
        }

        // Perbarui Factory:
        val factory = TambahViewModelFactory(
            transactionRepo,
            categoryRepo,
            walletRepo,
            activeWalletId,
            activeBookId
        )
        viewModel = ViewModelProvider(this, factory)[TambahViewModel::class.java]

        initViews(view)

        val transactionId = arguments?.getInt("ARG_TRANSACTION_ID", -1) ?: -1

        if (transactionId != -1) {
            // MODE EDIT
            // 1. Ubah Judul Halaman
            val tvTitle = view.findViewById<TextView>(R.id.tv_header_title) // Pastikan ID ini ada di XML header
            tvTitle?.text = "Edit Transaksi"
            btnSimpan.text = "Update Transaksi"

            // 2. Load Data dari Database ke ViewModel
            viewModel.loadTransaction(transactionId)
        }

        setupRadioGroup()
        setupDatePicker()
        setupPhotoPicker()
        setupButtons()
        // setupWalletSpinner() // Dihapus karena listener dipasang di observeData()



        observeData()
    }

    companion object {
        fun newInstance(transactionId: Int? = null): TambahTransaksiPage {
            val fragment = TambahTransaksiPage()
            if (transactionId != null) {
                val args = Bundle()
                args.putInt("ARG_TRANSACTION_ID", transactionId)
                fragment.arguments = args
            }
            return fragment
        }
    }

    /**
     * Langkah 1: Cek apakah izin sudah diberikan
     */
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                // Izin sudah ada -> Buka Kamera
                startCameraProcess()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                // User pernah menolak, beri penjelasan (Opsional, bisa langsung minta lagi)
                Toast.makeText(requireContext(), "Aplikasi butuh akses kamera untuk memotret bukti transaksi.", Toast.LENGTH_LONG).show()
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            else -> {
                // Belum pernah minta -> Minta Izin
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Langkah 2: Persiapkan File Uri dan Luncurkan Intent Kamera
     */
    private fun startCameraProcess() {
        try {
            val context = requireContext()

            // 1. Buat file temporary di cache untuk menampung hasil foto
            // File ini hanya sementara sebelum nanti disalin ke internal storage saat tombol Simpan ditekan
            val photoFile = java.io.File.createTempFile(
                "IMG_TEMP_",  /* prefix */
                ".jpg",       /* suffix */
                context.cacheDir /* directory */
            )

            // 2. Dapatkan URI menggunakan FileProvider (WAJIB COCOK DENGAN MANIFEST)
            // Authority: com.example.catetduls.fileprovider (sesuaikan dengan package name Anda)
            val authority = "${context.packageName}.fileprovider"

            // Simpan ke variabel lokal 'uri' (tipe: Uri, bukan Uri?)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                authority,
                photoFile
            )

            imageUri = uri

            // 3. Luncurkan Kamera
            cameraLauncher.launch(uri)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews(view: View) {
        radioGroupType = view.findViewById(R.id.radio_group_type)
        radioPemasukan = view.findViewById(R.id.radio_pemasukan)
        radioPengeluaran = view.findViewById(R.id.radio_pengeluaran)
        tvLabelPemasukan = view.findViewById(R.id.tv_label_pemasukan)
        tvLabelPengeluaran = view.findViewById(R.id.tv_label_pengeluaran)
        tvIconPemasukan = view.findViewById(R.id.tv_icon_pemasukan)
        tvIconPengeluaran = view.findViewById(R.id.tv_icon_pengeluaran)
        cardPemasukan = view.findViewById(R.id.card_pemasukan)
        cardPengeluaran = view.findViewById(R.id.card_pengeluaran)
        spinnerKategori = view.findViewById(R.id.spinner_kategori)
        spinnerWallet = view.findViewById(R.id.spinner_wallet)
        etAmount = view.findViewById(R.id.et_amount)
        tvDate = view.findViewById(R.id.tv_date)

        cardSelectDate = view.findViewById(R.id.card_select_date)

        etNotes = view.findViewById(R.id.et_notes)
        btnSimpan = view.findViewById(R.id.btn_simpan)
        btnReset = view.findViewById(R.id.btn_reset)
        progressBar = view.findViewById(R.id.progress_bar)

        btnAddPhoto = view.findViewById(R.id.btn_add_photo)
        ivProofImage = view.findViewById(R.id.iv_proof_image)
        btnLanjut = view.findViewById(R.id.btn_lanjut)
    }

    private fun setupWalletSpinnerListener(wallets: List<Wallet>) {
        spinnerWallet.onItemSelectedListener = null

        spinnerWallet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < wallets.size) {
                    val selectedWalletId = wallets[position].id
                    viewModel.setWallet(selectedWalletId)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    private fun setupPhotoPicker() {
        btnAddPhoto.setOnClickListener {
            if (imageUri == null) {
                showImageSourceDialog()
            } else {
                showClearImageDialog()
            }
        }

        ivProofImage.setOnClickListener {
            if (imageUri != null) {
                showClearImageDialog()
            }
        }
        resetImageProof()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Ambil Foto", "Pilih dari Galeri")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Pilih Sumber Foto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun launchCamera() {
        val context = requireContext()
        val filesDir = context.getExternalFilesDir(null)
        val imageFile = java.io.File(filesDir, "temp_photo_${System.currentTimeMillis()}.jpg")

        try {
            imageUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            imageUri?.let { uri ->
                cameraLauncher.launch(uri)
            }

        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, "Error: FileProvider belum dikonfigurasi.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun showClearImageDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Hapus Foto")
            .setMessage("Apakah Anda ingin menghapus bukti foto ini?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                resetImageProof()
                Toast.makeText(requireContext(), "Foto dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setImageProof(uri: Uri) {
        imageUri = uri
        ivProofImage.setImageURI(uri)
        ivProofImage.visibility = View.VISIBLE
        btnAddPhoto.setText("Hapus Foto")
        btnAddPhoto.setIconResource(R.drawable.ic_baseline_close_24)
    }

    private fun resetImageProof() {
        imageUri = null
        ivProofImage.visibility = View.GONE
        ivProofImage.setImageResource(0)
        btnAddPhoto.setText("Tambah Foto")
        btnAddPhoto.setIconResource(R.drawable.ic_photo_24)
        viewModel.setImagePath(null)
    }

    private fun setupRadioGroup() {
        val cardPemasukan = view?.findViewById<MaterialCardView>(R.id.card_pemasukan)
        val cardPengeluaran = view?.findViewById<MaterialCardView>(R.id.card_pengeluaran)

        val typeClickListener = View.OnClickListener { v ->
            val type = if (v.id == R.id.card_pemasukan) TransactionType.PEMASUKAN else TransactionType.PENGELUARAN
            viewModel.setType(type)
            updateCardSelection()
            btnLanjut.visibility = View.GONE
        }
        cardPemasukan?.setOnClickListener(typeClickListener)
        cardPengeluaran?.setOnClickListener(typeClickListener)

        radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            val type = if (checkedId == R.id.radio_pemasukan) TransactionType.PEMASUKAN else TransactionType.PENGELUARAN
            viewModel.setType(type)
            updateCardSelection()
            btnLanjut.visibility = View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedType.collect { type ->
                if (type == TransactionType.PEMASUKAN) {
                    radioPemasukan.isChecked = true
                    radioPengeluaran.isChecked = false
                } else {
                    radioPengeluaran.isChecked = true
                    radioPemasukan.isChecked = false
                }
                updateCardSelection()
            }
        }
        updateCardSelection()
    }

    private fun updateCardSelection() {
        val cardPemasukan = view?.findViewById<MaterialCardView>(R.id.card_pemasukan)
        val cardPengeluaran = view?.findViewById<MaterialCardView>(R.id.card_pengeluaran)
        val tvLabelPemasukan = view?.findViewById<TextView>(R.id.tv_label_pemasukan)
        val tvLabelPengeluaran = view?.findViewById<TextView>(R.id.tv_label_pengeluaran)
        val tvIconPemasukan = view?.findViewById<TextView>(R.id.tv_icon_pemasukan)
        val tvIconPengeluaran = view?.findViewById<TextView>(R.id.tv_icon_pengeluaran)

        val colorSuccess = ContextCompat.getColor(requireContext(), R.color.success)
        val colorDanger = ContextCompat.getColor(requireContext(), R.color.danger)
        val colorBorder = ContextCompat.getColor(requireContext(), R.color.border)

        val isPemasukanChecked = radioPemasukan.isChecked

        if (isPemasukanChecked) {
            cardPemasukan?.apply {
                strokeColor = colorSuccess
                strokeWidth = 3
                cardElevation = 4f
            }
            tvLabelPemasukan?.setTypeface(null, android.graphics.Typeface.BOLD)
            tvIconPemasukan?.setTypeface(null, android.graphics.Typeface.BOLD)

            cardPengeluaran?.apply {
                strokeColor = colorBorder
                strokeWidth = 1
                cardElevation = 2f
            }
            tvLabelPengeluaran?.setTypeface(null, android.graphics.Typeface.NORMAL)
            tvIconPengeluaran?.setTypeface(null, android.graphics.Typeface.NORMAL)

        } else {
            cardPengeluaran?.apply {
                strokeColor = colorDanger
                strokeWidth = 3
                cardElevation = 4f
            }
            tvLabelPengeluaran?.setTypeface(null, android.graphics.Typeface.BOLD)
            tvIconPengeluaran?.setTypeface(null, android.graphics.Typeface.BOLD)

            cardPemasukan?.apply {
                strokeColor = colorBorder
                strokeWidth = 1
                cardElevation = 2f
            }
            tvLabelPemasukan?.setTypeface(null, android.graphics.Typeface.NORMAL)
            tvIconPemasukan?.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }


    private fun setupDatePicker() {
        cardSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = viewModel.date.value

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    viewModel.setDate(calendar.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupButtons() {
        btnSimpan.setOnClickListener {
            btnLanjut.visibility = View.GONE

            val amount = etAmount.text.toString()
            val notes = etNotes.text.toString()

            android.util.Log.d("DEBUG_FOTO", "=== SAVE BUTTON CLICKED ===")
            android.util.Log.d("DEBUG_FOTO", "Current imageUri: $imageUri")
            android.util.Log.d("DEBUG_FOTO", "Current ViewModel imagePath: ${viewModel.imagePath.value}")

            // ✅ LOGIKA BARU YANG LEBIH SEDERHANA
            if (imageUri != null) {
                val currentPath = viewModel.imagePath.value

                // Cek apakah ini foto BARU atau foto LAMA
                if (currentPath != null) {
                    // Kasus A: Foto lama masih ada (mode edit, tidak ganti foto)
                    val file = java.io.File(currentPath)
                    if (file.exists()) {
                        android.util.Log.d("DEBUG_FOTO", "Using existing photo from: $currentPath")
                        // Tidak perlu set lagi, ViewModel sudah punya path-nya
                    } else {
                        // File hilang? Save foto baru
                        android.util.Log.d("DEBUG_FOTO", "Old file missing, saving new photo")
                        val newPath = saveImageToInternalStorage(imageUri!!)
                        viewModel.setImagePath(newPath)
                    }
                } else {
                    // Kasus B: Foto baru (dari gallery/camera)
                    android.util.Log.d("DEBUG_FOTO", "New photo detected, saving to internal storage")
                    val newPath = saveImageToInternalStorage(imageUri!!)

                    if (newPath != null) {
                        android.util.Log.d("DEBUG_FOTO", "✅ Photo saved to: $newPath")
                        viewModel.setImagePath(newPath)
                    } else {
                        android.util.Log.e("DEBUG_FOTO", "❌ Failed to save photo!")
                        viewModel.setImagePath(null)
                        Toast.makeText(requireContext(), "Gagal menyimpan foto", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Kasus C: Tidak ada foto
                android.util.Log.d("DEBUG_FOTO", "No photo selected")
                viewModel.setImagePath(null)
            }

            // Set data lain
            viewModel.setAmount(amount)
            viewModel.setNotes(notes)

            // Validasi & Simpan
            if (viewModel.isFormValid()) {
                android.util.Log.d("DEBUG_FOTO", "Form valid, saving transaction...")
                android.util.Log.d("DEBUG_FOTO", "Final imagePath: ${viewModel.imagePath.value}")
                viewModel.saveTransaction()
            } else {
                Toast.makeText(requireContext(), "Form belum lengkap", Toast.LENGTH_SHORT).show()
            }
        }

        btnLanjut.setOnClickListener {
            // Reset form, KEEP TYPE, dan set Tanggal Hari Ini
            viewModel.resetForm(keepType = true, keepCategory = false, setTodayDate = true)

            btnLanjut.visibility = View.GONE
            resetImageProof()

            etAmount.requestFocus()
            Toast.makeText(requireContext(), "Siap mencatat transaksi ${viewModel.selectedType.value} berikutnya!", Toast.LENGTH_SHORT).show()
        }

        btnReset.setOnClickListener {
            viewModel.resetForm(keepType = false, keepCategory = false, setTodayDate = true)
            btnLanjut.visibility = View.GONE
            resetImageProof()
        }
    }

    private fun observeData() {

        // 1. OBSERVE UNTUK STATEFLOW
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe tanggal
                launch {
                    viewModel.date.collect { timestamp ->
                        tvDate.text = viewModel.formatDate(timestamp)
                    }
                }

                launch {
                    viewModel.amount.collect { amountString ->
                        // HINDARI LOOP UPDATE: Hanya update jika beda dengan yang diketik
                        if (etAmount.text.toString() != amountString) {

                            // --- PERBAIKAN LOGIKA FORMATTING ---
                            // Jika amountString berisi notasi ilmiah (E9), kita konversi dulu ke Long/BigDecimal
                            val cleanString = if (amountString.contains("E")) {
                                try {
                                    val doubleVal = amountString.toDouble()
                                    // Ubah ke format string biasa (tanpa koma/titik ribuan dulu agar EditText bisa baca)
                                    java.math.BigDecimal(doubleVal).toPlainString().replace(".0", "")
                                } catch (e: Exception) {
                                    amountString
                                }
                            } else {
                                amountString
                            }

                            etAmount.setText(cleanString)
                            etAmount.setSelection(cleanString.length) // Cursor di akhir
                        }
                    }
                }

                // Observe amount & notes untuk reset
//                launch { viewModel.amount.collect { if (it.isEmpty() && etAmount.text.toString().isNotEmpty()) etAmount.text?.clear() } }
                launch { viewModel.notes.collect { if (it.isEmpty() && etNotes.text.toString().isNotEmpty()) etNotes.text?.clear() } }

                // Observe loading state
                launch {
                    viewModel.isSaving.collect { isSaving ->
                        progressBar.visibility = if (isSaving) View.VISIBLE else View.GONE
                        btnSimpan.isEnabled = !isSaving
                        btnReset.isEnabled = !isSaving
                        btnLanjut.isEnabled = !isSaving
                    }
                }

                // Observe success message
                launch {
                    viewModel.successMessage.collect { message ->
                        message?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()

                            // Setelah Sukses: Reset form, tampilkan tombol Lanjut
                            viewModel.resetForm(keepType = true, keepCategory = false, setTodayDate = true)

                            btnLanjut.visibility = View.VISIBLE
                            resetImageProof()

                            viewModel.clearMessages()
                        }
                    }
                }

                // Observe error message
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.clearMessages()
                        }
                    }
                }

                launch {
                    viewModel.imagePath.collect { path ->
                        android.util.Log.d("DEBUG_FOTO", "=== IMAGE PATH CHANGED ===")
                        android.util.Log.d("DEBUG_FOTO", "New path from ViewModel: $path")

                        if (path != null) {
                            val file = java.io.File(path)
                            android.util.Log.d("DEBUG_FOTO", "File exists: ${file.exists()}")
                            android.util.Log.d("DEBUG_FOTO", "File size: ${file.length()} bytes")

                            if (file.exists()) {
                                // ✅ PERBAIKAN: Set imageUri dari file path
                                val uri = Uri.fromFile(file)
                                imageUri = uri

                                // Tampilkan gambar
                                ivProofImage.setImageURI(uri)
                                ivProofImage.visibility = View.VISIBLE
                                btnAddPhoto.setText("Hapus Foto")
                                btnAddPhoto.setIconResource(R.drawable.ic_baseline_close_24)

                                android.util.Log.d("DEBUG_FOTO", "✅ Image displayed successfully!")
                            } else {
                                android.util.Log.e("DEBUG_FOTO", "❌ File not found at path: $path")
                                resetImageProof()
                            }
                        } else {
                            // Path null = tidak ada gambar
                            android.util.Log.d("DEBUG_FOTO", "Path is null, resetting image")
                            if (imageUri == null) {
                                resetImageProof()
                            }
                        }
                    }
                }
            }
        }

        // 2. OBSERVE UNTUK LIVE DATA (Categories)
        viewModel.categories.observe(viewLifecycleOwner) { categories: List<Category> ->
            categoriesList = categories

            if (categories.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada kategori tersedia!", Toast.LENGTH_LONG).show()
                return@observe
            }

            val categoryNames = categories.map { "${it.icon} ${it.name}" }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerKategori.adapter = adapter

            // Pilih kategori pertama jika belum ada yang dipilih
            var selectedIndex = 0
            val currentId = viewModel.selectedCategoryId.value
            if (currentId != null) {
                val existingIndex = categories.indexOfFirst { it.id == currentId }
                if (existingIndex != -1) {
                    selectedIndex = existingIndex
                }
            }

            // Penting: Nonaktifkan listener sementara
            spinnerKategori.onItemSelectedListener = null
            spinnerKategori.setSelection(selectedIndex, false)
            if (categories.isNotEmpty() && selectedIndex >= 0) {
                viewModel.setCategory(categories[selectedIndex].id)
            }
            // Set listener baru
            spinnerKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position < categories.size) {
                        viewModel.setCategory(categories[position].id)
                        btnLanjut.visibility = View.GONE
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Biarkan kosong
                }
            }
        }

        // 3. OBSERVE UNTUK LIVE DATA (Wallets)
        viewModel.wallets.observe(viewLifecycleOwner) { wallets: List<Wallet> ->
            walletsList = wallets

            if (wallets.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada dompet tersedia untuk Buku ini!", Toast.LENGTH_LONG).show()
                return@observe
            }

            val walletNames = wallets.map { "${it.icon} ${it.name}" }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                walletNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerWallet.adapter = adapter

            // Pilih dompet aktif default
            var selectedIndex = 0
            val currentId = viewModel.selectedWalletId.value
            if (currentId > 0) {
                val existingIndex = wallets.indexOfFirst { it.id == currentId }
                if (existingIndex != -1) {
                    selectedIndex = existingIndex
                }
            }

            // Set pilihan default
            spinnerWallet.setSelection(selectedIndex, false)
            if (wallets.isNotEmpty() && selectedIndex >= 0) {
                viewModel.setWallet(wallets[selectedIndex].id)
            }
            // Pasang listener di sini (setelah selection)
            setupWalletSpinnerListener(walletsList)
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        android.util.Log.d("DEBUG_FOTO", "=== SAVING IMAGE ===")
        android.util.Log.d("DEBUG_FOTO", "Source URI: $uri")

        return try {
            val context = requireContext()

            // 1. Buka InputStream
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                android.util.Log.e("DEBUG_FOTO", "❌ Cannot open InputStream")
                return null
            }

            // 2. Buat folder tujuan
            val directory = java.io.File(context.filesDir, "transaction_images")
            if (!directory.exists()) {
                val created = directory.mkdirs()
                android.util.Log.d("DEBUG_FOTO", "Directory created: $created")
            }

            // 3. Generate nama file unique
            val fileName = "IMG_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"
            val file = java.io.File(directory, fileName)

            android.util.Log.d("DEBUG_FOTO", "Target file: ${file.absolutePath}")

            // 4. Copy stream
            val outputStream = java.io.FileOutputStream(file)
            val bytesCopied = inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            // 5. Verify
            if (file.exists() && file.length() > 0) {
                android.util.Log.d("DEBUG_FOTO", "✅ SUCCESS!")
                android.util.Log.d("DEBUG_FOTO", "   Path: ${file.absolutePath}")
                android.util.Log.d("DEBUG_FOTO", "   Size: ${file.length()} bytes")
                android.util.Log.d("DEBUG_FOTO", "   Copied: $bytesCopied bytes")

                file.absolutePath
            } else {
                android.util.Log.e("DEBUG_FOTO", "❌ File saved but invalid")
                null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("DEBUG_FOTO", "❌ EXCEPTION: ${e.message}")
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}