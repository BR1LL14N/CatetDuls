package com.example.catetduls.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.AppDatabase
import com.example.catetduls.data.BookClosing
import com.example.catetduls.data.BookClosingRepository
import com.example.catetduls.data.getBookRepository
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TutupBukuViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = BookClosingRepository(database.bookClosingDao())
    private val bookRepository = application.getBookRepository()

    private val _closings = MutableLiveData<List<BookClosing>>()
    val closings: LiveData<List<BookClosing>> = _closings

    private val _currentPeriodClosed = MutableLiveData<BookClosing?>()
    val currentPeriodClosed: LiveData<BookClosing?> = _currentPeriodClosed

    private val _finalBalance = MutableLiveData<Double>()
    val finalBalance: LiveData<Double> = _finalBalance

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> = _success

    private var currentBookId: Int = 0

    init {
        // Get active book ID
        val sharedPrefs = application.getSharedPreferences("app_settings", 0)
        currentBookId = sharedPrefs.getInt("active_book_id", 1)
        loadClosings()
    }

    private fun loadClosings() {
        viewModelScope.launch {
            repository.getAllClosingsByBook(currentBookId).collect { list ->
                _closings.postValue(list)
            }
        }
    }

    fun checkPeriodStatus(year: Int, month: Int) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 15) // Mid month
            val timestamp = calendar.timeInMillis

            val closing = repository.getClosingForDate(currentBookId, timestamp)
            _currentPeriodClosed.postValue(closing)
        }
    }

    fun calculateFinalBalance(periodStart: Long, periodEnd: Long) {
        viewModelScope.launch {
            try {
                // Get all transactions in the period
                val transactions =
                        database.transactionDao().getAllTransactions(currentBookId).first().filter {
                            it.date >= periodStart && it.date <= periodEnd
                        }

                // Get all wallets
                val wallets = database.walletDao().getWalletsByBook(currentBookId).first()

                // Calculate total balance from all wallets
                var totalBalance = 0.0
                wallets.forEach { wallet ->
                    val walletTransactions = transactions.filter { it.walletId == wallet.id }
                    val income =
                            walletTransactions
                                    .filter {
                                        it.type ==
                                                com.example.catetduls.data.TransactionType.PEMASUKAN
                                    }
                                    .sumOf { it.amount }
                    val expense =
                            walletTransactions
                                    .filter {
                                        it.type ==
                                                com.example.catetduls.data.TransactionType
                                                        .PENGELUARAN
                                    }
                                    .sumOf { it.amount }
                    totalBalance += wallet.initialBalance + income - expense
                }

                _finalBalance.postValue(totalBalance)
            } catch (e: Exception) {
                _error.postValue("Gagal menghitung saldo: ${e.message}")
            }
        }
    }

    fun closeBook(
            periodStart: Long,
            periodEnd: Long,
            periodLabel: String,
            finalBalance: Double,
            isVerified: Boolean = false,
            notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                // Check if period is already closed
                val existing = repository.getClosingForDate(currentBookId, periodStart)
                if (existing != null) {
                    _error.postValue("Periode ini sudah ditutup sebelumnya")
                    return@launch
                }

                repository.closeBook(
                        bookId = currentBookId,
                        periodStart = periodStart,
                        periodEnd = periodEnd,
                        periodLabel = periodLabel,
                        finalBalance = finalBalance,
                        isVerified = isVerified,
                        notes = notes
                )

                _success.postValue(true)
                loadClosings()
            } catch (e: Exception) {
                _error.postValue("Gagal menutup buku: ${e.message}")
            }
        }
    }

    fun reopenBook(closingId: Int) {
        viewModelScope.launch {
            try {
                repository.reopenBook(closingId)
                _success.postValue(true)
                _currentPeriodClosed.postValue(null)
                loadClosings()
            } catch (e: Exception) {
                _error.postValue("Gagal membuka kembali periode: ${e.message}")
            }
        }
    }

    fun getTransactionsForPeriod(year: Int, month: Int, onResult: (List<com.example.catetduls.data.Transaction>, Map<Int, String>) -> Unit) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, 1, 0, 0, 0)
                val periodStart = calendar.timeInMillis
                
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val periodEnd = calendar.timeInMillis

                val transactions = database.transactionDao().getAllTransactions(currentBookId).first()
                    .filter { it.date in periodStart..periodEnd }
                
                // Fetch categories
                val categories = database.categoryDao().getAllCategoriesSync(currentBookId)
                val categoryMap = categories.associate { it.id to it.name }
                
                onResult(transactions, categoryMap)
            } catch (e: Exception) {
                _error.postValue("Gagal memuat data transaksi: ${e.message}")
                onResult(emptyList(), emptyMap())
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _success.value = false
    }
}

class TutupBukuViewModelFactory(private val application: Application) :
        androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TutupBukuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return TutupBukuViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
