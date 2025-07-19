package com.maxrave.simpmusic.data.model.explore.mood

import androidx.compose.runtime.Immutable

@Immutable
data class Mood(
    val genres: ArrayList<Genre>,
    val moodsMoments: ArrayList<MoodsMoment>,
)