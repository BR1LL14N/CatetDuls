package com.example.catetduls.data

import com.example.catetduls.data.*
import com.example.catetduls.data.UserDao
import com.example.catetduls.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.Response
import com.example.catetduls.data.remote.User as RemoteUser
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File


class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService
) {

    // ===================================
    // LOCAL OPERATIONS
    // ===================================

    fun getCurrentUserFlow(): Flow<User?> = userDao.getCurrentUserFlow()

    suspend fun getCurrentUser(): User? = userDao.getCurrentUser()

    suspend fun isLoggedIn(): Boolean = userDao.isLoggedIn() > 0

    suspend fun saveUser(user: User) = userDao.insertUser(user)

    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun logout() {
        val user = getCurrentUser()
        user?.let {
            // Konversi ID ke String karena DAO minta String
            userDao.clearToken(it.id.toString())
        }
    }

    suspend fun logoutAll() {
        userDao.clearAllTokens()
    }

    suspend fun deleteAccount() {
        val user = getCurrentUser()
        user?.let {
            userDao.deleteUserById(it.id.toString())
        }
    }

    // ===================================
    // AUTH OPERATIONS
    // ===================================

    suspend fun register(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ): Result<User> {
        return try {
            val request = RegisterRequest(name, email, password, passwordConfirmation)
            val response = apiService.register(request)

            // 1. Cek HTTP Success
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!

                // 2. Cek Logic Success & Data Availability
                // Data ada di dalam apiResponse.data
                if (apiResponse.success && apiResponse.data != null) {
                    val authData = apiResponse.data
                    val remoteUser: RemoteUser = authData.user

                    // ✅ MAPPING FIXED
                    val localUser = User(
                        // Fix: Int -> String
                        id = remoteUser.id.toString(),
                        name = remoteUser.name,
                        email = remoteUser.email,
                        photo_url = remoteUser.photo_url,
                        email_verified_at = remoteUser.email_verified_at,

                        // Fix: String? -> String (Handle null dengan default empty string)
                        created_at = remoteUser.created_at ?: "",
                        updated_at = remoteUser.updated_at ?: "",

                        // Fix: Ambil token dari authData.token (sesuai JSON)
                        access_token = authData.token,
                        // Fix: JSON Sanctum tidak kirim expires_in, set default (misal 1 tahun) atau null
                        token_expires_at = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),

                        // Metadata Sync
                        last_synced_at = System.currentTimeMillis(),
                        is_synced = true
                    )

                    // Simpan ke Database Lokal
                    userDao.deleteAllUsers()
                    userDao.insertUser(localUser)

                    Result.success(localUser)
                } else {
                    Result.failure(Exception(apiResponse.message ?: "Registration failed"))
                }
            } else {
                Result.failure(Exception(response.message() ?: "Registration HTTP error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!

                if (apiResponse.success && apiResponse.data != null) {
                    val authData = apiResponse.data
                    val remoteUser: RemoteUser = authData.user

                    val localUser = User(
                        id = remoteUser.id.toString(),
                        name = remoteUser.name,
                        email = remoteUser.email,
                        photo_url = remoteUser.photo_url,
                        email_verified_at = remoteUser.email_verified_at,
                        created_at = remoteUser.created_at ?: "",
                        updated_at = remoteUser.updated_at ?: "",
                        access_token = authData.token,
                        token_expires_at = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),
                        last_synced_at = System.currentTimeMillis(),
                        is_synced = true
                    )

                    userDao.deleteAllUsers()
                    userDao.insertUser(localUser)

                    Result.success<User>(localUser)  // ✅ Tambahkan <User>
                } else {
                    Result.failure<User>(Exception(apiResponse.message ?: "Login failed"))  // ✅ Tambahkan <User>
                }
            } else {
                Result.failure<User>(Exception(response.message() ?: "Login HTTP error"))  // ✅ Tambahkan <User>
            }
        } catch (e: Exception) {
            Result.failure<User>(e)  // ✅ Tambahkan <User>
        }
    }

    suspend fun logoutRemote(): Result<String> {
        return try {
            val response = apiService.logout()

            if (response.isSuccessful) {
                logout() // Hapus token lokal
                Result.success(response.body()?.message ?: "Logged out successfully")
            } else {
                Result.failure(Exception(response.message() ?: "Logout failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logoutAllDevices(): Result<String> {
        return try {
            val response = apiService.logoutAll()

            if (response.isSuccessful) {
                logoutAll() // Hapus semua token lokal
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
                    // Kita hanya update tokennya saja pada user yang sedang login
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
                    // Update data user lokal dengan data baru dari remote
                    // TAPI JANGAN HAPUS TOKEN YANG ADA DI LOKAL
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