package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import com.eygraber.uri.Uri
import com.maxrave.common.Config
import com.maxrave.common.QUALITY
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.VIDEO_QUALITY
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.GoogleAccountEntity
import com.maxrave.domain.data.model.lyrics.RomanizationDictionaryState
import com.maxrave.domain.data.player.GenericCastState
import com.maxrave.domain.extension.toNetScapeString
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.repository.AccountRepository
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.domain.repository.LyricsRomanizerRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.LocalResource
import com.maxrave.logger.LogLevel
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.simpmusic.lastfm.isLastfmAvailable
import org.jetbrains.compose.resources.getString as formatString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.backup_create_failed
import simpmusic.composeapp.generated.resources.backup_create_success
import simpmusic.composeapp.generated.resources.backup_in_progress
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.clear_canvas_cache
import simpmusic.composeapp.generated.resources.clear_listening_history
import simpmusic.composeapp.generated.resources.clear_downloaded_cache
import simpmusic.composeapp.generated.resources.clear_listening_history_done
import simpmusic.composeapp.generated.resources.clear_player_cache
import simpmusic.composeapp.generated.resources.clear_thumbnail_cache
import simpmusic.composeapp.generated.resources.downloading_liked_songs
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.log_out_confirm_message
import simpmusic.composeapp.generated.resources.restore_failed
import simpmusic.composeapp.generated.resources.restore_in_progress
import simpmusic.composeapp.generated.resources.romanization_japanese_dict_failed
import simpmusic.composeapp.generated.resources.romanization_japanese_dict_ready
import simpmusic.composeapp.generated.resources.warning
import kotlin.coroutines.cancellation.CancellationException

