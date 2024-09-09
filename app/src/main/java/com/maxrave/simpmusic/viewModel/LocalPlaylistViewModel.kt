package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertFooterItem
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.DownloadState.STATE_DOWNLOADED
import com.maxrave.simpmusic.common.DownloadState.STATE_DOWNLOADING
import com.maxrave.simpmusic.common.DownloadState.STATE_NOT_DOWNLOADED
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.manager.LocalPlaylistManager
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.extension.toListVideoId
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.pagination.PagingActions
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.utils.collectLatestResource
import com.maxrave.simpmusic.utils.collectResource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.inject
import java.time.LocalDateTime

@UnstableApi
@KoinViewModel
class LocalPlaylistViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    override val tag: String
        get() = this.javaClass.simpleName

    private val localPlaylistManager: LocalPlaylistManager by inject()

    private val downloadUtils: DownloadUtils by inject()

    val id: MutableLiveData<Long> = MutableLiveData()

    private var _localPlaylist: MutableStateFlow<LocalPlaylistEntity?> =
        MutableStateFlow(null)
    val localPlaylist: StateFlow<LocalPlaylistEntity?> = _localPlaylist

    private var _listTrack: MutableStateFlow<List<SongEntity>?> = MutableStateFlow(null)
    val listTrack: StateFlow<List<SongEntity>?> = _listTrack

    private var _listPair: MutableStateFlow<List<PairSongLocalPlaylist>?> =
        MutableStateFlow(null)
    val listPair: StateFlow<List<PairSongLocalPlaylist>?> = _listPair

    private var _offset: MutableStateFlow<Int> = MutableStateFlow(0)
    val offset: StateFlow<Int> = _offset

    fun setOffset(offset: Int) {
        _offset.value = offset
    }

    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()

    private var _listSuggestions: MutableStateFlow<ArrayList<Track>?> =
        MutableStateFlow(arrayListOf())
    val listSuggestions: StateFlow<ArrayList<Track>?> = _listSuggestions

    private var reloadParams: MutableStateFlow<String?> = MutableStateFlow(null)

    var loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _loadingMore = MutableStateFlow<Boolean>(false)
    val loadingMore: StateFlow<Boolean> get() = _loadingMore

    private var _filter: MutableStateFlow<FilterState> =
        MutableStateFlow(FilterState.OlderFirst)
    val filter: StateFlow<FilterState> = _filter

    fun setFilter(filterState: FilterState) {
        _filter.value = filterState
    }

    private var _brush: MutableStateFlow<List<Color>> =
        MutableStateFlow(
            listOf(
                Color.Black,
                Color(
                    application.resources.getColor(R.color.md_theme_dark_background, null),
                ),
            ),
        )
    val brush: StateFlow<List<Color>> = _brush

    fun setBrush(brush: List<Color>) {
        _brush.value = brush
    }

    init {
        viewModelScope.launch {
            val checkDownloadedJob =
                launch {
                    listTrack.collect {
                        if (it != null) {
                            mainRepository.getPlaylistPairSong(id.value!!).collect {
                                Log.w("Pair LocalPlaylistViewModel", "init: ${it?.size}")
                            }
                            val temp: ArrayList<SongEntity> = arrayListOf()
                            var count = 0
                            it.forEach { track ->
                                temp.add(track)
                                if (track.downloadState == STATE_DOWNLOADED) {
                                    count++
                                }
                            }
                            localPlaylist.value?.id?.let { id ->
                                if (count == it.size &&
                                    localPlaylist.value?.downloadState != STATE_DOWNLOADED
                                ) {
                                    updatePlaylistDownloadState(
                                        id,
                                        STATE_DOWNLOADED,
                                    )
                                    getLocalPlaylist(id)
                                } else if (
                                    count != it.size &&
                                    localPlaylist.value?.downloadState != STATE_NOT_DOWNLOADED &&
                                    localPlaylist.value?.downloadState != STATE_DOWNLOADING
                                ) {
                                    updatePlaylistDownloadState(
                                        id,
                                        STATE_NOT_DOWNLOADED,
                                    )
                                    getLocalPlaylist(id)
                                } else if (
                                    count < it.size && localPlaylist.value?.downloadState == STATE_DOWNLOADED
                                ) {
                                    updatePlaylistDownloadState(
                                        id,
                                        STATE_NOT_DOWNLOADED,
                                    )
                                    getLocalPlaylist(id)
                                }
                            }
                        }
                    }
                }
            checkDownloadedJob.join()
        }
    }

    private val _tracksPagingState: MutableStateFlow<PagingData<SongEntity>> =
        MutableStateFlow(
            PagingData.empty(),
        )
    val tracksPagingState: StateFlow<PagingData<SongEntity>> get() = _tracksPagingState

    private val modifications = MutableStateFlow<List<PagingActions<SongEntity>>>(emptyList())

    private fun getTracksPagingState() {
        viewModelScope.launch {
            Log.w("LocalPlaylistViewModel", "getTracksPagingState: ${localPlaylist.value}")
            localPlaylist.value?.let { local ->
                modifications.value = listOf()
                localPlaylistManager
                    .getTracksPaging(
                        local.id,
                        filter.value,
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
    }

    private fun applyActions(
        pagingData: PagingData<SongEntity>,
        actions: PagingActions<SongEntity>,
    ): PagingData<SongEntity> =
        when (actions) {
            is PagingActions.Insert -> {
                pagingData.insertFooterItem(item = actions.item)
            }

            is PagingActions.Remove -> {
                pagingData.filter {
                    actions.item.videoId != it.videoId
                }
            }
        }
    }

    private fun onApplyActions(actions: PagingActions<SongEntity>) {
        modifications.value += actions
    }

    fun getSuggestions(ytPlaylistId: String) {
        loading.value = true
        viewModelScope.launch {
            mainRepository.getSuggestionPlaylist(ytPlaylistId).collect {
                _listSuggestions.value = it?.second
                reloadParams.value = it?.first
                withContext(Dispatchers.Main) {
                    loading.value = false
                }
            }
            delay(200)
            mainRepository.getSongById(song.videoId).collect {
                if (it != null) _songEntity.emit(it)
            }
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
            val param = reloadParams.value
            if (param != null) {
                mainRepository.reloadSuggestionPlaylist(param).collect {
                    _listSuggestions.value = it?.second
                    reloadParams.value = it?.first
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
            } else {
                Toast
                    .makeText(
                        application,
                        application.getString(R.string.error),
                        Toast.LENGTH_SHORT,
                    ).show()
                withContext(Dispatchers.Main) {
                    loading.value = false
                }
            } else {
                Toast.makeText(
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

    fun getLocalPlaylist(id: Long) {
        viewModelScope.launch {
            mainRepository.getLocalPlaylist(id).collect {
                _localPlaylist.emit(it)
            }
            localPlaylistManager.getLocalPlaylist(id).collectLatestResource(
                onSuccess = {
                    _localPlaylist.value = it
                    getTracksPagingState()
                },
                onLoading = {
                    _localPlaylist.value = null
                },
                onError = {
                    _localPlaylist.value = null
                },
            )
        }
    }

    fun getListTrack(
        playlistId: Long,
        offset: Int,
        filterState: FilterState,
        totalCount: Int,
    ) {
        viewModelScope.launch {
            _loadingMore.value = true
            val pairJob =
                launch {
                    mainRepository
                        .getPlaylistPairSongByOffset(
                            playlistId,
                            offset,
                            filterState,
                            totalCount,
                        ).cancellable()
                        .singleOrNull()
                        .let { listPairPlaylist ->
                            Log.w("Pair", "getListTrack: $listPairPlaylist")
                            if (listPairPlaylist != null) {
                                if (_listPair.value == null || offset == 0) {
                                    _listPair.value = listPairPlaylist
                                } else {
                                    val temp: ArrayList<PairSongLocalPlaylist> = arrayListOf()
                                    temp.addAll(_listPair.value ?: emptyList())
                                    temp.addAll(listPairPlaylist)
                                    _listPair.value = temp
                                }
                                setOffset(offset + 1)
                                Log.w("Pair LocalPlaylistViewModel", "offset: ${_offset.value}")
                                Log.w("Pair LocalPlaylistViewModel", "listPair: $listPairPlaylist")
                                Log.w("Pair LocalPlaylistViewModel", "firstPair: ${listPairPlaylist.firstOrNull()?.position}")
                                mainRepository
                                    .getSongsByListVideoId(
                                        listPairPlaylist.map { it.songId },
                                    ).collect { list ->
                                        val temp = mutableListOf<SongEntity>()
                                        temp.addAll(listTrack.value ?: emptyList())
                                        val newList =
                                            listPairPlaylist.mapNotNull { pair ->
                                                list.find { it.videoId == pair.songId }
                                            }
                                        temp.addAll(newList)
                                        _listTrack.value = temp
                                        _loadingMore.value = false
                                        Log.w("Pair", "getListTrack: ${_listTrack.value}")
                                        if (listTrack.value?.size == localPlaylist.value?.tracks?.size && localPlaylist.value?.tracks != null) {
                                            setOffset(-1)
                                        }
                                    }
                            } else {
                                setOffset(-1)
                            }
                        } else {
                            setOffset(-1)
                        }
                }
            pairJob.join()
//                mainRepository.getSongsByListVideoIdOffset(list, offset).collect {
//                    val temp: ArrayList<SongEntity> = arrayListOf()
//                    var count = 0
//                    it.forEach { track ->
//                        temp.add(track)
//                        if (track.downloadState == DownloadState.STATE_DOWNLOADED) {
//                            count++
//                        }
//                    }
//                    if (_listTrack.value == null) {
//                        _listTrack.value = (temp)
//                    } else {
//                        val temp2: ArrayList<SongEntity> = arrayListOf()
//                        temp2.addAll(_listTrack.value!!)
//                        temp2.addAll(temp)
//                        _listTrack.value = temp2
//                    }
//                    localPlaylist.value?.id?.let {
//                            id ->
//                        getPairSongLocalPlaylist(id)
//                        if (count == it.size &&
//                            localPlaylist.value?.downloadState != DownloadState.STATE_DOWNLOADED
//                        ) {
//                            updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADED)
//                            getLocalPlaylist(id)
//                        } else if (
//                            count != it.size &&
//                            localPlaylist.value?.downloadState != DownloadState.STATE_NOT_DOWNLOADED &&
//                            localPlaylist.value?.downloadState != DownloadState.STATE_DOWNLOADING
//                        ) {
//                            updatePlaylistDownloadState(
//                                id,
//                                DownloadState.STATE_NOT_DOWNLOADED,
//                            )
//                            getLocalPlaylist(id)
//                        }
//                    }
//                }
        }
    }

    val playlistDownloadState: MutableStateFlow<Int> =
        MutableStateFlow(STATE_NOT_DOWNLOADED)

    fun updatePlaylistDownloadState(
        id: Long,
        state: Int,
    ) {
        viewModelScope.launch {
            mainRepository.getLocalPlaylist(id).collect { playlist ->
                _localPlaylist.value = playlist
                mainRepository.updateLocalPlaylistDownloadState(state, id)
                playlistDownloadState.value = state
            }
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

    @UnstableApi
    fun getDownloadStateFromService(videoId: String) {
        viewModelScope.launch {
            val downloadState = downloadUtils.getDownload(videoId).stateIn(viewModelScope)
            downloadState.collect { down ->
                if (down != null) {
                    when (down.state) {
                        Download.STATE_COMPLETED -> {
                            mainRepository.getSongById(videoId).collect { song ->
                                if (song?.downloadState != STATE_DOWNLOADED) {
                                    mainRepository.updateDownloadState(
                                        videoId,
                                        STATE_DOWNLOADED,
                                    )
                                    _listTrack.value?.find { it.videoId == videoId }?.copy(downloadState = STATE_DOWNLOADED)?.let { copy ->
                                        val temp: ArrayList<SongEntity> = arrayListOf()
                                        temp.addAll(listTrack.value ?: emptyList())
                                        temp.replaceAll { s -> if (s.videoId == videoId) copy else s }
                                        _listTrack.value = temp.toList()
                                    }
                                }
                            }
                            Log.d("Check Downloaded", "Downloaded")
                        }

                        Download.STATE_FAILED -> {
                            mainRepository.getSongById(videoId).collect { song ->
                                if (song?.downloadState != STATE_NOT_DOWNLOADED) {
                                    mainRepository.updateDownloadState(
                                        videoId,
                                        STATE_NOT_DOWNLOADED,
                                    )
                                }
                            }
                            Log.d("Check Downloaded", "Failed")
                        }

                        Download.STATE_DOWNLOADING -> {
                            mainRepository.getSongById(videoId).collect { song ->
                                if (song?.downloadState != DownloadState.STATE_DOWNLOADING) {
                                    mainRepository.updateDownloadState(
                                        videoId,
                                        DownloadState.STATE_DOWNLOADING,
                                    )
                                }
                            }
                            Log.d("Check Downloaded", "Downloading ${down.percentDownloaded}")
                        }

                        Download.STATE_QUEUED -> {
                            mainRepository.getSongById(videoId).collect { song ->
                                if (song?.downloadState != DownloadState.STATE_PREPARING) {
                                    mainRepository.updateDownloadState(
                                        videoId,
                                        DownloadState.STATE_PREPARING,
                                    )
                                }
                            }
                            Log.d("Check Downloaded", "Queued")
                        }

                        else -> {
                            Log.d("Check Downloaded", "Not Downloaded")
                        }
                    }
                }
            }
        }
    }

    fun updatePlaylistTitle(
        title: String,
        id: Long,
    ) {
        viewModelScope.launch {
            localPlaylistManager
                .updateTitleLocalPlaylist(id, title)
                .collectResource(
                    onSuccess = {
                        makeToast(it)
                        getLocalPlaylist(id)
                    },
                    onError = {
                        makeToast(it)
                    },
                )
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            localPlaylistManager.deleteLocalPlaylist(id).collectLatestResource(
                onSuccess = {
                    makeToast(it)
                },
                onError = {
                    makeToast(it)
                },
            )
        }
    }

    fun updatePlaylistThumbnail(
        uri: String,
        id: Long,
    ) {
        viewModelScope.launch {
            localPlaylistManager.updateThumbnailLocalPlaylist(id, uri).collectResource(
                onSuccess = {
                    makeToast(it)
                    getLocalPlaylist(id)
                },
                onError = {
                    makeToast(it)
                },
            )
        }
    }

    fun clearLocalPlaylist() {
        _localPlaylist.value = null
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
                },
                onError = {
                    makeToast(it)
                },
            )
        }
    }

    @UnstableApi
    fun downloadFullPlaylistState(id: Long) {
        viewModelScope.launch {
            downloadUtils.downloads.collect { download ->
                playlistDownloadState.value =
                    if (listJob.value.all { download[it.videoId]?.state == Download.STATE_COMPLETED }) {
                        mainRepository.updateLocalPlaylistDownloadState(
                            STATE_DOWNLOADED,
                            id,
                        )
                        STATE_DOWNLOADED
                    } else if (listJob.value.all {
                            download[it.videoId]?.state == Download.STATE_QUEUED ||
                                download[it.videoId]?.state == Download.STATE_DOWNLOADING ||
                                download[it.videoId]?.state == Download.STATE_COMPLETED
                        }
                    ) {
                        mainRepository.updateLocalPlaylistDownloadState(
                            DownloadState.STATE_DOWNLOADING,
                            id,
                        )
                        DownloadState.STATE_DOWNLOADING
                    } else {
                        mainRepository.updateLocalPlaylistDownloadState(
                            STATE_NOT_DOWNLOADED,
                            id,
                        )
                        STATE_NOT_DOWNLOADED
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

    fun syncPlaylistWithYouTubePlaylist(playlist: LocalPlaylistEntity) {
        viewModelScope.launch {
            mainRepository.createYouTubePlaylist(playlist).collect {
                if (it != null) {
                    val ytId = "VL$it"
                    mainRepository.updateLocalPlaylistYouTubePlaylistId(playlist.id, ytId)
                    mainRepository.updateLocalPlaylistYouTubePlaylistSynced(playlist.id, 1)
                    mainRepository.getLocalPlaylistByYoutubePlaylistId(ytId).collect { yt ->
                        if (yt != null) {
                            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                                yt.id,
                                LocalPlaylistEntity.YouTubeSyncState.Synced,
                            )
                            mainRepository.getLocalPlaylist(playlist.id).collect { last ->
                                _localPlaylist.emit(last)
                                Toast
                                    .makeText(
                                        application,
                                        application.getString(R.string.synced),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                    }
                } else {
                    Toast
                        .makeText(
                            application,
                            application.getString(R.string.error),
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }

    fun unsyncPlaylistWithYouTubePlaylist(playlist: LocalPlaylistEntity) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistId(playlist.id, null)
            mainRepository.updateLocalPlaylistYouTubePlaylistSynced(playlist.id, 0)
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                playlist.id,
                LocalPlaylistEntity.YouTubeSyncState.NotSynced,
            )
            mainRepository.getLocalPlaylist(playlist.id).collect { last ->
                if (last.syncedWithYouTubePlaylist == 0) {
                    _localPlaylist.emit(last)
                    Toast
                        .makeText(
                            application,
                            application.getString(R.string.unsynced),
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }

    fun updateYouTubePlaylistTitle(
        title: String,
        youtubePlaylistId: String,
    ) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                localPlaylist.value?.id!!,
                LocalPlaylistEntity.YouTubeSyncState.Syncing,
            )
            mainRepository.editYouTubePlaylist(title, youtubePlaylistId).collect { status ->
                if (status == 200) {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylist.value?.id!!,
                        LocalPlaylistEntity.YouTubeSyncState.Synced,
                    )
                    Toast
                        .makeText(
                            application,
                            application.getString(R.string.synced),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylist.value?.id!!,
                        LocalPlaylistEntity.YouTubeSyncState.NotSynced,
                    )
                    Toast
                        .makeText(
                            application,
                            application.getString(R.string.error),
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }

    fun updateListTrackSynced(
        id: Long,
        list: List<String>,
        youtubeId: String,
    ) {
        viewModelScope.launch {
            mainRepository.getPlaylistData(youtubeId).collect { yt ->
                if (yt is Resource.Success) {
                    if (yt.data != null) {
                        val listTrack: ArrayList<String> = arrayListOf()
                        listTrack.addAll(list)
                        yt.data.tracks.forEach { track ->
                            if (!list.contains(track.videoId)) {
                                listTrack.add(track.videoId)
                                mainRepository.insertSong(track.toSongEntity()).first().let {
                                    println("Insert song $it")
                                }
                                mainRepository.insertPairSongLocalPlaylist(
                                    PairSongLocalPlaylist(
                                        playlistId = id,
                                        songId = track.videoId,
                                        position = yt.data.tracks.indexOf(track),
                                        inPlaylist = LocalDateTime.now(),
                                    ),
                                )
                            }
                        }
                        mainRepository.updateLocalPlaylistTracks(listTrack, id)
                        if (yt.data.tracks.size < list.size) {
                            list.forEach { track2 ->
                                if (!yt.data.tracks
                                        .toListVideoId()
                                        .contains(track2)
                                ) {
                                    mainRepository
                                        .addYouTubePlaylistItem(youtubeId, track2)
                                        .collect { status ->
                                            if (status == "STATUS_SUCCEEDED") {
                                                Toast
                                                    .makeText(
                                                        application,
                                                        application.getString(
                                                            R.string.added_to_youtube_playlist,
                                                        ),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            } else {
                                                Toast
                                                    .makeText(
                                                        application,
                                                        application.getString(R.string.error),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            }
                                        }
                                }
                            }
                        }
                        Toast
                            .makeText(
                                application,
                                application.getString(R.string.synced),
                                Toast.LENGTH_SHORT,
                            ).show()
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                            id,
                            LocalPlaylistEntity.YouTubeSyncState.Synced,
                        )
                        mainRepository.getLocalPlaylist(id).collect { last ->
                            _localPlaylist.emit(last)
                        }
                    }
                }
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
                getLocalPlaylist(id)
                getPairSongLocalPlaylist(id)
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

    fun getPairSongLocalPlaylist(id: Long) {
        viewModelScope.launch {
            mainRepository.getPlaylistPairSong(id).collect {
                _listPair.value = (it)
                Log.w("Pair", "getPairSongLocalPlaylist: $it")
            }
        }
    }

    fun removeListSuggestion() {
        _listSuggestions.value = null
    }

    fun removeData() {
        _localPlaylist.value = null
        _listTrack.value = null
        _listPair.value = null
    }

    fun updateLikeStatus(
        videoId: String,
        likeStatus: Int,
    ) {
        viewModelScope.launch {
            mainRepository.updateLikeStatus(likeStatus = likeStatus, videoId = videoId)
            delay(150)
            mainRepository.getSongById(videoId).collect { song ->
                if (song != null) {
                    val temp = mutableListOf<SongEntity>()
                    temp.addAll(_listTrack.value ?: emptyList())
                    temp.replaceAll { s -> if (s.videoId == videoId) song else s }
                    _listTrack.value = temp.toList()
                }
            }
        }
    }

    fun clearListPair() {
        _listPair.value = null
    }

    fun addSuggestTrackToListTrack(track: Track) {
        viewModelScope.launch {
            _listSuggestions.value?.remove(track)
            localPlaylist.value?.let { local ->
                localPlaylistManager
                    .addTrackToLocalPlaylist(local.id, track.toSongEntity())
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

    private fun isLoadedFullPlaylist(): Boolean = _listTrack.value?.size == _localPlaylist.value?.tracks?.size

    fun clearListTracks() {
        _listTrack.value = null
    }

    fun onUIEvent(ev: LocalPlaylistUIEvent) {
        when (ev) {
            is LocalPlaylistUIEvent.ChangeFilter -> {
                if (_filter.value == FilterState.OlderFirst) {
                    setFilter(FilterState.NewerFirst)
                } else {
                    setFilter(FilterState.OlderFirst)
                }
                Log.w("PlaylistScreen", "new filterState: ${filter.value}")
                setOffset(0)
                clearListPair()
                clearListTracks()
                if (localPlaylist.value != null) {
                    localPlaylist.value?.let {
                        Log.w(
                            "Pair LocalPlaylistViewModel",
                            "localPlaylist: ${it.tracks?.size}, listTrack: ${listTrack.value?.size}, listPair: ${listPair.value?.size}",
                        )
                        getListTrack(it.id, offset.value, filter.value, it.tracks?.size ?: 0)
                        // New
                        getLocalPlaylist(it.id)
                    }
                }
            }
        }
    }

    fun downloadTracks(listJob: List<String>) {
        viewModelScope.launch {
            listJob.forEach { videoId ->
                mainRepository.getSongById(videoId).singleOrNull()?.let { song ->
                    if (song.downloadState != STATE_DOWNLOADED) {
                        downloadUtils.downloadTrack(videoId, song.title)
                    }
                }
            }
        }
    }
}

sealed class FilterState {
    data object OlderFirst : FilterState()

    data object NewerFirst : FilterState()
}

sealed class LocalPlaylistUIEvent {
    data object ChangeFilter : LocalPlaylistUIEvent()
}