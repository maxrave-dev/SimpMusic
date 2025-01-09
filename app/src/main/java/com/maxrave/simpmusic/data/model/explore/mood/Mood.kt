package com.maxrave.simpmusic.data.model.explore.mood

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class Mood(
    @SerializedName(value = "Genres", alternate = ["Thể loại", "Gatunki", "Per te"])
    val genres: ArrayList<Genre>,
    @SerializedName(value = "Moods & moments", alternate = ["Tâm trạng và khoảnh khắc", "Nastroje i momenty", "Mood e momenti"])
    val moodsMoments: ArrayList<MoodsMoment>,
)