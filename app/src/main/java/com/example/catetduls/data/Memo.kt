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
        tableName = "memos",
        foreignKeys =
                [
                        ForeignKey(
                                entity = Book::class,
                                parentColumns = ["id"],
                                childColumns = ["book_id"],
                                onDelete = ForeignKey.CASCADE
                        )],
        indices = [
                Index(value = ["book_id"]),
                Index(value = ["date"])
        ]
)
data class Memo(
        @PrimaryKey(autoGenerate = true) @SerializedName("local_id") override val id: Int = 0,
        @ColumnInfo(name = "book_id") @SerializedName("book_id") val bookId: Int,
        val title: String,
        val content: String,
        val tags: String = "", // Comma separated tags
        val date: Long = System.currentTimeMillis(),

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
) : SyncableEntity, Parcelable {
    fun isValid(): Boolean {
        return title.isNotBlank() && content.isNotBlank()
    }
}
