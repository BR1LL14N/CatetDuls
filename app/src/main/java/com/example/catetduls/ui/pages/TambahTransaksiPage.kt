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
import com.example.catetduls.data.getBookRepository
import com.example.catetduls.data.getCategoryRepository
import com.example.catetduls.data.getTransactionRepository
import com.example.catetduls.data.getWalletRepository
import com.example.catetduls.viewmodel.TambahViewModel
import com.example.catetduls.viewmodel.TambahViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.util.*
import kotlinx.coroutines.launch

class TambahTransaksiPage : Fragment() {

    private lateinit var viewModel: TambahViewModel

    // Views
    private lateinit var radioGroupType: RadioGroup
    private lateinit var radioPemasukan: RadioButton
    private lateinit var radioPengeluaran: RadioButton
    private lateinit var radioTransfer: RadioButton
    private lateinit var tvLabelPemasukan: TextView
    private lateinit var tvLabelPengeluaran: TextView
    private lateinit var tvLabelTransfer: TextView
    private lateinit var tvIconPemasukan: TextView
    private lateinit var tvIconPengeluaran: TextView
    private lateinit var tvIconTransfer: TextView
    private lateinit var cardPemasukan: MaterialCardView
    private lateinit var cardPengeluaran: MaterialCardView
    private lateinit var cardTransfer: MaterialCardView

    // Views Kategori
    private lateinit var tvLabelKategori: TextView
    private lateinit var cardKategori: MaterialCardView
    private lateinit var spinnerKategori: Spinner

    // Views Dompet
    private lateinit var tvLabelWalletSource: TextView
    private lateinit var spinnerWallet: Spinner // Dompet Sumber
    private lateinit var tvLabelWalletTarget: TextView
    private lateinit var cardWalletTarget: MaterialCardView
    private lateinit var spinnerWalletTarget: Spinner // Dompet Tujuan

    private lateinit var etAmount: EditText
    private lateinit var tvCurrencySymbol: TextView
    private lateinit var tvDate: TextView
    private lateinit var cardSelectDate: MaterialCardView
    private lateinit var etNotes: com.google.android.material.textfield.TextInputEditText
    private lateinit var btnSimpan: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var progressBar: ProgressBar

    // Photo Views
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var ivProofImage: ImageView
    private lateinit var btnLanjut: MaterialButton

    private lateinit var tvHeaderTitle: TextView

    private var categoriesList: List<Category> = emptyList()
    private var walletsList: List<Wallet> = emptyList()
    private var imageUri: Uri? = null

    // Calculator State
    private var calculatorDialog: BottomSheetDialog? = null
    private var currentInput = ""
    private var operator = ""
    private var firstValue = ""
    private var isNewInput = true
    private var isCalculated = false

