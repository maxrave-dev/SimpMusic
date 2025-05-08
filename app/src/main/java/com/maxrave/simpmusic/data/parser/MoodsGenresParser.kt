package com.maxrave.simpmusic.data.parser

import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import com.maxrave.simpmusic.data.model.explore.mood.genre.GenreObject
import com.maxrave.simpmusic.data.model.explore.mood.genre.ItemsPlaylist
import com.maxrave.simpmusic.data.model.explore.mood.genre.ItemsSong
import com.maxrave.simpmusic.data.model.explore.mood.genre.Title
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.Content
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.Item
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.MoodsMomentObject
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist

fun parseMoodsMomentObject(data: BrowseResponse?): MoodsMomentObject? {
    if (data != null) {
        val title =
            data.header
                ?.musicHeaderRenderer
                ?.title
                ?.runs
                ?.get(0)
                ?.text ?: ""
        val items =
            data.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.get(0)
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
        val listItem: MutableList<Item> = mutableListOf()
        if (items != null) {
            for (item in items) {
                if (item.musicCarouselShelfRenderer != null) {
                    val contents = item.musicCarouselShelfRenderer?.contents
                    val header =
                        item.musicCarouselShelfRenderer
                            ?.header
                            ?.musicCarouselShelfBasicHeaderRenderer
                            ?.title
                            ?.runs
                            ?.get(
                                0,
                            )?.text
                    val listContent: MutableList<Content> = mutableListOf()
                    if (!contents.isNullOrEmpty()) {
                        for (content in contents) {
                            if (content.musicResponsiveListItemRenderer != null) {
                                // Song
                            } else if (content.musicTwoRowItemRenderer != null) {
                                // Playlist
                                val thumbnails =
                                    content.musicTwoRowItemRenderer
                                        ?.thumbnailRenderer
                                        ?.musicThumbnailRenderer
                                        ?.thumbnail
                                        ?.thumbnails
                                        ?.toListThumbnail()
                                var subtitle = ""
                                val runs = content.musicTwoRowItemRenderer?.subtitle?.runs
                                if (runs != null) {
                                    for (i in runs.indices) {
                                        subtitle += runs[i].text
                                    }
                                }
                                val contentTitle =
                                    content.musicTwoRowItemRenderer
                                        ?.title
                                        ?.runs
                                        ?.get(0)
                                        ?.text
                                val playlistBrowseId =
                                    content.musicTwoRowItemRenderer
                                        ?.navigationEndpoint
                                        ?.browseEndpoint
                                        ?.browseId
                                listContent.add(
                                    Content(
                                        playlistBrowseId = playlistBrowseId ?: "",
                                        subtitle = subtitle,
                                        thumbnails = thumbnails ?: listOf(),
                                        title = contentTitle ?: "",
                                    ),
                                )
                            }
                        }
                    }
                    listItem.add(Item(contents = listContent, header = header ?: ""))
                } else if (item.gridRenderer != null) {
                    val contents = item.gridRenderer?.items
                    val header =
                        item.gridRenderer
                            ?.header
                            ?.gridHeaderRenderer
                            ?.title
                            ?.runs
                            ?.get(0)
                            ?.text
                    val listContent: MutableList<Content> = mutableListOf()
                    if (!contents.isNullOrEmpty()) {
                        for (content in contents) {
                            if (content.musicTwoRowItemRenderer != null) {
                                // Playlist
                                val thumbnails =
                                    content.musicTwoRowItemRenderer
                                        ?.thumbnailRenderer
                                        ?.musicThumbnailRenderer
                                        ?.thumbnail
                                        ?.thumbnails
                                        ?.toListThumbnail()
                                var subtitle = ""
                                val runs = content.musicTwoRowItemRenderer?.subtitle?.runs
                                if (runs != null) {
                                    for (i in runs.indices) {
                                        subtitle += runs[i].text
                                    }
                                }
                                val contentTitle =
                                    content.musicTwoRowItemRenderer
                                        ?.title
                                        ?.runs
                                        ?.get(0)
                                        ?.text
                                val playlistBrowseId =
                                    content.musicTwoRowItemRenderer
                                        ?.navigationEndpoint
                                        ?.browseEndpoint
                                        ?.browseId
                                listContent.add(
                                    Content(
                                        playlistBrowseId = playlistBrowseId ?: "",
                                        subtitle = subtitle,
                                        thumbnails = thumbnails ?: listOf(),
                                        title = contentTitle ?: "",
                                    ),
                                )
                            }
                        }
                    }
                    listItem.add(Item(contents = listContent, header = header ?: ""))
                }
            }
        }
        return MoodsMomentObject(endpoint = "FEmusic_moods_and_genres_category", header = title, items = listItem, params = "")
    } else {
        return null
    }
}

