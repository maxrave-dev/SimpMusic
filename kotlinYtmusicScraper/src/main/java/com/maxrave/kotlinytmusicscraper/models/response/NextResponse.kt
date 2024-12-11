package com.maxrave.kotlinytmusicscraper.models.response

import com.maxrave.kotlinytmusicscraper.models.NavigationEndpoint
import com.maxrave.kotlinytmusicscraper.models.PlaylistPanelRenderer
import com.maxrave.kotlinytmusicscraper.models.Tabs
import com.maxrave.kotlinytmusicscraper.models.youtube.data.YouTubeDataPage
import kotlinx.serialization.Serializable

@Serializable
data class NextResponse(
    val contents: Contents,
    val continuationContents: ContinuationContents?,
    val currentVideoEndpoint: NavigationEndpoint?,
) {
    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?,
        val twoColumnWatchNextResults: YouTubeDataPage.Contents.TwoColumnWatchNextResults?,
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer,
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer,
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tabs.Tab>,
                )
            }
        }
    }

    @Serializable
    data class ContinuationContents(
        val playlistPanelContinuation: PlaylistPanelRenderer?,
        val sectionListContinuation: BrowseResponse.ContinuationContents.SectionListContinuation?,
        val musicPlaylistShelfContinuation: BrowseResponse.ContinuationContents.MusicPlaylistShelfContinuation?,
    )
}

@Serializable
data class NextAndroidMusicResponse(
    val playerOverlays: PlayerOverlays?,
) {
    @Serializable
    data class PlayerOverlays(
        val playerOverlayRenderer: PlayerOverlayRenderer?,
    ) {
        @Serializable
        data class PlayerOverlayRenderer(
            val actions: List<Action>?,
        ) {
            @Serializable
            data class Action(
                val likeButtonRenderer: LikeButtonRenderer?,
            ) {
                @Serializable
                data class LikeButtonRenderer(
                    val likeStatus: String?,
                    val likesAllowed: Boolean?,
                )
            }
        }
    }
}