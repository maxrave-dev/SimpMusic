package com.maxrave.simpmusic.di

import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.OnConflictStrategy
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.lyricsproviders.LyricsClient
import com.maxrave.simpmusic.common.DB_NAME
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.Converters
import com.maxrave.simpmusic.data.db.DatabaseDao
import com.maxrave.simpmusic.data.db.LocalDataSource
import com.maxrave.simpmusic.data.db.MusicDatabase
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.manager.LocalPlaylistManager
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.dataStore
import com.maxrave.simpmusic.extension.toSQLiteQuery
import com.maxrave.spotify.Spotify
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.simpmusic.aiservice.AiClient
import org.simpmusic.lyrics.SimpMusicLyricsClient
import java.time.ZoneOffset

val databaseModule =
    module {
        // Database
        single(createdAtStart = true) {
            val json =
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                }
            Room
                .databaseBuilder(androidContext(), MusicDatabase::class.java, DB_NAME)
                .addTypeConverter(Converters())
                .addMigrations(
                    object : Migration(5, 6) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            val playlistSongMaps = mutableListOf<PairSongLocalPlaylist>()
                            db.query("SELECT * FROM local_playlist".toSQLiteQuery()).use { cursor ->
                                while (cursor.moveToNext()) {
                                    val input = cursor.getString(8)
                                    if (input != null) {
                                        val tracks =
                                            json.decodeFromString<ArrayList<String?>?>(input)
                                        Log.w("MIGRATION_5_6", "tracks: $tracks")
                                        tracks?.mapIndexed { index, track ->
                                            if (track != null) {
                                                playlistSongMaps.add(
                                                    PairSongLocalPlaylist(
                                                        playlistId = cursor.getLong(0),
                                                        songId = track,
                                                        position = index,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            db.execSQL("ALTER TABLE `format` ADD COLUMN `lengthSeconds` INTEGER DEFAULT NULL")
                            db.execSQL("ALTER TABLE `format` ADD COLUMN `youtubeCaptionsUrl` TEXT DEFAULT NULL")
                            db.execSQL("ALTER TABLE `format` ADD COLUMN `cpn` TEXT DEFAULT NULL")
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `pair_song_local_playlist` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, `songId` TEXT NOT NULL, `position` INTEGER NOT NULL, `inPlaylist` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `local_playlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`songId`) REFERENCES `song`(`videoId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                            )
                            db.execSQL(
                                "CREATE INDEX IF NOT EXISTS `index_pair_song_local_playlist_playlistId` ON `pair_song_local_playlist` (`playlistId`)",
                            )
                            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pair_song_local_playlist_songId` ON `pair_song_local_playlist` (`songId`)")
                            playlistSongMaps.forEach { pair ->
                                db.insert(
                                    table = "pair_song_local_playlist",
                                    conflictAlgorithm = OnConflictStrategy.IGNORE,
                                    values =
                                        contentValuesOf(
                                            "playlistId" to pair.playlistId,
                                            "songId" to pair.songId,
                                            "position" to pair.position,
                                            "inPlaylist" to
                                                pair.inPlaylist
                                                    .atZone(ZoneOffset.UTC)
                                                    .toInstant()
                                                    .toEpochMilli(),
                                        ),
                                )
                            }
                        }
                    },
                    object : Migration(10, 11) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            val listYouTubeSyncedId = mutableListOf<Pair<String, List<String>>>() // Pair<youtubePlaylistId, listVideoId>
                            db
                                .query(
                                    "SELECT youtubePlaylistId, tracks FROM local_playlist WHERE synced_with_youtube_playlist = 1 AND youtubePlaylistId NOT NULL"
                                        .toSQLiteQuery(),
                                ).use { cursor ->
                                    while (cursor.moveToNext()) {
                                        val youtubePlaylistId = cursor.getString(0)
                                        val input = cursor.getString(1)
                                        val tracks =
                                            json.decodeFromString<ArrayList<String?>?>(input)
                                        listYouTubeSyncedId.add(Pair(youtubePlaylistId, tracks?.toMutableList()?.filterNotNull() ?: emptyList()))
                                    }
                                }
                            val setVideoIdList = mutableListOf<SetVideoIdEntity>()
                            db.query("SELECT * FROM set_video_id".toSQLiteQuery()).use { cursor ->
                                while (cursor.moveToNext()) {
                                    val videoId = cursor.getString(0)
                                    val setVideoId = cursor.getString(1)
                                    for (pair in listYouTubeSyncedId) {
                                        if (pair.second.contains(videoId)) {
                                            setVideoIdList.add(SetVideoIdEntity(videoId, setVideoId, pair.first))
                                            break
                                        }
                                    }
                                }
                            }
                            db.execSQL("DROP TABLE set_video_id")
                            db.execSQL(
                                "CREATE TABLE IF NOT EXISTS `set_video_id` (`videoId` TEXT NOT NULL, `setVideoId` TEXT, `youtubePlaylistId` TEXT NOT NULL, PRIMARY KEY(`videoId`, `youtubePlaylistId`))",
                            )
                            setVideoIdList.forEach { setVideoIdEntity ->
                                db.insert(
                                    table = "set_video_id",
                                    conflictAlgorithm = OnConflictStrategy.IGNORE,
                                    values =
                                        contentValuesOf(
                                            "videoId" to setVideoIdEntity.videoId,
                                            "setVideoId" to setVideoIdEntity.setVideoId,
                                            "youtubePlaylistId" to setVideoIdEntity.youtubePlaylistId,
                                        ),
                                )
                            }
                        }
                    },
                    object : Migration(12, 13) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE song ADD COLUMN canvasUrl TEXT")
                        }
                    },
                ).addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL(
                                "CREATE TRIGGER  IF NOT EXISTS on_delete_pair_song_local_playlist AFTER DELETE ON pair_song_local_playlist\n" +
                                    "FOR EACH ROW\n" +
                                    "BEGIN\n" +
                                    "    UPDATE pair_song_local_playlist\n" +
                                    "    SET position = position - 1\n" +
                                    "    WHERE playlistId = OLD.playlistId AND position > OLD.position;\n" +
                                    "END;",
                            )
                        }
                    },
                ).build()
        }
        // DatabaseDao
        single(createdAtStart = true) {
            get<MusicDatabase>().getDatabaseDao()
        }
        // LocalDataSource
        single(createdAtStart = true) {
            LocalDataSource(get<DatabaseDao>())
        }
        // Datastore
        single(createdAtStart = true) {
            androidContext().dataStore
        }
        // DatastoreManager
        single(createdAtStart = true) {
            DataStoreManager(get<DataStore<Preferences>>())
        }

        // Move YouTube from Singleton to Koin DI
        single(createdAtStart = true) {
            YouTube(androidContext())
        }

        single(createdAtStart = true) {
            Spotify()
        }

        single(createdAtStart = true) {
            LyricsClient(androidContext())
        }

        single(createdAtStart = true) {
            AiClient()
        }

        single(createdAtStart = true) {
            SimpMusicLyricsClient(
                androidContext(),
            )
        }

        // MainRepository
        single(createdAtStart = true) {
            MainRepository(
                get<LocalDataSource>(),
                get<DataStoreManager>(),
                get<YouTube>(),
                get<Spotify>(),
                get<LyricsClient>(),
                get<SimpMusicLyricsClient>(),
                get<AiClient>(),
                get<MusicDatabase>(),
                androidContext(),
            )
        }
        // List of managers

        single(createdAtStart = true) {
            LocalPlaylistManager(androidContext(), get<YouTube>())
        }
    }