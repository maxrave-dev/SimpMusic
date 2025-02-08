package com.maxrave.kotlinytmusicscraper.models

import com.maxrave.kotlinytmusicscraper.models.youtube.data.YouTubeDataPage
import kotlinx.serialization.Serializable

@Serializable
data class MusicShelfRenderer(
    val title: Runs?,
    val contents: List<Content>?,
    val bottomEndpoint: NavigationEndpoint?,
    val moreContentButton: Button?,
    val continuations: List<Continuation>?,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
        val musicMultiRowListItemRenderer: MusicMultiRowListItemRenderer?,
        val continuationItemRenderer:
            YouTubeDataPage.Contents.TwoColumnWatchNextResults.Results.Results.Content.ItemSectionRenderer.Content.ContinuationItemRenderer?,
    ) {
        @Serializable
        data class MusicMultiRowListItemRenderer(
            val description: Description?,
            val subtitle: Subtitle?,
            val playbackProgress: PlaybackProgress?,
            val title: Title?,
            val thumbnail: Thumbnail?,
            val onTap: OnTap?,
        ) {
            @Serializable
            data class PlaybackProgress(
                val musicPlaybackProgressRenderer: MusicPlaybackProgressRenderer?,
            ) {
                @Serializable
                data class MusicPlaybackProgressRenderer(
                    val durationText: DurationText?,
                ) {
                    @Serializable
                    data class DurationText(
                        val runs: List<Run>?,
                    )
                }
            }

            @Serializable
            data class Description(
                val runs: List<Run>?,
            )

            @Serializable
            data class Subtitle(
                val runs: List<Run>?,
            )

            @Serializable
            data class Title(
                val runs: List<Run>?,
            )

            @Serializable
            data class Thumbnail(
                val musicThumbnailRenderer: ThumbnailRenderer.MusicThumbnailRenderer?,
            )

            @Serializable
            data class OnTap(
                val watchEndpoint: WatchEndpoint?,
            )
        }
    }
}

fun List<Continuation>.getContinuation() = firstOrNull()?.nextContinuationData?.continuation