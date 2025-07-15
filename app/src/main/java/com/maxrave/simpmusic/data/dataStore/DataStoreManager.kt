package com.maxrave.simpmusic.data.dataStore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.Player
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.SPONSOR_BLOCK
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.Proxy
import com.maxrave.simpmusic.common.QUALITY as COMMON_QUALITY

class DataStoreManager(
    private val settingsDataStore: DataStore<Preferences>,
) {
    val appVersion: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[APP_VERSION] ?: ""
        }

    suspend fun setAppVersion(version: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[APP_VERSION] = version
            }
        }
    }

    val openAppTime: Flow<Int> =
        settingsDataStore.data.map { preferences ->
            preferences[OPEN_APP_TIME] ?: 0
        }

    suspend fun openApp() {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[OPEN_APP_TIME] = openAppTime.first() + 1
            }
        }
    }

    suspend fun resetOpenAppTime() {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[OPEN_APP_TIME] = 0
            }
        }
    }

    suspend fun doneOpenAppTime() {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[OPEN_APP_TIME] = 31
            }
        }
    }

    val location: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[LOCATION] ?: "VN"
        }

    suspend fun setLocation(location: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[LOCATION] = location
            }
        }
    }

    val quality: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[QUALITY] ?: COMMON_QUALITY.items[0].toString()
        }

    suspend fun setQuality(quality: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[QUALITY] = quality
            }
        }
    }

    val language: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[stringPreferencesKey(SELECTED_LANGUAGE)] ?: SUPPORTED_LANGUAGE.codes.first()
        }

    fun getString(key: String): Flow<String?> =
        settingsDataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)]
        }

    suspend fun putString(
        key: String,
        value: String,
    ) {
        settingsDataStore.edit { settings ->
            settings[stringPreferencesKey(key)] = value
        }
    }

    val loggedIn: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[LOGGED_IN] ?: FALSE
        }

    val cookie: Flow<String> =
        settingsDataStore.data.map { preferences ->
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

    val normalizeVolume: Flow<String> =
        settingsDataStore.data.map { preferences ->
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

    val skipSilent: Flow<String> =
        settingsDataStore.data.map { preferences ->
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

    val saveStateOfPlayback: Flow<String> =
        settingsDataStore.data.map { preferences ->
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

    val shuffleKey: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SHUFFLE_KEY] ?: FALSE
        }
    val repeatKey: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[REPEAT_KEY] ?: REPEAT_MODE_OFF
        }

    suspend fun recoverShuffleAndRepeatKey(
        shuffle: Boolean,
        repeat: Int,
    ) {
        withContext(Dispatchers.IO) {
            if (shuffle) {
                settingsDataStore.edit { settings ->
                    settings[SHUFFLE_KEY] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SHUFFLE_KEY] = FALSE
                }
            }
            settingsDataStore.edit { settings ->
                settings[REPEAT_KEY] =
                    when (repeat) {
                        Player.REPEAT_MODE_ONE -> REPEAT_ONE
                        Player.REPEAT_MODE_ALL -> REPEAT_ALL
                        Player.REPEAT_MODE_OFF -> REPEAT_MODE_OFF
                        else -> REPEAT_MODE_OFF
                    }
            }
        }
    }

    val saveRecentSongAndQueue: Flow<String> =
        settingsDataStore.data.map { preferences ->
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

    val recentMediaId =
        settingsDataStore.data.map { preferences ->
            preferences[RECENT_SONG_MEDIA_ID_KEY] ?: ""
        }
    val recentPosition =
        settingsDataStore.data.map { preferences ->
            preferences[RECENT_SONG_POSITION_KEY] ?: "0"
        }

    suspend fun saveRecentSong(
        mediaId: String,
        position: Long,
    ) {
        Log.w("saveRecentSong", "$mediaId $position")
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[RECENT_SONG_MEDIA_ID_KEY] = mediaId
                settings[RECENT_SONG_POSITION_KEY] = position.toString()
            }
        }
    }

    val playlistFromSaved =
        settingsDataStore.data.map { preferences ->
            preferences[FROM_SAVED_PLAYLIST] ?: ""
        }

    suspend fun setPlaylistFromSaved(playlist: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[FROM_SAVED_PLAYLIST] = playlist
            }
        }
    }

    val sendBackToGoogle =
        settingsDataStore.data.map { preferences ->
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

    val sponsorBlockEnabled =
        settingsDataStore.data.map { preferences ->
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
        val list: ArrayList<String> = arrayListOf()
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
            SPONSOR_BLOCK.list.filter { !categories.contains(it) }.forEach { category ->
                settingsDataStore.edit { settings ->
                    settings[stringPreferencesKey(category.toString())] = FALSE
                }
            }
        }
    }

    val enableTranslateLyric =
        settingsDataStore.data.map { preferences ->
            preferences[USE_TRANSLATION_LANGUAGE] ?: FALSE
        }

    suspend fun setEnableTranslateLyric(enable: Boolean) {
        withContext(Dispatchers.IO) {
            if (enable) {
                settingsDataStore.edit { settings ->
                    settings[USE_TRANSLATION_LANGUAGE] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[USE_TRANSLATION_LANGUAGE] = FALSE
                }
            }
        }
    }

    val lyricsProvider =
        settingsDataStore.data.map { preferences ->
            preferences[LYRICS_PROVIDER] ?: MUSIXMATCH
        }

    suspend fun setLyricsProvider(provider: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[LYRICS_PROVIDER] = provider
            }
        }
    }

    val musixmatchLoggedIn =
        settingsDataStore.data.map { preferences ->
            preferences[MUSIXMATCH_LOGGED_IN] ?: FALSE
        }

    suspend fun setMusixmatchLoggedIn(loggedIn: Boolean) {
        withContext(Dispatchers.IO) {
            if (loggedIn) {
                settingsDataStore.edit { settings ->
                    settings[MUSIXMATCH_LOGGED_IN] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[MUSIXMATCH_LOGGED_IN] = FALSE
                }
            }
        }
    }

    val translationLanguage =
        settingsDataStore.data.map { preferences ->
            val languageValue = language.first()
            preferences[TRANSLATION_LANGUAGE] ?: if (languageValue.length >= 2) {
                languageValue
                    .substring(0..1)
            } else {
                "en"
            }
        }

    suspend fun setTranslationLanguage(language: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[TRANSLATION_LANGUAGE] = language
            }
        }
    }

    val musixmatchCookie =
        settingsDataStore.data.map { preferences ->
            preferences[MUSIXMATCH_COOKIE] ?: ""
        }

    suspend fun setMusixmatchCookie(cookie: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[MUSIXMATCH_COOKIE] = cookie
            }
        }
    }

    val musixmatchUserToken =
        settingsDataStore.data.map { preferences ->
            preferences[MUSIXMATCH_USER_TOKEN] ?: ""
        }

    suspend fun setMusixmatchUserToken(token: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[MUSIXMATCH_USER_TOKEN] = token
            }
        }
    }

    val maxSongCacheSize =
        settingsDataStore.data.map { preferences ->
            preferences[MAX_SONG_CACHE_SIZE] ?: -1
        }

    suspend fun setMaxSongCacheSize(size: Int) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[MAX_SONG_CACHE_SIZE] = size
            }
        }
    }

    val watchVideoInsteadOfPlayingAudio =
        settingsDataStore.data.map { preferences ->
            preferences[WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO] ?: FALSE
        }

    suspend fun setWatchVideoInsteadOfPlayingAudio(watch: Boolean) {
        withContext(Dispatchers.IO) {
            if (watch) {
                settingsDataStore.edit { settings ->
                    settings[WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO] = FALSE
                }
            }
        }
    }

    val videoQuality =
        settingsDataStore.data.map { preferences ->
            preferences[VIDEO_QUALITY] ?: "720p"
        }

    suspend fun setVideoQuality(quality: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[VIDEO_QUALITY] = quality
            }
        }
    }

    val spdc =
        settingsDataStore.data.map { preferences ->
            preferences[SPDC] ?: ""
        }

    suspend fun setSpdc(spdc: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPDC] = spdc
            }
        }
    }

    val spotifyLyrics: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_LYRICS] ?: FALSE
        }

    suspend fun setSpotifyLyrics(spotifyLyrics: Boolean) {
        withContext(Dispatchers.IO) {
            if (spotifyLyrics) {
                settingsDataStore.edit { settings ->
                    settings[SPOTIFY_LYRICS] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SPOTIFY_LYRICS] = FALSE
                }
            }
        }
    }

    val spotifyCanvas: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_CANVAS] ?: FALSE
        }

    suspend fun setSpotifyCanvas(spotifyCanvas: Boolean) {
        withContext(Dispatchers.IO) {
            if (spotifyCanvas) {
                settingsDataStore.edit { settings ->
                    settings[SPOTIFY_CANVAS] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SPOTIFY_CANVAS] = FALSE
                }
            }
        }
    }

    val spotifyClientToken: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_CLIENT_TOKEN] ?: ""
        }

    suspend fun setSpotifyClientToken(token: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPOTIFY_CLIENT_TOKEN] = token
            }
        }
    }

    val spotifyClientTokenExpires: Flow<Long> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_CLIENT_TOKEN_EXPIRES] ?: 0
        }

    suspend fun setSpotifyClientTokenExpires(expires: Long) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPOTIFY_CLIENT_TOKEN_EXPIRES] = expires
            }
        }
    }

    val spotifyPersonalToken: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_PERSONAL_TOKEN] ?: ""
        }

    suspend fun setSpotifyPersonalToken(token: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPOTIFY_PERSONAL_TOKEN] = token
            }
        }
    }

    val spotifyPersonalTokenExpires: Flow<Long> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_PERSONAL_TOKEN_EXPIRES] ?: 0
        }

    suspend fun setSpotifyPersonalTokenExpires(expires: Long) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPOTIFY_PERSONAL_TOKEN_EXPIRES] = expires
            }
        }
    }

    val homeLimit: Flow<Int> =
        settingsDataStore.data.map { preferences ->
            preferences[HOME_LIMIT] ?: 5
        }

    suspend fun setHomeLimit(limit: Int) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[HOME_LIMIT] = limit
            }
        }
    }

    val chartKey =
        settingsDataStore.data.map { preferences ->
            preferences[CHART_KEY] ?: "ZZ"
        }

    suspend fun setChartKey(key: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[CHART_KEY] = key
            }
        }
    }

    val translucentBottomBar =
        settingsDataStore.data.map { preferences ->
            preferences[TRANSLUCENT_BOTTOM_BAR] ?: TRUE
        }

    suspend fun setTranslucentBottomBar(translucent: Boolean) {
        withContext(Dispatchers.IO) {
            if (translucent) {
                settingsDataStore.edit { settings ->
                    settings[TRANSLUCENT_BOTTOM_BAR] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[TRANSLUCENT_BOTTOM_BAR] = FALSE
                }
            }
        }
    }

    val usingProxy =
        settingsDataStore.data.map { preferences ->
            preferences[USING_PROXY] ?: FALSE
        }

    suspend fun setUsingProxy(usingProxy: Boolean) {
        withContext(Dispatchers.IO) {
            if (usingProxy) {
                settingsDataStore.edit { settings ->
                    settings[USING_PROXY] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[USING_PROXY] = FALSE
                }
            }
        }
    }

    val proxyType =
        settingsDataStore.data
            .map { preferences ->
                preferences[PROXY_TYPE]
            }.map {
                when (it) {
                    PROXY_TYPE_HTTP -> ProxyType.PROXY_TYPE_HTTP
                    PROXY_TYPE_SOCKS -> ProxyType.PROXY_TYPE_SOCKS
                    else -> ProxyType.PROXY_TYPE_HTTP
                }
            }

    suspend fun setProxyType(proxyType: ProxyType) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[PROXY_TYPE] =
                    when (proxyType) {
                        ProxyType.PROXY_TYPE_HTTP -> PROXY_TYPE_HTTP
                        ProxyType.PROXY_TYPE_SOCKS -> PROXY_TYPE_SOCKS
                    }
            }
        }
    }

    val proxyHost =
        settingsDataStore.data.map { preferences ->
            preferences[PROXY_HOST] ?: ""
        }

    suspend fun setProxyHost(proxyHost: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[PROXY_HOST] = proxyHost
            }
        }
    }

    val proxyPort =
        settingsDataStore.data.map { preferences ->
            preferences[PROXY_PORT] ?: 8000
        }

    suspend fun setProxyPort(proxyPort: Int) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[PROXY_PORT] = proxyPort
            }
        }
    }

    fun getJVMProxy(): Proxy? =
        runBlocking {
            try {
                if (usingProxy.first() == TRUE) {
                    val proxyType = proxyType.first()
                    val proxyHost = proxyHost.first()
                    val proxyPort = proxyPort.first()
                    return@runBlocking Proxy(
                        when (proxyType) {
                            ProxyType.PROXY_TYPE_HTTP -> Proxy.Type.HTTP
                            ProxyType.PROXY_TYPE_SOCKS -> Proxy.Type.SOCKS
                        },
                        java.net.InetSocketAddress(proxyHost, proxyPort),
                    )
                } else {
                    return@runBlocking null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@runBlocking null
            }
        }

    val endlessQueue =
        settingsDataStore.data.map { preferences ->
            preferences[ENDLESS_QUEUE] ?: FALSE
        }

    suspend fun setEndlessQueue(endlessQueue: Boolean) {
        withContext(Dispatchers.IO) {
            if (endlessQueue) {
                settingsDataStore.edit { settings ->
                    settings[ENDLESS_QUEUE] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[ENDLESS_QUEUE] = FALSE
                }
            }
        }
    }

    val shouldShowLogInRequiredAlert =
        settingsDataStore.data.map { preferences ->
            preferences[SHOULD_SHOW_LOG_IN_REQUIRED_ALERT] ?: TRUE
        }

    suspend fun setShouldShowLogInRequiredAlert(shouldShow: Boolean) {
        withContext(Dispatchers.IO) {
            if (shouldShow) {
                settingsDataStore.edit { settings ->
                    settings[SHOULD_SHOW_LOG_IN_REQUIRED_ALERT] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SHOULD_SHOW_LOG_IN_REQUIRED_ALERT] = FALSE
                }
            }
        }
    }

    val autoCheckForUpdates =
        settingsDataStore.data.map { preferences ->
            preferences[AUTO_CHECK_FOR_UPDATES] ?: TRUE
        }

    suspend fun setAutoCheckForUpdates(autoCheck: Boolean) {
        withContext(Dispatchers.IO) {
            if (autoCheck) {
                settingsDataStore.edit { settings ->
                    settings[AUTO_CHECK_FOR_UPDATES] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[AUTO_CHECK_FOR_UPDATES] = FALSE
                }
            }
        }
    }

    val blurFullscreenLyrics =
        settingsDataStore.data.map { preferences ->
            preferences[BLUR_FULLSCREEN_LYRICS] ?: FALSE
        }

    suspend fun setBlurFullscreenLyrics(blur: Boolean) {
        withContext(Dispatchers.IO) {
            if (blur) {
                settingsDataStore.edit { settings ->
                    settings[BLUR_FULLSCREEN_LYRICS] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[BLUR_FULLSCREEN_LYRICS] = FALSE
                }
            }
        }
    }

    val blurPlayerBackground =
        settingsDataStore.data.map { preferences ->
            preferences[BLUR_PLAYER_BACKGROUND] ?: FALSE
        }

    suspend fun setBlurPlayerBackground(blur: Boolean) {
        withContext(Dispatchers.IO) {
            if (blur) {
                settingsDataStore.edit { settings ->
                    settings[BLUR_PLAYER_BACKGROUND] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[BLUR_PLAYER_BACKGROUND] = FALSE
                }
            }
        }
    }

    val playbackSpeed =
        settingsDataStore.data.map { preferences ->
            preferences[PLAYBACK_SPEED] ?: 1.0f
        }

    fun setPlaybackSpeed(speed: Float) {
        runBlocking {
            settingsDataStore.edit { settings ->
                settings[PLAYBACK_SPEED] = speed
            }
        }
    }

    val pitch =
        settingsDataStore.data.map { preferences ->
            preferences[PITCH] ?: 0
        }

    fun setPitch(pitch: Int) {
        runBlocking {
            settingsDataStore.edit { settings ->
                settings[PITCH] = pitch
            }
        }
    }

    val dataSyncId =
        settingsDataStore.data.map { preferences ->
            preferences[DATA_SYNC_ID] ?: ""
        }

    suspend fun setDataSyncId(dataSyncId: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[DATA_SYNC_ID] = dataSyncId
            }
        }
    }

    val visitorData =
        settingsDataStore.data.map { preferences ->
            preferences[VISITOR_DATA] ?: ""
        }

    suspend fun setVisitorData(visitorData: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[VISITOR_DATA] = visitorData
            }
        }
    }

    suspend fun setAIProvider(provider: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[stringPreferencesKey("ai_provider")] = provider
            }
        }
    }

    val aiProvider: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[AI_PROVIDER] ?: AI_PROVIDER_GEMINI
        }

    suspend fun setAIApiKey(apiKey: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[AI_API_KEY] = apiKey
            }
        }
    }

    val aiApiKey: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[AI_API_KEY] ?: ""
        }

    val useAITranslation: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[USE_AI_TRANSLATION] ?: FALSE
        }

    suspend fun setUseAITranslation(use: Boolean) {
        withContext(Dispatchers.IO) {
            if (use) {
                settingsDataStore.edit { settings ->
                    settings[USE_AI_TRANSLATION] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[USE_AI_TRANSLATION] = FALSE
                }
            }
        }
    }

    val customModelId =
        settingsDataStore.data.map { preferences ->
            preferences[CUSTOM_MODEL_ID] ?: ""
        }

    suspend fun setCustomModelId(modelId: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[CUSTOM_MODEL_ID] = modelId
            }
        }
    }

    val localPlaylistFilter: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[LOCAL_PLAYLIST_FILTER] ?: LOCAL_PLAYLIST_FILTER_OLDER_FIRST
        }

    suspend fun setLocalPlaylistFilter(filter: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[LOCAL_PLAYLIST_FILTER] = filter
            }
        }
    }

    val killServiceOnExit: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[KILL_SERVICE_ON_EXIT] ?: FALSE
        }

    suspend fun setKillServiceOnExit(kill: Boolean) {
        withContext(Dispatchers.IO) {
            if (kill) {
                settingsDataStore.edit { settings ->
                    settings[KILL_SERVICE_ON_EXIT] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[KILL_SERVICE_ON_EXIT] = FALSE
                }
            }
        }
    }

    val crossfadeEnabled: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[CROSSFADE_ENABLED] ?: FALSE
        }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (enabled) {
                settingsDataStore.edit { settings ->
                    settings[CROSSFADE_ENABLED] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[CROSSFADE_ENABLED] = FALSE
                }
            }
        }
    }

    val crossfadeDuration: Flow<Int> =
        settingsDataStore.data.map { preferences ->
            preferences[CROSSFADE_DURATION] ?: 5000
        }

    suspend fun setCrossfadeDuration(duration: Int) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[CROSSFADE_DURATION] = duration
            }
        }
    }

    val youtubeSubtitleLanguage =
        settingsDataStore.data.map { preferences ->
            val languageValue = language.first()
            preferences[YOUTUBE_SUBTITLE_LANGUAGE] ?: if (languageValue.length >= 2) {
                languageValue
                    .substring(0..1)
            } else {
                "en"
            }
        }

    suspend fun setYoutubeSubtitleLanguage(language: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[YOUTUBE_SUBTITLE_LANGUAGE] = language
            }
        }
    }

    companion object Settings {
        val APP_VERSION = stringPreferencesKey("app_version")
        val COOKIE = stringPreferencesKey("cookie")
        val LOGGED_IN = stringPreferencesKey("logged_in")
        val LOCATION = stringPreferencesKey("location")
        val QUALITY = stringPreferencesKey("quality")
        val NORMALIZE_VOLUME = stringPreferencesKey("normalize_volume")
        val SKIP_SILENT = stringPreferencesKey("skip_silent")
        val SAVE_STATE_OF_PLAYBACK = stringPreferencesKey("save_state_of_playback")
        val SAVE_RECENT_SONG = stringPreferencesKey("save_recent_song")
        val RECENT_SONG_MEDIA_ID_KEY = stringPreferencesKey("recent_song_media_id")
        val RECENT_SONG_POSITION_KEY = stringPreferencesKey("recent_song_position")
        val SHUFFLE_KEY = stringPreferencesKey("shuffle_key")
        val REPEAT_KEY = stringPreferencesKey("repeat_key")
        val SEND_BACK_TO_GOOGLE = stringPreferencesKey("send_back_to_google")
        val FROM_SAVED_PLAYLIST = stringPreferencesKey("from_saved_playlist")
        val MUSIXMATCH_LOGGED_IN = stringPreferencesKey("musixmatch_logged_in")
        val KILL_SERVICE_ON_EXIT = stringPreferencesKey("kill_service_on_exit")
        val CROSSFADE_ENABLED = stringPreferencesKey("crossfade_enabled")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        const val SIMPMUSIC = "simpmusic"
        const val YOUTUBE = "youtube"
        const val MUSIXMATCH = "musixmatch"
        const val LRCLIB = "lrclib"
        val LYRICS_PROVIDER = stringPreferencesKey("lyrics_provider")
        val TRANSLATION_LANGUAGE = stringPreferencesKey("translation_language")
        val USE_TRANSLATION_LANGUAGE = stringPreferencesKey("use_translation_language")
        val MUSIXMATCH_COOKIE = stringPreferencesKey("musixmatch_cookie")
        val MUSIXMATCH_USER_TOKEN = stringPreferencesKey("musixmatch_user_token")
        val SPONSOR_BLOCK_ENABLED = stringPreferencesKey("sponsor_block_enabled")
        val MAX_SONG_CACHE_SIZE = intPreferencesKey("maxSongCacheSize")
        val WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO =
            stringPreferencesKey("watch_video_instead_of_playing_audio")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val SPDC = stringPreferencesKey("sp_dc")
        val SPOTIFY_LYRICS = stringPreferencesKey("spotify_lyrics")
        val SPOTIFY_CANVAS = stringPreferencesKey("spotify_canvas")
        val SPOTIFY_CLIENT_TOKEN = stringPreferencesKey("spotify_client_token")
        val SPOTIFY_CLIENT_TOKEN_EXPIRES = longPreferencesKey("spotify_client_token_expires")
        val SPOTIFY_PERSONAL_TOKEN = stringPreferencesKey("spotify_personal_token")
        val SPOTIFY_PERSONAL_TOKEN_EXPIRES = longPreferencesKey("spotify_personal_token_expires")
        val HOME_LIMIT = intPreferencesKey("home_limit")
        val CHART_KEY = stringPreferencesKey("chart_key")
        val TRANSLUCENT_BOTTOM_BAR = stringPreferencesKey("translucent_bottom_bar")
        val USING_PROXY = stringPreferencesKey("using_proxy")
        val PROXY_TYPE = stringPreferencesKey("proxy_type")
        val PROXY_HOST = stringPreferencesKey("proxy_host")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val ENDLESS_QUEUE = stringPreferencesKey("endless_queue")
        val SHOULD_SHOW_LOG_IN_REQUIRED_ALERT = stringPreferencesKey("should_show_log_in_required_alert")
        val AUTO_CHECK_FOR_UPDATES = stringPreferencesKey("auto_check_for_updates")
        val BLUR_FULLSCREEN_LYRICS = stringPreferencesKey("blur_fullscreen_lyrics")
        val BLUR_PLAYER_BACKGROUND = stringPreferencesKey("blur_player_background")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PITCH = intPreferencesKey("pitch")
        val OPEN_APP_TIME = intPreferencesKey("open_app_time")
        val DATA_SYNC_ID = stringPreferencesKey("data_sync_id")
        val VISITOR_DATA = stringPreferencesKey("visitor_data")
        const val REPEAT_MODE_OFF = "REPEAT_MODE_OFF"
        const val REPEAT_ONE = "REPEAT_ONE"
        const val REPEAT_ALL = "REPEAT_ALL"
        const val TRUE = "TRUE"
        const val FALSE = "FALSE"
        const val PROXY_TYPE_HTTP = "http"
        const val PROXY_TYPE_SOCKS = "socks"

        // AI
        const val AI_PROVIDER_GEMINI = "gemini"
        const val AI_PROVIDER_OPENAI = "openai"

        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_API_KEY = stringPreferencesKey("ai_gemini_api_key")

        val CUSTOM_MODEL_ID = stringPreferencesKey("custom_model_id")

        val USE_AI_TRANSLATION = stringPreferencesKey("use_ai_translation")

        val LOCAL_PLAYLIST_FILTER = stringPreferencesKey("local_playlist_filter")
        const val LOCAL_PLAYLIST_FILTER_OLDER_FIRST = "older_first"
        const val LOCAL_PLAYLIST_FILTER_NEWER_FIRST = "newer_first"
        const val LOCAL_PLAYLIST_FILTER_TITLE = "title"
        val YOUTUBE_SUBTITLE_LANGUAGE = stringPreferencesKey("youtube_subtitle_language")
        val HELP_BUILD_LYRICS_DATABASE = stringPreferencesKey("help_build_lyrics_database")
        val CONTRIBUTOR_NAME = stringPreferencesKey("contributor_name")
        val CONTRIBUTOR_EMAIL = stringPreferencesKey("contributor_email")

        // Proxy type
        enum class ProxyType {
            PROXY_TYPE_HTTP,
            PROXY_TYPE_SOCKS,
        }
    }

    val helpBuildLyricsDatabase: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[HELP_BUILD_LYRICS_DATABASE] ?: FALSE
        }

    suspend fun setHelpBuildLyricsDatabase(help: Boolean) {
        withContext(Dispatchers.IO) {
            if (help) {
                settingsDataStore.edit { settings ->
                    settings[HELP_BUILD_LYRICS_DATABASE] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[HELP_BUILD_LYRICS_DATABASE] = FALSE
                }
            }
        }
    }

    val contributorName: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[CONTRIBUTOR_NAME] ?: ""
        }

    val contributorEmail: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[CONTRIBUTOR_EMAIL] ?: ""
        }

    suspend fun setContributorLyricsDatabase(
        contributor: Pair<String, String>?, // contributor name and email, null if anonymous
    ) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                if (contributor == null) {
                    settings[CONTRIBUTOR_NAME] = ""
                    settings[CONTRIBUTOR_EMAIL] = ""
                } else {
                    settings[CONTRIBUTOR_NAME] = contributor.first
                    settings[CONTRIBUTOR_EMAIL] = contributor.second
                }
            }
        }
    }
}