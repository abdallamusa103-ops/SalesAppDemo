package com.abdallamusa.salesapp.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun triggerImmediateSync() {
        val syncRequest = OneTimeWorkRequestBuilder<OdooSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "OdooImmediateSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    fun schedulePeriodicSync() {
        val periodicRequest = PeriodicWorkRequestBuilder<OdooSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            OdooSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }
}
