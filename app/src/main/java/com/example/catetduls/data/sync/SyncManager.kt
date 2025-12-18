package com.example.catetduls.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncManager {

    private const val SYNC_WORK_NAME = "DataSyncWork"

    /**
     * Menjadwalkan sinkronisasi secara periodik dan unik
     *
     * ALUR SYNC:
     * 1. Setelah login -> forceOneTimeSync() dipanggil
     * 2. Setelah one-time sync -> schedulePeriodicSync() dijadwalkan
     * 3. Sync otomatis setiap 2 menit selama user login
     * 4. WorkManager akan tetap jalan di background meskipun app ditutup
     * 5. Saat app dibuka ulang, periodic work tetap aktif (tidak perlu reschedule)
     *
     * Note: Gunakan KEEP policy agar tidak mengganggu schedule yang sudah ada
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints =
                Constraints.Builder()
                        // Network check dilakukan manual di DataSyncWorker
                        // Gunakan NOT_REQUIRED untuk hindari masalah "network not validated" di
                        // emulator
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()

        // Jadwalkan setiap 2 menit
        val syncRequest =
                PeriodicWorkRequestBuilder<DataSyncWorker>(
                                repeatInterval = 2,
                                repeatIntervalTimeUnit = TimeUnit.MINUTES
                        )
                        .setConstraints(constraints)
                        .build()

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        SYNC_WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncRequest
                )

        Log.d("SyncManager", "Periodic sync scheduled (every 2 minutes)")
    }

    /**
     * Memaksa sinkronisasi segera (setelah login atau klik tombol Sync)
     *
     * Dipanggil saat:
     * - User baru login
     * - User klik tombol sync manual
     * - App perlu force sync setelah operasi penting
     */
    fun forceOneTimeSync(context: Context) {
        val constraints =
                Constraints.Builder()
                        // Network check sudah dilakukan di dalam DataSyncWorker
                        // Ini menghindari masalah "network not validated" di emulator
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()

        val syncRequest =
                OneTimeWorkRequestBuilder<DataSyncWorker>()
                        .setConstraints(constraints)
                        // Tidak ada delay, langsung jalan (worker sendiri yang handle checks)
                        .build()

        WorkManager.getInstance(context)
                .enqueueUniqueWork("ForceOneTimeSync", ExistingWorkPolicy.REPLACE, syncRequest)

        Log.d("SyncManager", "🔄 Force one-time sync enqueued (will start immediately)")
    }
}
