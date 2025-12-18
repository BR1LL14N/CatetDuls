package com.example.catetduls.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** TokenManager - Simpan access_token & refresh_token dengan aman */
object TokenManager {

    private const val PREF_NAME = "secure_auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_TOKEN_EXPIRY = "token_expiry"
    private const val KEY_REFRESH_EXPIRY = "refresh_expiry"

    /** Get EncryptedSharedPreferences (lebih aman dari SharedPreferences biasa) */
    private fun getSecurePreferences(context: Context): SharedPreferences {
        return try {
            val masterKey =
                    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

            EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback ke SharedPreferences biasa jika EncryptedSharedPreferences gagal
            e.printStackTrace()
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Simpan access_token dan refresh_token
     *
     * @param accessToken Token untuk API request
     * @param refreshToken Token untuk refresh access_token
     * @param expiresIn Durasi access token dalam detik (misal: 3600 = 1 jam)
     */
    fun saveTokens(
            context: Context,
            accessToken: String,
            refreshToken: String,
            expiresIn: Long = 3600 // default 1 jam
    ) {
        val prefs = getSecurePreferences(context)
        val currentTime = System.currentTimeMillis()

        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_TOKEN_EXPIRY, currentTime + (expiresIn * 1000)) // Convert detik ke milis
            putLong(KEY_REFRESH_EXPIRY, currentTime + (30L * 24 * 60 * 60 * 1000)) // 30 hari
            apply()
        }
    }

    /** Ambil access token */
    fun getAccessToken(context: Context): String? {
        return getSecurePreferences(context).getString(KEY_ACCESS_TOKEN, null)
    }

    /** Ambil refresh token */
    fun getRefreshToken(context: Context): String? {
        return getSecurePreferences(context).getString(KEY_REFRESH_TOKEN, null)
    }

    /** Cek apakah access token sudah expired */
    fun isAccessTokenExpired(context: Context): Boolean {
        val expiryTime = getSecurePreferences(context).getLong(KEY_TOKEN_EXPIRY, 0L)
        if (expiryTime == 0L) return true

        // Buffer 5 menit sebelum expiry untuk hindari race condition
        val bufferTime = 5 * 60 * 1000
        return System.currentTimeMillis() > (expiryTime - bufferTime)
    }

    /** Cek apakah refresh token masih valid */
    fun isRefreshTokenValid(context: Context): Boolean {
        val expiryTime = getSecurePreferences(context).getLong(KEY_REFRESH_EXPIRY, 0L)
        return expiryTime > 0L && System.currentTimeMillis() < expiryTime
    }

    /** Update hanya access token (setelah refresh) */
    fun updateAccessToken(context: Context, newAccessToken: String, expiresIn: Long) {
        val prefs = getSecurePreferences(context)
        val currentTime = System.currentTimeMillis()

        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, newAccessToken)
            putLong(KEY_TOKEN_EXPIRY, currentTime + (expiresIn * 1000))
            apply()
        }
    }

    /** Hapus semua token (untuk logout) */
    fun clearTokens(context: Context) {
        getSecurePreferences(context).edit().clear().apply()
    }

    /**
     * Cek apakah user sudah login
     *
     * Note: Hanya cek keberadaan access token, tidak cek validitas refresh token. Validasi refresh
     * token hanya dilakukan saat operasi refresh token, bukan saat cek login status. Ini mencegah
     * race condition dimana sync worker jalan sebelum refresh token expiry ditulis.
     */
    fun isLoggedIn(context: Context): Boolean {
        return getAccessToken(context) != null
    }

    // ===== BACKWARD COMPATIBILITY (untuk kode lama) =====

    /** @deprecated Gunakan saveTokens() untuk simpan access + refresh token */
    @Deprecated(
            "Gunakan saveTokens()",
            ReplaceWith("saveTokens(context, token, refreshToken, expiresIn)")
    )
    fun saveToken(context: Context, token: String?) { // ⭐ Ganti String menjadi String?
        if (token != null) { // ⭐ Tambahkan null check
            getSecurePreferences(context).edit().apply {
                putString(KEY_ACCESS_TOKEN, token)
                apply()
            }
        }
    }

    /** @deprecated Gunakan getAccessToken() */
    @Deprecated("Gunakan getAccessToken()", ReplaceWith("getAccessToken(context)"))
    fun getToken(context: Context): String? {
        return getAccessToken(context)
    }

    /** @deprecated Gunakan clearTokens() */
    @Deprecated("Gunakan clearTokens()", ReplaceWith("clearTokens(context)"))
    fun clearToken(context: Context) {
        clearTokens(context)
    }
}
