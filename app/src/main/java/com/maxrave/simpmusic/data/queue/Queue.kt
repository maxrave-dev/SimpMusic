package com.maxrave.simpmusic.data.queue

import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.playlist.TrackPlaylist
import com.maxrave.simpmusic.data.model.mediaService.Song
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.LinkedList
import java.util.Queue

object Queue {
    private var queue: ArrayList<Track> = ArrayList()
    private var recently: ArrayList<Track> = ArrayList()
    private var nowPlaying: Track? = null
    fun add(song: Track) {
        queue.add(song)
    }
    fun addAll(songs: ArrayList<Track>) {
        queue.addAll(songs)
    }
    fun getQueue(): ArrayList<Track> {
        return queue
    }
    fun clear() {
        queue = ArrayList()
    }
    fun getTrack(index: Int): Track {
        recently.add(queue.elementAt(index))
        nowPlaying = queue.elementAt(index)
        for (i in index downTo 0) {
            queue.removeAt(i)
        }
        return nowPlaying!!
    }
}