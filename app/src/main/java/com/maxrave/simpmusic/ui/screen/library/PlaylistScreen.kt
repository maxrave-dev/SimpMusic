package com.maxrave.simpmusic.ui.screen.library

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.ripple
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants.IterateForever
import com.airbnb.lottie.compose.rememberLottieComposition
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.data.queue.Queue.ASC
import com.maxrave.simpmusic.data.queue.Queue.DESC
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.toArrayListTrack
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.LocalPlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistItems
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SuggestItems
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.FilterState
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.palette.PalettePlugin
import com.skydoves.landscapist.palette.rememberPaletteState
import com.skydoves.landscapist.placeholder.placeholder.PlaceholderPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.format.DateTimeFormatter

@UnstableApi
@ExperimentalFoundationApi
@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
fun PlaylistScreen(
    id: Long?,
    sharedViewModel: SharedViewModel,
    viewModel: LocalPlaylistViewModel,
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.downloading_animation),
    )

    val offset by viewModel.offset.collectAsState()
    val isLoadingMore by viewModel.loadingMore.collectAsState(initial = false)
    val filterState by viewModel.filter.collectAsState()

    val aiPainter = painterResource(id = R.drawable.baseline_tips_and_updates_24)
    val limit = 1.5f
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progressAnimated by transition.animateFloat(
        initialValue = -limit,
        targetValue = limit,
        animationSpec =
            infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmer",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "rotation",
    )

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    val lastItemVisible by remember {
        derivedStateOf {
            lazyState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lazyState.layoutInfo.totalItemsCount - 1
        }
    }
    val downloadState by viewModel.playlistDownloadState.collectAsState()
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestions by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestButton by rememberSaveable { mutableStateOf(false) }
    var palette by rememberPaletteState(null)
    val bg by viewModel.brush.collectAsState()
    val localPlaylist by viewModel.localPlaylist.collectAsState()
    val listTrack by viewModel.listTrack.collectAsState()
    val playingTrack by sharedViewModel.nowPlayingMediaItem.collectAsState(initial = null)
    val isPlaying by sharedViewModel.isPlaying.collectAsState()
    val suggestedTracks by viewModel.listSuggestions.collectAsState()
    val suggestionsLoading by viewModel.loading.collectAsState()
    val fullListTracks by viewModel.fullListTracks.collectAsState()
    var showSyncAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showUnsyncAlertDialog by rememberSaveable { mutableStateOf(false) }
    var shouldDownload by remember {
        mutableStateOf(false)
    }
    var shouldAddAllToQueue by remember {
        mutableStateOf(false)
    }
    var firstTimeGetLocalPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var currentItem by remember {
        mutableStateOf<SongEntity?>(null)
    }

    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var playlistBottomSheetShow by remember {
        mutableStateOf(false)
    }

    val onPlaylistItemClick: (videoId: String) -> Unit = { videoId ->
        val list = listTrack
        val track = listTrack?.find { it.videoId == videoId }
        if (!list.isNullOrEmpty() && track != null) {
            val args = Bundle()
            args.putString("type", Config.PLAYLIST_CLICK)
            args.putString("videoId", track.videoId)
            args.putString("from", "Playlist \"${localPlaylist?.title}\"")
            args.putInt("index", list.indexOf(track))
            if (localPlaylist?.downloadState == DownloadState.STATE_DOWNLOADED) {
                args.putInt("downloaded", 1)
            }
            if (localPlaylist?.syncedWithYouTubePlaylist == 1) {
                args.putString(
                    "playlistId",
                    localPlaylist?.youtubePlaylistId?.replaceFirst("VL", ""),
                )
            }
            Queue.initPlaylist(
                Queue.LOCAL_PLAYLIST_ID + localPlaylist?.id,
                "Playlist \"${localPlaylist?.title}\"",
                Queue.PlaylistType.PLAYLIST,
            )
            Queue.setNowPlaying(track.toTrack())
            val tempList: ArrayList<Track> = arrayListOf()
            for (i in list) {
                tempList.add(i.toTrack())
            }
            Queue.addAll(tempList)
            if (Queue.getQueue().size >= 1) {
                Queue.removeTrackWithIndex(list.indexOf(track))
            }
            if (offset > 0) {
                Queue.setContinuation(
                    Queue.LOCAL_PLAYLIST_ID + localPlaylist?.id,
                    if (filterState == FilterState.OlderFirst) {
                        (ASC + offset.toString())
                    } else {
                        (DESC + offset)
                    },
                )
            }
            navController.navigateSafe(R.id.action_global_nowPlayingFragment, args)
        }
    }
    val onItemMoreClick: (videoId: String) -> Unit = { videoId ->
        currentItem = listTrack?.findLast { it.videoId == videoId }
        if (currentItem != null) {
            itemBottomSheetShow = true
        }
    }
    val onPlaylistMoreClick: () -> Unit = {
        playlistBottomSheetShow = true
    }

    LaunchedEffect(key1 = shouldShowSuggestions) {
        if (suggestedTracks.isNullOrEmpty()) {
            localPlaylist?.youtubePlaylistId?.let { viewModel.getSuggestions(it) }
        }
    }
    LaunchedEffect(key1 = id) {
        if (id != viewModel.id.value && id != null) {
            Log.w("PlaylistScreen", "new id: $id")
            viewModel.id.postValue(id)
            viewModel.setOffset(0)
            viewModel.removeListSuggestion()
            viewModel.removeData()
            viewModel.removeFullListTracks()
            viewModel.getLocalPlaylist(id)
            delay(100)
            firstTimeGetLocalPlaylist = true
        }
    }
    LaunchedEffect(key1 = fullListTracks, key2 = shouldDownload) {
        val flt = fullListTracks.toArrayListTrack()
        if (flt.isNotEmpty() && shouldDownload) {
            Log.w("PlaylistScreen", "fullListTracks: $flt")
            shouldDownload = false
            val listJob: ArrayList<SongEntity> = arrayListOf()
            for (song in viewModel.listTrack.value!!) {
                if (song.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
                    listJob.add(song)
                }
            }
            viewModel.listJob.value = listJob
            Log.d("PlaylistFragment", "ListJob: ${viewModel.listJob.value}")
            listJob.forEach { job ->
                val downloadRequest =
                    DownloadRequest.Builder(job.videoId, job.videoId.toUri())
                        .setData(job.title.toByteArray())
                        .setCustomCacheKey(job.videoId)
                        .build()
                viewModel.updateDownloadState(
                    job.videoId,
                    DownloadState.STATE_DOWNLOADING,
                )
                DownloadService.sendAddDownload(
                    context,
                    MusicDownloadService::class.java,
                    downloadRequest,
                    false,
                )
                viewModel.getDownloadStateFromService(job.videoId)
            }
            localPlaylist?.let { viewModel.downloadFullPlaylistState(it.id) }
        }
    }
    LaunchedEffect(key1 = fullListTracks, key2 = shouldAddAllToQueue) {
        val flt = fullListTracks.toArrayListTrack()
        if (flt.isNotEmpty() && shouldAddAllToQueue) {
            shouldAddAllToQueue = false
            sharedViewModel.addListToQueue(flt)
            Toast.makeText(context, context.getString(R.string.added_to_queue), Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(key1 = localPlaylist, key2 = firstTimeGetLocalPlaylist) {
        if (localPlaylist != null && firstTimeGetLocalPlaylist && localPlaylist?.id == viewModel.id.value) {
            Log.w("PlaylistScreen", "new localPlaylist: $localPlaylist")
            localPlaylist?.id?.let { viewModel.getListTrack(it, offset, filterState) }
            localPlaylist?.downloadState?.let { viewModel.playlistDownloadState.emit(it) }
            shouldShowSuggestButton =
                localPlaylist?.youtubePlaylistId != null &&
                localPlaylist?.youtubePlaylistId != ""
            firstTimeGetLocalPlaylist = false
        }
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    LaunchedEffect(key1 = lastItemVisible) {
        if (lastItemVisible && offset > 0 && !isLoadingMore) {
            localPlaylist?.id?.let { viewModel.getListTrack(it, offset, filterState) }
        }
    }
    LaunchedEffect(key1 = palette) {
        val p = palette
        if (p != null) {
            val defaultColor = 0x000000
            var startColor = p.getDarkVibrantColor(defaultColor)
            if (startColor == defaultColor) {
                startColor = p.getDarkMutedColor(defaultColor)
                if (startColor == defaultColor) {
                    startColor = p.getVibrantColor(defaultColor)
                    if (startColor == defaultColor) {
                        startColor =
                            p.getMutedColor(defaultColor)
                        if (startColor == defaultColor) {
                            startColor =
                                p.getLightVibrantColor(
                                    defaultColor,
                                )
                            if (startColor == defaultColor) {
                                startColor =
                                    p.getLightMutedColor(
                                        defaultColor,
                                    )
                            }
                        }
                    }
                }
            }
            val endColor =
                context.resources.getColor(R.color.md_theme_dark_background, null)
            val colorAndroid = ColorUtils.setAlphaComponent(startColor, 255)
            val brush =
                listOf(
                    Color(colorAndroid),
                    Color(endColor),
                )
            viewModel.setBrush(brush)
        }
    }
//    Box {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.Black),
        state = lazyState,
    ) {
        item(contentType = "header") {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.Transparent),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
//                                .haze(
//                                    hazeState,
//                                    style = HazeMaterials.regular(),
//                                ),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(
                                    RoundedCornerShape(8.dp),
                                ).angledGradientBackground(bg, 25f),
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    brush =
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0x75000000),
                                                Color.Black,
                                            ),
                                        ),
                                ),
                    )
                }
                Column(
                    Modifier
                        .background(Color.Transparent),
//                            .hazeChild(hazeState, style = HazeMaterials.regular()),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .wrapContentWidth()
                                .padding(16.dp)
                                .windowInsetsPadding(WindowInsets.statusBars),
                    ) {
                        RippleIconButton(
                            resId = R.drawable.baseline_arrow_back_ios_new_24,
                        ) {
                            navController.popBackStack()
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.Start,
                    ) {
                        CoilImage(
                            imageModel = {
                                localPlaylist?.thumbnail
                            },
                            imageOptions =
                                ImageOptions(
                                    contentScale = ContentScale.FillHeight,
                                    alignment = Alignment.Center,
                                ),
                            previewPlaceholder = painterResource(id = R.drawable.holder),
                            component =
                                rememberImageComponent {
                                    add(
                                        CrossfadePlugin(
                                            duration = 550,
                                        ),
                                    )
                                    add(
                                        PalettePlugin(
                                            paletteLoadedListener = {
                                                palette = it
                                            },
                                            useCache = true,
                                        ),
                                    )
                                    +PlaceholderPlugin.Loading(painterResource(id = R.drawable.holder))
                                    +PlaceholderPlugin.Failure(painterResource(id = R.drawable.holder))
                                },
                            modifier =
                                Modifier
                                    .height(250.dp)
                                    .wrapContentWidth()
                                    .align(Alignment.CenterHorizontally)
                                    .clip(
                                        RoundedCornerShape(8.dp),
                                    ),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                        ) {
                            Column(Modifier.padding(horizontal = 32.dp)) {
                                Spacer(modifier = Modifier.size(25.dp))
                                Text(
                                    text = localPlaylist?.title ?: "",
                                    style = typo.titleLarge,
                                    color = Color.White,
                                )
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.your_playlist),
                                        style = typo.bodyLarge,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text =
                                            stringResource(
                                                id = R.string.created_at,
                                                localPlaylist?.inLibrary?.format(
                                                    DateTimeFormatter.ofPattern(
                                                        "kk:mm - dd MMM uuuu",
                                                    ),
                                                ) ?: "",
                                            ),
                                        style = typo.bodyLarge,
                                        color = Color(0xC4FFFFFF),
                                    )
                                }
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RippleIconButton(
                                        resId = R.drawable.baseline_play_circle_24,
                                        fillMaxSize = true,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        val temp = listTrack
                                        if (!temp.isNullOrEmpty()) {
                                            val args = Bundle()
                                            args.putString("type", Config.ALBUM_CLICK)
                                            args.putString("videoId", temp.first().videoId)
                                            args.putString("from", "Playlist \"${(localPlaylist)?.title}\"")
                                            if (downloadState == DownloadState.STATE_DOWNLOADED) {
                                                args.putInt("downloaded", 1)
                                            }
                                            if (viewModel.localPlaylist.value?.syncedWithYouTubePlaylist == 1) {
                                                args.putString(
                                                    "playlistId",
                                                    viewModel.localPlaylist.value?.youtubePlaylistId?.replaceFirst("VL", ""),
                                                )
                                            }
                                            Queue.initPlaylist(
                                                Queue.LOCAL_PLAYLIST_ID + localPlaylist?.id,
                                                "Playlist \"${(viewModel.localPlaylist.value)?.title}\"",
                                                Queue.PlaylistType.PLAYLIST,
                                            )
                                            Queue.setNowPlaying(temp.first().toTrack())
                                            val tempList: ArrayList<Track> = arrayListOf()
                                            for (i in temp) {
                                                tempList.add(i.toTrack())
                                            }
                                            Queue.addAll(tempList)
                                            if (Queue.getQueue().size >= 1) {
                                                Queue.removeFirstTrackForPlaylistAndAlbum()
                                            }
                                            if (offset > 0) {
                                                Queue.setContinuation(
                                                    Queue.LOCAL_PLAYLIST_ID + localPlaylist?.id,
                                                    if (filterState == FilterState.OlderFirst) {
                                                        (ASC + offset.toString())
                                                    } else {
                                                        (DESC + offset)
                                                    },
                                                )
                                            }
                                            navController.navigateSafe(R.id.action_global_nowPlayingFragment, args)
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.playlist_is_empty), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    Spacer(modifier = Modifier.size(5.dp))
                                    Crossfade(targetState = downloadState) {
                                        when (it) {
                                            DownloadState.STATE_DOWNLOADED -> {
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .size(36.dp)
                                                            .clip(
                                                                CircleShape,
                                                            )
                                                            .clickable(
                                                                onClick = {
                                                                    Toast
                                                                        .makeText(
                                                                            context,
                                                                            context.getString(R.string.downloaded),
                                                                            Toast.LENGTH_SHORT,
                                                                        )
                                                                        .show()
                                                                },
                                                                interactionSource =
                                                                    remember {
                                                                        MutableInteractionSource()
                                                                    },
                                                                indication = ripple(),
                                                            ),
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.baseline_downloaded),
                                                        tint = Color(0xFF00A0CB),
                                                        contentDescription = "",
                                                        modifier =
                                                            Modifier
                                                                .size(36.dp)
                                                                .padding(2.dp),
                                                    )
                                                }
                                            }

                                            DownloadState.STATE_DOWNLOADING -> {
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .size(36.dp)
                                                            .clip(
                                                                CircleShape,
                                                            )
                                                            .clickable(
                                                                onClick = {
                                                                    Toast
                                                                        .makeText(
                                                                            context,
                                                                            context.getString(R.string.downloading),
                                                                            Toast.LENGTH_SHORT,
                                                                        )
                                                                        .show()
                                                                },
                                                                interactionSource =
                                                                    remember {
                                                                        MutableInteractionSource()
                                                                    },
                                                                indication = ripple(),
                                                            ),
                                                ) {
                                                    LottieAnimation(
                                                        composition,
                                                        iterations = IterateForever,
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                }
                                            }

                                            else -> {
                                                RippleIconButton(
                                                    fillMaxSize = true,
                                                    resId = R.drawable.download_button,
                                                    modifier = Modifier.size(36.dp),
                                                ) {
                                                    Log.w("PlaylistScreen", "downloadState: $downloadState")
                                                    localPlaylist?.let { it1 -> viewModel.getAllTracksOfPlaylist(it1.id) }
                                                    shouldDownload = true
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Spacer(Modifier.size(5.dp))
                                    AnimatedVisibility(visible = shouldShowSuggestButton) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .graphicsLayer {
                                                        compositingStrategy =
                                                            CompositingStrategy.Offscreen
                                                    }
                                                    .clickable(
                                                        onClick = {
                                                            shouldShowSuggestions = !shouldShowSuggestions
                                                        },
                                                        interactionSource =
                                                            remember {
                                                                MutableInteractionSource()
                                                            },
                                                        indication = ripple(),
                                                    )
                                                    .drawWithCache {
                                                        val width = size.width - 10
                                                        val height = size.height - 10

                                                        val offsetDraw = width * progressAnimated
                                                        val gradientColors =
                                                            listOf(
                                                                Color(0xFF4C82EF),
                                                                Color(0xFFD96570),
                                                            )
                                                        val brush =
                                                            Brush.linearGradient(
                                                                colors = gradientColors,
                                                                start = Offset(offsetDraw, 0f),
                                                                end =
                                                                    Offset(
                                                                        offsetDraw + width,
                                                                        height,
                                                                    ),
                                                            )

                                                        onDrawBehind {
                                                            // Destination
                                                            with(aiPainter) {
                                                                draw(
                                                                    size = Size(width, width),
                                                                )
                                                            }

                                                            // Source
                                                            drawRect(
                                                                brush = brush,
                                                                blendMode = BlendMode.SrcIn,
                                                            )
                                                        }
                                                    },
                                        )
                                    }
                                    RippleIconButton(
                                        modifier =
                                            Modifier.size(36.dp),
                                        resId = R.drawable.baseline_shuffle_24,
                                        fillMaxSize = true,
                                    ) {
                                        val temp = listTrack
                                        if (!temp.isNullOrEmpty()) {
                                            val random = temp.random()
                                            val args = Bundle()
                                            args.putString("type", Config.ALBUM_CLICK)
                                            args.putString("videoId", random.videoId)
                                            args.putString("from", "Playlist \"${(localPlaylist)?.title}\"")
                                            args.putInt("index", temp.indexOf(random))
                                            if (downloadState == DownloadState.STATE_DOWNLOADED) {
                                                args.putInt("downloaded", 1)
                                            }
                                            if (viewModel.localPlaylist.value?.syncedWithYouTubePlaylist == 1) {
                                                args.putString(
                                                    "playlistId",
                                                    viewModel.localPlaylist.value?.youtubePlaylistId?.replaceFirst("VL", ""),
                                                )
                                            }
                                            Queue.initPlaylist(
                                                Queue.LOCAL_PLAYLIST_ID + localPlaylist?.id,
                                                "Playlist \"${(viewModel.localPlaylist.value)?.title}\"",
                                                Queue.PlaylistType.PLAYLIST,
                                            )
                                            Queue.setNowPlaying(random.toTrack())
                                            val tempList: ArrayList<Track> = arrayListOf()
                                            for (i in temp) {
                                                tempList.add(i.toTrack())
                                            }
                                            tempList.remove(random.toTrack())
                                            Queue.addAll(tempList)
                                            if (offset > 0) {
                                                Queue.setContinuation(
                                                    Queue.LOCAL_PLAYLIST_ID + localPlaylist?.id,
                                                    if (filterState == FilterState.OlderFirst) {
                                                        (ASC + offset.toString())
                                                    } else {
                                                        (DESC + offset)
                                                    },
                                                )
                                            }
                                            if (runBlocking { sharedViewModel.simpleMediaServiceHandler?.shuffle?.first() } != true) {
                                                sharedViewModel.onUIEvent(UIEvent.Shuffle)
                                            }
                                            navController.navigateSafe(R.id.action_global_nowPlayingFragment, args)
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.playlist_is_empty), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    Spacer(Modifier.size(5.dp))
                                    RippleIconButton(
                                        modifier =
                                            Modifier.size(36.dp),
                                        resId = R.drawable.baseline_more_vert_24,
                                        fillMaxSize = true,
                                    ) {
                                        onPlaylistMoreClick()
                                    }
                                }
                                // Hide in local playlist
                                //                                ExpandableText(
                                //                                    modifier = Modifier.padding(vertical = 8.dp),
                                //                                    text = stringResource(id = R.string.demo_description),
                                //                                    fontSize = typo.bodyLarge.fontSize,
                                //                                    showMoreStyle = SpanStyle(Color.Gray),
                                //                                    showLessStyle = SpanStyle(Color.Gray),
                                //                                    style = TextStyle(
                                //                                        color = Color(0xC4FFFFFF)
                                //                                    )
                                //                                )
                                Text(
                                    text =
                                        stringResource(
                                            id = R.string.album_length,
                                            (localPlaylist?.tracks?.size ?: 0).toString(),
                                            "",
                                        ),
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                                AnimatedVisibility(visible = shouldShowSuggestions) {
                                    Column(
                                        modifier = Modifier.animateContentSize(),
                                    ) {
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text =
                                                stringResource(
                                                    id = R.string.suggest,
                                                ),
                                            color = Color.White,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Crossfade(targetState = suggestionsLoading) {
                                            if (it) {
                                                CenterLoadingBox(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .height(200.dp)
                                                            .align(Alignment.CenterHorizontally),
                                                )
                                            } else {
                                                Column {
                                                    suggestedTracks?.forEachIndexed { index, track ->
                                                        SuggestItems(
                                                            track = track,
                                                            isPlaying = playingTrack?.mediaId == track.videoId,
                                                            onAddClickListener = {
                                                                viewModel.addSuggestTrackToListTrack(
                                                                    track,
                                                                )
                                                            },
                                                            onClickListener = {
                                                                val args = Bundle()
                                                                args.putString("type", Config.PLAYLIST_CLICK)
                                                                args.putString("videoId", track.videoId)
                                                                args.putString(
                                                                    "from",
                                                                    "${
                                                                        context.getString(
                                                                            R.string.playlist,
                                                                        )
                                                                    } \"${localPlaylist?.title}\" ${
                                                                        context.getString(R.string.suggest)
                                                                    }",
                                                                )
                                                                args.putInt("index", index)
                                                                Queue.initPlaylist(
                                                                    "RDAMVM${track.videoId}",
                                                                    "${
                                                                        context.getString(
                                                                            R.string.playlist,
                                                                        )
                                                                    } \"${localPlaylist?.title}\" ${
                                                                        context.getString(R.string.suggest)
                                                                    }",
                                                                    Queue.PlaylistType.RADIO,
                                                                )
                                                                Queue.setNowPlaying(track)
                                                                val tempList: ArrayList<Track> = arrayListOf()
                                                                for (i in suggestedTracks!!) {
                                                                    tempList.add(i)
                                                                }
                                                                Queue.addAll(tempList)
                                                                if (Queue.getQueue().size >= 1) {
                                                                    Queue.removeTrackWithIndex(index)
                                                                }
                                                                navController.navigateSafe(
                                                                    R.id.action_global_nowPlayingFragment,
                                                                    args,
                                                                )
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.size(8.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.reloadSuggestion() },
                                            modifier =
                                                Modifier.drawWithContent {
                                                    val strokeWidthPx = 2.dp.toPx()
                                                    val width = size.width
                                                    val height = size.height

                                                    drawContent()

                                                    with(drawContext.canvas.nativeCanvas) {
                                                        val checkPoint = saveLayer(null, null)

                                                        // Destination
                                                        drawRoundRect(
                                                            cornerRadius = CornerRadius(x = 60f, y = 60f),
                                                            color = Color.Gray,
                                                            topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
                                                            size = Size(width - strokeWidthPx, height - strokeWidthPx),
                                                            style = Stroke(strokeWidthPx),
                                                        )
                                                        val gradientColors =
                                                            listOf(
                                                                Color(0xFF4C82EF),
                                                                Color(0xFFD96570),
                                                            )
                                                        val brush =
                                                            Brush.linearGradient(
                                                                colors = gradientColors,
                                                                start = Offset(2f, 0f),
                                                                end =
                                                                    Offset(
                                                                        2 + width,
                                                                        height,
                                                                    ),
                                                            )

                                                        // Source
                                                        rotate(degrees = angle) {
                                                            drawCircle(
                                                                brush = brush,
                                                                radius = size.width,
                                                                blendMode = BlendMode.SrcIn,
                                                            )
                                                        }

                                                        restoreToCount(checkPoint)
                                                    }
                                                },
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.reload),
                                                color = Color.White,
                                                modifier =
                                                    Modifier.align(
                                                        Alignment.CenterVertically,
                                                    ),
                                            )
                                        }
                                        Spacer(modifier = Modifier.size(12.dp))
                                        HorizontalDivider(
                                            color = Color.Gray,
                                            thickness = 0.5.dp,
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                    }
                                }
                                ElevatedButton(
                                    contentPadding = PaddingValues(0.dp),
                                    modifier =
                                        Modifier
                                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                                    onClick = {
                                        if (filterState == FilterState.OlderFirst) {
                                            viewModel.setFilter(FilterState.NewerFirst)
                                        } else {
                                            viewModel.setFilter(FilterState.OlderFirst)
                                        }
                                        Log.w("PlaylistScreen", "new filterState: $filterState")
                                        viewModel.setOffset(0)
                                        viewModel.clearListPair()
                                        viewModel.clearListTracks()
                                        if (localPlaylist != null) {
                                            localPlaylist?.id?.let { viewModel.getListTrack(it, offset, filterState) }
                                        }
                                    },
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (filterState == FilterState.OlderFirst) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.baseline_arrow_drop_down_24),
                                                contentDescription = "Older First",
                                                tint = Color.White,
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.baseline_arrow_drop_up_24),
                                                contentDescription = "Newer First",
                                                tint = Color.White,
                                            )
                                        }
                                        Spacer(modifier = Modifier.size(3.dp))
                                        Text(text = stringResource(id = R.string.added_date), style = typo.bodySmall, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        items(listTrack ?: listOf(), contentType = { it.videoId }) { item ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            }
            if (playingTrack?.mediaId == item.videoId && isPlaying) {
                PlaylistItems(
                    isPlaying = true,
                    songEntity = item,
                    onMoreClickListener = { onItemMoreClick(it) },
                ) {
                    onPlaylistItemClick(it)
                }
            } else {
                PlaylistItems(
                    isPlaying = false,
                    songEntity = item,
                    onMoreClickListener = { onItemMoreClick(it) },
                ) {
                    onPlaylistItemClick(it)
                }
            }
        }
        item {
            AnimatedVisibility(visible = isLoadingMore) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(15.dp))
                    CenterLoadingBox(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(15.dp))
                }
            }
        }
        item {
            EndOfPage()
        }
    }
    if (itemBottomSheetShow) {
        val track = currentItem
        if (track != null) {
            viewModel.getSongEntity(track)
            NowPlayingBottomSheet(
                isBottomSheetVisible = itemBottomSheetShow,
                onDelete = {
                    localPlaylist?.let {
                        Log.w("PlaylistScreen", "Delete: $track")
                        viewModel.deleteItem(track, it.id)
                        if (it.syncedWithYouTubePlaylist == 1 && it.youtubePlaylistId != null) {
                            val videoId = track.videoId
                            viewModel.removeYouTubePlaylistItem(
                                it.youtubePlaylistId,
                                videoId,
                            )
                        }
                        itemBottomSheetShow = false
                    }
                },
                onDismiss = { itemBottomSheetShow = false },
                navController = navController,
                sharedViewModel = sharedViewModel,
                songEntity = viewModel.songEntity.collectAsState(),
                onToggleLike = { checked ->
                    if (checked) {
                        viewModel.updateLikeStatus(track.videoId, 1)
                    } else {
                        viewModel.updateLikeStatus(track.videoId, 0)
                    }
                    coroutineScope.launch {
                        if (playingTrack?.mediaId == track.videoId) {
                            delay(500)
                            sharedViewModel.refreshSongDB()
                        }
                    }
                },
                getLocalPlaylist = { sharedViewModel.getAllLocalPlaylist() },
                listLocalPlaylist = sharedViewModel.localPlaylist.collectAsState(),
            )
        }
    }
    if (playlistBottomSheetShow) {
        Log.w("PlaylistScreen", "PlaylistBottomSheet")
        localPlaylist?.let {
            LocalPlaylistBottomSheet(
                isBottomSheetVisible = playlistBottomSheetShow,
                onDismiss = { playlistBottomSheetShow = false },
                localPlaylist = it,
                onEditTitle =
                    { newTitle ->
                        viewModel.updatePlaylistTitle(newTitle, it.id)
                        if (it.syncedWithYouTubePlaylist == 1) {
                            viewModel.updateYouTubePlaylistTitle(
                                newTitle,
                                it.youtubePlaylistId!!,
                            )
                        }
                    },
                onEditThumbnail =
                    { thumbUri ->
                        viewModel.updatePlaylistThumbnail(thumbUri, it.id)
                    },
                onAddToQueue = {
                    viewModel.getAllTracksOfPlaylist(it.id)
                    /*
                    Add to queue in LaunchedEffect
                     */
                    shouldAddAllToQueue = true
                },
                onSync = {
                    if (it.syncedWithYouTubePlaylist == 1) {
                        showUnsyncAlertDialog = true
                    } else {
                        showSyncAlertDialog = true
                    }
                },
                onUpdatePlaylist = {
                    it.tracks?.let { tracks ->
                        it.youtubePlaylistId?.let { it1 ->
                            viewModel.updateListTrackSynced(
                                it.id,
                                tracks,
                                it1,
                            )
                            viewModel.getSetVideoId(it1)
                        }
                    }
                },
                onDelete = {
                    viewModel.deletePlaylist(it.id)
                    navController.popBackStack()
                },
            )
        }
    }
    if (showSyncAlertDialog) {
        AlertDialog(
            title = { Text(text = stringResource(id = R.string.warning)) },
            text = { Text(text = stringResource(id = R.string.sync_playlist_warning)) },
            onDismissRequest = { showSyncAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    localPlaylist?.let {
                        Toast.makeText(
                            context,
                            context.getString(R.string.syncing),
                            Toast.LENGTH_SHORT,
                        ).show()
                        viewModel.syncPlaylistWithYouTubePlaylist(it)
                        showSyncAlertDialog = false
                    }
                }) {
                    Text(text = stringResource(id = R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSyncAlertDialog = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
    if (showUnsyncAlertDialog) {
        AlertDialog(
            title = { Text(text = stringResource(id = R.string.warning)) },
            text = { Text(text = stringResource(id = R.string.unsync_playlist_warning)) },
            onDismissRequest = { showUnsyncAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    localPlaylist?.let {
                        Toast.makeText(
                            context,
                            context.getString(R.string.unsyncing),
                            Toast.LENGTH_SHORT,
                        ).show()
                        viewModel.unsyncPlaylistWithYouTubePlaylist(it)
                    }
                    showUnsyncAlertDialog = false
                }) {
                    Text(text = stringResource(id = R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsyncAlertDialog = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
    AnimatedVisibility(
        visible = shouldHideTopBar,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = localPlaylist?.title ?: stringResource(id = R.string.playlist),
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
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            modifier = Modifier.angledGradientBackground(bg, 90f),
        )
    }
//    }
}