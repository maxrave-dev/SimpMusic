package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.maxrave.kotlinytmusicscraper.models.YTItem
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.extension.toQueryList
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// State cho tìm kiếm
data class SearchScreenState(
    val searchType: SearchType = SearchType.ALL,
    val searchAllResult: List<Any> = emptyList(),
    val searchSongsResult: List<SongsResult> = emptyList(),
    val searchVideosResult: List<VideosResult> = emptyList(),
    val searchAlbumsResult: List<AlbumsResult> = emptyList(),
    val searchArtistsResult: List<ArtistsResult> = emptyList(),
    val searchPlaylistsResult: List<PlaylistsResult> = emptyList(),
    val searchFeaturedPlaylistsResult: List<PlaylistsResult> = emptyList(),
    val searchPodcastsResult: List<PlaylistsResult> = emptyList(),
    val suggestQueries: List<String> = emptyList(),
    val suggestYTItems: List<YTItem> = emptyList(),
)

// Loại tìm kiếm
enum class SearchType {
    ALL,
    SONGS,
    VIDEOS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    FEATURED_PLAYLISTS,
    PODCASTS,
}

fun SearchType.toStringRes(): Int =
    when (this) {
        SearchType.ALL -> R.string.all
        SearchType.SONGS -> R.string.songs
        SearchType.VIDEOS -> R.string.videos
        SearchType.ALBUMS -> R.string.albums
        SearchType.ARTISTS -> R.string.artists
        SearchType.PLAYLISTS -> R.string.playlists
        SearchType.FEATURED_PLAYLISTS -> R.string.featured_playlists
        SearchType.PODCASTS -> R.string.podcasts
    }

// UI state cho tìm kiếm
sealed class SearchScreenUIState {
    object Empty : SearchScreenUIState()

    object Loading : SearchScreenUIState()

    object Success : SearchScreenUIState()

    object Error : SearchScreenUIState()
}

