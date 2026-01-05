package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.simpmusic.ui.component.PlayPauseButton
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_skip_next_24
import simpmusic.composeapp.generated.resources.baseline_skip_previous_24
import simpmusic.composeapp.generated.resources.holder
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
    windowState: WindowState
) {
    val nowPlayingData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timeline by sharedViewModel.timeline.collectAsStateWithLifecycle()
    
    // Check if there's any track playing
    val hasTrack = nowPlayingData?.nowPlayingTitle?.isNotBlank() == true
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!hasTrack) {
                // Show empty state
                EmptyMiniPlayerState()
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    when {
                        maxWidth < 260.dp -> CompactMiniLayout(
                            controllerState = controllerState,
                            timeline = timeline,
                            onUIEvent = sharedViewModel::onUIEvent
                        )
                        maxWidth < 360.dp -> MediumMiniLayout(
                            nowPlayingData = nowPlayingData!!,
                            controllerState = controllerState,
                            timeline = timeline,
                            onUIEvent = sharedViewModel::onUIEvent
                        )
                        else -> ExpandedMiniLayout(
                            nowPlayingData = nowPlayingData!!,
                            controllerState = controllerState,
                            timeline = timeline,
                            onUIEvent = sharedViewModel::onUIEvent
                        )
                    }
                }
            }
            
            // Close button (top-right corner)
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Drag handle (top area for moving window)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.7f)
                    .height(32.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val location = MouseInfo.getPointerInfo().location
                            windowState.position = androidx.compose.ui.window.WindowPosition(
                                (location.x - dragAmount.x).dp,
                                (location.y - dragAmount.y).dp
                            )
                        }
                    }
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.MOVE_CURSOR)))
            )
        }
    }
}

/**
 * Empty state when no track is playing
 */
@Composable
private fun EmptyMiniPlayerState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No track playing",
                style = typo().bodyMedium.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Play something to see controls",
                style = typo().bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Legacy full layout - now used only when BoxWithConstraints shows > 360dp
 * Kept for backwards compatibility
 */
@Composable
private fun ExpandedMiniLayout(
    nowPlayingData: com.maxrave.simpmusic.viewModel.NowPlayingScreenData,
    controllerState: com.maxrave.domain.mediaservice.handler.ControlState,
    timeline: TimeLine,
    onUIEvent: (UIEvent) -> Unit
) {
    val artworkInteractionSource = remember { MutableInteractionSource() }
    val isArtworkHovered by artworkInteractionSource.collectIsHoveredAsState()
    val artworkScale by animateFloatAsState(
        targetValue = if (isArtworkHovered) 1.08f else 1f,
        animationSpec = tween(250)
    )
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = tween(300)),
        color = Color(0xFF1C1C1E)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main content area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Album artwork with hover animation
                AsyncImage(
                    model = nowPlayingData.thumbnailURL,
                    contentDescription = "Album Art",
                    placeholder = painterResource(Res.drawable.holder),
                    error = painterResource(Res.drawable.holder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(artworkScale)
                        .clip(RoundedCornerShape(8.dp))
                        .hoverable(artworkInteractionSource)
                )
                
                // Track info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = nowPlayingData.nowPlayingTitle,
                        style = typo().bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nowPlayingData.artistName,
                        style = typo().bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
                
                // Playback controls
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300, delayMillis = 150)),
                    exit = fadeOut(tween(200))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RippleIconButton(
                            resId = Res.drawable.baseline_skip_previous_24,
                            modifier = Modifier.size(28.dp),
                            tint = if (controllerState.isPreviousAvailable) Color.White else Color.Gray,
                            onClick = {
                                if (controllerState.isPreviousAvailable) {
                                    onUIEvent(UIEvent.Previous)
                                }
                            }
                        )
                    
                        PlayPauseButton(
                            isPlaying = controllerState.isPlaying,
                            modifier = Modifier.size(40.dp),
                            onClick = {
                                onUIEvent(UIEvent.PlayPause)
                            }
                        )
                    
                        RippleIconButton(
                            resId = Res.drawable.baseline_skip_next_24,
                            modifier = Modifier.size(32.dp),
                            tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                            onClick = {
                                if (controllerState.isNextAvailable) {
                                    onUIEvent(UIEvent.Next)
                                }
                            }
                        )
                    }
                }
            }
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0xFF2C2C2E))
            ) {
                if (timeline.total > 0L && timeline.current >= 0L) {
                    LinearProgressIndicator(
                        progress = { timeline.current.toFloat() / timeline.total },
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White,
                        trackColor = Color.Transparent,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
