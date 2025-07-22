package com.maxrave.simpmusic.data.model.explore.mood.moodmoments

import androidx.compose.runtime.Immutable

@Immutable
data class MoodsMomentObject(
    val endpoint: String,
    val header: String,
    val items: List<Item>,
    val params: String,
)