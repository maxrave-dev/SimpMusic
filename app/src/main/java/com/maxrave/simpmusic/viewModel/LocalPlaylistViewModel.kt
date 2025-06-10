package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.filter
import androidx.paging.insertFooterItem
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.ASC
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DESC
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.DownloadState.STATE_DOWNLOADED
import com.maxrave.simpmusic.common.DownloadState.STATE_DOWNLOADING
import com.maxrave.simpmusic.common.DownloadState.STATE_NOT_DOWNLOADED
import com.maxrave.simpmusic.common.LOCAL_PLAYLIST_ID
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.manager.LocalPlaylistManager
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.extension.toArrayListTrack
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.pagination.PagingActions
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.ui.theme.md_theme_dark_background
import com.maxrave.simpmusic.utils.collectLatestResource
import com.maxrave.simpmusic.utils.collectResource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.time.LocalDateTime

@UnstableApi
class LocalPlaylistViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    private val localPlaylistManager: LocalPlaylistManager by inject()

    private val downloadUtils: DownloadUtils by inject()

    private var _offset: MutableStateFlow<Int> = MutableStateFlow(0)
    val offset: StateFlow<Int> = _offset

    fun setOffset(offset: Int) {
        _offset.value = offset
    }

    var loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _uiState: MutableStateFlow<LocalPlaylistState> = MutableStateFlow(LocalPlaylistState.initial())
    val uiState: StateFlow<LocalPlaylistState> get() = _uiState

    private fun setFilter(filterState: FilterState) {
        _uiState.update {
            it.copy(
                filterState = filterState,
            )
        }
    }

    fun setBrush(brush: List<Color>) {
        _uiState.update {
            it.copy(
                colors = brush,
            )
        }
    }

    private var newUpdateJob: Job? = null

    init {
        viewModelScope.launch {
            setFilter(
                when (dataStoreManager.localPlaylistFilter.first()) {
                    DataStoreManager.LOCAL_PLAYLIST_FILTER_OLDER_FIRST -> FilterState.OlderFirst
                    DataStoreManager.LOCAL_PLAYLIST_FILTER_NEWER_FIRST -> FilterState.NewerFirst
                    DataStoreManager.LOCAL_PLAYLIST_FILTER_TITLE -> FilterState.Title
                    else -> FilterState.OlderFirst
                },
            )
            val listTrackStringJob =
                launch {
                    uiState
                        .map { it.downloadState }
                        .distinctUntilChanged()
                        .collectLatest { downloadState ->
                            if (downloadState == STATE_DOWNLOADED || downloadState == STATE_DOWNLOADING) {
                                newUpdateJob?.cancel()
                                newUpdateJob =
                                    launch(Dispatchers.IO) {
                                        localPlaylistManager
                                            .listTrackFlow(uiState.value.id)
                                            .distinctUntilChanged()
                                            .collectLatest { list ->
                                                delay(500)
                                                val currentList = uiState.value.trackCount
                                                val newList = list.size
                                                log("newList: $list", Log.DEBUG)
                                                log("currentList: $currentList, newList: $newList", Log.DEBUG)
                                                if (newList > currentList) {
                                                    updatePlaylistState(uiState.value.id, refresh = true)
                                                }
                                                delay(500)
                                                val fullTracks = localPlaylistManager.getFullPlaylistTracks(id = uiState.value.id)
                                                val notDownloadedList =
                                                    fullTracks.filter { it.downloadState != STATE_DOWNLOADED }.map { it.videoId }
                                                if (fullTracks.isEmpty()) {
                                                    updatePlaylistDownloadState(uiState.value.id, STATE_NOT_DOWNLOADED)
                                                } else if (fullTracks.all { it.downloadState == STATE_DOWNLOADED } &&
                                                    downloadState != STATE_DOWNLOADED
                                                ) {
                                                    updatePlaylistDownloadState(uiState.value.id, STATE_DOWNLOADED)
                                                } else if (
                                                    fullTracks.filter { it.downloadState == STATE_DOWNLOADING }.isNotEmpty() &&
                                                    notDownloadedList.isNotEmpty() &&
                                                    downloadState != STATE_DOWNLOADING
                                                ) {
                                                    updatePlaylistDownloadState(uiState.value.id, STATE_DOWNLOADING)
                                                } else if (notDownloadedList.isNotEmpty()) {
                                                    updatePlaylistDownloadState(uiState.value.id, STATE_DOWNLOADING)
                                                    downloadTracks(notDownloadedList)
                                                }
                                            }
                                    }
                            }
                        }
                }
            val resetSuggestions =
                launch {
                    uiState
                        .map {
                            it.id
                        }.distinctUntilChanged()
                        .collectLatest {
                            _uiState.update {
                                it.copy(
                                    suggestions = null,
                                )
                            }
                        }
                }
            listTrackStringJob.join()
            resetSuggestions.join()
        }
    }

    private val _tracksPagingState: MutableStateFlow<PagingData<SongEntity>> =
        MutableStateFlow(
            PagingData.empty(),
        )
    val tracksPagingState: StateFlow<PagingData<SongEntity>> get() = _tracksPagingState
    private val lazyTrackPagingItems: MutableStateFlow<LazyPagingItems<SongEntity>?> = MutableStateFlow(null)

    fun setLazyTrackPagingItems(lazyPagingItems: LazyPagingItems<SongEntity>) {
        lazyTrackPagingItems.value = lazyPagingItems
        Log.d(tag, "setLazyTrackPagingItems: ${lazyTrackPagingItems.value?.itemCount}")
    }

    private val modifications = MutableStateFlow<List<PagingActions<SongEntity>>>(emptyList())

    private fun getTracksPagingState(
        id: Long,
        filterState: FilterState,
    ) {
        viewModelScope.launch {
            Log.w("LocalPlaylistViewModel", "getTracksPagingState: ${uiState.value}")
            modifications.value = listOf()
            localPlaylistManager
                .getTracksPaging(
                    id,
                    filterState,
                ).distinctUntilChanged()
                .cachedIn(viewModelScope)
                .combine(modifications) { pagingData, modifications ->
                    modifications.fold(pagingData) { data, actions ->
                        applyActions(data, actions)
                    }
                }.collect {
                    _tracksPagingState.value = it
                }
        }
    }

    private fun applyActions(
        pagingData: PagingData<SongEntity>,
        actions: PagingActions<SongEntity>,
    ): PagingData<SongEntity> =
        when (actions) {
            is PagingActions.Insert -> {
                val loadState = lazyTrackPagingItems.value?.loadState
                val list =
                    lazyTrackPagingItems.value
                        ?.itemSnapshotList
                        ?.toList()
                        ?.filterNotNull()
                        ?.map { it.videoId }
                        ?: emptyList()
                Log.w(tag, "applyActions: $loadState")
                if (loadState?.refresh is LoadState.NotLoading &&
                    loadState.append is LoadState.NotLoading &&
                    !list.contains(actions.item.videoId)
                ) {
                    pagingData
                        .insertFooterItem(item = actions.item)
                } else {
                    pagingData
                }
            }

            is PagingActions.Remove -> {
                pagingData.filter {
                    actions.item.videoId != it.videoId
                }
            }
        }

    private fun onApplyActions(actions: PagingActions<SongEntity>) {
        modifications.value += actions
    }

    fun getSuggestions(playlistId: Long) {
        loading.value = true
        viewModelScope.launch {
            localPlaylistManager.getSuggestionsTrackForPlaylist(playlistId).collectLatestResource(
                onSuccess = { res ->
                    val reloadParams = res?.first
                    val songs = res?.second
                    if (reloadParams != null && songs != null) {
                        _uiState.update {
                            it.copy(
                                suggestions =
                                    LocalPlaylistState.SuggestionSongs(
                                        reloadParams = reloadParams,
                                        songs = songs,
                                    ),
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                suggestions = null,
                            )
                        }
                    }
                    loading.value = false
                },
                onError = { e ->
                    makeToast(e)
                    loading.value = false
                    _uiState.update {
                        it.copy(
                            suggestions = null,
                        )
                    }
                },
            )
        }
    }

    private val _songEntity = MutableStateFlow<SongEntity?>(null)
    val songEntity: StateFlow<SongEntity?> get() = _songEntity

    fun getSongEntity(song: SongEntity) {
        viewModelScope.launch {
            mainRepository.insertSong(song).first().let {
                println("Insert song $it")
            }
            delay(200)
            mainRepository.getSongById(song.videoId).collect {
                if (it != null) _songEntity.emit(it)
            }
        }
    }

    fun reloadSuggestion() {
        loading.value = true
        viewModelScope.launch {
            val param = uiState.value.suggestions?.reloadParams
            if (param != null) {
                mainRepository.reloadSuggestionPlaylist(param).collect { res ->
                    val reloadParams = res?.first
                    val songs = res?.second
                    if (reloadParams != null && songs != null) {
                        _uiState.update {
                            it.copy(
                                suggestions =
                                    LocalPlaylistState.SuggestionSongs(
                                        reloadParams = reloadParams,
                                        songs = songs,
                                    ),
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                suggestions = null,
                            )
                        }
                    }
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
            } else {
                _uiState.update { it.copy(suggestions = null) }
                Toast
                    .makeText(
                        application,
                        application.getString(R.string.error),
                        Toast.LENGTH_SHORT,
                    ).show()
                withContext(Dispatchers.Main) {
                    loading.value = false
                }
            }
        }
    }

    fun updatePlaylistDownloadState(
        id: Long,
        state: Int,
    ) {
        viewModelScope.launch {
            localPlaylistManager.updateDownloadState(id, state).collectLatestResource(
                onSuccess = { mess ->
                    Log.d(tag, "updatePlaylistDownloadState: $mess")
                    _uiState.update {
                        it.copy(
                            downloadState = state,
                        )
                    }
                },
            )
        }
    }

    val listJob: MutableStateFlow<ArrayList<SongEntity>> = MutableStateFlow(arrayListOf())

//        var downloadState: StateFlow<List<Download?>>
//        viewModelScope.launch {
//            downloadState = downloadUtils.getAllDownloads().stateIn(viewModelScope)
//            downloadState.collectLatest { down ->
//                if (down.isNotEmpty()){
//                    var count = 0
//                    down.forEach { downloadItem ->
//                        if (downloadItem?.state == Download.STATE_COMPLETED) {
//                            count++
//                        }
//                        else if (downloadItem?.state == Download.STATE_FAILED) {
//                            updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADING)
//                        }
//                    }
//                    if (count == down.size) {
//                        mainRepository.getLocalPlaylist(id).collect{ playlist ->
//                            mainRepository.getSongsByListVideoId(playlist.tracks!!).collect{ tracks ->
//                                tracks.forEach { track ->
//                                    if (track.downloadState != DownloadState.STATE_DOWNLOADED) {
//                                        mainRepository.updateDownloadState(track.videoId, DownloadState.STATE_NOT_DOWNLOADED)
//                                        Toast.makeText(getApplication(), "Download Failed", Toast.LENGTH_SHORT).show()
//                                    }
//                                }
//                            }
//                        }
//                        Log.d("Check Downloaded", "Downloaded")
//                        updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADED)
//                        Toast.makeText(getApplication(), "Download Completed", Toast.LENGTH_SHORT).show()
//                    }
//                    else {
//                        updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADING)
//                    }
//                }
//                else {
//                    updatePlaylistDownloadState(id, DownloadState.STATE_NOT_DOWNLOADED)
//                }
//            }
//        }
//    }

    fun updatePlaylistTitle(
        title: String,
        id: Long,
    ) {
        viewModelScope.launch {
            showLoadingDialog(message = getString(R.string.updating))
            localPlaylistManager
                .updateTitleLocalPlaylist(id, title)
                .collectResource(
                    onSuccess = {
                        makeToast(it)
                        updatePlaylistState(id)
                        hideLoadingDialog()
                    },
                    onError = {
                        makeToast(it)
                        hideLoadingDialog()
                    },
                )
        }
    }

    fun deletePlaylist(id: Long) {
        showLoadingDialog(message = getString(R.string.delete))
        viewModelScope.launch {
            _uiState.value = LocalPlaylistState.initial()
            localPlaylistManager.deleteLocalPlaylist(id).collectLatestResource(
                onSuccess = {
                    makeToast(it)
                    hideLoadingDialog()
                },
                onError = {
                    makeToast(it)
                    hideLoadingDialog()
                },
            )
        }
    }

    fun updatePlaylistThumbnail(
        uri: String,
        id: Long,
    ) {
        showLoadingDialog(message = getString(R.string.updating))
        viewModelScope.launch {
            localPlaylistManager.updateThumbnailLocalPlaylist(id, uri).collectResource(
                onSuccess = {
                    makeToast(it)
                    updatePlaylistState(id)
                    hideLoadingDialog()
                },
                onError = {
                    makeToast(it)
                    hideLoadingDialog()
                },
            )
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

    fun deleteItem(
        id: Long,
        song: SongEntity,
    ) {
        viewModelScope.launch {
            localPlaylistManager.removeTrackFromLocalPlaylist(id, song).collectLatestResource(
                onSuccess = {
                    makeToast(it)
                    onApplyActions(PagingActions.Remove(song))
                    updatePlaylistState(id)
                },
                onError = {
                    makeToast(it)
                },
            )
        }
    }

    @UnstableApi
    fun downloadFullPlaylistState(
        id: Long,
        listJob: List<String>,
    ) {
        viewModelScope.launch {
            downloadUtils.downloadTask.collect { download ->
                _uiState.update { ui ->
                    ui.copy(
                        downloadState =
                            if (listJob.all { download[it] == STATE_DOWNLOADED }) {
                                mainRepository.updateLocalPlaylistDownloadState(
                                    STATE_DOWNLOADED,
                                    id,
                                )
                                STATE_DOWNLOADED
                            } else if (listJob.any { download[it] == STATE_DOWNLOADING }) {
                                mainRepository.updateLocalPlaylistDownloadState(
                                    STATE_DOWNLOADING,
                                    id,
                                )
                                STATE_DOWNLOADING
                            } else {
                                mainRepository.updateLocalPlaylistDownloadState(
                                    STATE_NOT_DOWNLOADED,
                                    id,
                                )
                                STATE_NOT_DOWNLOADED
                            },
                    )
                }
            }
        }
    }

    private var _listSetVideoId: MutableStateFlow<ArrayList<SetVideoIdEntity>?> =
        MutableStateFlow(null)
    val listSetVideoId: StateFlow<ArrayList<SetVideoIdEntity>?> = _listSetVideoId

    fun getSetVideoId(youtubePlaylistId: String) {
        viewModelScope.launch {
            mainRepository.getYouTubeSetVideoId(youtubePlaylistId).collect {
                _listSetVideoId.value = it
            }
        }
    }

    fun syncPlaylistWithYouTubePlaylist(id: Long) {
        makeToast(getString(R.string.syncing))
        showLoadingDialog(message = getString(R.string.syncing))
        viewModelScope.launch {
            localPlaylistManager
                .syncLocalPlaylistToYouTubePlaylist(id)
                .collectLatestResource(
                    onSuccess = { ytId ->
                        _uiState.update {
                            it.copy(
                                syncState = LocalPlaylistEntity.YouTubeSyncState.Synced,
                                ytPlaylistId = ytId,
                            )
                        }
                        makeToast(getString(R.string.synced))
                        hideLoadingDialog()
                    },
                    onError = {
                        makeToast(it)
                        updateLocalPlaylistSyncState(id, LocalPlaylistEntity.YouTubeSyncState.NotSynced)
                        hideLoadingDialog()
                    },
                )
//            mainRepository.createYouTubePlaylist(playlist).collect {
//                if (it != null) {
//                    val ytId = "VL$it"
//                    mainRepository.updateLocalPlaylistYouTubePlaylistId(playlist.id, ytId)
//                    mainRepository.updateLocalPlaylistYouTubePlaylistSynced(playlist.id, 1)
//                    mainRepository.getLocalPlaylistByYoutubePlaylistId(ytId).collect { yt ->
//                        if (yt != null) {
//                            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
//                                yt.id,
//                                LocalPlaylistEntity.YouTubeSyncState.Synced,
//                            )
//                            mainRepository.getLocalPlaylist(playlist.id).collect { last ->
//                                _localPlaylist.emit(last)
//                                Toast
//                                    .makeText(
//                                        application,
//                                        application.getString(R.string.synced),
//                                        Toast.LENGTH_SHORT,
//                                    ).show()
//                            }
//                        }
//                    }
//                } else {
//                    Toast
//                        .makeText(
//                            application,
//                            application.getString(R.string.error),
//                            Toast.LENGTH_SHORT,
//                        ).show()
//                }
//            }
        }
    }

    private fun updateLocalPlaylistSyncState(
        id: Long,
        syncState: Int,
        ytId: String? = null,
    ) {
        showLoadingDialog()
        viewModelScope.launch {
            localPlaylistManager.updateSyncState(id, syncState).collectLatestResource(
                onSuccess = { mess ->
                    makeToast(mess)
                    _uiState.update {
                        it.copy(
                            syncState = syncState,
                        )
                    }
                    hideLoadingDialog()
                },
                onError = {
                    makeToast(it)
                    hideLoadingDialog()
                },
            )
            if (ytId != null) {
                localPlaylistManager.updateYouTubePlaylistId(id, ytId).collectLatestResource(
                    onSuccess = { mess ->
                        Log.d(tag, "updateLocalPlaylistSyncState: $mess")
                        _uiState.update {
                            it.copy(
                                ytPlaylistId = ytId,
                            )
                        }
                    },
                )
            }
        }
    }

    fun unsyncPlaylistWithYouTubePlaylist(id: Long) {
        makeToast(getString(R.string.unsyncing))
        showLoadingDialog(message = getString(R.string.unsyncing))
        viewModelScope.launch {
            localPlaylistManager.unsyncLocalPlaylist(id).collectLatestResource(
                onSuccess = { mess ->
                    makeToast(mess)
                    _uiState.update {
                        it.copy(
                            syncState = LocalPlaylistEntity.YouTubeSyncState.NotSynced,
                            ytPlaylistId = null,
                        )
                    }
                    hideLoadingDialog()
                },
                onError = {
                    makeToast(it)
                    hideLoadingDialog()
                },
            )
        }
    }

    fun updateListTrackSynced(id: Long) {
        makeToast(getString(R.string.syncing))
        showLoadingDialog(message = getString(R.string.syncing))
        viewModelScope.launch {
            localPlaylistManager.updateListTrackSynced(id).collectLatest { done ->
                if (done) {
                    makeToast(application.getString(R.string.updated))
                    updatePlaylistState(id, refresh = true)
                }
                hideLoadingDialog()
            }
        }
    }

    fun updateInLibrary(videoId: String) {
        viewModelScope.launch {
            mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
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
                    if (song.downloadState == STATE_DOWNLOADED) {
                        count++
                    }
                }
                mainRepository.updateLocalPlaylistTracks(list, id)
                Toast
                    .makeText(
                        application,
                        application.getString(R.string.added_to_playlist),
                        Toast.LENGTH_SHORT,
                    ).show()
                if (count == values.size) {
                    mainRepository.updateLocalPlaylistDownloadState(
                        STATE_DOWNLOADED,
                        id,
                    )
                } else {
                    mainRepository.updateLocalPlaylistDownloadState(
                        STATE_NOT_DOWNLOADED,
                        id,
                    )
                }
                updatePlaylistState(id)
            }
        }
    }

    fun insertSong(song: Track) {
        viewModelScope.launch {
            mainRepository.insertSong(song.toSongEntity()).collect {
                println("Insert Song $it")
            }
        }
    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

    fun removeListSuggestion() {
        _uiState.update { it.copy(suggestions = null) }
    }

    fun addSuggestTrackToListTrack(track: Track) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    suggestions =
                        state.suggestions?.copy(
                            songs = state.suggestions.songs.filter { it.videoId != track.videoId },
                        ),
                )
            }
            _uiState.value.id.let { id ->
                localPlaylistManager
                    .addTrackToLocalPlaylist(id, track.toSongEntity())
                    .collectLatestResource(
                        onSuccess = {
                            makeToast(it)
                            // Add to UI
                            onApplyActions(PagingActions.Insert(track.toSongEntity()))
                        },
                        onError = {
                            makeToast(it)
                        },
                    )
            }
        }
    }

    fun onUIEvent(ev: LocalPlaylistUIEvent) {
        when (ev) {
            is LocalPlaylistUIEvent.ChangeFilter -> {
                viewModelScope.launch {
                    dataStoreManager.setLocalPlaylistFilter(
                        when (ev.filterState) {
                            FilterState.OlderFirst -> DataStoreManager.LOCAL_PLAYLIST_FILTER_OLDER_FIRST
                            FilterState.NewerFirst -> DataStoreManager.LOCAL_PLAYLIST_FILTER_NEWER_FIRST
                            FilterState.Title -> DataStoreManager.LOCAL_PLAYLIST_FILTER_TITLE
                        },
                    )
                }
                setFilter(ev.filterState)
                Log.w("PlaylistScreen", "new filterState: ${ev.filterState}")
                getTracksPagingState(uiState.value.id, ev.filterState)
            }

            is LocalPlaylistUIEvent.ItemClick -> {
                val loadedList = lazyTrackPagingItems.value?.itemSnapshotList?.toList() ?: return
                val clickedSong = loadedList.find { it?.videoId == ev.videoId } ?: return

                setQueueData(
                    QueueData(
                        listTracks = loadedList.filterNotNull().toArrayListTrack(),
                        firstPlayedTrack = clickedSong.toTrack(),
                        playlistId = LOCAL_PLAYLIST_ID + uiState.value.id,
                        playlistName = "${
                            getString(
                                R.string.playlist,
                            )
                        } \"${uiState.value.title}\"",
                        playlistType = PlaylistType.LOCAL_PLAYLIST,
                        continuation =
                            if (offset.value > 0) {
                                if (uiState.value.filterState == FilterState.OlderFirst) {
                                    ASC + offset.value.toString()
                                } else {
                                    DESC + offset.value.toString()
                                }
                            } else {
                                null
                            },
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    loadedList.indexOf(clickedSong),
                )
            }

            is LocalPlaylistUIEvent.SuggestionsItemClick -> {
                val suggestionsList = uiState.value.suggestions?.songs ?: return
                val clickedSong = suggestionsList.find { it.videoId == ev.videoId } ?: return

                setQueueData(
                    QueueData(
                        listTracks = suggestionsList.toCollection(ArrayList()),
                        firstPlayedTrack = clickedSong,
                        playlistId = "RDAMVM${clickedSong.videoId}",
                        playlistName = "${
                            getString(
                                R.string.playlist,
                            )
                        } \"${uiState.value.title}\" ${
                            getString(R.string.suggest)
                        }",
                        playlistType = PlaylistType.RADIO,
                        continuation = null,
                    ),
                )
                loadMediaItem(
                    clickedSong,
                    Config.PLAYLIST_CLICK,
                    0,
                )
            }

            is LocalPlaylistUIEvent.PlayClick -> {
                val loadedList =
                    lazyTrackPagingItems.value?.itemSnapshotList?.toList().let {
                        if (it.isNullOrEmpty()) {
                            makeToast(getString(R.string.playlist_is_empty))
                            return
                        } else {
                            it.filterNotNull().toArrayListTrack()
                        }
                    }
                val firstPlayTrack = loadedList.firstOrNull()
                setQueueData(
                    QueueData(
                        listTracks = loadedList,
                        firstPlayedTrack = firstPlayTrack,
                        playlistId = LOCAL_PLAYLIST_ID + uiState.value.id,
                        playlistName = "${
                            getString(
                                R.string.playlist,
                            )
                        } \"${uiState.value.title}\"",
                        playlistType = PlaylistType.LOCAL_PLAYLIST,
                        continuation =
                            if (offset.value > 0) {
                                if (uiState.value.filterState == FilterState.OlderFirst) {
                                    (ASC + offset.toString())
                                } else {
                                    (DESC + offset)
                                }
                            } else {
                                null
                            },
                    ),
                )
                loadMediaItem(
                    firstPlayTrack,
                    Config.PLAYLIST_CLICK,
                    0,
                )
            }

            is LocalPlaylistUIEvent.ShuffleClick -> {
                viewModelScope.launch {
                    val listVideoId = localPlaylistManager.getListTrackVideoId(uiState.value.id)
                    log("ShuffleClick: uiState id ${uiState.value.id}", Log.DEBUG)
                    log("ShuffleClick: $listVideoId", Log.DEBUG)
                    if (listVideoId.isEmpty()) {
                        makeToast(getString(R.string.playlist_is_empty))
                        return@launch
                    }
                    val random = listVideoId.random()
                    val randomIndex = listVideoId.indexOf(random)
                    val firstPlayedTrack = mainRepository.getSongById(random).singleOrNull()?.toTrack() ?: return@launch
                    setQueueData(
                        QueueData(
                            listTracks = arrayListOf(firstPlayedTrack),
                            firstPlayedTrack = firstPlayedTrack,
                            playlistId = LOCAL_PLAYLIST_ID + uiState.value.id,
                            playlistName = "${
                                getString(
                                    R.string.playlist,
                                )
                            } \"${uiState.value.title}\"",
                            playlistType = PlaylistType.LOCAL_PLAYLIST,
                            continuation = "",
                        ),
                    )
                    shufflePlaylist(randomIndex)
                }
            }
        }
    }

    fun updatePlaylistState(
        id: Long,
        refresh: Boolean = false,
    ) {
        viewModelScope.launch {
            localPlaylistManager.getLocalPlaylist(id).collectLatestResource(
                onSuccess = { pl ->
                    if (pl != null) {
                        _uiState.update {
                            it.copy(
                                id = pl.id,
                                title = pl.title,
                                thumbnail = pl.thumbnail,
                                inLibrary = pl.inLibrary,
                                downloadState = pl.downloadState,
                                syncState = pl.syncState,
                                ytPlaylistId = pl.youtubePlaylistId,
                                trackCount = pl.tracks?.size ?: 0,
                            )
                        }
                        if (refresh) {
                            getTracksPagingState(id, _uiState.value.filterState)
                        }
                    }
                },
            )
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

    fun downloadFullPlaylist() {
        viewModelScope.launch {
            val fullTracks = localPlaylistManager.getFullPlaylistTracks(id = uiState.value.id)
            val listJob = fullTracks.filter { it.downloadState != STATE_DOWNLOADED }.map { it.videoId }
            if (listJob.isNotEmpty()) {
                downloadTracks(listJob)
                downloadFullPlaylistState(uiState.value.id, listJob)
            } else if (fullTracks.isNotEmpty() && fullTracks.all { it.downloadState == STATE_DOWNLOADED }) {
                updatePlaylistDownloadState(uiState.value.id, STATE_DOWNLOADED)
            } else {
                makeToast(getString(R.string.playlist_is_empty))
            }
        }
    }

    fun addAllToQueue() {
        viewModelScope.launch {
            showLoadingDialog(getString(R.string.add_to_queue))
            val fullTracks = localPlaylistManager.getFullPlaylistTracks(id = uiState.value.id)
            if (fullTracks.isNotEmpty()) {
                simpleMediaServiceHandler.loadMoreCatalog(fullTracks.toArrayListTrack(), true)
                makeToast(getString(R.string.added_to_queue))
                hideLoadingDialog()
            } else {
                makeToast(getString(R.string.playlist_is_empty))
                hideLoadingDialog()
            }
        }
    }
}

sealed interface FilterState {
    val displayNameRes: Int

    object OlderFirst : FilterState {
        override val displayNameRes = R.string.older_first
    }

    object NewerFirst : FilterState {
        override val displayNameRes = R.string.newer_first
    }

    object Title : FilterState {
        override val displayNameRes = R.string.title
    }
}

sealed class LocalPlaylistUIEvent {
    data class ChangeFilter(
        val filterState: FilterState,
    ) : LocalPlaylistUIEvent()

    data class ItemClick(
        val videoId: String,
    ) : LocalPlaylistUIEvent()

    data class SuggestionsItemClick(
        val videoId: String,
    ) : LocalPlaylistUIEvent()

    data object PlayClick : LocalPlaylistUIEvent()

    data object ShuffleClick : LocalPlaylistUIEvent()
}

data class LocalPlaylistState(
    val id: Long,
    val title: String,
    val thumbnail: String? = null,
    val colors: List<Color> =
        listOf(
            Color.Black,
            md_theme_dark_background,
        ),
    val inLibrary: LocalDateTime? = null,
    val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,
    val syncState: Int = LocalPlaylistEntity.YouTubeSyncState.NotSynced,
    val ytPlaylistId: String? = null,
    val trackCount: Int = 0,
    val page: Int = 0,
    val isLoadedFull: Boolean = false,
    val loadState: PlaylistLoadState = PlaylistLoadState.Loading,
    val filterState: FilterState = FilterState.OlderFirst,
    val suggestions: SuggestionSongs? = null,
) {
    sealed class SuggestionState {
        data object Loading : SuggestionState()

        data object Error : SuggestionState()

        data class Success(
            val suggestSongs: SuggestionSongs,
        ) : SuggestionState()
    }

    sealed class PlaylistLoadState {
        data object Loading : PlaylistLoadState()

        data object Error : PlaylistLoadState()

        data object Success : PlaylistLoadState()
    }

    data class SuggestionSongs(
        val reloadParams: String,
        val songs: List<Track>,
    )

    companion object {
        fun initial(): LocalPlaylistState =
            LocalPlaylistState(
                id = 0,
                title = "",
                thumbnail = null,
                inLibrary = null,
                downloadState = 0,
                syncState = 0,
                trackCount = 0,
            )
    }
}