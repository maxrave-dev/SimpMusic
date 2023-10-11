package com.maxrave.simpmusic.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.maxrave.simpmusic.common.DB_NAME
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.Converters
import com.maxrave.simpmusic.data.db.DatabaseDao
import com.maxrave.simpmusic.data.db.MusicDatabase
import com.maxrave.simpmusic.extension.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalServiceModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase = Room.databaseBuilder(context, MusicDatabase::class.java, DB_NAME)
        .addTypeConverter(Converters())
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