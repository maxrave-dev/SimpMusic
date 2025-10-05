@file:OptIn(ExperimentalSerializationApi::class)

package com.maxrave.kotlinytmusicscraper.models

import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_AUDIOBOOK
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_PLAYLIST
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Typical list item
 * Used in [MusicCarouselShelfRenderer], [MusicShelfRenderer]
 * Appears in quick picks, search results, table items, etc.
 */
@Serializable
data class MusicResponsiveListItemRenderer(
    val badges: List<Badges>?,
    val fixedColumns: List<FlexColumn>?,
    val flexColumns: List<FlexColumn>,
    val thumbnail: ThumbnailRenderer?,
    val menu: Menu?,
    val playlistItemData: PlaylistItemData?,
    val overlay: Overlay?,
    val navigationEndpoint: NavigationEndpoint?,
) {
    val isSong: Boolean
        get() = navigationEndpoint == null || navigationEndpoint.watchEndpoint != null || navigationEndpoint.watchPlaylistEndpoint != null
    val isVideo: Boolean
        get() =
            navigationEndpoint
                ?.watchEndpoint
                ?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig
                ?.musicVideoType != null
    val isPlaylist: Boolean
        get() =
            navigationEndpoint
                ?.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType == MUSIC_PAGE_TYPE_PLAYLIST
    val isAlbum: Boolean
        get() =
            navigationEndpoint
                ?.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType == MUSIC_PAGE_TYPE_ALBUM ||
                navigationEndpoint
                    ?.browseEndpoint
                    ?.browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig
                    ?.pageType == MUSIC_PAGE_TYPE_AUDIOBOOK
    val isArtist: Boolean
        get() =
            navigationEndpoint
                ?.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType == MUSIC_PAGE_TYPE_ARTIST

    @Serializable
    data class FlexColumn(
        @JsonNames("musicResponsiveListItemFixedColumnRenderer")
        val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer,
    ) {
        @Serializable
        data class MusicResponsiveListItemFlexColumnRenderer(
            val text: Runs?,
        ) {
            fun toAlbum(): Album? {
                val run = text?.runs?.firstOrNull()
                if (run != null && isAlbum()) {
                    return Album(
                        name = run.text,
                        id = run.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                    )
                }
                return null
            }

            fun toArtist(): Artist? {
                val run = text?.runs?.firstOrNull()
                if (run != null && isArtist()) {
                    return Artist(
                        name = run.text,
                        id = run.navigationEndpoint?.browseEndpoint?.browseId ?: "",
                    )
                }
                return null
            }

            fun isAlbum(): Boolean =
                text
                    ?.runs
                    ?.firstOrNull()
                    ?.navigationEndpoint
                    ?.browseEndpoint
                    ?.isAlbumEndpoint == true

            fun isArtist(): Boolean =
                text
                    ?.runs
                    ?.firstOrNull()
                    ?.navigationEndpoint
                    ?.browseEndpoint
                    ?.isArtistEndpoint == true ||
                    (
                        text
                            ?.runs
                            ?.firstOrNull()
                            ?.navigationEndpoint
                            ?.watchEndpoint == null &&
                            text
                                ?.runs
                                ?.firstOrNull()
                                ?.navigationEndpoint
                                ?.browseEndpoint == null
                    )
        }
    }

    @Serializable
    data class PlaylistItemData(
        val playlistSetVideoId: String?,
        val videoId: String,
    )

    @Serializable
    data class Overlay(
        val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer,
    ) {
        @Serializable
        data class MusicItemThumbnailOverlayRenderer(
            val content: Content,
        ) {
            @Serializable
            data class Content(
                val musicPlayButtonRenderer: MusicPlayButtonRenderer,
            ) {
                @Serializable
                data class MusicPlayButtonRenderer(
                    val playNavigationEndpoint: NavigationEndpoint?,
                )
            }
        }
    }
}