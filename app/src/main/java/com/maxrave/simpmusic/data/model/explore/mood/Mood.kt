package com.maxrave.simpmusic.data.model.explore.mood


import com.google.gson.annotations.SerializedName

data class Mood(
    @SerializedName("Genres")
    val genres: ArrayList<Genre>,
    @SerializedName("Moods & moments")
    val moodsMoments: ArrayList<MoodsMoment>
)