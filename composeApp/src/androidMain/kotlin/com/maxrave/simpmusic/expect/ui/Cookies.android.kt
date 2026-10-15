package com.maxrave.simpmusic.expect.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewOutcomeReceiver
import androidx.webkit.WebViewStartUpConfig
import androidx.webkit.WebViewStartUpResult
import androidx.webkit.WebViewStartupException
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.unavailable
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

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
    clearCookies: Boolean,
    onPageFinished: (String) -> Unit,
) {
    // Once per visit: Retry rebuilds the WebView, and clearing again would drop a sign-in that
    // finished before the renderer was killed.
    val cookiesCleared = remember { AtomicBoolean(false) }
    Box {
        ManagedWebView { onRendererGone ->
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

                    override fun onRenderProcessGone(
                        view: WebView,
                        detail: RenderProcessGoneDetail,
                    ): Boolean = handleRendererGone(detail, onRendererGone)
                }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            // Runs inside the factory, so after ManagedWebView's startup has finished.
            if (clearCookies && cookiesCleared.compareAndSet(false, true)) {
                createWebViewCookieManager().removeAllCookies()
            }
            loadUrl(initUrl)
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
    val url = "https://discord.com/login"
    Box {
        ManagedWebView { onRendererGone ->
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

                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail,
                ): Boolean = handleRendererGone(detail, onRendererGone)
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
        aboveContent()
    }
}

/**
 * Hosts a login WebView so that a low-RAM phone cannot take the whole app down with it.
 *
 * - The first WebView in a process starts Chromium on the main thread; on 3–4 GB phones that blocked
 *   for seconds and ended in an ANR (Sentry ANDROID-NFV). [WebViewCompat.startUpWebView] runs that
 *   startup off the main thread first, with a spinner in the meantime.
 * - When the system kills the renderer to reclaim memory, an unhandled `onRenderProcessGone` kills the
 *   app too. The page gets a Retry button instead, which builds a fresh WebView: a WebView whose
 *   renderer is gone cannot be reused.
 * - Leaving the screen destroys the WebView, so the heavy page it ended on (music.youtube.com,
 *   discord.com/app) stops holding memory right away instead of whenever it is finalized.
 */
@Composable
private fun ManagedWebView(configure: WebView.(onRendererGone: () -> Unit) -> Unit) {
    val appContext = LocalContext.current.applicationContext
    val unavailable = stringResource(Res.string.unavailable)
    var ready by remember { mutableStateOf(false) }
    var startupFailed by remember { mutableStateOf(false) }
    var rendererGone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // startUpWebView runs its tasks on this executor with no try/catch of its own: on a device
        // without a usable WebView the provider load throws, which would crash the process from an
        // IO worker. Catch it here instead.
        val executor =
            Executor { task ->
                Dispatchers.IO.asExecutor().execute {
                    try {
                        task.run()
                    } catch (e: Throwable) {
                        Logger.e("PlatformWebView", "WebView startup threw", e)
                        startupFailed = true
                    }
                }
            }
        WebViewCompat.startUpWebView(
            appContext,
            WebViewStartUpConfig.Builder(executor).build(),
            object : WebViewOutcomeReceiver<WebViewStartUpResult, WebViewStartupException> {
                // Both callbacks arrive on the UI thread.
                override fun onResult(result: WebViewStartUpResult) {
                    ready = true
                }

                // After a reported failure any WebView call may throw or crash natively, so none is made.
                override fun onError(error: WebViewStartupException) {
                    Logger.e("PlatformWebView", "WebView startup failed", error)
                    startupFailed = true
                }
            },
        )
    }

    when {
        startupFailed -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(unavailable)
            }
        }

        !ready -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        rendererGone -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Leaving this branch for the AndroidView below composes a new one, so factory runs again.
                Button(onClick = { rendererGone = false }) {
                    Text(stringResource(Res.string.retry), color = LocalContentColor.current)
                }
            }
        }

        else -> {
            AndroidView(
                factory = { ctx ->
                    webViewOrFallback(ctx, unavailable) { configure { rendererGone = true } }
                },
                onRelease = { view ->
                    (view as? WebView)?.run {
                        stopLoading()
                        destroy()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun handleRendererGone(
    detail: RenderProcessGoneDetail,
    onRendererGone: () -> Unit,
): Boolean {
    Logger.w("PlatformWebView", "WebView renderer gone, crashed=${detail.didCrash()}")
    onRendererGone()
    // true keeps the app alive; the WebView is destroyed when the Retry branch replaces it.
    return true
}

// Devices without a working WebView provider (watches, a WebView mid-update) throw from the constructor.
private fun webViewOrFallback(
    context: Context,
    unavailable: String,
    setup: WebView.() -> Unit,
): View {
    val webView = runCatching { WebView(context) }.getOrElse { e -> return unavailableView(context, unavailable, e) }
    return runCatching { webView.apply(setup) }.getOrElse { e ->
        // onRelease only ever sees the fallback view, so a WebView that failed setup is destroyed here.
        runCatching { webView.destroy() }
        unavailableView(context, unavailable, e)
    }
}

private fun unavailableView(
    context: Context,
    unavailable: String,
    e: Throwable,
): View {
    Logger.e("PlatformWebView", "WebView unavailable", e)
    return TextView(context).apply {
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
