package com.maxrave.simpmusic.di

import android.content.Context
import androidx.room.Room
import com.maxrave.simpmusic.data.db.DatabaseDao
import com.maxrave.simpmusic.data.db.MusicDatabase
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
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase = Room.databaseBuilder(context, MusicDatabase::class.java, "Music Database").build()

    @Provides
    @Singleton
    fun provideDatabaseDao(musicDatabase: MusicDatabase): DatabaseDao = musicDatabase.getDatabaseDao()

}