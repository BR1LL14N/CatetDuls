package com.example.catetduls

import android.app.Application
import androidx.work.Configuration
import com.example.catetduls.data.*
import com.example.catetduls.data.remote.ApiService
import com.example.catetduls.data.sync.DataSyncWorker
import com.example.catetduls.data.sync.SyncManager
import com.example.catetduls.di.NetworkModule

// Kelas Application Kustom untuk inisialisasi Singleton dan WorkManager
class CatetDulsApp : Application(), Configuration.Provider {

    // Dependency Injection (DI) Manual: Menyediakan NetworkModule
    private val networkModule: NetworkModule by lazy { NetworkModule }

    // Asumsi Anda telah memiliki semua Repository dan database diinisialisasi
    // Anda harus mengganti implementasi ini dengan cara Anda mengakses repositori
    // (misalnya melalui AppDatabase.getDatabase)
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    val bookRepository: BookRepository by lazy { BookRepository(database.bookDao()) }
    val walletRepository: WalletRepository by lazy { WalletRepository(database.walletDao()) }
    val categoryRepository: CategoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository: TransactionRepository by lazy { TransactionRepository(database.transactionDao()) }

    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao(), apiService)
    }
    val apiService: ApiService by lazy {
        networkModule.apiService
    }

    // WorkManager Configuration: Menggunakan Custom Worker Factory (penting untuk DI manual)
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(AppWorkerFactory(
                apiService,
                bookRepository,
                walletRepository,
                categoryRepository,
                transactionRepository
            ))
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        // 1. Jadwalkan Sinkronisasi setelah semua sistem siap
        SyncManager.schedulePeriodicSync(applicationContext)
    }
}