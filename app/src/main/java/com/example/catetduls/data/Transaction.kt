package com.example.catetduls.data

import android.os.Parcelable // Tambahkan ini agar @Parcelize jalan
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize // Tambahkan ini

/** Entity untuk Transaksi Sekarang setiap transaksi terikat pada kategori dan dompet */
@Parcelize // Tambahkan ini agar bisa dipassing antar Fragment/Activity
@Entity(
        tableName = "transactions",
        foreignKeys = [
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
                )
        ],
        indices = [
                Index(value = ["categoryId"]),
                Index(value = ["walletId"]),
                Index(value = ["date"]),
                Index(value = ["type"])
        ]
)
data class Transaction(
        @PrimaryKey(autoGenerate = true)
        @SerializedName("local_id")
        override val id: Int = 0,

        val type: TransactionType,

        val amount: Double,

        @SerializedName("category_id")
        val categoryId: Int,

        @SerializedName("wallet_id")
        val walletId: Int,

        // ✅ PERBAIKAN 1: Mapping tanggal dari "created_at_ms" ke "date"
        @SerializedName("created_at_ms")
        val date: Long = System.currentTimeMillis(),

        // ✅ PERBAIKAN 2: Mapping catatan dari "note" ke "notes"
        @SerializedName("note")
        val notes: String = "",

        @ColumnInfo(name = "book_id")
        @SerializedName("book_id")
        val bookId: Int = 0,

        // Opsional: Mapping image_url dari server jika ada
        @ColumnInfo(name = "image_path")
        @SerializedName("image_url")
        val imagePath: String? = null,

        // --- Sync Metadata ---

        @ColumnInfo(name = "created_at")
        val createdAt: Long = System.currentTimeMillis(),

        @ColumnInfo(name = "updated_at")
        override val updatedAt: Long = System.currentTimeMillis(),

        @ColumnInfo(name = "server_id")
        @SerializedName("id") // Mapping ID server ke kolom server_id lokal
        override val serverId: String? = null,

        @ColumnInfo(name = "is_synced")
        override val isSynced: Boolean = false,

        @ColumnInfo(name = "is_deleted")
        override val isDeleted: Boolean = false,

        @ColumnInfo(name = "last_sync_at")
        override val lastSyncAt: Long = 0,

        @ColumnInfo(name = "sync_action")
        override val syncAction: String? = null

) : SyncableEntity, Parcelable { // Tambahkan Parcelable

        fun isIncome(): Boolean = type == TransactionType.PEMASUKAN
        fun isExpense(): Boolean = type == TransactionType.PENGELUARAN

        fun isValid(): Boolean {
                return amount > 0 && categoryId > 0 && walletId > 0
        }
}