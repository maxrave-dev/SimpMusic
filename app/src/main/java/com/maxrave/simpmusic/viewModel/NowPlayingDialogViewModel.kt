package com.maxrave.simpmusic.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.maxrave.simpmusic.di.YoutubeModule
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.FeedbackTokens
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult

class NowPlayingDialogViewModel: ViewModel() {
    var demoSong: SongsResult = SongsResult(Album("", ""), listOf(), "", "", 1213546, FeedbackTokens("", ""), false, "Songs", listOf(), "People Change", "fkNjMuvtpLU", "MV", 2023)
    fun refresh() {
        isPlaying = false
        isShuffle = false
        isRepeat = false
    }

    var isPlaying = false
    var isShuffle = false
    var isRepeat = false
    var queueSong: MutableLiveData<ArrayList<SongsResult>> = MutableLiveData()
    var currentSong: MutableLiveData<SongsResult> = MutableLiveData()
    var currentSongPosition: MutableLiveData<Float> = MutableLiveData() // 0.0 - 100.0
    fun init() {
        currentSong.value = demoSong
        currentSongPosition.value = 0.0f
    }

    fun getStreamUrl(): String {
        val yt =  YoutubeModule(currentSong.value!!.videoId)
        return yt.Request().bestAudioFormat().url()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("NowPlayingDialogViewModel", "onCleared")
    }
}