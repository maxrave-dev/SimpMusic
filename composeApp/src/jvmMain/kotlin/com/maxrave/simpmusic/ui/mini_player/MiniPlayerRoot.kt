package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
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

/**
 * Root composable for the mini player window content.
 * Displays current track information and playback controls.
 */
@Composable
fun MiniPlayerRoot(sharedViewModel: SharedViewModel) {
    val nowPlayingData by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timeline by sharedViewModel.timeline.collectAsStateWithLifecycle()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
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
                // Album artwork
                AsyncImage(
                    model = nowPlayingData.thumbnailURL,
                    contentDescription = "Album Art",
                    placeholder = painterResource(Res.drawable.holder),
                    error = painterResource(Res.drawable.holder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RippleIconButton(
                        resId = Res.drawable.baseline_skip_previous_24,
                        modifier = Modifier.size(32.dp),
                        tint = if (controllerState.isPreviousAvailable) Color.White else Color.Gray,
                        onClick = {
                            if (controllerState.isPreviousAvailable) {
                                sharedViewModel.onUIEvent(UIEvent.Previous)
                            }
                        }
                    )
                    
                    PlayPauseButton(
                        isPlaying = controllerState.isPlaying,
                        modifier = Modifier.size(40.dp),
                        onClick = {
                            sharedViewModel.onUIEvent(UIEvent.PlayPause)
                        }
                    )
                    
                    RippleIconButton(
                        resId = Res.drawable.baseline_skip_next_24,
                        modifier = Modifier.size(32.dp),
                        tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                        onClick = {
                            if (controllerState.isNextAvailable) {
                                sharedViewModel.onUIEvent(UIEvent.Next)
                            }
                        }
                    )
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
