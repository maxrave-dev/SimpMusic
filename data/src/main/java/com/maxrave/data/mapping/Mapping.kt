package com.maxrave.data.mapping

import android.text.Html
import com.maxrave.data.parser.toListThumbnail
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.canvas.CanvasResult
import com.maxrave.domain.data.model.mediaService.SponsorSkipSegments
import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.AccountInfo
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.SearchSuggestions
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.response.PipedResponse
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.maxrave.kotlinytmusicscraper.models.youtube.Transcript
import com.maxrave.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.maxrave.spotify.model.response.spotify.CanvasResponse
import com.maxrave.spotify.model.response.spotify.SpotifyLyricsResponse
import org.simpmusic.lyrics.models.response.LyricsResponse
import org.simpmusic.lyrics.models.response.TranslatedLyricsResponse
import org.simpmusic.lyrics.parser.parseSyncedLyrics
import org.simpmusic.lyrics.parser.parseUnsyncedLyrics

internal fun SongItem.toTrack(): Track =
    Track(
        album = this.album.let { Album(it?.id ?: "", it?.name ?: "") },
        artists = this.artists.map { artist -> Artist(id = artist.id ?: "", name = artist.name) },
        duration = this.duration.toString(),
        durationSeconds = this.duration,
        isAvailable = false,
        isExplicit = this.explicit,
        likeStatus = null,
        thumbnails = this.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
        title = this.title,
        videoId = this.id,
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = null,
    )

internal fun VideoItem.toTrack(): Track =
    Track(
        album = this.album.let { Album(it?.id ?: "", it?.name ?: "") },
        artists = this.artists.map { artist -> Artist(id = artist.id ?: "", name = artist.name) },
        duration = this.duration.toString(),
        durationSeconds = this.duration,
        isAvailable = false,
        isExplicit = false,
        likeStatus = null,
        thumbnails = this.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
        title = this.title,
        videoId = this.id,
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = null,
    )

@JvmName("SongItemtoTrack")
internal fun List<SongItem>?.toListTrack(): ArrayList<Track> {
    val listTrack = arrayListOf<Track>()
    if (this != null) {
        for (item in this) {
            listTrack.add(item.toTrack())
        }
    }
    return listTrack
}

internal fun Track.toSongItemForDownload(): SongItem =
    SongItem(
        id = this.videoId,
        title = this.title,
        artists =
            this.artists?.map {
                com.maxrave.kotlinytmusicscraper.models.Artist(
                    id = it.id ?: "",
                    name = it.name,
                )
            } ?: emptyList(),
        album =
            com.maxrave.kotlinytmusicscraper.models.Album(
                id = this.album?.id ?: "",
                name = this.album?.name ?: "",
            ),
        duration = this.durationSeconds,
        thumbnail = this.thumbnails?.lastOrNull()?.url ?: "",
        explicit = this.isExplicit,
    )

internal fun org.simpmusic.lyrics.domain.Lyrics.toLyrics(): Lyrics {
    val lines: ArrayList<Line> = arrayListOf()
    if (this.lyrics != null) {
        this.lyrics?.lines?.forEach {
            lines.add(
                Line(
                    endTimeMs = it.endTimeMs,
                    startTimeMs = it.startTimeMs,
                    syllables = it.syllables ?: listOf(),
                    words = it.words,
                ),
            )
        }
        return Lyrics(
            error = false,
            lines = lines,
            syncType = this.lyrics!!.syncType,
        )
    } else {
        return Lyrics(
            error = true,
            lines = null,
            syncType = null,
        )
    }
}

internal fun Lyrics.toLibraryLyrics(): org.simpmusic.lyrics.domain.Lyrics =
    org.simpmusic.lyrics.domain.Lyrics(
        lyrics =
            org.simpmusic.lyrics.domain.Lyrics.LyricsX(
                lines =
                    this.lines?.map {
                        org.simpmusic.lyrics.domain.Lyrics.LyricsX.Line(
                            endTimeMs = it.endTimeMs,
                            startTimeMs = it.startTimeMs,
                            syllables = listOf(),
                            words = it.words,
                        )
                    },
                syncType = this.syncType,
            ),
    )

internal fun SpotifyLyricsResponse.toLyrics(): Lyrics {
    val lines: ArrayList<Line> = arrayListOf()
    this.lyrics.lines.forEach {
        lines.add(
            Line(
                endTimeMs = it.endTimeMs,
                startTimeMs = it.startTimeMs,
                syllables = listOf(),
                words = it.words,
            ),
        )
    }
    return Lyrics(
        error = false,
        lines = lines,
        syncType = this.lyrics.syncType,
    )
}

