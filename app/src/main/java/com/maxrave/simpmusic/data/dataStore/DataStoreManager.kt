package com.maxrave.simpmusic.data.dataStore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreManager @Inject constructor(@ApplicationContext appContext: Context) {
    private val Context.dataStore by preferencesDataStore("settings")

    private val settingsDataStore = appContext.dataStore

    val location: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[LOCATION] ?: "VN"
    }

    suspend fun setLocation(location: String) {
        settingsDataStore.edit { settings ->
            settings[LOCATION] = location
        }
    }

    companion object Settings {
        val LOCATION = stringPreferencesKey("location")
    }
}