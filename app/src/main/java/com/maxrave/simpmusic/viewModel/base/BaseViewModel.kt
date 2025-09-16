package com.maxrave.simpmusic.viewModel.base

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.common.R
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.logger.Logger
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseViewModel(
    private val application: Application,
) : AndroidViewModel(application),
    KoinComponent {
    protected val mediaPlayerHandler: MediaPlayerHandler by inject<MediaPlayerHandler>()
    private val _nowPlayingVideoId: MutableStateFlow<String> = MutableStateFlow("")

    /**
     * Get now playing video id
     * If empty, no video is playing
     */
    val nowPlayingVideoId: StateFlow<String> get() = _nowPlayingVideoId

    /**
     * Tag for logging
     */
    protected val tag: String = javaClass.simpleName

    /**
     * Log with viewModel tag
     */
    protected fun log(
        message: String,
        logType: Int = Log.WARN,
    ) {
        when (logType) {
            Log.DEBUG -> Logger.d(tag, message)
            Log.INFO -> Logger.i(tag, message)
            Log.WARN -> Logger.w(tag, message)
            Log.ERROR -> Logger.e(tag, message)
            else -> Logger.d(tag, message)
        }
    }

    /**
     * Cancel all jobs
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
        log("ViewModel cleared", Log.WARN)
    }

    init {
        getNowPlayingVideoId()
    }

    protected fun makeToast(message: String?) {
        Toast.makeText(application, message ?: "NO MESSAGE", Toast.LENGTH_SHORT).show()
    }

    protected fun getString(resId: Int): String = application.getString(resId)

    // Loading dialog
    private val _showLoadingDialog: MutableStateFlow<Pair<Boolean, String>> = MutableStateFlow(false to getString(R.string.loading))
    val showLoadingDialog: StateFlow<Pair<Boolean, String>> get() = _showLoadingDialog

    fun showLoadingDialog(message: String? = null) {
        _showLoadingDialog.value = true to (message ?: getString(R.string.loading))
    }

    fun hideLoadingDialog() {
        _showLoadingDialog.value = false to getString(R.string.loading)
    }

    private fun getNowPlayingVideoId() {
        viewModelScope.launch {
            combine(mediaPlayerHandler.nowPlayingState, mediaPlayerHandler.controlState) { nowPlayingState, controlState ->
                Pair(nowPlayingState, controlState)
            }.collect { (nowPlayingState, controlState) ->
                if (controlState.isPlaying) {
                    _nowPlayingVideoId.value = nowPlayingState.songEntity?.videoId ?: ""
                } else {
                    _nowPlayingVideoId.value = ""
                }
            }
        }
    }

    /**
     * Communicate with SimpleMediaServiceHandler to load media item
     */
    fun setQueueData(queueData: QueueData.Data) {
        mediaPlayerHandler.reset()
        mediaPlayerHandler.setQueueData(queueData)
    }

    fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int? = null,
    ) {
        viewModelScope.launch {
            mediaPlayerHandler.loadMediaItem(
                anyTrack = anyTrack,
                type = type,
                index = index,
            )
        }
    }

    fun shufflePlaylist(firstPlayIndex: Int = 0) {
        mediaPlayerHandler.shufflePlaylist(firstPlayIndex)
    }
}