@UnstableApi
class SearchViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    private val _searchScreenUIState = MutableStateFlow<SearchScreenUIState>(SearchScreenUIState.Empty)
    val searchScreenUIState: StateFlow<SearchScreenUIState> get() = _searchScreenUIState.asStateFlow()

    private val _searchScreenState = MutableStateFlow(SearchScreenState())
    val searchScreenState: StateFlow<SearchScreenState> get() = _searchScreenState.asStateFlow()

    private val _searchHistory: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val searchHistory: StateFlow<List<String>> get() = _searchHistory.asStateFlow()

    var regionCode: String? = null
    var language: String? = null

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
        getSearchHistory()
    }

    private fun getSearchHistory() {
        viewModelScope.launch {
            mainRepository.getSearchHistory().collect { values ->
                if (values.isNotEmpty()) {
                    values.toQueryList().reversed().let { list ->
                        _searchHistory.value = list
                        log("Search history updated: $list")
                    }
                }
            }
        }
    }

    fun insertSearchHistory(query: String) {
        viewModelScope.launch {
            mainRepository.insertSearchHistory(SearchHistory(query = query)).collectLatest {
                Log.d(tag, "Inserted search history: $query, $it")
                getSearchHistory()
            }
        }
    }

    fun deleteSearchHistory() {
        viewModelScope.launch {
            mainRepository.deleteSearchHistory()
            delay(1000)
            getSearchHistory()
        }
    }

    fun searchSongs(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            mainRepository.getSearchDataSong(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { songsList ->
                            _searchScreenState.update { state ->
                                state.copy(searchSongsResult = songsList)
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }
                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchAll(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            var song = ArrayList<SongsResult>()
            val video = ArrayList<VideosResult>()
            var album = ArrayList<AlbumsResult>()
            var artist = ArrayList<ArtistsResult>()
            var playlist = ArrayList<PlaylistsResult>()
            var featuredPlaylist = ArrayList<PlaylistsResult>()
            var podcast = ArrayList<PlaylistsResult>()
            val temp: ArrayList<Any> = ArrayList()

            val job1 =
                launch {
                    mainRepository.getSearchDataSong(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { song = it }
                            is Resource.Error -> {}
                        }
                    }
                }
            val job2 =
                launch {
                    mainRepository.getSearchDataArtist(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { artist = it }
                            is Resource.Error -> {}
                        }
                    }
                }
            val job3 =
                launch {
                    mainRepository.getSearchDataAlbum(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { album = it }
                            is Resource.Error -> {}
                        }
                    }
                }
            val job4 =
                launch {
                    mainRepository.getSearchDataPlaylist(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { playlist = it }
                            is Resource.Error -> {}
                        }
                    }
                }
            val job5 =
                launch {
                    mainRepository.getSearchDataVideo(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { video.addAll(it) }
                            is Resource.Error -> {}
                        }
                    }
                }
            val job6 =
                launch {
                    mainRepository.getSearchDataFeaturedPlaylist(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { featuredPlaylist = it }
                            is Resource.Error -> {}
                        }
                    }
                }
            val job7 =
                launch {
                    mainRepository.getSearchDataPodcast(query).collect { values ->
                        when (values) {
                            is Resource.Success -> values.data?.let { podcast = it }
                            is Resource.Error -> {}
                        }
                    }
                }
            job1.join()
            job2.join()
            job3.join()
            job4.join()
            job5.join()
            job6.join()
            job7.join()

            try {
                if (artist.size >= 3) {
                    for (i in 0..2) {
                        temp += artist[i]
                    }
                    temp.addAll(song)
                    temp.addAll(video)
                    temp.addAll(album)
                    temp.addAll(playlist)
                    temp.addAll(featuredPlaylist)
                    temp.addAll(podcast)
                } else {
                    temp.addAll(artist)
                    temp.addAll(song)
                    temp.addAll(video)
                    temp.addAll(album)
                    temp.addAll(playlist)
                    temp.addAll(featuredPlaylist)
                    temp.addAll(podcast)
                }

                _searchScreenState.update { state ->
                    state.copy(
                        searchType = SearchType.ALL,
                        searchAllResult = temp,
                        searchSongsResult = song,
                        searchArtistsResult = artist,
                        searchAlbumsResult = album,
                        searchPlaylistsResult = playlist,
                        searchVideosResult = video,
                        searchFeaturedPlaylistsResult = featuredPlaylist,
                        searchPodcastsResult = podcast,
                    )
                }
                _searchScreenUIState.value = SearchScreenUIState.Success
            } catch (e: Exception) {
                e.printStackTrace()
                _searchScreenUIState.value = SearchScreenUIState.Error
            }
        }
    }

    fun suggestQuery(query: String) {
        viewModelScope.launch {
            mainRepository.getSuggestQuery(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { suggestData ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    suggestQueries = suggestData.queries,
                                    suggestYTItems = suggestData.recommendedItems,
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        // Không cần xử lý lỗi đặc biệt cho gợi ý
                        log("Error fetching suggest queries: ${values.message}", Log.ERROR)
                    }
                }
            }
        }
    }

    fun searchAlbums(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            mainRepository.getSearchDataAlbum(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { albumsList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.ALBUMS,
                                    searchAlbumsResult = albumsList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }
                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchFeaturedPlaylist(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            mainRepository.getSearchDataFeaturedPlaylist(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { featuredPlaylistList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.FEATURED_PLAYLISTS,
                                    searchFeaturedPlaylistsResult = featuredPlaylistList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }
                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchPodcast(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            mainRepository.getSearchDataPodcast(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { podcastList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.PODCASTS,
                                    searchPodcastsResult = podcastList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }
                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchArtists(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            mainRepository.getSearchDataArtist(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { artistsList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.ARTISTS,
                                    searchArtistsResult = artistsList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }
                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchPlaylists(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            mainRepository.getSearchDataPlaylist(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { playlistsList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.PLAYLISTS,
                                    searchPlaylistsResult = playlistsList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }
                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun searchVideos(query: String) {
        _searchScreenUIState.value = SearchScreenUIState.Loading
        viewModelScope.launch {
            mainRepository.getSearchDataVideo(query).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        values.data?.let { videosList ->
                            _searchScreenState.update { state ->
                                state.copy(
                                    searchType = SearchType.VIDEOS,
                                    searchVideosResult = videosList,
                                )
                            }
                        }
                        _searchScreenUIState.value = SearchScreenUIState.Success
                    }
                    is Resource.Error -> {
                        _searchScreenUIState.value = SearchScreenUIState.Error
                    }
                }
            }
        }
    }

    fun updateLikeStatus(
        videoId: String,
        b: Boolean,
    ) {
        viewModelScope.launch {
            if (b) {
                mainRepository.updateLikeStatus(videoId, 1)
            } else {
                mainRepository.updateLikeStatus(videoId, 0)
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
                if (count == values.size) {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_DOWNLOADED, id)
                } else {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_NOT_DOWNLOADED, id)
                }
            }
        }
    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

    fun setSearchType(searchType: SearchType) {
        _searchScreenState.update { state ->
            state.copy(searchType = searchType)
        }
    }
}