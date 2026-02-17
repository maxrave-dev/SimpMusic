package com.maxrave.simpmusic.wear.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.wearable.Wearable
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AccountRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.simpmusic.wear.auth.WearAccountManager
import com.maxrave.simpmusic.wear.ui.screens.AccountsScreen
import com.maxrave.simpmusic.wear.ui.screens.AlbumBrowseScreen
import com.maxrave.simpmusic.wear.ui.screens.ArtistBrowseScreen
import com.maxrave.simpmusic.wear.ui.screens.DiscoverScreen
import com.maxrave.simpmusic.wear.ui.screens.DownloadsScreen
import com.maxrave.simpmusic.wear.ui.screens.FollowedArtistsScreen
import com.maxrave.simpmusic.wear.ui.screens.HomeScreen
import com.maxrave.simpmusic.wear.ui.screens.LibraryScreen
import com.maxrave.simpmusic.wear.ui.screens.LikedSongsScreen
import com.maxrave.simpmusic.wear.ui.screens.LoginScreen
import com.maxrave.simpmusic.wear.ui.screens.NowPlayingScreen
import com.maxrave.simpmusic.wear.ui.screens.OnlinePlaylistsScreen
import com.maxrave.simpmusic.wear.ui.screens.PlaylistScreen
import com.maxrave.simpmusic.wear.ui.screens.QueueScreen
import com.maxrave.simpmusic.wear.ui.screens.RecentlyPlayedScreen
import com.maxrave.simpmusic.wear.ui.screens.SearchScreen
import com.maxrave.simpmusic.wear.ui.screens.VolumeScreen
import com.maxrave.simpmusic.wear.ui.screens.YtPlaylistScreen
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

private object Routes {
    const val DISCOVER = "discover"
    const val HOME = "home"
    const val SEARCH = "search"
    const val PLAYLISTS = "playlists"
    const val DOWNLOADS = "downloads"
    const val LIKED_SONGS = "liked_songs"
    const val RECENTLY_PLAYED = "recently_played"
    const val FOLLOWED_ARTISTS = "followed_artists"
    const val NOW_PLAYING = "now_playing"
    const val QUEUE = "queue"
    const val LIBRARY = "library"
    const val PLAYLIST = "playlist"
    const val YT_PLAYLIST = "yt_playlist"
    const val ALBUM = "album"
    const val ARTIST = "artist"
    const val VOLUME = "volume"
    const val ACCOUNTS = "accounts"
    const val LOGIN = "login"
}

private const val PATH_SYNC_LOGIN_FROM_PHONE = "/simpmusic/login/sync"
private fun encodeNavArg(value: String): String = Uri.encode(value)
private fun decodeNavArg(value: String?): String? = value?.let(Uri::decode)

