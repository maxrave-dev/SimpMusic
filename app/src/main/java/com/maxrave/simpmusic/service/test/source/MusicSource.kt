package com.maxrave.simpmusic.service.test.source

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.request.ImageRequest
import com.maxrave.kotlinyoutubeextractor.State
import com.maxrave.kotlinyoutubeextractor.YTExtractor
import com.maxrave.kotlinyoutubeextractor.bestQuality
import com.maxrave.kotlinyoutubeextractor.getAudioOnly
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.service.test.source.StateSource.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class MusicSource @Inject constructor(val context: Context, val simpleMediaServiceHandler: SimpleMediaServiceHandler) {

    var catalog: ArrayList<MediaItem> = arrayListOf()
    var catalogMetadata: ArrayList<Track> = (arrayListOf())
    var downloadUrl: ArrayList<String> = arrayListOf()

    var added: MutableStateFlow<Boolean> = MutableStateFlow(false)

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
    @UnstableApi
    fun addFirstMediaItemToIndex(mediaItem: MediaItem?, index: Int) {
        if (mediaItem != null){
            Log.d("MusicSource", "addFirstMediaItem: ${mediaItem.mediaId}")
            catalog.add(index, mediaItem)
            simpleMediaServiceHandler.moveMediaItem(0, index)
        }
    }
    fun reset() {
        _currentSongIndex.value = 0
        catalog.clear()
        catalogMetadata.clear()
        downloadUrl.clear()
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
        state = STATE_INITIALIZING
        if (Queue.getQueue() == null) return null
        else {
            val tempQueue = Queue.getQueue()
            val tempCatalog = arrayListOf<MediaItem>()
                for (i in 0 until tempQueue.size){
                    val track = tempQueue[i]
                    val yt = YTExtractor(con = context, CACHING = false, LOGGING = false)
                    yt.extract(track.videoId)
                    var retry_count = 0
                    while (yt.state == State.ERROR && retry_count < 3){
                        Log.e("Get URI", "Retry: ${retry_count}")
                        yt.extract(track.videoId)
                        retry_count++
                    }
                    if (yt.state == State.SUCCESS){
                            val artistName: String = track.artists.toListName().connectArtists()
                            var thumbUrl = track.thumbnails?.last()?.url ?: "http://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
                            if (thumbUrl.contains("w120")){
                                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
                            }
                            Log.d("Music Source URI", yt.getYTFiles()?.getAudioOnly()?.bestQuality()?.url.toString())
                            if (yt.getYTFiles()?.getAudioOnly()?.bestQuality()?.url != null){
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
                                catalogMetadata.add(track)
                                Log.d("MusicSource", "updateCatalog: ${track.title}, ${catalogMetadata.size}")
                                downloadUrl.add(yt.getYTFiles()?.getAudioOnly()?.bestQuality()?.url.toString())
                                added.value = true
                                Log.d("MusicSource", "updateCatalog: ${track.title}")
                            }
                    }
                }
//            simpleMediaServiceHandler.addMediaItemList(tempCatalog)
            return tempCatalog
        }
    }
    fun changeAddedState() {
        added.value = false
    }

    fun addFirstMetadata(it: Track) {
        added.value = true
        catalogMetadata.add(0, it)
        Log.d("MusicSource", "addFirstMetadata: ${it.title}, ${catalogMetadata.size}")
    }
}

enum class StateSource {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}

