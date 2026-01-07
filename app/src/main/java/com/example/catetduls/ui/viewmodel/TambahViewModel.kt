package com.example.catetduls.viewmodel

// TransactionType
import androidx.lifecycle.*
import com.example.catetduls.data.* // Asumsi ini berisi kelas Transaction, Category, Wallet, dan
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TambahViewModel(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val walletRepository: WalletRepository,
        private val bookRepository: BookRepository,
        private val activeWalletId: Int,
        private val activeBookId: Int
) : ViewModel() {

    // === State Management ===
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _imagePath = MutableStateFlow<String?>(null)
    val imagePath: StateFlow<String?> = _imagePath.asStateFlow()

    // === Form Data ===
    private val _transactionId = MutableStateFlow<Int?>(null)
    val transactionId: StateFlow<Int?> = _transactionId.asStateFlow()

    private val _selectedWalletId = MutableStateFlow(activeWalletId) // Source Wallet
    val selectedWalletId: StateFlow<Int> = _selectedWalletId.asStateFlow()

    private val _selectedTargetWalletId = MutableStateFlow<Int?>(null) // Target Wallet
    val selectedTargetWalletId: StateFlow<Int?> = _selectedTargetWalletId.asStateFlow()

    private val _selectedType = MutableStateFlow(TransactionType.PENGELUARAN)
    val selectedType: StateFlow<TransactionType> = _selectedType.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _amount = MutableStateFlow("0")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _date = MutableStateFlow(System.currentTimeMillis())
    val date: StateFlow<Long> = _date.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _transferCategoryId = MutableStateFlow<Int?>(null)
    private var activeCurrencyCode: String = "IDR" // Default

    private suspend fun getWalletNameById(walletId: Int): String {
        val wallet = walletRepository.getSingleWalletById(walletId)
        return wallet?.name ?: "Dompet ID $walletId"
    }

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Get Transfer Category ID on init
                val id =
                        categoryRepository.getCategoryIdByType(
                                TransactionType.TRANSFER,
                                activeBookId
                        )
                _transferCategoryId.value = id

                // Fetch Active Book Currency Code
                val book = bookRepository.getBookByIdSync(activeBookId)
                if (book != null) {
                    // Jika book.currencyCode null, gunakan "IDR" sebagai default
                    activeCurrencyCode = book.currencyCode ?: "IDR"
                }
            } catch (e: Exception) {
                android.util.Log.e("TambahViewModel", "Failed to fetch Init Data: ${e.message}")
            }
        }
    }

    // === Data Observation ===
    val categories: LiveData<List<Category>> =
            _selectedType.asLiveData().switchMap { type ->
                if (type != TransactionType.TRANSFER) {
                    categoryRepository.getCategoriesByBookIdAndType(activeBookId, type).asLiveData()
                } else {
                    MutableLiveData(emptyList())
                }
            }

    val wallets: LiveData<List<Wallet>> =
            walletRepository.getWalletsByBook(activeBookId).asLiveData()

    // === Computed Properties ===
    fun isEditMode(): Boolean = _transactionId.value != null

    fun isFormValid(): Boolean {
        val amountValue = parseAmount()
        if (amountValue == null || amountValue <= 0) return false

        val currentType = _selectedType.value
        val sourceWalletId = _selectedWalletId.value
        val targetWalletId = _selectedTargetWalletId.value

        return when (currentType) {
            TransactionType.TRANSFER -> {
                targetWalletId != null &&
                        sourceWalletId > 0 &&
                        targetWalletId > 0 &&
                        sourceWalletId != targetWalletId &&
                        _transferCategoryId.value != null
            }
            TransactionType.PEMASUKAN, TransactionType.PENGELUARAN -> {
                _selectedCategoryId.value != null &&
                        _selectedCategoryId.value!! > 0 &&
                        sourceWalletId > 0
            }
        }
    }

    // === Actions ===
    fun setType(type: TransactionType) {
        _selectedType.value = type
        if (type == TransactionType.TRANSFER) {
            _selectedCategoryId.value = _transferCategoryId.value
        } else {
            _selectedCategoryId.value = null
        }
    }

    fun setWallet(walletId: Int) {
        _selectedWalletId.value = walletId
    }

    fun setTargetWallet(walletId: Int?) {
        _selectedTargetWalletId.value = walletId
    }

    fun setCategory(categoryId: Int) {
        _selectedCategoryId.value = categoryId
    }

    fun setAmount(value: String) {
        val cleaned = value.replace(Regex("[^0-9,.]"), "")
        _amount.value = cleaned
    }

    fun setDate(timestamp: Long) {
        _date.value = timestamp
    }

    fun setNotes(value: String) {
        _notes.value = value
    }

    fun setImagePath(path: String?) {
        _imagePath.value = path
    }

    private fun parseAmount(): Double? {
        val raw = _amount.value
        if (raw.isBlank()) return null

        // Asumsi format input mengikuti Locale Indonesia (Titik = Ribuan, Koma = Desimal)
        // Contoh: "1.000.000" -> 1000000
        // Contoh: "10,50" -> 10.5

        // 1. Hapus titik (ribuan)
        var cleaned = raw.replace(".", "")

        // 2. Ganti koma dengan titik (untuk format Double standar program)
        cleaned = cleaned.replace(",", ".")

        return cleaned.toDoubleOrNull()
    }

    fun saveTransaction(context: android.content.Context) {
        viewModelScope.launch {
            _isSaving.value = true

            try {
                val amountValue = parseAmount()
                val currentType = _selectedType.value
                val isEdit = isEditMode()
                val currentTime = System.currentTimeMillis()

                // --- CONVERSION LOGIC ---
                // Convert Input Amount (Displayed Currency) -> IDR (Database Currency)
                val amountInIdr =
                        com.example.catetduls.utils.CurrencyHelper.convertToIdr(
                                amountValue!!,
                                activeCurrencyCode
                        )

                // --- INITIAL VALIDATION ---
                if (amountInIdr <= 0 || !isFormValid()) {
                    if (amountInIdr <= 0) {
                        _errorMessage.value = "Jumlah harus lebih dari 0"
                    } else if (currentType == TransactionType.TRANSFER) {
                        if (_transferCategoryId.value == null) {
                            _errorMessage.value =
                                    "Kategori Transfer belum tersedia. Coba restart aplikasi."
                        } else {
                            _errorMessage.value =
                                    "Transfer tidak valid: Pastikan Dompet Sumber dan Tujuan berbeda dan jumlah diisi."
                        }
                    } else {
                        _errorMessage.value =
                                "Transaksi tidak valid: Kategori dan Dompet harus dipilih."
                    }
                    _isSaving.value = false
                    return@launch
                }

                if (isEdit && currentType == TransactionType.TRANSFER) {
                    _errorMessage.value = "Edit transaksi Transfer belum didukung"
                    _isSaving.value = false
                    return@launch
                }

                // --- 3. IMAGE HANDLING LOGIC ---
                var finalImagePath = _imagePath.value

                // Jika path adalah Content URI (dari Galeri/Kamera), copy ke Internal Storage
                // Jangan copy jika sudah berupa path file di internal storage atau URL remote
                if (finalImagePath != null &&
                                (finalImagePath.startsWith("content:") ||
                                        finalImagePath.startsWith("file:"))
                ) {
                    val uri = android.net.Uri.parse(finalImagePath)
                    val newPath =
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                copyImageToInternalStorage(context, uri)
                            }

                    if (newPath != null) {
                        finalImagePath = newPath
                    } else {
                        android.util.Log.w(
                                "TambahViewModel",
                                "Failed to copy image to internal storage"
                        )
                        // Fallback: keep original URI (might fail later if temp permission lost,
                        // but better than null)
                    }
                }

                // --- 4. DATA PREPARATION ---
                val syncAction = if (isEdit) "UPDATE" else "CREATE"

                // Get existing for serverId
                val existingTransaction =
                        if (isEdit) {
                            transactionRepository.getSingleTransactionById(_transactionId.value!!)
                        } else null
                val existingServerId = existingTransaction?.serverId

                // Check Transfer
                if (currentType == TransactionType.TRANSFER) {
                    val sourceWalletId = _selectedWalletId.value
                    val targetWalletId = _selectedTargetWalletId.value!!
                    val transferCategoryId = _transferCategoryId.value!!

                    val timestamp = _date.value
                    val notes = _notes.value

                    val sourceWalletName = getWalletNameById(sourceWalletId)
                    val targetWalletName = getWalletNameById(targetWalletId)

                    // 1. EXPENSE
                    val expenseTransaction =
                            Transaction(
                                    id = 0,
                                    type = TransactionType.PENGELUARAN,
                                    amount = amountInIdr,
                                    categoryId = transferCategoryId,
                                    date = timestamp,
                                    notes =
                                            "[TRANSFER OUT: dari ${sourceWalletName} ke ${targetWalletName}], $notes",
                                    walletId = sourceWalletId,
                                    bookId = activeBookId,
                                    imagePath = finalImagePath, // PATH BARU
                                    createdAt = currentTime,
                                    updatedAt = currentTime,
                                    lastSyncAt = 0,
                                    isSynced = false,
                                    syncAction = "CREATE",
                                    serverId = null,
                                    isDeleted = false
                            )

                    // 2. INCOME
                    val incomeTransaction =
                            Transaction(
                                    id = 0,
                                    type = TransactionType.PEMASUKAN,
                                    amount = amountInIdr,
                                    categoryId = transferCategoryId,
                                    date = timestamp,
                                    notes =
                                            "[TRANSFER IN: dari ${sourceWalletName} ke ${targetWalletName}], $notes",
                                    walletId = targetWalletId,
                                    bookId = activeBookId,
                                    imagePath = finalImagePath, // PATH BARU
                                    createdAt = currentTime,
                                    updatedAt = currentTime,
                                    lastSyncAt = 0,
                                    isSynced = false,
                                    syncAction = "CREATE",
                                    serverId = null,
                                    isDeleted = false
                            )

                    transactionRepository.insertTransaction(expenseTransaction)
                    transactionRepository.insertTransaction(incomeTransaction)

                    _successMessage.value =
                            "Transfer Rp ${formatAmountDisplay(amountValue.toLong().toString())} berhasil dilakukan"
                } else {

                    // --- INCOME/EXPENSE LOGIC (Single Transaction) ---
                    val categoryId = _selectedCategoryId.value
                    val walletId = _selectedWalletId.value

                    val transaction =
                            Transaction(
                                    id = _transactionId.value ?: 0,
                                    type = currentType,
                                    amount = amountInIdr, // Save IDR
                                    categoryId = categoryId!!,
                                    date = _date.value,
                                    notes = _notes.value,
                                    walletId = walletId,
                                    bookId = activeBookId,
                                    imagePath = finalImagePath, // PATH BARU

                                    // === SYNC FIELDS ===
                                    createdAt =
                                            if (isEdit) existingTransaction?.createdAt ?: 0
                                            else currentTime,
                                    updatedAt = currentTime,
                                    lastSyncAt = 0,
                                    isSynced = false,
                                    syncAction = syncAction,
                                    serverId = if (isEdit) existingServerId else null,
                                    isDeleted = false
                            )

                    if (isEdit) {
                        transactionRepository.updateTransaction(transaction)
                        _successMessage.value = "Transaksi berhasil diupdate"
                    } else {
                        transactionRepository.insertTransaction(transaction)
                        _successMessage.value = "Transaksi berhasil ditambahkan"
                    }
                }

                _isSaving.value = false
            } catch (e: Exception) {
                _isSaving.value = false
                _errorMessage.value = "Gagal menyimpan transaksi: ${e.message}"
            }
        }
    }

    fun loadTransaction(transactionId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Ensure we have the correct active currency code first
                val book =
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            bookRepository.getBookByIdSync(activeBookId)
                        }
                if (book != null) {
                    activeCurrencyCode = book.currencyCode ?: "IDR"
                }

                transactionRepository
                        .getTransactionById(transactionId)
                        .asLiveData()
                        .observeForever { transaction ->
                            if (transaction != null) {
                                _transactionId.value = transaction.id
                                _selectedType.value = transaction.type
                                _selectedCategoryId.value = transaction.categoryId

                                // Convert IDR -> Active Currency
                                val converted =
                                        com.example.catetduls.utils.CurrencyHelper.convertIdrTo(
                                                transaction.amount,
                                                activeCurrencyCode
                                        )

                                // Format strictly with COMMA as decimal separator to match
                                // parseAmount logic
                                // Use US locale to ensure dot is decimal, then swap to comma
                                val formatted =
                                        if (converted % 1.0 == 0.0) {
                                            String.format(java.util.Locale.US, "%.0f", converted)
                                        } else {
                                            String.format(java.util.Locale.US, "%.2f", converted)
                                                    .replace('.', ',')
                                        }
                                // android.util.Log.d("TambahViewModel", "Formatted Strings:
                                // $formatted")

                                _amount.value = formatted
                                _date.value = transaction.date
                                _notes.value = transaction.notes
                                _imagePath.value = transaction.imagePath
                            }
                            _isLoading.value = false
                        }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Gagal memuat transaksi: ${e.message}"
            }
        }
    }

    fun resetForm(
            keepType: Boolean = false,
            keepCategory: Boolean = false,
            setTodayDate: Boolean = true
    ) {
        _transactionId.value = null

        if (!keepType) {
            _selectedType.value = TransactionType.PENGELUARAN
            _selectedCategoryId.value = null
        } else if (_selectedType.value == TransactionType.TRANSFER) {
            _selectedCategoryId.value = _transferCategoryId.value
        } else if (!keepCategory) {
            _selectedCategoryId.value = null
        }

        _selectedWalletId.value = activeWalletId
        _selectedTargetWalletId.value = null
        _amount.value = "0"
        _notes.value = ""
        _imagePath.value = null

        if (setTodayDate) {
            _date.value = System.currentTimeMillis()
        }

        clearMessages()
    }

    fun clearImage() {
        val oldPath = _imagePath.value
        _imagePath.value = null

        if (oldPath != null) {
            try {
                // Hanya hapus jika file lokal dan bukan URL
                val isLocalFile = !oldPath.startsWith("http") && !oldPath.startsWith("content")
                if (isLocalFile) {
                    val file = java.io.File(oldPath)
                    if (file.exists()) {
                        file.delete()
                        android.util.Log.d("TambahViewModel", "Image file deleted: $oldPath")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TambahViewModel", "Error deleting image: ${e.message}")
            }
        }
    }

    private fun copyImageToInternalStorage(
            context: android.content.Context,
            uri: android.net.Uri
    ): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            // Create a dedicated directory for transaction images
            val imagesDir = java.io.File(context.filesDir, "transaction_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            // Generate a unique filename
            val fileName = "IMG_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"
            val destFile = java.io.File(imagesDir, fileName)

            val outputStream = java.io.FileOutputStream(destFile)

            inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }

            destFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("TambahViewModel", "Error copying image: ${e.message}")
            null
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        return "$day/$month/$year"
    }

    fun formatAmountDisplay(value: String): String {
        // Allow digits and comma
        val cleaned = value.replace(Regex("[^0-9,]"), "")
        if (cleaned.isEmpty()) return ""

        val parts = cleaned.split(",")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else null

        val number = integerPart.toLongOrNull() ?: return value

        // Consistent formatting with Fragment
        val symbols = java.text.DecimalFormatSymbols(Locale.US)
        symbols.groupingSeparator = '.'
        symbols.decimalSeparator = ','
        val decimalFormat = java.text.DecimalFormat("#,###", symbols)

        val formattedInteger = decimalFormat.format(number)

        return if (decimalPart != null) {
            "$formattedInteger,$decimalPart"
        } else {
            formattedInteger
        }
    }

    fun getCategoryName(categoryId: Int): LiveData<String> {
        return categoryRepository.getCategoryById(categoryId).asLiveData().map {
            it?.name ?: "Unknown"
        }
    }
}

class TambahViewModelFactory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val walletRepository: WalletRepository,
        private val bookRepository: BookRepository, // Add Book Repo
        private val activeWalletId: Int,
        private val activeBookId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TambahViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TambahViewModel(
                    transactionRepository,
                    categoryRepository,
                    walletRepository,
                    bookRepository,
                    activeWalletId,
                    activeBookId
            ) as
                    T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
