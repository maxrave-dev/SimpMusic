package com.maxrave.simpmusic.extension

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.net.toUri
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.sqlite.db.SimpleSQLiteQuery
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.models.response.PipedResponse
import com.maxrave.kotlinytmusicscraper.models.youtube.Transcript
import com.maxrave.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.maxrave.lyricsproviders.models.response.MusixmatchTranslationLyricsResponse
import com.maxrave.lyricsproviders.parser.parseMusixmatchLyrics
import com.maxrave.lyricsproviders.parser.parseUnsyncedLyrics
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.SETTINGS_FILENAME
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.LyricsEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.db.entities.TranslatedLyricsEntity
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
import com.maxrave.simpmusic.data.model.browse.artist.ResultSong
import com.maxrave.simpmusic.data.model.browse.artist.ResultVideo
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.home.Content
import com.maxrave.simpmusic.data.model.metadata.Line
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.data.model.podcast.PodcastBrowse
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.parser.toListThumbnail
import com.maxrave.simpmusic.service.test.source.MergingMediaSourceFactory
import com.maxrave.simpmusic.viewModel.ArtistScreenData
import com.maxrave.spotify.model.response.spotify.SpotifyLyricsResponse
import org.simpmusic.lyrics.models.response.LyricsResponse
import org.simpmusic.lyrics.models.response.TranslatedLyricsResponse
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

val Context.dataStore by preferencesDataStore(name = SETTINGS_FILENAME)

@Suppress("deprecation")
fun Context.isMyServiceRunning(serviceClass: Class<out Service>) =
    try {
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    } catch (e: Exception) {
        false
    }

fun SearchHistory.toQuery(): String = this.query

fun List<SearchHistory>.toQueryList(): ArrayList<String> {
    val list = ArrayList<String>()
    for (item in this) {
        list.add(item.query)
    }
    return list
}

fun ResultSong.toTrack(): Track =
    Track(
        album = album,
        artists = artists,
        duration = "",
        durationSeconds = this.durationSeconds,
        isAvailable = isAvailable,
        isExplicit = isExplicit,
        likeStatus = likeStatus,
        thumbnails = thumbnails,
        title = title,
        videoId = videoId,
        videoType = videoType,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = "",
    )

fun ResultVideo.toTrack(): Track =
    Track(
        album = null,
        artists = this.artists ?: listOf(),
        duration = this.duration,
        durationSeconds = this.durationSeconds,
        isAvailable = false,
        isExplicit = false,
        likeStatus = null,
        thumbnails = this.thumbnails,
        title = this.title,
        videoId = this.videoId,
        videoType = this.views,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = "",
    )

fun SongsResult.toTrack(): Track =
    Track(
        this.album,
        this.artists,
        this.duration ?: "",
        this.durationSeconds ?: 0,
        true,
        this.isExplicit ?: false,
        "",
        this.thumbnails,
        this.title ?: "",
        this.videoId,
        this.videoType ?: "",
        this.category,
        this.feedbackTokens,
        this.resultType,
        "",
    )

fun SongItem.toTrack(): Track =
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

fun VideoItem.toTrack(): Track =
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

@UnstableApi
fun MediaItem.isSong(): Boolean = this.mediaMetadata.description?.contains(MergingMediaSourceFactory.isSong) == true

@UnstableApi
fun MediaItem.isVideo(): Boolean = this.mediaMetadata.description?.contains(MergingMediaSourceFactory.isVideo) == true

@JvmName("SongItemtoTrack")
fun List<SongItem>?.toListTrack(): ArrayList<Track> {
    val listTrack = arrayListOf<Track>()
    if (this != null) {
        for (item in this) {
            listTrack.add(item.toTrack())
        }
    }
    return listTrack
}

fun List<Artist>?.toListName(): List<String> {
    val list = mutableListOf<String>()
    if (this != null) {
        for (item in this) {
            list.add(item.name)
        }
    }
    return list
}

fun List<Artist>?.toListId(): List<String> {
    val list = mutableListOf<String>()
    if (this != null) {
        for (item in this) {
            list.add(item.id ?: "")
        }
    }
    return list
}

