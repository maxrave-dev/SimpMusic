package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CenterLoadingBox(modifier: Modifier) {
    Box(modifier = modifier) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .width(32.dp)
                    .align(Alignment.Center),
        )
    }
}