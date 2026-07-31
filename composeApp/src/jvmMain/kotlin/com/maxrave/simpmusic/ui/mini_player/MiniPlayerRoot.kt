package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import java.awt.Cursor
import java.awt.MouseInfo

/**
 * Root composable for the mini player window content.
 * Shows a compact bar (art + track info + controls) by default; clicking the
 * expand chevron switches to the expanded layout with full animated synced lyrics.
 *
 * The background is dynamically themed from the album artwork's dominant color.
 *
 * Shows placeholder when no track is playing.
 * Includes close button and drag handle since window is frameless.
 */
@Composable
fun MiniPlayerRoot(
    sharedViewModel: SharedViewModel,
    onClose: () -> Unit,
    windowState: WindowState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    val nowPlayingData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timeline by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val queueDataState by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()

    val lyricsData by remember {
        derivedStateOf {
            nowPlayingData.lyricsData
        }
    }

    // Up Next queue for the expanded panel (same source as the main app's queue sheet).
    // Use track.videoId (prefix-stripped) to match the currently playing track.
    val artworkQueue by remember {
        derivedStateOf { queueDataState?.data?.listTracks ?: emptyList() }
    }
    val nowPlayingVideoId: String? = nowPlayingState?.track?.videoId

    // Track mouse position for dragging
    var dragStartMousePos by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragStartWindowPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // Check if there's any track playing
    val hasTrack = nowPlayingData.nowPlayingTitle.isNotBlank()

    // Dynamic background color from the album artwork's dominant color
    val defaultBg = Color(0xFF1C1C1E)
    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = defaultBg,
            defaultOnColor = Color.White,
            loader = networkLoader,
        )
    var miniPlayerBackground by remember { mutableStateOf(defaultBg) }
    LaunchedEffect(nowPlayingData.thumbnailURL) {
        nowPlayingData.thumbnailURL?.let { url ->
            dominantColorState.updateFrom(Url(url))
        }
    }
    LaunchedEffect(dominantColorState) {
        snapshotFlow { dominantColorState.color }.collect { color ->
            // Darken the artwork color so white text stays readable (same as HomeScreen)
            miniPlayerBackground = color.rgbFactor(0.3f)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        color = miniPlayerBackground,
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            if (!hasTrack) {
                // Show empty state
                EmptyMiniPlayerState()
            } else if (isExpanded) {
                // Expanded: compact bar + Lyrics / Up Next tabs
                LyricsExpandedMiniLayout(
                    nowPlayingData = nowPlayingData,
                    controllerState = controllerState,
                    timeline = timeline,
                    timelineFlow = sharedViewModel.timeline,
                    lyricsData = lyricsData,
                    queue = artworkQueue,
                    currentVideoId = nowPlayingVideoId,
                    background = miniPlayerBackground,
                    onUIEvent = sharedViewModel::onUIEvent,
                    onToggleExpand = onToggleExpand,
                    onPlayQueueItem = { index ->
                        sharedViewModel.onUIEvent(UIEvent.PlayQueueItem(index))
                    },
                )
            } else {
                // Collapsed: clean art + track info bar
                MiniPlayerBarLayout(
                    nowPlayingData = nowPlayingData,
                    controllerState = controllerState,
                    timeline = timeline,
                    background = miniPlayerBackground,
                    onUIEvent = sharedViewModel::onUIEvent,
                    onToggleExpand = onToggleExpand,
                )
            }

            // Close button (top-right corner)
            IconButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
            }

            // Drag handle (top center area for moving window - narrower to avoid resize corners)
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.5f)
                        .height(28.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    // Store initial mouse and window positions
                                    val mousePos = MouseInfo.getPointerInfo().location
                                    dragStartMousePos = Pair(mousePos.x, mousePos.y)
                                    val currentPos = windowState.position
                                    if (currentPos is androidx.compose.ui.window.WindowPosition.Absolute) {
                                        dragStartWindowPos = Pair(currentPos.x.value, currentPos.y.value)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val startMouse = dragStartMousePos
                                    val startWindow = dragStartWindowPos
                                    if (startMouse != null && startWindow != null) {
                                        val currentMousePos = MouseInfo.getPointerInfo().location
                                        val deltaX = currentMousePos.x - startMouse.first
                                        val deltaY = currentMousePos.y - startMouse.second
                                        windowState.position =
                                            androidx.compose.ui.window.WindowPosition(
                                                (startWindow.first + deltaX).dp,
                                                (startWindow.second + deltaY).dp,
                                            )
                                    }
                                },
                                onDragEnd = {
                                    dragStartMousePos = null
                                    dragStartWindowPos = null
                                },
                            )
                        }.pointerHoverIcon(PointerIcon(Cursor(Cursor.MOVE_CURSOR))),
            )
        }
    }
}