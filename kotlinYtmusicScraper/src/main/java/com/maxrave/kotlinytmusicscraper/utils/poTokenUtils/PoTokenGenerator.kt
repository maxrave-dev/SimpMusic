package com.maxrave.kotlinytmusicscraper.utils.poTokenUtils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.maxrave.kotlinytmusicscraper.utils.poTokenUtils.JavaScriptUtil.buildExceptionForJsError
import com.maxrave.kotlinytmusicscraper.utils.poTokenUtils.JavaScriptUtil.parseChallengeData
import com.maxrave.kotlinytmusicscraper.utils.poTokenUtils.JavaScriptUtil.parseIntegrityTokenData
import com.maxrave.kotlinytmusicscraper.utils.poTokenUtils.JavaScriptUtil.stringToU8
import com.maxrave.kotlinytmusicscraper.utils.poTokenUtils.JavaScriptUtil.u8ToBase64
import com.maxrave.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.downloader.Downloader
import java.time.Instant
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PoTokenWebView(
    context: Context,
    private val downloader: Downloader,
    // to be used exactly once only during initialization!
    private val generatorContinuation: Continuation<PoTokenGenerator>,
) : PoTokenGenerator {
    private val webView = WebView(context)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val poTokenContinuations = mutableListOf<Pair<String, Continuation<String>>>()
    private lateinit var expirationInstant: Instant

    //region Initialization
    init {
        val webViewSettings = webView.settings
        //noinspection SetJavaScriptEnabled we want to use JavaScript!
        webViewSettings.javaScriptEnabled = true
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(webViewSettings, false)
        }
        webViewSettings.userAgentString = USER_AGENT
        webViewSettings.blockNetworkLoads = true // the WebView does not need internet access

        // so that we can run async functions and get back the result
        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webChromeClient =
            object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                    if (m.message().contains("Uncaught")) {
                        // There should not be any uncaught errors while executing the code, because
                        // everything that can fail is guarded by try-catch. Therefore, this likely
                        // indicates that there was a syntax error in the code, i.e. the WebView only
                        // supports a really old version of JS.

                        val fmt = "\"${m.message()}\", source: ${m.sourceId()} (${m.lineNumber()})"
                        val exception =
                            _root_ide_package_.com.maxrave.kotlinytmusicscraper.utils.poTokenUtils
                                .BadWebViewException(fmt)
                        Logger.e(TAG, "This WebView implementation is broken: $fmt")

                        onInitializationErrorCloseAndCancel(exception)
                        popAllPoTokenContinuations().forEach { (_, continuation) -> continuation.resumeWithException(exception) }
                    }
                    return super.onConsoleMessage(m)
                }
            }
    }

    /**
     * Must be called right after instantiating [PoTokenWebView] to perform the actual
     * initialization. This will asynchronously go through all the steps needed to load BotGuard,
     * run it, and obtain an `integrityToken`.
     */
    fun loadHtmlAndObtainBotguard(context: Context) {
        Logger.d(TAG, "loadHtmlAndObtainBotguard() called")

        coroutineScope.launch {
            try {
                val html =
                    withContext(Dispatchers.IO) {
                        context.assets
                            .open("po_token.html")
                            .bufferedReader()
                            .use { it.readText() }
                    }

                withContext(Dispatchers.Main) {
                    webView.loadDataWithBaseURL(
                        "https://www.youtube.com",
                        html.replaceFirst(
                            "</script>",
                            // calls downloadAndRunBotguard() when the page has finished loading
                            "\n$JS_INTERFACE.downloadAndRunBotguard()</script>",
                        ),
                        "text/html",
                        "utf-8",
                        null,
                    )
                }
            } catch (e: Exception) {
                onInitializationErrorCloseAndCancel(e)
            }
        }
    }

    /**
     * Called during initialization by the JavaScript snippet appended to the HTML page content in
     * [loadHtmlAndObtainBotguard] after the WebView content has been loaded.
     */
    @JavascriptInterface
    fun downloadAndRunBotguard() {
        Logger.d(TAG, "downloadAndRunBotguard() called")

        coroutineScope.launch {
            try {
                val responseBody =
                    makeBotguardServiceRequest(
                        "https://www.youtube.com/api/jnn/v1/Create",
                        "[ \"$REQUEST_KEY\" ]",
                    )
                val parsedChallengeData = parseChallengeData(responseBody)

                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(
                        """try {
                            data = $parsedChallengeData
                            runBotGuard(data).then(function (result) {
                                this.webPoSignalOutput = result.webPoSignalOutput
                                $JS_INTERFACE.onRunBotguardResult(result.botguardResponse)
                            }, function (error) {
                                $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                            })
                        } catch (error) {
                            $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                        }""",
                        null,
                    )
                }
            } catch (e: Exception) {
                onInitializationErrorCloseAndCancel(e)
            }
        }
    }

    /**
     * Called during initialization by the JavaScript snippets from either
     * [downloadAndRunBotguard] or [onRunBotguardResult].
     */
    @JavascriptInterface
    fun onJsInitializationError(error: String) {
        Logger.e(TAG, "Initialization error from JavaScript: $error")
        onInitializationErrorCloseAndCancel(buildExceptionForJsError(error))
    }

    /**
     * Called during initialization by the JavaScript snippet from [downloadAndRunBotguard] after
     * obtaining the BotGuard execution output [botguardResponse].
     */
    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        Logger.d(TAG, "botguardResponse: $botguardResponse")

        coroutineScope.launch {
            try {
                val responseBody =
                    makeBotguardServiceRequest(
                        "https://www.youtube.com/api/jnn/v1/GenerateIT",
                        "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]",
                    )

                Logger.d(TAG, "GenerateIT response: $responseBody")

                val (integrityToken, expirationTimeInSeconds) = parseIntegrityTokenData(responseBody)

                // leave 10 minutes of margin just to be sure
                expirationInstant = Instant.now().plusSeconds(expirationTimeInSeconds - 600)

                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(
                        "this.integrityToken = $integrityToken",
                    ) {
                        Logger.d(TAG, "initialization finished, expiration=${expirationTimeInSeconds}s")
                        generatorContinuation.resume(this@PoTokenWebView)
                    }
                }
            } catch (e: Exception) {
                onInitializationErrorCloseAndCancel(e)
            }
        }
    }
    //endregion

    //region Obtaining poTokens
    override suspend fun generatePoToken(identifier: String): String {
        Logger.d(TAG, "generatePoToken() called with identifier $identifier")

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                addPoTokenContinuation(identifier, continuation)
                val u8Identifier = stringToU8(identifier)
                webView.evaluateJavascript(
                    """try {
                        identifier = "$identifier"
                        u8Identifier = $u8Identifier
                        poTokenU8 = obtainPoToken(webPoSignalOutput, integrityToken, u8Identifier)
                        poTokenU8String = ""
                        for (i = 0; i < poTokenU8.length; i++) {
                            if (i != 0) poTokenU8String += ","
                            poTokenU8String += poTokenU8[i]
                        }
                        $JS_INTERFACE.onObtainPoTokenResult(identifier, poTokenU8String)
                    } catch (error) {
                        $JS_INTERFACE.onObtainPoTokenError(identifier, error + "\n" + error.stack)
                    }""",
                ) {}

                continuation.invokeOnCancellation {
                    // Remove the continuation if the coroutine is cancelled
                    popPoTokenContinuation(identifier)
                }
            }
        }
    }

    /**
     * Called by the JavaScript snippet from [generatePoToken] when an error occurs in calling the
     * JavaScript `obtainPoToken()` function.
     */
    @JavascriptInterface
    fun onObtainPoTokenError(
        identifier: String,
        error: String,
    ) {
        Logger.e(TAG, "obtainPoToken error from JavaScript: $error")
        popPoTokenContinuation(identifier)?.resumeWithException(buildExceptionForJsError(error))
    }

    /**
     * Called by the JavaScript snippet from [generatePoToken] with the original identifier and the
     * result of the JavaScript `obtainPoToken()` function.
     */
    @JavascriptInterface
    fun onObtainPoTokenResult(
        identifier: String,
        poTokenU8: String,
    ) {
        Logger.d(TAG, "Generated poToken (before decoding): identifier=$identifier poTokenU8=$poTokenU8")
        val poToken =
            try {
                u8ToBase64(poTokenU8)
            } catch (t: Throwable) {
                popPoTokenContinuation(identifier)?.resumeWithException(t)
                return
            }

        Logger.d(TAG, "Generated poToken: identifier=$identifier poToken=$poToken")
        popPoTokenContinuation(identifier)?.resume(poToken)
    }

    override fun isExpired(): Boolean = Instant.now().isAfter(expirationInstant)
    //endregion

    //region Handling multiple continuations

    /**
     * Adds the ([identifier], [continuation]) pair to the [poTokenContinuations] list. This makes it so that
     * multiple poToken requests can be generated in parallel, and the results will be notified to
     * the right continuations.
     */
    private fun addPoTokenContinuation(
        identifier: String,
        continuation: Continuation<String>,
    ) {
        synchronized(poTokenContinuations) {
            poTokenContinuations.add(Pair(identifier, continuation))
        }
    }

    /**
     * Extracts and removes from the [poTokenContinuations] list a [Continuation] based on its
     * [identifier]. The continuation is supposed to be used immediately after to either signal a success
     * or an error.
     */
    private fun popPoTokenContinuation(identifier: String): Continuation<String>? =
        synchronized(poTokenContinuations) {
            poTokenContinuations.indexOfFirst { it.first == identifier }.takeIf { it >= 0 }?.let {
                poTokenContinuations.removeAt(it).second
            }
        }

    /**
     * Clears [poTokenContinuations] and returns its previous contents. The continuations are supposed to be
     * used immediately after to either signal a success or an error.
     */
    private fun popAllPoTokenContinuations(): List<Pair<String, Continuation<String>>> =
        synchronized(poTokenContinuations) {
            val result = poTokenContinuations.toList()
            poTokenContinuations.clear()
            result
        }
    //endregion

    //region Utils

    /**
     * Makes a POST request to [url] with the given [data] by setting the correct headers.
     * Throws an exception in case of any network errors and also if the response
     * does not have HTTP code 200, therefore this is supposed to be used only during
     * initialization. Returns the response body if the response is successful.
     */
    private suspend fun makeBotguardServiceRequest(
        url: String,
        data: String,
    ): String =
        withContext(Dispatchers.IO) {
            val response =
                downloader.post(
                    url,
                    mapOf(
                        // replace the downloader user agent
                        "User-Agent" to listOf(USER_AGENT),
                        "Accept" to listOf("application/json"),
                        "Content-Type" to listOf("application/json+protobuf"),
                        "x-goog-api-key" to listOf(GOOGLE_API_KEY),
                        "x-user-agent" to listOf("grpc-web-javascript/0.1"),
                    ),
                    data.toByteArray(),
                )

            val httpCode = response.responseCode()
            if (httpCode != 200) {
                throw PoTokenException("Invalid response code: $httpCode")
            }

            return@withContext response.responseBody()
        }

    /**
     * Handles any error happening during initialization, releasing resources and sending the error
     * to [generatorContinuation].
     */
    private fun onInitializationErrorCloseAndCancel(error: Throwable) {
        runOnMainThread(generatorContinuation) {
            close()
            generatorContinuation.resumeWithException(error)
        }
    }

    /**
     * Releases all [webView] and coroutine resources.
     */
    @MainThread
    override fun close() {
        coroutineScope.cancel()

        webView.clearHistory()
        // clears RAM cache and disk cache (globally for all WebViews)
        webView.clearCache(true)

        // ensures that the WebView isn't doing anything when destroying it
        webView.loadUrl("about:blank")

        webView.onPause()
        webView.removeAllViews()
        webView.destroy()
    }

    companion object {
        private val TAG = "PoTokenWebView"

        // Public API key used by BotGuard, which has been got by looking at BotGuard requests
        const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw" // NOSONAR
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        private const val JS_INTERFACE = "PoTokenWebView"
    }
    //endregion
}

interface PoTokenGenerator : AutoCloseable {
    suspend fun generatePoToken(identifier: String): String

    fun isExpired(): Boolean

    interface Factory {
        suspend fun newPoTokenGenerator(context: Context): PoTokenGenerator
    }
}

/**
 * Runs [runnable] on the main thread using `Handler(Looper.getMainLooper()).post()`, and
 * if the `post` fails emits an error on [continuationIfPostFails].
 */
fun <T> runOnMainThread(
    continuationIfPostFails: Continuation<T>,
    runnable: Runnable,
) {
    if (!Handler(Looper.getMainLooper()).post(runnable)) {
        continuationIfPostFails.resumeWithException(PoTokenException("Could not run on main thread"))
    }
}