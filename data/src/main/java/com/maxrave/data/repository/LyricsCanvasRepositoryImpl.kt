package com.maxrave.data.repository

import com.maxrave.data.db.LocalDataSource
import com.maxrave.data.mapping.toCanvasResult
import com.maxrave.data.mapping.toLyrics
import com.maxrave.domain.data.entities.LyricsEntity
import com.maxrave.domain.data.entities.TranslatedLyricsEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.canvas.CanvasResult
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.LyricsCanvasRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.connectArtists
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toPlainLrcString
import com.maxrave.domain.utils.toSyncedLrcString
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.logger.Logger
import com.maxrave.spotify.Spotify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.simpmusic.aiservice.AiClient
import org.simpmusic.lyrics.SimpMusicLyricsClient
import org.simpmusic.lyrics.models.request.LyricsBody
import org.simpmusic.lyrics.models.request.TranslatedLyricsBody
import kotlin.math.abs

internal class LyricsCanvasRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
    private val spotify: Spotify,
    private val simpMusicLyrics: SimpMusicLyricsClient,
    private val aiClient: AiClient,
) : LyricsCanvasRepository {
    override fun getSavedLyrics(videoId: String): Flow<LyricsEntity?> = flow { emit(localDataSource.getSavedLyrics(videoId)) }.flowOn(Dispatchers.IO)

    override suspend fun insertLyrics(lyricsEntity: LyricsEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertLyrics(lyricsEntity)
        }

    override suspend fun insertTranslatedLyrics(translatedLyrics: TranslatedLyricsEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertTranslatedLyrics(translatedLyrics)
        }

    override fun getSavedTranslatedLyrics(
        videoId: String,
        language: String,
    ): Flow<TranslatedLyricsEntity?> = flow { emit(localDataSource.getTranslatedLyrics(videoId, language)) }.flowOn(Dispatchers.IO)

    override suspend fun removeTranslatedLyrics(
        videoId: String,
        language: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.removeTranslatedLyrics(videoId, language)
    }

    override fun getYouTubeCaption(
        preferLang: String,
        videoId: String,
    ): Flow<Resource<Pair<Lyrics, Lyrics?>>> =
        flow {
            runCatching {
                youTube
                    .getYouTubeCaption(videoId, preferLang)
                    .onSuccess { lyrics ->
                        emit(
                            Resource.Success<Pair<Lyrics, Lyrics?>>(
                                Pair(lyrics.first.toLyrics(), lyrics.second?.toLyrics()),
                            ),
                        )
                    }.onFailure { e ->
                        Logger.d("Lyrics", "Error: ${e.message}")
                        emit(Resource.Error<Pair<Lyrics, Lyrics?>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getCanvas(
        dataStoreManager: DataStoreManager,
        videoId: String,
        duration: Int,
    ): Flow<Resource<CanvasResult>> =
        flow {
            runCatching {
                localDataSource.getSong(videoId).let { song ->
                    val q =
                        "${song?.title} ${song?.artistName?.firstOrNull() ?: ""}"
                            .replace(
                                Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                                " ",
                            ).replace(
                                Regex("( và | & | и | e | und |, |和| dan)"),
                                " ",
                            ).replace("  ", " ")
                            .replace(Regex("([()])"), "")
                            .replace(".", " ")
                            .replace("  ", " ")
                    var spotifyPersonalToken = ""
                    var spotifyClientToken = ""
                    Logger.w("Lyrics", "getSpotifyLyrics: ${dataStoreManager.spotifyPersonalTokenExpires.first()}")
                    Logger.w("Lyrics", "getSpotifyLyrics ${dataStoreManager.spotifyClientTokenExpires.first()}")
                    Logger.w("Lyrics", "getSpotifyLyrics now: ${System.currentTimeMillis()}")
                    if (dataStoreManager.spotifyPersonalToken
                            .first()
                            .isNotEmpty() &&
                        dataStoreManager.spotifyClientToken.first().isNotEmpty() &&
                        dataStoreManager.spotifyPersonalTokenExpires.first() > System.currentTimeMillis() &&
                        dataStoreManager.spotifyPersonalTokenExpires.first() != 0L &&
                        dataStoreManager.spotifyClientTokenExpires.first() > System.currentTimeMillis() &&
                        dataStoreManager.spotifyClientTokenExpires.first() != 0L
                    ) {
                        spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                        spotifyClientToken = dataStoreManager.spotifyClientToken.first()
                        Logger.d("Canvas", "spotifyPersonalToken: $spotifyPersonalToken")
                        Logger.d("Canvas", "spotifyClientToken: $spotifyClientToken")
                    } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                        spotify
                            .getClientToken()
                            .onSuccess {
                                Logger.d("Canvas", "Request clientToken: ${it.grantedToken.token}")
                                dataStoreManager.setSpotifyClientTokenExpires(
                                    (it.grantedToken.expiresAfterSeconds * 1000L) + System.currentTimeMillis(),
                                )
                                dataStoreManager.setSpotifyClientToken(it.grantedToken.token)
                                spotifyClientToken = it.grantedToken.token
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<CanvasResult>(it.message ?: "Not found"))
                            }
                        spotify
                            .getPersonalTokenWithTotp(dataStoreManager.spdc.first())
                            .onSuccess {
                                spotifyPersonalToken = it.accessToken
                                dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                                dataStoreManager.setSpotifyPersonalTokenExpires(
                                    it.accessTokenExpirationTimestampMs,
                                )
                                Logger.d("Canvas", "Request spotifyPersonalToken: $spotifyPersonalToken")
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<CanvasResult>(it.message ?: "Not found"))
                            }
                    }
                    if (spotifyPersonalToken.isNotEmpty() && spotifyClientToken.isNotEmpty()) {
                        val authToken = spotifyPersonalToken
                        spotify
                            .searchSpotifyTrack(q, authToken, spotifyClientToken)
                            .onSuccess { searchResponse ->
                                Logger.w("Canvas", "searchSpotifyResponse: $searchResponse")
                                val track =
                                    if (duration != 0) {
                                        searchResponse.data?.searchV2?.tracksV2?.items?.find {
                                            abs(
                                                (
                                                    (
                                                        (
                                                            it.item
                                                                ?.data
                                                                ?.duration
                                                                ?.totalMilliseconds ?: (0 / 1000)
                                                        ) - duration
                                                    )
                                                ),
                                            ) < 1
                                        }
                                            ?: searchResponse.data
                                                ?.searchV2
                                                ?.tracksV2
                                                ?.items
                                                ?.firstOrNull()
                                    } else {
                                        searchResponse.data
                                            ?.searchV2
                                            ?.tracksV2
                                            ?.items
                                            ?.firstOrNull()
                                    }
                                if (track != null) {
                                    Logger.w("Canvas", "track: $track")
                                    spotify
                                        .getSpotifyCanvas(
                                            track.item?.data?.id ?: "",
                                            spotifyPersonalToken,
                                            spotifyClientToken,
                                        ).onSuccess {
                                            Logger.w("Canvas", "canvas: $it")
                                            it.toCanvasResult()?.let {
                                                emit(Resource.Success(it))
                                            } ?: run {
                                                emit(Resource.Error<CanvasResult>("Not found"))
                                            }
                                        }.onFailure {
                                            it.printStackTrace()
                                            emit(Resource.Error<CanvasResult>(it.message ?: "Not found"))
                                        }
                                } else {
                                    emit(Resource.Error<CanvasResult>("Not found"))
                                }
                            }.onFailure { throwable ->
                                throwable.printStackTrace()
                                emit(Resource.Error<CanvasResult>(throwable.message ?: "Not found"))
                            }
                    } else {
                        emit(Resource.Error<CanvasResult>("Not found"))
                    }
                }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun updateCanvasUrl(
        videoId: String,
        canvasUrl: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateCanvasUrl(videoId, canvasUrl)
    }

    override suspend fun updateCanvasThumbUrl(
        videoId: String,
        canvasThumbUrl: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateCanvasThumbUrl(videoId, canvasThumbUrl)
    }

    override fun getSpotifyLyrics(
        dataStoreManager: DataStoreManager,
        query: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>> =
        flow {
            runCatching {
                val q =
                    query
                        .replace(
                            Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                            " ",
                        ).replace(
                            Regex("( và | & | и | e | und |, |和| dan)"),
                            " ",
                        ).replace("  ", " ")
                        .replace(Regex("([()])"), "")
                        .replace(".", " ")
                        .replace("  ", " ")
                Logger.d("Lyrics", "query: $q")
                var spotifyPersonalToken = ""
                var spotifyClientToken = ""
                Logger.w("Lyrics", "getSpotifyLyrics: ${dataStoreManager.spotifyPersonalTokenExpires.first()}")
                if (dataStoreManager.spotifyPersonalToken
                        .first()
                        .isNotEmpty() &&
                    dataStoreManager.spotifyPersonalTokenExpires.first() > System.currentTimeMillis() &&
                    dataStoreManager.spotifyPersonalTokenExpires.first() != 0L &&
                    dataStoreManager.spotifyClientTokenExpires.first() > System.currentTimeMillis() &&
                    dataStoreManager.spotifyClientTokenExpires.first() != 0L
                ) {
                    spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                    spotifyClientToken = dataStoreManager.spotifyClientToken.first()
                    Logger.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                    Logger.d("Lyrics", "spotifyClientToken: $spotifyClientToken")
                } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                    runBlocking {
                        spotify
                            .getClientToken()
                            .onSuccess {
                                Logger.d("Canvas", "Request clientToken: ${it.grantedToken.token}")
                                dataStoreManager.setSpotifyClientTokenExpires(
                                    (it.grantedToken.expiresAfterSeconds * 1000L) + System.currentTimeMillis(),
                                )
                                dataStoreManager.setSpotifyClientToken(it.grantedToken.token)
                                spotifyClientToken = it.grantedToken.token
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                    }
                    runBlocking {
                        spotify
                            .getPersonalTokenWithTotp(dataStoreManager.spdc.first())
                            .onSuccess {
                                spotifyPersonalToken = it.accessToken
                                dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                                dataStoreManager.setSpotifyPersonalTokenExpires(
                                    it.accessTokenExpirationTimestampMs,
                                )
                                Logger.d("Lyrics", "REQUEST spotifyPersonalToken: $spotifyPersonalToken")
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                    }
                }
                if (spotifyPersonalToken.isNotEmpty() && spotifyClientToken.isNotEmpty()) {
                    val authToken = spotifyPersonalToken
                    Logger.d("Lyrics", "authToken: $authToken")
                    spotify
                        .searchSpotifyTrack(q, authToken, spotifyClientToken)
                        .onSuccess { searchResponse ->
                            val track =
                                if (duration != 0 && duration != null) {
                                    searchResponse.data?.searchV2?.tracksV2?.items?.find {
                                        abs(
                                            (
                                                (
                                                    (
                                                        it.item
                                                            ?.data
                                                            ?.duration
                                                            ?.totalMilliseconds ?: (0 / 1000)
                                                    ) - duration
                                                )
                                            ),
                                        ) < 1
                                    }
                                        ?: searchResponse.data
                                            ?.searchV2
                                            ?.tracksV2
                                            ?.items
                                            ?.firstOrNull()
                                } else {
                                    searchResponse.data
                                        ?.searchV2
                                        ?.tracksV2
                                        ?.items
                                        ?.firstOrNull()
                                }
                            Logger.d("Lyrics", "track: $track")
                            if (track != null) {
                                spotify
                                    .getSpotifyLyrics(track.item?.data?.id ?: "", spotifyPersonalToken, spotifyClientToken)
                                    .onSuccess {
                                        emit(Resource.Success<Lyrics>(it.toLyrics()))
                                    }.onFailure {
                                        it.printStackTrace()
                                        emit(Resource.Error<Lyrics>("Not found"))
                                    }
                            } else {
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                        }.onFailure { throwable ->
                            throwable.printStackTrace()
                            emit(Resource.Error<Lyrics>("Not found"))
                        }
                }
            }
        }

    override fun getLrclibLyricsData(
        sartist: String,
        strack: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>> =
        flow {
            Logger.w("Lyrics", "getLrclibLyricsData: $sartist $strack $duration")
            val qartist =
                sartist
                    .replace(
                        Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                        " ",
                    ).replace(
                        Regex("( và | & | и | e | und |, |和| dan)"),
                        " ",
                    ).replace("  ", " ")
                    .replace(Regex("([()])"), "")
                    .replace(".", " ")
            val qtrack =
                strack
                    .replace(
                        Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                        " ",
                    ).replace(
                        Regex("( và | & | и | e | und |, |和| dan)"),
                        " ",
                    ).replace("  ", " ")
                    .replace(Regex("([()])"), "")
                    .replace(".", " ")
            simpMusicLyrics
                .searchLrclibLyrics(qtrack, qartist, duration)
                .onSuccess {
                    it?.let { emit(Resource.Success<Lyrics>(it.toLyrics())) }
                }.onFailure {
                    it.printStackTrace()
                    emit(Resource.Error<Lyrics>("Not found"))
                }
        }.flowOn(Dispatchers.IO)

    override fun getAITranslationLyrics(
        lyrics: Lyrics,
        targetLanguage: String,
    ): Flow<Resource<Lyrics>> =
        flow {
            runCatching {
                Logger.w("AI Translation", "targetLanguage: $targetLanguage")
                aiClient
                    .translateLyrics(lyrics, targetLanguage)
                    .onSuccess { translatedLyrics ->
                        Logger.w("AI Translation", "translatedLyrics: $translatedLyrics")
                        emit(Resource.Success(translatedLyrics))
                    }.onFailure { throwable ->
                        Logger.e("AI Translation", "Error: ${throwable.message}")
                        emit(Resource.Error<Lyrics>("Translation failed"))
                    }
            }
        }.flowOn(Dispatchers.IO)

    // SimpMusic Lyrics
    private val simpMusicLyricsTag = "SimpMusicLyricsRepository"

    override fun getSimpMusicLyrics(videoId: String): Flow<Resource<Lyrics>> =
        flow {
            simpMusicLyrics
                .getLyrics(videoId)
                .onSuccess { lyrics ->
                    Logger.d(simpMusicLyricsTag, "Lyrics found: $lyrics")
                    val result = lyrics.firstOrNull()
                    if (result == null) {
                        Logger.w(simpMusicLyricsTag, "No lyrics found for videoId: $videoId")
                        emit(Resource.Error<Lyrics>("No lyrics found"))
                        return@onSuccess
                    }
                    val appLyrics =
                        result.toLyrics()?.copy(
                            simpMusicLyricsId = result.id,
                        )
                    if (appLyrics == null) {
                        Logger.w(simpMusicLyricsTag, "Failed to convert lyrics for videoId: $videoId")
                        emit(Resource.Error<Lyrics>("Failed to convert lyrics"))
                        return@onSuccess
                    }
                    emit(
                        Resource.Success<Lyrics>(
                            appLyrics,
                        ),
                    )
                }.onFailure {
                    Logger.e(simpMusicLyricsTag, "Get Lyrics Error: ${it.message}")
                    emit(Resource.Error<Lyrics>(it.message ?: "Failed to get lyrics"))
                }
        }.flowOn(Dispatchers.IO)

    override fun getSimpMusicTranslatedLyrics(
        videoId: String,
        language: String,
    ): Flow<Resource<Lyrics>> =
        flow {
            simpMusicLyrics
                .getTranslatedLyrics(videoId, language)
                .onSuccess { lyrics ->
                    Logger.d(simpMusicLyricsTag, "Translated Lyrics found: ${lyrics.toLyrics()}")
                    emit(
                        Resource.Success<Lyrics>(
                            lyrics
                                .toLyrics()
                                .copy(
                                    simpMusicLyricsId = lyrics.id,
                                ),
                        ),
                    )
                }.onFailure {
                    Logger.e(simpMusicLyricsTag, "Get Translated Lyrics Error: ${it.message}")
                    emit(Resource.Error<Lyrics>(it.message ?: "Failed to get translated lyrics"))
                }
        }.flowOn(Dispatchers.IO)

    override fun voteSimpMusicTranslatedLyrics(
        translatedLyricsId: String,
        upvote: Boolean,
    ): Flow<Resource<String>> =
        flow {
            simpMusicLyrics
                .voteTranslatedLyrics(translatedLyricsId, upvote)
                .onSuccess {
                    Logger.d(simpMusicLyricsTag, "Vote Translated Lyrics Success: $it")
                    emit(Resource.Success(it.id))
                }.onFailure {
                    Logger.e(simpMusicLyricsTag, "Vote Translated Lyrics Error: ${it.message}")
                    emit(Resource.Error<String>(it.message ?: "Failed to vote translated lyrics"))
                }
        }.flowOn(Dispatchers.IO)

    override fun insertSimpMusicLyrics(
        dataStoreManager: DataStoreManager,
        track: Track,
        duration: Int,
        lyrics: Lyrics,
    ): Flow<Resource<String>> =
        flow {
            if (lyrics.lines.isNullOrEmpty()) {
                emit(
                    Resource.Error<String>("Lyrics are empty"),
                )
                return@flow
            }
            val (contributorName, contributorEmail) = dataStoreManager.contributorName.first() to dataStoreManager.contributorEmail.first()
            simpMusicLyrics
                .insertLyrics(
                    LyricsBody(
                        videoId = track.videoId,
                        songTitle = track.title,
                        artistName = track.artists?.toListName()?.connectArtists() ?: "",
                        albumName = track.album?.name ?: "",
                        durationSeconds = duration,
                        plainLyric = lyrics.toPlainLrcString() ?: "",
                        syncedLyrics = lyrics.toSyncedLrcString(),
                        richSyncLyrics = "",
                        contributor = contributorName,
                        contributorEmail = contributorEmail,
                    ),
                ).onSuccess {
                    Logger.d(simpMusicLyricsTag, "Inserted Lyrics: $it")
                    emit(Resource.Success(it.id))
                }.onFailure {
                    Logger.e(simpMusicLyricsTag, "Insert Lyrics Error: ${it.message}")
                    emit(Resource.Error<String>(it.message ?: "Failed to insert lyrics"))
                }
        }.flowOn(Dispatchers.IO)

    override fun insertSimpMusicTranslatedLyrics(
        dataStoreManager: DataStoreManager,
        track: Track,
        translatedLyrics: Lyrics,
        language: String,
    ): Flow<Resource<String>> =
        flow {
            val syncedLyrics = translatedLyrics.toSyncedLrcString()
            if (translatedLyrics.lines.isNullOrEmpty() || syncedLyrics == null || language.length != 2) {
                emit(
                    Resource.Error<String>("Lyrics are empty"),
                )
                return@flow
            }
            val (contributorName, contributorEmail) = dataStoreManager.contributorName.first() to dataStoreManager.contributorEmail.first()
            simpMusicLyrics
                .insertTranslatedLyrics(
                    TranslatedLyricsBody(
                        videoId = track.videoId,
                        translatedLyric = syncedLyrics,
                        language = language,
                        contributor = contributorName,
                        contributorEmail = contributorEmail,
                    ),
                ).onSuccess {
                    Logger.d(simpMusicLyricsTag, "Inserted Translated Lyrics: $it")
                    emit(Resource.Success(it.id))
                }.onFailure {
                    Logger.e(simpMusicLyricsTag, "Insert Translated Lyrics Error: ${it.message}")
                    emit(Resource.Error<String>(it.message ?: "Failed to insert translated lyrics"))
                }
        }.flowOn(Dispatchers.IO)
}