package com.maxrave.simpmusic.data.model.home

import androidx.compose.runtime.Immutable
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.utils.Resource

@Immutable
data class HomeDataCombine(
    val home: Resource<ArrayList<HomeItem>>,
    val mood: Resource<Mood>,
    val chart: Resource<Chart>,
    val newRelease: Resource<ArrayList<HomeItem>>,
)