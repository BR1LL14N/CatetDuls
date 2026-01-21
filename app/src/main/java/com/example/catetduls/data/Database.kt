package com.example.catetduls.data

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
 * Database ini berisi 6 tabel:
 * 1. books - Menyimpan buku/akun
 * 2. wallets - Menyimpan dompet per buku
 * 3. categories - Menyimpan kategori per buku
 * 4. transactions - Menyimpan transaksi per dompet
 * 5. book_closings - Menyimpan data tutup buku periode
 * 6. memos - Menyimpan catatan/memo
 */
@Database(
        entities =
                [
                        Book::class,
                        Wallet::class,
                        Category::class,
                        Transaction::class,
                        User::class,
                        BookClosing::class,
                        Memo::class,
                        TagEntity::class],
        version = 5,
        exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

        abstract fun bookDao(): BookDao
        abstract fun walletDao(): WalletDao
        abstract fun categoryDao(): CategoryDao
        abstract fun transactionDao(): TransactionDao
        abstract fun userDao(): UserDao
        abstract fun bookClosingDao(): BookClosingDao
        abstract fun memoDao(): MemoDao
        abstract fun tagDao(): TagDao

        companion object {
                @Volatile private var INSTANCE: AppDatabase? = null

                /**
                 * ============================================================ MIGRATION DARI
                 * VERSION 2 KE 3 ============================================================
                 */
                private val MIGRATION_2_3 =
                        object : Migration(2, 3) {
                                override fun migrate(database: SupportSQLiteDatabase) {
                                        val currentTime = System.currentTimeMillis()

                                        fun safeAddColumn(
                                                tableName: String,
                                                columnName: String,
                                                columnDef: String
                                        ) {
                                                try {
                                                        database.execSQL(
                                                                "ALTER TABLE $tableName ADD COLUMN $columnName $columnDef"
                                                        )
                                                } catch (e: Exception) {
                                                        // Ignore duplicate column error
                                                }
                                        }

                                        // ========================================
                                        // USER TABLE
                                        // ========================================
                                        safeAddColumn(
                                                "users",
                                                "created_at",
                                                "INTEGER NOT NULL DEFAULT $currentTime"
                                        )
                                        safeAddColumn(
                                                "users",
                                                "updated_at",
                                                "INTEGER NOT NULL DEFAULT $currentTime"
                                        )
                                        safeAddColumn("users", "server_id", "TEXT")
                                        safeAddColumn(
                                                "users",
                                                "is_synced",
                                                "INTEGER NOT NULL DEFAULT 0"
                                        )
                                        safeAddColumn(
                                                "users",
                                                "is_deleted",
                                                "INTEGER NOT NULL DEFAULT 0"
                                        )
                                        safeAddColumn("users", "last_sync_at", "INTEGER")
                                        safeAddColumn("users", "sync_action", "TEXT")

                                        // ========================================
                                        // BOOKS TABLE
                                        // ========================================
                                        safeAddColumn(
                                                "books",
                                                "created_at",
                                                "INTEGER NOT NULL DEFAULT $currentTime"
                                        )
                                        safeAddColumn(
                                                "books",
                                                "updated_at",
                                                "INTEGER NOT NULL DEFAULT $currentTime"
                                        )
                                        safeAddColumn("books", "server_id", "TEXT")
                                        safeAddColumn(
                                                "books",
                                                "is_synced",
                                                "INTEGER NOT NULL DEFAULT 0"
                                        )
                                        safeAddColumn(
                                                "books",
                                                "is_deleted",
                                                "INTEGER NOT NULL DEFAULT 0"
                                        )
                                        safeAddColumn("books", "last_sync_at", "INTEGER")
                                        safeAddColumn("books", "sync_action", "TEXT")

                                        // ========================================
                                        // WALLETS TABLE
                                        // ========================================
                                        safeAddColumn(
                                                "wallets",
                                                "created_at",
                                                "INTEGER NOT NULL DEFAULT $currentTime"
                                        )
                                        safeAddColumn(
                                                "wallets",
                                                "updated_at",
                                                "INTEGER NOT NULL DEFAULT $currentTime"
                                        )
                                        safeAddColumn("wallets", "server_id", "TEXT")
                                        safeAddColumn(
                                                "wallets",
                                                "is_synced",
                                                "INTEGER NOT NULL DEFAULT 0"
                                        )
                                        safeAddColumn(
                                                "wallets",
                                                "is_deleted",
                                                "INTEGER NOT NULL DEFAULT 0"
                                        )
                                        safeAddColumn("wallets", "last_sync_at", "INTEGER")
                                        safeAddColumn("wallets", "sync_action", "TEXT")

                                        // ========================================
                                        // CATEGORIES TABLE
                                        // ========================================
                                        safeAddColumn(
                                                "categories",
                                                "created_at",
                                                "INTEGER NOT NULL DEFAULT $currentTime"
                                        )
                                        safeAddColumn(
                                                "categories",
                                                "updated_at",
                                                "INTEGER NOT NULL DEFAULT $currentTime"
                                        )
                                        safeAddColumn("categories", "server_id", "TEXT")
                                        safeAddColumn(
                                                "categories",
                                                "is_synced",
                                                "INTEGER NOT NULL DEFAULT 0"
                                        )
                                        safeAddColumn(
                                                "categories",
                                                "is_deleted",
                                                "INTEGER NOT NULL DEFAULT 0"
                                        )
                                        safeAddColumn("categories", "last_sync_at", "INTEGER")
                                        safeAddColumn("categories", "sync_action", "TEXT")

                                        // ========================================
                                        // TRANSACTIONS TABLE
                                        // ========================================
                                        safeAddColumn(
                                                "transactions",
                                                "created_at",
                                                "INTEGER NOT NULL DEFAULT $currentTime"
                                        )
                                        safeAddColumn(
                                                "transactions",
                                                "updated_at",
                                                "INTEGER NOT NULL DEFAULT $currentTime"
                                        )
                                        safeAddColumn("transactions", "server_id", "TEXT")
                                        safeAddColumn(
                                                "transactions",
                                                "is_synced",
                                                "INTEGER NOT NULL DEFAULT 0"
                                        )
                                        safeAddColumn(
                                                "transactions",
                                                "is_deleted",
                                                "INTEGER NOT NULL DEFAULT 0"
                                        )
                                        safeAddColumn("transactions", "last_sync_at", "INTEGER")
                                        safeAddColumn("transactions", "sync_action", "TEXT")
                                }
                        }

                /**
                 * ============================================================ MIGRATION DARI
                 * VERSION 3 KE 4 ============================================================
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

                                        safeAddColumn(
                                                "ALTER TABLE books ADD COLUMN currency_code TEXT NOT NULL DEFAULT 'IDR'"
                                        )
                                        safeAddColumn(
                                                "ALTER TABLE books ADD COLUMN currency_symbol TEXT NOT NULL DEFAULT 'Rp'"
                                        )
                                }
                        }

                val MIGRATION_4_5 =
                        object : Migration(4, 5) {
                                override fun migrate(database: SupportSQLiteDatabase) {
                                        // 1. Create Tags table
                                        database.execSQL(
                                                """
                                                CREATE TABLE IF NOT EXISTS `tags` (
                                                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                                                    `name` TEXT NOT NULL, 
                                                    `color` INTEGER NOT NULL, 
                                                    `created_at` INTEGER NOT NULL, 
                                                    `updated_at` INTEGER NOT NULL, 
                                                    `server_id` TEXT, 
                                                    `is_synced` INTEGER NOT NULL, 
                                                    `is_deleted` INTEGER NOT NULL, 
                                                    `last_sync_at` INTEGER NOT NULL, 
                                                    `sync_action` TEXT
                                                )
                                            """
                                        )

                                        // 2. Create index for tags.name
                                        database.execSQL(
                                                "CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)"
                                        )

                                        // 3. Rename Memo.tag to Memo.tags
                                        // Since SQLite doesn't support RENAME COLUMN in older
                                        // versions easily with other constraints,
                                        // we will create a new table and copy data.

                                        // Create new Memo table
                                        database.execSQL(
                                                """
                                                CREATE TABLE IF NOT EXISTS `memos_new` (
                                                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                                                    `book_id` INTEGER NOT NULL, 
                                                    `title` TEXT NOT NULL, 
                                                    `content` TEXT NOT NULL, 
                                                    `tags` TEXT NOT NULL DEFAULT '', 
                                                    `date` INTEGER NOT NULL, 
                                                    `created_at` INTEGER NOT NULL, 
                                                    `updated_at` INTEGER NOT NULL, 
                                                    `server_id` TEXT, 
                                                    `is_synced` INTEGER NOT NULL, 
                                                    `is_deleted` INTEGER NOT NULL, 
                                                    `last_sync_at` INTEGER NOT NULL, 
                                                    `sync_action` TEXT,
                                                    FOREIGN KEY(`book_id`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                                                )
                                            """
                                        )

                                        database.execSQL(
                                                """
                                                INSERT INTO memos_new (id, book_id, title, content, tags, date, created_at, updated_at, server_id, is_synced, is_deleted, last_sync_at, sync_action)
                                                SELECT id, book_id, title, content, tag, date, created_at, updated_at, server_id, is_synced, is_deleted, last_sync_at, sync_action FROM memos
                                            """
                                        )

                                        // Drop old table
                                        database.execSQL("DROP TABLE memos")

                                        // Rename new table to memos
                                        database.execSQL("ALTER TABLE memos_new RENAME TO memos")

                                        // Recreate indices
                                        database.execSQL(
                                                "CREATE INDEX IF NOT EXISTS `index_memos_book_id` ON `memos` (`book_id`)"
                                        )
                                        database.execSQL(
                                                "CREATE INDEX IF NOT EXISTS `index_memos_date` ON `memos` (`date`)"
                                        )
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
                                                                MIGRATION_3_4,
                                                                MIGRATION_4_5
                                                        )
                                                        .addCallback(DatabaseCallback(context))
                                                        // Ensure non-destructive migration if
                                                        // possible, but fallback is set
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

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {

                override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)

                        CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database -> initializeDefaultData(database) }
                        }
                }

                private suspend fun initializeDefaultData(database: AppDatabase) {
                        val bookDao = database.bookDao()
                        val walletDao = database.walletDao()
                        val categoryDao = database.categoryDao()
                        val tagDao = database.tagDao()

                        val defaultBook =
                                Book(
                                        name = "Buku Utama",
                                        description = "Buku keuangan utama",
                                        icon = "📖",
                                        color = "#4CAF50",
                                        isActive = true,
                                        isSynced = false,
                                        syncAction = "CREATE",
                                        lastSyncAt = 0L
                                )
                        val bookId = bookDao.insert(defaultBook).toInt()

                        val prefs =
                                context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                        prefs.edit().putInt("active_book_id", bookId).apply()

                        insertDefaultWallets(walletDao, bookId)

                        insertDefaultCategories(categoryDao, bookId)

                        insertDefaultTags(tagDao)
                }

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
                                                isSynced = false,
                                                syncAction = "CREATE",
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
                                                isSynced = false,
                                                syncAction = "CREATE",
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
                                                isSynced = false,
                                                syncAction = "CREATE",
                                                lastSyncAt = 0L
                                        )
                                )

                        walletDao.insertAll(defaultWallets)
                }

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
                                                isSynced = false,
                                                syncAction = "CREATE",
                                                lastSyncAt = 0L
                                        )
                                )

                        categoryDao.insertAll(defaultCategories)
                }

                private suspend fun insertDefaultTags(tagDao: TagDao) {
                        val defaultTags =
                                listOf(
                                        TagEntity(name = "Penting", color = "#F44336"), // Red
                                        TagEntity(name = "Pribadi", color = "#2196F3"), // Blue
                                        TagEntity(name = "Pekerjaan", color = "#4CAF50"), // Green
                                        TagEntity(name = "Ide", color = "#FFC107"), // Amber
                                        TagEntity(name = "Tagihan", color = "#9C27B0") // Purple
                                )
                        // Check if empty before inserting to avoid duplication
                        if (tagDao.getAllTagsList().isEmpty()) {
                                defaultTags.forEach { tagDao.insert(it) }
                        }
                }
        }
}

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

fun Context.getTagRepository(): TagRepository {
        val database = AppDatabase.getDatabase(this)
        return TagRepository(database.tagDao())
}
