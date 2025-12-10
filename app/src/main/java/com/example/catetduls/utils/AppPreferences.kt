package com.example.catetduls.utils

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREF_NAME = "app_settings"
    private const val KEY_FIRST_RUN = "is_first_run"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Cek apakah ini pertama kali buka aplikasi
    fun isFirstRun(context: Context): Boolean {
        // Default true (artinya ya, ini pertama kali)
        return getPrefs(context).getBoolean(KEY_FIRST_RUN, true)
    }

    // Tandai bahwa user sudah melewati onboarding (Login/Skip)
    fun setFirstRunDone(context: Context) {
        val editor = getPrefs(context).edit()
        editor.putBoolean(KEY_FIRST_RUN, false)
        editor.apply()
    }
}