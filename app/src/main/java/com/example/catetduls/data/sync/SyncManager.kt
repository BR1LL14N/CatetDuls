package com.example.catetduls.data.sync

import android.content.Context
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
            // Hanya jalan jika perangkat sedang idle (tidak aktif)
            .setRequiresDeviceIdle(true)
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
            // Jika ada pekerjaan yang sudah dijadwalkan dengan nama yang sama, KEEP (pertahankan yang lama)
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Memaksa sinkronisasi segera (misalnya saat pengguna mengklik tombol Sync)
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
            // Gunakan REPLACE agar bisa langsung menggantikan pekerjaan sebelumnya
            "ForceOneTimeSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}