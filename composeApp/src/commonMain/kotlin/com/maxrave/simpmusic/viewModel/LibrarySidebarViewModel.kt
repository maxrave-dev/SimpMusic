package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.LOCAL_PLAYLIST_ID
import com.maxrave.domain.data.model.library.LibraryOverview
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.PodcastRepository
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.LocalPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

/**
 * Feeds the Desktop sidebar: the pinned collections, everything else the user keeps (local and
 * saved playlists, saved albums, followed artists, favourite podcasts) as one list, which queue is
 * playing, and whether the sidebar is folded. The five library sources are one-shot reads, not
 * observed queries, so the list is read again on [refresh].
 */
class LibrarySidebarViewModel(
    private val dataStoreManager: DataStoreManager,
    commonRepository: CommonRepository,
    localPlaylistRepository: LocalPlaylistRepository,
    playlistRepository: PlaylistRepository,
    albumRepository: AlbumRepository,
    artistRepository: ArtistRepository,
    podcastRepository: PodcastRepository,
) : BaseViewModel() {
    val overview: StateFlow<LibraryOverview?> =
        commonRepository
            .getLibraryOverview()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val refreshes = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<LibrarySidebarItem>> =
        refreshes
            .flatMapLatest {
                combine(
                    localPlaylistRepository.getAllLocalPlaylists(),
                    playlistRepository.getLikedPlaylists(),
                    albumRepository.getLikedAlbums(),
                    artistRepository.getFollowedArtists(),
                    podcastRepository.getFavoritePodcasts(),
                ) { local, playlists, albums, artists, podcasts ->
                    local.map {
                        LibrarySidebarItem(
                            key = "local_${it.id}",
                            kind = LibrarySidebarKind.PLAYLIST,
                            title = it.title,
                            subtitle = null,
                            trackCount = it.tracks?.size ?: 0,
                            thumbnail = it.thumbnail,
                            addedAt = it.inLibrary,
                            destination = LocalPlaylistDestination(it.id),
                            queueId = LOCAL_PLAYLIST_ID + it.id,
                        )
                    } +
                        playlists.map {
                            LibrarySidebarItem(
                                key = "playlist_${it.id}",
                                kind = LibrarySidebarKind.PLAYLIST,
                                title = it.title,
                                subtitle = it.author,
                                thumbnail = it.thumbnails,
                                addedAt = it.favoriteAt ?: it.inLibrary,
                                destination = PlaylistDestination(it.id),
                                queueId = it.id,
                            )
                        } +
                        albums.map {
                            LibrarySidebarItem(
                                key = "album_${it.browseId}",
                                kind = LibrarySidebarKind.ALBUM,
                                title = it.title,
                                subtitle = it.artistName?.joinToString(", "),
                                thumbnail = it.thumbnails,
                                addedAt = it.favoriteAt ?: it.inLibrary,
                                destination = AlbumDestination(it.browseId),
                                queueId = it.browseId,
                            )
                        } +
                        artists.map {
                            LibrarySidebarItem(
                                key = "artist_${it.channelId}",
                                kind = LibrarySidebarKind.ARTIST,
                                title = it.name,
                                subtitle = null,
                                thumbnail = it.thumbnails,
                                addedAt = it.followedAt ?: it.inLibrary,
                                destination = ArtistDestination(it.channelId),
                                queueId = null,
                            )
                        } +
                        podcasts.map {
                            LibrarySidebarItem(
                                key = "podcast_${it.podcastId}",
                                kind = LibrarySidebarKind.PODCAST,
                                title = it.title,
                                subtitle = it.authorName,
                                thumbnail = it.thumbnail,
                                addedAt = it.favoriteTime ?: it.inLibrary,
                                destination = PodcastDestination(it.podcastId),
                                queueId = it.podcastId,
                            )
                        }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // The id the playing queue was started from, in the same shape LibrarySidebarItem.queueId uses.
    val playingQueueId: StateFlow<String?> =
        mediaPlayerHandler.queueData
            .map { it?.data?.playlistId?.removePrefix("VL") }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val collapsed: StateFlow<Boolean> =
        dataStoreManager
            .getString(COLLAPSED_KEY)
            .map { it == DataStoreManager.TRUE }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun refresh() {
        refreshes.update { it + 1 }
    }

    fun setCollapsed(collapsed: Boolean) {
        viewModelScope.launch {
            dataStoreManager.putString(COLLAPSED_KEY, if (collapsed) DataStoreManager.TRUE else DataStoreManager.FALSE)
        }
    }

    private companion object {
        const val COLLAPSED_KEY = "desktop_sidebar_collapsed"
    }
}

enum class LibrarySidebarKind { PLAYLIST, ARTIST, ALBUM, PODCAST }

data class LibrarySidebarItem(
    val key: String,
    val kind: LibrarySidebarKind,
    val title: String,
    // The playlist's author, the album's artists or the podcast's host; null where the kind says enough.
    val subtitle: String?,
    val thumbnail: String?,
    val addedAt: LocalDateTime,
    val destination: Any,
    // What the player's queue carries as its playlist id while this item plays; null if it never does.
    val queueId: String?,
    // Only local playlists show their size, since they have no author to show instead.
    val trackCount: Int? = null,
)
