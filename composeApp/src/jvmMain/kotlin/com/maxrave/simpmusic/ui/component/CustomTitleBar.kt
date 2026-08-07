package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Remove
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.UnfoldLess
import com.maxrave.simpmusic.ui.icon.UnfoldMore
import com.maxrave.simpmusic.ui.theme.typo
import java.awt.MouseInfo
import java.awt.Window

/**
 * Custom title bar for JVM desktop application.
 *
 * Provides minimize, maximize/restore and close buttons, native-feel drag-to-move (the OS move
 * loop on Windows), double-click to maximize/restore, and a top resize band so the window can be
 * sized by dragging its top edge — the one edge Compose's built-in resizer can't reach, because
 * this bar otherwise owns it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomTitleBar(
    title: String,
    windowState: WindowState,
    window: Window,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMaximized by remember { mutableStateOf(windowState.placement == WindowPlacement.Maximized) }

    // Track drag start position (manual fallback for non-Windows hosts).
    var dragStartX by remember { mutableStateOf(0) }
    var dragStartY by remember { mutableStateOf(0) }

    val resizeBandDp = 14.dp

    // Update isMaximized when window state changes
    LaunchedEffect(windowState.placement) {
        isMaximized = windowState.placement == WindowPlacement.Maximized
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.Black),
    ) {
        // Drag-to-move + double-click maximize over the title bar body. Drawn BELOW the buttons so
        // they stay clickable; on Windows the drag is handed to the OS (native move loop with Aero
        // snap), elsewhere the manual AWT path below is used. Double-click keeps working because
        // detectDragGestures only fires after the touch slop, so a plain click/double-click never
        // reaches the move logic.
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                // Double-click to maximize/restore
                                if (windowState.placement == WindowPlacement.Maximized) {
                                    windowState.placement = WindowPlacement.Floating
                                } else {
                                    windowState.placement = WindowPlacement.Maximized
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        val bandPx = resizeBandDp.toPx()
                        detectDragGestures(
                            onDragStart = { offset ->
                                // Never start a move from the top resize band (the band overlay
                                // consumes those presses anyway; this is a belt-and-suspenders guard).
                                if (offset.y < bandPx) return@detectDragGestures
                                if (WindowNative.isWindows) {
                                    // Native move loop — Windows restores-from-maximized and snaps
                                    // to the screen edges/top on its own.
                                    WindowNative.startNativeWindowCommand(window, WindowNative.HT_CAPTION)
                                } else {
                                    val mouseLocation = MouseInfo.getPointerInfo().location
                                    dragStartX = mouseLocation.x - window.x
                                    dragStartY = mouseLocation.y - window.y
                                }
                            },
                            onDrag = { change, _ ->
                                if (WindowNative.isWindows) return@detectDragGestures
                                change.consume()
                                val mouseLocation = MouseInfo.getPointerInfo().location
                                // If maximized, restore before moving
                                if (windowState.placement == WindowPlacement.Maximized) {
                                    windowState.placement = WindowPlacement.Floating
                                    // Recalculate drag offset after restore
                                    dragStartX = (windowState.size.width.value / 2).toInt()
                                    dragStartY = 20
                                }
                                window.setLocation(
                                    mouseLocation.x - dragStartX,
                                    mouseLocation.y - dragStartY,
                                )
                            },
                        )
                    },
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Window control buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Close button
                WindowControlButton(
                    onClick = onCloseRequest,
                    backgroundColor = Color(0xFFFF605C),
                    hoverColor = Color(0xFFE54942),
                    icon = WindowControlIcon.Close,
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Minimize button
                WindowControlButton(
                    onClick = {
                        windowState.isMinimized = true
                    },
                    backgroundColor = Color(0xFFFFBD44),
                    hoverColor = Color(0xFFE5A93D),
                    icon = WindowControlIcon.Minimize,
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Maximize/Restore button
                WindowControlButton(
                    onClick = {
                        if (windowState.placement == WindowPlacement.Maximized) {
                            windowState.placement = WindowPlacement.Floating
                        } else {
                            windowState.placement = WindowPlacement.Maximized
                        }
                    },
                    backgroundColor = Color(0xFF00CA4E),
                    hoverColor = Color(0xFF00B344),
                    icon = if (isMaximized) WindowControlIcon.Restore else WindowControlIcon.Maximize,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Title text (optional)
            Text(
                text = title,
                style = typo().labelSmall,
                color = Color.White,
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        // Top-edge resize band. Drawn last, so it sits above everything and owns the top of the
        // window: dragging it resizes instead of moving. On Windows the OS resize loop is started
        // (smooth + snapping); elsewhere we resize manually via AWT bounds.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(resizeBandDp)
                    .align(Alignment.TopCenter)
                    .pointerInput(window) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (currentEvent.button != PointerButton.Primary) return@awaitEachGesture
                            down.consume()
                            if (WindowNative.isWindows) {
                                WindowNative.startNativeWindowCommand(window, WindowNative.HT_TOP)
                            } else {
                                manualTopResize(window)
                            }
                        }
                    },
        )
    }
}

/**
 * Manual top-edge resize loop (non-Windows hosts). Grows/shrinks the window by moving its top edge
 * with the cursor, keeping the bottom edge anchored and the width unchanged.
 */