    // Launcher untuk Galeri
    private val galleryLauncher: ActivityResultLauncher<String> =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let { setImageProof(it) }
            }

    // Launcher untuk Kamera
    private val cameraLauncher: ActivityResultLauncher<Uri> =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
                if (success) {
                    imageUri?.let { setImageProof(it) }
                } else {
                    // Jika pengambilan gambar gagal atau dibatalkan, reset imageUri jika itu adalah
                    // temp uri
                    if (imageUri?.scheme == "content" &&
                                    imageUri?.authority?.endsWith(".fileprovider") == true
                    ) {
                        // Hapus file sementara jika ada
                        try {
                            requireContext().contentResolver.delete(imageUri!!, null, null)
                        } catch (_: Exception) {}
                    }
                    imageUri = null
                }
            }

    private val requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted: Boolean ->
                if (isGranted) {
                    startCameraProcess()
                } else {
                    Toast.makeText(
                                    requireContext(),
                                    "Izin kamera diperlukan untuk mengambil foto",
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
        return inflater.inflate(R.layout.fragment_tambah_transaksi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionRepo = requireContext().getTransactionRepository()
        val categoryRepo = requireContext().getCategoryRepository()
        val walletRepo = requireContext().getWalletRepository()

        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val activeWalletId: Int =
                try {
                    prefs.getInt("active_wallet_id", 1)
                } catch (e: Exception) {
                    1
                }

        val activeBookId: Int =
                try {
                    prefs.getInt("active_book_id", 1)
                } catch (e: Exception) {
                    1
                }

        val factory =
                TambahViewModelFactory(
                        transactionRepo,
                        categoryRepo,
                        walletRepo,
                        requireContext().getBookRepository(), // Add this
                        activeWalletId,
                        activeBookId
                )
        viewModel = ViewModelProvider(this, factory)[TambahViewModel::class.java]

        initViews(view)

        // --- Update Currency Symbol ---
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bookSame = requireContext().getBookRepository().getBookByIdSync(activeBookId)
                if (bookSame != null) {
                    tvCurrencySymbol.text = bookSame.currencySymbol
                }
            } catch (e: Exception) {
                // Ignore, default to Rp (already set in XML)
            }
        }

        val transactionId = arguments?.getInt("ARG_TRANSACTION_ID", -1) ?: -1

        if (transactionId != -1) {
            tvHeaderTitle.text = "Edit Transaksi"
            btnSimpan.text = "Update Transaksi"

            // Nonaktifkan pengeditan transfer
            if (viewModel.selectedType.value == TransactionType.TRANSFER) {
                Toast.makeText(
                                requireContext(),
                                "Edit Transfer tidak didukung. Hapus dan buat baru.",
                                Toast.LENGTH_LONG
                        )
                        .show()
                btnSimpan.isEnabled = false
            }

            viewModel.loadTransaction(transactionId)
        } else {
            tvHeaderTitle.text = "Tambah Transaksi"
            btnSimpan.text = "Simpan Transaksi"
        }

        setupRadioGroup()
        setupDatePicker()
        setupPhotoPicker()
        setupAmountInput()
        setupButtons()

        // Inisialisasi awal UI berdasarkan ViewModel state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedType.collect { updateCardSelection() }
        }

        observeData()
    }

    companion object {
        fun newInstance(transactionId: Int? = null): TambahTransaksiPage {
            val fragment = TambahTransaksiPage()
            if (transactionId != null && transactionId != -1) {
                val args = Bundle()
                args.putInt("ARG_TRANSACTION_ID", transactionId)
                fragment.arguments = args
            }
            return fragment
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                startCameraProcess()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                Toast.makeText(
                                requireContext(),
                                "Aplikasi butuh akses kamera untuk memotret bukti transaksi.",
                                Toast.LENGTH_LONG
                        )
                        .show()
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCameraProcess() {
        try {
            val context = requireContext()
            // Menggunakan File.createTempFile untuk memastikan file sementara
            val photoFile = File.createTempFile("IMG_TEMP_", ".jpg", context.cacheDir)

            val authority = "${context.packageName}.fileprovider"
            val uri =
                    androidx.core.content.FileProvider.getUriForFile(context, authority, photoFile)

            imageUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                            requireContext(),
                            "Gagal membuka kamera: ${e.message}",
                            Toast.LENGTH_SHORT
                    )
                    .show()
        }
    }

    private fun initViews(view: View) {
        // Inisialisasi View
        radioGroupType = view.findViewById(R.id.radio_group_type)
        radioPemasukan = view.findViewById(R.id.radio_pemasukan)
        radioPengeluaran = view.findViewById(R.id.radio_pengeluaran)
        radioTransfer = view.findViewById(R.id.radio_transfer)

        tvLabelPemasukan = view.findViewById(R.id.tv_label_pemasukan)
        tvLabelPengeluaran = view.findViewById(R.id.tv_label_pengeluaran)
        tvLabelTransfer = view.findViewById(R.id.tv_label_transfer)

        tvIconPemasukan = view.findViewById(R.id.tv_icon_pemasukan)
        tvIconPengeluaran = view.findViewById(R.id.tv_icon_pengeluaran)
        tvIconTransfer = view.findViewById(R.id.tv_icon_transfer)

        cardPemasukan = view.findViewById(R.id.card_pemasukan)
        cardPengeluaran = view.findViewById(R.id.card_pengeluaran)
        cardTransfer = view.findViewById(R.id.card_transfer)

        // Kategori Views
        tvLabelKategori = view.findViewById(R.id.tv_label_kategori)
        cardKategori = view.findViewById(R.id.card_kategori)
        spinnerKategori = view.findViewById(R.id.spinner_kategori)

        // Dompet Views
        tvLabelWalletSource = view.findViewById(R.id.tv_label_wallet_source)
        spinnerWallet = view.findViewById(R.id.spinner_wallet) // SUMBER
        tvLabelWalletTarget = view.findViewById(R.id.tv_label_wallet_target)
        cardWalletTarget = view.findViewById(R.id.card_wallet_target)
        spinnerWalletTarget = view.findViewById(R.id.spinner_wallet_target) // TUJUAN

        etAmount = view.findViewById(R.id.et_amount)
        tvCurrencySymbol = view.findViewById(R.id.tv_currency_symbol) // Initialize Currency Symbol
        tvDate = view.findViewById(R.id.tv_date)
        cardSelectDate = view.findViewById(R.id.card_select_date)
        etNotes = view.findViewById(R.id.et_notes)
        btnSimpan = view.findViewById(R.id.btn_simpan)
        btnReset = view.findViewById(R.id.btn_reset)
        progressBar = view.findViewById(R.id.progress_bar)

        btnAddPhoto = view.findViewById(R.id.btn_add_photo)
        ivProofImage = view.findViewById(R.id.iv_proof_image)
        btnLanjut = view.findViewById(R.id.btn_lanjut)

        tvHeaderTitle = view.findViewById(R.id.tv_header_title)
    }

    private fun setupAmountInput() {
        // Menonaktifkan keyboard saat diklik, dan memunculkan kalkulator
        etAmount.apply {
            keyListener = null
            isFocusable = false
            isFocusableInTouchMode = false
            setOnClickListener { showCalculatorDialog() }
        }
    }

    private fun showCalculatorDialog() {
        // Ambil nilai yang sudah ada dari ViewModel/EditText, bersihkan dari pemisah
        // ALLOW COMMA
        val cleanAmount = viewModel.amount.value.replace(Regex("[^0-9,]"), "")

        // Reset state kalkulator
        currentInput = if (cleanAmount.isNotEmpty() && (cleanAmount != "0")) cleanAmount else "0"
        operator = ""
        firstValue = ""
        isNewInput = true
        isCalculated = false

        val dialogView = layoutInflater.inflate(R.layout.dialog_calculator, null)
        calculatorDialog = BottomSheetDialog(requireContext())
        calculatorDialog?.setContentView(dialogView)

        val tvDisplay = dialogView.findViewById<TextView>(R.id.tv_calculator_display)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btn_close_calculator)
        val btnDone = dialogView.findViewById<MaterialButton>(R.id.btn_done_calculator)

        tvDisplay.text = formatAmount(currentInput)

        // Number buttons
        val numberButtons =
                mapOf(
                        R.id.calc_btn_0 to "0",
                        R.id.calc_btn_1 to "1",
                        R.id.calc_btn_2 to "2",
                        R.id.calc_btn_3 to "3",
                        R.id.calc_btn_4 to "4",
                        R.id.calc_btn_5 to "5",
                        R.id.calc_btn_6 to "6",
                        R.id.calc_btn_7 to "7",
                        R.id.calc_btn_8 to "8",
                        R.id.calc_btn_9 to "9"
                )

        numberButtons.forEach { (id, number) ->
            dialogView.findViewById<MaterialButton>(id)?.setOnClickListener {
                handleNumberClick(number, tvDisplay)
            }
        }

        // Decimal button
        dialogView.findViewById<MaterialButton>(R.id.calc_btn_decimal)?.setOnClickListener {
            handleDecimalClick(tvDisplay)
        }

        // Operator buttons
        dialogView.findViewById<MaterialButton>(R.id.calc_btn_add)?.setOnClickListener {
            handleOperatorClick("+", tvDisplay)
        }
        dialogView.findViewById<MaterialButton>(R.id.calc_btn_subtract)?.setOnClickListener {
            handleOperatorClick("-", tvDisplay)
        }
        dialogView.findViewById<MaterialButton>(R.id.calc_btn_multiply)?.setOnClickListener {
            handleOperatorClick("×", tvDisplay)
        }
        dialogView.findViewById<MaterialButton>(R.id.calc_btn_divide)?.setOnClickListener {
            handleOperatorClick("÷", tvDisplay)
        }

        // Backspace button
        dialogView.findViewById<MaterialButton>(R.id.calc_btn_backspace)?.setOnClickListener {
            handleBackspace(tvDisplay)
        }

        // Close button
        btnClose?.setOnClickListener { calculatorDialog?.dismiss() }

        // Done button (Selesai)
        btnDone?.setOnClickListener {
            // Hitung hasil jika ada operasi yang tertunda
            if (operator.isNotEmpty() && firstValue.isNotEmpty() && !isNewInput) {
                calculateResult()
            }

            // Format result: remove trailing comma if any
            var finalResult = currentInput
            if (finalResult.endsWith(",")) {
                finalResult = finalResult.dropLast(1)
            }

            etAmount.setText(formatAmount(finalResult))
            viewModel.setAmount(finalResult)
            calculatorDialog?.dismiss()
        }

        calculatorDialog?.setOnDismissListener { etAmount.clearFocus() }

        calculatorDialog?.show()
    }

    private fun handleNumberClick(number: String, display: TextView) {
        if (isCalculated) {
            currentInput = "0"
            operator = ""
            firstValue = ""
            isCalculated = false
            isNewInput = true
        }

        if (isNewInput) {
            currentInput = if (number == "0" && currentInput == "0") "0" else number
            isNewInput = false
        } else {
            if (currentInput.replace(",", "").length < 15) {
                if (currentInput == "0") {
                    currentInput = number
                } else {
                    currentInput += number
                }
            }
        }
        updateDisplay(display)
    }

    private fun handleDecimalClick(display: TextView) {
        if (isCalculated) {
            currentInput = "0,"
            operator = ""
            firstValue = ""
            isCalculated = false
            isNewInput = false
        } else if (isNewInput) {
            currentInput = "0,"
            isNewInput = false
        } else {
            if (!currentInput.contains(",")) {
                currentInput += ","
            }
        }
        updateDisplay(display)
    }

    private fun handleOperatorClick(op: String, display: TextView) {
        if (operator.isNotEmpty() && firstValue.isNotEmpty() && !isNewInput) {
            calculateResult()
        }

        firstValue = currentInput
        operator = op
        isNewInput = true
        isCalculated = false
        updateDisplay(display)
    }

    private fun handleBackspace(display: TextView) {
        if (isCalculated) return

        if (currentInput.length > 1) {
            currentInput = currentInput.dropLast(1)
            if (currentInput.isEmpty()) currentInput = "0" // Safety
        } else {
            currentInput = "0"
            isNewInput = true
        }
        updateDisplay(display)
    }

    private fun updateDisplay(display: TextView) {
        val displayValue =
                if (firstValue.isNotEmpty() && operator.isNotEmpty()) {
                    "${formatAmount(firstValue)} $operator ${formatAmount(currentInput)}"
                } else {
                    formatAmount(currentInput)
                }
        display.text = displayValue
    }

    private fun calculateResult(display: TextView? = null) {
        if (firstValue.isEmpty() || operator.isEmpty() || currentInput.isEmpty()) return

        val first = parseToDouble(firstValue)
        val second = parseToDouble(currentInput)

        val result: Double =
                when (operator) {
                    "+" -> first + second
                    "-" -> first - second
                    "×" -> first * second
                    "÷" -> if (second != 0.0) first / second else 0.0
                    else -> second
                }

        // Format result back to String with comma
        // If result is integer (ends with .0), removing decimal part
        currentInput =
                if (result % 1.0 == 0.0) {
                    String.format(Locale("in", "ID"), "%.0f", result)
                } else {
                    // Limit decimals to 2 or 3 digits
                    val formatted =
                            String.format(Locale.US, "%.2f", result) // Use US for dot then replace
                    formatted.replace('.', ',')
                }

        operator = ""
        firstValue = ""
        isNewInput = true
        isCalculated = true

        display?.text = formatAmount(currentInput)
    }

    private fun parseToDouble(value: String): Double {
        // "1.000,50" -> "1000.50"
        var clean = value.replace(".", "") // Remove thousands separator
        clean = clean.replace(",", ".") // Replace decimal separator
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun formatAmount(amount: String): String {
        // Input: "1000,50" -> Output "1.000,50"

        var clean = amount.replace(".", "") // Ensure clean raw string
        val parts = clean.split(",")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else null

        val number = integerPart.toLongOrNull() ?: 0L

        // Use explicit naming for symbols to avoid Locale issues
        val symbols = java.text.DecimalFormatSymbols(Locale.US)
        symbols.groupingSeparator = '.'
        symbols.decimalSeparator = ','
        val decimalFormat = java.text.DecimalFormat("#,###", symbols)

        val formattedInt = decimalFormat.format(number)

        return if (decimalPart != null) {
            "$formattedInt,$decimalPart"
        } else if (clean.endsWith(",")) {
            "$formattedInt,"
        } else {
            formattedInt
        }
    }

    // --- SETUP LISTENERS ---

    private fun setupWalletSpinnerListener(wallets: List<Wallet>) {
        spinnerWallet.onItemSelectedListener = null

        spinnerWallet.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        if (position < wallets.size) {
                            val selectedWalletId = wallets[position].id
                            viewModel.setWallet(selectedWalletId)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
    }

    private fun setupTargetWalletSpinnerListener(wallets: List<Wallet>) {
        spinnerWalletTarget.onItemSelectedListener = null

        spinnerWalletTarget.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                    ) {
                        if (position < wallets.size) {
                            val selectedWalletId = wallets[position].id
                            viewModel.setTargetWallet(selectedWalletId)
                            btnLanjut.visibility = View.GONE
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
        // Pastikan reset dipanggil untuk tampilan awal
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
        // Menggunakan String literal
        btnAddPhoto.setText("Hapus Foto")
        btnAddPhoto.setIconResource(R.drawable.ic_baseline_close_24)
        // Note: viewModel.setImagePath akan di-set saat btnSimpan diklik
    }

    private fun resetImageProof() {
        imageUri = null
        ivProofImage.visibility = View.GONE
        ivProofImage.setImageResource(0)
        // Menggunakan String literal
        btnAddPhoto.setText("Tambah Foto")
        btnAddPhoto.setIconResource(R.drawable.ic_photo_24)
        viewModel.setImagePath(null)
    }

    private fun setupRadioGroup() {
        val typeClickListener =
                View.OnClickListener { v ->
                    val type =
                            when (v.id) {
                                R.id.card_pemasukan -> TransactionType.PEMASUKAN
                                R.id.card_transfer -> TransactionType.TRANSFER
                                R.id.card_pengeluaran -> TransactionType.PENGELUARAN
                                else ->
                                        viewModel
                                                .selectedType
                                                .value // Mempertahankan nilai saat ini jika id
                            // tidak dikenal
                            }

                    // Set radio button berdasarkan klik card
                    when (type) {
                        TransactionType.PEMASUKAN -> radioPemasukan.isChecked = true
                        TransactionType.PENGELUARAN -> radioPengeluaran.isChecked = true
                        TransactionType.TRANSFER -> radioTransfer.isChecked = true
                    }

                    viewModel.setType(type)
                    updateCardSelection()
                    btnLanjut.visibility = View.GONE
                }

        cardPemasukan.setOnClickListener(typeClickListener)
        cardPengeluaran.setOnClickListener(typeClickListener)
        cardTransfer.setOnClickListener(typeClickListener)

        radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            val type =
                    when (checkedId) {
                        R.id.radio_pemasukan -> TransactionType.PEMASUKAN
                        R.id.radio_transfer -> TransactionType.TRANSFER
                        R.id.radio_pengeluaran -> TransactionType.PENGELUARAN
                        else -> TransactionType.PENGELUARAN // Default
                    }
            viewModel.setType(type)
            updateCardSelection()
            btnLanjut.visibility = View.GONE
        }

        // Observasi untuk memastikan radio button dan card diperbarui jika ViewModel berubah
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedType.collect { type ->
                    val checkedId =
                            when (type) {
                                TransactionType.PEMASUKAN -> R.id.radio_pemasukan
                                TransactionType.TRANSFER -> R.id.radio_transfer
                                TransactionType.PENGELUARAN -> R.id.radio_pengeluaran
                            }
                    if (radioGroupType.checkedRadioButtonId != checkedId) {
                        // Mencegah loop tak terbatas dengan hanya mengatur jika berbeda
                        radioGroupType.check(checkedId)
                    }
                    updateCardSelection()
                }
            }
        }
    }

    private fun updateCardSelection() {
        val colorSuccess = ContextCompat.getColor(requireContext(), R.color.success)
        val colorDanger = ContextCompat.getColor(requireContext(), R.color.danger)
        val colorSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        val colorBorder = ContextCompat.getColor(requireContext(), R.color.border)

        val isTransfer = viewModel.selectedType.value == TransactionType.TRANSFER

        // Reset all cards
        listOf(cardPemasukan, cardPengeluaran, cardTransfer).forEach { card ->
            card.apply {
                strokeColor = colorBorder
                strokeWidth = 1
                cardElevation = 2f
            }
        }
        listOf(tvLabelPemasukan, tvLabelPengeluaran, tvLabelTransfer).forEach {
            it.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
        listOf(tvIconPemasukan, tvIconPengeluaran, tvIconTransfer).forEach {
            it.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        // Highlight selected card
        when (viewModel.selectedType.value) {
            TransactionType.PEMASUKAN -> {
                cardPemasukan.apply {
                    strokeColor = colorSuccess
                    strokeWidth = 3
                    cardElevation = 4f
                }
                tvLabelPemasukan.setTypeface(null, android.graphics.Typeface.BOLD)
                tvIconPemasukan.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            TransactionType.TRANSFER -> {
                cardTransfer.apply {
                    strokeColor = colorSecondary
                    strokeWidth = 3
                    cardElevation = 4f
                }
                tvLabelTransfer.setTypeface(null, android.graphics.Typeface.BOLD)
                tvIconTransfer.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            TransactionType.PENGELUARAN -> {
                cardPengeluaran.apply {
                    strokeColor = colorDanger
                    strokeWidth = 3
                    cardElevation = 4f
                }
                tvLabelPengeluaran.setTypeface(null, android.graphics.Typeface.BOLD)
                tvIconPengeluaran.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }

        // Kontrol Visibilitas Kategori
        if (isTransfer) {
            tvLabelKategori.visibility = View.GONE
            cardKategori.visibility = View.GONE
        } else {
            tvLabelKategori.visibility = View.VISIBLE
            cardKategori.visibility = View.VISIBLE
        }

        // Kontrol Visibilitas Dompet Sumber/Tujuan
        if (isTransfer) {
            tvLabelWalletSource.text = "Dompet Sumber"
            tvLabelWalletTarget.visibility = View.VISIBLE
            cardWalletTarget.visibility = View.VISIBLE
        } else {
            tvLabelWalletSource.text = "Dompet / Sumber Dana"
            tvLabelWalletTarget.visibility = View.GONE
            cardWalletTarget.visibility = View.GONE
            // Reset dompet tujuan saat beralih dari Transfer
            viewModel.setTargetWallet(null)
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
                    )
                    .show()
        }
    }

    private fun setupButtons() {
        btnSimpan.setOnClickListener {
            btnLanjut.visibility = View.GONE

            // Ambil input dari UI
            // Ambil input dari UI
            val rawAmount = etAmount.text.toString().replace(Regex("[^0-9,]"), "")
            val notes = etNotes.text.toString()

            // Update ViewModel dengan input terbaru
            viewModel.setAmount(rawAmount)
            viewModel.setNotes(notes)

            // Penanganan Gambar: simpan ke internal storage sebelum menyimpan transaksi
            // PERBAIKAN: Mengganti isEditMode.value dengan pemanggilan fungsi isEditMode()
            if (imageUri != null && viewModel.isEditMode() == false) {
                // Hanya simpan jika ini transaksi baru atau jika uri adalah temp uri dari kamera
                val newPath = saveImageToInternalStorage(imageUri!!)
                if (newPath != null) {
                    viewModel.setImagePath(newPath)
                } else {
                    viewModel.setImagePath(null)
                    Toast.makeText(requireContext(), "Gagal menyimpan foto", Toast.LENGTH_SHORT)
                            .show()
                }
            } else if (imageUri == null) {
                viewModel.setImagePath(null)
            }
            // Jika imageUri != null dan isEditMode = true, asumsi path lama sudah benar

            if (viewModel.isFormValid()) {
                viewModel.saveTransaction()
            } else {
                Toast.makeText(
                                requireContext(),
                                "Form belum lengkap atau Dompet Sumber/Tujuan sama!",
                                Toast.LENGTH_SHORT
                        )
                        .show()
            }
        }

        btnLanjut.setOnClickListener {
            viewModel.resetForm(keepType = true, keepCategory = false, setTodayDate = true)

            btnLanjut.visibility = View.GONE
            resetImageProof()

            etAmount.setText("") // Clear the visible amount
            etAmount.requestFocus()
            Toast.makeText(
                            requireContext(),
                            "Siap mencatat transaksi ${viewModel.selectedType.value} berikutnya!",
                            Toast.LENGTH_SHORT
                    )
                    .show()
        }

        btnReset.setOnClickListener {
            viewModel.resetForm(keepType = false, keepCategory = false, setTodayDate = true)
            btnLanjut.visibility = View.GONE
            resetImageProof()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.date.collect { timestamp ->
                        tvDate.text = viewModel.formatDate(timestamp)
                    }
                }

                launch {
                    viewModel.amount.collect { amountString ->
                        // Hapus pemformatan angka yang ada jika pengguna mengetik saat observasi
                        // (tapi biarkan koma)
                        val cleanString = amountString.replace(Regex("[^0-9,]"), "")

                        if (etAmount.text.toString() != amountString) {
                            etAmount.setText(amountString)
                        }
                    }
                }

                // Observasi notes hanya untuk set value (misal saat edit)
                launch {
                    viewModel.notes.collect { notes ->
                        if (etNotes.text.toString() != notes) {
                            etNotes.setText(notes)
                        }
                    }
                }

                launch {
                    viewModel.isSaving.collect { isSaving ->
                        progressBar.visibility = if (isSaving) View.VISIBLE else View.GONE
                        btnSimpan.isEnabled = !isSaving
                        btnReset.isEnabled = !isSaving
                        btnLanjut.isEnabled = !isSaving
                    }
                }

                launch {
                    viewModel.successMessage.collect { message ->
                        message?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            triggerImmediateSync()
                            // Reset setelah sukses menyimpan, namun pertahankan tipe
                            viewModel.resetForm(
                                    keepType = true,
                                    keepCategory = false,
                                    setTodayDate = true
                            )

                            btnLanjut.visibility = View.VISIBLE
                            resetImageProof()

                            viewModel.clearMessages()
                        }
                    }
                }

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
                        // Tentukan apakah kita dalam mode edit
                        val isEditing = viewModel.isEditMode()

                        if (path != null) {
                            val file = File(path)
                            if (file.exists()) {
                                val uri = Uri.fromFile(file)
                                imageUri = uri

                                ivProofImage.setImageURI(uri)
                                ivProofImage.visibility = View.VISIBLE
                                btnAddPhoto.setText("Hapus Foto")
                                btnAddPhoto.setIconResource(R.drawable.ic_baseline_close_24)
                            } else {
                                resetImageProof()
                            }
                        } else {
                            // PERBAIKAN: Mengganti isEditMode.value dengan pemanggilan fungsi
                            // isEditMode()
                            if (imageUri != null && isEditing == false) {
                                // Jika path di ViewModel null dan ini bukan proses edit
                                resetImageProof()
                            } else if (imageUri != null) {
                                // Jika imageUri masih ada tapi path null (misal imageUri adalah
                                // temp uri)
                                // Biarkan UI tetap menampilkan imageUri, namun path akan di-set
                                // null saat simpan
                            } else {
                                resetImageProof()
                            }
                        }
                    }
                }
            }
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories: List<Category> ->
            categoriesList = categories

            // --- START PERUBAHAN DI SINI ---
            val isTransfer = viewModel.selectedType.value == TransactionType.TRANSFER

            // Hanya tampilkan dan proses kategori jika bukan Transfer
            if (!isTransfer) {
                if (categories.isEmpty()) {
                    // Toast DIHAPUS agar tidak muncul berulang-ulang saat sync
                    spinnerKategori.adapter = null
                    viewModel.setCategory(0)
                    return@observe
                }

                val categoryNames = categories.map { "${it.icon} ${it.name}" }
                val adapter =
                        ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_spinner_item,
                                categoryNames
                        )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerKategori.adapter = adapter

                // Set selection
                var selectedIndex = 0
                val currentId = viewModel.selectedCategoryId.value
                if (currentId != null && currentId > 0) {
                    val existingIndex = categories.indexOfFirst { it.id == currentId }
                    if (existingIndex != -1) {
                        selectedIndex = existingIndex
                    }
                }

                spinnerKategori.onItemSelectedListener = null
                spinnerKategori.setSelection(selectedIndex, false)
                if (categories.isNotEmpty() && selectedIndex >= 0) {
                    viewModel.setCategory(categories[selectedIndex].id)
                }

                spinnerKategori.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                            ) {
                                if (position < categories.size) {
                                    viewModel.setCategory(categories[position].id)
                                    btnLanjut.visibility = View.GONE
                                }
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
            }
        }

        viewModel.wallets.observe(viewLifecycleOwner) { wallets: List<Wallet> ->
            walletsList = wallets

            if (wallets.isEmpty()) {
                // Toast DIHAPUS
                spinnerWallet.adapter = null
                spinnerWalletTarget.adapter = null
                viewModel.setWallet(0)
                viewModel.setTargetWallet(null)
                return@observe
            }

            val walletNames = wallets.map { "${it.icon} ${it.name}" }
            val adapter =
                    ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            walletNames
                    )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerWallet.adapter = adapter
            spinnerWalletTarget.adapter = adapter // Atur adapter untuk Dompet Tujuan

            // --- Logika Dompet Sumber (spinnerWallet) ---
            var selectedSourceIndex = 0
            val currentSourceId = viewModel.selectedWalletId.value
            if (currentSourceId > 0) {
                val existingIndex = wallets.indexOfFirst { it.id == currentSourceId }
                if (existingIndex != -1) {
                    selectedSourceIndex = existingIndex
                }
            }
            spinnerWallet.setSelection(selectedSourceIndex, false)
            if (wallets.isNotEmpty() && selectedSourceIndex >= 0) {
                viewModel.setWallet(wallets[selectedSourceIndex].id)
            }
            setupWalletSpinnerListener(walletsList)

            // --- Logika Dompet Tujuan (spinnerWalletTarget) ---
            var selectedTargetIndex = -1
            val currentTargetId = viewModel.selectedTargetWalletId.value

            if (currentTargetId != null && currentTargetId > 0) {
                val existingIndex = wallets.indexOfFirst { it.id == currentTargetId }
                if (existingIndex != -1) {
                    selectedTargetIndex = existingIndex
                }
            } else if (viewModel.selectedType.value == TransactionType.TRANSFER) {
                // Jika Transfer dan Dompet Tujuan belum di-set, default ke dompet yang berbeda
                if (wallets.size > 1) {
                    selectedTargetIndex = if (selectedSourceIndex == 0) 1 else 0
                }
            }

            if (selectedTargetIndex != -1) {
                spinnerWalletTarget.setSelection(selectedTargetIndex, false)
                viewModel.setTargetWallet(wallets[selectedTargetIndex].id)
            } else {
                // Jika tidak ada dompet tujuan yang valid atau hanya satu dompet
                spinnerWalletTarget.setSelection(0, false)
                viewModel.setTargetWallet(
                        if (wallets.isNotEmpty()) wallets[0].id else null
                ) // Set default
            }

            setupTargetWalletSpinnerListener(walletsList)
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val context = requireContext()
            // Menggunakan contentResolver.openInputStream
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return null
            }

            val directory = File(context.filesDir, "transaction_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = "IMG_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)

            val outputStream = java.io.FileOutputStream(file)
            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            if (file.exists() && file.length() > 0) {
                file.absolutePath
            } else {
                file.delete()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun triggerImmediateSync() {
        val workRequest =
                androidx.work.OneTimeWorkRequest.Builder(
                                com.example.catetduls.data.sync.DataSyncWorker::class.java
                        )
                        .setExpedited(
                                androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST
                        )
                        .build()

        androidx.work.WorkManager.getInstance(requireContext()).enqueue(workRequest)
    }
}
