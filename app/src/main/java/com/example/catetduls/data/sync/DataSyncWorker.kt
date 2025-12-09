package com.example.catetduls.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.catetduls.data.*
import com.example.catetduls.data.remote.ApiResponse
import com.example.catetduls.data.remote.ApiService
import com.example.catetduls.data.remote.CreateResponse
import com.example.catetduls.data.remote.MessageResponse
import retrofit2.Response
import java.io.IOException

class DataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val bookRepository: BookRepository,
    private val walletRepository: WalletRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val apiService: ApiService
) : CoroutineWorker(appContext, workerParams) {


    companion object {
        private const val TAG = "DataSyncWorker"
        private const val PREF_NAME = "sync_prefs"
        private const val KEY_LAST_SYNC = "last_sync_at"
    }

    private fun getLastSyncAt(): Long {
        val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    private fun setLastSyncAt(time: Long) {
        val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC, time).apply()
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔄 SYNC WORKER STARTED. Checking network...")

        if (!isNetworkConnected(applicationContext)) {
            Log.w(TAG, "❌ Network not connected. Retrying...")
            return Result.retry()
        }

        try {
            // --- PUSH ---
            Log.d(TAG, "📤 Starting PUSH sync (Local -> Server)...")
            val pushSuccess = performPushSync()
            if (!pushSuccess) {
                Log.e(TAG, "❌ Push sync failed. Retrying.")
                return Result.retry()
            }

            // --- PULL ---
            Log.d(TAG, "📥 Starting PULL sync (Server -> Local)...")
            val pullSuccess = performPullSync()
            if (!pullSuccess) {
                Log.e(TAG, "❌ Pull sync failed. Retrying.")
                return Result.retry()
            }

            // --- CLEANUP (Hanya jika PULL sukses) ---
            Log.d(TAG, "🗑️ Cleanup synced deletes...")
            transactionRepository.cleanupSyncedDeletes()

            val currentTime = System.currentTimeMillis()
            setLastSyncAt(currentTime)
            Log.i(TAG, "✅ Sync successful! Last sync: $currentTime")

            return Result.success()

        } catch (e: Exception) {
            // Log ini harus menangkap kegagalan inisialisasi tak terduga (misalnya DB/Repo crash)
            Log.e(TAG, "⛔ Critical sync error: ${e.message}", e)
            return Result.retry()
        }
    }

    private suspend fun performPushSync(): Boolean {
        Log.d(TAG, "📤 Starting PUSH sync...")

        try {
            // PUSH UNIT CHANGES (Book & Wallet)
            pushUnitChanges<Book>(
                repository = bookRepository as SyncRepository<Book>,
                createApi = { book -> apiService.createBook(book) },
                updateApi = { book -> apiService.updateBook(book.serverId!!, book) },
                deleteApi = { book -> apiService.deleteBook(book.serverId!!) }
            )

            pushUnitChanges<Wallet>(
                repository = walletRepository as SyncRepository<Wallet>,
                createApi = { wallet -> apiService.createWallet(wallet) },
                updateApi = { wallet -> apiService.updateWallet(wallet.serverId!!, wallet) },
                deleteApi = { wallet -> apiService.deleteWallet(wallet.serverId!!) }
            )

            // PUSH COMPLEX CHANGES (Category & Transaction)
            pushComplexChanges<Category>(
                repository = categoryRepository as SyncRepository<Category>,
                createApi = { category -> apiService.createCategory(category) },
                updateApi = { category -> apiService.updateCategory(category.serverId!!, category) },
                deleteApi = { category -> apiService.deleteCategory(category.serverId!!) }
            )

            pushComplexChanges<Transaction>(
                repository = transactionRepository as SyncRepository<Transaction>,
                createApi = { transaction -> apiService.createTransaction(transaction) },
                updateApi = { transaction -> apiService.updateTransaction(transaction.serverId!!, transaction) },
                deleteApi = { transaction -> apiService.deleteTransaction(transaction.serverId!!) }
            )

            Log.i(TAG, "✅ PUSH sync completed successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during PUSH sync: ${e.message}", e)
            return false
        }
    }

    private suspend inline fun <reified T : SyncableEntity> pushUnitChanges(
        repository: SyncRepository<T>,
        createApi: suspend (T) -> Response<CreateResponse>,
        updateApi: suspend (T) -> Response<Unit>,
        deleteApi: suspend (T) -> Response<Unit>
    ) {
        val unsyncedItems = repository.getAllUnsynced()
        val currentSyncTime = System.currentTimeMillis()

        Log.d(TAG, "Found ${unsyncedItems.size} unsynced items for ${T::class.java.simpleName}")

        for (item in unsyncedItems) {
            try {
                when (item.syncAction) {
                    "CREATE" -> {
                        val response = createApi(item)
                        if (response.isSuccessful && response.body() != null) {
                            repository.updateSyncStatus(item.id.toLong(), response.body()!!.server_id, currentSyncTime)
                            Log.i(TAG, "CREATE SUCCESS: ${T::class.java.simpleName} ID ${item.id} -> ${response.body()!!.server_id}")
                        } else throw IOException("CREATE failed: ${response.code()}")
                    }
                    "UPDATE" -> {
                        val response = updateApi(item)
                        if (response.isSuccessful) {
                            repository.updateSyncStatus(item.id.toLong(), item.serverId!!, currentSyncTime)
                            Log.i(TAG, "UPDATE SUCCESS: ${T::class.java.simpleName} ID ${item.id}")
                        } else throw IOException("UPDATE failed: ${response.code()}")
                    }
                    "DELETE" -> {
                        val response = deleteApi(item)
                        if (response.isSuccessful) {
                            repository.deleteByIdPermanently(item.id.toLong())
                            Log.i(TAG, "DELETE SUCCESS (Permanently removed local copy): ${T::class.java.simpleName} ID ${item.id}")
                        } else throw IOException("DELETE failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Push action failed for ID ${item.id}: ${e.message}")
                throw e
            }
        }
    }

    private suspend inline fun <reified T : SyncableEntity> pushComplexChanges(
        repository: SyncRepository<T>,
        createApi: suspend (T) -> Response<CreateResponse>,
        updateApi: suspend (T) -> Response<CreateResponse>,
        deleteApi: suspend (T) -> Response<MessageResponse>
    ) {
        val unsyncedItems = repository.getAllUnsynced()
        val currentSyncTime = System.currentTimeMillis()

        Log.d(TAG, "Found ${unsyncedItems.size} unsynced items for ${T::class.java.simpleName}")

        for (item in unsyncedItems) {
            try {
                when (item.syncAction) {
                    "CREATE" -> {
                        val response = createApi(item)
                        if (response.isSuccessful && response.body() != null) {
                            repository.updateSyncStatus(item.id.toLong(), response.body()!!.server_id, currentSyncTime)
                            Log.i(TAG, "CREATE SUCCESS: ${T::class.java.simpleName} ID ${item.id} -> ${response.body()!!.server_id}")
                        } else throw IOException("CREATE failed: ${response.code()}")
                    }
                    "UPDATE" -> {
                        val response = updateApi(item)
                        if (response.isSuccessful) {
                            repository.updateSyncStatus(item.id.toLong(), item.serverId!!, currentSyncTime)
                            Log.i(TAG, "UPDATE SUCCESS: ${T::class.java.simpleName} ID ${item.id}")
                        } else throw IOException("UPDATE failed: ${response.code()}")
                    }
                    "DELETE" -> {
                        val response = deleteApi(item)
                        if (response.isSuccessful) {
                            repository.deleteByIdPermanently(item.id.toLong())
                            Log.i(TAG, "DELETE SUCCESS (Permanently removed local copy): ${T::class.java.simpleName} ID ${item.id}")
                        } else throw IOException("DELETE failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Push action failed for ID ${item.id}: ${e.message}")
                throw e
            }
        }
    }


    private suspend fun performPullSync(): Boolean {
        val lastSyncTime = getLastSyncAt()
        Log.d(TAG, "📥 Starting PULL sync since $lastSyncTime...")

        try {
            pullEntityChanges<Book>(
                remoteApi = apiService.getUpdatedBooks(lastSyncTime),
                repository = bookRepository as SyncRepository<Book>
            )

            pullEntityChanges<Wallet>(
                remoteApi = apiService.getUpdatedWallets(lastSyncTime),
                repository = walletRepository as SyncRepository<Wallet>
            )

            pullEntityChanges<Category>(
                remoteApi = apiService.getUpdatedCategories(lastSyncTime),
                repository = categoryRepository as SyncRepository<Category>
            )

            pullEntityChanges<Transaction>(
                remoteApi = apiService.getUpdatedTransactions(lastSyncTime),
                repository = transactionRepository as SyncRepository<Transaction>
            )

            Log.i(TAG, "✅ PULL sync completed successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during PULL sync: ${e.message}", e)
            return false
        }
    }

    private suspend inline fun <reified T : SyncableEntity> pullEntityChanges(
        remoteApi: Response<ApiResponse<List<T>>>,
        repository: SyncRepository<T>
    ) {
        val apiBody = remoteApi.body()
            ?: throw IOException("PULL failed: null body, code ${remoteApi.code()}")

        val items = apiBody.data ?: emptyList()
        Log.d(TAG, "PULL received ${items.size} items for ${T::class.java.simpleName}")

        for (remoteItem in items) {
            val localItem = remoteItem.serverId?.let { repository.getByServerId(it) }

            if (remoteItem.isDeleted) {
                localItem?.let { repository.deleteByIdPermanently(it.id.toLong()) }
            } else if (localItem == null || remoteItem.updatedAt > localItem.updatedAt) {
                repository.saveFromRemote(remoteItem)
            }
        }
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}