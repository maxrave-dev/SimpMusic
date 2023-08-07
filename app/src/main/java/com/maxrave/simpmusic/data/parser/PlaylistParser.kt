package com.maxrave.simpmusic.data.parser

import android.util.Log
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.playlist.Author
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse

fun parsePlaylistData(header:  BrowseResponse.Header.MusicDetailHeaderRenderer?, listContent: List<MusicShelfRenderer.Content>, playlistId: String): PlaylistBrowse? {
    if (header != null){
        val title = header.title.runs?.get(0)?.text
        Log.d("PlaylistParser", "title: $title")
        val author = Author(id = header.subtitle.runs?.get(2)?.navigationEndpoint?.browseEndpoint?.browseId ?: "", name = header.subtitle.runs?.get(2)?.text ?: "")
        Log.d("PlaylistParser", "author: $author")
        val duration = header.secondSubtitle.runs?.get(2)?.text
        Log.d("PlaylistParser", "duration: $duration")
        var description = ""
        if (!header.description?.runs.isNullOrEmpty()) {
            for (run in header.description?.runs!!) {
                description += (run.text)
            }
        }
        Log.d("PlaylistParser", "description: $description")
        val listTrack: MutableList<Track> = arrayListOf()
        for (content in listContent){
            val track = Track(
                album = null,
                artists = parseSongArtists(content.musicResponsiveListItemRenderer, 1) ?: listOf(),
                duration = content.musicResponsiveListItemRenderer.fixedColumns?.get(0)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.get(0)?.text ?: "",
                durationSeconds = content.musicResponsiveListItemRenderer.fixedColumns?.get(0)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.get(0)?.text?.split(":")?.let { it[0].toInt() * 60 + it[1].toInt()} ?: 0,
                isAvailable = false,
                isExplicit = false,
                likeStatus = "INDIFFERENT",
                thumbnails = content.musicResponsiveListItemRenderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail() ?: listOf(),
                title = content.musicResponsiveListItemRenderer.flexColumns[0].musicResponsiveListItemFlexColumnRenderer.text?.runs?.get(0)?.text ?: "",
                videoId = content.musicResponsiveListItemRenderer.flexColumns[0].musicResponsiveListItemFlexColumnRenderer.text?.runs?.get(0)?.navigationEndpoint?.watchEndpoint?.videoId ?: "",
                videoType = "video",
                category = null,
                feedbackTokens = null,
                resultType = null,
                year = null
            )
            listTrack.add(track)
        }
        Log.d("PlaylistParser", "description: $description")
        return PlaylistBrowse(
            author = author,
            description = description,
            duration = duration ?: "",
            durationSeconds = 0,
            id = playlistId,
            privacy = "PUBLIC",
            thumbnails = header.thumbnail.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail() ?: listOf(),
            title = title ?: "",
            trackCount = listContent.size,
            tracks = listTrack,
            year = header.subtitle.runs?.get(4)?.text ?: ""
        )
    }
    else {
        return null
    }
}