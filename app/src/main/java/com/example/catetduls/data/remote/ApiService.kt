package com.example.catetduls.data.remote

import retrofit2.Response
import retrofit2.http.*
import com.example.catetduls.data.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.MultipartBody


// Asumsi model AuthData yang direspons oleh API
data class AuthData(
    val user: User, // Ini adalah RemoteUser
    val token: String, // Diperbaiki dari 'token'
    val refresh_token: String? = null, // Ditambahkan
    val expires_in: Long, // Ditambahkan, digunakan untuk menghitung token_expires_at
    val token_type: String
)

// Gunakan RemoteUser untuk menghindari konflik dengan model User lokal
data class RemoteUser(
    val id: Int,
    val name: String,
    val email: String,
    val email_verified_at: String?,
    val photo_url: String?,
    val created_at: String?,
    val updated_at: String?
)

data class PhotoUploadData(
    val photo_url: String
)


interface ApiService {
    // ===================================
    // TEST ENDPOINTS
    // ===================================

    @GET("{endpoint}")
    suspend fun dynamicGet(
        @Path("endpoint", encoded = true) endpoint: String,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @POST("{endpoint}")
    suspend fun dynamicPost(
        @Path("endpoint", encoded = true) endpoint: String,
        @Body body: RequestBody,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>


    @PUT("{endpoint}")
    suspend fun dynamicPut(
        @Path("endpoint", encoded = true) endpoint: String,
        @Body body: RequestBody,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @DELETE("{endpoint}")
    suspend fun dynamicDelete(
        @Path("endpoint", encoded = true) endpoint: String,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @GET("ping")
    suspend fun testPing(): Response<ResponseBody>

    @GET("categories")
    suspend fun testGetCategories(): Response<ResponseBody>

    @GET("books")
    suspend fun testGetBooks(): Response<ResponseBody>

    // ===================================
    // AUTH ENDPOINTS
    // ===================================

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthData>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthData>>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<MessageResponse>

    @POST("auth/logout-all")
    suspend fun logoutAll(): Response<MessageResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<User>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>

    // ===================================
    // BOOK ENDPOINTS
    // ===================================

    @GET("books")
    suspend fun getBooks(
        @Query("updatedSince") lastSyncAt: Long? = null
    ): Response<List<Book>>


    @GET("books")
    suspend fun getUpdatedBooks(
        @Query("updatedSince") lastSyncAt: Long
    ): Response<ApiResponse<List<Book>>>

    @GET("books/{id}")
    suspend fun getBook(@Path("id") bookId: String): Response<Book>

    @POST("books")
    suspend fun createBook(@Body book: Book): Response<CreateResponse>

    @PUT("books/{id}")
    suspend fun updateBook(
        @Path("id") serverId: String,
        @Body book: Book
    ): Response<Unit>

    @DELETE("books/{id}")
    suspend fun deleteBook(@Path("id") serverId: String): Response<Unit>

    @GET("books/{id}/categories")
    suspend fun getBookCategories(@Path("id") bookId: String): Response<List<Category>>

    @GET("books/{id}/wallets")
    suspend fun getBookWallets(@Path("id") bookId: String): Response<List<Wallet>>

    // ===================================
    // WALLET ENDPOINTS
    // ===================================

    @GET("wallets")
    suspend fun getWallets(
        @Query("updatedSince") lastSyncAt: Long? = null
    ): Response<List<Wallet>>

    // PULL: Get Updated Wallets
    @GET("wallets")
    suspend fun getUpdatedWallets(
        @Query("updatedSince") lastSyncAt: Long
    ): Response<ApiResponse<List<Wallet>>>

    @GET("wallets/{id}")
    suspend fun getWallet(@Path("id") walletId: String): Response<Wallet>

    @POST("wallets")
    suspend fun createWallet(@Body wallet: Wallet): Response<CreateResponse>

    @PUT("wallets/{id}")
    suspend fun updateWallet(
        @Path("id") serverId: String,
        @Body wallet: Wallet
    ): Response<Unit>

    @DELETE("wallets/{id}")
    suspend fun deleteWallet(@Path("id") serverId: String): Response<Unit>

    // ===================================
    // CATEGORY ENDPOINTS
    // ===================================

    @GET("categories")
    suspend fun getCategories(
        @Query("updatedSince") lastSyncAt: Long? = null
    ): Response<List<Category>>

    @GET("categories")
    suspend fun getUpdatedCategories(
        @Query("updatedSince") lastSyncAt: Long
    ): Response<ApiResponse<List<Category>>>


    @GET("categories/{id}")
    suspend fun getCategory(@Path("id") categoryId: String): Response<Category>

    @POST("categories")
    suspend fun createCategory(@Body category: Category): Response<CreateResponse>

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") serverId: Long, @Body category: Category): Response<MessageResponse>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") serverId: String): Response<MessageResponse>

    @GET("pub-categories")
    suspend fun getPublicCategories(): Response<List<Category>>

    // ===================================
    // TRANSACTION ENDPOINTS
    // ===================================

    @GET("transactions")
    suspend fun getTransactions(
        @Query("updatedSince") lastSyncAt: Long? = null
    ): Response<List<Transaction>>

    @GET("transactions")
    suspend fun getUpdatedTransactions(
        @Query("updatedSince") lastSyncAt: Long
    ): Response<ApiResponse<List<Transaction>>>

    @GET("transactions/{id}")
    suspend fun getTransaction(@Path("id") transactionId: String): Response<Transaction>

    @POST("transactions")
    suspend fun createTransaction(@Body transaction: Transaction): Response<CreateResponse>

    @PUT("transactions/{id}")
    suspend fun updateTransaction(@Path("id") serverId: Long, @Body transaction: Transaction): Response<MessageResponse>

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") serverId: String): Response<MessageResponse>

    @POST("transactions/bulk-delete")
    suspend fun bulkDeleteTransactions(@Body request: BulkDeleteRequest): Response<MessageResponse>

    @GET("transactions/by-category")
    suspend fun getTransactionsByCategory(
        @Query("categoryId") categoryId: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<List<Transaction>>

    @GET("transactions/by-date")
    suspend fun getTransactionsByDate(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<Transaction>>

    @GET("transactions/summary")
    suspend fun getTransactionsSummary(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<TransactionSummary>

    // ===================================
    // USER ENDPOINTS
    // ===================================

    @GET("users")
    suspend fun getUsers(): Response<List<User>>

    @GET("users/{user}")
    suspend fun getUser(@Path("user") userId: String): Response<User>

    @GET("user/profile")
    suspend fun getUserProfile(): Response<User>

    @GET("user/profile")
    suspend fun getUserProfileWithPhoto(): Response<ApiResponse<UserProfileData>>

    @PUT("user/profile")
    suspend fun updateUserProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<User>>

    @Multipart
    @POST("user/photo")
    suspend fun uploadUserPhoto(
        @Part photo: MultipartBody.Part
    ): Response<ApiResponse<PhotoUploadData>>
    @DELETE("user/photo")
    suspend fun deleteUserPhoto(): Response<MessageResponse>

    @GET("user/preferences")
    suspend fun getUserPreferences(): Response<UserPreferences>

    @PUT("user/preferences")
    suspend fun updateUserPreferences(@Body preferences: UserPreferences): Response<UserPreferences>

    @GET("user/statistics")
    suspend fun getUserStatistics(): Response<UserStatistics>

    @DELETE("user/account")
    suspend fun deleteAccount(): Response<MessageResponse>
}

// ===================================
// REQUEST/RESPONSE DATA CLASSES
// ===================================

data class CreateResponse(
    val server_id: String,
    val message: String
)

data class MessageResponse(
    val message: String
)

data class AuthResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Long,
    val user: User
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val token: String,
    val password: String,
    val password_confirmation: String
)

data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String,
    val new_password_confirmation: String
)

data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val bio: String? = null
)

data class UploadPhotoRequest(
    val photo: String // Base64 encoded image
)

data class BulkDeleteRequest(
    val ids: List<String>
)

data class UserPreferences(
    val theme: String? = null,
    val language: String? = null,
    val currency: String? = null,
    val notifications_enabled: Boolean? = null
)

data class UserStatistics(
    val total_transactions: Int,
    val total_income: Double,
    val total_expense: Double,
    val total_books: Int,
    val total_wallets: Int,
    val total_categories: Int
)

data class TransactionSummary(
    val total_income: Double,
    val total_expense: Double,
    val balance: Double,
    val transaction_count: Int,
    val by_category: List<CategorySummary>? = null
)

data class CategorySummary(
    val category_id: String,
    val category_name: String,
    val total_amount: Double,
    val transaction_count: Int
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val email_verified_at: String?,
    val photo_url: String?, // Pastikan ini ada
    val created_at: String?,
    val updated_at: String?,
    // Tambahkan field lainnya jika perlu
)

// Atau buat data class khusus
data class UserProfileData(
    val user: User,
    val photo_url: String?
)

data class PhotoUrlData(
    val photo_url: String
)