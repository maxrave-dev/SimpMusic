package com.maxrave.simpmusic.ui.screen.home

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.kotlinytmusicscraper.extension.isTwoLetterCode
import com.maxrave.kotlinytmusicscraper.extension.isValidProxyHost
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.LIMIT_CACHE_SIZE
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.common.SPONSOR_BLOCK
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LOCATION
import com.maxrave.simpmusic.common.VIDEO_QUALITY
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.TRUE
import com.maxrave.simpmusic.extension.bytesToMB
import com.maxrave.simpmusic.ui.component.ActionButton
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SettingItem
import com.maxrave.simpmusic.ui.navigation.destination.home.CreditDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.SpotifyLoginDestination
import com.maxrave.simpmusic.ui.theme.DarkColors
import com.maxrave.simpmusic.ui.theme.md_theme_dark_outline
import com.maxrave.simpmusic.ui.theme.md_theme_dark_primary
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.LocalResource
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.SettingAlertState
import com.maxrave.simpmusic.viewModel.SettingBasicAlertState
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.mikepenz.aboutlibraries.ui.compose.ChipColors
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@UnstableApi
@Composable
fun SettingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val context = LocalContext.current
    val localDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current

    var width by rememberSaveable { mutableIntStateOf(0) }

    // Backup and restore
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(uri)
            }
        }
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restore(uri)
            }
        }

    // Open equalizer
    val resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    val enableTranslucentNavBar by viewModel.translucentBottomBar.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val language by viewModel.language.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val quality by viewModel.quality.collectAsStateWithLifecycle()
    val homeLimit by viewModel.homeLimit.collectAsStateWithLifecycle()
    val playVideo by viewModel.playVideoInsteadOfAudio.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val videoQuality by viewModel.videoQuality.collectAsStateWithLifecycle()
    val sendData by viewModel.sendBackToGoogle.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val normalizeVolume by viewModel.normalizeVolume.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val skipSilent by viewModel.skipSilent.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val savePlaybackState by viewModel.savedPlaybackState.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val saveLastPlayed by viewModel.saveRecentSongAndQueue.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val killServiceOnExit by viewModel.killServiceOnExit.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = true)
    val mainLyricsProvider by viewModel.mainLyricsProvider.collectAsStateWithLifecycle()
    val youtubeSubtitleLanguage by viewModel.youtubeSubtitleLanguage.collectAsStateWithLifecycle()
    val spotifyLoggedIn by viewModel.spotifyLogIn.collectAsStateWithLifecycle()
    val spotifyLyrics by viewModel.spotifyLyrics.collectAsStateWithLifecycle()
    val spotifyCanvas by viewModel.spotifyCanvas.collectAsStateWithLifecycle()
    val enableSponsorBlock by viewModel.sponsorBlockEnabled.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val skipSegments by viewModel.sponsorBlockCategories.collectAsStateWithLifecycle()
    val playerCache by viewModel.cacheSize.collectAsStateWithLifecycle()
    val downloadedCache by viewModel.downloadedCacheSize.collectAsStateWithLifecycle()
    val thumbnailCache by viewModel.thumbCacheSize.collectAsStateWithLifecycle()
    val canvasCache by viewModel.canvasCacheSize.collectAsStateWithLifecycle()
    val limitPlayerCache by viewModel.playerCacheLimit.collectAsStateWithLifecycle()
    val fraction by viewModel.fraction.collectAsStateWithLifecycle()
    val lastCheckUpdate by viewModel.lastCheckForUpdate.collectAsStateWithLifecycle()
    val usingProxy by viewModel.usingProxy.collectAsStateWithLifecycle()
    val proxyType by viewModel.proxyType.collectAsStateWithLifecycle()
    val proxyHost by viewModel.proxyHost.collectAsStateWithLifecycle()
    val proxyPort by viewModel.proxyPort.collectAsStateWithLifecycle()
    val autoCheckUpdate by viewModel.autoCheckUpdate.collectAsStateWithLifecycle()
    val blurFullscreenLyrics by viewModel.blurFullscreenLyrics.collectAsStateWithLifecycle()
    val blurPlayerBackground by viewModel.blurPlayerBackground.collectAsStateWithLifecycle()
    val aiProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val isHasApiKey by viewModel.isHasApiKey.collectAsStateWithLifecycle()
    val useAITranslation by viewModel.useAITranslation.collectAsStateWithLifecycle()
    val translationLanguage by viewModel.translationLanguage.collectAsStateWithLifecycle()
    val customModelId by viewModel.customModelId.collectAsStateWithLifecycle()
    val helpBuildLyricsDatabase by viewModel.helpBuildLyricsDatabase.collectAsStateWithLifecycle()
    val contributor by viewModel.contributor.collectAsStateWithLifecycle()
    val backupDownloaded by viewModel.backupDownloaded.collectAsStateWithLifecycle()

    var checkForUpdateSubtitle by rememberSaveable {
        mutableStateOf("")
    }
    var showYouTubeAccountDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showThirdPartyLibraries by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(true) {
        viewModel.getAllGoogleAccount()
    }

    LaunchedEffect(lastCheckUpdate) {
        val lastCheckLong = lastCheckUpdate?.toLong() ?: 0L
        if (lastCheckLong > 0L) {
            checkForUpdateSubtitle =
                context.getString(
                    R.string.last_checked_at,
                    DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(lastCheckLong)),
                )
        }
    }

    LaunchedEffect(true) {
        viewModel.getData()
    }

    LazyColumn(
        contentPadding = innerPadding,
        modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 64.dp),
    ) {
        item(key = "user_interface") {
            Column {
                Spacer(Modifier.height(16.dp))
                Text(text = stringResource(R.string.user_interface), style = typo.labelMedium)
                SettingItem(
                    title = stringResource(R.string.translucent_bottom_navigation_bar),
                    subtitle = stringResource(R.string.you_can_see_the_content_below_the_bottom_bar),
                    smallSubtitle = true,
                    switch = (enableTranslucentNavBar to { viewModel.setTranslucentBottomBar(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.blur_fullscreen_lyrics),
                    subtitle = stringResource(R.string.blur_fullscreen_lyrics_description),
                    smallSubtitle = true,
                    switch = (blurFullscreenLyrics to { viewModel.setBlurFullscreenLyrics(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.blur_player_background),
                    subtitle = stringResource(R.string.blur_player_background_description),
                    smallSubtitle = true,
                    switch = (blurPlayerBackground to { viewModel.setBlurPlayerBackground(it) }),
                )
            }
        }
        item(key = "content") {
            Column {
                Text(text = stringResource(R.string.content), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.youtube_account),
                    subtitle = stringResource(R.string.manage_your_youtube_accounts),
                    onClick = {
                        viewModel.getAllGoogleAccount()
                        showYouTubeAccountDialog = true
                    },
                )
                SettingItem(
                    title = stringResource(R.string.language),
                    subtitle = SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US"),
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.language),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            SUPPORTED_LANGUAGE.items.map {
                                                (it.toString() == SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US")) to it.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        val code = SUPPORTED_LANGUAGE.getCodeFromLanguage(state.selectOne?.getSelected() ?: "English")
                                        viewModel.setBasicAlertData(
                                            SettingBasicAlertState(
                                                title = context.getString(R.string.warning),
                                                message = context.getString(R.string.change_language_warning),
                                                confirm =
                                                    context.getString(R.string.change) to {
                                                        sharedViewModel.activityRecreate()
                                                        viewModel.setBasicAlertData(null)
                                                        viewModel.changeLanguage(code)
                                                    },
                                                dismiss = context.getString(R.string.cancel),
                                            ),
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.content_country),
                    subtitle = location ?: "",
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.content_country),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            SUPPORTED_LOCATION.items.map { item ->
                                                (item.toString() == location) to item.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.changeLocation(
                                            state.selectOne?.getSelected() ?: "US",
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.quality),
                    subtitle = quality ?: "",
                    smallSubtitle = true,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.quality),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            QUALITY.items.map { item ->
                                                (item.toString() == quality) to item.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.changeQuality(state.selectOne?.getSelected())
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.home_limit),
                    subtitle = homeLimit?.toString() ?: stringResource(R.string.unknown),
                ) {
                    Slider(
                        value = homeLimit?.toFloat() ?: 3f,
                        onValueChange = {
                            viewModel.setHomeLimit(it.toInt())
                        },
                        modifier = Modifier,
                        enabled = true,
                        valueRange = 3f..8f,
                        steps = 4,
                        onValueChangeFinished = {},
                    )
                }
                SettingItem(
                    title = stringResource(R.string.play_video_for_video_track_instead_of_audio_only),
                    subtitle = stringResource(R.string.such_as_music_video_lyrics_video_podcasts_and_more),
                    smallSubtitle = true,
                    switch = (playVideo to { viewModel.setPlayVideoInsteadOfAudio(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.video_quality),
                    subtitle = videoQuality ?: "",
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.video_quality),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            VIDEO_QUALITY.items.map { item ->
                                                (item.toString() == videoQuality) to item.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.changeVideoQuality(state.selectOne?.getSelected() ?: "")
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.send_back_listening_data_to_google),
                    subtitle =
                        stringResource(
                            R.string
                                .upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in,
                        ),
                    smallSubtitle = true,
                    switch = (sendData to { viewModel.setSendBackToGoogle(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.proxy),
                    subtitle = stringResource(R.string.proxy_description),
                    switch = (usingProxy to { viewModel.setUsingProxy(it) }),
                )
            }
        }
        item(key = "proxy") {
            Crossfade(usingProxy) { it ->
                if (it) {
                    Column {
                        SettingItem(
                            title = stringResource(R.string.proxy_type),
                            subtitle =
                                when (proxyType) {
                                    DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP -> stringResource(R.string.http)
                                    DataStoreManager.Settings.ProxyType.PROXY_TYPE_SOCKS -> stringResource(R.string.socks)
                                },
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = context.getString(R.string.proxy_type),
                                        selectOne =
                                            SettingAlertState.SelectData(
                                                listSelect =
                                                    listOf(
                                                        (proxyType == DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP) to
                                                            context.getString(
                                                                R.string.http,
                                                            ),
                                                        (proxyType == DataStoreManager.Settings.ProxyType.PROXY_TYPE_SOCKS) to
                                                            context.getString(R.string.socks),
                                                    ),
                                            ),
                                        confirm =
                                            context.getString(R.string.change) to { state ->
                                                viewModel.setProxy(
                                                    if (state.selectOne?.getSelected() == context.getString(R.string.socks)) {
                                                        DataStoreManager.Settings.ProxyType.PROXY_TYPE_SOCKS
                                                    } else {
                                                        DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP
                                                    },
                                                    proxyHost,
                                                    proxyPort,
                                                )
                                            },
                                        dismiss = context.getString(R.string.cancel),
                                    ),
                                )
                            },
                        )
                        SettingItem(
                            title = stringResource(R.string.proxy_host),
                            subtitle = proxyHost,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = context.getString(R.string.proxy_host),
                                        message = context.getString(R.string.proxy_host_message),
                                        textField =
                                            SettingAlertState.TextFieldData(
                                                label = context.getString(R.string.proxy_host),
                                                value = proxyHost,
                                                verifyCodeBlock = {
                                                    isValidProxyHost(it) to context.getString(R.string.invalid_host)
                                                },
                                            ),
                                        confirm =
                                            context.getString(R.string.change) to { state ->
                                                viewModel.setProxy(
                                                    proxyType,
                                                    state.textField?.value ?: "",
                                                    proxyPort,
                                                )
                                            },
                                        dismiss = context.getString(R.string.cancel),
                                    ),
                                )
                            },
                        )
                        SettingItem(
                            title = stringResource(R.string.proxy_port),
                            subtitle = proxyPort.toString(),
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = context.getString(R.string.proxy_port),
                                        message = context.getString(R.string.proxy_port_message),
                                        textField =
                                            SettingAlertState.TextFieldData(
                                                label = context.getString(R.string.proxy_port),
                                                value = proxyPort.toString(),
                                                verifyCodeBlock = {
                                                    (it.toIntOrNull() != null) to context.getString(R.string.invalid_port)
                                                },
                                            ),
                                        confirm =
                                            context.getString(R.string.change) to { state ->
                                                viewModel.setProxy(
                                                    proxyType,
                                                    proxyHost,
                                                    state.textField?.value?.toIntOrNull() ?: 0,
                                                )
                                            },
                                        dismiss = context.getString(R.string.cancel),
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
        item(key = "audio") {
            Column {
                Text(text = stringResource(R.string.audio), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.normalize_volume),
                    subtitle = stringResource(R.string.balance_media_loudness),
                    switch = (normalizeVolume to { viewModel.setNormalizeVolume(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.skip_silent),
                    subtitle = stringResource(R.string.skip_no_music_part),
                    switch = (skipSilent to { viewModel.setSkipSilent(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.open_system_equalizer),
                    subtitle = stringResource(R.string.use_your_system_equalizer),
                    onClick = {
                        val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                        eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, viewModel.getAudioSessionId())
                        eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                        val packageManager = context.packageManager
                        val resolveInfo: List<*> = packageManager.queryIntentActivities(eqIntent, 0)
                        Log.d("EQ", resolveInfo.toString())
                        if (resolveInfo.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.no_equalizer), Toast.LENGTH_SHORT).show()
                        } else {
                            resultLauncher.launch(eqIntent)
                        }
                    },
                )
            }
        }
        item(key = "playback") {
            Column {
                Text(text = stringResource(R.string.playback), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.save_playback_state),
                    subtitle = stringResource(R.string.save_shuffle_and_repeat_mode),
                    switch = (savePlaybackState to { viewModel.setSavedPlaybackState(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.save_last_played),
                    subtitle = stringResource(R.string.save_last_played_track_and_queue),
                    switch = (saveLastPlayed to { viewModel.setSaveLastPlayed(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.kill_service_on_exit),
                    subtitle = stringResource(R.string.kill_service_on_exit_description),
                    switch = (killServiceOnExit to { viewModel.setKillServiceOnExit(it) }),
                )
            }
        }
        item(key = "lyrics") {
            Column {
                Text(text = stringResource(R.string.lyrics), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.main_lyrics_provider),
                    subtitle =
                        when (mainLyricsProvider) {
                            DataStoreManager.SIMPMUSIC -> stringResource(R.string.simpmusic_lyrics)
                            DataStoreManager.YOUTUBE -> stringResource(R.string.youtube_transcript)
                            DataStoreManager.LRCLIB -> stringResource(R.string.lrclib)
                            else -> stringResource(R.string.unknown)
                        },
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.main_lyrics_provider),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            listOf(
                                                (mainLyricsProvider == DataStoreManager.SIMPMUSIC) to context.getString(R.string.simpmusic_lyrics),
                                                (mainLyricsProvider == DataStoreManager.YOUTUBE) to context.getString(R.string.youtube_transcript),
                                                (mainLyricsProvider == DataStoreManager.LRCLIB) to context.getString(R.string.lrclib),
                                            ),
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.setLyricsProvider(
                                            when (state.selectOne?.getSelected()) {
                                                context.getString(R.string.simpmusic_lyrics) -> DataStoreManager.SIMPMUSIC
                                                context.getString(R.string.youtube_transcript) -> DataStoreManager.YOUTUBE
                                                context.getString(R.string.lrclib) -> DataStoreManager.LRCLIB
                                                else -> DataStoreManager.SIMPMUSIC
                                            },
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )

                SettingItem(
                    title = stringResource(R.string.translation_language),
                    subtitle = translationLanguage ?: "",
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.translation_language),
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = context.getString(R.string.translation_language),
                                        value = translationLanguage ?: "",
                                        verifyCodeBlock = {
                                            (it.length == 2 && it.isTwoLetterCode()) to context.getString(R.string.invalid_language_code)
                                        },
                                    ),
                                message = context.getString(R.string.translation_language_message),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.setTranslationLanguage(state.textField?.value ?: "")
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                    isEnable = useAITranslation,
                )
                SettingItem(
                    title = stringResource(R.string.youtube_subtitle_language),
                    subtitle = youtubeSubtitleLanguage,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.youtube_subtitle_language),
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = context.getString(R.string.youtube_subtitle_language),
                                        value = youtubeSubtitleLanguage,
                                        verifyCodeBlock = {
                                            (it.length == 2 && it.isTwoLetterCode()) to context.getString(R.string.invalid_language_code)
                                        },
                                    ),
                                message = context.getString(R.string.youtube_subtitle_language_message),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.setYoutubeSubtitleLanguage(state.textField?.value ?: "")
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.help_build_lyrics_database),
                    subtitle = stringResource(R.string.help_build_lyrics_database_description),
                    switch = (helpBuildLyricsDatabase to { viewModel.setHelpBuildLyricsDatabase(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.contributor_name),
                    subtitle = contributor.first.ifEmpty { stringResource(R.string.anonymous) },
                    isEnable = helpBuildLyricsDatabase,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.contributor_name),
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = context.getString(R.string.contributor_name),
                                        value = "",
                                    ),
                                message = "",
                                confirm =
                                    context.getString(R.string.set) to { state ->
                                        viewModel.setContributorName(state.textField?.value ?: "")
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.contributor_email),
                    subtitle = contributor.second.ifEmpty { stringResource(R.string.anonymous) },
                    isEnable = helpBuildLyricsDatabase,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.contributor_email),
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = context.getString(R.string.contributor_email),
                                        value = "",
                                        verifyCodeBlock = {
                                            if (it.isNotEmpty()) {
                                                (it.contains("@")) to context.getString(R.string.invalid)
                                            } else {
                                                true to ""
                                            }
                                        },
                                    ),
                                message = "",
                                confirm =
                                    context.getString(R.string.set) to { state ->
                                        viewModel.setContributorEmail(state.textField?.value ?: "")
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                Text(
                    buildAnnotatedString {
                        append(stringResource(R.string.lyrics_database_description))
                        append(" ")
                        withLink(
                            LinkAnnotation.Url(
                                "https://github.com/maxrave-dev/lyrics",
                                TextLinkStyles(style = SpanStyle(color = md_theme_dark_primary)),
                            ),
                        ) {
                            append("https://github.com/maxrave-dev/lyrics")
                        }
                    },
                    style = typo.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        }
        item(key = "AI") {
            Column {
                Text(text = stringResource(R.string.ai), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.ai_provider),
                    subtitle =
                        when (aiProvider) {
                            DataStoreManager.AI_PROVIDER_OPENAI -> stringResource(R.string.openai)
                            DataStoreManager.AI_PROVIDER_GEMINI -> stringResource(R.string.gemini)
                            else -> stringResource(R.string.unknown)
                        },
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.ai_provider),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            listOf(
                                                (mainLyricsProvider == DataStoreManager.AI_PROVIDER_OPENAI) to context.getString(R.string.openai),
                                                (mainLyricsProvider == DataStoreManager.AI_PROVIDER_GEMINI) to context.getString(R.string.gemini),
                                            ),
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.setAIProvider(
                                            when (state.selectOne?.getSelected()) {
                                                context.getString(R.string.openai) -> DataStoreManager.AI_PROVIDER_OPENAI
                                                context.getString(R.string.gemini) -> DataStoreManager.AI_PROVIDER_GEMINI
                                                else -> DataStoreManager.AI_PROVIDER_OPENAI
                                            },
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.ai_api_key),
                    subtitle = if (isHasApiKey) "XXXXXXXXXX" else "N/A",
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.ai_api_key),
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = context.getString(R.string.ai_api_key),
                                        value = "",
                                        verifyCodeBlock = {
                                            (it.isNotEmpty()) to context.getString(R.string.invalid_api_key)
                                        },
                                    ),
                                message = "",
                                confirm =
                                    context.getString(R.string.set) to { state ->
                                        viewModel.setAIApiKey(state.textField?.value ?: "")
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.custom_ai_model_id),
                    subtitle = customModelId.ifEmpty { stringResource(R.string.default_models) },
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.custom_ai_model_id),
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = context.getString(R.string.custom_ai_model_id),
                                        value = "",
                                        verifyCodeBlock = {
                                            (it.isNotEmpty() && !it.contains(" ")) to context.getString(R.string.invalid)
                                        },
                                    ),
                                message = context.getString(R.string.custom_model_id_messages),
                                confirm =
                                    context.getString(R.string.set) to { state ->
                                        viewModel.setCustomModelId(state.textField?.value ?: "")
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.use_ai_translation),
                    subtitle = stringResource(R.string.use_ai_translation_description),
                    switch = (useAITranslation to { viewModel.setAITranslation(it) }),
                    isEnable = isHasApiKey,
                    onDisable = {
                        if (useAITranslation) {
                            viewModel.setAITranslation(false)
                        }
                    },
                )
            }
        }
        item(key = "spotify") {
            Column {
                Text(text = stringResource(R.string.spotify), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.log_in_to_spotify),
                    subtitle =
                        if (spotifyLoggedIn) {
                            stringResource(R.string.logged_in)
                        } else {
                            stringResource(R.string.intro_login_to_spotify)
                        },
                    onClick = {
                        if (spotifyLoggedIn) {
                            viewModel.setSpotifyLogIn(false)
                        } else {
                            navController.navigate(SpotifyLoginDestination)
                        }
                    },
                )
                SettingItem(
                    title = stringResource(R.string.enable_spotify_lyrics),
                    subtitle = stringResource(R.string.spotify_lyrícs_info),
                    switch = (spotifyLyrics to { viewModel.setSpotifyLyrics(it) }),
                    isEnable = spotifyLoggedIn,
                    onDisable = {
                        if (spotifyLyrics) {
                            viewModel.setSpotifyLyrics(false)
                        }
                    },
                )
                SettingItem(
                    title = stringResource(R.string.enable_canvas),
                    subtitle = stringResource(R.string.canvas_info),
                    switch = (spotifyCanvas to { viewModel.setSpotifyCanvas(it) }),
                    isEnable = spotifyLoggedIn,
                    onDisable = {
                        if (spotifyCanvas) {
                            viewModel.setSpotifyCanvas(false)
                        }
                    },
                )
            }
        }
        item(key = "sponsor_block") {
            Column {
                Text(text = stringResource(R.string.sponsorBlock), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.enable_sponsor_block),
                    subtitle = stringResource(R.string.skip_sponsor_part_of_video),
                    switch = (enableSponsorBlock to { viewModel.setSponsorBlockEnabled(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.categories_sponsor_block),
                    subtitle = stringResource(R.string.what_segments_will_be_skipped),
                    onClick = {
                        val listName =
                            SPONSOR_BLOCK.listName.map {
                                context.getString(it)
                            }
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.categories_sponsor_block),
                                multipleSelect =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            listName
                                                .mapIndexed { index, item ->
                                                    (
                                                        skipSegments?.contains(
                                                            SPONSOR_BLOCK.list.getOrNull(index),
                                                        ) == true
                                                    ) to item
                                                }.also {
                                                    Log.w("SettingScreen", "SettingAlertState: $skipSegments")
                                                    Log.w("SettingScreen", "SettingAlertState: $it")
                                                },
                                    ),
                                confirm =
                                    context.getString(R.string.save) to { state ->
                                        viewModel.setSponsorBlockCategories(
                                            state.multipleSelect
                                                ?.getListSelected()
                                                ?.map { selected ->
                                                    listName.indexOf(selected)
                                                }?.mapNotNull { s ->
                                                    SPONSOR_BLOCK.list.getOrNull(s).let {
                                                        it?.toString()
                                                    }
                                                }?.toCollection(ArrayList()) ?: arrayListOf(),
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                    isEnable = enableSponsorBlock,
                )
                val beforeUrl = stringResource(R.string.sponsor_block_intro).substringBefore("https://sponsor.ajay.app/")
                val afterUrl = stringResource(R.string.sponsor_block_intro).substringAfter("https://sponsor.ajay.app/")
                Text(
                    buildAnnotatedString {
                        append(beforeUrl)
                        withLink(
                            LinkAnnotation.Url(
                                "https://sponsor.ajay.app/",
                                TextLinkStyles(style = SpanStyle(color = md_theme_dark_primary)),
                            ),
                        ) {
                            append("https://sponsor.ajay.app/")
                        }
                        append(afterUrl)
                    },
                    style = typo.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        }
        item(key = "storage") {
            Column {
                Text(text = stringResource(R.string.storage), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.player_cache),
                    subtitle = "${playerCache.bytesToMB()} MB",
                    onClick = {
                        viewModel.setBasicAlertData(
                            SettingBasicAlertState(
                                title = context.getString(R.string.clear_player_cache),
                                message = null,
                                confirm =
                                    context.getString(R.string.clear) to {
                                        viewModel.clearPlayerCache()
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.downloaded_cache),
                    subtitle = "${downloadedCache.bytesToMB()} MB",
                    onClick = {
                        viewModel.setBasicAlertData(
                            SettingBasicAlertState(
                                title = context.getString(R.string.clear_downloaded_cache),
                                message = null,
                                confirm =
                                    context.getString(R.string.clear) to {
                                        viewModel.clearDownloadedCache()
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.thumbnail_cache),
                    subtitle = "${thumbnailCache.bytesToMB()} MB",
                    onClick = {
                        viewModel.setBasicAlertData(
                            SettingBasicAlertState(
                                title = context.getString(R.string.clear_thumbnail_cache),
                                message = null,
                                confirm =
                                    context.getString(R.string.clear) to {
                                        viewModel.clearThumbnailCache()
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.spotify_canvas_cache),
                    subtitle = "${canvasCache.bytesToMB()} MB",
                    onClick = {
                        viewModel.setBasicAlertData(
                            SettingBasicAlertState(
                                title = context.getString(R.string.clear_canvas_cache),
                                message = null,
                                confirm =
                                    context.getString(R.string.clear) to {
                                        viewModel.clearCanvasCache()
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(R.string.limit_player_cache),
                    subtitle = LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache).toString(),
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = context.getString(R.string.limit_player_cache),
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            LIMIT_CACHE_SIZE.items.map { item ->
                                                (item == LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache)) to item.toString()
                                            },
                                    ),
                                confirm =
                                    context.getString(R.string.change) to { state ->
                                        viewModel.setPlayerCacheLimit(
                                            LIMIT_CACHE_SIZE.getDataFromItem(state.selectOne?.getSelected()),
                                        )
                                    },
                                dismiss = context.getString(R.string.cancel),
                            ),
                        )
                    },
                )
                Box(
                    Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 16.dp,
                    ),
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .onGloballyPositioned { layoutCoordinates ->
                                    with(localDensity) {
                                        width =
                                            layoutCoordinates.size.width
                                                .toDp()
                                                .value
                                                .toInt()
                                    }
                                },
                    ) {
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            (fraction.otherApp * width).dp,
                                        ).background(
                                            md_theme_dark_primary,
                                        ).fillMaxHeight(),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            (fraction.downloadCache * width).dp,
                                        ).background(
                                            Color(0xD540FF17),
                                        ).fillMaxHeight(),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            (fraction.playerCache * width).dp,
                                        ).background(
                                            Color(0xD5FFFF00),
                                        ).fillMaxHeight(),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            (fraction.canvasCache * width).dp,
                                        ).background(
                                            Color.Cyan,
                                        ).fillMaxHeight(),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            (fraction.thumbCache * width).dp,
                                        ).background(
                                            Color.Magenta,
                                        ).fillMaxHeight(),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            (fraction.appDatabase * width).dp,
                                        ).background(
                                            Color.White,
                                        ),
                            )
                        }
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .width(
                                            (fraction.freeSpace * width).dp,
                                        ).background(
                                            Color.DarkGray,
                                        ).fillMaxHeight(),
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                md_theme_dark_primary,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.other_app), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.Green,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.downloaded_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.Yellow,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.player_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.Cyan,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.spotify_canvas_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.Magenta,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.thumbnail_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.White,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.database), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                Color.LightGray,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.free_space), style = typo.bodySmall)
                }
            }
        }
        item(key = "backup") {
            Column {
                Text(text = stringResource(R.string.backup), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.backup_downloaded),
                    subtitle = stringResource(R.string.backup_downloaded_description),
                    switch = (backupDownloaded to { viewModel.setBackupDownloaded(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.backup),
                    subtitle = stringResource(R.string.save_all_your_playlist_data),
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        backupLauncher.launch("${context.getString(R.string.app_name)}_${LocalDateTime.now().format(formatter)}.backup")
                    },
                )
                SettingItem(
                    title = stringResource(R.string.restore_your_data),
                    subtitle = stringResource(R.string.restore_your_saved_data),
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/octet-stream"))
                    },
                )
            }
        }
        item(key = "about_us") {
            Column {
                Text(text = stringResource(R.string.about_us), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.version),
                    subtitle = stringResource(R.string.version_format, VersionManager.getVersionName()),
                    onClick = {
                        navController.navigate(CreditDestination)
                    },
                )
                SettingItem(
                    title = stringResource(R.string.auto_check_for_update),
                    subtitle = stringResource(R.string.auto_check_for_update_description),
                    switch = (autoCheckUpdate to { viewModel.setAutoCheckUpdate(it) }),
                )
                SettingItem(
                    title = stringResource(R.string.check_for_update),
                    subtitle = checkForUpdateSubtitle,
                    onClick = {
                        checkForUpdateSubtitle = context.getString(R.string.checking)
                        sharedViewModel.checkForUpdate()
                    },
                )
                SettingItem(
                    title = stringResource(R.string.author),
                    subtitle = stringResource(R.string.maxrave_dev),
                    onClick = {
                        uriHandler.openUri("https://github.com/maxrave-dev")
                    },
                )
                SettingItem(
                    title = stringResource(R.string.buy_me_a_coffee),
                    subtitle = stringResource(R.string.donation),
                    onClick = {
                        uriHandler.openUri("https://github.com/sponsors/maxrave-dev")
                    },
                )
                SettingItem(
                    title = stringResource(R.string.third_party_libraries),
                    subtitle = stringResource(R.string.description_and_licenses),
                    onClick = {
                        showThirdPartyLibraries = true
                    },
                )
            }
        }
        item(key = "end") {
            EndOfPage()
        }
    }
    val basisAlertData by viewModel.basicAlertData.collectAsStateWithLifecycle()
    if (basisAlertData != null) {
        val alertBasicState = basisAlertData ?: return
        AlertDialog(
            onDismissRequest = { viewModel.setBasicAlertData(null) },
            title = {
                Text(
                    text = alertBasicState.title,
                    style = typo.titleSmall,
                )
            },
            text = {
                if (alertBasicState.message != null) {
                    Text(text = alertBasicState.message)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alertBasicState.confirm.second.invoke()
                        viewModel.setBasicAlertData(null)
                    },
                ) {
                    Text(text = alertBasicState.confirm.first)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setBasicAlertData(null)
                    },
                ) {
                    Text(text = alertBasicState.dismiss)
                }
            },
        )
    }
    if (showYouTubeAccountDialog) {
        BasicAlertDialog(
            onDismissRequest = { },
            modifier = Modifier.wrapContentSize(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = Color(0xFF242424),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                shadowElevation = 1.dp,
            ) {
                val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle(
                    minActiveState = Lifecycle.State.RESUMED,
                )
                LaunchedEffect(googleAccounts) {
                    Log.w(
                        "SettingScreen",
                        "LaunchedEffect: ${
                            googleAccounts.data?.map {
                                it.name to it.isUsed
                            }
                        }",
                    )
                }
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                        ) {
                            IconButton(
                                onClick = { showYouTubeAccountDialog = false },
                                colors =
                                    IconButtonDefaults.iconButtonColors().copy(
                                        contentColor = Color.White,
                                    ),
                                modifier =
                                    Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight(),
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color.White)
                            }
                            Text(
                                stringResource(R.string.youtube_account),
                                style = typo.titleMedium,
                                modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                        .wrapContentWidth(),
                            )
                        }
                    }
                    if (googleAccounts is LocalResource.Success) {
                        val data = googleAccounts.data
                        if (data.isNullOrEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.no_account),
                                    style = typo.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier =
                                        Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                )
                            }
                        } else {
                            items(data) {
                                Row(
                                    modifier =
                                        Modifier
                                            .padding(vertical = 8.dp)
                                            .clickable {
                                                viewModel.setUsedAccount(it)
                                            },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(Modifier.width(24.dp))
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalContext.current)
                                                .data(it.thumbnailUrl)
                                                .crossfade(550)
                                                .build(),
                                        placeholder = painterResource(R.drawable.baseline_people_alt_24),
                                        error = painterResource(R.drawable.baseline_people_alt_24),
                                        contentDescription = it.name,
                                        modifier =
                                            Modifier
                                                .size(48.dp)
                                                .clip(CircleShape),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(it.name, style = typo.labelMedium)
                                        Text(it.email, style = typo.bodySmall)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    AnimatedVisibility(it.isUsed) {
                                        Text(
                                            stringResource(R.string.signed_in),
                                            style = typo.bodySmall,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.widthIn(0.dp, 64.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(24.dp))
                                }
                            }
                        }
                    } else {
                        item {
                            CenterLoadingBox(
                                Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                            )
                        }
                    }
                    item {
                        Column {
                            ActionButton(
                                icon = painterResource(R.drawable.baseline_people_alt_24),
                                text = R.string.guest,
                            ) {
                                viewModel.setUsedAccount(null)
                                showYouTubeAccountDialog = false
                            }
                            ActionButton(
                                icon = painterResource(R.drawable.baseline_close_24),
                                text = R.string.log_out,
                            ) {
                                viewModel.setBasicAlertData(
                                    SettingBasicAlertState(
                                        title = context.getString(R.string.warning),
                                        message = context.getString(R.string.log_out_warning),
                                        confirm =
                                            context.getString(R.string.log_out) to {
                                                viewModel.logOutAllYouTube()
                                                showYouTubeAccountDialog = false
                                            },
                                        dismiss = context.getString(R.string.cancel),
                                    ),
                                )
                            }
                            ActionButton(
                                icon = painterResource(R.drawable.baseline_playlist_add_24),
                                text = R.string.add_an_account,
                            ) {
                                showYouTubeAccountDialog = false
                                navController.navigate(LoginDestination)
                            }
                        }
                    }
                }
            }
        }
    }
    val alertData by viewModel.alertData.collectAsStateWithLifecycle()
    if (alertData != null) {
        val alertState = alertData ?: return
        // AlertDialog
        AlertDialog(
            onDismissRequest = { viewModel.setAlertData(null) },
            title = {
                Text(
                    text = alertState.title,
                    style = typo.titleSmall,
                )
            },
            text = {
                if (alertState.message != null) {
                    Column {
                        Text(text = alertState.message)
                        if (alertState.textField != null) {
                            val verify =
                                alertState.textField.verifyCodeBlock?.invoke(
                                    alertState.textField.value,
                                ) ?: (true to null)
                            TextField(
                                value = alertState.textField.value,
                                onValueChange = {
                                    viewModel.setAlertData(
                                        alertState.copy(
                                            textField =
                                                alertState.textField.copy(
                                                    value = it,
                                                ),
                                        ),
                                    )
                                },
                                isError = !verify.first,
                                label = { Text(text = alertState.textField.label) },
                                supportingText = {
                                    if (!verify.first) {
                                        Text(
                                            modifier = Modifier.fillMaxWidth(),
                                            text = verify.second ?: "",
                                            color = DarkColors.error,
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (!verify.first) {
                                        Icons.Outlined.Error
                                    }
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 6.dp,
                                        ),
                            )
                        }
                    }
                } else if (alertState.selectOne != null) {
                    LazyColumn(
                        Modifier
                            .padding(vertical = 6.dp)
                            .heightIn(0.dp, 500.dp),
                    ) {
                        items(alertState.selectOne.listSelect) { item ->
                            val onSelect = {
                                viewModel.setAlertData(
                                    alertState.copy(
                                        selectOne =
                                            alertState.selectOne.copy(
                                                listSelect =
                                                    alertState.selectOne.listSelect.toMutableList().map {
                                                        if (it == item) {
                                                            true to it.second
                                                        } else {
                                                            false to it.second
                                                        }
                                                    },
                                            ),
                                    ),
                                )
                            }
                            Row(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onSelect.invoke()
                                    }.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = item.first,
                                    onClick = {
                                        onSelect.invoke()
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = item.second, style = typo.bodyMedium, maxLines = 1)
                            }
                        }
                    }
                } else if (alertState.multipleSelect != null) {
                    LazyColumn(
                        Modifier.padding(vertical = 6.dp),
                    ) {
                        items(alertState.multipleSelect.listSelect) { item ->
                            val onCheck = {
                                viewModel.setAlertData(
                                    alertState.copy(
                                        multipleSelect =
                                            alertState.multipleSelect.copy(
                                                listSelect =
                                                    alertState.multipleSelect.listSelect.toMutableList().map {
                                                        if (it == item) {
                                                            !it.first to it.second
                                                        } else {
                                                            it
                                                        }
                                                    },
                                            ),
                                    ),
                                )
                            }
                            Row(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onCheck.invoke()
                                    }.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = item.first,
                                    onCheckedChange = {
                                        onCheck.invoke()
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = item.second, style = typo.bodyMedium, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alertState.confirm.second.invoke(alertState)
                        viewModel.setAlertData(null)
                    },
                    enabled =
                        if (alertState.textField?.verifyCodeBlock != null) {
                            alertState.textField.verifyCodeBlock
                                .invoke(
                                    alertState.textField.value,
                                ).first
                        } else {
                            true
                        },
                ) {
                    Text(text = alertState.confirm.first)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setAlertData(null)
                    },
                ) {
                    Text(text = alertState.dismiss)
                }
            },
        )
    }

    if (showThirdPartyLibraries) {
        val libraries by rememberLibraries(R.raw.aboutlibraries)
        val lazyListState = rememberLazyListState()
        val canScrollBackward by remember {
            derivedStateOf {
                lazyListState.canScrollBackward
            }
        }
        val sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = {
                    !canScrollBackward
                },
            )
        val coroutineScope = rememberCoroutineScope()
        ModalBottomSheet(
            modifier =
                Modifier
                    .fillMaxHeight(),
            onDismissRequest = {
                showThirdPartyLibraries = false
            },
            containerColor = Color.Black,
            dragHandle = {},
            scrimColor = Color.Black,
            sheetState = sheetState,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RectangleShape,
        ) {
            LibrariesContainer(
                libraries,
                Modifier.fillMaxSize(),
                lazyListState = lazyListState,
                showDescription = true,
                contentPadding = innerPadding,
                colors =
                    LibraryDefaults.libraryColors(
                        licenseChipColors =
                            object : ChipColors {
                                override val containerColor: Color
                                    get() = md_theme_dark_outline
                                override val contentColor: Color
                                    get() = Color.White
                            },
                    ),
                header = {
                    item {
                        TopAppBar(
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            title = {
                                Text(
                                    text =
                                        stringResource(
                                            R.string.third_party_libraries,
                                        ),
                                    style = typo.titleMedium,
                                )
                            },
                            navigationIcon = {
                                Box(Modifier.padding(horizontal = 5.dp)) {
                                    RippleIconButton(
                                        R.drawable.baseline_arrow_back_ios_new_24,
                                        Modifier
                                            .size(32.dp),
                                        true,
                                    ) {
                                        coroutineScope.launch {
                                            sheetState.hide()
                                            showThirdPartyLibraries = false
                                        }
                                    }
                                }
                            },
                        )
                    }
                },
            )
        }
    }

    TopAppBar(
        title = {
            Text(
                text =
                    stringResource(
                        R.string.settings,
                    ),
                style = typo.titleMedium,
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(
                    R.drawable.baseline_arrow_back_ios_new_24,
                    Modifier
                        .size(32.dp),
                    true,
                ) {
                    navController.navigateUp()
                }
            }
        },
    )
}