package com.maxrave.simpmusic

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.simpmusic.di.viewModelModule
import multiplatform.network.cmptoast.ToastHost
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_icon

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
        var windowState =
            rememberWindowState(
                size = DpSize(1280.dp, 1000.dp),
            )
        Window(
            onCloseRequest = ::exitApplication,
            title = "SimpMusic",
            icon = painterResource(Res.drawable.app_icon),
            undecorated = false,
            state = windowState,
        ) {
            App()
            ToastHost()
        }
    }