fun List<String>.connectArtists(): String {
    val stringBuilder = StringBuilder()

    for ((index, artist) in this.withIndex()) {
        stringBuilder.append(artist)

        if (index < this.size - 1) {
            stringBuilder.append(", ")
        }
    }

    return stringBuilder.toString()
}

fun Track.toSongItemForDownload(): SongItem =
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

fun Track.toSongEntity(): SongEntity {
    return SongEntity(
        videoId = this.videoId,
        albumId = this.album?.id,
        albumName = this.album?.name,
        artistId = this.artists?.toListId(),
        artistName = this.artists?.toListName(),
        duration = this.duration ?: "",
        durationSeconds = this.durationSeconds ?: 0,
        isAvailable = this.isAvailable,
        isExplicit = this.isExplicit,
        likeStatus = this.likeStatus ?: "",
        thumbnails =
            this.thumbnails?.last()?.url?.let {
                if (it.contains("w120")) {
                    return@let Regex("([wh])120").replace(it, "$1544")
                } else if (it.contains("sddefault")) {
                    return@let it.replace("sddefault", "maxresdefault")
                } else {
                    return@let it
                }
            },
        title = this.title,
        videoType = this.videoType ?: "",
        category = this.category,
        resultType = this.resultType,
        liked = false,
        totalPlayTime = 0,
        downloadState = 0,
    )
}

fun String?.removeDuplicateWords(): String {
    if (this == null) {
        return "null"
    } else {
        val regex = Regex("\\b(\\w+)\\b\\s*(?=.*\\b\\1\\b)")
        return this.replace(regex, "")
    }
}

fun SongEntity.toTrack(): Track {
    val listArtist = mutableListOf<Artist>()
    if (this.artistName != null) {
        for (i in 0 until this.artistName.size) {
            listArtist.add(Artist(this.artistId?.get(i) ?: "", this.artistName[i]))
        }
    }
    val isSong = (this.thumbnails?.contains("w544") == true && this.thumbnails.contains("h544"))
    return Track(
        album = this.albumId?.let { this.albumName?.let { it1 -> Album(it, it1) } },
        artists = listArtist,
        duration = this.duration,
        durationSeconds = this.durationSeconds,
        isAvailable = this.isAvailable,
        isExplicit = this.isExplicit,
        likeStatus = this.likeStatus,
        thumbnails = if (isSong) listOf(Thumbnail(544, this.thumbnails ?: "", 544)) else listOf(Thumbnail(720, this.thumbnails ?: "", 1080)),
        title = this.title,
        videoId = this.videoId,
        videoType = this.videoType,
        category = this.category,
        feedbackTokens = null,
        resultType = null,
        year = "",
    )
}

fun List<SongEntity>?.toArrayListTrack(): ArrayList<Track> {
    val listTrack: ArrayList<Track> = arrayListOf()
    if (this != null) {
        for (item in this) {
            listTrack.add(item.toTrack())
        }
    }
    return listTrack
}

fun MediaItem?.toSongEntity(): SongEntity? =
    if (this != null) {
        SongEntity(
            videoId = this.mediaId,
            albumId = null,
            albumName = this.mediaMetadata.albumTitle.toString(),
            artistId = null,
            artistName = listOf(this.mediaMetadata.artist.toString()),
            duration = "",
            durationSeconds = 0,
            isAvailable = true,
            isExplicit = false,
            likeStatus = "INDIFFERENT",
            thumbnails = this.mediaMetadata.artworkUri.toString(),
            title = this.mediaMetadata.title.toString(),
            videoType = "",
            category = "",
            resultType = "",
            liked = false,
            totalPlayTime = 0,
            downloadState = 0,
        )
    } else {
        null
    }

@JvmName("MediaItemtoSongEntity")
@UnstableApi
fun SongEntity.toMediaItem(): MediaItem {
    val isSong = (this.thumbnails?.contains("w544") == true && this.thumbnails.contains("h544"))
    return MediaItem
        .Builder()
        .setMediaId(this.videoId)
        .setUri(this.videoId)
        .setCustomCacheKey(this.videoId)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(this.title)
                .setArtist(this.artistName?.connectArtists())
                .setArtworkUri(this.thumbnails?.toUri())
                .setAlbumTitle(this.albumName)
                .setDescription(
                    if (isSong) MergingMediaSourceFactory.isSong else MergingMediaSourceFactory.isVideo,
                ).build(),
        ).build()
}

