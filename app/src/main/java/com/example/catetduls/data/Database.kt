package com.example.catetduls.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    entities = [
        Book::class,
        Wallet::class,
        Category::class,
        Transaction::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finnote_database"
                )
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigration() // Untuk development
                    .build()

                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }

    /**
     * Callback untuk mengisi data awal saat database pertama kali dibuat
     */
    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    initializeDefaultData(database)
                }
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
            val defaultBook = Book(
                name = "Buku Utama",
                description = "Buku keuangan utama",
                icon = "📖",
                color = "#4CAF50",
                isActive = true
            )
            val bookId = bookDao.insert(defaultBook).toInt()

            val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putInt("active_book_id", bookId).apply()

            // 2. Buat dompet default
            insertDefaultWallets(walletDao, bookId)

            // 3. Buat kategori default
            insertDefaultCategories(categoryDao, bookId)
        }

        /**
         * Insert dompet default
         */
        private suspend fun insertDefaultWallets(walletDao: WalletDao, bookId: Int) {
            val defaultWallets = listOf(
                Wallet(
                    bookId = bookId,
                    name = "Tunai",
                    type = WalletType.CASH,
                    icon = "💵",
                    color = "#4CAF50",
                    initialBalance = 0.0,
                    isActive = true
                ),
                Wallet(
                    bookId = bookId,
                    name = "Bank",
                    type = WalletType.BANK,
                    icon = "🏦",
                    color = "#2196F3",
                    initialBalance = 0.0,
                    isActive = true
                ),
                Wallet(
                    bookId = bookId,
                    name = "E-Wallet",
                    type = WalletType.E_WALLET,
                    icon = "📱",
                    color = "#FF9800",
                    initialBalance = 0.0,
                    isActive = true
                )
            )

            walletDao.insertAll(defaultWallets)
        }

        /**
         * Insert kategori default
         */
        private suspend fun insertDefaultCategories(categoryDao: CategoryDao, bookId: Int) {
            val defaultCategories = listOf(
                // Kategori Pengeluaran
                Category(
                    bookId = bookId,
                    name = "Makanan & Minuman",
                    icon = "🍔",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Transport",
                    icon = "🚌",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Belanja",
                    icon = "🛒",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Hiburan",
                    icon = "🎮",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Kesehatan",
                    icon = "💊",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Pendidikan",
                    icon = "📚",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Tagihan",
                    icon = "💡",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Rumah Tangga",
                    icon = "🏠",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Olahraga",
                    icon = "⚽",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Kecantikan",
                    icon = "💄",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                ),

                // Kategori Pemasukan
                Category(
                    bookId = bookId,
                    name = "Gaji",
                    icon = "💼",
                    type = TransactionType.PEMASUKAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Bonus",
                    icon = "💰",
                    type = TransactionType.PEMASUKAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Investasi",
                    icon = "📈",
                    type = TransactionType.PEMASUKAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Hadiah",
                    icon = "🎁",
                    type = TransactionType.PEMASUKAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Freelance",
                    icon = "💻",
                    type = TransactionType.PEMASUKAN,
                    isDefault = true
                ),

                // Kategori Lainnya
                Category(
                    bookId = bookId,
                    name = "Lainnya (Pemasukan)",
                    icon = "⚙️",
                    type = TransactionType.PEMASUKAN,
                    isDefault = true
                ),
                Category(
                    bookId = bookId,
                    name = "Lainnya (Pengeluaran)",
                    icon = "⚙️",
                    type = TransactionType.PENGELUARAN,
                    isDefault = true
                )
            )

            categoryDao.insertAll(defaultCategories)
        }
    }
}

/**
 * Extension functions untuk mendapatkan repository
 */
fun Context.getBookRepository(): BookRepository {
    val database = AppDatabase.getDatabase(this)
    return BookRepository(database.bookDao())
}

fun Context.getWalletRepository(): WalletRepository {
    val database = AppDatabase.getDatabase(this)
    return WalletRepository(database.walletDao())
}

fun Context.getCategoryRepository(): CategoryRepository {
    val database = AppDatabase.getDatabase(this)
    return CategoryRepository(database.categoryDao())
}

fun Context.getTransactionRepository(): TransactionRepository {
    val database = AppDatabase.getDatabase(this)
    return TransactionRepository(database.transactionDao())
}