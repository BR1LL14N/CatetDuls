//package com.example.catetduls.utils
//
//import android.content.Context
//import com.example.catetduls.data.local.TokenManager
//
//enum class AppMode {
//    GUEST_OFFLINE,      // Guest, tidak perlu sync
//    LOGGED_IN_OFFLINE,  // Login tapi offline, data pending sync
//    LOGGED_IN_ONLINE    // Login dan online, full sync
//}
//
//object AppModeManager {
//
//    fun getCurrentMode(context: Context, isOnline: Boolean): AppMode {
//        val isLoggedIn = TokenManager.isLoggedIn(context)
//
//        return when {
//            !isLoggedIn -> AppMode.GUEST_OFFLINE
//            isLoggedIn && !isOnline -> AppMode.LOGGED_IN_OFFLINE
//            isLoggedIn && isOnline -> AppMode.LOGGED_IN_ONLINE
//            else -> AppMode.GUEST_OFFLINE
//        }
//    }
//}