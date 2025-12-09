package com.example.catetduls.viewmodel

import androidx.lifecycle.*
import com.example.catetduls.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.IllegalArgumentException
import java.io.File

/**
 * ViewModel untuk TambahTransaksiPage (Form Input/Edit)
 */
class TambahViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository, // ✅ TAMBAH: Wallet Repository
    private val activeWalletId: Int,
    private val activeBookId: Int
) : ViewModel() {

    // ========================================
    // State Management
    // ========================================

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

    // ========================================
    // Form Data
    // ========================================

    private val _transactionId = MutableStateFlow<Int?>(null)
    val transactionId: StateFlow<Int?> = _transactionId.asStateFlow()
    private val _selectedWalletId = MutableStateFlow(activeWalletId)
    val selectedWalletId: StateFlow<Int> = _selectedWalletId.asStateFlow()
    private val _selectedType = MutableStateFlow(TransactionType.PENGELUARAN) // Default: Pengeluaran
    val selectedType: StateFlow<TransactionType> = _selectedType.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _amount = MutableStateFlow("0")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _date = MutableStateFlow(System.currentTimeMillis())
    val date: StateFlow<Long> = _date.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    // ========================================
    // Data Observasi
    // ========================================

    /**
     * List kategori sesuai tipe yang dipilih (Difilter oleh Book ID)
     */
    val categories: LiveData<List<Category>> = _selectedType.asLiveData().switchMap { type ->
        categoryRepository.getCategoriesByBookIdAndType(activeBookId, type).asLiveData()
    }

    /**
     * List dompet berdasarkan buku aktif
     */
    val wallets: LiveData<List<Wallet>> = walletRepository.getWalletsByBook(activeBookId).asLiveData()


    // ========================================
    // Computed Properties
    // ========================================

    /**
     * Apakah sedang dalam mode edit?
     */
    fun isEditMode(): Boolean = _transactionId.value != null

    /**
     * Validasi apakah form sudah valid
     */
    fun isFormValid(): Boolean {
        // Hanya menghapus koma/titik jika Anda menggunakan format lokal.
        // Untuk input sederhana, cukup parse ke DoubleOrNull.
        val amountValue = _amount.value.replace(",", "").replace(".", "").toDoubleOrNull()
        return amountValue != null &&
                amountValue > 0 &&
                _selectedCategoryId.value != null &&
                _selectedWalletId.value > 0 // ✅ PASTIKAN WALLET TERPILIH
    }

    // ========================================
    // Actions
    // ========================================

    /**
     * Set tipe transaksi (Pemasukan/Pengeluaran)
     */
    fun setType(type: TransactionType) {
        _selectedType.value = type
        // Reset kategori ketika tipe berubah
        _selectedCategoryId.value = null
    }

    fun setWallet(walletId: Int) {
        _selectedWalletId.value = walletId
    }

    /**
     * Set kategori
     */
    fun setCategory(categoryId: Int) {
        _selectedCategoryId.value = categoryId
    }

    /**
     * Set amount (dalam format string untuk input)
     */
    fun setAmount(value: String) {
        // Remove non-digit characters except comma and dot
        val cleaned = value.replace(Regex("[^0-9,.]"), "")
        _amount.value = cleaned
    }

    /**
     * Set tanggal
     */
    fun setDate(timestamp: Long) {
        _date.value = timestamp
    }

    /**
     * Set notes
     */
    fun setNotes(value: String) {
        _notes.value = value
    }


    fun setImagePath(path: String?) {
        _imagePath.value = path
    }

    /**
     * Parse amount string ke Double
     */
    private fun parseAmount(): Double? {
        val cleaned = _amount.value.replace(",", "").replace(".", "")
        return cleaned.toDoubleOrNull()
    }

    /**
     * Simpan transaksi (insert atau update)
     */
    fun saveTransaction() {
        viewModelScope.launch {
            _isSaving.value = true

            try {
                val amountValue = parseAmount()
                val categoryId = _selectedCategoryId.value
                val walletId = _selectedWalletId.value

                if (amountValue == null || amountValue <= 0) {
                    _errorMessage.value = "Jumlah harus lebih dari 0"
                    _isSaving.value = false
                    return@launch
                }

                if (categoryId == null || walletId <= 0) {
                    _errorMessage.value = "Kategori dan Dompet harus dipilih"
                    _isSaving.value = false
                    return@launch
                }

                val currentTime = System.currentTimeMillis()
                val isEdit = isEditMode()

                // Tentukan Sync Action (CREATE jika baru, UPDATE jika edit)
                val action = if (isEdit) "UPDATE" else "CREATE"

                // Buat object Transaction
                // Kita harus mengisi field-field baru (Sync Metadata)
                val transaction = Transaction(
                    id = _transactionId.value ?: 0,
                    type = _selectedType.value,
                    amount = amountValue,
                    categoryId = categoryId,
                    date = _date.value,
                    notes = _notes.value,
                    walletId = walletId,
                    imagePath = _imagePath.value,

                    // === TAMBAHAN FIELD SYNC (WAJIB DIISI) ===
                    createdAt = if (isEdit) 0 else currentTime, // Nanti di Repository/DAO biasanya di-handle agar tidak menimpa created_at lama jika Update
                    updatedAt = currentTime,
                    lastSyncAt = 0,      // 0 artinya belum disinkronkan ke server
                    isSynced = false,    // False karena baru diubah di lokal
                    syncAction = action, // "CREATE" atau "UPDATE"
                    serverId = null,     // Null karena ini operasi lokal
                    isDeleted = false
                )

                // Simpan ke database
                if (isEdit) {
                    // Jika Edit: Sebaiknya kita pertahankan created_at dan server_id yang lama
                    // Tapi karena TransactionDao @Update biasanya replace,
                    // Kita asumsikan Repository menghandle logic mempertahankan field lama
                    // atau kita harus load data lama dulu di sini.

                    // Untuk solusi cepat agar tidak error:
                    transactionRepository.updateTransaction(transaction)
                    _successMessage.value = "Transaksi berhasil diupdate"
                } else {
                    transactionRepository.insertTransaction(transaction)
                    _successMessage.value = "Transaksi berhasil ditambahkan"
                }

                _isSaving.value = false

            } catch (e: Exception) {
                _isSaving.value = false
                _errorMessage.value = "Gagal menyimpan transaksi: ${e.message}"
            }
        }
    }

    /**
     * Load transaksi untuk edit
     */
    fun loadTransaction(transactionId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                transactionRepository.getTransactionById(transactionId).asLiveData().observeForever { transaction ->
                    if (transaction != null) {
                        _transactionId.value = transaction.id
                        _selectedType.value = transaction.type
                        _selectedCategoryId.value = transaction.categoryId
                        _selectedWalletId.value = transaction.walletId
                        _amount.value = java.math.BigDecimal(transaction.amount).toPlainString().replace(".00", "").replace(".0", "")
                        _date.value = transaction.date
                        _notes.value = transaction.notes
                        _imagePath.value = transaction.imagePath  // ✅ TAMBAHKAN INI!

                        // ✅ DEBUG LOG
                        android.util.Log.d("TambahViewModel", "=== LOAD IMAGE DEBUG ===")
                        android.util.Log.d("TambahViewModel", "Transaction ID: ${transaction.id}")
                        android.util.Log.d("TambahViewModel", "Image Path: ${transaction.imagePath}")

                        if (transaction.imagePath != null) {
                            val file = java.io.File(transaction.imagePath)
                            android.util.Log.d("TambahViewModel", "File exists: ${file.exists()}")
                            android.util.Log.d("TambahViewModel", "File size: ${file.length()} bytes")
                        }
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Gagal memuat transaksi: ${e.message}"
            }
        }
    }

    /**
     * Reset form (untuk mode tambah baru).
     */
    fun resetForm(
        keepType: Boolean = false,
        keepCategory: Boolean = false,
        setTodayDate: Boolean = true
    ) {
        _transactionId.value = null

        if (!keepType) {
            _selectedType.value = TransactionType.PENGELUARAN
        }

        if (!keepCategory) {
            _selectedCategoryId.value = null
        }

        _selectedWalletId.value = activeWalletId
        _amount.value = "0"
        _notes.value = ""
        _imagePath.value = null  // ✅ RESET IMAGE PATH

        if (setTodayDate) {
            _date.value = System.currentTimeMillis()
        }

        clearMessages()
    }

    fun clearImage() {
        val oldPath = _imagePath.value
        _imagePath.value = null

        // Optional: Hapus file jika ingin cleanup langsung
        if (oldPath != null) {
            try {
                val file = java.io.File(oldPath)
                if (file.exists()) {
                    file.delete()
                    android.util.Log.d("TambahViewModel", "Image file deleted: $oldPath")
                }
            } catch (e: Exception) {
                android.util.Log.e("TambahViewModel", "Error deleting image: ${e.message}")
            }
        }
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // ========================================
    // Helper Functions
    // ========================================

    /**
     * Format tanggal untuk display
     */
    fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        return "$day/$month/$year"
    }

    /**
     * Format amount dengan pemisah ribuan
     */
    fun formatAmountDisplay(value: String): String {
        val cleaned = value.replace(Regex("[^0-9]"), "")
        if (cleaned.isEmpty()) return ""

        val number = cleaned.toLongOrNull() ?: return value
        return String.format("%,d", number).replace(',', '.')
    }

    /**
     * Get nama kategori berdasarkan ID
     */
    fun getCategoryName(categoryId: Int): LiveData<String> {
        return categoryRepository.getCategoryById(categoryId)
            .asLiveData()
            .map { it?.name ?: "Unknown" }
    }
}

/**
 * Factory untuk TambahViewModel
 */
class TambahViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository, // ✅ TAMBAH: Wallet Repository
    private val activeWalletId: Int,
    private val activeBookId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TambahViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TambahViewModel(
                transactionRepository,
                categoryRepository,
                walletRepository, // ✅ PASS REPO BARU
                activeWalletId,
                activeBookId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}