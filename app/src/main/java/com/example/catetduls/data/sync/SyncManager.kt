package com.example.catetduls.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncManager {

    private const val SYNC_WORK_NAME = "DataSyncWork"

    /**
     * Menjadwalkan sinkronisasi secara periodik dan unik
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            // Hanya jalan jika ada koneksi internet
            .setRequiredNetworkType(NetworkType.CONNECTED)
            // !!! PERBAIKAN: setRequiresDeviceIdle(true) DIHAPUS UNTUK DEBUGGING !!!
            // Jika ingin diaktifkan kembali saat production, uncomment baris di bawah:
            // .setRequiresDeviceIdle(true)
            .build()

        // Jadwalkan setiap 6 jam
        val syncRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Memaksa sinkronisasi segera (mengklik tombol Sync)
     */
    fun forceOneTimeSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.SECONDS) // Beri sedikit jeda
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "ForceOneTimeSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )

        Log.d("SyncManager", "Forcing one-time sync...")
    }
}