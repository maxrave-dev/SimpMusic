package com.maxrave.simpmusic.data.parser

import android.util.Log
import com.maxrave.kotlinytmusicscraper.models.MusicCarouselShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.SectionListRenderer
import com.maxrave.kotlinytmusicscraper.models.oddElements
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.home.chart.Artists
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.model.home.chart.ItemArtist
import com.maxrave.simpmusic.data.model.home.chart.ItemVideo
import com.maxrave.simpmusic.data.model.home.chart.Videos
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist

fun parseChart(data: SectionListRenderer?): Chart? {
    if (data?.contents != null) {
        val listTrendingItem: ArrayList<Track> = arrayListOf()
        val listSongItem: ArrayList<Track> = arrayListOf()
        val listVideoItem: ArrayList<ItemVideo> = arrayListOf()
        val listArtistItem: ArrayList<ItemArtist> = arrayListOf()
        var videoPlaylistId = ""
        for (section in data.contents!!) {
            if (section.musicCarouselShelfRenderer != null) {
                val musicCarouselShelfRenderer = section.musicCarouselShelfRenderer
                val pageType =
                    musicCarouselShelfRenderer
                        ?.header
                        ?.musicCarouselShelfBasicHeaderRenderer
                        ?.title
                        ?.runs
                        ?.get(
                            0,
                        )?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseEndpointContextSupportedConfigs
                        ?.browseEndpointContextMusicConfig
                        ?.pageType
                if (pageType == "MUSIC_PAGE_TYPE_PLAYLIST" && musicCarouselShelfRenderer.numItemsPerColumn == null) {
                    videoPlaylistId =
                        musicCarouselShelfRenderer.header
                            ?.musicCarouselShelfBasicHeaderRenderer
                            ?.title
                            ?.runs
                            ?.get(
                                0,
                            )?.navigationEndpoint
                            ?.browseEndpoint
                            ?.browseId ?: ""
                    val contents = musicCarouselShelfRenderer.contents
                    listVideoItem.addAll(parseSongChart(contents))
                } else if (pageType == "MUSIC_PAGE_TYPE_PLAYLIST" && musicCarouselShelfRenderer.numItemsPerColumn == "4") {
                    val contents = musicCarouselShelfRenderer.contents
                    contents.forEachIndexed { index, content ->
                        val musicResponsiveListItemRenderer =
                            content.musicResponsiveListItemRenderer
                        if (musicResponsiveListItemRenderer != null) {
                            val thumb =
                                musicResponsiveListItemRenderer.thumbnail
                                    ?.musicThumbnailRenderer
                                    ?.thumbnail
                                    ?.thumbnails
                            val firstThumb = thumb?.firstOrNull()
                            if (firstThumb != null && (firstThumb.width == firstThumb.height && firstThumb.width != null)) {
                                val song =
                                    Track(
                                        album =
                                            musicResponsiveListItemRenderer.flexColumns
                                                .getOrNull(
                                                    2,
                                                )?.musicResponsiveListItemFlexColumnRenderer
                                                ?.text
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.let {
                                                    Album(
                                                        name = it.text,
                                                        id =
                                                            it.navigationEndpoint?.browseEndpoint?.browseId
                                                                ?: return null,
                                                    )
                                                },
                                        artists =
                                            musicResponsiveListItemRenderer.flexColumns
                                                .getOrNull(
                                                    1,
                                                )?.musicResponsiveListItemFlexColumnRenderer
                                                ?.text
                                                ?.runs
                                                ?.oddElements()
                                                ?.map {
                                                    Artist(
                                                        name = it.text,
                                                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                                    )
                                                } ?: return null,
                                        duration = null,
                                        durationSeconds = null,
                                        isAvailable = false,
                                        isExplicit = false,
                                        likeStatus = "INDIFFERENT",
                                        thumbnails = thumb.toListThumbnail(),
                                        title =
                                            musicResponsiveListItemRenderer.flexColumns
                                                .firstOrNull()
                                                ?.musicResponsiveListItemFlexColumnRenderer
                                                ?.text
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.text ?: return null,
                                        videoId =
                                            musicResponsiveListItemRenderer.playlistItemData?.videoId
                                                ?: return null,
                                        videoType = null,
                                        category = null,
                                        feedbackTokens = null,
                                        resultType = null,
                                        year = null,
                                    )
                                listSongItem.add(song)
                            } else {
                                val song =
                                    Track(
                                        album =
                                            musicResponsiveListItemRenderer.flexColumns
                                                .getOrNull(
                                                    2,
                                                )?.musicResponsiveListItemFlexColumnRenderer
                                                ?.text
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.let {
                                                    Album(
                                                        name = it.text,
                                                        id =
                                                            it.navigationEndpoint?.browseEndpoint?.browseId
                                                                ?: return null,
                                                    )
                                                },
                                        artists =
                                            musicResponsiveListItemRenderer.flexColumns
                                                .getOrNull(
                                                    1,
                                                )?.musicResponsiveListItemFlexColumnRenderer
                                                ?.text
                                                ?.runs
                                                ?.oddElements()
                                                ?.map {
                                                    Artist(
                                                        name = it.text,
                                                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                                    )
                                                }?.toMutableList()
                                                ?.apply {
                                                    runCatching { removeAt(this.lastIndex) }
                                                        .onSuccess {
                                                            Log.i("parse_mixed_content", "Removed last artist")
                                                        }.onFailure {
                                                            Log.e("parse_mixed_content", "Failed to remove last artist")
                                                            it.printStackTrace()
                                                        }
                                                } ?: return null,
                                        duration = null,
                                        durationSeconds = null,
                                        isAvailable = false,
                                        isExplicit = false,
                                        likeStatus = "INDIFFERENT",
                                        thumbnails = thumb?.toListThumbnail(),
                                        title =
                                            musicResponsiveListItemRenderer.flexColumns
                                                .firstOrNull()
                                                ?.musicResponsiveListItemFlexColumnRenderer
                                                ?.text
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.text ?: return null,
                                        videoId =
                                            musicResponsiveListItemRenderer.playlistItemData?.videoId
                                                ?: return null,
                                        videoType = null,
                                        category = null,
                                        feedbackTokens = null,
                                        resultType = null,
                                        year = null,
                                    )
                                listTrendingItem.add(song)
                            }
                        }
                    }
                } else {
                    val contents = musicCarouselShelfRenderer?.contents
                    parseArtistChart(contents)?.let { listArtistItem.addAll(it) }
                }
            }
        }
        Log.w("parseChart", "listSongItem: $listSongItem")
        Log.w("parseChart", "listVideoItem: $listVideoItem")
        Log.w("parseChart", "listArtistItem: $listArtistItem")
        Log.w("parseChart", "listTrendingItem: $listTrendingItem")

        return Chart(
            artists = Artists(itemArtists = listArtistItem, playlist = ""),
            countries = null,
            videos = Videos(items = listVideoItem, playlist = videoPlaylistId),
            songs = listSongItem,
            trending = listTrendingItem,
        )
    } else {
        return null
    }
}

