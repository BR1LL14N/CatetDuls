package com.example.catetduls.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.catetduls.data.*
import com.example.catetduls.data.remote.ApiResponse
import com.example.catetduls.data.remote.ApiService
import com.example.catetduls.data.remote.CreateResponse
import java.io.IOException
import retrofit2.Response

// Catatan: Ganti dengan implementasi DI (Hilt/Koin) Anda
class DataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val bookRepository: BookRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val apiService: ApiService
) : CoroutineWorker(appContext, workerParams) {

    // TODO: Ganti dengan mekanisme penyimpanan waktu yang persisten (SharedPrefs/DB)
    private fun getLastSyncAt(): Long = 0L
    private fun setLastSyncAt(time: Long) { /* Implementasi penyimpanan waktu */ }

    override suspend fun doWork(): Result {
        if (!isNetworkConnected(applicationContext)) {
            return Result.retry()
        }

        try {
            // 1. PUSH LOGIC (Local -> Server)
            val pushSuccess = performPushSync()
            if (!pushSuccess) return Result.retry()

            // 2. PULL LOGIC (Server -> Local)
            val pullSuccess = performPullSync()
            if (!pullSuccess) return Result.retry()

            // 3. CLEANUP (Hapus soft-delete yang sudah sukses disinkronkan)
            transactionRepository.cleanupSyncedDeletes()

            // Set waktu sync berhasil
            setLastSyncAt(System.currentTimeMillis())

            return Result.success()

        } catch (e: Exception) {
            // Log e
            return Result.retry()
        }
    }

    // ===================================
    // PUSH LOGIC (Generic)
    // ===================================

    private suspend fun performPushSync(): Boolean {
        // Urutan PUSH penting: Buku -> Dompet -> Kategori -> Transaksi

        // Asumsi BookRepository mengimplementasikan SyncRepository<Book>
        pushEntityChanges(
            repository = bookRepository as SyncRepository<Book>,
            createApi = { book -> apiService.createBook(book) },
            updateApi = { book -> apiService.updateBook(book.serverId!!, book) },
            deleteApi = { book -> apiService.deleteBook(book.serverId!!) }
        )

        pushEntityChanges(
            repository = walletRepository as SyncRepository<Wallet>,
            createApi = { wallet -> apiService.createWallet(wallet) },
            updateApi = { wallet -> apiService.updateWallet(wallet.serverId!!, wallet) },
            deleteApi = { wallet -> apiService.deleteWallet(wallet.serverId!!) }
        )

        // ... Lanjutkan untuk Category dan Transaction

        return true
    }

    private suspend fun <T : SyncableEntity> pushEntityChanges(
        repository: SyncRepository<T>,
        createApi: suspend (T) -> Response<CreateResponse>,
        updateApi: suspend (T) -> Response<Unit>,
        deleteApi: suspend (T) -> Response<Unit>
    ) {
        val unsyncedItems = repository.getAllUnsynced()
        val currentSyncTime = System.currentTimeMillis()

        for (item in unsyncedItems) {
            try {
                when (item.syncAction) {
                    "CREATE" -> {
                        val response = createApi(item)
                        if (response.isSuccessful && response.body() != null) {
                            repository.updateSyncStatus(item.id, response.body()!!.server_id, currentSyncTime)
                        } else throw IOException("CREATE failed: ${response.code()}")
                    }
                    "UPDATE" -> {
                        val response = updateApi(item)
                        if (response.isSuccessful) {
                            repository.updateSyncStatus(item.id, item.serverId!!, currentSyncTime)
                        } else throw IOException("UPDATE failed: ${response.code()}")
                    }
                    "DELETE" -> {
                        val response = deleteApi(item)
                        if (response.isSuccessful) {
                            repository.deleteByIdPermanently(item.id)
                        } else throw IOException("DELETE failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                // Biarkan item unsynced dan coba lagi di kesempatan berikutnya (Result.retry)
                throw e // Meneruskan exception ke doWork
            }
        }
    }

    // ===================================
    // PULL LOGIC
    // ===================================

    private suspend fun performPullSync(): Boolean {
        val lastSyncTime = getLastSyncAt()

        try {
            // PULL Books
            pullEntityChanges(
                remoteApi = apiService.getUpdatedBooks(lastSyncTime),
                repository = bookRepository as SyncRepository<Book>
            )

            // PULL Wallets
            pullEntityChanges(
                remoteApi = apiService.getUpdatedWallets(lastSyncTime),
                repository = walletRepository as SyncRepository<Wallet>
            )

            // PULL Categories
            pullEntityChanges(
                remoteApi = apiService.getUpdatedCategories(lastSyncTime),
                repository = categoryRepository as SyncRepository<Category>
            )

            // PULL Transactions
            pullEntityChanges(
                remoteApi = apiService.getUpdatedTransactions(lastSyncTime),
                repository = transactionRepository as SyncRepository<Transaction>
            )

            return true
        } catch (e: Exception) {
            // Log error
            return false
        }
    }


    private suspend fun <T : SyncableEntity> pullEntityChanges(
        remoteApi: Response<ApiResponse<List<T>>>,
        repository: SyncRepository<T>
    ) {
        val apiBody = remoteApi.body()
            ?: throw IOException("PULL failed: null body, code ${remoteApi.code()}")

        val items = apiBody.data ?: emptyList()

        for (remoteItem in items) {

            val localItem = remoteItem.serverId?.let { repository.getByServerId(it) }

            if (remoteItem.isDeleted) {
                // Delete jika server menandai deleted
                localItem?.let { repository.deleteByIdPermanently(it.id) }

            } else if (localItem == null || remoteItem.updatedAt > localItem.updatedAt) {
                // Data baru atau server punya versi terbaru
                repository.saveFromRemote(remoteItem)
            }
        }
    }



    // ===================================
    // HELPER JARINGAN
    // ===================================

    private fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}