package com.maxrave.simpmusic.service.test.source

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.request.ImageRequest
import com.maxrave.kotlinyoutubeextractor.State
import com.maxrave.kotlinyoutubeextractor.YTExtractor
import com.maxrave.kotlinyoutubeextractor.bestQuality
import com.maxrave.kotlinyoutubeextractor.getAudioOnly
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.service.test.source.StateSource.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class MusicSource @Inject constructor(val context: Context, val simpleMediaServiceHandler: SimpleMediaServiceHandler) {

    var catalog: ArrayList<MediaItem> = arrayListOf()

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var _stateFlow = MutableStateFlow<StateSource>(STATE_CREATED)
    val stateFlow = _stateFlow.asStateFlow()
    private var _currentSongIndex = MutableStateFlow<Int>(0)
    val currentSongIndex = _currentSongIndex.asStateFlow()

    var state: StateSource = STATE_CREATED
        set(value) {
            if(value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    _stateFlow.value = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                _stateFlow.value = value
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
    fun addFirstMediaItem(mediaItem: MediaItem?) {
        if (mediaItem != null){
            Log.d("MusicSource", "addFirstMediaItem: ${mediaItem.mediaId}")
            catalog.add(0, mediaItem)
        }
    }
    fun reset() {
        _currentSongIndex.value = 0
        catalog.clear()
        state = STATE_CREATED
    }
    fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.value = index
    }

    @UnstableApi
    suspend fun load() {
        updateCatalog()?.let { updatedCatalog ->
            catalog.addAll(updatedCatalog)
            Log.d("MusicSource", "load: ${catalog.size}")
            state = STATE_INITIALIZED
        } ?: run {
//            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    @UnstableApi
    private suspend fun updateCatalog(): ArrayList<MediaItem>? {
        if (Queue.getQueue() == null) return null
        else {
            val tempQueue = Queue.getQueue()
            val tempCatalog = arrayListOf<MediaItem>()
                for (i in 0 until tempQueue.size){
                    val track = tempQueue[i]
                    val yt = YTExtractor(context)
                    yt.extract(track.videoId)
                    if (yt.state == State.SUCCESS){
                            val tempArtist = mutableListOf<String>()
                            if (track.artists != null){
                                for (artist in track.artists) {
                                    tempArtist.add(artist.name)
                                }
                            }
                            val artistName: String = connectArtists(tempArtist)
                            var thumbUrl = track.thumbnails?.last()?.url!!
                            if (thumbUrl.contains("w120")){
                                thumbUrl = Regex("(w|h)120").replace(thumbUrl, "$1544")
                            }
                            val mediaItem = MediaItem.Builder()
                                .setUri(yt.getYTFiles()?.getAudioOnly()?.bestQuality()?.url)
                                .setMediaId(track.videoId)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setArtworkUri(thumbUrl.toUri())
                                        .setAlbumTitle(track.album?.name)
                                        .setTitle(track.title)
                                        .setArtist(artistName)
                                        .build()
                                )
                                .build()
                            simpleMediaServiceHandler.addMediaItemNotSet(mediaItem)
                            tempCatalog.add(mediaItem)
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