@JvmName("TracktoMediaItem")
@UnstableApi
fun Track.toMediaItem(): MediaItem {
    var thumbUrl =
        this.thumbnails?.last()?.url
            ?: "http://i.ytimg.com/vi/${this.videoId}/maxresdefault.jpg"
    if (thumbUrl.contains("w120")) {
        thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
    }
    val artistName: String = this.artists.toListName().connectArtists()
    val isSong =
        (
            this.thumbnails?.last()?.height != 0 &&
                this.thumbnails?.last()?.height == this.thumbnails?.last()?.width &&
                this.thumbnails?.last()?.height != null
        ) &&
            (!thumbUrl.contains("hq720") && !thumbUrl.contains("maxresdefault"))
    return MediaItem
        .Builder()
        .setMediaId(this.videoId)
        .setUri(this.videoId)
        .setCustomCacheKey(this.videoId)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(this.title)
                .setArtist(this.artists.toListName().connectArtists())
                .setArtworkUri(thumbUrl.toUri())
                .setAlbumTitle(this.album?.name)
                .setDescription(
                    if (isSong) MergingMediaSourceFactory.isSong else MergingMediaSourceFactory.isVideo,
                ).build(),
        ).build()
}

@UnstableApi
fun List<Track>.toMediaItems(): List<MediaItem> {
    val listMediaItem = mutableListOf<MediaItem>()
    for (item in this) {
        listMediaItem.add(item.toMediaItem())
    }
    return listMediaItem
}

@JvmName("SongResulttoListTrack")
fun ArrayList<SongsResult>.toListTrack(): ArrayList<Track> {
    val listTrack = arrayListOf<Track>()
    for (song in this) {
        listTrack.add(song.toTrack())
    }
    return listTrack
}

fun VideosResult.toTrack(): Track {
    val thumb = Thumbnail(720, "http://i.ytimg.com/vi/${this.videoId}/maxresdefault.jpg", 1280)
    val thumbList: List<Thumbnail>?
    thumbList = this.thumbnails ?: mutableListOf(thumb)
    return Track(
        album = null,
        artists = this.artists,
        duration = this.duration ?: "",
        durationSeconds = this.durationSeconds ?: 0,
        isAvailable = true,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = thumbList,
        title = this.title,
        videoId = this.videoId,
        videoType = this.videoType ?: "",
        category = this.category,
        feedbackTokens = null,
        resultType = this.resultType,
        year = "",
    )
}

@JvmName("VideoResulttoTrack")
fun ArrayList<VideosResult>.toListTrack(): ArrayList<Track> {
    val listTrack = arrayListOf<Track>()
    for (video in this) {
        listTrack.add(video.toTrack())
    }
    return listTrack
}

fun Content.toTrack(): Track =
    Track(
        album = album,
        artists = artists ?: listOf(Artist("", "")),
        duration = "",
        durationSeconds = durationSeconds,
        isAvailable = false,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = thumbnails,
        title = title,
        videoId = videoId!!,
        videoType = "",
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = "",
    )

fun List<Track>.toListVideoId(): List<String> {
    val list = mutableListOf<String>()
    for (item in this) {
        list.add(item.videoId)
    }
    return list
}

fun AlbumBrowse.toAlbumEntity(id: String): AlbumEntity =
    AlbumEntity(
        browseId = id,
        artistId = this.artists.toListId(),
        artistName = this.artists.toListName(),
        audioPlaylistId = this.audioPlaylistId,
        description = this.description ?: "",
        duration = this.duration,
        durationSeconds = this.durationSeconds,
        thumbnails = this.thumbnails?.last()?.url,
        title = this.title,
        trackCount = this.trackCount,
        tracks = this.tracks.toListVideoId(),
        type = this.type,
        year = this.year,
    )