fun parseSongChart(contents: List<MusicCarouselShelfRenderer.Content>): ArrayList<ItemVideo> {
    val listVideoItem: ArrayList<ItemVideo> = arrayListOf()
    for (content in contents) {
        val title =
            content.musicTwoRowItemRenderer
                ?.title
                ?.runs
                ?.get(0)
                ?.text
        val runs = content.musicTwoRowItemRenderer?.subtitle?.runs
        var view = ""
        val artists: ArrayList<Artist> = arrayListOf()
        val albums: ArrayList<Album> = arrayListOf()
        if (runs != null) {
            for (i in runs.indices) {
                if (i.rem(2) == 0) {
                    if (i == runs.size - 1) {
                        view += runs[i].text
                    } else {
                        val name = runs[i].text
                        val id = runs[i].navigationEndpoint?.browseEndpoint?.browseId
                        if (id != null) {
                            if (id.startsWith("MPRE")) {
                                albums.add(Album(id = id, name = name))
                            } else {
                                artists.add(Artist(name = name, id = id))
                            }
                        }
                    }
                }
            }
        }
        val thumbnails =
            content.musicTwoRowItemRenderer
                ?.thumbnailRenderer
                ?.musicThumbnailRenderer
                ?.thumbnail
                ?.thumbnails
        val videoId =
            content.musicTwoRowItemRenderer
                ?.navigationEndpoint
                ?.watchEndpoint
                ?.videoId
        listVideoItem.add(
            ItemVideo(
                artists = artists,
                playlistId = "",
                thumbnails = thumbnails?.toListThumbnail() ?: listOf(),
                title = title ?: "",
                videoId = videoId ?: "",
                views = view,
            ),
        )
    }
    return listVideoItem
}

fun parseArtistChart(contents: List<MusicCarouselShelfRenderer.Content>?): ArrayList<ItemArtist>? =
    if (contents != null) {
        val artists: ArrayList<ItemArtist> = arrayListOf()
        for (i in contents.indices) {
            val content = contents[i]
            if (content.musicResponsiveListItemRenderer != null) {
                val title =
                    content.musicResponsiveListItemRenderer
                        ?.flexColumns
                        ?.get(0)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.get(
                            0,
                        )?.text
                val subscriber =
                    content.musicResponsiveListItemRenderer
                        ?.flexColumns
                        ?.get(1)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.get(
                            0,
                        )?.text
                val thumbnails =
                    content.musicResponsiveListItemRenderer
                        ?.thumbnail
                        ?.musicThumbnailRenderer
                        ?.thumbnail
                        ?.thumbnails
                val artistId =
                    content.musicResponsiveListItemRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseId
                artists.add(
                    ItemArtist(
                        browseId = artistId ?: "",
                        rank = "${i + 1}",
                        subscribers = subscriber ?: "",
                        thumbnails = thumbnails?.toListThumbnail() ?: listOf(),
                        title = title ?: "",
                        trend = "",
                    ),
                )
            }
        }
        artists
    } else {
        null
    }