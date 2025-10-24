package com.maxrave.simpmusic.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowWidthSizeClass
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.maxrave.common.FIRST_TIME_MIGRATION
import com.maxrave.common.R
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.STATUS_DONE
import com.maxrave.common.SUPPORTED_LANGUAGE
import com.maxrave.common.SUPPORTED_LOCATION
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.ui.component.AppBottomNavigationBar
import com.maxrave.simpmusic.ui.component.AppNavigationRail
import com.maxrave.simpmusic.ui.component.LiquidGlassAppBottomNavigationBar
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.player.FullscreenDestination
import com.maxrave.simpmusic.ui.navigation.graph.AppNavigationGraph
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.screen.player.NowPlayingScreen
import com.maxrave.simpmusic.ui.screen.player.NowPlayingScreenContent
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.ui.theme.fontFamily
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import org.koin.android.ext.android.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    val viewModel: SharedViewModel by inject()
    val mediaPlayerHandler by inject<MediaPlayerHandler>()

    private var mBound = false
    private var shouldUnbind = false
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                mediaPlayerHandler.setActivitySession(this@MainActivity, MainActivity::class.java, service)
                Logger.w("MainActivity", "onServiceConnected: ")
                mBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Logger.w("MainActivity", "onServiceDisconnected: ")
                mBound = false
            }
        }

    override fun onStart() {
        super.onStart()
        startMusicService()
    }

    override fun onStop() {
        super.onStop()
        if (shouldUnbind) {
            unbindService(serviceConnection)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d("MainActivity", "onNewIntent: $intent")
        viewModel.setIntent(intent)
    }

    @ExperimentalMaterial3Api
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recreate view model to fix the issue of view model not getting data from the service
        unloadKoinModules(viewModelModule)
        loadKoinModules(viewModelModule)
        VersionManager.initialize(applicationContext)
        checkForUpdate()
        if (viewModel.recreateActivity.value || viewModel.isServiceRunning) {
            viewModel.activityRecreateDone()
        } else {
            startMusicService()
        }
        Logger.d("MainActivity", "onCreate: ")
        val data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
        if (data != null) {
            viewModel.setIntent(intent)
        }
        Logger.d("Italy", "Key: ${Locale.ITALY.toLanguageTag()}")

        // Check if the migration has already been done or not
        if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
            Logger.d("Locale Key", "onCreate: ${Locale.getDefault().toLanguageTag()}")
            if (SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())) {
                Logger.d(
                    "Contains",
                    "onCreate: ${
                        SUPPORTED_LANGUAGE.codes.contains(
                            Locale.getDefault().toLanguageTag(),
                        )
                    }",
                )
                putString(SELECTED_LANGUAGE, Locale.getDefault().toLanguageTag())
                if (SUPPORTED_LOCATION.items.contains(Locale.getDefault().country)) {
                    putString("location", Locale.getDefault().country)
                } else {
                    putString("location", "US")
                }
            } else {
                putString(SELECTED_LANGUAGE, "en-US")
            }
            // Fetch the selected language from wherever it was stored. In this case its SharedPref
            getString(SELECTED_LANGUAGE)?.let {
                Logger.d("Locale Key", "getString: $it")
                // Set this locale using the AndroidX library that will handle the storage itself
                val localeList = LocaleListCompat.forLanguageTags(it)
                AppCompatDelegate.setApplicationLocales(localeList)
                // Set the migration flag to ensure that this is executed only once
                putString(FIRST_TIME_MIGRATION, STATUS_DONE)
            }
        }
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() !=
            getString(
                SELECTED_LANGUAGE,
            )
        ) {
            Logger.d(
                "Locale Key",
                "onCreate: ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}",
            )
            putString(SELECTED_LANGUAGE, AppCompatDelegate.getApplicationLocales().toLanguageTags())
        }

        enableEdgeToEdge(
            navigationBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.Transparent.toArgb(),
                ),
            statusBarStyle =
                SystemBarStyle.dark(
                    scrim = Color.Transparent.toArgb(),
                ),
        )
        viewModel.checkIsRestoring()
        viewModel.runWorker()

        if (!EasyPermissions.hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.this_app_needs_to_access_your_notification),
                    1,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }
        }
        viewModel.getLocation()

        setContent {
            val windowSize = currentWindowAdaptiveInfo().windowSizeClass
            val resources = LocalResources.current
            val navController = rememberNavController()

            val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
            val nowPlayingData by viewModel.nowPlayingState.collectAsStateWithLifecycle()
            val updateData by viewModel.updateResponse.collectAsStateWithLifecycle()
            val intent by viewModel.intent.collectAsStateWithLifecycle()

            val isTranslucentBottomBar by viewModel.getTranslucentBottomBar().collectAsStateWithLifecycle(DataStoreManager.FALSE)
            val isLiquidGlassEnabled by viewModel.getEnableLiquidGlass().collectAsStateWithLifecycle(DataStoreManager.FALSE)
            // MiniPlayer visibility logic
            var isShowMiniPlayer by rememberSaveable {
                mutableStateOf(true)
            }

            // Now playing screen
            var isShowNowPlaylistScreen by rememberSaveable {
                mutableStateOf(false)
            }

            // Fullscreen
            var isInFullscreen by rememberSaveable {
                mutableStateOf(false)
            }

            var isNavBarVisible by rememberSaveable {
                mutableStateOf(true)
            }

            var shouldShowUpdateDialog by rememberSaveable {
                mutableStateOf(false)
            }

            LaunchedEffect(nowPlayingData) {
                isShowMiniPlayer = !(nowPlayingData?.mediaItem == null || nowPlayingData?.mediaItem == GenericMediaItem.EMPTY)
            }

            LaunchedEffect(intent) {
                val intent = intent ?: return@LaunchedEffect
                val data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
                Logger.d("MainActivity", "onCreate: $data")
                if (data != null) {
                    if (data == "simpmusic://notification".toUri()) {
                        viewModel.setIntent(null)
                        navController.navigate(
                            NotificationDestination,
                        )
                    } else {
                        Logger.d("MainActivity", "onCreate: $data")
                        when (val path = data.pathSegments.firstOrNull()) {
                            "playlist" ->
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

                            "channel", "c" ->
                                data.lastPathSegment?.let { artistId ->
                                    if (artistId.startsWith("UC")) {
                                        viewModel.setIntent(null)
                                        navController.navigate(
                                            ArtistDestination(
                                                channelId = artistId,
                                            ),
                                        )
                                    } else {
                                        Toast
                                            .makeText(
                                                this@MainActivity,
                                                getString(
                                                    R.string.this_link_is_not_supported,
                                                ),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                }

                            else ->
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

            LaunchedEffect(updateData) {
                val response = updateData ?: return@LaunchedEffect
                if (!this@MainActivity.isInPictureInPictureMode &&
                    viewModel.showedUpdateDialog &&
                    response.tagName != getString(R.string.version_format, VersionManager.getVersionName())
                ) {
                    shouldShowUpdateDialog = true
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
            val backdrop = rememberLayerBackdrop()
            var isScrolledToTop by rememberSaveable {
                mutableStateOf(false)
            }
            val isTablet = windowSize.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
            val isTabletLandscape = isTablet && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            AppTheme {
                Scaffold(
                    bottomBar = {
                        if (!isTablet) {
                            AnimatedVisibility(
                                isNavBarVisible,
                                enter = fadeIn() + slideInHorizontally(),
                                exit = fadeOut(),
                            ) {
                                Column {
                                    AnimatedVisibility(
                                        isShowMiniPlayer && isLiquidGlassEnabled == DataStoreManager.FALSE,
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
                                    if (isLiquidGlassEnabled == TRUE) {
                                        LiquidGlassAppBottomNavigationBar(
                                            navController = navController,
                                            backdrop = backdrop,
                                            viewModel = viewModel,
                                            onOpenNowPlaying = { isShowNowPlaylistScreen = true },
                                            isScrolledToTop = isScrolledToTop,
                                        ) { klass ->
                                            viewModel.reloadDestination(klass)
                                        }
                                    } else {
                                        AppBottomNavigationBar(
                                            navController = navController,
                                            isTranslucentBackground = isTranslucentBottomBar == TRUE,
                                        ) { klass ->
                                            viewModel.reloadDestination(klass)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    content = { innerPadding ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .then(
                                    if (isLiquidGlassEnabled == TRUE && !isTablet) {
                                        Modifier.layerBackdrop(backdrop)
                                    } else {
                                        Modifier
                                    },
                                ),
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
                                            .then(
                                                if (isLiquidGlassEnabled == TRUE && isTablet && !isInFullscreen) {
                                                    Modifier.layerBackdrop(backdrop)
                                                } else {
                                                    Modifier
                                                },
                                            ),
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
                                        visible = isShowMiniPlayer && isTablet && !isInFullscreen,
                                        enter = fadeIn() + slideInHorizontally(),
                                        exit = fadeOut(),
                                    ) {
                                        MiniPlayer(
                                            Modifier
                                                .height(56.dp)
                                                .fillMaxWidth(0.8f)
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
                                                    onSwipeEnabledChange = {},
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
                                            stringResource(R.string.yes),
                                            style = typo.bodySmall,
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        stringResource(R.string.sleep_timer_off),
                                        style = typo.labelSmall,
                                    )
                                },
                                title = {
                                    Text(
                                        stringResource(R.string.good_night),
                                        style = typo.bodySmall,
                                    )
                                },
                            )
                        }

                        if (shouldShowUpdateDialog) {
                            val response = updateData ?: return@Scaffold
                            AlertDialog(
                                properties =
                                    DialogProperties(
                                        dismissOnBackPress = false,
                                        dismissOnClickOutside = false,
                                    ),
                                onDismissRequest = {
                                    shouldShowUpdateDialog = false
                                    viewModel.showedUpdateDialog = false
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            shouldShowUpdateDialog = false
                                            viewModel.showedUpdateDialog = false
                                            val browserIntent =
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    "https://simpmusic.org/download".toUri(),
                                                )
                                            startActivity(browserIntent)
                                        },
                                    ) {
                                        Text(
                                            stringResource(R.string.download),
                                            style = typo.bodySmall,
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            shouldShowUpdateDialog = false
                                            viewModel.showedUpdateDialog = false
                                        },
                                    ) {
                                        Text(
                                            stringResource(R.string.cancel),
                                            style = typo.bodySmall,
                                        )
                                    }
                                },
                                title = {
                                    Text(
                                        stringResource(R.string.update_available),
                                        style = typo.labelSmall,
                                    )
                                },
                                text = {
                                    val inputFormat =
                                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                    val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
                                    val formatted =
                                        response.releaseTime?.let { input ->
                                            inputFormat
                                                .parse(input)
                                                ?.let { outputFormat.format(it) }
                                        } ?: stringResource(R.string.unknown)
                                    val updateMessage =
                                        getString(
                                            R.string.update_message,
                                            response.tagName,
                                            formatted,
                                            "",
                                        )
                                    Column(
                                        Modifier
                                            .heightIn(
                                                max = 400.dp,
                                            ).verticalScroll(
                                                rememberScrollState(),
                                            ),
                                    ) {
                                        Text(
                                            text = updateMessage,
                                            style = typo.bodySmall,
                                            modifier =
                                                Modifier.padding(
                                                    vertical = 8.dp,
                                                ),
                                        )
                                        Markdown(
                                            response.body,
                                            typography =
                                                markdownTypography(
                                                    h1 = typo.labelLarge,
                                                    h2 = typo.labelMedium,
                                                    h3 = typo.labelSmall,
                                                    text = typo.bodySmall,
                                                    bullet = typo.bodySmall,
                                                    paragraph = typo.bodySmall,
                                                    textLink =
                                                        TextLinkStyles(
                                                            SpanStyle(
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Normal,
                                                                fontFamily = fontFamily,
                                                                textDecoration = TextDecoration.Underline,
                                                            ),
                                                        ),
                                                ),
                                        )
                                    }
                                },
                            )
                        }
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        val shouldStopMusicService = viewModel.shouldStopMusicService()
        Logger.w("MainActivity", "onDestroy: Should stop service $shouldStopMusicService")

        // Always unbind service if it was bound to prevent MusicBinder leak
        if (shouldStopMusicService && shouldUnbind && isFinishing) {
            viewModel.isServiceRunning = false
        }
        unloadKoinModules(viewModelModule)
        super.onDestroy()
        Logger.d("MainActivity", "onDestroy: ")
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.activityRecreate()
    }

    private fun startMusicService() {
        mediaPlayerHandler.startMediaService(this, serviceConnection)
        viewModel.isServiceRunning = true
        shouldUnbind = true
        Logger.d("Service", "Service started")
    }

    private fun checkForUpdate() {
        if (viewModel.shouldCheckForUpdate()) {
            viewModel.checkForUpdate()
        }
    }

    private fun putString(
        key: String,
        value: String,
    ) {
        viewModel.putString(key, value)
    }

    private fun getString(key: String): String? = viewModel.getString(key)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.activityRecreate()
    }
}