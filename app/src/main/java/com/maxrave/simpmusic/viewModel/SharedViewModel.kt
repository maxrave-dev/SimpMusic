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
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.maxrave.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.Config.ALBUM_CLICK
import com.maxrave.simpmusic.common.Config.PLAYLIST_CLICK
import com.maxrave.simpmusic.common.Config.RECOVER_TRACK_QUEUE
import com.maxrave.simpmusic.common.Config.SHARE
import com.maxrave.simpmusic.common.Config.SONG_CLICK
import com.maxrave.simpmusic.common.Config.VIDEO_CLICK
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.TRUE
import com.maxrave.simpmusic.data.db.entities.FormatEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.LyricsEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
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
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.PlayerEvent
import com.maxrave.simpmusic.service.RepeatState
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.service.SimpleMediaState
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
class SharedViewModel @Inject constructor(private var dataStoreManager: DataStoreManager, @DownloadCache private val downloadedCache: SimpleCache, private val mainRepository: MainRepository, private val application: Application) : AndroidViewModel(application){
    @Inject
    lateinit var downloadUtils: DownloadUtils

    private var restoreLastPlayedTrackDone: Boolean = false

    var simpleMediaServiceHandler: SimpleMediaServiceHandler? = null

    private var _songDB: MutableLiveData<SongEntity?> = MutableLiveData()
    val songDB: LiveData<SongEntity?> = _songDB
    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked: SharedFlow<Boolean> = _liked.asSharedFlow()

    protected val context
        get() = getApplication<Application>()

    val isServiceRunning = MutableLiveData<Boolean>(false)

    private var _related = MutableStateFlow<Resource<ArrayList<Track>>?>(null)
    val related: StateFlow<Resource<ArrayList<Track>>?> = _related

    var videoId = MutableLiveData<String>()
    var from = MutableLiveData<String>()
    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    var lyricsBackground: MutableLiveData<Int> = MutableLiveData()
    private var _metadata = MutableLiveData<Resource<MetadataSong>>()
    val metadata: LiveData<Resource<MetadataSong>> = _metadata

    private var _bufferedPercentage = MutableStateFlow<Int>(0)
    val bufferedPercentage: SharedFlow<Int> = _bufferedPercentage.asSharedFlow()

    private var _progress = MutableStateFlow<Float>(0F)
    private var _progressMillis = MutableStateFlow<Long>(0L)
    val progressMillis: SharedFlow<Long> = _progressMillis.asSharedFlow()
    val progress: SharedFlow<Float> = _progress.asSharedFlow()
    private var _progressString : MutableStateFlow<String> = MutableStateFlow("00:00")
    val progressString: SharedFlow<String> = _progressString.asSharedFlow()

    private val _duration = MutableStateFlow<Long>(0L)
    val duration: SharedFlow<Long> = _duration.asSharedFlow()
    private val _uiState = MutableStateFlow<UIState>(UIState.Initial)
    val uiState = _uiState.asStateFlow()

    var isPlaying = MutableStateFlow<Boolean>(false)
    var notReady = MutableLiveData<Boolean>(true)

    var _lyrics = MutableStateFlow<Resource<Lyrics>?>(null)
//    val lyrics: LiveData<Resource<Lyrics>> = _lyrics
    private var lyricsFormat: MutableLiveData<ArrayList<Line>> = MutableLiveData()
    var lyricsFull = MutableLiveData<String>()

    private var _translateLyrics: MutableStateFlow<Lyrics?> = MutableStateFlow(null)
    val translateLyrics: StateFlow<Lyrics?> = _translateLyrics

    private var _nowPlayingMediaItem = MutableLiveData<MediaItem?>()
    val nowPlayingMediaItem: LiveData<MediaItem?> = _nowPlayingMediaItem

    private var _songTransitions = MutableStateFlow<Boolean>(false)
    val songTransitions: StateFlow<Boolean> = _songTransitions