fun PlaylistBrowse.toPlaylistEntity(): PlaylistEntity =
    PlaylistEntity(
        id = this.id,
        author = this.author.name,
        description = this.description ?: "",
        duration = this.duration,
        durationSeconds = this.durationSeconds,
        privacy = this.privacy,
        thumbnails = this.thumbnails.last().url,
        title = this.title,
        trackCount = this.trackCount,
        tracks = this.tracks.toListVideoId(),
        year = this.year,
        downloadState = DownloadState.STATE_NOT_DOWNLOADED,
    )

fun Track.addThumbnails(): Track =
    Track(
        album = this.album,
        artists = this.artists,
        duration = this.duration,
        durationSeconds = this.durationSeconds,
        isAvailable = this.isAvailable,
        isExplicit = this.isExplicit,
        likeStatus = this.likeStatus,
        thumbnails =
            listOf(
                Thumbnail(
                    720,
                    "https://i.ytimg.com/vi/${this.videoId}/maxresdefault.jpg",
                    1280,
                ),
            ),
        title = this.title,
        videoId = this.videoId,
        videoType = this.videoType,
        category = this.category,
        feedbackTokens = this.feedbackTokens,
        resultType = this.resultType,
        year = this.year,
    )

fun LyricsEntity.toLyrics(): Lyrics =
    Lyrics(
        error = this.error,
        lines = this.lines,
        syncType = this.syncType,
    )

fun Lyrics.toLyricsEntity(videoId: String): LyricsEntity =
    LyricsEntity(
        videoId = videoId,
        error = this.error,
        lines = this.lines,
        syncType = this.syncType,
    )

fun Collection<SongEntity>.toVideoIdList(): List<String> {
    val list = mutableListOf<String>()
    for (item in this) {
        list.add(item.videoId)
    }
    return list
}

fun ArtistBrowse.toArtistScreenData(): ArtistScreenData =
    ArtistScreenData(
        title = this.name,
        imageUrl = this.thumbnails?.lastOrNull()?.url,
        subscribers = this.subscribers,
        playCount = this.views,
        isChannel = this.songs == null,
        channelId = this.channelId,
        radioParam = this.radioId,
        shuffleParam = this.shuffleId,
        description = this.description,
        listSongParam = this.songs?.browseId,
        popularSongs = this.songs?.results?.map { it.toTrack() } ?: emptyList(),
        singles = this.singles,
        albums = this.albums,
        video =
            this.video?.let { video ->
                ArtistBrowse.Videos(video.map { it.toTrack() }, this.videoList)
            },
        related = this.related,
        featuredOn = this.featuredOn ?: emptyList(),
    )

fun setEnabledAll(
    v: View,
    enabled: Boolean,
) {
    v.isEnabled = enabled
    v.isFocusable = enabled
    if (v is ImageButton) {
        if (enabled) v.setColorFilter(Color.WHITE) else v.setColorFilter(Color.GRAY)
    }
    if (v is TextView) {
        v.isEnabled = enabled
    }
    if (v is ViewGroup) {
        val vg = v
        for (i in 0 until vg.childCount) setEnabledAll(vg.getChildAt(i), enabled)
    }
}

fun getScreenSize(context: Context): Point {
    val x: Int = context.resources.displayMetrics.widthPixels
    val y: Int = context.resources.displayMetrics.heightPixels
    return Point(x, y)
}

fun ArrayList<String>.removeConflicts(): ArrayList<String> {
    val nonConflictingSet = HashSet<String>()
    val nonConflictingList = ArrayList<String>()

    for (item in this) {
        if (nonConflictingSet.add(item)) {
            nonConflictingList.add(item)
        }
    }

    return nonConflictingList
}

fun <T> Iterable<T>.indexMap(): Map<T, Int> {
    val map = mutableMapOf<T, Int>()
    forEachIndexed { i, v ->
        map[v] = i
    }
    return map
}

