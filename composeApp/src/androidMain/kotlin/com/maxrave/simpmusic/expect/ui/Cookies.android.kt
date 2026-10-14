package com.maxrave.simpmusic.expect.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.maxrave.logger.Logger
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.unavailable

actual fun createWebViewCookieManager(): WebViewCookieManager =
    object : WebViewCookieManager {
        // getCookie is null when no cookie matches this url, even if hasCookies() is true for others.
        override fun getCookie(url: String): String = CookieManager.getInstance().getCookie(url).orEmpty()

        override fun removeAllCookies() {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }

@Composable
actual fun PlatformWebView(
    state: MutableState<WebViewState>,
    initUrl: String,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onPageFinished: (String) -> Unit,
) {
    val unavailable = stringResource(Res.string.unavailable)
    Box {
        AndroidView(
            factory = { ctx ->
                webViewOrFallback(ctx, unavailable) {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    webViewClient =
                        object : WebViewClient() {
                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                url?.let {
                                    onPageFinished(it)
                                }
                            }
                        }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    loadUrl(initUrl)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        aboveContent()
    }
}

@Composable
actual fun DiscordWebView(
    state: MutableState<WebViewState>,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onLoginDone: (String) -> Unit
) {
    val url = "https://discord.com/login"
    val unavailable = stringResource(Res.string.unavailable)
    Box {
        AndroidView(factory = {
            webViewOrFallback(it, unavailable) {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(
                        webView: WebView,
                        url: String,
                    ): Boolean {
                        stopLoading()
                        if (url.endsWith("/app")) {
                            loadUrl(JS_SNIPPET)
                        }
                        return false
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                if (android.os.Build.MANUFACTURER.equals(MOTOROLA, ignoreCase = true)) {
                    settings.userAgentString = SAMSUNG_USER_AGENT
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onJsAlert(
                        view: WebView,
                        url: String,
                        message: String,
                        result: JsResult,
                    ): Boolean {
                        onLoginDone(message)
                        return true
                    }
                }
                loadUrl(url)
            }
        })
        aboveContent()
    }
}

// Devices without a working WebView provider (watches, a WebView mid-update) throw from the constructor.
private fun webViewOrFallback(
    context: Context,
    unavailable: String,
    setup: WebView.() -> Unit,
): View =
    runCatching { WebView(context).apply(setup) }.getOrElse { e ->
        Logger.e("PlatformWebView", "WebView unavailable", e)
        TextView(context).apply {
            text = unavailable
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
    }

const val JS_SNIPPET =
    "javascript:(function()%7Bvar%20i%3Ddocument.createElement('iframe')%3Bdocument.body.appendChild(i)%3Balert(i.contentWindow.localStorage.token.slice(1,-1))%7D)()"
private const val MOTOROLA = "motorola"
private const val SAMSUNG_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.363"