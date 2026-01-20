package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.analytics.PlaybackEventEntity
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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class AnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
) : BaseViewModel() {
    private val _analyticsUIState: MutableStateFlow<AnalyticsUiState> =
        MutableStateFlow(AnalyticsUiState())
    val analyticsUIState: StateFlow<AnalyticsUiState> get() = _analyticsUIState.asStateFlow()

    init {
        getScrobblesCount()
        getArtistCount()
        getTotalListenTime()
        getRecentlyRecord()
        getDataForDayRange(analyticsUIState.value.dayRange)
    }

    private fun getDataForDayRange(dayRange: AnalyticsUiState.DayRange) {
        getTopTracks(dayRange)
        getTopArtists(dayRange)
        getTopAlbums(dayRange)
        getScrobblesLineChart(dayRange)
    }

    private fun getScrobblesCount() {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    scrobblesCount = LocalResource.Loading(),
                )
            }
            analyticsRepository.getTotalPlaybackEventCount().collect { count ->
                _analyticsUIState.update {
                    it.copy(
                        scrobblesCount = LocalResource.Success(count),
                    )
                }
            }
        }
    }

    private fun getArtistCount() {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    artistCount = LocalResource.Loading(),
                )
            }
            analyticsRepository.getTotalEventArtistCount().collect { count ->
                _analyticsUIState.update {
                    it.copy(
                        artistCount = LocalResource.Success(count),
                    )
                }
            }
        }
    }

    private fun getTotalListenTime() {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    totalListenTimeInSeconds = LocalResource.Loading(),
                )
            }
            analyticsRepository.getTotalListeningTimeInSeconds().collect { total ->
                _analyticsUIState.update {
                    it.copy(
                        totalListenTimeInSeconds = LocalResource.Success(total),
                    )
                }
            }
        }
    }

    private fun getTopTracks(dayRange: AnalyticsUiState.DayRange) {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    topTracks = LocalResource.Loading(),
                )
            }
            if (dayRange == AnalyticsUiState.DayRange.THIS_YEAR) {
                analyticsRepository
                    .queryTopPlayedSongsInRange(
                        startTimestamp = startTimestampOfThisYear(),
                        endTimestamp = now(),
                    ).collect { topPlayedTracks ->
                        topPlayedTracks
                            .mapNotNull {
                                val song = songRepository.getSongById(it.videoId).lastOrNull() ?: return@mapNotNull null
                                it to song
                            }.let { pairs ->
                                _analyticsUIState.update {
                                    it.copy(
                                        topTracks = LocalResource.Success(pairs),
                                    )
                                }
                            }
                    }
            } else {
                val x =
                    when (dayRange) {
                        AnalyticsUiState.DayRange.LAST_7_DAYS -> 7
                        AnalyticsUiState.DayRange.LAST_30_DAYS -> 30
                        AnalyticsUiState.DayRange.LAST_90_DAYS -> 90
                    }
                analyticsRepository.queryTopPlayedSongsLastXDays(x).collect { topPlayedTracks ->
                    topPlayedTracks
                        .mapNotNull {
                            val song = songRepository.getSongById(it.videoId).lastOrNull() ?: return@mapNotNull null
                            it to song
                        }.let { pairs ->
                            log("Top played tracks: $pairs")
                            _analyticsUIState.update {
                                it.copy(
                                    topTracks = LocalResource.Success(pairs),
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
                    topArtists = LocalResource.Loading(),
                )
            }
            if (dayRange == AnalyticsUiState.DayRange.THIS_YEAR) {
                analyticsRepository
                    .queryTopArtistsInRange(
                        startTimestamp = startTimestampOfThisYear(),
                        endTimestamp = now(),
                    ).collect { topPlayedArtists ->
                        topPlayedArtists
                            .mapNotNull { topPlayedArtist ->
                                val artist =
                                    artistRepository.getArtistById(topPlayedArtist.channelId).lastOrNull()
                                        ?: getArtistFromYouTube(topPlayedArtist.channelId)
                                        ?: return@mapNotNull null
                                topPlayedArtist to artist
                            }.let { pairs ->
                                log("Top played artists: $pairs")
                                _analyticsUIState.update {
                                    it.copy(
                                        topArtists = LocalResource.Success(pairs),
                                    )
                                }
                            }
                    }
            } else {
                val x =
                    when (dayRange) {
                        AnalyticsUiState.DayRange.LAST_7_DAYS -> 7
                        AnalyticsUiState.DayRange.LAST_30_DAYS -> 30
                        AnalyticsUiState.DayRange.LAST_90_DAYS -> 90
                    }
                analyticsRepository.queryTopArtistsLastXDays(x).collect { topPlayedArtists ->
                    topPlayedArtists
                        .mapNotNull { topPlayedArtist ->
                            val artist =
                                artistRepository.getArtistById(topPlayedArtist.channelId).lastOrNull()
                                    ?: getArtistFromYouTube(topPlayedArtist.channelId)
                                    ?: return@mapNotNull null
                            topPlayedArtist to artist
                        }.let { pairs ->
                            _analyticsUIState.update {
                                it.copy(
                                    topArtists = LocalResource.Success(pairs),
                                )
                            }
                        }
                }
            }
        }
    }

    private suspend fun getArtistFromYouTube(channelId: String): ArtistEntity? =
        artistRepository
            .getArtistData(channelId)
            .lastOrNull()
            ?.takeIf {
                it is Resource.Success && it.data != null
            }.let { it?.data }
            ?.let {
                val entity =
                    ArtistEntity(
                        channelId = channelId,
                        name = it.name,
                        thumbnails = it.thumbnails?.lastOrNull()?.url,
                        followed = false,
                        followedAt = null,
                        inLibrary = now(),
                    )
                artistRepository.insertArtist(entity)
                entity
            }

    private fun getTopAlbums(dayRange: AnalyticsUiState.DayRange) {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    topAlbums = LocalResource.Loading(),
                )
            }
            if (dayRange == AnalyticsUiState.DayRange.THIS_YEAR) {
                analyticsRepository
                    .queryTopAlbumsInRange(
                        startTimestamp = startTimestampOfThisYear(),
                        endTimestamp = now(),
                    ).collect { topPlayedAlbums ->
                        topPlayedAlbums
                            .mapNotNull {
                                val album = albumRepository.getAlbum(it.albumBrowseId).lastOrNull() ?: return@mapNotNull null
                                it to album
                            }.let { pairs ->
                                _analyticsUIState.update {
                                    it.copy(
                                        topAlbums = LocalResource.Success(pairs),
                                    )
                                }
                            }
                    }
            } else {
                val x =
                    when (dayRange) {
                        AnalyticsUiState.DayRange.LAST_7_DAYS -> 7
                        AnalyticsUiState.DayRange.LAST_30_DAYS -> 30
                        AnalyticsUiState.DayRange.LAST_90_DAYS -> 90
                    }
                analyticsRepository.queryTopAlbumsLastXDays(x).collect { topPlayedAlbums ->
                    topPlayedAlbums
                        .mapNotNull {
                            val album = albumRepository.getAlbum(it.albumBrowseId).lastOrNull() ?: return@mapNotNull null
                            it to album
                        }.let { pairs ->
                            log("Top played albums: $pairs")
                            _analyticsUIState.update {
                                it.copy(
                                    topAlbums = LocalResource.Success(pairs),
                                )
                            }
                        }
                }
            }
        }
    }

    private fun getRecentlyRecord() {
        viewModelScope.launch {
            analyticsRepository
                .getPlaybackEventsByOffset(
                    offset = 0,
                    limit = 5,
                ).collect { events ->
                    events
                        .mapNotNull { event ->
                            val song = songRepository.getSongById(event.videoId).lastOrNull() ?: return@mapNotNull null
                            event to song
                        }.let {
                            if (it.isNotEmpty()) {
                                _analyticsUIState.update { state ->
                                    state.copy(
                                        recentlyRecord = LocalResource.Success(it),
                                    )
                                }
                            }
                        }
                }
        }
    }

    private fun getScrobblesLineChart(dayRange: AnalyticsUiState.DayRange) {
        viewModelScope.launch {
            _analyticsUIState.update {
                it.copy(
                    scrobblesLineChart = LocalResource.Loading(),
                )
            }
            val chartTypes =
                when (dayRange) {
                    AnalyticsUiState.DayRange.LAST_7_DAYS -> {
                        (0 until 7).map {
                            AnalyticsUiState.ChartType.Day(
                                day = now().date.minus(DatePeriod(days = it)),
                            )
                        }
                    }

                    AnalyticsUiState.DayRange.LAST_30_DAYS -> {
                        (0 until 30).map {
                            AnalyticsUiState.ChartType.Day(
                                day = now().date.minus(DatePeriod(days = it)),
                            )
                        }
                    }

                    AnalyticsUiState.DayRange.LAST_90_DAYS -> {
                        (0 until 3).map {
                            AnalyticsUiState.ChartType.Month(
                                month = now().date.minus(DatePeriod(months = it)).month,
                                year = now().date.minus(DatePeriod(months = it)).year,
                            )
                        }
                    }

                    AnalyticsUiState.DayRange.THIS_YEAR -> {
                        val currentMonth = now().date.month
                        (1..currentMonth.number).map {
                            AnalyticsUiState.ChartType.Month(
                                month = kotlinx.datetime.Month(it),
                                year = now().date.year,
                            )
                        }
                    }
                }
            val currentTimeZone = TimeZone.currentSystemDefault()
            val data =
                chartTypes.map {
                    when (it) {
                        is AnalyticsUiState.ChartType.Day -> {
                            val startTimestamp = it.day.atStartOfDayIn(currentTimeZone).toLocalDateTime(currentTimeZone)
                            val endTimestamp =
                                it.day
                                    .plus(DatePeriod(days = 1))
                                    .atStartOfDayIn(currentTimeZone)
                                    .toLocalDateTime(currentTimeZone)
                            val count =
                                analyticsRepository
                                    .getPlaybackEventCountInRange(
                                        startTimestamp = startTimestamp,
                                        endTimestamp = endTimestamp,
                                    ).lastOrNull() ?: 0L
                            Pair(it, count)
                        }

                        is AnalyticsUiState.ChartType.Month -> {
                            val startTimestamp =
                                LocalDate(
                                    year = it.year,
                                    month = it.month.number,
                                    day = 1,
                                ).atStartOfDayIn(currentTimeZone).toLocalDateTime(currentTimeZone)
                            val endTimestamp =
                                if (it.month == kotlinx.datetime.Month.DECEMBER) {
                                    LocalDate(
                                        year = it.year + 1,
                                        month = 1,
                                        day = 1,
                                    ).atStartOfDayIn(currentTimeZone).toLocalDateTime(currentTimeZone)
                                } else {
                                    LocalDate(
                                        year = it.year,
                                        month = it.month.number + 1,
                                        day = 1,
                                    ).atStartOfDayIn(currentTimeZone).toLocalDateTime(currentTimeZone)
                                }
                            val count =
                                analyticsRepository
                                    .getPlaybackEventCountInRange(
                                        startTimestamp = startTimestamp,
                                        endTimestamp = endTimestamp,
                                    ).lastOrNull() ?: 0L
                            Pair(it, count)
                        }
                    }
                }
            log("Scrobbles line chart data: $data")
            _analyticsUIState.update {
                it.copy(
                    scrobblesLineChart = LocalResource.Success(data),
                )
            }
        }
    }

    fun setDayRange(dayRange: AnalyticsUiState.DayRange) {
        _analyticsUIState.update {
            it.copy(
                dayRange = dayRange,
            )
        }
        getDataForDayRange(dayRange)
    }
}