fun com.maxrave.lyricsproviders.models.lyrics.Lyrics.toLyrics(): Lyrics {
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

fun Lyrics.toLibraryLyrics(): com.maxrave.lyricsproviders.models.lyrics.Lyrics =
    com.maxrave.lyricsproviders.models.lyrics.Lyrics(
        lyrics =
            com.maxrave.lyricsproviders.models.lyrics.Lyrics.LyricsX(
                lines =
                    this.lines?.map {
                        com.maxrave.lyricsproviders.models.lyrics.Line(
                            endTimeMs = it.endTimeMs,
                            startTimeMs = it.startTimeMs,
                            syllables = listOf(),
                            words = it.words,
                        )
                    },
                syncType = this.syncType,
            ),
    )

fun SpotifyLyricsResponse.toLyrics(): Lyrics {
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

fun PipedResponse.toTrack(videoId: String): Track =
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

fun YouTubeInitialPage.toTrack(): Track {
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

fun MusixmatchTranslationLyricsResponse.toLyrics(originalLyrics: Lyrics): Lyrics? {
    if (this.message.body.translations_list
            .isEmpty()
    ) {
        return null
    } else {
        val listTranslation = this.message.body.translations_list
        val translation =
            originalLyrics.copy(
                lines =
                    originalLyrics.lines?.mapIndexed { index, line ->
                        line.copy(
                            words =
                                if (!line.words.contains("♫")) {
                                    listTranslation
                                        .find {
                                            it.translation.matched_line == line.words ||
                                                it.translation.subtitle_matched_line == line.words ||
                                                it.translation.snippet == line.words
                                        }?.translation
                                        ?.description
                                        ?: ""
                                } else {
                                    line.words
                                },
                        )
                    },
            )
        return translation
    }
}

fun TranslatedLyricsEntity.toLyrics(): Lyrics =
    Lyrics(
        error = this.error,
        lines = this.lines,
        syncType = this.syncType,
    )

fun Transcript.toLyrics(): Lyrics {
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

fun PodcastBrowse.EpisodeItem.toTrack(): Track =
    Track(
        album = null,
        artists = listOf(this.author),
        duration = this.durationString,
        durationSeconds = null,
        isAvailable = true,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = this.thumbnail,
        title = this.title,
        videoId = this.videoId,
        videoType = "Podcast",
        category = "Podcast",
        feedbackTokens = null,
        resultType = "Podcast",
        year = this.createdDay,
    )

@JvmName("PodcastBrowseEpisodeItemtoListTrack")
fun List<PodcastBrowse.EpisodeItem>.toListTrack(): ArrayList<Track> {
    val listTrack = arrayListOf<Track>()
    for (item in this) {
        listTrack.add(item.toTrack())
    }
    return listTrack
}

fun AlbumItem.toAlbumsResult(): AlbumsResult =
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

fun TextView.setTextAnimation(
    text: String,
    duration: Long = 300,
    completion: (() -> Unit)? = null,
) {
    if (text != "null") {
        fadOutAnimation(duration) {
            this.text = text
            fadInAnimation(duration) {
                completion?.let {
                    it()
                }
            }
        }
    }
}

fun View.fadOutAnimation(
    duration: Long = 300,
    visibility: Int = View.INVISIBLE,
    completion: (() -> Unit)? = null,
) {
    animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction {
            this.visibility = visibility
            completion?.let {
                it()
            }
        }
}

fun View.fadInAnimation(
    duration: Long = 300,
    completion: (() -> Unit)? = null,
) {
    alpha = 0f
    visibility = View.VISIBLE
    animate()
        .alpha(1f)
        .setDuration(duration)
        .withEndAction {
            completion?.let {
                it()
            }
        }
}

infix fun <E> Collection<E>.symmetricDifference(other: Collection<E>): Set<E> {
    val left = this subtract other
    val right = other subtract this
    return left union right
}

fun LocalDateTime.formatTimeAgo(context: Context): String {
    val now = LocalDateTime.now()
    val hoursDiff = ChronoUnit.HOURS.between(this, now)
    val daysDiff = ChronoUnit.DAYS.between(this, now)
    val monthsDiff = ChronoUnit.MONTHS.between(this, now)

    return when {
        monthsDiff >= 1 -> context.getString(R.string.month_s_ago, monthsDiff)
        daysDiff >= 30 -> context.getString(R.string.month_s_ago, daysDiff / 30)
        hoursDiff >= 24 -> context.getString(R.string.day_s_ago, daysDiff)
        hoursDiff > 1 -> context.getString(R.string.hour_s_ago, hoursDiff)
        hoursDiff <= 1 -> context.getString(R.string.recently)
        else -> context.getString(R.string.unknown)
    }
}

fun formatDuration(
    duration: Long,
    context: Context,
): String {
    if (duration < 0L) return context.getString(R.string.na_na)
    val minutes: Long = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
    val seconds: Long = (
        TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) -
            minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)
    )
    return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
}

fun parseTimestampToMilliseconds(timestamp: String): Double {
    val parts = timestamp.split(":")
    val totalSeconds =
        when (parts.size) {
            2 -> {
                try {
                    val minutes = parts[0].toDouble()
                    val seconds = parts[1].toDouble()
                    (minutes * 60 + seconds)
                } catch (e: NumberFormatException) {
                    // Handle parsing error
                    e.printStackTrace()
                    return 0.0
                }
            }

            3 -> {
                try {
                    val hours = parts[0].toDouble()
                    val minutes = parts[1].toDouble()
                    val seconds = parts[2].toDouble()
                    (hours * 3600 + minutes * 60 + seconds)
                } catch (e: NumberFormatException) {
                    // Handle parsing error
                    e.printStackTrace()
                    return 0.0
                }
            }

            else -> {
                // Handle incorrect format
                return 0.0
            }
        }
    return totalSeconds * 1000
}

operator fun File.div(child: String): File = File(this, child)

fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)

fun InputStream.zipInputStream(): ZipInputStream = ZipInputStream(this)

fun OutputStream.zipOutputStream(): ZipOutputStream = ZipOutputStream(this)

fun Long?.bytesToMB(): Long {
    val mbInBytes = 1024 * 1024
    return this?.div(mbInBytes) ?: 0L
}

fun getSizeOfFile(dir: File): Long {
    var dirSize: Long = 0
    if (!dir.listFiles().isNullOrEmpty()) {
        for (f in dir.listFiles()!!) {
            dirSize += f.length()
            if (f.isDirectory) {
                dirSize += getSizeOfFile(f)
            }
        }
    }
    return dirSize
}

fun isNetworkAvailable(context: Context?): Boolean {
    val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // Returns a Network object corresponding to
    // the currently active default data network.
    val network = connectivityManager.activeNetwork ?: return false

    // Representation of the capabilities of an active network.
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

    return when {
        // Indicates this network uses a Wi-Fi transport,
        // or WiFi has network connectivity
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

        // Indicates this network uses a Cellular transport. or
        // Cellular has network connectivity
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

        // else return false
        else -> false
    }
}

fun Lyrics.toSyncedLrcString(): String? {
    if (this.lines.isNullOrEmpty() || this.syncType != "LINE_SYNCED") {
        return null
    }
    return this.lines.joinToString("\n") { line ->
        val startTimeMs = line.startTimeMs.toLong()
        val minutes = (startTimeMs / 60000).toString().padStart(2, '0')
        val seconds = ((startTimeMs % 60000) / 1000).toString().padStart(2, '0')
        val milliseconds = ((startTimeMs % 1000) / 10).toString().padStart(2, '0')

        // Add space before the content as it was removed in the parsing function
        val content = if (line.words == "♫") " " else " ${line.words}"

        "[$minutes:$seconds.$milliseconds]$content"
    }
}

fun Lyrics.toPlainLrcString(): String? {
    if (this.lines.isNullOrEmpty()) {
        return null
    }
    return this.lines.joinToString("\n") { it.words }
}

// SimpMusic Lyrics Extension
fun LyricsResponse.toLyrics(): Lyrics? =
    (
        syncedLyrics?.let { if (it.isNotEmpty() && it.isNotBlank()) parseMusixmatchLyrics(it) else null }
            ?: (
                if (plainLyric.isNotEmpty() && plainLyric.isNotBlank()) {
                    parseUnsyncedLyrics(plainLyric)
                } else {
                    null
                }
            )
    )?.toLyrics()

fun TranslatedLyricsResponse.toLyrics(): Lyrics = parseMusixmatchLyrics(this.translatedLyric).toLyrics()