    private var _shuffleModeEnabled = MutableStateFlow<Boolean>(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled

    private var _repeatMode = MutableStateFlow<RepeatState>(RepeatState.None)
    val repeatMode: StateFlow<RepeatState> = _repeatMode

    //SponsorBlock
    private var _skipSegments: MutableStateFlow<List<SkipSegments>?> = MutableStateFlow(null)
    val skipSegments: StateFlow<List<SkipSegments>?> = _skipSegments

    private var _sleepTimerRunning: MutableLiveData<Boolean> = MutableLiveData(false)
    val sleepTimerRunning: LiveData<Boolean> = _sleepTimerRunning

    private var watchTimeList: ArrayList<Float> = arrayListOf()


    private var regionCode: String? = null
    private var language: String? = null
    private var quality: String? = null
    var from_backup: String? = null
    private var isRestoring = MutableStateFlow(false)

    private var _format: MutableStateFlow<FormatEntity?> = MutableStateFlow(null)
    val format: SharedFlow<FormatEntity?> = _format.asSharedFlow()

    private var _saveLastPlayedSong: MutableLiveData<Boolean> = MutableLiveData()
    val saveLastPlayedSong: LiveData<Boolean> = _saveLastPlayedSong

    var recentPosition: String = 0L.toString()

    val intent: MutableStateFlow<Intent?> = MutableStateFlow(null)

    private var jobWatchtime: Job? = null

    var playlistId: MutableStateFlow<String?> = MutableStateFlow(null)
    private var initJob: Job? = null

    var loadingMore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var isFullScreen: Boolean = false
    var isSubtitle: Boolean = true

    //    init {
//        Log.w("Check SharedViewModel init", (simpleMediaServiceHandler != null).toString())
////        regionCode = runBlocking { dataStoreManager.location.first() }
////        quality = runBlocking { dataStoreManager.quality.first() }
////        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
////        val from_backup = runBlocking { dataStoreManager.playlistFromSaved.first() }
////        if (runBlocking { dataStoreManager.saveRecentSongAndQueue.first() == TRUE }) {
////            if (from_backup != null) {
////                from.postValue(from_backup)
////            }
////            recentPosition = runBlocking { (dataStoreManager.recentPosition.first()) }
////        }
//
//    }
    fun init() {
        if (simpleMediaServiceHandler != null) {
            initJob = viewModelScope.launch {
                val job1 = launch {
                    simpleMediaServiceHandler!!.simpleMediaState.collect { mediaState ->
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
                                if (_duration.value > 0){
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
                                calculateProgressValues(simpleMediaServiceHandler!!.getProgress())
                                _uiState.value = UIState.Ready
                            }
                        }
                    }
                }
                val job2 = launch {
                    simpleMediaServiceHandler!!.nowPlaying.collectLatest { nowPlaying ->
                        nowPlaying?.let { now ->
                            _format.value = null
                            getSkipSegments(now.mediaId)
                        }
                        if (nowPlaying != null && getCurrentMediaItemIndex() > 0) {
                            _nowPlayingMediaItem.postValue(nowPlaying)
                            var downloaded = false
                            val tempSong = simpleMediaServiceHandler!!.catalogMetadata.getOrNull(
                                getCurrentMediaItemIndex()
                            )
                            if (tempSong != null) {
                                Log.d("Check tempSong", tempSong.toString())
                                mainRepository.insertSong(tempSong.toSongEntity())
                                mainRepository.getSongById(tempSong.videoId)
                                    .collectLatest { songEntity ->
                                        _songDB.value = songEntity
                                        if (songEntity != null) {
                                            _liked.value = songEntity.liked
                                            simpleMediaServiceHandler!!.like(songEntity.liked)
                                            downloaded =
                                                songEntity.downloadState == DownloadState.STATE_DOWNLOADED
                                            Log.d("Check like", songEntity.toString())
                                        }
                                    }
                                mainRepository.updateSongInLibrary(
                                    LocalDateTime.now(),
                                    tempSong.videoId
                                )
                                mainRepository.updateListenCount(tempSong.videoId)
                                tempSong.durationSeconds?.let {
                                    mainRepository.updateDurationSeconds(
                                        it,
                                        tempSong.videoId
                                    )
                                }
                                videoId.postValue(tempSong.videoId)
                                _nowPlayingMediaItem.value = nowPlaying
                            }
                            val index = getCurrentMediaItemIndex() + 1
                            Log.w("Check index", index.toString())
                            val size = simpleMediaServiceHandler!!.catalogMetadata.size
                            Log.w("Check size", size.toString())
                            Log.w("Check loadingMore", loadingMore.toString())
                            if (size > 3 && size - index < 3 && size - index >= 0 && !loadingMore.first()) {
                                Log.d("Check loadMore", "loadMore")
                                loadMore()
                            }
                        }
                    }
                }
                val job3 = launch {
                    simpleMediaServiceHandler!!.shuffle.collect { shuffle ->
                        _shuffleModeEnabled.value = shuffle
                    }
                }
                val job4 = launch {
                    simpleMediaServiceHandler!!.repeat.collect { repeat ->
                        _repeatMode.value = repeat
                    }
                }
                val job6 = launch {
                    simpleMediaServiceHandler!!.liked.collect { liked ->
                        if (liked != _liked.value) {
                            videoId.value?.let { updateLikeStatus(it, liked) }
                        }
                    }
                }
                val job8 = launch {
                    duration.collect {
                        if (it > 0) {
                            getFormat(simpleMediaServiceHandler!!.nowPlaying.first()?.mediaId)
                        }
                    }
                }
                val job7 = launch {
                    format.collect {formatTemp ->
                        if (dataStoreManager.sendBackToGoogle.first() == TRUE) {
                            if (formatTemp != null) {
                                println("format in viewModel: $formatTemp")
                                initPlayback(formatTemp.playbackTrackingVideostatsPlaybackUrl, formatTemp.playbackTrackingAtrUrl, formatTemp.playbackTrackingVideostatsWatchtimeUrl, formatTemp.cpn)
                            }
                        }
                        resetLyrics()
                        Log.w("Check Youtube Captions URL", formatTemp?.youtubeCaptionsUrl.toString())
                        Log.w("Check CPN", formatTemp?.cpn.toString())
                        formatTemp?.lengthSeconds?.let {
                            getLyricsFromFormat(formatTemp.videoId, it)
                        }
                    }
                }

                job1.join()
                job2.join()
                job3.join()
                job4.join()
                job6.join()
                job7.join()
                job8.join()
            }
        }
    }

