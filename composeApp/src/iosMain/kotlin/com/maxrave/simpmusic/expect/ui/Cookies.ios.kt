@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.NSObject

private object IosCookies : WebViewCookieManager {
    private var cookies: List<NSHTTPCookie> = emptyList()
    fun refresh(onDone: () -> Unit = {}) {
        WKWebsiteDataStore.defaultDataStore().httpCookieStore.getAllCookies { values ->
            cookies = values.orEmpty().filterIsInstance<NSHTTPCookie>()
            onDone()
        }
    }
    override fun getCookie(url: String): String {
        val host = NSURL.URLWithString(url)?.host.orEmpty()
        return cookies.filter { host == it.domain.trimStart('.') || host.endsWith("." + it.domain.trimStart('.')) }
            .joinToString("; ") { "${it.name}=${it.value}" }
    }
    override fun removeAllCookies() {
        cookies = emptyList()
        val store = WKWebsiteDataStore.defaultDataStore().httpCookieStore
        store.getAllCookies { values -> values.orEmpty().filterIsInstance<NSHTTPCookie>().forEach { store.deleteCookie(it, null) } }
    }
}
actual fun createWebViewCookieManager(): WebViewCookieManager = IosCookies

@Composable
actual fun PlatformWebView(state: MutableState<WebViewState>, initUrl: String,
    aboveContent: @Composable BoxScope.() -> Unit, clearCookies: Boolean, onPageFinished: (String) -> Unit) {
    val callback by rememberUpdatedState(onPageFinished)
    val delegate = remember { object : NSObject(), WKNavigationDelegateProtocol {
        @kotlinx.cinterop.ObjCSignatureOverride
        override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
            state.value = WebViewState.Loading(0)
        }
        @kotlinx.cinterop.ObjCSignatureOverride
        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            IosCookies.refresh {
                state.value = WebViewState.Finished
                callback(webView.URL?.absoluteString.orEmpty())
            }
        }
    } }
    Box(Modifier.fillMaxSize()) {
        UIKitView(factory = {
            WKWebView(frame = kotlinx.cinterop.cValue { }, configuration = WKWebViewConfiguration()).apply {
                navigationDelegate = delegate
                customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1"
                val request = NSURLRequest(NSURL.URLWithString(initUrl)!!)
                if (clearCookies) {
                    val store = WKWebsiteDataStore.defaultDataStore()
                    store.removeDataOfTypes(WKWebsiteDataStore.allWebsiteDataTypes(), NSDate.distantPast) { loadRequest(request) }
                } else loadRequest(request)
            }
        }, modifier = Modifier.fillMaxSize(), onRelease = { it.stopLoading(); it.navigationDelegate = null })
        aboveContent()
    }
}

@Composable
actual fun DiscordWebView(state: MutableState<WebViewState>, aboveContent: @Composable BoxScope.() -> Unit,
    onLoginDone: (String) -> Unit) {
    val callback by rememberUpdatedState(onLoginDone)
    var webView by remember { mutableStateOf<WKWebView?>(null) }
    val delegate = remember { object : NSObject(), WKNavigationDelegateProtocol {
        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            state.value = WebViewState.Finished
        }
    } }
    // Discord navigates inside a single page after sign-in, without a navigation callback.
    LaunchedEffect(webView) {
        val view = webView ?: return@LaunchedEffect
        var delivered = false
        while (isActive && !delivered) {
            if (view.URL?.host == "discord.com") {
                view.evaluateJavaScript("""
                    (function() {
                        var frame = document.createElement('iframe');
                        document.body.appendChild(frame);
                        var token = frame.contentWindow.localStorage.getItem('token');
                        frame.remove();
                        return token;
                    })()
                """.trimIndent()) { token, _ ->
                    (token as? String)?.trim('"')?.takeIf { it.isNotBlank() }?.let {
                        delivered = true
                        callback(it)
                    }
                }
            }
            delay(1000)
        }
    }
    Box(Modifier.fillMaxSize()) {
        UIKitView(factory = {
            WKWebView(frame = kotlinx.cinterop.cValue { }, configuration = WKWebViewConfiguration()).apply {
                navigationDelegate = delegate
                webView = this
                loadRequest(NSURLRequest(NSURL.URLWithString("https://discord.com/login")!!))
            }
        }, modifier = Modifier.fillMaxSize(), onRelease = { it.stopLoading(); it.navigationDelegate = null; webView = null })
        aboveContent()
    }
}
