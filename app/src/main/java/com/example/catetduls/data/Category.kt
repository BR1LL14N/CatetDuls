package com.example.catetduls.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize

enum class TransactionType {
    PEMASUKAN,
    PENGELUARAN,
    TRANSFER
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
    override val id: Int = 0,

    val bookId: Int,

    val name: String,

    val icon: String = "⚙️",

    val type: TransactionType,

    val isDefault: Boolean = false,

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

): Parcelable, SyncableEntity {

    fun isValid(): Boolean {
        return name.isNotBlank() && bookId > 0
    }
}