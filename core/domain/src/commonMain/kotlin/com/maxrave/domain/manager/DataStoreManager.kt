package com.maxrave.domain.manager

import com.maxrave.domain.data.model.network.ProxyConfiguration
import kotlinx.coroutines.flow.Flow

interface DataStoreManager {
    val appVersion: Flow<String>

    suspend fun setAppVersion(version: String)

    val openAppTime: Flow<Int>

    suspend fun openApp()

    suspend fun resetOpenAppTime()

    suspend fun doneOpenAppTime()

    val location: Flow<String>

    suspend fun setLocation(location: String)

    val quality: Flow<String>

    suspend fun setQuality(quality: String)

    val downloadQuality: Flow<String>

    suspend fun setDownloadQuality(quality: String)

    val videoDownloadQuality: Flow<String>

    suspend fun setVideoDownloadQuality(quality: String)

    val language: Flow<String>

    fun getString(key: String): Flow<String?>

    suspend fun putString(
        key: String,
        value: String,
    )

    val loggedIn: Flow<String>
    val cookie: Flow<String>
    val pageId: Flow<String>

    suspend fun setCookie(
        cookie: String,
        pageId: String?,
    )

    suspend fun setLoggedIn(logged: Boolean)

    val normalizeVolume: Flow<String>

    suspend fun setNormalizeVolume(normalize: Boolean)

    val skipSilent: Flow<String>

    suspend fun setSkipSilent(skip: Boolean)

    val saveStateOfPlayback: Flow<String>

    suspend fun setSaveStateOfPlayback(save: Boolean)

    val shuffleKey: Flow<String>
    val repeatKey: Flow<String>

    suspend fun recoverShuffleAndRepeatKey(
        shuffle: Boolean,
        repeat: Int,
    )

    val saveRecentSongAndQueue: Flow<String>

    suspend fun setSaveRecentSongAndQueue(save: Boolean)

    val recentMediaId: Flow<String>
    val recentPosition: Flow<String>

    suspend fun saveRecentSong(
        mediaId: String,
        position: Long,
    )

    val playlistFromSaved: Flow<String>

    suspend fun setPlaylistFromSaved(playlist: String)

    val sendBackToGoogle: Flow<String>

    suspend fun setSendBackToGoogle(send: Boolean)

    val sponsorBlockEnabled: Flow<String>

    suspend fun setSponsorBlockEnabled(enabled: Boolean)

    suspend fun getSponsorBlockCategories(): ArrayList<String>

    suspend fun setSponsorBlockCategories(categories: ArrayList<String>)

    val enableTranslateLyric: Flow<String>

    suspend fun setEnableTranslateLyric(enable: Boolean)

    val lyricsProvider: Flow<String>

    suspend fun setLyricsProvider(provider: String)

    val translationLanguage: Flow<String>

    suspend fun setTranslationLanguage(language: String)

    val maxSongCacheSize: Flow<Int>

    suspend fun setMaxSongCacheSize(size: Int)

    val watchVideoInsteadOfPlayingAudio: Flow<String>

    suspend fun setWatchVideoInsteadOfPlayingAudio(watch: Boolean)

    val playerVolume: Flow<Float>

    suspend fun setPlayerVolume(volume: Float)

    val videoQuality: Flow<String>

    suspend fun setVideoQuality(quality: String)

    val spdc: Flow<String>

    suspend fun setSpdc(spdc: String)

    val spotifyLyrics: Flow<String>

    suspend fun setSpotifyLyrics(spotifyLyrics: Boolean)

    val spotifyCanvas: Flow<String>

    suspend fun setSpotifyCanvas(spotifyCanvas: Boolean)

    val spotifyClientToken: Flow<String>

    suspend fun setSpotifyClientToken(token: String)

    val spotifyClientTokenExpires: Flow<Long>

    suspend fun setSpotifyClientTokenExpires(expires: Long)

