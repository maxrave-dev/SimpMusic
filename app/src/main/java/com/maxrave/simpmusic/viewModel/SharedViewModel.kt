package com.maxrave.simpmusic.viewModel


import android.app.Application
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import com.maxrave.kotlinytmusicscraper.models.response.PipedResponse
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.TRUE
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.LyricsEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.metadata.Line
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.di.DownloadCache
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toLyrics
import com.maxrave.simpmusic.extension.toLyricsEntity
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.service.PlayerEvent
import com.maxrave.simpmusic.service.RepeatState
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.service.SimpleMediaState
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.service.test.source.MusicSource
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
@UnstableApi
class SharedViewModel @Inject constructor(private var dataStoreManager: DataStoreManager, @DownloadCache private val downloadedCache: SimpleCache, private val musicSource: MusicSource, private val mainRepository: MainRepository, private val simpleMediaServiceHandler: SimpleMediaServiceHandler, application: Application) : AndroidViewModel(application){
    @Inject
    lateinit var downloadUtils: DownloadUtils

    private var _allSongsDB: MutableLiveData<List<SongEntity>> = MutableLiveData()
    val allSongsDB: LiveData<List<SongEntity>> = _allSongsDB

    private var _songDB: MutableLiveData<SongEntity> = MutableLiveData()
    val songDB: LiveData<SongEntity> = _songDB
    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked: StateFlow<Boolean> = _liked

    var _firstTrackAdded: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val firstTrackAdded: StateFlow<Boolean> = _firstTrackAdded

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

    var _lyrics = MutableLiveData<Resource<Lyrics>>()
//    val lyrics: LiveData<Resource<Lyrics>> = _lyrics
    private var lyricsFormat: MutableLiveData<ArrayList<Line>> = MutableLiveData()
    var lyricsFull = MutableLiveData<String>()

    val playbackState = simpleMediaServiceHandler.simpleMediaState

    private var _nowPlayingMediaItem = MutableLiveData<MediaItem?>()
    val nowPlayingMediaItem: LiveData<MediaItem?> = _nowPlayingMediaItem

    private var _songTransitions = MutableStateFlow<Boolean>(false)
    val songTransitions: StateFlow<Boolean> = _songTransitions

    private var _nextTrackAvailable = MutableStateFlow<Boolean>(false)
    val nextTrackAvailable: StateFlow<Boolean> = _nextTrackAvailable

    private var _previousTrackAvailable = MutableStateFlow<Boolean>(false)
    val previousTrackAvailable: StateFlow<Boolean> = _previousTrackAvailable

