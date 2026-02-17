package com.maxrave.data.repository

import android.graphics.Bitmap
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.data.db.LocalDataSource
import com.maxrave.data.extension.getFullDataFromDB
import com.maxrave.data.mapping.toListTrack
import com.maxrave.data.mapping.toSongItemForDownload
import com.maxrave.data.mapping.toWatchEndpoint
import com.maxrave.domain.data.entities.QueueEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.SongInfoEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.download.DownloadProgress
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.response.LikeStatus
import com.maxrave.kotlinytmusicscraper.pages.NextPage
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistContinuation
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

private const val TAG = "SongRepositoryImpl"

internal class SongRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : SongRepository {
    override fun getAllSongs(limit: Int): Flow<List<SongEntity>> =
        flow {
            emit(localDataSource.getAllSongs(limit))
        }.flowOn(Dispatchers.IO)

    override suspend fun setInLibrary(
        videoId: String,
        inLibrary: LocalDateTime,
    ) = withContext(Dispatchers.IO) { localDataSource.setInLibrary(videoId, inLibrary) }

    override fun getSongsByListVideoId(listVideoId: List<String>): Flow<List<SongEntity>> =
        flow {
            emit(
                localDataSource.getSongByListVideoIdFull(listVideoId),
            )
        }.flowOn(Dispatchers.IO)

