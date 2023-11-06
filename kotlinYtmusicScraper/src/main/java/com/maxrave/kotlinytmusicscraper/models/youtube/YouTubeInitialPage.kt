package com.maxrave.kotlinytmusicscraper.models.youtube

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeInitialPage(
    @SerialName("videoDetails")
    val videoDetails: VideoDetails?,
    @SerialName("captions")
    val captions: Captions? = null,
) {
    @Serializable
    data class Captions(
        @SerialName("playerCaptionsTracklistRenderer")
        val playerCaptionsTracklistRenderer: PlayerCaptionsTracklistRenderer?,
    ) {
        @Serializable
        data class PlayerCaptionsTracklistRenderer(
            @SerialName("captionTracks")
            val captionTracks: List<CaptionTrack>?,
        ) {
            @Serializable
            data class CaptionTrack(
                @SerialName("baseUrl")
                val baseUrl: String?,
                @SerialName("name")
                val name: Name?,
                @SerialName("vssId")
                val vssId: String?,
            ) {
                @Serializable
                data class Name(
                    @SerialName("simpleText")
                    val simpleText: String?,
                )
            }
        }
    }
}
