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
import com.example.catetduls.data.remote.BookRequest
import com.example.catetduls.data.remote.CategoryRequest
import com.example.catetduls.data.remote.CreateResponse
import com.example.catetduls.data.remote.MessageResponse
import com.example.catetduls.data.remote.PaginatedData
import com.example.catetduls.data.remote.TransactionRequest
import com.example.catetduls.data.remote.WalletRequest
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
                createApi = { book ->
                    val request = BookRequest(
                        name = book.name,
                        description = book.description ?: "", // Safety: Handle null description
                        icon = book.icon ?: "📖", // Safety
                        color = book.color ?: "#000000" // Safety
                    )
                    apiService.createBook(request)
                },// UPDATE: Mapping sama seperti create
                updateApi = { book ->
                    // Cek ServerID sebelum kirim request
                    val safeServerId = book.serverId
                        ?: throw IOException("Cannot update Book ID ${book.id}: Server ID is missing (NULL)")

                    val request = BookRequest(
                        name = book.name,
                        description = book.description ?: "",
                        icon = book.icon ?: "📖",
                        color = book.color ?: "#000000"
                    )
                    apiService.updateBook(safeServerId, request)
                },

                deleteApi = { book ->
                    val safeServerId = book.serverId
                        ?: throw IOException("Cannot delete Book ID ${book.id}: Server ID is missing")
                    apiService.deleteBook(safeServerId)
                }
            )

            pushUnitChanges<Wallet>(
                    repository = walletRepository as SyncRepository<Wallet>,
                createApi = { wallet ->
                    // Cari Buku Induknya dulu untuk dapatkan Server ID
                    val book = bookRepository.getBookByIdSync(wallet.bookId)
                    // Jika Buku belum naik ke server (serverId null), Wallet JANGAN dikirim dulu (Tunda)
                    val serverBookId = book?.serverId ?: throw IOException("Induk Buku belum ter-sync, tunda sync Wallet")

                    val request = WalletRequest(
                        bookId = serverBookId, // KIRIM SERVER ID!
                        name = wallet.name,
                        type = wallet.type.name, // Enum to String
                        icon = wallet.icon,
                        color = wallet.color ?: "#000000",
                        initialBalance = wallet.initialBalance
                    )
                    apiService.createWallet(request)
                }, updateApi = { wallet ->
                    val book = bookRepository.getBookByIdSync(wallet.bookId)
                    val serverBookId = book?.serverId ?: throw IOException("Induk Buku belum ter-sync")

                    val request = WalletRequest(
                        bookId = serverBookId,
                        name = wallet.name,
                        type = wallet.type.name,
                        icon = wallet.icon,
                        color = wallet.color ?: "#000000",
                        initialBalance = wallet.initialBalance
                    )
                    apiService.updateWallet(wallet.serverId!!, request)
                },
                deleteApi = { wallet -> apiService.deleteWallet(wallet.serverId!!) }
            )

            pushComplexChanges<Category>(
                    repository = categoryRepository as SyncRepository<Category>,
                createApi = { category ->
                    val book = bookRepository.getBookByIdSync(category.bookId)
                    val serverBookId = book?.serverId ?: throw IOException("Induk Buku belum ter-sync")

                    val request = CategoryRequest(
                        bookId = serverBookId, // KIRIM SERVER ID!
                        name = category.name,
                        type = category.type.name, // Enum to String
                        icon = category.icon
                    )
                    apiService.createCategory(request)
                },
                updateApi = { category ->
                    val book = bookRepository.getBookByIdSync(category.bookId)
                    val serverBookId = book?.serverId ?: throw IOException("Induk Buku belum ter-sync")

                    val request = CategoryRequest(
                        bookId = serverBookId,
                        name = category.name,
                        type = category.type.name,
                        icon = category.icon
                    )
                    // Hati-hati: serverId di DB lokal String, tapi updateCategory minta Long di ApiService lama?
                    // Sebaiknya samakan jadi String di ApiService (Langkah 5 di atas sudah pakai String)
                    apiService.updateCategory(category.serverId!!, request)
                },
                deleteApi = { category -> apiService.deleteCategory(category.serverId!!) }
            )

            pushComplexChanges<Transaction>(
                    repository = transactionRepository as SyncRepository<Transaction>,
                createApi = { transaction ->
                    // 1. Cari ID Server untuk BUKU
                    val book = bookRepository.getBookByIdSync(transaction.bookId)
                    val serverBookId = book?.serverId ?: throw IOException("Parent Book belum sync")

                    // 2. Cari ID Server untuk WALLET
                    val wallet = walletRepository.getWalletByIdSync(transaction.walletId) // Pastikan fungsi ini ada di Repo
                    val serverWalletId = wallet?.serverId ?: throw IOException("Parent Wallet belum sync")

                    // 3. Cari ID Server untuk CATEGORY
                    val category = categoryRepository.getCategoryByIdSync(transaction.categoryId) // Pastikan fungsi ini ada di Repo
                    val serverCategoryId = category?.serverId ?: throw IOException("Parent Category belum sync")

                    // 4. Bungkus dalam DTO
                    val request = TransactionRequest(
                        bookId = serverBookId,
                        walletId = serverWalletId,
                        categoryId = serverCategoryId,
                        amount = transaction.amount,
                        type = transaction.type.name, // Enum ke String
                        note = transaction.notes,
                        createdAt = transaction.date // Pastikan field tanggal sesuai
                    )

                    apiService.createTransaction(request)
                },
                updateApi = { transaction ->
                    // Logic lookup ID sama persis dengan createApi di atas
                    val book = bookRepository.getBookByIdSync(transaction.bookId)
                    val serverBookId = book?.serverId ?: throw IOException("Parent Book belum sync")

                    val wallet = walletRepository.getWalletByIdSync(transaction.walletId)
                    val serverWalletId = wallet?.serverId ?: throw IOException("Parent Wallet belum sync")

                    val category = categoryRepository.getCategoryByIdSync(transaction.categoryId)
                    val serverCategoryId = category?.serverId ?: throw IOException("Parent Category belum sync")

                    val request = TransactionRequest(
                        bookId = serverBookId,
                        walletId = serverWalletId,
                        categoryId = serverCategoryId,
                        amount = transaction.amount,
                        type = transaction.type.name,
                        note = transaction.notes,
                        createdAt = transaction.date
                    )

                    // Pastikan konversi ServerID (String -> Long) aman jika server minta Long
                    apiService.updateTransaction(transaction.serverId!!.toLong(), request)
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
                Log.d(TAG, "[PUSH] Processing item ID: ${item.id}, Action: ${item.syncAction}")

                when (item.syncAction) {
                    "CREATE" -> {
                        val response = createApi(item)

                        if (response.isSuccessful) {
                            val body = response.body()

                            if (body == null) {
                                Log.e(TAG, "[PUSH] ❌ Response Body NULL untuk item ID ${item.id}")
                                throw IOException("Response Body is NULL")
                            }

                            Log.d(TAG, "[PUSH] ✅ CREATE Success. Data: $body")

                            // === PERBAIKAN DI SINI ===
                            // Kita panggil .server_id (sesuai class CreatedData Anda)
                            // Gson sudah otomatis memasukkan nilai JSON "id" ke dalam variabel .server_id ini
                            val finalServerId = body.data?.server_id
                                ?: item.serverId
                                ?: throw IOException("Server ID tidak ditemukan di respon API")
                            // ========================

                            repository.updateSyncStatus(item.id.toLong(), finalServerId, currentSyncTime)

                        } else {
                            val errorMsg = response.errorBody()?.string()
                            Log.e(TAG, "[PUSH] ❌ CREATE Gagal. Code: ${response.code()}, Error: $errorMsg")
                            throw IOException("CREATE failed: ${response.code()} - $errorMsg")
                        }
                    }
                    "UPDATE" -> {
                        val response = updateApi(item)
                        if (response.isSuccessful) {
                            val sId = item.serverId ?: throw IOException("Item lokal tidak punya Server ID saat UPDATE")
                            repository.updateSyncStatus(
                                item.id.toLong(),
                                sId,
                                currentSyncTime
                            )
                        } else {
                            val errorMsg = response.errorBody()?.string()
                            Log.e(TAG, "[PUSH] ❌ UPDATE Gagal. Code: ${response.code()}, Error: $errorMsg")
                            throw IOException("UPDATE failed: ${response.code()}")
                        }
                    }
                    "DELETE" -> {
                        val response = deleteApi(item)
                        if (response.isSuccessful) {
                            repository.deleteByIdPermanently(item.id.toLong())
                        } else {
                            Log.e(TAG, "[PUSH] ❌ DELETE Gagal. Code: ${response.code()}")
                            throw IOException("DELETE failed: ${response.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[PUSH] ❌ Exception processing item ID ${item.id}: ${e.message}", e)
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
        // 1. Buat Notification Channel (Wajib untuk Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sinkronisasi Data",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi untuk proses sinkronisasi data"
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Buat Objek Notifikasi
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Sinkronisasi Data")
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_sync_24) // Pastikan icon ini ada di drawable
            .setOngoing(true)
            // Tambahkan ini agar user bisa membatalkan jika macet (opsional tapi disarankan)
            // .addAction(android.R.drawable.ic_delete, "Batal", workManager.createCancelPendingIntent(id))
            .build()

        // 3. Return ForegroundInfo dengan Tipe Service (FIX UTAMA)
        return if (Build.VERSION.SDK_INT >= 34) { // Android 14+
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            // Android 13 ke bawah
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
