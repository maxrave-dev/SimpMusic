package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import com.maxrave.simpmusic.data.model.mediaService.Song
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.di.YoutubeModule
import com.maxrave.simpmusic.service.PlayerEvent
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.service.SimpleMediaState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
@UnstableApi
class NowPlayingDialogViewModel @Inject constructor( private val simpleMediaServiceHandler: SimpleMediaServiceHandler, application: Application): AndroidViewModel(application) {

    var duration = MutableLiveData<Long>(0)
    var progress = MutableLiveData<Float>(0.0F)
    var progressString = MutableLiveData<String>("00:00")
    var isPlaying = MutableLiveData<Boolean>(false)

    var currentSong = MutableLiveData<Song>()

    var songResult = MutableLiveData<String>()


    private val _uiState = MutableStateFlow<UIState>(UIState.Initial)
    val uiState = _uiState.asStateFlow()



    init {
        viewModelScope.launch {
            loadMediaSource(songResult.value!!)

            simpleMediaServiceHandler.simpleMediaState.collect { mediaState ->
                when (mediaState) {
                    is SimpleMediaState.Buffering -> calculateProgressValues(mediaState.progress)
                    SimpleMediaState.Initial -> _uiState.value = UIState.Initial
                    is SimpleMediaState.Playing -> isPlaying.value = mediaState.isPlaying
                    is SimpleMediaState.Progress -> calculateProgressValues(mediaState.progress)
                    is SimpleMediaState.Ready -> {
                        duration.postValue(mediaState.duration)
                        _uiState.value = UIState.Ready
                    }
                }
            }
        }
    }

    private fun loadMediaSource(song: String): String {
        val url = getStreamLink(song)
        return url
    }

    private fun loadData(song: String) {
        val mediaItem = MediaItem.Builder()
            .setUri(getStreamLink(song))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setFolderType(MediaMetadata.FOLDER_TYPE_ALBUMS)
                    .build()
            ).build()
        simpleMediaServiceHandler.addMediaItem(mediaItem)
    }

    fun onUIEvent(uiEvent: UIEvent) = viewModelScope.launch {
        when (uiEvent) {
            UIEvent.Backward -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Backward)
            UIEvent.Forward -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Forward)
            UIEvent.PlayPause -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.PlayPause)
            is UIEvent.UpdateProgress -> {
                progress.value = uiEvent.newProgress
                simpleMediaServiceHandler.onPlayerEvent(
                    PlayerEvent.UpdateProgress(
                        uiEvent.newProgress
                    )
                )
            }
        }
    }


    fun formatDuration(duration: Long): String {
        val minutes: Long = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
        val seconds: Long = (TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)
                - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun calculateProgressValues(currentProgress: Long) {
        progress.value = if (currentProgress > 0) (currentProgress.toFloat() / duration.value!!) else 0f
        progressString.value = formatDuration(currentProgress)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("NowPlayingDialogViewModel", "onCleared")
    }
    sealed class UIEvent {
        object PlayPause : UIEvent()
        object Backward : UIEvent()
        object Forward : UIEvent()
        data class UpdateProgress(val newProgress: Float) : UIEvent()
    }

    sealed class UIState {
        object Initial : UIState()
        object Ready : UIState()
    }
    private fun getStreamLink(videoId: String): String{
        val yt = YoutubeModule(videoId)
        return yt.Request().bestAudioFormat().url()
    }
}