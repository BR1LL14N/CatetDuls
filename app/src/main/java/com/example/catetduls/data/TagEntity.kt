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
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) override val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("color") val color: String = "#000000",
    
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
