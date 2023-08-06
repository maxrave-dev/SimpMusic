package com.maxrave.kotlinytmusicscraper.pages

import com.maxrave.kotlinytmusicscraper.models.Album
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.Artist
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.MusicCardShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.YTItem
import com.maxrave.kotlinytmusicscraper.models.oddElements
import com.maxrave.kotlinytmusicscraper.models.splitBySeparator
import com.maxrave.kotlinytmusicscraper.utils.parseTime

data class SearchSummary(
    val title: String,
    val items: List<YTItem>,
)

data class SearchSummaryPage(
    val summaries: List<SearchSummary>,
) {
    companion object {
        fun fromMusicCardShelfRenderer(renderer: MusicCardShelfRenderer): YTItem? {
            val subtitle = renderer.subtitle.runs?.splitBySeparator()
            return when {
                renderer.onTap.watchEndpoint != null -> {
                    SongItem(
                        id = renderer.onTap.watchEndpoint.videoId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = subtitle?.getOrNull(1)?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        } ?: return null,
                        album = subtitle.getOrNull(2)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId!!
                            )
                        },
                        duration = subtitle.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer.icon.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
                }

                renderer.onTap.browseEndpoint?.isArtistEndpoint == true -> {
                    ArtistItem(
                        id = renderer.onTap.browseEndpoint.browseId,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        shuffleEndpoint = renderer.buttons
                            .find { it.buttonRenderer.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.buttonRenderer?.command?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = renderer.buttons
                            .find { it.buttonRenderer.icon?.iconType == "MIX" }
                            ?.buttonRenderer?.command?.watchPlaylistEndpoint ?: return null,
                    )
                }

                renderer.onTap.browseEndpoint?.isAlbumEndpoint == true -> {
                    AlbumItem(
                        browseId = renderer.onTap.browseEndpoint.browseId,
                        playlistId = renderer.buttons.firstOrNull()?.buttonRenderer?.command?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = subtitle?.getOrNull(1)?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        } ?: return null,
                        year = null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer.icon.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
                }

                else -> null
            }
        }

        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): YTItem? {
            val secondaryLine = renderer.flexColumns.getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.splitBySeparator()
                ?: return null
            return when {
                renderer.isSong -> {
                    SongItem(
                        id = renderer.playlistItemData?.videoId ?: return null,
                        title = renderer.flexColumns.firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                            ?.firstOrNull()?.text ?: return null,
                        artists = secondaryLine.getOrNull(1)?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        } ?: return null,
                        album = secondaryLine.getOrNull(2)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId!!
                            )
                        },
                        duration = secondaryLine.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.badges?.find {
                            it.musicInlineBadgeRenderer.icon.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
                }

                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        title = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: return null,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = renderer.menu.menuRenderer.items
                            .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null
                    )
                }

                renderer.isAlbum -> {
                    AlbumItem(
                        browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.flexColumns.firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                            ?.firstOrNull()?.text ?: return null,
                        artists = secondaryLine.getOrNull(1)?.oddElements()?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        } ?: return null,
                        year = secondaryLine.getOrNull(2)?.firstOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.badges?.find {
                            it.musicInlineBadgeRenderer.icon.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
                }

                renderer.isPlaylist -> {
                    PlaylistItem(
                        id = renderer.navigationEndpoint?.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                        title = renderer.flexColumns.firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                            ?.firstOrNull()?.text ?: return null,
                        author = secondaryLine.getOrNull(1)?.firstOrNull()?.let {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        } ?: return null,
                        songCountText = renderer.flexColumns.getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                            ?.lastOrNull()?.text ?: return null,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        playEndpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = renderer.menu.menuRenderer.items
                            .find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null
                    )
                }

                else -> null
            }
        }
    }
}
