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
 * Room Database untuk FinNote
 *
 * Database ini berisi 2 tabel:
 * 1. categories - Menyimpan kategori transaksi
 * 2. transactions - Menyimpan semua transaksi keuangan
 */
@Database(
    entities = [Category::class, Transaction::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class) // <-- [PERBAIKAN 2] Daftarkan Converters
abstract class AppDatabase : RoomDatabase() {

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
                    insertDefaultCategories(database.categoryDao())
                }
            }
        }

        /**
         * Insert kategori default
         */
        // --- [PERBAIKAN 3] Seluruh fungsi ini diubah untuk menggunakan Enum ---
        private suspend fun insertDefaultCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                // Kategori Pengeluaran
                Category(
                    name = "Makanan & Minuman",
                    icon = "🍔",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Transport",
                    icon = "🚌",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Belanja",
                    icon = "🛒",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Hiburan",
                    icon = "🎮",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Kesehatan",
                    icon = "💊",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Pendidikan",
                    icon = "📚",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Tagihan",
                    icon = "💡",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Rumah Tangga",
                    icon = "🏠",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Olahraga",
                    icon = "⚽",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Kecantikan",
                    icon = "💄",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                ),

                // Kategori Pemasukan
                Category(
                    name = "Gaji",
                    icon = "💼",
                    type = TransactionType.PEMASUKAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Bonus",
                    icon = "💰",
                    type = TransactionType.PEMASUKAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Investasi",
                    icon = "📈",
                    type = TransactionType.PEMASUKAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Hadiah",
                    icon = "🎁",
                    type = TransactionType.PEMASUKAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Freelance",
                    icon = "💻",
                    type = TransactionType.PEMASUKAN, // <-- Diperbaiki
                    isDefault = true
                ),

                // Kategori Umum (Logika "Semua" dihilangkan)
                Category(
                    name = "Lainnya (Pemasukan)",
                    icon = "⚙️",
                    type = TransactionType.PEMASUKAN, // <-- Diperbaiki
                    isDefault = true
                ),
                Category(
                    name = "Lainnya (Pengeluaran)",
                    icon = "⚙️",
                    type = TransactionType.PENGELUARAN, // <-- Diperbaiki
                    isDefault = true
                )
            )

            categoryDao.insertAll(defaultCategories)
        }
    }
}

/**
 * Extension function untuk mendapatkan repository
 * (Bagian ini sudah benar, tidak perlu diubah)
 */
fun Context.getTransactionRepository(): TransactionRepository {
    val database = AppDatabase.getDatabase(this)
    return TransactionRepository(database.transactionDao())
}

fun Context.getCategoryRepository(): CategoryRepository {
    val database = AppDatabase.getDatabase(this)
    return CategoryRepository(database.categoryDao())
}