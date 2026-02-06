package com.maxrave.simpmusic.service.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.TimeUnit

class AutoBackupScheduler(
    private val context: Context,
    private val dataStoreManager: DataStoreManager,
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Observe DataStore preferences and schedule/cancel WorkManager accordingly.
     * This should be called in a coroutine scope that lives as long as the application.
     */
    suspend fun observeAndSchedule() {
        combine(
            dataStoreManager.autoBackupEnabled,
            dataStoreManager.autoBackupFrequency,
        ) { enabled, frequency ->
            Pair(enabled, frequency)
        }.distinctUntilChanged().collect { (enabled, frequency) ->
            Logger.i(TAG, "Auto backup settings changed: enabled=$enabled, frequency=$frequency")

            if (enabled == DataStoreManager.TRUE) {
                scheduleBackup(frequency)
            } else {
                cancelBackup()
            }
        }
    }

    private fun scheduleBackup(frequency: String) {
        val (intervalValue, intervalUnit) = when (frequency) {
            DataStoreManager.AUTO_BACKUP_FREQUENCY_DAILY -> 24L to TimeUnit.HOURS
            DataStoreManager.AUTO_BACKUP_FREQUENCY_WEEKLY -> 7L to TimeUnit.DAYS
            DataStoreManager.AUTO_BACKUP_FREQUENCY_MONTHLY -> 30L to TimeUnit.DAYS
            else -> 24L to TimeUnit.HOURS
        }

        Logger.i(TAG, "Scheduling auto backup: interval=$intervalValue $intervalUnit")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            intervalValue,
            intervalUnit
        )
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        Logger.i(TAG, "Auto backup scheduled successfully")
    }

    private fun cancelBackup() {
        Logger.i(TAG, "Cancelling auto backup")
        workManager.cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val TAG = "AutoBackupScheduler"
        const val WORK_NAME = "AutoBackupWorker"
        const val WORK_TAG = "auto_backup"
    }
}
