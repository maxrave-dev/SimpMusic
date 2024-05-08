package com.maxrave.simpmusic.extension

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.net.toUri
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.sqlite.db.SimpleSQLiteQuery
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.models.musixmatch.MusixmatchTranslationLyricsResponse
import com.maxrave.kotlinytmusicscraper.models.response.PipedResponse
import com.maxrave.kotlinytmusicscraper.models.response.spotify.SpotifyLyricsResponse
import com.maxrave.kotlinytmusicscraper.models.youtube.Transcript
import com.maxrave.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.SETTINGS_FILENAME
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.LyricsEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.artist.ResultSong
import com.maxrave.simpmusic.data.model.browse.artist.ResultVideo
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.home.Content
import com.maxrave.simpmusic.data.model.metadata.Line
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.data.model.podcast.PodcastBrowse
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.parser.toListThumbnail
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

val Context.dataStore by preferencesDataStore(name = SETTINGS_FILENAME)

fun Context.isMyServiceRunning(serviceClass: Class<out Service>) =
    try {
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    } catch (e: Exception) {
        false
    }

fun SearchHistory.toQuery(): String {
    return this.query
}

fun List<SearchHistory>.toQueryList(): ArrayList<String> {
    val list = ArrayList<String>()
    for (item in this) {
        list.add(item.query)
    }
    return list
}

fun ResultSong.toTrack(): Track {
    return Track(
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
}

fun ResultVideo.toTrack(): Track {
    return Track(
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
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = "",
    )
}

fun SongsResult.toTrack(): Track {
    return Track(
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
}

fun SongItem.toTrack(): Track {
    return Track(
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
}

fun VideoItem.toTrack(): Track {
    return Track(
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
}

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
    return Track(
        album = this.albumId?.let { this.albumName?.let { it1 -> Album(it, it1) } },
        artists = listArtist,
        duration = this.duration,
        durationSeconds = this.durationSeconds,
        isAvailable = this.isAvailable,
        isExplicit = this.isExplicit,
        likeStatus = this.likeStatus,
        thumbnails = listOf(Thumbnail(720, this.thumbnails ?: "", 1080)),
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

fun MediaItem?.toSongEntity(): SongEntity? {
    return if (this != null) {
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
}

@JvmName("MediaItemtoSongEntity")
@UnstableApi
fun SongEntity.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(this.videoId)
        .setUri(this.videoId)
        .setCustomCacheKey(this.videoId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(this.title)
                .setArtist(this.artistName?.connectArtists())
                .setArtworkUri(this.thumbnails?.toUri())
                .setAlbumTitle(this.albumName)
                .build(),
        )
        .build()
}

@JvmName("TracktoMediaItem")
@UnstableApi
fun Track.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(this.videoId)
        .setUri(this.videoId)
        .setCustomCacheKey(this.videoId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(this.title)
                .setArtist(this.artists.toListName().connectArtists())
                .setArtworkUri(this.thumbnails?.lastOrNull()?.url?.toUri())
                .setAlbumTitle(this.album?.name)
                .build(),
        )
        .build()
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

fun Content.toTrack(): Track {
    return Track(
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
}

fun List<Track>.toListVideoId(): List<String> {
    val list = mutableListOf<String>()
    for (item in this) {
        list.add(item.videoId)
    }
    return list
}

fun AlbumBrowse.toAlbumEntity(id: String): AlbumEntity {
    return AlbumEntity(
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
}

fun PlaylistBrowse.toPlaylistEntity(): PlaylistEntity {
    return PlaylistEntity(
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
    )
}

fun Track.addThumbnails(): Track {
    return Track(
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
}

fun LyricsEntity.toLyrics(): Lyrics {
    return Lyrics(
        error = this.error,
        lines = this.lines,
        syncType = this.syncType,
    )
}

fun Lyrics.toLyricsEntity(videoId: String): LyricsEntity {
    return LyricsEntity(
        videoId = videoId,
        error = this.error,
        lines = this.lines,
        syncType = this.syncType,
    )
}

fun Collection<SongEntity>.toVideoIdList(): List<String> {
    val list = mutableListOf<String>()
    for (item in this) {
        list.add(item.videoId)
    }
    return list
}

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

fun com.maxrave.kotlinytmusicscraper.models.lyrics.Lyrics.toLyrics(): Lyrics {
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

fun PipedResponse.toTrack(videoId: String): Track {
    return Track(
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
}

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
        thumbnails = initialPage.videoDetails?.thumbnail?.thumbnails?.toListThumbnail() ?: listOf(),
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
    if (this.message.body.translations_list.isEmpty()) {
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
                                    listTranslation.find {
                                        it.translation.matched_line == line.words || it.translation.subtitle_matched_line == line.words || it.translation.snippet == line.words
                                    }?.translation?.description
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

fun NavController.navigateSafe(
    resId: Int,
    bundle: Bundle? = null,
) {
    if (currentDestination?.id != resId) {
        if (bundle != null) {
            navigate(resId, bundle)
        } else {
            navigate(resId)
        }
    }
}

fun <T> LiveData<T>.observeOnce(
    lifecycleOwner: LifecycleOwner,
    observer: Observer<T>,
) {
    observe(
        lifecycleOwner,
        object : Observer<T> {
            override fun onChanged(value: T) {
                observer.onChanged(value)
                removeObserver(this)
            }
        },
    )
}

fun <A, B> zip(
    first: LiveData<A>,
    second: LiveData<B>,
): Flow<Pair<A, B>> {
    val mediatorLiveData = MediatorLiveData<Pair<A, B>>()

    var isFirstEmitted = false
    var isSecondEmitted = false
    var firstValue: A? = null
    var secondValue: B? = null

    mediatorLiveData.addSource(first) {
        isFirstEmitted = true
        firstValue = it
        if (isSecondEmitted) {
            mediatorLiveData.value = Pair(firstValue!!, secondValue!!)
            isFirstEmitted = false
            isSecondEmitted = false
        }
    }
    mediatorLiveData.addSource(second) {
        isSecondEmitted = true
        secondValue = it
        if (isFirstEmitted) {
            mediatorLiveData.value = Pair(firstValue!!, secondValue!!)
            isFirstEmitted = false
            isSecondEmitted = false
        }
    }

    return mediatorLiveData.asFlow()
}

fun PodcastBrowse.EpisodeItem.toTrack(): Track {
    return Track(
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
}

@JvmName("PodcastBrowseEpisodeItemtoListTrack")
fun List<PodcastBrowse.EpisodeItem>.toListTrack(): ArrayList<Track> {
    val listTrack = arrayListOf<Track>()
    for (item in this) {
        listTrack.add(item.toTrack())
    }
    return listTrack
}

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
        else -> context.getString(androidx.media3.ui.R.string.exo_track_unknown)
    }
}

operator fun File.div(child: String): File = File(this, child)

fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)

fun InputStream.zipInputStream(): ZipInputStream = ZipInputStream(this)

fun OutputStream.zipOutputStream(): ZipOutputStream = ZipOutputStream(this)