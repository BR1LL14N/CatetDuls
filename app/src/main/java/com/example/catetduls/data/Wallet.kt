package com.example.catetduls.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

enum class WalletType {
    CASH,
    BANK,
    E_WALLET,
    INVESTMENT
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
    override val id: Int = 0,

    val bookId: Int,

    val name: String,

    val type: WalletType,

    val icon: String = "💰",

    val color: String = "#2196F3",

    val initialBalance: Double = 0.0,

    val currentBalance: Double = 0.0,

    val description: String? = null,
    
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    override val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "server_id")
    override val serverId: String? = null,

    @ColumnInfo(name = "is_synced")
    override val isSynced: Boolean = false,

    @ColumnInfo(name = "is_deleted")
    override val isDeleted: Boolean = false,

    @ColumnInfo(name = "last_sync_at")
    override val lastSyncAt: Long,

    @ColumnInfo(name = "sync_action")
    override val syncAction: String? = null
) : Parcelable, SyncableEntity {

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