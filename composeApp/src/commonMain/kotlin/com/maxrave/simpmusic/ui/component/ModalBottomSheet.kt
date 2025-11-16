package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.download.DownloadProgress
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.utils.FilterState
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.copyToClipboard
import com.maxrave.simpmusic.expect.shareUrl
import com.maxrave.simpmusic.expect.ui.photoPickerResult
import com.maxrave.simpmusic.extension.displayNameRes
import com.maxrave.simpmusic.extension.greyScale
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.theme.white
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetUIEvent
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.add_to_a_playlist
import simpmusic.composeapp.generated.resources.add_to_queue
import simpmusic.composeapp.generated.resources.album
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.baseline_access_alarm_24
import simpmusic.composeapp.generated.resources.baseline_add_photo_alternate_24
import simpmusic.composeapp.generated.resources.baseline_album_24
import simpmusic.composeapp.generated.resources.baseline_delete_24
import simpmusic.composeapp.generated.resources.baseline_downloaded
import simpmusic.composeapp.generated.resources.baseline_downloading_white
import simpmusic.composeapp.generated.resources.baseline_edit_24
import simpmusic.composeapp.generated.resources.baseline_favorite_24
import simpmusic.composeapp.generated.resources.baseline_favorite_border_24
import simpmusic.composeapp.generated.resources.baseline_keyboard_arrow_down_24
import simpmusic.composeapp.generated.resources.baseline_keyboard_double_arrow_down_24
import simpmusic.composeapp.generated.resources.baseline_keyboard_double_arrow_up_24
import simpmusic.composeapp.generated.resources.baseline_lyrics_24
import simpmusic.composeapp.generated.resources.baseline_people_alt_24
import simpmusic.composeapp.generated.resources.baseline_playlist_add_24
import simpmusic.composeapp.generated.resources.baseline_queue_music_24
import simpmusic.composeapp.generated.resources.baseline_sensors_24
import simpmusic.composeapp.generated.resources.baseline_share_24
import simpmusic.composeapp.generated.resources.baseline_sync_24
import simpmusic.composeapp.generated.resources.baseline_sync_disabled_24
import simpmusic.composeapp.generated.resources.baseline_update_24
import simpmusic.composeapp.generated.resources.bitrate
import simpmusic.composeapp.generated.resources.can_not_be_empty
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.codec
import simpmusic.composeapp.generated.resources.copied_to_clipboard
import simpmusic.composeapp.generated.resources.delete
import simpmusic.composeapp.generated.resources.delete_playlist
import simpmusic.composeapp.generated.resources.delete_song_from_playlist
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.done
import simpmusic.composeapp.generated.resources.download
import simpmusic.composeapp.generated.resources.download_speed
import simpmusic.composeapp.generated.resources.download_this_song_video_file_to_your_device
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.downloading_audio
import simpmusic.composeapp.generated.resources.downloading_video
import simpmusic.composeapp.generated.resources.edit_thumbnail
import simpmusic.composeapp.generated.resources.edit_title
import simpmusic.composeapp.generated.resources.endless_queue
import simpmusic.composeapp.generated.resources.error_occurred
import simpmusic.composeapp.generated.resources.holder
import simpmusic.composeapp.generated.resources.itag
import simpmusic.composeapp.generated.resources.like
import simpmusic.composeapp.generated.resources.like_and_dislike
import simpmusic.composeapp.generated.resources.liked
import simpmusic.composeapp.generated.resources.list_all_cookies_of_this_page
import simpmusic.composeapp.generated.resources.lrclib
import simpmusic.composeapp.generated.resources.main_lyrics_provider
import simpmusic.composeapp.generated.resources.merging_audio_and_video
import simpmusic.composeapp.generated.resources.mime_type
import simpmusic.composeapp.generated.resources.move_down
import simpmusic.composeapp.generated.resources.move_up
import simpmusic.composeapp.generated.resources.no_album
import simpmusic.composeapp.generated.resources.no_description
import simpmusic.composeapp.generated.resources.no_playlist_found
import simpmusic.composeapp.generated.resources.now_playing
import simpmusic.composeapp.generated.resources.now_playing_upper
import simpmusic.composeapp.generated.resources.ok
import simpmusic.composeapp.generated.resources.outline_download_for_offline_24
import simpmusic.composeapp.generated.resources.pitch
import simpmusic.composeapp.generated.resources.play_circle
import simpmusic.composeapp.generated.resources.play_next
import simpmusic.composeapp.generated.resources.playback_speed
import simpmusic.composeapp.generated.resources.playback_speed_pitch
import simpmusic.composeapp.generated.resources.playlist_name_cannot_be_empty
import simpmusic.composeapp.generated.resources.plays
import simpmusic.composeapp.generated.resources.processing
import simpmusic.composeapp.generated.resources.queue
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.round_speed_24
import simpmusic.composeapp.generated.resources.save
import simpmusic.composeapp.generated.resources.save_to_local_playlist
import simpmusic.composeapp.generated.resources.saved_to_local_playlist
import simpmusic.composeapp.generated.resources.set
import simpmusic.composeapp.generated.resources.share
import simpmusic.composeapp.generated.resources.share_url
import simpmusic.composeapp.generated.resources.simpmusic_lyrics
import simpmusic.composeapp.generated.resources.sleep_minutes
import simpmusic.composeapp.generated.resources.sleep_timer
import simpmusic.composeapp.generated.resources.sleep_timer_off
import simpmusic.composeapp.generated.resources.sleep_timer_set_error
import simpmusic.composeapp.generated.resources.sleep_timer_warning
import simpmusic.composeapp.generated.resources.sort_by
import simpmusic.composeapp.generated.resources.start_radio
import simpmusic.composeapp.generated.resources.sync
import simpmusic.composeapp.generated.resources.sync_first
import simpmusic.composeapp.generated.resources.synced
import simpmusic.composeapp.generated.resources.title
import simpmusic.composeapp.generated.resources.to_download_folder
import simpmusic.composeapp.generated.resources.unknown
import simpmusic.composeapp.generated.resources.update_playlist
import simpmusic.composeapp.generated.resources.warning
import simpmusic.composeapp.generated.resources.yes
import simpmusic.composeapp.generated.resources.your_discord_token
import simpmusic.composeapp.generated.resources.your_playlists
import simpmusic.composeapp.generated.resources.your_sp_dc_param_of_spotify_cookie
import simpmusic.composeapp.generated.resources.your_youtube_cookie
import simpmusic.composeapp.generated.resources.your_youtube_playlists
import simpmusic.composeapp.generated.resources.youtube_transcript
import simpmusic.composeapp.generated.resources.youtube_url

