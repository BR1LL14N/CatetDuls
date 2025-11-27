package com.example.catetduls.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class WalletType {
    CASH,      // Tunai
    BANK,      // Bank
    E_WALLET,  // E-Wallet (GoPay, OVO, dll)
    INVESTMENT // Investasi
}

/**
 * Entity untuk Dompet
 * Setiap dompet terikat pada satu buku
 */
@Parcelize
@Entity(
    tableName = "wallets",
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
data class Wallet(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val bookId: Int, // Foreign key ke Book

    val name: String,

    val type: WalletType,

    val icon: String = "💰",

    val color: String = "#2196F3",

    val initialBalance: Double = 0.0, // Saldo awal

    val currentBalance: Double = 0.0, // Saldo saat ini (dihitung dari transaksi)

    val description: String = "",

    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {

    fun isValid(): Boolean {
        return name.isNotBlank() && bookId > 0
    }

    fun getDisplayName(): String {
        return when (type) {
            WalletType.CASH -> "💵 $name"
            WalletType.BANK -> "🏦 $name"
            WalletType.E_WALLET -> "📱 $name"
            WalletType.INVESTMENT -> "📈 $name"
        }
    }
}