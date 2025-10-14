package com.maxrave.simpmusic

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.di.viewModelModule
import com.multiplatform.webview.util.addTempDirectoryRemovalHook
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.ToastHost
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_icon
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
fun main() =
    application {
        addTempDirectoryRemovalHook()
        startKoin {
            loadAllModules()
            loadKoinModules(viewModelModule)
        }

        var windowState = rememberWindowState(
            size = DpSize(1280.dp, 720.dp),
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "SimpMusic",
            icon = painterResource(Res.drawable.app_icon),
            undecorated = false,
            state = windowState
        ) {
            var restartRequired by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableStateOf(-1F) }
            var initialized by remember { mutableStateOf(false) } // if true, KCEF can be used to create clients, browsers etc
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    KCEF.init(builder = {
                        installDir(File("kcef-bundle")) // recommended, but not necessary
                        progress {
                            onDownloading {
                                Logger.d("MAIN", "KCEF Download Progress: $it")
                                downloadProgress = it
                                // use this if you want to display a download progress for example
                            }
                            onInitialized {
                                initialized = true
                            }
                        }
                    }, onError = {
                        it?.printStackTrace()
                        downloadProgress = -1f
                        restartRequired = false
                        initialized = false
                        showToast(
                            "Failed to initialize WebView. Please check logs. ${it?.localizedMessage}",
                            gravity = ToastGravity.Bottom
                        )
                    }, onRestartRequired = {
                        restartRequired = true
                    })
                }
            }
            App()
            ToastHost()
            if (restartRequired) {
                showToast(
                    "SimpMusic needs to be restarted to complete WebView initialization.",
                    gravity = ToastGravity.Bottom
                ) // to ensure toast host is initialized
            }
            if (initialized) {
                showToast("KCEF Initialized", gravity = ToastGravity.Bottom)
            }
            if (downloadProgress in 1f..99f) {
                showToast("Downloading WebView Runtime: ${downloadProgress.toInt()}%", gravity = ToastGravity.Bottom)
            }
        }
    }