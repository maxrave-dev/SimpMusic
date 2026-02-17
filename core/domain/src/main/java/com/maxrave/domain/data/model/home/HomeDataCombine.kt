package com.maxrave.domain.data.model.home

import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.utils.Resource

data class HomeDataCombine(
    val home: Resource<List<HomeItem>>,
    val mood: Resource<Mood>,
    val chart: Resource<Chart>,
    val newRelease: Resource<List<HomeItem>>,
)