package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.desktop_webview_description
import simpmusic.composeapp.generated.resources.open_blog_post
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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(Res.string.desktop_webview_description),
                style = typo().labelMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {
                    openUrl("https://www.simpmusic.org/blogs/en/how-to-log-in-on-desktop-app")
                },
            ) {
                Text(
                    stringResource(Res.string.open_blog_post),
                    style = typo().labelMedium,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
        aboveContent()
    }
}

@Composable
actual fun DiscordWebView(
    state: MutableState<WebViewState>,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onLoginDone: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(Res.string.desktop_webview_description),
                style = typo().labelMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {
                    openUrl("https://www.simpmusic.org/blogs/en/how-to-log-in-to-Discord-on-desktop-app")
                },
            ) {
                Text(
                    stringResource(Res.string.open_blog_post),
                    style = typo().labelMedium,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
        aboveContent()
    }
}