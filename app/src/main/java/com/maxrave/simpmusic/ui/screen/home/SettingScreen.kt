package com.maxrave.simpmusic.ui.screen.home

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.TRUE
import com.maxrave.simpmusic.extension.bytesToMB
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SettingItem
import com.maxrave.simpmusic.ui.theme.md_theme_dark_primary
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@UnstableApi
@Composable
fun SettingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
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
    val mainLyricsProvider by viewModel.mainLyricsProvider.collectAsStateWithLifecycle()
    val musixmatchLoggedIn by viewModel.musixmatchLoggedIn.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val useMusixmatchTranslation by viewModel.useTranslation.map { it == TRUE }.collectAsStateWithLifecycle(initialValue = false)
    val musixmatchTranslationLanguage by viewModel.translationLanguage.collectAsStateWithLifecycle()
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

    LaunchedEffect(true) {
        viewModel.getData()
    }

    LazyColumn(
        contentPadding = innerPadding,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 64.dp)
    ) {
        item(key = "user_interface") {
            Column {
                Spacer(Modifier.height(16.dp))
                Text(text = stringResource(R.string.user_interface), style = typo.labelMedium)
                SettingItem(
                    title = stringResource(R.string.translucent_bottom_navigation_bar),
                    subtitle = stringResource(R.string.you_can_see_the_content_below_the_bottom_bar),
                    smallSubtitle = true,
                    switch = (enableTranslucentNavBar to { viewModel.setTranslucentBottomBar(it) })
                )
            }
        }
        item(key = "content") {
            Column {
                Text(text = stringResource(R.string.content), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.youtube_account),
                    subtitle = stringResource(R.string.manage_your_youtube_accounts),
                    onClick = {}
                )
                SettingItem(
                    title = stringResource(R.string.language),
                    subtitle = language ?: "",
                    onClick = {}
                )
                SettingItem(
                    title = stringResource(R.string.content_country),
                    subtitle = location ?: "",
                    onClick = {}
                )
                SettingItem(
                    title = stringResource(R.string.quality),
                    subtitle = quality ?: "",
                    smallSubtitle = true,
                    onClick = {}
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
                    switch = (playVideo to { viewModel.setPlayVideoInsteadOfAudio(it) })
                )
                SettingItem(
                    title = stringResource(R.string.video_quality),
                    subtitle = videoQuality ?: "",
                )
                SettingItem(
                    title = stringResource(R.string.send_back_listening_data_to_google),
                    subtitle = stringResource(
                        R.string.
                        upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in
                    ),
                    smallSubtitle = true,
                    switch = (sendData to { viewModel.setSendBackToGoogle(it) })
                )
            }
        }
        item(key = "audio") {
            Column {
                Text(text = stringResource(R.string.audio), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.normalize_volume),
                    subtitle = stringResource(R.string.balance_media_loudness),
                    switch = (normalizeVolume to { viewModel.setNormalizeVolume(it) })
                )
                SettingItem(
                    title = stringResource(R.string.skip_silent),
                    subtitle = stringResource(R.string.skip_no_music_part),
                    switch = (skipSilent to { viewModel.setSkipSilent(it) })
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
                    }
                )
            }
        }
        item(key = "playback") {
            Column {
                Text(text = stringResource(R.string.playback), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.save_playback_state),
                    subtitle = stringResource(R.string.save_shuffle_and_repeat_mode),
                    switch = (savePlaybackState to { viewModel.setSavedPlaybackState(it) })
                )
                SettingItem(
                    title = stringResource(R.string.save_last_played),
                    subtitle = stringResource(R.string.save_last_played_track_and_queue),
                    switch = (saveLastPlayed to { viewModel.setSaveLastPlayed(it) })
                )
            }
        }
        item(key = "lyrics") {
            Column {
                Text(text = stringResource(R.string.lyrics), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.main_lyrics_provider),
                    subtitle = mainLyricsProvider ?: "",
                    onClick = {

                    }
                )
                SettingItem(
                    title = if (musixmatchLoggedIn) stringResource(R.string.log_out_from_musixmatch)
                    else stringResource(R.string.log_in_to_Musixmatch),
                    subtitle = if (musixmatchLoggedIn) stringResource(R.string.logged_in)
                    else stringResource(R.string.only_support_email_and_password_type),
                    onClick = {
                        if (musixmatchLoggedIn) {
                            viewModel.clearMusixmatchCookie()
                        } else {
                            //TODO: Open musixmatch login page
                        }
                    }
                )
                SettingItem(
                    title = stringResource(R.string.use_musixmatch_translation),
                    subtitle = stringResource(R.string.use_musixmatch_translation_description),
                    switch = (useMusixmatchTranslation to { viewModel.setUseTranslation(it) }),
                    isEnable = musixmatchLoggedIn
                )
                SettingItem(
                    title = stringResource(R.string.translation_language),
                    subtitle = musixmatchTranslationLanguage ?: "",
                    onClick = {},
                    isEnable = useMusixmatchTranslation
                )
            }
        }
        item(key = "spotify") {
            Column {
                Text(text = stringResource(R.string.spotify), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.log_in_to_spotify),
                    subtitle = if (spotifyLoggedIn) stringResource(R.string.logged_in)
                    else stringResource(R.string.intro_login_to_spotify),
                    onClick = {

                    }
                )
                SettingItem(
                    title = stringResource(R.string.enable_spotify_lyrics),
                    subtitle = stringResource(R.string.spotify_lyrícs_info),
                    switch = (spotifyLyrics to { viewModel.setSpotifyLyrics(it) }),
                    isEnable = spotifyLoggedIn
                )
                SettingItem(
                    title = stringResource(R.string.enable_canvas),
                    subtitle = stringResource(R.string.canvas_info),
                    switch = (spotifyCanvas to { viewModel.setSpotifyCanvas(it) }),
                    isEnable = spotifyLoggedIn
                )
            }
        }
        item(key = "sponsor_block") {
            Column {
                Text(text = stringResource(R.string.sponsorBlock), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.enable_sponsor_block),
                    subtitle = stringResource(R.string.skip_sponsor_part_of_video),
                    switch = (enableSponsorBlock to { viewModel.setSponsorBlockEnabled(it) })
                )
                SettingItem(
                    title = stringResource(R.string.categories_sponsor_block),
                    subtitle = stringResource(R.string.what_segments_will_be_skipped),
                    onClick = {},
                    isEnable = enableSponsorBlock
                )
                val beforeUrl = stringResource(R.string.sponsor_block_intro).substringBefore("https://sponsor.ajay.app/")
                val afterUrl = stringResource(R.string.sponsor_block_intro).substringAfter("https://sponsor.ajay.app/")
                Text(
                    buildAnnotatedString {
                        append(beforeUrl)
                        withLink(
                            LinkAnnotation.Url(
                                "https://sponsor.ajay.app/",
                                TextLinkStyles(style = SpanStyle(color = md_theme_dark_primary))
                            )
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

                    }
                )
                SettingItem(
                    title = stringResource(R.string.downloaded_cache),
                    subtitle = "${downloadedCache.bytesToMB()} MB",
                    onClick = {

                    }
                )
                SettingItem(
                    title = stringResource(R.string.thumbnail_cache),
                    subtitle = "${thumbnailCache.bytesToMB()} MB",
                    onClick = {

                    }
                )
                SettingItem(
                    title = stringResource(R.string.spotify_canvas_cache),
                    subtitle = "${canvasCache.bytesToMB()} MB",
                    onClick = {

                    }
                )
                SettingItem(
                    title = stringResource(R.string.limit_player_cache),
                    subtitle = if (limitPlayerCache != -1) "${limitPlayerCache} MB"
                    else stringResource(R.string.unlimited),
                    onClick = {

                    }
                )
                Box(
                    Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 16.dp
                    )
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .onGloballyPositioned { layoutCoordinates ->
                                with(localDensity) {
                                    width = layoutCoordinates.size.width.toDp().value.toInt()
                                }
                            }
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(
                                        (fraction.otherApp * width).dp
                                    )
                                    .background(
                                        md_theme_dark_primary
                                    )
                                    .fillMaxHeight()
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .width(
                                        (fraction.downloadCache * width).dp
                                    )
                                    .background(
                                        Color(0xD540FF17)
                                    )
                                    .fillMaxHeight()
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .width(
                                        (fraction.playerCache * width).dp
                                    )
                                    .background(
                                        Color(0xD5FFFF00)
                                    )
                                    .fillMaxHeight()
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .width(
                                        (fraction.canvasCache * width).dp
                                    )
                                    .background(
                                        Color.Cyan
                                    )
                                    .fillMaxHeight()
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .width(
                                        (fraction.thumbCache * width).dp
                                    )
                                    .background(
                                        Color.Magenta
                                    )
                                    .fillMaxHeight()
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .width(
                                        (fraction.appDatabase * width).dp
                                    )
                                    .background(
                                        Color.White
                                    )
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .width(
                                        (fraction.freeSpace * width).dp
                                    )
                                    .background(
                                        Color.DarkGray
                                    )
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(
                        md_theme_dark_primary
                    ))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.other_app), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(
                        Color.Green
                    ))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.downloaded_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(
                        Color.Yellow
                    ))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.player_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(
                        Color.Cyan
                    ))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.spotify_canvas_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(
                        Color.Magenta
                    ))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.thumbnail_cache), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(
                        Color.White
                    ))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.database), style = typo.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(
                        Color.LightGray
                    ))
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.free_space), style = typo.bodySmall)
                }
            }
        }
        item(key = "backup") {
            Column {
                Text(text = stringResource(R.string.backup), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.backup),
                    subtitle = stringResource(R.string.save_all_your_playlist_data),
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        backupLauncher.launch("${context.getString(R.string.app_name)}_${LocalDateTime.now().format(formatter)}.backup")
                    }
                )
                SettingItem(
                    title = stringResource(R.string.restore_your_data),
                    subtitle = stringResource(R.string.restore_your_saved_data),
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/octet-stream"))
                    }
                )
            }
        }
        item(key = "about_us") {
            Column {
                Text(text = stringResource(R.string.about_us), style = typo.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                SettingItem(
                    title = stringResource(R.string.version),
                    subtitle = stringResource(R.string.version_name),
                    onClick = {

                    }
                )
                SettingItem(
                    title = stringResource(R.string.check_for_update),

                    onClick = {

                    }
                )
                SettingItem(
                    title = stringResource(R.string.author),
                    subtitle = stringResource(R.string.maxrave_dev),
                    onClick = {
                        uriHandler.openUri("https://github.com/maxrave-dev")
                    }
                )
                SettingItem(
                    title = stringResource(R.string.buy_me_a_coffee),
                    subtitle = stringResource(R.string.donation),
                    onClick = {
                        uriHandler.openUri("https://www.buymeacoffee.com/maxrave")
                    }
                )
                SettingItem(
                    title = stringResource(R.string.third_party_libraries),
                    subtitle = stringResource(R.string.description_and_licenses),
                    onClick = {

                    }
                )
            }
        }
        item(key = "end") {
            EndOfPage()
        }
    }
    TopAppBar(
        title = {
            Text(
                text = stringResource(
                    R.string.settings
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
                    navController.popBackStack()
                }
            }
        }
    )
}