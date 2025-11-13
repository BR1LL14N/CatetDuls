package com.example.catetduls.ui

import androidx.fragment.app.Fragment
import com.example.catetduls.R
import com.example.catetduls.ui.pages.DashboardPage
import com.example.catetduls.ui.pages.PengaturanPage
import com.example.catetduls.ui.pages.StatistikPage
import com.example.catetduls.ui.pages.TransaksiPage

/**
 * Sealed class yang mendefinisikan item-item navigasi utama
 * yang akan digunakan di Bottom Navigation Bar.
 *
 * Setiap object merepresentasikan satu tab di bagian bawah layar.
 */
sealed class BottomNavItem(
    /**
     * Resource ID untuk judul (dari res/values/strings.xml)
     */
    val titleResId: Int,

    /**
     * Resource ID untuk ikon (dari res/drawable)
     */
    val iconResId: Int,

    /**
     * Tag unik yang digunakan oleh FragmentManager untuk
     * mengidentifikasi setiap fragment.
     */
    val tag: String
) {
    object Dashboard : BottomNavItem(
        titleResId = R.string.nav_dashboard, // Anda perlu membuat string ini
        iconResId = R.drawable.ic_nav_dashboard, // Anda perlu membuat drawable ini
        tag = "DASHBOARD_PAGE"
    )

    object Transaksi : BottomNavItem(
        titleResId = R.string.nav_transaksi,
        iconResId = R.drawable.ic_nav_transaksi,
        tag = "TRANSAKSI_PAGE"
    )

    object Statistik : BottomNavItem(
        titleResId = R.string.nav_statistik,
        iconResId = R.drawable.ic_nav_statistik,
        tag = "STATISTIK_PAGE"
    )

    object Pengaturan : BottomNavItem(
        titleResId = R.string.nav_pengaturan,
        iconResId = R.drawable.ic_nav_pengaturan,
        tag = "PENGATURAN_PAGE"
    )
}

/**
 * Helper list yang berisi semua item navigasi utama.
 * Ini bisa digunakan oleh MainActivity untuk membuat menu
 * di BottomNavigationView secara programatik.
 */
val mainNavigationItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Transaksi,
    BottomNavItem.Statistik,
    BottomNavItem.Pengaturan
)

/**
 * Helper function untuk mendapatkan instance Fragment yang sesuai
 * berdasarkan tag navigasi yang diberikan.
 *
 * Ini adalah pola umum yang digunakan di MainActivity untuk
 * menangani perpindahan antar fragment.
 */
fun getFragmentForTag(tag: String): Fragment {
    return when (tag) {
        BottomNavItem.Dashboard.tag -> DashboardPage()
        BottomNavItem.Transaksi.tag -> TransaksiPage()
        BottomNavItem.Statistik.tag -> StatistikPage()
        BottomNavItem.Pengaturan.tag -> PengaturanPage()
        else -> DashboardPage() // Default ke Dashboard jika tag tidak dikenal
    }
}