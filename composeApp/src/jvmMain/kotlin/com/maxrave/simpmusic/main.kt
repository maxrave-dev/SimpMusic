package com.maxrave.simpmusic

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.ToastType
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.changeLanguageNative
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.ToastHost
import multiplatform.network.cmptoast.showToast
import okhttp3.OkHttpClient
import okio.FileSystem
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import org.koin.mp.KoinPlatform.getKoin
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.circle_app_icon
import simpmusic.composeapp.generated.resources.explicit_content_blocked
import simpmusic.composeapp.generated.resources.time_out_check_internet_connection_or_change_piped_instance_in_settings

@OptIn(ExperimentalMaterial3Api::class)
fun main() =
    application {
        System.setProperty("compose.swing.render.on.graphics", "true")
        System.setProperty("compose.interop.blending", "true")
        System.setProperty("compose.layers.type", "COMPONENT")
        startKoin {
            loadAllModules()
            loadKoinModules(viewModelModule)
        }
        val language =
            runBlocking {
                getKoin()
                    .get<DataStoreManager>()
                    .language
                    .first()
                    .substring(0..1)
            }
        changeLanguageNative(language)
        val windowState =
            rememberWindowState(
                size = DpSize(1280.dp, 720.dp),
            )
        VersionManager.initialize()
        if (BuildKonfig.sentryDsn.isNotEmpty()) {
            Sentry.init { options ->
                options.dsn = BuildKonfig.sentryDsn
                options.release = "simpmusic-desktop@${VersionManager.getVersionName()}"
                options.setDiagnosticLevel(SentryLevel.ERROR)
            }
        }
        val mediaPlayerHandler by inject<MediaPlayerHandler>(MediaPlayerHandler::class.java)
        mediaPlayerHandler.showToast = { type ->
            showToast(
                when (type) {
                    ToastType.ExplicitContent -> runBlocking { getString(Res.string.explicit_content_blocked) }
                    is ToastType.PlayerError ->
                        runBlocking { getString(Res.string.time_out_check_internet_connection_or_change_piped_instance_in_settings, type.error) }
                },
            )
        }
        mediaPlayerHandler.pushPlayerError = { error ->
            Sentry.withScope { scope ->
                Sentry.captureMessage("Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
            }
        }
        val onExitApplication: () -> Unit = {
            mediaPlayerHandler.release()
            exitApplication()
        }
        val sharedViewModel = getKoin().get<SharedViewModel>()
        if (sharedViewModel.shouldCheckForUpdate()) {
            sharedViewModel.checkForUpdate()
        }
        Window(
            onCloseRequest = {
                onExitApplication()
            },
            title = "SimpMusic",
            icon = painterResource(Res.drawable.circle_app_icon),
            undecorated = false,
            state = windowState,
        ) {
            val context = LocalPlatformContext.current
            setSingletonImageLoaderFactory {
                ImageLoader
                    .Builder(context)
                    .components {
                        add(
                            OkHttpNetworkFetcherFactory(
                                callFactory = {
                                    OkHttpClient()
                                },
                            ),
                        )
                    }
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .diskCache(
                        DiskCache
                            .Builder()
                            .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                            .maxSizeBytes(512L * 1024 * 1024)
                            .build(),
                    ).crossfade(true)
                    .build()
            }
            App()
            ToastHost()
        }
    }