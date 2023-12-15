package com.maxrave.simpmusic.data.parser

import android.content.Context
import android.util.Log
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import com.maxrave.kotlinytmusicscraper.models.response.SearchResponse
import com.maxrave.kotlinytmusicscraper.pages.PodcastItem
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.playlist.Author
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.podcast.PodcastBrowse
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

fun parsePlaylistData(
    header: Any?,
    listContent: List<MusicShelfRenderer.Content>,
    playlistId: String,
    context: Context
): PlaylistBrowse? {
    if (header != null) {
        var title = ""
        val listAuthor: ArrayList<Author> = arrayListOf()
        var duration = ""
        var description = ""
        val listThumbnails: ArrayList<Thumbnail> = arrayListOf()
        var year = ""
        if (header is BrowseResponse.Header.MusicDetailHeaderRenderer) {
            title += header.title.runs?.get(0)?.text
            Log.d("PlaylistParser", "title: $title")
            if (!header.subtitle.runs.isNullOrEmpty() && header.subtitle.runs?.size!! > 2) {
                val author = Author(id = header.subtitle.runs?.get(2)?.navigationEndpoint?.browseEndpoint?.browseId ?: "", name = header.subtitle.runs?.get(2)?.text ?: "")
                listAuthor.add(author)
                Log.d("PlaylistParser", "author: $author")
            }
            if (!header.secondSubtitle.runs.isNullOrEmpty() && header.secondSubtitle.runs?.size!! > 2) {
                duration += header.secondSubtitle.runs?.get(2)?.text
            }
            Log.d("PlaylistParser", "duration: $duration")
            if (!header.description?.runs.isNullOrEmpty()) {
                for (run in header.description?.runs!!) {
                    description += (run.text)
                }
            }
            if (!header.subtitle.runs.isNullOrEmpty() && header.subtitle.runs?.size!! > 4) {
                year += header.subtitle.runs?.get(4)?.text
            }
            header.thumbnail.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail()
                ?.let { listThumbnails.addAll(it) }
        }
        else if (header is BrowseResponse.Header.MusicEditablePlaylistDetailHeaderRenderer?) {
            title += header.header.musicDetailHeaderRenderer.title.runs?.get(0)?.text
            Log.d("PlaylistParser", "title: $title")
            val author = Author(id = header.header.musicDetailHeaderRenderer.subtitle.runs?.get(2)?.navigationEndpoint?.browseEndpoint?.browseId ?: "", name = header.header.musicDetailHeaderRenderer.subtitle.runs?.get(2)?.text ?: "")
            listAuthor.add(author)
            Log.d("PlaylistParser", "author: $author")
            if (header.header.musicDetailHeaderRenderer.secondSubtitle.runs?.size!! > 4) {
                duration += header.header.musicDetailHeaderRenderer.secondSubtitle.runs?.get(4)?.text
            }
            else if (header.header.musicDetailHeaderRenderer.secondSubtitle.runs?.size!! == 3) {
                duration += header.header.musicDetailHeaderRenderer.secondSubtitle.runs?.get(2)?.text
            }
            Log.d("PlaylistParser", "duration: $duration")
            if (!header.header.musicDetailHeaderRenderer.description?.runs.isNullOrEmpty()) {
                for (run in header.header.musicDetailHeaderRenderer.description?.runs!!) {
                    description += (run.text)
                }
            }
            header.header.musicDetailHeaderRenderer.thumbnail.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail()
                ?.let { listThumbnails.addAll(it) }
        }
        Log.d("PlaylistParser", "description: $description")
        val listTrack: MutableList<Track> = arrayListOf()
        for (content in listContent){
            val track = Track(
                album = null,
                artists = content.musicResponsiveListItemRenderer?.let {
                    parseSongArtists(
                        it,
                        1,
                        context
                    )
                }
                    ?: listOf(),
                duration = content.musicResponsiveListItemRenderer?.fixedColumns?.get(0)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.get(
                    0
                )?.text ?: "",
                durationSeconds = content.musicResponsiveListItemRenderer?.fixedColumns?.get(0)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.get(
                    0
                )?.text?.split(":")?.let { it[0].toInt() * 60 + it[1].toInt() } ?: 0,
                isAvailable = false,
                isExplicit = false,
                likeStatus = "INDIFFERENT",
                thumbnails = content.musicResponsiveListItemRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail()
                    ?: listOf(),
                title = content.musicResponsiveListItemRenderer?.flexColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.get(
                    0
                )?.text ?: "",
                videoId = content.musicResponsiveListItemRenderer?.flexColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.get(
                    0
                )?.navigationEndpoint?.watchEndpoint?.videoId ?: "",
                videoType = "video",
                category = null,
                feedbackTokens = null,
                resultType = null,
                year = null
            )
            if (track.videoId != "") {
                listTrack.add(track)
            }
        }
        Log.d("PlaylistParser", "description: $description")
        return PlaylistBrowse(
            author = listAuthor.firstOrNull() ?: Author("", "YouTube Music"),
            description = description,
            duration = duration,
            durationSeconds = 0,
            id = playlistId,
            privacy = "PUBLIC",
            thumbnails =  listThumbnails,
            title = title,
            trackCount = listContent.size,
            tracks = listTrack,
            year = year
        )
    }
    else {
        return null
    }
}

