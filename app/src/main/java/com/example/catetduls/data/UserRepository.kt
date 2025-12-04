package com.example.catetduls.data

import com.example.catetduls.data.*
import com.example.catetduls.data.UserDao
import com.example.catetduls.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.Response

class UserRepository(
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
            userDao.clearToken(it.id)
        }
    }

    suspend fun logoutAll() {
        userDao.clearAllTokens()
    }

    suspend fun deleteAccount() {
        val user = getCurrentUser()
        user?.let {
            userDao.deleteUserById(it.id)
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

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                val user = authResponse.user.copy(
                    access_token = authResponse.access_token,
                    token_expires_at = System.currentTimeMillis() + (authResponse.expires_in * 1000)
                )

                // Clear previous users and save new user
                userDao.deleteAllUsers()
                userDao.insertUser(user)

                Result.success(user)
            } else {
                Result.failure(Exception(response.message() ?: "Registration failed"))
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
                val authResponse = response.body()!!
                val user = authResponse.user.copy(
                    access_token = authResponse.access_token,
                    token_expires_at = System.currentTimeMillis() + (authResponse.expires_in * 1000)
                )

                // Clear previous users and save new user
                userDao.deleteAllUsers()
                userDao.insertUser(user)

                Result.success(user)
            } else {
                Result.failure(Exception(response.message() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logoutRemote(): Result<String> {
        return try {
            val response = apiService.logout()

            if (response.isSuccessful) {
                logout() // Clear local token
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
                logoutAll() // Clear all local tokens
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
                    val updatedUser = authResponse.user.copy(
                        access_token = authResponse.access_token,
                        token_expires_at = System.currentTimeMillis() + (authResponse.expires_in * 1000)
                    )
                    userDao.updateUser(updatedUser)
                    Result.success(updatedUser)
                } else {
                    Result.failure(Exception("No user found"))
                }
            } else {
                Result.failure(Exception(response.message() ?: "Token refresh failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
            val response = apiService.getUserProfile()

            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                val currentUser = getCurrentUser()

                if (currentUser != null) {
                    val updatedUser = user.copy(
                        access_token = currentUser.access_token,
                        token_expires_at = currentUser.token_expires_at
                    )
                    userDao.updateUser(updatedUser)
                    Result.success(updatedUser)
                } else {
                    Result.success(user)
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

            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                val currentUser = getCurrentUser()

                if (currentUser != null) {
                    val updatedUser = user.copy(
                        access_token = currentUser.access_token,
                        token_expires_at = currentUser.token_expires_at
                    )
                    userDao.updateUser(updatedUser)
                    Result.success(updatedUser)
                } else {
                    Result.success(user)
                }
            } else {
                Result.failure(Exception(response.message() ?: "Update failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPhoto(photoBase64: String): Result<User> {
        return try {
            val request = UploadPhotoRequest(photoBase64)
            val response = apiService.uploadUserPhoto(request)

            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                val currentUser = getCurrentUser()

                if (currentUser != null) {
                    val updatedUser = user.copy(
                        access_token = currentUser.access_token,
                        token_expires_at = currentUser.token_expires_at
                    )
                    userDao.updateUser(updatedUser)
                    Result.success(updatedUser)
                } else {
                    Result.success(user)
                }
            } else {
                Result.failure(Exception(response.message() ?: "Upload failed"))
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
                deleteAccount() // Clear local data
                Result.success(response.body()?.message ?: "Account deleted")
            } else {
                Result.failure(Exception(response.message() ?: "Delete failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}