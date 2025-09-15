package com.maxrave.data.di

import com.maxrave.common.Config
import com.maxrave.data.mediaservice.MediaServiceHandlerImpl
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.media3.exoplayer.ExoPlayerAdapter
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val mediaHandlerModule =
    module {
        single<MediaPlayerHandler> {
            MediaServiceHandlerImpl(
                inputPlayer = get<ExoPlayerAdapter>(),
                context = androidContext(),
                dataStoreManager = get(),
                songRepository = get(),
                streamRepository = get(),
                localPlaylistRepository = get(),
                coroutineScope = get(named(Config.SERVICE_SCOPE)),
                updateWidget = {},
                updatePlayStatusForWidget = { _, _ -> },
                setNotificationLayout = { _, _, _ -> },
                pushPlayerError = { _ -> },
            )
        }
    }