package com.maxrave.simpmusic.viewModel


import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.SparseArray
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.YoutubeCallback
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo
import com.github.kiulian.downloader.downloader.response.Response
import com.github.kiulian.downloader.model.videos.VideoInfo
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.data.model.mediaService.Song
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.PlayerEvent
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(private val mainRepository: MainRepository, private val simpleMediaServiceHandler: SimpleMediaServiceHandler, private val ytDownloader: YoutubeDownloader, application: Application) : AndroidViewModel(application){
    protected val context
        get() = getApplication<Application>()

    val listItag = listOf(171,249,250,251,140,141,256,258)
    var videoId = MutableLiveData<String>()
    var from = MutableLiveData<String>()
    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    var lyricsBackground: MutableLiveData<Int> = MutableLiveData()
    private var _metadata = MutableLiveData<Resource<MetadataSong>>()
    val metadata: LiveData<Resource<MetadataSong>> = _metadata

    private var _progress = MutableStateFlow<Float>(0F)
    val progress: StateFlow<Float> = _progress

    private val _mediaItems = MutableLiveData<Resource<ArrayList<Song>>>()
    val mediaItems: LiveData<Resource<ArrayList<Song>>> = _mediaItems

    private val _mediaSources = MutableLiveData<Resource<String>>()
    val mediaSources: LiveData<Resource<String>> = _mediaSources

    val playbackState = simpleMediaServiceHandler.simpleMediaState

    fun getMetadata(videoId: String) {
        viewModelScope.launch {
            mainRepository.getMetadata(videoId).collect{response ->
                _metadata.value = response
            }
        }
    }
    @UnstableApi
    fun loadMediaItems(videoId: String){
        val title = metadata.value?.data?.title
        var artist = ""
        if (metadata.value?.data?.artists != null) {
            for (a in metadata.value?.data?.artists!!) {
                artist += a.name + ", "
            }
        }
        artist = removeTrailingComma(artist)
        artist = removeComma(artist)
        var uri = ""
//
//        val job = viewModelScope.launch{
//            val request = RequestVideoInfo(videoId)
//                .callback(object : YoutubeCallback<VideoInfo?> {
//                    override fun onFinished(videoInfo: VideoInfo?) {
//                        println("Finished parsing")
//                    }
//
//                    override fun onError(throwable: Throwable) {
//                        println("Error: " + throwable.message)
//                    }
//                })
//                .async()
//            val response: Response<VideoInfo> = ytDownloader.getVideoInfo(request)
//            val video: VideoInfo = response.data()
//            Log.d("SharedViewModel", "loadMediaItems: ${video.audioFormats().toString()}")
//            uri = video.bestAudioFormat().url()
            @SuppressLint("StaticFieldLeak")
            val request = object: YouTubeExtractor(context) {

                override fun onExtractionComplete(
                    ytFiles: SparseArray<YtFile>?,
                    videoMeta: VideoMeta?,
                ) {
                    var uri = ""
                    Log.d("Check YT data", ytFiles.toString())
                    if (ytFiles != null)
                    {
                        if (ytFiles[251] != null){
                            uri = ytFiles[251].url
                        }
                        else if (ytFiles[140] != null){
                            uri = ytFiles[140].url
                        }
                        else if (ytFiles[250] != null){
                            uri = ytFiles[249].url
                        }
                        if(uri != ""){
                            val mediaItem = MediaItem.Builder().setUri(uri)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(title)
                                        .setArtist(artist)
                                        .build()
                                )
                                .build()
                            simpleMediaServiceHandler.addMediaItem(mediaItem)
                        }
                    }
                }

            }.extract("https://youtube.com/watch?v="+videoId)


//            val mediaItem = MediaItem.Builder().setUri(uri)
//                .setMediaMetadata(
//                    MediaMetadata.Builder()
//                        .setTitle(title)
//                        .setArtist(artist)
//                        .build()
//                )
//                .build()
//            simpleMediaServiceHandler.addMediaItem(mediaItem)


    }


    @UnstableApi
    fun onUIEvent(uiEvent: UIEvent) = viewModelScope.launch {
        when (uiEvent) {
            UIEvent.Backward -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Backward)
            UIEvent.Forward -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Forward)
            UIEvent.PlayPause -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.PlayPause)
            is UIEvent.UpdateProgress -> {
                _progress.value = uiEvent.newProgress
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

    fun seekTo(position: Long) {

    }
    fun nextSong() {

    }
    fun previousSong() {

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