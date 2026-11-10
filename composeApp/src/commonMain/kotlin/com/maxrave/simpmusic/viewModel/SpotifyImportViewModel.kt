package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.model.spotify.SpotifyLibraryError
import com.maxrave.domain.data.model.spotify.SpotifyLibraryResult
import com.maxrave.domain.data.model.spotify.SpotifyPlaylistInfo
import com.maxrave.domain.data.model.spotify.SpotifyTrackInfo
import com.maxrave.domain.data.model.spotify.SpotifyTracksPage
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SpotifyLibraryRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.utils.SpotifyTrackMatcher
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** What the screen shows. Everything is derived from this one object. */
data class SpotifyImportUiState(
    val loggedIn: Boolean = false,
    val loadingPlaylists: Boolean = false,
    val playlists: List<SpotifyPlaylistInfo> = emptyList(),
    val error: SpotifyLibraryError? = null,
    val progress: SpotifyImportProgress? = null,
    val report: SpotifyImportReport? = null,
)

data class SpotifyImportProgress(
    val name: String,
    val processed: Int,
    val total: Int,
)

data class SpotifyImportReport(
    val name: String,
    val total: Int,
    val added: Int,
    /** Already in the local playlist from an earlier import, so not added twice. */
    val alreadyThere: Int,
    /** Local files, podcast episodes and removed tracks: nothing exists to look up. */
    val skippedUnavailable: Int,
    /** No YT Music result agreed on title, artist and length. Shown so the user can add them by hand. */
    val unmatched: List<String>,
    /** The playlist could not be read at all (Spotify-made, or not allowed for this account). */
    val error: SpotifyLibraryError? = null,
)

