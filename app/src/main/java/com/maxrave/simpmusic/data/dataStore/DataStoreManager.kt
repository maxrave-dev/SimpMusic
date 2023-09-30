package com.maxrave.simpmusic.data.dataStore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.Player
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.SPONSOR_BLOCK
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.maxrave.simpmusic.common.QUALITY as COMMON_QUALITY

class DataStoreManager @Inject constructor(@ApplicationContext appContext: Context, private val settingsDataStore: DataStore<Preferences>) {

    val location: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[LOCATION] ?: "VN"
    }

    suspend fun setLocation(location: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[LOCATION] = location
            }
        }
    }

    val quality: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[QUALITY] ?: COMMON_QUALITY.items[0].toString()
    }

    suspend fun restore(isRestoring: Boolean) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[IS_RESTORING_DATABASE] = if (isRestoring) TRUE else FALSE
            }
        }
    }

    suspend fun setQuality(quality: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[QUALITY] = quality
            }
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
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[COOKIE] = cookie
            }
        }
    }

    suspend fun setLoggedIn(logged: Boolean) {
        withContext(Dispatchers.IO) {
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
    }

    val spotifyCookie = settingsDataStore.data.map { preferences ->
        preferences[SPOTIFY_COOKIE] ?: ""
    }

    suspend fun setSpotifyCookie(cookie: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPOTIFY_COOKIE] = cookie
            }
        }
    }

    val spotifyLoggedIn = settingsDataStore.data.map { preferences ->
        preferences[SPOTIFY_LOGGED_IN] ?: FALSE
    }

    suspend fun setSpotifyLoggedIn(logged: Boolean) {
        withContext(Dispatchers.IO) {
            if (logged) {
                settingsDataStore.edit { settings ->
                    settings[SPOTIFY_LOGGED_IN] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SPOTIFY_LOGGED_IN] = FALSE
                }
            }
        }
    }

    val spotifyAccessToken = settingsDataStore.data.map { preferences ->
        preferences[stringPreferencesKey("spotify_access_token")] ?: ""
    }

    suspend fun setSpotifyAccessToken(token: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[stringPreferencesKey("spotify_access_token")] = token
            }
        }
    }

    val spotifyAccessTokenExpire = settingsDataStore.data.map { preferences ->
        preferences[stringPreferencesKey("spotify_access_token_expire")] ?: ""
    }

    suspend fun setSpotifyAccessTokenExpire(token: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[stringPreferencesKey("spotify_access_token_expire")] = token
            }
        }
    }

    val normalizeVolume: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[NORMALIZE_VOLUME] ?: FALSE
    }

    suspend fun setNormalizeVolume(normalize: Boolean) {
        withContext(Dispatchers.IO) {
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
    }
    val skipSilent: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[SKIP_SILENT] ?: FALSE
    }
    suspend fun setSkipSilent(skip: Boolean) {
        withContext(Dispatchers.IO) {
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
    }

    val saveStateOfPlayback: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[SAVE_STATE_OF_PLAYBACK] ?: FALSE
    }
    suspend fun setSaveStateOfPlayback(save: Boolean) {
        withContext(Dispatchers.IO) {
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
    }
    val shuffleKey: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[SHUFFLE_KEY] ?: FALSE
    }
    val repeatKey: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[REPEAT_KEY] ?: REPEAT_MODE_OFF
    }

    suspend fun recoverShuffleAndRepeatKey(shuffle: Boolean, repeat: Int) {
        withContext(Dispatchers.IO) {
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
    }

    val saveRecentSongAndQueue: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[SAVE_RECENT_SONG] ?: FALSE
    }
    suspend fun setSaveRecentSongAndQueue(save: Boolean) {
        withContext(Dispatchers.IO) {
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
    }
    val recentMediaId = settingsDataStore.data.map { preferences ->
        preferences[RECENT_SONG_MEDIA_ID_KEY] ?: ""
    }
    val recentPosition = settingsDataStore.data.map { preferences ->
        preferences[RECENT_SONG_POSITION_KEY] ?: "0"
    }
    suspend fun saveRecentSong (mediaId: String, position: Long) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[RECENT_SONG_MEDIA_ID_KEY] = mediaId
                settings[RECENT_SONG_POSITION_KEY] = position.toString()
            }
        }
    }

    val playlistFromSaved = settingsDataStore.data.map { preferences ->
        preferences[FROM_SAVED_PLAYLIST] ?: ""
    }
    suspend fun setPlaylistFromSaved(playlist: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[FROM_SAVED_PLAYLIST] = playlist
            }
        }
    }

    val sendBackToGoogle = settingsDataStore.data.map { preferences ->
        preferences[SEND_BACK_TO_GOOGLE] ?: FALSE
    }
    suspend fun setSendBackToGoogle(send: Boolean) {
        withContext(Dispatchers.IO) {
            if (send) {
                settingsDataStore.edit { settings ->
                    settings[SEND_BACK_TO_GOOGLE] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SEND_BACK_TO_GOOGLE] = FALSE
                }
            }
        }
    }

    val sponsorBlockEnabled = settingsDataStore.data.map { preferences ->
        preferences[SPONSOR_BLOCK_ENABLED] ?: FALSE
    }
    suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (enabled) {
                settingsDataStore.edit { settings ->
                    settings[SPONSOR_BLOCK_ENABLED] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SPONSOR_BLOCK_ENABLED] = FALSE
                }
            }
        }
    }
    suspend fun getSponsorBlockCategories(): ArrayList<String> {
        val list : ArrayList<String> = arrayListOf()
        for (category in SPONSOR_BLOCK.list) {
            if (getString(category.toString()).first() == TRUE) list.add(category.toString())
        }
        return list
    }
    suspend fun setSponsorBlockCategories(categories: ArrayList<String>) {
        withContext(Dispatchers.IO) {
            Log.w("setSponsorBlockCategories", categories.toString())
            for (category in categories) {
                settingsDataStore.edit { settings ->
                    settings[stringPreferencesKey(category)] = TRUE
                }
            }
        }
    }

    companion object Settings {
        val COOKIE = stringPreferencesKey("cookie")
        val LOGGED_IN = stringPreferencesKey("logged_in")
        val SPOTIFY_COOKIE = stringPreferencesKey("spotify_cookie")
        val SPOTIFY_LOGGED_IN = stringPreferencesKey("spotify_logged_in")
        val LOCATION = stringPreferencesKey("location")
        val QUALITY = stringPreferencesKey("quality")
        val NORMALIZE_VOLUME = stringPreferencesKey("normalize_volume")
        val IS_RESTORING_DATABASE = stringPreferencesKey("is_restoring_database")
        val SKIP_SILENT = stringPreferencesKey("skip_silent")
        val SAVE_STATE_OF_PLAYBACK = stringPreferencesKey("save_state_of_playback")
        val SAVE_RECENT_SONG = stringPreferencesKey("save_recent_song")
        val RECENT_SONG_MEDIA_ID_KEY = stringPreferencesKey("recent_song_media_id")
        val RECENT_SONG_POSITION_KEY = stringPreferencesKey("recent_song_position")
        val SHUFFLE_KEY = stringPreferencesKey("shuffle_key")
        val REPEAT_KEY = stringPreferencesKey("repeat_key")
        val SEND_BACK_TO_GOOGLE = stringPreferencesKey("send_back_to_google")
        val FROM_SAVED_PLAYLIST = stringPreferencesKey("from_saved_playlist")
        val RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE = "RestoreLastPlayedTrackAndQueueDone"
        val SPONSOR_BLOCK_ENABLED = stringPreferencesKey("sponsor_block_enabled")
        val REPEAT_MODE_OFF = "REPEAT_MODE_OFF"
        val REPEAT_ONE = "REPEAT_ONE"
        val REPEAT_ALL = "REPEAT_ALL"
        val TRUE = "TRUE"
        val FALSE = "FALSE"
    }
}