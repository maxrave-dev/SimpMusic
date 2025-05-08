package com.maxrave.simpmusic.data.model.home

import androidx.compose.runtime.Immutable
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.utils.Resource

@Immutable
data class HomeResponse(
    val homeItem: Resource<ArrayList<HomeItem>>,
    val exploreMood: Resource<Mood>,
    val exploreChart: Resource<Chart>,
)