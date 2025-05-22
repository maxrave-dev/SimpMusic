package com.maxrave.simpmusic.service

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.MEDIA_CUSTOM_COMMAND
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.home.HomeItem
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toListTrack
import com.maxrave.simpmusic.extension.toMediaItem
import com.maxrave.simpmusic.extension.toPlaylistEntity
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future

class SimpleMediaSessionCallback(
    private val context: Context,
    private val mainRepository: MainRepository,
) : MediaLibrarySession.Callback {
    private val tag = "AndroidAuto"
    var toggleLike: () -> Unit = {}
    var toggleRadio: () -> Unit = {}
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val searchTempList = mutableListOf<Track>()
    private val listHomeItem = mutableListOf<HomeItem>()

    init {
        if (!mainRepository.init) {
            mainRepository.initYouTube(scope)
        }
    }

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
            val searchResult =
                mainRepository.getSearchDataSong(query).first().let { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            resource.data?.let {
                                searchTempList.clear()
                                searchTempList.addAll(it.toListTrack())
                            }
                            resource.data
                        }

                        else -> emptyList()
                    }
                }
            if (searchResult != null) {
                session.notifySearchResultChanged(browser, query, searchResult.size, params)
            }
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
            LibraryResult.ofItemList(
                searchTempList.map {
                    it.toMediaItemWithoutPath()
                },
                params,
            )
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
                        )

                    SONG ->
                        mainRepository
                            .getAllSongs()
                            .first()
                            .sortedBy { it.inLibrary }
                            .map { it.toMediaItem(parentId) }

                    PLAYLIST ->
                        mainRepository
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
                        val temp = mainRepository.getHomeData().first().data
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
                                            mainRepository.getFullPlaylistData(playlistId).first()
                                        if (playlist.data?.tracks.isNullOrEmpty()) {
                                            emptyList()
                                        } else {
                                            mainRepository.insertAndReplacePlaylist(playlist.data.toPlaylistEntity())
                                            playlist.data.tracks.map { track ->
                                                track
                                                    .toSongEntity()
                                                    .also {
                                                        mainRepository.insertSong(it).first()
                                                    }.toMediaItem(parentId)
                                            }
                                        }
                                    } else {
                                        emptyList()
                                    }
                                }
                            }

                            parentId.startsWith("$PLAYLIST/") -> {
                                val playlistId = parentId.split("/").getOrNull(1)
                                if (playlistId != null) {
                                    val playlist =
                                        mainRepository.getLocalPlaylist(playlistId.toLong()).first()
                                    if (playlist != null) {
                                        Log.w(tag, "onGetChildren: $playlist")
                                        if (playlist.tracks.isNullOrEmpty()) {
                                            emptyList()
                                        } else {
                                            mainRepository
                                                .getSongsByListVideoId(playlist.tracks)
                                                .first()
                                                .map { it.toMediaItem(parentId) }
                                        }
                                    } else {
                                        emptyList()
                                    }
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
            mainRepository.getSongById(mediaId).first()?.let {
                LibraryResult.ofItem(it.toMediaItem(), null)
            } ?: mainRepository.getFullMetadata(mediaId).first()?.let {
                LibraryResult.ofItem(it.toTrack().toMediaItemWithoutPath(), null)
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
                    val allSongs = mainRepository.getAllSongs().first().sortedBy { it.inLibrary }
                    if (allSongs.find { it.videoId == songId } == null) {
                        val song =
                            searchTempList.find { it.videoId == songId }
                                ?: mainRepository.getFullMetadata(songId).first()?.toTrack()
                                ?: return@future defaultResult
                        mainRepository.insertSong(song.toSongEntity()).first()
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
                    Log.d("SimpleMediaSessionCallback", "onSetMediaItems playlistId: $playlistId")
                    val songs =
                        mainRepository
                            .getLocalPlaylist(playlistId.toLong())
                            .first()
                            ?.tracks
                            ?.let {
                                mainRepository.getSongsByListVideoId(it)
                            }?.first()
                    Log.w("SimpleMediaSessionCallback", "onSetMediaItems songs: $songs")
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
                                mainRepository.insertSong(it.toSongEntity()).first()
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
                        Log.d(tag, "onSetMediaItems playlistId: $playlistId")
                        val playlistEntity = mainRepository.getPlaylist(playlistId).first()
                        Log.w(tag, "onSetMediaItems playlistEntity: $playlistEntity")
                        if (playlistEntity?.tracks.isNullOrEmpty()) {
                            defaultResult
                        } else if (playlistEntity?.tracks?.isNotEmpty() == true) {
                            playlistEntity.tracks
                                .let { tracks ->
                                    mainRepository
                                        .getSongsByListVideoId(tracks)
                                        .first()
                                        .sortedBy {
                                            tracks.indexOf(it.videoId)
                                        }.also {
                                            Log.w(tag, "onSetMediaItems list songs: $it")
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

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val HOME = "home"
        const val ONLINE_PLAYLIST = "online_playlist"
        const val PLAYLIST = "playlist"
        const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
    }
}