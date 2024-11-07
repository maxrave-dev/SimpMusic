package com.maxrave.simpmusic.ui.component

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.greyScale
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetUIEvent
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingBottomSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    song: SongEntity?,
    viewModel: NowPlayingBottomSheetViewModel = koinViewModel(),
    setSleepTimerEnable: Boolean = false,
    changeMainLyricsProviderEnable: Boolean = false,
    // Delete is specific to playlist
    onDelete: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
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

    LaunchedEffect(uiState) {
        if (uiState.songUIState.videoId.isNotEmpty() && !isBottomSheetVisible) {
            isBottomSheetVisible = true
        }
    }

    LaunchedEffect(key1 = song) {
        viewModel.setSongEntity(song)
    }

    if (addToAPlaylist) {
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = uiState.listLocalPlaylist,
            onDismiss = { addToAPlaylist = false },
            onClick = {
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToPlaylist(it.id))
            },
            videoId = uiState.songUIState.videoId
        )
    }
    if (artist) {
        ArtistModalBottomSheet(
            isBottomSheetVisible = artist,
            artists = uiState.songUIState.listArtists,
            navController = navController,
            onDismiss = { artist = false },
        )
    }

    if (sleepTimer) {
        SleepTimerBottomSheet(onDismiss = { sleepTimer = false }) { minutes: Int ->
            if (setSleepTimerEnable) {
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.SetSleepTimer(
                    cancel = false,
                    minutes = minutes
                ))
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
                    viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.SetSleepTimer(
                        cancel = true
                    ))
                }) {
                    Text(text = stringResource(id = R.string.yes), style = typo.labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { sleepTimerWarning = false }) {
                    Text(text = stringResource(id = R.string.cancel), style = typo.labelSmall)
                }
            },
            title = {
                Text(text = stringResource(id = R.string.warning), style = typo.labelSmall)
            },
            text = {
                Text(text = stringResource(id = R.string.sleep_timer_warning), style = typo.bodyMedium)
            },
        )
    }

    if (mainLyricsProvider) {
        var selected by remember {
            mutableIntStateOf(
                if (uiState.mainLyricsProvider == DataStoreManager.MUSIXMATCH) 0 else 1
            )
        }

        AlertDialog(
            onDismissRequest = { mainLyricsProvider = false },
            containerColor = Color(0xFF242424),
            title = {
                Text(
                    text = stringResource(id = R.string.main_lyrics_provider),
                    style = typo.titleMedium,
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == 0,
                            onClick = {
                                selected = 0
                            }
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(text = stringResource(id = R.string.musixmatch), style = typo.labelSmall)
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == 1,
                            onClick = {
                                selected = 1
                            }
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(text = stringResource(id = R.string.youtube_transcript), style = typo.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ChangeLyricsProvider(
                            if (selected == 0) DataStoreManager.MUSIXMATCH else DataStoreManager.YOUTUBE
                        ))
                        mainLyricsProvider = false
                    },
                ) {
                    Text(text = stringResource(id = R.string.yes), style = typo.labelSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mainLyricsProvider = false
                }) {
                    Text(text = stringResource(id = R.string.cancel), style = typo.labelSmall)
                }
            }
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
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoilImage(
                            imageModel = { uiState.songUIState.thumbnails },
                            imageOptions =
                                ImageOptions(
                                    contentScale = ContentScale.Inside,
                                    alignment = Alignment.Center,
                                ),
                            previewPlaceholder = painterResource(id = R.drawable.holder),
                            component =
                                rememberImageComponent {
                                    CrossfadePlugin(
                                        duration = 550,
                                    )
                                },
                            modifier =
                            Modifier
                                .align(Alignment.CenterVertically)
                                .size(60.dp),
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = uiState.songUIState.title,
                                style = typo.labelMedium,
                                maxLines = 1,
                                modifier =
                                Modifier
                                    .wrapContentHeight(Alignment.CenterVertically)
                                    .basicMarquee(
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    )
                                    .focusable(),
                            )
                            Text(
                                text = uiState.songUIState.listArtists.toListName().connectArtists(),
                                style = typo.bodyMedium,
                                maxLines = 1,
                                modifier =
                                Modifier
                                    .wrapContentHeight(Alignment.CenterVertically)
                                    .basicMarquee(
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    )
                                    .focusable(),
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
                                icon = painterResource(id = R.drawable.baseline_delete_24),
                                text = R.string.delete_song_from_playlist,
                            ) {
                                onDelete?.invoke()
                                hideModalBottomSheet()
                            }
                        }
                    }
                    CheckBoxActionButton(
                        defaultChecked = uiState.songUIState.liked,
                        onChangeListener = {
                            viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ToggleLike)
                        },
                    )
                    ActionButton(
                        icon =
                        when (uiState.songUIState.downloadState) {
                            DownloadState.STATE_NOT_DOWNLOADED ->
                                painterResource(
                                    R.drawable.outline_download_for_offline_24,
                                )

                            DownloadState.STATE_DOWNLOADING ->
                                painterResource(
                                    R.drawable.baseline_downloading_white,
                                )

                            DownloadState.STATE_DOWNLOADED ->
                                painterResource(
                                    R.drawable.baseline_downloaded,
                                )

                            DownloadState.STATE_PREPARING ->
                                painterResource(
                                    R.drawable.baseline_downloading_white,
                                )

                            else ->
                                painterResource(
                                    R.drawable.outline_download_for_offline_24,
                                )
                        },
                        text =
                        when (uiState.songUIState.downloadState) {
                            DownloadState.STATE_NOT_DOWNLOADED -> R.string.download
                            DownloadState.STATE_DOWNLOADING -> R.string.downloading
                            DownloadState.STATE_DOWNLOADED -> R.string.downloaded
                            DownloadState.STATE_PREPARING -> R.string.downloading
                            else -> R.string.download
                        },
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_playlist_add_24),
                        text = R.string.add_to_a_playlist,
                    ) {
                        addToAPlaylist = true
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.play_circle),
                        text = R.string.play_next,
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.PlayNext)
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_queue_music_24),
                        text = R.string.add_to_queue,
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToQueue)
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_people_alt_24),
                        text = R.string.artists,
                    ) {
                        artist = true
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_album_24),
                        text = if (uiState.songUIState.album == null) R.string.no_album else null,
                        textString = uiState.songUIState.album?.name,
                        enable = uiState.songUIState.album != null,
                    ) {
                        navController.navigateSafe(
                            R.id.action_global_albumFragment,
                            Bundle().apply {
                                putString("browseId", uiState.songUIState.album?.id)
                            },
                        )
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_sensors_24),
                        text = R.string.start_radio,
                    ) {
                        val args = Bundle()
                        args.putString("radioId", "RDAMVM${uiState.songUIState.videoId}")
                        args.putString(
                            "videoId",
                            uiState.songUIState.videoId,
                        )
                        hideModalBottomSheet()
                        navController.navigateSafe(
                            R.id.action_global_playlistFragment,
                            args,
                        )
                    }
                    Crossfade(targetState = changeMainLyricsProviderEnable) {
                        if (it) {
                            ActionButton(
                                icon = painterResource(id = R.drawable.baseline_lyrics_24),
                                text = R.string.main_lyrics_provider,
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
                                        icon = painterResource(id = R.drawable.baseline_access_alarm_24),
                                        textString = stringResource(id = R.string.sleep_timer, sleepTimerState.timeRemaining.toString()),
                                        text = null,
                                        textColor = seed,
                                        iconColor = seed,
                                    ) {
                                        sleepTimerWarning = true
                                    }
                                }
                                else {
                                    ActionButton(
                                        icon = painterResource(id = R.drawable.baseline_access_alarm_24),
                                        text = R.string.sleep_timer_off,
                                    ) {
                                        sleepTimer = true
                                    }
                                }
                            }
                        }
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_share_24),
                        text = R.string.share,
                    ) {
                        viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Share)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: Painter,
    @StringRes text: Int?,
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
            .clickable {
                if (enable) onClick()
            }
            .apply {
                if (!enable) greyScale()
            },
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
                    )
                    .padding(12.dp),
                colorFilter =
                    if (enable) {
                        ColorFilter.tint(iconColor)
                    } else {
                        ColorFilter.colorMatrix(
                            ColorMatrix().apply {
                                setToSaturation(
                                    0f,
                                )
                            },
                        )
                    },
            )

            Text(
                text = if (text != null) stringResource(text) else textString ?: "",
                style = typo.labelSmall,
                color = textColor ?: Color.Unspecified,
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
                HeartCheckBox(checked = stateChecked, size = 30)
            }
            Text(
                text =
                    if (stateChecked) {
                        stringResource(
                            R.string.liked,
                        )
                    } else {
                        stringResource(R.string.like)
                    },
                style = typo.labelSmall,
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
            )
            .clickable {
                onStateChange?.invoke()
            },
    ) {
        Crossfade(targetState = checked, modifier = Modifier.fillMaxSize()) {
            if (it) {
                Image(
                    painter = painterResource(id = R.drawable.baseline_favorite_24),
                    contentDescription = "Favorite checked",
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.baseline_favorite_border_24),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistModalBottomSheet(
    isBottomSheetVisible: Boolean,
    listLocalPlaylist: List<LocalPlaylistEntity>,
    videoId: String? = null,
    onClick: (LocalPlaylistEntity) -> Unit,
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
                        items(listLocalPlaylist) { playlist ->
                            Box(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        onClick(playlist)
                                        hideModalBottomSheet()
                                    },
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
                                                id = R.drawable.baseline_playlist_add_24,
                                            ),
                                        contentDescription = "",
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = playlist.title,
                                        style = typo.labelSmall,
                                    )
                                }
                                Crossfade(
                                    targetState = playlist.tracks?.contains(videoId) == true,
                                ) {
                                    if (it) {
                                        Image(
                                            painter = painterResource(id = R.drawable.done),
                                            contentDescription = "",
                                            modifier = Modifier.align(Alignment.CenterEnd),
                                        )
                                    }
                                }
                            }
                        }
                    }
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
    val context = LocalContext.current
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
                Text(text = stringResource(id = R.string.sleep_minutes), style = typo.labelSmall)
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    value = minutes.toString(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    onValueChange = { if (it.isDigitsOnly() && it.isNotEmpty() && it.isNotBlank()) minutes = it.toInt() },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                Spacer(modifier = Modifier.height(5.dp))
                TextButton(onClick = {
                    if (minutes > 0) {
                        onSetTimer(minutes)
                        coroutineScope.launch {
                            modelBottomSheetState.hide()
                            onDismiss()
                        }
                    }
                    else {
                        Toast.makeText(context, context.getString(R.string.sleep_timer_set_error), Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                ) {
                    Text(text = stringResource(R.string.set), style = typo.labelSmall)
                }
                Spacer(modifier = Modifier.height(5.dp))
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
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val hideModalBottomSheet: () -> Unit =
        { coroutineScope.launch {
            modelBottomSheetState.hide()
            onDismiss()
        } }
    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
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
                                        if (!artist.id.isNullOrBlank()) {
                                            navController.navigateSafe(
                                                R.id.action_global_artistFragment,
                                                Bundle().apply {
                                                    putString("channelId", artist.id)
                                                },
                                            )
                                        }
                                        hideModalBottomSheet()
                                    }
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
                                                id = R.drawable.baseline_people_alt_24,
                                            ),
                                        contentDescription = "",
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = artist.name,
                                        style = typo.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
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
        { coroutineScope.launch {
            modelBottomSheetState.hide()
            onDismiss()
        } }
    val context = LocalContext.current
    val resultLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                Log.d("ID", Build.ID.toString())
                val intentRef = activityResult.data
                val data = intentRef?.data
                if (data != null) {
                    val contentResolver = context.contentResolver

                    val takeFlags: Int =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    // Check for the freshest data.
                    context.grantUriPermission(
                        context.packageName,
                        data,
                        takeFlags,
                    )
                    contentResolver?.takePersistableUriPermission(data, takeFlags)
                    val uri = data.toString()
                    onEditThumbnail(uri)
                }
            }
        }
    if (showEditTitle) {
        var newTitle by remember { mutableStateOf(title) }
        val showEditTitleSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            { coroutineScope.launch {
                showEditTitleSheetState.hide()
                onDismiss()
            } }
        ModalBottomSheet(
            onDismissRequest = { showEditTitle = false },
            sheetState = showEditTitleSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
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
                            Text(text = stringResource(id = R.string.title))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.playlist_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
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
                        Text(text = stringResource(id = R.string.save))
                    }
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
                    ActionButton(icon = painterResource(id = R.drawable.baseline_edit_24), text = R.string.edit_title) {
                        showEditTitle = true
                    }
                    ActionButton(icon = painterResource(id = R.drawable.baseline_add_photo_alternate_24), text = R.string.edit_thumbnail) {
                        val intent = Intent()
                        intent.type = "image/*"
                        intent.action = Intent.ACTION_OPEN_DOCUMENT
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        resultLauncher.launch(intent)
                    }
                    ActionButton(icon = painterResource(id = R.drawable.baseline_queue_music_24), text = R.string.add_to_queue) {
                        onAddToQueue()
                    }
                    ActionButton(
                        icon =
                            if (ytPlaylistId != null) {
                                painterResource(id = R.drawable.baseline_sync_disabled_24)
                            } else {
                                painterResource(id = R.drawable.baseline_sync_24)
                            },
                        text =
                            if (ytPlaylistId != null) {
                                R.string.synced
                            } else {
                                R.string.sync
                            },
                    ) {
                        onSync()
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_update_24),
                        text = R.string.update_playlist,
                        enable = (ytPlaylistId != null),
                    ) {
                        onUpdatePlaylist()
                    }
                    ActionButton(icon = painterResource(id = R.drawable.baseline_delete_24), text = R.string.delete_playlist) {
                        onDelete()
                        hideModalBottomSheet()
                    }
                }
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode",
    group = "Local Playlist",
)
@Composable
fun LocalPlaylistBottomSheetPreview() {
}