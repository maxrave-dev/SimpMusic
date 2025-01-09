package com.maxrave.simpmusic.data.parser

import com.maxrave.kotlinytmusicscraper.models.PlaylistPanelRenderer
import com.maxrave.kotlinytmusicscraper.utils.parseTime
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist

fun parseRelated(data: List<PlaylistPanelRenderer.Content>?): ArrayList<Track>? {
    if (!data.isNullOrEmpty()) {
        val listTrack: ArrayList<Track> = arrayListOf()
        data.forEach { track ->
            val title =
                track.playlistPanelVideoRenderer
                    ?.title
                    ?.runs
                    ?.get(0)
                    ?.text
            val duration =
                track.playlistPanelVideoRenderer
                    ?.lengthText
                    ?.runs
                    ?.get(0)
                    ?.text
            val durationSeconds =
                track.playlistPanelVideoRenderer
                    ?.lengthText
                    ?.runs
                    ?.firstOrNull()
                    ?.text
                    ?.parseTime()
            val longByTextRuns = track.playlistPanelVideoRenderer?.longBylineText?.runs
            val artist: ArrayList<Artist> = arrayListOf()
            val album: ArrayList<Album> = arrayListOf()
            if (!longByTextRuns.isNullOrEmpty()) {
                for (i in longByTextRuns.indices) {
                    if (longByTextRuns[i].navigationEndpoint?.browseEndpoint?.browseId != null) {
                        if (longByTextRuns[i]
                                .navigationEndpoint
                                ?.browseEndpoint
                                ?.browseId
                                ?.startsWith("UC") == true
                        ) {
                            val artistName = longByTextRuns[i].text
                            val artistId =
                                longByTextRuns[i].navigationEndpoint?.browseEndpoint?.browseId
                            artist.add(Artist(artistId ?: "", artistName))
                        } else if (longByTextRuns[i].navigationEndpoint?.browseEndpoint?.browseId?.startsWith(
                                "MP",
                            ) == true
                        ) {
                            val albumName = longByTextRuns[i].text
                            val albumId =
                                longByTextRuns[i].navigationEndpoint?.browseEndpoint?.browseId
                            album.add(Album(albumId ?: "", albumName))
                        }
                    }
                }
                if (artist.isEmpty()) {
                    val artistName = longByTextRuns[0].text
                    val artistId = longByTextRuns[0].navigationEndpoint?.browseEndpoint?.browseId
                    artist.add(Artist(artistId ?: "", artistName))
                }
            }
            val videoId =
                track.playlistPanelVideoRenderer
                    ?.navigationEndpoint
                    ?.watchEndpoint
                    ?.videoId
            val thumbnail =
                track.playlistPanelVideoRenderer
                    ?.thumbnail
                    ?.thumbnails
                    ?.toListThumbnail()
            listTrack.add(
                Track(
                    album = album.firstOrNull(),
                    artists = artist,
                    duration = duration ?: "",
                    durationSeconds = durationSeconds,
                    isAvailable = true,
                    isExplicit = false,
                    likeStatus = "INDIFFERENT",
                    thumbnails = thumbnail,
                    title = title ?: "",
                    videoId = videoId ?: "",
                    videoType = "MUSIC_VIDEO",
                    category = "MUSIC",
                    feedbackTokens = null,
                    resultType = null,
                    year = null,
                ),
            )
        }
        return listTrack
    } else {
        return null
    }
}