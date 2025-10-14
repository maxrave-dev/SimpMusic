package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
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

@Composable
actual fun PlatformWebView(
    state: MutableState<WebViewState>,
    initUrl: String,
    onPageFinished: (String) -> Unit,
) {
    val jfxPanel = remember { JFXPanel() }
    var webView by remember {
        mutableStateOf<WebView?>(null)
    }

    LaunchedEffect(Unit) {
        Platform.runLater {
            val wv =
                WebView().apply {
                    engine.isJavaScriptEnabled = true
                }
            jfxPanel.scene = Scene(wv)
            webView = wv
        }
    }

    webView?.let { wv ->
        LaunchedEffect(wv) {
            Platform.runLater {
                wv.engine.load(initUrl)
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
        }

        SwingPanel(
            factory = {
                jfxPanel
            },
            update = {
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}