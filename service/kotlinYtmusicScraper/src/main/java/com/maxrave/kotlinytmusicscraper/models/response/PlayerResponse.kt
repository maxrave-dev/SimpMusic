package com.maxrave.kotlinytmusicscraper.models.response

import com.maxrave.kotlinytmusicscraper.models.ResponseContext
import com.maxrave.kotlinytmusicscraper.models.Thumbnails
import com.maxrave.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * PlayerResponse with [com.zionhuang.innertube.models.YouTubeClient.ANDROID_MUSIC] client
 */
@Serializable
data class PlayerResponse(
    val responseContext: ResponseContext,
    val playabilityStatus: PlayabilityStatus,
    val playerConfig: PlayerConfig?,
    val streamingData: StreamingData?,
    val videoDetails: VideoDetails?,
    @SerialName("playbackTracking")
    val playbackTracking: PlaybackTracking?,
    @SerialName("captions")
    val captions: YouTubeInitialPage.Captions?,
) {
    @Serializable
    data class PlayabilityStatus(
        val status: String,
        val reason: String?,
    )

    @Serializable
    data class PlayerConfig(
        val audioConfig: AudioConfig,
    ) {
        @Serializable
        data class AudioConfig(
            val loudnessDb: Double?,
            val perceptualLoudnessDb: Double?,
        )
    }

    @Serializable
    data class StreamingData(
        val formats: List<Format>?,
        val adaptiveFormats: List<Format>,
        val expiresInSeconds: Int,
    ) {
        @Serializable
        data class Format(
            val itag: Int,
            val url: String?,
            val mimeType: String,
            val bitrate: Int,
            val width: Int?,
            val height: Int?,
            val contentLength: Long?,
            val quality: String,
            val fps: Int?,
            val qualityLabel: String?,
            val averageBitrate: Int?,
            val audioQuality: String?,
            val approxDurationMs: String?,
            val audioSampleRate: Int?,
            val audioChannels: Int?,
            val loudnessDb: Double?,
            val lastModified: Long?,
            val signatureCipher: String?,
        ) {
            val isAudio: Boolean
                get() = width == null
        }
    }

    @Serializable
    data class VideoDetails(
        val videoId: String,
        val title: String?,
        val author: String?,
        val channelId: String,
        val authorAvatar: String?,
        val authorSubCount: String?,
        val lengthSeconds: String,
        val musicVideoType: String?,
        val viewCount: String?,
        val thumbnail: Thumbnails,
        val description: String?,
    )

    @Serializable
    data class PlaybackTracking(
        @SerialName("videostatsPlaybackUrl")
        val videostatsPlaybackUrl: VideostatsPlaybackUrl?,
        @SerialName("videostatsWatchtimeUrl")
        val videostatsWatchtimeUrl: VideostatsWatchtimeUrl?,
        @SerialName("atrUrl")
        val atrUrl: AtrUrl?,
    ) {
        @Serializable
        data class VideostatsPlaybackUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )

        @Serializable
        data class VideostatsWatchtimeUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )

        @Serializable
        data class AtrUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )
    }
}