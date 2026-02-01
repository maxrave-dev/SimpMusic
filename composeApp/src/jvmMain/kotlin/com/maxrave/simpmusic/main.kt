package com.maxrave.simpmusic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.utils.SingleInstanceManager
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.ToastType
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.ui.component.CustomTitleBar
import com.maxrave.simpmusic.ui.mini_player.MiniPlayerManager
import com.maxrave.simpmusic.ui.mini_player.MiniPlayerWindow
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
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import org.koin.mp.KoinPlatform.getKoin
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.circle_app_icon
import simpmusic.composeapp.generated.resources.close_miniplayer
import simpmusic.composeapp.generated.resources.explicit_content_blocked
import simpmusic.composeapp.generated.resources.open_app
import simpmusic.composeapp.generated.resources.open_miniplayer
import simpmusic.composeapp.generated.resources.quit_app
import simpmusic.composeapp.generated.resources.time_out_check_internet_connection_or_change_piped_instance_in_settings

@OptIn(ExperimentalMaterial3Api::class)
fun main() {
    System.setProperty("compose.swing.render.on.graphics", "true")
    System.setProperty("compose.interop.blending", "true")
    System.setProperty("compose.layers.type", "COMPONENT")

    // Initialize Koin ONCE before application starts
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
                ToastType.ExplicitContent -> {
                    runBlocking { getString(Res.string.explicit_content_blocked) }
                }

                is ToastType.PlayerError -> {
                    runBlocking { getString(Res.string.time_out_check_internet_connection_or_change_piped_instance_in_settings, type.error) }
                }
            },
        )
    }
    mediaPlayerHandler.pushPlayerError = { error ->
        Sentry.withScope { scope ->
            Sentry.captureMessage("Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
        }
    }

    val sharedViewModel = getKoin().get<SharedViewModel>()
    if (sharedViewModel.shouldCheckForUpdate()) {
        sharedViewModel.checkForUpdate()
    }

    application {
        // Main Window
        val windowState =
            rememberWindowState(
                size = DpSize(1340.dp, 860.dp),
            )
        var isVisible by remember { mutableStateOf(true) }
        // Single management
        val isSingleInstance =
            SingleInstanceManager.isSingleInstance(
                onRestoreRequest = {
                    isVisible = true
                    windowState.isMinimized = false
                },
            )

        if (!isSingleInstance) {
            exitApplication()
            return@application
        }
        val openAppString = stringResource(Res.string.open_app)
        val quitAppString = stringResource(Res.string.quit_app)
        val openMiniPlayer = stringResource(Res.string.open_miniplayer)
        val closeMiniPlayer = stringResource(Res.string.close_miniplayer)
        Tray(
            icon = painterResource(Res.drawable.circle_app_icon),
            tooltip = stringResource(Res.string.app_name),
            primaryAction = {
                isVisible = true
                windowState.isMinimized = false
            },
        ) {
            if (!isVisible) {
                Item(openAppString) {
                    isVisible = true
                    windowState.isMinimized = false
                }
            }
            if (MiniPlayerManager.isOpen) {
                Item(closeMiniPlayer) {
                    MiniPlayerManager.isOpen = false
                }
            } else {
                Item(openMiniPlayer) {
                    MiniPlayerManager.isOpen = true
                }
            }
            Divider()
            Item(quitAppString) {
                mediaPlayerHandler.release()
                exitApplication()
            }
        }
        Window(
            onCloseRequest = {
                isVisible = false
            },
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.circle_app_icon),
            undecorated = true,
            transparent = true,
            state = windowState,
            visible = isVisible,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
            ) {
                CustomTitleBar(
                    title = stringResource(Res.string.app_name),
                    windowState = windowState,
                    window = window,
                    onCloseRequest = {
                        isVisible = false
                    },
                )

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
                        }.diskCachePolicy(CachePolicy.ENABLED)
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

        // Mini Player Window (separate window)
        if (MiniPlayerManager.isOpen) {
            MiniPlayerWindow(
                sharedViewModel = sharedViewModel,
                onCloseRequest = {
                    MiniPlayerManager.isOpen = false
                },
            )
        }
    }
}