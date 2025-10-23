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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.transparent
import com.maxrave.simpmusic.viewModel.UIEvent

@Composable
fun PlayerControlLayout(
    controllerState: ControlState,
    onUIEvent: (UIEvent) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 20.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .background(transparent)
                    .size(42.dp)
                    .aspectRatio(1f)
                    .clip(
                        CircleShape,
                    ).weight(1f)
                    .clickable {
                        onUIEvent(UIEvent.Shuffle)
                    },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = controllerState.isShuffle, label = "Shuffle Button") { isShuffle ->
                if (!isShuffle) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        tint = Color.White,
                        contentDescription = "",
                        modifier = Modifier.size(32.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        tint = seed,
                        contentDescription = "",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .background(transparent)
                    .size(52.dp)
                    .aspectRatio(1f)
                    .clip(
                        CircleShape,
                    ).weight(1f)
                    .clickable {
                        if (controllerState.isPreviousAvailable) {
                            onUIEvent(UIEvent.Previous)
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                tint = if (controllerState.isPreviousAvailable) Color.White else Color.Gray,
                contentDescription = "",
                modifier = Modifier.size(42.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .background(transparent)
                    .size(96.dp)
                    .aspectRatio(1f)
                    .clip(
                        CircleShape,
                    ).weight(1f)
                    .clickable {
                        onUIEvent(UIEvent.PlayPause)
                    },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = controllerState.isPlaying) { isPlaying ->
                if (!isPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        tint = Color.White,
                        contentDescription = "",
                        modifier = Modifier.size(72.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PauseCircle,
                        tint = Color.White,
                        contentDescription = "",
                        modifier = Modifier.size(72.dp),
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .background(transparent)
                    .size(52.dp)
                    .aspectRatio(1f)
                    .clip(
                        CircleShape,
                    ).weight(1f)
                    .clickable {
                        if (controllerState.isNextAvailable) {
                            onUIEvent(UIEvent.Next)
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                tint = if (controllerState.isNextAvailable) Color.White else Color.Gray,
                contentDescription = "",
                modifier = Modifier.size(42.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(42.dp)
                    .aspectRatio(1f)
                    .clip(
                        CircleShape,
                    ).weight(1f)
                    .clickable {
                        onUIEvent(UIEvent.Repeat)
                    },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = controllerState.repeatState) { rs ->
                when (rs) {
                    is RepeatState.None -> {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            tint = Color.White,
                            contentDescription = "",
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    RepeatState.All -> {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            tint = seed,
                            contentDescription = "",
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    RepeatState.One -> {
                        Icon(
                            imageVector = Icons.Rounded.RepeatOne,
                            tint = seed,
                            contentDescription = "",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }
    }
}