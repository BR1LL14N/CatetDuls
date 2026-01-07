package com.example.catetduls.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "books")
data class Book(
        @PrimaryKey(autoGenerate = true) @SerializedName("local_id") override val id: Int = 0,
        val name: String,
        val description: String = "",
        val icon: String = "📖",
        val color: String = "#4CAF50",
        val isActive: Boolean = true,
        @ColumnInfo(name = "currency_code")
        @SerializedName("currency_code")
        val currencyCode: String? = "IDR",
        @ColumnInfo(name = "currency_symbol")
        @SerializedName("currency_symbol")
        val currencySymbol: String? = "Rp",

        // ========================

        @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
        @ColumnInfo(name = "updated_at") override val updatedAt: Long = System.currentTimeMillis(),
        @ColumnInfo(name = "server_id") @SerializedName("id") override val serverId: String? = null,
        @ColumnInfo(name = "is_synced") override val isSynced: Boolean = false,
        @ColumnInfo(name = "is_deleted") override val isDeleted: Boolean = false,
        @ColumnInfo(name = "last_sync_at") override val lastSyncAt: Long = 0,
        @ColumnInfo(name = "sync_action") override val syncAction: String? = null
) : Parcelable, SyncableEntity {

        fun isValid(): Boolean {
                return name.isNotBlank()
        }

        fun getSafeCurrencyCode(): String = currencyCode ?: "IDR"
        fun getSafeCurrencySymbol(): String = currencySymbol ?: "Rp"
}
