package com.example.catetduls.data

import android.content.Context
import com.example.catetduls.data.local.TokenManager
import com.example.catetduls.data.remote.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File


class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {

    // ===================================
    // LOCAL OPERATIONS
    // ===================================

    fun getCurrentUserFlow(): Flow<User?> = userDao.getCurrentUserFlow()


    suspend fun getLocalUser(): User? = userDao.getCurrentUser()


    fun isLoggedIn(): Boolean {
        return TokenManager.isLoggedIn(context)
    }

    suspend fun saveUser(user: User) = userDao.insertUser(user)

    suspend fun updateUser(user: User) = userDao.updateUser(user)


    suspend fun logoutLocalOnly() {
        val user = getLocalUser()

        user?.let {
            userDao.clearToken(it.id)
        }
    }

    suspend fun logoutAll() {
        userDao.clearAllTokens()
    }

    suspend fun deleteAccount() {
        val user = getLocalUser()

        user?.let {
            userDao.deleteUserById(it.id)
        }
    }

    // ===================================
    // AUTH OPERATIONS
    // ===================================

    /**
     * REGISTER dengan Refresh Token
     */
    suspend fun register(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ): Result<User> {
        return try {
            val request = RegisterRequest(name, email, password, passwordConfirmation)
            val response = apiService.register(request)

            if (response.isSuccessful && response.body() != null) {

                val apiResponse = response.body()!!

                if (apiResponse.success && apiResponse.data != null) {

                    val authData = apiResponse.data
                    val remoteUser = authData.user


                    val localUser = User(
                        id = remoteUser.id.toString(),
                        name = remoteUser.name,
                        email = remoteUser.email,
                        photo_url = remoteUser.photo_url,
                        email_verified_at = remoteUser.email_verified_at,
                        created_at = remoteUser.created_at ?: "",
                        updated_at = remoteUser.updated_at ?: "",
                        access_token = authData.token,
                        token_expires_at = System.currentTimeMillis() + (authData.expires_in * 1000),
                        last_synced_at = System.currentTimeMillis(),
                        is_synced = true
                    )

                    userDao.deleteAllUsers()
                    userDao.insertUser(localUser)


                    if (authData.refresh_token != null) {
                        TokenManager.saveTokens(
                            context = context,
                            accessToken = authData.token,
                            refreshToken = authData.refresh_token,
                            expiresIn = authData.expires_in
                        )
                    } else {

                        TokenManager.saveToken(context, authData.token)
                    }

                    Result.success(localUser)
                } else {
                    Result.failure(Exception(apiResponse.message ?: "Registrasi gagal"))
                }
            } else {

                val errorBodyString = response.errorBody()?.string()
                val apiErrorResponse: ApiResponse<Any?>? = try {

                    Gson().fromJson(errorBodyString, object : TypeToken<ApiResponse<Any?>>() {}.type)
                } catch (e: Exception) {
                    null
                }

                val errorMsg = when (response.code()) {
                    422 -> apiErrorResponse?.message ?: "Data input tidak valid (422)"
                    500 ->  apiErrorResponse?.message ?: "Server error, coba lagi (500)"
                    else -> "Registrasi gagal: ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Koneksi gagal: ${e.message}"))
        }
    }


