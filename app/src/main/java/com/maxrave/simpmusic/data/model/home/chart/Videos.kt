package com.maxrave.simpmusic.data.model.home.chart

import androidx.compose.runtime.Immutable

@Immutable
data class Videos(
    val items: ArrayList<ItemVideo>,
    val playlist: String,
)