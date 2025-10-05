package com.maxrave.media3.extension

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName

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
    val isSong = (this.thumbnails?.contains("w544") == true && this.thumbnails?.contains("h544") == true)
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
                    if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
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
                    if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
                ).build(),
        ).build()
}

@androidx.annotation.OptIn(UnstableApi::class)
fun List<Track>.toMediaItems(): List<MediaItem> {
    val listMediaItem = mutableListOf<MediaItem>()
    for (item in this) {
        listMediaItem.add(item.toMediaItem())
    }
    return listMediaItem
}

@UnstableApi
fun MediaItem.isSong(): Boolean = this.mediaMetadata.description?.contains(MERGING_DATA_TYPE.SONG) == true

@UnstableApi
fun MediaItem.isVideo(): Boolean = this.mediaMetadata.description?.contains(MERGING_DATA_TYPE.VIDEO) == true