package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.model.analytics.AnalyticsPeriodStats
import com.maxrave.domain.data.model.analytics.ListeningFingerprint
import com.maxrave.domain.extension.now
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime

/**
 * The whole Wrapped reel for one calendar year, composed once.
 *
 * Every figure the ten cards print is derived here rather than inside a card, so two cards showing
 * the same number cannot disagree about it. The arithmetic itself is almost all already done by
 * [AnalyticsRepository.getPeriodStats]; what is left is the presentation work the domain layer
 * deliberately does not do — pairing ids with the entities that carry names and artwork, and
 * naming things (the archetype, the band of the day) that only mean something on a card.
 */
class WrappedViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
) : BaseViewModel() {
    /**
     * Composed on the first collector, then held.
     *
     * [SharingStarted.Lazily] rather than an eager `init`: the Analytics screen holds an instance
     * only to decide whether its Wrapped entry has anything to point at, and composing a year is
     * two full-range scans plus, for an artist never stored locally, a network round trip. Nothing
     * pays that until something is watching, and Lazily never restarts, so the year is composed
     * once per instance however often the screen comes and goes.
     */
    val uiState: StateFlow<WrappedUiState> =
        flow { emit(composeYear(now().date.year)) }
            .stateIn(viewModelScope, SharingStarted.Lazily, WrappedUiState.Loading)

    private suspend fun composeYear(year: Int): WrappedUiState {
        val (start, end) = yearRange(year)
        val stats = analyticsRepository.getPeriodStats(start, end)
        if (stats.activeDays < WrappedYear.REQUIRED_ACTIVE_DAYS) {
            return WrappedUiState.NotEnoughData(
                year = year,
                activeDays = stats.activeDays,
                requiredDays = WrappedYear.REQUIRED_ACTIVE_DAYS,
            )
        }

        val (previousStart, previousEnd) = yearRange(year - 1)
        // Null rather than an empty snapshot: card 08 draws this as its second polygon, and five
        // axes collapsed on the origin read as a real measurement of a year that never happened.
        val previousStats =
            analyticsRepository.getPeriodStats(previousStart, previousEnd).takeIf { !it.isEmpty }

        // Share of the year's plays that could be dated at all. The chart below is drawn from those
        // plays only, so this is both the gate and the caveat card 09 prints.
        val decadeCoverage = if (stats.plays > 0) stats.datedPlays / stats.plays.toFloat() else 0f
        val showDecades = decadeCoverage >= WrappedYear.DECADE_COVERAGE_FLOOR

        return WrappedUiState.Ready(
            WrappedYear(
                year = year,
                daysInYear = LocalDate(year, 12, 31).dayOfYear,
                stats = stats,
                previousStats = previousStats,
                topTracks = topTracksOf(start, end),
                topArtists = topArtistsOf(start, end, previousStart, previousEnd),
                topAlbums = topAlbumsOf(start, end),
                biggestDay = biggestDayOf(stats),
                clock = clockOf(stats),
                archetype = archetypeOf(stats.fingerprint),
                decades = if (showDecades) decadeSharesOf(stats) else emptyList(),
                showDecades = showDecades,
                decadeCoverage = decadeCoverage,
            ),
        )
    }

    /**
     * The whole calendar year, local, inclusive at both ends.
     *
     * `end` is the last second of 31 December rather than the first of 1 January, because every
     * range query underneath matches with `BETWEEN` — an exclusive end would drop the last day.
     * The Analytics screen's own ranges are built the same way.
     */
    private fun yearRange(year: Int): Pair<LocalDateTime, LocalDateTime> =
        LocalDate(year, 1, 1).atTime(0, 0) to LocalDate(year, 12, 31).atTime(23, 59, 59)

    private suspend fun topTracksOf(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<WrappedTrack> =
        analyticsRepository
            .queryTopPlayedSongsInRange(startTimestamp = start, endTimestamp = end)
            .lastOrNull()
            .orEmpty()
            .resolveTop { songRepository.getSongById(it.videoId).lastOrNull() }
            .mapIndexed { index, (row, song) ->
                WrappedTrack(
                    rank = index + 1,
                    song = song,
                    playCount = row.playCount,
                    listenedSeconds = row.totalListeningTime,
                )
            }

    private suspend fun topArtistsOf(
        start: LocalDateTime,
        end: LocalDateTime,
        previousStart: LocalDateTime,
        previousEnd: LocalDateTime,
    ): List<WrappedArtist> {
        // Last year goes through the SAME query, so "first" means the same thing in both years —
        // a badge claiming two years running is only true if the two rankings were built alike.
        val previousTopChannelId =
            analyticsRepository
                .queryTopArtistsWithTimeInRange(startTimestamp = previousStart, endTimestamp = previousEnd)
                .lastOrNull()
                ?.firstOrNull()
                ?.channelId
        return analyticsRepository
            .queryTopArtistsWithTimeInRange(startTimestamp = start, endTimestamp = end)
            .lastOrNull()
            .orEmpty()
            .resolveTop { resolveArtist(it.channelId) }
            .mapIndexed { index, (row, artist) ->
                WrappedArtist(
                    rank = index + 1,
                    artist = artist,
                    playCount = row.playCount,
                    listenedSeconds = row.totalListeningTime,
                    // First in BOTH years, nothing looser. "Somewhere in last year's top five"
                    // is close to a certainty for this year's number one, and a badge that is
                    // nearly always true says nothing.
                    wasTopArtistLastYear = index == 0 && previousTopChannelId == row.channelId,
                )
            }
    }

    private suspend fun topAlbumsOf(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<WrappedAlbum> =
        analyticsRepository
            .queryTopAlbumsInRange(startTimestamp = start, endTimestamp = end)
            .lastOrNull()
            .orEmpty()
            .resolveTop { albumRepository.getAlbum(it.albumBrowseId).lastOrNull() }
            .mapIndexed { index, (row, album) ->
                WrappedAlbum(rank = index + 1, album = album, playCount = row.playCount)
            }

    /**
     * The one day with the most plays, and what was on repeat during it.
     *
     * The day's own top track is a second query over that day alone — the year's number one is not
     * necessarily what carried the day, and card 07 is about the day.
     */
    private suspend fun biggestDayOf(stats: AnalyticsPeriodStats): WrappedBiggestDay? {
        val date = stats.busiestDay ?: return null
        val top =
            analyticsRepository
                .queryTopPlayedSongsInRange(
                    startTimestamp = date.atTime(0, 0),
                    endTimestamp = date.atTime(23, 59, 59),
                ).lastOrNull()
                ?.firstOrNull()
        return WrappedBiggestDay(
            date = date,
            plays = stats.busiestDayPlays,
            // Plays over the days that had any, so the comparison is against a day the user had.
            typicalPlays = stats.playsPerActiveDay,
            topTrack = top?.let { songRepository.getSongById(it.videoId).lastOrNull() },
            topTrackPlays = top?.playCount ?: 0,
        )
    }

    /**
     * The ring, its peak hour, and the six-hour band holding the most plays.
     *
     * The band is the busiest band, NOT the band containing the peak hour: a single loud hour can
     * sit in an otherwise quiet stretch, and the caption would then name a part of the day the
     * ring beside it plainly does not show as busy.
     */
    private fun clockOf(stats: AnalyticsPeriodStats): WrappedClock {
        val playsByHour = stats.playsByHour
        val total = playsByHour.sum()
        val band =
            WrappedListeningBand.entries.maxByOrNull { it.playsIn(playsByHour) }
                ?: WrappedListeningBand.NIGHT
        return WrappedClock(
            playsByHour = playsByHour,
            peakHour = playsByHour.indices.maxByOrNull { playsByHour[it] } ?: 0,
            band = band,
            bandShare = if (total > 0) band.playsIn(playsByHour) / total.toFloat() else 0f,
        )
    }

    private fun WrappedListeningBand.playsIn(playsByHour: List<Int>): Int =
        playsByHour.subList(startHour, endHour).sum()

    /**
     * Argmax over the fingerprint's five axes.
     *
     * The mapping is the two declaration orders lining up: [ListeningFingerprint.axes] runs
     * consistency, discovery, diversity, concentration, replay, and [WrappedArchetype] names them
     * in that same order. [maxByOrNull] keeps the FIRST maximum, so a tie falls to the earlier
     * archetype and one year always yields one name.
     */
    private fun archetypeOf(fingerprint: ListeningFingerprint): WrappedArchetype {
        val axes = fingerprint.axes
        val top = axes.indices.maxByOrNull { axes[it] } ?: 0
        return WrappedArchetype.entries.getOrElse(top) { WrappedArchetype.THE_REGULAR }
    }

    /**
     * Shares of the plays that could be DATED, which is the denominator card 09 names out loud.
     *
     * Dividing by every play instead would understate every decade and leave the chart summing to
     * the coverage figure rather than to one.
     */
    private fun decadeSharesOf(stats: AnalyticsPeriodStats): List<WrappedDecadeShare> {
        if (stats.datedPlays <= 0) return emptyList()
        return stats.decades
            .map {
                WrappedDecadeShare(
                    decade = it.decade,
                    plays = it.plays,
                    share = it.plays / stats.datedPlays.toFloat(),
                )
            }.sortedByDescending { it.share }
    }

    /**
     * The stored artist, or one built from YouTube and stored on the way past.
     *
     * An artist can be all over the year and still have no row: rows are written when their page
     * is opened, and a radio queue never opens one. Same path the Analytics screen takes — without
     * it the top five silently loses whoever was never visited, which is exactly the artist a year
     * of radio would surface.
     */
    private suspend fun resolveArtist(channelId: String): ArtistEntity? =
        artistRepository.getArtistById(channelId).lastOrNull() ?: fetchArtistFromYouTube(channelId)

    private suspend fun fetchArtistFromYouTube(channelId: String): ArtistEntity? =
        artistRepository
            .getArtistData(channelId)
            .lastOrNull()
            ?.takeIf { it is Resource.Success && it.data != null }
            ?.data
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

    /**
     * The first [TOP_COUNT] rows that resolve to an entity, each paired with it.
     *
     * Rank is assigned afterwards, over what survived, so a row whose entity is missing leaves no
     * gap in the printed 1..5 and the list reaches further down to stay five long — the queries
     * return up to 100 rows, so there is room to. Ranking by position in the query result instead
     * would print 1, 2, 4, 5 the first time a row could not be resolved.
     */
    private suspend fun <T, R> List<T>.resolveTop(resolve: suspend (T) -> R?): List<Pair<T, R>> {
        val resolved = mutableListOf<Pair<T, R>>()
        for (row in this) {
            if (resolved.size == TOP_COUNT) break
            val entity = resolve(row) ?: continue
            resolved += row to entity
        }
        return resolved
    }

    companion object {
        /** Entries per top list. The reel prints five and the cards are laid out for five. */
        private const val TOP_COUNT = 5
    }
}
