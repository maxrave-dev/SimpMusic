package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.analytics.query.TopPlayedArtistTime
import com.maxrave.domain.data.model.taste.TasteAlbum
import com.maxrave.domain.data.model.taste.TasteException
import com.maxrave.domain.data.model.taste.TasteInput
import com.maxrave.domain.data.model.taste.TasteProfile
import com.maxrave.domain.data.model.taste.TasteSong
import com.maxrave.domain.extension.now
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.TasteRepository
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.taste_error_generic
import simpmusic.composeapp.generated.resources.taste_not_configured_title
import kotlin.coroutines.cancellation.CancellationException

/**
 * What the Library taste card shows.
 *
 * Every state that comes before a reading carries the top artists' pictures, so the card keeps the
 * same faces from the invitation through loading and errors.
 */
sealed interface TasteUiState {
    /** Local tracking is off, or the history has not been counted yet. */
    data object Hidden : TasteUiState

    data class Invite(
        val artistImages: List<String>,
    ) : TasteUiState

    data class NotEnoughData(
        val activeDays: Int,
        val requiredDays: Int,
        val artistImages: List<String>,
    ) : TasteUiState

    data class Loading(
        val artistImages: List<String>,
    ) : TasteUiState

    data class Failed(
        val kind: TasteException.Kind,
        val statusCode: Int?,
        val detail: String?,
        val artistImages: List<String>,
    ) : TasteUiState

    data class Ready(
        val profile: TasteProfile,
        val regenerating: Boolean,
    ) : TasteUiState
}

/**
 * The Library "Your music taste" card: whether it shows, what it shows, and asking the AI.
 *
 * The reading is written by whichever AI provider the user configured, and the AI knows nothing of
 * the listener on its own: everything it reads is gathered here from the local history, through the
 * same queries [WrappedViewModel] composes a year from.
 */
