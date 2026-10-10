package com.maxrave.simpmusic.service.test.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.FollowedArtistSingleAndAlbum
import com.maxrave.domain.data.entities.NotificationEntity
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.extension.now
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.isNewRelease
import com.maxrave.simpmusic.viewModel.MoreAlbumsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotifyWork(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val albumRepository: AlbumRepository by inject()
    private val artistRepository: ArtistRepository by inject()

    private val commonRepository: CommonRepository by inject()

    private val mapOfNotification = arrayListOf<NotificationModel>()

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            Logger.w("NotifyWork", "doWork: ")
            val artistList: List<ArtistEntity> = artistRepository.getFollowedArtists().lastOrNull() ?: listOf()
            val listFollowedArtistSingleAndAlbum =
                albumRepository.getAllFollowedArtistSingleAndAlbums().lastOrNull() ?: listOf()
            Logger.w("NotifyWork", "doWork: $artistList")
            Logger.w("NotifyWork", "doWork: $listFollowedArtistSingleAndAlbum")
            artistList.forEach { art ->
                combine(
                    albumRepository.getAlbumMore("MPAD${art.channelId}", MoreAlbumsViewModel.ALBUM_PARAM),
                    albumRepository.getAlbumMore("MPAD${art.channelId}", MoreAlbumsViewModel.SINGLE_PARAM),
                ) { album, single ->
                    Pair(album, single)
                }.first().let { pair ->
                    val albumItem =
                        pair.first
                            ?.second
                    val singleItem =
                        pair.second
                            ?.second
                    val saved = listFollowedArtistSingleAndAlbum.find { it.channelId == art.channelId }
                    // Everything ever seen for this artist, in BOTH grids. YouTube moves EPs between
                    // Albums and Singles, re-keys old releases and lists only the first 100, so
                    // comparing against the last fetch alone kept announcing old releases.
                    val known =
                        saved
                            ?.let { it.album + it.single }
                            .orEmpty()
                            .mapNotNull { it["browseId"] }
                            .toSet()
                    // The first check of an artist only records what already exists.
                    if (known.isNotEmpty()) {
                        mapOfNotification.add(
                            NotificationModel(
                                name = art.name,
                                channelId = art.channelId,
                                single = listOf(),
                                album = albumItem.orEmpty().filter { isNewRelease(it.browseId, it.year, known) },
                            ),
                        )
                        mapOfNotification.add(
                            NotificationModel(
                                name = art.name,
                                channelId = art.channelId,
                                single = singleItem.orEmpty().filter { isNewRelease(it.browseId, it.year, known) },
                                album = listOf(),
                            ),
                        )
                    }
                    // A union, never overwritten: a failed or partial fetch must not erase what was
                    // already seen, or those releases come back as "new" on the next run.
                    albumRepository.insertFollowedArtistSingleAndAlbum(
                        FollowedArtistSingleAndAlbum(
                            channelId = art.channelId,
                            name = art.name,
                            single = (saved?.single.orEmpty() + singleItem.toMap()).distinctBy { it["browseId"] },
                            album = (saved?.album.orEmpty() + albumItem.toMap()).distinctBy { it["browseId"] },
                        ),
                    )
                }
            }
            Logger.w("NotifyWork", "doWork: $mapOfNotification")
            NotificationHandler.createNotificationChannel(applicationContext)
            mapOfNotification.forEach { noti ->
                if (noti.album.isNotEmpty() || noti.single.isNotEmpty()) {
                    NotificationHandler.createReminderNotification(
                        applicationContext,
                        noti,
                    )
                    commonRepository.insertNotification(
                        NotificationEntity(
                            channelId = noti.channelId,
                            thumbnail = artistList.find { it.channelId == noti.channelId }?.thumbnails,
                            name = noti.name,
                            single = noti.single.toMap(),
                            album = noti.album.toMap(),
                            time = now(),
                        ),
                    )
                }
            }
            Result.success()
        }
}

private fun List<AlbumsResult>?.toMap(): List<Map<String, String>> =
    this?.map { single ->
        mapOf(
            "browseId" to single.browseId,
            "title" to single.title,
            "thumbnails" to (single.thumbnails.lastOrNull()?.url ?: ""),
        )
    } ?: emptyList()

data class NotificationModel(
    val name: String,
    val channelId: String,
    val single: List<AlbumsResult>,
    val album: List<AlbumsResult>,
)