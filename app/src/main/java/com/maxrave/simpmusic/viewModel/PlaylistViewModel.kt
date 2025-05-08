@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.test.main
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.DownloadState.STATE_DOWNLOADED
import com.maxrave.simpmusic.common.DownloadState.STATE_DOWNLOADING
import com.maxrave.simpmusic.common.DownloadState.STATE_NOT_DOWNLOADED
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.manager.LocalPlaylistManager
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.playlist.Author
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.extension.toListVideoId
import com.maxrave.simpmusic.extension.toPlaylistEntity
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.extension.toVideoIdList
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.utils.collectLatestResource
import com.maxrave.simpmusic.viewModel.PlaylistUIState.*
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import java.time.LocalDateTime

@UnstableApi
class PlaylistViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    val downloadUtils: DownloadUtils by inject()
    private val localPlaylistManager: LocalPlaylistManager by inject()

    private var _uiState = MutableStateFlow<PlaylistUIState>(Loading)
    val uiState: StateFlow<PlaylistUIState> = _uiState

    private var _listColors = MutableStateFlow<List<Color>>(emptyList())
    val listColors: StateFlow<List<Color>> = _listColors

    private var _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation

    private var _playlistEntity: MutableStateFlow<PlaylistEntity?> = MutableStateFlow(null)
    var playlistEntity: StateFlow<PlaylistEntity?> = _playlistEntity

    val downloadState = _playlistEntity.map { it?.downloadState ?: 0 }.stateIn(viewModelScope, WhileSubscribed(1000), 0)
    val liked = _playlistEntity.map { it?.liked == true }.stateIn(viewModelScope, WhileSubscribed(1000), false)

    private var _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks

    private var _tracksListState = MutableStateFlow<ListState>(ListState.IDLE)
    val tracksListState: StateFlow<ListState> = _tracksListState

    private var collectDownloadedJob: Job? = null
    private var _downloadedList = MutableStateFlow<List<String>>(emptyList())
    val downloadedList: StateFlow<List<String>> = _downloadedList

    private var playlistEntityJob: Job? = null
    private var newUpdateJob: Job? = null
    private var checkDownloadedPlaylist: Job? = null

    init {
        viewModelScope.launch {
            val listTrackStringJob =
                launch(Dispatchers.IO) {
                    downloadState
                        .collectLatest { state ->
                            newUpdateJob?.cancel()
                            val id = playlistEntity.value?.id ?: return@collectLatest
                            if (state == STATE_DOWNLOADING || state == STATE_DOWNLOADED) {
                                getFullTracks { tracks ->
                                    newUpdateJob =
                                        launch {
                                            val listSongs = mainRepository.getSongsByListVideoId(tracks.toListVideoId()).firstOrNull() ?: emptyList()
                                            if (state == STATE_DOWNLOADED && listSongs.isNotEmpty()) {
                                                listSongs.filter { it.downloadState != STATE_DOWNLOADED }.let { notDownloaded ->
                                                    if (notDownloaded.isNotEmpty()) {
                                                        downloadTracks(notDownloaded.map { it.videoId })
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADING)
                                                    } else {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                                                    }
                                                }
                                            }
                                            downloadUtils.downloads.collectLatest { downloads ->
                                                var count = 0
                                                tracks.forEachIndexed { index, track ->
                                                    val trackDownloadState = downloads[track.videoId]?.first?.state
                                                    val videoDownloadState = downloads[track.videoId]?.second?.state ?: Download.STATE_COMPLETED
                                                    if (trackDownloadState == Download.STATE_DOWNLOADING ||
                                                        videoDownloadState == Download.STATE_DOWNLOADING
                                                    ) {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADING)
                                                    } else if (trackDownloadState == Download.STATE_COMPLETED &&
                                                        videoDownloadState == Download.STATE_COMPLETED
                                                    ) {
                                                        count++
                                                    }
                                                    if (count == tracks.size) {
                                                        updatePlaylistDownloadState(id, STATE_DOWNLOADED)
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                        }
                }
            listTrackStringJob.join()
        }
    }

    private fun updatePlaylistDownloadState(
        id: String,
        state: Int,
    ) {
        viewModelScope.launch {
            mainRepository.updatePlaylistDownloadState(id, state)
            delay(500)
            _playlistEntity.update {
                it?.copy(downloadState = state)
            }
        }
    }

    private fun downloadTracks(listJob: List<String>) {
        viewModelScope.launch {
            listJob.forEach { videoId ->
                mainRepository.getSongById(videoId).singleOrNull()?.let { song ->
                    if (song.downloadState != STATE_DOWNLOADED) {
                        downloadUtils.downloadTrack(videoId, song.title, song.thumbnails ?: "")
                    }
                }
            }
        }
    }

    private fun resetData() {
        _uiState.value = Loading
        _playlistEntity.value = null
        _listTrack.value = emptyList()
        _downloadedList.value = emptyList()
        _listColors.value = emptyList()
        checkDownloadedPlaylist?.cancel()
        checkDownloadedPlaylist = null
    }

    fun getData(id: String) {
        resetData()
        viewModelScope.launch {
            // Check radio
            if (id.matches(Regex("(RDAMVM|RDEM|RDAT).*"))) {
                mainRepository.getRadio(id).collect { res ->
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null) -> {
                            Log.d(tag, "Radio data: $data")
                            _uiState.value =
                                Success(
                                    data =
                                        PlaylistState(
                                            id = data.first.id,
                                            title = data.first.title,
                                            isRadio = true,
                                            author = data.first.author,
                                            thumbnail =
                                                data.first.thumbnails
                                                    .lastOrNull()
                                                    ?.url,
                                            description = data.first.description,
                                            trackCount = data.first.trackCount,
                                            year = data.first.year,
                                        ),
                                )
                            _tracks.value = data.first.tracks
                            _continuation.value = data.second
                            if (data.second.isNullOrEmpty()) _tracksListState.value = ListState.PAGINATION_EXHAUST
                            mainRepository.insertRadioPlaylist(data.first.toPlaylistEntity())
                        }
                        else -> {
                            _uiState.value = Error(res.message ?: "Empty response")
                        }
                    }
                }
            } else {
                // This is an online playlist
                mainRepository.getPlaylistData(id).collect { res ->
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null) -> {
                            Log.d(tag, "Playlist data: $data")
                            log("Playlist endpoint: ${data.first.shuffleEndpoint}")
                            _uiState.value =
                                Success(
                                    data =
                                        PlaylistState(
                                            id = data.first.id,
                                            title = data.first.title,
                                            isRadio = false,
                                            author = data.first.author,
                                            thumbnail =
                                                data.first.thumbnails
                                                    .lastOrNull()
                                                    ?.url,
                                            description = data.first.description,
                                            trackCount = data.first.trackCount,
                                            year = data.first.year,
                                            shuffleEndpoint = data.first.shuffleEndpoint,
                                            radioEndpoint = data.first.radioEndpoint,
                                        ),
                                )
                            _tracks.value = data.first.tracks
                            _continuation.value = data.second
                            if (data.second.isNullOrEmpty()) _tracksListState.value = ListState.PAGINATION_EXHAUST
                            getPlaylistEntity(id = data.first.id, playlistBrowse = data.first)
                        }
                        else -> {
                            getPlaylistEntity(id)
                        }
                    }
                }
            }
        }
    }

    fun getContinuationTrack(
        playlistId: String,
        continuation: String?,
    ) {
        viewModelScope.launch {
            if (continuation.isNullOrEmpty()) {
                _tracksListState.value = ListState.PAGINATION_EXHAUST
                return@launch
            } else {
                _tracksListState.value = ListState.PAGINATING
                mainRepository
                    .getContinueTrack(
                        playlistId,
                        continuation,
                        fromPlaylist = true,
                    ).collectLatest { res ->
                        res.first?.forEach { track ->
                            mainRepository.insertSong(
                                track.toSongEntity()
                                    .copy(
                                        inLibrary = Config.REMOVED_SONG_DATE_TIME
                                    )
                            ).singleOrNull()?.let {
                                log("Insert song: $it")
                            }
                        }
                        _tracks.update {
                            val newList = it.toMutableList()
                            newList.addAll(res.first ?: emptyList())
                            newList
                        }
                        if (res.second.isNullOrEmpty()) {
                            _continuation.value = null
                            _tracksListState.value = ListState.PAGINATION_EXHAUST
                        } else {
                            _continuation.value = res.second
                            _tracksListState.value = ListState.IDLE
                        }
                    }
            }
        }
    }

    private fun getPlaylistEntity(
        id: String,
        playlistBrowse: PlaylistBrowse? = null,
    ) {
        playlistEntityJob?.cancel()
        playlistEntityJob =
            viewModelScope.launch {
                val playlistEntity = mainRepository.getPlaylist(id).firstOrNull()
                if (playlistBrowse != null) {
                    if (playlistEntity == null) {
                        mainRepository.insertAndReplacePlaylist(
                            playlistBrowse.toPlaylistEntity(),
                        )
                        delay(500)
                        mainRepository.getPlaylist(id).collectLatest { playlist ->
                            _playlistEntity.value = playlist
                            mainRepository.updatePlaylistInLibrary(
                                playlistId = id,
                                inLibrary = LocalDateTime.now(),
                            )
                        }
                    } else {
                        _playlistEntity.value = playlistEntity
                        mainRepository.updatePlaylistInLibrary(
                            playlistId = id,
                            inLibrary = LocalDateTime.now(),
                        )
                    }
                    playlistBrowse.tracks.forEach { tracks ->
                        mainRepository.insertSong(tracks.toSongEntity().copy(
                            inLibrary = Config.REMOVED_SONG_DATE_TIME
                        )).firstOrNull()?.let {
                            log("Insert song: $it")
                        }
                    }
                } else if (playlistEntity != null) {
                    _playlistEntity.value = playlistEntity
                    mainRepository.updatePlaylistInLibrary(
                        playlistId = id,
                        inLibrary = LocalDateTime.now(),
                    )
                    _uiState.value =
                        Success(
                            data =
                                PlaylistState(
                                    id = playlistEntity.id,
                                    title = playlistEntity.title,
                                    isRadio = false,
                                    author =
                                        Author(
                                            id = "",
                                            name = playlistEntity.author ?: "",
                                        ),
                                    thumbnail = playlistEntity.thumbnails,
                                    description = playlistEntity.description,
                                    trackCount = playlistEntity.trackCount,
                                    year = playlistEntity.year ?: LocalDateTime.now().year.toString(),
                                ),
                        )
                    _tracksListState.value = ListState.LOADING
                    playlistEntity.tracks?.let {
                        mainRepository
                            .getSongsByListVideoId(it)
                            .singleOrNull()
                            ?.let { song ->
                                _tracks.value = song.map { it.toTrack() }
                            }
                    }
                    _tracksListState.value = ListState.PAGINATION_EXHAUST
                    if (playlistEntity.downloadState != STATE_DOWNLOADED) {
                        checkDownloadedPlaylist = launch {
                            val listSong = mainRepository.getSongsByListVideoId(
                                playlistEntity.tracks ?: emptyList(),
                            ).firstOrNull()
                            Log.d(tag, "List song: $listSong")
                            if (!listSong.isNullOrEmpty() && listSong.size == playlistEntity.tracks?.size && listSong.all {
                                it.downloadState == STATE_DOWNLOADED
                                }) {
                                updatePlaylistDownloadState(playlistEntity.id, STATE_DOWNLOADED)
                            }
                        }
                    }
                } else {
                    _uiState.value = Error("Empty response")
                }
            }
    }

    fun setBrush(listColors: List<Color>) {
        _listColors.value = listColors
    }

    private var _listTrack: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    var listTrack: StateFlow<List<SongEntity>> = _listTrack

    fun getListTrack(tracks: List<String>?) {
        viewModelScope.launch {
            val listFlow = mutableListOf<Flow<List<SongEntity>>>()
            tracks?.chunked(500)?.forEachIndexed { index, videoIds ->
                listFlow.add(mainRepository.getSongsByListVideoId(videoIds).stateIn(viewModelScope))
            }
            combine(
                listFlow,
            ) { list ->
                list.map { it }.flatten()
            }.collectLatest { values ->
                val sortedList =
                    values.sortedBy {
                        tracks?.indexOf(it.videoId)
                    }
                _listTrack.value = sortedList
                collectDownloadedJob?.cancel()
                if (values.isNotEmpty()) {
                    collectDownloadedJob =
                        launch {
                            mainRepository.getDownloadedVideoIdListFromListVideoIdAsFlow(values.toVideoIdList()).collectLatest {
                                _downloadedList.value = it
                            }
                        }
                } else {
                    _downloadedList.value = emptyList()
                }
            }
        }
    }

    fun updatePlaylistLiked(
        liked: Boolean,
        id: String,
    ) {
        viewModelScope.launch {
            val tempLiked = if (liked) 1 else 0
            mainRepository.updatePlaylistLiked(id, tempLiked)
            _playlistEntity.update {
                it?.copy(
                    liked = tempLiked == 1,
                )
            }
            getFullTracks { }
        }
    }

    fun updateLocalPlaylistTracks(
        list: List<String>,
        id: Long,
    ) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(list).collect { values ->
                var count = 0
                values.forEach { song ->
                    if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                        count++
                    }
                }
                mainRepository.updateLocalPlaylistTracks(list, id)
                Toast.makeText(getApplication(), application.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
                if (count == values.size && count > 0) {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_DOWNLOADED, id)
                } else {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_NOT_DOWNLOADED, id)
                }
            }
        }
    }

    fun updateDownloadState(
        videoId: String,
        state: Int,
    ) {
        viewModelScope.launch {
            mainRepository.updateDownloadState(videoId, state)
        }
    }

    fun insertSong(songEntity: SongEntity) {
        viewModelScope.launch {
            mainRepository.insertSong(songEntity).collect {
                println("Insert Song $it")
            }
        }
    }

    fun onUIEvent(event: PlaylistUIEvent) {
        val data = uiState.value.data ?: return
        when (event) {
            is PlaylistUIEvent.ItemClick -> {
                val videoId = event.videoId
                val loadedList = tracks.value
                val clickedSong = loadedList.first { it.videoId == videoId }
                val index = loadedList.indexOf(clickedSong)
                setQueueData(
                    QueueData(
                        listTracks = loadedList.toCollection(arrayListOf<Track>()),
                        firstPlayedTrack = clickedSong,
                        playlistId = data.id,
                        playlistName = "${
                            getString(
                                R.string.playlist,
                            )
                        } \"${data.title}\"",
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = continuation.value,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    index,
                )
            }
            PlaylistUIEvent.PlayAll -> {
                val loadedList = tracks.value
                if (loadedList.isEmpty()) {
                    makeToast(
                        application.getString(R.string.playlist_is_empty),
                    )
                    return
                }
                val clickedSong = loadedList.first()
                setQueueData(
                    QueueData(
                        listTracks = loadedList.toCollection(arrayListOf<Track>()),
                        firstPlayedTrack = clickedSong,
                        playlistId = data.id,
                        playlistName = "${
                            getString(
                                R.string.playlist,
                            )
                        } \"${data.title}\"",
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = continuation.value,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    0,
                )
            }
            PlaylistUIEvent.Shuffle -> {
                if (data.shuffleEndpoint == null) {
                    makeToast(
                        application.getString(R.string.shuffle_not_available),
                    )
                    return
                } else {
                    viewModelScope.launch {
                        mainRepository.getRadioArtist(data.shuffleEndpoint).collectLatest { res ->
                            val result = res.data
                            when (res) {
                                is Resource.Success if (result != null) -> {
                                    Log.d(tag, "Shuffle data: ${result.first.size}")
                                    setQueueData(
                                        QueueData(
                                            listTracks = result.first.toCollection(arrayListOf<Track>()),
                                            firstPlayedTrack = result.first.firstOrNull() ?: return@collectLatest,
                                            playlistId = data.shuffleEndpoint.playlistId,
                                            playlistName = "\"${data.title}\" ${getString(R.string.shuffle)}",
                                            playlistType = PlaylistType.RADIO,
                                            continuation = result.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        result.first.firstOrNull() ?: return@collectLatest,
                                        Config.RADIO_CLICK,
                                        0,
                                    )
                                }
                                else -> {
                                    makeToast(
                                        res.message ?: application.getString(R.string.error),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            PlaylistUIEvent.StartRadio -> {
                if (data.radioEndpoint == null) {
                    makeToast(
                        application.getString(R.string.radio_not_available),
                    )
                    return
                } else {
                    viewModelScope.launch {
                        mainRepository.getRadioArtist(data.radioEndpoint).collectLatest { res ->
                            val result = res.data
                            when (res) {
                                is Resource.Success if (result != null) -> {
                                    Log.d(tag, "Radio data: ${result.first.size}")
                                    setQueueData(
                                        QueueData(
                                            listTracks = result.first.toCollection(arrayListOf<Track>()),
                                            firstPlayedTrack = result.first.firstOrNull() ?: return@collectLatest,
                                            playlistId = data.radioEndpoint.playlistId,
                                            playlistName = "\"${data.title}\" ${getString(R.string.radio)}",
                                            playlistType = PlaylistType.RADIO,
                                            continuation = result.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        result.first.firstOrNull() ?: return@collectLatest,
                                        Config.RADIO_CLICK,
                                        0,
                                    )
                                }
                                else -> {
                                    makeToast(
                                        res.message ?: application.getString(R.string.error),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PlaylistUIEvent.Download -> {
                downloadFullPlaylist()
            }
            PlaylistUIEvent.Favorite -> {
                updatePlaylistLiked(!liked.value, data.id)
            }
        }
    }

    fun getFullTracks(callback: (List<Track>) -> Unit) {
        viewModelScope.launch {
            if (tracksListState.value == ListState.PAGINATION_EXHAUST) {
                _playlistEntity.value
                    ?.copy(
                        tracks = tracks.value.toListVideoId(),
                        trackCount = tracks.value.size,
                    )?.let {
                        mainRepository.insertAndReplacePlaylist(it)
                    }
                callback(tracks.value)
            } else {
                val id = uiState.value.data?.id ?: return@launch
                tracksListState.collectLatest { state ->
                    if (state == ListState.PAGINATION_EXHAUST) {
                        _playlistEntity.value
                            ?.copy(
                                tracks = tracks.value.toListVideoId(),
                                trackCount = tracks.value.size,
                            )?.let {
                                mainRepository.insertAndReplacePlaylist(it)
                            }
                        callback(tracks.value)
                    } else if (state != ListState.PAGINATING) {
                        getContinuationTrack(id, continuation.value)
                    }
                }
            }
        }
    }

    fun saveToLocal(tracks: List<Track>) {
        viewModelScope.launch {
            val data = uiState.value.data ?: return@launch
            localPlaylistManager
                .syncYouTubePlaylistToLocalPlaylist(
                    data,
                    tracks,
                ).collectLatestResource(
                    onSuccess = {
                        makeToast(it)
                    },
                    onLoading = {
                        makeToast(application.getString(R.string.syncing))
                    },
                    onError = {
                        makeToast(it)
                    },
                )
        }
    }

    fun downloadFullPlaylist() {
        viewModelScope.launch {
            val id = playlistEntity.value?.id ?: return@launch
            makeToast(application.getString(R.string.downloading))
            updatePlaylistDownloadState(id, STATE_DOWNLOADING)
            getFullTracks { tracks ->
                tracks.forEach {
                    viewModelScope.launch {
                        downloadUtils.downloadTrack(it.videoId, it.title, it.thumbnails?.lastOrNull()?.url ?: "")
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        collectDownloadedJob?.cancel()
        playlistEntityJob?.cancel()
    }
}

sealed class PlaylistUIState(
    val data: PlaylistState? = null,
    val message: String? = null,
) {
    data object Loading : PlaylistUIState()

    class Success(
        data: PlaylistState,
    ) : PlaylistUIState(
            data = data,
        )

    class Error(
        message: String? = null,
    ) : PlaylistUIState(
            message = message,
        )
}

data class PlaylistState(
    val id: String,
    val title: String,
    val isRadio: Boolean,
    val author: Author,
    val thumbnail: String? = null,
    val description: String? = null,
    val year: String,
    val trackCount: Int = 0,
    val radioEndpoint: WatchEndpoint? = null,
    val shuffleEndpoint: WatchEndpoint? = null,
)

sealed class PlaylistUIEvent {
    data object PlayAll : PlaylistUIEvent()

    data object Shuffle : PlaylistUIEvent()

    data object StartRadio : PlaylistUIEvent()

    data class ItemClick(
        val videoId: String,
    ) : PlaylistUIEvent()

    data object Favorite : PlaylistUIEvent()

    data object Download : PlaylistUIEvent()
}

enum class ListState {
    IDLE,
    LOADING,
    PAGINATING,
    ERROR,
    PAGINATION_EXHAUST,
}