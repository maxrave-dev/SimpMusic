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
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.MEDIA_CUSTOM_COMMAND
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.toMediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import javax.inject.Inject

class SimpleMediaSessionCallback @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mainRepository: MainRepository,
) : MediaLibrarySession.Callback {
    var toggleLike: () -> Unit = {}
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        val sessionCommands =
            connectionResult.availableSessionCommands
                .buildUpon()
                // Add custom commands
                .add(SessionCommand(MEDIA_CUSTOM_COMMAND.LIKE, Bundle()))
                .add(SessionCommand(MEDIA_CUSTOM_COMMAND.REPEAT, Bundle()))
                .build()
        return MediaSession.ConnectionResult.accept(
            sessionCommands, connectionResult.availablePlayerCommands
        )
    }

    @UnstableApi
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            MEDIA_CUSTOM_COMMAND.LIKE -> {
                toggleLike()
            }
            MEDIA_CUSTOM_COMMAND.REPEAT -> {
                session.player.repeatMode = when (session.player.repeatMode) {
                    ExoPlayer.REPEAT_MODE_OFF -> ExoPlayer.REPEAT_MODE_ONE
                    ExoPlayer.REPEAT_MODE_ONE -> ExoPlayer.REPEAT_MODE_ALL
                    ExoPlayer.REPEAT_MODE_ALL -> ExoPlayer.REPEAT_MODE_OFF
                    else -> ExoPlayer.REPEAT_MODE_OFF
                }
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
        LibraryResult.ofItem(
            MediaItem.Builder()
                .setMediaId(ROOT)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsPlayable(false)
                        .setIsBrowsable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build(),
            params
        )
    )

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future(Dispatchers.IO) {
        LibraryResult.ofItemList(
            when (parentId) {
                ROOT -> listOf(
                    browsableMediaItem(
                        SONG,
                        context.getString(R.string.songs),
                        null,
                        drawableUri(R.drawable.baseline_album_24),
                        MediaMetadata.MEDIA_TYPE_PLAYLIST
                    ),
                    browsableMediaItem(
                        PLAYLIST,
                        context.getString(R.string.playlists),
                        null,
                        drawableUri(R.drawable.baseline_playlist_add_24),
                        MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS
                    )
                )

                SONG -> mainRepository.getAllSongs().first().sortedBy { it.inLibrary }
                    .map { it.toMediaItem(parentId) }


                PLAYLIST -> mainRepository.getAllLocalPlaylists().first().sortedBy { it.inLibrary }
                    .map {
                        browsableMediaItem(
                            "$PLAYLIST/${it.id}",
                            it.title,
                            "${it.tracks?.size ?: 0} ${context.getString(R.string.track)}",
                            it.thumbnail?.toUri(),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST
                        )
                    }

                else -> {
                    when {
                        parentId.startsWith("$PLAYLIST/") -> {
                            val playlistId = parentId.split("/").getOrNull(1)
                            if (playlistId != null) {
                                val playlist =
                                    mainRepository.getLocalPlaylist(playlistId.toLong()).first()
                                Log.w("SimpleMediaSessionCallback", "onGetChildren: $playlist")
                                if (playlist.tracks.isNullOrEmpty()) {
                                    emptyList()
                                } else {
                                    mainRepository.getSongsByListVideoId(playlist.tracks).first()
                                        .map { it.toMediaItem(parentId) }
                                }
                            } else {
                                emptyList()
                            }
                        }

                        else -> emptyList()
                    }
                }

            },
            params
        )
    }

    @UnstableApi
    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> = scope.future(Dispatchers.IO) {
        mainRepository.getSongById(mediaId).first()?.toMediaItem()?.let {
            LibraryResult.ofItem(it, null)
        } ?: LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN)
    }

    @UnstableApi
    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
        // Play from Android Auto
        val defaultResult =
            MediaSession.MediaItemsWithStartPosition(emptyList(), startIndex, startPositionMs)
        val path = mediaItems.firstOrNull()?.mediaId?.split("/")
            ?: return@future defaultResult
        when (path.firstOrNull()) {
            SONG -> {
                val songId = path.getOrNull(1) ?: return@future defaultResult
                val allSongs = mainRepository.getAllSongs().first().sortedBy { it.inLibrary }
                MediaSession.MediaItemsWithStartPosition(
                    allSongs.map { it.toMediaItem() },
                    allSongs.indexOfFirst { it.videoId == songId }.takeIf { it != -1 } ?: 0,
                    startPositionMs
                )
            }

            PLAYLIST -> {
                val songId = path.getOrNull(2) ?: return@future defaultResult
                val playlistId = path.getOrNull(1) ?: return@future defaultResult
                Log.d("SimpleMediaSessionCallback", "onSetMediaItems: $playlistId")
                val songs =
                    mainRepository.getLocalPlaylist(playlistId.toLong()).first().tracks?.let {
                        mainRepository.getSongsByListVideoId(it)
                    }?.first()
                Log.w("SimpleMediaSessionCallback", "onSetMediaItems: $songs")
                if (songs.isNullOrEmpty()) {
                    defaultResult
                } else {
                    MediaSession.MediaItemsWithStartPosition(
                        songs.map { it.toMediaItem() },
                        songs.indexOfFirst { it.videoId == songId }.takeIf { it != -1 } ?: 0,
                        startPositionMs
                    )
                }
            }

            else -> defaultResult
        }
    }

    private fun drawableUri(@DrawableRes id: Int) = Uri.Builder()
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
        mediaType: Int = MediaMetadata.MEDIA_TYPE_MUSIC
    ) =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setArtist(subtitle)
                    .setArtworkUri(iconUri)
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(mediaType)
                    .build()
            )
            .build()

    private fun SongEntity.toMediaItem(path: String) =
        MediaItem.Builder()
            .setMediaId("$path/${this.videoId}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(this.title)
                    .setSubtitle(this.artistName?.joinToString(", "))
                    .setArtist(this.artistName?.joinToString(" "))
                    .setArtworkUri(this.thumbnails?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val PLAYLIST = "playlist"
    }
}