    override fun getDownloadedSongs(): Flow<List<SongEntity>?> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getDownloadedSongs(limit, offset)
                },
            )
        }.flowOn(Dispatchers.IO)

    override fun getDownloadingSongs(): Flow<List<SongEntity>?> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getDownloadingSongs(limit, offset)
                },
            )
        }.flowOn(Dispatchers.IO)

    override fun getPreparingSongs(): Flow<List<SongEntity>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getPreparingSongs(limit, offset)
                },
            )
        }.flowOn(Dispatchers.IO)

    override fun getDownloadedVideoIdListFromListVideoIdAsFlow(listVideoId: List<String>) =
        localDataSource.getDownloadedVideoIdListFromListVideoIdAsFlow(listVideoId)

    override fun getLikedSongs(): Flow<List<SongEntity>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getLikedSongs(limit, offset)
                },
            )
        }.flowOn(Dispatchers.IO)

    override fun getCanvasSong(max: Int): Flow<List<SongEntity>> =
        flow {
            emit(localDataSource.getCanvasSong(max))
        }.flowOn(Dispatchers.IO)

    override fun getSongById(id: String): Flow<SongEntity?> =
        flow {
            emit(localDataSource.getSong(id))
        }.flowOn(Dispatchers.IO)

    override fun getSongAsFlow(id: String) = localDataSource.getSongAsFlow(id)

    override fun insertSong(songEntity: SongEntity): Flow<Long> = flow<Long> { emit(localDataSource.insertSong(songEntity)) }.flowOn(Dispatchers.IO)

    override fun updateThumbnailsSongEntity(
        thumbnail: String,
        videoId: String,
    ): Flow<Int> = flow { emit(localDataSource.updateThumbnailsSongEntity(thumbnail, videoId)) }.flowOn(Dispatchers.IO)

    override suspend fun updateListenCount(videoId: String) =
        withContext(Dispatchers.IO) {
            localDataSource.updateListenCount(videoId)
        }

    override suspend fun resetTotalPlayTime(videoId: String) =
        withContext(Dispatchers.IO) {
            localDataSource.resetTotalPlayTime(videoId)
        }

    override suspend fun updateLikeStatus(
        videoId: String,
        likeStatus: Int,
    ) = withContext(Dispatchers.Main) { localDataSource.updateLiked(likeStatus, videoId) }

    override fun updateSongInLibrary(
        inLibrary: LocalDateTime,
        videoId: String,
    ): Flow<Int> = flow { emit(localDataSource.updateSongInLibrary(inLibrary, videoId)) }

    override suspend fun updateDurationSeconds(
        durationSeconds: Int,
        videoId: String,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updateDurationSeconds(
            durationSeconds,
            videoId,
        )
    }

    override fun getMostPlayedSongs(): Flow<List<SongEntity>> = localDataSource.getMostPlayedSongs()

    override suspend fun updateDownloadState(
        videoId: String,
        downloadState: Int,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updateDownloadState(
            downloadState,
            videoId,
        )
    }

    override suspend fun getRecentSong(
        limit: Int,
        offset: Int,
    ) = localDataSource.getRecentSongs(limit, offset)

    override suspend fun insertSongInfo(songInfo: SongInfoEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertSongInfo(songInfo)
        }

    override suspend fun getSongInfoEntity(videoId: String): Flow<SongInfoEntity?> =
        flow { emit(localDataSource.getSongInfo(videoId)) }.flowOn(Dispatchers.Main)

    override suspend fun recoverQueue(temp: List<Track>) {
        val queueEntity = QueueEntity(listTrack = temp)
        withContext(Dispatchers.IO) { localDataSource.recoverQueue(queueEntity) }
    }

    override suspend fun removeQueue() {
        withContext(Dispatchers.IO) { localDataSource.deleteQueue() }
    }

    override suspend fun getSavedQueue(): Flow<List<QueueEntity>?> =
        flow {
            emit(localDataSource.getQueue())
        }.flowOn(Dispatchers.IO)

    override fun getContinueTrack(
        playlistId: String,
        continuation: String,
        fromPlaylist: Boolean,
    ): Flow<Pair<ArrayList<Track>?, String?>> =
        flow {
            runCatching {
                var newContinuation: String? = null
                Logger.d(TAG, "getContinueTrack -> playlistId: $playlistId")
                Logger.d(TAG, "getContinueTrack -> continuation: $continuation")
                if (!fromPlaylist) {
                    youTube
                        .next(
                            if (playlistId.startsWith("RRDAMVM")) {
                                WatchEndpoint(videoId = playlistId.removePrefix("RRDAMVM"))
                            } else {
                                WatchEndpoint(playlistId = playlistId)
                            },
                            continuation = continuation,
                        ).onSuccess { next ->
                            val data: ArrayList<SongItem> = arrayListOf()
                            data.addAll(next.items)
                            newContinuation = next.continuation
                            emit(Pair(data.toListTrack(), newContinuation))
                        }.onFailure { exception ->
                            exception.printStackTrace()
                            emit(Pair(null, null))
                        }
                } else {
                    youTube
                        .customQuery(
                            browseId = null,
                            continuation = continuation,
                            setLogin = true,
                        ).onSuccess { values ->
                            Logger.d(TAG, "getPlaylistData -> continue: $continuation")
                            Logger.d(TAG, "getPlaylistData -> values: ${values.onResponseReceivedActions}")
                            val dataMore: List<SongItem> =
                                values.onResponseReceivedActions
                                    ?.firstOrNull()
                                    ?.appendContinuationItemsAction
                                    ?.continuationItems
                                    ?.apply {
                                        Logger.w(TAG, "getContinueTrack -> dataMore: ${this.size}")
                                    }?.mapNotNull {
                                        NextPage.fromMusicResponsiveListItemRenderer(
                                            it.musicResponsiveListItemRenderer ?: return@mapNotNull null,
                                        )
                                    } ?: emptyList()
                            newContinuation =
                                values.getPlaylistContinuation()
                            emit(
                                Pair<ArrayList<Track>?, String?>(
                                    dataMore.toListTrack(),
                                    newContinuation,
                                ),
                            )
                        }.onFailure {
                            Logger.e(TAG, "getContinueTrack -> Error: ${it.message}")
                            emit(Pair(null, null))
                        }
                }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSongInfo(videoId: String): Flow<SongInfoEntity?> =
        flow {
            runCatching {
                val id =
                    if (videoId.contains(MERGING_DATA_TYPE.VIDEO)) {
                        videoId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                    } else {
                        videoId
                    }
                youTube
                    .getSongInfo(id)
                    .onSuccess { songInfo ->
                        val song =
                            SongInfoEntity(
                                videoId = songInfo.videoId,
                                author = songInfo.author,
                                authorId = songInfo.authorId,
                                authorThumbnail = songInfo.authorThumbnail,
                                description = songInfo.description,
                                uploadDate = songInfo.uploadDate,
                                subscribers = songInfo.subscribers,
                                viewCount = songInfo.viewCount,
                                like = songInfo.like,
                                dislike = songInfo.dislike,
                            )
                        emit(song)
                        insertSongInfo(
                            song,
                        )
                    }.onFailure {
                        it.printStackTrace()
                        emit(getSongInfoEntity(videoId).lastOrNull())
                    }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun getLikeStatus(videoId: String): Flow<Boolean> =
        flow {
            runCatching {
                youTube
                    .getLikedInfo(videoId)
                    .onSuccess {
                        if (it == LikeStatus.LIKE) emit(true) else emit(false)
                    }.onFailure {
                        it.printStackTrace()
                        emit(false)
                    }
            }
        }

    override suspend fun addToYouTubeLiked(mediaId: String?): Flow<Int> =
        flow {
            if (mediaId != null) {
                runCatching {
                    youTube
                        .addToLiked(mediaId)
                        .onSuccess {
                            Logger.d(TAG, "Liked -> Success: $it")
                            emit(it)
                        }.onFailure {
                            it.printStackTrace()
                            emit(0)
                        }
                }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun removeFromYouTubeLiked(mediaId: String?): Flow<Int> =
        flow {
            if (mediaId != null) {
                runCatching {
                    youTube
                        .removeFromLiked(mediaId)
                        .onSuccess {
                            Logger.d(TAG, "Liked -> Success: $it")
                            emit(it)
                        }.onFailure {
                            it.printStackTrace()
                            emit(0)
                        }
                }
            }
        }.flowOn(Dispatchers.IO)

    override fun downloadToFile(
        track: Track,
        path: String,
        bitmap: Bitmap,
        videoId: String,
        isVideo: Boolean,
    ): Flow<DownloadProgress> =
        youTube
            .download(track.toSongItemForDownload(), path, bitmap, videoId, isVideo)
            .map {
                DownloadProgress(
                    audioDownloadProgress = it.audioDownloadProgress,
                    videoDownloadProgress = it.videoDownloadProgress,
                    downloadSpeed = it.downloadSpeed,
                    errorMessage = it.errorMessage,
                    isMerging = it.isMerging,
                    isError = it.isError,
                    isDone = it.isDone,
                )
            }

    override fun getRelatedData(videoId: String): Flow<Resource<Pair<List<Track>, String?>>> =
        flow {
            runCatching {
                youTube
                    .next(WatchEndpoint(videoId = videoId))
                    .onSuccess { next ->
                        val data: ArrayList<SongItem> = arrayListOf()
                        data.addAll(next.items.filter { it.id != videoId }.toSet())
                        val nextContinuation = next.continuation
                        emit(Resource.Success<Pair<List<Track>, String?>>(Pair(data.toListTrack().toList(), nextContinuation)))
                    }.onFailure { exception ->
                        exception.printStackTrace()
                        emit(Resource.Error<Pair<List<Track>, String?>>(exception.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getRadioFromEndpoint(endpoint: YouTubeWatchEndpoint): Flow<Resource<Pair<List<Track>, String?>>> =
        flow {
            runCatching {
                youTube
                    .next(endpoint.toWatchEndpoint())
                    .onSuccess { next ->
                        emit(Resource.Success(Pair(next.items.toListTrack(), next.continuation)))
                    }.onFailure {
                        it.printStackTrace()
                        emit(Resource.Error(it.message ?: it.localizedMessage ?: "Error"))
                    }
            }
        }
}