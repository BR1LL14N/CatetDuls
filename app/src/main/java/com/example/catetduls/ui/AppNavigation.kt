package com.example.catetduls.ui

import androidx.fragment.app.Fragment
import com.example.catetduls.R
import com.example.catetduls.ui.pages.DashboardPage
import com.example.catetduls.ui.pages.PengaturanPage
import com.example.catetduls.ui.pages.StatistikPage
import com.example.catetduls.ui.pages.TransaksiPage


sealed class BottomNavItem(

    val titleResId: Int,


    val iconResId: Int,

    val tag: String
) {
    object Dashboard : BottomNavItem(
        titleResId = R.string.nav_dashboard,
        iconResId = R.drawable.ic_nav_dashboard,
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

val mainNavigationItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Transaksi,
    BottomNavItem.Statistik,
    BottomNavItem.Pengaturan
)

fun getFragmentForTag(tag: String): Fragment {
    return when (tag) {
        BottomNavItem.Dashboard.tag -> DashboardPage()
        BottomNavItem.Transaksi.tag -> TransaksiPage()
        BottomNavItem.Statistik.tag -> StatistikPage()
        BottomNavItem.Pengaturan.tag -> PengaturanPage()
        else -> DashboardPage()
    }
}