    fun loadMore() {
        val continuation = Queue.getContinuation()
        Log.w("Check loadMore", continuation.toString())
        if (continuation != null) {
            viewModelScope.launch {
                loadingMore.value = true
                Log.w("Check loadMore continuation", continuation.toString())
                mainRepository.getContinueTrack(continuation.first, continuation.second)
                    .collect { response ->
                        if (response != null) {
                            Log.w("Check loadMore response", response.toString())
                            simpleMediaServiceHandler?.loadMoreCatalog(response)
                        }
                        loadingMore.value = false
                    }
            }
        }
    }

    private fun initPlayback(playback: String?, atr: String?, watchTime: String?, cpn: String?) {
        jobWatchtime?.cancel()
        viewModelScope.launch {
            if (playback != null && atr != null && watchTime != null && cpn != null) {
                watchTimeList.clear()
                mainRepository.initPlayback(playback, atr, watchTime, cpn, playlistId.value)
                    .collect {
                        if (it.first == 204) {
                            Log.d("Check initPlayback", "Success")
                            watchTimeList.add(0f)
                        watchTimeList.add(5.54f)
                        watchTimeList.add(it.second)
                        updateWatchTime()
                    }
                }
            }
        }
    }

    private fun updateWatchTime() {
        viewModelScope.launch {
            jobWatchtime = launch {
                progressMillis.collect { value ->
                    if (value > 0 && watchTimeList.isNotEmpty()) {
                        val second = (value / 1000).toFloat()
                        if (second in watchTimeList.last()..watchTimeList.last() + 1.2f) {
                            val watchTimeUrl = _format.value?.playbackTrackingVideostatsWatchtimeUrl
                            val cpn = _format.value?.cpn
                            if (second + 20.23f < (duration.first()/1000).toFloat()) {
                                watchTimeList.add(second + 20.23f)
                                if (watchTimeUrl != null && cpn != null) {
                                    Log.w("Check updateWatchTime", _format.value?.uploader.toString())
                                    mainRepository.updateWatchTime(watchTimeUrl, watchTimeList, cpn, playlistId.value).collect { response ->
                                        if (response == 204) {
                                            Log.d("Check updateWatchTime", "Success")
                                        }
                                    }
                                }
                            }
                            else {
                                watchTimeList.clear()
                                if (watchTimeUrl != null && cpn != null) {
                                    Log.w("Check updateWatchTime", _format.value?.uploader.toString())
                                    mainRepository.updateWatchTimeFull(watchTimeUrl, cpn, playlistId.value).collect { response ->
                                        if (response == 204) {
                                            Log.d("Check updateWatchTimeFull", "Success")
                                        }
                                    }
                                }
                            }
                            Log.w("Check updateWatchTime", watchTimeList.toString())
                        }
                    }
                }
            }
            jobWatchtime?.join()
        }
    }

