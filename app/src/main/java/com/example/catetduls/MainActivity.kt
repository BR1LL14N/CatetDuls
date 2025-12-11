package com.example.catetduls

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.catetduls.ui.pages.*
import com.example.catetduls.utils.AppPreferences
import com.example.catetduls.data.local.TokenManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.FragmentManager

// Antarmuka yang didefinisikan di RegisterPage, harus diimplementasikan di Activity
interface NavigationController {
    fun setNavBarVisibility(visibility: Int)
}

@AndroidEntryPoint
// ⭐ IMPLEMENTASI NAVIGATIONCONTROLLER DITAMBAHKAN
class MainActivity : AppCompatActivity(), NavigationCallback, NavigationController {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_CatetDuls)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        // =========================================================================
        // Logika Lifecycle Callback Sederhana
        // Kita hanya perlu memastikan Navbar muncul secara default, dan biarkan
        // Fragment seperti LoginPage/RegisterPage yang menyembunyikannya.
        // Jika ada Fragment yang di-replace, kita atur visibilitas default.
        // =========================================================================
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                super.onFragmentViewCreated(fm, f, v, savedInstanceState)

                // Jika Fragment yang ditampilkan adalah LoginPage atau RegisterPage, sembunyikan Navbar.
                if (f is LoginPage || f is RegisterPage) {
                    bottomNav.visibility = View.GONE
                }
                // Jika Fragment lain (TransaksiPage, dll.) sedang dimuat, tampilkan.
                else {
                    bottomNav.visibility = View.VISIBLE
                }
            }
        }, true)


        if (savedInstanceState == null) {
            // Cek Status User
            val isFirstRun = AppPreferences.isFirstRun(this)
            val isLoggedIn = TokenManager.getToken(this) != null

            if (isFirstRun && !isLoggedIn) {
                // User Baru -> Login Page (Navbar akan disembunyikan oleh callback)
                loadFragment(LoginPage())
            } else {
                // User Lama -> Halaman Utama (Navbar akan ditampilkan oleh callback)
                loadFragment(TransaksiPage())
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_transaksi -> {
                    loadFragment(TransaksiPage())
                    true
                }
                R.id.nav_kalender -> {
                    loadFragment(CalendarPage())
                    true
                }
                R.id.nav_tambah -> {
                    loadFragment(TambahTransaksiPage())
                    true
                }
                R.id.nav_statistik -> {
                    loadFragment(StatistikPage())
                    true
                }
                R.id.nav_pengaturan -> {
                    loadFragment(PengaturanPage())
                    true
                }
                else -> false
            }
        }
    }

    // ⭐ IMPLEMENTASI METODE UNTUK MENGONTROL NAVIGASI BAR
    override fun setNavBarVisibility(visibility: Int) {
        if (::bottomNav.isInitialized) {
            bottomNav.visibility = visibility
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun navigateTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}