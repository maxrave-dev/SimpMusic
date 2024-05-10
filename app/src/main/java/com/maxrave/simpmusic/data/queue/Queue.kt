package com.maxrave.simpmusic.data.queue

import android.util.Log
import com.maxrave.simpmusic.data.model.browse.album.Track

object Queue {
    private val queue: ArrayList<Track> = arrayListOf()
    private var nowPlaying: Track? = null
    private var playlistId: String? = null
    private var playlistName: String? = null
    private var playlistType: PlaylistType = PlaylistType.PLAYLIST
    private var continuation: String? = null

    fun initPlaylist(
        playlistId: String,
        playlistName: String,
        playlistType: PlaylistType,
    ) {
        this.continuation = null
        queue.clear()
        this.playlistId = playlistId
        this.playlistName = playlistName
        this.playlistType = playlistType
        Log.w("Queue", "initPlaylist: $playlistId, $playlistName, $playlistType")
    }

    fun getPlaylistId(): String? {
        return playlistId
    }

    fun getPlaylistName(): String? {
        return playlistName
    }

    fun getPlaylistType(): PlaylistType {
        return playlistType
    }

    fun setContinuation(
        playlistId: String,
        continuation: String?,
    ) {
        if (this.playlistId == playlistId) {
            this.continuation = continuation
        }
    }

    fun getContinuation(playlistId: String): String? {
        if (this.playlistId == playlistId) {
            return this.continuation
        }
        return null
    }

    fun add(song: Track) {
        queue.add(song)
    }

    fun addAll(songs: Collection<Track>) {
        queue.addAll(songs)
    }

    fun getQueue(): ArrayList<Track> {
        return queue
    }

//    fun clear() {
//        queue.clear()
//        playlistName = null
//        playlistId = null
//        playlistType = PlaylistType.PLAYLIST
//        continuation = null
//    }

    fun setNowPlaying(song: Track) {
        nowPlaying = song
    }

    fun getNowPlaying(): Track? {
        return nowPlaying
    }

    fun removeFirstTrackForPlaylistAndAlbum() {
        queue.removeAt(0)
    }

    fun removeTrackWithIndex(index: Int) {
        queue.removeAt(index)
    }

    enum class PlaylistType {
        PLAYLIST,
        LOCAL_PLAYLIST,
        RADIO,
    }

    const val LOCAL_PLAYLIST_ID_SAVED_QUEUE = "LOCAL_PLAYLIST_ID_SAVED_QUEUE"
    const val LOCAL_PLAYLIST_ID_DOWNLOADED = "LOCAL_PLAYLIST_ID_DOWNLOADED"
    const val LOCAL_PLAYLIST_ID_LIKED = "LOCAL_PLAYLIST_ID_LIKED"
    const val LOCAL_PLAYLIST_ID = "LOCAL_PLAYLIST_ID"
    const val ASC = "ASC"
    const val DESC = "DESC"
}