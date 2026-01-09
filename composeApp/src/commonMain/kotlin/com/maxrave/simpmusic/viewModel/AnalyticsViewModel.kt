package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.analytics.query.TopPlayedAlbum
import com.maxrave.domain.data.entities.analytics.query.TopPlayedArtist
import com.maxrave.domain.data.entities.analytics.query.TopPlayedTracks
import com.maxrave.domain.extension.now
import com.maxrave.domain.extension.startTimestampOfThisYear
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository
): BaseViewModel() {
    private val _analyticsUIState: MutableStateFlow<AnalyticsUiState> =
        MutableStateFlow(AnalyticsUiState())
    val analyticsUiState: StateFlow<AnalyticsUiState> get() = _analyticsUIState.asStateFlow()

    init {
        getScrobblesCount()
        getArtistCount()
        getTotalListenTime()
        getTopTracks(AnalyticsUiState.DayRange.LAST_7_DAYS)
        getTopArtists(AnalyticsUiState.DayRange.LAST_7_DAYS)
        getTopAlbums(AnalyticsUiState.DayRange.LAST_7_DAYS)
    }

    private fun getScrobblesCount() {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    scrobblesCount = LocalResource.Loading()
                )
            }
            analyticsRepository.getTotalPlaybackEventCount().collect { count ->
                _analyticsUIState.update {
                    it.copy(
                        scrobblesCount = LocalResource.Success(count)
                    )
                }
            }
        }
    }

    private fun getArtistCount() {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    artistCount = LocalResource.Loading()
                )
            }
            analyticsRepository.getTotalEventArtistCount().collect { count ->
                _analyticsUIState.update {
                    it.copy(
                        artistCount = LocalResource.Success(count)
                    )
                }
            }
        }
    }

    private fun getTotalListenTime() {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    totalListenTimeInSeconds = LocalResource.Loading()
                )
            }
            analyticsRepository.getTotalListeningTimeInSeconds().collect { total ->
                _analyticsUIState.update {
                    it.copy(
                        totalListenTimeInSeconds = LocalResource.Success(total)
                    )
                }
            }
        }
    }

    private fun getTopTracks(dayRange: AnalyticsUiState.DayRange) {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    topTracks = LocalResource.Loading()
                )
            }
            if (dayRange == AnalyticsUiState.DayRange.THIS_YEAR) {
                analyticsRepository.queryTopPlayedSongsInRange(
                    startTimestamp = startTimestampOfThisYear(),
                    endTimestamp = now()
                ).collect { topPlayedTracks ->
                    topPlayedTracks.mapNotNull {
                        val song = songRepository.getSongById(it.videoId).lastOrNull() ?: return@mapNotNull null
                        it to song
                    }.let { pairs ->
                        _analyticsUIState.update {
                            it.copy(
                                topTracks = LocalResource.Success(pairs)
                            )
                        }
                    }
                }
            } else {
                val x = when (dayRange) {
                    AnalyticsUiState.DayRange.LAST_7_DAYS -> 7
                    AnalyticsUiState.DayRange.LAST_30_DAYS -> 30
                    AnalyticsUiState.DayRange.LAST_90_DAYS -> 90
                }
                analyticsRepository.queryTopPlayedSongsLastXDays(x).collect { topPlayedTracks ->
                    topPlayedTracks.mapNotNull {
                        val song = songRepository.getSongById(it.videoId).lastOrNull() ?: return@mapNotNull null
                        it to song
                    }.let { pairs ->
                        log("Top played tracks: $pairs")
                        _analyticsUIState.update {
                            it.copy(
                                topTracks = LocalResource.Success(pairs)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getTopArtists(dayRange: AnalyticsUiState.DayRange) {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    topArtists = LocalResource.Loading()
                )
            }
            if (dayRange == AnalyticsUiState.DayRange.THIS_YEAR) {
                analyticsRepository.queryTopArtistsInRange(
                    startTimestamp = startTimestampOfThisYear(),
                    endTimestamp = now()
                ).collect { topPlayedArtists ->
                    topPlayedArtists.mapNotNull { topPlayedArtist ->
                        val artist = artistRepository.getArtistById(topPlayedArtist.channelId).lastOrNull() ?:
                        getArtistFromYouTube(topPlayedArtist.channelId) ?: return@mapNotNull null
                        topPlayedArtist to artist
                    }.let { pairs ->
                        log("Top played artists: $pairs")
                        _analyticsUIState.update {
                            it.copy(
                                topArtists = LocalResource.Success(pairs)
                            )
                        }
                    }
                }
            } else {
                val x = when (dayRange) {
                    AnalyticsUiState.DayRange.LAST_7_DAYS -> 7
                    AnalyticsUiState.DayRange.LAST_30_DAYS -> 30
                    AnalyticsUiState.DayRange.LAST_90_DAYS -> 90
                }
                analyticsRepository.queryTopArtistsLastXDays(x).collect { topPlayedArtists ->
                    topPlayedArtists.mapNotNull { topPlayedArtist ->
                        val artist = artistRepository.getArtistById(topPlayedArtist.channelId).lastOrNull() ?:
                        getArtistFromYouTube(topPlayedArtist.channelId) ?: return@mapNotNull null
                        topPlayedArtist to artist
                    }.let { pairs ->
                        _analyticsUIState.update {
                            it.copy(
                                topArtists = LocalResource.Success(pairs)
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun getArtistFromYouTube(
        channelId: String,
    ): ArtistEntity? =
        artistRepository.getArtistData(channelId).lastOrNull()?.takeIf {
            it is Resource.Success && it.data != null
        }.let { it?.data }?.let {
            val entity = ArtistEntity(
                channelId = channelId,
                name = it.name,
                thumbnails = it.thumbnails?.lastOrNull()?.url,
                followed = false,
                followedAt = null,
                inLibrary = now()
            )
            artistRepository.insertArtist(entity)
            entity
        }

    private fun getTopAlbums(dayRange: AnalyticsUiState.DayRange) {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    topAlbums = LocalResource.Loading()
                )
            }
            if (dayRange == AnalyticsUiState.DayRange.THIS_YEAR) {
                analyticsRepository.queryTopAlbumsInRange(
                    startTimestamp = startTimestampOfThisYear(),
                    endTimestamp = now()
                ).collect { topPlayedAlbums ->
                    topPlayedAlbums.mapNotNull {
                        val album = albumRepository.getAlbum(it.albumBrowseId).lastOrNull() ?: return@mapNotNull null
                        it to album
                    }.let { pairs ->
                        _analyticsUIState.update {
                            it.copy(
                                topAlbums = LocalResource.Success(pairs)
                            )
                        }
                    }
                }
            } else {
                val x = when (dayRange) {
                    AnalyticsUiState.DayRange.LAST_7_DAYS -> 7
                    AnalyticsUiState.DayRange.LAST_30_DAYS -> 30
                    AnalyticsUiState.DayRange.LAST_90_DAYS -> 90
                }
                analyticsRepository.queryTopAlbumsLastXDays(x).collect { topPlayedAlbums ->
                    topPlayedAlbums.mapNotNull {
                        val album = albumRepository.getAlbum(it.albumBrowseId).lastOrNull() ?: return@mapNotNull null
                        it to album
                    }.let { pairs ->
                        log("Top played albums: $pairs")
                        _analyticsUIState.update {
                            it.copy(
                                topAlbums = LocalResource.Success(pairs)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class AnalyticsUiState(
    val scrobblesCount: LocalResource<Long> = LocalResource.Loading(),
    val artistCount: LocalResource<Long> = LocalResource.Loading(),
    val totalListenTimeInSeconds: LocalResource<Long> = LocalResource.Loading(),
    val dayRange: DayRange = DayRange.LAST_7_DAYS,
    val topTracks: LocalResource<List<Pair<TopPlayedTracks, SongEntity>>> = LocalResource.Loading(),
    val topArtists: LocalResource<List<Pair<TopPlayedArtist, ArtistEntity>>> = LocalResource.Loading(),
    val topAlbums: LocalResource<List<Pair<TopPlayedAlbum, AlbumEntity>>> = LocalResource.Loading()
) {
    enum class DayRange {
        LAST_7_DAYS,
        LAST_30_DAYS,
        LAST_90_DAYS,
        THIS_YEAR,
    }
}