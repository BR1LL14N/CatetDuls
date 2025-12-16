package com.example.catetduls.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/** Entity untuk Transaksi Sekarang setiap transaksi terikat pada kategori dan dompet */
@Entity(
        tableName = "transactions",
        foreignKeys =
                [
                        ForeignKey(
                                entity = Category::class,
                                parentColumns = ["id"],
                                childColumns = ["categoryId"],
                                onDelete = ForeignKey.CASCADE
                        ),
                        ForeignKey(
                                entity = Wallet::class,
                                parentColumns = ["id"],
                                childColumns = ["walletId"],
                                onDelete = ForeignKey.CASCADE
                        )],
        indices =
                [
                        Index(value = ["categoryId"]),
                        Index(value = ["walletId"]),
                        Index(value = ["date"]),
                        Index(value = ["type"])]
)
data class Transaction(
        @PrimaryKey(autoGenerate = true) override val id: Int = 0,
        val type: TransactionType,
        val amount: Double,
        @com.google.gson.annotations.SerializedName("category_id") val categoryId: Int,
        @com.google.gson.annotations.SerializedName("wallet_id") val walletId: Int,
        val date: Long = System.currentTimeMillis(),
        val notes: String = "",
        @ColumnInfo(name = "book_id")
        @SerializedName("book_id") // Agar cocok dengan JSON API "book_id"
        val bookId: Int = 0,
        @ColumnInfo(name = "image_path") val imagePath: String? = null,
        @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
        @ColumnInfo(name = "updated_at") override val updatedAt: Long = System.currentTimeMillis(),
        @ColumnInfo(name = "server_id") override val serverId: String? = null,
        @ColumnInfo(name = "is_synced") override val isSynced: Boolean = false,
        @ColumnInfo(name = "is_deleted") override val isDeleted: Boolean = false,
        @ColumnInfo(name = "last_sync_at") override val lastSyncAt: Long,
        @ColumnInfo(name = "sync_action") override val syncAction: String? = null
) : SyncableEntity {

    fun isIncome(): Boolean = type == TransactionType.PEMASUKAN
    fun isExpense(): Boolean = type == TransactionType.PENGELUARAN

    fun isValid(): Boolean {
        return amount > 0 && categoryId > 0 && walletId > 0
    }
}
