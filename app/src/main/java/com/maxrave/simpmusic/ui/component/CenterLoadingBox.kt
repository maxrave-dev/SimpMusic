package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CenterLoadingBox(modifier: Modifier) {
    var containerShape by remember {
        mutableStateOf(LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.first())
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            containerShape = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.random()
            delay(500)
        }
    }

    Box(modifier = modifier) {
        Crossfade(
            containerShape,
            modifier =
                Modifier
                    .wrapContentSize()
                    .align(Alignment.Center),
        ) { shape ->
            ContainedLoadingIndicator(
                modifier =
                    Modifier
                        .size(56.dp),
                polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons,
                containerColor = Color.DarkGray,
                indicatorColor = Color.LightGray,
                containerShape = shape.toShape(),
            )
        }
    }
}