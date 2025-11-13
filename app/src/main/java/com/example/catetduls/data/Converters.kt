package com.example.catetduls.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name // Menyimpan Enum sebagai String (misal: "PEMASUKAN")
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        // Mengubah String dari DB kembali menjadi Enum
        return TransactionType.valueOf(value)
    }
}