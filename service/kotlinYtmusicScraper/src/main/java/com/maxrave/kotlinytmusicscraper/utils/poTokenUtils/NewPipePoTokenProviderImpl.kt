package com.maxrave.kotlinytmusicscraper.utils.poTokenUtils

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.maxrave.logger.Logger
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor

private const val TAG = "NewPipePoTokenProviderImpl"

class NewPipePoTokenProviderImpl(
    private val context: Context,
    downloader: Downloader,
) : PoTokenProvider {
    init {
        YoutubeStreamExtractor.setPoTokenProvider(this)
    }

    private var webViewBadImpl = false // whether the system has a bad WebView implementation
    private val poTokenProviderImpl = PoTokenWebViewImpl(downloader)

    private var webPoTokenVisitorData: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenGenerator? = null

    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        if (webViewBadImpl) {
            return null
        }

        try {
            return runBlocking { getWebClientPoToken(videoId = videoId, forceRecreate = false) }
        } catch (e: RuntimeException) {
            // RxJava's Single wraps exceptions into RuntimeErrors, so we need to unwrap them here
            when (val cause = e.cause) {
                is BadWebViewException -> {
                    Logger.e(TAG, "Could not obtain poToken because WebView is broken", e)
                    webViewBadImpl = true
                    return null
                }
                null -> throw e
                else -> throw cause // includes PoTokenException
            }
        }
    }

    /**
     * @param forceRecreate whether to force the recreation of [webPoTokenGenerator], to be used in
     * case the current [webPoTokenGenerator] threw an error last time
     * [PoTokenGenerator.generatePoToken] was called
     */
    private suspend fun getWebClientPoToken(
        videoId: String,
        forceRecreate: Boolean,
    ): PoTokenResult {
        // just a helper class since Kotlin does not have builtin support for 4-tuples
        data class Quadruple<T1, T2, T3, T4>(
            val t1: T1,
            val t2: T2,
            val t3: T3,
            val t4: T4,
        )
        val shouldRecreate =
            webPoTokenGenerator == null || forceRecreate ||
                webPoTokenGenerator!!.isExpired()

        if (shouldRecreate) {
            val innertubeClientRequestInfo = InnertubeClientRequestInfo.ofWebClient()
            innertubeClientRequestInfo.clientInfo.clientVersion =
                YoutubeParsingHelper.getClientVersion()

            webPoTokenVisitorData =
                YoutubeParsingHelper.getVisitorDataFromInnertube(
                    innertubeClientRequestInfo,
                    NewPipe.getPreferredLocalization(),
                    NewPipe.getPreferredContentCountry(),
                    YoutubeParsingHelper.getYouTubeHeaders(),
                    YoutubeParsingHelper.YOUTUBEI_V1_URL,
                    null,
                    false,
                )
            // close the current webPoTokenGenerator on the main thread
            webPoTokenGenerator?.let { Handler(Looper.getMainLooper()).post { it.close() } }

            // create a new webPoTokenGenerator
            webPoTokenGenerator =
                poTokenProviderImpl
                    .newPoTokenGenerator(context)

            // The streaming poToken needs to be generated exactly once before generating
            // any other (player) tokens.
            webPoTokenStreamingPot =
                webPoTokenGenerator!!
                    .generatePoToken(webPoTokenVisitorData!!)
        }
        val (poTokenGenerator, visitorData, streamingPot, hasBeenRecreated) =
            Quadruple(
                webPoTokenGenerator!!,
                webPoTokenVisitorData!!,
                webPoTokenStreamingPot!!,
                shouldRecreate,
            )

        val playerPot =
            try {
                // Not using synchronized here, since poTokenGenerator would be able to generate
                // multiple poTokens in parallel if needed. The only important thing is for exactly one
                // visitorData/streaming poToken to be generated before anything else.
                poTokenGenerator.generatePoToken(videoId)
            } catch (throwable: Throwable) {
                if (hasBeenRecreated) {
                    // the poTokenGenerator has just been recreated (and possibly this is already the
                    // second time we try), so there is likely nothing we can do
                    throw throwable
                } else {
                    // retry, this time recreating the [webPoTokenGenerator] from scratch;
                    // this might happen for example if NewPipe goes in the background and the WebView
                    // content is lost
                    Logger.e(TAG, "Failed to obtain poToken, retrying", throwable)
                    return getWebClientPoToken(videoId = videoId, forceRecreate = true)
                }
            }

        Logger.d(
            TAG,
            "poToken for $videoId: playerPot=$playerPot, " +
                "streamingPot=$streamingPot, visitor_data=$visitorData",
        )

        return PoTokenResult(visitorData, playerPot, streamingPot)
    }

    override fun getWebEmbedClientPoToken(videoId: String): PoTokenResult? = null

    override fun getAndroidClientPoToken(videoId: String): PoTokenResult? = null

    override fun getIosClientPoToken(videoId: String): PoTokenResult? = null
}