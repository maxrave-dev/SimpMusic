package com.maxrave.simpmusic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import coil3.toUri
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.Orientation
import com.maxrave.simpmusic.expect.currentOrientation
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.ui.component.AppBottomNavigationBar
import com.maxrave.simpmusic.ui.component.AppNavigationRail
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.player.FullscreenDestination
import com.maxrave.simpmusic.ui.navigation.graph.AppNavigationGraph
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.screen.player.NowPlayingScreen
import com.maxrave.simpmusic.ui.screen.player.NowPlayingScreenContent
import com.maxrave.simpmusic.ui.screen.splash.SplashScreen
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.do_not_show_again
import simpmusic.composeapp.generated.resources.good_night
import simpmusic.composeapp.generated.resources.notification
import simpmusic.composeapp.generated.resources.sleep_timer_off
import simpmusic.composeapp.generated.resources.this_app_needs_to_access_your_notification
import simpmusic.composeapp.generated.resources.this_link_is_not_supported
import simpmusic.composeapp.generated.resources.yes
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun App(
    viewModel: SharedViewModel = koinInject(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val navController = rememberNavController()

    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    val intent by viewModel.intent.collectAsStateWithLifecycle()
    val showNotificationPermissionDialog by viewModel.showNotificationPermissionDialog.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable { mutableStateOf(true) }

    var isShowMiniPlayer by rememberSaveable {
        mutableStateOf(true)
    }

    var isShowNowPlaylistScreen by rememberSaveable {
        mutableStateOf(false)
    }

    var isInFullscreen by rememberSaveable {
        mutableStateOf(false)
    }

    var isNavBarVisible by rememberSaveable {
        mutableStateOf(true)
    }

    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    val followSystemTheme by settingsViewModel.followSystemTheme.collectAsStateWithLifecycle()
    val forceLightTheme by settingsViewModel.forceLightTheme.collectAsStateWithLifecycle()

    LaunchedEffect(nowPlayingData) {
        isShowMiniPlayer = !(nowPlayingData?.mediaItem == null || nowPlayingData?.mediaItem == GenericMediaItem.EMPTY)
    }

    LaunchedEffect(intent) {
        val intent = intent ?: return@LaunchedEffect
        val data = intent.data
        Logger.d("MainActivity", "onCreate: $data")
        if (data != null) {
            if (data == "simpmusic://notification".toUri()) {
                viewModel.setIntent(null)
                navController.navigate(
                    NotificationDestination,
                )
            } else if (data.host == "simpmusic.org" || data.scheme == "simpmusic") {
                val segments = data.pathSegments
                val appPath =
                    if (data.scheme == "simpmusic") {
                        data.host
                    } else {
                        segments.getOrNull(1)
                    }
                Logger.d("MainActivity", "simpmusic.org deep link, appPath: $appPath")
                viewModel.setIntent(null)
                when (appPath) {
                    "watch" -> {
                        data.getQueryParameter("v")?.let { videoId ->
                            viewModel.loadSharedMediaItem(videoId)
                        }
                    }

                    "playlist" -> {
                        data.getQueryParameter("list")?.let { playlistId ->
                            if (playlistId.startsWith("OLAK5uy_")) {
                                navController.navigate(AlbumDestination(browseId = playlistId))
                            } else if (playlistId.startsWith("VL")) {
                                navController.navigate(PlaylistDestination(playlistId = playlistId))
                            } else {
                                navController.navigate(PlaylistDestination(playlistId = "VL$playlistId"))
                            }
                        }
                    }

                    "channel", "c" -> {
                        val artistId =
                            if (data.scheme == "simpmusic") {
                                segments.firstOrNull()
                            } else {
                                segments.getOrNull(2)
                            }
                        artistId?.let {
                            if (it.startsWith("UC")) {
                                navController.navigate(ArtistDestination(channelId = it))
                            } else {
                                viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                            }
                        }
                    }

                    "album" -> {
                        data.getQueryParameter("id")?.let { albumId ->
                            navController.navigate(AlbumDestination(browseId = albumId))
                        }
                    }

                    else -> {
                        viewModel.makeToast(getString(Res.string.this_link_is_not_supported))
                    }
                }
            } else {
                Logger.d("MainActivity", "onCreate: $data")
                when (val path = data.pathSegments.firstOrNull()) {
                    "playlist" -> {
                        data
                            .getQueryParameter("list")
                            ?.let { playlistId ->
                                viewModel.setIntent(null)
                                if (playlistId.startsWith("OLAK5uy_")) {
                                    navController.navigate(
                                        AlbumDestination(
                                            browseId = playlistId,
                                        ),
                                    )
                                } else if (playlistId.startsWith("VL")) {
                                    navController.navigate(
                                        PlaylistDestination(
                                            playlistId = playlistId,
                                        ),
                                    )
                                } else {
                                    navController.navigate(
                                        PlaylistDestination(
                                            playlistId = "VL$playlistId",
                                        ),
                                    )
                                }
                            }
                    }

                    "channel", "c" -> {
                        data.lastPathSegment?.let { artistId ->
                            if (artistId.startsWith("UC")) {
                                viewModel.setIntent(null)
                                navController.navigate(
                                    ArtistDestination(
                                        channelId = artistId,
                                    ),
                                )
                            } else {
                                viewModel.makeToast(
                                    getString(
                                        Res.string.this_link_is_not_supported,
                                    ),
                                )
                            }
                        }
                    }

                    else -> {
                        when {
                            path == "watch" -> data.getQueryParameter("v")
                            data.host == "youtu.be" -> path
                            else -> null
                        }?.let { videoId ->
                            viewModel.loadSharedMediaItem(videoId)
                        }
                    }
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        Logger.d("MainActivity", "Current destination: ${navBackStackEntry?.destination?.route}")
        if (navBackStackEntry?.destination?.route?.contains("FullscreenDestination") == true) {
            isShowNowPlaylistScreen = false
        }
        isInFullscreen = navBackStackEntry?.destination?.hierarchy?.any {
            it.hasRoute(FullscreenDestination::class)
        } == true
    }
    var isScrolledToTop by rememberSaveable {
        mutableStateOf(false)
    }
    val isTablet = windowSize.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isTabletLandscape = isTablet && currentOrientation() == Orientation.LANDSCAPE

    val backdrop = rememberBackdrop(Color.Black)

    AppTheme(
        followSystemTheme = followSystemTheme,
        forceLightTheme = forceLightTheme
    )  {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    if (!isTablet) {
                        AnimatedVisibility(
                            isNavBarVisible && !showSplash,
                            enter = fadeIn() + slideInHorizontally(),
                            exit = fadeOut(),
                        ) {
                            Column {
                                AnimatedVisibility(
                                    isShowMiniPlayer,
                                    enter = fadeIn() + slideInHorizontally(),
                                    exit = fadeOut(),
                                ) {
                                    MiniPlayer(
                                        Modifier
                                            .height(56.dp)
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 12.dp,
                                            ).padding(
                                                bottom = 4.dp,
                                            ),
                                        backdrop = backdrop,
                                        onClick = {
                                            isShowNowPlaylistScreen = true
                                        },
                                        onClose = {
                                            viewModel.stopPlayer()
                                            viewModel.isServiceRunning = false
                                        },
                                    )
                                }

                                AppBottomNavigationBar(
                                    navController = navController,
                                    isTranslucentBackground = false,
                                ) { klass ->
                                    viewModel.reloadDestination(klass)
                                }

                            }
                        }
                    }
                },
                content = { innerPadding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                        ) {
                            if (isTablet && !isInFullscreen) {
                                AppNavigationRail(
                                    navController = navController,
                                ) { klass ->
                                    viewModel.reloadDestination(klass)
                                }
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .hazeSource(hazeState),
                                ) {
                                    AppNavigationGraph(
                                        innerPadding = innerPadding,
                                        navController = navController,
                                        hideNavBar = {
                                            isNavBarVisible = false
                                        },
                                        showNavBar = {
                                            isNavBarVisible = true
                                        },
                                        showNowPlayingSheet = {
                                            isShowNowPlaylistScreen = true
                                        },
                                        onScrolling = {
                                            isScrolledToTop = it
                                        },
                                    )
                                }
                                this@Row.AnimatedVisibility(
                                    modifier =
                                        Modifier
                                            .padding(innerPadding)
                                            .align(Alignment.BottomCenter),
                                    visible = isShowMiniPlayer && isTablet && !isInFullscreen && !showSplash,
                                    enter = fadeIn() + slideInHorizontally(),
                                    exit = fadeOut(),
                                ) {
                                    MiniPlayer(
                                        if (getPlatform() == Platform.Android) {
                                            Modifier
                                                .height(56.dp)
                                                .fillMaxWidth(0.8f)
                                                .padding(
                                                    horizontal = 12.dp,
                                                ).padding(
                                                    bottom = 4.dp,
                                                )
                                        } else {
                                            Modifier
                                                .fillMaxWidth()
                                                .height(84.dp)
                                                .background(Color.Transparent)
                                                .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                                                    blurEnabled = true
                                                }
                                        },
                                        backdrop = backdrop,
                                        onClick = {
                                            isShowNowPlaylistScreen = true
                                        },
                                        onClose = {
                                            viewModel.stopPlayer()
                                            viewModel.isServiceRunning = false
                                        },
                                    )
                                }
                            }
                            if (isTablet && isTabletLandscape && !isInFullscreen) {
                                AnimatedVisibility(
                                    isShowNowPlaylistScreen,
                                    enter = expandHorizontally() + fadeIn(),
                                    exit = fadeOut() + shrinkHorizontally(),
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.35f),
                                    ) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            Modifier
                                                .padding(
                                                    innerPadding.copy(
                                                        start = 0.dp,
                                                        top = 0.dp,
                                                        bottom = 0.dp,
                                                    ),
                                                ).clip(
                                                    RoundedCornerShape(12.dp),
                                                ),
                                        ) {
                                            NowPlayingScreenContent(
                                                navController = navController,
                                                sharedViewModel = viewModel,
                                                isExpanded = true,
                                                dismissIcon = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                            ) {
                                                isShowNowPlaylistScreen = false
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isShowNowPlaylistScreen && !isTabletLandscape) {
                        NowPlayingScreen(
                            navController = navController,
                        ) {
                            isShowNowPlaylistScreen = false
                        }
                    }

                    if (sleepTimerState.isDone) {
                        Logger.w("MainActivity", "Sleep Timer Done: $sleepTimerState")
                        AlertDialog(
                            properties =
                                DialogProperties(
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false,
                                ),
                            onDismissRequest = {
                                viewModel.stopSleepTimer()
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.stopSleepTimer()
                                }) {
                                    Text(
                                        stringResource(Res.string.yes),
                                        style = typo().bodySmall,
                                    )
                                }
                            },
                            text = {
                                Text(
                                    stringResource(Res.string.sleep_timer_off),
                                    style = typo().labelSmall,
                                )
                            },
                            title = {
                                Text(
                                    stringResource(Res.string.good_night),
                                    style = typo().bodySmall,
                                )
                            },
                        )
                    }

                    if (showNotificationPermissionDialog) {
                        var doNotShowAgain by remember { mutableStateOf(false) }
                        AlertDialog(
                            onDismissRequest = {
                                viewModel.dismissNotificationPermissionDialog(doNotShowAgain)
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.dismissNotificationPermissionDialog(doNotShowAgain)
                                    },
                                ) {
                                    Text(
                                        stringResource(Res.string.yes),
                                        style = typo().bodySmall,
                                    )
                                }
                            },
                            title = {
                                Text(
                                    stringResource(Res.string.notification),
                                    style = typo().labelSmall,
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        stringResource(Res.string.this_app_needs_to_access_your_notification),
                                        style = typo().bodySmall,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier =
                                            Modifier
                                                .clickable { doNotShowAgain = !doNotShowAgain }
                                                .fillMaxWidth(),
                                    ) {
                                        Checkbox(
                                            checked = doNotShowAgain,
                                            onCheckedChange = { doNotShowAgain = it },
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            stringResource(Res.string.do_not_show_again),
                                            style = typo().bodySmall,
                                        )
                                    }
                                }
                            },
                        )
                    }
                },
            )

            AnimatedVisibility(
                visible = showSplash,
                exit = fadeOut(animationSpec = tween(durationMillis = 800))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    SplashScreen(
                        onSplashFinished = {
                            showSplash = false
                        }
                    )
                }
            }
        }
    }
}