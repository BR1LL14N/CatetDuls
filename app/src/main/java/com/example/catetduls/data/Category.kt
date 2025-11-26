package com.example.catetduls.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class TransactionType {
    PEMASUKAN,
    PENGELUARAN
}
@Parcelize
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val icon: String = "⚙️",

    val type: TransactionType,
    val isDefault: Boolean = false
): Parcelable
