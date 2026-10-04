package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.maxrave.simpmusic.extension.DesktopWindowChrome
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Remove
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.UnfoldLess
import com.maxrave.simpmusic.ui.icon.UnfoldMore
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.theme.windowCloseButton
import com.maxrave.simpmusic.ui.theme.windowCloseButtonHover
import com.maxrave.simpmusic.ui.theme.windowMaximiseButton
import com.maxrave.simpmusic.ui.theme.windowMaximiseButtonHover
import com.maxrave.simpmusic.ui.theme.windowMinimiseButton
import com.maxrave.simpmusic.ui.theme.windowMinimiseButtonHover
import java.awt.MouseInfo
import java.awt.Window

/**
 * Custom title bar for JVM desktop application
 * Provides minimize, maximize/restore, and close buttons with drag-to-move functionality
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomTitleBar(
    title: String,
    windowState: WindowState,
    window: Window,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    // Passed in rather than read from MaterialTheme: this bar is drawn outside AppTheme, so
    // MaterialTheme here would hand back the default light scheme no matter what the user picked.
    containerColor: Color = Color.Black,
    titleColor: Color = Color.White,
) {
    var isMaximized by remember { mutableStateOf(windowState.placement == WindowPlacement.Maximized) }

    // Track drag start position
    var dragStartX by remember { mutableStateOf(0) }
    var dragStartY by remember { mutableStateOf(0) }

    // Update isMaximized when window state changes
    LaunchedEffect(windowState.placement) {
        isMaximized = windowState.placement == WindowPlacement.Maximized
    }

    val toggleMaximize: () -> Unit = {
        val frame = window as? javax.swing.JFrame
        val gc = frame?.graphicsConfiguration ?: window.graphicsConfiguration
        val bounds = gc?.bounds ?: java.awt.Rectangle(0, 0, 1920, 1080)
        val insets =
            runCatching { java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc) }.getOrNull()
                ?: java.awt.Insets(0, 0, 0, 0)
        val maxBounds =
            java.awt.Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom,
            )
        frame?.maximizedBounds = maxBounds

        if (windowState.placement == WindowPlacement.Maximized) {
            windowState.placement = WindowPlacement.Floating
            val floatSize = com.maxrave.simpmusic.DesktopWindowStateStore.getFloatingSize()
            windowState.size = floatSize
            val floatPos = com.maxrave.simpmusic.DesktopWindowStateStore.getFloatingPosition()
            if (floatPos.isSpecified) {
                windowState.position = floatPos
            } else {
                val screenW = bounds.width - insets.left - insets.right
                val screenH = bounds.height - insets.top - insets.bottom
                val targetW = floatSize.width.value.toInt()
                val targetH = floatSize.height.value.toInt()
                val centerX = bounds.x + insets.left + (screenW - targetW).coerceAtLeast(0) / 2
                val centerY = bounds.y + insets.top + (screenH - targetH).coerceAtLeast(0) / 2
                windowState.position = androidx.compose.ui.window.WindowPosition(centerX.dp, centerY.dp)
            }
        } else {
            windowState.placement = WindowPlacement.Maximized
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(DesktopWindowChrome.TITLE_BAR_HEIGHT_DP.dp)
                .background(containerColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            toggleMaximize()
                        },
                    )
                }.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            val mouseLocation = MouseInfo.getPointerInfo().location
                            dragStartX = mouseLocation.x - window.x
                            dragStartY = mouseLocation.y - window.y
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val mouseLocation = MouseInfo.getPointerInfo().location
                            // If maximized, restore before moving
                            if (windowState.placement == WindowPlacement.Maximized) {
                                windowState.placement = WindowPlacement.Floating
                                windowState.size = com.maxrave.simpmusic.DesktopWindowStateStore.getFloatingSize()
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
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Title text on the left
            Text(
                text = title,
                style = typo().labelSmall,
                color = titleColor,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Window control buttons on the RIGHT: Minimize first, Maximize/Restore in middle, Close last
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Minimize button (yellow)
                WindowControlButton(
                    onClick = {
                        windowState.isMinimized = true
                    },
                    backgroundColor = windowMinimiseButton,
                    hoverColor = windowMinimiseButtonHover,
                    icon = WindowControlIcon.Minimize,
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Maximize/Restore button (green in the middle)
                WindowControlButton(
                    onClick = toggleMaximize,
                    backgroundColor = windowMaximiseButton,
                    hoverColor = windowMaximiseButtonHover,
                    icon = if (isMaximized) WindowControlIcon.Restore else WindowControlIcon.Maximize,
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Close button (red)
                WindowControlButton(
                    onClick = onCloseRequest,
                    backgroundColor = windowCloseButton,
                    hoverColor = windowCloseButtonHover,
                    icon = WindowControlIcon.Close,
                )
            }
        }
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
                .size(14.dp)
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
        // Show icons on hover for better UX
        if (isHovered) {
            Box(modifier = Modifier.padding(1.dp)) {
                when (icon) {
                    WindowControlIcon.Minimize -> {
                        Icon(SimpIcons.Remove, tint = Color.DarkGray, contentDescription = "Minimize")
                    }

                    WindowControlIcon.Maximize -> {
                        Icon(
                            modifier = Modifier.rotate(45f),
                            imageVector = SimpIcons.UnfoldMore,
                            tint = Color.DarkGray,
                            contentDescription = "Minimize",
                        )
                    }

                    WindowControlIcon.Restore -> {
                        Icon(
                            modifier = Modifier.rotate(45f),
                            imageVector = SimpIcons.UnfoldLess,
                            tint = Color.DarkGray,
                            contentDescription = "Minimize",
                        )
                    }

                    WindowControlIcon.Close -> {
                        Icon(
                            imageVector = SimpIcons.Close,
                            tint = Color.DarkGray,
                            contentDescription = "Close",
                        )
                    }
                }
            }
        }
    }
}