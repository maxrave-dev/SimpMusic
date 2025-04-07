package com.maxrave.kotlinytmusicscraper.pages

import com.maxrave.kotlinytmusicscraper.models.Album
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.Artist
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint
import com.maxrave.kotlinytmusicscraper.models.MusicCarouselShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.SectionListRenderer
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.models.YTItem
import com.maxrave.kotlinytmusicscraper.models.oddElements

data class ArtistSection(
    val title: String,
    val items: List<YTItem>,
    val moreEndpoint: BrowseEndpoint?,
)

data class ArtistPage(
    val artist: ArtistItem,
    val sections: List<ArtistSection>,
    val description: String?,
    val subscribers: String? = null,
    val view: String? = null,
) {
    companion object {
        fun fromSectionListRendererContent(content: SectionListRenderer.Content): ArtistSection? =
            when {
                content.musicShelfRenderer != null -> fromMusicShelfRenderer(content.musicShelfRenderer)
                content.musicCarouselShelfRenderer != null -> fromMusicCarouselShelfRenderer(content.musicCarouselShelfRenderer)
                else -> null
            }

        private fun fromMusicShelfRenderer(renderer: MusicShelfRenderer): ArtistSection? {
            return ArtistSection(
                title =
                    renderer.title
                        ?.runs
                        ?.firstOrNull()
                        ?.text ?: return null,
                items =
                    renderer.contents?.mapNotNull {
                        fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                    } ?: return null,
                moreEndpoint =
                    renderer.title.runs
                        .firstOrNull()
                        ?.navigationEndpoint
                        ?.browseEndpoint,
            )
        }

        private fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): ArtistSection? {
            return ArtistSection(
                title =
                    renderer.header
                        ?.musicCarouselShelfBasicHeaderRenderer
                        ?.title
                        ?.runs
                        ?.firstOrNull()
                        ?.text
                        ?: return null,
                items =
                    renderer.contents.mapNotNull {
                        it.musicTwoRowItemRenderer?.let { renderer ->
                            fromMusicTwoRowItemRenderer(renderer)
                        }
                    },
                moreEndpoint =
                    renderer.header.musicCarouselShelfBasicHeaderRenderer
                        .moreContentButton
                        ?.buttonRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint,
            )
        }

        private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer?): SongItem? {
            if (renderer == null) {
                return null
            } else {
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
                            .lastOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.let {
                                Album(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                                )
                            },
                    duration = null,
                    thumbnail =
                        renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                            ?: return null,
                    thumbnails = renderer.thumbnail.musicThumbnailRenderer.thumbnail,
                    explicit =
                        renderer.badges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null,
                )
            }
        }

        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isSong -> {
                    print("isSong")
                    SongItem(
                        id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists =
                            listOfNotNull(
                                renderer.subtitle?.runs?.firstOrNull()?.let {
                                    Artist(
                                        name = it.text,
                                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                    )
                                },
                            ),
                        album = null,
                        duration = null,
                        thumbnail =
                            renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                        thumbnails = renderer.thumbnailRenderer.musicThumbnailRenderer.thumbnail,
                        explicit =
                            renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                    )
                }

                renderer.isAlbum -> {
                    print("isAlbum")
                    AlbumItem(
                        browseId =
                            renderer.navigationEndpoint.browseEndpoint?.browseId
                                ?: return null,
                        playlistId =
                            renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint
                                ?.playlistId
                                ?: renderer.navigationEndpoint.browseEndpoint.browseId
                                    .removePrefix("VL"),
                        title =
                            renderer.title.runs
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
                            renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                        explicit =
                            renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null,
                        isSingle = renderer.subtitle?.runs?.size != 1,
                    )
                }

                renderer.isPlaylist -> {
                    print("isPlaylist")
                    // Playlist from YouTube Music
                    PlaylistItem(
                        id =
                            renderer.navigationEndpoint.browseEndpoint
                                ?.browseId
                                ?.removePrefix("VL")
                                ?: return null,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        author =
                            Artist(
                                name =
                                    renderer.subtitle
                                        ?.runs
                                        ?.lastOrNull()
                                        ?.text ?: return null,
                                id = null,
                            ),
                        songCountText = null,
                        thumbnail =
                            renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                        playEndpoint =
                            renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint ?: return null,
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

                renderer.isArtist -> {
                    print("isArtist")
                    ArtistItem(
                        id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        title =
                            renderer.title.runs
                                ?.lastOrNull()
                                ?.text ?: return null,
                        thumbnail =
                            renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
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
                        subscribers =
                            renderer.subtitle
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                    )
                }

                renderer.isVideo -> {
                    print("isVideo")
                    VideoItem(
                        id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                        title =
                            renderer.title.runs
                                ?.get(0)
                                ?.text ?: return null,
                        thumbnail =
                            renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                        endpoint = renderer.navigationEndpoint.watchEndpoint,
                        thumbnails = renderer.thumbnailRenderer.musicThumbnailRenderer.thumbnail,
                        artists =
                            renderer.subtitle?.runs?.let { list ->
                                val artist = mutableListOf<Artist>()
                                for (i in list.indices) {
                                    if (i % 2 == 0 && i != list.lastIndex) {
                                        artist.add(
                                            Artist(
                                                list[i].text,
                                                list[i].navigationEndpoint?.browseEndpoint?.browseId,
                                            ),
                                        )
                                    }
                                }
                                artist
                            } ?: listOf(),
                        album = null,
                        duration = null,
                        view =
                            renderer.subtitle
                                ?.runs
                                ?.lastOrNull()
                                ?.text,
                    )
                }

                else -> null
            }
        }
    }
}