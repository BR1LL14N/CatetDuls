package com.example.catetduls.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.catetduls.R
import com.example.catetduls.data.*
import com.example.catetduls.data.local.TokenManager
import com.example.catetduls.data.remote.ApiResponse
import com.example.catetduls.data.remote.ApiService
import com.example.catetduls.data.remote.CreateResponse
import com.example.catetduls.data.remote.MessageResponse
import com.example.catetduls.data.remote.PaginatedData
import com.example.catetduls.utils.ConnectionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import retrofit2.Response

@HiltWorker
class DataSyncWorker
@AssistedInject
constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
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
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "data_sync_channel"
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
        Log.d(TAG, "[SYNC] 🔄 Sync Worker dimulai...")

        // CEK AUTENTIKASI - Jangan sync jika user belum login
        if (!TokenManager.isLoggedIn(applicationContext)) {
            Log.w(TAG, "[SYNC] 🚫 User belum login. Sync dibatalkan (tidak perlu retry).")
            return Result.failure() // Bukan retry, karena user harus login dulu
        }
        Log.d(TAG, "[SYNC] ✅ User terautentikasi. Melanjutkan proses sync...")

        // CEK KONEKSI INTERNET
        if (!ConnectionManager.isOnline(applicationContext)) {
            Log.w(TAG, "[SYNC] 📡 Tidak ada koneksi internet. Menunggu koneksi...")
            return Result.retry()
        }
        Log.d(TAG, "[SYNC] 🌐 Koneksi internet tersedia.")

        // TAMPILKAN NOTIFIKASI SYNC
        setForeground(createForegroundInfo("Memulai sinkronisasi..."))

        try {
            // --- PUSH: Kirim perubahan lokal ke server ---
            Log.d(TAG, "[SYNC] 📤 Memulai PUSH sync (Lokal → Server)...")
            val pushSuccess = performPushSync()
            if (!pushSuccess) {
                Log.e(TAG, "[SYNC] ❌ PUSH sync gagal. Akan mencoba lagi nanti...")
                return Result.retry()
            }

            // --- PULL: Tarik perubahan dari server ke lokal ---
            Log.d(TAG, "[SYNC] 📥 Memulai PULL sync (Server → Lokal)...")
            val pullSuccess = performPullSync()
            if (!pullSuccess) {
                Log.e(TAG, "[SYNC] ❌ PULL sync gagal. Akan mencoba lagi nanti...")
                return Result.retry()
            }

            // --- CLEANUP: Bersihkan data yang sudah dihapus ---
            Log.d(TAG, "[SYNC] 🗑️ Membersihkan data yang sudah di-sync...")
            transactionRepository.cleanupSyncedDeletes()

            val currentTime = System.currentTimeMillis()
            setLastSyncAt(currentTime)
            Log.i(TAG, "[SYNC] ✅ Sinkronisasi selesai! Waktu sync terakhir: $currentTime")

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "[SYNC] ⛔ Error kritis saat sync: ${e.message}", e)
            return Result.retry()
        }
    }

    private suspend fun performPushSync(): Boolean {
        Log.d(TAG, "[PUSH] 📤 Memulai proses PUSH sync...")

        try {
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

            pushComplexChanges<Category>(
                    repository = categoryRepository as SyncRepository<Category>,
                    createApi = { category -> apiService.createCategory(category) },
                    updateApi = { category ->
                        apiService.updateCategory(category.serverId!!.toLong(), category)
                    },
                    deleteApi = { category -> apiService.deleteCategory(category.serverId!!) }
            )

            pushComplexChanges<Transaction>(
                    repository = transactionRepository as SyncRepository<Transaction>,
                    createApi = { transaction -> apiService.createTransaction(transaction) },
                    updateApi = { transaction ->
                        apiService.updateTransaction(transaction.serverId!!.toLong(), transaction)
                    },
                    deleteApi = { transaction ->
                        apiService.deleteTransaction(transaction.serverId!!)
                    }
            )

            Log.i(TAG, "[PUSH] ✅ PUSH sync berhasil diselesaikan.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "[PUSH] ❌ Error saat PUSH sync: ${e.message}", e)
            throw e // Tetap lemparkan error agar doWork bisa menangkap dan me-retry
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

        for (item in unsyncedItems) {
            try {
                when (item.syncAction) {
                    "CREATE" -> {
                        val response = createApi(item)
                        if (response.isSuccessful && response.body() != null) {
                            val serverId =
                                    response.body()!!.data?.server_id
                                            ?: item.serverId
                                                    ?: throw IOException(
                                                    "Server ID missing in response"
                                            )
                            repository.updateSyncStatus(item.id.toLong(), serverId, currentSyncTime)
                        } else throw IOException("CREATE failed: ${response.code()}")
                    }
                    "UPDATE" -> {
                        val response = updateApi(item)
                        if (response.isSuccessful) {
                            repository.updateSyncStatus(
                                    item.id.toLong(),
                                    item.serverId!!,
                                    currentSyncTime
                            )
                        } else throw IOException("UPDATE failed: ${response.code()}")
                    }
                    "DELETE" -> {
                        val response = deleteApi(item)
                        if (response.isSuccessful) {
                            repository.deleteByIdPermanently(item.id.toLong())
                        } else throw IOException("DELETE failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[PUSH] ❌ Aksi PUSH gagal untuk item ID ${item.id}: ${e.message}")
                throw e
            }
        }
    }

    private suspend inline fun <reified T : SyncableEntity> pushComplexChanges(
            repository: SyncRepository<T>,
            createApi: suspend (T) -> Response<CreateResponse>,
            updateApi: suspend (T) -> Response<MessageResponse>,
            deleteApi: suspend (T) -> Response<MessageResponse>
    ) {
        val unsyncedItems = repository.getAllUnsynced()
        val currentSyncTime = System.currentTimeMillis()

        for (item in unsyncedItems) {
            try {
                when (item.syncAction) {
                    "CREATE" -> {
                        val response = createApi(item)
                        if (response.isSuccessful && response.body() != null) {
                            val serverId =
                                    response.body()!!.data?.server_id
                                            ?: item.serverId
                                                    ?: throw IOException(
                                                    "Server ID missing in response"
                                            )
                            repository.updateSyncStatus(item.id.toLong(), serverId, currentSyncTime)
                        } else throw IOException("CREATE failed: ${response.code()}")
                    }
                    "UPDATE" -> {
                        val response = updateApi(item)
                        if (response.isSuccessful) {
                            repository.updateSyncStatus(
                                    item.id.toLong(),
                                    item.serverId!!,
                                    currentSyncTime
                            )
                        } else throw IOException("UPDATE failed: ${response.code()}")
                    }
                    "DELETE" -> {
                        val response = deleteApi(item)
                        if (response.isSuccessful) {
                            repository.deleteByIdPermanently(item.id.toLong())
                        } else throw IOException("DELETE failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[PUSH] ❌ Aksi PUSH gagal untuk item ID ${item.id}: ${e.message}")
                throw e
            }
        }
    }

    private suspend fun performPullSync(): Boolean {
        val lastSyncTime = getLastSyncAt()
        Log.d(TAG, "[PULL] 📥 Memulai PULL sync sejak timestamp: $lastSyncTime...")

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
            pullPaginatedEntityChanges<Transaction>(
                    remoteApi = apiService.getUpdatedTransactions(lastSyncTime),
                    repository = transactionRepository as SyncRepository<Transaction>
            )

            Log.i(TAG, "[PULL] ✅ PULL sync berhasil diselesaikan.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "[PULL] ❌ Error saat PULL sync: ${e.message}", e)
            return false
        }
    }

    private suspend inline fun <reified T : SyncableEntity> pullEntityChanges(
            remoteApi: Response<ApiResponse<List<T>>>,
            repository: SyncRepository<T>
    ) {
        val apiBody =
                remoteApi.body()
                        ?: throw IOException("PULL failed: null body, code ${remoteApi.code()}")

        val items = apiBody.data ?: emptyList()
        Log.d(TAG, "[PULL] 📦 Menerima ${items.size} item untuk ${T::class.java.simpleName}")
        try {
            Log.d(
                    TAG,
                    "Response Data for ${T::class.java.simpleName}: ${com.google.gson.Gson().toJson(items)}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log response data: ${e.message}")
        }

        for (remoteItem in items) {
            val localItem = remoteItem.serverId?.let { repository.getByServerId(it) }

            if (remoteItem.isDeleted) {
                localItem?.let { repository.deleteByIdPermanently(it.id.toLong()) }
            } else if (localItem == null || remoteItem.updatedAt > localItem.updatedAt) {
                repository.saveFromRemote(remoteItem)
            }
        }
    }
    private suspend inline fun <reified T : SyncableEntity> pullPaginatedEntityChanges(
            remoteApi: Response<ApiResponse<PaginatedData<T>>>,
            repository: SyncRepository<T>
    ) {
        val apiBody =
                remoteApi.body()
                        ?: throw IOException("PULL failed: null body, code ${remoteApi.code()}")

        // Extract list from PaginatedData (apiBody.data is PaginatedData, and .data inside it is
        // the list)
        val items = apiBody.data?.data ?: emptyList()
        Log.d(
                TAG,
                "[PULL] 📦 Menerima ${items.size} item (paginated) untuk ${T::class.java.simpleName}"
        )
        try {
            Log.d(
                    TAG,
                    "Response Data for ${T::class.java.simpleName}: ${com.google.gson.Gson().toJson(items)}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log response data: ${e.message}")
        }

        for (remoteItem in items) {
            val localItem = remoteItem.serverId?.let { repository.getByServerId(it) }

            if (remoteItem.isDeleted) {
                localItem?.let { repository.deleteByIdPermanently(it.id.toLong()) }
            } else if (localItem == null || remoteItem.updatedAt > localItem.updatedAt) {
                repository.saveFromRemote(remoteItem)
            }
        }
    }

    /** Buat notifikasi foreground untuk sync */
    private fun createForegroundInfo(progress: String): ForegroundInfo {
        // Buat notification channel untuk Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                                    CHANNEL_ID,
                                    "Sinkronisasi Data",
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply { description = "Notifikasi untuk proses sinkronisasi data" }

            val notificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                            NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Buat notification
        val notification =
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                        .setContentTitle("Sinkronisasi Data")
                        .setContentText(progress)
                        .setSmallIcon(R.drawable.ic_sync_24) // Pastikan icon ini ada
                        .setOngoing(true)
                        .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
