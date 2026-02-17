package com.maxrave.kotlinytmusicscraper.utils.poTokenUtils

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import org.schabi.newpipe.extractor.downloader.Downloader
import kotlin.coroutines.resumeWithException

class PoTokenWebViewImpl(
    private val downloader: Downloader,
) : PoTokenGenerator.Factory {
    override suspend fun newPoTokenGenerator(context: Context): PoTokenGenerator =
        suspendCancellableCoroutine { continuation ->
            runOnMainThread(continuation) {
                try {
                    val potWv = PoTokenWebView(context, downloader, continuation)
                    potWv.loadHtmlAndObtainBotguard(context)

                    continuation.invokeOnCancellation {
                        potWv.close()
                    }
                } catch (t: Throwable) {
                    // WearOS devices may not ship a WebView provider at all, which can cause
                    // WebView construction to throw. Treat this as a broken WebView.
                    continuation.resumeWithException(
                        BadWebViewException(t.message ?: "WebView is unavailable"),
                    )
                }
            }
        }
}