fun parseSetVideoId(listContent: List<MusicShelfRenderer.Content>): ArrayList<SetVideoIdEntity> {
    val listSetVideoId: ArrayList<SetVideoIdEntity> = arrayListOf()
    for (content in listContent) {
        val videoId =
            content.musicResponsiveListItemRenderer?.flexColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.get(
                0
            )?.navigationEndpoint?.watchEndpoint?.videoId
        val setVideoId =
            content.musicResponsiveListItemRenderer?.menu?.menuRenderer?.items?.find { it.menuServiceItemRenderer?.icon?.iconType == "REMOVE_FROM_PLAYLIST" }?.menuServiceItemRenderer?.serviceEndpoint?.playlistEditEndpoint?.actions?.get(
                0
            )?.setVideoId
        if (videoId != null && setVideoId != null) {
            listSetVideoId.add(SetVideoIdEntity(videoId, setVideoId))
        } else {
            Log.d("PlaylistParser", "videoId or setVideoId is null")
        }
    }
    return listSetVideoId
}

fun parsePodcast(list: List<PodcastItem>): ArrayList<PlaylistsResult> {
    val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
    for (item in list) {
        listPlaylist.add(
            PlaylistsResult(
                author = item.author.name,
                browseId = item.id,
                category = "podcast",
                itemCount = "",
                resultType = "Podcast",
                thumbnails = item.thumbnail.thumbnails.toListThumbnail(),
                title = item.title
            )
        )
    }
    return listPlaylist
}

fun parsePodcastData(
    listContent: List<MusicShelfRenderer.Content>?,
    author: Artist?
): List<PodcastBrowse.EpisodeItem> {
    if (listContent == null || author == null) return emptyList()
    else {
        val listEpisode: ArrayList<PodcastBrowse.EpisodeItem> = arrayListOf()
        listContent.forEach { content ->
            listEpisode.add(
                PodcastBrowse.EpisodeItem(
                    title = content.musicMultiRowListItemRenderer?.title?.runs?.firstOrNull()?.text
                        ?: "",
                    author = author,
                    description = content.musicMultiRowListItemRenderer?.description?.runs?.joinToString(
                        separator = ""
                    ) { it.text } ?: "",
                    thumbnail = content.musicMultiRowListItemRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail()
                        ?: emptyList<Thumbnail>(),
                    createdDay = content.musicMultiRowListItemRenderer?.subtitle?.runs?.firstOrNull()?.text
                        ?: "",
                    durationString = content.musicMultiRowListItemRenderer?.subtitle?.runs?.getOrNull(
                        1
                    )?.text ?: "",
                    videoId = content.musicMultiRowListItemRenderer?.onTap?.watchEndpoint?.videoId
                        ?: ""
                )
            )
        }

        return listEpisode
    }
}

fun parsePodcastContinueData(
    listContent: List<SearchResponse.ContinuationContents.MusicShelfContinuation.Content>?,
    author: Artist?
): List<PodcastBrowse.EpisodeItem> {
    if (listContent == null || author == null) return emptyList()
    else {
        val listEpisode: ArrayList<PodcastBrowse.EpisodeItem> = arrayListOf()
        listContent.forEach { content ->
            listEpisode.add(
                PodcastBrowse.EpisodeItem(
                    title = content.musicMultiRowListItemRenderer?.title?.runs?.firstOrNull()?.text
                        ?: "",
                    author = author,
                    description = content.musicMultiRowListItemRenderer?.description?.runs?.joinToString(
                        separator = ""
                    ) { it.text } ?: "",
                    thumbnail = content.musicMultiRowListItemRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail()
                        ?: emptyList<Thumbnail>(),
                    createdDay = content.musicMultiRowListItemRenderer?.subtitle?.runs?.firstOrNull()?.text
                        ?: "",
                    durationString = content.musicMultiRowListItemRenderer?.subtitle?.runs?.lastOrNull()?.text
                        ?: "",
                    videoId = content.musicMultiRowListItemRenderer?.onTap?.watchEndpoint?.videoId
                        ?: ""
                )
            )
        }

        return listEpisode
    }
}