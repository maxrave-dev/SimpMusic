@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.maxrave.simpmusic.expect.ui

import kotlinx.cinterop.useContents
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.maxrave.data.player.AvPlayerAdapter
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.manager.DataStoreManager
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

@Composable
actual fun MediaPlayerView(url: String, modifier: Modifier, cropToBounds: Boolean, onFirstFrame: () -> Unit) {
    val player = remember(url) { AVPlayer(NSURL.URLWithString(url)!!) }
    val controller = remember(player) { AVPlayerViewController().apply {
        this.player = player
        showsPlaybackControls = false
        videoGravity = if (cropToBounds) AVLayerVideoGravityResizeAspectFill else AVLayerVideoGravityResizeAspect
    } }
    val callback by rememberUpdatedState(onFirstFrame)
    LaunchedEffect(controller) {
        player.play()
        while (!controller.readyForDisplay) delay(50)
        callback()
    }
    DisposableEffect(player) { onDispose { player.pause(); player.replaceCurrentItemWithPlayerItem(null) } }
    UIKitViewController(factory = { controller }, modifier = modifier)
}

@Composable
actual fun rememberVideoAspectRatio(playerName: String): Float? {
    val player = koinInject<AvPlayerAdapter>().nativePlayer
    var ratio by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(player) {
        while (true) {
            ratio = player.currentItem?.presentationSize?.let {
                it.useContents { if (height > 0) (width / height).toFloat() else null }
            }
            delay(500)
        }
    }
    return ratio
}

@Composable
actual fun MediaPlayerViewWithSubtitle(modifier: Modifier, playerName: String, shouldPip: Boolean,
    shouldShowSubtitle: Boolean, shouldScaleDownSubtitle: Boolean, isInPipMode: Boolean,
    timelineState: TimeLine, lyricsData: Lyrics?, translatedLyricsData: Lyrics?,
    mainTextStyle: TextStyle, translatedTextStyle: TextStyle) {
    val player = koinInject<AvPlayerAdapter>().nativePlayer
    val offset by koinInject<DataStoreManager>().lyricsOffsetMs.collectAsState(0)
    val controller = remember(player) { AVPlayerViewController().apply {
        this.player = player
        showsPlaybackControls = false
        allowsPictureInPicturePlayback = shouldPip
    } }
    Box(modifier) {
        UIKitViewController(factory = { controller }, modifier = Modifier.fillMaxSize())
        if (shouldShowSubtitle) {
            val time = timelineState.current - offset
            val line = lyricsData?.lines?.lastOrNull { (it.startTimeMs.toLongOrNull() ?: Long.MAX_VALUE) <= time }
            val translation = translatedLyricsData?.lines?.lastOrNull { (it.startTimeMs.toLongOrNull() ?: Long.MAX_VALUE) <= time }
            Column(Modifier.align(Alignment.BottomCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                line?.let { Text(it.words, style = mainTextStyle, color = Color.White) }
                translation?.let { Text(it.words, style = translatedTextStyle, color = Color.White) }
            }
        }
    }
}
