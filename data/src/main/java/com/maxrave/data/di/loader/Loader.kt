package com.maxrave.data.di.loader

import com.maxrave.data.di.databaseModule
import com.maxrave.data.di.mediaHandlerModule
import com.maxrave.data.di.repositoryModule
import com.maxrave.media3.di.loadMediaService
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