internal fun PipedResponse.toTrack(videoId: String): Track =
    Track(
        album = null,
        artists =
            listOf(
                Artist(
                    this.uploaderUrl?.replace("/channel/", ""),
                    this.uploader.toString(),
                ),
            ),
        duration = "",
        durationSeconds = 0,
        isAvailable = false,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails =
            listOf(
                Thumbnail(
                    720,
                    this.thumbnailUrl ?: "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
                    1080,
                ),
            ),
        title = this.title ?: " ",
        videoId = videoId,
        videoType = "Song",
        category = "",
        feedbackTokens = null,
        resultType = null,
        year = "",
    )

internal fun YouTubeInitialPage.toTrack(): Track {
    val initialPage = this

    return Track(
        album = null,
        artists =
            listOf(
                Artist(
                    name = initialPage.videoDetails?.author ?: "",
                    id = initialPage.videoDetails?.channelId,
                ),
            ),
        duration = initialPage.videoDetails?.lengthSeconds,
        durationSeconds = initialPage.videoDetails?.lengthSeconds?.toInt() ?: 0,
        isAvailable = false,
        isExplicit = false,
        likeStatus = null,
        thumbnails =
            initialPage.videoDetails
                ?.thumbnail
                ?.thumbnails
                ?.toListThumbnail() ?: listOf(),
        title = initialPage.videoDetails?.title ?: "",
        videoId = initialPage.videoDetails?.videoId ?: "",
        videoType = "",
        category = "",
        feedbackTokens = null,
        resultType = "",
        year = "",
    )
}

internal fun Transcript.toLyrics(): Lyrics {
    val lines =
        this.text.map {
            Line(
                endTimeMs = "0",
                startTimeMs = (it.start.toFloat() * 1000).toInt().toString(),
                syllables = listOf(),
                words = Html.fromHtml(it.content, Html.FROM_HTML_MODE_COMPACT).toString(),
            )
        }
    val sortedLine = lines.sortedBy { it.startTimeMs.toInt() }
    return Lyrics(
        error = false,
        lines = sortedLine,
        syncType = "LINE_SYNCED",
    )
}

internal fun AlbumItem.toAlbumsResult(): AlbumsResult =
    AlbumsResult(
        artists =
            this.artists?.map {
                Artist(
                    id = it.id ?: "",
                    name = it.name,
                )
            } ?: emptyList(),
        browseId = this.id,
        category = this.title,
        duration = "",
        isExplicit = this.explicit,
        resultType = "ALBUM",
        thumbnails =
            listOf(
                Thumbnail(
                    width = 720,
                    url = this.thumbnail,
                    height = 720,
                ),
            ),
        title = this.title,
        type = if (isSingle) "SINGLE" else "ALBUM",
        year = this.year?.toString() ?: "",
    )

// SimpMusic Lyrics Extension
internal fun LyricsResponse.toLyrics(): Lyrics? =
    (
        syncedLyrics?.let { if (it.isNotEmpty() && it.isNotBlank()) parseSyncedLyrics(it) else null }
            ?: (
                if (plainLyric.isNotEmpty() && plainLyric.isNotBlank()) {
                    parseUnsyncedLyrics(plainLyric)
                } else {
                    null
                }
            )
    )?.toLyrics()

internal fun TranslatedLyricsResponse.toLyrics(): Lyrics = parseSyncedLyrics(this.translatedLyric).toLyrics()

