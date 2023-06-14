package com.maxrave.simpmusic.viewModel


import android.annotation.SuppressLint
import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.maxrave.kotlinyoutubeextractor.State
import com.maxrave.kotlinyoutubeextractor.YTExtractor
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.mediaService.Song
import com.maxrave.simpmusic.data.model.metadata.Line
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.PlayerEvent
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.service.SimpleMediaState
import com.maxrave.simpmusic.service.test.source.MusicSource
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
@UnstableApi
class SharedViewModel @Inject constructor(private val mainRepository: MainRepository, private val simpleMediaServiceHandler: SimpleMediaServiceHandler, application: Application) : AndroidViewModel(application){
    protected val context
        get() = getApplication<Application>()

    val isServiceRunning = MutableLiveData<Boolean>(false)

    private var _related = MutableLiveData<Resource<ArrayList<Track>>>()
    val related: LiveData<Resource<ArrayList<Track>>> = _related

    val listItag = listOf(171,249,250,251,140,141,256,258)
    var videoId = MutableLiveData<String>()
    var from = MutableLiveData<String>()
    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    var lyricsBackground: MutableLiveData<Int> = MutableLiveData()
    private var _metadata = MutableLiveData<Resource<MetadataSong>>()
    val metadata: LiveData<Resource<MetadataSong>> = _metadata

    private var _bufferedPercentage = MutableStateFlow<Int>(0)
    val bufferedPercentage: StateFlow<Int> = _bufferedPercentage

    private var _progress = MutableStateFlow<Float>(0F)
    private var _progressMillis = MutableStateFlow<Long>(0L)
    val progressMillis: StateFlow<Long> = _progressMillis
    val progress: StateFlow<Float> = _progress
    var progressString : MutableLiveData<String> = MutableLiveData("00:00")

    private val _duration = MutableStateFlow<Long>(0L)
    val duration: StateFlow<Long> = _duration
    private val _uiState = MutableStateFlow<UIState>(UIState.Initial)
    val uiState = _uiState.asStateFlow()

    var isPlaying = MutableLiveData<Boolean>(false)
    var notReady = MutableLiveData<Boolean>(true)

    private var _lyrics = MutableLiveData<Lyrics>()
    private var lyricsFormat: MutableLiveData<ArrayList<Line>> = MutableLiveData()
    var lyricsFull = MutableLiveData<String>()

    private val _mediaItems = MutableLiveData<Resource<ArrayList<Song>>>()
    val mediaItems: LiveData<Resource<ArrayList<Song>>> = _mediaItems

    private val _mediaSources = MutableLiveData<Resource<String>>()
    val mediaSources: LiveData<Resource<String>> = _mediaSources


    val playbackState = simpleMediaServiceHandler.simpleMediaState

    init {
        viewModelScope.launch {
            val job1 = launch {
                simpleMediaServiceHandler.simpleMediaState.collect { mediaState ->
                    when (mediaState) {
                        is SimpleMediaState.Buffering -> {
                            notReady.value = true
                        }
                        SimpleMediaState.Initial -> _uiState.value = UIState.Initial
                        SimpleMediaState.Ended -> {
                            _uiState.value = UIState.Ended
                            Log.d("Check lại videoId", videoId.value.toString())
                        }
                        is SimpleMediaState.Playing -> isPlaying.value = mediaState.isPlaying
                        is SimpleMediaState.Progress -> {
                            if (duration.value > 0){
                                calculateProgressValues(mediaState.progress)
                                _progressMillis.value = mediaState.progress
                            }
                        }
                        is SimpleMediaState.Loading -> {
                            _bufferedPercentage.value = mediaState.bufferedPercentage
                        }
                        is SimpleMediaState.Ready -> {
                            notReady.value = false
                            _duration.value = mediaState.duration
                            _uiState.value = UIState.Ready
                        }
                        else -> {}
                    }
                }
            }
            val job2 = launch {
                simpleMediaServiceHandler.changeTrack.collect { isChanged ->
                    Log.d("Check Change Track", "Change Track: $isChanged")
                    if (isChanged){
                        if (simpleMediaServiceHandler.getCurrentMediaItem()?.mediaId != videoId.value){
                            videoId.postValue(simpleMediaServiceHandler.getCurrentMediaItem()?.mediaId)
                            simpleMediaServiceHandler.changeTrackToFalse()
                        }
                        Log.d("Change Track in ViewModel", "Change Track")
                    }
                }
            }
            job1.join()
            job2.join()
        }
    }
    fun getRelated(videoId: String){
        Queue.clear()
        viewModelScope.launch {
            mainRepository.getRelated(videoId).collect{ response ->
                _related.value = response
            }
        }
    }

