package com.maxrave.simpmusic.data.dataStore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.SETTINGS_FILENAME
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.maxrave.simpmusic.common.QUALITY as COMMON_QUALITY

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

    val language: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[stringPreferencesKey(SELECTED_LANGUAGE)] ?: SUPPORTED_LANGUAGE.codes.first()
    }

    fun getString(key: String): Flow<String?> {
        return settingsDataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)]
        }
    }

    suspend fun putString(key: String, value: String) {
        settingsDataStore.edit { settings ->
            settings[stringPreferencesKey(key)] = value
        }
    }

    val isRestoringDatabase: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[IS_RESTORING_DATABASE] ?: FALSE
    }
    val loggedIn: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[LOGGED_IN] ?: FALSE
    }

    val cookie: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[COOKIE] ?: ""
    }
    suspend fun setCookie(cookie: String) {
        settingsDataStore.edit { settings ->
            settings[COOKIE] = cookie
        }
    }

    suspend fun setLoggedIn(logged: Boolean) {
        if (logged) {
            settingsDataStore.edit { settings ->
                settings[LOGGED_IN] = TRUE
            }
        } else {
            settingsDataStore.edit { settings ->
                settings[LOGGED_IN] = FALSE
            }
        }
    }

    val normalizeVolume: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[NORMALIZE_VOLUME] ?: FALSE
    }

    suspend fun setNormalizeVolume(normalize: Boolean) {
        if (normalize) {
            settingsDataStore.edit { settings ->
                settings[NORMALIZE_VOLUME] = TRUE
            }
        } else {
            settingsDataStore.edit { settings ->
                settings[NORMALIZE_VOLUME] = FALSE
            }
        }
    }

    val pipedInstance: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[PIPED] ?: "watchapi.whatever.social"
    }

    suspend fun setPipedInstance(instance: String) {
        settingsDataStore.edit { settings ->
            settings[PIPED] = instance
        }
    }


    companion object Settings {
        val COOKIE = stringPreferencesKey("cookie")
        val LOGGED_IN = stringPreferencesKey("logged_in")
        val LOCATION = stringPreferencesKey("location")
        val QUALITY = stringPreferencesKey("quality")
        val NORMALIZE_VOLUME = stringPreferencesKey("normalize_volume")
        val IS_RESTORING_DATABASE = stringPreferencesKey("is_restoring_database")
        val PIPED = stringPreferencesKey("piped")
        val TRUE = "TRUE"
        val FALSE = "FALSE"
    }
}