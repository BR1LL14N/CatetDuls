package com.example.catetduls.data

import androidx.room.Embedded

/**
* Result untuk total harian
**/
data class DailyTotal(
    val date: Long,
    val income: Double,
    val expense: Double
)

/**
 * Result untuk statistik kategori detail
 */
data class CategoryStats(
    val categoryId: Int,
    val categoryName: String,
    val transactionCount: Int,
    val total: Double,
    val average: Double
)


/**
 * Result untuk total pengeluaran per kategori (Pie Chart)
 */
data class CategoryExpense(
    val categoryId: Int,
    val categoryName: String,
    val total: Double
)

/**
 * Result untuk total bulanan (Bar Chart)
 */
data class MonthlyTotal(
    val month: Int,
    val year: Int,
    val income: Double,
    val expense: Double,
    val balance: Double
)

data class CategoryWithCount(
    @Embedded
    val category: Category,

    val transactionCount: Int
)

data class DailySummary(
    val dayOfMonth: Int, // Nomor hari dalam bulan (1 - 31)
    val totalIncome: Double,
    val totalExpense: Double
)


/**
 * Sealed class untuk hasil validasi
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}