@ExperimentalMaterial3Api
@Composable
fun InfoPlayerBottomSheet(
    onDismiss: () -> Unit,
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val windowInsets = WindowInsets.systemBars
    var swipeEnabled by rememberSaveable { mutableStateOf(true) }
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = {
                swipeEnabled
            },
        )
    val scrollState = rememberScrollState()

    LaunchedEffect(true) {
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collectLatest {
                swipeEnabled = scrollState.value == 0
            }
    }

    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val songEntity by sharedViewModel.nowPlayingState.map { it?.songEntity }.collectAsState(null)
    val format by sharedViewModel.format.collectAsState(null)
    val downloadProgress by sharedViewModel.downloadFileProgress.collectAsStateWithLifecycle()

    if (downloadProgress != DownloadProgress.INIT) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                    Column(
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 20.dp,
                        ),
                    ) {
                        Text(
                            stringResource(Res.string.downloading),
                            style = typo().headlineMedium,
                        )
                        Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!downloadProgress.isDone && !downloadProgress.isError) {
                                CircularProgressIndicator()
                                Spacer(Modifier.size(15.dp))
                            }
                            Crossfade(downloadProgress) {
                                if (it.isMerging) {
                                    Text(
                                        text = stringResource(Res.string.merging_audio_and_video),
                                        modifier = Modifier.padding(vertical = 5.dp),
                                        style = typo().bodyMedium,
                                    )
                                } else if (it.isError) {
                                    Column {
                                        Text(
                                            text = stringResource(Res.string.error_occurred),
                                            modifier = Modifier.padding(vertical = 5.dp),
                                            style = typo().bodyMedium,
                                        )
                                        Text(
                                            text = downloadProgress.errorMessage,
                                            modifier = Modifier.padding(bottom = 5.dp),
                                            maxLines = 2,
                                            style = typo().bodyMedium,
                                        )
                                    }
                                } else if (it.isDone) {
                                    Text(
                                        text = stringResource(Res.string.downloaded) + stringResource(Res.string.to_download_folder),
                                        modifier = Modifier.padding(vertical = 5.dp),
                                        style = typo().bodyMedium,
                                    )
                                } else {
                                    Column {
                                        if (it.audioDownloadProgress != 0f) {
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.downloading_audio,
                                                        (downloadProgress.audioDownloadProgress * 100).toString() + "%",
                                                    ),
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                style = typo().bodyMedium,
                                            )
                                        }
                                        if (it.videoDownloadProgress != 0f) {
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.downloading_video,
                                                        (downloadProgress.videoDownloadProgress * 100).toString() + "%",
                                                    ),
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                style = typo().bodyMedium,
                                            )
                                        }
                                        if (downloadProgress.downloadSpeed != 0) {
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.download_speed,
                                                        downloadProgress.downloadSpeed.toString() + " kb/s",
                                                    ),
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                style = typo().bodyMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Crossfade(downloadProgress) {
                            if (it.isError) {
                                Column {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(onClick = {
                                        sharedViewModel.downloadFileDone()
                                    }) { Text(stringResource(Res.string.ok)) }
                                }
                            }
                            if (it.isDone) {
                                Column {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(onClick = {
                                        sharedViewModel.downloadFileDone()
                                    }) { Text(stringResource(Res.string.ok)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = Color.Black,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = .5f),
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            shape = RectangleShape,
            colors = CardDefaults.cardColors().copy(containerColor = Color.Black),
        ) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(scrollState)
                        .padding(
                            top =
                                with(localDensity) {
                                    windowInsets.getTop(localDensity).toDp()
                                },
                        ),
            ) {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors =
                        TopAppBarDefaults.topAppBarColors().copy(
                            containerColor = Color.Transparent,
                        ),
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.now_playing_upper),
                                style = typo().bodyMedium,
                                color = Color.White,
                            )
                            Text(
                                text = screenDataState.nowPlayingTitle,
                                style = typo().labelMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
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
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_keyboard_arrow_down_24),
                                contentDescription = "",
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier.size(48.dp),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.title),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = screenDataState.nowPlayingTitle,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .padding(horizontal = 10.dp)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.artists),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = screenDataState.artistName,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.album),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = songEntity?.albumName ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.itag),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.itag?.toString() ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.mime_type),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.mimeType ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.codec),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.codecs ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.bitrate),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = format?.bitrate?.toString() ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.plays),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = screenDataState.songInfoData?.viewCount?.toString() ?: stringResource(Res.string.unknown),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.like),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text =
                        stringResource(
                            Res.string.like_and_dislike,
                            screenDataState.songInfoData?.like ?: 0,
                            screenDataState.songInfoData?.dislike ?: 0,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable()
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.description),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text = screenDataState.songInfoData?.description ?: stringResource(Res.string.no_description),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .padding(horizontal = 10.dp),
                    style = typo().bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.youtube_url),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = typo().labelMedium,
                    color = white,
                )
                Text(
                    text =
                        buildAnnotatedString {
                            withLink(
                                LinkAnnotation.Url(
                                    "https://music.youtube.com/watch?v=${songEntity?.videoId}",
                                    TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline)),
                                ),
                            ) {
                                append("https://music.youtube.com/watch?v=${songEntity?.videoId}")
                            }
                        },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                    style = typo().bodyMedium,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(
                    enabled = screenDataState.bitmap != null,
                    onClick = {
                        sharedViewModel.downloadFile(
                            bitmap = screenDataState.bitmap ?: return@OutlinedButton,
                        )
                    },
                    modifier =
                        Modifier
                            .wrapContentSize()
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 10.dp),
                ) {
                    Text(text = stringResource(Res.string.download_this_song_video_file_to_your_device))
                }
                Spacer(modifier = Modifier.height(10.dp))

                EndOfModalBottomSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class, ExperimentalCoroutinesApi::class, ExperimentalFoundationApi::class)
