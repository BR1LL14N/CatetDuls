package com.example.catetduls.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity untuk tabel categories
 *
 * Kategori default yang bisa dibuat:
 * - Makanan & Minuman 🍔
 * - Transport 🚌
 * - Belanja 🛒
 * - Hiburan 🎮
 * - Kesehatan 💊
 * - Pendidikan 📚
 * - Tagihan 💡
 * - Gaji 💼
 * - Lainnya ⚙️
 */

enum class TransactionType {
    PEMASUKAN,
    PENGELUARAN
}
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String, // Nama kategori

    val icon: String = "⚙️", // Emoji icon

    val type: TransactionType, //hanya boleh pemasukkan dan pengeluaran

    val isDefault: Boolean = false // Kategori default tidak bisa dihapus
)