    val spotifyPersonalToken: Flow<String>

    suspend fun setSpotifyPersonalToken(token: String)

    val spotifyPersonalTokenExpires: Flow<Long>

    suspend fun setSpotifyPersonalTokenExpires(expires: Long)

    val homeLimit: Flow<Int>

    suspend fun setHomeLimit(limit: Int)

    val chartKey: Flow<String>

    suspend fun setChartKey(key: String)

    val translucentBottomBar: Flow<String>

    suspend fun setTranslucentBottomBar(translucent: Boolean)

    val usingProxy: Flow<String>

    suspend fun setUsingProxy(usingProxy: Boolean)

    val proxyType: Flow<ProxyType>

    suspend fun setProxyType(proxyType: ProxyType)

    val proxyHost: Flow<String>

    suspend fun setProxyHost(proxyHost: String)

    val proxyPort: Flow<Int>

    suspend fun setProxyPort(proxyPort: Int)

    val proxyUsername: Flow<String>

    suspend fun setProxyUsername(proxyUsername: String)

    val proxyPassword: Flow<String>

    suspend fun setProxyPassword(proxyPassword: String)

    fun getJVMProxy(): ProxyConfiguration?

    val endlessQueue: Flow<String>

    suspend fun setEndlessQueue(endlessQueue: Boolean)

    val keepYouTubePlaylistOffline: Flow<String>

    suspend fun setKeepYouTubePlaylistOffline(keep: Boolean)

    val combineLocalAndYouTubeLiked: Flow<String>

    suspend fun setCombineLocalAndYouTubeLiked(combine: Boolean)

    val shouldShowLogInRequiredAlert: Flow<String>

    suspend fun setShouldShowLogInRequiredAlert(shouldShow: Boolean)

    val autoCheckForUpdates: Flow<String>

    suspend fun setAutoCheckForUpdates(autoCheck: Boolean)

    val updateChannel: Flow<String>

    suspend fun setUpdateChannel(channel: String)

    val blurFullscreenLyrics: Flow<String>

    suspend fun setBlurFullscreenLyrics(blur: Boolean)

    val blurPlayerBackground: Flow<String>

    suspend fun setBlurPlayerBackground(blur: Boolean)

    val playbackSpeed: Flow<Float>

    fun setPlaybackSpeed(speed: Float)

    val pitch: Flow<Int>

    fun setPitch(pitch: Int)

    val dataSyncId: Flow<String>

    suspend fun setDataSyncId(dataSyncId: String)

    val visitorData: Flow<String>

    suspend fun setVisitorData(visitorData: String)

    suspend fun setAIProvider(provider: String)

    val aiProvider: Flow<String>

    suspend fun setAIApiKey(apiKey: String)

    val aiApiKey: Flow<String>

    val useAITranslation: Flow<String>

    suspend fun setUseAITranslation(use: Boolean)

    val customModelId: Flow<String>

    suspend fun setCustomModelId(modelId: String)

    val customOpenAIBaseUrl: Flow<String>

    suspend fun setCustomOpenAIBaseUrl(baseUrl: String)

    val customOpenAIHeaders: Flow<String>

    suspend fun setCustomOpenAIHeaders(headers: String)

    val localPlaylistFilter: Flow<String>

    suspend fun setLocalPlaylistFilter(filter: String)

    val killServiceOnExit: Flow<String>

    suspend fun setKillServiceOnExit(kill: Boolean)

    val keepServiceAlive: Flow<String>

    suspend fun setKeepServiceAlive(keep: Boolean)

    val crossfadeEnabled: Flow<String>

    suspend fun setCrossfadeEnabled(enabled: Boolean)

    val crossfadeDuration: Flow<Int>

    suspend fun setCrossfadeDuration(duration: Int)

    val crossfadeDjMode: Flow<String>

    suspend fun setCrossfadeDjMode(enabled: Boolean)

    val prefer320kbpsStream: Flow<String>

