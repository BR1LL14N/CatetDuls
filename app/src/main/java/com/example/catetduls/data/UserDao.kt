package com.example.catetduls.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE access_token IS NOT NULL LIMIT 1")
    suspend fun getCurrentUser(): User?

    @Query("SELECT * FROM users WHERE access_token IS NOT NULL LIMIT 1")
    fun getCurrentUserFlow(): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId") suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users ORDER BY name ASC") fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertUsers(users: List<User>)

    @Update suspend fun updateUser(user: User)

    @Query(
            "UPDATE users SET access_token = :token, token_expires_at = :expiresAt WHERE id = :userId"
    )
    suspend fun updateToken(userId: String, token: String, expiresAt: Long)

    @Query("UPDATE users SET access_token = NULL, token_expires_at = NULL WHERE id = :userId")
    suspend fun clearToken(userId: String)

    @Query("UPDATE users SET access_token = NULL, token_expires_at = NULL")
    suspend fun clearAllTokens()

    @Delete suspend fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId") suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM users") suspend fun deleteAllUsers()

    @Query("SELECT COUNT(*) FROM users WHERE access_token IS NOT NULL")
    suspend fun isLoggedIn(): Int
}
