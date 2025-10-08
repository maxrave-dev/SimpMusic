package com.maxrave.kotlinytmusicscraper.models

import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_AUDIOBOOK
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_PLAYLIST
import kotlinx.serialization.Serializable

/**
 * Two row: a big thumbnail, a title, and a subtitle
 * Used in [GridRenderer] and [MusicCarouselShelfRenderer]
 * Item type: song, video, album, playlist, artist
 */
@Serializable
data class MusicTwoRowItemRenderer(
    val title: Runs,
    val subtitle: Runs?,
    val subtitleBadges: List<Badges>? = null,
    val menu: Menu?,
    val thumbnailRenderer: ThumbnailRenderer,
    val navigationEndpoint: NavigationEndpoint,
    val thumbnailOverlay: MusicResponsiveListItemRenderer.Overlay?,
    val aspectRatio: String? = null,
) {
    val isSong: Boolean
        get() =
            navigationEndpoint?.endpoint is WatchEndpoint &&
                (
                    if (aspectRatio != null) {
                        aspectRatio != "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_RECTANGLE_16_9"
                    } else {
                        val thumbnail =
                            thumbnailRenderer
                                ?.musicThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.firstOrNull()
                        thumbnail != null && thumbnail.height == thumbnail.width
                    }
                )
    val isPlaylist: Boolean
        get() =
            navigationEndpoint
                ?.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType ==
                MUSIC_PAGE_TYPE_PLAYLIST &&
                aspectRatio != "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_RECTANGLE_16_9"
    val isAlbum: Boolean
        get() =
            navigationEndpoint
                ?.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType ==
                MUSIC_PAGE_TYPE_ALBUM ||
                navigationEndpoint
                    ?.browseEndpoint
                    ?.browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig
                    ?.pageType ==
                MUSIC_PAGE_TYPE_AUDIOBOOK
    val isArtist: Boolean
        get() =
            navigationEndpoint
                ?.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType ==
                MUSIC_PAGE_TYPE_ARTIST
    val isVideo: Boolean
        get() =
            navigationEndpoint?.endpoint is WatchEndpoint &&
                (
                    if (aspectRatio != null) {
                        aspectRatio == "MUSIC_TWO_ROW_ITEM_THUMBNAIL_ASPECT_RATIO_RECTANGLE_16_9"
                    } else {
                        val thumbnail =
                            thumbnailRenderer
                                ?.musicThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.firstOrNull()
                        thumbnail != null && thumbnail.height != thumbnail.width
                    }
                )
}