    fun getString(key: String): String? {
        return runBlocking { dataStoreManager.getString(key).first() }
    }

    fun putString(key: String, value: String) {
        runBlocking { dataStoreManager.putString(key, value) }
    }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerRunning.value = true
        simpleMediaServiceHandler!!.sleepStart(minutes)
    }
    fun stopSleepTimer() {
        _sleepTimerRunning.value = false
        simpleMediaServiceHandler!!.sleepStop()
    }

    fun updateLikeInNotification(liked: Boolean) {
        simpleMediaServiceHandler!!.like(liked)
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
                isRestoring.collect { it ->
                    if (it) {
                        Toast.makeText(context, context.getString(R.string.restore_success), Toast.LENGTH_SHORT).show()
                        mainRepository.getDownloadedSongs().collect { songs ->
                            songs?.forEach { song ->
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
    fun getSkipSegments(videoId: String) {
        resetSkipSegments()
        viewModelScope.launch {
            mainRepository.getSkipSegments(videoId).collect { segments ->
                if (segments != null) {
                    Log.w("Check segments ${videoId}", segments.toString())
                    _skipSegments.value = segments
                }
                else {
                    _skipSegments.value = null
                }
            }
        }
    }
    private fun resetSkipSegments() {
        _skipSegments.value = null
    }
    fun getSavedLyrics(track: Track, query: String) {
        viewModelScope.launch {
            resetLyrics()
            mainRepository.getSavedLyrics(track.videoId).collect { lyrics ->
                if (lyrics != null) {
                    _lyrics.value = Resource.Success(lyrics.toLyrics())
                    val lyricsData = lyrics.toLyrics()
                    Log.d("Check Lyrics In DB", lyricsData.toString())
                    parseLyrics(lyricsData)
                }
                else {
                    resetLyrics()
                    mainRepository.getLyricsData(query, track.durationSeconds).collect { response ->
                        _lyrics.value = response.second
                        when(_lyrics.value) {
                            is Resource.Success -> {
                                if (_lyrics.value?.data != null) {
                                    insertLyrics(_lyrics.value?.data!!.toLyricsEntity(track.videoId))
                                    parseLyrics(_lyrics.value?.data)
                                    if (dataStoreManager.enableTranslateLyric.first() == TRUE) {
                                        mainRepository.getTranslateLyrics(response.first)
                                            .collect { translate ->
                                                if (translate != null) {
                                                    _translateLyrics.value =
                                                        translate.toLyrics(_lyrics.value?.data!!)
                                                }
                                            }
                                    }
                                }
                            }
                            else -> {
                                Log.d("Check lyrics", "Loading")
                            }
                        }
                    }
                }
            }
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
        _nowPlayingMediaItem.value = simpleMediaServiceHandler?.getCurrentMediaItem()
        return simpleMediaServiceHandler?.getCurrentMediaItem()
    }

    fun getCurrentMediaItemIndex(): Int {
        return simpleMediaServiceHandler?.currentIndex() ?: 0
    }
    @UnstableApi
    fun playMediaItemInMediaSource(index: Int){
        simpleMediaServiceHandler?.playMediaItemInMediaSource(index)
    }
    @UnstableApi
    fun loadMediaItemFromTrack(track: Track, type: String, index: Int? = null) {
        quality = runBlocking { dataStoreManager.quality.first() }
        viewModelScope.launch {
            simpleMediaServiceHandler?.clearMediaItems()
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
            track.durationSeconds?.let { mainRepository.updateDurationSeconds(it, track.videoId) }
            if (songDB.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                Log.d("Check Downloaded", "Downloaded")
                var thumbUrl = track.thumbnails?.last()?.url!!
                if (thumbUrl.contains("w120")) {
                    thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
                }
                simpleMediaServiceHandler?.addMediaItem(
                    MediaItem.Builder()
                        .setUri(track.videoId)
                        .setMediaId(track.videoId)
                        .setCustomCacheKey(track.videoId)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artists.toListName().connectArtists())
                                .setArtworkUri(thumbUrl.toUri())
                                .setAlbumTitle(track.album?.name)
                                .build()
                        )
                        .build(),
                    type != RECOVER_TRACK_QUEUE
                )
                _nowPlayingMediaItem.value = getCurrentMediaItem()
                Log.d(
                    "Check MediaItem Thumbnail",
                    getCurrentMediaItem()?.mediaMetadata?.artworkUri.toString()
                )
                simpleMediaServiceHandler?.addFirstMetadata(track)
                getSavedLyrics(track, "${track.title} ${track.artists?.firstOrNull()?.name}")
            } else {
//                var itag = 0
//                when (quality) {
//                    QUALITY.items[0].toString() -> {
//                        itag = QUALITY.itags[0]
//                    }
//
//                    QUALITY.items[1].toString() -> {
//                        itag = QUALITY.itags[1]
//                    }
//                }
//                mainRepository.getStream(track.videoId, itag).collect{ stream ->
//                    if (stream != null){
//                        uri = stream
                        Log.d("Check URI", uri)
                        val artistName: String = track.artists.toListName().connectArtists()
                        var thumbUrl = track.thumbnails?.last()?.url!!
                        if (thumbUrl.contains("w120")) {
                            thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
                        }
                        Log.d("Check URI", uri)
                        simpleMediaServiceHandler?.addMediaItem(
                            MediaItem.Builder()
                                .setUri(track.videoId)
                                .setMediaId(track.videoId)
                                .setCustomCacheKey(track.videoId)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(track.title)
                                        .setArtist(artistName)
                                        .setArtworkUri(thumbUrl.toUri())
                                        .setAlbumTitle(track.album?.name)
                                        .build()
                                )
                                .build(),
                            type != RECOVER_TRACK_QUEUE
                        )
                        _nowPlayingMediaItem.value = getCurrentMediaItem()
                        Log.d(
                            "Check MediaItem Thumbnail",
                            getCurrentMediaItem()?.mediaMetadata?.artworkUri.toString()
                        )
                        simpleMediaServiceHandler?.addFirstMetadata(track)
            }
            when (type) {
                SONG_CLICK -> {
                    getRelated(track.videoId)
                }
                VIDEO_CLICK -> {
                    getRelated(track.videoId)
                }
                SHARE -> {
                    getRelated(track.videoId)
                }
                PLAYLIST_CLICK -> {
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum()
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }
                ALBUM_CLICK -> {
                    Queue.setContinuation(null)
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum()
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }
                RECOVER_TRACK_QUEUE -> {
                    if (getString(RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE) == DataStoreManager.FALSE) {
                        recentPosition = runBlocking { dataStoreManager.recentPosition.first() }
                        restoreLastPLayedTrackDone()
                        from.postValue(from_backup)
                        simpleMediaServiceHandler?.seekTo(recentPosition)
                        Log.d("Check recentPosition", recentPosition)
                        if (songDB.value?.duration != null) {
                            if (songDB.value?.duration != "" && songDB.value?.duration?.contains(":") == true) {
                                songDB.value?.duration?.split(":")?.let { split ->
                                    _duration.emit(((split[0].toInt() * 60) + split[1].toInt())*1000.toLong())
                                    Log.d("Check Duration", _duration.value.toString())
                                    calculateProgressValues(recentPosition.toLong())
                                }
                            }
                        }
                        else {
                            simpleMediaServiceHandler?.getPlayerDuration()?.let {
                                _duration.emit(it)
                                calculateProgressValues(recentPosition.toLong())
                            }
                        }
                        getSaveQueue()
                    }
                }
            }
        }
    }


    @UnstableApi
    fun onUIEvent(uiEvent: UIEvent) = viewModelScope.launch {
        when (uiEvent) {
            UIEvent.Backward -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Backward)
            UIEvent.Forward -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Forward)
            UIEvent.PlayPause -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.PlayPause)
            UIEvent.Next -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Next)
            UIEvent.Previous -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Previous)
            UIEvent.Stop -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Stop)
            is UIEvent.UpdateProgress -> {
                _progress.value = uiEvent.newProgress
                simpleMediaServiceHandler?.onPlayerEvent(
                    PlayerEvent.UpdateProgress(
                        uiEvent.newProgress
                    )
                )
            }
            UIEvent.Repeat -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Repeat)
            UIEvent.Shuffle -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Shuffle)
        }
    }
    fun formatDuration(duration: Long): String {
        val minutes: Long = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
        val seconds: Long = (TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS)
                - minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
        return String.format("%02d:%02d", minutes, seconds)
    }
    private fun calculateProgressValues(currentProgress: Long) {
        _progress.value = if (currentProgress > 0) (currentProgress.toFloat() / _duration.value) else 0f
        _progressString.value = formatDuration(currentProgress)
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
                Toast.makeText(getApplication(), application.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
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
    fun getActiveLyrics(current: Long): Int? {
        val lyricsFormat = _lyrics.value?.data?.lines
        lyricsFormat?.indices?.forEach { i ->
            val sentence = lyricsFormat[i]
            val startTimeMs = sentence.startTimeMs.toLong()

            // estimate the end time of the current sentence based on the start time of the next sentence
            val endTimeMs = if (i < lyricsFormat.size - 1) {
                lyricsFormat[i + 1].startTimeMs.toLong()
            } else {
                // if this is the last sentence, set the end time to be some default value (e.g., 1 minute after the start time)
                startTimeMs + 60000
            }
            if (current in startTimeMs..endTimeMs) {
                return i
            }
        }
        if (current in (0..(lyricsFormat?.get(0)?.startTimeMs ?: "0").toLong())) {
            return -1
        }
        return null
    }


    @UnstableApi
    override fun onCleared() {
        runBlocking {
            jobWatchtime?.cancel()
            if (from.value != null) {
                Log.d("Check from", from.value!!)
                dataStoreManager.setPlaylistFromSaved(from.value!!)
            }
            simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Stop)
        }
        Log.w("Check onCleared", "onCleared")
    }

    fun changeSongTransitionToFalse() {
        _songTransitions.value = false
    }

    fun resetLyrics() {
        _lyrics.value = (Resource.Error<Lyrics>("reset"))
        lyricsFormat.postValue(arrayListOf())
        lyricsFull.postValue("")
        _translateLyrics.value = null
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
                songs?.forEach { song ->
                    mainRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
        }
    }
    private val _songFull: MutableLiveData<YouTubeInitialPage?> = MutableLiveData()
    var songFull: LiveData<YouTubeInitialPage?> = _songFull

    fun getSongFull(videoId: String) {
        viewModelScope.launch {
            mainRepository.getFullMetadata(videoId).collect {
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
        from_backup = runBlocking { dataStoreManager.playlistFromSaved.first() }
        recentPosition = runBlocking { (dataStoreManager.recentPosition.first()) }
    }

    fun getSaveLastPlayedSong () {
        viewModelScope.launch {
            dataStoreManager.saveRecentSongAndQueue.first().let { saved ->
                Log.d("Check SaveLastPlayedSong", restoreLastPlayedTrackDone.toString())
                _saveLastPlayedSong.postValue(saved == TRUE)
            }
        }
    }
    private var _savedQueue: MutableLiveData<List<Track>> = MutableLiveData()
    val savedQueue: LiveData<List<Track>> = _savedQueue
    fun getSavedSongAndQueue() {
        viewModelScope.launch {
            dataStoreManager.recentMediaId.first().let{ mediaId ->
                mainRepository.getSongById(mediaId).collect {song ->
                    if (song != null) {
                        Queue.clear()
                        Queue.setNowPlaying(song.toTrack())
                        loadMediaItemFromTrack(song.toTrack(), RECOVER_TRACK_QUEUE)
                    }
                }
            }
        }
    }
    private fun getSaveQueue() {
        viewModelScope.launch {
            mainRepository.getSavedQueue().collect { queue ->
                Log.d("Check Queue", queue.toString())
                if (!queue.isNullOrEmpty()) {
                    _savedQueue.value = queue.first().listTrack
                }
            }
        }
    }


    fun checkAllDownloadingSongs() {
        viewModelScope.launch {
            mainRepository.getDownloadingSongs().collect {songs ->
                songs?.forEach { song ->
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

    fun checkAuth() {
        viewModelScope.launch {
            dataStoreManager.cookie.first().let { cookie ->
                if (cookie != "") {
                    YouTube.cookie = cookie
                    Log.d("Cookie", "Cookie is not empty")
                }
                else {
                    Log.e("Cookie", "Cookie is empty")
                }
            }
            dataStoreManager.musixmatchCookie.first().let { cookie ->
                if (cookie != "") {
                    YouTube.musixMatchCookie = cookie
                    Log.d("Musixmatch", "Cookie is not empty")
                }
                else {
                    Log.e("Musixmatch", "Cookie is empty")
                }
            }
        }
    }

    fun getFormat(mediaId: String?) {
        viewModelScope.launch {
            if (mediaId != null){
                mainRepository.getFormat(mediaId).collect { f ->
                    if (f != null){
                        _format.emit(f)
                    }
                    else {
                        _format.emit(null)
                    }
                }
            }
        }
    }

    fun restoreLastPLayedTrackDone() {
        putString(RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE, TRUE)
    }

    fun removeSaveQueue() {
        viewModelScope.launch {
            mainRepository.removeQueue()
        }
    }

    private var _githubResponse = MutableLiveData<GithubResponse?>()
    val githubResponse: LiveData<GithubResponse?> = _githubResponse

    fun checkForUpdate() {
        viewModelScope.launch {
            mainRepository.checkForUpdate().collect { response ->
                dataStoreManager.putString(
                    "CheckForUpdateAt",
                    System.currentTimeMillis().toString()
                )
                _githubResponse.postValue(response)
            }
        }
    }

    fun skipSegment(position: Long) {
        simpleMediaServiceHandler?.skipSegment(position)
    }
    fun sponsorBlockEnabled() = runBlocking { dataStoreManager.sponsorBlockEnabled.first() }
    fun sponsorBlockCategories() = runBlocking { dataStoreManager.getSponsorBlockCategories() }
    fun stopPlayer() {
        onUIEvent(UIEvent.Stop)
    }

    fun addToYouTubePlaylist(localPlaylistId: Long, youtubePlaylistId: String, videoId: String) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Syncing)
            mainRepository.addYouTubePlaylistItem(youtubePlaylistId, videoId).collect { response ->
                if (response == "STATUS_SUCCEEDED") {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Synced)
                    Toast.makeText(getApplication(), application.getString(R.string.added_to_youtube_playlist), Toast.LENGTH_SHORT).show()
                }
                else {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.NotSynced)
                    Toast.makeText(getApplication(), application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun addQueueToPlayer() {
        Log.d("Check Queue in viewmodel", Queue.getQueue().toString())
        simpleMediaServiceHandler?.addQueueToPlayer()
    }

    private fun loadPlaylistOrAlbum(index: Int? = null) {
        simpleMediaServiceHandler?.loadPlaylistOrAlbum(index)
    }

    fun resetRelated() {
        _related.value = null
    }
    fun getLyricsFromFormat(videoId: String, duration: Int) {
        viewModelScope.launch {
            if (dataStoreManager.lyricsProvider.first() == DataStoreManager.MUSIXMATCH) {
                mainRepository.getSongById(videoId).first().let { song ->
                    val artist = if (song?.artistName?.firstOrNull() != null && song.artistName.firstOrNull()?.contains("Various Artists") == false) {
                        song.artistName.firstOrNull()
                    } else {
                        simpleMediaServiceHandler?.nowPlaying?.first()?.mediaMetadata?.artist ?: ""
                    }
                    song?.let {
                        if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                            getSavedLyrics(
                                song.toTrack().copy(
                                    durationSeconds = duration
                                ), "${song.title} $artist"
                            )
                        } else {
                            mainRepository.getLyricsData(
                                "${song.title} $artist",
                                duration
                            ).collect { response ->
                                _lyrics.value = response.second

                                when (response.second) {
                                    is Resource.Success -> {
                                        if (response.second.data != null) {
                                            insertLyrics(response.second.data!!.toLyricsEntity(videoId))
                                            parseLyrics(response.second.data)
                                            if (dataStoreManager.enableTranslateLyric.first() == TRUE) {
                                                mainRepository.getTranslateLyrics(response.first)
                                                    .collect { translate ->
                                                        if (translate != null) {
                                                            _translateLyrics.value =
                                                                translate.toLyrics(response.second.data!!)
                                                        }
                                                    }

                                            }
                                        }
                                    }

                                    is Resource.Error -> {
                                        if (_lyrics.value?.message != "reset") {
                                            getSavedLyrics(
                                                song.toTrack().copy(
                                                    durationSeconds = duration
                                                ), "${song.title} $artist"
                                            )
                                        }
                                    }

                                }
//                        }
//                    }
                            }
                        }
                    }
                }
            }
            else if (dataStoreManager.lyricsProvider.first() == DataStoreManager.YOUTUBE) {
                mainRepository.getSongById(videoId).first().let {song ->
                    mainRepository.getYouTubeCaption(videoId).collect {response ->
                        _lyrics.value = response
                        when (response) {
                            is Resource.Success -> {
                                if (response.data != null) {
                                    insertLyrics(response.data.toLyricsEntity(videoId))
                                    parseLyrics(response.data)
                                }
                            }
                            is Resource.Error -> {
                                if (_lyrics.value?.message != "reset" && song != null) {
                                    getSavedLyrics(
                                        song.toTrack().copy(
                                            durationSeconds = duration
                                        ), "${song.title} ${song.artistName?.firstOrNull() ?: simpleMediaServiceHandler?.nowPlaying?.first()?.mediaMetadata?.artist ?: ""}"
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    fun getLyricsProvier(): String {
        return runBlocking { dataStoreManager.lyricsProvider.first() }
    }

    fun setLyricsProvider(provider: String) {
        viewModelScope.launch {
            dataStoreManager.setLyricsProvider(provider)
            delay(500)
            _format.value?.let { format ->
                getLyricsFromFormat(format.videoId, format.lengthSeconds ?: 0)
            }
        }
    }

    fun updateInLibrary(videoId: String) {
        viewModelScope.launch {
            mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
        }
    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

    private var _recreateActivity: MutableLiveData<Boolean> = MutableLiveData()
    val recreateActivity: LiveData<Boolean> = _recreateActivity

    fun activityRecreate() {
        _recreateActivity.value = true
    }

    fun activityRecreateDone() {
        _recreateActivity.value = false
    }

    fun updateSubtitle(url: String?) {
        if (url != null) {
            simpleMediaServiceHandler?.updateSubtitle(url)
        }
    }

    fun addToQueue(track: Track) {
        viewModelScope.launch {
            simpleMediaServiceHandler?.loadMoreCatalog(arrayListOf(track))
            Toast.makeText(context, context.getString(R.string.added_to_queue), Toast.LENGTH_SHORT)
                .show()
        }
    }
}
sealed class UIEvent {
    data object PlayPause : UIEvent()
    data object Backward : UIEvent()
    data object Forward : UIEvent()
    data object Next : UIEvent()
    data object Previous : UIEvent()
    data object Stop : UIEvent()
    data object Shuffle : UIEvent()
    data object Repeat : UIEvent()
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