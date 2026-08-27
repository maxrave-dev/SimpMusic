package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.Config.REMOVED_SONG_DATE_TIME
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.extension.now
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.toArrayListTrack
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.ui.screen.home.analytics.monthFullNameResource
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.wrapped_recap_month
import simpmusic.composeapp.generated.resources.wrapped_recap_month_year

class LibraryDynamicPlaylistViewModel(
    private val songRepository: SongRepository,
    private val artistRepository: ArtistRepository,
    private val analyticsRepository: AnalyticsRepository,
) : BaseViewModel() {
    private val _listFavoriteSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listFavoriteSong: StateFlow<List<SongEntity>> get() = _listFavoriteSong

    private val _listFollowedArtist: MutableStateFlow<List<ArtistEntity>> = MutableStateFlow(emptyList())
    val listFollowedArtist: StateFlow<List<ArtistEntity>> get() = _listFollowedArtist

    private val _listMostPlayedSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listMostPlayedSong: StateFlow<List<SongEntity>> get() = _listMostPlayedSong

    private val _listDownloadedSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listDownloadedSong: StateFlow<List<SongEntity>> get() = _listDownloadedSong

    /**
     * One month's top songs, filled in only once a route names the month.
     *
     * Unlike the four lists above it is not started in [init] and not observed: there is no
     * "current" month here — the screen may be showing any of the last twelve — so loading is
     * driven by [getMonthlyRecapSong].
     */
    private val _listMonthlyRecapSong: MutableStateFlow<List<SongEntity>> = MutableStateFlow(emptyList())
    val listMonthlyRecapSong: StateFlow<List<SongEntity>> get() = _listMonthlyRecapSong

    /**
     * The name of the month [_listMonthlyRecapSong] currently holds, resolved when it was loaded.
     *
     * Resolved there rather than where the queue is built because "Recap January" needs a format
     * argument, and the only `getString` that takes one suspends — [playAll] and [shuffle] do not.
     * Loading already runs in a coroutine, so the name comes free at the one moment it is knowable.
     */
    private var loadedRecapName: String? = null

    init {
        getFavoriteSong()
        getFollowedArtist()
        getMostPlayedSong()
        getDownloadedSong()
    }

    private fun getFavoriteSong() {
        viewModelScope.launch {
            songRepository.getLikedSongs().collectLatest { likedSong ->
                _listFavoriteSong.value =
                    likedSong.sortedByDescending {
                        it.favoriteAt ?: REMOVED_SONG_DATE_TIME
                    }
            }
        }
    }

    private fun getFollowedArtist() {
        viewModelScope.launch {
            artistRepository.getFollowedArtists().collectLatest { followedArtist ->
                _listFollowedArtist.value =
                    followedArtist.sortedByDescending {
                        it.followedAt ?: REMOVED_SONG_DATE_TIME
                    }
            }
        }
    }

    private fun getMostPlayedSong() {
        viewModelScope.launch {
            songRepository.getMostPlayedSongs().collectLatest { mostPlayedSong ->
                _listMostPlayedSong.value = mostPlayedSong.sortedByDescending { it.totalPlayTime }
            }
        }
    }

    private fun getDownloadedSong() {
        viewModelScope.launch {
            songRepository.getDownloadedSongs().collectLatest { downloadedSong ->
                _listDownloadedSong.value =
                    (downloadedSong ?: emptyList()).sortedByDescending {
                        it.downloadedAt ?: REMOVED_SONG_DATE_TIME
                    }
            }
        }
    }

    /**
     * One calendar month's top songs, newest ranking first.
     *
     * Same two steps the Analytics screen's top-tracks list takes — the ranking from
     * [com.maxrave.domain.repository.AnalyticsRepository], then each row's `videoId` paired with
     * its stored [SongEntity] — so a recap and the Analytics list can never disagree about what
     * was played.
     *
     * The month's bounds are local wall-clock and inclusive at both ends, matching
     * `WrappedViewModel.yearRange`: the query underneath matches with `BETWEEN`, so ending at
     * midnight on the 1st of the next month would drop the last day of this one.
     */
    fun getMonthlyRecapSong(recap: LibraryDynamicPlaylistType.MonthlyRecap) {
        viewModelScope.launch {
            loadedRecapName = recapName(recap)
            val firstDay = LocalDate(recap.year, recap.month, 1)
            val lastDay = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
            analyticsRepository
                .queryTopPlayedSongsInRange(
                    startTimestamp = firstDay.atTime(0, 0),
                    endTimestamp = lastDay.atTime(23, 59, 59),
                ).collectLatest { rows ->
                    _listMonthlyRecapSong.value =
                        rows
                            // Capped after resolving, not before: a row whose song row is gone
                            // would otherwise leave a "top 50" 49 long. The query already stops at
                            // 100, so this reaches at most that far — the same shape as
                            // `WrappedViewModel.resolveTop`.
                            .mapNotNull { songRepository.getSongById(it.videoId).lastOrNull() }
                            .take(MONTHLY_RECAP_LIMIT)
                }
        }
    }

    /** "Recap January", or "Recap January 2025" once the year stops being obvious. */
    private suspend fun recapName(recap: LibraryDynamicPlaylistType.MonthlyRecap): String {
        val month =
            org.jetbrains.compose.resources
                .getString(monthFullNameResource(Month(recap.month)))
        return if (recap.year == now().date.year) {
            org.jetbrains.compose.resources
                .getString(Res.string.wrapped_recap_month, month)
        } else {
            org.jetbrains.compose.resources
                .getString(Res.string.wrapped_recap_month_year, month, recap.year.toString())
        }
    }

    /**
     * What the Now Playing screen calls the queue this screen started.
     *
     * A recap cannot answer [LibraryDynamicPlaylistType.name] with its own name, so it is the one
     * case read from the name resolved at load time instead.
     */
    private fun playlistName(type: LibraryDynamicPlaylistType): String {
        val name =
            when (type) {
                is LibraryDynamicPlaylistType.MonthlyRecap -> loadedRecapName ?: getString(type.name())
                else -> getString(type.name())
            }
        return "${getString(Res.string.playlist)} $name"
    }

    fun playSong(
        videoId: String,
        type: LibraryDynamicPlaylistType,
    ) {
        val (targetList, playTrack) =
            when (type) {
                LibraryDynamicPlaylistType.Favorite -> listFavoriteSong.value to listFavoriteSong.value.find { it.videoId == videoId }
                LibraryDynamicPlaylistType.Downloaded -> listDownloadedSong.value to listDownloadedSong.value.find { it.videoId == videoId }
                LibraryDynamicPlaylistType.Followed -> return
                LibraryDynamicPlaylistType.MostPlayed -> listMostPlayedSong.value to listMostPlayedSong.value.find { it.videoId == videoId }
                is LibraryDynamicPlaylistType.MonthlyRecap ->
                    listMonthlyRecapSong.value to listMonthlyRecapSong.value.find { it.videoId == videoId }
                else -> return
            }
        if (playTrack == null) return
        setQueueData(
            QueueData.Data(
                listTracks = targetList.toArrayListTrack(),
                firstPlayedTrack = playTrack.toTrack(),
                playlistId = null,
                playlistName = playlistName(type),
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(
            playTrack.toTrack(),
            Config.PLAYLIST_CLICK,
            targetList.indexOf(playTrack).coerceAtLeast(0),
        )
    }

    private fun getSongList(type: LibraryDynamicPlaylistType): List<SongEntity> =
        when (type) {
            LibraryDynamicPlaylistType.Favorite -> listFavoriteSong.value
            LibraryDynamicPlaylistType.Downloaded -> listDownloadedSong.value
            LibraryDynamicPlaylistType.MostPlayed -> listMostPlayedSong.value
            is LibraryDynamicPlaylistType.MonthlyRecap -> listMonthlyRecapSong.value
            else -> emptyList()
        }

    fun playAll(type: LibraryDynamicPlaylistType) {
        val targetList = getSongList(type)
        val firstTrack = targetList.firstOrNull() ?: return
        setQueueData(
            QueueData.Data(
                listTracks = targetList.toArrayListTrack(),
                firstPlayedTrack = firstTrack.toTrack(),
                playlistId = null,
                playlistName = playlistName(type),
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(
            firstTrack.toTrack(),
            Config.PLAYLIST_CLICK,
            0,
        )
    }

    fun shuffle(type: LibraryDynamicPlaylistType) {
        val targetList = getSongList(type)
        if (targetList.isEmpty()) return
        val shuffledList = targetList.shuffled()
        val firstTrack = shuffledList.first()
        setQueueData(
            QueueData.Data(
                listTracks = shuffledList.toArrayListTrack(),
                firstPlayedTrack = firstTrack.toTrack(),
                playlistId = null,
                playlistName = playlistName(type),
                playlistType = PlaylistType.RADIO,
                continuation = null,
            ),
        )
        loadMediaItem(
            firstTrack.toTrack(),
            Config.PLAYLIST_CLICK,
            0,
        )
    }

    companion object {
        /** Songs a monthly recap holds. The ranking underneath already stops at 100. */
        private const val MONTHLY_RECAP_LIMIT = 50
    }
}