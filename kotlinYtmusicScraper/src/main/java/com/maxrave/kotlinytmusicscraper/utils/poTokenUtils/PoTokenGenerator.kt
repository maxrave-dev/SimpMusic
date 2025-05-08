package com.maxrave.kotlinytmusicscraper.utils.poTokenUtils

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.maxrave.kotlinytmusicscraper.models.PoToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.Proxy

class PoTokenGenerator(
    private val context: Context,
) {
    @Suppress("ktlint:standard:property-naming")
    private val TAG = "PoTokenGenerator"

    private val webViewSupported by lazy { runCatching { CookieManager.getInstance() }.isSuccess }
    private var webViewBadImpl = false // whether the system has a bad WebView implementation

    private val webPoTokenGenLock = Mutex()
    private var webPoTokenSessionId: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenWebView? = null

    fun getWebClientPoToken(
        videoId: String,
        sessionId: String,
        proxy: Proxy? = null,
    ): PoToken? {
        if (!webViewSupported || webViewBadImpl) {
            return null
        }

        return try {
            runBlocking { getWebClientPoToken(videoId, sessionId, forceRecreate = false, proxy) }
        } catch (e: Exception) {
            when (e) {
                is BadWebViewException -> {
                    Log.e(TAG, "Could not obtain poToken because WebView is broken", e)
                    webViewBadImpl = true
                    null
                }
                else -> throw e // includes PoTokenException
            }
        }
    }

    /**
     * @param forceRecreate whether to force the recreation of [webPoTokenGenerator], to be used in
     * case the current [webPoTokenGenerator] threw an error last time
     * [PoTokenWebView.generatePoToken] was called
     */
    private suspend fun getWebClientPoToken(
        videoId: String,
        sessionId: String,
        forceRecreate: Boolean,
        proxy: Proxy? = null,
    ): PoToken {
        Log.d(TAG, "Web poToken requested: $videoId, $sessionId")

        val (poTokenGenerator, streamingPot, hasBeenRecreated) =
            webPoTokenGenLock.withLock {
                val shouldRecreate =
                    forceRecreate || webPoTokenGenerator == null || webPoTokenGenerator!!.isExpired || webPoTokenSessionId != sessionId

                if (shouldRecreate) {
                    webPoTokenSessionId = sessionId

                    withContext(Dispatchers.Main) {
                        webPoTokenGenerator?.close()
                    }

                    // create a new webPoTokenGenerator
                    webPoTokenGenerator = PoTokenWebView.getNewPoTokenGenerator(context, proxy)

                    // The streaming poToken needs to be generated exactly once before generating
                    // any other (player) tokens.
                    webPoTokenStreamingPot = webPoTokenGenerator!!.generatePoToken(webPoTokenSessionId!!)
                }

                Triple(webPoTokenGenerator!!, webPoTokenStreamingPot!!, shouldRecreate)
            }

        val playerPot =
            try {
                // Not using synchronized here, since poTokenGenerator would be able to generate
                // multiple poTokens in parallel if needed. The only important thing is for exactly one
                // streaming poToken (based on [sessionId]) to be generated before anything else.
                poTokenGenerator.generatePoToken(videoId)
            } catch (throwable: Throwable) {
                if (hasBeenRecreated) {
                    // the poTokenGenerator has just been recreated (and possibly this is already the
                    // second time we try), so there is likely nothing we can do
                    throw throwable
                } else {
                    // retry, this time recreating the [webPoTokenGenerator] from scratch;
                    // this might happen for example if the app goes in the background and the WebView
                    // content is lost
                    Log.e(TAG, "Failed to obtain poToken, retrying", throwable)
                    return getWebClientPoToken(videoId = videoId, sessionId = sessionId, forceRecreate = true, proxy)
                }
            }

        Log.d(TAG, "[$videoId] playerPot=$playerPot, streamingPot=$streamingPot")

        return PoToken(playerPot, streamingPot)
    }
}