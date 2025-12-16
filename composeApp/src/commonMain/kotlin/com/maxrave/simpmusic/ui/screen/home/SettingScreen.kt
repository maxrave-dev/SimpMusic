package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eygraber.uri.toKmpUri
import com.maxrave.common.LIMIT_CACHE_SIZE
import com.maxrave.common.QUALITY
import com.maxrave.common.SUPPORTED_LANGUAGE
import com.maxrave.common.SUPPORTED_LOCATION
import com.maxrave.common.SponsorBlockType
import com.maxrave.common.VIDEO_QUALITY
import com.maxrave.domain.extension.now
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.utils.LocalResource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.fileSaverResult
import com.maxrave.simpmusic.expect.ui.openEqResult
import com.maxrave.simpmusic.extension.bytesToMB
import com.maxrave.simpmusic.extension.displayString
import com.maxrave.simpmusic.extension.isTwoLetterCode
import com.maxrave.simpmusic.extension.isValidProxyHost
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.ActionButton
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SettingItem
import com.maxrave.simpmusic.ui.navigation.destination.home.CreditDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.DiscordLoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.SpotifyLoginDestination
import com.maxrave.simpmusic.ui.theme.DarkColors
import com.maxrave.simpmusic.ui.theme.md_theme_dark_primary
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.theme.white
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.SettingAlertState
import com.maxrave.simpmusic.viewModel.SettingBasicAlertState
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.ChipColors
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.mohamedrejeb.calf.io.getPath
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.about_us
import simpmusic.composeapp.generated.resources.add_an_account
import simpmusic.composeapp.generated.resources.ai
import simpmusic.composeapp.generated.resources.ai_api_key
import simpmusic.composeapp.generated.resources.ai_provider
import simpmusic.composeapp.generated.resources.anonymous
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.audio
import simpmusic.composeapp.generated.resources.author
import simpmusic.composeapp.generated.resources.auto_check_for_update
import simpmusic.composeapp.generated.resources.auto_check_for_update_description
import simpmusic.composeapp.generated.resources.backup
import simpmusic.composeapp.generated.resources.backup_downloaded
import simpmusic.composeapp.generated.resources.backup_downloaded_description
import simpmusic.composeapp.generated.resources.balance_media_loudness
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.baseline_close_24
import simpmusic.composeapp.generated.resources.baseline_people_alt_24
import simpmusic.composeapp.generated.resources.baseline_playlist_add_24
import simpmusic.composeapp.generated.resources.blur_fullscreen_lyrics
import simpmusic.composeapp.generated.resources.blur_fullscreen_lyrics_description
import simpmusic.composeapp.generated.resources.blur_player_background
import simpmusic.composeapp.generated.resources.blur_player_background_description
import simpmusic.composeapp.generated.resources.buy_me_a_coffee
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.canvas_info
import simpmusic.composeapp.generated.resources.categories_sponsor_block
import simpmusic.composeapp.generated.resources.change
import simpmusic.composeapp.generated.resources.change_language_warning
import simpmusic.composeapp.generated.resources.check_for_update
import simpmusic.composeapp.generated.resources.checking
import simpmusic.composeapp.generated.resources.clear
import simpmusic.composeapp.generated.resources.clear_canvas_cache
import simpmusic.composeapp.generated.resources.clear_downloaded_cache
import simpmusic.composeapp.generated.resources.clear_player_cache
import simpmusic.composeapp.generated.resources.clear_thumbnail_cache
import simpmusic.composeapp.generated.resources.content
import simpmusic.composeapp.generated.resources.content_country
import simpmusic.composeapp.generated.resources.contributor_email
import simpmusic.composeapp.generated.resources.contributor_name
import simpmusic.composeapp.generated.resources.custom_ai_model_id
import simpmusic.composeapp.generated.resources.custom_model_id_messages
import simpmusic.composeapp.generated.resources.database
import simpmusic.composeapp.generated.resources.default_models
import simpmusic.composeapp.generated.resources.description_and_licenses
import simpmusic.composeapp.generated.resources.discord_integration
import simpmusic.composeapp.generated.resources.donation
import simpmusic.composeapp.generated.resources.download_quality
import simpmusic.composeapp.generated.resources.downloaded_cache
import simpmusic.composeapp.generated.resources.enable_canvas
import simpmusic.composeapp.generated.resources.enable_liquid_glass_effect
import simpmusic.composeapp.generated.resources.enable_liquid_glass_effect_description
import simpmusic.composeapp.generated.resources.enable_rich_presence
import simpmusic.composeapp.generated.resources.enable_sponsor_block
import simpmusic.composeapp.generated.resources.enable_spotify_lyrics
import simpmusic.composeapp.generated.resources.free_space
import simpmusic.composeapp.generated.resources.gemini
import simpmusic.composeapp.generated.resources.guest
import simpmusic.composeapp.generated.resources.help_build_lyrics_database
import simpmusic.composeapp.generated.resources.help_build_lyrics_database_description
import simpmusic.composeapp.generated.resources.http
import simpmusic.composeapp.generated.resources.intro_login_to_discord
import simpmusic.composeapp.generated.resources.intro_login_to_spotify
import simpmusic.composeapp.generated.resources.invalid
import simpmusic.composeapp.generated.resources.invalid_api_key
import simpmusic.composeapp.generated.resources.invalid_host
import simpmusic.composeapp.generated.resources.invalid_language_code
import simpmusic.composeapp.generated.resources.invalid_port
import simpmusic.composeapp.generated.resources.keep_service_alive
import simpmusic.composeapp.generated.resources.keep_service_alive_description
import simpmusic.composeapp.generated.resources.keep_your_youtube_playlist_offline
import simpmusic.composeapp.generated.resources.keep_your_youtube_playlist_offline_description
import simpmusic.composeapp.generated.resources.kill_service_on_exit
import simpmusic.composeapp.generated.resources.kill_service_on_exit_description
import simpmusic.composeapp.generated.resources.language
import simpmusic.composeapp.generated.resources.last_checked_at
import simpmusic.composeapp.generated.resources.limit_player_cache
import simpmusic.composeapp.generated.resources.log_in_to_discord
import simpmusic.composeapp.generated.resources.log_in_to_spotify
import simpmusic.composeapp.generated.resources.log_out
import simpmusic.composeapp.generated.resources.log_out_warning
import simpmusic.composeapp.generated.resources.logged_in
import simpmusic.composeapp.generated.resources.lrclib
import simpmusic.composeapp.generated.resources.lyrics
import simpmusic.composeapp.generated.resources.main_lyrics_provider
import simpmusic.composeapp.generated.resources.manage_your_youtube_accounts
import simpmusic.composeapp.generated.resources.maxrave_dev
import simpmusic.composeapp.generated.resources.no_account
import simpmusic.composeapp.generated.resources.normalize_volume
import simpmusic.composeapp.generated.resources.open_system_equalizer
import simpmusic.composeapp.generated.resources.openai
import simpmusic.composeapp.generated.resources.other_app
import simpmusic.composeapp.generated.resources.play_explicit_content
import simpmusic.composeapp.generated.resources.play_explicit_content_description
import simpmusic.composeapp.generated.resources.play_video_for_video_track_instead_of_audio_only
import simpmusic.composeapp.generated.resources.playback
import simpmusic.composeapp.generated.resources.player_cache
import simpmusic.composeapp.generated.resources.proxy
import simpmusic.composeapp.generated.resources.proxy_description
import simpmusic.composeapp.generated.resources.proxy_host
import simpmusic.composeapp.generated.resources.proxy_host_message
import simpmusic.composeapp.generated.resources.proxy_port
import simpmusic.composeapp.generated.resources.proxy_port_message
import simpmusic.composeapp.generated.resources.proxy_type
import simpmusic.composeapp.generated.resources.quality
import simpmusic.composeapp.generated.resources.restore_your_data
import simpmusic.composeapp.generated.resources.restore_your_saved_data
import simpmusic.composeapp.generated.resources.rich_presence_info
import simpmusic.composeapp.generated.resources.save
import simpmusic.composeapp.generated.resources.save_all_your_playlist_data
import simpmusic.composeapp.generated.resources.save_last_played
import simpmusic.composeapp.generated.resources.save_last_played_track_and_queue
import simpmusic.composeapp.generated.resources.save_playback_state
import simpmusic.composeapp.generated.resources.save_shuffle_and_repeat_mode
import simpmusic.composeapp.generated.resources.send_back_listening_data_to_google
import simpmusic.composeapp.generated.resources.set
import simpmusic.composeapp.generated.resources.settings
import simpmusic.composeapp.generated.resources.signed_in
import simpmusic.composeapp.generated.resources.simpmusic_lyrics
import simpmusic.composeapp.generated.resources.skip_no_music_part
import simpmusic.composeapp.generated.resources.skip_silent
import simpmusic.composeapp.generated.resources.skip_sponsor_part_of_video
import simpmusic.composeapp.generated.resources.socks
import simpmusic.composeapp.generated.resources.sponsorBlock
import simpmusic.composeapp.generated.resources.sponsor_block_intro
import simpmusic.composeapp.generated.resources.spotify
import simpmusic.composeapp.generated.resources.spotify_canvas_cache
import simpmusic.composeapp.generated.resources.spotify_lyrícs_info
import simpmusic.composeapp.generated.resources.storage
import simpmusic.composeapp.generated.resources.such_as_music_video_lyrics_video_podcasts_and_more
import simpmusic.composeapp.generated.resources.third_party_libraries
import simpmusic.composeapp.generated.resources.thumbnail_cache
import simpmusic.composeapp.generated.resources.translation_language
import simpmusic.composeapp.generated.resources.translation_language_message
import simpmusic.composeapp.generated.resources.translucent_bottom_navigation_bar
import simpmusic.composeapp.generated.resources.unknown
import simpmusic.composeapp.generated.resources.update_channel
import simpmusic.composeapp.generated.resources.upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in
import simpmusic.composeapp.generated.resources.use_ai_translation
import simpmusic.composeapp.generated.resources.use_ai_translation_description
import simpmusic.composeapp.generated.resources.use_your_system_equalizer
import simpmusic.composeapp.generated.resources.user_interface
import simpmusic.composeapp.generated.resources.version
import simpmusic.composeapp.generated.resources.version_format
import simpmusic.composeapp.generated.resources.video_download_quality
import simpmusic.composeapp.generated.resources.video_quality
import simpmusic.composeapp.generated.resources.warning
import simpmusic.composeapp.generated.resources.what_segments_will_be_skipped
import simpmusic.composeapp.generated.resources.you_can_see_the_content_below_the_bottom_bar
import simpmusic.composeapp.generated.resources.youtube_account
import simpmusic.composeapp.generated.resources.youtube_subtitle_language
import simpmusic.composeapp.generated.resources.youtube_subtitle_language_message
import simpmusic.composeapp.generated.resources.youtube_transcript
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalCoilApi::class,
    ExperimentalHazeMaterialsApi::class,
    FormatStringsInDatetimeFormats::class,
    ExperimentalCalfApi::class,
)
@Composable
fun SettingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val platformContext = LocalPlatformContext.current
    val pl = com.mohamedrejeb.calf.core.LocalPlatformContext.current
    val localDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    var width by rememberSaveable { mutableIntStateOf(0) }

    // Backup and restore
    val formatter =
        LocalDateTime.Format {
            byUnicodePattern("yyyyMMddHHmmss")
        }
    val appName = stringResource(Res.string.app_name)

    val backupLauncher =
        fileSaverResult(
            "${appName}_${
                now().format(
                    formatter,
                )
            }.backup",
            "application/octet-stream",
        ) { uri ->
            uri?.let {
                viewModel.backup(it.toKmpUri())
            }
        }

    val restoreLauncher =
        rememberFilePickerLauncher(
            type =
                FilePickerFileType.All,
            selectionMode = FilePickerSelectionMode.Single,
        ) { file ->
            file.firstOrNull()?.getPath(pl)?.toKmpUri()?.let {
                viewModel.restore(it)
            }
        }

    // Open equalizer
    val resultLauncher = openEqResult(viewModel.getAudioSessionId())

    val enableTranslucentNavBar by viewModel.translucentBottomBar.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val language by viewModel.language.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val quality by viewModel.quality.collectAsStateWithLifecycle()
    val downloadQuality by viewModel.downloadQuality.collectAsStateWithLifecycle()
    val videoDownloadQuality by viewModel.videoDownloadQuality.collectAsStateWithLifecycle()
    val keepYoutubePlaylistOffline by viewModel.keepYouTubePlaylistOffline.collectAsStateWithLifecycle()
    val combineLocalAndYouTubeLiked by viewModel.combineLocalAndYouTubeLiked.collectAsStateWithLifecycle()
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
    val explicitContentEnabled by viewModel.explicitContentEnabled.collectAsStateWithLifecycle()
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
    val updateChannel by viewModel.updateChannel.collectAsStateWithLifecycle()
    val enableLiquidGlass by viewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val discordLoggedIn by viewModel.discordLoggedIn.collectAsStateWithLifecycle()
    val richPresenceEnabled by viewModel.richPresenceEnabled.collectAsStateWithLifecycle()
    val keepServiceAlive by viewModel.keepServiceAlive.collectAsStateWithLifecycle()

    val isCheckingUpdate by sharedViewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    val checkForUpdateSubtitle by remember {
        derivedStateOf {
            if (isCheckingUpdate) {
                return@derivedStateOf runBlocking { getString(Res.string.checking) }
            } else {
                val lastCheckLong = lastCheckUpdate?.toLong() ?: 0L
                return@derivedStateOf runBlocking {
                    getString(
                        Res.string.last_checked_at,
                        DateTimeFormatter
                            .ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(lastCheckLong)),
                    )
                }
            }
        }
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

    LaunchedEffect(true) {
        viewModel.getData()
        viewModel.getThumbCacheSize(platformContext)
    }

    LazyColumn(
        contentPadding = innerPadding,
        modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .hazeSource(hazeState),
    ) {
        item {
            Spacer(Modifier.height(64.dp))
        }
        item(key = "user_interface") {
            Column {
                Spacer(Modifier.height(16.dp))
                Text(text = stringResource(Res.string.user_interface), style = typo().labelMedium, color = white)
                SettingItem(
                    title = stringResource(Res.string.translucent_bottom_navigation_bar),
                    subtitle = stringResource(Res.string.you_can_see_the_content_below_the_bottom_bar),
                    smallSubtitle = true,
                    switch = (enableTranslucentNavBar to { viewModel.setTranslucentBottomBar(it) }),
                )
                SettingItem(
                    title = stringResource(Res.string.blur_fullscreen_lyrics),
                    subtitle = stringResource(Res.string.blur_fullscreen_lyrics_description),
                    smallSubtitle = true,
                    switch = (blurFullscreenLyrics to { viewModel.setBlurFullscreenLyrics(it) }),
                )
                SettingItem(
                    title = stringResource(Res.string.blur_player_background),
                    subtitle = stringResource(Res.string.blur_player_background_description),
                    smallSubtitle = true,
                    switch = (blurPlayerBackground to { viewModel.setBlurPlayerBackground(it) }),
                )
                if (getPlatform() == Platform.Android) {
                    SettingItem(
                        title = stringResource(Res.string.enable_liquid_glass_effect),
                        subtitle = stringResource(Res.string.enable_liquid_glass_effect_description),
                        smallSubtitle = true,
                        switch = (enableLiquidGlass to { viewModel.setEnableLiquidGlass(it) }),
                        isEnable = getPlatform() == Platform.Android,
                    )
                }
            }
        }
        item(key = "content") {
            Column {
                Text(
                    text = stringResource(Res.string.content),
                    style = typo().labelMedium,
                    color = white,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                SettingItem(
                    title = stringResource(Res.string.youtube_account),
                    subtitle = stringResource(Res.string.manage_your_youtube_accounts),
                    onClick = {
                        viewModel.getAllGoogleAccount()
                        showYouTubeAccountDialog = true
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.language),
                    subtitle = SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US"),
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.language) },
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            SUPPORTED_LANGUAGE.items.map {
                                                (it.toString() == SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US")) to it.toString()
                                            },
                                    ),
                                confirm =
                                    runBlocking { getString(Res.string.change) } to { state ->
                                        val code = SUPPORTED_LANGUAGE.getCodeFromLanguage(state.selectOne?.getSelected() ?: "English")
                                        viewModel.setBasicAlertData(
                                            SettingBasicAlertState(
                                                title = runBlocking { getString(Res.string.warning) },
                                                message = runBlocking { getString(Res.string.change_language_warning) },
                                                confirm =
                                                    runBlocking { getString(Res.string.change) } to {
                                                        sharedViewModel.activityRecreate()
                                                        viewModel.setBasicAlertData(null)
                                                        viewModel.changeLanguage(code)
                                                    },
                                                dismiss = runBlocking { getString(Res.string.cancel) },
                                            ),
                                        )
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.content_country),
                    subtitle = location ?: "",
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.content_country) },
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            SUPPORTED_LOCATION.items.map { item ->
                                                (item.toString() == location) to item.toString()
                                            },
                                    ),
                                confirm =
                                    runBlocking { getString(Res.string.change) } to { state ->
                                        viewModel.changeLocation(
                                            state.selectOne?.getSelected() ?: "US",
                                        )
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.quality),
                    subtitle = quality ?: "",
                    smallSubtitle = true,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.quality) },
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            QUALITY.items.map { item ->
                                                (item.toString() == quality) to item.toString()
                                            },
                                    ),
                                confirm =
                                    runBlocking { getString(Res.string.change) } to { state ->
                                        viewModel.changeQuality(state.selectOne?.getSelected())
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.download_quality),
                    subtitle = downloadQuality ?: "",
                    smallSubtitle = true,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.download_quality) },
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            QUALITY.items.map { item ->
                                                (item.toString() == downloadQuality) to item.toString()
                                            },
                                    ),
                                confirm =
                                    runBlocking { getString(Res.string.change) } to { state ->
                                        state.selectOne?.getSelected()?.let { viewModel.setDownloadQuality(it) }
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
                if (getPlatform() != Platform.Desktop) {
                    SettingItem(
                        title = stringResource(Res.string.play_video_for_video_track_instead_of_audio_only),
                        subtitle = stringResource(Res.string.such_as_music_video_lyrics_video_podcasts_and_more),
                        smallSubtitle = true,
                        switch = (playVideo to { viewModel.setPlayVideoInsteadOfAudio(it) }),
                        isEnable = getPlatform() != Platform.Desktop,
                    )
                    SettingItem(
                        title = stringResource(Res.string.video_quality),
                        subtitle = videoQuality ?: "",
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.video_quality) },
                                    selectOne =
                                        SettingAlertState.SelectData(
                                            listSelect =
                                                VIDEO_QUALITY.items.map { item ->
                                                    (item.toString() == videoQuality) to item.toString()
                                                },
                                        ),
                                    confirm =
                                        runBlocking { getString(Res.string.change) } to { state ->
                                            viewModel.changeVideoQuality(state.selectOne?.getSelected() ?: "")
                                        },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                ),
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.video_download_quality),
                        subtitle = videoDownloadQuality ?: "",
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.video_download_quality) },
                                    selectOne =
                                        SettingAlertState.SelectData(
                                            listSelect =
                                                VIDEO_QUALITY.items.map { item ->
                                                    (item.toString() == videoDownloadQuality) to item.toString()
                                                },
                                        ),
                                    confirm =
                                        runBlocking { getString(Res.string.change) } to { state ->
                                            viewModel.setVideoDownloadQuality(state.selectOne?.getSelected() ?: "")
                                        },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                ),
                            )
                        },
                    )
                }
                SettingItem(
                    title = stringResource(Res.string.send_back_listening_data_to_google),
                    subtitle =
                        stringResource(
                            Res.string
                                .upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in,
                        ),
                    smallSubtitle = true,
                    switch = (sendData to { viewModel.setSendBackToGoogle(it) }),
                )
                SettingItem(
                    title = stringResource(Res.string.play_explicit_content),
                    subtitle = stringResource(Res.string.play_explicit_content_description),
                    switch = (explicitContentEnabled to { viewModel.setExplicitContentEnabled(it) }),
                )
                SettingItem(
                    title = stringResource(Res.string.keep_your_youtube_playlist_offline),
                    subtitle = stringResource(Res.string.keep_your_youtube_playlist_offline_description),
                    switch = (keepYoutubePlaylistOffline to { viewModel.setKeepYouTubePlaylistOffline(it) }),
                )
                /*
                SettingItem(
                    title = stringResource(Res.string.combine_local_and_youtube_liked_songs),
                    subtitle = stringResource(Res.string.combine_local_and_youtube_liked_songs_description),
                    switch = (combineLocalAndYouTubeLiked to { viewModel.setCombineLocalAndYouTubeLiked(it) })
                )
                 */
                SettingItem(
                    title = stringResource(Res.string.proxy),
                    subtitle = stringResource(Res.string.proxy_description),
                    switch = (usingProxy to { viewModel.setUsingProxy(it) }),
                )
            }
        }
        item(key = "proxy") {
            Crossfade(usingProxy) { it ->
                if (it) {
                    Column {
                        SettingItem(
                            title = stringResource(Res.string.proxy_type),
                            subtitle =
                                when (proxyType) {
                                    DataStoreManager.ProxyType.PROXY_TYPE_HTTP -> stringResource(Res.string.http)
                                    DataStoreManager.ProxyType.PROXY_TYPE_SOCKS -> stringResource(Res.string.socks)
                                },
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.proxy_type) },
                                        selectOne =
                                            SettingAlertState.SelectData(
                                                listSelect =
                                                    listOf(
                                                        (proxyType == DataStoreManager.ProxyType.PROXY_TYPE_HTTP) to
                                                            runBlocking {
                                                                getString(
                                                                    Res.string.http,
                                                                )
                                                            },
                                                        (proxyType == DataStoreManager.ProxyType.PROXY_TYPE_SOCKS) to
                                                            runBlocking { getString(Res.string.socks) },
                                                    ),
                                            ),
                                        confirm =
                                            runBlocking { getString(Res.string.change) } to { state ->
                                                viewModel.setProxy(
                                                    if (state.selectOne?.getSelected() == runBlocking { getString(Res.string.socks) }) {
                                                        DataStoreManager.ProxyType.PROXY_TYPE_SOCKS
                                                    } else {
                                                        DataStoreManager.ProxyType.PROXY_TYPE_HTTP
                                                    },
                                                    proxyHost,
                                                    proxyPort,
                                                )
                                            },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        SettingItem(
                            title = stringResource(Res.string.proxy_host),
                            subtitle = proxyHost,
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.proxy_host) },
                                        message = runBlocking { getString(Res.string.proxy_host_message) },
                                        textField =
                                            SettingAlertState.TextFieldData(
                                                label = runBlocking { getString(Res.string.proxy_host) },
                                                value = proxyHost,
                                                verifyCodeBlock = {
                                                    isValidProxyHost(it) to runBlocking { getString(Res.string.invalid_host) }
                                                },
                                            ),
                                        confirm =
                                            runBlocking { getString(Res.string.change) } to { state ->
                                                viewModel.setProxy(
                                                    proxyType,
                                                    state.textField?.value ?: "",
                                                    proxyPort,
                                                )
                                            },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                        SettingItem(
                            title = stringResource(Res.string.proxy_port),
                            subtitle = proxyPort.toString(),
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.proxy_port) },
                                        message = runBlocking { getString(Res.string.proxy_port_message) },
                                        textField =
                                            SettingAlertState.TextFieldData(
                                                label = runBlocking { getString(Res.string.proxy_port) },
                                                value = proxyPort.toString(),
                                                verifyCodeBlock = {
                                                    (it.toIntOrNull() != null) to runBlocking { getString(Res.string.invalid_port) }
                                                },
                                            ),
                                        confirm =
                                            runBlocking { getString(Res.string.change) } to { state ->
                                                viewModel.setProxy(
                                                    proxyType,
                                                    proxyHost,
                                                    state.textField?.value?.toIntOrNull() ?: 0,
                                                )
                                            },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
        if (getPlatform() == Platform.Android) {
            item(key = "audio") {
                Column {
                    Text(
                        text = stringResource(Res.string.audio),
                        style = typo().labelMedium,
                        color = white,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    SettingItem(
                        title = stringResource(Res.string.normalize_volume),
                        subtitle = stringResource(Res.string.balance_media_loudness),
                        switch = (normalizeVolume to { viewModel.setNormalizeVolume(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.skip_silent),
                        subtitle = stringResource(Res.string.skip_no_music_part),
                        switch = (skipSilent to { viewModel.setSkipSilent(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.open_system_equalizer),
                        subtitle = stringResource(Res.string.use_your_system_equalizer),
                        onClick = {
                            coroutineScope.launch {
                                resultLauncher.launch()
                            }
                        },
                    )
                }
            }
        }
        item(key = "playback") {
            Column {
                Text(
                    text = stringResource(Res.string.playback),
                    style = typo().labelMedium,
                    color = white,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                SettingItem(
                    title = stringResource(Res.string.save_playback_state),
                    subtitle = stringResource(Res.string.save_shuffle_and_repeat_mode),
                    switch = (savePlaybackState to { viewModel.setSavedPlaybackState(it) }),
                )
                SettingItem(
                    title = stringResource(Res.string.save_last_played),
                    subtitle = stringResource(Res.string.save_last_played_track_and_queue),
                    switch = (saveLastPlayed to { viewModel.setSaveLastPlayed(it) }),
                )
                if (getPlatform() == Platform.Android) {
                    SettingItem(
                        title = stringResource(Res.string.kill_service_on_exit),
                        subtitle = stringResource(Res.string.kill_service_on_exit_description),
                        switch = (killServiceOnExit to { viewModel.setKillServiceOnExit(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.keep_service_alive),
                        subtitle = stringResource(Res.string.keep_service_alive_description),
                        switch = (keepServiceAlive to { viewModel.setKeepServiceAlive(it) }),
                    )
                }
            }
        }
        item(key = "lyrics") {
            Column {
                Text(
                    text = stringResource(Res.string.lyrics),
                    style = typo().labelMedium,
                    color = white,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                SettingItem(
                    title = stringResource(Res.string.main_lyrics_provider),
                    subtitle =
                        when (mainLyricsProvider) {
                            DataStoreManager.SIMPMUSIC -> stringResource(Res.string.simpmusic_lyrics)
                            DataStoreManager.YOUTUBE -> stringResource(Res.string.youtube_transcript)
                            DataStoreManager.LRCLIB -> stringResource(Res.string.lrclib)
                            else -> stringResource(Res.string.unknown)
                        },
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.main_lyrics_provider) },
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            listOf(
                                                (mainLyricsProvider == DataStoreManager.SIMPMUSIC) to
                                                    runBlocking { getString(Res.string.simpmusic_lyrics) },
                                                (mainLyricsProvider == DataStoreManager.YOUTUBE) to
                                                    runBlocking { getString(Res.string.youtube_transcript) },
                                                (mainLyricsProvider == DataStoreManager.LRCLIB) to runBlocking { getString(Res.string.lrclib) },
                                            ),
                                    ),
                                confirm =
                                    runBlocking { getString(Res.string.change) } to { state ->
                                        viewModel.setLyricsProvider(
                                            when (state.selectOne?.getSelected()) {
                                                runBlocking { getString(Res.string.simpmusic_lyrics) } -> DataStoreManager.SIMPMUSIC
                                                runBlocking { getString(Res.string.youtube_transcript) } -> DataStoreManager.YOUTUBE
                                                runBlocking { getString(Res.string.lrclib) } -> DataStoreManager.LRCLIB
                                                else -> DataStoreManager.SIMPMUSIC
                                            },
                                        )
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )

                SettingItem(
                    title = stringResource(Res.string.translation_language),
                    subtitle = translationLanguage ?: "",
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.translation_language) },
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = runBlocking { getString(Res.string.translation_language) },
                                        value = translationLanguage ?: "",
                                        verifyCodeBlock = {
                                            (it.length == 2 && it.isTwoLetterCode()) to
                                                runBlocking { getString(Res.string.invalid_language_code) }
                                        },
                                    ),
                                message = runBlocking { getString(Res.string.translation_language_message) },
                                confirm =
                                    runBlocking { getString(Res.string.change) } to { state ->
                                        viewModel.setTranslationLanguage(state.textField?.value ?: "")
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                    isEnable = useAITranslation,
                )
                SettingItem(
                    title = stringResource(Res.string.youtube_subtitle_language),
                    subtitle = youtubeSubtitleLanguage,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.youtube_subtitle_language) },
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = runBlocking { getString(Res.string.youtube_subtitle_language) },
                                        value = youtubeSubtitleLanguage,
                                        verifyCodeBlock = {
                                            (it.length == 2 && it.isTwoLetterCode()) to
                                                runBlocking { getString(Res.string.invalid_language_code) }
                                        },
                                    ),
                                message = runBlocking { getString(Res.string.youtube_subtitle_language_message) },
                                confirm =
                                    runBlocking { getString(Res.string.change) } to { state ->
                                        viewModel.setYoutubeSubtitleLanguage(state.textField?.value ?: "")
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.help_build_lyrics_database),
                    subtitle = stringResource(Res.string.help_build_lyrics_database_description),
                    switch = (helpBuildLyricsDatabase to { viewModel.setHelpBuildLyricsDatabase(it) }),
                )
                SettingItem(
                    title = stringResource(Res.string.contributor_name),
                    subtitle = contributor.first.ifEmpty { stringResource(Res.string.anonymous) },
                    isEnable = helpBuildLyricsDatabase,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.contributor_name) },
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = runBlocking { getString(Res.string.contributor_name) },
                                        value = "",
                                    ),
                                message = "",
                                confirm =
                                    runBlocking { getString(Res.string.set) } to { state ->
                                        viewModel.setContributorName(state.textField?.value ?: "")
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.contributor_email),
                    subtitle = contributor.second.ifEmpty { stringResource(Res.string.anonymous) },
                    isEnable = helpBuildLyricsDatabase,
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.contributor_email) },
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = runBlocking { getString(Res.string.contributor_email) },
                                        value = "",
                                        verifyCodeBlock = {
                                            if (it.isNotEmpty()) {
                                                (it.contains("@")) to runBlocking { getString(Res.string.invalid) }
                                            } else {
                                                true to ""
                                            }
                                        },
                                    ),
                                message = "",
                                confirm =
                                    runBlocking { getString(Res.string.set) } to { state ->
                                        viewModel.setContributorEmail(state.textField?.value ?: "")
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
            }
        }
        item(key = "AI") {
            Column {
                Text(text = stringResource(Res.string.ai), style = typo().labelMedium, color = white, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(Res.string.ai_provider),
                    subtitle =
                        when (aiProvider) {
                            DataStoreManager.AI_PROVIDER_OPENAI -> stringResource(Res.string.openai)
                            DataStoreManager.AI_PROVIDER_GEMINI -> stringResource(Res.string.gemini)
                            else -> stringResource(Res.string.unknown)
                        },
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.ai_provider) },
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            listOf(
                                                (mainLyricsProvider == DataStoreManager.AI_PROVIDER_OPENAI) to
                                                    runBlocking { getString(Res.string.openai) },
                                                (mainLyricsProvider == DataStoreManager.AI_PROVIDER_GEMINI) to
                                                    runBlocking { getString(Res.string.gemini) },
                                            ),
                                    ),
                                confirm =
                                    runBlocking { getString(Res.string.change) } to { state ->
                                        viewModel.setAIProvider(
                                            when (state.selectOne?.getSelected()) {
                                                runBlocking { getString(Res.string.openai) } -> DataStoreManager.AI_PROVIDER_OPENAI
                                                runBlocking { getString(Res.string.gemini) } -> DataStoreManager.AI_PROVIDER_GEMINI
                                                else -> DataStoreManager.AI_PROVIDER_OPENAI
                                            },
                                        )
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.ai_api_key),
                    subtitle = if (isHasApiKey) "XXXXXXXXXX" else "N/A",
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.ai_api_key) },
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = runBlocking { getString(Res.string.ai_api_key) },
                                        value = "",
                                        verifyCodeBlock = {
                                            (it.isNotEmpty()) to runBlocking { getString(Res.string.invalid_api_key) }
                                        },
                                    ),
                                message = "",
                                confirm =
                                    runBlocking { getString(Res.string.set) } to { state ->
                                        viewModel.setAIApiKey(state.textField?.value ?: "")
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.custom_ai_model_id),
                    subtitle = customModelId.ifEmpty { stringResource(Res.string.default_models) },
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.custom_ai_model_id) },
                                textField =
                                    SettingAlertState.TextFieldData(
                                        label = runBlocking { getString(Res.string.custom_ai_model_id) },
                                        value = "",
                                        verifyCodeBlock = {
                                            (it.isNotEmpty() && !it.contains(" ")) to runBlocking { getString(Res.string.invalid) }
                                        },
                                    ),
                                message = runBlocking { getString(Res.string.custom_model_id_messages) },
                                confirm =
                                    runBlocking { getString(Res.string.set) } to { state ->
                                        viewModel.setCustomModelId(state.textField?.value ?: "")
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.use_ai_translation),
                    subtitle = stringResource(Res.string.use_ai_translation_description),
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
                Text(
                    text = stringResource(Res.string.spotify),
                    style = typo().labelMedium,
                    color = white,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                SettingItem(
                    title = stringResource(Res.string.log_in_to_spotify),
                    subtitle =
                        if (spotifyLoggedIn) {
                            stringResource(Res.string.logged_in)
                        } else {
                            stringResource(Res.string.intro_login_to_spotify)
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
                    title = stringResource(Res.string.enable_spotify_lyrics),
                    subtitle = stringResource(Res.string.spotify_lyrícs_info),
                    switch = (spotifyLyrics to { viewModel.setSpotifyLyrics(it) }),
                    isEnable = spotifyLoggedIn,
                    onDisable = {
                        if (spotifyLyrics) {
                            viewModel.setSpotifyLyrics(false)
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.enable_canvas),
                    subtitle = stringResource(Res.string.canvas_info),
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
        item(key = "discord") {
            Column {
                Text(
                    text = stringResource(Res.string.discord_integration),
                    style = typo().labelMedium,
                    color = white,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                SettingItem(
                    title = stringResource(Res.string.log_in_to_discord),
                    subtitle =
                        if (discordLoggedIn) {
                            stringResource(Res.string.logged_in)
                        } else {
                            stringResource(Res.string.intro_login_to_discord)
                        },
                    onClick = {
                        if (discordLoggedIn) {
                            viewModel.logOutDiscord()
                        } else {
                            navController.navigate(DiscordLoginDestination)
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.enable_rich_presence),
                    subtitle = stringResource(Res.string.rich_presence_info),
                    switch = (richPresenceEnabled to { viewModel.setDiscordRichPresenceEnabled(it) }),
                    isEnable = discordLoggedIn,
                    onDisable = {
                        if (discordLoggedIn) {
                            viewModel.setDiscordRichPresenceEnabled(false)
                        }
                    },
                )
            }
        }
        item(key = "sponsor_block") {
            Column {
                Text(
                    text = stringResource(Res.string.sponsorBlock),
                    style = typo().labelMedium,
                    color = white,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                SettingItem(
                    title = stringResource(Res.string.enable_sponsor_block),
                    subtitle = stringResource(Res.string.skip_sponsor_part_of_video),
                    switch = (enableSponsorBlock to { viewModel.setSponsorBlockEnabled(it) }),
                )
                val listName =
                    SponsorBlockType.toList().map { it.displayString() }
                SettingItem(
                    title = stringResource(Res.string.categories_sponsor_block),
                    subtitle = stringResource(Res.string.what_segments_will_be_skipped),
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.categories_sponsor_block) },
                                multipleSelect =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            listName
                                                .mapIndexed { index, item ->
                                                    (
                                                        skipSegments?.contains(
                                                            SponsorBlockType.toList().getOrNull(index)?.value,
                                                        ) == true
                                                    ) to item
                                                }.also {
                                                    Logger.w("SettingScreen", "SettingAlertState: $skipSegments")
                                                    Logger.w("SettingScreen", "SettingAlertState: $it")
                                                },
                                    ),
                                confirm =
                                    runBlocking { getString(Res.string.save) } to { state ->
                                        viewModel.setSponsorBlockCategories(
                                            state.multipleSelect
                                                ?.getListSelected()
                                                ?.map { selected ->
                                                    listName.indexOf(selected)
                                                }?.mapNotNull { s ->
                                                    SponsorBlockType.toList().getOrNull(s).let {
                                                        it?.value
                                                    }
                                                }?.toCollection(ArrayList()) ?: arrayListOf(),
                                        )
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                    isEnable = enableSponsorBlock,
                )
                val beforeUrl = stringResource(Res.string.sponsor_block_intro).substringBefore("https://sponsor.ajay.app/")
                val afterUrl = stringResource(Res.string.sponsor_block_intro).substringAfter("https://sponsor.ajay.app/")
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
                    style = typo().bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        }
        if (getPlatform() == Platform.Android) {
            item(key = "storage") {
                Column {
                    Text(
                        text = stringResource(Res.string.storage),
                        style = typo().labelMedium,
                        color = white,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    SettingItem(
                        title = stringResource(Res.string.player_cache),
                        subtitle = "${playerCache.bytesToMB()} MB",
                        onClick = {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = runBlocking { getString(Res.string.clear_player_cache) },
                                    message = null,
                                    confirm =
                                        runBlocking { getString(Res.string.clear) } to {
                                            viewModel.clearPlayerCache()
                                        },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                ),
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.downloaded_cache),
                        subtitle = "${downloadedCache.bytesToMB()} MB",
                        onClick = {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = runBlocking { getString(Res.string.clear_downloaded_cache) },
                                    message = null,
                                    confirm =
                                        runBlocking { getString(Res.string.clear) } to {
                                            viewModel.clearDownloadedCache()
                                        },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                ),
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.thumbnail_cache),
                        subtitle = "${thumbnailCache.bytesToMB()} MB",
                        onClick = {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = runBlocking { getString(Res.string.clear_thumbnail_cache) },
                                    message = null,
                                    confirm =
                                        runBlocking { getString(Res.string.clear) } to {
                                            viewModel.clearThumbnailCache(platformContext)
                                        },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                ),
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.spotify_canvas_cache),
                        subtitle = "${canvasCache.bytesToMB()} MB",
                        onClick = {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = runBlocking { getString(Res.string.clear_canvas_cache) },
                                    message = null,
                                    confirm =
                                        runBlocking { getString(Res.string.clear) } to {
                                            viewModel.clearCanvasCache()
                                        },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                ),
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.limit_player_cache),
                        subtitle = LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache).toString(),
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.limit_player_cache) },
                                    selectOne =
                                        SettingAlertState.SelectData(
                                            listSelect =
                                                LIMIT_CACHE_SIZE.items.map { item ->
                                                    (item == LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache)) to item.toString()
                                                },
                                        ),
                                    confirm =
                                        runBlocking { getString(Res.string.change) } to { state ->
                                            viewModel.setPlayerCacheLimit(
                                                LIMIT_CACHE_SIZE.getDataFromItem(state.selectOne?.getSelected()),
                                            )
                                        },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
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
                        Text(text = stringResource(Res.string.other_app), style = typo().bodySmall)
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
                        Text(text = stringResource(Res.string.downloaded_cache), style = typo().bodySmall)
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
                        Text(text = stringResource(Res.string.player_cache), style = typo().bodySmall)
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
                        Text(text = stringResource(Res.string.spotify_canvas_cache), style = typo().bodySmall)
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
                        Text(text = stringResource(Res.string.thumbnail_cache), style = typo().bodySmall)
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
                        Text(text = stringResource(Res.string.database), style = typo().bodySmall)
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
                        Text(text = stringResource(Res.string.free_space), style = typo().bodySmall)
                    }
                }
            }
        }
        item(key = "backup") {
            Column {
                Text(
                    text = stringResource(Res.string.backup),
                    style = typo().labelMedium,
                    color = white,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                SettingItem(
                    title = stringResource(Res.string.backup_downloaded),
                    subtitle = stringResource(Res.string.backup_downloaded_description),
                    switch = (backupDownloaded to { viewModel.setBackupDownloaded(it) }),
                )
                SettingItem(
                    title = stringResource(Res.string.backup),
                    subtitle = stringResource(Res.string.save_all_your_playlist_data),
                    onClick = {
                        coroutineScope.launch {
                            backupLauncher.launch()
                        }
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.restore_your_data),
                    subtitle = stringResource(Res.string.restore_your_saved_data),
                    onClick = {
                        coroutineScope.launch {
                            restoreLauncher.launch()
                        }
                    },
                )
            }
        }
        item(key = "about_us") {
            Column {
                Text(
                    text = stringResource(Res.string.about_us),
                    style = typo().labelMedium,
                    color = white,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                SettingItem(
                    title = stringResource(Res.string.version),
                    subtitle = stringResource(Res.string.version_format, VersionManager.getVersionName()),
                    onClick = {
                        navController.navigate(CreditDestination)
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.auto_check_for_update),
                    subtitle = stringResource(Res.string.auto_check_for_update_description),
                    switch = (autoCheckUpdate to { viewModel.setAutoCheckUpdate(it) }),
                )
                SettingItem(
                    title = stringResource(Res.string.update_channel),
                    subtitle =
                        if (updateChannel == DataStoreManager.FDROID) {
                            "F-Droid"
                        } else {
                            "SimpMusic GitHub Release"
                        },
                    onClick = {
                        viewModel.setAlertData(
                            SettingAlertState(
                                title = runBlocking { getString(Res.string.update_channel) },
                                selectOne =
                                    SettingAlertState.SelectData(
                                        listSelect =
                                            listOf(
                                                (updateChannel == DataStoreManager.FDROID) to "F-Droid",
                                                (updateChannel == DataStoreManager.GITHUB) to "SimpMusic GitHub Release",
                                            ),
                                    ),
                                confirm =
                                    runBlocking { getString(Res.string.change) } to { state ->
                                        viewModel.setUpdateChannel(
                                            when (state.selectOne?.getSelected()) {
                                                "F-Droid" -> DataStoreManager.FDROID
                                                "SimpMusic GitHub Release" -> DataStoreManager.GITHUB
                                                else -> DataStoreManager.GITHUB
                                            },
                                        )
                                    },
                                dismiss = runBlocking { getString(Res.string.cancel) },
                            ),
                        )
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.check_for_update),
                    subtitle = checkForUpdateSubtitle,
                    onClick = {
                        sharedViewModel.checkForUpdate()
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.author),
                    subtitle = stringResource(Res.string.maxrave_dev),
                    onClick = {
                        uriHandler.openUri("https://github.com/maxrave-dev")
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.buy_me_a_coffee),
                    subtitle = stringResource(Res.string.donation),
                    onClick = {
                        uriHandler.openUri("https://github.com/sponsors/maxrave-dev")
                    },
                )
                SettingItem(
                    title = stringResource(Res.string.third_party_libraries),
                    subtitle = stringResource(Res.string.description_and_licenses),
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
                    style = typo().titleSmall,
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
                    Logger.w(
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
                                stringResource(Res.string.youtube_account),
                                style = typo().titleMedium,
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
                                    stringResource(Res.string.no_account),
                                    style = typo().bodyMedium,
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
                                                .Builder(LocalPlatformContext.current)
                                                .data(it.thumbnailUrl)
                                                .crossfade(550)
                                                .build(),
                                        placeholder = painterResource(Res.drawable.baseline_people_alt_24),
                                        error = painterResource(Res.drawable.baseline_people_alt_24),
                                        contentDescription = it.name,
                                        modifier =
                                            Modifier
                                                .size(48.dp)
                                                .clip(CircleShape),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(it.name, style = typo().labelMedium, color = white)
                                        Text(it.email, style = typo().bodySmall)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    AnimatedVisibility(it.isUsed) {
                                        Text(
                                            stringResource(Res.string.signed_in),
                                            style = typo().bodySmall,
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
                                    .height(80.dp),
                            )
                        }
                    }
                    item {
                        Column {
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_people_alt_24),
                                text = Res.string.guest,
                            ) {
                                viewModel.setUsedAccount(null)
                                showYouTubeAccountDialog = false
                            }
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_close_24),
                                text = Res.string.log_out,
                            ) {
                                viewModel.setBasicAlertData(
                                    SettingBasicAlertState(
                                        title = runBlocking { getString(Res.string.warning) },
                                        message = runBlocking { getString(Res.string.log_out_warning) },
                                        confirm =
                                            runBlocking { getString(Res.string.log_out) } to {
                                                viewModel.logOutAllYouTube()
                                                showYouTubeAccountDialog = false
                                            },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    ),
                                )
                            }
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_playlist_add_24),
                                text = Res.string.add_an_account,
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
                    style = typo().titleSmall,
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
                                Text(
                                    text = item.second,
                                    style = typo().bodyMedium,
                                    maxLines = 1,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                            .basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                animationMode = MarqueeAnimationMode.Immediately,
                                            ).focusable(),
                                )
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
                                Text(text = item.second, style = typo().bodyMedium, maxLines = 1)
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
        val libraries by produceLibraries {
            Res.readBytes("files/aboutlibraries.json").decodeToString()
        }
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
                libraries?.copy(
                    libraries =
                        libraries
                            ?.libraries
                            ?.distinctBy {
                                it.name
                            }?.toImmutableList() ?: emptyList<Library>().toImmutableList(),
                ),
                Modifier.fillMaxSize(),
                lazyListState = lazyListState,
                showDescription = true,
                contentPadding = innerPadding,
                typography = typo(),
                colors =
                    LibraryDefaults.libraryColors(
                        licenseChipColors =
                            object : ChipColors {
                                override val containerColor: Color
                                    get() = Color.DarkGray
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
                                            Res.string.third_party_libraries,
                                        ),
                                    style = typo().titleMedium,
                                )
                            },
                            navigationIcon = {
                                Box(Modifier.padding(horizontal = 5.dp)) {
                                    RippleIconButton(
                                        Res.drawable.baseline_arrow_back_ios_new_24,
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
                        Res.string.settings,
                    ),
                style = typo().titleMedium,
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(
                    Res.drawable.baseline_arrow_back_ios_new_24,
                    Modifier
                        .size(32.dp),
                    true,
                ) {
                    navController.navigateUp()
                }
            }
        },
        modifier =
            Modifier
                .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                    blurEnabled = true
                },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
    )
}