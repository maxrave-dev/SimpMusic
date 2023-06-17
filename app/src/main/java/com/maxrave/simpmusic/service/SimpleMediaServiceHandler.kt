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

    private val _shuffle = MutableStateFlow<Boolean>(false)
    val shuffle = _shuffle.asStateFlow()

    private val _repeat = MutableStateFlow<RepeatState>(RepeatState.None)
    val repeat = _repeat.asStateFlow()

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
        Log.i("Check song index", "${player.currentMediaItemIndex}")
    }

    fun addMediaItem(mediaItem: MediaItem) {
        player.clearMediaItems()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun addMediaItemNotSet(mediaItem: MediaItem) {
        player.addMediaItem(mediaItem)
        if (player.mediaItemCount == 1){
            player.prepare()
            player.playWhenReady = true
        }
    }

    fun clearMediaItems() {
        player.clearMediaItems()
    }

    fun addMediaItemList(mediaItemList: List<MediaItem>) {
        player.addMediaItems(mediaItemList)
        player.prepare()
    }

    fun playMediaItemInMediaSource(index: Int){
//        player.moveMediaItems(index, player.mediaItemCount - 1, 0)
//        player.prepare()
//        player.playWhenReady = true
        player.seekTo(index, 0)
        player.prepare()
        player.playWhenReady = true
    }

    fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        player.moveMediaItems(fromIndex, toIndex, 0)
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
            PlayerEvent.Stop -> {stopProgressUpdate()
            player.stop()}
            is PlayerEvent.UpdateProgress -> player.seekTo((player.duration * playerEvent.newProgress/100).toLong())
            PlayerEvent.Shuffle -> {
                if (player.shuffleModeEnabled) {
                    player.shuffleModeEnabled = false
                    _shuffle.value = false
                } else {
                    player.shuffleModeEnabled = true
                    _shuffle.value = true
                }
            }
            PlayerEvent.Repeat -> {
                when (player.repeatMode) {
                    ExoPlayer.REPEAT_MODE_OFF -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                        _repeat.value = RepeatState.One
                    }
                    ExoPlayer.REPEAT_MODE_ONE -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                        _repeat.value = RepeatState.All
                    }
                    ExoPlayer.REPEAT_MODE_ALL -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                        _repeat.value = RepeatState.None
                    }
                    else -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                        _repeat.value = RepeatState.None
                    }
                }
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        _changeTrack.value = true
        Log.d("Change Track", "onMediaItemTransition: ${changeTrack.value}")
        Log.d("Media Item Transition", "Media Item: $mediaItem")
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

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        when (shuffleModeEnabled){
            true -> _shuffle.value = true
            false -> _shuffle.value = false
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        when (repeatMode){
            ExoPlayer.REPEAT_MODE_OFF -> _repeat.value = RepeatState.None
            ExoPlayer.REPEAT_MODE_ONE -> _repeat.value = RepeatState.One
            ExoPlayer.REPEAT_MODE_ALL -> _repeat.value = RepeatState.All
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
    fun currentIndex(): Int {
        return player.currentMediaItemIndex
    }

    fun mediaListSize(): Int {
        return player.mediaItemCount
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



}

sealed class RepeatState {
    object None : RepeatState()
    object All : RepeatState()
    object One : RepeatState()
}

sealed class PlayerEvent {
    object PlayPause : PlayerEvent()
    object Backward : PlayerEvent()
    object Forward : PlayerEvent()
    object Stop : PlayerEvent()
    object Next : PlayerEvent()
    object Previous : PlayerEvent()
    object Shuffle : PlayerEvent()
    object Repeat : PlayerEvent()
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
