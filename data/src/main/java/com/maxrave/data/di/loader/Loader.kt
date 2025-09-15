package com.maxrave.data.di.loader

import android.app.Activity
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.maxrave.data.di.databaseModule
import com.maxrave.data.di.mediaHandlerModule
import com.maxrave.data.di.repositoryModule
import com.maxrave.media3.di.loadMediaService
import com.maxrave.media3.di.setServiceActivitySession
import com.maxrave.media3.di.startService
import com.maxrave.media3.di.stopService
import org.koin.core.context.loadKoinModules

fun loadAllModules() {
    loadKoinModules(
        listOf(
            databaseModule,
            repositoryModule,
        ),
    )
    loadMediaService()
    loadKoinModules(mediaHandlerModule)
}

fun startMediaService(
    context: Context,
    serviceConnection: ServiceConnection,
) {
    startService(context, serviceConnection)
}

fun stopMediaService(context: Context) {
    stopService(context)
}

fun setActivitySession(
    context: Context,
    cls: Class<out Activity>,
    service: IBinder?,
) {
    setServiceActivitySession(context, cls, service)
}