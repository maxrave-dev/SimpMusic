package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.PodcastRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

class LibraryViewModel(
    application: Application,
    private val dataStoreManager: DataStoreManager,
    private val songRepository: SongRepository,
    private val commonRepository: CommonRepository,
    private val playlistRepository: PlaylistRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val podcastRepository: PodcastRepository,
) : BaseViewModel(application) {
    private val _recentlyAdded: MutableStateFlow<LocalResource<List<RecentlyType>>> =
        MutableStateFlow(LocalResource.Loading())
    val recentlyAdded: StateFlow<LocalResource<List<RecentlyType>>> get() = _recentlyAdded.asStateFlow()

    private val _yourLocalPlaylist: MutableStateFlow<LocalResource<List<LocalPlaylistEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val yourLocalPlaylist: StateFlow<LocalResource<List<LocalPlaylistEntity>>> get() = _yourLocalPlaylist.asStateFlow()

    private val _youTubePlaylist: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubePlaylist: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubePlaylist.asStateFlow()

    private val _youTubeMixForYou: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
        MutableStateFlow(LocalResource.Loading())
    val youTubeMixForYou: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubeMixForYou.asStateFlow()

    private val _favoritePlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePlaylist.asStateFlow()

    private val _favoritePodcasts: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val favoritePodcasts: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePodcasts.asStateFlow()

    private val _downloadedPlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
        MutableStateFlow(LocalResource.Loading())
    val downloadedPlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _downloadedPlaylist.asStateFlow()

    private val _listCanvasSong: MutableStateFlow<LocalResource<List<SongEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val listCanvasSong: StateFlow<LocalResource<List<SongEntity>>> get() = _listCanvasSong.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val youtubeLoggedIn = dataStoreManager.loggedIn.mapLatest { it == DataStoreManager.TRUE }

    fun getRecentlyAdded() {
        viewModelScope.launch {
            commonRepository.getAllRecentData().collectLatest { data ->
                val temp: MutableList<RecentlyType> = mutableListOf()
                temp.addAll(data)
                temp
                    .find {
                        it is PlaylistEntity && (it.id.contains("RDEM") || it.id.contains("RDAMVM"))
                    }.let {
                        temp.remove(it)
                    }
                temp.removeIf { it is SongEntity && it.inLibrary == Config.REMOVED_SONG_DATE_TIME }
                temp.reverse()
                _recentlyAdded.value = LocalResource.Success(temp.toImmutableList())
            }
        }
    }

    fun getYouTubePlaylist() {
        _youTubePlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getLibraryPlaylist().collect { data ->
                _youTubePlaylist.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeMixedForYou() {
        _youTubeMixForYou.value = LocalResource.Loading()
        viewModelScope.launch {
            playlistRepository.getMixedForYou().collect { data ->
                _youTubeMixForYou.value = LocalResource.Success(data ?: emptyList())
            }
        }
    }

    fun getYouTubeLoggedIn(): Boolean = runBlocking { dataStoreManager.loggedIn.first() } == DataStoreManager.TRUE

    fun getPlaylistFavorite() {
        viewModelScope.launch {
            albumRepository.getLikedAlbums().collect { album ->
                val temp: MutableList<PlaylistType> = mutableListOf()
                temp.addAll(album)
                playlistRepository.getLikedPlaylists().collect { playlist ->
                    temp.addAll(playlist)
                    val sortedList =
                        temp.sortedWith<PlaylistType>(
                            Comparator { p0, p1 ->
                                val timeP0: LocalDateTime? =
                                    when (p0) {
                                        is AlbumEntity -> p0.favoriteAt ?: p0.inLibrary
                                        is PlaylistEntity -> p0.favoriteAt ?: p0.inLibrary
                                        else -> null
                                    }
                                val timeP1: LocalDateTime? =
                                    when (p1) {
                                        is AlbumEntity -> p1.favoriteAt ?: p1.inLibrary
                                        is PlaylistEntity -> p1.favoriteAt ?: p1.inLibrary
                                        else -> null
                                    }
                                if (timeP0 == null || timeP1 == null) {
                                    return@Comparator if (timeP0 == null && timeP1 == null) {
                                        0
                                    } else if (timeP0 == null) {
                                        -1
                                    } else {
                                        1
                                    }
                                }
                                timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
                            },
                        )
                    _favoritePlaylist.value = LocalResource.Success(sortedList)
                }
            }
        }
    }

    fun getFavoritePodcasts() {
        viewModelScope.launch {
            podcastRepository.getFavoritePodcasts().collectLatest { podcasts ->
                val sortedList = podcasts.sortedByDescending { it.favoriteTime }
                _favoritePodcasts.value = LocalResource.Success(sortedList)
            }
        }
    }

    fun getCanvasSong() {
        _listCanvasSong.value = LocalResource.Loading()
        viewModelScope.launch {
            songRepository.getCanvasSong(max = 5).collect { data ->
                _listCanvasSong.value = LocalResource.Success(data)
            }
        }
    }

    fun getLocalPlaylist() {
        _yourLocalPlaylist.value = LocalResource.Loading()
        viewModelScope.launch {
            localPlaylistRepository.getAllLocalPlaylists().collect { values ->
//                    _listLocalPlaylist.postValue(values)
                _yourLocalPlaylist.value = LocalResource.Success(values.reversed())
            }
        }
    }

    fun getDownloadedPlaylist() {
        viewModelScope.launch {
            playlistRepository.getAllDownloadedPlaylist().collect { values ->
                _downloadedPlaylist.value = LocalResource.Success(values)
            }
        }
    }

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            val localPlaylistEntity = LocalPlaylistEntity(title = title)
            localPlaylistRepository.insertLocalPlaylist(localPlaylistEntity).lastOrNull()?.let {
                log("Created playlist with id: $it")
            }
            getLocalPlaylist()
        }
    }

    fun deleteSong(videoId: String) {
        _recentlyAdded.value = LocalResource.Loading()
        viewModelScope.launch {
            songRepository.setInLibrary(videoId, Config.REMOVED_SONG_DATE_TIME)
            songRepository.resetTotalPlayTime(videoId)
            delay(500) // Wait for the database to update
            getRecentlyAdded()
        }
    }
}