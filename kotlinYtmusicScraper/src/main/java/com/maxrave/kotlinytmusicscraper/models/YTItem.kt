package com.maxrave.kotlinytmusicscraper.models

import com.maxrave.kotlinytmusicscraper.models.response.LikeStatus

sealed class YTItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnail: String
    abstract val explicit: Boolean
    abstract val shareLink: String
    abstract val type: YTItemType
}

data class Artist(
    val name: String,
    val id: String?,
)

data class Album(
    val name: String,
    val id: String,
)

data class SongItem(
    override val id: String,
    override val title: String,
    val artists: List<Artist>,
    val album: Album? = null,
    val duration: Int? = null,
    override val thumbnail: String,
    override val explicit: Boolean = false,
    val endpoint: WatchEndpoint? = null,
    val thumbnails: Thumbnails? = null,
    val badges: List<SongBadges>? = null,
    val likeStatus: LikeStatus = LikeStatus.INDIFFERENT,
    val setVideoId: String? = null,
) : YTItem() {
    override val shareLink: String
        get() = "https://music.youtube.com/watch?v=$id"
    override val type: YTItemType
        get() = YTItemType.SONG

    sealed class SongBadges {
        data object Explicit : SongBadges()

        data object Remix : SongBadges()
    }
}

data class VideoItem(
    override val id: String,
    override val title: String,
    override val thumbnail: String,
    override val explicit: Boolean = false,
    val endpoint: WatchEndpoint? = null,
    val thumbnails: Thumbnails? = null,
    val artists: List<Artist>,
    val album: Album? = null,
    val duration: Int? = null,
    val view: String? = null,
    val likeStatus: LikeStatus = LikeStatus.INDIFFERENT,
    val setVideoId: String? = null,
) : YTItem() {
    override val shareLink: String
        get() = "https://music.youtube.com/watch?v=$id"
    override val type: YTItemType
        get() = YTItemType.VIDEO
}

data class AlbumItem(
    val browseId: String,
    val playlistId: String,
    override val id: String = browseId,
    override val title: String,
    val artists: List<Artist>?,
    val year: Int? = null,
    val isSingle: Boolean = false,
    override val thumbnail: String,
    override val explicit: Boolean = false,
) : YTItem() {
    override val shareLink: String
        get() = "https://music.youtube.com/playlist?list=$playlistId"
    override val type: YTItemType
        get() = YTItemType.ALBUM
}

data class PlaylistItem(
    override val id: String,
    override val title: String,
    val author: Artist?,
    val songCountText: String?,
    override val thumbnail: String,
    val playEndpoint: WatchEndpoint?,
    val shuffleEndpoint: WatchEndpoint,
    val radioEndpoint: WatchEndpoint? = null,
) : YTItem() {
    override val explicit: Boolean
        get() = false
    override val shareLink: String
        get() = "https://music.youtube.com/playlist?list=$id"
    override val type: YTItemType
        get() = YTItemType.PLAYLIST
}

data class ArtistItem(
    override val id: String,
    override val title: String,
    override val thumbnail: String,
    val shuffleEndpoint: WatchEndpoint?,
    val radioEndpoint: WatchEndpoint?,
    val subscribers: String? = null,
) : YTItem() {
    override val explicit: Boolean
        get() = false
    override val shareLink: String
        get() = "https://music.youtube.com/channel/$id"
    override val type: YTItemType
        get() = YTItemType.ARTIST
}

enum class YTItemType {
    SONG,
    VIDEO,
    ALBUM,
    PLAYLIST,
    ARTIST,
}