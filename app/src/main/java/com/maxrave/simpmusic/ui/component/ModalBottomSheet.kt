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
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.ripple
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.greyScale
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import kotlinx.coroutines.launch

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingBottomSheet(
    isBottomSheetVisible: Boolean,
    onDismiss: () -> Unit,
    navController: NavController,
    sharedViewModel: SharedViewModel,
    songEntity: State<SongEntity?>,
    onToggleLike: (Boolean) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onMainLyricsProvider: ((String) -> Unit)? = null,
    onSleepTimer: (() -> Unit)? = null,
    getLocalPlaylist: () -> Unit,
    listLocalPlaylist: State<List<LocalPlaylistEntity>?>,
    onAddToLocalPlaylist: (LocalPlaylistEntity) -> Unit = { _ -> },
) {
    val downloadState = songEntity.value?.downloadState
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val hideModalBottomSheet: () -> Unit =
        { coroutineScope.launch { modelBottomSheetState.hide() } }

    var addToAPlaylist by remember { mutableStateOf(false) }
    var artist by remember { mutableStateOf(false) }
    if (addToAPlaylist && listLocalPlaylist.value != null) {
        getLocalPlaylist()
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = addToAPlaylist,
            listLocalPlaylist = listLocalPlaylist.value ?: arrayListOf(),
            onDismiss = { addToAPlaylist = false },
            onClick = { onAddToLocalPlaylist(it) },
            videoId = songEntity.value?.videoId,
        )
    }
    if (artist) {
        ArtistModalBottomSheet(
            isBottomSheetVisible = artist,
            artists =
                songEntity.value?.artistName?.mapIndexed { index, name ->
                    Artist(
                        id = songEntity.value?.artistId?.get(index),
                        name = name,
                    )
                } ?: arrayListOf(),
            navController = navController,
            onDismiss = { artist = false },
        )
    }

    if (isBottomSheetVisible && songEntity.value != null) {
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
                            imageModel = { songEntity.value?.thumbnails },
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
                                text = songEntity.value?.title ?: "",
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
                                text = songEntity.value?.artistName?.connectArtists() ?: "",
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
                                if (onDelete != null) {
                                    onDelete()
                                    hideModalBottomSheet()
                                }
                            }
                        }
                    }
                    CheckBoxActionButton(
                        defaultChecked = songEntity.value?.liked ?: false,
                        onChangeListener = onToggleLike,
                    )
                    Crossfade(targetState = onDownload != null) {
                        if (it && onDownload != null) {
                            ActionButton(
                                icon =
                                    when (downloadState) {
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
                                    when (downloadState) {
                                        DownloadState.STATE_NOT_DOWNLOADED -> R.string.download
                                        DownloadState.STATE_DOWNLOADING -> R.string.downloading
                                        DownloadState.STATE_DOWNLOADED -> R.string.downloaded
                                        DownloadState.STATE_PREPARING -> R.string.downloading
                                        else -> R.string.download
                                    },
                            ) {
                                onDownload()
                            }
                        }
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
                        sharedViewModel.playNext(songEntity.value?.toTrack() ?: return@ActionButton)
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_queue_music_24),
                        text = R.string.add_to_queue,
                    ) {
                        sharedViewModel.addToQueue(
                            songEntity.value?.toTrack() ?: return@ActionButton,
                        )
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_people_alt_24),
                        text = R.string.artists,
                    ) {
                        artist = true
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_album_24),
                        text = if (songEntity.value?.albumName.isNullOrBlank()) R.string.no_album else null,
                        textString = songEntity.value?.albumName,
                        enable = !songEntity.value?.albumName.isNullOrBlank(),
                    ) {
                        navController.navigateSafe(
                            R.id.action_global_albumFragment,
                            Bundle().apply {
                                putString("browseId", songEntity.value?.albumId)
                            },
                        )
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_sensors_24),
                        text = R.string.start_radio,
                    ) {
                        val args = Bundle()
                        args.putString("radioId", "RDAMVM${songEntity.value?.videoId}")
                        args.putString(
                            "videoId",
                            songEntity.value?.videoId,
                        )
                        hideModalBottomSheet()
                        navController.navigateSafe(
                            R.id.action_global_playlistFragment,
                            args,
                        )
                    }
                    Crossfade(targetState = onMainLyricsProvider != null) {
                        if (it && onMainLyricsProvider != null) {
                            ActionButton(
                                icon = painterResource(id = R.drawable.baseline_lyrics_24),
                                text = R.string.main_lyrics_provider,
                            ) {
                                onMainLyricsProvider("")
                            }
                        }
                    }
                    Crossfade(targetState = onSleepTimer != null) {
                        if (it && onSleepTimer != null) {
                            ActionButton(
                                icon = painterResource(id = R.drawable.baseline_access_alarm_24),
                                text = R.string.sleep_timer_off,
                            ) {
                                onSleepTimer()
                            }
                        }
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_share_24),
                        text = R.string.share,
                    ) {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        val url = "https://youtube.com/watch?v=${songEntity.value?.videoId}"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                        val chooserIntent =
                            Intent.createChooser(shareIntent, context.getString(R.string.share_url))
                        context.startActivity(chooserIntent)
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
    enable: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.CenterVertically)
                .clickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    onClick = if (enable) onClick else ({}),
                    indication = ripple(),
                )
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
                        null
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
                style = typo.bodyLarge,
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
                .clickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },
                    onClick = {
                        stateChecked = !stateChecked
                        onChangeListener(stateChecked)
                    },
                    indication = ripple(),
                ),
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
                style = typo.bodyLarge,
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
                .clickable(
                    onClick = onStateChange ?: {},
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                ),
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
        { coroutineScope.launch { modelBottomSheetState.hide() } }
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
                                        .padding(20.dp)
                                        .clickable(
                                            onClick = {
                                                onClick(playlist)
                                                hideModalBottomSheet()
                                            },
                                            indication = ripple(),
                                            interactionSource = remember { MutableInteractionSource() },
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
                                                id = R.drawable.baseline_playlist_add_24,
                                            ),
                                        contentDescription = "",
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = playlist.title,
                                        style = typo.bodyLarge,
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
        { coroutineScope.launch { modelBottomSheetState.hide() } }
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
                                        .clickable(
                                            onClick = {
                                                if (!artist.id.isNullOrBlank()) {
                                                    navController.navigateSafe(
                                                        R.id.action_global_artistFragment,
                                                        Bundle().apply {
                                                            putString("channelId", artist.id)
                                                        },
                                                    )
                                                }
                                                hideModalBottomSheet()
                                            },
                                            indication = ripple(),
                                            interactionSource = remember { MutableInteractionSource() },
                                        ),
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
                                        style = typo.bodyLarge,
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
    localPlaylist: LocalPlaylistEntity,
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
        { coroutineScope.launch { modelBottomSheetState.hide() } }
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
        var newTitle by remember { mutableStateOf(localPlaylist.title) }
        val showEditTitleSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            { coroutineScope.launch { showEditTitleSheetState.hide() } }
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
                            if (localPlaylist.youtubePlaylistId == null) {
                                painterResource(id = R.drawable.baseline_sync_24)
                            } else {
                                painterResource(id = R.drawable.baseline_sync_disabled_24)
                            },
                        text =
                            if (localPlaylist.youtubePlaylistId == null) {
                                R.string.sync
                            } else {
                                R.string.synced
                            },
                    ) {
                        onSync()
                    }
                    ActionButton(
                        icon = painterResource(id = R.drawable.baseline_update_24),
                        text = R.string.update_playlist,
                        enable = (localPlaylist.youtubePlaylistId != null),
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