package com.example.catetduls.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity untuk tabel transactions
 *
 * Struktur tabel:
 * - id: Primary key (auto-generated)
 * - type: "Pemasukan" atau "Pengeluaran"
 * - amount: Jumlah uang (dalam Rupiah)
 * - categoryId: Foreign key ke tabel categories
 * - date: Timestamp dalam milliseconds
 * - notes: Catatan tambahan (opsional)
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE // Jika kategori dihapus, transaksi juga terhapus
        )
    ],
    indices = [
        Index(value = ["categoryId"]), // Index untuk mempercepat query berdasarkan kategori
        Index(value = ["date"]), // Index untuk mempercepat query berdasarkan tanggal
        Index(value = ["type"]) // Index untuk mempercepat query berdasarkan tipe
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val type: TransactionType, // "Pemasukan" atau "Pengeluaran"

    val amount: Double, // Jumlah dalam Rupiah

    val categoryId: Int, // Foreign key ke Category

    val date: Long = System.currentTimeMillis(), // Timestamp

    val notes: String = "" // Catatan opsional
) {
    // Helper function ini sekarang lebih sederhana
    fun isIncome(): Boolean = type == TransactionType.PEMASUKAN
    fun isExpense(): Boolean = type == TransactionType.PENGELUARAN

    // Anda bisa hapus fungsi isValid() atau sesuaikan
    fun isValid(): Boolean {
        return amount > 0 && categoryId > 0
    }
}