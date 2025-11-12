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

    val type: String, // "Pemasukan" atau "Pengeluaran"

    val amount: Double, // Jumlah dalam Rupiah

    val categoryId: Int, // Foreign key ke Category

    val date: Long = System.currentTimeMillis(), // Timestamp

    val notes: String = "" // Catatan opsional
) {
    /**
     * Helper function untuk validasi
     */
    fun isValid(): Boolean {
        return amount > 0 &&
                type.isNotBlank() &&
                (type == "Pemasukan" || type == "Pengeluaran") &&
                categoryId > 0
    }

    /**
     * Format amount ke Rupiah
     */
    fun getFormattedAmount(): String {
        return "Rp ${String.format("%,.0f", amount)}"
    }

    /**
     * Apakah transaksi ini pemasukan?
     */
    fun isIncome(): Boolean = type == "Pemasukan"

    /**
     * Apakah transaksi ini pengeluaran?
     */
    fun isExpense(): Boolean = type == "Pengeluaran"
}