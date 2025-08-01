package com.maxrave.simpmusic.ui.component

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.state.rememberPresentationState
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.extension.KeepScreenOn
import com.maxrave.simpmusic.extension.findActivity
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.rememberIsInPipMode
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.TimeLine
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import kotlin.math.roundToInt

@UnstableApi
@Composable
fun MediaPlayerView(
    modifier: Modifier = Modifier,
    url: String,
    canvasCache: SimpleCache = koinInject(named(Config.CANVAS_CACHE)),
) {
    // Get the current context
    val context = LocalContext.current
    val density = LocalDensity.current

    val screenSize = getScreenSizeInfo()

    var widthPx by rememberSaveable {
        mutableIntStateOf(screenSize.wPX)
    }

    var keepScreenOn by rememberSaveable {
        mutableStateOf(false)
    }

    val playerListener =
        remember {
            object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    super.onVideoSizeChanged(videoSize)
                    Log.w("MediaPlayerView", "Video size changed: ${videoSize.width} / ${videoSize.height}")
                    if (videoSize.width != 0 && videoSize.height != 0) {
                        val h = (videoSize.width.toFloat() / videoSize.height) * screenSize.hPX
                        Log.w("MediaPlayerView", "Calculated width: $h")
                        widthPx = h.roundToInt()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    keepScreenOn = isPlaying
                }
            }
        }

    // Initialize ExoPlayer
    val exoPlayer =
        remember {
            val cacheSink =
                CacheDataSink
                    .Factory()
                    .setCache(canvasCache)
            val upstreamFactory = DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory())
            val downStreamFactory = FileDataSource.Factory()
            val cacheDataSourceFactory =
                CacheDataSource
                    .Factory()
                    .setCache(canvasCache)
                    .setCacheWriteDataSinkFactory(cacheSink)
                    .setCacheReadDataSourceFactory(downStreamFactory)
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            ExoPlayer
                .Builder(context)
                .setLoadControl(
                    DefaultLoadControl
                        .Builder()
                        .setPrioritizeTimeOverSizeThresholds(false)
                        .build(),
                ).setMediaSourceFactory(
                    DefaultMediaSourceFactory(cacheDataSourceFactory),
                ).build()
                .apply {
                    addListener(playerListener)
                    videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                }
        }

    // Create a MediaSource
    val mediaSource =
        remember(url) {
            MediaItem.fromUri(url)
        }

    // Set MediaSource to ExoPlayer
    LaunchedEffect(mediaSource) {
        exoPlayer.setMediaItem(mediaSource)
        exoPlayer.prepare()
        exoPlayer.play()
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
    }

    // Manage lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.removeListener(playerListener)
            exoPlayer.release()
        }
    }

    if (keepScreenOn) {
        KeepScreenOn()
    }

    val presentationState = rememberPresentationState(exoPlayer)

    Box(modifier = modifier.graphicsLayer { clip = true }) {
        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(with(density) { widthPx.toDp() })
                    .align(Alignment.Center),
        )

        if (presentationState.coverSurface) {
            // Cover the surface that is being prepared with a shutter
            Box(Modifier.background(Color.Black))
        }
    }
}

