package com.maxrave.simpmusic.extension

import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.artist.ResultSong
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.home.Content
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult


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
        durationSeconds = 0,
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
        year = ""
    )
}

fun SongsResult.toTrack(): Track {
    return Track(
        this.album,
        this.artists,
        this.duration ?: "",
        this.durationSeconds?: 0,
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
        ""
    )
}
fun List<Artist>?.toListName(): List<String> {
    val list = mutableListOf<String>()
    if (this != null){
        for (item in this) {
            list.add(item.name)
        }
    }
    return list
}
fun List<Artist>.toListId(): List<String> {
    val list = mutableListOf<String>()
    for (item in this) {
        if (item.id != null) {
            list.add(item.id)
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
        duration = this.duration,
        durationSeconds = this.durationSeconds,
        isAvailable = this.isAvailable,
        isExplicit = this.isExplicit,
        likeStatus = this.likeStatus,
        thumbnails = this.thumbnails?.last()?.url,
        title = this.title,
        videoType = this.videoType,
        category = this.category,
        resultType = this.resultType,
        liked = false,
        totalPlayTime = 0,
        downloadState = 0
    )
}

fun SongEntity.toTrack(): Track {
    val listArtist = mutableListOf<Artist>()
    if (this.artistName != null ) {
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
        thumbnails = listOf(Thumbnail(720, this.thumbnails ?: "",1080)),
        title = this.title,
        videoId = this.videoId,
        videoType = this.videoType,
        category = this.category,
        feedbackTokens = null,
        resultType = null,
        year = ""
    )
}

fun MediaItem?.toSongEntity(): SongEntity? {
    return if (this != null) SongEntity(
        videoId = this.mediaId,
        albumId = null,
        albumName = this.mediaMetadata.albumTitle.toString(),
        artistId = null,
        artistName = listOf(this.mediaMetadata.artist.toString()),
        duration =  "",
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
        downloadState = 0
    ) else null
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
        duration = this.duration?: "",
        durationSeconds = this.durationSeconds?: 0,
        isAvailable = true,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = thumbList,
        title = this.title,
        videoId = this.videoId,
        videoType = this.videoType?: "",
        category = this.category,
        feedbackTokens = null,
        resultType = this.resultType,
        year = "")
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
        artists = artists,
        duration = "",
        durationSeconds = 0,
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
        year = ""
    )
}
fun List<Track>.toListVideoId(): List<String> {
    val list = mutableListOf<String>()
    for (item in this) {
        list.add(item.videoId)
    }
    return list
}

fun AlbumBrowse.toAlbumEntity(id : String): AlbumEntity {
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
        year = this.year
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
        year = this.year
    )
}

fun setEnabledAll(v: View, enabled: Boolean) {
    v.isEnabled = enabled
    v.isFocusable = enabled
    if (v is ViewGroup) {
        val vg = v
        for (i in 0 until vg.childCount) setEnabledAll(vg.getChildAt(i), enabled)
    }
}