data class AnalyticsUiState(
    val scrobblesCount: LocalResource<Long> = LocalResource.Loading(),
    val artistCount: LocalResource<Long> = LocalResource.Loading(),
    val totalListenTimeInSeconds: LocalResource<Long> = LocalResource.Loading(),
    val dayRange: DayRange = DayRange.LAST_7_DAYS,
    val recentlyRecord: LocalResource<List<Pair<PlaybackEventEntity, SongEntity>>> = LocalResource.Loading(),
    val topTracks: LocalResource<List<Pair<TopPlayedTracks, SongEntity>>> = LocalResource.Loading(),
    val topArtists: LocalResource<List<Pair<TopPlayedArtist, ArtistEntity>>> = LocalResource.Loading(),
    val topAlbums: LocalResource<List<Pair<TopPlayedAlbum, AlbumEntity>>> = LocalResource.Loading(),
    val scrobblesLineChart: LocalResource<List<Pair<ChartType, Long>>> = LocalResource.Loading(),
) {
    enum class DayRange {
        LAST_7_DAYS,
        LAST_30_DAYS,
        LAST_90_DAYS,
        THIS_YEAR,
    }

    sealed class ChartType {
        data class Day(
            val day: LocalDate,
        ) : ChartType()

        data class Month(
            val month: kotlinx.datetime.Month,
            val year: Int,
        ) : ChartType()
    }
}