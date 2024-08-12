package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.toPlaylistEntity
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toVideoIdList
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel
    @Inject
    constructor(
        private val mainRepository: MainRepository,
        private val application: Application,
        private var dataStoreManager: DataStoreManager,
    ) : AndroidViewModel(application) {
        @Inject
        @UnstableApi
        lateinit var downloadUtils: DownloadUtils

        private var _gradientDrawable: MutableStateFlow<GradientDrawable?> = MutableStateFlow(null)
        var gradientDrawable: StateFlow<GradientDrawable?> = _gradientDrawable

        private var _uiState = MutableStateFlow<PlaylistUIState>(PlaylistUIState.Loading)
        val uiState: StateFlow<PlaylistUIState> = _uiState

        private val _playlistBrowse: MutableStateFlow<PlaylistBrowse?> =  MutableStateFlow(null)
        var playlistBrowse: StateFlow<PlaylistBrowse?> = _playlistBrowse

        private val _id: MutableLiveData<String> = MutableLiveData()
        var id: LiveData<String> = _id

        private val _isRadio: MutableStateFlow<Boolean> = MutableStateFlow(false)
        var isRadio: StateFlow<Boolean> = _isRadio

        private var _radioContinuation: MutableStateFlow<Pair<String, String?>?> = MutableStateFlow(null)
        var radioContinuation: StateFlow<Pair<String, String?>?> = _radioContinuation

        private var _playlistEntity: MutableStateFlow<PlaylistEntity?> = MutableStateFlow(null)
        var playlistEntity: StateFlow<PlaylistEntity?> = _playlistEntity

        private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
        var liked: MutableStateFlow<Boolean> = _liked

        private var _songEntity: MutableLiveData<SongEntity?> = MutableLiveData()
        val songEntity: LiveData<SongEntity?> = _songEntity
        private var _listLocalPlaylist: MutableLiveData<List<LocalPlaylistEntity>> = MutableLiveData()
        val listLocalPlaylist: LiveData<List<LocalPlaylistEntity>> = _listLocalPlaylist

        private var regionCode: String? = null
        private var language: String? = null

        private var collectDownloadedJob: Job? = null
        private var _downloadedList = MutableStateFlow<List<String>>(emptyList())
        val downloadedList: StateFlow<List<String>> = _downloadedList

        init {
            regionCode = runBlocking { dataStoreManager.location.first() }
            language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
        }

        fun updateId(id: String) {
            _id.value = id
        }

        fun updateIsRadio(isRadio: Boolean) {
            _isRadio.value = isRadio
        }

        fun browsePlaylist(id: String) {
            _uiState.value = PlaylistUIState.Loading
            viewModelScope.launch {
//            mainRepository.browsePlaylist(id, regionCode!!, SUPPORTED_LANGUAGE.serverCodes[SUPPORTED_LANGUAGE.codes.indexOf(language!!)]).collect{ values ->
//                _playlistBrowse.value = values
//            }
                mainRepository.getPlaylistData(id).collect {
                    when(it) {
                        is Resource.Success -> {
                            _playlistBrowse.value = it.data
                            it.data?.let { playlistEntity ->
                                getPlaylist(id, playlistEntity, false)
                            }
                        }
                        is Resource.Error -> {
                            Log.w("PlaylistViewModel", "Error: ${it.message}")
                            _playlistBrowse.value = null
                            getPlaylist(id, null, false, it.message)
                        }
                    }
                }
            }
        }

        fun getRadio(
            radioId: String,
            videoId: String? = null,
            channelId: String? = null,
        ) {
            _uiState.value = PlaylistUIState.Loading
            viewModelScope.launch {
                if (videoId != null) {
                    mainRepository.getSongById(videoId).collectLatest { song ->
                        if (song != null) {
                            mainRepository.getRadio(radioId, song).collect {
                                when (it) {
                                    is Resource.Success -> {
                                        _playlistBrowse.value = it.data?.first
                                        it.data?.first?.id?.let { id ->
                                            _radioContinuation.value = Pair(id, it.data.second)
                                        }
                                        it.data?.first?.let { playlistEntity ->
                                            getPlaylist(radioId, playlistEntity, true)
                                        }
                                    }
                                    is Resource.Error -> {
                                        Log.w("PlaylistViewModel", "Error: ${it.message}")
                                        _playlistBrowse.value = null
                                        _radioContinuation.value = null
                                        withContext(Dispatchers.Main) {
                                            _uiState.value = PlaylistUIState.Error(it.message)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (channelId != null) {
                    mainRepository.getArtistById(channelId).collectLatest { artist ->
                        mainRepository.getRadio(radioId = radioId, artist = artist).collect {
                            when (it) {
                                is Resource.Success -> {
                                    _playlistBrowse.value = it.data?.first
                                    it.data?.first?.id?.let { id ->
                                        _radioContinuation.value = Pair(id, it.data.second)
                                    }
                                    it.data?.first?.let { playlistEntity ->
                                        getPlaylist(radioId, playlistEntity, true)
                                    }
                                }
                                is Resource.Error -> {
                                    Log.w("PlaylistViewModel", "Error: ${it.message}")
                                    _playlistBrowse.value = null
                                    withContext(Dispatchers.Main) {
                                        _uiState.value = PlaylistUIState.Error(it.message)
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    mainRepository.getRDATRadioData(radioId).collect {
                        when (it) {
                            is Resource.Success -> {
                                _playlistBrowse.value = it.data?.first
                                it.data?.first?.id?.let { id ->
                                    _radioContinuation.value = Pair(id, it.data.second)
                                }
                                it.data?.first?.let { playlistEntity ->
                                    getPlaylist(radioId, playlistEntity, true)
                                }
                            }
                            is Resource.Error -> {
                                Log.w("PlaylistViewModel", "Error: ${it.message}")
                                _playlistBrowse.value = null
                                withContext(Dispatchers.Main) {
                                    _uiState.value = PlaylistUIState.Error(it.message)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun insertPlaylist(playlistEntity: PlaylistEntity) {
            viewModelScope.launch {
                mainRepository.insertPlaylist(playlistEntity)
                mainRepository.getPlaylist(playlistEntity.id).collect { values ->
                    _playlistEntity.value = values
                    if (values != null) {
                        _liked.value = values.liked
                    }
                }
            }
        }

        fun getPlaylist(id: String, playlistBrowse: PlaylistBrowse?, isRadio: Boolean?, message: String? = null) {
            viewModelScope.launch {
                mainRepository.updatePlaylistInLibrary(
                    LocalDateTime.now(),
                    id
                )
                mainRepository.getPlaylist(id).collect { values ->
                    if (values != null) {
                        _playlistEntity.value = values
                        _liked.value = values.liked
                        playlistDownloadState.value = values.downloadState
                        val list = values.tracks
                        var count = 0
                        list?.forEach { track ->
                            mainRepository.getSongById(track).singleOrNull()?.let { song ->
                                if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                                    count++
                                }
                            }
                        }
                        if (count == list?.size && count > 0) {
                            updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADED)
                        } else {
                            updatePlaylistDownloadState(id, DownloadState.STATE_NOT_DOWNLOADED)
                        }
                        getListTrack(list)
                        withContext(Dispatchers.Main) {
                            _uiState.value = PlaylistUIState.Success
                        }
                    }
                    else if (isRadio != null && playlistBrowse != null) {
                        _liked.value = false
                        playlistDownloadState.value = DownloadState.STATE_NOT_DOWNLOADED
                        _playlistEntity.value = null
                        when (isRadio) {
                            true -> {
                                insertRadioPlaylist(playlistBrowse.toPlaylistEntity())
                            }
                            false -> {
                                insertPlaylist(playlistBrowse.toPlaylistEntity())
                            }
                        }
                        withContext(Dispatchers.Main) {
                            _uiState.value = PlaylistUIState.Success
                        }
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            _uiState.value = PlaylistUIState.Error(message)
                        }
                    }
                }
            }
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
                    listFlow
                ) { list ->
                    list.map { it }.flatten()
                }.collectLatest { values ->
                    val sortedList = values.sortedBy {
                        tracks?.indexOf(it.videoId)
                    }
                    _listTrack.value = sortedList
                    collectDownloadedJob?.cancel()
                    if (values.isNotEmpty()) {
                        collectDownloadedJob = launch {
                            mainRepository.getDownloadedVideoIdListFromListVideoIdAsFlow(values.toVideoIdList()).collectLatest {
                                _downloadedList.value = it
                            }
                        }
                    }
                    else {
                        _downloadedList.value = emptyList()
                    }
                }
            }
        }

        fun checkAllSongDownloaded(list: ArrayList<Track>) {
            viewModelScope.launch {
                var count = 0
                list.forEach { track ->
                    mainRepository.getSongById(track.videoId).collect { song ->
                        if (song != null) {
                            if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                                count++
                            }
                        }
                    }
                }
                if (count == list.size && count > 0) {
                    id.value?.let { updatePlaylistDownloadState(it, DownloadState.STATE_DOWNLOADED) }
                }
                    id.value?.let {
                    mainRepository.getPlaylist(it).collect { album ->
                        if (album != null) {
                            if (playlistEntity.value?.downloadState != album.downloadState) {
                                _playlistEntity.value = album
                            }
                        }
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
                mainRepository.getPlaylist(id).collect { values ->
                    _playlistEntity.value = values
                    if (values != null) {
                        _liked.value = values.liked
                    }
                }
            }
        }

        val listJob: MutableStateFlow<ArrayList<SongEntity>> = MutableStateFlow(arrayListOf())
        val playlistDownloadState: MutableStateFlow<Int> = MutableStateFlow(DownloadState.STATE_NOT_DOWNLOADED)

        fun updatePlaylistDownloadState(
            id: String,
            state: Int,
        ) {
            viewModelScope.launch {
                mainRepository.getPlaylist(id).collect { playlist ->
                    _playlistEntity.value = playlist
                    mainRepository.updatePlaylistDownloadState(id, state)
                    playlistDownloadState.value = state
                }
            }
        }
//    fun downloading() {
//        _prevPlaylistDownloading.value = true
//    }
//    fun collectDownloadState(id: String) {
//        viewModelScope.launch {
//            listJob.collect { jobs->
//                    Log.w("PlaylistFragment", "ListJob: $jobs")
//                    if (jobs.isNotEmpty()){
//                        var count = 0
//                        jobs.forEach { job ->
//                            if (job.downloadState == DownloadState.STATE_DOWNLOADED) {
//                                count++
//                            }
//                            else if (job.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
//                                updatePlaylistDownloadState(id, DownloadState.STATE_NOT_DOWNLOADED)
//                                _prevPlaylistDownloading.value = false
//                            }
//                        }
//                        if (count == jobs.size) {
//                            updatePlaylistDownloadState(
//                                id,
//                                DownloadState.STATE_DOWNLOADED
//                            )
//                            _prevPlaylistDownloading.value = false
//                            Toast.makeText(
//                                context,
//                                context.getString(R.string.downloaded),
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    }
//            }
//        }
//    }

        @UnstableApi
        fun getDownloadStateFromService(videoId: String) {
            viewModelScope.launch {
            }
        }

        fun clearPlaylistBrowse() {
            _playlistBrowse.value = null
            _gradientDrawable.value = null
            _radioContinuation.value = null
        }

        fun clearPlaylistEntity() {
            _listTrack.value = emptyList()
            _playlistEntity.value = null
        }

        fun getLocation() {
            regionCode = runBlocking { dataStoreManager.location.first() }
            language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
        }

        fun getSongEntity(song: SongEntity) {
            viewModelScope.launch {
                mainRepository.insertSong(song).first().let {
                    println("Insert song $it")
                }
                mainRepository.getSongById(song.videoId).collect { values ->
                    _songEntity.value = values
                }
            }
        }

        fun updateLikeStatus(
            videoId: String,
            likeStatus: Int,
        ) {
            viewModelScope.launch {
                mainRepository.updateLikeStatus(likeStatus = likeStatus, videoId = videoId)
            }
        }

        fun getLocalPlaylist() {
            viewModelScope.launch {
                mainRepository.getAllLocalPlaylists().collect { values ->
                    _listLocalPlaylist.postValue(values)
                }
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

        @UnstableApi
        fun downloadFullPlaylistState(id: String) {
            viewModelScope.launch {
                downloadUtils.downloads.collect { download ->
                    playlistDownloadState.value =
                        if (listJob.value.all { download[it.videoId]?.state == Download.STATE_COMPLETED }) {
                            mainRepository.updatePlaylistDownloadState(
                                id,
                                DownloadState.STATE_DOWNLOADED,
                            )
                            DownloadState.STATE_DOWNLOADED
                        } else if (listJob.value.all {
                                download[it.videoId]?.state == Download.STATE_QUEUED ||
                                    download[it.videoId]?.state == Download.STATE_DOWNLOADING ||
                                    download[it.videoId]?.state == Download.STATE_COMPLETED
                            }
                        ) {
                            mainRepository.updatePlaylistDownloadState(
                                id,
                                DownloadState.STATE_DOWNLOADING,
                            )
                            DownloadState.STATE_DOWNLOADING
                        } else {
                            mainRepository.updatePlaylistDownloadState(id, DownloadState.STATE_NOT_DOWNLOADED)
                            DownloadState.STATE_NOT_DOWNLOADED
                        }
                }
            }
        }

        fun insertLocalPlaylist(
            localPlaylistEntity: LocalPlaylistEntity,
            listTrack: List<Track>,
        ) {
            viewModelScope.launch {
                mainRepository.insertLocalPlaylist(localPlaylistEntity)
                delay(500)
                mainRepository.getLocalPlaylistByYoutubePlaylistId(localPlaylistEntity.youtubePlaylistId!!).singleOrNull()?.let { playlist ->
                    if (playlist.youtubePlaylistId == localPlaylistEntity.youtubePlaylistId) {
                        for (i in listTrack.indices) {
                            mainRepository.insertSong(listTrack[i].toSongEntity()).first().let {
                                println("Insert song $it")
                            }
                            mainRepository.insertPairSongLocalPlaylist(
                                PairSongLocalPlaylist(
                                    playlistId = playlist.id,
                                    songId = listTrack[i].videoId,
                                    position = i,
                                    inPlaylist = LocalDateTime.now(),
                                ),
                            )
                            if (i == 100) {
                                delay(100)
                            }
                        }
                    }
                }
                Toast.makeText(application, application.getString(R.string.added_local_playlist), Toast.LENGTH_SHORT).show()
            }
        }

        private var _localPlaylistIfYouTubePlaylist: MutableStateFlow<LocalPlaylistEntity?> = MutableStateFlow(null)
        var localPlaylistIfYouTubePlaylist: MutableStateFlow<LocalPlaylistEntity?> = _localPlaylistIfYouTubePlaylist

        fun checkSyncedPlaylist(value: String?) {
            viewModelScope.launch {
                if (value != null) {
                    mainRepository.getLocalPlaylistByYoutubePlaylistId(value).collect {
                        _localPlaylistIfYouTubePlaylist.value = it
                    }
                }
            }
        }

        fun addToYouTubePlaylist(
            localPlaylistId: Long,
            youtubePlaylistId: String,
            videoId: String,
        ) {
            viewModelScope.launch {
                mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Syncing)
                mainRepository.addYouTubePlaylistItem(youtubePlaylistId, videoId).collect { response ->
                    if (response == "STATUS_SUCCEEDED") {
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Synced)
                        Toast.makeText(application, application.getString(R.string.added_to_youtube_playlist), Toast.LENGTH_SHORT).show()
                    } else {
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.NotSynced)
                        Toast.makeText(application, application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        fun insertRadioPlaylist(playlistEntity: PlaylistEntity) {
            viewModelScope.launch {
                mainRepository.insertRadioPlaylist(playlistEntity)
                mainRepository.getPlaylist(playlistEntity.id).collect { values ->
                    _playlistEntity.value = values
                    if (values != null) {
                        _liked.value = values.liked
                    }
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

        fun insertPlaylistSongEntity(tracks: List<Track>) {
            viewModelScope.launch {
                tracks.forEach { track ->
                    mainRepository.insertSong(track.toSongEntity()).first().let {
                        println("Insert Song: $it")
                    }
                    Log.w("PlaylistFragment", "Insert Song: ${track.toSongEntity()}")
                }
            }
        }

    fun checkSuccess() {
        if (_playlistBrowse.value != null)  {
            _uiState.value = PlaylistUIState.Success
        }
    }

    fun setGradientDrawable(gd: GradientDrawable) {
        _gradientDrawable.value = gd
    }

    override fun onCleared() {
        super.onCleared()
        collectDownloadedJob?.cancel()
    }
}

sealed class PlaylistUIState {
    data object Loading : PlaylistUIState()
    data object Success : PlaylistUIState()
    data class Error(val message: String? = null) : PlaylistUIState()
}