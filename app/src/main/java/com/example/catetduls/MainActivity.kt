package com.example.catetduls

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.catetduls.ui.pages.*
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * MainActivity - Activity utama dengan Bottom Navigation
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        // Set default fragment (Dashboard)
        if (savedInstanceState == null) {
            loadFragment(DashboardPage())
        }

        // Setup bottom navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardPage())
                    true
                }
                R.id.nav_transaksi -> {
                    loadFragment(TransaksiPage())
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

    /**
     * Load fragment ke container
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}