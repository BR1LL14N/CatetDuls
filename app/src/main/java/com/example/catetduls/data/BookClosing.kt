package com.example.catetduls.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
        tableName = "book_closings",
        foreignKeys =
                [
                        ForeignKey(
                                entity = Book::class,
                                parentColumns = ["id"],
                                childColumns = ["book_id"],
                                onDelete = ForeignKey.CASCADE
                        )],
        indices =
                [
                        Index(value = ["book_id"]),
                        Index(value = ["period_start"]),
                        Index(value = ["period_end"])]
)
data class BookClosing(
        @PrimaryKey(autoGenerate = true) @SerializedName("local_id") override val id: Int = 0,
        @ColumnInfo(name = "book_id") @SerializedName("book_id") val bookId: Int,
        @ColumnInfo(name = "period_start") @SerializedName("period_start") val periodStart: Long,
        @ColumnInfo(name = "period_end") @SerializedName("period_end") val periodEnd: Long,
        @ColumnInfo(name = "period_label") @SerializedName("period_label") val periodLabel: String,
        @ColumnInfo(name = "closed_at")
        @SerializedName("closed_at")
        val closedAt: Long = System.currentTimeMillis(),
        @ColumnInfo(name = "final_balance")
        @SerializedName("final_balance")
        val finalBalance: Double,
        @ColumnInfo(name = "is_verified")
        @SerializedName("is_verified")
        val isVerified: Boolean = false,
        val notes: String = "",

        // Sync metadata
        @ColumnInfo(name = "created_at")
        @SerializedName("created_at_ms")
        val createdAt: Long = System.currentTimeMillis(),
        @ColumnInfo(name = "updated_at")
        @SerializedName("updated_at_ms")
        override val updatedAt: Long = System.currentTimeMillis(),
        @ColumnInfo(name = "server_id") @SerializedName("id") override val serverId: String? = null,
        @ColumnInfo(name = "is_synced") override val isSynced: Boolean = false,
        @ColumnInfo(name = "is_deleted") override val isDeleted: Boolean = false,
        @ColumnInfo(name = "last_sync_at") override val lastSyncAt: Long = 0,
        @ColumnInfo(name = "sync_action") override val syncAction: String? = null
) : SyncableEntity, Parcelable
