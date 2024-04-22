package com.maxrave.simpmusic.service.test.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.FollowedArtistSingleAndAlbum
import com.maxrave.simpmusic.data.db.entities.NotificationEntity
import com.maxrave.simpmusic.data.model.browse.artist.ResultAlbum
import com.maxrave.simpmusic.data.model.browse.artist.ResultSingle
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.symmetricDifference
import com.maxrave.simpmusic.viewModel.MoreAlbumsViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltWorker
class NotifyWork
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
    ) : CoroutineWorker(context, params) {
        @Inject
        lateinit var mainRepository: MainRepository

        private val mapOfNotification = arrayListOf<NotificationModel>()

        override suspend fun doWork(): Result {
            return withContext(Dispatchers.IO) {
                Log.w("NotifyWork", "doWork: ")
                val artistList: List<ArtistEntity> = mainRepository.getFollowedArtists().first()
                val listFollowedArtistSingleAndAlbum =
                    mainRepository.getAllFollowedArtistSingleAndAlbums().first() ?: listOf()
                Log.w("NotifyWork", "doWork: $artistList")
                Log.w("NotifyWork", "doWork: $listFollowedArtistSingleAndAlbum")
                artistList.forEach { art ->
                    combine(mainRepository.getAlbumMore("MPAD${art.channelId}", MoreAlbumsViewModel.ALBUM_PARAM), mainRepository.getAlbumMore("MPAD${art.channelId}", MoreAlbumsViewModel.SINGLE_PARAM)) {
                        album, single -> Pair(album, single)
                    }.first().let { pair ->
                        val albumItem = pair.first?.items?.firstOrNull()?.items
                        val singleItem = pair.second?.items?.firstOrNull()?.items
                        val savedAlbum = listFollowedArtistSingleAndAlbum.find { it.channelId == art.channelId }?.album
                        if (!savedAlbum.isNullOrEmpty() && !albumItem.isNullOrEmpty()) {
                            val differentAlbum =
                                albumItem.filter { ytItem ->
                                    (albumItem.map { item -> item.id } symmetricDifference (savedAlbum.map { it["browseId"] })).contains(ytItem.id)
                                }.filterIsInstance<AlbumItem>()
                            mapOfNotification.add(
                                NotificationModel(
                                    name = art.name,
                                    channelId = art.channelId,
                                    single = listOf(),
                                    album = differentAlbum,
                                ),
                            )
                        }
                        val savedSingle = listFollowedArtistSingleAndAlbum.find { it.channelId == art.channelId }?.single
                        if (!savedSingle.isNullOrEmpty() && !singleItem.isNullOrEmpty()) {
                            val differentSingle =
                                singleItem.filter { ytItem ->
                                    (singleItem.map { item -> item.id } symmetricDifference (savedSingle.map { it["browseId"] })).contains(ytItem.id)
                                }.filterIsInstance<AlbumItem>()
                            mapOfNotification.add(
                                NotificationModel(
                                    name = art.name,
                                    channelId = art.channelId,
                                    single = differentSingle,
                                    album = listOf(),
                                ),
                            )
                        }
                        mainRepository.insertFollowedArtistSingleAndAlbum(
                            FollowedArtistSingleAndAlbum(
                                channelId = art.channelId,
                                name = art.name,
                                single = singleItem.toMap(),
                                album = albumItem.toMap(),
                            ),
                        )
                    }
                }
                Log.w("NotifyWork", "doWork: $mapOfNotification")
                NotificationHandler.createNotificationChannel(applicationContext)
                mapOfNotification.forEach { noti ->
                    if (noti.album.isNotEmpty() || noti.single.isNotEmpty()) {
                        NotificationHandler.createReminderNotification(
                            applicationContext,
                            noti,
                        )
                        mainRepository.insertNotification(
                            NotificationEntity(
                                channelId = noti.channelId,
                                thumbnail = artistList.find { it.channelId == noti.channelId }?.thumbnails,
                                name = noti.name,
                                single = noti.single.toMap(),
                                album = noti.album.toMap(),
                                time = LocalDateTime.now(),
                            ),
                        )
                    }
                }
                Result.success()
            }
        }
    }

private fun List<Any>?.toMap(): List<Map<String, String>> {
    return when (this?.firstOrNull()) {
        is ResultSingle -> {
            this.map {
                val single = it as ResultSingle
                mapOf(
                    "browseId" to single.browseId,
                    "title" to single.title,
                    "thumbnails" to (single.thumbnails.lastOrNull()?.url ?: ""),
                )
            }
        }

        is ResultAlbum -> {
            this.map {
                val album = it as ResultAlbum
                mapOf(
                    "browseId" to album.browseId,
                    "title" to album.title,
                    "thumbnails" to (album.thumbnails.lastOrNull()?.url ?: ""),
                )
            }
        }

        is AlbumItem -> {
            this.map {
                val album = it as AlbumItem
                mapOf(
                    "browseId" to album.id,
                    "title" to album.title,
                    "thumbnails" to album.thumbnail,
                )
            }
        }
        else -> listOf()
    }
}

data class NotificationModel(
    val name: String,
    val channelId: String,
    val single: List<AlbumItem>,
    val album: List<AlbumItem>,
)
