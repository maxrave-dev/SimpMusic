package com.maxrave.simpmusic.ui.component

import android.util.Log
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.extension.KeepScreenOn
import com.maxrave.simpmusic.extension.getScreenSizeInfo
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

    // Initialize ExoPlayer
    val exoPlayer =
        ExoPlayer
            .Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(cacheDataSourceFactory),
            ).build()
            .apply {
                addListener(playerListener)
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

    // Use AndroidView to embed an Android View (PlayerView) into Compose
    Box(modifier = modifier.graphicsLayer { clip = true }) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also {
                    exoPlayer.setVideoTextureView(it)
                    exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                }
            },
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(with(density) { widthPx.toDp() })
                    .align(Alignment.Center),
        )
    }
}

@Composable
@UnstableApi
fun MediaPlayerView(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    var videoRatio by rememberSaveable {
        mutableFloatStateOf(16f / 9)
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
                        videoRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    keepScreenOn = isPlaying
                }
            }
        }

    DisposableEffect(Unit) {
        onDispose {
            player.removeListener(playerListener)
            Log.w("MediaPlayerView", "Disposing ExoPlayer")
        }
    }
    LaunchedEffect(player) {
        player.addListener(playerListener)
    }

    if (keepScreenOn) {
        KeepScreenOn()
    }

    Box(modifier) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also {
                    player.setVideoTextureView(it)
                    player.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                }
            },
            modifier =
                Modifier
                    .wrapContentSize()
                    .aspectRatio(if (videoRatio > 0f) videoRatio else 16f / 9)
                    .align(Alignment.Center),
        )
    }
}