package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    // Get current logged in user
    @Query("SELECT * FROM users WHERE access_token IS NOT NULL LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Query("SELECT * FROM users WHERE access_token IS NOT NULL LIMIT 1")
    fun getCurrentUserFlow(): Flow<User?>

    // Get user by ID
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    // Get all users
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    // Insert or update user
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    // Update user
    @Update
    suspend fun updateUser(user: User)

    // Update token
    @Query("UPDATE users SET access_token = :token, token_expires_at = :expiresAt WHERE id = :userId")
    suspend fun updateToken(userId: String, token: String, expiresAt: Long)

    // Clear token (logout)
    @Query("UPDATE users SET access_token = NULL, token_expires_at = NULL WHERE id = :userId")
    suspend fun clearToken(userId: String)

    @Query("UPDATE users SET access_token = NULL, token_expires_at = NULL")
    suspend fun clearAllTokens()

    // Delete user
    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    // Delete all users
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    // Check if user is logged in
    @Query("SELECT COUNT(*) FROM users WHERE access_token IS NOT NULL")
    suspend fun isLoggedIn(): Int
}