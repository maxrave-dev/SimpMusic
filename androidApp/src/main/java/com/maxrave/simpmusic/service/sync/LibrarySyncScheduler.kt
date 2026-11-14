package com.maxrave.simpmusic.service.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.maxrave.domain.repository.LibrarySyncPhoneRepository
import com.maxrave.logger.Logger
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * Runs a library sync round in the background while this phone is paired with a computer (#2663).
 * Fifteen minutes is the shortest interval WorkManager allows; opening the app and "Sync now" fill in
 * between.
 */
class LibrarySyncScheduler(
    private val context: Context,
    private val repository: LibrarySyncPhoneRepository,
) {
    /** Call from a scope that lives as long as the application. */
    suspend fun observeAndSchedule() {
        var wasPaired = false
        repository.state
            .map { it.isPaired }
            .distinctUntilChanged()
            .collect { paired ->
                val workManager = WorkManager.getInstance(context)
                if (paired) {
                    val request =
                        PeriodicWorkRequestBuilder<LibrarySyncWorker>(15, TimeUnit.MINUTES)
                            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                            .build()
                    // KEEP: a launch must not restart the period, or cancel the job that woke the process.
                    workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
                    Logger.i(TAG, "background sync scheduled")
                } else if (wasPaired) {
                    // Only a real unpairing cancels it. The first value is "not paired" until the
                    // pairing file has been read, and acting on it would cancel a job that is running.
                    workManager.cancelUniqueWork(WORK_NAME)
                    Logger.i(TAG, "background sync cancelled")
                }
                wasPaired = paired
            }
    }

    private companion object {
        const val TAG = "LibrarySync"
        const val WORK_NAME = "LibrarySyncWorker"
    }
}
