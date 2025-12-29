package com.example.catetduls.data

import android.content.Context
import com.example.catetduls.data.local.TokenManager
import com.example.catetduls.data.remote.*
import com.example.catetduls.utils.ErrorUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class UserRepository
@Inject
constructor(
        private val userDao: UserDao,
        private val apiService: ApiService,
        @ApplicationContext private val context: Context
) {

    // ===================================

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

        user?.let { userDao.clearToken(it.id) }
    }

    suspend fun logoutAll() {
        userDao.clearAllTokens()
    }

    suspend fun deleteAccount() {
        val user = getLocalUser()

        user?.let { userDao.deleteUserById(it.id) }
    }

    // ===================================

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

            if (response.isSuccessful && response.body() != null) {

                val apiResponse = response.body()!!

                if (apiResponse.success && apiResponse.data != null) {

                    val authData = apiResponse.data
                    val remoteUser = authData.user

                    val localUser =
                            User(
                                    id = remoteUser.id.toString(),
                                    name = remoteUser.name,
                                    email = remoteUser.email,
                                    photo_url = remoteUser.photo_url,
                                    email_verified_at = remoteUser.email_verified_at,
                                    created_at = remoteUser.created_at ?: "",
                                    updated_at = remoteUser.updated_at ?: "",
                                    access_token = authData.token,
                                    token_expires_at =
                                            System.currentTimeMillis() +
                                                    ((authData.expires_in ?: 3600) * 1000),
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
                                expiresIn = authData.expires_in ?: 3600L
                        )
                    } else {

                        TokenManager.saveToken(context, authData.token)
                    }

                    Result.success(localUser)
                } else {
                    Result.failure(Exception(apiResponse.message ?: "Registrasi gagal"))
                }
            } else {
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
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
                    val remoteUser = authData.user

                    val localUser =
                            User(
                                    id = remoteUser.id.toString(),
                                    name = remoteUser.name,
                                    email = remoteUser.email,
                                    photo_url = remoteUser.photo_url,
                                    email_verified_at = remoteUser.email_verified_at,
                                    created_at = remoteUser.created_at ?: "",
                                    updated_at = remoteUser.updated_at ?: "",
                                    access_token = authData.token,
                                    token_expires_at =
                                            System.currentTimeMillis() +
                                                    ((authData.expires_in ?: 3600) * 1000), // Default 1 hour
                                    last_synced_at = System.currentTimeMillis(),
                                    is_synced = true
                            )

                    userDao.deleteAllUsers()
                    userDao.insertUser(localUser)

                    // Always use saveTokens() with proper expiry
                    TokenManager.saveTokens(
                            context = context,
                            accessToken = authData.token,
                            refreshToken = authData.refresh_token ?: authData.token,
                            expiresIn = authData.expires_in ?: 3600L // Default 1 hour
                    )

                    Result.success(localUser)
                } else {
                    Result.failure(Exception(apiResponse.message ?: "Login gagal"))
                }
            } else {
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            try {
                val token = TokenManager.getAccessToken(context)
                if (token != null) {
                    apiService.logout("Bearer $token")
                }
            } catch (e: Exception) {}

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
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
        }
    }

    suspend fun refreshToken(): Result<User> {
        return try {
            val response = apiService.refreshToken()

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val currentUser = getCurrentUser()

                if (currentUser != null) {

                    val updatedUser =
                            currentUser.copy(
                                    access_token = authResponse.access_token,
                                    token_expires_at =
                                            System.currentTimeMillis() +
                                                    (authResponse.expires_in * 1000)
                            )
                    userDao.updateUser(updatedUser)
                    Result.success(updatedUser)
                } else {
                    Result.failure(Exception("No user found locally"))
                }
            } else {
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
        }
    }

    // ===================================

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
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
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
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
        }
    }

    suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
            newPasswordConfirmation: String
    ): Result<String> {
        return try {
            val request =
                    ChangePasswordRequest(currentPassword, newPassword, newPasswordConfirmation)
            val response = apiService.changePassword(request)

            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Password changed successfully")
            } else {
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
        }
    }

    // ===================================

    // ===================================

    suspend fun fetchUserProfile(): Result<User> {
        return try {
            val response = apiService.getUserProfile()

            if (response.isSuccessful && response.body() != null) {
                val remoteUser = response.body()!!
                val currentUser = getCurrentUser()

                if (currentUser != null) {

                    val updatedUser =
                            currentUser.copy(
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
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
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

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!

                if (apiResponse.success && apiResponse.data != null) {
                    val remoteUser = apiResponse.data
                    val currentUser = getCurrentUser()

                    if (currentUser != null) {
                        // Update data user lokal dengan data baru dari remote
                        val updatedUser =
                                currentUser.copy(
                                        name = remoteUser.name,
                                        email = remoteUser.email,
                                        photo_url = remoteUser.photo_url,
                                        updated_at = remoteUser.updated_at ?: currentUser.updated_at
                                )

                        userDao.updateUser(updatedUser)

                        Result.success(updatedUser)
                    } else {
                        Result.failure(Exception("No local user found to update"))
                    }
                } else {
                    Result.failure(Exception(apiResponse.message ?: "Update failed"))
                }
            } else {
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
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
                val apiResponse = response.body()!!

                // Cek flag success dari backend
                if (apiResponse.success && apiResponse.data != null) {
                    val photoData = apiResponse.data
                    val currentUser = getCurrentUser()

                    if (currentUser != null) {
                        val updatedUser =
                                currentUser.copy(
                                        photo_url = photoData.photo_url,
                                        updated_at = System.currentTimeMillis().toString()
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
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
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
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
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
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorUtils.getReadableMessage(e)))
        }
    }

    suspend fun deleteAccountRemote(): Result<String> {
        return try {
            val response = apiService.deleteAccount()

            if (response.isSuccessful) {
                deleteAccount() // Hapus data lokal
                Result.success(response.body()?.message ?: "Account deleted")
            } else {
                val errorMsg = ErrorUtils.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
