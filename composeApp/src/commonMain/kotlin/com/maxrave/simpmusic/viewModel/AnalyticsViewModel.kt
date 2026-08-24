package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.analytics.PlaybackEventEntity
import com.maxrave.domain.data.entities.analytics.query.TopPlayedAlbum
import com.maxrave.domain.data.entities.analytics.query.TopPlayedArtist
import com.maxrave.domain.data.entities.analytics.query.TopPlayedTracks
import com.maxrave.domain.data.model.analytics.AnalyticsPeriodStats
import com.maxrave.domain.extension.now
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
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
    private val dataStoreManager: DataStoreManager,
) : BaseViewModel() {
    private val _analyticsUIState: MutableStateFlow<AnalyticsUiState> =
        MutableStateFlow(AnalyticsUiState())
    val analyticsUIState: StateFlow<AnalyticsUiState> get() = _analyticsUIState.asStateFlow()

    init {
        getScrobblesCount()
        getArtistCount()
        getTotalListenTime()
        getRecentlyRecord()
        viewModelScope.launch {
            val saved = dataStoreManager.getString(ANALYTICS_DAY_RANGE_KEY).firstOrNull()
            val dayRange = saved?.let {
                runCatching { AnalyticsUiState.DayRange.valueOf(it) }.getOrNull()
            } ?: AnalyticsUiState.DayRange.LAST_7_DAYS
            _analyticsUIState.update { it.copy(dayRange = dayRange) }
            loadPeriod()
        }
    }

    companion object {
        private const val ANALYTICS_DAY_RANGE_KEY = "analytics_day_range"
    }

    /**
     * The span the screen is showing, as [start, end].
     *
     * [offset] counts periods BACKWARDS from now: 0 is the current one, 1 the one before it. The
     * whole navigator and every delta on the screen are this one function called twice — the range
     * queries underneath already existed and were only ever used for "this year".
     *
     * Ends are inclusive-by-day: `end` is the last moment of its day, so a play at 23:59 belongs to
     * the period it happened in rather than to the next one.
     */
    private fun rangeFor(
        dayRange: AnalyticsUiState.DayRange,
        offset: Int,
    ): Pair<LocalDateTime, LocalDateTime> {
        val today = now().date
        return if (dayRange == AnalyticsUiState.DayRange.THIS_YEAR) {
            val year = today.year - offset
            val start = LocalDate(year, 1, 1)
            // The current year stops at today; an earlier one runs to its own 31 December.
            val end = if (offset == 0) today else LocalDate(year, 12, 31)
            start.atTime(0, 0) to end.atTime(23, 59, 59)
        } else {
            val length = dayRange.lengthInDays
            val end = today.minus(DatePeriod(days = offset * length))
            val start = end.minus(DatePeriod(days = length - 1))
            start.atTime(0, 0) to end.atTime(23, 59, 59)
        }
    }

    private fun loadPeriod() {
        val state = _analyticsUIState.value
        val (start, end) = rangeFor(state.dayRange, state.periodOffset)
        _analyticsUIState.update {
            it.copy(periodStart = start.date, periodEnd = end.date)
        }
        getTopTracks(start, end)
        getTopArtists(start, end)
        getTopAlbums(start, end)
        getScrobblesLineChart(state.dayRange, end.date)
        getPeriodStats(state.dayRange, state.periodOffset)
    }

    /**
     * This period and the one before it, fetched as a matched pair.
     *
     * The previous one is what turns every number on the screen from a quantity into a change. It
     * is deliberately not shown when it is empty: a first-week user comparing against zero would
     * see the same "+∞%" against every single figure.
     */
    private fun getPeriodStats(
        dayRange: AnalyticsUiState.DayRange,
        offset: Int,
    ) {
        viewModelScope.launch {
            _analyticsUIState.update { it.copy(stats = LocalResource.Loading()) }
            val (start, end) = rangeFor(dayRange, offset)
            val (prevStart, prevEnd) = rangeFor(dayRange, offset + 1)
            val current = analyticsRepository.getPeriodStats(start, end)
            val previous = analyticsRepository.getPeriodStats(prevStart, prevEnd)
            _analyticsUIState.update {
                it.copy(
                    stats = LocalResource.Success(current),
                    previousStats = previous.takeIf { p -> !p.isEmpty },
                )
            }
        }
    }

    /** Step the window back ([delta] = -1) or forward ([delta] = +1). Never past the present. */
    fun stepPeriod(delta: Int) {
        val next = (_analyticsUIState.value.periodOffset - delta).coerceAtLeast(0)
        if (next == _analyticsUIState.value.periodOffset) return
        _analyticsUIState.update { it.copy(periodOffset = next) }
        loadPeriod()
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

    private fun getTopTracks(
        start: LocalDateTime,
        end: LocalDateTime,
    ) {
        viewModelScope.launch {
            _analyticsUIState.update { it.copy(topTracks = LocalResource.Loading()) }
            analyticsRepository
                .queryTopPlayedSongsInRange(startTimestamp = start, endTimestamp = end)
                .collect { topPlayedTracks ->
                    topPlayedTracks
                        .mapNotNull {
                            val song = songRepository.getSongById(it.videoId).lastOrNull() ?: return@mapNotNull null
                            it to song
                        }.let { pairs ->
                            _analyticsUIState.update { it.copy(topTracks = LocalResource.Success(pairs)) }
                        }
                }
        }
    }

    private fun getTopArtists(
        start: LocalDateTime,
        end: LocalDateTime,
    ) {
        viewModelScope.launch {
            _analyticsUIState.update { it.copy(topArtists = LocalResource.Loading()) }
            analyticsRepository
                .queryTopArtistsInRange(startTimestamp = start, endTimestamp = end)
                .collect { topPlayedArtists ->
                    topPlayedArtists
                        .mapNotNull { topPlayedArtist ->
                            val artist =
                                artistRepository.getArtistById(topPlayedArtist.channelId).lastOrNull()
                                    ?: getArtistFromYouTube(topPlayedArtist.channelId)
                                    ?: return@mapNotNull null
                            topPlayedArtist to artist
                        }.let { pairs ->
                            _analyticsUIState.update { it.copy(topArtists = LocalResource.Success(pairs)) }
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

    private fun getTopAlbums(
        start: LocalDateTime,
        end: LocalDateTime,
    ) {
        viewModelScope.launch {
            _analyticsUIState.update { it.copy(topAlbums = LocalResource.Loading()) }
            analyticsRepository
                .queryTopAlbumsInRange(startTimestamp = start, endTimestamp = end)
                .collect { topPlayedAlbums ->
                    topPlayedAlbums
                        .mapNotNull {
                            val album = albumRepository.getAlbum(it.albumBrowseId).lastOrNull() ?: return@mapNotNull null
                            it to album
                        }.let { pairs ->
                            _analyticsUIState.update { it.copy(topAlbums = LocalResource.Success(pairs)) }
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

    private fun getScrobblesLineChart(
        dayRange: AnalyticsUiState.DayRange,
        endDate: LocalDate,
    ) {
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
                                day = endDate.minus(DatePeriod(days = it)),
                            )
                        }
                    }

                    AnalyticsUiState.DayRange.LAST_30_DAYS -> {
                        // Newest week first, matching how the day buckets above are ordered.
                        (0 until 4).map { week ->
                            AnalyticsUiState.ChartType.Week(
                                start = endDate.minus(DatePeriod(days = week * 7 + 6)),
                                end = endDate.minus(DatePeriod(days = week * 7)),
                            )
                        }
                    }

                    AnalyticsUiState.DayRange.LAST_90_DAYS -> {
                        (0 until 3).map {
                            AnalyticsUiState.ChartType.Month(
                                month = endDate.minus(DatePeriod(months = it)).month,
                                year = endDate.minus(DatePeriod(months = it)).year,
                            )
                        }
                    }

                    AnalyticsUiState.DayRange.THIS_YEAR -> {
                        val currentMonth = endDate.month
                        (1..currentMonth.number).map {
                            AnalyticsUiState.ChartType.Month(
                                month = kotlinx.datetime.Month(it),
                                year = endDate.year,
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

                        is AnalyticsUiState.ChartType.Week -> {
                            val startTimestamp =
                                it.start.atStartOfDayIn(currentTimeZone).toLocalDateTime(currentTimeZone)
                            // `end` is inclusive, so the range runs to the start of the day after it.
                            val endTimestamp =
                                it.end
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
        // A different range length makes the old offset meaningless — three periods back at
        // 7 days is not three periods back at 90 — so switching always returns to the present.
        _analyticsUIState.update {
            it.copy(
                dayRange = dayRange,
                periodOffset = 0,
            )
        }
        loadPeriod()
        viewModelScope.launch {
            dataStoreManager.putString(ANALYTICS_DAY_RANGE_KEY, dayRange.name)
        }
    }
}

data class AnalyticsUiState(
    val scrobblesCount: LocalResource<Long> = LocalResource.Loading(),
    val artistCount: LocalResource<Long> = LocalResource.Loading(),
    val totalListenTimeInSeconds: LocalResource<Long> = LocalResource.Loading(),
    val dayRange: DayRange = DayRange.LAST_7_DAYS,
    /** Periods back from now: 0 is the present one, 1 the one before it. */
    val periodOffset: Int = 0,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val stats: LocalResource<AnalyticsPeriodStats> = LocalResource.Loading(),
    /** Null when the previous period held nothing — the screen then shows no deltas at all. */
    val previousStats: AnalyticsPeriodStats? = null,
    val recentlyRecord: LocalResource<List<Pair<PlaybackEventEntity, SongEntity>>> = LocalResource.Loading(),
    val topTracks: LocalResource<List<Pair<TopPlayedTracks, SongEntity>>> = LocalResource.Loading(),
    val topArtists: LocalResource<List<Pair<TopPlayedArtist, ArtistEntity>>> = LocalResource.Loading(),
    val topAlbums: LocalResource<List<Pair<TopPlayedAlbum, AlbumEntity>>> = LocalResource.Loading(),
    val scrobblesLineChart: LocalResource<List<Pair<ChartType, Long>>> = LocalResource.Loading(),
) {
    /** True while the window is in the past, so the forward arrow has somewhere to go. */
    val canStepForward: Boolean get() = periodOffset > 0

    enum class DayRange(
        val lengthInDays: Int,
    ) {
        LAST_7_DAYS(7),
        LAST_30_DAYS(30),
        LAST_90_DAYS(90),

        /** Length is unused — a year steps by calendar years, see rangeFor. */
        THIS_YEAR(365),
    }

    sealed class ChartType {
        data class Day(
            val day: LocalDate,
        ) : ChartType()

        /**
         * Seven days, inclusive at both ends.
         *
         * Thirty rows is not a chart, it is a list nobody reads to the end — so the 30-day range
         * buckets by week. Four buckets of exactly seven days, rather than four-and-a-bit covering
         * all thirty: an uneven last bucket would carry more days than the others and draw a
         * longer bar for it, which is the one thing a bar chart must not do.
         */
        data class Week(
            val start: LocalDate,
            val end: LocalDate,
        ) : ChartType()

        data class Month(
            val month: kotlinx.datetime.Month,
            val year: Int,
        ) : ChartType()
    }
}