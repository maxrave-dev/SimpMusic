package com.maxrave.media3.service.callback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.common.MEDIA_CUSTOM_COMMAND
import com.maxrave.common.R
import com.maxrave.domain.data.entities.EpisodeEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.ChartItemPlaylist
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.data.model.mood.genre.GenreObject
import com.maxrave.domain.data.model.mood.moodmoments.MoodsMomentObject
import com.maxrave.domain.data.model.podcast.PodcastBrowse
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PodcastRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toListTrack
import com.maxrave.domain.utils.toPlaylistEntity
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.media3.extension.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.guava.future

private const val TAG = "AndroidAuto"

@UnstableApi
internal class SimpleMediaSessionCallback(
    private val context: Context,
    private val scope: CoroutineScope,
    private val mediaPlayerHandler: MediaPlayerHandler,
    private val searchRepository: SearchRepository,
    private val songRepository: SongRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val homeRepository: HomeRepository,
    private val streamRepository: StreamRepository,
    private val podcastRepository: PodcastRepository,
    private val albumRepository: AlbumRepository,
) : MediaLibrarySession.Callback {
    var toggleLike: () -> Unit = {
        mediaPlayerHandler.toggleLike()
    }
    var toggleRadio: () -> Unit = {
        mediaPlayerHandler.toggleRadio()
    }
    private val searchTempList = mutableListOf<Track>()
    private val listHomeItem = mutableListOf<HomeItem>()
    private val listChartItem = mutableListOf<ChartItemPlaylist>()
    private var moodCache: Mood? = null
    private val genreDetailsCache = mutableMapOf<String, GenreObject>()
    private val moodsMomentDetailsCache = mutableMapOf<String, MoodsMomentObject>()
    private val searchPlaylistResults = mutableListOf<PlaylistsResult>()
    private val searchAlbumResults = mutableListOf<AlbumsResult>()
    private val searchArtistResults = mutableListOf<ArtistsResult>()
    private val searchPodcastResults = mutableListOf<PlaylistsResult>()

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        val sessionCommands =
            connectionResult.availableSessionCommands
                .buildUpon()
                // Add custom commands
                .add(SessionCommand(MEDIA_CUSTOM_COMMAND.LIKE, Bundle()))
                .add(SessionCommand(MEDIA_CUSTOM_COMMAND.REPEAT, Bundle()))
                .add(SessionCommand(MEDIA_CUSTOM_COMMAND.RADIO, Bundle()))
                .add(SessionCommand(MEDIA_CUSTOM_COMMAND.SHUFFLE, Bundle()))
                .build()
        return MediaSession.ConnectionResult.accept(
            sessionCommands,
            connectionResult.availablePlayerCommands,
        )
    }

    @UnstableApi
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            MEDIA_CUSTOM_COMMAND.LIKE -> {
                toggleLike()
            }

            MEDIA_CUSTOM_COMMAND.REPEAT -> {
                session.player.repeatMode =
                    when (session.player.repeatMode) {
                        ExoPlayer.REPEAT_MODE_OFF -> ExoPlayer.REPEAT_MODE_ONE
                        ExoPlayer.REPEAT_MODE_ONE -> ExoPlayer.REPEAT_MODE_ALL
                        ExoPlayer.REPEAT_MODE_ALL -> ExoPlayer.REPEAT_MODE_OFF
                        else -> ExoPlayer.REPEAT_MODE_OFF
                    }
            }

            MEDIA_CUSTOM_COMMAND.RADIO -> {
                toggleRadio()
            }

            MEDIA_CUSTOM_COMMAND.SHUFFLE -> {
                val isShuffle = session.player.shuffleModeEnabled
                session.player.shuffleModeEnabled = !isShuffle
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(
            LibraryResult.ofItem(
                MediaItem
                    .Builder()
                    .setMediaId(ROOT)
                    .setMediaMetadata(
                        MediaMetadata
                            .Builder()
                            .setIsPlayable(false)
                            .setIsBrowsable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .build(),
                    ).build(),
                params,
            ),
        )

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> =
        scope.future(Dispatchers.IO) {
            searchTempList.clear()
            searchPlaylistResults.clear()
            searchAlbumResults.clear()
            searchArtistResults.clear()
            searchPodcastResults.clear()

            // Songs
            searchRepository.getSearchDataSong(query).lastOrNull()?.let { resource ->
                if (resource is Resource.Success) {
                    resource.data?.let { searchTempList.addAll(it.toListTrack()) }
                }
            }

            // Playlists (community and featured)
            val playlistMap = linkedMapOf<String, PlaylistsResult>()
            searchRepository.getSearchDataPlaylist(query).lastOrNull()?.let { resource ->
                if (resource is Resource.Success) {
                    resource.data?.forEach { playlistMap[it.browseId] = it }
                }
            }
            searchRepository.getSearchDataFeaturedPlaylist(query).lastOrNull()?.let { resource ->
                if (resource is Resource.Success) {
                    resource.data?.forEach { playlistMap[it.browseId] = it }
                }
            }
            // Filter out podcasts from playlists bucket; podcasts go to dedicated list
            playlistMap.values.filter { it.objectType().name != "PODCAST" }.let { searchPlaylistResults.addAll(it) }

            // Albums
            searchRepository.getSearchDataAlbum(query).lastOrNull()?.let { resource ->
                if (resource is Resource.Success) {
                    resource.data?.let { searchAlbumResults.addAll(it) }
                }
            }

            // Artists
            searchRepository.getSearchDataArtist(query).lastOrNull()?.let { resource ->
                if (resource is Resource.Success) {
                    resource.data?.let { searchArtistResults.addAll(it) }
                }
            }

            // Podcasts
            searchRepository.getSearchDataPodcast(query).lastOrNull()?.let { resource ->
                if (resource is Resource.Success) {
                    resource.data?.let { searchPodcastResults.addAll(it) }
                }
            }

            val total = searchTempList.size +
                searchPlaylistResults.size +
                searchAlbumResults.size +
                searchArtistResults.size +
                searchPodcastResults.size
            session.notifySearchResultChanged(browser, query, total, params)
            LibraryResult.ofVoid()
        }

    @UnstableApi
    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        scope.future(Dispatchers.IO) {
            val songs = searchTempList.map { it.toMediaItemWithoutPath() }
            val playlists = searchPlaylistResults.map { it.toMediaItemSearchPlaylist() }
            val albums = searchAlbumResults.map { it.toMediaItemSearchAlbum() }
            val artists = searchArtistResults.map { it.toMediaItemSearchArtist() }
            val podcasts = searchPodcastResults.map { it.toMediaItemSearchPodcast() }

            val buckets = listOf(songs, playlists, albums, artists, podcasts)
            val indices = IntArray(buckets.size) { 0 }
            val merged = mutableListOf<MediaItem>()

            val safePage = if (page < 0) 0 else page
            val hasPaging = pageSize > 0
            val targetCount = if (hasPaging) (safePage + 1) * pageSize else Int.MAX_VALUE

            var progressed: Boolean
            do {
                progressed = false
                for (i in buckets.indices) {
                    val bucket = buckets[i]
                    val idx = indices[i]
                    if (idx < bucket.size) {
                        merged.add(bucket[idx])
                        indices[i] = idx + 1
                        progressed = true
                        if (merged.size >= targetCount) break
                    }
                }
            } while (progressed && merged.size < targetCount)

            val from = if (hasPaging) (safePage * pageSize).coerceAtMost(merged.size) else 0
            val to = if (hasPaging) (from + pageSize).coerceAtMost(merged.size) else merged.size
            val pageItems = if (from < to) merged.subList(from, to) else emptyList()
            LibraryResult.ofItemList(pageItems, params)
        }

    @UnstableApi
    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        scope.future(Dispatchers.IO) {
            val rootExtras =
                Bundle().apply {
                    putBoolean(
                        MEDIA_SEARCH_SUPPORTED,
                        true,
                    )
                }
            val libraryParams =
                MediaLibraryService.LibraryParams
                    .Builder()
                    .setExtras(rootExtras)
                    .build()
            return@future LibraryResult.ofItemList(
                when (parentId) {
                    ROOT ->
                        listOf(
                            browsableMediaItem(
                                HOME,
                                context.getString(R.string.home),
                                context.getString(R.string.available_online),
                                drawableUri(R.drawable.home_android_auto),
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            ),
                            browsableMediaItem(
                                SONG,
                                context.getString(R.string.songs),
                                null,
                                drawableUri(R.drawable.baseline_album_24),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                            ),
                            browsableMediaItem(
                                PLAYLIST,
                                context.getString(R.string.playlists),
                                null,
                                drawableUri(R.drawable.baseline_playlist_add_24),
                                MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                            ),
                            browsableMediaItem(
                                CHART,
                                context.getString(R.string.chart),
                                context.getString(R.string.available_online),
                                drawableUri(R.drawable.baseline_trending_up_24),
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            ),
                            browsableMediaItem(
                                MOOD,
                                context.getString(R.string.moods_amp_moment),
                                context.getString(R.string.available_online),
                                drawableUri(R.drawable.baseline_tips_and_updates_24),
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            ),
                            browsableMediaItem(
                                GENRE,
                                context.getString(R.string.genre),
                                context.getString(R.string.available_online),
                                drawableUri(R.drawable.baseline_sort_by_alpha_24),
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            ),
                            browsableMediaItem(
                                PODCAST,
                                context.getString(R.string.podcasts),
                                null,
                                drawableUri(R.drawable.round_library_music_24),
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            ),
                        )

                    SONG ->
                        songRepository
                            .getAllSongs(1000)
                            .last()
                            .sortedBy { it.inLibrary }
                            .map { it.toMediaItem(parentId) }

                    PLAYLIST ->
                        localPlaylistRepository
                            .getAllLocalPlaylists()
                            .first()
                            .sortedBy { it.inLibrary }
                            .map {
                                browsableMediaItem(
                                    "$PLAYLIST/${it.id}",
                                    it.title,
                                    "${it.tracks?.size ?: 0} ${context.getString(R.string.track)}",
                                    it.thumbnail?.toUri(),
                                    MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                )
                            }

                    HOME -> {
                        val temp = homeRepository.getHomeData(context).lastOrNull()?.data
                        listHomeItem.clear()
                        listHomeItem.addAll(temp ?: emptyList())
                        temp?.map {
                            browsableMediaItem(
                                "$HOME/${it.title}",
                                it.title,
                                it.subtitle,
                                null,
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            )
                        } ?: emptyList()
                    }

                    CHART -> {
                        val chart = homeRepository.getChartData().lastOrNull()?.data
                        val categories = chart?.listChartItem ?: emptyList()
                        listChartItem.clear()
                        listChartItem.addAll(categories)
                        categories.map {
                            browsableMediaItem(
                                "$CHART/${it.title}",
                                it.title,
                                null,
                                drawableUri(R.drawable.baseline_trending_up_24),
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            )
                        }
                    }

                    MOOD -> {
                        val mood = homeRepository.getMoodAndMomentsData().lastOrNull()?.data
                        moodCache = mood
                        mood?.moodsMoments?.map {
                            browsableMediaItem(
                                "$MOOD/${it.params}",
                                it.title,
                                null,
                                drawableUri(R.drawable.baseline_tips_and_updates_24),
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            )
                        } ?: emptyList()
                    }

                    GENRE -> {
                        val mood = moodCache ?: homeRepository.getMoodAndMomentsData().lastOrNull()?.data
                        moodCache = mood
                        mood?.genres?.map {
                            browsableMediaItem(
                                "$GENRE/${it.params}",
                                it.title,
                                null,
                                drawableUri(R.drawable.baseline_sort_by_alpha_24),
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            )
                        } ?: emptyList()
                    }

                    PODCAST -> {
                        val podcasts = podcastRepository.getAllPodcasts(100).first()
                        podcasts.sortedBy { it.inLibrary }.map {
                            browsableMediaItem(
                                "$PODCAST/${it.podcastId}",
                                it.title,
                                it.authorName,
                                it.thumbnail?.toUri(),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                            )
                        }
                    }

                    else -> {
                        when {
                            parentId.startsWith("$HOME/") -> {
                                if (parentId.split("/").size == 2) {
                                    val homeItem =
                                        listHomeItem.find {
                                            it.title == parentId.split("/").getOrNull(1)
                                        }
                                    homeItem
                                        ?.contents
                                        ?.filter { it?.playlistId != null || it?.videoId != null }
                                        ?.mapNotNull {
                                            if (it?.playlistId != null) {
                                                browsableMediaItem(
                                                    id = "$HOME/${homeItem.title}/$PLAYLIST/${it.playlistId}",
                                                    title = it.title,
                                                    subtitle = it.description,
                                                    iconUri =
                                                        it.thumbnails
                                                            .lastOrNull()
                                                            ?.url
                                                            ?.toUri(),
                                                    mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                                )
                                            } else if (it?.videoId != null) {
                                                it
                                                    .toTrack()
                                                    .toSongEntity()
                                                    .toMediaItem("$HOME/${homeItem.title}/$SONG")
                                            } else {
                                                null
                                            }
                                        }
                                        ?: emptyList()
                                } else {
                                    val playlistId = parentId.split("/").getOrNull(3)
                                    if (playlistId != null) {
                                        val playlist =
                                            playlistRepository.getFullPlaylistData(playlistId).lastOrNull()
                                        if (playlist?.data?.tracks.isNullOrEmpty()) {
                                            emptyList()
                                        } else {
                                            playlist.data?.toPlaylistEntity()?.let { playlistRepository.insertAndReplacePlaylist(it) }
                                            playlist.data?.tracks?.map { track ->
                                                track
                                                    .toSongEntity()
                                                    .also {
                                                        songRepository.insertSong(it).first()
                                                    }.toMediaItem(parentId)
                                            } ?: emptyList()
                                        }
                                    } else {
                                        emptyList()
                                    }
                                }
                            }

                            parentId.startsWith("$CHART/") -> {
                                val parts = parentId.split("/")
                                if (parts.size == 2) {
                                    val categoryTitle = parts.getOrNull(1)
                                    val category = listChartItem.find { it.title == categoryTitle }
                                    category?.playlists?.map {
                                        browsableMediaItem(
                                            id = "$CHART/${category?.title}/$PLAYLIST/${it.id}",
                                            title = it.title,
                                            subtitle = it.author,
                                            iconUri = it.thumbnails.lastOrNull()?.url?.toUri(),
                                            mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                        )
                                    } ?: emptyList()
                                } else {
                                    val playlistId = parts.getOrNull(3)
                                    if (playlistId != null) {
                                        val playlist = playlistRepository.getFullPlaylistData(playlistId).lastOrNull()
                                        if (playlist?.data?.tracks.isNullOrEmpty()) {
                                            emptyList()
                                        } else {
                                            playlist.data?.toPlaylistEntity()?.let { playlistRepository.insertAndReplacePlaylist(it) }
                                            playlist.data?.tracks?.map { track ->
                                                track
                                                    .toSongEntity()
                                                    .also { songRepository.insertSong(it).first() }
                                                    .toMediaItem(parentId)
                                            } ?: emptyList()
                                        }
                                    } else emptyList()
                                }
                            }

                            parentId.startsWith("$MOOD/") -> {
                                val parts = parentId.split("/")
                                if (parts.size == 2) {
                                    val paramsMood = parts.getOrNull(1)
                                    val detail = paramsMood?.let {
                                        moodsMomentDetailsCache[it]
                                            ?: homeRepository.getMoodData(it).lastOrNull()?.data?.also { data ->
                                                moodsMomentDetailsCache[it] = data
                                            }
                                    }
                                    detail?.items
                                        ?.flatMap { it.contents }
                                        ?.map {
                                            browsableMediaItem(
                                                id = "$MOOD/${paramsMood}/$PLAYLIST/${it.playlistBrowseId}",
                                                title = it.title,
                                                subtitle = null,
                                                iconUri = it.thumbnails?.lastOrNull()?.url?.toUri(),
                                                mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                            )
                                        } ?: emptyList()
                                } else {
                                    val playlistId = parts.getOrNull(3)
                                    if (playlistId != null) {
                                        val playlist = playlistRepository.getFullPlaylistData(playlistId).lastOrNull()
                                        if (playlist?.data?.tracks.isNullOrEmpty()) {
                                            emptyList()
                                        } else {
                                            playlist.data?.toPlaylistEntity()?.let { playlistRepository.insertAndReplacePlaylist(it) }
                                            playlist.data?.tracks?.map { track ->
                                                track
                                                    .toSongEntity()
                                                    .also { songRepository.insertSong(it).first() }
                                                    .toMediaItem(parentId)
                                            } ?: emptyList()
                                        }
                                    } else emptyList()
                                }
                            }

                            parentId.startsWith("$GENRE/") -> {
                                val parts = parentId.split("/")
                                if (parts.size == 2) {
                                    val paramsGenre = parts.getOrNull(1)
                                    val detail = paramsGenre?.let {
                                        genreDetailsCache[it]
                                            ?: homeRepository.getGenreData(it).lastOrNull()?.data?.also { data ->
                                                genreDetailsCache[it] = data
                                            }
                                    }
                                    detail?.itemsPlaylist
                                        ?.flatMap { it.contents }
                                        ?.map {
                                            browsableMediaItem(
                                                id = "$GENRE/${paramsGenre}/$PLAYLIST/${it.playlistBrowseId}",
                                                title = it.title.title,
                                                subtitle = it.title.subtitle,
                                                iconUri = it.thumbnail?.lastOrNull()?.url?.toUri(),
                                                mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                            )
                                        } ?: emptyList()
                                } else {
                                    val playlistId = parts.getOrNull(3)
                                    if (playlistId != null) {
                                        val playlist = playlistRepository.getFullPlaylistData(playlistId).lastOrNull()
                                        if (playlist?.data?.tracks.isNullOrEmpty()) {
                                            emptyList()
                                        } else {
                                            playlist.data?.toPlaylistEntity()?.let { playlistRepository.insertAndReplacePlaylist(it) }
                                            playlist.data?.tracks?.map { track ->
                                                track
                                                    .toSongEntity()
                                                    .also { songRepository.insertSong(it).first() }
                                                    .toMediaItem(parentId)
                                            } ?: emptyList()
                                        }
                                    } else emptyList()
                                }
                            }

                            parentId.startsWith("$PODCAST/") -> {
                                val parts = parentId.split("/")
                                if (parts.size == 2) {
                                    val podcastId = parts.getOrNull(1)
                                    if (podcastId != null) {
                                        val episodes = podcastRepository.getPodcastEpisodes(podcastId).first()
                                        episodes.map { it.toDisplayMediaItem("$PODCAST/$podcastId/$EPISODE") }
                                    } else emptyList()
                                } else {
                                    emptyList()
                                }
                            }

                            else -> emptyList()
                        }
                    }
                },
                libraryParams,
            )
        }

    @UnstableApi
    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        scope.future(Dispatchers.IO) {
            songRepository.getSongById(mediaId).first()?.let {
                LibraryResult.ofItem(it.toMediaItem(), null)
            } ?: streamRepository.getFullMetadata(mediaId).lastOrNull()?.data?.let {
                LibraryResult.ofItem(it.toMediaItemWithoutPath(), null)
            } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
        }

    @UnstableApi
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
        scope.future {
            // Play from Android Auto
            val defaultResult =
                MediaSession.MediaItemsWithStartPosition(emptyList(), startIndex, startPositionMs)
            val path =
                mediaItems.firstOrNull()?.mediaId?.split("/")
                    ?: return@future defaultResult
            when (path.firstOrNull()) {
                SONG -> {
                    val songId = path.getOrNull(1) ?: return@future defaultResult
                    val allSongs = songRepository.getAllSongs(1000).lastOrNull()?.sortedBy { it.inLibrary } ?: emptyList()
                    if (allSongs.find { it.videoId == songId } == null) {
                        val song =
                            searchTempList.find { it.videoId == songId }
                                ?: streamRepository.getFullMetadata(songId).lastOrNull()?.data
                                ?: return@future defaultResult
                        songRepository.insertSong(song.toSongEntity()).lastOrNull()
                        val cloneList = allSongs.toMutableList()
                        cloneList.add(0, song.toSongEntity())
                        MediaSession.MediaItemsWithStartPosition(
                            cloneList.map { it.toMediaItem() },
                            cloneList.indexOfFirst { it.videoId == songId }.takeIf { it != -1 } ?: 0,
                            startPositionMs,
                        )
                    } else {
                        MediaSession.MediaItemsWithStartPosition(
                            allSongs.map { it.toMediaItem() },
                            allSongs.indexOfFirst { it.videoId == songId }.takeIf { it != -1 } ?: 0,
                            startPositionMs,
                        )
                    }
                }

                PLAYLIST -> {
                    val songId = path.getOrNull(2) ?: return@future defaultResult
                    val playlistId = path.getOrNull(1) ?: return@future defaultResult
                    Logger.d("SimpleMediaSessionCallback", "onSetMediaItems playlistId: $playlistId")
                    val songs =
                        localPlaylistRepository
                            .getLocalPlaylist(playlistId.toLong())
                            .lastOrNull()
                            ?.data
                            ?.tracks
                            ?.let {
                                songRepository.getSongsByListVideoId(it)
                            }?.lastOrNull()
                    Logger.w("SimpleMediaSessionCallback", "onSetMediaItems songs: $songs")
                    if (songs.isNullOrEmpty()) {
                        defaultResult
                    } else {
                        MediaSession.MediaItemsWithStartPosition(
                            songs.map { it.toMediaItem() },
                            songs.indexOfFirst { it.videoId == songId }.takeIf { it != -1 } ?: 0,
                            startPositionMs,
                        )
                    }
                }

                HOME -> {
                    val type = path.getOrNull(2) ?: return@future defaultResult
                    val content = listHomeItem.find { it.title == path.getOrNull(1) }?.contents
                    if (type == SONG) {
                        val songId = path.getOrNull(3) ?: return@future defaultResult
                        val songs =
                            content?.filter { it?.videoId != null }?.mapNotNull {
                                it?.toTrack()
                            }
                        if (songs.isNullOrEmpty()) {
                            defaultResult
                        } else {
                            songs.forEach {
                                songRepository.insertSong(it.toSongEntity()).first()
                            }
                            MediaSession.MediaItemsWithStartPosition(
                                songs.map { it.toMediaItem() },
                                songs.indexOfFirst { it.videoId == songId }.takeIf { it != -1 } ?: 0,
                                startPositionMs,
                            )
                        }
                    } else if (type == PLAYLIST) {
                        val songId = path.getOrNull(4) ?: return@future defaultResult
                        val playlistId = path.getOrNull(3) ?: return@future defaultResult
                        Logger.d(TAG, "onSetMediaItems playlistId: $playlistId")
                        val playlistEntity = playlistRepository.getPlaylist(playlistId).first()
                        Logger.w(TAG, "onSetMediaItems playlistEntity: $playlistEntity")
                        val tracks = playlistEntity?.tracks
                        if (tracks.isNullOrEmpty()) {
                            defaultResult
                        } else if (tracks.isNotEmpty()) {
                            tracks
                                .let {
                                    songRepository
                                        .getSongsByListVideoId(tracks)
                                        .first()
                                        .sortedBy {
                                            tracks.indexOf(it.videoId)
                                        }.also {
                                            Logger.w(TAG, "onSetMediaItems list songs: $it")
                                        }.map { it.toMediaItem() }
                                }.let { mediaItemList ->
                                    MediaSession.MediaItemsWithStartPosition(
                                        mediaItemList,
                                        mediaItemList
                                            .indexOfFirst { it.mediaId == songId }
                                            .takeIf { it != -1 } ?: 0,
                                        startPositionMs,
                                    )
                                }
                        } else {
                            defaultResult
                        }
                    } else {
                        return@future defaultResult
                    }
                }

                ONLINE_PLAYLIST -> {
                    val playlistId = path.getOrNull(1) ?: return@future defaultResult
                    val playlistEntity = playlistRepository.getPlaylist(playlistId).first()
                    val tracks = playlistEntity?.tracks
                    val songs = if (tracks.isNullOrEmpty()) {
                        val full = playlistRepository.getFullPlaylistData(playlistId).lastOrNull()
                        if (full?.data?.tracks.isNullOrEmpty()) {
                            emptyList()
                        } else {
                            full.data?.toPlaylistEntity()?.let { playlistRepository.insertAndReplacePlaylist(it) }
                            full.data?.tracks?.onEach { songRepository.insertSong(it.toSongEntity()).first() }?.map { it.toMediaItem() }
                                ?: emptyList()
                        }
                    } else {
                        songRepository.getSongsByListVideoId(tracks).first().sortedBy { tracks.indexOf(it.videoId) }.map { it.toMediaItem() }
                    }
                    if (songs.isEmpty()) defaultResult else MediaSession.MediaItemsWithStartPosition(songs, 0, startPositionMs)
                }

                ALBUM -> {
                    val albumId = path.getOrNull(1) ?: return@future defaultResult
                    val album = albumRepository.getAlbumData(albumId).lastOrNull()?.data
                    val tracks = album?.tracks
                    if (tracks.isNullOrEmpty()) {
                        defaultResult
                    } else {
                        tracks.forEach { songRepository.insertSong(it.toSongEntity()).first() }
                        val mediaItemList = tracks.map { it.toMediaItem() }
                        MediaSession.MediaItemsWithStartPosition(mediaItemList, 0, startPositionMs)
                    }
                }

                ARTIST -> {
                    // artist/<channelId>/radio/<radioId>
                    val radioId = path.getOrNull(3) ?: return@future defaultResult
                    val radio = playlistRepository.getRadio(radioId).lastOrNull()?.data?.first
                    val tracks = radio?.tracks
                    if (tracks.isNullOrEmpty()) {
                        defaultResult
                    } else {
                        tracks.forEach { songRepository.insertSong(it.toSongEntity()).first() }
                        val mediaItemList = tracks.map { it.toMediaItem() }
                        MediaSession.MediaItemsWithStartPosition(mediaItemList, 0, startPositionMs)
                    }
                }

                CHART -> {
                    val type = path.getOrNull(2) ?: return@future defaultResult
                    if (type == PLAYLIST) {
                        val songId = path.getOrNull(4) ?: return@future defaultResult
                        val playlistId = path.getOrNull(3) ?: return@future defaultResult
                        val playlistEntity = playlistRepository.getPlaylist(playlistId).first()
                        val tracks = playlistEntity?.tracks
                        if (tracks.isNullOrEmpty()) {
                            defaultResult
                        } else {
                            val mediaItemList =
                                songRepository
                                    .getSongsByListVideoId(tracks)
                                    .first()
                                    .sortedBy { tracks.indexOf(it.videoId) }
                                    .map { it.toMediaItem() }
                            MediaSession.MediaItemsWithStartPosition(
                                mediaItemList,
                                mediaItemList.indexOfFirst { it.mediaId == songId }.takeIf { it != -1 } ?: 0,
                                startPositionMs,
                            )
                        }
                    } else defaultResult
                }

                MOOD -> {
                    val type = path.getOrNull(2) ?: return@future defaultResult
                    if (type == PLAYLIST) {
                        val songId = path.getOrNull(4) ?: return@future defaultResult
                        val playlistId = path.getOrNull(3) ?: return@future defaultResult
                        val playlistEntity = playlistRepository.getPlaylist(playlistId).first()
                        val tracks = playlistEntity?.tracks
                        if (tracks.isNullOrEmpty()) {
                            defaultResult
                        } else {
                            val mediaItemList =
                                songRepository
                                    .getSongsByListVideoId(tracks)
                                    .first()
                                    .sortedBy { tracks.indexOf(it.videoId) }
                                    .map { it.toMediaItem() }
                            MediaSession.MediaItemsWithStartPosition(
                                mediaItemList,
                                mediaItemList.indexOfFirst { it.mediaId == songId }.takeIf { it != -1 } ?: 0,
                                startPositionMs,
                            )
                        }
                    } else defaultResult
                }

                GENRE -> {
                    val type = path.getOrNull(2) ?: return@future defaultResult
                    if (type == PLAYLIST) {
                        val songId = path.getOrNull(4) ?: return@future defaultResult
                        val playlistId = path.getOrNull(3) ?: return@future defaultResult
                        val playlistEntity = playlistRepository.getPlaylist(playlistId).first()
                        val tracks = playlistEntity?.tracks
                        if (tracks.isNullOrEmpty()) {
                            defaultResult
                        } else {
                            val mediaItemList =
                                songRepository
                                    .getSongsByListVideoId(tracks)
                                    .first()
                                    .sortedBy { tracks.indexOf(it.videoId) }
                                    .map { it.toMediaItem() }
                            MediaSession.MediaItemsWithStartPosition(
                                mediaItemList,
                                mediaItemList.indexOfFirst { it.mediaId == songId }.takeIf { it != -1 } ?: 0,
                                startPositionMs,
                            )
                        }
                    } else defaultResult
                }

                PODCAST -> {
                    val podcastId = path.getOrNull(1) ?: return@future defaultResult
                    val type = path.getOrNull(2)
                    if (type == EPISODE) {
                        val videoId = path.getOrNull(3) ?: return@future defaultResult
                        val episodes = podcastRepository.getPodcastEpisodes(podcastId).first()
                        val mediaItemList = episodes.map { it.toPlaybackMediaItem() }
                        MediaSession.MediaItemsWithStartPosition(
                            mediaItemList,
                            mediaItemList.indexOfFirst { it.mediaId == videoId }.takeIf { it != -1 } ?: 0,
                            startPositionMs,
                        )
                    } else {
                        // Play podcast episodes from network if not cached locally
                        val episodesDb = podcastRepository.getPodcastEpisodes(podcastId).first()
                        val mediaItemList = if (episodesDb.isNotEmpty()) {
                            episodesDb.map { it.toPlaybackMediaItem() }
                        } else {
                            val pb = podcastRepository.getPodcastData(podcastId).lastOrNull()?.data
                            pb?.listEpisode?.map { it.toPlaybackMediaItem() } ?: emptyList()
                        }
                        if (mediaItemList.isEmpty()) defaultResult else MediaSession.MediaItemsWithStartPosition(mediaItemList, 0, startPositionMs)
                    }
                }

                else -> defaultResult
            }
        }

    private fun drawableUri(
        @DrawableRes id: Int,
    ) = Uri
        .Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.resources.getResourcePackageName(id))
        .appendPath(context.resources.getResourceTypeName(id))
        .appendPath(context.resources.getResourceEntryName(id))
        .build()

    private fun browsableMediaItem(
        id: String,
        title: String,
        subtitle: String?,
        iconUri: Uri?,
        mediaType: Int = MediaMetadata.MEDIA_TYPE_MUSIC,
    ) = MediaItem
        .Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtist(subtitle)
                .setArtworkUri(iconUri)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(mediaType)
                .build(),
        ).build()

    private fun SongEntity.toMediaItem(path: String) =
        MediaItem
            .Builder()
            .setMediaId("$path/${this.videoId}")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(this.title)
                    .setSubtitle(this.artistName?.joinToString(", "))
                    .setArtist(this.artistName?.joinToString(" "))
                    .setArtworkUri(this.thumbnails?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build(),
            ).build()

    private fun Track.toMediaItemWithoutPath(path: String? = SONG) =
        MediaItem
            .Builder()
            .setMediaId(if (path == null) this.videoId else "$path/${this.videoId}")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(this.title)
                    .setSubtitle(this.artists?.toListName()?.connectArtists())
                    .setArtist(this.artists?.toListName()?.connectArtists())
                    .setArtworkUri(
                        this.thumbnails
                            ?.lastOrNull()
                            ?.url
                            ?.toUri(),
                    ).setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build(),
            ).build()

    private fun PlaylistsResult.toMediaItemSearchPlaylist(): MediaItem =
        MediaItem
            .Builder()
            .setMediaId("$ONLINE_PLAYLIST/${this.browseId}")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(this.title)
                    .setSubtitle(this.author)
                    .setArtist(this.author)
                    .setArtworkUri(this.thumbnails.lastOrNull()?.url?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    .build(),
            ).build()

    private fun AlbumsResult.toMediaItemSearchAlbum(): MediaItem =
        MediaItem
            .Builder()
            .setMediaId("$ALBUM/${this.browseId}")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(this.title)
                    .setSubtitle(this.artists.toListName().connectArtists())
                    .setArtist(this.artists.toListName().connectArtists())
                    .setArtworkUri(this.thumbnails.lastOrNull()?.url?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    .build(),
            ).build()

    private fun ArtistsResult.toMediaItemSearchArtist(): MediaItem =
        MediaItem
            .Builder()
            .setMediaId("$ARTIST/${this.browseId}/radio/${this.radioId}")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(this.artist)
                    .setSubtitle(null)
                    .setArtist(this.artist)
                    .setArtworkUri(this.thumbnails.lastOrNull()?.url?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build(),
            ).build()

    private fun PlaylistsResult.toMediaItemSearchPodcast(): MediaItem =
        MediaItem
            .Builder()
            .setMediaId("$PODCAST/${this.browseId}")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(this.title)
                    .setSubtitle(this.author)
                    .setArtist(this.author)
                    .setArtworkUri(this.thumbnails.lastOrNull()?.url?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    .build(),
            ).build()

    private fun PodcastBrowse.EpisodeItem.toPlaybackMediaItem(): MediaItem =
        MediaItem
            .Builder()
            .setMediaId(this.videoId)
            .setUri(this.videoId)
            .setCustomCacheKey(this.videoId)
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(this.title)
                    .setSubtitle(this.author.name)
                    .setArtist(this.author.name)
                    .setArtworkUri(this.thumbnail.lastOrNull()?.url?.toUri())
                    .setDescription(MERGING_DATA_TYPE.VIDEO)
                    .build(),
            ).build()

    private fun EpisodeEntity.toDisplayMediaItem(path: String) =
        MediaItem
            .Builder()
            .setMediaId("$path/${this.videoId}")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(this.title)
                    .setSubtitle(this.authorName)
                    .setArtist(this.authorName)
                    .setArtworkUri(this.thumbnail?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build(),
            ).build()

    private fun EpisodeEntity.toPlaybackMediaItem(): MediaItem =
        MediaItem
            .Builder()
            .setMediaId(this.videoId)
            .setUri(this.videoId)
            .setCustomCacheKey(this.videoId)
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(this.title)
                    .setSubtitle(this.authorName)
                    .setArtist(this.authorName)
                    .setArtworkUri(this.thumbnail?.toUri())
                    .setDescription(MERGING_DATA_TYPE.VIDEO)
                    .build(),
            ).build()

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val HOME = "home"
        const val ONLINE_PLAYLIST = "online_playlist"
        const val PLAYLIST = "playlist"
        const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
        const val CHART = "chart"
        const val MOOD = "mood"
        const val GENRE = "genre"
        const val PODCAST = "podcast"
        const val EPISODE = "episode"
        const val ALBUM = "album"
        const val ARTIST = "artist"
    }
}