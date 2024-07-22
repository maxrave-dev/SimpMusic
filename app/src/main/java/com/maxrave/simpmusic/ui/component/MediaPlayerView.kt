package com.maxrave.simpmusic.ui.component

import android.util.Log
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.maxrave.simpmusic.extension.KeepScreenOn
import com.maxrave.simpmusic.extension.getScreenSizeInfo

@UnstableApi
@Composable
fun MediaPlayerView(
    modifier: Modifier = Modifier,
    url: String
) {
    // Get the current context
    val context = LocalContext.current

    // Initialize ExoPlayer
    val exoPlayer = ExoPlayer.Builder(context).build()

    // Create a MediaSource
    val mediaSource = remember(url) {
        MediaItem.fromUri(url)
    }

    var screenSize = getScreenSizeInfo()

    var videoRatio by rememberSaveable {
        mutableFloatStateOf(
            9f / 16
        )
    }

    var keepScreenOn by rememberSaveable {
        mutableStateOf(false)
    }

    val playerListener = remember {
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
                if (isPlaying) {
                    keepScreenOn = true
                } else {
                    keepScreenOn = false
                }
            }
        }
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
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also {
                    exoPlayer.setVideoTextureView(it)
                    exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                }
            },
            modifier = Modifier
                .wrapContentSize()
                .aspectRatio(if (videoRatio > 0f) videoRatio else 16f / 9)
                .align(Alignment.Center)
        )
    }
}

@Composable
@UnstableApi
fun MediaPlayerView(player: ExoPlayer, modifier: Modifier = Modifier) {

    var videoRatio by rememberSaveable {
        mutableFloatStateOf(16f/9)
    }

    var keepScreenOn by rememberSaveable {
        mutableStateOf(false)
    }

    val playerListener = remember {
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
                if (isPlaying) {
                    keepScreenOn = true
                } else {
                    keepScreenOn = false
                }
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

    Box (modifier) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also {
                    player.setVideoTextureView(it)
                    player.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                }
            },
            modifier = Modifier
                .wrapContentSize()
                .aspectRatio(if (videoRatio > 0f) videoRatio else 16f / 9)
                .align(Alignment.Center)
        )
    }
}