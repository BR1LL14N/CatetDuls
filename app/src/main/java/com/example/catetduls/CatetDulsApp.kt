package com.example.catetduls

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.catetduls.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp // 1. Wajib ada
class CatetDulsApp : Application(), Configuration.Provider {

    // 2. Inject Factory khusus dari Hilt untuk WorkManager
    @Inject lateinit var workerFactory: HiltWorkerFactory

    // 3. Konfigurasi WorkManager agar menggunakan Factory dari Hilt
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Hapus kode manual DI (networkModule, database, repository, apiService)
        // karena Hilt sudah menanganinya.

        // Jadwalkan Sinkronisasi
        SyncManager.schedulePeriodicSync(this)
    }
}