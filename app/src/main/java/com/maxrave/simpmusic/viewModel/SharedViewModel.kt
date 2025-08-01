package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.maxrave.kotlinytmusicscraper.models.response.DownloadProgress
import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config.ALBUM_CLICK
import com.maxrave.simpmusic.common.Config.DOWNLOAD_CACHE
import com.maxrave.simpmusic.common.Config.PLAYLIST_CLICK
import com.maxrave.simpmusic.common.Config.RECOVER_TRACK_QUEUE
import com.maxrave.simpmusic.common.Config.SHARE
import com.maxrave.simpmusic.common.Config.SONG_CLICK
import com.maxrave.simpmusic.common.Config.VIDEO_CLICK
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.STATUS_DONE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.FALSE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.TRUE
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.LyricsEntity
import com.maxrave.simpmusic.data.db.entities.NewFormatEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.db.entities.SongInfoEntity
import com.maxrave.simpmusic.data.db.entities.TranslatedLyricsEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.extension.isSong
import com.maxrave.simpmusic.extension.isVideo
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toLyrics
import com.maxrave.simpmusic.extension.toLyricsEntity
import com.maxrave.simpmusic.extension.toMediaItem
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.ControlState
import com.maxrave.simpmusic.service.NowPlayingTrackState
import com.maxrave.simpmusic.service.PlayerEvent
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
import com.maxrave.simpmusic.service.RepeatState
import com.maxrave.simpmusic.service.SimpleMediaState
import com.maxrave.simpmusic.service.SleepTimerState
import com.maxrave.simpmusic.service.test.notification.NotifyWork
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import com.maxrave.spotify.model.response.spotify.CanvasResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.reflect.KClass

