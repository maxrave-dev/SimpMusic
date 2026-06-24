package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.simpmusic.viewModel.SharedViewModel
import java.awt.Cursor
import java.awt.MouseInfo

/**
 * Root composable for the mini player window content.
 * Automatically switches between layouts based on window width:
 * - < 260dp: Compact (controls only)
 * - 260-360dp: Medium (artwork + controls)
 * - > 360dp: Full (artwork + info + controls)
 *
 * Shows placeholder when no track is playing.
 * Includes close button and drag handle since window is frameless.
 */
@Composable
fun MiniPlayerRoot(
    sharedViewModel: SharedViewModel,
    onClose: () -> Unit,
    windowState: WindowState,
) {
    val nowPlayingData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timeline by sharedViewModel.timeline.collectAsStateWithLifecycle()

    val lyricsData by remember {
        derivedStateOf {
            nowPlayingData.lyricsData
        }
    }

    // Track mouse position for dragging
    var dragStartMousePos by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragStartWindowPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // Check if there's any track playing
    val hasTrack = nowPlayingData.nowPlayingTitle.isNotBlank()

    Surface(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            if (!hasTrack) {
                // Show empty state
                EmptyMiniPlayerState()
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // Calculate aspect ratio to detect square/tall layout
                    val aspectRatio = maxWidth.value / maxHeight.value
                    val isSquareOrTall = aspectRatio <= 1.3f && maxHeight >= 200.dp

                    when {
                        isSquareOrTall -> {
                            SquareMiniLayout(
                                nowPlayingData = nowPlayingData,
                                controllerState = controllerState,
                                timeline = timeline,
                                lyricsData = lyricsData,
                                onUIEvent = sharedViewModel::onUIEvent,
                            )
                        }

                        maxWidth < 260.dp -> {
                            CompactMiniLayout(
                                controllerState = controllerState,
                                timeline = timeline,
                                onUIEvent = sharedViewModel::onUIEvent,
                            )
                        }

                        maxWidth < 360.dp -> {
                            MediumMiniLayout(
                                nowPlayingData = nowPlayingData,
                                controllerState = controllerState,
                                timeline = timeline,
                                lyricsData = lyricsData,
                                onUIEvent = sharedViewModel::onUIEvent,
                            )
                        }

                        else -> {
                            ExpandedMiniLayout(
                                nowPlayingData = nowPlayingData,
                                controllerState = controllerState,
                                timeline = timeline,
                                lyricsData = lyricsData,
                                onUIEvent = sharedViewModel::onUIEvent,
                            )
                        }
                    }
                }
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