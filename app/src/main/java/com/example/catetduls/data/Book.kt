package com.example.catetduls.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    val name: String,

    val description: String = "",

    val icon: String = "📖",

    val color: String = "#4CAF50",

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
        return name.isNotBlank()
    }
}