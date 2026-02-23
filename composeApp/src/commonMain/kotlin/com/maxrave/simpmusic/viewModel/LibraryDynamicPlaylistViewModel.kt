package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.toArrayListTrack
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.ui.screen.library.DownloadSortType
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.playlist

class LibraryDynamicPlaylistViewModel(
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
) : BaseViewModel() {
    private val dataStoreManager: DataStoreManager by inject()

    private val _listFavoriteSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listFavoriteSong: StateFlow<List<SongEntity>> get() = _listFavoriteSong

    private val _listFollowedArtist: MutableStateFlow<List<ArtistEntity>> = MutableStateFlow(emptyList())
    val listFollowedArtist: StateFlow<List<ArtistEntity>> get() = _listFollowedArtist

    private val _listMostPlayedSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listMostPlayedSong: StateFlow<List<SongEntity>> get() = _listMostPlayedSong

    // Raw downloaded songs from repository (unsorted)
    private val _rawDownloadedSongs: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())

    // Current download sort preference
    private val _downloadSortType: MutableStateFlow<DownloadSortType> = MutableStateFlow(DownloadSortType.DEFAULT)
    val downloadSortType: StateFlow<DownloadSortType> get() = _downloadSortType

    // Sorted downloaded songs (combines raw data + sort type)
    private val _listDownloadedSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listDownloadedSong: StateFlow<List<SongEntity>> get() = _listDownloadedSong

    init {
        getFavoriteSong()
        getFollowedArtist()
        getMostPlayedSong()
        loadDownloadSortPreference()
        getDownloadedSong()
        observeSortedDownloads()
    }

    private fun loadDownloadSortPreference() {
        viewModelScope.launch {
            val savedKey = dataStoreManager.getString(DownloadSortType.PREFERENCE_KEY).first()
            _downloadSortType.value = DownloadSortType.fromKey(savedKey)
        }
    }

    fun setDownloadSort(sortType: DownloadSortType) {
        _downloadSortType.value = sortType
        viewModelScope.launch {
            dataStoreManager.putString(DownloadSortType.PREFERENCE_KEY, sortType.toKey())
        }
    }

    private fun getFavoriteSong() {
        viewModelScope.launch {
            songRepository.getLikedSongs().collectLatest { likedSong ->
                _listFavoriteSong.value =
                    likedSong.sortedByDescending {
                        it.favoriteAt ?: it.inLibrary
                    }
            }
        }
    }

    private fun getFollowedArtist() {
        viewModelScope.launch {
            artistRepository.getFollowedArtists().collectLatest { followedArtist ->
                _listFollowedArtist.value =
                    followedArtist.sortedByDescending {
                        it.followedAt ?: it.inLibrary
                    }
            }
        }
    }

    private fun getMostPlayedSong() {
        viewModelScope.launch {
            songRepository.getMostPlayedSongs().collectLatest { mostPlayedSong ->
                _listMostPlayedSong.value = mostPlayedSong.sortedByDescending { it.totalPlayTime }
            }
        }
    }

    private fun getDownloadedSong() {
        viewModelScope.launch {
            songRepository.getDownloadedSongs().collectLatest { downloadedSong ->
                _rawDownloadedSongs.value = downloadedSong ?: emptyList()
            }
        }
    }

    /**
     * Observes both the raw downloaded songs list and the current sort type,
     * automatically re-sorting whenever either changes.
     */
    private fun observeSortedDownloads() {
        viewModelScope.launch {
            combine(_rawDownloadedSongs, _downloadSortType) { songs, sortType ->
                applySorting(songs, sortType)
            }.collectLatest { sorted ->
                _listDownloadedSong.value = sorted
            }
        }
    }

    private fun applySorting(
        songs: List<SongEntity>,
        sortType: DownloadSortType,
    ): List<SongEntity> =
        when (sortType) {
            DownloadSortType.TitleAsc -> songs.sortedBy { it.title.lowercase() }
            DownloadSortType.TitleDesc -> songs.sortedByDescending { it.title.lowercase() }
            DownloadSortType.DateNewest -> songs.sortedByDescending { it.downloadedAt ?: it.inLibrary }
            DownloadSortType.DateOldest -> songs.sortedBy { it.downloadedAt ?: it.inLibrary }
            DownloadSortType.ArtistAsc -> songs.sortedBy { it.artistName?.firstOrNull()?.lowercase() ?: "" }
            DownloadSortType.ArtistDesc -> songs.sortedByDescending { it.artistName?.firstOrNull()?.lowercase() ?: "" }
        }

    fun playSong(
        videoId: String,
        type: LibraryDynamicPlaylistType,
    ) {
        val (targetList, playTrack) =
            when (type) {
                LibraryDynamicPlaylistType.Favorite -> listFavoriteSong.value to listFavoriteSong.value.find { it.videoId == videoId }
                LibraryDynamicPlaylistType.Downloaded -> listDownloadedSong.value to listDownloadedSong.value.find { it.videoId == videoId }
                LibraryDynamicPlaylistType.Followed -> return
                LibraryDynamicPlaylistType.MostPlayed -> listMostPlayedSong.value to listMostPlayedSong.value.find { it.videoId == videoId }
                else -> return
            }
        if (playTrack == null) return
        setQueueData(
            QueueData.Data(
                listTracks = targetList.toArrayListTrack(),
                firstPlayedTrack = playTrack.toTrack(),
                playlistId = null,
                playlistName = "${
                    getString(
                        Res.string.playlist,
                    )
                } ${getString(type.name())}",
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(
            playTrack.toTrack(),
            Config.PLAYLIST_CLICK,
            targetList.indexOf(playTrack).coerceAtLeast(0),
        )
    }
}