    fun getMetadata(videoId: String) {
        viewModelScope.launch {
            mainRepository.getMetadata(videoId).collect{response ->
                _metadata.value = response
                _lyrics.value = response.data?.lyrics
//                Log.d("Check lại Lyrics", _lyrics.value.toString())
                parseLyrics(_lyrics.value)
            }
        }
    }
    @UnstableApi
    fun addMediaItemList(song: List<MediaItem>){
        simpleMediaServiceHandler.addMediaItemList(song)
    }
    @UnstableApi
    fun loadMediaItems(videoId: String){
        val title = metadata.value?.data?.title
        viewModelScope.launch {
            var uri = ""
            val yt = YTExtractor(context)
            yt.extract(videoId)
            if (yt.state == State.SUCCESS){
                var ytFiles = yt.getYTFiles()
                if (ytFiles != null){
                    if (ytFiles[251] != null) {
                        ytFiles[251].url.let {
                            if (it != null) {
                                uri = it
                            }
                        }
                    } else if (ytFiles[171] != null) {
                        ytFiles[171].url.let {
                            if (it != null) {
                                uri = it
                            }
                        }
                    } else if (ytFiles[250] != null) {
                        ytFiles[250].url.let {
                            if (it != null) {
                                uri = it
                            }
                        }
                    } else if (ytFiles[249] != null) {
                        ytFiles[249].url.let {
                            if (it != null) {
                                uri = it
                            }
                        }
                    }
                }
                else {
                    Toast.makeText(
                        context,
                        "This track is not available in your country! Use VPN to fix this problem",
                        Toast.LENGTH_LONG
                    ).show()
                }
                if(uri != ""){
                    var artist = ""
                    Log.d("Itag", uri)
                    if (metadata.value?.data?.artists != null) {
                        for (a in metadata.value?.data?.artists!!) {
                            artist += a.name + ", "
                        }
                    }
                    artist = removeTrailingComma(artist)
                    artist = removeComma(artist)
                    Log.d("Check Title", title + " " + artist)
                    val mediaItem = MediaItem.Builder().setUri(uri)
                        .setMediaId(videoId)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(metadata.value?.data?.title)
                                .setArtist(artist)
                                .setArtworkUri(Uri.parse(metadata.value?.data?.thumbnails?.last()?.url))
                                .build()
                        )
                        .build()
                    simpleMediaServiceHandler.addMediaItem(mediaItem)
                    simpleMediaServiceHandler.changeTrackToFalse()
                }
            }
        }
    }



    @UnstableApi
    fun onUIEvent(uiEvent: UIEvent) = viewModelScope.launch {
        when (uiEvent) {
            UIEvent.Backward -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Backward)
            UIEvent.Forward -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Forward)
            UIEvent.PlayPause -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.PlayPause)
            UIEvent.Next -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Next)
            UIEvent.Previous -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Previous)
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
    private fun calculateProgressValues(currentProgress: Long) {
        _progress.value = if (currentProgress > 0) (currentProgress.toFloat() / duration.value) else 0f
        progressString.value = formatDuration(currentProgress)
    }


    @UnstableApi
    fun seekTo(position: Long) {
    }
    fun nextSong() {

    }
    fun previousSong() {

    }
    fun parseLyrics(lyrics: Lyrics?){
        if (lyrics != null){
            if (!lyrics.error){
                if (lyrics.syncType == "LINE_SYNCED")
                {
                    val firstLine = Line("0", "0", listOf(), "")
                    val lines: ArrayList<Line> = ArrayList()
                    lines.addAll(lyrics.lines as ArrayList<Line>)
                    lines.add(0, firstLine)
                    lyricsFormat.postValue(lines)
                    var txt = ""
                    for (line in lines){
                        txt += if (line == lines.last()){
                            line.words
                        } else{
                            line.words + "\n"
                        }
                    }
                    lyricsFull.postValue(txt)
//                    Log.d("Check Lyrics", lyricsFormat.value.toString())
                }
                else if (lyrics.syncType == "UNSYNCED"){
                    val lines: ArrayList<Line> = ArrayList()
                    lines.addAll(lyrics.lines as ArrayList<Line>)
                    var txt = ""
                    for (line in lines){
                        if (line == lines.last()){
                            txt += line.words
                        }
                        else{
                            txt += line.words + "\n"
                        }
                    }
                    lyricsFormat.postValue(arrayListOf(Line("0", "0", listOf(), txt)))
                    lyricsFull.postValue(txt)
                }
            }
            else {
                val lines = Line("0", "0", listOf(), "Lyrics not found")
                lyricsFormat.postValue(arrayListOf(lines))
//                Log.d("Check Lyrics", "Lyrics not found")
            }
        }
    }
    fun getLyricsSyncState(): Config.SyncState {
        return when(_lyrics.value?.syncType) {
            null -> Config.SyncState.NOT_FOUND
            "LINE_SYNCED" -> Config.SyncState.LINE_SYNCED
            "UNSYNCED" -> Config.SyncState.UNSYNCED
            else -> Config.SyncState.NOT_FOUND
        }
    }



    fun getLyricsString(current: Long): LyricDict? {
//        viewModelScope.launch {
//            while (isPlaying.value == true){
//                val lyric = lyricsFormat.value?.firstOrNull { it.startTimeMs.toLong() <= progressMillis.value!! }
//                lyricsString.postValue(lyric?.words ?: "")
//                delay(100)
//            }
//        }
//        return if (lyricsFormat.value != null){
//            val lyric = lyricsFormat.value?.firstOrNull { it.startTimeMs.toLong() <= current }
//            (lyric?.words ?: "")
//        } else {
//            ""
//        }
        var listLyricDict: LyricDict? = null
        for (i in 0 until lyricsFormat.value?.size!!) {
            val sentence = lyricsFormat.value!![i]
            val next = if (i > 1) listOf(lyricsFormat.value!![i - 2].words, lyricsFormat.value!![i - 1].words) else if (i > 0) listOf(lyricsFormat.value!![0].words) else null
            val prev = if (i < lyricsFormat.value!!.size - 2) listOf(lyricsFormat.value!![i + 1].words, lyricsFormat.value!![i + 2].words) else if (i < lyricsFormat.value!!.size - 1) listOf(lyricsFormat.value!![i + 1].words) else null
            // get the start time of the current sentence
            val startTimeMs = sentence.startTimeMs.toLong()

            // estimate the end time of the current sentence based on the start time of the next sentence
            val endTimeMs = if (i < lyricsFormat.value!!.size - 1) {
                lyricsFormat.value!![i + 1].startTimeMs.toLong()
            } else {
                // if this is the last sentence, set the end time to be some default value (e.g., 1 minute after the start time)
                startTimeMs + 60000
            }
            if (current in startTimeMs..endTimeMs) {
                val lyric = if (sentence.words != "") sentence.words else null
                listLyricDict = LyricDict(lyric, prev, next)
//                Log.d("Check Lyric", listLyricDict.toString())
                break
            }
            else {
                continue
            }
        }
        return listLyricDict
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
    @UnstableApi
    override fun onCleared() {
        viewModelScope.launch {
            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Stop)
        }
    }
}
sealed class UIEvent {
    object PlayPause : UIEvent()
    object Backward : UIEvent()
    object Forward : UIEvent()
    object Next : UIEvent()
    object Previous : UIEvent()
    data class UpdateProgress(val newProgress: Float) : UIEvent()
}

sealed class UIState {
    object Initial : UIState()
    object Ready : UIState()
    object Ended : UIState()
}

data class LyricDict(
    val nowLyric: String?,
    val nextLyric: List<String>?,
    val prevLyrics: List<String>?
)