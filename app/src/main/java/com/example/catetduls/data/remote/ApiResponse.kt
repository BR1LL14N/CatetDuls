package com.example.catetduls.data.remote

data class ApiResponse<T>(val success: Boolean, val message: String? = null, val data: T? = null)

// data class AuthData(
//    val user: User,        // Object User dari API
//    val token: String,     // "token" (bukan access_token)
//    val token_type: String // "Bearer"
// )

// 3. User dari API (Remote User)
// data class User(
//    val id: Int,
//    val name: String,
//    val email: String,
//    val email_verified_at: String?,
//    val photo_url: String?,
//    val created_at: String?,
//    val updated_at: String?
// )

data class PaginatedData<T>(
        val data: List<T>,
        val current_page: Int,
        val last_page: Int,
        val per_page: Int,
        val total: Int
)
