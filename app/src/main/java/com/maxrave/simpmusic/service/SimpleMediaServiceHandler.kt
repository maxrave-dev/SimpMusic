package com.maxrave.simpmusic.service

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.maxrave.kotlinyoutubeextractor.State
import com.maxrave.kotlinyoutubeextractor.YTExtractor
import com.maxrave.simpmusic.data.queue.Queue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@UnstableApi
class SimpleMediaServiceHandler @Inject constructor(
    private val player: ExoPlayer,
    private val context: Context
) : Player.Listener {

    private val _simpleMediaState = MutableStateFlow<SimpleMediaState>(SimpleMediaState.Initial)
    val simpleMediaState = _simpleMediaState.asStateFlow()

    private val _changeTrack = MutableStateFlow<Boolean>(false)
    val changeTrack = _changeTrack.asStateFlow()

    private var job: Job? = null

    init {
        player.addListener(this)
        job = Job()
    }
    fun addMediaSource(mediaSource: MediaSource) {
        player.setMediaSource(mediaSource)
        player.prepare()
    }
    fun addMediaSourceList(mediaSourceList: List<MediaSource>) {
        player.setMediaSources(mediaSourceList)
        player.prepare()
    }
    fun changeTrackToFalse() {
        _changeTrack.value = false
    }

    fun addMediaItem(mediaItem: MediaItem) {
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun addMediaItemList(mediaItemList: List<MediaItem>) {
        player.addMediaItems(mediaItemList)
        player.prepare()
    }

    suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            PlayerEvent.Backward -> player.seekBack()
            PlayerEvent.Forward -> player.seekForward()
            PlayerEvent.PlayPause -> {
                if (player.isPlaying) {
                    player.pause()
                    stopProgressUpdate()
                } else {
                    player.play()
                    _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = true)
                    startProgressUpdate()
                }
            }
            PlayerEvent.Next -> player.seekToNext()
            PlayerEvent.Previous -> player.seekToPrevious()
            PlayerEvent.Stop -> stopProgressUpdate()
            is PlayerEvent.UpdateProgress -> player.seekTo((player.duration * playerEvent.newProgress/100).toLong())
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        _changeTrack.value = true
        Log.d("Change Track", "onMediaItemTransition: ${changeTrack.value}")
        Log.d("Media Item Transition", "onMediaItemTransition: $mediaItem")
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SwitchIntDef")
    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_IDLE -> _simpleMediaState.value = SimpleMediaState.Initial
            ExoPlayer.STATE_ENDED -> _simpleMediaState.value = SimpleMediaState.Ended
            ExoPlayer.STATE_BUFFERING -> _simpleMediaState.value =
                SimpleMediaState.Buffering(player.currentPosition)
            ExoPlayer.STATE_READY -> _simpleMediaState.value =
                SimpleMediaState.Ready(player.duration)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = isPlaying)
        if (isPlaying) {
            GlobalScope.launch(Dispatchers.Main) {
                startProgressUpdate()
            }
        } else {
            stopProgressUpdate()
        }
    }

    private suspend fun startProgressUpdate() = job.run {
        while (true) {
            delay(100)
            _simpleMediaState.value = SimpleMediaState.Progress(player.currentPosition)
        }
    }

    private suspend fun startBufferedUpdate() = job.run {
        while (true){
            delay(500)
            _simpleMediaState.value = SimpleMediaState.Loading(player.bufferedPercentage)
        }
    }
    fun getCurrentMediaItem(): MediaItem? {
        return player.currentMediaItem
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = false)
    }
    private fun stopBufferedUpdate() {
        job?.cancel()
        _simpleMediaState.value = SimpleMediaState.Loading(player.bufferedPercentage)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onIsLoadingChanged(isLoading: Boolean) {
        super.onIsLoadingChanged(isLoading)
        _simpleMediaState.value = SimpleMediaState.Loading(player.bufferedPercentage)
        if (isLoading) {
            GlobalScope.launch(Dispatchers.Main) {
                startBufferedUpdate()
            }
        } else {
            stopBufferedUpdate()
        }
    }


    private fun removeTrailingComma(sentence: String): String {
        val trimmed = sentence.trimEnd()
        return if (trimmed.endsWith(", ")) {
            trimmed.dropLast(2)
        } else {
            trimmed
        }
    }


    private fun removeComma(string: String): String {
        return if (string.endsWith(',')) {
            string.substring(0, string.length - 1)
        } else {
            string
        }
    }
}

sealed class PlayerEvent {
    object PlayPause : PlayerEvent()
    object Backward : PlayerEvent()
    object Forward : PlayerEvent()
    object Stop : PlayerEvent()
    object Next : PlayerEvent()
    object Previous : PlayerEvent()
    data class UpdateProgress(val newProgress: Float) : PlayerEvent()
}

sealed class SimpleMediaState {
    object Initial : SimpleMediaState()
    object Ended : SimpleMediaState()
    data class Ready(val duration: Long) : SimpleMediaState()
    data class Loading(val bufferedPercentage: Int): SimpleMediaState()
    data class Progress(val progress: Long) : SimpleMediaState()
    data class Buffering(val position: Long) : SimpleMediaState()
    data class Playing(val isPlaying: Boolean) : SimpleMediaState()
}