internal fun SearchSuggestions.toDomainSearchSuggestions(): com.maxrave.domain.data.model.searchResult.SearchSuggestions =
    com.maxrave.domain.data.model.searchResult.SearchSuggestions(
        queries = this.queries,
        recommendedItems =
            this.recommendedItems.map {
                when (it) {
                    is SongItem ->
                        SongsResult(
                            album =
                                Album(
                                    id = it.album?.id ?: "",
                                    name = it.album?.name ?: "",
                                ),
                            artists =
                                it.artists.map { artist ->
                                    Artist(
                                        id = artist.id ?: "",
                                        name = artist.name,
                                    )
                                },
                            category = "",
                            duration = it.duration.toString(),
                            durationSeconds = it.duration,
                            feedbackTokens = null,
                            isExplicit = it.explicit,
                            resultType = "Song",
                            thumbnails = it.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
                            title = it.title,
                            videoId = it.id,
                            videoType = null,
                            year = "",
                        )
                    is AlbumItem ->
                        AlbumsResult(
                            artists =
                                it.artists?.map {
                                    Artist(
                                        id = it.id ?: "",
                                        name = it.name,
                                    )
                                } ?: emptyList(),
                            browseId = it.browseId,
                            category = "",
                            duration = "",
                            isExplicit = it.explicit,
                            resultType = "ALBUM",
                            thumbnails =
                                listOf(
                                    Thumbnail(
                                        width = 720,
                                        url = it.thumbnail,
                                        height = 720,
                                    ),
                                ),
                            title = it.title,
                            type = if (it.isSingle) "SINGLE" else "ALBUM",
                            year = it.year?.toString() ?: "",
                        )
                    is ArtistItem ->
                        ArtistsResult(
                            artist = it.title,
                            browseId = it.id,
                            category = "",
                            radioId = it.radioEndpoint?.playlistId ?: "",
                            resultType = "ARTIST",
                            shuffleId = it.shuffleEndpoint?.playlistId ?: "",
                            thumbnails =
                                listOf(
                                    Thumbnail(
                                        width = 720,
                                        url = it.thumbnail,
                                        height = 720,
                                    ),
                                ),
                        )
                    is PlaylistItem ->
                        PlaylistsResult(
                            author = it.author?.name ?: "YouTube Music",
                            browseId = it.id,
                            category = "",
                            itemCount = "0",
                            resultType = "PLAYLIST",
                            thumbnails =
                                listOf(
                                    Thumbnail(
                                        width = 720,
                                        url = it.thumbnail,
                                        height = 720,
                                    ),
                                ),
                            title = it.title,
                        )
                    is VideoItem ->
                        VideosResult(
                            artists =
                                it.artists.map { artist ->
                                    Artist(
                                        id = artist.id ?: "",
                                        name = artist.name,
                                    )
                                },
                            category = null,
                            duration = it.duration?.toString(),
                            durationSeconds = it.duration,
                            resultType = "VIDEO",
                            thumbnails = it.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
                            title = it.title,
                            videoId = it.id,
                            videoType = null,
                            views = it.view,
                            year = "",
                        )
                }
            },
    )

internal fun CanvasResponse.toCanvasResult(): CanvasResult? {
    val canvasUrl = this.canvases.firstOrNull()?.canvas_url ?: return null
    val canvasThumbs = this.canvases.firstOrNull()?.thumbsOfCanva
    val thumbUrl =
        if (!canvasThumbs.isNullOrEmpty()) {
            (
                canvasThumbs.let { thumb ->
                    thumb
                        .maxByOrNull {
                            (it.height ?: 0) + (it.width ?: 0)
                        }?.url
                } ?: canvasThumbs.first().url
            )
        } else {
            null
        }
    return CanvasResult(
        isVideo = canvasUrl.contains(".mp4"),
        canvasUrl = canvasUrl,
        canvasThumbUrl = thumbUrl,
    )
}

internal fun YouTubeWatchEndpoint.toWatchEndpoint(): WatchEndpoint =
    WatchEndpoint(
        videoId = this.videoId,
        playlistId = this.playlistId,
        playlistSetVideoId = this.playlistSetVideoId,
        params = this.params,
        index = this.index,
        watchEndpointMusicSupportedConfigs =
            this.watchEndpointMusicSupportedConfigs?.let { supportedConfig ->
                WatchEndpoint.WatchEndpointMusicSupportedConfigs(
                    watchEndpointMusicConfig =
                        WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig(
                            musicVideoType =
                                supportedConfig.watchEndpointMusicConfig.musicVideoType,
                        ),
                )
            },
    )

internal fun WatchEndpoint.toYouTubeWatchEndpoint(): YouTubeWatchEndpoint =
    YouTubeWatchEndpoint(
        videoId = this.videoId,
        playlistId = this.playlistId,
        playlistSetVideoId = this.playlistSetVideoId,
        params = this.params,
        index = this.index,
        watchEndpointMusicSupportedConfigs =
            this.watchEndpointMusicSupportedConfigs?.let { supportedConfig ->
                YouTubeWatchEndpoint.WatchEndpointMusicSupportedConfigs(
                    watchEndpointMusicConfig =
                        YouTubeWatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig(
                            musicVideoType =
                                supportedConfig.watchEndpointMusicConfig.musicVideoType,
                        ),
                )
            },
    )

internal fun SkipSegments.toSponsorSkipSegments(): SponsorSkipSegments =
    SponsorSkipSegments(
        actionType = this.actionType,
        category = this.category,
        description = this.description,
        locked = this.locked,
        segment = this.segment,
        uUID = this.uUID,
        videoDuration = this.videoDuration,
        votes = this.votes,
    )

internal fun AccountInfo.toDomainAccountInfo(): com.maxrave.domain.data.model.account.AccountInfo =
    com.maxrave.domain.data.model.account.AccountInfo(
        name = this.name,
        email = this.email,
        pageId = this.pageId,
        thumbnails = thumbnails.toListThumbnail(),
    )