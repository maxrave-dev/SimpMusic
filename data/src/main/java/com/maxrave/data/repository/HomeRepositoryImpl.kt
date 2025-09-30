package com.maxrave.data.repository

import android.content.Context
import com.maxrave.data.parser.parseChart
import com.maxrave.data.parser.parseGenreObject
import com.maxrave.data.parser.parseMixedContent
import com.maxrave.data.parser.parseMoodsMomentObject
import com.maxrave.data.parser.parseNewRelease
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.mood.Genre
import com.maxrave.domain.data.model.mood.Mood
import com.maxrave.domain.data.model.mood.MoodsMoment
import com.maxrave.domain.data.model.mood.genre.GenreObject
import com.maxrave.domain.data.model.mood.moodmoments.MoodsMomentObject
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class HomeRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val youTube: YouTube,
) : HomeRepository {
    override fun getHomeData(
        context: Context,
        params: String?,
    ): Flow<Resource<List<HomeItem>>> =
        flow {
            runCatching {
                val limit = dataStoreManager.homeLimit.first()
                youTube
                    .customQuery(browseId = "FEmusic_home", params = params)
                    .onSuccess { result ->
                        val list: ArrayList<HomeItem> = arrayListOf()
                        if (result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicCarouselShelfRenderer
                                ?.header
                                ?.musicCarouselShelfBasicHeaderRenderer
                                ?.strapline
                                ?.runs
                                ?.get(
                                    0,
                                )?.text != null
                        ) {
                            val accountName =
                                result.contents
                                    ?.singleColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(
                                        0,
                                    )?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(
                                        0,
                                    )?.musicCarouselShelfRenderer
                                    ?.header
                                    ?.musicCarouselShelfBasicHeaderRenderer
                                    ?.strapline
                                    ?.runs
                                    ?.get(
                                        0,
                                    )?.text ?: ""
                            val accountThumbUrl =
                                result.contents
                                    ?.singleColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(
                                        0,
                                    )?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(
                                        0,
                                    )?.musicCarouselShelfRenderer
                                    ?.header
                                    ?.musicCarouselShelfBasicHeaderRenderer
                                    ?.thumbnail
                                    ?.musicThumbnailRenderer
                                    ?.thumbnail
                                    ?.thumbnails
                                    ?.get(
                                        0,
                                    )?.url
                                    ?.replace("s88", "s352") ?: ""
                            if (accountName != "" && accountThumbUrl != "") {
                                dataStoreManager.putString("AccountName", accountName)
                                dataStoreManager.putString("AccountThumbUrl", accountThumbUrl)
                            }
                        }
                        var continueParam =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.continuations
                                ?.get(
                                    0,
                                )?.nextContinuationData
                                ?.continuation
                        val data =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                        list.addAll(parseMixedContent(data, context))
                        var count = 0
                        while (count < limit && continueParam != null) {
                            youTube
                                .customQuery(browseId = "", continuation = continueParam)
                                .onSuccess { response ->
                                    continueParam =
                                        response.continuationContents
                                            ?.sectionListContinuation
                                            ?.continuations
                                            ?.get(
                                                0,
                                            )?.nextContinuationData
                                            ?.continuation
                                    Logger.d("Repository", "continueParam: $continueParam")
                                    val dataContinue =
                                        response.continuationContents?.sectionListContinuation?.contents
                                    list.addAll(parseMixedContent(dataContinue, context))
                                    count++
                                    Logger.d("Repository", "count: $count")
                                }.onFailure {
                                    Logger.e("Repository", "Error: ${it.message}")
                                    count++
                                }
                        }
                        Logger.d("Repository", "List size: ${list.size}")
                        emit(Resource.Success<List<HomeItem>>(list.toList()))
                    }.onFailure { error ->
                        emit(Resource.Error<List<HomeItem>>(error.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getNewRelease(context: Context): Flow<Resource<List<HomeItem>>> =
        flow {
            youTube
                .newRelease()
                .onSuccess { result ->
                    emit(Resource.Success<List<HomeItem>>(parseNewRelease(result, context)))
                }.onFailure { error ->
                    emit(Resource.Error<List<HomeItem>>(error.message.toString()))
                }
        }.flowOn(Dispatchers.IO)

    override fun getChartData(countryCode: String): Flow<Resource<Chart>> =
        flow {
            runCatching {
                youTube
                    .customQuery("FEmusic_charts", country = countryCode)
                    .onSuccess { result ->
                        val data =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                        val chart = parseChart(data)
                        if (chart != null) {
                            emit(Resource.Success<Chart>(chart))
                        } else {
                            emit(Resource.Error<Chart>("Error"))
                        }
                    }.onFailure { error ->
                        emit(Resource.Error<Chart>(error.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getMoodAndMomentsData(): Flow<Resource<Mood>> =
        flow {
            runCatching {
                youTube
                    .moodAndGenres()
                    .onSuccess { result ->
                        val listMoodMoments: ArrayList<MoodsMoment> = arrayListOf()
                        val listGenre: ArrayList<Genre> = arrayListOf()
                        result[0].let { moodsmoment ->
                            for (item in moodsmoment.items) {
                                listMoodMoments.add(
                                    MoodsMoment(
                                        params = item.endpoint.params ?: "",
                                        title = item.title,
                                    ),
                                )
                            }
                        }
                        result[1].let { genres ->
                            for (item in genres.items) {
                                listGenre.add(
                                    Genre(
                                        params = item.endpoint.params ?: "",
                                        title = item.title,
                                    ),
                                )
                            }
                        }
                        emit(Resource.Success<Mood>(Mood(listGenre, listMoodMoments)))
                    }.onFailure { e ->
                        emit(Resource.Error<Mood>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getMoodData(params: String): Flow<Resource<MoodsMomentObject>> =
        flow {
            runCatching {
                youTube
                    .customQuery(
                        browseId = "FEmusic_moods_and_genres_category",
                        params = params,
                    ).onSuccess { result ->
                        val data = parseMoodsMomentObject(result)
                        if (data != null) {
                            emit(Resource.Success<MoodsMomentObject>(data))
                        } else {
                            emit(Resource.Error<MoodsMomentObject>("Error"))
                        }
                    }.onFailure { e ->
                        emit(Resource.Error<MoodsMomentObject>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getGenreData(params: String): Flow<Resource<GenreObject>> =
        flow {
            runCatching {
                youTube
                    .customQuery(
                        browseId = "FEmusic_moods_and_genres_category",
                        params = params,
                    ).onSuccess { result ->
                        val data = parseGenreObject(result)
                        if (data != null) {
                            emit(Resource.Success<GenreObject>(data))
                        } else {
                            emit(Resource.Error<GenreObject>("Error"))
                        }
                    }.onFailure { e ->
                        emit(Resource.Error<GenreObject>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)
}