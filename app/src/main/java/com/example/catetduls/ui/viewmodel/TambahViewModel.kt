package com.example.catetduls.viewmodel

import androidx.lifecycle.*
import com.example.catetduls.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*

class TambahViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository,
    private val activeWalletId: Int,
    private val activeBookId: Int
) : ViewModel() {

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

    private val _transactionId = MutableStateFlow<Int?>(null)
    val transactionId: StateFlow<Int?> = _transactionId.asStateFlow()

    private val _selectedWalletId = MutableStateFlow(activeWalletId) // Dompet Sumber
    val selectedWalletId: StateFlow<Int> = _selectedWalletId.asStateFlow()

    private val _selectedTargetWalletId = MutableStateFlow<Int?>(null) // Dompet Tujuan
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

    private suspend fun getWalletNameById(walletId: Int): String {
        val wallet = walletRepository.getSingleWalletById(walletId)
        return wallet?.name ?: "Dompet ID $walletId"
    }

    init {

        viewModelScope.launch {
            try {

                val id = categoryRepository.getCategoryIdByType(TransactionType.TRANSFER, activeBookId)
                _transferCategoryId.value = id
            } catch (e: Exception) {

                android.util.Log.e("TambahViewModel", "Failed to fetch Transfer Category ID: ${e.message}")
            }
        }
    }

    // Data Observasi
    val categories: LiveData<List<Category>> = _selectedType.asLiveData().switchMap { type ->

        if (type != TransactionType.TRANSFER) {
            categoryRepository.getCategoriesByBookIdAndType(activeBookId, type).asLiveData()
        } else {
            MutableLiveData(emptyList())
        }
    }

    val wallets: LiveData<List<Wallet>> = walletRepository.getWalletsByBook(activeBookId).asLiveData()

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
        val cleaned = _amount.value.replace(",", "").replace(".", "")
        return cleaned.toDoubleOrNull()
    }

    fun saveTransaction() {
        viewModelScope.launch {
            _isSaving.value = true

            try {
                val amountValue = parseAmount()
                val currentType = _selectedType.value


                if (amountValue == null || amountValue <= 0 || !isFormValid()) {
                    val baseError = if (amountValue == null || amountValue <= 0) "Jumlah harus lebih dari 0" else "Form belum lengkap atau Dompet Sumber/Tujuan sama"

                    if (currentType == TransactionType.TRANSFER && _transferCategoryId.value == null) {
                        _errorMessage.value = "Kategori Transfer belum tersedia. Coba restart aplikasi."
                    } else if (currentType == TransactionType.TRANSFER) {
                        _errorMessage.value = "Transfer tidak valid: Pastikan Dompet Sumber dan Tujuan berbeda dan jumlah diisi."
                    } else {
                        _errorMessage.value = "Transaksi tidak valid: Kategori dan Dompet harus dipilih."
                    }
                    _isSaving.value = false
                    return@launch
                }

                if (isEditMode() && currentType == TransactionType.TRANSFER) {
                    _errorMessage.value = "Edit transaksi Transfer belum didukung"
                    _isSaving.value = false
                    return@launch
                }

                if (currentType == TransactionType.TRANSFER) {
                    val sourceWalletId = _selectedWalletId.value
                    val targetWalletId = _selectedTargetWalletId.value!!
                    val transferCategoryId = _transferCategoryId.value!! // Dipastikan tidak null oleh isFormValid()

                    val timestamp = _date.value
                    val imagePath = _imagePath.value
                    val notes = _notes.value

                    val sourceWalletName = getWalletNameById(sourceWalletId)
                    val targetWalletName = getWalletNameById(targetWalletId)

                    // (Dari Dompet Sumber - Pengeluaran)
                    val expenseTransaction = Transaction(
                        id = 0,
                        type = TransactionType.PENGELUARAN,
                        amount = amountValue,
                        categoryId = transferCategoryId,
                        date = timestamp,
                        notes = "[TRANSFER OUT: ke ${targetWalletName}], $notes",
                        walletId = sourceWalletId,
                        imagePath = imagePath,
                        lastSyncAt = System.currentTimeMillis()
                    )

                    // (Ke Dompet Tujuan - Pemasukan)
                    val incomeTransaction = Transaction(
                        id = 0,
                        type = TransactionType.PEMASUKAN,
                        amount = amountValue,
                        categoryId = transferCategoryId,
                        date = timestamp,
                        notes = "[TRANSFER IN: dari ${sourceWalletName}], $notes",
                        walletId = targetWalletId,
                        imagePath = imagePath,
                        lastSyncAt = System.currentTimeMillis()
                    )


                    transactionRepository.insertTransaction(expenseTransaction)
                    transactionRepository.insertTransaction(incomeTransaction)

                    _successMessage.value = "Transfer Rp ${formatAmountDisplay(amountValue.toLong().toString())} berhasil dilakukan"

                } else {
                    // Logika Pemasukan/Pengeluaran yang sudah ada
                    val categoryId = _selectedCategoryId.value
                    val walletId = _selectedWalletId.value

                    val transaction = Transaction(
                        id = _transactionId.value ?: 0,
                        type = currentType,
                        amount = amountValue,
                        categoryId = categoryId!!,
                        date = _date.value,
                        notes = _notes.value,
                        walletId = walletId,
                        imagePath = _imagePath.value,
                        lastSyncAt = System.currentTimeMillis()
                    )

                    if (isEditMode()) {
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
                transactionRepository.getTransactionById(transactionId).asLiveData().observeForever { transaction ->
                    if (transaction != null) {
                        _transactionId.value = transaction.id
                        _selectedType.value = transaction.type
                        _selectedCategoryId.value = transaction.categoryId
                        _selectedWalletId.value = transaction.walletId
                        _amount.value = java.math.BigDecimal(transaction.amount).toPlainString().replace(".00", "").replace(".0", "")
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
        val cleaned = value.replace(Regex("[^0-9]"), "")
        if (cleaned.isEmpty()) return ""

        val number = cleaned.toLongOrNull() ?: return value
        // Menggunakan format titik sebagai pemisah ribuan
        return String.format(Locale("in", "ID"), "%,d", number).replace(',', '.')
    }

    fun getCategoryName(categoryId: Int): LiveData<String> {
        return categoryRepository.getCategoryById(categoryId)
            .asLiveData()
            .map { it?.name ?: "Unknown" }
    }
}

class TambahViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val walletRepository: WalletRepository,
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
                activeWalletId,
                activeBookId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}