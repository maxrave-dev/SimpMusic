package com.maxrave.simpmusic.viewModel

import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.analytics.AnalyticsPeriodStats
import kotlinx.datetime.LocalDate

/**
 * Everything the Wrapped reel shows, resolved once, before the first card is drawn.
 *
 * The reel is ten cards over ONE year and the share card at the end repeats figures the earlier
 * cards already showed, so every number is computed here rather than inside a card: two cards
 * deriving the same figure independently is how they end up disagreeing. Cards read this and
 * render — they do no arithmetic beyond formatting.
 *
 * Composed in the view model, not in the domain layer, for the same reason [AnalyticsUiState] is:
 * the top-played queries return ids and play counts only, and pairing those with the entities that
 * carry names and artwork — including the YouTube fallback for an artist never stored locally — is
 * presentation work, not a business rule.
 */
sealed interface WrappedUiState {
    data object Loading : WrappedUiState

    /**
     * Local tracking is on, but the year does not hold enough to say anything true.
     *
     * Distinct from an error and from tracking being off: with tracking off the entry point never
     * appears at all, exactly as the Analytics tab already behaves.
     */
    data class NotEnoughData(
        val year: Int,
        val activeDays: Int,
        val requiredDays: Int,
    ) : WrappedUiState

    data class Ready(
        val wrapped: WrappedYear,
    ) : WrappedUiState
}

/**
 * @property previousStats the same span one year earlier, or null when the user was not here for
 *   it. Card 08 draws it as the second polygon; without it the fingerprint's five self-normalised
 *   axes say almost nothing, so that card must handle the null rather than assume a shape.
 */
data class WrappedYear(
    val year: Int,
    /** 365, or 366 — the denominator card 01 prints, never hardcoded. */
    val daysInYear: Int,
    val stats: AnalyticsPeriodStats,
    val previousStats: AnalyticsPeriodStats?,
    /** Up to five, already in rank order. Fewer than five is normal and cards must survive it. */
    val topTracks: List<WrappedTrack>,
    val topArtists: List<WrappedArtist>,
    val topAlbums: List<WrappedAlbum>,
    val biggestDay: WrappedBiggestDay?,
    val clock: WrappedClock,
    val archetype: WrappedArchetype,
    /** Descending by share. Empty when [showDecades] is false. */
    val decades: List<WrappedDecadeShare>,
    /**
     * False below [DECADE_COVERAGE_FLOOR], where the chart would be drawn from a minority of plays
     * and read as a claim about the whole year. Card 09 is skipped entirely, not shown empty.
     */
    val showDecades: Boolean,
    /** Share of plays whose album carried a usable year, 0..1. Printed on card 09 as a caveat. */
    val decadeCoverage: Float,
) {
    /** The reel's cards, in order, with the ones this year cannot fill already dropped. */
    val cards: List<WrappedCard>
        get() =
            WrappedCard.entries.filter { card ->
                when (card) {
                    WrappedCard.DECADES -> showDecades
                    WrappedCard.TOP_ALBUMS -> topAlbums.isNotEmpty()
                    WrappedCard.TOP_TRACKS -> topTracks.isNotEmpty()
                    WrappedCard.TOP_ARTISTS -> topArtists.isNotEmpty()
                    WrappedCard.BIGGEST_DAY -> biggestDay != null
                    else -> true
                }
            }

    companion object {
        /** Below this, card 09 is dropped rather than drawn from a minority of plays. */
        const val DECADE_COVERAGE_FLOOR = 0.5f

        /** Days of listening before Wrapped says anything at all. */
        const val REQUIRED_ACTIVE_DAYS = 30
    }
}

/** The reel, in order. Declaration order IS the running order. */
enum class WrappedCard {
    OPENING,
    MINUTES,
    TOP_TRACKS,
    TOP_ARTISTS,
    TOP_ALBUMS,
    CLOCK,
    BIGGEST_DAY,
    TYPE,
    DECADES,
    SHARE,
}

data class WrappedTrack(
    /** 1-based, as printed. */
    val rank: Int,
    val song: SongEntity,
    val playCount: Int,
    val listenedSeconds: Long,
)

/**
 * @property wasTopArtistLastYear true only for the artist ranked first BOTH years — what card 04's
 *   "top artist two years running" badge claims. Any other reading makes the badge a lie.
 */
data class WrappedArtist(
    val rank: Int,
    val artist: ArtistEntity,
    val playCount: Int,
    val listenedSeconds: Long,
    val wasTopArtistLastYear: Boolean,
)

data class WrappedAlbum(
    val rank: Int,
    val album: AlbumEntity,
    val playCount: Int,
)

/**
 * @property typicalPlays [AnalyticsPeriodStats.playsPerActiveDay] — plays over the days that had
 *   any, not over the calendar year, so the comparison on card 07 is against a real day.
 * @property topTrack the track played most on that one day, or null when it could not be resolved.
 */
data class WrappedBiggestDay(
    val date: LocalDate,
    val plays: Int,
    val typicalPlays: Int,
    val topTrack: SongEntity?,
    val topTrackPlays: Int,
)

/**
 * @property playsByHour 24 entries, local hours, index 0 = midnight — passed through from
 *   [AnalyticsPeriodStats] so the ring and the peak agree.
 * @property bandShare share of plays landing inside [band]'s six hours, 0..1.
 */
data class WrappedClock(
    val playsByHour: List<Int>,
    val peakHour: Int,
    val band: WrappedListeningBand,
    val bandShare: Float,
)

/** Six-hour bands. [WrappedClock.band] is whichever holds the most plays, not whichever holds the peak hour. */
enum class WrappedListeningBand(
    val startHour: Int,
    val endHour: Int,
) {
    NIGHT(0, 6),
    MORNING(6, 12),
    AFTERNOON(12, 18),
    EVENING(18, 24),
}

/**
 * One archetype per fingerprint axis, chosen by whichever axis reads highest.
 *
 * Deliberately a straight argmax over [com.maxrave.domain.data.model.analytics.ListeningFingerprint.axes]
 * with ties broken by declaration order, so the same year always yields the same name. Every axis
 * is already bounded 0..1 by construction, which is what makes them comparable at all.
 */
enum class WrappedArchetype {
    /** consistency — music nearly every day, evenly spread. */
    THE_REGULAR,

    /** discovery — a large share of the artists heard were new. */
    THE_EXPLORER,

    /** diversity — many artists, none of them dominant. */
    THE_OMNIVORE,

    /** concentration — most of the year went to a handful of artists. */
    THE_DEVOTEE,

    /** replay — the same songs, again. */
    THE_DEEP_DIVER,
}

data class WrappedDecadeShare(
    /** 2020, 2010, … as stored. */
    val decade: Int,
    val plays: Int,
    /** Of dated plays, not of all plays — the denominator card 09 names out loud. */
    val share: Float,
)
