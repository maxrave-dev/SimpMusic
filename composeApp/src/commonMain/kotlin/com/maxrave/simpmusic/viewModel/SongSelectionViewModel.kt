package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.collectResource
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_to_playlist
import simpmusic.composeapp.generated.resources.added_to_queue
import simpmusic.composeapp.generated.resources.delete_song_from_playlist
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.play_next
import simpmusic.composeapp.generated.resources.removed_from_YouTube_playlist

/**
 * Runs the bulk actions offered by
 * [com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet].
 *
 * Shared by every screen that offers multi-selection: the actions only ever need videoIds, so
 * they do not depend on which screen the selection was made on. The one exception is removing
 * from a local playlist, which takes the playlist id from the screen.
 */
class SongSelectionViewModel(
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
) : BaseViewModel() {
    private val downloadUtils: DownloadHandler by inject()

    val listLocalPlaylist: StateFlow<List<LocalPlaylistEntity>> =
        localPlaylistRepository
            .getAllLocalPlaylists()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Each call inserts right after the current track, so playing them in order would leave the
     * queue reversed — the last one inserted ends up first. Walking the list backwards puts them
     * in the order the user saw them.
     */
    fun playNext(videoIds: List<String>) {
        viewModelScope.launch {
            val songs = songsOf(videoIds)
            if (songs.isEmpty()) {
                makeToast(getString(Res.string.error_occurred))
                return@launch
            }
            songs.asReversed().forEach { mediaPlayerHandler.playNext(it.toTrack()) }
            makeToast(getString(Res.string.play_next))
        }
    }

    fun addToQueue(videoIds: List<String>) {
        viewModelScope.launch {
            val songs = songsOf(videoIds)
            if (songs.isEmpty()) {
                makeToast(getString(Res.string.error_occurred))
                return@launch
            }
            mediaPlayerHandler.loadMoreCatalog(
                ArrayList(songs.map { it.toTrack() }),
                isAddToQueue = true,
            )
            makeToast(getString(Res.string.added_to_queue))
        }
    }

    /**
     * Only starts songs that are not already downloaded or in flight — unlike the single-song
     * menu, this is not a toggle: a selection of 25 will usually mix downloaded and not, and
     * toggling would silently delete the ones already on disk.
     */
    fun download(videoIds: List<String>) {
        viewModelScope.launch {
            val pending =
                songsOf(videoIds).filter {
                    it.downloadState == DownloadState.STATE_NOT_DOWNLOADED
                }
            if (pending.isEmpty()) return@launch
            pending.forEach { song ->
                songRepository.updateDownloadState(
                    videoId = song.videoId,
                    downloadState = DownloadState.STATE_PREPARING,
                )
                downloadUtils.downloadTrack(
                    videoId = song.videoId,
                    title = song.title,
                    thumbnail = song.thumbnails ?: "",
                )
            }
            makeToast(getString(Res.string.downloading))
        }
    }

    fun addToFavorite(videoIds: List<String>) {
        viewModelScope.launch {
            songsOf(videoIds)
                .filterNot { it.liked }
                .forEach { songRepository.updateLikeStatus(it.videoId, 1) }
        }
    }

    fun addToPlaylist(
        playlistId: Long,
        videoIds: List<String>,
    ) {
        viewModelScope.launch {
            val playlist = localPlaylistRepository.getAllLocalPlaylists().firstOrNull()?.find { it.id == playlistId }
            val already = playlist?.tracks.orEmpty()
            val toAdd = songsOf(videoIds).filterNot { already.contains(it.videoId) }
            if (toAdd.isEmpty()) return@launch
            var added = 0
            toAdd.forEach { song ->
                localPlaylistRepository
                    .addTrackToLocalPlaylist(
                        id = playlistId,
                        song = song,
                        successMessage = getString(Res.string.added_to_playlist),
                        updatedYtMessage = getString(Res.string.added_to_playlist),
                        errorMessage = getString(Res.string.error),
                    ).collectResource(
                        onSuccess = { added++ },
                        onError = { },
                    )
            }
            // One toast for the batch: 25 songs would otherwise stack 25 toasts.
            makeToast(
                if (added > 0) getString(Res.string.added_to_playlist) else getString(Res.string.error),
            )
        }
    }

    fun removeFromLocalPlaylist(
        playlistId: Long,
        videoIds: List<String>,
    ) {
        viewModelScope.launch {
            val songs = songsOf(videoIds)
            if (songs.isEmpty()) {
                makeToast(getString(Res.string.error_occurred))
                return@launch
            }
            songs.forEach { song ->
                localPlaylistRepository
                    .removeTrackFromLocalPlaylist(
                        id = playlistId,
                        song = song,
                        successMessage = getString(Res.string.delete_song_from_playlist),
                        updatedYtMessage = getString(Res.string.removed_from_YouTube_playlist),
                        errorMessage = getString(Res.string.error_occurred),
                    ).collectResource(
                        onSuccess = { },
                        onError = { },
                    )
            }
            makeToast(getString(Res.string.delete_song_from_playlist))
        }
    }

    /**
     * The database returns rows in its own order, so the result is re-sorted back into the order
     * the user picked them in — which is what makes "play next" land in the expected sequence.
     */
    private suspend fun songsOf(videoIds: List<String>): List<SongEntity> =
        songRepository
            .getSongsByListVideoId(videoIds)
            .firstOrNull()
            .orEmpty()
            .sortedBy { videoIds.indexOf(it.videoId) }
}
