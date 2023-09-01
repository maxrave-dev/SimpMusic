package com.maxrave.simpmusic.data.dataStore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
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
    val skipSilent: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[SKIP_SILENT] ?: FALSE
    }
    suspend fun setSkipSilent(skip: Boolean) {
        if (skip) {
            settingsDataStore.edit { settings ->
                settings[SKIP_SILENT] = TRUE
            }
        } else {
            settingsDataStore.edit { settings ->
                settings[SKIP_SILENT] = FALSE
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

    val saveStateOfPlayback: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[SAVE_STATE_OF_PLAYBACK] ?: FALSE
    }
    suspend fun setSaveStateOfPlayback(save: Boolean) {
        if (save) {
            settingsDataStore.edit { settings ->
                settings[SAVE_STATE_OF_PLAYBACK] = TRUE
            }
        } else {
            settingsDataStore.edit { settings ->
                settings[SAVE_STATE_OF_PLAYBACK] = FALSE
            }
        }
    }
    val shuffleKey: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[SHUFFLE_KEY] ?: FALSE
    }
    val repeatKey: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[REPEAT_KEY] ?: REPEAT_MODE_OFF
    }

    suspend fun recoverShuffleAndRepeatKey(shuffle: Boolean, repeat: Int) {
        if (shuffle) {
            settingsDataStore.edit { settings ->
                settings[SHUFFLE_KEY] = TRUE
            }
        }
        else {
            settingsDataStore.edit { settings ->
                settings[SHUFFLE_KEY] = FALSE
            }
        }
        settingsDataStore.edit { settings ->
            settings[REPEAT_KEY] = when (repeat) {
                Player.REPEAT_MODE_ONE -> REPEAT_ONE
                Player.REPEAT_MODE_ALL -> REPEAT_ALL
                Player.REPEAT_MODE_OFF -> REPEAT_MODE_OFF
                else -> REPEAT_MODE_OFF
            }
        }
    }

    val saveRecentSongAndQueue: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[SAVE_RECENT_SONG] ?: FALSE
    }
    suspend fun setSaveRecentSongAndQueue(save: Boolean) {
        if (save) {
            settingsDataStore.edit { settings ->
                settings[SAVE_RECENT_SONG] = TRUE
            }
        } else {
            settingsDataStore.edit { settings ->
                settings[SAVE_RECENT_SONG] = FALSE
            }
        }
    }
    val recentMediaId = settingsDataStore.data.map { preferences ->
        preferences[RECENT_SONG_MEDIA_ID_KEY] ?: ""
    }
    val recentPosition = settingsDataStore.data.map { preferences ->
        preferences[RECENT_SONG_POSITION_KEY] ?: "0"
    }
    suspend fun saveRecentSong (mediaId: String, position: Long) {
        settingsDataStore.edit { settings ->
            settings[RECENT_SONG_MEDIA_ID_KEY] = mediaId
            settings[RECENT_SONG_POSITION_KEY] = position.toString()
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
        val SKIP_SILENT = stringPreferencesKey("skip_silent")
        val SAVE_STATE_OF_PLAYBACK = stringPreferencesKey("save_state_of_playback")
        val SAVE_RECENT_SONG = stringPreferencesKey("save_recent_song")
        val RECENT_SONG_MEDIA_ID_KEY = stringPreferencesKey("recent_song_media_id")
        val RECENT_SONG_POSITION_KEY = stringPreferencesKey("recent_song_position")
        val SHUFFLE_KEY = stringPreferencesKey("shuffle_key")
        val REPEAT_KEY = stringPreferencesKey("repeat_key")
        val REPEAT_MODE_OFF = "REPEAT_MODE_OFF"
        val REPEAT_ONE = "REPEAT_ONE"
        val REPEAT_ALL = "REPEAT_ALL"
        val TRUE = "TRUE"
        val FALSE = "FALSE"
    }
}