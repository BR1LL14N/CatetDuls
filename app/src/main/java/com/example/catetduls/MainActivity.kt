package com.example.catetduls

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.catetduls.data.local.TokenManager
import com.example.catetduls.ui.pages.*
import com.example.catetduls.utils.AppPreferences
import com.example.catetduls.utils.NetworkUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope // ✅ Solusi error lifecycleScope
import com.example.catetduls.data.Book // ✅ Solusi error infer type
import com.example.catetduls.data.getBookRepository
import android.content.Context

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
        setupNetworkObserver()
        setupSyncObserver()

        // =========================================================================
        // --- TAMBAHAN FIX AUTO-UPDATE ID (MULAI) ---
        // Logika ini memastikan jika ID Buku di database berubah (misal dari server),
        // SharedPreferences langsung diupdate agar tidak "nyangkut" di ID 1.
        // =========================================================================
        val bookRepo = this.getBookRepository()
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        lifecycleScope.launch {
            // 2. Tambahkan tipe eksplisit 'book: Book?' agar Kotlin tidak bingung
            bookRepo.getActiveBook().collect { book: Book? ->
                if (book != null) {
                    val currentPrefId = prefs.getInt("active_book_id", 1)

                    // Jika ID di Prefs (1) beda dengan ID asli dari Database (misal 2)
                    if (currentPrefId != book.id) {
                        prefs.edit().putInt("active_book_id", book.id).apply()
                        android.util.Log.d("MainActivity", "✅ FIX: Active Book ID updated to ${book.id}")

                        // Opsional: Reload fragment jika sedang di halaman transaksi
                        // loadFragment(TransaksiPage())
                    }
                }
            }
        }
        // --- TAMBAHAN FIX AUTO-UPDATE ID (SELESAI) ---


        // =========================================================================
        // Logika Lifecycle Callback Sederhana
        // =========================================================================
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    f: Fragment,
                    v: View,
                    savedInstanceState: Bundle?
                ) {
                    super.onFragmentViewCreated(fm, f, v, savedInstanceState)

                    if (f is LoginPage || f is RegisterPage) {
                        bottomNav.visibility = View.GONE
                    } else {
                        bottomNav.visibility = View.VISIBLE
                    }
                }
            },
            true
        )

        if (savedInstanceState == null) {
            val isFirstRun = AppPreferences.isFirstRun(this)
            val isLoggedIn = TokenManager.isLoggedIn(this)

            if (isFirstRun && !isLoggedIn) {
                loadFragment(LoginPage())
            } else {
                loadFragment(TransaksiPage())

                if (isLoggedIn) {
                    com.example.catetduls.data.sync.SyncManager.forceOneTimeSync(this)
                    com.example.catetduls.data.sync.SyncManager.schedulePeriodicSync(this)
                }
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

    private fun setupNetworkObserver() {
        lifecycleScope.launch {
            NetworkUtils.observeConnectivity(this@MainActivity).collect { isConnected ->
                val message =
                        if (isConnected) "🟢 Online - Terhubung ke Internet"
                        else "🔴 Offline - Koneksi Terputus"
                val duration = if (isConnected) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
                Snackbar.make(findViewById(android.R.id.content), message, duration).show()

                // OTOMATIS SYNC SAAT KONEKSI KEMBALI
                if (isConnected && TokenManager.isLoggedIn(this@MainActivity)) {
                    com.example.catetduls.data.sync.SyncManager.forceOneTimeSync(this@MainActivity)
                }
            }
        }
    }

    private fun setupSyncObserver() {
        // Observe Periodic Sync
        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData("DataSyncWork").observe(
                        this
                ) { workInfos -> handleWorkInfo(workInfos) }

        // Observe One-Time Force Sync
        WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData("ForceOneTimeSync").observe(
                        this
                ) { workInfos -> handleWorkInfo(workInfos) }
    }

    private fun handleWorkInfo(workInfos: List<WorkInfo>) {
        if (workInfos.isNullOrEmpty()) return

        val workInfo = workInfos.first()
        val rootView = findViewById<View>(android.R.id.content)

        when (workInfo.state) {
            WorkInfo.State.RUNNING -> {
                // Tampilkan pesan loading saat sync sedang berjalan
                Snackbar.make(rootView, "🔄 Sedang menyinkronkan data...", Snackbar.LENGTH_SHORT)
                        .show()
            }
            WorkInfo.State.SUCCEEDED -> {
                Snackbar.make(
                                rootView,
                                "✅ Sinkronisasi berhasil! Data Anda sudah diperbarui.",
                                Snackbar.LENGTH_SHORT
                        )
                        .show()
            }
            WorkInfo.State.FAILED -> {
                Snackbar.make(
                                rootView,
                                "❌ Sinkronisasi gagal. Pastikan Anda sudah login dan koneksi internet stabil.",
                                Snackbar.LENGTH_LONG
                        )
                        .show()
            }
            else -> {
                // State lain seperti ENQUEUED, BLOCKED, CANCELLED - tidak perlu notifikasi
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
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
    }

    override fun navigateTo(fragment: Fragment) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
    }
}
