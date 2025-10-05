package com.maxrave.domain.data.model.mood.moodmoments

data class MoodsMomentObject(
    val endpoint: String,
    val header: String,
    val items: List<Item>,
    val params: String,
)