class SettingsViewModel(
    private val dataStoreManager: DataStoreManager,
    private val commonRepository: CommonRepository,
    private val songRepository: SongRepository,
    private val accountRepository: AccountRepository,
    private val cacheRepository: CacheRepository,
    private val artistRepository: ArtistRepository,
    private val lyricsRomanizerRepository: LyricsRomanizerRepository,
) : BaseViewModel() {
    private val databasePath: String? = commonRepository.getDatabasePath()
    private val downloadUtils: DownloadHandler by inject()

    val castState: StateFlow<GenericCastState> get() = mediaPlayerHandler.castState

    /** READY everywhere the dictionary is bundled (Desktop), so only Android ever leaves it. */
    val japaneseDictionaryState: StateFlow<RomanizationDictionaryState> =
        lyricsRomanizerRepository.japaneseDictionaryState

    private var _location: MutableStateFlow<String?> = MutableStateFlow(null)
    val location: StateFlow<String?> = _location
    private var _language: MutableStateFlow<String?> = MutableStateFlow(null)
    val language: StateFlow<String?> = _language
    private var _loggedIn: MutableStateFlow<String?> = MutableStateFlow(null)
    val loggedIn: StateFlow<String?> = _loggedIn
    private var _normalizeVolume: MutableStateFlow<String?> = MutableStateFlow(null)
    val normalizeVolume: StateFlow<String?> = _normalizeVolume
    private var _skipSilent: MutableStateFlow<String?> = MutableStateFlow(null)
    val skipSilent: StateFlow<String?> = _skipSilent
    private var _savedPlaybackState: MutableStateFlow<String?> = MutableStateFlow(null)
    val savedPlaybackState: StateFlow<String?> = _savedPlaybackState
    private var _saveRecentSongAndQueue: MutableStateFlow<String?> = MutableStateFlow(null)
    val saveRecentSongAndQueue: StateFlow<String?> = _saveRecentSongAndQueue
    private var _lastCheckForUpdate: MutableStateFlow<String?> = MutableStateFlow(null)
    val lastCheckForUpdate: StateFlow<String?> = _lastCheckForUpdate
    private var _sponsorBlockEnabled: MutableStateFlow<String?> = MutableStateFlow(null)
    val sponsorBlockEnabled: StateFlow<String?> = _sponsorBlockEnabled
    private var _sponsorBlockCategories: MutableStateFlow<ArrayList<String>?> =
        MutableStateFlow(null)
    val sponsorBlockCategories: StateFlow<ArrayList<String>?> = _sponsorBlockCategories
    private var _sendBackToGoogle: MutableStateFlow<String?> = MutableStateFlow(null)
    val sendBackToGoogle: StateFlow<String?> = _sendBackToGoogle
    private var _mainLyricsProvider: MutableStateFlow<String?> = MutableStateFlow(null)
    val mainLyricsProvider: StateFlow<String?> = _mainLyricsProvider

    private var _translationLanguage: MutableStateFlow<String?> = MutableStateFlow(null)
    val translationLanguage: StateFlow<String?> = _translationLanguage
    private var _useTranslation: MutableStateFlow<String?> = MutableStateFlow(null)
    val useTranslation: StateFlow<String?> = _useTranslation
    private var _playerCacheLimit: MutableStateFlow<Int?> = MutableStateFlow(null)
    val playerCacheLimit: StateFlow<Int?> = _playerCacheLimit
    private var _playVideoInsteadOfAudio: MutableStateFlow<String?> = MutableStateFlow(null)
    val playVideoInsteadOfAudio: StateFlow<String?> = _playVideoInsteadOfAudio

    private var _radioAudioOnly: MutableStateFlow<String?> = MutableStateFlow(null)
    val radioAudioOnly: StateFlow<String?> = _radioAudioOnly
    private var _videoQuality: MutableStateFlow<String?> = MutableStateFlow(null)
    val videoQuality: StateFlow<String?> = _videoQuality
    private var _thumbCacheSize = MutableStateFlow<Long?>(null)
    val thumbCacheSize: StateFlow<Long?> = _thumbCacheSize
    private var _canvasCacheSize: MutableStateFlow<Long?> = MutableStateFlow(null)
    val canvasCacheSize: StateFlow<Long?> = _canvasCacheSize
    private var _translucentBottomBar: MutableStateFlow<String?> = MutableStateFlow(null)
    val translucentBottomBar: StateFlow<String?> = _translucentBottomBar
    private var _usingProxy = MutableStateFlow(false)
    val usingProxy: StateFlow<Boolean> = _usingProxy
    private var _proxyType = MutableStateFlow(DataStoreManager.ProxyType.PROXY_TYPE_HTTP)
    val proxyType: StateFlow<DataStoreManager.ProxyType> = _proxyType
    private var _proxyHost = MutableStateFlow("")
    val proxyHost: StateFlow<String> = _proxyHost
    private var _proxyPort = MutableStateFlow(8000)
    val proxyPort: StateFlow<Int> = _proxyPort
    private var _proxyUsername = MutableStateFlow("")
    val proxyUsername: StateFlow<String> = _proxyUsername
    private var _proxyPassword = MutableStateFlow("")
    val proxyPassword: StateFlow<String> = _proxyPassword
    private var _autoCheckUpdate = MutableStateFlow(false)
    val autoCheckUpdate: StateFlow<Boolean> = _autoCheckUpdate
    private var _updateChannel: MutableStateFlow<String> = MutableStateFlow(DataStoreManager.GITHUB)
    val updateChannel: StateFlow<String> = _updateChannel
    private val _aiProvider = MutableStateFlow<String>(DataStoreManager.AI_PROVIDER_OPENAI)
    val aiProvider: StateFlow<String> = _aiProvider
    private val _isHasApiKey = MutableStateFlow<Boolean>(false)
    val isHasApiKey: StateFlow<Boolean> = _isHasApiKey
    private val _useAITranslation = MutableStateFlow<Boolean>(false)
    val useAITranslation: StateFlow<Boolean> = _useAITranslation
    private val _customModelId = MutableStateFlow<String>("")
    val customModelId: StateFlow<String> = _customModelId
    private val _customOpenAIBaseUrl = MutableStateFlow<String>("")
    val customOpenAIBaseUrl: StateFlow<String> = _customOpenAIBaseUrl
    private val _customOpenAIHeaders = MutableStateFlow<String>("")
    val customOpenAIHeaders: StateFlow<String> = _customOpenAIHeaders
    private val _crossfadeEnabled = MutableStateFlow<Boolean>(false)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled
    private val _crossfadeDuration = MutableStateFlow<Int>(5000)
    val crossfadeDuration: StateFlow<Int> = _crossfadeDuration
    private val _crossfadeDjMode = MutableStateFlow<Boolean>(true)
    val crossfadeDjMode: StateFlow<Boolean> = _crossfadeDjMode
    private val _crossfadeSkipAlbum = MutableStateFlow<Boolean>(false)
    val crossfadeSkipAlbum: StateFlow<Boolean> = _crossfadeSkipAlbum
    private val _autoDownloadLikedSongs = MutableStateFlow<Boolean>(false)
    val autoDownloadLikedSongs: StateFlow<Boolean> = _autoDownloadLikedSongs
    private val _youtubeSubtitleLanguage = MutableStateFlow<String>("")
    val youtubeSubtitleLanguage: StateFlow<String> = _youtubeSubtitleLanguage

    private var _helpBuildLyricsDatabase: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val helpBuildLyricsDatabase: StateFlow<Boolean> = _helpBuildLyricsDatabase
    private var _contributor: MutableStateFlow<Pair<String, String>> = MutableStateFlow(Pair("", ""))
    val contributor: StateFlow<Pair<String, String>> = _contributor

    private var _backupDownloaded: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val backupDownloaded: StateFlow<Boolean> = _backupDownloaded

    private var _enableLiquidGlass: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val enableLiquidGlass: StateFlow<Boolean> = _enableLiquidGlass

    private val _explicitContentEnabled = MutableStateFlow(false)
    val explicitContentEnabled: StateFlow<Boolean> = _explicitContentEnabled

    private val _discordLoggedIn = MutableStateFlow(false)
    val discordLoggedIn: StateFlow<Boolean> = _discordLoggedIn

    private val _richPresenceEnabled = MutableStateFlow(false)
    val richPresenceEnabled: StateFlow<Boolean> = _richPresenceEnabled

    /**
     * False in a FOSS build, and in a full build with no API key in `local.properties`. The whole
     * Last.fm block in settings is hidden when it is false, rather than offering a login that
     * could never succeed.
     */
    val lastfmAvailable: Boolean = isLastfmAvailable()

    private val _lastfmUsername = MutableStateFlow("")
    val lastfmUsername: StateFlow<String> = _lastfmUsername

    private val _lastfmLoggedIn = MutableStateFlow(false)
    val lastfmLoggedIn: StateFlow<Boolean> = _lastfmLoggedIn

    private val _lastfmScrobbleEnabled = MutableStateFlow(false)
    val lastfmScrobbleEnabled: StateFlow<Boolean> = _lastfmScrobbleEnabled

    private val _keepServiceAlive = MutableStateFlow<Boolean>(false)
    val keepServiceAlive: StateFlow<Boolean> = _keepServiceAlive

    private val _keepYouTubePlaylistOffline = MutableStateFlow<Boolean>(false)
    val keepYouTubePlaylistOffline: StateFlow<Boolean> = _keepYouTubePlaylistOffline

    private val _combineLocalAndYouTubeLiked = MutableStateFlow<Boolean>(false)
    val combineLocalAndYouTubeLiked: StateFlow<Boolean> = _combineLocalAndYouTubeLiked

    private val _downloadQuality = MutableStateFlow<String?>(null)
    val downloadQuality: StateFlow<String?> = _downloadQuality

    private val _videoDownloadQuality = MutableStateFlow<String?>(null)
    val videoDownloadQuality: StateFlow<String?> = _videoDownloadQuality

    private val _localTrackingEnabled = MutableStateFlow<Boolean>(false)
    val localTrackingEnabled: StateFlow<Boolean> = _localTrackingEnabled

    private val _blogNotificationEnabled = MutableStateFlow(true)
    val blogNotificationEnabled: StateFlow<Boolean> = _blogNotificationEnabled

    // Auto Backup
    private val _autoBackupEnabled = MutableStateFlow<Boolean>(false)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled

    private val _autoBackupFrequency = MutableStateFlow<String>(DataStoreManager.AUTO_BACKUP_FREQUENCY_DAILY)
    val autoBackupFrequency: StateFlow<String> = _autoBackupFrequency

    private val _autoBackupMaxFiles = MutableStateFlow<Int>(5)
    val autoBackupMaxFiles: StateFlow<Int> = _autoBackupMaxFiles

    private val _autoBackupLastTime = MutableStateFlow<Long>(0L)
    val autoBackupLastTime: StateFlow<Long> = _autoBackupLastTime

    private var _alertData: MutableStateFlow<SettingAlertState?> = MutableStateFlow(null)
    val alertData: StateFlow<SettingAlertState?> = _alertData

    private var _basicAlertData: MutableStateFlow<SettingBasicAlertState?> = MutableStateFlow(null)
    val basicAlertData: StateFlow<SettingBasicAlertState?> = _basicAlertData

    // Fraction of storage
    private var _fraction: MutableStateFlow<SettingsStorageSectionFraction> =
        MutableStateFlow(
            SettingsStorageSectionFraction(),
        )
    val fraction: StateFlow<SettingsStorageSectionFraction> = _fraction

    // Biến để lưu trữ và hiển thị trạng thái killServiceOnExit
    private var _killServiceOnExit: MutableStateFlow<String?> = MutableStateFlow(null)
    val killServiceOnExit: StateFlow<String?> = _killServiceOnExit

    init {
        getYoutubeSubtitleLanguage()
        getHelpBuildLyricsDatabase()
        viewModelScope.launch {
            enableLiquidGlass.collect {
                if (getPlatform() != Platform.Android && it) {
                    setEnableLiquidGlass(false)
                }
            }
        }
    }

    fun getData() {
        getLocation()
        getLanguage()
        getQuality()
        getPlayerCacheSize()
        getDownloadedCacheSize()
        getPlayerCacheLimit()
        getLoggedIn()
        getNormalizeVolume()
        getSkipSilent()
        getSavedPlaybackState()
        getSendBackToGoogle()
        getSaveRecentSongAndQueue()
        getLastCheckForUpdate()
        getSponsorBlockEnabled()
        getSponsorBlockCategories()
        getTranslationLanguage()
        getYoutubeSubtitleLanguage()
        getLyricsProvider()
        getUseTranslation()
        getPlayVideoInsteadOfAudio()
        getRadioAudioOnly()
        getVideoQuality()
        getSpotifyLogIn()
        getSpotifyLyrics()
        getSyncFollowToYouTube()
        getEqualizer()
        getSpotifyCanvas()
        getUsingProxy()
        getCanvasCache()
        getTranslucentBottomBar()
        getAutoCheckUpdate()
        getAIProvider()
        getAIApiKey()
        getAITranslation()
        getCustomModelId()
        getCustomOpenAIBaseUrl()
        getCustomOpenAIHeaders()
        getKillServiceOnExit()
        getCrossfadeEnabled()
        getCrossfadeDuration()
        getCrossfadeDjMode()
        getCrossfadeSkipAlbum()
        getAutoDownloadLikedSongs()
        getContributorNameAndEmail()
        getBackupDownloaded()
        getUpdateChannel()
        getEnableLiquidGlass()
        getExplicitContentEnabled()
        getDiscordLoggedIn()
        getDiscordRichPresenceEnabled()
        getLastfmSession()
        getLastfmScrobbleEnabled()
        getKeepServiceAlive()
        getKeepYouTubePlaylistOffline()
        getCombineLocalAndYouTubeLiked()
        getDownloadQuality()
        getVideoDownloadQuality()
        getLocalTrackingEnabled()
        getBlogNotificationEnabled()
        getAutoBackupEnabled()
        getAutoBackupFrequency()
        getAutoBackupMaxFiles()
        getAutoBackupLastTime()
        viewModelScope.launch {
            calculateDataFraction(
                cacheRepository,
            )?.let {
                _fraction.value = it
            }
        }
    }

    private fun getLocalTrackingEnabled() {
        viewModelScope.launch {
            dataStoreManager.localTrackingEnabled.collect { enabled ->
                _localTrackingEnabled.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setLocalTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setLocalTrackingEnabled(enabled)
            getLocalTrackingEnabled()
        }
    }

    private fun getBlogNotificationEnabled() {
        viewModelScope.launch {
            dataStoreManager.blogNotificationEnabled.collect { enabled ->
                _blogNotificationEnabled.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setBlogNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setBlogNotificationEnabled(enabled)
            getBlogNotificationEnabled()
        }
    }

    private fun getDownloadQuality() {
        viewModelScope.launch {
            dataStoreManager.downloadQuality.collect { quality ->
                _downloadQuality.emit(QUALITY.normalize(quality))
            }
        }
    }

    fun setDownloadQuality(quality: String) {
        viewModelScope.launch {
            dataStoreManager.setDownloadQuality(quality)
            getDownloadQuality()
        }
    }

    private fun getVideoDownloadQuality() {
        viewModelScope.launch {
            dataStoreManager.videoDownloadQuality.collect { videoQuality ->
                when (videoQuality) {
                    VIDEO_QUALITY.items[0].toString() -> _videoDownloadQuality.emit(VIDEO_QUALITY.items[0].toString())
                    VIDEO_QUALITY.items[1].toString() -> _videoDownloadQuality.emit(VIDEO_QUALITY.items[1].toString())
                    VIDEO_QUALITY.items[2].toString() -> _videoDownloadQuality.emit(VIDEO_QUALITY.items[2].toString())
                }
            }
        }
    }

    fun setVideoDownloadQuality(quality: String) {
        viewModelScope.launch {
            if (VIDEO_QUALITY.items.contains(quality)) {
                dataStoreManager.setVideoDownloadQuality(quality)
            }
            getVideoDownloadQuality()
        }
    }

    private fun getKeepYouTubePlaylistOffline() {
        viewModelScope.launch {
            dataStoreManager.keepYouTubePlaylistOffline.collect { keep ->
                _keepYouTubePlaylistOffline.value = keep == DataStoreManager.TRUE
            }
        }
    }

    fun setKeepYouTubePlaylistOffline(keep: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setKeepYouTubePlaylistOffline(keep)
            getKeepYouTubePlaylistOffline()
        }
    }

    private fun getCombineLocalAndYouTubeLiked() {
        viewModelScope.launch {
            dataStoreManager.combineLocalAndYouTubeLiked.collect { combine ->
                _combineLocalAndYouTubeLiked.value = combine == DataStoreManager.TRUE
            }
        }
    }

    fun setCombineLocalAndYouTubeLiked(combine: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setCombineLocalAndYouTubeLiked(combine)
            getCombineLocalAndYouTubeLiked()
        }
    }

    private fun getKeepServiceAlive() {
        viewModelScope.launch {
            dataStoreManager.keepServiceAlive.collect { keepServiceAlive ->
                _keepServiceAlive.value = keepServiceAlive == DataStoreManager.TRUE
            }
        }
    }

    fun setKeepServiceAlive(keepServiceAlive: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setKeepServiceAlive(keepServiceAlive)
            getKeepServiceAlive()
        }
    }

    private fun getCrossfadeEnabled() {
        viewModelScope.launch {
            dataStoreManager.crossfadeEnabled.collect { enabled ->
                _crossfadeEnabled.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setCrossfadeEnabled(enabled)
            getCrossfadeEnabled()
        }
    }

    private fun getCrossfadeDuration() {
        viewModelScope.launch {
            dataStoreManager.crossfadeDuration.collect { duration ->
                _crossfadeDuration.value = duration
            }
        }
    }

    fun setCrossfadeDuration(duration: Int) {
        viewModelScope.launch {
            dataStoreManager.setCrossfadeDuration(duration)
            getCrossfadeDuration()
        }
    }

    private fun getCrossfadeDjMode() {
        viewModelScope.launch {
            dataStoreManager.crossfadeDjMode.collect { enabled ->
                _crossfadeDjMode.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setCrossfadeDjMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setCrossfadeDjMode(enabled)
            getCrossfadeDjMode()
        }
    }

    private fun getCrossfadeSkipAlbum() {
        viewModelScope.launch {
            dataStoreManager.crossfadeSkipAlbum.collect { enabled ->
                _crossfadeSkipAlbum.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setCrossfadeSkipAlbum(enabled: Boolean) {
        viewModelScope.launch {
            // No re-read afterwards: the collector started in init never completes, so it already
            // picks this up. Calling the getter again would leave a second collector running for
            // the life of the ViewModel, one more per toggle.
            dataStoreManager.setCrossfadeSkipAlbum(enabled)
        }
    }

    private fun getAutoDownloadLikedSongs() {
        viewModelScope.launch {
            dataStoreManager.autoDownloadLikedSongs.collect { enabled ->
                _autoDownloadLikedSongs.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setAutoDownloadLikedSongs(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setAutoDownloadLikedSongs(enabled)
            // Switching it on also catches up on everything liked before now. Songs already
            // downloaded are skipped, so toggling it off and on again queues nothing.
            if (enabled) {
                val queued = songRepository.downloadAllLikedSongs()
                if (queued > 0) {
                    // Not BaseViewModel.getString: that one takes no format arguments and would
                    // leave the placeholder in the text.
                    makeToast(formatString(Res.string.downloading_liked_songs, queued))
                }
            }
        }
    }

    private fun getDiscordLoggedIn() {
        viewModelScope.launch {
            dataStoreManager.discordToken.collect { loggedIn ->
                _discordLoggedIn.value = loggedIn.isNotEmpty()
            }
        }
    }

    private fun getLastfmSession() {
        viewModelScope.launch {
            dataStoreManager.lastfmSessionKey.collect { key ->
                _lastfmLoggedIn.value = key.isNotEmpty()
            }
        }
        viewModelScope.launch {
            dataStoreManager.lastfmUsername.collect { username ->
                _lastfmUsername.value = username
            }
        }
    }

    fun logOutLastfm() {
        viewModelScope.launch {
            dataStoreManager.setLastfmSession(sessionKey = "", username = "")
            // Scrobbling is gated on the session, so logging out must clear it here — the same
            // teardown setSpotifyLogIn and logOutDiscord already do. Doing it from the Settings
            // row instead only works if the user happens to open Settings and scroll to that row.
            dataStoreManager.setLastfmScrobbleEnabled(false)
        }
    }

    private fun getLastfmScrobbleEnabled() {
        viewModelScope.launch {
            dataStoreManager.lastfmScrobbleEnabled.collect { enabled ->
                _lastfmScrobbleEnabled.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setLastfmScrobbleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setLastfmScrobbleEnabled(enabled)
        }
    }

    fun logOutDiscord() {
        viewModelScope.launch {
            dataStoreManager.setDiscordToken("")
            // Turn Rich Presence off on logout: without a token it can't run, and leaving the flag on
            // would both strand the user (the toggle greys out when logged out) and let the player keep
            // a dead RPC alive (issue #2157). The existing richPresenceEnabled collector refreshes the UI.
            dataStoreManager.setRichPresenceEnabled(false)
            delay(100)
            getDiscordLoggedIn()
        }
    }

    private fun getDiscordRichPresenceEnabled() {
        viewModelScope.launch {
            dataStoreManager.richPresenceEnabled.collect { enabled ->
                _richPresenceEnabled.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setDiscordRichPresenceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setRichPresenceEnabled(enabled)
            delay(100)
            getDiscordRichPresenceEnabled()
        }
    }

    private fun getExplicitContentEnabled() {
        viewModelScope.launch {
            dataStoreManager.explicitContentEnabled.collect { enabled ->
                _explicitContentEnabled.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setExplicitContentEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setExplicitContentEnabled(enabled)
            getExplicitContentEnabled()
        }
    }

    private fun getEnableLiquidGlass() {
        viewModelScope.launch {
            dataStoreManager.enableLiquidGlass.collect { enableLiquidGlass ->
                _enableLiquidGlass.value = enableLiquidGlass == DataStoreManager.TRUE
            }
        }
    }

    fun setEnableLiquidGlass(enableLiquidGlass: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setEnableLiquidGlass(enableLiquidGlass)
            getEnableLiquidGlass()
        }
    }

    private fun getUpdateChannel() {
        viewModelScope.launch {
            dataStoreManager.updateChannel.collect { channel ->
                _updateChannel.value = channel
            }
        }
    }

    fun setUpdateChannel(channel: String) {
        viewModelScope.launch {
            dataStoreManager.setUpdateChannel(channel)
            getUpdateChannel()
        }
    }

    private fun getBackupDownloaded() {
        viewModelScope.launch {
            dataStoreManager.backupDownloaded.collect { backupDownloaded ->
                _backupDownloaded.value = backupDownloaded == DataStoreManager.TRUE
            }
        }
    }

    fun setBackupDownloaded(backupDownloaded: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setBackupDownloaded(backupDownloaded)
            getBackupDownloaded()
        }
    }

    // Auto Backup functions
    private fun getAutoBackupEnabled() {
        viewModelScope.launch {
            dataStoreManager.autoBackupEnabled.collect { enabled ->
                _autoBackupEnabled.value = enabled == DataStoreManager.TRUE
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setAutoBackupEnabled(enabled)
            getAutoBackupEnabled()
        }
    }

    private fun getAutoBackupFrequency() {
        viewModelScope.launch {
            dataStoreManager.autoBackupFrequency.collect { frequency ->
                _autoBackupFrequency.value = frequency
            }
        }
    }

    fun setAutoBackupFrequency(frequency: String) {
        viewModelScope.launch {
            dataStoreManager.setAutoBackupFrequency(frequency)
            getAutoBackupFrequency()
        }
    }

    private fun getAutoBackupMaxFiles() {
        viewModelScope.launch {
            dataStoreManager.autoBackupMaxFiles.collect { max ->
                _autoBackupMaxFiles.value = max
            }
        }
    }

    fun setAutoBackupMaxFiles(max: Int) {
        viewModelScope.launch {
            dataStoreManager.setAutoBackupMaxFiles(max)
            getAutoBackupMaxFiles()
        }
    }

    private fun getAutoBackupLastTime() {
        viewModelScope.launch {
            dataStoreManager.autoBackupLastTime.collect { time ->
                _autoBackupLastTime.value = time
            }
        }
    }

    private fun getContributorNameAndEmail() {
        viewModelScope.launch {
            combine(dataStoreManager.contributorName, dataStoreManager.contributorEmail) { name, email ->
                name to email
            }.collect { contributor ->
                _contributor.value = contributor
            }
        }
    }

    fun setContributorName(name: String) {
        viewModelScope.launch {
            dataStoreManager.setContributorLyricsDatabase(name to contributor.value.second)
            getContributorNameAndEmail()
        }
    }

    fun setContributorEmail(email: String) {
        viewModelScope.launch {
            dataStoreManager.setContributorLyricsDatabase(contributor.value.first to email)
            getContributorNameAndEmail()
        }
    }

    private fun getCustomModelId() {
        viewModelScope.launch {
            dataStoreManager.customModelId.collect { customModelId ->
                _customModelId.value = customModelId
            }
        }
    }

    fun setCustomModelId(modelId: String) {
        viewModelScope.launch {
            dataStoreManager.setCustomModelId(modelId)
            getCustomModelId()
        }
    }

    private fun getCustomOpenAIBaseUrl() {
        viewModelScope.launch {
            dataStoreManager.customOpenAIBaseUrl.collect { baseUrl ->
                _customOpenAIBaseUrl.value = baseUrl
            }
        }
    }

    fun setCustomOpenAIBaseUrl(baseUrl: String) {
        viewModelScope.launch {
            dataStoreManager.setCustomOpenAIBaseUrl(baseUrl)
            getCustomOpenAIBaseUrl()
        }
    }

    private fun getCustomOpenAIHeaders() {
        viewModelScope.launch {
            dataStoreManager.customOpenAIHeaders.collect { headers ->
                _customOpenAIHeaders.value = headers
            }
        }
    }

    fun setCustomOpenAIHeaders(headers: String) {
        viewModelScope.launch {
            dataStoreManager.setCustomOpenAIHeaders(headers)
            getCustomOpenAIHeaders()
        }
    }

    private fun getAIProvider() {
        viewModelScope.launch {
            dataStoreManager.aiProvider.collect { aiProvider ->
                _aiProvider.value = aiProvider
            }
        }
    }

    fun setAIProvider(provider: String) {
        viewModelScope.launch {
            dataStoreManager.setAIProvider(provider)
            getAIProvider()
        }
    }

    private fun getAITranslation() {
        viewModelScope.launch {
            dataStoreManager.useAITranslation.collect { useAITranslation ->
                _useAITranslation.value = useAITranslation == DataStoreManager.TRUE
            }
        }
    }

    fun setAITranslation(useAITranslation: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setUseAITranslation(useAITranslation)
            getAITranslation()
        }
    }

    private fun getAIApiKey() {
        viewModelScope.launch {
            dataStoreManager.aiApiKey.collect { aiApiKey ->
                if (aiApiKey.isNotEmpty()) {
                    _isHasApiKey.value = true
                    log("getAIApiKey: $aiApiKey")
                } else {
                    _isHasApiKey.value = false
                }
            }
        }
    }

    fun setAIApiKey(apiKey: String) {
        viewModelScope.launch {
            dataStoreManager.setAIApiKey(apiKey)
            // An empty key IS the sign-out here — isHasApiKey is derived from aiApiKey.isNotEmpty()
            // — so AI translation, which is gated on it, has to come off with it.
            if (apiKey.isEmpty()) {
                dataStoreManager.setUseAITranslation(false)
            }
            getAIApiKey()
        }
    }

    private fun getAutoCheckUpdate() {
        viewModelScope.launch {
            dataStoreManager.autoCheckForUpdates.collect { autoCheckUpdate ->
                _autoCheckUpdate.value = autoCheckUpdate == DataStoreManager.TRUE
            }
        }
    }

    fun setAutoCheckUpdate(autoCheckUpdate: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setAutoCheckForUpdates(autoCheckUpdate)
            getAutoCheckUpdate()
        }
    }

    private fun getCanvasCache() {
        viewModelScope.launch {
            _canvasCacheSize.value = cacheRepository.getCacheSize(Config.CANVAS_CACHE)
        }
    }

    fun setAlertData(alertData: SettingAlertState?) {
        _alertData.value = alertData
    }

    fun setBasicAlertData(alertData: SettingBasicAlertState?) {
        _basicAlertData.value = alertData
    }

    /**
     * Asks before signing out of a linked account.
     *
     * Every one of these rows sits inside a long settings list and does its work on a single tap,
     * with no undo — and getting back in is not symmetric with getting out: Last.fm sends the user
     * through a browser again, YouTube and Spotify through a full web login. The confirmation is
     * cheap next to that.
     *
     * @param confirmLabel names the service on the confirming button, because the dialog is the
     * only thing on screen at that moment and "Log out" alone does not say out of what.
     */
    fun confirmLogOut(
        confirmLabel: String,
        onConfirm: () -> Unit,
    ) {
        viewModelScope.launch {
            setBasicAlertData(
                SettingBasicAlertState(
                    title = getString(Res.string.warning),
                    message = getString(Res.string.log_out_confirm_message),
                    confirm = confirmLabel to onConfirm,
                    dismiss = getString(Res.string.cancel),
                ),
            )
        }
    }

    private fun getUsingProxy() {
        viewModelScope.launch {
            dataStoreManager.usingProxy.collectLatest { usingProxy ->
                if (usingProxy == DataStoreManager.TRUE) {
                    getProxy()
                }
                _usingProxy.value = usingProxy == DataStoreManager.TRUE
            }
        }
    }

    fun setUsingProxy(usingProxy: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setUsingProxy(usingProxy)
            getUsingProxy()
            getProxy()
        }
    }

    private fun getProxy() {
        viewModelScope.launch {
            val host =
                launch {
                    dataStoreManager.proxyHost.collect {
                        _proxyHost.value = it
                    }
                }
            val port =
                launch {
                    dataStoreManager.proxyPort.collect {
                        _proxyPort.value = it
                    }
                }
            val type =
                launch {
                    dataStoreManager.proxyType.collect {
                        _proxyType.value = it
                        log("getProxy: $it")
                    }
                }
            val username =
                launch {
                    dataStoreManager.proxyUsername.collect {
                        _proxyUsername.value = it
                    }
                }
            val password =
                launch {
                    dataStoreManager.proxyPassword.collect {
                        _proxyPassword.value = it
                    }
                }
            host.join()
            port.join()
            type.join()
            username.join()
            password.join()
        }
    }

    fun setProxy(
        proxyType: DataStoreManager.ProxyType,
        host: String,
        port: Int,
    ) {
        log("setProxy: $proxyType, $host, $port")
        viewModelScope.launch {
            dataStoreManager.setProxyType(proxyType)
            dataStoreManager.setProxyHost(host)
            dataStoreManager.setProxyPort(port)
        }
    }

    fun setProxyCredentials(
        username: String,
        password: String,
    ) {
        log("setProxyCredentials: username=$username")
        viewModelScope.launch {
            dataStoreManager.setProxyUsername(username)
            dataStoreManager.setProxyPassword(password)
        }
    }

    fun getTranslucentBottomBar() {
        viewModelScope.launch {
            dataStoreManager.translucentBottomBar.collect { translucentBottomBar ->
                _translucentBottomBar.emit(translucentBottomBar)
            }
        }
    }

    fun setTranslucentBottomBar(translucentBottomBar: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setTranslucentBottomBar(translucentBottomBar)
            getTranslucentBottomBar()
        }
    }

    fun getThumbCacheSize(context: PlatformContext) {
        viewModelScope.launch {
            val diskCache = SingletonImageLoader.get(context).diskCache
            _thumbCacheSize.emit(diskCache?.size)
        }
    }

    fun getVideoQuality() {
        viewModelScope.launch {
            dataStoreManager.videoQuality.collect { videoQuality ->
                when (videoQuality) {
                    VIDEO_QUALITY.items[0].toString() -> _videoQuality.emit(VIDEO_QUALITY.items[0].toString())
                    VIDEO_QUALITY.items[1].toString() -> _videoQuality.emit(VIDEO_QUALITY.items[1].toString())
                    VIDEO_QUALITY.items[2].toString() -> _videoQuality.emit(VIDEO_QUALITY.items[2].toString())
                }
            }
        }
    }

    fun getTranslationLanguage() {
        viewModelScope.launch {
            dataStoreManager.translationLanguage.collect { translationLanguage ->
                _translationLanguage.emit(translationLanguage)
            }
        }
    }

    fun setTranslationLanguage(language: String) {
        viewModelScope.launch {
            dataStoreManager.setTranslationLanguage(language)
            getTranslationLanguage()
        }
    }

    fun getUseTranslation() {
        viewModelScope.launch {
            dataStoreManager.enableTranslateLyric.collect { useTranslation ->
                _useTranslation.emit(useTranslation)
            }
        }
    }

    fun setUseTranslation(useTranslation: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setEnableTranslateLyric(useTranslation)
            getUseTranslation()
        }
    }

    fun getLyricsProvider() {
        viewModelScope.launch {
            dataStoreManager.lyricsProvider.collect { mainLyricsProvider ->
                _mainLyricsProvider.emit(mainLyricsProvider)
            }
        }
    }

    fun setLyricsProvider(provider: String) {
        viewModelScope.launch {
            dataStoreManager.setLyricsProvider(provider)
            getLyricsProvider()
        }
    }

    /**
     * Fetches the Japanese romanization dictionary if this platform needs one and does not have
     * it yet. Called from the settings screen every time a saved selection includes Japanese, so
     * a FAILED attempt is retried by simply confirming the dialog again; READY and an already
     * running download make this a no-op. Progress and outcome live in [japaneseDictionaryState],
     * which the romanization row's subtitle watches.
     */
    fun downloadJapaneseDictionaryIfNeeded() {
        val state = japaneseDictionaryState.value
        if (state == RomanizationDictionaryState.READY || state == RomanizationDictionaryState.DOWNLOADING) return
        viewModelScope.launch {
            lyricsRomanizerRepository.downloadJapaneseDictionary()
            // The repository settles the state before returning, so reading it back here is the
            // completion signal — no separate callback needed for the two toasts.
            when (japaneseDictionaryState.value) {
                RomanizationDictionaryState.READY ->
                    makeToast(getString(Res.string.romanization_japanese_dict_ready))
                RomanizationDictionaryState.FAILED ->
                    makeToast(getString(Res.string.romanization_japanese_dict_failed))
                else -> {}
            }
        }
    }

    fun getLocation() {
        viewModelScope.launch {
            dataStoreManager.location.collect { location ->
                _location.emit(location)
            }
        }
    }

    fun getLoggedIn() {
        viewModelScope.launch {
            dataStoreManager.loggedIn.collect { loggedIn ->
                _loggedIn.emit(loggedIn)
            }
        }
    }

    fun changeLocation(location: String) {
        viewModelScope.launch {
            dataStoreManager.setLocation(location)
            getLocation()
        }
    }

    fun getSaveRecentSongAndQueue() {
        viewModelScope.launch {
            dataStoreManager.saveRecentSongAndQueue.collect { saved ->
                _saveRecentSongAndQueue.emit(saved)
            }
        }
    }

    fun getLastCheckForUpdate() {
        viewModelScope.launch {
            dataStoreManager.getString("CheckForUpdateAt").first().let { lastCheckForUpdate ->
                _lastCheckForUpdate.emit(lastCheckForUpdate)
            }
        }
    }

    fun getSponsorBlockEnabled() {
        viewModelScope.launch {
            dataStoreManager.sponsorBlockEnabled.first().let { enabled ->
                _sponsorBlockEnabled.emit(enabled)
            }
        }
    }

    fun setSponsorBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSponsorBlockEnabled(enabled)
            getSponsorBlockEnabled()
        }
    }

    fun getPlayVideoInsteadOfAudio() {
        viewModelScope.launch {
            dataStoreManager.watchVideoInsteadOfPlayingAudio.collect { playVideoInsteadOfAudio ->
                _playVideoInsteadOfAudio.emit(playVideoInsteadOfAudio)
            }
        }
    }

    fun setPlayVideoInsteadOfAudio(playVideoInsteadOfAudio: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setWatchVideoInsteadOfPlayingAudio(playVideoInsteadOfAudio)
            getPlayVideoInsteadOfAudio()
        }
    }

    fun getRadioAudioOnly() {
        viewModelScope.launch {
            dataStoreManager.radioAudioOnly.collect { radioAudioOnly ->
                _radioAudioOnly.emit(radioAudioOnly)
            }
        }
    }

    /**
     * No re-collect after writing, unlike the settings above: [getRadioAudioOnly] already collects
     * the DataStore flow, which emits again on every write. Calling the getter here would only
     * stack a second collector on each toggle.
     */
    fun setRadioAudioOnly(audioOnly: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setRadioAudioOnly(audioOnly)
        }
    }

    fun getSponsorBlockCategories() {
        viewModelScope.launch {
            dataStoreManager.getSponsorBlockCategories().let {
                log("getSponsorBlockCategories: $it", LogLevel.WARN)
                _sponsorBlockCategories.emit(it)
            }
        }
    }

    fun setSponsorBlockCategories(list: ArrayList<String>) {
        log("setSponsorBlockCategories: $list", LogLevel.WARN)
        viewModelScope.launch {
            runBlocking(Dispatchers.IO) {
                dataStoreManager.setSponsorBlockCategories(list)
            }
            getSponsorBlockCategories()
        }
    }

    private var _quality: MutableStateFlow<String?> = MutableStateFlow(null)
    val quality: StateFlow<String?> = _quality

    fun getQuality() {
        viewModelScope.launch {
            dataStoreManager.quality.collect { quality ->
                _quality.emit(QUALITY.normalize(quality))
            }
        }
    }

    fun changeVideoQuality(item: String) {
        viewModelScope.launch {
            if (VIDEO_QUALITY.items.contains(item)) {
                dataStoreManager.setVideoQuality(item)
            }
            getVideoQuality()
        }
    }

    fun changeQuality(qualityItem: String?) {
        viewModelScope.launch {
            log("changeQuality: $qualityItem")
            dataStoreManager.setQuality(qualityItem ?: QUALITY.items.first().toString())
            getQuality()
        }
    }

    private val _cacheSize: MutableStateFlow<Long?> = MutableStateFlow(null)
    var cacheSize: StateFlow<Long?> = _cacheSize

    fun getPlayerCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = cacheRepository.getCacheSize(Config.PLAYER_CACHE)
        }
    }

    fun clearPlayerCache() {
        viewModelScope.launch {
            cacheRepository.clearCache(Config.PLAYER_CACHE)
            makeToast(getString(Res.string.clear_player_cache))
            getPlayerCacheSize()
        }
    }

    private val _downloadedCacheSize: MutableStateFlow<Long?> = MutableStateFlow(null)
    var downloadedCacheSize: StateFlow<Long?> = _downloadedCacheSize

    fun getDownloadedCacheSize() {
        viewModelScope.launch {
            _downloadedCacheSize.value = cacheRepository.getCacheSize(Config.DOWNLOAD_CACHE)
        }
    }

    fun clearDownloadedCache() {
        viewModelScope.launch {
            cacheRepository.clearCache(Config.DOWNLOAD_CACHE)
            songRepository.getDownloadedSongs().singleOrNull()?.let { songs ->
                songs.forEach { song ->
                    songRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
            makeToast(getString(Res.string.clear_downloaded_cache))
            getDownloadedCacheSize()
            downloadUtils.removeAllDownloads()
        }
    }

    fun clearCanvasCache() {
        viewModelScope.launch {
            cacheRepository.clearCache(Config.CANVAS_CACHE)
            makeToast(getString(Res.string.clear_canvas_cache))
            getCanvasCache()
        }
    }

    fun clearListeningHistory() {
        viewModelScope.launch {
            // The sweep walks every container table and then the whole song table, and finishes with
            // a VACUUM — seconds of work on a large library, and the screen would look frozen.
            showLoadingDialog(getString(Res.string.clear_listening_history))
            val removed =
                try {
                    // NonCancellable because the sweep is only coherent once it has finished: the
                    // cached containers are deleted first and the songs they were holding alive
                    // last, so a run stopped in between — the user leaving Settings is enough to do
                    // it — has already dropped every cached playlist, album and artist and left the
                    // songs behind. Nothing resumes it afterwards, so it has to run to the end.
                    withContext(NonCancellable) { songRepository.clearHistoryAndOrphanedSongs() }
                } catch (e: CancellationException) {
                    // Not a failure, and it must not be swallowed: catching it breaks structured
                    // concurrency, and it would put the generic error toast in front of a user whose
                    // sweep had in fact just completed, inviting them to run the whole thing again.
                    throw e
                } catch (e: Throwable) {
                    Logger.e(tag, "clearHistoryAndOrphanedSongs failed: ${e.stackTraceToString()}")
                    null
                } finally {
                    // In a finally so a failure — or the user leaving the screen — cannot strand the
                    // dialog on top of the app with no way to dismiss it.
                    hideLoadingDialog()
                }
            if (removed == null) {
                makeToast(getString(Res.string.error))
                return@launch
            }
            makeToast(formatString(Res.string.clear_listening_history_done, removed))
            // Only the database slice of the storage bar moved; getData() would also restart every
            // collecting getter it owns.
            calculateDataFraction(cacheRepository)?.let { _fraction.value = it }
        }
    }

    fun backup(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                makeToast(getString(Res.string.backup_in_progress))
                withContext(Dispatchers.IO) {
                    backupNative(commonRepository, uri, backupDownloaded.value)
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    makeToast(getString(Res.string.backup_create_success))
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    it.printStackTrace()
                    makeToast(getString(Res.string.backup_create_failed))
                }
            }
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch {
            makeToast(getString(Res.string.restore_in_progress))
            withContext(Dispatchers.IO) {
                runCatching {
                    restoreNative(commonRepository, uri) {
                        getData()
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        it.printStackTrace()
                        makeToast(getString(Res.string.restore_failed))
                    }
                }
            }
        }
    }

    fun getLanguage() {
        viewModelScope.launch {
            dataStoreManager.getString(SELECTED_LANGUAGE).collect { language ->
                _language.emit(language)
            }
        }
    }

    fun changeLanguage(code: String) {
        viewModelScope.launch {
            dataStoreManager.putString(SELECTED_LANGUAGE, code)
            Logger.w("SettingsViewModel", "changeLanguage: $code")
            getLanguage()
            changeLanguageNative(code)
        }
    }

    fun getNormalizeVolume() {
        viewModelScope.launch {
            dataStoreManager.normalizeVolume.collect { normalizeVolume ->
                _normalizeVolume.emit(normalizeVolume)
            }
        }
    }

    fun setNormalizeVolume(normalizeVolume: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setNormalizeVolume(normalizeVolume)
            getNormalizeVolume()
        }
    }

    fun getSendBackToGoogle() {
        viewModelScope.launch {
            dataStoreManager.sendBackToGoogle.collect { sendBackToGoogle ->
                _sendBackToGoogle.emit(sendBackToGoogle)
            }
        }
    }

    fun setSendBackToGoogle(sendBackToGoogle: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSendBackToGoogle(sendBackToGoogle)
            getSendBackToGoogle()
        }
    }

    fun getSkipSilent() {
        viewModelScope.launch {
            dataStoreManager.skipSilent.collect { skipSilent ->
                _skipSilent.emit(skipSilent)
            }
        }
    }

    fun setSkipSilent(skip: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSkipSilent(skip)
            getSkipSilent()
        }
    }

    fun getSavedPlaybackState() {
        viewModelScope.launch {
            dataStoreManager.saveStateOfPlayback.collect { savedPlaybackState ->
                _savedPlaybackState.emit(savedPlaybackState)
            }
        }
    }

    fun setSavedPlaybackState(savedPlaybackState: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSaveStateOfPlayback(savedPlaybackState)
            getSavedPlaybackState()
        }
    }

    fun setSaveLastPlayed(b: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSaveRecentSongAndQueue(b)
            getSaveRecentSongAndQueue()
        }
    }

    fun getPlayerCacheLimit() {
        viewModelScope.launch {
            dataStoreManager.maxSongCacheSize.collect {
                _playerCacheLimit.emit(it)
            }
        }
    }

    fun setPlayerCacheLimit(size: Int) {
        viewModelScope.launch {
            dataStoreManager.setMaxSongCacheSize(size)
            getPlayerCacheLimit()
        }
    }

    private var _googleAccounts: MutableStateFlow<LocalResource<List<GoogleAccountEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val googleAccounts: StateFlow<LocalResource<List<GoogleAccountEntity>>> = _googleAccounts

    fun getAllGoogleAccount() {
        Logger.w("getAllGoogleAccount", "getAllGoogleAccount: Go to function")
        viewModelScope.launch {
            _googleAccounts.emit(LocalResource.Loading())
            accountRepository.getGoogleAccounts().collectLatest { accounts ->
                Logger.w("getAllGoogleAccount", "getAllGoogleAccount: $accounts")
                if (!accounts.isNullOrEmpty()) {
                    _googleAccounts.emit(LocalResource.Success(accounts))
                } else {
                    if (loggedIn.value == DataStoreManager.TRUE) {
                        accountRepository
                            .getAccountInfo(
                                dataStoreManager.cookie.first(),
                            ).collect {
                                Logger.w("getAllGoogleAccount", "getAllGoogleAccount: $it")
                                if (it.isNotEmpty()) {
                                    dataStoreManager.putString("AccountName", it.first().name)
                                    dataStoreManager.putString(
                                        "AccountThumbUrl",
                                        it
                                            .first()
                                            .thumbnails
                                            .lastOrNull()
                                            ?.url ?: "",
                                    )
                                    accountRepository
                                        .insertGoogleAccount(
                                            GoogleAccountEntity(
                                                email = it.first().email,
                                                name = it.first().name,
                                                thumbnailUrl =
                                                    it
                                                        .first()
                                                        .thumbnails
                                                        .lastOrNull()
                                                        ?.url ?: "",
                                                cache = accountRepository.getYouTubeCookie(),
                                                pageId = it.first().pageId,
                                                isUsed = true,
                                            ),
                                        ).singleOrNull()
                                        ?.let { account ->
                                            Logger.w("getAllGoogleAccount", "inserted: $account")
                                        }
                                    getAllGoogleAccount()
                                } else {
                                    _googleAccounts.emit(LocalResource.Success(emptyList()))
                                }
                            }
                    } else {
                        _googleAccounts.emit(LocalResource.Success(emptyList()))
                    }
                }
            }
        }
    }

    suspend fun addAccount(
        cookie: String,
        netscapeCookie: String? = null,
    ): Boolean {
        val currentCookie = dataStoreManager.cookie.first()
        val currentPageId = dataStoreManager.pageId.first()
        val currentLoggedIn = dataStoreManager.loggedIn.first() == DataStoreManager.TRUE
        try {
            runBlocking {
                dataStoreManager.setCookie(cookie, "")
                dataStoreManager.setLoggedIn(true)
            }
            return accountRepository
                .getAccountInfo(
                    cookie,
                ).lastOrNull()
                ?.takeIf {
                    it.isNotEmpty()
                }?.let { accountInfoList ->
                    Logger.d("getAllGoogleAccount", "addAccount: $accountInfoList")
                    accountRepository.getGoogleAccounts().lastOrNull()?.forEach {
                        Logger.d("getAllGoogleAccount", "set used: $it start")
                        accountRepository
                            .updateGoogleAccountUsed(it.email, false)
                            .singleOrNull()
                            ?.let {
                                Logger.w("getAllGoogleAccount", "set used: $it")
                            }
                    }
                    dataStoreManager.putString("AccountName", accountInfoList.first().name)
                    dataStoreManager.putString(
                        "AccountThumbUrl",
                        accountInfoList
                            .first()
                            .thumbnails
                            .lastOrNull()
                            ?.url ?: "",
                    )
                    val cookieItem =
                        netscapeCookie ?: commonRepository
                            .getCookiesFromInternalDatabase(Config.YOUTUBE_MUSIC_MAIN_URL, getPackageName())
                            .toNetScapeString()
                    commonRepository.writeTextToFile(cookieItem, (getFileDir() + "/ytdlp-cookie.txt")).let {
                        Logger.d("getAllGoogleAccount", "addAccount: write cookie file: $it")
                    }
                    accountInfoList.forEachIndexed { index, account ->
                        accountRepository
                            .insertGoogleAccount(
                                GoogleAccountEntity(
                                    email = account.email,
                                    name = account.name,
                                    thumbnailUrl =
                                        account
                                            .thumbnails
                                            .lastOrNull()
                                            ?.url ?: "",
                                    cache = cookie,
                                    isUsed = index == 0,
                                    netscapeCookie = cookieItem,
                                    pageId = account.pageId,
                                ),
                            ).firstOrNull()
                            ?.let {
                                log("addAccount: $it", LogLevel.WARN)
                            }
                    }
                    dataStoreManager.setLoggedIn(true)
                    dataStoreManager.setCookie(cookie, accountInfoList.first().pageId)
                    getAllGoogleAccount()
                    getLoggedIn()
                    true
                } ?: run {
                Logger.w("getAllGoogleAccount", "addAccount: Account info is null")
                runBlocking {
                    dataStoreManager.setCookie(currentCookie, currentPageId)
                    dataStoreManager.setLoggedIn(currentLoggedIn)
                }
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e("getAllGoogleAccount", "addAccount: ${e.message}")
            runBlocking {
                dataStoreManager.setCookie(currentCookie, currentPageId)
                dataStoreManager.setLoggedIn(currentLoggedIn)
            }
            return false
        }
    }

    fun setUsedAccount(acc: GoogleAccountEntity?) {
        viewModelScope.launch {
            if (acc != null) {
                googleAccounts.value.data?.forEach {
                    accountRepository
                        .updateGoogleAccountUsed(it.email, false)
                        .singleOrNull()
                        ?.let {
                            Logger.w("getAllGoogleAccount", "set used: $it")
                        }
                }
                dataStoreManager.putString("AccountName", acc.name)
                dataStoreManager.putString("AccountThumbUrl", acc.thumbnailUrl)
                accountRepository
                    .updateGoogleAccountUsed(acc.email, true)
                    .singleOrNull()
                    ?.let {
                        Logger.w("getAllGoogleAccount", "set used: $it")
                    }
                acc.netscapeCookie?.let { commonRepository.writeTextToFile(it, (getFileDir() + "/ytdlp-cookie.txt")) }.let {
                    Logger.d("getAllGoogleAccount", "addAccount: write cookie file: $it")
                }
                dataStoreManager.setCookie(acc.cache ?: "", acc.pageId)
                dataStoreManager.setLoggedIn(true)
                delay(500)
                getAllGoogleAccount()
                getLoggedIn()
            } else {
                googleAccounts.value.data?.forEach {
                    accountRepository
                        .updateGoogleAccountUsed(it.email, false)
                        .singleOrNull()
                        ?.let {
                            Logger.w("getAllGoogleAccount", "set used: $it")
                        }
                }
                dataStoreManager.putString("AccountName", "")
                dataStoreManager.putString("AccountThumbUrl", "")
                dataStoreManager.setLoggedIn(false)
                dataStoreManager.setCookie("", null)
                // Mirroring follows needs a session to write to, so signing out clears the flag
                // here rather than from the Settings row — same teardown as setSpotifyLogIn and
                // logOutDiscord. Only this branch: acc != null is switching account, not logout.
                dataStoreManager.setSyncFollowToYouTube(false)
                delay(500)
                getAllGoogleAccount()
                getLoggedIn()
            }
        }
    }

    fun logOutAllYouTube() {
        viewModelScope.launch {
            googleAccounts.value.data?.forEach { account ->
                accountRepository.deleteGoogleAccount(account.email)
            }
            dataStoreManager.putString("AccountName", "")
            dataStoreManager.putString("AccountThumbUrl", "")
            dataStoreManager.setLoggedIn(false)
            dataStoreManager.setCookie("", null)
            dataStoreManager.setSyncFollowToYouTube(false)
            delay(500)
            getAllGoogleAccount()
            getLoggedIn()
        }
    }

    @ExperimentalCoilApi
    fun clearThumbnailCache(platformContext: PlatformContext) {
        viewModelScope.launch {
            SingletonImageLoader.get(platformContext).diskCache?.clear()
            makeToast(getString(Res.string.clear_thumbnail_cache))
            getThumbCacheSize(platformContext)
        }
    }

    private var _spotifyLogIn: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyLogIn: StateFlow<Boolean> = _spotifyLogIn

    fun getSpotifyLogIn() {
        viewModelScope.launch {
            dataStoreManager.spdc.collect { loggedIn ->
                if (loggedIn.isNotEmpty()) {
                    _spotifyLogIn.emit(true)
                } else {
                    _spotifyLogIn.emit(false)
                }
            }
        }
    }

    fun setSpotifyLogIn(loggedIn: Boolean) {
        viewModelScope.launch {
            _spotifyLogIn.emit(loggedIn)
            if (!loggedIn) {
                dataStoreManager.setSpdc("")
                // Logging out of Spotify must also tear down everything gated behind it. Otherwise the
                // lyrics/canvas flags stay stuck ON with no way to switch them off (the toggles grey out
                // when logged out) and stale tokens linger — issue #2064, same family as Discord #2157.
                dataStoreManager.setSpotifyLyrics(false)
                dataStoreManager.setSpotifyCanvas(false)
                dataStoreManager.setSpotifyClientToken("")
                dataStoreManager.setSpotifyClientTokenExpires(0)
                dataStoreManager.setSpotifyPersonalToken("")
                dataStoreManager.setSpotifyPersonalTokenExpires(0)
                delay(500)
            }
            getSpotifyLogIn()
        }
    }

    private var _equalizerEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setEqualizerEnabled(enabled)
        }
    }

    private var _equalizerBands: MutableStateFlow<List<Float>> = MutableStateFlow(List(EQUALIZER_BAND_COUNT) { 0f })
    val equalizerBands: StateFlow<List<Float>> = _equalizerBands

    private var _equalizerPreamp: MutableStateFlow<Float> = MutableStateFlow(0f)
    val equalizerPreamp: StateFlow<Float> = _equalizerPreamp

    /** Raw `"<label>\n<gains>"` of the last imported AutoEq profile; the UI decides if it still applies. */
    private var _equalizerAutoEqProfile: MutableStateFlow<String> = MutableStateFlow("")
    val equalizerAutoEqProfile: StateFlow<String> = _equalizerAutoEqProfile

    /**
     * Guards the collectors below against being started twice.
     *
     * Two callers ask for them: [getData], which every visit to the settings screen runs, and the
     * equalizer block itself, which asks on its own so it keeps working if it is ever hosted
     * anywhere else. Both land on this same view model, and the collectors live in
     * [viewModelScope] rather than in a composition — so without this, toggling the switch off and
     * on left another four behind every time, each re-reading the preference file for a value
     * three others were already publishing.
     */
    private var equalizerCollectorsStarted = false

    fun getEqualizer() {
        if (equalizerCollectorsStarted) return
        equalizerCollectorsStarted = true
        viewModelScope.launch {
            launch {
                dataStoreManager.equalizerEnabled.collect {
                    _equalizerEnabled.emit(it == DataStoreManager.TRUE)
                }
            }
            launch {
                dataStoreManager.equalizerBands.collect { stored ->
                    // Stored blank means flat. Short or malformed input is padded rather than
                    // rejected, so a curve saved by a future build with more bands still loads.
                    val parsed = stored.split(",").mapNotNull { it.trim().toFloatOrNull() }
                    _equalizerBands.emit(
                        List(EQUALIZER_BAND_COUNT) { parsed.getOrElse(it) { 0f } },
                    )
                }
            }
            launch {
                dataStoreManager.equalizerPreamp.collect { _equalizerPreamp.emit(it) }
            }
            launch {
                dataStoreManager.equalizerAutoEqProfile.collect { _equalizerAutoEqProfile.emit(it) }
            }
        }
    }

    /**
     * Store a whole curve at once.
     *
     * The whole list rather than one band at a time because a single drag sweeps across several
     * bands, and because the per-band version had to read [_equalizerBands] to rebuild the list —
     * a value that only updates once the write has been through storage, so two edits in quick
     * succession could reinstate the first band's old gain.
     */
    fun setEqualizerBands(bandsDb: List<Float>) {
        viewModelScope.launch {
            dataStoreManager.setEqualizerBands(bandsDb)
        }
    }

    /**
     * Move the curve and its preamp together.
     *
     * These are two separate preference keys, so the player does briefly see one of them applied
     * against the other's old value. The preamp goes first deliberately: that way the moment in
     * between is the new headroom under the old curve — quieter — rather than a freshly boosted
     * curve still running on the previous preset's headroom, which is the direction that clips.
     */
    fun applyEqualizerPreset(
        bandsDb: List<Float>,
        preampDb: Float,
    ) {
        viewModelScope.launch {
            dataStoreManager.setEqualizerPreamp(preampDb)
            dataStoreManager.setEqualizerBands(bandsDb)
        }
    }

    fun setEqualizerPreamp(preampDb: Float) {
        viewModelScope.launch {
            dataStoreManager.setEqualizerPreamp(preampDb)
        }
    }

    fun resetEqualizer() {
        viewModelScope.launch {
            dataStoreManager.setEqualizerBands(List(EQUALIZER_BAND_COUNT) { 0f })
            dataStoreManager.setEqualizerPreamp(0f)
        }
    }

    private var _syncFollowToYouTube: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val syncFollowToYouTube: StateFlow<Boolean> = _syncFollowToYouTube

    fun getSyncFollowToYouTube() {
        viewModelScope.launch {
            dataStoreManager.syncFollowToYouTube.collect {
                _syncFollowToYouTube.emit(it == DataStoreManager.TRUE)
            }
        }
    }

    fun setSyncFollowToYouTube(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSyncFollowToYouTube(enabled)
            // Turning it on is a statement about the whole library: artists followed before the
            // switch would otherwise never reach the account. Turning it off deliberately does
            // NOT unsubscribe — stopping the mirroring is not the same as asking us to undo it.
            if (enabled) {
                // Runs silently. The only toast in this feature belongs to the Follow button on
                // the artist screen, where the user performed the action and is waiting to see it
                // take effect; a switch in Settings is not the place to report on a background
                // sweep the user is not watching.
                artistRepository.syncFollowedArtistsToYouTube().collect { }
            }
        }
    }

    private var _spotifyLyrics: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyLyrics: StateFlow<Boolean> = _spotifyLyrics

    private var _spotifyCanvas: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyCanvas: StateFlow<Boolean> = _spotifyCanvas

    fun getSpotifyLyrics() {
        viewModelScope.launch {
            dataStoreManager.spotifyLyrics.collect {
                if (it == DataStoreManager.TRUE) {
                    _spotifyLyrics.emit(true)
                } else {
                    _spotifyLyrics.emit(false)
                }
            }
        }
    }

    fun setSpotifyLyrics(loggedIn: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSpotifyLyrics(loggedIn)
            getSpotifyLyrics()
        }
    }

    fun getSpotifyCanvas() {
        viewModelScope.launch {
            dataStoreManager.spotifyCanvas.collect {
                if (it == DataStoreManager.TRUE) {
                    _spotifyCanvas.emit(true)
                } else {
                    _spotifyCanvas.emit(false)
                }
            }
        }
    }

    fun setSpotifyCanvas(loggedIn: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSpotifyCanvas(loggedIn)
            getSpotifyCanvas()
        }
    }

    // Lấy giá trị của killServiceOnExit từ DataStore
    fun getKillServiceOnExit() {
        viewModelScope.launch {
            dataStoreManager.killServiceOnExit.collect { killServiceOnExit ->
                _killServiceOnExit.emit(killServiceOnExit)
            }
        }
    }

    // Lưu giá trị killServiceOnExit vào DataStore
    fun setKillServiceOnExit(kill: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setKillServiceOnExit(kill)
            getKillServiceOnExit()
        }
    }

    fun getYoutubeSubtitleLanguage() {
        viewModelScope.launch {
            dataStoreManager.youtubeSubtitleLanguage.collect { language ->
                _youtubeSubtitleLanguage.emit(language)
            }
        }
    }

    fun setYoutubeSubtitleLanguage(language: String) {
        viewModelScope.launch {
            dataStoreManager.setYoutubeSubtitleLanguage(language)
            getYoutubeSubtitleLanguage()
        }
    }

    fun getHelpBuildLyricsDatabase() {
        viewModelScope.launch {
            dataStoreManager.helpBuildLyricsDatabase.collect { helpBuildLyricsDatabase ->
                _helpBuildLyricsDatabase.emit(helpBuildLyricsDatabase == DataStoreManager.TRUE)
            }
        }
    }

    fun setHelpBuildLyricsDatabase(help: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setHelpBuildLyricsDatabase(help)
            getHelpBuildLyricsDatabase()
        }
    }
}

