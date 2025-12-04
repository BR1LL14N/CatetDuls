package com.example.catetduls.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
//    val phone: String? = null,
//    val bio: String? = null,
    val photo_url: String? = null,
    val email_verified_at: String? = null,
    val created_at: String,
    val updated_at: String,

    val access_token: String? = null,
    val token_expires_at: Long? = null,

    // Sync metadata
    val last_synced_at: Long = System.currentTimeMillis(),
    val is_synced: Boolean = true
)