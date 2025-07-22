package com.maxrave.simpmusic.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.EpisodeEntity
import com.maxrave.simpmusic.data.db.entities.FollowedArtistSingleAndAlbum
import com.maxrave.simpmusic.data.db.entities.GoogleAccountEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.LyricsEntity
import com.maxrave.simpmusic.data.db.entities.NewFormatEntity
import com.maxrave.simpmusic.data.db.entities.NotificationEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PodcastWithEpisodes
import com.maxrave.simpmusic.data.db.entities.PodcastsEntity
import com.maxrave.simpmusic.data.db.entities.QueueEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.db.entities.SongInfoEntity
import com.maxrave.simpmusic.data.db.entities.TranslatedLyricsEntity
import com.maxrave.simpmusic.data.type.PlaylistType
import com.maxrave.simpmusic.data.type.RecentlyType
import com.maxrave.simpmusic.extension.toSQLiteQuery
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface DatabaseDao {
    // Transaction request with multiple queries
    @Transaction
    suspend fun getAllRecentData(): List<RecentlyType> {
        val a = mutableListOf<RecentlyType>()
        a.addAll(getAllSongs())
        a.addAll(getAllArtists())
        a.addAll(getAllAlbums())
        a.addAll(getAllPlaylists())
        a.addAll(getAllPodcasts())
        val sortedList =
            a.sortedWith<RecentlyType>(
                Comparator { p0, p1 ->
                    val timeP0: LocalDateTime? =
                        when (p0) {
                            is SongEntity -> p0.inLibrary
                            is ArtistEntity -> p0.inLibrary
                            is AlbumEntity -> p0.inLibrary
                            is PlaylistEntity -> p0.inLibrary
                            is PodcastsEntity -> p0.inLibrary
                            else -> null
                        }
                    val timeP1: LocalDateTime? =
                        when (p1) {
                            is SongEntity -> p1.inLibrary
                            is ArtistEntity -> p1.inLibrary
                            is AlbumEntity -> p1.inLibrary
                            is PlaylistEntity -> p1.inLibrary
                            is PodcastsEntity -> p1.inLibrary
                            else -> null
                        }
                    if (timeP0 == null || timeP1 == null) {
                        return@Comparator if (timeP0 == null && timeP1 == null) {
                            0
                        } else if (timeP0 == null) {
                            -1
                        } else {
                            1
                        }
                    }
                    timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
                },
            )
        return sortedList.takeLast(20)
    }

    @Transaction
    suspend fun getAllDownloadedPlaylist(): List<PlaylistType> {
        val a = mutableListOf<PlaylistType>()
        a.addAll(getDownloadedAlbums())
        a.addAll(getDownloadedPlaylists())
        val sortedList =
            a.sortedWith<PlaylistType>(
                Comparator { p0, p1 ->
                    val timeP0: LocalDateTime? =
                        when (p0) {
                            is AlbumEntity -> p0.inLibrary
                            is PlaylistEntity -> p0.inLibrary
                            else -> null
                        }
                    val timeP1: LocalDateTime? =
                        when (p1) {
                            is AlbumEntity -> p1.inLibrary
                            is PlaylistEntity -> p1.inLibrary
                            else -> null
                        }
                    if (timeP0 == null || timeP1 == null) {
                        return@Comparator if (timeP0 == null && timeP1 == null) {
                            0
                        } else if (timeP0 == null) {
                            -1
                        } else {
                            1
                        }
                    }
                    timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
                },
            )
        return sortedList
    }

    // Get search history
    @Query("SELECT * FROM search_history")
    suspend fun getSearchHistory(): List<SearchHistory>

    @Query("DELETE FROM search_history")
    suspend fun deleteSearchHistory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(searchHistory: SearchHistory): Long

    // Song
    @Query("SELECT * FROM song ORDER BY inLibrary DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecentSongs(
        limit: Int,
        offset: Int,
    ): List<SongEntity>

    @Query("SELECT * FROM song")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT * FROM song WHERE liked = 1")
    fun getLikedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL")
    suspend fun getLibrarySongs(): List<SongEntity>

    @Query("SELECT * FROM song WHERE videoId = :videoId")
    suspend fun getSong(videoId: String): SongEntity?

    @Query("SELECT * FROM song WHERE videoId = :videoId")
    fun getSongAsFlow(videoId: String): Flow<SongEntity?>

    @Query("UPDATE song SET totalPlayTime = totalPlayTime + 1 WHERE videoId = :videoId")
    suspend fun updateTotalPlayTime(videoId: String)

    @Query("UPDATE song SET inLibrary = :inLibrary WHERE videoId = :videoId")
    suspend fun updateSongInLibrary(
        inLibrary: LocalDateTime,
        videoId: String,
    ): Int

    @Query("UPDATE song SET liked = :liked WHERE videoId = :videoId")
    suspend fun updateLiked(
        liked: Int,
        videoId: String,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: SongEntity): Long

    @Query("UPDATE song SET canvasUrl = :canvasUrl WHERE videoId = :videoId")
    suspend fun updateCanvasUrl(
        videoId: String,
        canvasUrl: String,
    )

    @Query("UPDATE song SET canvasThumbUrl = :canvasThumbUrl WHERE videoId = :videoId")
    suspend fun updateCanvasThumbUrl(
        videoId: String,
        canvasThumbUrl: String,
    )

    @Query("UPDATE song SET thumbnails = :thumbnails WHERE videoId = :videoId")
    suspend fun updateThumbnailsSongEntity(
        thumbnails: String,
        videoId: String,
    ): Int

    @Query("SELECT * FROM song WHERE totalPlayTime > 1 ORDER BY totalPlayTime DESC LIMIT 50")
    fun getMostPlayedSongs(): Flow<List<SongEntity>>

    @Query("UPDATE song SET downloadState = :downloadState WHERE videoId = :videoId")
    suspend fun updateDownloadState(
        downloadState: Int,
        videoId: String,
    )

    @Query("UPDATE song SET durationSeconds = :durationSeconds WHERE videoId = :videoId")
    suspend fun updateDurationSeconds(
        durationSeconds: Int,
        videoId: String,
    )

    @Query("SELECT * FROM song WHERE downloadState = 3")
    suspend fun getDownloadedSongs(): List<SongEntity>

    @Query("SELECT * FROM song WHERE downloadState = 3 LIMIT 1000 OFFSET :offset")
    fun getDownloadedSongsAsFlow(offset: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE downloadState = 1 OR downloadState = 2")
    suspend fun getDownloadingSongs(): List<SongEntity>

    @Query("SELECT * FROM song WHERE videoId IN (:primaryKeyList) LIMIT 1000")
    suspend fun getSongByListVideoIdFull(primaryKeyList: List<String>): List<SongEntity>

    @Query("SELECT * FROM song WHERE videoId IN (:primaryKeyList) LIMIT 50 OFFSET :offset")
    suspend fun getSongByListVideoId(
        primaryKeyList: List<String>,
        offset: Int,
    ): List<SongEntity>

    @Query("SELECT * FROM song WHERE canvasThumbUrl IS NOT NULL ORDER BY totalPlayTime DESC LIMIT :max")
    suspend fun getCanvasSong(max: Int): List<SongEntity>

    @Query("SELECT videoId FROM song WHERE videoId IN (:primaryKeyList) AND downloadState = 3")
    fun getDownloadedVideoIdByListVideoId(primaryKeyList: List<String>): Flow<List<String>>

    // Artist
    @Query("SELECT * FROM artist")
    suspend fun getAllArtists(): List<ArtistEntity>

    @Query("SELECT * FROM artist WHERE channelId = :channelId")
    suspend fun getArtist(channelId: String): ArtistEntity

    @Query("SELECT * FROM artist WHERE followed = 1")
    fun getFollowedArtists(): Flow<List<ArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtist(artist: ArtistEntity)

    @Query("UPDATE artist SET thumbnails = :thumbnails WHERE channelId = :channelId")
    suspend fun updateArtistImage(
        channelId: String,
        thumbnails: String,
    )

    @Query("UPDATE artist SET followed = :followed WHERE channelId = :channelId")
    suspend fun updateFollowed(
        followed: Int,
        channelId: String,
    )

    @Query("UPDATE artist SET inLibrary = :inLibrary WHERE channelId = :channelId")
    suspend fun updateArtistInLibrary(
        inLibrary: LocalDateTime,
        channelId: String,
    )

    // Album
    @Query("SELECT * FROM album")
    suspend fun getAllAlbums(): List<AlbumEntity>

    @Query("SELECT * FROM album WHERE browseId = :browseId")
    suspend fun getAlbum(browseId: String): AlbumEntity?

    @Query("SELECT * FROM album WHERE browseId = :browseId")
    fun getAlbumAsFlow(browseId: String): Flow<AlbumEntity?>

    @Query("SELECT * FROM album WHERE liked = 1")
    suspend fun getLikedAlbums(): List<AlbumEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Query("UPDATE album SET liked = :liked WHERE browseId = :browseId")
    suspend fun updateAlbumLiked(
        liked: Int,
        browseId: String,
    )

    @Query("UPDATE album SET inLibrary = :inLibrary WHERE browseId = :browseId")
    suspend fun updateAlbumInLibrary(
        inLibrary: LocalDateTime,
        browseId: String,
    )

    @Query("UPDATE album SET downloadState = :downloadState WHERE browseId = :browseId")
    suspend fun updateAlbumDownloadState(
        downloadState: Int,
        browseId: String,
    )

    @Query("SELECT * FROM album WHERE downloadState = 3")
    suspend fun getDownloadedAlbums(): List<AlbumEntity>

    // Playlist
    @Query("SELECT * FROM playlist")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    suspend fun getPlaylist(playlistId: String): PlaylistEntity?

    @Query("SELECT * FROM playlist WHERE liked = 1")
    suspend fun getLikedPlaylists(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAndReplacePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRadioPlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlist SET liked = :liked WHERE id = :playlistId")
    suspend fun updatePlaylistLiked(
        liked: Int,
        playlistId: String,
    )

    @Query("UPDATE playlist SET inLibrary = :inLibrary WHERE id = :playlistId")
    suspend fun updatePlaylistInLibrary(
        inLibrary: LocalDateTime,
        playlistId: String,
    )

    @Query("UPDATE playlist SET downloadState = :downloadState WHERE id = :playlistId")
    suspend fun updatePlaylistDownloadState(
        downloadState: Int,
        playlistId: String,
    )

    @Query("SELECT * FROM playlist WHERE downloadState = 3")
    suspend fun getDownloadedPlaylists(): List<PlaylistEntity>

    // Local Playlist
    @Query("SELECT * FROM local_playlist")
    suspend fun getAllLocalPlaylists(): List<LocalPlaylistEntity>

    @Query("SELECT * FROM local_playlist WHERE id = :id")
    suspend fun getLocalPlaylist(id: Long): LocalPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocalPlaylist(localPlaylist: LocalPlaylistEntity)

    @Query("DELETE FROM local_playlist WHERE id = :id")
    suspend fun deleteLocalPlaylist(id: Long)

    @Query("UPDATE local_playlist SET title = :title WHERE id = :id")
    suspend fun updateLocalPlaylistTitle(
        title: String,
        id: Long,
    )

    @Query("UPDATE local_playlist SET tracks = :tracks WHERE id = :id")
    suspend fun updateLocalPlaylistTracks(
        tracks: List<String>,
        id: Long,
    )

    @Query("UPDATE local_playlist SET thumbnail = :thumbnail WHERE id = :id")
    suspend fun updateLocalPlaylistThumbnail(
        thumbnail: String,
        id: Long,
    )

    @Query("UPDATE local_playlist SET inLibrary = :inLibrary WHERE id = :id")
    suspend fun updateLocalPlaylistInLibrary(
        inLibrary: LocalDateTime,
        id: Long,
    )

    @Query("UPDATE local_playlist SET downloadState = :downloadState WHERE id = :id")
    suspend fun updateLocalPlaylistDownloadState(
        downloadState: Int,
        id: Long,
    )

    @Query("SELECT * FROM local_playlist WHERE downloadState = 3")
    suspend fun getDownloadedLocalPlaylists(): List<LocalPlaylistEntity>

    @Query("UPDATE local_playlist SET youtubePlaylistId = :youtubePlaylistId WHERE id = :id")
    suspend fun updateLocalPlaylistYouTubePlaylistId(
        id: Long,
        youtubePlaylistId: String?,
    )

    @Query("UPDATE local_playlist SET youtube_sync_state = :state WHERE id = :id")
    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(
        id: Long,
        state: Int,
    )

    @Query("UPDATE local_playlist SET youtube_sync_state = 0, youtubePlaylistId = NULL WHERE id = :id")
    suspend fun unsyncLocalPlaylist(id: Long)

    @Query("SELECT downloadState FROM local_playlist WHERE id = :id")
    fun getDownloadStateFlowOfLocalPlaylist(id: Long): Flow<Int>

    @Query("SELECT tracks FROM local_playlist WHERE id = :id")
    fun getListTracksFlowOfLocalPlaylist(id: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("SELECT * FROM lyrics WHERE videoId = :videoId")
    suspend fun getLyrics(videoId: String): LyricsEntity?

    @RawQuery
    fun raw(supportSQLiteQuery: SupportSQLiteQuery): Int

    fun checkpoint() {
        raw("pragma wal_checkpoint(full)".toSQLiteQuery())
    }

    @Query("SELECT * FROM song WHERE downloadState = 1")
    suspend fun getPreparingSongs(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewFormat(format: NewFormatEntity)

    @Update
    suspend fun updateNewFormat(format: NewFormatEntity)

    @Query("SELECT * FROM new_format WHERE videoId = :videoId")
    suspend fun getNewFormat(videoId: String): NewFormatEntity?

    @Query("SELECT * FROM new_format WHERE videoId = :videoId")
    fun getNewFormatAsFlow(videoId: String): Flow<NewFormatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongInfo(songInfo: SongInfoEntity)

    @Query("SELECT * FROM song_info WHERE videoId = :videoId")
    suspend fun getSongInfo(videoId: String): SongInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recoverQueue(queue: QueueEntity)

    @Query("DELETE FROM queue")
    suspend fun deleteQueue()

    @Query("SELECT * FROM queue")
    suspend fun getQueue(): List<QueueEntity>

    @Query("SELECT * FROM local_playlist WHERE youtubePlaylistId = :youtubePlaylistId")
    suspend fun getLocalPlaylistByYoutubePlaylistId(youtubePlaylistId: String): LocalPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetVideoId(setVideoIdEntity: SetVideoIdEntity)

    @Query("SELECT * FROM set_video_id WHERE videoId = :videoId")
    suspend fun getSetVideoId(videoId: String): SetVideoIdEntity?

    // PairSongLocalPlaylist
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist)

    @Query("SELECT * FROM pair_song_local_playlist WHERE playlistId = :playlistId")
    suspend fun getPlaylistPairSong(playlistId: Long): List<PairSongLocalPlaylist>

    @Query("SELECT * FROM pair_song_local_playlist WHERE playlistId = :playlistId AND position in (:positionList)")
    suspend fun getPlaylistPairSongByListPosition(
        playlistId: Long,
        positionList: List<Int>,
    ): List<PairSongLocalPlaylist>

    @Query(
        "SELECT * FROM pair_song_local_playlist WHERE playlistId = :playlistId ORDER BY position " +
            "ASC LIMIT 50 OFFSET :offset",
    )
    suspend fun getPlaylistPairSongByOffsetAsc(
        playlistId: Long,
        offset: Int,
    ): List<PairSongLocalPlaylist>

    @Query(
        "SELECT * FROM pair_song_local_playlist WHERE playlistId = :playlistId AND position >= :offset ORDER BY position " +
            "LIMIT 50",
    )
    suspend fun getPlaylistPairSongByOffsetDesc(
        playlistId: Long,
        offset: Int,
    ): List<PairSongLocalPlaylist>

    @Query(
        "SELECT * FROM pair_song_local_playlist WHERE playlistId = :playlistId AND position >= :from AND position < :to ORDER BY position " +
            "LIMIT 50",
    )
    suspend fun getPlaylistPairSongByFromToDesc(
        playlistId: Long,
        from: Int,
        to: Int,
    ): List<PairSongLocalPlaylist>

    @Query(
        "SELECT p.* FROM pair_song_local_playlist p JOIN song s ON p.songId = s.videoId WHERE" +
            " p.playlistId = :playlistId ORDER BY s.title ASC LIMIT 50 OFFSET :offset",
    )
    suspend fun getPlaylistPairSongByTitle(
        playlistId: Long,
        offset: Int,
    ): List<PairSongLocalPlaylist>

    @Query(
        "DELETE FROM pair_song_local_playlist WHERE songId = :videoId AND playlistId = :playlistId",
    )
    suspend fun deletePairSongLocalPlaylist(
        playlistId: Long,
        videoId: String,
    )

    // GoogleAccountEntity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoogleAccount(googleAccountEntity: GoogleAccountEntity): Long

    @Query("SELECT * FROM googleaccountentity")
    suspend fun getAllGoogleAccount(): List<GoogleAccountEntity>

    @Query("SELECT * FROM googleaccountentity WHERE isUsed = 1")
    suspend fun getUsedGoogleAccount(): GoogleAccountEntity?

    @Query("UPDATE googleaccountentity SET isUsed = :isUsed WHERE email = :email")
    suspend fun updateGoogleAccountUsed(
        isUsed: Boolean,
        email: String,
    ): Int

    @Query("DELETE FROM googleaccountentity WHERE email = :email")
    suspend fun deleteGoogleAccount(email: String)

    @Query("UPDATE song SET inLibrary = :inLibrary WHERE videoId = :videoId")
    suspend fun setInLibrary(
        videoId: String,
        inLibrary: LocalDateTime,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum: FollowedArtistSingleAndAlbum)

    @Query("SELECT * FROM followed_artist_single_and_album WHERE channelId = :channelId")
    suspend fun getFollowedArtistSingleAndAlbum(channelId: String): FollowedArtistSingleAndAlbum?

    @Query("DELETE FROM followed_artist_single_and_album WHERE channelId = :channelId")
    suspend fun deleteFollowedArtistSingleAndAlbum(channelId: String)

    @Query("SELECT * FROM followed_artist_single_and_album")
    suspend fun getAllFollowedArtistSingleAndAlbum(): List<FollowedArtistSingleAndAlbum>

    @Insert
    suspend fun insertNotification(notificationEntity: NotificationEntity)

    @Query("SELECT * FROM notification")
    suspend fun getAllNotification(): List<NotificationEntity>

    @Query("DELETE FROM notification WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslatedLyrics(translatedLyricsEntity: TranslatedLyricsEntity)

    @Query("SELECT * FROM translated_lyrics WHERE videoId = :videoId AND language = :language")
    suspend fun getTranslatedLyrics(
        videoId: String,
        language: String = "en",
    ): TranslatedLyricsEntity?

    @Query("DELETE FROM translated_lyrics WHERE videoId = :videoId AND language = :language")
    suspend fun removeTranslatedLyrics(
        videoId: String,
        language: String,
    )

    // Insert methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcast(podcast: PodcastsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>): List<Long>

    // Get methods
    @Transaction
    @Query("SELECT * FROM podcast_table WHERE podcastId = :podcastId")
    suspend fun getPodcastWithEpisodes(podcastId: String): PodcastWithEpisodes?

    @Transaction
    @Query("SELECT * FROM podcast_table")
    suspend fun getAllPodcastWithEpisodes(): List<PodcastWithEpisodes>

    @Query("SELECT * FROM podcast_table")
    suspend fun getAllPodcasts(): List<PodcastsEntity>

    @Query("SELECT * FROM podcast_table WHERE podcastId = :podcastId")
    suspend fun getPodcast(podcastId: String): PodcastsEntity?

    @Query("SELECT * FROM podcast_episode_table WHERE podcastId = :podcastId")
    suspend fun getPodcastEpisodes(podcastId: String): List<EpisodeEntity>

    @Query("SELECT * FROM podcast_episode_table WHERE videoId = :videoId")
    suspend fun getEpisode(videoId: String): EpisodeEntity?

    @Query("SELECT * FROM podcast_table WHERE isFavorite = 1")
    suspend fun getFavoritePodcasts(): List<PodcastsEntity>

    @Query("UPDATE podcast_table SET inLibrary = :inLibrary WHERE podcastId = :id")
    suspend fun updatePodcastInLibrary(
        id: String,
        inLibrary: LocalDateTime,
    )

    // Delete methods
    @Query("DELETE FROM podcast_table WHERE podcastId = :podcastId")
    suspend fun deletePodcast(podcastId: String)
}