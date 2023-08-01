package com.maxrave.simpmusic.data.dataStore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.maxrave.simpmusic.common.QUALITY as COMMON_QUALITY
import com.maxrave.simpmusic.common.SETTINGS_FILENAME
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreManager @Inject constructor(@ApplicationContext appContext: Context) {
    private val Context.dataStore by preferencesDataStore(SETTINGS_FILENAME)

    private val settingsDataStore = appContext.dataStore

    val location: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[LOCATION] ?: "VN"
    }

    suspend fun setLocation(location: String) {
        settingsDataStore.edit { settings ->
            settings[LOCATION] = location
        }
    }

    val quality: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[QUALITY] ?: COMMON_QUALITY.items[0].toString()
    }

    suspend fun restore(isRestoring: Boolean) {
        settingsDataStore.edit { settings ->
            settings[IS_RESTORING_DATABASE] = if (isRestoring) TRUE else FALSE
        }
    }

    suspend fun setQuality(quality: String) {
        settingsDataStore.edit { settings ->
            settings[QUALITY] = quality
        }
    }

    val isRestoringDatabase: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[IS_RESTORING_DATABASE] ?: FALSE
    }

    companion object Settings {
        val LOCATION = stringPreferencesKey("location")
        val QUALITY = stringPreferencesKey("quality")
        val IS_RESTORING_DATABASE = stringPreferencesKey("is_restoring_database")
        val TRUE = "TRUE"
        val FALSE = "FALSE"
    }
}