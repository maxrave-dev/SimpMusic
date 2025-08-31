package com.maxrave.kotlinytmusicscraper.utils.poTokenUtils

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import org.schabi.newpipe.extractor.downloader.Downloader

class PoTokenWebViewImpl(
    private val downloader: Downloader,
) : PoTokenGenerator.Factory {
    override suspend fun newPoTokenGenerator(context: Context): PoTokenGenerator =
        suspendCancellableCoroutine { continuation ->
            runOnMainThread(continuation) {
                val potWv = PoTokenWebView(context, downloader, continuation)
                potWv.loadHtmlAndObtainBotguard(context)

                continuation.invokeOnCancellation {
                    potWv.close()
                }
            }
        }
}