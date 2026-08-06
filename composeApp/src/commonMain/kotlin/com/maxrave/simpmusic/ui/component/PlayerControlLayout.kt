package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.ui.icon.PauseCircle
import com.maxrave.simpmusic.ui.icon.PlayCircle
import com.maxrave.simpmusic.ui.icon.Forward5
import com.maxrave.simpmusic.ui.icon.Repeat
import com.maxrave.simpmusic.ui.icon.RepeatOne
import com.maxrave.simpmusic.ui.icon.Replay5
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.SkipNext
import com.maxrave.simpmusic.ui.icon.SkipPrevious
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.viewModel.UIEvent

@Composable
fun PlayerControlLayout(
    controllerState: ControlState,
    isSmallSize: Boolean = false,
    isPodcast: Boolean = false,
    contentColor: Color = Color.White,
    onUIEvent: (UIEvent) -> Unit,
) {
    val height = if (isSmallSize) 48.dp else 96.dp
    val smallIcon = if (isSmallSize) 20.dp to 28.dp else 32.dp to 42.dp
    val mediumIcon = if (isSmallSize) 28.dp to 38.dp else 42.dp to 52.dp
    val bigIcon = if (isSmallSize) 38.dp to 48.dp else 72.dp to 96.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 20.dp),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(smallIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .clickable {
                            if (isPodcast) {
                                if (controllerState.isPreviousAvailable) onUIEvent(UIEvent.Previous)
                            } else {
                                onUIEvent(UIEvent.Shuffle)
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                if (isPodcast) {
                    Icon(
                        imageVector = SimpIcons.SkipPrevious,
                        tint = if (controllerState.isPreviousAvailable) contentColor else contentColor.copy(alpha = 0.4f),
                        contentDescription = "Previous episode",
                        modifier = Modifier.size(smallIcon.first),
                    )
                } else {
                    Crossfade(targetState = controllerState.isShuffle, label = "Shuffle Button") { isShuffle ->
                        Icon(
                            imageVector = SimpIcons.Shuffle,
                            tint = if (isShuffle) seed else contentColor,
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(smallIcon.first),
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(mediumIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .clickable {
                            if (isPodcast) {
                                onUIEvent(UIEvent.Backward)
                            } else if (controllerState.isPreviousAvailable) {
                                onUIEvent(UIEvent.Previous)
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPodcast) SimpIcons.Replay5 else SimpIcons.SkipPrevious,
                    tint = if (isPodcast || controllerState.isPreviousAvailable) contentColor else contentColor.copy(alpha = 0.4f),
                    contentDescription = if (isPodcast) "Back 5 seconds" else "Previous",
                    modifier = Modifier.size(mediumIcon.first),
                )
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(bigIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .clickable {
                            onUIEvent(UIEvent.PlayPause)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = controllerState.isPlaying) { isPlaying ->
                    if (!isPlaying) {
                        Icon(
                            imageVector = SimpIcons.PlayCircle,
                            tint = contentColor,
                            contentDescription = "",
                            modifier = Modifier.size(bigIcon.first),
                        )
                    } else {
                        Icon(
                            imageVector = SimpIcons.PauseCircle,
                            tint = contentColor,
                            contentDescription = "",
                            modifier = Modifier.size(bigIcon.first),
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(mediumIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .clickable {
                            if (isPodcast) {
                                onUIEvent(UIEvent.Forward)
                            } else if (controllerState.isNextAvailable) {
                                onUIEvent(UIEvent.Next)
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPodcast) SimpIcons.Forward5 else SimpIcons.SkipNext,
                    tint = if (isPodcast || controllerState.isNextAvailable) contentColor else contentColor.copy(alpha = 0.4f),
                    contentDescription = if (isPodcast) "Forward 5 seconds" else "Next",
                    modifier = Modifier.size(mediumIcon.first),
                )
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .size(smallIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .clickable {
                            if (isPodcast) {
                                if (controllerState.isNextAvailable) onUIEvent(UIEvent.Next)
                            } else {
                                onUIEvent(UIEvent.Repeat)
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                if (isPodcast) {
                    Icon(
                        imageVector = SimpIcons.SkipNext,
                        tint = if (controllerState.isNextAvailable) contentColor else contentColor.copy(alpha = 0.4f),
                        contentDescription = "Next episode",
                        modifier = Modifier.size(smallIcon.first),
                    )
                } else {
                    Crossfade(targetState = controllerState.repeatState) { rs ->
                        when (rs) {
                            is RepeatState.None -> {
                                Icon(
                                    imageVector = SimpIcons.Repeat,
                                    tint = contentColor,
                                    contentDescription = "Repeat",
                                    modifier = Modifier.size(smallIcon.first),
                                )
                            }

                            RepeatState.All -> {
                                Icon(
                                    imageVector = SimpIcons.Repeat,
                                    tint = seed,
                                    contentDescription = "Repeat all",
                                    modifier = Modifier.size(smallIcon.first),
                                )
                            }

                            RepeatState.One -> {
                                Icon(
                                    imageVector = SimpIcons.RepeatOne,
                                    tint = seed,
                                    contentDescription = "Repeat one",
                                    modifier = Modifier.size(smallIcon.first),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
