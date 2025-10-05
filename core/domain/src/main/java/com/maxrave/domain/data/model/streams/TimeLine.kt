package com.maxrave.domain.data.model.streams

data class TimeLine(
    val current: Long,
    val total: Long,
    val bufferedPercent: Int,
    val loading: Boolean = true,
)