package com.example.catetduls.viewmodel

import androidx.lifecycle.*
import com.example.catetduls.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel untuk TambahTransaksiPage (Form Input/Edit)
 *
 * Mengelola:
 * - Input form transaksi
 * - Validasi data
 * - Simpan transaksi baru
 * - Update transaksi existing
 */
class TambahViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
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

    // ========================================
    // Form Data
    // ========================================

    private val _transactionId = MutableStateFlow<Int?>(null)
    val transactionId: StateFlow<Int?> = _transactionId.asStateFlow()

    private val _selectedType = MutableStateFlow("Pengeluaran") // Default: Pengeluaran
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _date = MutableStateFlow(System.currentTimeMillis())
    val date: StateFlow<Long> = _date.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    // ========================================
    // Categories Data
    // ========================================

    /**
     * List kategori sesuai tipe yang dipilih
     */
    val categories: LiveData<List<Category>> = _selectedType.asLiveData().switchMap { type ->
        categoryRepository.getCategoriesByType(type).asLiveData()
    }

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
        val amountValue = _amount.value.replace(",", "").replace(".", "").toDoubleOrNull()
        return amountValue != null &&
                amountValue > 0 &&
                _selectedCategoryId.value != null
    }

    // ========================================
    // Actions
    // ========================================

    /**
     * Set tipe transaksi (Pemasukan/Pengeluaran)
     */
    fun setType(type: String) {
        _selectedType.value = type
        _selectedCategoryId.value = null // Reset kategori saat ganti tipe
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

                // Validasi
                if (amountValue == null || amountValue <= 0) {
                    _errorMessage.value = "Jumlah harus lebih dari 0"
                    _isSaving.value = false
                    return@launch
                }

                if (categoryId == null) {
                    _errorMessage.value = "Kategori harus dipilih"
                    _isSaving.value = false
                    return@launch
                }

                // Buat object Transaction
                val transaction = Transaction(
                    id = _transactionId.value ?: 0,
                    type = _selectedType.value,
                    amount = amountValue,
                    categoryId = categoryId,
                    date = _date.value,
                    notes = _notes.value
                )

                // Validasi dengan repository
                val validation = transactionRepository.validateTransaction(transaction)
                if (validation is ValidationResult.Error) {
                    _errorMessage.value = validation.message
                    _isSaving.value = false
                    return@launch
                }

                // Simpan ke database
                if (isEditMode()) {
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
                transactionRepository.getTransactionById(transactionId)
                    .collect { transaction ->
                        if (transaction != null) {
                            _transactionId.value = transaction.id
                            _selectedType.value = transaction.type
                            _selectedCategoryId.value = transaction.categoryId
                            _amount.value = transaction.amount.toString()
                            _date.value = transaction.date
                            _notes.value = transaction.notes
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
     * Reset form (untuk mode tambah baru)
     */
    fun resetForm() {
        _transactionId.value = null
        _selectedType.value = "Pengeluaran"
        _selectedCategoryId.value = null
        _amount.value = ""
        _date.value = System.currentTimeMillis()
        _notes.value = ""
        clearMessages()
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
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TambahViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TambahViewModel(transactionRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}