@Composable
fun QueueBottomSheet(
    onDismiss: () -> Unit,
    sharedViewModel: SharedViewModel = koinInject(),
    musicServiceHandler: MediaPlayerHandler = koinInject<MediaPlayerHandler>(),
    dataStoreManager: DataStoreManager = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val windowInsets = WindowInsets.systemBars
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val lazyListState = rememberLazyListState()
    val dragDropState =
        rememberDragDropState(lazyListState) { from, to ->
            coroutineScope.launch {
                musicServiceHandler.swap(from, to)
            }
        }
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    var shouldShowQueueItemBottomSheet by rememberSaveable { mutableStateOf(false) }
    var clickMoreIndex by rememberSaveable { mutableIntStateOf(0) }
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val songEntity by sharedViewModel.nowPlayingState.map { it?.songEntity }.collectAsState(null)
    val queueData by musicServiceHandler.queueData.collectAsStateWithLifecycle()
    val queue by remember {
        derivedStateOf {
            queueData?.data?.listTracks ?: emptyList()
        }
    }
    val loadMoreState by remember {
        derivedStateOf {
            queueData?.queueState ?: QueueData.StateSource.STATE_CREATED
        }
    }
    val endlessQueueEnable by dataStoreManager.endlessQueue.map { it == DataStoreManager.TRUE }.collectAsState(false)

    val shouldLoadMore =
        remember {
            derivedStateOf {
                val layoutInfo = lazyListState.layoutInfo
                val lastVisibleItem =
                    layoutInfo.visibleItemsInfo.lastOrNull()
                        ?: return@derivedStateOf true

                lastVisibleItem.index >= layoutInfo.totalItemsCount - 3 && layoutInfo.totalItemsCount > 0
            }
        }

    // Convert the state into a cold flow and collect
    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .collect {
                // if should load more, then invoke loadMore
                if (it && loadMoreState == QueueData.StateSource.STATE_INITIALIZED) musicServiceHandler.loadMore()
            }
    }

    LaunchedEffect(queue) {
        Logger.w("QueueBottomSheet", "queue: $queue")
    }

    DisposableEffect(Unit) {
        val currentSongIndex = musicServiceHandler.currentOrderIndex().takeIf { i -> i > -1 } ?: 0
        Logger.d("QueueBottomSheet", "currentSongIndex: $currentSongIndex")
        coroutineScope.launch {
            lazyListState.requestScrollToItem(currentSongIndex)
        }
        onDispose { }
    }

    val showQueueItemBottomSheet: (Int) -> Unit = { index ->
        clickMoreIndex = index
        shouldShowQueueItemBottomSheet = true
    }

    if (shouldShowQueueItemBottomSheet) {
        QueueItemBottomSheet(
            onDismiss = { shouldShowQueueItemBottomSheet = false },
            index = clickMoreIndex,
            musicServiceHandler = musicServiceHandler,
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = Color.Black,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = .5f),
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            shape = RectangleShape,
            colors = CardDefaults.cardColors().copy(containerColor = Color.Black),
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        top =
                            with(localDensity) {
                                windowInsets.getTop(localDensity).toDp()
                            },
                    ),
            ) {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors =
                        TopAppBarDefaults.topAppBarColors().copy(
                            containerColor = Color.Transparent,
                        ),
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.now_playing_upper),
                                style = typo().bodyMedium,
                                color = Color.White,
                            )
                            Text(
                                text = screenDataState.playlistName,
                                style = typo().labelMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
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
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.baseline_keyboard_arrow_down_24),
                                contentDescription = "",
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier.size(32.dp),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(Res.string.now_playing),
                    style = typo().titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                SongFullWidthItems(
                    songEntity = songEntity,
                    isPlaying = false,
                    onAddToQueue = null,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.queue),
                        style = typo().titleMedium,
                        modifier =
                            Modifier
                                .padding(horizontal = 20.dp)
                                .weight(1f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.endless_queue),
                            style = typo().bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        Switch(
                            checked = endlessQueueEnable,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    dataStoreManager.setEndlessQueue(it)
                                }
                            },
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    horizontalAlignment = Alignment.Start,
                    state = lazyListState,
                    modifier =
                        Modifier
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDrag = { change, offset ->
                                        Logger.d("QueueBottomSheet", "onDrag $offset")
                                        change.consume()
                                        dragDropState.onDrag(offset = offset)

                                        if (overscrollJob?.isActive == true) {
                                            return@detectDragGesturesAfterLongPress
                                        }

                                        dragDropState
                                            .checkForOverScroll()
                                            .takeIf { it != 0f }
                                            ?.let {
                                                overscrollJob =
                                                    coroutineScope.launch {
                                                        dragDropState.state.animateScrollBy(
                                                            it * 1.3f,
                                                            tween(easing = FastOutLinearInEasing),
                                                        )
                                                    }
                                            }
                                            ?: run { overscrollJob?.cancel() }
                                    },
                                    onDragStart = { offset ->
                                        Logger.d("QueueBottomSheet", "onDragStart $offset")
                                        dragDropState.onDragStart(offset)
                                    },
                                    onDragEnd = {
                                        Logger.d("QueueBottomSheet", "onDragEnd")
                                        dragDropState.onDragInterrupted(true)
                                        overscrollJob?.cancel()
                                    },
                                    onDragCancel = {
                                        Logger.d("QueueBottomSheet", "onDragCancel")
                                        dragDropState.onDragInterrupted()
                                        overscrollJob?.cancel()
                                    },
                                )
                            },
                ) {
                    itemsIndexed(
                        queue,
                        key = { i, t -> i.toString() + t.videoId },
                    ) { index, track ->
                        if (index != -1) {
                            DraggableItem(
                                dragDropState = dragDropState,
                                index = index,
                                modifier = Modifier,
                            ) { _ ->
                                SongFullWidthItems(
                                    track = track,
                                    isPlaying = track.videoId == songEntity?.videoId,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
                                    onClickListener = { videoId ->
                                        if (videoId == track.videoId) {
                                            musicServiceHandler.playMediaItemInMediaSource(index)
                                        }
                                    },
                                    onMoreClickListener = {
                                        showQueueItemBottomSheet(index)
                                    },
                                    onAddToQueue = {
                                        sharedViewModel.addListToQueue(
                                            arrayListOf(track),
                                        )
                                    },
                                )
                            }
                        }
                    }
                    item {
                        if (loadMoreState == QueueData.StateSource.STATE_INITIALIZING) {
                            CenterLoadingBox(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                            )
                        }
                    }
                    item {
                        EndOfPage()
                    }
                }
            }
        }
    }
}

private enum class QueueItemAction {
    UP,
    DOWN,
    DELETE,
}

