package com.example.catetduls


import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.catetduls.data.*
import com.example.catetduls.data.remote.ApiService
import com.example.catetduls.data.sync.DataSyncWorker


class AppWorkerFactory(
    private val apiService: ApiService,
    private val bookRepository: BookRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            DataSyncWorker::class.java.name ->
                DataSyncWorker(
                    appContext,
                    workerParameters,
                    bookRepository,
                    walletRepository,
                    categoryRepository,
                    transactionRepository,
                    apiService
                )
            else -> null
        }
    }
}
