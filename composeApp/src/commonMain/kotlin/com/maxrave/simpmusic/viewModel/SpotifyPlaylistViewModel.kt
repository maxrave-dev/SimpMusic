package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.spotify.SpotifyTrack
import com.maxrave.domain.repository.SpotifyLibraryRepository
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs [com.maxrave.simpmusic.ui.screen.library.SpotifyPlaylistScreen]: fetches a Spotify
 * playlist's tracks live, then resolves each one to a YouTube Music [Track] in the background
 * (see [SpotifyLibraryRepository.resolveTrack]) so it can be played through SimpMusic's normal
 * player pipeline. Tracks are resolved one at a time, in playlist order, rather than all at
 * once, to avoid hammering Spotify's GraphQL endpoint and YouTube Music search simultaneously.
 */
class SpotifyPlaylistViewModel(
    private val spotifyLibraryRepository: SpotifyLibraryRepository,
) : BaseViewModel() {
    private val _tracks: MutableStateFlow<LocalResource<List<SpotifyTrack>>> = MutableStateFlow(LocalResource.Loading())
    val tracks: StateFlow<LocalResource<List<SpotifyTrack>>> get() = _tracks.asStateFlow()

    /** Spotify track id -> resolved YouTube Music [Track], or `null` if no match was found. */
    private val _resolvedTracks: MutableStateFlow<Map<String, Track?>> = MutableStateFlow(emptyMap())
    val resolvedTracks: StateFlow<Map<String, Track?>> get() = _resolvedTracks.asStateFlow()

    private var loadedPlaylistId: String? = null

    fun loadTracks(playlistId: String) {
        if (loadedPlaylistId == playlistId) return
        loadedPlaylistId = playlistId
        _tracks.value = LocalResource.Loading()
        _resolvedTracks.value = emptyMap()
        viewModelScope.launch {
            spotifyLibraryRepository.getPlaylistTracks(playlistId).collect { resource ->
                _tracks.value = resource
                if (resource is LocalResource.Success) {
                    resolveTracks(resource.data.orEmpty())
                }
            }
        }
    }

    private fun resolveTracks(tracks: List<SpotifyTrack>) {
        viewModelScope.launch {
            for (track in tracks) {
                if (_resolvedTracks.value.containsKey(track.id)) continue
                val resolved = runCatching { spotifyLibraryRepository.resolveTrack(track) }.getOrNull()
                _resolvedTracks.update { it + (track.id to resolved) }
            }
        }
    }
}
