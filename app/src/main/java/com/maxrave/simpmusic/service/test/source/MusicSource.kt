package com.maxrave.simpmusic.service.test.source

import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.service.test.source.StateSource.STATE_CREATED
import com.maxrave.simpmusic.service.test.source.StateSource.STATE_ERROR
import com.maxrave.simpmusic.service.test.source.StateSource.STATE_INITIALIZED
import com.maxrave.simpmusic.service.test.source.StateSource.STATE_INITIALIZING
import com.maxrave.simpmusic.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class MusicSource @Inject constructor(val simpleMediaServiceHandler: SimpleMediaServiceHandler, private val mainRepository: MainRepository) {

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

    fun addFirstMediaItem(mediaItem: MediaItem?) {
        if (mediaItem != null){
            Log.d("MusicSource", "addFirstMediaItem: ${mediaItem.mediaId}")
        }
    }
    @UnstableApi
    fun addFirstMediaItemToIndex(mediaItem: MediaItem?, index: Int) {
        if (mediaItem != null){
            Log.d("MusicSource", "addFirstMediaItem: ${mediaItem.mediaId}")
            simpleMediaServiceHandler.moveMediaItem(0, index)
        }
    }
    fun reset() {
        _currentSongIndex.value = 0
        catalogMetadata.clear()
        downloadUrl.clear()
        state = STATE_CREATED
    }
    fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.value = index
    }

    @UnstableApi
    suspend fun load(downloaded: Int = 0) {
        updateCatalog(downloaded).let {
            state = STATE_INITIALIZED
        }
    }

    @UnstableApi
    private suspend fun updateCatalog(downloaded: Int = 0): Boolean {
        state = STATE_INITIALIZING
        val tempQueue = Queue.getQueue()
        for (i in 0 until tempQueue.size){
            val track = tempQueue[i]
            var thumbUrl = track.thumbnails?.last()?.url ?: "http://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
            if (thumbUrl.contains("w120")){
                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
            }
            if (downloaded == 1) {
                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.videoId)
                    .setUri(track.videoId.toUri())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setArtworkUri(thumbUrl.toUri())
                            .setAlbumTitle(track.album?.name)
                            .setTitle(track.title)
                            .setArtist(track.artists.toListName().connectArtists())
                            .build()
                    )
                    .build()
                simpleMediaServiceHandler.addMediaItemNotSet(mediaItem)
                catalogMetadata.add(track)
                Log.d("MusicSource", "updateCatalog: ${track.title}, ${catalogMetadata.size}")
                downloadUrl.add(" ")
                added.value = true
            }
            else {
                mainRepository.getSong(track.videoId).collect { values ->
                    Log.d("MusicSource", "updateCatalog: $values")
                    when (values) {
                        is Resource.Success -> {
                            val listAudioStream = values.data
                            listAudioStream?.forEach {
                                if (it.itag == 251) {
                                    val uri = it.url
                                    val artistName: String =
                                        track.artists.toListName().connectArtists()
                                    Log.d("Check URI", uri)
                                    simpleMediaServiceHandler.addMediaItemNotSet(MediaItem.Builder().setUri(uri)
                                        .setMediaId(track.videoId)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(track.title)
                                                .setArtist(artistName)
                                                .setArtworkUri(thumbUrl.toUri())
                                                .setAlbumTitle(track.album?.name)
                                                .build()
                                        )
                                        .build())
                                    catalogMetadata.add(track)
                                    Log.d(
                                        "MusicSource",
                                        "updateCatalog: ${track.title}, ${catalogMetadata.size}"
                                    )
                                    downloadUrl.add(uri)
                                    added.value = true
                                    Log.d("MusicSource", "updateCatalog: ${track.title}")
                                }
                            }
                        }
                        is Resource.Error -> {
                            Log.d("MusicSource", "updateCatalog: ${values.message}")
                        }
                    }
                }
            }
        }
        return true
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