class SpotifyImportViewModel(
    private val spotifyLibraryRepository: SpotifyLibraryRepository,
    private val searchRepository: SearchRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(SpotifyImportUiState())
    val state: StateFlow<SpotifyImportUiState> = _state.asStateFlow()

    private var importJob: Job? = null

    fun refresh() {
        viewModelScope.launch {
            val loggedIn = spotifyLibraryRepository.isLoggedIn()
            _state.update { it.copy(loggedIn = loggedIn, error = null) }
            if (!loggedIn) {
                _state.update { it.copy(error = SpotifyLibraryError.NOT_LOGGED_IN, playlists = emptyList()) }
                return@launch
            }
            _state.update { it.copy(loadingPlaylists = true) }
            when (val result = spotifyLibraryRepository.getPlaylists()) {
                is SpotifyLibraryResult.Success ->
                    _state.update { it.copy(loadingPlaylists = false, playlists = result.data) }
                is SpotifyLibraryResult.Failure ->
                    _state.update { it.copy(loadingPlaylists = false, error = result.error) }
            }
        }
    }

    fun importPlaylist(playlist: SpotifyPlaylistInfo) =
        startImport(title = playlist.name, thumbnail = playlist.thumbnail) {
            spotifyLibraryRepository.getPlaylistTracks(playlist.id)
        }

    fun importLikedSongs() =
        startImport(title = LIKED_SONGS_TITLE, thumbnail = null) {
            spotifyLibraryRepository.getLikedTracks()
        }

    fun cancelImport() {
        importJob?.cancel()
        importJob = null
        _state.update { it.copy(progress = null) }
    }

    fun dismissReport() {
        _state.update { it.copy(report = null) }
    }

    private fun startImport(
        title: String,
        thumbnail: String?,
        fetch: suspend () -> SpotifyLibraryResult<SpotifyTracksPage>,
    ) {
        // One import at a time: two would race on the same local playlist's track positions.
        if (importJob?.isActive == true) return
        importJob =
            viewModelScope.launch {
                _state.update { it.copy(report = null, progress = SpotifyImportProgress(title, 0, 0)) }
                try {
                    val page =
                        when (val result = fetch()) {
                            is SpotifyLibraryResult.Failure -> {
                                _state.update {
                                    it.copy(
                                        progress = null,
                                        error = result.error.takeIf { e -> e != SpotifyLibraryError.NOT_ALLOWED },
                                        report = SpotifyImportReport(title, 0, 0, 0, 0, emptyList(), result.error),
                                    )
                                }
                                return@launch
                            }
                            is SpotifyLibraryResult.Success -> result.data
                        }
                    val report = importTracks(title = title, thumbnail = thumbnail, page = page)
                    _state.update { it.copy(progress = null, report = report) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e("SpotifyImport", "import failed: ${e.message}")
                    _state.update {
                        it.copy(
                            progress = null,
                            report = SpotifyImportReport(title, 0, 0, 0, 0, emptyList(), SpotifyLibraryError.NETWORK),
                        )
                    }
                }
            }
    }

    private suspend fun importTracks(
        title: String,
        thumbnail: String?,
        page: SpotifyTracksPage,
    ): SpotifyImportReport {
        val tracks = page.tracks
        if (tracks.isEmpty()) {
            return SpotifyImportReport(title, 0, 0, 0, page.skippedUnavailable, emptyList())
        }
        val playlistId = findOrCreateLocalPlaylist(localTitle(title), thumbnail)
        val inPlaylist = localPlaylistRepository.getListTrackVideoId(playlistId).toHashSet()

        var added = 0
        var alreadyThere = 0
        val unmatched = ArrayList<String>()
        val gate = Semaphore(SEARCH_PARALLELISM)

        // Searches run in parallel, but writes happen one by one in Spotify's order afterwards: the
        // local playlist derives each new position from its current length, so concurrent adds would collide.
        for ((index, chunk) in tracks.chunked(CHUNK_SIZE).withIndex()) {
            val matches =
                coroutineScope {
                    chunk
                        .map { track -> async(Dispatchers.IO) { gate.withPermit { findOnYtMusic(track) } } }
                        .awaitAll()
                }
            for ((track, match) in chunk.zip(matches)) {
                if (match == null) {
                    unmatched += "${track.artists.firstOrNull().orEmpty()} - ${track.title}".trim(' ', '-')
                    continue
                }
                if (!inPlaylist.add(match.videoId)) {
                    alreadyThere++
                    continue
                }
                localPlaylistRepository
                    .addTrackToLocalPlaylist(
                        id = playlistId,
                        song = match.toTrack().toSongEntity(),
                        successMessage = "",
                        updatedYtMessage = "",
                        errorMessage = "",
                    ).collect { }
                added++
            }
            _state.update {
                it.copy(progress = SpotifyImportProgress(title, minOf((index + 1) * CHUNK_SIZE, tracks.size), tracks.size))
            }
        }
        return SpotifyImportReport(
            name = title,
            total = tracks.size,
            added = added,
            alreadyThere = alreadyThere,
            skippedUnavailable = page.skippedUnavailable,
            unmatched = unmatched,
        )
    }

    private suspend fun findOnYtMusic(track: SpotifyTrackInfo) =
        try {
            when (val result = searchRepository.searchSongsFirstPage(SpotifyTrackMatcher.searchQuery(track))) {
                is Resource.Success -> SpotifyTrackMatcher.bestMatch(track, result.data.orEmpty())
                is Resource.Error -> null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w("SpotifyImport", "search failed for '${track.title}': ${e.message}")
            null
        }

    /** Reuses the playlist from a previous import, so importing again only adds what is new. */
    private suspend fun findOrCreateLocalPlaylist(
        title: String,
        thumbnail: String?,
    ): Long {
        localPlaylistRepository.getAllLocalPlaylists().first().firstOrNull { it.title == title }?.let { return it.id }
        localPlaylistRepository
            .insertLocalPlaylist(LocalPlaylistEntity(title = title, thumbnail = thumbnail), successMessage = "")
            .collect { }
        return localPlaylistRepository.getAllLocalPlaylists().first().first { it.title == title }.id
    }

    private fun localTitle(spotifyName: String) = if (spotifyName == LIKED_SONGS_TITLE) spotifyName else "$spotifyName (Spotify)"

    private companion object {
        const val LIKED_SONGS_TITLE = "Spotify Liked Songs"
        const val CHUNK_SIZE = 8
        const val SEARCH_PARALLELISM = 4
    }
}