    private var _shuffleModeEnabled = MutableStateFlow<Boolean>(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled

    private var _repeatMode = MutableStateFlow<RepeatState>(RepeatState.None)
    val repeatMode: StateFlow<RepeatState> = _repeatMode

    private var regionCode: String? = null
    private var language: String? = null
    private var quality: String? = null
    private var isRestoring = MutableStateFlow(false)

    val intent: MutableStateFlow<Intent?> = MutableStateFlow(null)

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        quality = runBlocking { dataStoreManager.quality.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
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
                            _duration.value = mediaState.duration
                        }
                        is SimpleMediaState.Ready -> {
                            notReady.value = false
                            _duration.value = mediaState.duration
                            _uiState.value = UIState.Ready
                        }
                    }
                }
            }
            val job2 = launch {
                simpleMediaServiceHandler.changeTrack.collectLatest { isChanged ->
                    val song = getCurrentMediaItem()
                    if (song != null && getCurrentMediaItemIndex() > 0) {
                        val tempSong = musicSource.catalogMetadata[getCurrentMediaItemIndex()]
                        Log.d("Check tempSong", tempSong.toString())
                        mainRepository.insertSong(tempSong.toSongEntity())
                        mainRepository.getSongById(tempSong.videoId)
                            .collectLatest { songEntity ->
                                _songDB.value = songEntity
                                if (songEntity != null) {
                                    _liked.value = songEntity.liked
                                    Log.d("Check like", songEntity.toString())
                                }
                            }
                        mainRepository.updateSongInLibrary(LocalDateTime.now(), tempSong.videoId)
                        mainRepository.updateListenCount(tempSong.videoId)
                        videoId.postValue(tempSong.videoId)
                        _nowPlayingMediaItem.value = song
                        _songTransitions.value = true
                        resetLyrics()
                        mainRepository.getLyricsData("${tempSong.title} ${tempSong.artists?.firstOrNull()?.name}").collect { response ->
                            _lyrics.value = response
                            when(_lyrics.value) {
                                is Resource.Success -> {
                                    if (_lyrics.value?.data != null) {
                                        insertLyrics(_lyrics.value?.data!!.toLyricsEntity(song.mediaId))
                                        parseLyrics(_lyrics.value?.data)
                                    }
                                }
                                is Resource.Error -> {
                                    if (_lyrics.value?.message != "reset") {
                                        getSavedLyrics(tempSong.videoId)
                                    }
                                }

                                else -> {
                                    Log.d("Check lyrics", "Loading")
                                }
                            }
                        }
                    }
                    Log.d("Check Change Track", "Change Track: $isChanged")
                }
            }
            val job3 = launch {
                simpleMediaServiceHandler.shuffle.collect { shuffle ->
                    _shuffleModeEnabled.value = shuffle
                }
            }
            val job4 = launch {
                simpleMediaServiceHandler.repeat.collect { repeat ->
                    _repeatMode.value = repeat
                }
            }
            val job5 = launch {
                simpleMediaServiceHandler.nextTrackAvailable.collect {  available ->
                    _nextTrackAvailable.value = available
                }
            }
            val job6 = launch {
                simpleMediaServiceHandler.previousTrackAvailable.collect { available ->
                    _previousTrackAvailable.value = available
                }
            }

            job1.join()
            job2.join()
            job3.join()
            job4.join()
            job5.join()
            job6.join()
        }
    }

    fun getString(key: String): String? {
        return runBlocking { dataStoreManager.getString(key).first() }
    }

    fun putString(key: String, value: String) {
        runBlocking { dataStoreManager.putString(key, value) }
    }

    private var _downloadState: MutableStateFlow<Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<Download?> = _downloadState.asStateFlow()

    fun getDownloadStateFromService(videoId: String) {
        viewModelScope.launch {
            downloadState = downloadUtils.getDownload(videoId).stateIn(viewModelScope)
            downloadState.collect { down ->
                if (down != null) {
                    when (down.state) {
                        Download.STATE_COMPLETED -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song?.downloadState != DownloadState.STATE_DOWNLOADED) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADED)
                                }
                            }
                            Log.d("Check Downloaded", "Downloaded")
                        }
                        Download.STATE_FAILED -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song?.downloadState != DownloadState.STATE_NOT_DOWNLOADED) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_NOT_DOWNLOADED)
                                }
                            }
                            Log.d("Check Downloaded", "Failed")
                        }
                        Download.STATE_DOWNLOADING -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song?.downloadState != DownloadState.STATE_DOWNLOADING) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADING)
                                }
                            }
                            Log.d("Check Downloaded", "Downloading ${down.percentDownloaded}")
                        }
                        else -> {
                            Log.d("Check Downloaded", "${down.state}")
                        }
                    }
                }
            }
        }
    }

    fun checkIsRestoring() {
        viewModelScope.launch {
            dataStoreManager.isRestoringDatabase.first().let { restoring ->
                isRestoring.value = restoring == TRUE
                isRestoring.collect() { it ->
                    if (it) {
                        mainRepository.getDownloadedSongs().collect { songs ->
                            songs.forEach { song ->
                                if (!downloadedCache.keys.contains(song.videoId)) {
                                    mainRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                dataStoreManager.restore(false)
                                isRestoring.value = false
                            }
                        }
                    }
                }
            }
        }
    }
    fun insertLyrics(lyrics: LyricsEntity) {
        viewModelScope.launch {
            mainRepository.insertLyrics(lyrics)
        }
    }
    fun getSavedLyrics(videoId: String) {
        viewModelScope.launch {
            mainRepository.getSavedLyrics(videoId).collect { lyrics ->
                if (lyrics != null) {
                    _lyrics.value = Resource.Success(lyrics.toLyrics())
                    val lyricsData = lyrics.toLyrics()
                    Log.d("Check Lyrics In DB", lyricsData.toString())
                    parseLyrics(lyricsData)
                }
            }
        }
    }
    fun getLyrics(query: String) {
        viewModelScope.launch {

        }
    }

    fun getRelated(videoId: String){
        Queue.clear()
        viewModelScope.launch {
            mainRepository.getRelatedData(videoId).collect{ response ->
                _related.value = response
            }
        }
    }
    fun getCurrentMediaItem(): MediaItem? {
        _nowPlayingMediaItem.value = simpleMediaServiceHandler.getCurrentMediaItem()
        return simpleMediaServiceHandler.getCurrentMediaItem()
    }

    fun getCurrentMediaItemIndex(): Int {
        return simpleMediaServiceHandler.currentIndex()
    }
    @UnstableApi
    fun playMediaItemInMediaSource(index: Int){
        simpleMediaServiceHandler.playMediaItemInMediaSource(index)
    }
    @UnstableApi
    fun loadMediaItemFromTrack(track: Track) {
        quality = runBlocking { dataStoreManager.quality.first() }
        viewModelScope.launch {
            _firstTrackAdded.value = false
            simpleMediaServiceHandler.clearMediaItems()
            var uri = ""
            mainRepository.insertSong(track.toSongEntity())
            mainRepository.getSongById(track.videoId)
                .collect { songEntity ->
                    _songDB.value = songEntity
                    if (songEntity != null) {
                        _liked.value = songEntity.liked
                    }
                }
            mainRepository.updateSongInLibrary(LocalDateTime.now(), track.videoId)
            mainRepository.updateListenCount(track.videoId)
            if (songDB.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                Log.d("Check Downloaded", "Downloaded")
                musicSource.downloadUrl.add(0, "")
                var thumbUrl = track.thumbnails?.last()?.url!!
                if (thumbUrl.contains("w120")) {
                    thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
                }
                simpleMediaServiceHandler.addMediaItem(
                    MediaItem.Builder()
                        .setUri(track.videoId.toUri())
                        .setMediaId(track.videoId)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artists.toListName().connectArtists())
                                .setArtworkUri(thumbUrl.toUri())
                                .setAlbumTitle(track.album?.name)
                                .build()
                        )
                        .build()
                )
                _nowPlayingMediaItem.value = getCurrentMediaItem()
                Log.d(
                    "Check MediaItem Thumbnail",
                    getCurrentMediaItem()?.mediaMetadata?.artworkUri.toString()
                )
                simpleMediaServiceHandler.changeTrackToFalse()
                _firstTrackAdded.value = true
                musicSource.addFirstMetadata(track)
            } else {
                var itag = 0
                when (quality) {
                    QUALITY.items[0].toString() -> {
                        itag = QUALITY.itags[0]
                    }

                    QUALITY.items[1].toString() -> {
                        itag = QUALITY.itags[1]
                    }
                }
                mainRepository.getStream(track.videoId, itag).collect{
                    if (it != null){
                        uri = it
                        val artistName: String = track.artists.toListName().connectArtists()
                        var thumbUrl = track.thumbnails?.last()?.url!!
                        if (thumbUrl.contains("w120")) {
                            thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
                        }
                        Log.d("Check URI", uri)
                        musicSource.downloadUrl.add(0, uri)
                        simpleMediaServiceHandler.addMediaItem(
                            MediaItem.Builder().setUri(uri)
                                .setMediaId(track.videoId)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(track.title)
                                        .setArtist(artistName)
                                        .setArtworkUri(thumbUrl.toUri())
                                        .setAlbumTitle(track.album?.name)
                                        .build()
                                )
                                .build()
                        )
                        _nowPlayingMediaItem.value = getCurrentMediaItem()
                        Log.d(
                            "Check MediaItem Thumbnail",
                            getCurrentMediaItem()?.mediaMetadata?.artworkUri.toString()
                        )
                        simpleMediaServiceHandler.changeTrackToFalse()
                        _firstTrackAdded.value = true
                        musicSource.addFirstMetadata(track)
                    }
                }
            }
            resetLyrics()
            mainRepository.getLyricsData("${track.title} ${track.artists?.firstOrNull()?.name}").collect { response ->
                _lyrics.value = response
                when(_lyrics.value) {
                    is Resource.Success -> {
                        if (_lyrics.value?.data != null) {
                            insertLyrics(_lyrics.value?.data!!.toLyricsEntity(track.videoId))
                            parseLyrics(_lyrics.value?.data)
                        }
                    }
                    is Resource.Error -> {
                        if (_lyrics.value?.message != "reset") {
                            getSavedLyrics(track.videoId)
                        }
                    }

                    else -> {
                        Log.d("Check lyrics", "Loading")
                    }
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
            UIEvent.Stop -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Stop)
            is UIEvent.UpdateProgress -> {
                _progress.value = uiEvent.newProgress
                simpleMediaServiceHandler.onPlayerEvent(
                    PlayerEvent.UpdateProgress(
                        uiEvent.newProgress
                    )
                )
            }
            UIEvent.Repeat -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Repeat)
            UIEvent.Shuffle -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Shuffle)
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

    private var _listLocalPlaylist: MutableLiveData<List<LocalPlaylistEntity>> = MutableLiveData()
    val localPlaylist: LiveData<List<LocalPlaylistEntity>> = _listLocalPlaylist
    fun getAllLocalPlaylist() {
        viewModelScope.launch {
            mainRepository.getAllLocalPlaylists().collect { values ->
                _listLocalPlaylist.postValue(values)
            }
        }
    }
    fun updateLocalPlaylistTracks(list: List<String>, id: Long) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(list).collect { values ->
                var count = 0
                values.forEach { song ->
                    if (song.downloadState == DownloadState.STATE_DOWNLOADED){
                        count++
                    }
                }
                mainRepository.updateLocalPlaylistTracks(list, id)
                Toast.makeText(getApplication(), "Added to playlist", Toast.LENGTH_SHORT).show()
                if (count == values.size) {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_DOWNLOADED, id)
                }
                else {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_NOT_DOWNLOADED, id)
                }
            }
        }
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
        return when(_lyrics.value?.data?.syncType) {
            null -> Config.SyncState.NOT_FOUND
            "LINE_SYNCED" -> Config.SyncState.LINE_SYNCED
            "UNSYNCED" -> Config.SyncState.UNSYNCED
            else -> Config.SyncState.NOT_FOUND
        }
    }

    fun getLyricsString(current: Long): LyricDict? {
        val lyricsFormat = lyricsFormat.value
        lyricsFormat?.indices?.forEach { i ->
            val sentence = lyricsFormat[i]
            val next = if (i > 1) listOf(
                lyricsFormat[i - 2].words,
                lyricsFormat[i - 1].words
            ) else if (i > 0)
                listOf(lyricsFormat[0].words) else null
            val prev = if (i < lyricsFormat.size - 2) listOf(
                lyricsFormat[i + 1].words,
                lyricsFormat[i + 2].words
            ) else if (i < lyricsFormat.size - 1)
                listOf(lyricsFormat[i + 1].words)
            else
                null
            // get the start time of the current sentence
            val startTimeMs = sentence.startTimeMs.toLong()

            // estimate the end time of the current sentence based on the start time of the next sentence
            val endTimeMs = if (i < lyricsFormat.size - 1) {
                lyricsFormat[i + 1].startTimeMs.toLong()
            } else {
                // if this is the last sentence, set the end time to be some default value (e.g., 1 minute after the start time)
                startTimeMs + 60000
            }
            if (current in startTimeMs..endTimeMs) {
                val lyric = if (sentence.words != "") sentence.words else null
                return LyricDict(lyric, prev, next)
//                Log.d("Check Lyric", listLyricDict.toString())
            }
        }
        return null
    }




    @UnstableApi
    override fun onCleared() {
        viewModelScope.launch {
            simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Stop)
        }
    }

    fun changeSongTransitionToFalse() {
        _songTransitions.value = false
        simpleMediaServiceHandler.changeTrackToFalse()
    }

    fun changeFirstTrackAddedToFalse() {
        _firstTrackAdded.value = false
    }

    fun resetLyrics() {
        _lyrics.postValue(Resource.Error<Lyrics>("reset"))
        lyricsFormat.postValue(arrayListOf())
        lyricsFull.postValue("")
    }

    fun updateLikeStatus(videoId: String, likeStatus: Boolean) {
        viewModelScope.launch{
            _liked.value = likeStatus
            if (likeStatus) {
                mainRepository.updateLikeStatus(videoId, 1)
            }
            else
            {
                mainRepository.updateLikeStatus(videoId, 0)
            }
        }
    }

    fun updateDownloadState(videoId: String, state: Int) {
        viewModelScope.launch {
            mainRepository.getSongById(videoId).collect { songEntity ->
                _songDB.value = songEntity
                if (songEntity != null) {
                    _liked.value = songEntity.liked
                }
            }
            mainRepository.updateDownloadState(videoId, state)
        }
    }

    fun refreshSongDB() {
        viewModelScope.launch {
            mainRepository.getSongById(videoId.value!!).collect { songEntity ->
                _songDB.value = songEntity
                if (songEntity != null) {
                    _liked.value = songEntity.liked
                }
            }
        }
    }

    fun changeAllDownloadingToError() {
        viewModelScope.launch {
            mainRepository.getDownloadingSongs().collect {songs ->
                songs.forEach { song ->
                    mainRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
        }
    }
    private val _songFull: MutableLiveData<PipedResponse> = MutableLiveData()
    var songFull: LiveData<PipedResponse> = _songFull

    fun getSongFull(videoId: String) {
        viewModelScope.launch {
            mainRepository.getSongFull(videoId).collect {
                _songFull.postValue(it)
            }
        }
    }

//    val _artistId: MutableLiveData<Resource<ChannelId>> = MutableLiveData()
//    var artistId: LiveData<Resource<ChannelId>> = _artistId
//    fun convertNameToId(artistId: String) {
//        viewModelScope.launch {
//            mainRepository.convertNameToId(artistId).collect {
//                _artistId.postValue(it)
//            }
//        }
//    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        quality = runBlocking { dataStoreManager.quality.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    fun stopPlayer() {
        simpleMediaServiceHandler.stopPlayer()
    }

    fun checkAllDownloadingSongs() {
        viewModelScope.launch {
            mainRepository.getDownloadingSongs().collect {songs ->
                songs.forEach { song ->
                    mainRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
            mainRepository.getPreparingSongs().collect {songs ->
                songs.forEach { song ->
                    mainRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
        }
    }
}
sealed class UIEvent {
    object PlayPause : UIEvent()
    object Backward : UIEvent()
    object Forward : UIEvent()
    object Next : UIEvent()
    object Previous : UIEvent()
    object Stop : UIEvent()
    object Shuffle : UIEvent()
    object Repeat : UIEvent()
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