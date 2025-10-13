package com.maxrave.simpmusic

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.simpmusic.di.viewModelModule
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin

fun main() =
    application {
        startKoin {
            loadAllModules()
            loadKoinModules(viewModelModule)
        }
        Window(
            onCloseRequest = ::exitApplication,
            title = "SimpMusic",
        ) {
            App()
        }
    }