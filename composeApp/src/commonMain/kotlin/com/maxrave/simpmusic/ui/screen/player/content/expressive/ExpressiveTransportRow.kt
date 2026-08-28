package com.maxrave.simpmusic.ui.screen.player.content.expressive

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.viewModel.UIEvent

// Base weights: prev 0.55 | play 1.2 | next 0.55. A pressed button grows ×1.15; because Row
// normalizes weights, the other two shrink proportionally without any extra bookkeeping.
private const val SIDE_WEIGHT = 0.55f
private const val PLAY_WEIGHT = 1.2f
private const val PRESS_GROWTH = 1.15f

/**
 * M3-Expressive transport: three pill buttons in a 68dp row.
 *
 * - Play/pause corner radius morphs 22dp (playing) ↔ 34dp (paused) on
 *   [MaterialTheme.motionScheme]'s default spatial spring.
 * - The pressed button's weight grows ×1.15 on the fast spatial spring while the
 *   neighbours shrink proportionally (Row weight normalization).
 * - Icon swaps ride Crossfade with the fast effects spec.
 * - While [loading], a small CircularProgressIndicator replaces the play/pause icon and the
 *   press is a no-op — mirroring the spinner block in NowPlayingContentSpotify's toolbar.
 * - Prev/next respect [ControlState.isPreviousAvailable]/[ControlState.isNextAvailable] the
 *   way the Classic style does: icon at 0.4f alpha and a no-op click.
 *
 * Sends [UIEvent.PlayPause] / [UIEvent.Previous] / [UIEvent.Next] exactly like
 * [com.maxrave.simpmusic.ui.component.PlayerControlLayout].
 */
@Composable
fun ExpressiveTransportRow(
    controllerState: ControlState,
    loading: Boolean,
    onUIEvent: (UIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val motionScheme = MaterialTheme.motionScheme

    val prevInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val prevPressed by prevInteraction.collectIsPressedAsState()
    val playPressed by playInteraction.collectIsPressedAsState()
    val nextPressed by nextInteraction.collectIsPressedAsState()

    val prevWeight by animateFloatAsState(
        targetValue = if (prevPressed) SIDE_WEIGHT * PRESS_GROWTH else SIDE_WEIGHT,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "prevWeight",
    )
    val playWeight by animateFloatAsState(
        targetValue = if (playPressed) PLAY_WEIGHT * PRESS_GROWTH else PLAY_WEIGHT,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "playWeight",
    )
    val nextWeight by animateFloatAsState(
        targetValue = if (nextPressed) SIDE_WEIGHT * PRESS_GROWTH else SIDE_WEIGHT,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "nextWeight",
    )

    val playCorner by animateDpAsState(
        targetValue = if (controllerState.isPlaying) 22.dp else 34.dp,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "playCorner",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .height(68.dp),
    ) {
        // Previous — full pill on secondaryContainer.
        Surface(
            onClick = {
                if (controllerState.isPreviousAvailable) {
                    onUIEvent(UIEvent.Previous)
                }
            },
            shape = RoundedCornerShape(34.dp),
            color = colorScheme.secondaryContainer,
            interactionSource = prevInteraction,
            modifier =
                Modifier
                    .weight(prevWeight)
                    .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = SimpIcons.SkipPrevious,
                    contentDescription = "",
                    tint =
                        colorScheme.onSecondaryContainer.copy(
                            alpha = if (controllerState.isPreviousAvailable) 1f else 0.4f,
                        ),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        // Play / Pause — primary container, corner radius morphs with playback state.
        Surface(
            onClick = {
                if (!loading) {
                    onUIEvent(UIEvent.PlayPause)
                }
            },
            shape = RoundedCornerShape(playCorner),
            color = colorScheme.primary,
            interactionSource = playInteraction,
            modifier =
                Modifier
                    .weight(playWeight)
                    .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Crossfade(
                    targetState = loading,
                    animationSpec = motionScheme.fastEffectsSpec(),
                    label = "playLoading",
                ) { isLoading ->
                    if (isLoading) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 3.dp,
                            )
                        }
                    } else {
                        Crossfade(
                            targetState = controllerState.isPlaying,
                            animationSpec = motionScheme.fastEffectsSpec(),
                            label = "playPauseIcon",
                        ) { isPlaying ->
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                                    contentDescription = "",
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        // Next — mirror of Previous.
        Surface(
            onClick = {
                if (controllerState.isNextAvailable) {
                    onUIEvent(UIEvent.Next)
                }
            },
            shape = RoundedCornerShape(34.dp),
            color = colorScheme.secondaryContainer,
            interactionSource = nextInteraction,
            modifier =
                Modifier
                    .weight(nextWeight)
                    .fillMaxHeight(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = SimpIcons.SkipNext,
                    contentDescription = "",
                    tint =
                        colorScheme.onSecondaryContainer.copy(
                            alpha = if (controllerState.isNextAvailable) 1f else 0.4f,
                        ),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
