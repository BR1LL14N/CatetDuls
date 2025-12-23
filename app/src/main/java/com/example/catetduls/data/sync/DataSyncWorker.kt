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
import kotlinx.coroutines.flow.firstOrNull
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

        // REPAIR: Cek data zombie sebelum mulai
        repairZombieData()

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
                Log.e(TAG, "[SYNC] ❌ PUSH sync gagal. Menghentikan worker (Failure).")
                return Result.failure() // Stop retraining on logic/data errors
            }

            // --- PULL: Tarik perubahan dari server ke lokal ---
            Log.d(TAG, "[SYNC] 📥 Memulai PULL sync (Server → Lokal)...")
            val pullSuccess = performPullSync()
            if (!pullSuccess) {
                Log.e(TAG, "[SYNC] ❌ PULL sync gagal. Menghentikan worker (Failure).")
                return Result.failure() // Stop retrying on logic/data errors
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
            return Result.failure() // Stop retrying on critical errors
        }
    }

    private suspend fun performPushSync(): Boolean {
        Log.d(TAG, "[PUSH] 📤 Memulai proses PUSH sync...")

        try {
            // DEBUG: Cek status semua buku sebelum push
            val allBooks = bookRepository.getAllBooks().firstOrNull() ?: emptyList()
            Log.d(TAG, "[DEBUG_BOOK] Total buku di DB: ${allBooks.size}")
            allBooks.forEach { book ->
                Log.d(
                        TAG,
                        "[DEBUG_BOOK] Book ID: ${book.id}, Name: ${book.name}, ServerID: ${book.serverId}, IsSynced: ${book.isSynced}, Action: ${book.syncAction}"
                )
            }

            pushUnitChanges<Book>(
                    repository = bookRepository as SyncRepository<Book>,
                    createApi = { book ->
                        val request =
                                BookRequest(
                                        name = book.name,
                                        description = book.description
                                                        ?: "", // Safety: Handle null description
                                        icon = book.icon ?: "📖", // Safety
                                        color = book.color ?: "#000000" // Safety
                                )
                        apiService.createBook(request)
                    }, // UPDATE: Mapping sama seperti create
                    updateApi = { book ->
                        // Cek ServerID sebelum kirim request
                        val safeServerId =
                                book.serverId
                                        ?: throw IOException(
                                                "Cannot update Book ID ${book.id}: Server ID is missing (NULL)"
                                        )

                        val request =
                                BookRequest(
                                        name = book.name,
                                        description = book.description ?: "",
                                        icon = book.icon ?: "📖",
                                        color = book.color ?: "#000000"
                                )
                        apiService.updateBook(safeServerId, request)
                    },
                    deleteApi = { book ->
                        val safeServerId =
                                book.serverId
                                        ?: throw IOException(
                                                "Cannot delete Book ID ${book.id}: Server ID is missing"
                                        )
                        apiService.deleteBook(safeServerId)
                    }
            )

            pushUnitChanges<Wallet>(
                    repository = walletRepository as SyncRepository<Wallet>,
                    createApi = { wallet ->
                        // Cari Buku Induknya dulu untuk dapatkan Server ID
                        val book = bookRepository.getBookByIdSync(wallet.bookId)
                        // Jika Buku belum naik ke server (serverId null), Wallet JANGAN dikirim
                        // dulu (Tunda)
                        val serverBookId =
                                book?.serverId
                                        ?: throw IOException(
                                                "Induk Buku belum ter-sync, tunda sync Wallet"
                                        )

                        val request =
                                WalletRequest(
                                        bookId = serverBookId, // KIRIM SERVER ID!
                                        name = wallet.name,
                                        type = wallet.type.name, // Enum to String
                                        icon = wallet.icon,
                                        color = wallet.color ?: "#000000",
                                        initialBalance = wallet.initialBalance
                                )
                        apiService.createWallet(request)
                    },
                    updateApi = { wallet ->
                        val book = bookRepository.getBookByIdSync(wallet.bookId)
                        val serverBookId =
                                book?.serverId ?: throw IOException("Induk Buku belum ter-sync")

                        val request =
                                WalletRequest(
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
                        val serverBookId =
                                book?.serverId ?: throw IOException("Induk Buku belum ter-sync")

                        val request =
                                CategoryRequest(
                                        bookId = serverBookId, // KIRIM SERVER ID!
                                        name = category.name,
                                        type = category.type.name, // Enum to String
                                        icon = category.icon
                                )
                        apiService.createCategory(request)
                    },
                    updateApi = { category ->
                        val book = bookRepository.getBookByIdSync(category.bookId)
                        val serverBookId =
                                book?.serverId ?: throw IOException("Induk Buku belum ter-sync")

                        val request =
                                CategoryRequest(
                                        bookId = serverBookId,
                                        name = category.name,
                                        type = category.type.name,
                                        icon = category.icon
                                )
                        // Hati-hati: serverId di DB lokal String, tapi updateCategory minta Long di
                        // ApiService lama?
                        // Sebaiknya samakan jadi String di ApiService (Langkah 5 di atas sudah
                        // pakai String)
                        apiService.updateCategory(category.serverId!!, request)
                    },
                    deleteApi = { category -> apiService.deleteCategory(category.serverId!!) }
            )

            pushComplexChanges<Transaction>(
                    repository = transactionRepository as SyncRepository<Transaction>,
                    createApi = { transaction ->
                        // 1. Cari ID Server untuk BUKU
                        val book = bookRepository.getBookByIdSync(transaction.bookId)
                        if (book == null) {
                            Log.e(
                                    TAG,
                                    "[PUSH_TRANS_FAIL] Book Not Found! Trans ID: ${transaction.id}, Book ID: ${transaction.bookId}"
                            )
                        } else {
                            Log.d(
                                    TAG,
                                    "[PUSH_TRANS_CHECK] Trans ID: ${transaction.id} linked to Book ID: ${book.id}, ServerID: ${book.serverId}"
                            )
                        }
                        val serverBookId =
                                book?.serverId
                                        ?: throw IOException(
                                                "Parent Book belum sync (Trans ID: ${transaction.id}, Book ID: ${transaction.bookId})"
                                        )

                        // 2. Cari ID Server untuk WALLET
                        val wallet =
                                walletRepository.getWalletByIdSync(
                                        transaction.walletId
                                ) // Pastikan fungsi ini ada di Repo
                        val serverWalletId =
                                wallet?.serverId ?: throw IOException("Parent Wallet belum sync")

                        // 3. Cari ID Server untuk CATEGORY
                        val category =
                                categoryRepository.getCategoryByIdSync(
                                        transaction.categoryId
                                ) // Pastikan fungsi ini ada di Repo
                        val serverCategoryId =
                                category?.serverId
                                        ?: throw IOException("Parent Category belum sync")

                        // 4. Bungkus dalam DTO
                        val request =
                                TransactionRequest(
                                        bookId = serverBookId,
                                        walletId = serverWalletId,
                                        categoryId = serverCategoryId,
                                        amount = transaction.amount,
                                        type = transaction.type.name, // Enum ke String
                                        note = transaction.notes,
                                        createdAt =
                                                transaction.date // Pastikan field tanggal sesuai
                                )

                        apiService.createTransaction(request)
                    },
                    updateApi = { transaction ->
                        // Logic lookup ID sama persis dengan createApi di atas
                        val book = bookRepository.getBookByIdSync(transaction.bookId)
                        val serverBookId =
                                book?.serverId ?: throw IOException("Parent Book belum sync")

                        val wallet = walletRepository.getWalletByIdSync(transaction.walletId)
                        val serverWalletId =
                                wallet?.serverId ?: throw IOException("Parent Wallet belum sync")

                        val category =
                                categoryRepository.getCategoryByIdSync(transaction.categoryId)
                        val serverCategoryId =
                                category?.serverId
                                        ?: throw IOException("Parent Category belum sync")

                        val request =
                                TransactionRequest(
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

                // FALLBACK: Jika syncAction NULL (misal dari default data lama), tentukan aksi
                // berdasarkan serverId
                val effectiveAction =
                        item.syncAction ?: if (item.serverId == null) "CREATE" else "UPDATE"

                when (effectiveAction) {
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
                            // Gson sudah otomatis memasukkan nilai JSON "id" ke dalam variabel
                            // .server_id ini
                            val finalServerId =
                                    body.data?.server_id
                                            ?: item.serverId
                                                    ?: throw IOException(
                                                    "Server ID tidak ditemukan di respon API"
                                            )
                            // ========================

                            repository.updateSyncStatus(
                                    item.id.toLong(),
                                    finalServerId,
                                    currentSyncTime
                            )
                        } else {
                            val errorMsg = response.errorBody()?.string()
                            Log.e(
                                    TAG,
                                    "[PUSH] ❌ CREATE Gagal. Code: ${response.code()}, Error: $errorMsg"
                            )
                            throw IOException("CREATE failed: ${response.code()} - $errorMsg")
                        }
                    }
                    "UPDATE" -> {
                        val response = updateApi(item)
                        if (response.isSuccessful) {
                            val sId =
                                    item.serverId
                                            ?: throw IOException(
                                                    "Item lokal tidak punya Server ID saat UPDATE"
                                            )
                            repository.updateSyncStatus(item.id.toLong(), sId, currentSyncTime)
                        } else {
                            val errorMsg = response.errorBody()?.string()
                            Log.e(
                                    TAG,
                                    "[PUSH] ❌ UPDATE Gagal. Code: ${response.code()}, Error: $errorMsg"
                            )
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
                // FALLBACK: Jika syncAction NULL
                val effectiveAction =
                        item.syncAction ?: if (item.serverId == null) "CREATE" else "UPDATE"

                when (effectiveAction) {
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

        var allSuccess = true

        try {
            // 1. PULL BOOKS (Strict Order: Parent First)
            try {
                pullEntityChanges<Book>(
                        remoteApi = apiService.getUpdatedBooks(lastSyncTime),
                        repository = bookRepository as SyncRepository<Book>
                )
            } catch (e: Exception) {
                Log.e(TAG, "[PULL] ❌ Gagal pull Books: ${e.message}")
                return false // STOP: Jangan lanjut ke Child jika Parent gagal
            }

            // 2. PULL WALLETS
            try {
                pullEntityChanges<Wallet>(
                        remoteApi = apiService.getUpdatedWallets(lastSyncTime),
                        repository = walletRepository as SyncRepository<Wallet>
                )
            } catch (e: Exception) {
                Log.e(TAG, "[PULL] ❌ Gagal pull Wallets: ${e.message}")
                return false // STOP
            }

            // 3. PULL CATEGORIES
            try {
                pullEntityChanges<Category>(
                        remoteApi = apiService.getUpdatedCategories(lastSyncTime),
                        repository = categoryRepository as SyncRepository<Category>
                )
            } catch (e: Exception) {
                Log.e(TAG, "[PULL] ❌ Gagal pull Categories: ${e.message}")
                return false // STOP
            }

            // 4. PULL TRANSACTIONS
            try {
                pullPaginatedEntityChanges<Transaction>(
                        remoteApi = apiService.getUpdatedTransactions(lastSyncTime),
                        repository = transactionRepository as SyncRepository<Transaction>
                )
            } catch (e: Exception) {
                Log.e(TAG, "[PULL] ❌ Gagal pull Transactions: ${e.message}")
                return false // STOP
            }

            Log.i(TAG, "[PULL] ✅ PULL sync berhasil diselesaikan.")
            return true
        } finally {
            // --- RECONCILIATION ---
            // Must run to merge "Buku Utama" duplicates even if Sync partially failed (e.g.
            // Categories error)
            try {
                reconcileDefaultBook()
            } catch (e: Exception) {
                Log.e(TAG, "[SYNC] ⚠️ Reconciliation failed: ${e.message}")
            }
        }
    }

    /**
     * Reconciles the "Default Book" state. Problem: A fresh install creates a default "Buku Utama"
     * (ID=1). If sync pulls existing books (ID=2+), the user is stuck on ID=1 which has no server
     * link.
     *
     * Solution:
     * 1. Check if we have multiple books.
     * 2. Identify the "Default Book" (server_id == null, usually ID 1, has 0 transactions).
     * 3. Identify "Synced Books" (server_id != null).
     * 4. If found, SWITCH active book to the first Synced Book and DELETE the empty Default Book.
     */
    private suspend fun reconcileDefaultBook() {
        val allBooks = bookRepository.getAllBooks().firstOrNull() ?: return
        val syncedBooks = allBooks.filter { !it.serverId.isNullOrBlank() }
        val defaultBooks =
                allBooks.filter { it.serverId.isNullOrBlank() && it.syncAction == "CREATE" }

        if (syncedBooks.isNotEmpty() && defaultBooks.isNotEmpty()) {
            Log.d(
                    TAG,
                    "[RECONCILE] Found ${syncedBooks.size} synced books and ${defaultBooks.size} default books."
            )

            for (defaultBook in defaultBooks) {
                // Check if default book is "empty" (no transactions)
                // We use TransactionRepository for this check
                val transactionCount =
                        transactionRepository.getTransactionCountByBookId(defaultBook.id)

                if (transactionCount == 0) {
                    val targetBook = syncedBooks.first()
                    Log.i(
                            TAG,
                            "[RECONCILE] 🔄 Switching active book from local '${defaultBook.name}' (ID ${defaultBook.id}) to synced '${targetBook.name}' (ID ${targetBook.id})"
                    )

                    // 1. Switch Active Book in DB
                    bookRepository.switchActiveBook(targetBook.id)

                    // 2. Switch Active Book in SharedPreferences
                    val prefs =
                            applicationContext.getSharedPreferences(
                                    "app_settings",
                                    Context.MODE_PRIVATE
                            )
                    prefs.edit().putInt("active_book_id", targetBook.id).apply()

                    // 3. Delete the empty default book permanently
                    bookRepository.deleteByIdPermanently(defaultBook.id)
                    Log.i(TAG, "[RECONCILE] 🗑️ Deleted empty default book ID ${defaultBook.id}")
                } else {
                    Log.w(
                            TAG,
                            "[RECONCILE] ⚠️ Default book ID ${defaultBook.id} is NOT empty ($transactionCount transactions). Skipping auto-delete."
                    )
                }
            }
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
        // Removed try-catch purely for logging payload to avoid noise if json fails,
        // or keep it but ensure main loop throws.
        try {
            Log.d(TAG, "Response Data: " + com.google.gson.Gson().toJson(items))
        } catch (_: Exception) {}

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
            Log.d(TAG, "Response Data: " + com.google.gson.Gson().toJson(items))
        } catch (_: Exception) {}

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

        // 2. Buat Objek Notifikasi
        val notification =
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                        .setContentTitle("Sinkronisasi Data")
                        .setContentText(progress)
                        .setSmallIcon(R.drawable.ic_sync_24) // Pastikan icon ini ada di drawable
                        .setOngoing(true)
                        // Tambahkan ini agar user bisa membatalkan jika macet (opsional tapi
                        // disarankan)
                        // .addAction(android.R.drawable.ic_delete, "Batal",
                        // workManager.createCancelPendingIntent(id))
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
    /**
     * Finds items that are marked as Synced (isSynced=true) but have no Server ID (serverId=null).
     * This is an invalid state ("Zombie") and prevents them from being pushed. We reset them to
     * Unsynced with action CREATE.
     */
    private suspend fun repairZombieData() {
        repairEntityState(bookRepository as SyncRepository<Book>, "Book")
        repairEntityState(walletRepository as SyncRepository<Wallet>, "Wallet")
        repairEntityState(categoryRepository as SyncRepository<Category>, "Category")
        repairEntityState(transactionRepository as SyncRepository<Transaction>, "Transaction")
    }

    private suspend inline fun <reified T : SyncableEntity> repairEntityState(
            repository: SyncRepository<T>,
            entityName: String
    ) {
        // Because SyncRepository is generic, we can't easily query "isSynced=1 AND serverId=null"
        // universally
        // without changing every DAO. BUT, we can assume that if we are in this state, we need to
        // be careful.
        // For safety/simplicity in this hotfix, we rely on the fact that if we just "touch" them it
        // might be enough.
        // But actually, we need to identify them.
        // The safest way without new queries is to iterate all local items if the list is not huge.
        // Or better: rely on the specific Repos if they have identifiers.

        // For this specific bug fix, we will try to fetch ALL items (assuming < 1000 for these
        // master entities)
        // and check in memory. For Transaction it might be large, so we skip it for now or rely on
        // specific logs.
        // NOTE: Books/Wallets/Categories are small tables.

        if (entityName == "Transaction") {
            // For transactions, iterating all might be slow.
            // But the critical "Zombie" parent issue is usually about Book/Wallet/Category.
            // If a transaction itself is zombie, it just won't push.
            // We can skip Transaction for now to avoid performance hit, or only do it if count <
            // 500.
            return
        }

        val allItems =
                when (entityName) {
                    "Book" -> (repository as BookRepository).getAllBooks().firstOrNull()
                                    ?: emptyList()
                    "Wallet" -> (repository as WalletRepository).getAllWallets().firstOrNull()
                                    ?: emptyList()
                    "Category" ->
                            (repository as CategoryRepository).getAllCategories().firstOrNull()
                                    ?: emptyList()
                    else -> emptyList()
                }

        @Suppress("UNCHECKED_CAST") val items = allItems as List<T>

        var repairedCount = 0
        for (item in items) {
            if (item.isSynced && item.serverId == null) {
                Log.w(
                        TAG,
                        "[REPAIR] 🧟 Zombie $entityName detected! ID: ${item.id}. Resetting sync status..."
                )
                repository.markAsUnsynced(item.id.toLong(), "CREATE")
                repairedCount++
            }
        }

        if (repairedCount > 0) {
            Log.i(TAG, "[REPAIR] ✅ Successfully repaired $repairedCount zombie $entityName(s).")
        }
    }
}
