package com.maxrave.simpmusic.di

import android.content.Context
import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.OnConflictStrategy
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.maxrave.simpmusic.common.DB_NAME
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.Converters
import com.maxrave.simpmusic.data.db.DatabaseDao
import com.maxrave.simpmusic.data.db.MusicDatabase
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.extension.dataStore
import com.maxrave.simpmusic.extension.toSQLiteQuery
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.lang.reflect.Type
import java.time.ZoneOffset
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalServiceModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase = Room.databaseBuilder(context, MusicDatabase::class.java, DB_NAME)
        .addTypeConverter(Converters())
        .addMigrations(object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val playlistSongMaps = mutableListOf<PairSongLocalPlaylist>()
            db.query("SELECT * FROM local_playlist".toSQLiteQuery()).use { cursor ->
                while (cursor.moveToNext()) {
                    val input = cursor.getString(8)
                    if (input != null) {
                        val listType: Type = object : TypeToken<ArrayList<String?>?>() {}.type
                        val tracks = Gson().fromJson<ArrayList<String?>?>(input, listType)
                        Log.w("MIGRATION_5_6", "tracks: $tracks")
                        tracks?.mapIndexed {index, track ->
                            if (track != null) {
                                playlistSongMaps.add(
                                    PairSongLocalPlaylist(
                                        playlistId = cursor.getLong(0),
                                        songId = track,
                                        position = index
                                    )
                                )
                            }
                        }
                    }
                }
            }
            db.execSQL("ALTER TABLE `format` ADD COLUMN `lengthSeconds` INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE `format` ADD COLUMN `youtubeCaptionsUrl` TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE `format` ADD COLUMN `cpn` TEXT DEFAULT NULL")
            db.execSQL("CREATE TABLE IF NOT EXISTS `pair_song_local_playlist` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, `songId` TEXT NOT NULL, `position` INTEGER NOT NULL, `inPlaylist` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `local_playlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`songId`) REFERENCES `song`(`videoId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pair_song_local_playlist_playlistId` ON `pair_song_local_playlist` (`playlistId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pair_song_local_playlist_songId` ON `pair_song_local_playlist` (`songId`)")
            playlistSongMaps.forEach { pair ->
                db.insert(table = "pair_song_local_playlist", conflictAlgorithm = OnConflictStrategy.IGNORE, values = contentValuesOf(
                    "playlistId" to pair.playlistId,
                    "songId" to pair.songId,
                    "position" to pair.position,
                    "inPlaylist" to pair.inPlaylist.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
                ))
            }
        }

    })
        .build()

    @Provides
    @Singleton
    fun provideDatabaseDao(musicDatabase: MusicDatabase): DatabaseDao = musicDatabase.getDatabaseDao()

    @Provides
    @Singleton
    fun provideDatastore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideDatastoreManager(settingsDataStore: DataStore<Preferences>): DataStoreManager = DataStoreManager(
        settingsDataStore
    )
}