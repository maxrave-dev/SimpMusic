package com.maxrave.simpmusic.expect

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.maxrave.simpmusic.service.test.notification.NotifyWork
import org.koin.mp.KoinPlatform.getKoin
import java.util.concurrent.TimeUnit

actual fun startWorker() {
    val context: Context = getKoin().get()
    val request =
        PeriodicWorkRequestBuilder<NotifyWork>(
            12L,
            TimeUnit.HOURS,
        ).addTag("Worker Test")
            .setConstraints(
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "Artist Worker",
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}