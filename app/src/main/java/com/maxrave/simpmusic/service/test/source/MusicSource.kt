package com.maxrave.simpmusic.service.test.source

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.maxrave.kotlinyoutubeextractor.State
import com.maxrave.kotlinyoutubeextractor.YTExtractor
import com.maxrave.kotlinyoutubeextractor.bestQuality
import com.maxrave.kotlinyoutubeextractor.getAudioOnly
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.service.test.source.StateSource.*
import javax.inject.Inject

class MusicSource @Inject constructor(val context: Context) {
    var catalog: List<MediaItem> = emptyList()

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    var state: StateSource = STATE_CREATED
        set(value) {
            if(value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    fun whenReady(action: (Boolean) -> Unit): Boolean {
        return if(state == STATE_CREATED || state == STATE_INITIALIZING) {
            onReadyListeners += action
            false
        } else {
            action(state == STATE_INITIALIZED)
            true
        }
    }
    fun reset() {
        catalog = emptyList()
        state = STATE_CREATED
    }

    suspend fun load() {
        updateCatalog()?.let { updatedCatalog ->
            catalog = updatedCatalog
            Log.d("MusicSource", "load: ${catalog.size}")
            state = STATE_INITIALIZED
        } ?: run {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    private suspend fun updateCatalog(): List<MediaItem>? {
        if (Queue.getQueue() == null) return null
        else {
            var tempQueue = Queue.getQueue()
            var tempCatalog = mutableListOf<MediaItem>()
                for (track in tempQueue){
                    var yt = YTExtractor(context)
                    yt.extract(track.videoId)
                    if (yt.state == State.SUCCESS){
                            var tempArtist = mutableListOf<String>()
                            if (track.artists != null){
                                for (artist in track.artists) {
                                    tempArtist.add(artist.name)
                                }
                            }
                            var artistName: String = connectArtists(tempArtist)
                            tempCatalog.add(MediaItem.Builder()
                                .setUri(yt.getYTFiles()?.getAudioOnly()?.bestQuality()?.url)
                                .setMediaId(track.videoId)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setArtworkUri(track.thumbnails?.last()?.url?.toUri())
                                        .setAlbumTitle(track.album?.name)
                                        .setTitle(track.title)
                                        .setArtist(artistName)
                                        .build()
                                )
                                .build())
                            Log.d("MusicSource", "updateCatalog: ${track.title}")
                    }
                }
            return tempCatalog
        }
    }
    fun connectArtists(artists: List<String>): String {
        val stringBuilder = StringBuilder()

        for ((index, artist) in artists.withIndex()) {
            stringBuilder.append(artist)

            if (index < artists.size - 1) {
                stringBuilder.append(", ")
            }
        }

        return stringBuilder.toString()
    }
}

enum class StateSource {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}

