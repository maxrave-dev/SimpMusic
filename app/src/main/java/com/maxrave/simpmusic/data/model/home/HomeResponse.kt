package com.maxrave.simpmusic.data.model.home

import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.utils.Resource


data class HomeResponse(
    val homeItem: Resource<ArrayList<HomeItem>>,
    val exploreMood: Resource<Mood>,
    val exploreChart: Resource<Chart>
)