package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_pause_24
import simpmusic.composeapp.generated.resources.baseline_play_arrow_24

@Composable
fun RippleIconButton(
    resId: DrawableResource,
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = false,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painterResource(resId),
            null,
            tint = tint,
            modifier = if (fillMaxSize) Modifier.fillMaxSize().padding(4.dp) else Modifier,
        )
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    RippleIconButton(
        if (!isPlaying) {
            Res.drawable.baseline_play_arrow_24
        } else {
            Res.drawable.baseline_pause_24
        },
        modifier = modifier,
        onClick = onClick,
    )
}