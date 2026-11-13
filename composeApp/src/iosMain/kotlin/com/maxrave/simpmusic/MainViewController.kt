package com.maxrave.simpmusic

import androidx.compose.ui.window.ComposeUIViewController
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
        App()
    }
}