class TasteViewModel(
    private val dataStoreManager: DataStoreManager,
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val tasteRepository: TasteRepository,
) : BaseViewModel() {
    private sealed interface Request {
        data object Idle : Request

        data object Running : Request

        data class Failed(
            val error: TasteException,
        ) : Request
    }

    private data class Preview(
        val activeDays: Int,
        val artistImages: List<String>,
    )

    private data class NamedArtist(
        val name: String,
        val thumbnails: String?,
    )

    private val request = MutableStateFlow<Request>(Request.Idle)

    // Counted once per view model: one scan over the window, the same cost the Wrapped tab pays for
    // a year, and only once something is watching. A failed count hides the card rather than
    // taking the screen down with it.
    private val preview: StateFlow<Preview?> =
        flow<Preview?> { emit(loadPreview()) }
            .catch { e ->
                Logger.e(tag, "Taste preview failed: ${e.stackTraceToString()}")
                emit(null)
            }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val uiState: StateFlow<TasteUiState> =
        combine(
            dataStoreManager.localTrackingEnabled,
            tasteRepository.tasteProfile,
            request,
            preview,
        ) { tracking, profile, request, preview ->
            val images = preview?.artistImages.orEmpty()
            when {
                tracking != DataStoreManager.TRUE -> TasteUiState.Hidden
                profile != null -> TasteUiState.Ready(profile, regenerating = request is Request.Running)
                request is Request.Running -> TasteUiState.Loading(images)
                request is Request.Failed ->
                    TasteUiState.Failed(request.error.kind, request.error.statusCode, request.error.message, images)
                preview == null -> TasteUiState.Hidden
                preview.activeDays < REQUIRED_ACTIVE_DAYS ->
                    TasteUiState.NotEnoughData(preview.activeDays, REQUIRED_ACTIVE_DAYS, images)
                else -> TasteUiState.Invite(images)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasteUiState.Hidden)

    init {
        // Back from Settings with a key: the "not set up" card has nothing left to say, so it turns
        // back into the invitation instead of waiting for a tap to find out.
        viewModelScope.launch {
            dataStoreManager.aiApiKey.collect { key ->
                val current = request.value
                if (key.isNotBlank() && current is Request.Failed && current.error.kind == TasteException.Kind.NOT_CONFIGURED) {
                    request.value = Request.Idle
                }
            }
        }
    }

    /** Asks for a reading, or a new one. A failed new one leaves the stored reading on the card. */
    fun generate() {
        if (request.value is Request.Running) return
        // Set before the coroutine starts, so a second tap in the same frame finds it and does not
        // send a second request.
        request.value = Request.Running
        viewModelScope.launch {
            val hadReading = tasteRepository.tasteProfile.first() != null
            val result =
                if (dataStoreManager.aiApiKey.first().isBlank()) {
                    Result.failure(TasteException(TasteException.Kind.NOT_CONFIGURED))
                } else {
                    try {
                        val (input, images) = buildInput()
                        tasteRepository.describeTaste(input, images)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            result
                .onSuccess { profile ->
                    // Stay "loading" until the stored reading is the one the card reads back;
                    // otherwise the invitation shows for a frame between the two.
                    withTimeoutOrNull(STORE_TIMEOUT_MS) {
                        tasteRepository.tasteProfile.first { it?.generatedAt == profile.generatedAt }
                    }
                    request.value = Request.Idle
                }.onFailure { e ->
                    Logger.e(tag, "Taste reading failed: ${e.stackTraceToString()}")
                    val error = e as? TasteException ?: TasteException(TasteException.Kind.TEMPORARY, message = e.message, cause = e)
                    if (hadReading) {
                        request.value = Request.Idle
                        makeToast(
                            if (error.kind == TasteException.Kind.NOT_CONFIGURED) {
                                getString(Res.string.taste_not_configured_title)
                            } else {
                                error.message ?: getString(Res.string.taste_error_generic)
                            },
                        )
                    } else {
                        request.value = Request.Failed(error)
                    }
                }
        }
    }

    private suspend fun loadPreview(): Preview {
        val (start, end) = window(periodsBack = 0)
        val activeDays = analyticsRepository.getPeriodStats(start, end).activeDays
        val images = resolveArtists(artistRows(start, end), creditedNames = emptyMap(), limit = ARTIST_IMAGES).imagesOf()
        return Preview(activeDays, images)
    }

    private suspend fun buildInput(): Pair<TasteInput, List<String>> {
        val (start, end) = window(periodsBack = 0)
        val (previousStart, previousEnd) = window(periodsBack = 1)
        val stats = analyticsRepository.getPeriodStats(start, end)
        val songs = topSongs(start, end)
        // An artist can be all over the year without a row of its own — rows are written when an
        // artist page is opened, and radio never opens one. The songs still credit it by name, id
        // for id, so the name comes from there instead of a network round trip per artist.
        val creditedNames =
            songs
                .flatMap { song -> song.artistId.orEmpty().zip(song.artistName.orEmpty()) }
                .filter { (id, name) -> id.isNotBlank() && name.isUsableName() }
                .toMap()
        val artists = resolveArtists(artistRows(start, end), creditedNames, TOP_ARTISTS)
        val previousRows = artistRows(previousStart, previousEnd)
        val previousArtists = resolveArtists(previousRows, creditedNames = emptyMap(), limit = PREVIOUS_ARTISTS)
        val input =
            TasteInput(
                language = dataStoreManager.language.first(),
                // Judged on the rows, not on the names that resolved: a year of artists nobody ever
                // opened a page for is still a year of history.
                hasEarlierHistory = previousRows.isNotEmpty(),
                topArtists = artists.map { it.name },
                topSongs = songs.map { TasteSong(title = it.title, artists = it.artistName.creditLine()) },
                topAlbums = topAlbums(start, end).map { TasteAlbum(it.title, it.artistName.creditLine(), it.year?.takeIf(String::isNotBlank)) },
                dayParts = dayPartsOf(stats.playsByHour),
                replay = stats.fingerprint.replay,
                concentration = stats.fingerprint.concentration,
                previousTopArtists = previousArtists.map { it.name },
            )
        return input to artists.imagesOf()
    }

    private suspend fun topSongs(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<SongEntity> {
        val rows = analyticsRepository.queryTopPlayedSongsInRange(startTimestamp = start, endTimestamp = end).firstOrNull().orEmpty()
        val songs = mutableListOf<SongEntity>()
        for (row in rows) {
            if (songs.size == TOP_SONGS) break
            songRepository.getSongById(row.videoId).firstOrNull()?.let { songs += it }
        }
        return songs
    }

    private suspend fun artistRows(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<TopPlayedArtistTime> =
        analyticsRepository
            .queryTopArtistsWithTimeInRange(startTimestamp = start, endTimestamp = end)
            .firstOrNull()
            .orEmpty()

    private suspend fun resolveArtists(
        rows: List<TopPlayedArtistTime>,
        creditedNames: Map<String, String>,
        limit: Int,
    ): List<NamedArtist> {
        val artists = mutableListOf<NamedArtist>()
        for (row in rows) {
            if (artists.size == limit) break
            // Plays credited to an empty channel id name no artist at all; on a real history they
            // were one play in eight, enough to make a blank line the top "artist".
            if (row.channelId.isBlank()) continue
            val stored = artistRepository.getArtistById(row.channelId).firstOrNull()
            val name = stored?.name?.takeIf { it.isUsableName() } ?: creditedNames[row.channelId] ?: continue
            artists += NamedArtist(name, stored?.thumbnails)
        }
        return artists
    }

    private suspend fun topAlbums(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<AlbumEntity> {
        val rows = analyticsRepository.queryTopAlbumsInRange(startTimestamp = start, endTimestamp = end).firstOrNull().orEmpty()
        val albums = mutableListOf<AlbumEntity>()
        for (row in rows) {
            if (albums.size == TOP_ALBUMS) break
            albumRepository.getAlbum(row.albumBrowseId).firstOrNull()?.let { albums += it }
        }
        return albums
    }

    private fun List<NamedArtist>.imagesOf(): List<String> = mapNotNull { it.thumbnails?.takeIf(String::isNotBlank) }.take(ARTIST_IMAGES)

    private fun List<String>?.creditLine(): String = orEmpty().filter { it.isUsableName() }.joinToString(", ")

    // A view count that slipped into an artist credit ("28M plays") is not a name the AI should read.
    private fun String.isUsableName(): Boolean = isNotBlank() && !NOT_A_NAME.matches(trim())

    /** Shares of the day's plays in four six-hour bands from midnight, the same bands Wrapped uses. */
    private fun dayPartsOf(playsByHour: List<Int>): List<Float> {
        val total = playsByHour.sum()
        if (total == 0) return List(DAY_PARTS) { 0f }
        val hoursPerPart = playsByHour.size / DAY_PARTS
        return List(DAY_PARTS) { part ->
            playsByHour.subList(part * hoursPerPart, (part + 1) * hoursPerPart).sum().toFloat() / total
        }
    }

    /** The twelve months up to today ([periodsBack] = 0), or the twelve months just before them (1). */
    private fun window(periodsBack: Int): Pair<LocalDateTime, LocalDateTime> {
        val today = now().date
        val end = today.minus(periodsBack * WINDOW_DAYS, DateTimeUnit.DAY)
        val start = end.minus(WINDOW_DAYS - 1, DateTimeUnit.DAY)
        return start.atTime(0, 0) to end.atTime(23, 59, 59)
    }

    companion object {
        /** The same bar Wrapped sets before it says anything about a listener. */
        const val REQUIRED_ACTIVE_DAYS = WrappedYear.REQUIRED_ACTIVE_DAYS

        private const val WINDOW_DAYS = 365
        private const val TOP_ARTISTS = 25
        private const val TOP_SONGS = 30
        private const val TOP_ALBUMS = 10
        private const val PREVIOUS_ARTISTS = 10
        private const val ARTIST_IMAGES = 3
        private const val DAY_PARTS = 4
        private const val STORE_TIMEOUT_MS = 2_000L
        private val NOT_A_NAME = Regex("""^\d[\d.,]*\s*[KMB]?\s+(plays?|views?|lượt\s+(nghe|xem|phát))$""", RegexOption.IGNORE_CASE)
    }
}
