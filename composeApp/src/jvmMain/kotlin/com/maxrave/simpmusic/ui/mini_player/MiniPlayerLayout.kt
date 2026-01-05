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
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.simpmusic.ui.component.PlayPauseButton
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_skip_next_24
import simpmusic.composeapp.generated.resources.baseline_skip_previous_24
import simpmusic.composeapp.generated.resources.holder

/**
 * Compact layout (< 260dp): Controls only, no artwork or text
 * Perfect for very narrow windows
 */
@Composable
fun CompactMiniLayout(
    controllerState: ControlState,
    timeline: TimeLine,
    onUIEvent: (UIEvent) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val alpha by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0.9f,
        animationSpec = tween(200)
    )
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = tween(300))
            .hoverable(interactionSource),
        color = Color(0xFF1C1C1E)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Controls only - centered
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .alpha(alpha),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        modifier = Modifier.size(36.dp),
                        onClick = { onUIEvent(UIEvent.PlayPause) }
                    )
                    
                    RippleIconButton(
                        resId = Res.drawable.baseline_skip_next_24,
                        modifier = Modifier.size(28.dp),
                        tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                        onClick = {
                            if (controllerState.isNextAvailable) {
                                onUIEvent(UIEvent.Next)
                            }
                        }
                    )
                }
            }
            
            // Progress bar
            ProgressBar(timeline)
        }
    }
}

/**
 * Medium layout (260-360dp): Artwork + controls, no text
 * Good balance for medium-sized windows
 */
@Composable
fun MediumMiniLayout(
    nowPlayingData: NowPlayingScreenData,
    controllerState: ControlState,
    timeline: TimeLine,
    onUIEvent: (UIEvent) -> Unit
) {
    val artworkInteractionSource = remember { MutableInteractionSource() }
    val isArtworkHovered by artworkInteractionSource.collectIsHoveredAsState()
    val artworkScale by animateFloatAsState(
        targetValue = if (isArtworkHovered) 1.05f else 1f,
        animationSpec = tween(200)
    )
    
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = tween(300)),
        color = Color(0xFF1C1C1E)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Smaller artwork with hover effect
                AsyncImage(
                    model = nowPlayingData.thumbnailURL,
                    contentDescription = "Album Art",
                    placeholder = painterResource(Res.drawable.holder),
                    error = painterResource(Res.drawable.holder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .scale(artworkScale)
                        .clip(RoundedCornerShape(6.dp))
                        .hoverable(artworkInteractionSource)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Controls
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
                        modifier = Modifier.size(36.dp),
                        onClick = { onUIEvent(UIEvent.PlayPause) }
                    )
                    
                    RippleIconButton(
                        resId = Res.drawable.baseline_skip_next_24,
                        modifier = Modifier.size(28.dp),
                        tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                        onClick = {
                            if (controllerState.isNextAvailable) {
                                onUIEvent(UIEvent.Next)
                            }
                        }
                    )
                }
            }
            
            // Progress bar
            ProgressBar(timeline)
        }
    }
}

/**
 * Progress bar component shared across all layouts
 */
@Composable
private fun ProgressBar(timeline: TimeLine) {
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
