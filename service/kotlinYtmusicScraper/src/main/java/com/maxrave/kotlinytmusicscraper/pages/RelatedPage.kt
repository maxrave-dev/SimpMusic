package com.maxrave.kotlinytmusicscraper.pages

import com.maxrave.kotlinytmusicscraper.models.Album
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.Artist
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.models.YTItem
import com.maxrave.kotlinytmusicscraper.models.oddElements
import com.maxrave.kotlinytmusicscraper.models.splitBySeparator

data class RelatedPage(
    val songs: List<SongItem>,
    val albums: List<AlbumItem>,
    val artists: List<ArtistItem>,
    val playlists: List<PlaylistItem>,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            return SongItem(
                id = renderer.playlistItemData?.videoId ?: return null,
                title =
                    renderer.flexColumns
                        .firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.firstOrNull()
                        ?.text ?: return null,
                artists =
                    renderer.flexColumns
                        .getOrNull(1)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.oddElements()
                        ?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: return null,
                album =
                    renderer.flexColumns
                        .getOrNull(2)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.firstOrNull()
                        ?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                            )
                        },
                duration = null,
                thumbnail =
                    renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                        ?: return null,
                explicit =
                    renderer.badges?.find {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } != null,
            )
        }

        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isSong ->
                    SongItem(
                        id = renderer.navigationEndpoint?.watchEndpoint?.videoId ?: return null,
                        title =
                            renderer.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists =
                            renderer.subtitle
                                ?.runs
                                ?.splitBySeparator()
                                ?.filterNot {
                                    it.firstOrNull()?.text == "Song" &&
                                        it.firstOrNull()?.navigationEndpoint == null
                                }?.firstOrNull()
                                ?.oddElements()
                                ?.map {
                                    Artist(
                                        name = it.text,
                                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                    )
                                } ?: return null,
                        album = null,
                        duration = null,
                        thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        endpoint = renderer.navigationEndpoint.watchEndpoint,
                    )
                renderer.isVideo ->
                    VideoItem(
                        id = renderer.navigationEndpoint?.watchEndpoint?.videoId ?: return null,
                        title =
                            renderer.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists =
                            renderer.subtitle?.runs?.splitBySeparator()?.firstOrNull()?.oddElements()?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            } ?: return null,
                        album = null,
                        duration = null,
                        thumbnail = renderer.thumbnailRenderer?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        endpoint = renderer.navigationEndpoint.watchEndpoint,
                    )
                renderer.isAlbum ->
                    AlbumItem(
                        browseId =
                            renderer.navigationEndpoint?.browseEndpoint?.browseId
                                ?: return null,
                        playlistId =
                            renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint
                                ?.playlistId
                                ?: renderer.thumbnailOverlay
                                    ?.musicItemThumbnailOverlayRenderer
                                    ?.content
                                    ?.musicPlayButtonRenderer
                                    ?.playNavigationEndpoint
                                    ?.watchEndpoint
                                    ?.playlistId
                                ?: renderer.navigationEndpoint.browseEndpoint.browseId
                                    .removePrefix("VL"),
                        title =
                            renderer.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists = null,
                        year =
                            renderer.subtitle
                                ?.runs
                                ?.lastOrNull()
                                ?.text
                                ?.toIntOrNull(),
                        thumbnail =
                            renderer.thumbnailRenderer?.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                        explicit =
                            renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                    )

                renderer.isPlaylist ->
                    PlaylistItem(
                        id =
                            renderer.navigationEndpoint
                                ?.browseEndpoint
                                ?.browseId
                                ?.removePrefix("VL")
                                ?: return null,
                        title =
                            renderer.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        author =
                            renderer.subtitle?.runs?.getOrNull(2)?.let {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            },
                        songCountText =
                            renderer.subtitle
                                ?.runs
                                ?.getOrNull(4)
                                ?.text,
                        thumbnail =
                            renderer.thumbnailRenderer?.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                        playEndpoint =
                            renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
                        // If the playlist is radio, shuffle is not available
                        shuffleEndpoint =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint
                                ?: renderer.thumbnailOverlay
                                    .musicItemThumbnailOverlayRenderer.content
                                    .musicPlayButtonRenderer.playNavigationEndpoint
                                    .watchPlaylistEndpoint,
                        radioEndpoint =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint
                                ?: renderer.thumbnailOverlay
                                    .musicItemThumbnailOverlayRenderer.content
                                    .musicPlayButtonRenderer.playNavigationEndpoint
                                    .watchPlaylistEndpoint,
                    )

                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        title =
                            renderer.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        thumbnail =
                            renderer.thumbnailRenderer?.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                        shuffleEndpoint =
                            renderer.menu
                                ?.menuRenderer
                                ?.items
                                ?.find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint
                                ?: return null,
                        radioEndpoint =
                            renderer.menu.menuRenderer.items
                                .find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint
                                ?: return null,
                    )
                }

                else -> null
            }
        }
    }
}