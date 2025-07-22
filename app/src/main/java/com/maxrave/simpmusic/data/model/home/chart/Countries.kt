package com.maxrave.simpmusic.data.model.home.chart

import androidx.compose.runtime.Immutable

@Immutable
data class Countries(
    val options: List<String>,
    val selected: Selected,
)