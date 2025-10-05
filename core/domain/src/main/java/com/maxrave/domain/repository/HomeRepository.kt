package com.maxrave.domain.repository

import android.content.Context
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.data.model.mood.genre.GenreObject
import com.maxrave.domain.data.model.mood.moodmoments.MoodsMomentObject
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun getHomeData(
        context: Context,
        params: String? = null,
    ): Flow<Resource<List<HomeItem>>>

    fun getNewRelease(context: Context): Flow<Resource<List<HomeItem>>>

    fun getChartData(countryCode: String = "KR"): Flow<Resource<Chart>>

    fun getMoodAndMomentsData(): Flow<Resource<Mood>>

    fun getGenreData(params: String): Flow<Resource<GenreObject>>

    fun getMoodData(params: String): Flow<Resource<MoodsMomentObject>>
}