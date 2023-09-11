package com.maxrave.simpmusic.data.model.explore.mood


import com.google.gson.annotations.SerializedName

data class Mood(
    @SerializedName(value = "Genres", alternate = ["Thể loại", "Gatunki"])
    val genres: ArrayList<Genre>,
    @SerializedName(value = "Moods & moments", alternate = ["Tâm trạng và khoảnh khắc", "Nastroje i momenty"])
    val moodsMoments: ArrayList<MoodsMoment>
)