@Composable
@ExperimentalMaterial3Api
fun QueueItemBottomSheet(
    onDismiss: () -> Unit,
    index: Int,
    musicServiceHandler: MediaPlayerHandler = koinInject<MediaPlayerHandler>(),
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val hideModalBottomSheet: () -> Unit =
        {
            coroutineScope.launch {
                modelBottomSheetState.hide()
                onDismiss()
            }
        }
    val listAction =
        listOf(
            QueueItemAction.UP,
            QueueItemAction.DOWN,
            QueueItemAction.DELETE,
        )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors =
                        CardDefaults.cardColors().copy(
                            containerColor = Color(0xFF474545),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(5.dp))
                LazyColumn {
                    val canMoveUp =
                        index > 0 &&
                            index < (
                                musicServiceHandler.queueData.value
                                    ?.data
                                    ?.listTracks
                                    ?.size ?: 0
                            )
                    val canMoveDown =
                        index >= 0 &&
                            index < (
                                musicServiceHandler.queueData.value
                                    ?.data
                                    ?.listTracks
                                    ?.size ?: 0
                            ) - 1
                    items(listAction) { action ->
                        val disable =
                            when (action) {
                                QueueItemAction.UP -> !canMoveUp
                                QueueItemAction.DOWN -> !canMoveDown
                                QueueItemAction.DELETE -> false
                            }
                        if (disable) return@items
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        hideModalBottomSheet()
                                        when (action) {
                                            QueueItemAction.UP -> {
                                                coroutineScope.launch {
                                                    musicServiceHandler.moveItemUp(index)
                                                }
                                            }

                                            QueueItemAction.DOWN -> {
                                                coroutineScope.launch {
                                                    musicServiceHandler.moveItemDown(index)
                                                }
                                            }

                                            QueueItemAction.DELETE -> {
                                                musicServiceHandler.removeMediaItem(index)
                                            }
                                        }
                                    },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
                                        .padding(20.dp)
                                        .align(Alignment.CenterStart),
                            ) {
                                when (action) {
                                    QueueItemAction.UP -> {
                                        Image(
                                            painter =
                                                painterResource(
                                                    Res.drawable.baseline_keyboard_double_arrow_up_24,
                                                ),
                                            contentDescription = "Move up",
                                        )
                                    }

                                    QueueItemAction.DOWN -> {
                                        Image(
                                            painter =
                                                painterResource(
                                                    Res.drawable.baseline_keyboard_double_arrow_down_24,
                                                ),
                                            contentDescription = "Move down",
                                        )
                                    }

                                    QueueItemAction.DELETE -> {
                                        Image(
                                            painter =
                                                painterResource(
                                                    Res.drawable.baseline_delete_24,
                                                ),
                                            contentDescription = "Delete",
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text =
                                        stringResource(
                                            when (action) {
                                                QueueItemAction.UP -> Res.string.move_up
                                                QueueItemAction.DOWN -> Res.string.move_down
                                                QueueItemAction.DELETE -> Res.string.delete
                                            },
                                        ),
                                    style = typo().labelSmall,
                                )
                            }
                        }
                    }
                    item {
                        EndOfModalBottomSheet()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingBottomSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    song: SongEntity?,
    viewModel: NowPlayingBottomSheetViewModel = koinViewModel(),
    setSleepTimerEnable: Boolean = false,
    changeMainLyricsProviderEnable: Boolean = false,
    onNavigateToOtherScreen: () -> Unit = {}, // Fix the now playing screen not disappearing when navigating to other screen
    // Delete is specific to playlist
    onDelete: (() -> Unit)? = null,
    onLibraryDelete: (() -> Unit)? = null,
    dataStoreManager: DataStoreManager = koinInject<DataStoreManager>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val hideModalBottomSheet: () -> Unit =
        {
            coroutineScope.launch {
                modelBottomSheetState.hide()
                onDismiss()
            }
        }

    var addToAPlaylist by remember { mutableStateOf(false) }
    var artist by remember { mutableStateOf(false) }
    var mainLyricsProvider by remember {
        mutableStateOf(false)
    }
    var sleepTimer by remember {
        mutableStateOf(false)
    }
    var sleepTimerWarning by remember {
        mutableStateOf(false)
    }
    var isBottomSheetVisible by rememberSaveable { mutableStateOf(false) }
    var changePlaybackSpeedPitch by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState.songUIState.videoId.isNotEmpty() && !isBottomSheetVisible) {
            isBottomSheetVisible = true
        }
    }

    LaunchedEffect(key1 = song) {
        viewModel.setSongEntity(song)
    }

    if (changePlaybackSpeedPitch) {
        val playbackSpeed by dataStoreManager.playbackSpeed.collectAsState(1f)
        val pitch by dataStoreManager.pitch.collectAsState(0)
        PlaybackSpeedPitchBottomSheet(
            onDismiss = { changePlaybackSpeedPitch = false },
            playbackSpeed = playbackSpeed,
            pitch = pitch,
        ) { speed, p ->
            viewModel.onUIEvent(
                NowPlayingBottomSheetUIEvent.ChangePlaybackSpeedPitch(
                    speed = speed,
                    pitch = p,
                ),
            )
        }
    }

    if (addToAPlaylist) {
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = uiState.listLocalPlaylist,
            listYouTubePlaylist = uiState.listYouTubePlaylist,
            onDismiss = { addToAPlaylist = false },
            onClick = {
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToPlaylist(it.id))
            },
            onYTPlaylistClick = {
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToYouTubePlaylist(it.browseId))
            },
            videoId = uiState.songUIState.videoId,
        )
    }
    if (artist) {
        ArtistModalBottomSheet(
            isBottomSheetVisible = true,
            artists = uiState.songUIState.listArtists,
            navController = navController,
            onNavigateToOtherScreen = onNavigateToOtherScreen,
            onDismiss = { artist = false },
        )
    }

    if (sleepTimer) {
        SleepTimerBottomSheet(onDismiss = { sleepTimer = false }) { minutes: Int ->
            if (setSleepTimerEnable) {
                viewModel.onUIEvent(
                    NowPlayingBottomSheetUIEvent.SetSleepTimer(
                        cancel = false,
                        minutes = minutes,
                    ),
                )
            }
        }
    }

    if (sleepTimerWarning) {
        AlertDialog(
            containerColor = Color(0xFF242424),
            onDismissRequest = { sleepTimerWarning = false },
            confirmButton = {
                TextButton(onClick = {
                    sleepTimerWarning = false
                    viewModel.onUIEvent(
                        NowPlayingBottomSheetUIEvent.SetSleepTimer(
                            cancel = true,
                        ),
                    )
                }) {
                    Text(text = stringResource(Res.string.yes), style = typo().labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { sleepTimerWarning = false }) {
                    Text(text = stringResource(Res.string.cancel), style = typo().labelSmall)
                }
            },
            title = {
                Text(text = stringResource(Res.string.warning), style = typo().labelSmall)
            },
            text = {
                Text(text = stringResource(Res.string.sleep_timer_warning), style = typo().bodyMedium)
            },
        )
    }

    if (mainLyricsProvider) {
        var selected by remember {
            mutableIntStateOf(
                when (uiState.mainLyricsProvider) {
                    DataStoreManager.SIMPMUSIC -> 0
                    DataStoreManager.LRCLIB -> 1
                    DataStoreManager.YOUTUBE -> 2
                    else -> 0
                },
            )
        }

        AlertDialog(
            onDismissRequest = { mainLyricsProvider = false },
            containerColor = Color(0xFF242424),
            title = {
                Text(
                    text = stringResource(Res.string.main_lyrics_provider),
                    style = typo().titleMedium,
                )
            },
            text = {
                Column {
                    Row(
                        modifier =
                            Modifier
                                .padding(horizontal = 4.dp)
                                .fillMaxWidth()
                                .clickable {
                                    selected = 0
                                },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == 0,
                            onClick = {
                                selected = 0
                            },
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(text = stringResource(Res.string.simpmusic_lyrics), style = typo().labelSmall)
                    }
                    Row(
                        modifier =
                            Modifier
                                .padding(horizontal = 4.dp)
                                .fillMaxWidth()
                                .clickable {
                                    selected = 1
                                },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == 1,
                            onClick = {
                                selected = 1
                            },
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(text = stringResource(Res.string.lrclib), style = typo().labelSmall)
                    }
                    Row(
                        modifier =
                            Modifier
                                .padding(horizontal = 4.dp)
                                .fillMaxWidth()
                                .clickable {
                                    selected = 2
                                },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == 2,
                            onClick = {
                                selected = 2
                            },
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(text = stringResource(Res.string.youtube_transcript), style = typo().labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onUIEvent(
                            NowPlayingBottomSheetUIEvent.ChangeLyricsProvider(
                                when (selected) {
                                    0 -> DataStoreManager.SIMPMUSIC
                                    1 -> DataStoreManager.LRCLIB
                                    2 -> DataStoreManager.YOUTUBE
                                    else -> DataStoreManager.SIMPMUSIC
                                },
                            ),
                        )
                        mainLyricsProvider = false
                    },
                ) {
                    Text(text = stringResource(Res.string.yes), style = typo().labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mainLyricsProvider = false
                }) {
                    Text(text = stringResource(Res.string.cancel), style = typo().labelSmall)
                }
            },
        )
    }

    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = Color(0xFF474545),
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val thumb = uiState.songUIState.thumbnails
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(thumb)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .diskCacheKey(thumb)
                                    .crossfade(550)
                                    .build(),
                            placeholder = painterResource(Res.drawable.holder),
                            error = painterResource(Res.drawable.holder),
                            contentDescription = null,
                            contentScale = ContentScale.Inside,
                            modifier =
                                Modifier
                                    .align(Alignment.CenterVertically)
                                    .clip(
                                        RoundedCornerShape(10.dp),
                                    ).size(60.dp),
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = uiState.songUIState.title,
                                style = typo().labelMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .wrapContentHeight(Alignment.CenterVertically)
                                        .basicMarquee(
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                            Text(
                                text =
                                    uiState.songUIState.listArtists
                                        .toListName()
                                        .connectArtists(),
                                style = typo().bodyMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .wrapContentHeight(Alignment.CenterVertically)
                                        .basicMarquee(
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    HorizontalDivider(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                        thickness = 1.dp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Crossfade(targetState = onDelete != null) {
                        if (it) {
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_delete_24),
                                text = Res.string.delete_song_from_playlist,
                            ) {
                                hideModalBottomSheet()
                                onDelete?.invoke()
                            }
                        }
                    }
                    Crossfade(targetState = onLibraryDelete != null) {
                        if (it) {
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_delete_24),
                                text = Res.string.delete,
                            ) {
                                hideModalBottomSheet()
                                onLibraryDelete?.invoke()
                            }
                        }
                    }
                    CheckBoxActionButton(
                        defaultChecked = uiState.songUIState.liked,
                        isHeartIcon = true,
                        onChangeListener = {
                            viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ToggleLike)
                        },
                    )
                    ActionButton(
                        icon =
                            when (uiState.songUIState.downloadState) {
                                DownloadState.STATE_NOT_DOWNLOADED ->
                                    painterResource(
                                        Res.drawable.outline_download_for_offline_24,
                                    )

                                DownloadState.STATE_DOWNLOADING ->
                                    painterResource(
                                        Res.drawable.baseline_downloading_white,
                                    )

                                DownloadState.STATE_DOWNLOADED ->
                                    painterResource(
                                        Res.drawable.baseline_downloaded,
                                    )

                                DownloadState.STATE_PREPARING ->
                                    painterResource(
                                        Res.drawable.baseline_downloading_white,
                                    )

                                else ->
                                    painterResource(
                                        Res.drawable.outline_download_for_offline_24,
                                    )
                            },
                        text =
                            when (uiState.songUIState.downloadState) {
                                DownloadState.STATE_NOT_DOWNLOADED -> Res.string.download
                                DownloadState.STATE_DOWNLOADING -> Res.string.downloading
                                DownloadState.STATE_DOWNLOADED -> Res.string.downloaded
                                DownloadState.STATE_PREPARING -> Res.string.downloading
                                else -> Res.string.download
                            },
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_playlist_add_24),
                        text = Res.string.add_to_a_playlist,
                    ) {
                        addToAPlaylist = true
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.play_circle),
                        text = Res.string.play_next,
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.PlayNext)
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_queue_music_24),
                        text = Res.string.add_to_queue,
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToQueue)
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_people_alt_24),
                        text = Res.string.artists,
                    ) {
                        artist = true
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_album_24),
                        text = if (uiState.songUIState.album == null) Res.string.no_album else null,
                        textString = uiState.songUIState.album?.name,
                        enable = uiState.songUIState.album != null,
                    ) {
                        uiState.songUIState.album?.id?.let { id ->
                            onNavigateToOtherScreen()
                            navController.navigate(
                                AlbumDestination(
                                    browseId = id,
                                ),
                            )
                        }
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_sensors_24),
                        text = Res.string.start_radio,
                    ) {
                        viewModel.onUIEvent(
                            NowPlayingBottomSheetUIEvent.StartRadio(
                                videoId = uiState.songUIState.videoId,
                                name = "\"${uiState.songUIState.title}\" ${runBlocking { getString(Res.string.radio) }}",
                            ),
                        )
                        hideModalBottomSheet()
                    }
                    Crossfade(targetState = changeMainLyricsProviderEnable) {
                        if (it) {
                            ActionButton(
                                icon = painterResource(Res.drawable.baseline_lyrics_24),
                                text = Res.string.main_lyrics_provider,
                            ) {
                                mainLyricsProvider = true
                            }
                        }
                    }
                    Crossfade(targetState = setSleepTimerEnable) {
                        val sleepTimerState = uiState.sleepTimer
                        if (it) {
                            Crossfade(targetState = sleepTimerState.timeRemaining > 0) { running ->
                                if (running) {
                                    ActionButton(
                                        icon = painterResource(Res.drawable.baseline_access_alarm_24),
                                        textString = stringResource(Res.string.sleep_timer, sleepTimerState.timeRemaining.toString()),
                                        text = null,
                                        textColor = seed,
                                        iconColor = seed,
                                    ) {
                                        sleepTimerWarning = true
                                    }
                                } else {
                                    ActionButton(
                                        icon = painterResource(Res.drawable.baseline_access_alarm_24),
                                        text = Res.string.sleep_timer_off,
                                    ) {
                                        sleepTimer = true
                                    }
                                }
                            }
                        }
                    }
                    Crossfade(targetState = setSleepTimerEnable) {
                        if (it) {
                            // Sleep timer is enabled, so this screen is player screen
                            ActionButton(
                                icon = painterResource(Res.drawable.round_speed_24),
                                text = Res.string.playback_speed_pitch,
                            ) {
                                changePlaybackSpeedPitch = true
                            }
                        }
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_share_24),
                        text = Res.string.share,
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Share)
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: Painter,
    text: StringResource?,
    textString: String? = null,
    textColor: Color? = null,
    iconColor: Color = Color.White,
    enable: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.CenterVertically)
                .then(
                    if (enable) Modifier.clickable { onClick.invoke() } else Modifier.greyScale(),
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Image(
                painter = icon,
                contentDescription = if (text != null) stringResource(text) else textString ?: "",
                modifier =
                    Modifier
                        .wrapContentSize(
                            Alignment.Center,
                        ).padding(12.dp),
                colorFilter =
                    if (enable) {
                        ColorFilter.tint(iconColor)
                    } else {
                        ColorFilter.tint(Color.Gray)
                    },
            )

            Text(
                text = if (text != null) stringResource(text) else textString ?: "",
                style = typo().labelSmall,
                color = if (enable) textColor ?: Color.Unspecified else Color.Gray,
                modifier =
                    Modifier
                        .padding(start = 10.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
fun CheckBoxActionButton(
    defaultChecked: Boolean,
    isHeartIcon: Boolean,
    onChangeListener: (checked: Boolean) -> Unit,
) {
    var stateChecked by remember { mutableStateOf(defaultChecked) }
    Box(
        modifier =
            Modifier
                .wrapContentSize(align = Alignment.Center)
                .clickable {
                    stateChecked = !stateChecked
                    onChangeListener(stateChecked)
                },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
        ) {
            Box(Modifier.padding(10.dp)) {
                if (isHeartIcon) {
                    HeartCheckBox(checked = stateChecked, size = 30)
                } else {
                    Crossfade(stateChecked) {
                        if (it) {
                            Icon(Icons.Rounded.CheckCircle, "")
                        } else {
                            Icon(Icons.Rounded.AddCircleOutline, "")
                        }
                    }
                }
            }
            Text(
                text =
                    if (stateChecked) {
                        stringResource(
                            Res.string.liked,
                        )
                    } else {
                        stringResource(Res.string.like)
                    },
                style = typo().labelSmall,
                modifier =
                    Modifier
                        .padding(start = 10.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
fun HeartCheckBox(
    size: Int = 24,
    checked: Boolean,
    onStateChange: (() -> Unit)? = null,
) {
    Box(
        modifier =
            Modifier
                .size(size.dp)
                .clip(
                    CircleShape,
                ).clickable {
                    onStateChange?.invoke()
                },
    ) {
        Crossfade(targetState = checked, modifier = Modifier.fillMaxSize()) {
            if (it) {
                Image(
                    painter = painterResource(Res.drawable.baseline_favorite_24),
                    contentDescription = "Favorite checked",
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.baseline_favorite_border_24),
                    contentDescription = "Favorite unchecked",
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                    colorFilter = ColorFilter.tint(Color.White),
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun PlaybackSpeedPitchBottomSheet(
    onDismiss: () -> Unit,
    playbackSpeed: Float,
    pitch: Int,
    onSet: (playbackSpeed: Float, pitch: Int) -> Unit,
) {
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 10.dp),
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors =
                        CardDefaults.cardColors().copy(
                            containerColor = Color(0xFF474545),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.playback_speed) + " ${playbackSpeed}x",
                    style = typo().labelSmall,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Slider(
                    value = playbackSpeed,
                    onValueChange = {
                        onSet(it, pitch)
                    },
                    modifier = Modifier,
                    enabled = true,
                    valueRange = 0.25f..2f,
                    steps = 13,
                    onValueChangeFinished = {},
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = stringResource(Res.string.pitch) + " $pitch",
                    style = typo().labelSmall,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Slider(
                    value = pitch.toFloat(),
                    onValueChange = {
                        onSet(playbackSpeed, it.toInt())
                    },
                    modifier = Modifier,
                    enabled = true,
                    valueRange = -12f..12f,
                    steps = 23,
                    onValueChangeFinished = {},
                )
                EndOfModalBottomSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistModalBottomSheet(
    isBottomSheetVisible: Boolean,
    listLocalPlaylist: List<LocalPlaylistEntity>,
    listYouTubePlaylist: List<PlaylistsResult>,
    videoId: String? = null,
    onClick: (LocalPlaylistEntity) -> Unit,
    onYTPlaylistClick: (PlaylistsResult) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        )
    val hideModalBottomSheet: () -> Unit =
        {
            coroutineScope.launch {
                modelBottomSheetState.hide()
                onDismiss()
            }
        }
    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier =
                    Modifier
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = BottomSheetDefaults.ExpandedShape,
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 10.dp),
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = Color(0xFF474545),
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))

                    val chipRowState = rememberScrollState()
                    var isYouTubePlaylistClicked by remember {
                        mutableStateOf(false)
                    }
                    if (listYouTubePlaylist.isNotEmpty()) {
                        Row(
                            modifier =
                                Modifier
                                    .horizontalScroll(chipRowState)
                                    .padding(horizontal = 15.dp)
                                    .padding(vertical = 8.dp)
                                    .background(Color.Transparent),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Chip(
                                isAnimated = false,
                                isSelected = !isYouTubePlaylistClicked,
                                text = stringResource(Res.string.your_playlists),
                                onClick = {
                                    isYouTubePlaylistClicked = false
                                },
                            )
                            Chip(
                                isAnimated = false,
                                isSelected = isYouTubePlaylistClicked,
                                text = stringResource(Res.string.your_youtube_playlists),
                                onClick = {
                                    isYouTubePlaylistClicked = true
                                },
                            )
                        }
                    }

                    if ((listLocalPlaylist.isEmpty() && !isYouTubePlaylistClicked) ||
                        (listYouTubePlaylist.isEmpty() && isYouTubePlaylistClicked)
                    ) {
                        Text(
                            text = stringResource(Res.string.no_playlist_found),
                            style = typo().labelSmall,
                            modifier = Modifier.padding(20.dp),
                            color = Color.Gray,
                        )
                    } else {
                        Crossfade(isYouTubePlaylistClicked) { clicked ->
                            if (clicked) {
                                LazyColumn {
                                    items(listYouTubePlaylist) { playlist ->
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clickable(
                                                        enabled = true,
                                                        onClick = {
                                                            onYTPlaylistClick(playlist)
                                                            hideModalBottomSheet()
                                                        },
                                                    ),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier =
                                                    Modifier
                                                        .padding(12.dp)
                                                        .align(Alignment.CenterStart),
                                            ) {
                                                Image(
                                                    painter =
                                                        painterResource(
                                                            Res.drawable.baseline_playlist_add_24,
                                                        ),
                                                    contentDescription = "",
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = playlist.title,
                                                    style = typo().labelSmall,
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn {
                                    items(listLocalPlaylist) { playlist ->
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clickable(
                                                        enabled = playlist.tracks?.contains(videoId) != true,
                                                        onClick = {
                                                            onClick(playlist)
                                                            hideModalBottomSheet()
                                                        },
                                                    ),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier =
                                                    Modifier
                                                        .padding(12.dp)
                                                        .align(Alignment.CenterStart),
                                            ) {
                                                Crossfade(
                                                    targetState = playlist.tracks?.contains(videoId) == true,
                                                ) {
                                                    if (it) {
                                                        Image(
                                                            painter = painterResource(Res.drawable.done),
                                                            contentDescription = "",
                                                        )
                                                    } else {
                                                        Image(
                                                            painter =
                                                                painterResource(
                                                                    Res.drawable.baseline_playlist_add_24,
                                                                ),
                                                            contentDescription = "",
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = playlist.title,
                                                    style = typo().labelSmall,
                                                    color = if (playlist.tracks?.contains(videoId) == true) Color.Gray else Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    onDismiss: () -> Unit,
    onSetTimer: (minutes: Int) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    var minutes by rememberSaveable { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors =
                        CardDefaults.cardColors().copy(
                            containerColor = Color(0xFF474545),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = stringResource(Res.string.sleep_minutes), style = typo().labelSmall)
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    value = minutes.toString(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    onValueChange = { value -> if (value.all { it.isDigit() } && value.isNotEmpty() && value.isNotBlank()) minutes = value.toInt() },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                Spacer(modifier = Modifier.height(5.dp))
                TextButton(
                    onClick = {
                        if (minutes > 0) {
                            onSetTimer(minutes)
                            coroutineScope.launch {
                                modelBottomSheetState.hide()
                                onDismiss()
                            }
                        } else {
                            showToast(runBlocking { getString(Res.string.sleep_timer_set_error) }, ToastGravity.Bottom)
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                ) {
                    Text(text = stringResource(Res.string.set), style = typo().labelSmall)
                }
                Spacer(modifier = Modifier.height(5.dp))
                EndOfModalBottomSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistModalBottomSheet(
    isBottomSheetVisible: Boolean,
    artists: List<Artist>,
    navController: NavController,
    onNavigateToOtherScreen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val hideModalBottomSheet: () -> Unit =
        {
            coroutineScope.launch {
                modelBottomSheetState.hide()
                onDismiss()
            }
        }
    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = Color(0xFF474545),
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    LazyColumn {
                        items(artists) { artist ->
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val id = artist.id
                                            if (!id.isNullOrBlank()) {
                                                onNavigateToOtherScreen()
                                                navController.navigate(
                                                    ArtistDestination(
                                                        id,
                                                    ),
                                                )
                                            }
                                        },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                        Modifier
                                            .padding(20.dp)
                                            .align(Alignment.CenterStart),
                                ) {
                                    Image(
                                        painter =
                                            painterResource(
                                                Res.drawable.baseline_people_alt_24,
                                            ),
                                        contentDescription = "",
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = artist.name,
                                        style = typo().labelSmall,
                                    )
                                }
                            }
                        }
                        item {
                            EndOfModalBottomSheet()
                        }
                    }
                }
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
fun PlaylistBottomSheet(
    onDismiss: () -> Unit,
    playlistId: String,
    playlistName: String,
    isYourYouTubePlaylist: Boolean,
    onEditTitle: (newTitle: String) -> Unit = {},
    onSaveToLocal: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    localPlaylistRepository: LocalPlaylistRepository = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    var isSavedToLocal by remember { mutableStateOf(false) }
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val hideModalBottomSheet: () -> Unit =
        {
            coroutineScope.launch {
                modelBottomSheetState.hide()
                onDismiss()
            }
        }
    var showEditTitle by remember { mutableStateOf(false) }
    if (showEditTitle) {
        var newTitle by remember { mutableStateOf(playlistName) }
        val showEditTitleSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    showEditTitleSheetState.hide()
                    onDismiss()
                }
            }
        ModalBottomSheet(
            onDismissRequest = { showEditTitle = false },
            sheetState = showEditTitleSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = Color(0xFF474545),
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(Res.string.title))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    val playlistNameError = stringResource(Res.string.playlist_name_cannot_be_empty)
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                showToast(playlistNameError, ToastGravity.Bottom)
                            } else {
                                onEditTitle(newTitle)
                                hideEditTitleBottomSheet()
                                hideModalBottomSheet()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(Res.string.save))
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }

    LaunchedEffect(true) {
        localPlaylistRepository.getAllLocalPlaylists().collect {
            isSavedToLocal = it.any { playlist -> playlist.youtubePlaylistId == playlistId }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors =
                        CardDefaults.cardColors().copy(
                            containerColor = Color(0xFF474545),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(5.dp))
                if (onAddToQueue != null) {
                    ActionButton(icon = painterResource(Res.drawable.baseline_queue_music_24), text = Res.string.add_to_queue) {
                        onAddToQueue()
                        hideModalBottomSheet()
                    }
                }
                if (isYourYouTubePlaylist) {
                    ActionButton(icon = painterResource(Res.drawable.baseline_edit_24), text = Res.string.edit_title) {
                        showEditTitle = true
                    }
                    ActionButton(
                        icon =
                            if (isSavedToLocal) {
                                painterResource(Res.drawable.baseline_sync_disabled_24)
                            } else {
                                painterResource(Res.drawable.baseline_sync_24)
                            },
                        text =
                            if (isSavedToLocal) {
                                Res.string.saved_to_local_playlist
                            } else {
                                Res.string.save_to_local_playlist
                            },
                        enable = !isSavedToLocal,
                    ) {
                        onSaveToLocal.invoke()
                        hideModalBottomSheet()
                    }
                }
                val shareTitle = stringResource(Res.string.share)
                ActionButton(
                    icon = painterResource(Res.drawable.baseline_share_24),
                    text = Res.string.share,
                ) {
                    val url = "https://music.youtube.com/playlist?list=${
                        playlistId.replaceFirst(
                            "VL",
                            "",
                        )
                    }"
                    shareUrl(shareTitle, url)
                }
                EndOfModalBottomSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistBottomSheet(
    isBottomSheetVisible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    ytPlaylistId: String? = null,
    onEditTitle: (newTitle: String) -> Unit,
    onEditThumbnail: (newThumbnailUri: String) -> Unit,
    onAddToQueue: () -> Unit,
    onSync: () -> Unit,
    onUpdatePlaylist: () -> Unit,
    onDelete: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var showEditTitle by remember { mutableStateOf(false) }
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val hideModalBottomSheet: () -> Unit =
        {
            coroutineScope.launch {
                modelBottomSheetState.hide()
                onDismiss()
            }
        }
    val resultLauncher =
        photoPickerResult {
            it?.let {
                onEditThumbnail(it)
            }
        }
    if (showEditTitle) {
        var newTitle by remember { mutableStateOf(title) }
        val showEditTitleSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    showEditTitleSheetState.hide()
                    onDismiss()
                }
            }
        ModalBottomSheet(
            onDismissRequest = { showEditTitle = false },
            sheetState = showEditTitleSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = Color(0xFF474545),
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(Res.string.title))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    val playlistNameError = stringResource(Res.string.playlist_name_cannot_be_empty)
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                showToast(playlistNameError, ToastGravity.Bottom)
                            } else {
                                onEditTitle(newTitle)
                                hideEditTitleBottomSheet()
                                hideModalBottomSheet()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(Res.string.save))
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = Color(0xFF474545),
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    ActionButton(icon = painterResource(Res.drawable.baseline_edit_24), text = Res.string.edit_title) {
                        showEditTitle = true
                    }
                    ActionButton(icon = painterResource(Res.drawable.baseline_add_photo_alternate_24), text = Res.string.edit_thumbnail) {
                        resultLauncher.launch()
                    }
                    ActionButton(icon = painterResource(Res.drawable.baseline_queue_music_24), text = Res.string.add_to_queue) {
                        onAddToQueue()
                    }
                    ActionButton(
                        icon =
                            if (ytPlaylistId != null) {
                                painterResource(Res.drawable.baseline_sync_disabled_24)
                            } else {
                                painterResource(Res.drawable.baseline_sync_24)
                            },
                        text =
                            if (ytPlaylistId != null) {
                                Res.string.synced
                            } else {
                                Res.string.sync
                            },
                    ) {
                        onSync()
                    }
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_update_24),
                        text = Res.string.update_playlist,
                        enable = (ytPlaylistId != null),
                    ) {
                        onUpdatePlaylist()
                    }
                    ActionButton(icon = painterResource(Res.drawable.baseline_delete_24), text = Res.string.delete_playlist) {
                        onDelete()
                        hideModalBottomSheet()
                    }
                    val shareTitle = stringResource(Res.string.share_url)
                    ActionButton(
                        icon = painterResource(Res.drawable.baseline_share_24),
                        text = if (ytPlaylistId != null) Res.string.share else Res.string.sync_first,
                        enable = (ytPlaylistId != null),
                    ) {
                        val url = "https://music.youtube.com/playlist?list=${
                            ytPlaylistId?.replaceFirst(
                                "VL",
                                "",
                            )
                        }"
                        shareUrl(shareTitle, url)
                    }
                    EndOfModalBottomSheet()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortPlaylistBottomSheet(
    selectedState: FilterState,
    onDismiss: () -> Unit,
    onSortChanged: (FilterState) -> Unit,
) {
    val modelBottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filterOptions =
        remember {
            listOf(
                FilterState.CustomOrder,
                FilterState.NewerFirst,
                FilterState.OlderFirst,
                FilterState.Title,
            )
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors =
                        CardDefaults.cardColors().copy(
                            containerColor = Color(0xFF474545),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Text(
                    stringResource(Res.string.sort_by),
                    style = typo().labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .padding(start = 16.dp, top = 16.dp, bottom = 24.dp)
                            .align(Alignment.Start),
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(filterOptions, key = { filterOption -> filterOption.hashCode() }) { filterOption ->
                        val isSelected = filterOption == selectedState
                        Row(
                            Modifier
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onSortChanged(filterOption)
                                    onDismiss()
                                }.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(filterOption.displayNameRes()),
                                style = typo().labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) seed else Color.White,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Image(
                                    painter = painterResource(Res.drawable.done),
                                    contentDescription = "Selected",
                                    colorFilter = ColorFilter.tint(seed),
                                    modifier = Modifier.size(32.dp),
                                )
                            } else {
                                Spacer(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
                EndOfModalBottomSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevLogInBottomSheet(
    onDismiss: () -> Unit,
    type: DevLogInType,
    onDone: (String, String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    var value by rememberSaveable { mutableStateOf("") }
    var secondValue by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors =
                        CardDefaults.cardColors().copy(
                            containerColor = Color(0xFF474545),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = runBlocking { type.getTitle() }, style = typo().labelSmall)
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    value = value,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    onValueChange = { value = it },
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(5.dp))
                if (type == DevLogInType.YouTube) {
                    Text(text = "Netscape cookie", style = typo().labelSmall)
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = secondValue,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                        onValueChange = { secondValue = it },
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }
                TextButton(
                    onClick = {
                        if (value.isNotEmpty() && value.isNotBlank() &&
                            (type != DevLogInType.YouTube || (secondValue.isNotEmpty() && secondValue.isNotBlank()))
                        ) {
                            showToast(runBlocking { getString(Res.string.processing) }, ToastGravity.Bottom)
                            onDismiss()
                            onDone(value, secondValue)
                        } else {
                            showToast(runBlocking { getString(Res.string.can_not_be_empty) }, ToastGravity.Bottom)
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                ) {
                    Text(text = stringResource(Res.string.set), style = typo().labelSmall)
                }
                Spacer(modifier = Modifier.height(5.dp))
                EndOfModalBottomSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevCookieLogInBottomSheet(
    onDismiss: () -> Unit,
    type: DevLogInType,
    cookies: List<Pair<String, String?>>,
) {
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(5.dp))
                Card(
                    modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                    colors =
                        CardDefaults.cardColors().copy(
                            containerColor = Color(0xFF474545),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = stringResource(Res.string.list_all_cookies_of_this_page), style = typo().labelSmall)
                cookies.forEach { cookie ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            text = cookie.first,
                            style = typo().bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        SelectionContainer(
                            modifier = Modifier.weight(2f),
                        ) {
                            Text(
                                text = cookie.second ?: "",
                                style = typo().bodyMedium,
                            )
                        }
                        val copied = stringResource(Res.string.copied_to_clipboard)
                        IconButton(
                            onClick = {
                                copyToClipboard(cookie.first, cookie.second ?: "")
                                showToast(copied, ToastGravity.Bottom)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                            )
                        }
                    }
                }
                EndOfModalBottomSheet()
            }
        }
    }
}

@Composable
fun EndOfModalBottomSheet() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                        .value
                        .toInt()
                        .dp + 8.dp,
                ),
    ) {}
}

sealed class DevLogInType {
    data object Spotify : DevLogInType()

    data object YouTube : DevLogInType()

    data object Discord : DevLogInType()

    suspend fun getTitle(): String =
        when (this) {
            is Spotify -> getString(Res.string.your_sp_dc_param_of_spotify_cookie)
            is YouTube -> getString(Res.string.your_youtube_cookie)
            is Discord -> getString(Res.string.your_discord_token)
        }
}