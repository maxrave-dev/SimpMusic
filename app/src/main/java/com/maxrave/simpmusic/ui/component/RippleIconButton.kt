package com.maxrave.simpmusic.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.R

@Composable
fun RippleIconButton(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painterResource(id = resId),
            null,
            tint = Color.White,
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
            R.drawable.baseline_play_arrow_24
        } else {
            R.drawable.baseline_pause_24
        },
        modifier = modifier,
        onClick = onClick,
    )
}