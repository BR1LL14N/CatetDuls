package com.example.catetduls.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        // Singleton instance
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Mendapatkan instance database
         * Menggunakan pattern Singleton untuk memastikan hanya ada 1 instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finnote_database"
                )
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigration() // Hapus data lama jika ada perubahan skema
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Untuk testing - bisa reset database
         */
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

            // Jalankan di background thread
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    // Insert kategori default
                    insertDefaultCategories(database.categoryDao())
                }
            }
        }

        /**
         * Insert kategori default
         */
        private suspend fun insertDefaultCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                // Kategori Pengeluaran
                Category(
                    name = "Makanan & Minuman",
                    icon = "🍔",
                    type = "Pengeluaran",
                    isDefault = true
                ),
                Category(
                    name = "Transport",
                    icon = "🚌",
                    type = "Pengeluaran",
                    isDefault = true
                ),
                Category(
                    name = "Belanja",
                    icon = "🛒",
                    type = "Pengeluaran",
                    isDefault = true
                ),
                Category(
                    name = "Hiburan",
                    icon = "🎮",
                    type = "Pengeluaran",
                    isDefault = true
                ),
                Category(
                    name = "Kesehatan",
                    icon = "💊",
                    type = "Pengeluaran",
                    isDefault = true
                ),
                Category(
                    name = "Pendidikan",
                    icon = "📚",
                    type = "Pengeluaran",
                    isDefault = true
                ),
                Category(
                    name = "Tagihan",
                    icon = "💡",
                    type = "Pengeluaran",
                    isDefault = true
                ),
                Category(
                    name = "Rumah Tangga",
                    icon = "🏠",
                    type = "Pengeluaran",
                    isDefault = true
                ),
                Category(
                    name = "Olahraga",
                    icon = "⚽",
                    type = "Pengeluaran",
                    isDefault = true
                ),
                Category(
                    name = "Kecantikan",
                    icon = "💄",
                    type = "Pengeluaran",
                    isDefault = true
                ),

                // Kategori Pemasukan
                Category(
                    name = "Gaji",
                    icon = "💼",
                    type = "Pemasukan",
                    isDefault = true
                ),
                Category(
                    name = "Bonus",
                    icon = "💰",
                    type = "Pemasukan",
                    isDefault = true
                ),
                Category(
                    name = "Investasi",
                    icon = "📈",
                    type = "Pemasukan",
                    isDefault = true
                ),
                Category(
                    name = "Hadiah",
                    icon = "🎁",
                    type = "Pemasukan",
                    isDefault = true
                ),
                Category(
                    name = "Freelance",
                    icon = "💻",
                    type = "Pemasukan",
                    isDefault = true
                ),

                // Kategori Umum
                Category(
                    name = "Lainnya",
                    icon = "⚙️",
                    type = "Semua",
                    isDefault = true
                )
            )

            categoryDao.insertAll(defaultCategories)
        }
    }
}

/**
 * Extension function untuk mendapatkan repository
 * Bisa digunakan di Activity/Fragment tanpa Hilt
 */
fun Context.getTransactionRepository(): TransactionRepository {
    val database = AppDatabase.getDatabase(this)
    return TransactionRepository(database.transactionDao())
}

fun Context.getCategoryRepository(): CategoryRepository {
    val database = AppDatabase.getDatabase(this)
    return CategoryRepository(database.categoryDao())
}