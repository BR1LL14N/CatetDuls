package com.example.catetduls.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class TransactionType {
    PEMASUKAN,
    PENGELUARAN
}

/**
 * Entity untuk Kategori
 * Sekarang setiap kategori terikat pada satu buku
 */
@Parcelize
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["type"])
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val bookId: Int,

    val name: String,

    val icon: String = "⚙️",

    val type: TransactionType,

    val isDefault: Boolean = false, // Kategori bawaan per buku

    val createdAt: Long = System.currentTimeMillis()
): Parcelable {

    fun isValid(): Boolean {
        return name.isNotBlank() && bookId > 0
    }
}