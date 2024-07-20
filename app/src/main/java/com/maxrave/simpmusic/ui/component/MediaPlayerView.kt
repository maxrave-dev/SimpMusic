package com.maxrave.simpmusic.ui.component

import android.util.Log
import android.view.TextureView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

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
            exoPlayer.release()
        }
    }

    // Use AndroidView to embed an Android View (PlayerView) into Compose
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).also {
                exoPlayer.setVideoTextureView(it)
            }
        },
        modifier = modifier.wrapContentHeight().fillMaxWidth().border(
            width = 1.dp,
            color = Color.Red
        )
    )
}

@Composable
@UnstableApi
fun MediaPlayerView(player: ExoPlayer, modifier: Modifier = Modifier) {
    DisposableEffect(Unit) {
        onDispose {
            Log.w("MediaPlayerView", "Disposing ExoPlayer")
        }
    }
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).also {
                player.setVideoTextureView(it)
                player.videoScalingMode = VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
        },
        modifier = modifier
    )
}