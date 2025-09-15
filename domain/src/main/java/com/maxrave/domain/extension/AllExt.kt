package com.maxrave.domain.extension

import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericMediaMetadata
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName

fun GenericMediaItem.isSong(): Boolean = this.metadata.description?.contains(MERGING_DATA_TYPE.SONG) == true

fun GenericMediaItem.isVideo(): Boolean = this.metadata.description?.contains(MERGING_DATA_TYPE.VIDEO) == true

fun GenericMediaItem.toSongEntity(): SongEntity =
    SongEntity(
        videoId = this.mediaId,
        albumId = null,
        albumName = this.metadata.albumTitle.toString(),
        artistId = null,
        artistName = listOf(this.metadata.artist.toString()),
        duration = "",
        durationSeconds = 0,
        isAvailable = true,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = this.metadata.artworkUri.toString(),
        title = this.metadata.title.toString(),
        videoType = "",
        category = "",
        resultType = "",
        liked = false,
        totalPlayTime = 0,
        downloadState = 0,
    )

fun SongEntity.toGenericMediaItem(): GenericMediaItem {
    val isSong = (this.thumbnails?.contains("w544") == true && this.thumbnails.contains("h544"))
    return GenericMediaItem(
        mediaId = this.videoId,
        uri = this.videoId,
        metadata =
            GenericMediaMetadata(
                title = this.title,
                artist = this.artistName?.connectArtists(),
                albumTitle = this.albumName,
                artworkUri = this.thumbnails,
                description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
            ),
        customCacheKey = this.videoId,
    )
}

fun Track.toGenericMediaItem(): GenericMediaItem {
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
    return GenericMediaItem(
        mediaId = this.videoId,
        uri = this.videoId,
        metadata =
            GenericMediaMetadata(
                title = this.title,
                artist = artistName,
                albumTitle = this.album?.name,
                artworkUri = thumbUrl,
                description = if (isSong) MERGING_DATA_TYPE.SONG else MERGING_DATA_TYPE.VIDEO,
            ),
        customCacheKey = this.videoId,
    )
}