package com.example.catetduls.data.local

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Simpan Token (Dipanggil saat Login sukses)
    fun saveToken(context: Context, token: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_TOKEN, token)
        editor.apply()
    }

    // Ambil Token (Dipanggil oleh NetworkModule)
    fun getToken(context: Context): String? {
        return getPreferences(context).getString(KEY_TOKEN, null)
    }

    // Hapus Token (Dipanggil saat Logout)
    fun clearToken(context: Context) {
        val editor = getPreferences(context).edit()
        editor.remove(KEY_TOKEN)
        editor.apply()
    }
}