@Composable
fun WearAppRoot(mediaPlayerHandler: MediaPlayerHandler) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val accountRepository: AccountRepository = remember { GlobalContext.get().get() }
    val commonRepository: CommonRepository = remember { GlobalContext.get().get() }
    val dataStoreManager: DataStoreManager = remember { GlobalContext.get().get() }
    val loggedIn by dataStoreManager.loggedIn.collectAsStateWithLifecycle(initialValue = DataStoreManager.FALSE)
    var startupGateOpen by remember { mutableStateOf(false) }
    val didAutoSyncAttempt = remember { AtomicBoolean(false) }

    LaunchedEffect(Unit) {
        // Short startup gate so account bootstrap can finish before first data queries.
        delay(1600L)
        startupGateOpen = true
    }

    LaunchedEffect(Unit) {
        runCatching {
            WearAccountManager(
                context = context.applicationContext,
                dataStoreManager = dataStoreManager,
                accountRepository = accountRepository,
                commonRepository = commonRepository,
            ).bootstrapSessionAtStartup()
        }
    }

    // Best-effort: if the user is not signed in locally, request sync from paired phone once.
    LaunchedEffect(loggedIn, startupGateOpen) {
        if (!startupGateOpen) return@LaunchedEffect
        if (loggedIn == DataStoreManager.TRUE) return@LaunchedEffect
        if (didAutoSyncAttempt.getAndSet(true)) return@LaunchedEffect

        // Give startup restore a brief moment to update DataStore before asking the phone.
        delay(300L)

        val appCtx = context.applicationContext
        val nodeClient = Wearable.getNodeClient(appCtx)
        val messageClient = Wearable.getMessageClient(appCtx)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, PATH_SYNC_LOGIN_FROM_PHONE, ByteArray(0))
                }
            }
    }

    if (!startupGateOpen) {
        AppScaffold(timeText = { TimeText() }) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Text("Loading session...")
            }
        }
        return
    }

    AppScaffold(
        timeText = { TimeText() },
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.DISCOVER,
        ) {
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    mediaPlayerHandler = mediaPlayerHandler,
                    openSearch = { navController.navigate(Routes.SEARCH) },
                    openPlaylistsDirectory = { navController.navigate(Routes.PLAYLISTS) },
                    openDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    openQueue = { navController.navigate(Routes.QUEUE) },
                    openLibrary = { navController.navigate(Routes.LIBRARY) },
                    openAccounts = { navController.navigate(Routes.ACCOUNTS) },
                    openYtPlaylist = { id -> navController.navigate("${Routes.YT_PLAYLIST}/${encodeNavArg(id)}") },
                    openAlbum = { id -> navController.navigate("${Routes.ALBUM}/${encodeNavArg(id)}") },
                    openArtist = { id -> navController.navigate("${Routes.ARTIST}/${encodeNavArg(id)}") },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    mediaPlayerHandler = mediaPlayerHandler,
                    openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    openQueue = { navController.navigate(Routes.QUEUE) },
                    openLibrary = { navController.navigate(Routes.LIBRARY) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    mediaPlayerHandler = mediaPlayerHandler,
                    onBack = { navController.popBackStack() },
                    openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    openPlaylistDirectory = { navController.navigate(Routes.PLAYLISTS) },
                    openPlaylist = { id -> navController.navigate("${Routes.YT_PLAYLIST}/${encodeNavArg(id)}") },
                    openAlbum = { id -> navController.navigate("${Routes.ALBUM}/${encodeNavArg(id)}") },
                    openArtist = { id -> navController.navigate("${Routes.ARTIST}/${encodeNavArg(id)}") },
                )
            }
            composable(Routes.PLAYLISTS) {
                OnlinePlaylistsScreen(
                    onBack = { navController.popBackStack() },
                    openPlaylist = { id -> navController.navigate("${Routes.YT_PLAYLIST}/${encodeNavArg(id)}") },
                )
            }
            composable(Routes.DOWNLOADS) {
                DownloadsScreen(
                    mediaPlayerHandler = mediaPlayerHandler,
                    onBack = { navController.popBackStack() },
                    openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                )
            }
            composable(Routes.NOW_PLAYING) {
                NowPlayingScreen(
                    mediaPlayerHandler = mediaPlayerHandler,
                    onBack = { navController.popBackStack() },
                    onOpenVolumeSettings = { navController.navigate(Routes.VOLUME) },
                )
            }
            composable(Routes.VOLUME) {
                VolumeScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.QUEUE) {
                QueueScreen(
                    mediaPlayerHandler = mediaPlayerHandler,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    mediaPlayerHandler = mediaPlayerHandler,
                    onBack = { navController.popBackStack() },
                    openPlaylist = { id -> navController.navigate("${Routes.PLAYLIST}/${encodeNavArg(id.toString())}") },
                    openDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    openSearch = { navController.navigate(Routes.SEARCH) },
                    openOnlinePlaylists = { navController.navigate(Routes.PLAYLISTS) },
                    openLikedSongs = { navController.navigate(Routes.LIKED_SONGS) },
                    openRecentPlays = { navController.navigate(Routes.RECENTLY_PLAYED) },
                    openFollowedArtists = { navController.navigate(Routes.FOLLOWED_ARTISTS) },
                )
            }
            composable(Routes.LIKED_SONGS) {
                LikedSongsScreen(
                    mediaPlayerHandler = mediaPlayerHandler,
                    onBack = { navController.popBackStack() },
                    openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    openArtist = { artistId -> navController.navigate("${Routes.ARTIST}/${encodeNavArg(artistId)}") },
                )
            }
            composable(Routes.RECENTLY_PLAYED) {
                RecentlyPlayedScreen(
                    mediaPlayerHandler = mediaPlayerHandler,
                    onBack = { navController.popBackStack() },
                    openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    openArtist = { artistId -> navController.navigate("${Routes.ARTIST}/${encodeNavArg(artistId)}") },
                )
            }
            composable(Routes.FOLLOWED_ARTISTS) {
                FollowedArtistsScreen(
                    onBack = { navController.popBackStack() },
                    openArtist = { artistId -> navController.navigate("${Routes.ARTIST}/${encodeNavArg(artistId)}") },
                )
            }
            composable("${Routes.PLAYLIST}/{id}") { entry ->
                val id = decodeNavArg(entry.arguments?.getString("id"))?.toLongOrNull()
                if (id != null) {
                    PlaylistScreen(
                        playlistId = id,
                        mediaPlayerHandler = mediaPlayerHandler,
                        onBack = { navController.popBackStack() },
                        openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    )
                }
            }
            composable("${Routes.YT_PLAYLIST}/{id}") { entry ->
                val id = decodeNavArg(entry.arguments?.getString("id"))
                if (!id.isNullOrBlank()) {
                    YtPlaylistScreen(
                        playlistId = id,
                        mediaPlayerHandler = mediaPlayerHandler,
                        onBack = { navController.popBackStack() },
                        openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    )
                }
            }
            composable("${Routes.ALBUM}/{id}") { entry ->
                val id = decodeNavArg(entry.arguments?.getString("id"))
                if (!id.isNullOrBlank()) {
                    AlbumBrowseScreen(
                        browseId = id,
                        mediaPlayerHandler = mediaPlayerHandler,
                        onBack = { navController.popBackStack() },
                        openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                        openAlbum = { albumId -> navController.navigate("${Routes.ALBUM}/${encodeNavArg(albumId)}") },
                        openArtist = { artistId -> navController.navigate("${Routes.ARTIST}/${encodeNavArg(artistId)}") },
                    )
                }
            }
            composable("${Routes.ARTIST}/{id}") { entry ->
                val id = decodeNavArg(entry.arguments?.getString("id"))
                if (!id.isNullOrBlank()) {
                    ArtistBrowseScreen(
                        channelId = id,
                        mediaPlayerHandler = mediaPlayerHandler,
                        onBack = { navController.popBackStack() },
                        openNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                        openAlbum = { albumId -> navController.navigate("${Routes.ALBUM}/${encodeNavArg(albumId)}") },
                        openArtist = { artistId -> navController.navigate("${Routes.ARTIST}/${encodeNavArg(artistId)}") },
                        openPlaylist = { playlistId -> navController.navigate("${Routes.YT_PLAYLIST}/${encodeNavArg(playlistId)}") },
                    )
                }
            }
            composable(Routes.ACCOUNTS) {
                AccountsScreen(
                    onBack = { navController.popBackStack() },
                    openLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
