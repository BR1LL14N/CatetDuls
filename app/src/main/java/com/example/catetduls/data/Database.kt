package com.example.catetduls.data

// import com.example.catetduls.di.NetworkModule.apiService
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room Database untuk FinNote dengan Multi-Book Support
 *
 * Database ini berisi 4 tabel:
 * 1. books - Menyimpan buku/akun
 * 2. wallets - Menyimpan dompet per buku
 * 3. categories - Menyimpan kategori per buku
 * 4. transactions - Menyimpan transaksi per dompet
 */
@Database(
        entities = [Book::class, Wallet::class, Category::class, Transaction::class, User::class],
        version = 3,
        exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * ============================================================ MIGRATION DARI VERSION 2 KE
         * 3 Menambahkan kolom untuk sync functionality dan timestamps
         * ============================================================
         */
        private val MIGRATION_2_3 =
                object : Migration(2, 3) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        val currentTime = System.currentTimeMillis()

                        // Function to safely add column
                        fun safeAddColumn(tableName: String, columnName: String, columnDef: String) {
                            try {
                                database.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnDef")
                            } catch (e: Exception) {
                                // Ignore duplicate column error
                                // Log it ideally, but for now just proceed
                            }
                        }

                        // ========================================
                        // USER TABLE
                        // ========================================
                        safeAddColumn("users", "created_at", "INTEGER NOT NULL DEFAULT $currentTime")
                        safeAddColumn("users", "updated_at", "INTEGER NOT NULL DEFAULT $currentTime")
                        safeAddColumn("users", "server_id", "TEXT")
                        safeAddColumn("users", "is_synced", "INTEGER NOT NULL DEFAULT 0")
                        safeAddColumn("users", "is_deleted", "INTEGER NOT NULL DEFAULT 0")
                        safeAddColumn("users", "last_sync_at", "INTEGER")
                        safeAddColumn("users", "sync_action", "TEXT")

                        // ========================================
                        // BOOKS TABLE
                        // ========================================
                        safeAddColumn("books", "created_at", "INTEGER NOT NULL DEFAULT $currentTime")
                        safeAddColumn("books", "updated_at", "INTEGER NOT NULL DEFAULT $currentTime")
                        safeAddColumn("books", "server_id", "TEXT")
                        safeAddColumn("books", "is_synced", "INTEGER NOT NULL DEFAULT 0")
                        safeAddColumn("books", "is_deleted", "INTEGER NOT NULL DEFAULT 0")
                        safeAddColumn("books", "last_sync_at", "INTEGER")
                        safeAddColumn("books", "sync_action", "TEXT")

                        // ========================================
                        // WALLETS TABLE
                        // ========================================
                        safeAddColumn("wallets", "created_at", "INTEGER NOT NULL DEFAULT $currentTime")
                        safeAddColumn("wallets", "updated_at", "INTEGER NOT NULL DEFAULT $currentTime")
                        safeAddColumn("wallets", "server_id", "TEXT")
                        safeAddColumn("wallets", "is_synced", "INTEGER NOT NULL DEFAULT 0")
                        safeAddColumn("wallets", "is_deleted", "INTEGER NOT NULL DEFAULT 0")
                        safeAddColumn("wallets", "last_sync_at", "INTEGER")
                        safeAddColumn("wallets", "sync_action", "TEXT")

                        // ========================================
                        // CATEGORIES TABLE
                        // ========================================
                        safeAddColumn("categories", "created_at", "INTEGER NOT NULL DEFAULT $currentTime")
                        safeAddColumn("categories", "updated_at", "INTEGER NOT NULL DEFAULT $currentTime")
                        safeAddColumn("categories", "server_id", "TEXT")
                        safeAddColumn("categories", "is_synced", "INTEGER NOT NULL DEFAULT 0")
                        safeAddColumn("categories", "is_deleted", "INTEGER NOT NULL DEFAULT 0")
                        safeAddColumn("categories", "last_sync_at", "INTEGER")
                        safeAddColumn("categories", "sync_action", "TEXT")

                        // ========================================
                        // TRANSACTIONS TABLE
                        // ========================================
                        safeAddColumn("transactions", "created_at", "INTEGER NOT NULL DEFAULT $currentTime")
                        safeAddColumn("transactions", "updated_at", "INTEGER NOT NULL DEFAULT $currentTime")
                        safeAddColumn("transactions", "server_id", "TEXT")
                        safeAddColumn("transactions", "is_synced", "INTEGER NOT NULL DEFAULT 0")
                        safeAddColumn("transactions", "is_deleted", "INTEGER NOT NULL DEFAULT 0")
                        safeAddColumn("transactions", "last_sync_at", "INTEGER")
                        safeAddColumn("transactions", "sync_action", "TEXT")
                    }
                }

        /**
         * ============================================================ MIGRATION DARI VERSION 3 KE
         * 4 Menambahkan kolom currency_code dan currency_symbol di tabel books
         * ============================================================
         */
        private val MIGRATION_3_4 =
                object : Migration(3, 4) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        fun safeAddColumn(sql: String) {
                            try {
                                database.execSQL(sql)
                            } catch (e: Exception) {
                                // Ignore duplicate column error
                            }
                        }

                        // Tambahkan kolom default 'IDR' dan 'Rp'
                        safeAddColumn("ALTER TABLE books ADD COLUMN currency_code TEXT NOT NULL DEFAULT 'IDR'")
                        safeAddColumn("ALTER TABLE books ADD COLUMN currency_symbol TEXT NOT NULL DEFAULT 'Rp'")
                    }
                }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                AppDatabase::class.java,
                                                "finnote_database"
                                        )
                                        .addMigrations(
                                                MIGRATION_2_3,
                                                MIGRATION_3_4
                                        ) // ← TAMBAHKAN INI
                                        .addCallback(DatabaseCallback(context))
                                        // COMMENT UNTUK PRODUCTION:
                                        .fallbackToDestructiveMigration()
                                        .build()

                        INSTANCE = instance
                        instance
                    }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }

    /** Callback untuk mengisi data awal saat database pertama kali dibuat */
    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database -> initializeDefaultData(database) }
            }
        }

        /**
         * Inisialisasi data default:
         * 1. Buat buku default
         * 2. Buat dompet default untuk buku tersebut
         * 3. Buat kategori default untuk buku tersebut
         */
        private suspend fun initializeDefaultData(database: AppDatabase) {
            val bookDao = database.bookDao()
            val walletDao = database.walletDao()
            val categoryDao = database.categoryDao()

            // 1. Buat buku default
            val defaultBook =
                    Book(
                            name = "Buku Utama",
                            description = "Buku keuangan utama",
                            icon = "📖",
                            color = "#4CAF50",
                            isActive = true,
                            lastSyncAt = 0L
                    )
            val bookId = bookDao.insert(defaultBook).toInt()

            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putInt("active_book_id", bookId).apply()

            // 2. Buat dompet default
            insertDefaultWallets(walletDao, bookId)

            // 3. Buat kategori default
            insertDefaultCategories(categoryDao, bookId)
        }

        /** Insert dompet default */
        private suspend fun insertDefaultWallets(walletDao: WalletDao, bookId: Int) {
            val defaultWallets =
                    listOf(
                            Wallet(
                                    bookId = bookId,
                                    name = "Tunai",
                                    type = WalletType.CASH,
                                    icon = "💵",
                                    color = "#4CAF50",
                                    initialBalance = 0.0,
                                    isActive = true,
                                    lastSyncAt = 0L
                            ),
                            Wallet(
                                    bookId = bookId,
                                    name = "Bank",
                                    type = WalletType.BANK,
                                    icon = "🏦",
                                    color = "#2196F3",
                                    initialBalance = 0.0,
                                    isActive = true,
                                    lastSyncAt = 0L
                            ),
                            Wallet(
                                    bookId = bookId,
                                    name = "E-Wallet",
                                    type = WalletType.E_WALLET,
                                    icon = "📱",
                                    color = "#FF9800",
                                    initialBalance = 0.0,
                                    isActive = true,
                                    lastSyncAt = 0L
                            )
                    )

            walletDao.insertAll(defaultWallets)
        }

        /** Insert kategori default */
        private suspend fun insertDefaultCategories(categoryDao: CategoryDao, bookId: Int) {
            val defaultCategories =
                    listOf(
                            // Kategori Pengeluaran
                            Category(
                                    bookId = bookId,
                                    name = "Makanan & Minuman",
                                    icon = "🍔",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Transport",
                                    icon = "🚌",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Belanja",
                                    icon = "🛒",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Hiburan",
                                    icon = "🎮",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Kesehatan",
                                    icon = "💊",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Pendidikan",
                                    icon = "📚",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Tagihan",
                                    icon = "💡",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Rumah Tangga",
                                    icon = "🏠",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Olahraga",
                                    icon = "⚽",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Kecantikan",
                                    icon = "💄",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),

                            // Kategori Pemasukan
                            Category(
                                    bookId = bookId,
                                    name = "Gaji",
                                    icon = "💼",
                                    type = TransactionType.PEMASUKAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Bonus",
                                    icon = "💰",
                                    type = TransactionType.PEMASUKAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Investasi",
                                    icon = "📈",
                                    type = TransactionType.PEMASUKAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Hadiah",
                                    icon = "🎁",
                                    type = TransactionType.PEMASUKAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Freelance",
                                    icon = "💻",
                                    type = TransactionType.PEMASUKAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),

                            // Kategori Lainnya
                            Category(
                                    bookId = bookId,
                                    name = "Lainnya (Pemasukan)",
                                    icon = "⚙️",
                                    type = TransactionType.PEMASUKAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Lainnya (Pengeluaran)",
                                    icon = "⚙️",
                                    type = TransactionType.PENGELUARAN,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            ),
                            Category(
                                    bookId = bookId,
                                    name = "Transfer",
                                    icon = "🔄️",
                                    type = TransactionType.TRANSFER,
                                    isDefault = true,
                                    lastSyncAt = 0L
                            )
                    )

            categoryDao.insertAll(defaultCategories)
        }
    }
}

/** Extension functions untuk mendapatkan repository */
fun Context.getBookRepository(): BookRepository {
    val database = AppDatabase.getDatabase(this)
    return BookRepository(database.bookDao(), database.walletDao(), database.categoryDao())
}

fun Context.getWalletRepository(): WalletRepository {
    val database = AppDatabase.getDatabase(this)
    val bookRepository =
            BookRepository(database.bookDao(), database.walletDao(), database.categoryDao())
    return WalletRepository(database.walletDao(), bookRepository)
}

fun Context.getCategoryRepository(): CategoryRepository {
    val database = AppDatabase.getDatabase(this)
    val bookRepository =
            BookRepository(database.bookDao(), database.walletDao(), database.categoryDao())
    return CategoryRepository(database.categoryDao(), bookRepository)
}

fun Context.getTransactionRepository(): TransactionRepository {
    val database = AppDatabase.getDatabase(this)
    val bookRepository =
            BookRepository(database.bookDao(), database.walletDao(), database.categoryDao())
    return TransactionRepository(database.transactionDao(), bookRepository)
}

// fun Context.getUserRepository(): UserRepository {
//    val database = AppDatabase.getDatabase(this)
//    return UserRepository(database.userDao(), apiService)
// }
