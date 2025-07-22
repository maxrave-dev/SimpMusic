package com.maxrave.simpmusic.data.model.explore.mood.moodmoments

import androidx.compose.runtime.Immutable

@Immutable
data class Item(
    val contents: List<Content>,
    val header: String,
)