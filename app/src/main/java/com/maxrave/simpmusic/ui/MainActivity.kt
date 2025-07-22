package com.maxrave.simpmusic.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.FIRST_TIME_MIGRATION
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.STATUS_DONE
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LOCATION
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.ui.component.AppBottomNavigationBar
import com.maxrave.simpmusic.ui.navigation.destination.home.NotificationDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.graph.AppNavigationGraph
import com.maxrave.simpmusic.ui.screen.MiniPlayer
import com.maxrave.simpmusic.ui.screen.player.NowPlayingScreen
import com.maxrave.simpmusic.ui.theme.AppTheme
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

@UnstableApi
@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    val viewModel: SharedViewModel by inject()

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is SimpleMediaService.MusicBinder) {
                    Log.w("MainActivity", "onServiceConnected: ")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.w("MainActivity", "onServiceDisconnected: ")
            }
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent: $intent")
        viewModel.setIntent(intent)
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray,
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
//    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume: ")
    }

    @UnstableApi
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
        Log.d("MainActivity", "onCreate: ")
        val data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
        if (data != null) {
            viewModel.setIntent(intent)
        }
        Log.d("Italy", "Key: ${Locale.ITALY.toLanguageTag()}")

        // Check if the migration has already been done or not
        if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
            Log.d("Locale Key", "onCreate: ${Locale.getDefault().toLanguageTag()}")
            if (SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())) {
                Log.d(
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
                Log.d("Locale Key", "getString: $it")
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
            Log.d(
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
            val navController = rememberNavController()

            val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
            val nowPlayingData by viewModel.nowPlayingState.collectAsState()
            val githubResponse by viewModel.githubResponse.collectAsState()
            val intent by viewModel.intent.collectAsState()

            val isTranslucentBottomBar by viewModel.getTranslucentBottomBar().collectAsStateWithLifecycle(DataStoreManager.FALSE)
            // MiniPlayer visibility logic
            var isShowMiniPlayer by rememberSaveable {
                mutableStateOf(true)
            }

            // Now playing screen
            var isShowNowPlaylistScreen by rememberSaveable {
                mutableStateOf(false)
            }

            var isNavBarVisible by rememberSaveable {
                mutableStateOf(true)
            }

            var shouldShowUpdateDialog by rememberSaveable {
                mutableStateOf(false)
            }

            LaunchedEffect(nowPlayingData) {
                if (nowPlayingData?.mediaItem == null || nowPlayingData?.mediaItem == MediaItem.EMPTY) {
                    isShowMiniPlayer = false
                } else {
                    isShowMiniPlayer = true
                }
            }

            LaunchedEffect(intent) {
                val intent = intent ?: return@LaunchedEffect
                val data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
                Log.d("MainActivity", "onCreate: $data")
                if (data != null) {
                    if (data == "simpmusic://notification".toUri()) {
                        viewModel.setIntent(null)
                        navController.navigate(
                            NotificationDestination,
                        )
                    } else {
                        Log.d("MainActivity", "onCreate: $data")
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

            LaunchedEffect(githubResponse) {
                val response = githubResponse ?: return@LaunchedEffect
                if (!this@MainActivity.isInPictureInPictureMode &&
                    viewModel.showedUpdateDialog &&
                    response.tagName != getString(R.string.version_format, VersionManager.getVersionName())
                ) {
                    shouldShowUpdateDialog = true
                }
            }

            AppTheme {
                Scaffold(
                    bottomBar = {
                        AnimatedVisibility(
                            isNavBarVisible,
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
                                    isTranslucentBackground = isTranslucentBottomBar == DataStoreManager.TRUE,
                                ) { klass ->
                                    viewModel.reloadDestination(klass)
                                }
                            }
                        }
                    },
                    content = { innerPadding ->
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
                        )

                        if (isShowNowPlaylistScreen) {
                            NowPlayingScreen(
                                navController = navController,
                            ) {
                                isShowNowPlaylistScreen = false
                            }
                        }

                        if (sleepTimerState.isDone) {
                            Log.w("MainActivity", "Sleep Timer Done: $sleepTimerState")
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
                            val response = githubResponse ?: return@Scaffold
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
                                        response.publishedAt?.let { input ->
                                            inputFormat
                                                .parse(input)
                                                ?.let { outputFormat.format(it) }
                                        }
                                    val updateMessage =
                                        getString(
                                            R.string.update_message,
                                            response.tagName,
                                            formatted,
                                            "",
                                        )
                                    Column(
                                        Modifier
                                            .height(
                                                400.dp,
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
                                            response.body ?: "",
                                            typography =
                                                markdownTypography(
                                                    h1 = typo.labelLarge,
                                                    h2 = typo.labelMedium,
                                                    h3 = typo.labelSmall,
                                                    text = typo.bodySmall,
                                                    bullet = typo.bodySmall,
                                                    paragraph = typo.bodySmall,
                                                    link =
                                                        typo.bodySmall.copy(
                                                            textDecoration = TextDecoration.Underline,
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
        Log.w("MainActivity", "onDestroy: ")
        if (viewModel.shouldStopMusicService()) {
            viewModel.isServiceRunning = false
            unbindService(serviceConnection)
        }
        unloadKoinModules(viewModelModule)
        super.onDestroy()
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.activityRecreate()
    }

    private fun startMusicService() {
        println("go to StartMusicService")
        if (!viewModel.recreateActivity.value) {
            val intent = Intent(this, SimpleMediaService::class.java)
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            viewModel.isServiceRunning = true
            Log.d("Service", "Service started")
        }
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