@OptIn(ExperimentalComposeUiApi::class)
private suspend fun AwaitPointerEventScope.manualTopResize(window: Window) {
    val minHeight = 480.dp.toPx().toInt()
    val startBounds = window.bounds
    val startScreenY = MouseInfo.getPointerInfo().location.y
    while (true) {
        val event = awaitPointerEvent()
        val changes = event.changes
        if (changes.any { it.changedToUp() }) break
        changes.forEach { it.consume() }
        val screenY = MouseInfo.getPointerInfo().location.y
        val delta = screenY - startScreenY
        val newHeight = (startBounds.height - delta).coerceAtLeast(minHeight)
        // Anchor the bottom edge: as the top moves down/up, grow/shrink by the same amount.
        val newY = startBounds.y + (startBounds.height - newHeight)
        window.setBounds(startBounds.x, newY, startBounds.width, newHeight)
    }
}

private enum class WindowControlIcon {
    Minimize,
    Maximize,
    Restore,
    Close,
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    hoverColor: Color,
    icon: WindowControlIcon,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier =
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (isHovered) hoverColor else backgroundColor)
                .hoverable(interactionSource)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        // Icons are always visible (dimmed until hover) so the controls are discoverable.
        val iconModifier =
            Modifier.size(12.dp).then(
                if (isHovered) Modifier else Modifier,
            )
        when (icon) {
            WindowControlIcon.Minimize -> {
                Icon(
                    modifier = iconModifier,
                    imageVector = SimpIcons.Remove,
                    tint = if (isHovered) Color(0xFF3B3B3B) else Color.DarkGray.copy(alpha = 0.55f),
                    contentDescription = "Minimize",
                )
            }

            WindowControlIcon.Maximize -> {
                Icon(
                    modifier = iconModifier.rotate(45f),
                    imageVector = SimpIcons.UnfoldMore,
                    tint = if (isHovered) Color(0xFF3B3B3B) else Color.DarkGray.copy(alpha = 0.55f),
                    contentDescription = "Maximize",
                )
            }

            WindowControlIcon.Restore -> {
                Icon(
                    modifier = iconModifier.rotate(45f),
                    imageVector = SimpIcons.UnfoldLess,
                    tint = if (isHovered) Color(0xFF3B3B3B) else Color.DarkGray.copy(alpha = 0.55f),
                    contentDescription = "Restore",
                )
            }

            WindowControlIcon.Close -> {
                Icon(
                    modifier = iconModifier,
                    imageVector = SimpIcons.Close,
                    tint = if (isHovered) Color(0xFF3B3B3B) else Color.DarkGray.copy(alpha = 0.55f),
                    contentDescription = "Close",
                )
            }
        }
    }
}