fun parseGenreObject(data: BrowseResponse?): GenreObject? {
    if (data != null) {
        val title =
            data.header
                ?.musicHeaderRenderer
                ?.title
                ?.runs
                ?.get(0)
                ?.text ?: ""
        val items =
            data.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.get(0)
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
        val listItemsPlaylist: MutableList<ItemsPlaylist> = mutableListOf()
        val listItemsSong: MutableList<ItemsSong> = mutableListOf()
        if (items != null) {
            for (item in items) {
                if (item.musicCarouselShelfRenderer != null) {
                    val contents = item.musicCarouselShelfRenderer?.contents
                    val header =
                        item.musicCarouselShelfRenderer
                            ?.header
                            ?.musicCarouselShelfBasicHeaderRenderer
                            ?.title
                            ?.runs
                            ?.get(
                                0,
                            )?.text
                    val listContent: MutableList<com.maxrave.simpmusic.data.model.explore.mood.genre.Content> = mutableListOf()
                    if (!contents.isNullOrEmpty()) {
                        for (content in contents) {
                            if (content.musicResponsiveListItemRenderer != null) {
                                // Song
                                val songName =
                                    content.musicResponsiveListItemRenderer
                                        ?.flexColumns
                                        ?.get(
                                            0,
                                        )?.musicResponsiveListItemFlexColumnRenderer
                                        ?.text
                                        ?.runs
                                        ?.get(0)
                                        ?.text
                                val songArtist =
                                    content.musicResponsiveListItemRenderer
                                        ?.flexColumns
                                        ?.get(
                                            1,
                                        )?.musicResponsiveListItemFlexColumnRenderer
                                        ?.text
                                        ?.runs
                                        ?.get(0)
                                        ?.text
                                val videoId =
                                    content.musicResponsiveListItemRenderer
                                        ?.flexColumns
                                        ?.get(
                                            0,
                                        )?.musicResponsiveListItemFlexColumnRenderer
                                        ?.text
                                        ?.runs
                                        ?.get(0)
                                        ?.navigationEndpoint
                                        ?.watchEndpoint
                                        ?.videoId
                                val thumbnails =
                                    content.musicResponsiveListItemRenderer
                                        ?.thumbnail
                                        ?.musicThumbnailRenderer
                                        ?.thumbnail
                                        ?.thumbnails
                                        ?.toListThumbnail()
                                listItemsSong.add(
                                    ItemsSong(
                                        title = songName ?: "",
                                        artist = listOf(Artist(id = null, name = songArtist ?: "")),
                                        videoId = videoId ?: "",
                                    ),
                                )
                            } else if (content.musicTwoRowItemRenderer != null) {
                                // Playlist
                                val thumbnails =
                                    content.musicTwoRowItemRenderer
                                        ?.thumbnailRenderer
                                        ?.musicThumbnailRenderer
                                        ?.thumbnail
                                        ?.thumbnails
                                        ?.toListThumbnail()
                                var subtitle = ""
                                val runs = content.musicTwoRowItemRenderer?.subtitle?.runs
                                if (runs != null) {
                                    for (i in runs.indices) {
                                        subtitle += runs[i].text
                                    }
                                }
                                val contentTitle =
                                    content.musicTwoRowItemRenderer
                                        ?.title
                                        ?.runs
                                        ?.get(0)
                                        ?.text
                                val playlistBrowseId =
                                    content.musicTwoRowItemRenderer
                                        ?.navigationEndpoint
                                        ?.browseEndpoint
                                        ?.browseId
                                listContent.add(
                                    com.maxrave.simpmusic.data.model.explore.mood.genre.Content(
                                        playlistBrowseId = playlistBrowseId ?: "",
                                        thumbnail = thumbnails ?: listOf(),
                                        title =
                                            Title(
                                                subtitle = subtitle,
                                                title = contentTitle ?: "",
                                            ),
                                    ),
                                )
                            }
                        }
                    }
                    listItemsPlaylist.add(ItemsPlaylist(contents = listContent, header = header ?: "", type = "playlist"))
                } else if (item.gridRenderer != null) {
                    val contents = item.gridRenderer?.items
                    val header =
                        item.gridRenderer
                            ?.header
                            ?.gridHeaderRenderer
                            ?.title
                            ?.runs
                            ?.get(0)
                            ?.text
                    val listContent: MutableList<com.maxrave.simpmusic.data.model.explore.mood.genre.Content> = mutableListOf()
                    if (!contents.isNullOrEmpty()) {
                        for (content in contents) {
                            if (content.musicTwoRowItemRenderer != null) {
                                // Playlist
                                val thumbnails =
                                    content.musicTwoRowItemRenderer
                                        ?.thumbnailRenderer
                                        ?.musicThumbnailRenderer
                                        ?.thumbnail
                                        ?.thumbnails
                                        ?.toListThumbnail()
                                var subtitle = ""
                                val runs = content.musicTwoRowItemRenderer?.subtitle?.runs
                                if (runs != null) {
                                    for (i in runs.indices) {
                                        subtitle += runs[i].text
                                    }
                                }
                                val contentTitle =
                                    content.musicTwoRowItemRenderer
                                        ?.title
                                        ?.runs
                                        ?.get(0)
                                        ?.text
                                val playlistBrowseId =
                                    content.musicTwoRowItemRenderer
                                        ?.navigationEndpoint
                                        ?.browseEndpoint
                                        ?.browseId
                                listContent.add(
                                    com.maxrave.simpmusic.data.model.explore.mood.genre.Content(
                                        playlistBrowseId = playlistBrowseId ?: "",
                                        thumbnail = thumbnails ?: listOf(),
                                        title =
                                            Title(
                                                subtitle = subtitle,
                                                title = contentTitle ?: "",
                                            ),
                                    ),
                                )
                            }
                        }
                    }
                    listItemsPlaylist.add(ItemsPlaylist(contents = listContent, header = header ?: "", type = "playlist"))
                }
            }
        }
        return GenreObject(header = title, itemsPlaylist = listItemsPlaylist, itemsSong = listItemsSong)
    } else {
        return null
    }
}