    suspend fun setPrefer320kbpsStream(enabled: Boolean)

    val your320kbpsUrl: Flow<String>

    suspend fun setYour320kbpsUrl(url: String)

    val youtubeSubtitleLanguage: Flow<String>

    suspend fun setYoutubeSubtitleLanguage(language: String)

    val helpBuildLyricsDatabase: Flow<String>

    suspend fun setHelpBuildLyricsDatabase(help: Boolean)

    val contributorName: Flow<String>
    val contributorEmail: Flow<String>

    suspend fun setContributorLyricsDatabase(contributor: Pair<String, String>?)

    val backupDownloaded: Flow<String>

    suspend fun setBackupDownloaded(backupDownloaded: Boolean)

    val enableLiquidGlass: Flow<String>

    suspend fun setEnableLiquidGlass(enable: Boolean)

    val explicitContentEnabled: Flow<String>

    suspend fun setExplicitContentEnabled(enabled: Boolean)

    val discordToken: Flow<String>

    suspend fun setDiscordToken(token: String)

    val richPresenceEnabled: Flow<String>

    suspend fun setRichPresenceEnabled(enabled: Boolean)

    val localTrackingEnabled: Flow<String>

    suspend fun setLocalTrackingEnabled(enabled: Boolean)

    val blogNotificationEnabled: Flow<String>

    suspend fun setBlogNotificationEnabled(enabled: Boolean)

    // Auto Backup
    val autoBackupEnabled: Flow<String>

    suspend fun setAutoBackupEnabled(enabled: Boolean)

    val autoBackupFrequency: Flow<String>

    suspend fun setAutoBackupFrequency(frequency: String)

    val autoBackupMaxFiles: Flow<Int>

    suspend fun setAutoBackupMaxFiles(max: Int)

    val autoBackupLastTime: Flow<Long>

    suspend fun setAutoBackupLastTime(time: Long)

    enum class ProxyType {
        PROXY_TYPE_HTTP,
        PROXY_TYPE_SOCKS,
    }

    companion object Values {
        const val FOLLOW_SYSTEM_THEME = "follow_system_theme"
        const val FORCE_LIGHT_THEME = "force_light_theme"

        const val SIMPMUSIC = "simpmusic"
        const val YOUTUBE = "youtube"
        const val LRCLIB = "lrclib"
        const val BETTER_LYRICS = "better_lyrics"

        const val FDROID = "fdroid"
        const val GITHUB_FOSS_NIGHTLY = "github_foss_nightly"
        const val GITHUB = "github_release"

        const val REPEAT_MODE_OFF = "REPEAT_MODE_OFF"
        const val REPEAT_ONE = "REPEAT_ONE"
        const val REPEAT_ALL = "REPEAT_ALL"

        const val TRUE = "TRUE"
        const val FALSE = "FALSE"

        const val CROSSFADE_DURATION_AUTO = 0

        const val PROXY_TYPE_HTTP = "http"
        const val PROXY_TYPE_SOCKS = "socks"

        // AI
        const val AI_PROVIDER_GEMINI = "gemini"
        const val AI_PROVIDER_OPENAI = "openai"
        const val AI_PROVIDER_CUSTOM_OPENAI = "custom_openai"

        const val LOCAL_PLAYLIST_FILTER_OLDER_FIRST = "older_first"
        const val LOCAL_PLAYLIST_FILTER_NEWER_FIRST = "newer_first"
        const val LOCAL_PLAYLIST_FILTER_TITLE = "title"
        const val LOCAL_PLAYLIST_FILTER_CUSTOM_ORDER = "custom_order"

        // Auto Backup Frequency
        const val AUTO_BACKUP_FREQUENCY_DAILY = "daily"
        const val AUTO_BACKUP_FREQUENCY_WEEKLY = "weekly"
        const val AUTO_BACKUP_FREQUENCY_MONTHLY = "monthly"
    }
}