@UnstableApi
class SharedViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    var isFirstLiked: Boolean = false
    var isFirstMiniplayer: Boolean = false
    var isFirstSuggestions: Boolean = false
    var showedUpdateDialog: Boolean = false

    private val downloadedCache: SimpleCache by inject(qualifier = named(DOWNLOAD_CACHE))
    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked: SharedFlow<Boolean> = _liked.asSharedFlow()

    private val context
        get() = getApplication<Application>()

    var isServiceRunning: Boolean = false

    private var _sleepTimerState = MutableStateFlow(SleepTimerState(false, 0))
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState

    private var regionCode: String? = null
    private var language: String? = null
    private var quality: String? = null

    private var _format: MutableStateFlow<NewFormatEntity?> = MutableStateFlow(null)
    val format: SharedFlow<NewFormatEntity?> = _format.asSharedFlow()

    private var _canvas: MutableStateFlow<CanvasResponse?> = MutableStateFlow(null)
    val canvas: StateFlow<CanvasResponse?> = _canvas

    private var canvasJob: Job? = null

    private val _intent: MutableStateFlow<Intent?> = MutableStateFlow(null)
    val intent: StateFlow<Intent?> = _intent

    private var getFormatFlowJob: Job? = null

    var playlistId: MutableStateFlow<String?> = MutableStateFlow(null)

    var isFullScreen: Boolean = false

    private var _nowPlayingState = MutableStateFlow<NowPlayingTrackState?>(null)
    val nowPlayingState: StateFlow<NowPlayingTrackState?> = _nowPlayingState

    val blurBg: StateFlow<Boolean> =
        dataStoreManager.blurPlayerBackground
            .map { it == TRUE }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(500L),
                initialValue = false,
            )

    private var _controllerState =
        MutableStateFlow<ControlState>(
            ControlState(
                isPlaying = false,
                isShuffle = false,
                repeatState = RepeatState.None,
                isLiked = false,
                isNextAvailable = false,
                isPreviousAvailable = false,
                isCrossfading = false,
            ),
        )
    val controllerState: StateFlow<ControlState> = _controllerState
    private val _getVideo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val getVideo: StateFlow<Boolean> = _getVideo

    private var _timeline =
        MutableStateFlow<TimeLine>(
            TimeLine(
                current = -1L,
                total = -1L,
                bufferedPercent = 0,
                loading = true,
            ),
        )
    val timeline: StateFlow<TimeLine> = _timeline

    private var _nowPlayingScreenData =
        MutableStateFlow<NowPlayingScreenData>(
            NowPlayingScreenData.initial(),
        )
    val nowPlayingScreenData: StateFlow<NowPlayingScreenData> = _nowPlayingScreenData

    private var _likeStatus = MutableStateFlow<Boolean>(false)
    val likeStatus: StateFlow<Boolean> = _likeStatus

    val openAppTime: StateFlow<Int> = dataStoreManager.openAppTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)
    private val _shareSavedLyrics: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val shareSavedLyrics: StateFlow<Boolean> get() = _shareSavedLyrics

    init {
        mainRepository.initYouTube(viewModelScope)
        viewModelScope.launch {
            log("SharedViewModel init")
            if (dataStoreManager.appVersion.first() != VersionManager.getVersionName()) {
                dataStoreManager.resetOpenAppTime()
                dataStoreManager.setAppVersion(
                    VersionManager.getVersionName(),
                )
            }
            dataStoreManager.openApp()
            val timeLineJob =
                launch {
                    combine(
                        timeline.filterNotNull(),
                        nowPlayingState.filterNotNull(),
                    ) { timeline, nowPlayingState ->
                        Pair(timeline, nowPlayingState)
                    }.distinctUntilChanged { old, new ->
                        (old.first.total.toString() + old.second?.songEntity?.videoId).hashCode() ==
                            (new.first.total.toString() + new.second?.songEntity?.videoId).hashCode()
                    }.collectLatest {
                        log("Timeline job ${(it.first.total.toString() + it.second?.songEntity?.videoId).hashCode()}")
                        val nowPlaying = it.second
                        val timeline = it.first
                        if (timeline.total > 0 && nowPlaying?.songEntity != null) {
                            if (nowPlaying.mediaItem.isSong() && nowPlayingScreenData.value.canvasData == null) {
                                Log.w(tag, "Duration is ${timeline.total}")
                                Log.w(tag, "MediaId is ${nowPlaying.mediaItem.mediaId}")
                                getCanvas(nowPlaying.mediaItem.mediaId, (timeline.total / 1000).toInt())
                            }
                            nowPlaying.songEntity.let { song ->
                                if (nowPlayingScreenData.value.lyricsData == null) {
                                    Log.w(tag, "Get lyrics from format")
                                    getLyricsFromFormat(song, (timeline.total / 1000).toInt())
                                }
                            }
                        }
                    }
                }
            val checkGetVideoJob =
                launch {
                    dataStoreManager.watchVideoInsteadOfPlayingAudio.collectLatest {
                        Log.w(tag, "GetVideo is $it")
                        _getVideo.value = it == TRUE
                    }
                }
            val lyricsProviderJob =
                launch {
                    dataStoreManager.lyricsProvider.distinctUntilChanged().collectLatest {
                        setLyricsProvider()
                    }
                }
            val shareSavedLyricsJob =
                launch {
                    dataStoreManager.helpBuildLyricsDatabase.distinctUntilChanged().collectLatest {
                        _shareSavedLyrics.value = it == TRUE
                    }
                }
            timeLineJob.join()
            checkGetVideoJob.join()
            lyricsProviderJob.join()
            shareSavedLyricsJob.join()
        }

        runBlocking {
            dataStoreManager.getString("miniplayer_guide").first().let {
                isFirstMiniplayer = it != STATUS_DONE
            }
            dataStoreManager.getString("suggest_guide").first().let {
                isFirstSuggestions = it != STATUS_DONE
            }
            dataStoreManager.getString("liked_guide").first().let {
                isFirstLiked = it != STATUS_DONE
            }
        }
        viewModelScope.launch {
            simpleMediaServiceHandler.nowPlayingState
                .distinctUntilChangedBy {
                    it.songEntity?.videoId
                }.collectLatest { state ->
                    Log.w(tag, "NowPlayingState is $state")
                    canvasJob?.cancel()
                    _nowPlayingState.value = state
                    state.track?.let { track ->
                        _nowPlayingScreenData.value =
                            NowPlayingScreenData(
                                nowPlayingTitle = state.track.title,
                                artistName =
                                    state.track
                                        .artists
                                        .toListName()
                                        .joinToString(", "),
                                isVideo = false,
                                thumbnailURL = null,
                                canvasData = null,
                                lyricsData = null,
                                songInfoData = null,
                                playlistName = simpleMediaServiceHandler.queueData.value?.playlistName ?: "",
                            )
                    }
                    state.mediaItem.let { now ->
                        _canvas.value = null
                        getLikeStatus(now.mediaId)
                        getSongInfo(now.mediaId)
                        getFormat(now.mediaId)
                        _nowPlayingScreenData.update {
                            it.copy(
                                isVideo = now.isVideo(),
                            )
                        }
                    }
                    state.songEntity?.let { song ->
                        _liked.value = song.liked == true
                        _nowPlayingScreenData.update {
                            it.copy(
                                thumbnailURL = song.thumbnails,
                                isExplicit = song.isExplicit,
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            val job1 =
                launch {
                    simpleMediaServiceHandler.simpleMediaState.collect { mediaState ->
                        when (mediaState) {
                            is SimpleMediaState.Buffering -> {
                                _timeline.update {
                                    it.copy(
                                        loading = true,
                                    )
                                }
                            }

                            SimpleMediaState.Initial -> {
                                _timeline.update { it.copy(loading = true) }
                            }
                            SimpleMediaState.Ended -> {
                                _timeline.update {
                                    it.copy(
                                        current = -1L,
                                        total = -1L,
                                        bufferedPercent = 0,
                                        loading = true,
                                    )
                                }
                            }

                            is SimpleMediaState.Progress -> {
                                if (mediaState.progress >= 0L && mediaState.progress != _timeline.value.current) {
                                    if (_timeline.value.total > 0L) {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = false,
                                            )
                                        }
                                    } else {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = true,
                                                total = simpleMediaServiceHandler.getPlayerDuration(),
                                            )
                                        }
                                    }
                                } else {
                                    _timeline.update {
                                        it.copy(
                                            loading = true,
                                        )
                                    }
                                }
                            }

                            is SimpleMediaState.Loading -> {
                                _timeline.update {
                                    it.copy(
                                        bufferedPercent = mediaState.bufferedPercentage,
                                        total = mediaState.duration,
                                    )
                                }
                            }

                            is SimpleMediaState.Ready -> {
                                _timeline.update {
                                    it.copy(
                                        current = simpleMediaServiceHandler.getProgress(),
                                        loading = false,
                                        total = mediaState.duration,
                                    )
                                }
                            }
                        }
                    }
                }
            val controllerJob =
                launch {
                    Log.w(tag, "ControllerJob is running")
                    simpleMediaServiceHandler.controlState.collectLatest {
                        Log.w(tag, "ControlState is $it")
                        _controllerState.value = it
                    }
                }
            val sleepTimerJob =
                launch {
                    simpleMediaServiceHandler.sleepTimerState.collectLatest {
                        _sleepTimerState.value = it
                    }
                }
            val playlistNameJob =
                launch {
                    simpleMediaServiceHandler.queueData.collectLatest {
                        _nowPlayingScreenData.update {
                            it.copy(playlistName = it.playlistName)
                        }
                    }
                }
            job1.join()
            controllerJob.join()
            sleepTimerJob.join()
            playlistNameJob.join()
        }
    }

    fun setIntent(intent: Intent?) {
        _intent.value = intent
    }

    fun blurFullscreenLyrics(): Boolean = runBlocking { dataStoreManager.blurFullscreenLyrics.first() == TRUE }

    private fun getLikeStatus(videoId: String?) {
        viewModelScope.launch {
            if (videoId != null) {
                _likeStatus.value = false
                mainRepository.getLikeStatus(videoId).collectLatest { status ->
                    _likeStatus.value = status
                }
            }
        }
    }

    private fun getCanvas(
        videoId: String,
        duration: Int,
    ) {
        Log.w(tag, "Start getCanvas: $videoId $duration")
//        canvasJob?.cancel()
        viewModelScope.launch {
            if (dataStoreManager.spotifyCanvas.first() == TRUE) {
                mainRepository.getCanvas(videoId, duration).cancellable().collect { response ->
                    _canvas.value = response
                    Log.w(tag, "Canvas is $response")
                    if (response != null && nowPlayingState.value?.mediaItem?.mediaId == videoId) {
                        _nowPlayingScreenData.update {
                            it.copy(
                                canvasData =
                                    response.canvases.firstOrNull()?.canvas_url?.let { canvasUrl ->
                                        NowPlayingScreenData.CanvasData(
                                            isVideo = canvasUrl.contains(".mp4"),
                                            url = canvasUrl,
                                        )
                                    },
                            )
                        }
                        if (response
                                .canvases
                                .firstOrNull()
                                ?.canvas_url
                                ?.contains(".mp4") == true
                        ) {
                            mainRepository.updateCanvasUrl(videoId, response.canvases.first().canvas_url)
                        }
                        val canvasThumbs = response.canvases.firstOrNull()?.thumbsOfCanva
                        if (!canvasThumbs.isNullOrEmpty()) {
                            (
                                canvasThumbs.let {
                                    it
                                        .maxByOrNull {
                                            (it.height ?: 0) + (it.width ?: 0)
                                        }?.url
                                } ?: canvasThumbs.first().url
                            )?.let { thumb ->
                                mainRepository.updateCanvasThumbUrl(videoId, thumb)
                            }
                        }
                    } else {
                        nowPlayingState.value?.songEntity?.canvasUrl?.let { url ->
                            _nowPlayingScreenData.update {
                                it.copy(
                                    canvasData =
                                        NowPlayingScreenData.CanvasData(
                                            isVideo = url.contains(".mp4"),
                                            url = url,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun getString(key: String): String? = runBlocking { dataStoreManager.getString(key).first() }

    fun putString(
        key: String,
        value: String,
    ) {
        runBlocking { dataStoreManager.putString(key, value) }
    }

    fun setSleepTimer(minutes: Int) {
        simpleMediaServiceHandler.sleepStart(minutes)
    }

    fun stopSleepTimer() {
        simpleMediaServiceHandler.sleepStop()
    }

    private var _downloadState: MutableStateFlow<Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<Download?> = _downloadState.asStateFlow()

    fun checkIsRestoring() {
        viewModelScope.launch {
            mainRepository.getDownloadedSongs().first().let { songs ->
                songs?.forEach { song ->
                    if (!downloadedCache.keys.contains(song.videoId)) {
                        mainRepository.updateDownloadState(
                            song.videoId,
                            DownloadState.STATE_NOT_DOWNLOADED,
                        )
                    }
                }
            }
            mainRepository.getAllDownloadedPlaylist().first().let { list ->
                for (data in list) {
                    when (data) {
                        is AlbumEntity -> {
                            if (data.tracks.isNullOrEmpty() ||
                                (
                                    !downloadedCache.keys.containsAll(
                                        data.tracks,
                                    )
                                )
                            ) {
                                mainRepository.updateAlbumDownloadState(
                                    data.browseId,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is PlaylistEntity -> {
                            if (data.tracks.isNullOrEmpty() ||
                                (
                                    !downloadedCache.keys.containsAll(
                                        data.tracks,
                                    )
                                )
                            ) {
                                mainRepository.updatePlaylistDownloadState(
                                    data.id,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is LocalPlaylistEntity -> {
                            if (data.tracks.isNullOrEmpty() ||
                                (
                                    !downloadedCache.keys.containsAll(
                                        data.tracks,
                                    )
                                )
                            ) {
                                mainRepository.updateLocalPlaylistDownloadState(
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                    data.id,
                                )
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

    private fun getSavedLyrics(track: Track) {
        viewModelScope.launch {
            mainRepository.getSavedLyrics(track.videoId).cancellable().collectLatest { lyrics ->
                if (lyrics != null) {
                    val lyricsData = lyrics.toLyrics()
                    Log.d(tag, "Saved Lyrics $lyricsData")
                    updateLyrics(
                        track.videoId,
                        track.durationSeconds ?: 0,
                        lyricsData,
                        false,
                        LyricsProvider.OFFLINE,
                    )
                    getAITranslationLyrics(
                        track.videoId,
                        lyricsData,
                    )
                }
            }
        }
    }

    fun loadSharedMediaItem(videoId: String) {
        viewModelScope.launch {
            mainRepository.getFullMetadata(videoId).collectLatest {
                if (it != null) {
                    val track = it.toTrack()
                    simpleMediaServiceHandler.setQueueData(
                        QueueData(
                            listTracks = arrayListOf(track),
                            firstPlayedTrack = track,
                            playlistId = "RDAMVM$videoId",
                            playlistName = context.getString(R.string.shared),
                            playlistType = PlaylistType.RADIO,
                            continuation = null,
                        ),
                    )
                    loadMediaItemFromTrack(track, SONG_CLICK)
                } else {
                    Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @UnstableApi
    fun loadMediaItemFromTrack(
        track: Track,
        type: String,
        index: Int? = null,
    ) {
        quality = runBlocking { dataStoreManager.quality.first() }
        viewModelScope.launch {
            simpleMediaServiceHandler.clearMediaItems()
            mainRepository.insertSong(track.toSongEntity()).first().let {
                println("insertSong: $it")
                mainRepository
                    .getSongById(track.videoId)
                    .collect { songEntity ->
                        if (songEntity != null) {
                            Log.w("Check like", "loadMediaItemFromTrack ${songEntity.liked}")
                            _liked.value = songEntity.liked
                        }
                    }
            }
            track.durationSeconds?.let {
                mainRepository.updateDurationSeconds(
                    it,
                    track.videoId,
                )
            }
            withContext(Dispatchers.Main) {
                simpleMediaServiceHandler.addMediaItem(track.toMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)
            }

            when (type) {
                SONG_CLICK -> {
                    simpleMediaServiceHandler.getRelated(track.videoId)
                }

                VIDEO_CLICK -> {
                    simpleMediaServiceHandler.getRelated(track.videoId)
                }

                SHARE -> {
                    simpleMediaServiceHandler.getRelated(track.videoId)
                }

                PLAYLIST_CLICK -> {
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = 0)
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }

                ALBUM_CLICK -> {
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = 0)
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }
            }
        }
    }

    @UnstableApi
    fun onUIEvent(uiEvent: UIEvent) =
        viewModelScope.launch {
            when (uiEvent) {
                UIEvent.Backward ->
                    simpleMediaServiceHandler.onPlayerEvent(
                        PlayerEvent.Backward,
                    )

                UIEvent.Forward -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Forward)
                UIEvent.PlayPause ->
                    simpleMediaServiceHandler.onPlayerEvent(
                        PlayerEvent.PlayPause,
                    )

                UIEvent.Next -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Next)
                UIEvent.Previous ->
                    simpleMediaServiceHandler.onPlayerEvent(
                        PlayerEvent.Previous,
                    )

                UIEvent.Stop -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Stop)
                is UIEvent.UpdateProgress -> {
                    simpleMediaServiceHandler.onPlayerEvent(
                        PlayerEvent.UpdateProgress(
                            uiEvent.newProgress,
                        ),
                    )
                }

                UIEvent.Repeat -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Repeat)
                UIEvent.Shuffle -> simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.Shuffle)
                UIEvent.ToggleLike -> {
                    Log.w(tag, "ToggleLike")
                    simpleMediaServiceHandler.onPlayerEvent(PlayerEvent.ToggleLike)
                }
            }
        }

    @UnstableApi
    override fun onCleared() {
        Log.w("Check onCleared", "onCleared")
    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        quality = runBlocking { dataStoreManager.quality.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    fun checkAllDownloadingSongs() {
        viewModelScope.launch {
            mainRepository.getDownloadingSongs().collect { songs ->
                songs?.forEach { song ->
                    mainRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
            mainRepository.getPreparingSongs().collect { songs ->
                songs.forEach { song ->
                    mainRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
        }
    }

    private fun getFormat(mediaId: String?) {
        if (mediaId != _format.value?.videoId && !mediaId.isNullOrEmpty()) {
            _format.value = null
            getFormatFlowJob?.cancel()
            getFormatFlowJob =
                viewModelScope.launch {
                    mainRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                        Log.w(tag, "Get format for $mediaId: $f")
                        if (f != null) {
                            _format.emit(f)
                        } else {
                            _format.emit(null)
                        }
                    }
                }
        }
    }

    private var songInfoJob: Job? = null

    fun getSongInfo(mediaId: String?) {
        songInfoJob?.cancel()
        songInfoJob =
            viewModelScope.launch {
                if (mediaId != null) {
                    mainRepository.getSongInfo(mediaId).collect { song ->
                        _nowPlayingScreenData.update {
                            it.copy(
                                songInfoData = song,
                            )
                        }
                    }
                }
            }
    }

    private var _githubResponse = MutableStateFlow<GithubResponse?>(null)
    val githubResponse: StateFlow<GithubResponse?> = _githubResponse

    fun checkForUpdate() {
        viewModelScope.launch {
            mainRepository.checkForUpdate().collect { response ->
                dataStoreManager.putString(
                    "CheckForUpdateAt",
                    System.currentTimeMillis().toString(),
                )
                _githubResponse.value = response
                showedUpdateDialog = true
            }
        }
    }

    fun stopPlayer() {
        _nowPlayingScreenData.value = NowPlayingScreenData.initial()
        _nowPlayingState.value = null
        simpleMediaServiceHandler.resetSongAndQueue()
        onUIEvent(UIEvent.Stop)
    }

    private fun loadPlaylistOrAlbum(index: Int? = null) {
        simpleMediaServiceHandler.loadPlaylistOrAlbum(index)
    }

    private fun updateLyrics(
        videoId: String,
        duration: Int, // 0 if translated lyrics
        lyrics: Lyrics?,
        isTranslatedLyrics: Boolean,
        lyricsProvider: LyricsProvider = LyricsProvider.SIMPMUSIC,
    ) {
        if (lyrics == null) {
            _nowPlayingScreenData.update {
                it.copy(
                    lyricsData = null,
                )
            }
            return
        }

        if (isTranslatedLyrics) {
            val originalLyrics = _nowPlayingScreenData.value.lyricsData?.lyrics
            if (originalLyrics != null && originalLyrics.lines != null && lyrics.lines != null) {
                var outOfSyncCount = 0

                originalLyrics.lines.forEach { originalLine ->
                    val originalTime = originalLine.startTimeMs.toLongOrNull() ?: 0L
                    val closestTranslatedLine =
                        lyrics.lines.minByOrNull {
                            abs((it.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
                        }

                    if (closestTranslatedLine != null) {
                        val translatedTime = closestTranslatedLine.startTimeMs.toLongOrNull() ?: 0L
                        val timeDiff = abs(originalTime - translatedTime)

                        if (timeDiff > 1000L) { // Lệch quá 1 giây
                            outOfSyncCount++
                        }
                    }
                }

                if (outOfSyncCount > 5) {
                    Log.w(tag, "Translated lyrics out of sync: $outOfSyncCount lines with time diff > 1s")

                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = null,
                                ),
                        )
                    }

                    viewModelScope.launch {
                        mainRepository.removeTranslatedLyrics(
                            videoId,
                            dataStoreManager.translationLanguage.first(),
                        )
                        Log.d(tag, "Removed out-of-sync translated lyrics for $videoId")
                        val simpMusicLyricsId = lyrics.simpMusicLyricsId
                        if (lyricsProvider == LyricsProvider.SIMPMUSIC && !simpMusicLyricsId.isNullOrEmpty()) {
                            viewModelScope.launch {
                                mainRepository
                                    .voteSimpMusicTranslatedLyrics(
                                        translatedLyricsId = simpMusicLyricsId,
                                        false,
                                    ).collectLatest {
                                        when (it) {
                                            is Resource.Error -> {
                                                Log.w(tag, "Vote SimpMusic Translated Lyrics Error ${it.message}")
                                            }
                                            is Resource.Success -> {
                                                Log.d(tag, "Vote SimpMusic Translated Lyrics Success")
                                            }
                                        }
                                    }
                            }
                        }
                    }
                    if (lyricsProvider != LyricsProvider.AI) {
                        viewModelScope.launch {
                            nowPlayingScreenData.value.lyricsData?.lyrics?.let {
                                getAITranslationLyrics(
                                    videoId,
                                    it,
                                )
                            }
                        }
                    }
                    return
                }
            }
        }

        val shouldSendLyricsToSimpMusic =
            runBlocking {
                dataStoreManager.helpBuildLyricsDatabase.first() == TRUE
            } &&
                lyricsProvider != LyricsProvider.SIMPMUSIC
        if (_nowPlayingState.value?.songEntity?.videoId == videoId) {
            val track = _nowPlayingState.value?.track
            when (isTranslatedLyrics) {
                true -> {
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = lyrics,
                                ),
                        )
                    }
                    if (shouldSendLyricsToSimpMusic && track != null) {
                        viewModelScope.launch {
                            mainRepository
                                .insertSimpMusicTranslatedLyrics(
                                    track,
                                    lyrics,
                                    dataStoreManager.translationLanguage.first(),
                                ).collect {
                                    when (it) {
                                        is Resource.Error -> {
                                            Log.w(tag, "Insert SimpMusic Translated Lyrics Error ${it.message}")
                                        }
                                        is Resource.Success -> {
                                            Log.d(tag, "Insert SimpMusic Translated Lyrics Success")
                                        }
                                    }
                                }
                        }
                    }
                }
                false -> {
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                NowPlayingScreenData.LyricsData(
                                    lyrics = lyrics,
                                    lyricsProvider = lyricsProvider,
                                ),
                        )
                    }
                    // Save lyrics to database
                    viewModelScope.launch {
                        mainRepository.insertLyrics(
                            LyricsEntity(
                                videoId = videoId,
                                error = false,
                                lines = lyrics.lines,
                                syncType = lyrics.syncType,
                            ),
                        )
                    }
                    if (shouldSendLyricsToSimpMusic && track != null) {
                        viewModelScope.launch {
                            mainRepository
                                .insertSimpMusicLyrics(
                                    track,
                                    duration,
                                    lyrics,
                                ).collect {
                                    when (it) {
                                        is Resource.Error -> {
                                            Log.w(tag, "Insert SimpMusic Lyrics Error ${it.message}")
                                        }
                                        is Resource.Success -> {
                                            Log.d(tag, "Insert SimpMusic Lyrics Success")
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    private fun getLyricsFromFormat(
        song: SongEntity,
        duration: Int,
    ) {
        viewModelScope.launch {
            val videoId = song.videoId
            Log.w(tag, "Get Lyrics From Format for $videoId")
            val artist =
                if (song.artistName?.firstOrNull() != null &&
                    song.artistName
                        .firstOrNull()
                        ?.contains("Various Artists") == false
                ) {
                    song.artistName.firstOrNull()
                } else {
                    simpleMediaServiceHandler.nowPlaying
                        .first()
                        ?.mediaMetadata
                        ?.artist
                        ?: ""
                }
            val lyricsProvider = dataStoreManager.lyricsProvider.first()
            when (lyricsProvider) {
                DataStoreManager.SIMPMUSIC -> {
                    mainRepository.getSimpMusicLyrics(videoId).collectLatest {
                        Log.w(tag, "Get SimpMusic Lyrics for $videoId: $it")
                        val data = it.data
                        if (it is Resource.Success && data != null) {
                            Log.d(tag, "Get SimpMusic Lyrics Success")
                            updateLyrics(
                                videoId,
                                duration,
                                data,
                                false,
                                LyricsProvider.SIMPMUSIC,
                            )
                            insertLyrics(
                                data.toLyricsEntity(videoId),
                            )
                            getSimpMusicTranslatedLyrics(
                                videoId,
                                data,
                            )
                        } else if (dataStoreManager.spotifyLyrics.first() == TRUE) {
                            getSpotifyLyrics(
                                song.toTrack().copy(durationSeconds = duration),
                                "${song.title} $artist",
                                duration,
                            )
                        } else {
                            getLrclibLyrics(
                                song,
                                (artist ?: "").toString(),
                                duration,
                            )
                        }
                    }
                }

                DataStoreManager.LRCLIB -> {
                    getLrclibLyrics(
                        song,
                        (artist ?: "").toString(),
                        duration,
                    )
                }
                DataStoreManager.YOUTUBE -> {
                    mainRepository.getYouTubeCaption(videoId).cancellable().collect { response ->
                        when (response) {
                            is Resource.Success -> {
                                if (response.data != null) {
                                    val lyrics = response.data.first
                                    val translatedLyrics = response.data.second
                                    insertLyrics(lyrics.toLyricsEntity(videoId))
                                    updateLyrics(
                                        videoId,
                                        duration,
                                        lyrics,
                                        false,
                                        LyricsProvider.YOUTUBE,
                                    )
                                    if (translatedLyrics != null) {
                                        updateLyrics(
                                            videoId,
                                            duration,
                                            translatedLyrics,
                                            true,
                                            LyricsProvider.YOUTUBE,
                                        )
                                    } else {
                                        getAITranslationLyrics(
                                            videoId,
                                            lyrics,
                                        )
                                    }
                                } else if (dataStoreManager.spotifyLyrics.first() == TRUE) {
                                    getSpotifyLyrics(
                                        song.toTrack().copy(
                                            durationSeconds = duration,
                                        ),
                                        "${song.title} ${song.artistName?.firstOrNull() ?: simpleMediaServiceHandler.nowPlaying
                                            .first()
                                            ?.mediaMetadata
                                            ?.artist ?: ""}",
                                        duration,
                                    )
                                }
                            }

                            is Resource.Error -> {
                                if (dataStoreManager.spotifyLyrics.first() == TRUE) {
                                    getSpotifyLyrics(
                                        song.toTrack().copy(
                                            durationSeconds = duration,
                                        ),
                                        "${song.title} ${song.artistName?.firstOrNull() ?: simpleMediaServiceHandler.nowPlaying
                                            .first()
                                            ?.mediaMetadata
                                            ?.artist ?: ""}",
                                        duration,
                                    )
                                } else {
                                    getLrclibLyrics(
                                        song,
                                        (artist ?: "").toString(),
                                        duration,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getLrclibLyrics(
        song: SongEntity,
        artist: String,
        duration: Int,
    ) {
        viewModelScope.launch {
            mainRepository
                .getLrclibLyricsData(
                    artist,
                    song.title,
                    duration,
                ).collectLatest { res ->
                    when (res) {
                        is Resource.Success -> {
                            Log.d(tag, "Get Lyrics Data Success")
                            updateLyrics(
                                song.videoId,
                                duration,
                                res.data,
                                false,
                                LyricsProvider.LRCLIB,
                            )
                            insertLyrics(
                                res.data?.toLyricsEntity(
                                    song.videoId,
                                ) ?: return@collectLatest,
                            )
                            getAITranslationLyrics(
                                song.videoId,
                                res.data,
                            )
                        }
                        is Resource.Error -> {
                            getSavedLyrics(
                                song.toTrack().copy(
                                    durationSeconds = duration,
                                ),
                            )
                        }
                    }
                }
        }
    }

    private suspend fun getSimpMusicTranslatedLyrics(
        videoId: String,
        lyrics: Lyrics,
    ) {
        val translationLanguage =
            dataStoreManager.translationLanguage.first()
        mainRepository.getSimpMusicTranslatedLyrics(videoId, translationLanguage).collectLatest { response ->
            val data = response.data
            when (response) {
                is Resource.Success if (data != null) -> {
                    Log.d(tag, "Get SimpMusic Translated Lyrics Success")
                    updateLyrics(
                        videoId,
                        0,
                        data,
                        true,
                        LyricsProvider.SIMPMUSIC,
                    )
                }

                else -> {
                    Log.w(tag, "Get SimpMusic Translated Lyrics Error: ${response.message}")
                    getAITranslationLyrics(
                        videoId,
                        lyrics,
                    )
                }
            }
        }
    }

    private suspend fun getAITranslationLyrics(
        videoId: String,
        lyrics: Lyrics,
    ) {
        if (dataStoreManager.useAITranslation.first() == TRUE &&
            dataStoreManager.aiApiKey.first().isNotEmpty() &&
            dataStoreManager.enableTranslateLyric.first() == FALSE
        ) {
            val savedTranslatedLyrics =
                mainRepository
                    .getSavedTranslatedLyrics(
                        videoId,
                        dataStoreManager.translationLanguage.first(),
                    ).firstOrNull()
            if (savedTranslatedLyrics != null) {
                Log.d(tag, "Get Saved Translated Lyrics")
                updateLyrics(
                    videoId,
                    0,
                    savedTranslatedLyrics.toLyrics(),
                    true,
                )
            } else {
                mainRepository
                    .getAITranslationLyrics(
                        lyrics,
                        dataStoreManager.translationLanguage.first(),
                    ).cancellable()
                    .collectLatest {
                        val data = it.data
                        when (it) {
                            is Resource.Success if (data != null) -> {
                                if (true) {
                                    Log.d(tag, "Get AI Translate Lyrics Success")
                                    mainRepository.insertTranslatedLyrics(
                                        TranslatedLyricsEntity(
                                            videoId = videoId,
                                            language = dataStoreManager.translationLanguage.first(),
                                            error = false,
                                            lines = data.lines,
                                            syncType = data.syncType,
                                        ),
                                    )
                                    updateLyrics(
                                        videoId,
                                        0,
                                        data,
                                        true,
                                        LyricsProvider.AI,
                                    )
                                }
                            }

                            else -> {
                                Log.w(tag, "Get AI Translate Lyrics Error: ${it.message}")
                            }
                        }
                    }
            }
        }
    }

    private fun getSpotifyLyrics(
        track: Track,
        query: String,
        duration: Int? = null,
    ) {
        viewModelScope.launch {
            Log.d("Check SpotifyLyrics", "SpotifyLyrics $query")
            mainRepository.getSpotifyLyrics(query, duration).cancellable().collect { response ->
                Log.d("Check SpotifyLyrics", response.toString())
                when (response) {
                    is Resource.Success -> {
                        if (response.data != null) {
                            insertLyrics(
                                response.data.toLyricsEntity(
                                    track.videoId,
                                ),
                            )
                            updateLyrics(
                                track.videoId,
                                duration ?: 0,
                                response.data,
                                false,
                                LyricsProvider.SPOTIFY,
                            )
                            getAITranslationLyrics(
                                track.videoId,
                                response.data,
                            )
                        }
                    }

                    is Resource.Error -> {
                        getLrclibLyrics(
                            track.toSongEntity(),
                            track.artists.toListName().firstOrNull() ?: "",
                            duration ?: 0,
                        )
                    }
                }
            }
        }
    }

    fun setLyricsProvider() {
        viewModelScope.launch {
            nowPlayingState.value?.songEntity?.let {
                getLyricsFromFormat(it, timeline.value.total.toInt() / 1000)
            }
        }
    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

    private var _recreateActivity: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recreateActivity: StateFlow<Boolean> = _recreateActivity

    fun activityRecreate() {
        _recreateActivity.value = true
    }

    fun activityRecreateDone() {
        _recreateActivity.value = false
    }

    fun addListToQueue(listTrack: ArrayList<Track>) {
        viewModelScope.launch {
            simpleMediaServiceHandler.loadMoreCatalog(listTrack)
            Toast
                .makeText(
                    context,
                    context.getString(R.string.added_to_queue),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    fun addToYouTubeLiked() {
        viewModelScope.launch {
            val videoId = simpleMediaServiceHandler.nowPlaying.first()?.mediaId
            if (videoId != null) {
                val like = likeStatus.value
                if (!like) {
                    mainRepository
                        .addToYouTubeLiked(
                            simpleMediaServiceHandler.nowPlaying.first()?.mediaId,
                        ).collect { response ->
                            if (response == 200) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.added_to_youtube_liked),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                getLikeStatus(videoId)
                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.error),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                } else {
                    mainRepository
                        .removeFromYouTubeLiked(
                            simpleMediaServiceHandler.nowPlaying.first()?.mediaId,
                        ).collect {
                            if (it == 200) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.removed_from_youtube_liked),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                getLikeStatus(videoId)
                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.error),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                }
            }
        }
    }

    fun getTranslucentBottomBar() = dataStoreManager.translucentBottomBar

    private val _reloadDestination: MutableStateFlow<KClass<*>?> = MutableStateFlow(null)
    val reloadDestination: StateFlow<KClass<*>?> = _reloadDestination.asStateFlow()

    fun reloadDestination(destination: KClass<*>) {
        _reloadDestination.value = destination
    }

    fun reloadDestinationDone() {
        _reloadDestination.value = null
    }

    fun shouldCheckForUpdate(): Boolean = runBlocking { dataStoreManager.autoCheckForUpdates.first() == TRUE }

    fun runWorker() {
        Log.w("Check Worker", "Worker")
        val request =
            PeriodicWorkRequestBuilder<NotifyWork>(
                12L,
                TimeUnit.HOURS,
            ).addTag("Worker Test")
                .setConstraints(
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                ).build()
        WorkManager.getInstance(application).enqueueUniquePeriodicWork(
            "Artist Worker",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private var _downloadFileProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.INIT)
    val downloadFileProgress: StateFlow<DownloadProgress> get() = _downloadFileProgress

    fun downloadFile(bitmap: Bitmap) {
        val fileName =
            "${nowPlayingScreenData.value.nowPlayingTitle} - ${nowPlayingScreenData.value.artistName}"
                .replace(Regex("""[|\\?*<":>]"""), "")
                .replace(" ", "_")
        val path =
            "${Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS,
            ).path}/$fileName"
        viewModelScope.launch {
            nowPlayingState.value?.track?.let { track ->
                mainRepository
                    .downloadToFile(
                        track = track,
                        bitmap = bitmap,
                        videoId = track.videoId,
                        path = path,
                        isVideo = nowPlayingScreenData.value.isVideo,
                    ).collectLatest {
                        _downloadFileProgress.value = it
                    }
            }
        }
    }

    fun downloadFileDone() {
        _downloadFileProgress.value = DownloadProgress.INIT
    }

    fun onDoneReview(isDismissOnly: Boolean = true) {
        viewModelScope.launch {
            if (!isDismissOnly) {
                dataStoreManager.doneOpenAppTime()
            } else {
                dataStoreManager.openApp()
            }
        }
    }

    fun onDoneRequestingShareLyrics(contributor: Pair<String, String>? = null) {
        viewModelScope.launch {
            dataStoreManager.setHelpBuildLyricsDatabase(true)
            dataStoreManager.setContributorLyricsDatabase(
                contributor,
            )
        }
    }

    fun setBitmap(bitmap: ImageBitmap?) {
        _nowPlayingScreenData.update {
            it.copy(bitmap = bitmap)
        }
    }

    fun shouldStopMusicService(): Boolean = runBlocking { dataStoreManager.killServiceOnExit.first() == TRUE }
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

    data class UpdateProgress(
        val newProgress: Float,
    ) : UIEvent()

    data object ToggleLike : UIEvent()
}

enum class LyricsProvider {
    SIMPMUSIC,
    YOUTUBE,
    SPOTIFY,
    LRCLIB,
    AI,
    OFFLINE,
}

data class TimeLine(
    val current: Long,
    val total: Long,
    val bufferedPercent: Int,
    val loading: Boolean = true,
)

data class NowPlayingScreenData(
    val playlistName: String,
    val nowPlayingTitle: String,
    val artistName: String,
    val isVideo: Boolean,
    val isExplicit: Boolean = false,
    val thumbnailURL: String?,
    val canvasData: CanvasData? = null,
    val lyricsData: LyricsData? = null,
    val songInfoData: SongInfoEntity? = null,
    val bitmap: ImageBitmap? = null,
) {
    data class CanvasData(
        val isVideo: Boolean,
        val url: String,
    )

    data class LyricsData(
        val lyrics: Lyrics,
        val translatedLyrics: Lyrics? = null,
        val lyricsProvider: LyricsProvider,
    )

    companion object {
        fun initial(): NowPlayingScreenData =
            NowPlayingScreenData(
                nowPlayingTitle = "",
                artistName = "",
                isVideo = false,
                thumbnailURL = null,
                canvasData = null,
                lyricsData = null,
                songInfoData = null,
                playlistName = "",
            )
    }
}