package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.maxrave.logger.Logger
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.net.CookieHandler
import java.net.CookieManager
import java.net.URI

actual fun createWebViewCookieManager(): WebViewCookieManager =
    object : WebViewCookieManager {
        override fun getCookie(url: String): String =
            CookieHandler
                .getDefault()
                .get(URI(url), emptyMap())["Cookie"]
                ?.joinToString("; ") ?: ""

        override fun removeAllCookies() {
            CookieHandler.setDefault(
                CookieManager(),
            )
        }
    }

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
actual fun PlatformWebView(
    state: MutableState<WebViewState>,
    initUrl: String,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onPageFinished: (String) -> Unit,
) {
    val jfxPanel = remember { JFXPanel() }
    var webView by remember {
        mutableStateOf<WebView?>(null)
    }

    DisposableEffect(Unit) {
        Platform.setImplicitExit(false)
        Platform.runLater {
            val wv =
                WebView().apply {
                    engine.isJavaScriptEnabled = true
                }
            webView = wv
            wv.engine.load(initUrl)
            Logger.w("WebView", "Loading URL: $initUrl")
            wv.engine.loadWorker.stateProperty().addListener(
                ChangeListener<Worker.State> { observable, oldValue, newValue ->
                    when (newValue) {
                        Worker.State.SUCCEEDED -> {
                            state.value = WebViewState.Finished
                            onPageFinished(
                                wv.engine.location,
                            )
                        }

                        Worker.State.RUNNING -> {
                            state.value = WebViewState.Loading((wv.engine.loadWorker.progress * 100).toInt())
                        }

                        else -> {}
                    }
                },
            )
        }
        onDispose {
            webView = null
            jfxPanel.scene = null
        }
    }
    Box {
        if (webView != null) {
            SwingPanel(
                factory = {
                    jfxPanel.apply {
                        // Set the scene only if webView is not null
                        webView?.let {
                            scene = Scene(it)
                        }
                    }
                },
                modifier =
                    Modifier
                        .fillMaxSize(),
            )
        }
        aboveContent()
    }
}