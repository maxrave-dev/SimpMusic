package com.maxrave.simpmusic.data.model.explore.mood


import com.google.gson.annotations.SerializedName

data class Mood(
    @SerializedName(value = "Genres", alternate = ["Thể loại"])
    val genres: ArrayList<Genre>,
    @SerializedName(value = "Moods & moments", alternate = ["Tâm trạng và khoảnh khắc"])
    val moodsMoments: ArrayList<MoodsMoment>
)