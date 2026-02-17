package com.maxrave.simpmusic.wear.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon

@Composable
fun PlaybackControlIcon(
    isPlaying: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val iconModifier = modifier.size(20.dp)
    when {
        isPlaying -> {
            Icon(
                imageVector = Icons.Filled.Pause,
                contentDescription = "Pause",
                modifier = iconModifier,
            )
        }
        isLoading -> {
            CircularProgressIndicator(modifier = iconModifier)
        }
        else -> {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                modifier = iconModifier,
            )
        }
    }
}
