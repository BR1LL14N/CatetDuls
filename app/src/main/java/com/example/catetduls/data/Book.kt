package com.example.catetduls.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName // Jangan lupa import ini
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

        // === PERBAIKAN DI SINI ===
        // 1. Tambahkan @SerializedName untuk mapping JSON snake_case
        // 2. Ubah tipe menjadi String? (Nullable) untuk mencegah crash "Parameter specified as non-null is null"

        @ColumnInfo(name = "currency_code")
        @SerializedName("currency_code")
        val currencyCode: String? = "IDR",

        @ColumnInfo(name = "currency_symbol")
        @SerializedName("currency_symbol")
        val currencySymbol: String? = "Rp",

        // ========================

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
        override val lastSyncAt: Long = 0, // Beri default value 0 agar aman

        @ColumnInfo(name = "sync_action")
        override val syncAction: String? = null

) : Parcelable, SyncableEntity {

        fun isValid(): Boolean {
                return name.isNotBlank()
        }

        // Helper untuk mendapatkan kode mata uang yang aman (tidak null)
        fun getSafeCurrencyCode(): String = currencyCode ?: "IDR"
        fun getSafeCurrencySymbol(): String = currencySymbol ?: "Rp"
}