@Composable
@UnstableApi
fun MediaPlayerViewWithSubtitle(
    modifier: Modifier = Modifier,
    player: ExoPlayer,
    shouldPip: Boolean = false,
    shouldShowSubtitle: Boolean,
    shouldScaleDownSubtitle: Boolean = false,
    timelineState: TimeLine,
    lyricsData: Lyrics? = null,
    translatedLyricsData: Lyrics? = null,
) {
    val context = LocalContext.current

    val isInPipMode = rememberIsInPipMode()

    var shouldEnterPipMode by rememberSaveable {
        mutableStateOf(false)
    }

    var videoRatio by rememberSaveable {
        mutableFloatStateOf(16f / 9)
    }

    var showArtwork by rememberSaveable {
        mutableStateOf(false)
    }

    var artworkUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var currentLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }
    var currentTranslatedLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }

    LaunchedEffect(key1 = timelineState) {
        val lines = lyricsData?.lines ?: return@LaunchedEffect
        val translatedLines = translatedLyricsData?.lines
        if (timelineState.current > 0L) {
            lines.indices.forEach { i ->
                val sentence = lines[i]
                val startTimeMs = sentence.startTimeMs.toLong()

                // estimate the end time of the current sentence based on the start time of the next sentence
                val endTimeMs =
                    if (i < lines.size - 1) {
                        lines[i + 1].startTimeMs.toLong()
                    } else {
                        // if this is the last sentence, set the end time to be some default value (e.g., 1 minute after the start time)
                        startTimeMs + 60000
                    }
                if (timelineState.current in startTimeMs..endTimeMs) {
                    currentLineIndex = i
                }
            }
            translatedLines?.indices?.forEach { i ->
                val sentence = translatedLines[i]
                val startTimeMs = sentence.startTimeMs.toLong()

                // estimate the end time of the current sentence based on the start time of the next sentence
                val endTimeMs =
                    if (i < translatedLines.size - 1) {
                        translatedLines[i + 1].startTimeMs.toLong()
                    } else {
                        // if this is the last sentence, set the end time to be some default value (e.g., 1 minute after the start time)
                        startTimeMs + 60000
                    }
                if (timelineState.current in startTimeMs..endTimeMs) {
                    currentTranslatedLineIndex = i
                }
            }
            if (lines.isNotEmpty() &&
                (
                    timelineState.current in (
                        0..(
                            lines.getOrNull(0)?.startTimeMs
                                ?: "0"
                        ).toLong()
                    )
                )
            ) {
                currentLineIndex = -1
                currentTranslatedLineIndex = -1
            }
        } else {
            currentLineIndex = -1
            currentTranslatedLineIndex = -1
        }
    }

    val playerListener =
        remember {
            object : Player.Listener {
                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    super.onMediaItemTransition(mediaItem, reason)
                    artworkUri = mediaItem?.mediaMetadata?.artworkUri?.toString()
                }

                override fun onTracksChanged(tracks: Tracks) {
                    super.onTracksChanged(tracks)
                    if (!tracks.groups.isEmpty()) {
                        for (arrayIndex in 0 until tracks.groups.size) {
                            var done = false
                            for (groupIndex in 0 until tracks.groups[arrayIndex].length) {
                                val sampleMimeType = tracks.groups[arrayIndex].getTrackFormat(groupIndex).sampleMimeType
                                if (sampleMimeType != null && sampleMimeType.contains("video")) {
                                    showArtwork = false
                                    done = true
                                    break
                                } else {
                                    showArtwork = true
                                }
                            }
                            if (done) {
                                break
                            }
                        }
                    }
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    super.onVideoSizeChanged(videoSize)
                    videoRatio = if (videoSize.width != 0 && videoSize.height != 0) {
                        videoSize.width.toFloat() / videoSize.height
                    } else {
                        16f / 9 // Default ratio if video size is not available
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    shouldEnterPipMode = isPlaying && shouldPip
                }
            }
        }

    DisposableEffect(Unit) {
        shouldEnterPipMode = shouldPip
        onDispose {
            shouldEnterPipMode = false
            player.removeListener(playerListener)
            Log.w("MediaPlayerView", "Disposing ExoPlayer")
            if (shouldPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val builder = PictureInPictureParams.Builder()

                // Add autoEnterEnabled for versions S and up
                builder.setAutoEnterEnabled(false)
                context.findActivity().setPictureInPictureParams(builder.build())
            }
        }
    }
    LaunchedEffect(player) {
        player.addListener(playerListener)
        player.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
    }

    val presentationState = rememberPresentationState(player)

    LaunchedEffect(shouldEnterPipMode) {
        Log.w("MediaPlayerView", "shouldEnterPipMode: $shouldEnterPipMode")
    }

    if (shouldPip && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        val currentShouldEnterPipMode by rememberUpdatedState(newValue = shouldEnterPipMode)
        DisposableEffect(context) {
            val onUserLeaveBehavior =
                Runnable {
                    if (currentShouldEnterPipMode) {
                        context
                            .findActivity()
                            .enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                    }
                }
            context.findActivity().addOnUserLeaveHintListener(
                onUserLeaveBehavior,
            )
            onDispose {
                context.findActivity().removeOnUserLeaveHintListener(
                    onUserLeaveBehavior,
                )
            }
        }
    }

    Box(
        modifier =
            modifier
                .then(
                    if (shouldPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.onGloballyPositioned { layoutCoordinates ->
                            val builder = PictureInPictureParams.Builder()

                            // Add autoEnterEnabled for versions S and up
                            builder.setAutoEnterEnabled(shouldEnterPipMode)
                            context.findActivity().setPictureInPictureParams(builder.build())
                        }
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        KeepScreenOn()
        Crossfade(showArtwork) {
            if (it) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(
                                artworkUri,
                            ).diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(
                                artworkUri,
                            ).crossfade(550)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .align(Alignment.Center),
                )
            } else {
                PlayerSurface(
                    player = player,
                    surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                    modifier =
                        Modifier
                            .wrapContentSize()
                            .aspectRatio(if (videoRatio > 0f) videoRatio else 16f / 9)
                            .align(Alignment.Center),
                )

                if (presentationState.coverSurface) {
                    // Cover the surface that is being prepared with a shutter
                    Box(Modifier.background(Color.Black))
                }
            }
        }
        if (lyricsData != null && shouldShowSubtitle) {
            Crossfade(
                currentLineIndex != -1,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxSize(),
            ) {
                val lines = lyricsData.lines ?: return@Crossfade
                if (it) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(bottom = if (isInPipMode || shouldScaleDownSubtitle) 10.dp else 40.dp)
                            .align(Alignment.BottomCenter),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(Modifier.fillMaxWidth(0.7f)) {
                            Column(
                                Modifier.align(Alignment.BottomCenter),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = lines.getOrNull(currentLineIndex)?.words ?: return@Crossfade,
                                    style =
                                        typo.bodyLarge
                                            .let {
                                                if (isInPipMode || shouldScaleDownSubtitle) {
                                                    it.copy(fontSize = it.fontSize * 0.8f)
                                                } else {
                                                    it
                                                }
                                            },
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier =
                                        Modifier
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .wrapContentWidth(),
                                )
                                Crossfade(translatedLyricsData?.lines != null, label = "") { translate ->
                                    val translateLines = translatedLyricsData?.lines ?: return@Crossfade
                                    if (translate) {
                                        Text(
                                            text = translateLines.getOrNull(currentTranslatedLineIndex)?.words ?: return@Crossfade,
                                            style =
                                                typo.bodyMedium.let {
                                                    if (isInPipMode || shouldScaleDownSubtitle) {
                                                        it.copy(fontSize = it.fontSize * 0.8f)
                                                    } else {
                                                        it
                                                    }
                                                },
                                            color = Color.Yellow,
                                            textAlign = TextAlign.Center,
                                            modifier =
                                                Modifier
                                                    .background(Color.Black.copy(alpha = 0.5f))
                                                    .wrapContentWidth(),
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