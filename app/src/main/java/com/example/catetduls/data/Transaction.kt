package com.example.catetduls.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["date"]),
        Index(value = ["type"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val type: TransactionType,

    val amount: Double,

    val categoryId: Int,

    val date: Long = System.currentTimeMillis(),

    val notes: String = ""
) {

    fun isIncome(): Boolean = type == TransactionType.PEMASUKAN
    fun isExpense(): Boolean = type == TransactionType.PENGELUARAN


    fun isValid(): Boolean {
        return amount > 0 && categoryId > 0
    }
}