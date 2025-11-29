package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

interface WebViewCookieManager {
    fun getCookie(url: String): String

    fun removeAllCookies()
}

expect fun createWebViewCookieManager(): WebViewCookieManager

sealed class WebViewState {
    data class Loading(
        val progress: Int,
    ) : WebViewState()

    object Finished : WebViewState()
}

@Composable
fun rememberWebViewState(): MutableState<WebViewState> =
    remember {
        mutableStateOf(WebViewState.Loading(0))
    }

@Composable
expect fun PlatformWebView(
    state: MutableState<WebViewState> = rememberWebViewState(),
    initUrl: String,
    aboveContent: @Composable (BoxScope.() -> Unit) = {},
    onPageFinished: (String) -> Unit,
)

@Composable
expect fun DiscordWebView(
    state: MutableState<WebViewState> = rememberWebViewState(),
    aboveContent: @Composable (BoxScope.() -> Unit) = {},
    onLoginDone: (token: String) -> Unit
)