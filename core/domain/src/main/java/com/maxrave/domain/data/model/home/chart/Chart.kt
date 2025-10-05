package com.maxrave.domain.data.model.home.chart

data class Chart(
    val artists: Artists,
    val countries: Countries?,
    val listChartItem: List<ChartItemPlaylist>,
)