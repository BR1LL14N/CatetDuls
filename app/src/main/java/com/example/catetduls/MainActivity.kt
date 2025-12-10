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



@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationCallback {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_CatetDuls)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        // =========================================================================
        // PERBAIKAN LOGIKA NAVBAR:
        // Gunakan 'registerFragmentLifecycleCallbacks' agar selalu terdeteksi
        // =========================================================================
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                super.onFragmentViewCreated(fm, f, v, savedInstanceState)

                // Logika:
                // Jika yang tampil adalah LoginPage -> Sembunyikan Menu
                // Jika yang tampil BUKAN LoginPage -> Tampilkan Menu
                if (f is LoginPage) {
                    bottomNav.visibility = View.GONE
                } else {
                    bottomNav.visibility = View.VISIBLE
                }
            }
        }, true)

        if (savedInstanceState == null) {
            // Cek Status User
            val isFirstRun = AppPreferences.isFirstRun(this)
            val isLoggedIn = TokenManager.getToken(this) != null

            if (isFirstRun && !isLoggedIn) {
                // User Baru -> Login Page
                loadFragment(LoginPage())
            } else {
                // User Lama -> Halaman Utama
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