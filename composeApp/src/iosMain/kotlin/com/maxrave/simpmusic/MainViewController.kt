package com.maxrave.simpmusic

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import kotlinx.coroutines.flow.first
import org.koin.mp.KoinPlatform.getKoin
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.maxrave.common.AppIdentity
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.data.io.fileDir
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.utils.VersionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.context.startKoin
import org.koin.core.context.loadKoinModules
import platform.UIKit.UIDevice

private var initialized = false

private fun initializeIosApp() {
    if (initialized) return
    com.maxrave.simpmusic.viewModel.applyPendingIosRestore()
    Logger.enableFileLogging(fileDir() + "/logs")
    startKoin {
        loadAllModules(AppIdentity(
            applicationId = "com.maxrave.simpmusic",
            versionName = BuildKonfig.versionName,
            platform = "iOS " + UIDevice.currentDevice.systemVersion,
            isDevBuild = false,
        ))
        loadKoinModules(viewModelModule)
    }
    VersionManager.initialize()
    initialized = true
}

fun MainViewController(): platform.UIKit.UIViewController {
    initializeIosApp()
    return ComposeUIViewController {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(HttpClient(Darwin))) }
            .build()
    }
        var preferencesReady by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            // Music is unavailable in some system regions. Apply the app's supported region
            // and language before Home launches requests, without blocking the main thread.
            val settings = getKoin().get<DataStoreManager>()
            getKoin().get<YouTube>().locale = YouTubeLocale(
                settings.location.first(), settings.language.first().take(2),
            )
            preferencesReady = true
        }
        if (preferencesReady) App()
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}