data class SettingsStorageSectionFraction(
    val otherApp: Float = 0f,
    val downloadCache: Float = 0f,
    val playerCache: Float = 0f,
    val canvasCache: Float = 0f,
    val thumbCache: Float = 0f,
    val appDatabase: Float = 0f,
    val freeSpace: Float = 0f,
) {
    fun combine(): Float = otherApp + downloadCache + playerCache + canvasCache + thumbCache + appDatabase + freeSpace
}

data class SettingAlertState(
    val title: String,
    val message: String? = null,
    val textField: TextFieldData? = null,
    val selectOne: SelectData? = null,
    val multipleSelect: SelectData? = null,
    val confirm: Pair<String, (SettingAlertState) -> Unit>,
    val dismiss: String,
) {
    data class TextFieldData(
        val label: String,
        val value: String = "",
        // User typing string -> (true or false, If false, show error message)
        val verifyCodeBlock: ((String) -> Pair<Boolean, String?>)? = null,
    )

    data class SelectData(
        // Selected / Data
        val listSelect: List<Pair<Boolean, String>>,
    ) {
        fun getSelected(): String = listSelect.firstOrNull { it.first }?.second ?: ""

        fun getListSelected(): List<String> = listSelect.filter { it.first }.map { it.second }
    }
}

data class SettingBasicAlertState(
    val title: String,
    val message: String? = null,
    val confirm: Pair<String, () -> Unit>,
    val dismiss: String,
)

expect suspend fun calculateDataFraction(cacheRepository: CacheRepository): SettingsStorageSectionFraction?

expect suspend fun restoreNative(
    commonRepository: CommonRepository,
    uri: Uri,
    getData: () -> Unit = {},
)

expect suspend fun backupNative(
    commonRepository: CommonRepository,
    uri: Uri,
    backupDownloaded: Boolean,
)

expect fun getPackageName(): String

expect fun getFileDir(): String

expect fun changeLanguageNative(code: String)

/** Number of equalizer bands, matching the ISO centres the desktop backend installs. */
const val EQUALIZER_BAND_COUNT = 10

/** Band centre labels, for display only — the backend owns the actual frequencies. */
val EQUALIZER_BAND_LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