    /**
     * LOGIN dengan Refresh Token
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!

                if (apiResponse.success && apiResponse.data != null) {
                    val authData = apiResponse.data
                    val remoteUser = authData.user

                    val localUser = User(
                        id = remoteUser.id.toString(),
                        name = remoteUser.name,
                        email = remoteUser.email,
                        photo_url = remoteUser.photo_url,
                        email_verified_at = remoteUser.email_verified_at,
                        created_at = remoteUser.created_at ?: "",
                        updated_at = remoteUser.updated_at ?: "",
                        access_token = authData.token,
                        token_expires_at = System.currentTimeMillis() + (authData.expires_in * 1000),
                        last_synced_at = System.currentTimeMillis(),
                        is_synced = true
                    )

                    userDao.deleteAllUsers()
                    userDao.insertUser(localUser)

                    if (authData.refresh_token != null) {
                        TokenManager.saveTokens(
                            context = context,
                            accessToken = authData.token,
                            refreshToken = authData.refresh_token,
                            expiresIn = authData.expires_in
                        )
                    } else {

                        TokenManager.saveToken(context, authData.token)
                    }

                    Result.success(localUser)
                } else {
                    Result.failure(Exception(apiResponse.message ?: "Login gagal"))
                }
            } else {
                // Parse error body untuk mendapatkan pesan error yang sebenarnya dari API
                val errorBodyString = response.errorBody()?.string()
                val apiErrorResponse: ApiResponse<Any?>? = try {
                    Gson().fromJson(errorBodyString, object : TypeToken<ApiResponse<Any?>>() {}.type)
                } catch (e: Exception) {
                    null
                }

                // Gunakan pesan dari API jika tersedia, jika tidak gunakan fallback
                val errorMsg = when (response.code()) {
                    401 -> apiErrorResponse?.message ?: "Email atau password salah (401)"
                    404 -> apiErrorResponse?.message ?: "Endpoint tidak ditemukan. Periksa URL API (404)"
                    422 -> apiErrorResponse?.message ?: "Data tidak valid (422)"
                    500 -> apiErrorResponse?.message ?: "Server error, coba lagi (500)"
                    else -> apiErrorResponse?.message ?: "Login gagal: ${response.code()} - ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Koneksi gagal: ${e.message}"))
        }
    }

    /**
     * LOGOUT
     */
    suspend fun logout(): Result<Unit> {
        return try {
            try {
                val token = TokenManager.getAccessToken(context)
                if (token != null) {
                    apiService.logout("Bearer $token")
                }
            } catch (e: Exception) {
            }


            TokenManager.clearTokens(context)


            userDao.deleteAllUsers()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getCurrentUser(): User? {
        return userDao.getCurrentUser()
    }

    suspend fun logoutAllDevices(): Result<String> {
        return try {
            val response = apiService.logoutAll()

            if (response.isSuccessful) {
                logoutAll()
                Result.success(response.body()?.message ?: "Logged out from all devices")
            } else {
                Result.failure(Exception(response.message() ?: "Logout failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Result<User> {
        return try {
            val response = apiService.refreshToken()

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val currentUser = getCurrentUser()

                if (currentUser != null) {

                    val updatedUser = currentUser.copy(
                        access_token = authResponse.access_token,
                        token_expires_at = System.currentTimeMillis() + (authResponse.expires_in * 1000)
                    )
                    userDao.updateUser(updatedUser)
                    Result.success(updatedUser)
                } else {
                    Result.failure(Exception("No user found locally"))
                }
            } else {
                Result.failure(Exception(response.message() ?: "Token refresh failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===================================
    // PASSWORD OPERATIONS
    // ===================================


    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val request = ForgotPasswordRequest(email)
            val response = apiService.forgotPassword(request)

            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Reset email sent")
            } else {
                Result.failure(Exception(response.message() ?: "Request failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(
        email: String,
        token: String,
        password: String,
        passwordConfirmation: String
    ): Result<String> {
        return try {
            val request = ResetPasswordRequest(email, token, password, passwordConfirmation)
            val response = apiService.resetPassword(request)

            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Password reset successful")
            } else {
                Result.failure(Exception(response.message() ?: "Reset failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        newPasswordConfirmation: String
    ): Result<String> {
        return try {
            val request = ChangePasswordRequest(currentPassword, newPassword, newPasswordConfirmation)
            val response = apiService.changePassword(request)

            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Password changed successfully")
            } else {
                Result.failure(Exception(response.message() ?: "Change password failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===================================
    // PROFILE OPERATIONS
    // ===================================

    suspend fun fetchUserProfile(): Result<User> {
        return try {
            val response = apiService.getUserProfile() // Return RemoteUser

            if (response.isSuccessful && response.body() != null) {
                val remoteUser = response.body()!!
                val currentUser = getCurrentUser()

                if (currentUser != null) {

                    val updatedUser = currentUser.copy(
                        name = remoteUser.name,
                        email = remoteUser.email,
                        photo_url = remoteUser.photo_url
                    )
                    userDao.updateUser(updatedUser)
                    Result.success(updatedUser)
                } else {
                    Result.failure(Exception("No local user found"))
                }
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        name: String? = null,
        email: String? = null,
        phone: String? = null,
        bio: String? = null
    ): Result<User> {
        return try {
            val request = UpdateProfileRequest(name, email, phone, bio)
            val response = apiService.updateUserProfile(request)

            // 1. Cek HTTP Success
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!! // Ini ApiResponse<User>

                // 2. Cek Logic Success (flag dari backend)
                if (apiResponse.success && apiResponse.data != null) {
                    val remoteUser = apiResponse.data // Ambil user dari dalam 'data'
                    val currentUser = getCurrentUser()

                    if (currentUser != null) {
                        // Update data user lokal dengan data baru dari remote
                        val updatedUser = currentUser.copy(
                            name = remoteUser.name,
                            email = remoteUser.email,
                            photo_url = remoteUser.photo_url,
                            updated_at = remoteUser.updated_at ?: currentUser.updated_at
                            // Tambahkan field lain jika ada (phone/bio jika didukung DB lokal)
                        )

                        // Simpan ke Room
                        userDao.updateUser(updatedUser)

                        // Return sukses
                        Result.success(updatedUser)
                    } else {
                        Result.failure(Exception("No local user found to update"))
                    }
                } else {
                    // API mengembalikan success: false
                    Result.failure(Exception(apiResponse.message ?: "Update failed"))
                }
            } else {
                // HTTP Error (400, 500, dll)
                Result.failure(Exception(response.message() ?: "HTTP Error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPhoto(file: File): Result<User> {
        return try {
            // 1. Buat RequestBody dari File (Tipe Image)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())

            // 2. Buat MultipartBody.Part
            val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)

            // 3. Panggil API
            val response = apiService.uploadUserPhoto(body)

            // 4. Cek Response
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!! // ApiResponse<PhotoUploadData>

                // Cek flag success dari backend
                if (apiResponse.success && apiResponse.data != null) {
                    val photoData = apiResponse.data // ✅ Ini PhotoUploadData (hanya photo_url)
                    val currentUser = getCurrentUser()

                    if (currentUser != null) {
                        // ✅ Update HANYA photo_url di database lokal
                        val updatedUser = currentUser.copy(
                            photo_url = photoData.photo_url,  // ✅ Ambil dari photoData
                            updated_at = System.currentTimeMillis().toString()  // ✅ Update timestamp
                        )
                        userDao.updateUser(updatedUser)

                        Result.success(updatedUser)
                    } else {
                        Result.failure(Exception("No local user found"))
                    }
                } else {
                    Result.failure(Exception(apiResponse.message ?: "Upload failed"))
                }
            } else {
                Result.failure(Exception(response.message() ?: "HTTP Upload failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePhoto(): Result<String> {
        return try {
            val response = apiService.deleteUserPhoto()

            if (response.isSuccessful) {

                val currentUser = getCurrentUser()
                if (currentUser != null) {

                    userDao.updateUser(currentUser.copy(photo_url = null))
                }
                Result.success(response.body()?.message ?: "Photo deleted")
            } else {
                Result.failure(Exception(response.message() ?: "Delete failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserStatistics(): Result<UserStatistics> {
        return try {
            val response = apiService.getUserStatistics()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message() ?: "Failed to fetch statistics"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccountRemote(): Result<String> {
        return try {
            val response = apiService.deleteAccount()

            if (response.isSuccessful) {
                deleteAccount() // Hapus data lokal
                Result.success(response.body()?.message ?: "Account deleted")
            } else {
                Result.failure(Exception(response.message() ?: "Delete failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}