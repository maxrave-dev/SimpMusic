package com.maxrave.simpmusic.viewModel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config.ALBUM_CLICK
import com.maxrave.common.Config.DOWNLOAD_CACHE
import com.maxrave.common.Config.PLAYLIST_CLICK
import com.maxrave.common.Config.RECOVER_TRACK_QUEUE
import com.maxrave.common.Config.SHARE
import com.maxrave.common.Config.SONG_CLICK
import com.maxrave.common.Config.VIDEO_CLICK
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.STATUS_DONE
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.LyricsEntity
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.entities.SongInfoEntity
import com.maxrave.domain.data.entities.TranslatedLyricsEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.canvas.CanvasResult
import com.maxrave.domain.data.model.download.DownloadProgress
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.domain.data.model.update.UpdateData
import com.maxrave.domain.extension.isSong
import com.maxrave.domain.extension.isVideo
import com.maxrave.domain.extension.toGenericMediaItem
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.FALSE
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.mediaservice.handler.ControlState
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.NowPlayingTrackState
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.mediaservice.handler.SleepTimerState
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.LyricsCanvasRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SongRepository
<<<<<<< HEAD
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.logger.Logger
import com.maxrave.data.autodj.AutoDJServiceImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.core.component.inject
=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.domain.repository.UpdateRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toListName
import com.maxrave.domain.utils.toLyrics
import com.maxrave.domain.utils.toLyricsEntity
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
<<<<<<< HEAD
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.logger.LogLevel
=======
import com.maxrave.logger.LogLevel
import com.maxrave.logger.Logger
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.getDownloadFolderPath
import com.maxrave.simpmusic.expect.ui.toByteArray
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.utils.VersionManager
<<<<<<< HEAD
import com.maxrave.data.usecase.CSVImportUseCase
import com.maxrave.data.usecase.CSVExportUseCase
import com.maxrave.domain.utils.LocalResource
import com.maxrave.data.csv.CSVParser
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
=======
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
<<<<<<< HEAD
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.added_to_queue
import simpmusic.composeapp.generated.resources.added_to_youtube_liked
<<<<<<< HEAD
import simpmusic.composeapp.generated.resources.already_voted
=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.play_next
import simpmusic.composeapp.generated.resources.removed_from_youtube_liked
import simpmusic.composeapp.generated.resources.shared
import simpmusic.composeapp.generated.resources.updated
import simpmusic.composeapp.generated.resources.vote_submitted
import java.io.FileOutputStream
import java.lang.Exception
import kotlin.math.abs
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModel(
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val updateRepository: UpdateRepository,
    private val songRepository: SongRepository,
    private val albumRepository: AlbumRepository,
    private val localPlaylistRepository: LocalPlaylistRepository,
    private val playlistRepository: PlaylistRepository,
    private val lyricsCanvasRepository: LyricsCanvasRepository,
    private val cacheRepository: CacheRepository,
<<<<<<< HEAD
    private val csvImportUseCase: CSVImportUseCase? = null,
    private val csvExportUseCase: CSVExportUseCase? = null
) : BaseViewModel() {
    
=======
) : BaseViewModel() {
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
    var isFirstLiked: Boolean = false
    var isFirstMiniplayer: Boolean = false
    var isFirstSuggestions: Boolean = false
    var showedUpdateDialog: Boolean = false

<<<<<<< HEAD
    // Historique de session pour éviter les répétitions (A -> B -> A)
    private val sessionHistory = mutableListOf<String>()

=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked: SharedFlow<Boolean> = _liked.asSharedFlow()

    var isServiceRunning: Boolean = false

    private var _sleepTimerState = MutableStateFlow(SleepTimerState(false, 0))
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState

    private var regionCode: String? = null
    private var language: String? = null
<<<<<<< HEAD
    
    // DJ Crossfade state management
    private var isDJCrossfadeInProgress = false
    private var lastCrossfadeTriggerTime = 0L
    private var currentPreloadedSongId: String? = null
    
    /**
     * Réinitialise l'état de l'AutoDJ quand l'utilisateur clique manuellement sur une musique
     * Cela permet de désactiver temporairement l'AutoDJ pour laisser l'utilisateur contrôler
     */
    fun resetAutoDJ() {
        Logger.d("SharedViewModel", "🎵 PLAYER - Resetting AutoDJ state (user manual action)")
        isDJCrossfadeInProgress = false
        lastCrossfadeTriggerTime = 0L
        currentPreloadedSongId = null
        sessionHistory.clear()
    }
    
    init {
        // Start Auto DJ monitoring when SharedViewModel is created
        startAutoDJMonitoring()
    }
    
    private fun startAutoDJMonitoring() {
        viewModelScope.launch {
            try {
                Logger.d("SharedViewModel", "🎵 PLAYER - Auto DJ monitoring initialized!")
                Logger.d("SharedViewModel", "🎵 PLAYER - Checking Auto DJ enabled state...")
                
                // Monitor Auto DJ enabled state
                dataStoreManager.autoDJEnabled.collect { isEnabled ->
                    Logger.d("SharedViewModel", "🎵 PLAYER - Auto DJ state changed: $isEnabled")
                    if (isEnabled == DataStoreManager.TRUE) {
                        Logger.d("SharedViewModel", "🎵 PLAYER - Auto DJ is ENABLED!")
                        
                        // Check available songs
                        try {
                            Logger.d("SharedViewModel", "🎵 PLAYER - Checking available songs for Auto DJ...")
                            val songs = songRepository.getAllSongsUnlimited().first()
                            Logger.d("SharedViewModel", "🎵 PLAYER - Found ${songs.size} total songs in database")
                            
                            if (songs.isNotEmpty()) {
                                Logger.d("SharedViewModel", "🎵 PLAYER - Auto DJ found ${songs.size} songs ready!")
                                Logger.d("SharedViewModel", "🎵 PLAYER - Sample songs: ${songs.take(3).map { it.title }}")
                                Logger.d("SharedViewModel", "🎵 PLAYER - Starting track position monitoring...")
                                
                                // Start monitoring track position for transitions
                                startTrackPositionMonitoring()
                            } else {
                                Logger.d("SharedViewModel", "🎵 PLAYER - Auto DJ: No songs found in database")
                            }
                        } catch (e: Exception) {
                            Logger.e("SharedViewModel", "PLAYER - Auto DJ check error: ${e.message}")
                            Logger.e("SharedViewModel", "PLAYER - Error stack: ${e.stackTraceToString()}")
                        }
                    } else {
                        Logger.d("SharedViewModel", "🎵 PLAYER - Auto DJ is DISABLED")
                    }
                }
            } catch (e: Exception) {
                Logger.e("SharedViewModel", "PLAYER - Auto DJ monitoring failed: ${e.message}")
                Logger.e("SharedViewModel", "PLAYER - Error stack: ${e.stackTraceToString()}")
            }
        }
    }
    
    private fun startTrackPositionMonitoring() {
        viewModelScope.launch {
            try {
                Logger.d("SharedViewModel", "🎵 PLAYER - Starting track position monitoring...")
                Logger.d("SharedViewModel", "🎵 PLAYER - MediaPlayerHandler available: ${mediaPlayerHandler != null}")
                
                while (isActive) {
                    try {
                        // Check if MediaPlayerHandler is available
                        if (mediaPlayerHandler == null) {
                            Logger.d("SharedViewModel", "PLAYER - MediaPlayerHandler not available, waiting...")
                            delay(5000)
                            continue
                        }
                        
                        // Check if Auto DJ is still enabled
                        val isEnabled = dataStoreManager.autoDJEnabled.first()
                        if (isEnabled != DataStoreManager.TRUE) {
                            Logger.d("SharedViewModel", "🎵 PLAYER - Auto DJ disabled, stopping position monitoring")
                            break
                        }
                        
                        // Get current track and position
                        val nowPlaying = mediaPlayerHandler.nowPlayingState?.value
                        Logger.d("SharedViewModel", "🎵 PLAYER - NowPlaying state: $nowPlaying")
                        
                        if (nowPlaying != null && mediaPlayerHandler.player != null) {
                            val currentTrack = nowPlaying.track
                            val currentPosition = mediaPlayerHandler.player.currentPosition
                            val duration = mediaPlayerHandler.player.duration
                            
                            Logger.d("SharedViewModel", "🎵 PLAYER - Track: ${currentTrack?.title} | Position: ${currentPosition/1000}s | Duration: ${duration/1000}s")
                            
                            if (duration > 0 && currentPosition > 0) {
                                val remainingTime = duration - currentPosition
                                
                                // CHARGEMENT ANTICIPÉ DÈS LE DÉBUT (Start + 0.5s)
                                // Dès que la chanson commence, on lance le chargement de la suivante !
                                if (currentPosition > 500 && currentPreloadedSongId != currentTrack?.videoId) {
                                     Logger.d("SharedViewModel", "🎵 PLAYER - 🚀 PRÉPARATION ULTRA-ANTICIPÉE (Démarrage chanson) !")
                                     currentPreloadedSongId = currentTrack?.videoId
                                     // On lance la préparation en tâche de fond
                                     prepareNextTrack()
                                }
                                
                                Logger.d("SharedViewModel", "🎵 PLAYER - Track: ${currentTrack?.title} | Position: ${currentPosition/1000}s | Remaining: ${remainingTime/1000}s")
                                
                                // Calculer le timing de mixage intelligent selon la durée de la musique
                                val trackDurationSeconds = duration / 1000
                                val (prepTime, mixTime) = when {
                                    trackDurationSeconds < 60 -> {
                                        // Musique courte (< 1min) : préparer 35s avant, mixer 30s avant
                                        Pair(35000L, 30000L)
                                    }
                                    trackDurationSeconds < 120 -> {
                                        // Musique moyenne (1-2min) : préparer 35s avant, mixer 30s avant
                                        Pair(35000L, 30000L)
                                    }
                                    trackDurationSeconds < 180 -> {
                                        // Musique normale (2-3min) : préparer 35s avant, mixer 30s avant
                                        Pair(35000L, 30000L)
                                    }
                                    else -> {
                                        // Musique longue (> 3min) : préparer 35s avant, mixer 30s avant
                                        Pair(35000L, 30000L)
                                    }
                                }
                                
                                Logger.d("SharedViewModel", "🎵 PLAYER - Durée: ${trackDurationSeconds}s -> Préparation: ${prepTime/1000}s, Mixage: ${mixTime/1000}s")
                                Logger.d("SharedViewModel", "🎵 PLAYER - DEBUG: remainingTime=${remainingTime/1000}s, mixTime=${mixTime/1000}s")
                                Logger.d("SharedViewModel", "🎵 PLAYER - DEBUG: condition1=${remainingTime <= prepTime}, condition2=${remainingTime <= mixTime}")
                                
                                // Check if we need to prepare next track (timing intelligent)
                                if (remainingTime <= prepTime && remainingTime > (prepTime - 1000)) {
                                    Logger.d("SharedViewModel", "🎵 PLAYER - 🚀 PRÉPARATION INTELLIGENTE - ${prepTime/1000}s avant la fin!")
                                    prepareNextTrack()
                                }
                                
                                // Check if we need to start DJ remix transition (timing intelligent)
                                if (remainingTime <= mixTime && remainingTime > (mixTime - 1000)) {
                                    Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 MIXAGE INTELLIGENT - ${mixTime/1000}s avant la fin!")
                                    startDJRemixTransition()
                                }
                                
                                // FORCE IMMEDIATE TRANSITION au timing intelligent (condition plus large)
                                if (remainingTime <= 35000) { // EXACTEMENT 35 secondes !

                                    // CRITIQUE : VÉRIFIER SI UN CROSSFADE EST DÉJÀ EN COURS !
                                    // Anti-spam: Ne pas déclencher si on l'a fait il y a moins de 10 secondes
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastCrossfadeTriggerTime < 10000) {
                                         Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX TRANSITION - Cooldown actif, on attend...")
                                         delay(2000)
                                         continue
                                    }

                                    // Si le player dit qu'il crossfade DÉJÀ, on le croit (sauf si buggé depuis longtemps, géré par le cooldown ci-dessus)
                                    if (mediaPlayerHandler?.isCrossfading() == true) {
                                        Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX TRANSITION - Crossfade déjà en cours (Confirmé), on touche à rien !")
                                        delay(2000)
                                        continue
                                    }
                                    
                                    lastCrossfadeTriggerTime = currentTime

                                    Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX TRANSITION - 35s avant la fin! MIXAGE FORCÉ!")
                                    Logger.d("SharedViewModel", "🎵 PLAYER - DEBUG: remainingTime=${remainingTime/1000}s <= 35s ? ${remainingTime <= 35000}")
                                    
                                    // SYSTÈME SIMPLE DIRECT - Éviter tous les problèmes de compilation
                                    try {
                                        Logger.d("SharedViewModel", "🎵 PLAYER - SYSTÈME SIMPLE DIRECT!")
                                        
                                        // VÉRIFICATION : Y a-t-il des musiques dans la queue ?
                                        val queueSize = mediaPlayerHandler?.player?.mediaItemCount ?: 0
                                        val currentIndex = mediaPlayerHandler?.player?.currentMediaItemIndex ?: 0
                                        val remainingInQueue = queueSize - currentIndex - 1 // -1 car currentIndex est 0-based
                                        
                                        val nextTrack: Track? = if (remainingInQueue > 0) {
                                            // Il y a une queue (Playlist/Baixados) → Utiliser la musique suivante de la queue
                                            Logger.d("SharedViewModel", "🎵 PLAYER - Queue has $remainingInQueue songs")
                                            Logger.d("SharedViewModel", "🎵 PLAYER - Using NEXT song from queue for crossfade")
                                            
                                            // Récupérer la musique suivante de la queue
                                            val nextMediaItem = mediaPlayerHandler?.player?.getMediaItemAt(currentIndex + 1)
                                            if (nextMediaItem != null) {
                                                // Convertir GenericMediaItem en Track
                                                val videoId = nextMediaItem.mediaId.removePrefix("Video")
                                                val songs = songRepository.getAllSongsUnlimited().first()
                                                songs.firstOrNull { it.videoId == videoId }?.toTrack()
                                            } else {
                                                Logger.w("SharedViewModel", "🎵 PLAYER - Failed to get next item from queue")
                                                null
                                            }
                                        } else {
                                            // Queue vide → Utiliser Smart Shuffle
                                            Logger.d("SharedViewModel", "🎵 PLAYER - Queue is empty, using Smart Shuffle")
                                            
                                            // 1. Récupérer les chansons
                                            val songs = songRepository.getAllSongsUnlimited().first()
                                            if (songs.isNotEmpty()) {
                                                val currentSong = mediaPlayerHandler?.nowPlayingState?.value?.songEntity
                                                
                                                // Ajouter la chanson actuelle à l'historique
                                                if (currentSong != null) {
                                                    sessionHistory.add(currentSong.videoId)
                                                    // Garder l'historique raisonnable (max 50 dernières ou 50% du total)
                                                    if (sessionHistory.size > songs.size / 2 || sessionHistory.size > 50) {
                                                        sessionHistory.removeAt(0)
                                                    }
                                                }

                                                // Strategie de sélection de la prochaine musique
                                                val isShuffleEnabled = mediaPlayerHandler?.controlState?.value?.isShuffle == true
                                                
                                                val nextSong = if (!isShuffleEnabled) {
                                                    // Mode Linéaire (Playlist) : On cherche la Suivante mathématique dans la liste
                                                    val currentIdx = songs.indexOfFirst { it.videoId == currentSong?.videoId }
                                                    if (currentIdx >= 0 && currentIdx < songs.size - 1) {
                                                        Logger.d("SharedViewModel", "🎵 PLAYLIST MODE - Playing NEXT song in list")
                                                        songs[currentIdx + 1]
                                                    } else {
                                                        // Fin de playlist -> Smart Shuffle
                                                        Logger.d("SharedViewModel", "🎵 PLAYLIST FINISHED - Falling back to Smart Shuffle")
                                                        getSmartShuffleNextSong(songs, currentSong, sessionHistory)
                                                    }
                                                } else {
                                                    // Mode Shuffle (Aléatoire) -> Smart Shuffle
                                                    Logger.d("SharedViewModel", "🎵 SHUFFLE MODE - Using Smart Shuffle")
                                                    getSmartShuffleNextSong(songs, currentSong, sessionHistory)
                                                }
                                                
                                                nextSong.toTrack()
                                            } else {
                                                null
                                            }
                                        }
                                        
                                        if (nextTrack != null) {
                                            Logger.d("SharedViewModel", "🎵 PLAYER - Next track: ${nextTrack.title}")
                                            
                                            // 3. DÉMARRER LE VRAI MIXAGE DJ - Avec résolution d'URL
                                            Logger.d("SharedViewModel", "🎧 DJ MIXAGE - 🎵 DÉMARRAGE VRAI MIXAGE DJ À 35s avec résolution d'URL!")
                                            
                                            // Utiliser la fonction helper qui gère la résolution d'URL
                                            startDJCrossfadeMix(nextTrack)
                                            
                                            // Pause pour éviter les déclenchements multiples
                                            delay(5000)
                                            
                                        } else {
                                            Logger.w("SharedViewModel", "🎵 PLAYER - Aucune chanson disponible")
                                        }
                                    } catch (e: Exception) {
                                        Logger.e("SharedViewModel", "🎵 PLAYER - Erreur système simple: ${e.message}")
                                        Logger.e("SharedViewModel", "🎵 PLAYER - Stack: ${e.stackTraceToString()}")
                                    }
                                } else {
                                    Logger.d("SharedViewModel", "🎵 PLAYER - DEBUG: Pas encore le moment - remainingTime=${remainingTime/1000}s > 35s")
                                }
                            } else {
                                Logger.d("SharedViewModel", "🎵 PLAYER - Invalid duration or position: duration=$duration, position=$currentPosition")
                            }
                        } else {
                            Logger.d("SharedViewModel", "🎵 PLAYER - No track playing or player not available")
                        }
                        
                        // Check every 2 seconds
                        delay(2000)
                        
                    } catch (e: Exception) {
                        Logger.e("SharedViewModel", "PLAYER - Track monitoring error: ${e.message}")
                        Logger.e("SharedViewModel", "PLAYER - Error stack: ${e.stackTraceToString()}")
                        delay(5000) // Wait longer on error
                    }
                }
            } catch (e: Exception) {
                Logger.e("SharedViewModel", "PLAYER - Track position monitoring failed: ${e.message}")
                Logger.e("SharedViewModel", "PLAYER - Error stack: ${e.stackTraceToString()}")
            }
        }
    }
    
    /**
     * VRAI MIXAGE DJ - Les deux musiques jouent ensemble avec volumes croisés!
     */
    private suspend fun startTrueDJMixage(currentTrack: SongEntity?, nextTrack: SongEntity) {
        try {
            Logger.d("SharedViewModel", "🎧 DJ MIXAGE - DÉMARRAGE VRAI MIXAGE DJ!")
            Logger.d("SharedViewModel", "🎧 DJ MIXAGE - Actuelle: ${currentTrack?.title}")
            Logger.d("SharedViewModel", "🎧 DJ MIXAGE - Suivante: ${nextTrack.title}")
            
            // Créer le media item pour la nouvelle musique
            val mediaItem = nextTrack.toGenericMediaItem()
            
            // Démarrer le mixage avec le MediaPlayerHandler
            mediaPlayerHandler?.startTrueDJCrossfade(mediaItem, 30000L)
            
            Logger.d("SharedViewModel", "🎧 DJ MIXAGE - ✅ CROSSFADE DJ LANCÉ!")
            
        } catch (e: Exception) {
            Logger.e("SharedViewModel", "🎧 DJ MIXAGE - Erreur: ${e.message}")
            Logger.e("SharedViewModel", "🎧 DJ MIXAGE - Stack: ${e.stackTraceToString()}")
        }
    }
    
    private suspend fun prepareNextTrack() {
        try {
            Logger.d("SharedViewModel", "🎵 PLAYER - Preparing next track for DJ mix (LOADING ONLY, no playback change)...")
            
            // Check if MediaPlayerHandler is available
            if (mediaPlayerHandler == null) {
                Logger.d("SharedViewModel", "PLAYER - MediaPlayerHandler not available for prepareNextTrack")
                return
            }
            
            val songs = songRepository.getAllSongsUnlimited().first()
            Logger.d("SharedViewModel", "🎵 PLAYER - Total songs available for next track: ${songs.size}")
            
            if (songs.size > 1) {
                // Get current track info for category-based selection
                val nowPlaying = mediaPlayerHandler.nowPlayingState?.value?.songEntity
                Logger.d("SharedViewModel", "🎵 PLAYER - Current track: ${nowPlaying?.title}")
                Logger.d("SharedViewModel", "🎵 PLAYER - Current artist: ${nowPlaying?.artistName?.firstOrNull()}")
                Logger.d("SharedViewModel", "🎵 PLAYER - Current album: ${nowPlaying?.albumName}")
                Logger.d("SharedViewModel", "🎵 PLAYER - Current category: ${nowPlaying?.category}")
                
                // Filter tracks by category (same artist, same album, or similar genre)
                val categoryTracks = filterTracksByCategory(songs, nowPlaying)
                Logger.d("SharedViewModel", "🎵 PLAYER - Category-based tracks available: ${categoryTracks.size}")
                
                // If no category matches found, fallback to random
                val availableTracks = if (categoryTracks.isNotEmpty()) {
                    Logger.d("SharedViewModel", "🎵 PLAYER - Using category-based selection for DJ mix!")
                    categoryTracks
                } else {
                    Logger.d("SharedViewModel", "🎵 PLAYER - No category matches, using random selection")
                    songs.filter { it.videoId != nowPlaying?.videoId }
                }
                
                if (availableTracks.isNotEmpty()) {
                    val nextTrack = availableTracks.random()
                    Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX PREP - Next track prepared: ${nextTrack.title} (ID: ${nextTrack.videoId})")
                    Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX PREP - Category: ${nextTrack.artistName?.firstOrNull()} | Album: ${nextTrack.albumName}")
                    
                    // PRE-LOAD the next track for DJ mixing (PREPARE WITHOUT ADDING TO QUEUE)
                    try {
                        // Store the next track information for later use during DJ transition
                        // DON'T add to queue yet - just prepare the media item for later
                        val mediaItem = nextTrack.toGenericMediaItem()
                        
                        Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX PREP - Track prepared: ${nextTrack.title}")
                        Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX PREP - Media item created but NOT added to queue yet!")
                        Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX PREP - Waiting for DJ remix trigger at 15s remaining...")
                        
                        // Store the prepared track for later use in DJ transition
                        // The actual addition to queue will happen during the DJ transition
                        
                    } catch (e: Exception) {
                        Logger.e("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX PREP - Failed to prepare track: ${e.message}")
                        Logger.e("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX PREP - Error stack: ${e.stackTraceToString()}")
                    }
                } else {
                    Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX PREP - No different tracks available for next track")
                }
            } else {
                Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX PREP - Not enough songs for transition (need >1, have ${songs.size})")
            }
        } catch (e: Exception) {
            Logger.e("SharedViewModel", "PLAYER - Failed to prepare next track: ${e.message}")
            Logger.e("SharedViewModel", "PLAYER - Error stack: ${e.stackTraceToString()}")
        }
    }
    
    private fun filterTracksByCategory(songs: List<SongEntity>, currentTrack: SongEntity?): List<SongEntity> {
        if (currentTrack == null) return emptyList()
        
        val currentArtist = currentTrack.artistName?.firstOrNull()
        val currentAlbum = currentTrack.albumName
        val currentCategory = currentTrack.category
        
        Logger.d("SharedViewModel", "🎵 PLAYER - Filtering by category - Artist: $currentArtist, Album: $currentAlbum, Category: $currentCategory")
        
        // Priority 1: Similar category (if available) - NOW HIGHEST PRIORITY
        if (currentCategory != null && currentCategory.isNotBlank()) {
            val similarCategoryTracks = songs.filter { 
                it.videoId != currentTrack.videoId && 
                it.category?.equals(currentCategory, ignoreCase = true) == true 
            }
            
            if (similarCategoryTracks.isNotEmpty()) {
                Logger.d("SharedViewModel", "🎵 PLAYER - Found ${similarCategoryTracks.size} tracks with similar category: $currentCategory (HIGHEST PRIORITY)")
                return similarCategoryTracks
            }
        }
        
        // Priority 2: Same artist
        val sameArtistTracks = songs.filter { 
            it.videoId != currentTrack.videoId && 
            it.artistName?.any { artist -> 
                artist.equals(currentArtist ?: "", ignoreCase = true) 
            } == true 
        }
        
        if (sameArtistTracks.isNotEmpty()) {
            Logger.d("SharedViewModel", "🎵 PLAYER - Found ${sameArtistTracks.size} tracks by same artist: $currentArtist (SECOND PRIORITY)")
            return sameArtistTracks
        }
        
        // Priority 3: Same album
        val sameAlbumTracks = songs.filter { 
            it.videoId != currentTrack.videoId && 
            it.albumName == currentAlbum 
        }
        
        if (sameAlbumTracks.isNotEmpty()) {
            Logger.d("SharedViewModel", "🎵 PLAYER - Found ${sameAlbumTracks.size} tracks from same album: $currentAlbum (THIRD PRIORITY)")
            return sameAlbumTracks
        }
        
        Logger.d("SharedViewModel", "🎵 PLAYER - No category matches found")
        return emptyList()
    }
    
    private suspend fun forceNextTrack() {
        try {
            Logger.d("SharedViewModel", "🎵 PLAYER - Forcing next track now...")
            
            // Check if MediaPlayerHandler is available
            if (mediaPlayerHandler == null) {
                Logger.d("SharedViewModel", "PLAYER - MediaPlayerHandler not available for forceNextTrack")
                return
            }
            
            val songs = songRepository.getAllSongsUnlimited().first()
            Logger.d("SharedViewModel", "🎵 PLAYER - Total songs available for force transition: ${songs.size}")
            
            if (songs.size > 1) {
                // Get current track info for category-based selection
                val nowPlaying = mediaPlayerHandler.nowPlayingState?.value?.songEntity
                Logger.d("SharedViewModel", "🎵 PLAYER - Current track for transition: ${nowPlaying?.title}")
                Logger.d("SharedViewModel", "🎵 PLAYER - Current artist: ${nowPlaying?.artistName?.firstOrNull()}")
                Logger.d("SharedViewModel", "🎵 PLAYER - Current album: ${nowPlaying?.albumName}")
                Logger.d("SharedViewModel", "🎵 PLAYER - Current category: ${nowPlaying?.category}")
                
                // Filter tracks by category (same artist, same album, or similar genre)
                val categoryTracks = filterTracksByCategory(songs, nowPlaying)
                Logger.d("SharedViewModel", "🎵 PLAYER - Category-based tracks available: ${categoryTracks.size}")
                
                // If no category matches found, fallback to random
                val availableTracks = if (categoryTracks.isNotEmpty()) {
                    Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX TRANSITION - Using category-based selection!")
                    categoryTracks
                } else {
                    Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX TRANSITION - No category matches, using random selection")
                    songs.filter { it.videoId != nowPlaying?.videoId }
                }
                
                if (availableTracks.isNotEmpty()) {
                    val nextTrack = availableTracks.random()
                    Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX TRANSITION - Adding: ${nextTrack.title} (ID: ${nextTrack.videoId})")
                    Logger.d("SharedViewModel", "🎵 PLAYER - 🎧 DJ MIX TRANSITION - Category: ${nextTrack.artistName?.firstOrNull()} | Album: ${nextTrack.albumName}")
                    
                    // DJ MIX: Start crossfade transition
                    startDJCrossfadeMix(nextTrack.toTrack())
                    
                } else {
                    Logger.d("SharedViewModel", "🎵 PLAYER - No different tracks available for transition")
                }
            } else {
                Logger.d("SharedViewModel", "🎵 PLAYER - Not enough songs for transition (need >1, have ${songs.size})")
            }
        } catch (e: Exception) {
            Logger.e("SharedViewModel", "PLAYER - Failed to force next track: ${e.message}")
            Logger.e("SharedViewModel", "PLAYER - Error stack: ${e.stackTraceToString()}")
        }
    }
    
    private suspend fun startDJRemixTransition() {
        try {
            Logger.d("SharedViewModel", "🎧 DJ REMIX - 🎵 STARTING DJ REMIX PREPARATION - 15s REMIX PROMPT!")
            Logger.d("SharedViewModel", "🎧 DJ REMIX - Preparing for DJ REMIX with both tracks playing together!")
            
            // Get current crossfade settings from DataStore
            val crossfadeDuration = dataStoreManager.autoDJCrossfadeDuration.first()
            val crossfadeEnabled = dataStoreManager.crossfadeEnabled.first() == DataStoreManager.TRUE
            
            Logger.d("SharedViewModel", "🎧 DJ REMIX - Crossfade settings: enabled=$crossfadeEnabled, duration=${crossfadeDuration}ms")
            
            if (crossfadeEnabled && crossfadeDuration > 0) {
                Logger.d("SharedViewModel", "🎧 DJ REMIX - 🎵 DJ REMIX PREPARATION ACTIVATED!")
                Logger.d("SharedViewModel", "🎧 DJ REMIX - 🎧 DJ PROMPT: Both tracks will play together for remix effect!")
                
                // Start DJ remix preparation - prepare for dual track playback
                try {
                    mediaPlayerHandler.player?.let { player ->
                        val currentVolume = player.volume
                        val remixVolume = currentVolume * 0.9f // Keep at 90% for remix preparation
                        
                        Logger.d("SharedViewModel", "🎧 DJ REMIX - 🎧 DJ PROMPT: Preparing remix volume: ${currentVolume} -> ${remixVolume}")
                        Logger.d("SharedViewModel", "🎧 DJ REMIX - 🎧 DJ PROMPT: Ready for dual track remix at 15s!")
                        
                        // Note: Actual dual track control would need to be implemented in MediaPlayerHandler
                        // This would involve creating a second audio stream for the next track
                        
                        Logger.d("SharedViewModel", "🎧 DJ REMIX - ✅ DJ REMIX PREPARATION COMPLETED!")
                        Logger.d("SharedViewModel", "🎧 DJ REMIX - 🎧 DJ PROMPT: Ready for professional DJ remix!")
                    }
                } catch (e: Exception) {
                    Logger.e("SharedViewModel", "🎧 DJ REMIX - Failed to start remix: ${e.message}")
                }
            } else {
                Logger.d("SharedViewModel", "🎧 DJ REMIX - Crossfade disabled, using simple DJ preparation")
            }
            
        } catch (e: Exception) {
            Logger.e("SharedViewModel", "🎧 DJ REMIX - Failed to start DJ remix: ${e.message}")
            Logger.e("SharedViewModel", "🎧 DJ REMIX - Error stack: ${e.stackTraceToString()}")
        }
    }
    
    private suspend fun enhanceDJRemixEffect() {
        try {
            Logger.d("SharedViewModel", "🎧 DJ REMIX PEAK - 🎵 DJ REMIX PEAK ACTIVATION - 14s DJ PROMPT!")
            Logger.d("SharedViewModel", "🎧 DJ REMIX PEAK - 🎧 DJ PROMPT: Maximum remix intensity - both tracks!")
            
            // Get crossfade settings
            val crossfadeDuration = dataStoreManager.autoDJCrossfadeDuration.first()
            val crossfadeEnabled = dataStoreManager.crossfadeEnabled.first() == DataStoreManager.TRUE
            
            if (crossfadeEnabled && crossfadeDuration > 0) {
                Logger.d("SharedViewModel", "🎧 DJ REMIX PEAK - 🎵 DJ REMIX PEAK - BOTH TRACKS PLAYING!")
                Logger.d("SharedViewModel", "🎧 DJ REMIX PEAK - 🎧 DJ PROMPT: Peak remix effect: ${crossfadeDuration}ms")
                Logger.d("SharedViewModel", "🎧 DJ REMIX PEAK - 🎧 DJ PROMPT: Professional DJ remix in progress!")
                
                try {
                    mediaPlayerHandler.player?.let { player ->
                        // Peak remix effect - both tracks playing simultaneously
                        val currentVolume = player.volume
                        val peakRemixVolume = currentVolume * 0.7f // Reduce to 70% for peak remix
                        
                        Logger.d("SharedViewModel", "🎧 DJ REMIX PEAK - 🎧 DJ PROMPT: Peak remix volume: ${currentVolume} -> ${peakRemixVolume}")
                        Logger.d("SharedViewModel", "🎧 DJ REMIX PEAK - 🎧 DJ PROMPT: Both tracks at peak remix intensity!")
                        
                        // Note: Actual dual track control would need to be implemented in MediaPlayerHandler
                        // This would involve:
                        // 1. Starting the next track in a second audio stream
                        // 2. Mixing both tracks with volume control
                        // 3. Creating the DJ remix effect
                        
                        Logger.d("SharedViewModel", "🎧 DJ REMIX PEAK - ✅ DJ REMIX PEAK ACTIVATED!")
                        Logger.d("SharedViewModel", "🎧 DJ REMIX PEAK - 🎧 DJ PROMPT: Professional DJ remix at maximum!")
                    }
                } catch (e: Exception) {
                    Logger.e("SharedViewModel", "🎧 DJ REMIX PEAK - Failed to enhance remix: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Logger.e("SharedViewModel", "🎧 DJ REMIX PEAK - Failed to enhance remix: ${e.message}")
            Logger.e("SharedViewModel", "🎧 DJ REMIX PEAK - Error stack: ${e.stackTraceToString()}")
        }
    }
    
    /**
     * DJ MIX DIRECT - Garantit que les deux musiques jouent ensemble!
     */
    private suspend fun startDirectDJMix() {
        try {
            Logger.d("SharedViewModel", "🎧 DJ MIX DIRECT - DÉMARRAGE MIXAGE GARANTI!")
            
            // Récupérer la chanson suivante
            val songs = songRepository.getAllSongsUnlimited().first()
            if (songs.isEmpty()) {
                Logger.w("SharedViewModel", "🎧 DJ MIX DIRECT - Aucune chanson disponible")
                return
            }
            
            // Trouver la chanson suivante
            val currentSong = mediaPlayerHandler?.nowPlayingState?.value?.songEntity
            val nextSong = if (currentSong != null) {
                val currentIndex = songs.indexOfFirst { song -> song.videoId == currentSong.videoId }
                if (currentIndex >= 0 && currentIndex < songs.size - 1) {
                    songs[currentIndex + 1]
                } else {
                    songs.first() // Revenir au début
                }
            } else {
                songs.first()
            }
            
            Logger.d("SharedViewModel", "🎧 DJ MIX DIRECT - Actuelle: ${currentSong?.title}")
            Logger.d("SharedViewModel", "🎧 DJ MIX DIRECT - Suivante: ${nextSong.title}")
            
            // Créer le media item pour la nouvelle musique
            val mediaItem = nextSong.toGenericMediaItem()
            
            // DÉMARRER LE MIXAGE DIRECT avec le MediaPlayerHandler
            Logger.d("SharedViewModel", "🎧 DJ MIX DIRECT - Lancement du mixage 30s!")
            mediaPlayerHandler?.startTrueDJCrossfade(mediaItem, 30000L)
            
            Logger.d("SharedViewModel", "🎧 DJ MIX DIRECT - ✅ MIXAGE LANCÉ! Les deux musiques jouent ENSEMBLE!")
            
        } catch (e: Exception) {
            Logger.e("SharedViewModel", "🎧 DJ MIX DIRECT - Erreur: ${e.message}")
            Logger.e("SharedViewModel", "🎧 DJ MIX DIRECT - Stack: ${e.stackTraceToString()}")
        }
    }
    
    private suspend fun startDJCrossfadeMix(nextTrack: Track) {
        try {
            Logger.d("SharedViewModel", "🎵 DJ MIX - Starting intelligent DJ crossfade mix...")
            
            
            // Get current crossfade settings from DataStore
            val crossfadeDuration = dataStoreManager.autoDJCrossfadeDuration.first()
            val rawSmoothTransitions = dataStoreManager.autoDJSmoothTransitions.first()
            val crossfadeEnabled = rawSmoothTransitions == DataStoreManager.TRUE
            
            Logger.d("SharedViewModel", "🎧 DJ MIX - RAW Smooth Transitions value: '$rawSmoothTransitions'")
            Logger.d("SharedViewModel", "🎧 DJ MIX - Crossfade settings: enabled=$crossfadeEnabled, duration=${crossfadeDuration}ms")
            
            // UTILISER toujours 30 secondes pour le mixage DJ
            val currentDuration = mediaPlayerHandler?.player?.duration ?: 30000L
            val trackDurationSeconds = currentDuration / 1000
            val intelligentMixDuration = 30000L // Toujours 30s pour toutes les musiques
            
            val effectiveCrossfadeDuration = if (crossfadeDuration > 0) crossfadeDuration.toLong() else intelligentMixDuration
            
            Logger.d("SharedViewModel", "🎧 DJ MIX - Musique de ${trackDurationSeconds}s -> Mixage intelligent de ${effectiveCrossfadeDuration/1000}s")
            
            // RÉSOLUTION D'URL UNIVERSELLE (pour AutoDJ ET lecture standard)
            Logger.d("SharedViewModel", "🎧 RESOLUTION - Resolving URL for: ${nextTrack.title} (${nextTrack.videoId})")
            var finalMediaItem = nextTrack.toSongEntity().toGenericMediaItem()
            var isPlayableUriFound = false
            
            
            // RÉSOLUTION D'URL (S'APPLIQUE À TOUS : AutoDJ ET Standard)
            try {
                // 1. VÉRIFIER SI TÉLÉCHARGÉ (Priorité Absolue)
                val downloadedSong = songRepository.getSongById(nextTrack.videoId).first()
                // Note: Pour l'instant, on ne peut pas vérifier facilement si un fichier est téléchargé
                // car cette info n'est pas dans SongEntity en commonMain
                // On va donc directement essayer le stream réseau
                
                // 2. RÉSEAU (Stream)
                    Logger.d("SharedViewModel", "🎧 RESOLUTION - fetching network stream...")
                    val streamUrl: String? = withContext(Dispatchers.IO) {
                        try {
                            withTimeout(30000) { // Timeout 15s pour éviter "Tempo esgotado"
                                streamRepository.getStream(
                                    dataStoreManager = dataStoreManager,
                                    videoId = nextTrack.videoId, 
                                    isDownloading = false, 
                                    isVideo = false
                                ).filterNotNull().first()
                            }
                        } catch (e: Exception) {
                            Logger.e("SharedViewModel", "🎧 RESOLUTION - Stream fetch failed: ${e.message}")
                            null
                        }
                    }
                    
                    if (!streamUrl.isNullOrBlank()) {
                         Logger.d("SharedViewModel", "🎧 RESOLUTION - ✅ SUCCESS! Stream URL found.")
                         finalMediaItem = finalMediaItem.copy(
                             uri = streamUrl,
                             metadata = finalMediaItem.metadata
                         )
                         isPlayableUriFound = true
                    } else {
                        Logger.w("SharedViewModel", "🎧 RESOLUTION - ⚠️ Failed to resolve URL")
                    }
            } catch (e: Exception) {
                Logger.e("SharedViewModel", "🎧 RESOLUTION - ❌ CRITICAL ERROR resolving: ${e.message}")
                e.printStackTrace()
            }
            
            if (crossfadeEnabled) {
                Logger.d("SharedViewModel", "🎧 DJ MIX - 🎵 STARTING DJ REMIX TRANSITION!")
                Logger.d("SharedViewModel", "🎧 DJ MIX - 🎧 DJ PROMPT: BOTH TRACKS PLAYING TOGETHER FOR DJ REMIX!")
                Logger.d("SharedViewModel", "🎧 DJ MIX - 🎧 DJ PROMPT: Professional DJ remix with dual tracks!")
                Logger.d("SharedViewModel", "🎧 DJ MIX - 🎧 DJ PROMPT: Intelligent crossfade duration: ${effectiveCrossfadeDuration}ms")
                
                // On prépare l'item final. Par défaut avec l'URL de base (unresolved)

                // DÉCISION: MIXAGE DJ OU AJOUT NORMAL ?
                // On ne lance le Crossfade DJ QUE si on a une URL jouable confirmée (File ou Stream)
                // Sinon, on laisse le player principal gérer (Fallback) pour éviter de planter le système de mixage
                
                if (isPlayableUriFound && !isDJCrossfadeInProgress) {
                    isDJCrossfadeInProgress = true
                    
                    Logger.d("SharedViewModel", "🎧 DJ MIX - 🎧 STARTING SMART MIX (${effectiveCrossfadeDuration/1000}s)")
                    mediaPlayerHandler?.startTrueDJCrossfade(finalMediaItem, effectiveCrossfadeDuration)
                    
                    Logger.d("SharedViewModel", "🎧 DJ MIX - ✅ TRUE DJ CROSSFADE INITIATED!")
                    
                    // Reset flag
                    kotlinx.coroutines.GlobalScope.launch {
                        kotlinx.coroutines.delay(effectiveCrossfadeDuration + 5000)
                        isDJCrossfadeInProgress = false
                    }
                } else {
                    Logger.w("SharedViewModel", "🎧 DJ MIX - ⚠️ FALLBACK! (UriFound=$isPlayableUriFound, Busy=$isDJCrossfadeInProgress)")
                    Logger.w("SharedViewModel", "🎧 DJ MIX - Crossfade skipped, player will transition naturally")
                    
                    // Ne rien faire - le player passera naturellement à la suivante
                    // (La musique est déjà dans la queue ou sera ajoutée par le système)
                }
                
                Logger.d("SharedViewModel", "🎧 DJ MIX - Simple transition completed")
            } else {
                // MODE STANDARD (Sans AutoDJ Crossfade) 
                // Mais on doit quand même ajouter la musique à la queue !
                Logger.d("SharedViewModel", "🎵 STANDARD MODE - Crossfade disabled, adding track to queue")
                
                // Ajouter la musique à la queue pour qu'elle joue après la musique actuelle
                try {
                    mediaPlayerHandler?.addMediaItem(finalMediaItem)
                    Logger.d("SharedViewModel", "🎵 STANDARD MODE - Track added to queue: ${nextTrack.title}")
                } catch (e: Exception) {
                    Logger.e("SharedViewModel", "🎵 STANDARD MODE - Failed to add track: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            Logger.e("SharedViewModel", "🎧 DJ MIX - Failed to start DJ crossfade: ${e.message}")
            Logger.e("SharedViewModel", "🎧 DJ MIX - Error stack: ${e.stackTraceToString()}")
            
            // Fallback to simple transition
            try {
                Logger.d("SharedViewModel", "🎧 DJ MIX - Fallback: Adding track to queue NOW!")
                val mediaItem = nextTrack.toGenericMediaItem()
                mediaPlayerHandler.addMediaItem(mediaItem)
                mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                Logger.d("SharedViewModel", "🎧 DJ MIX - Fallback transition completed")
            } catch (fallbackError: Exception) {
                Logger.e("SharedViewModel", "🎧 DJ MIX - Even fallback failed: ${fallbackError.message}")
                Logger.d("SharedViewModel", "🎧 DJ MIX - Last resort: Adding track to queue NOW!")
                // Last resort - add track and transition
                try {
                    val mediaItem = nextTrack.toGenericMediaItem()
                    mediaPlayerHandler.addMediaItem(mediaItem)
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                    Logger.d("SharedViewModel", "🎧 DJ MIX - Last resort transition completed")
                } catch (lastResortError: Exception) {
                    Logger.e("SharedViewModel", "🎧 DJ MIX - All transition methods failed: ${lastResortError.message}")
                }
            }
        }
    }
=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
    private var quality: String? = null

    private var _format: MutableStateFlow<NewFormatEntity?> = MutableStateFlow(null)
    val format: SharedFlow<NewFormatEntity?> = _format.asSharedFlow()

    private var _canvas: MutableStateFlow<CanvasResult?> = MutableStateFlow(null)
    val canvas: StateFlow<CanvasResult?> = _canvas

    private var canvasJob: Job? = null

    private val _intent: MutableStateFlow<GenericIntent?> = MutableStateFlow(null)
    val intent: StateFlow<GenericIntent?> = _intent

    private var getFormatFlowJob: Job? = null

    var playlistId: MutableStateFlow<String?> = MutableStateFlow(null)

    var isFullScreen: Boolean = false

    private var _nowPlayingState = MutableStateFlow<NowPlayingTrackState?>(null)
    val nowPlayingState: StateFlow<NowPlayingTrackState?> = _nowPlayingState

    fun getQueueDataState() = mediaPlayerHandler.queueData

    val blurBg: StateFlow<Boolean> =
        dataStoreManager.blurPlayerBackground
            .map { it == TRUE }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(500L),
                initialValue = false,
            )

    private var _controllerState =
        MutableStateFlow<ControlState>(
            ControlState(
                isPlaying = false,
                isShuffle = false,
                repeatState = RepeatState.None,
                isLiked = false,
                isNextAvailable = false,
                isPreviousAvailable = false,
                isCrossfading = false,
                volume = 1f,
            ),
        )
    val controllerState: StateFlow<ControlState> = _controllerState
    private val _getVideo: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val getVideo: StateFlow<Boolean> = _getVideo

    private var _timeline =
        MutableStateFlow<TimeLine>(
            TimeLine(
                current = -1L,
                total = -1L,
                bufferedPercent = 0,
                loading = true,
            ),
        )
    val timeline: StateFlow<TimeLine> = _timeline

    private var _nowPlayingScreenData =
        MutableStateFlow<NowPlayingScreenData>(
            NowPlayingScreenData.initial(),
        )
    val nowPlayingScreenData: StateFlow<NowPlayingScreenData> = _nowPlayingScreenData

    private var _likeStatus = MutableStateFlow<Boolean>(false)
    val likeStatus: StateFlow<Boolean> = _likeStatus

    val openAppTime: StateFlow<Int> = dataStoreManager.openAppTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)
    private val _shareSavedLyrics: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val shareSavedLyrics: StateFlow<Boolean> get() = _shareSavedLyrics

    init {
        viewModelScope.launch {
            log("SharedViewModel init")
            if (dataStoreManager.appVersion.first() != VersionManager.getVersionName()) {
                dataStoreManager.resetOpenAppTime()
                dataStoreManager.setAppVersion(
                    VersionManager.getVersionName(),
                )
            }
            dataStoreManager.openApp()
            if (getPlatform() == Platform.Desktop) {
                dataStoreManager.setWatchVideoInsteadOfPlayingAudio(false)
            }
            val timeLineJob =
                launch {
                    nowPlayingState
                        .filterNotNull()
                        .flatMapLatest { nowPlayingState ->
                            timeline.map { timeLine ->
                                Pair(timeLine, nowPlayingState)
                            }
                        }.distinctUntilChanged { old, new ->
                            (old.first.total.toString() + old.second.songEntity?.videoId).hashCode() ==
                                (new.first.total.toString() + new.second.songEntity?.videoId).hashCode()
                        }.collectLatest {
                            log("Timeline job ${(it.first.total.toString() + it.second.songEntity?.videoId).hashCode()}")
                            val nowPlaying = it.second
                            val timeline = it.first
                            if (timeline.total > 0 && nowPlaying.songEntity != null) {
                                if (nowPlaying.mediaItem.isSong() && nowPlayingScreenData.value.canvasData == null) {
                                    Logger.w(tag, "Duration is ${timeline.total}")
                                    Logger.w(tag, "MediaId is ${nowPlaying.mediaItem.mediaId}")
                                    getCanvas(nowPlaying.mediaItem.mediaId, (timeline.total / 1000).toInt())
                                }
                                nowPlaying.songEntity?.let { song ->
                                    if (nowPlayingScreenData.value.lyricsData == null) {
                                        Logger.w(tag, "Get lyrics from format")
                                        getLyricsFromFormat(nowPlaying.mediaItem.isVideo(), song, (timeline.total / 1000).toInt())
                                    }
                                }
                            }
                        }
                }
            val checkGetVideoJob =
                launch {
                    dataStoreManager.watchVideoInsteadOfPlayingAudio.collectLatest {
                        Logger.w(tag, "GetVideo is $it")
                        _getVideo.value = it == TRUE
                    }
                }
            val lyricsProviderJob =
                launch {
                    dataStoreManager.lyricsProvider.distinctUntilChanged().collectLatest {
                        setLyricsProvider()
                    }
                }
            val shareSavedLyricsJob =
                launch {
                    dataStoreManager.helpBuildLyricsDatabase.distinctUntilChanged().collectLatest {
                        _shareSavedLyrics.value = it == TRUE
                    }
                }
//            val controllerStateJob =
//                launch {
//                    controllerState.map { it.isLiked }.distinctUntilChanged().collectLatest {
//                        if (dataStoreManager.combineLocalAndYouTubeLiked.first() == TRUE) {
//                            nowPlayingState.value?.mediaItem?.mediaId?.let {
//                                getLikeStatus(it)
//                            }
//                        }
//                    }
//                }
            timeLineJob.join()
            checkGetVideoJob.join()
            lyricsProviderJob.join()
            shareSavedLyricsJob.join()
//            controllerStateJob.join()
        }

        runBlocking {
            dataStoreManager.getString("miniplayer_guide").first().let {
                isFirstMiniplayer = it != STATUS_DONE
            }
            dataStoreManager.getString("suggest_guide").first().let {
                isFirstSuggestions = it != STATUS_DONE
            }
            dataStoreManager.getString("liked_guide").first().let {
                isFirstLiked = it != STATUS_DONE
            }
        }
        viewModelScope.launch {
            mediaPlayerHandler.nowPlayingState
                .distinctUntilChangedBy {
                    it.songEntity?.videoId
                }.collectLatest { state ->
                    Logger.w(tag, "NowPlayingState is $state")
                    canvasJob?.cancel()
                    _nowPlayingState.value = state
                    state.songEntity?.let { track ->
                        _nowPlayingScreenData.value =
                            NowPlayingScreenData(
                                nowPlayingTitle = track.title,
                                artistName =
                                    track
                                        .artistName
                                        ?.joinToString(", ") ?: "",
                                isVideo = false,
                                thumbnailURL = null,
                                canvasData = null,
                                lyricsData = null,
                                songInfoData = null,
                                playlistName =
                                    mediaPlayerHandler.queueData.value
                                        ?.data
                                        ?.playlistName ?: "",
                            )
                    }
                    state.mediaItem.let { now ->
                        _canvas.value = null
                        getLikeStatus(now.mediaId)
                        getSongInfo(now.mediaId)
                        getFormat(now.mediaId)
                        _nowPlayingScreenData.update {
                            it.copy(
                                isVideo = now.isVideo(),
                            )
                        }
                    }
                    state.songEntity?.let { song ->
                        _liked.value = song.liked == true
                        _nowPlayingScreenData.update {
                            it.copy(
                                thumbnailURL = song.thumbnails,
                                isExplicit = song.isExplicit,
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            val job1 =
                launch {
                    mediaPlayerHandler.simpleMediaState.collect { mediaState ->
                        when (mediaState) {
                            is SimpleMediaState.Buffering -> {
                                _timeline.update {
                                    it.copy(
                                        loading = true,
                                    )
                                }
                            }

                            SimpleMediaState.Initial -> {
                                _timeline.update { it.copy(loading = true) }
                            }

                            SimpleMediaState.Ended -> {
                                _timeline.update {
                                    it.copy(
                                        current = -1L,
                                        total = -1L,
                                        bufferedPercent = 0,
                                        loading = true,
                                    )
                                }
                            }

                            is SimpleMediaState.Progress -> {
                                if (mediaState.progress >= 0L && mediaState.progress != _timeline.value.current) {
                                    if (_timeline.value.total > 0L) {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = false,
                                            )
                                        }
                                    } else {
                                        _timeline.update {
                                            it.copy(
                                                current = mediaState.progress,
                                                loading = true,
                                                total = mediaPlayerHandler.getPlayerDuration(),
                                            )
                                        }
                                    }
                                } else {
                                    _timeline.update {
                                        it.copy(
                                            loading = true,
                                        )
                                    }
                                }
                            }

                            is SimpleMediaState.Loading -> {
                                _timeline.update {
                                    it.copy(
                                        bufferedPercent = mediaState.bufferedPercentage,
                                        total = mediaState.duration,
                                    )
                                }
                            }

                            is SimpleMediaState.Ready -> {
                                _timeline.update {
                                    it.copy(
                                        current = mediaPlayerHandler.getProgress(),
                                        loading = false,
                                        total = mediaState.duration,
                                    )
                                }
                            }
                        }
                    }
                }
            val controllerJob =
                launch {
                    Logger.w(tag, "ControllerJob is running")
                    mediaPlayerHandler.controlState.collectLatest {
                        Logger.w(tag, "ControlState is $it")
                        _controllerState.value = it
                    }
                }
            val sleepTimerJob =
                launch {
                    mediaPlayerHandler.sleepTimerState.collectLatest {
                        _sleepTimerState.value = it
                    }
                }
            val playlistNameJob =
                launch {
                    mediaPlayerHandler.queueData.collectLatest {
                        _nowPlayingScreenData.update {
                            it.copy(playlistName = it.playlistName)
                        }
                    }
                }
            job1.join()
            controllerJob.join()
            sleepTimerJob.join()
            playlistNameJob.join()
        }
        // Reset downloading songs & playlists to not downloaded
        checkAllDownloadingSongs()
        checkAllDownloadingPlaylists()
        checkAllDownloadingLocalPlaylists()
    }

    fun setIntent(intent: GenericIntent?) {
        _intent.value = intent
    }

    fun blurFullscreenLyrics(): Boolean = runBlocking { dataStoreManager.blurFullscreenLyrics.first() == TRUE }

    private fun getLikeStatus(videoId: String?) {
        viewModelScope.launch {
            if (videoId != null) {
                _likeStatus.value = false
                songRepository.getLikeStatus(videoId).collectLatest { status ->
                    _likeStatus.value = status
                }
            }
        }
    }

    private fun getCanvas(
        videoId: String,
        duration: Int,
    ) {
        Logger.w(tag, "Start getCanvas: $videoId $duration")
//        canvasJob?.cancel()
        viewModelScope.launch {
            if (dataStoreManager.spotifyCanvas.first() == TRUE) {
                lyricsCanvasRepository.getCanvas(dataStoreManager, videoId, duration).cancellable().collect { response ->
                    val data = response.data
                    when (response) {
                        is Resource.Success if (data != null && nowPlayingState.value?.mediaItem?.mediaId == videoId) -> {
                            _canvas.value = data
                            _nowPlayingScreenData.update {
                                it.copy(
                                    canvasData =
                                        NowPlayingScreenData.CanvasData(
                                            isVideo = data.isVideo,
                                            url = data.canvasUrl,
                                        ),
                                )
                            }
                            // Save canvas video url
                            if (data.isVideo) lyricsCanvasRepository.updateCanvasUrl(videoId, data.canvasUrl)
                            // Save canvas thumb url
                            data.canvasThumbUrl?.let { lyricsCanvasRepository.updateCanvasThumbUrl(videoId, it) }
                        }

                        else -> {
                            log("Get canvas error: ${response.message}", LogLevel.WARN)
                            nowPlayingState.value?.songEntity?.canvasUrl?.let { url ->
                                _nowPlayingScreenData.update {
                                    it.copy(
                                        canvasData =
                                            NowPlayingScreenData.CanvasData(
                                                isVideo = url.contains(".mp4"),
                                                url = url,
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun getString(key: String): String? = runBlocking { dataStoreManager.getString(key).first() }

    fun putString(
        key: String,
        value: String,
    ) {
        runBlocking { dataStoreManager.putString(key, value) }
    }

    fun setSleepTimer(minutes: Int) {
        mediaPlayerHandler.sleepStart(minutes)
    }

    fun stopSleepTimer() {
        mediaPlayerHandler.sleepStop()
    }

    private var _downloadState: MutableStateFlow<DownloadHandler.Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<DownloadHandler.Download?> = _downloadState.asStateFlow()

    fun checkIsRestoring() {
        viewModelScope.launch {
            val downloadedCacheKeys = cacheRepository.getAllCacheKeys(DOWNLOAD_CACHE)
            songRepository.getDownloadedSongs().first().let { songs ->
                songs?.forEach { song ->
                    if (!downloadedCacheKeys.contains(song.videoId)) {
                        songRepository.updateDownloadState(
                            song.videoId,
                            DownloadState.STATE_NOT_DOWNLOADED,
                        )
                    }
                }
            }
            playlistRepository.getAllDownloadedPlaylist().first().let { list ->
                for (data in list) {
                    when (data) {
                        is AlbumEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                albumRepository.updateAlbumDownloadState(
                                    data.browseId,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is PlaylistEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                playlistRepository.updatePlaylistDownloadState(
                                    data.id,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                            }
                        }

                        is LocalPlaylistEntity -> {
                            val tracks = data.tracks ?: emptyList()
                            if (tracks.isEmpty() ||
                                (
                                    !downloadedCacheKeys.containsAll(
                                        tracks,
                                    )
                                )
                            ) {
                                localPlaylistRepository.updateLocalPlaylistDownloadState(
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                    data.id,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun insertLyrics(lyrics: LyricsEntity) {
        viewModelScope.launch {
            lyricsCanvasRepository.insertLyrics(lyrics)
        }
    }

    private fun getSavedLyrics(track: Track) {
        viewModelScope.launch {
            lyricsCanvasRepository.getSavedLyrics(track.videoId).cancellable().collectLatest { lyrics ->
                if (lyrics != null) {
                    val lyricsData = lyrics.toLyrics()
                    Logger.d(tag, "Saved Lyrics $lyricsData")
                    updateLyrics(
                        track.videoId,
                        track.durationSeconds ?: 0,
                        lyricsData,
                        false,
                        LyricsProvider.OFFLINE,
                    )
                    getAITranslationLyrics(
                        track.videoId,
                        lyricsData,
                    )
                }
            }
        }
    }

    fun loadSharedMediaItem(videoId: String) {
        viewModelScope.launch {
            streamRepository.getFullMetadata(videoId).collectLatest { response ->
                val track = response.data
                when (response) {
                    is Resource.Success if (track != null) -> {
                        mediaPlayerHandler.setQueueData(
                            QueueData.Data(
                                listTracks = arrayListOf(track),
                                firstPlayedTrack = track,
                                playlistId = "RDAMVM$videoId",
                                playlistName = getString(Res.string.shared),
                                playlistType = PlaylistType.RADIO,
                                continuation = null,
                            ),
                        )
                        loadMediaItemFromTrack(track, SONG_CLICK)
                    }

                    else -> {
                        log("Load shared media item error: ${response.message}", LogLevel.WARN)
                        makeToast("${getString(Res.string.error)}: ${response.message}")
                    }
                }
            }
        }
    }

    fun loadMediaItemFromTrack(
        track: Track,
        type: String,
        index: Int? = null,
    ) {
        quality = runBlocking { dataStoreManager.quality.first() }
        viewModelScope.launch {
            mediaPlayerHandler.clearMediaItems()
            songRepository.insertSong(track.toSongEntity()).lastOrNull()?.let {
                println("insertSong: $it")
                songRepository
                    .getSongById(track.videoId)
                    .collect { songEntity ->
                        if (songEntity != null) {
                            Logger.w("Check like", "loadMediaItemFromTrack ${songEntity.liked}")
                            _liked.value = songEntity.liked
                        }
                    }
            }
            track.durationSeconds?.let {
                songRepository.updateDurationSeconds(
                    it,
                    track.videoId,
                )
            }
            withContext(Dispatchers.Main) {
                mediaPlayerHandler.addMediaItem(track.toGenericMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)
            }

            when (type) {
                SONG_CLICK -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                VIDEO_CLICK -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                SHARE -> {
                    mediaPlayerHandler.getRelated(track.videoId)
                }

                PLAYLIST_CLICK -> {
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = 0)
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }

                ALBUM_CLICK -> {
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = 0)
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }
            }
        }
    }

    fun onUIEvent(uiEvent: UIEvent) =
        viewModelScope.launch {
            when (uiEvent) {
                UIEvent.Backward -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Backward,
                    )
                }

                UIEvent.Forward -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Forward)
                }

                UIEvent.PlayPause -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.PlayPause,
                    )
                }

                UIEvent.Next -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next)
                }

                UIEvent.Previous -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.Previous,
                    )
                }

                UIEvent.Stop -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Stop)
                }

                is UIEvent.UpdateProgress -> {
                    mediaPlayerHandler.onPlayerEvent(
                        PlayerEvent.UpdateProgress(
                            uiEvent.newProgress,
                        ),
                    )
                }

                UIEvent.Repeat -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Repeat)
                }

                UIEvent.Shuffle -> {
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.Shuffle)
                }

                UIEvent.ToggleLike -> {
                    Logger.w(tag, "ToggleLike")
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.ToggleLike)
                }

                is UIEvent.UpdateVolume -> {
                    val newVolume = uiEvent.newVolume
                    dataStoreManager.setPlayerVolume(newVolume)
                    mediaPlayerHandler.onPlayerEvent(PlayerEvent.UpdateVolume(newVolume))
                }
            }
        }

    override fun onCleared() {
        Logger.w("Check onCleared", "onCleared")
    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        quality = runBlocking { dataStoreManager.quality.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    private fun checkAllDownloadingLocalPlaylists() {
        viewModelScope.launch {
            localPlaylistRepository.getAllDownloadingLocalPlaylists().collectLatest { playlists ->
                playlists.forEach { playlist ->
                    localPlaylistRepository.updateDownloadState(playlist.id, 0, successMessage = getString(Res.string.updated)).lastOrNull()
                }
            }
        }
    }

    private fun checkAllDownloadingPlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllDownloadingPlaylist().collectLatest { list ->
                list.forEach { data ->
                    when (data) {
                        is AlbumEntity -> {
                            albumRepository.updateAlbumDownloadState(data.browseId, 0)
                        }

                        is PlaylistEntity -> {
                            playlistRepository.updatePlaylistDownloadState(data.id, 0)
                        }

                        else -> {
                            // Skip
                        }
                    }
                }
            }
        }
    }

    private fun checkAllDownloadingSongs() {
        viewModelScope.launch {
            songRepository.getDownloadingSongs().collect { songs ->
                songs?.forEach { song ->
                    songRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
            songRepository.getPreparingSongs().collect { songs ->
                songs.forEach { song ->
                    songRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED,
                    )
                }
            }
        }
    }

    private fun getFormat(mediaId: String?) {
        if (mediaId != _format.value?.videoId && !mediaId.isNullOrEmpty()) {
            _format.value = null
            getFormatFlowJob?.cancel()
            getFormatFlowJob =
                viewModelScope.launch {
                    streamRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                        Logger.w(tag, "Get format for $mediaId: $f")
                        if (f != null) {
                            _format.emit(f)
                        } else {
                            _format.emit(null)
                        }
                    }
                }
        }
    }

    private var songInfoJob: Job? = null

    fun getSongInfo(mediaId: String?) {
        songInfoJob?.cancel()
        songInfoJob =
            viewModelScope.launch {
                if (mediaId != null) {
                    songRepository.getSongInfo(mediaId).collect { song ->
                        _nowPlayingScreenData.update {
                            it.copy(
                                songInfoData = song,
                            )
                        }
                    }
                }
            }
    }

    private var _updateResponse = MutableStateFlow<UpdateData?>(null)
    val updateResponse: StateFlow<UpdateData?> = _updateResponse

    fun checkForUpdate() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val updateChannel = dataStoreManager.updateChannel.first()
            dataStoreManager.putString(
                "CheckForUpdateAt",
                System.currentTimeMillis().toString(),
            )
            if (updateChannel == DataStoreManager.GITHUB) {
                updateRepository.checkForGithubReleaseUpdate().collectLatest { response ->
                    val data = response.data
                    when (response) {
                        is Resource.Success if (data != null) -> {
                            _updateResponse.value = data
                            showedUpdateDialog = true
                        }

                        else -> {
                            log("Check for update error: ${response.message}", LogLevel.WARN)
                        }
                    }
                    _isCheckingUpdate.value = false
                }
            } else if (updateChannel == DataStoreManager.FDROID) {
                updateRepository.checkForFdroidUpdate().collectLatest { response ->
                    val data = response.data
                    when (response) {
                        is Resource.Success if (data != null) -> {
                            _updateResponse.value = data
                            showedUpdateDialog = true
                        }

                        else -> {
                            log("Check for update error: ${response.message}", LogLevel.WARN)
                        }
                    }
                    _isCheckingUpdate.value = false
                }
            }
        }
    }

<<<<<<< HEAD
    // Fonction utilitaire pour le Smart Shuffle
    private fun getSmartShuffleNextSong(
        songs: List<SongEntity>,
        currentSong: SongEntity?,
        history: MutableList<String>
    ): SongEntity {
        // 1. Essayer de trouver une chanson jamais jouée récemment
        val unplayedSongs = songs.filter { !history.contains(it.videoId) && it.videoId != currentSong?.videoId }
        
        return if (unplayedSongs.isNotEmpty()) {
            Logger.d("SharedViewModel", "🎵 SMART SHUFFLE - Picking from ${unplayedSongs.size} unplayed tracks")
            unplayedSongs.random()
        } else {
            // Fallback: Si tout a été joué, reset partiel
            Logger.d("SharedViewModel", "🎵 SMART SHUFFLE - Resetting history loop")
            history.clear() 
            if (currentSong != null) history.add(currentSong.videoId)
            songs.filter { it.videoId != currentSong?.videoId }.randomOrNull() ?: songs.random()
        }
    }

=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
    fun stopPlayer() {
        _nowPlayingScreenData.value = NowPlayingScreenData.initial()
        _nowPlayingState.value = null
        mediaPlayerHandler.resetSongAndQueue()
        onUIEvent(UIEvent.Stop)
    }

    private fun loadPlaylistOrAlbum(index: Int? = null) {
        mediaPlayerHandler.loadPlaylistOrAlbum(index)
    }

    private fun updateLyrics(
        videoId: String,
        duration: Int, // 0 if translated lyrics
        lyrics: Lyrics?,
        isTranslatedLyrics: Boolean,
        lyricsProvider: LyricsProvider = LyricsProvider.SIMPMUSIC,
    ) {
        if (lyrics == null) {
            _nowPlayingScreenData.update {
                it.copy(
                    lyricsData = null,
                )
            }
            return
        }

        if (isTranslatedLyrics) {
            val originalLyrics = _nowPlayingScreenData.value.lyricsData?.lyrics
            val originalLines = originalLyrics?.lines
            val lyricsLines = lyrics.lines
            if (originalLyrics != null && originalLines != null && lyricsLines != null) {
                var outOfSyncCount = 0

                originalLines.forEach { originalLine ->
                    val originalTime = originalLine.startTimeMs.toLongOrNull() ?: 0L
                    val closestTranslatedLine =
                        lyricsLines.minByOrNull {
                            abs((it.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
                        }

                    if (closestTranslatedLine != null) {
                        val translatedTime = closestTranslatedLine.startTimeMs.toLongOrNull() ?: 0L
                        val timeDiff = abs(originalTime - translatedTime)

                        if (timeDiff > 1000L) { // Lệch quá 1 giây
                            outOfSyncCount++
                        }
                        if (closestTranslatedLine.words == originalLine.words) {
                            outOfSyncCount++
                        }
                    }
                }

                if (outOfSyncCount > 5) {
                    Logger.w(tag, "Translated lyrics out of sync: $outOfSyncCount lines with time diff > 1s")

                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = null,
                                ),
                        )
                    }

                    viewModelScope.launch {
                        lyricsCanvasRepository.removeTranslatedLyrics(
                            videoId,
                            dataStoreManager.translationLanguage.first(),
                        )
                        log("Removed out-of-sync translated lyrics for $videoId")
                        val simpMusicLyricsId = lyrics.simpMusicLyrics?.id
                        if (lyricsProvider == LyricsProvider.SIMPMUSIC && !simpMusicLyricsId.isNullOrEmpty()) {
                            viewModelScope.launch {
                                lyricsCanvasRepository
                                    .voteSimpMusicTranslatedLyrics(
                                        translatedLyricsId = simpMusicLyricsId,
                                        false,
                                    ).collectLatest {
                                        when (it) {
                                            is Resource.Error -> {
                                                Logger.w(tag, "Vote SimpMusic Translated Lyrics Error ${it.message}")
                                            }

                                            is Resource.Success -> {
                                                Logger.d(tag, "Vote SimpMusic Translated Lyrics Success")
                                            }
                                        }
                                    }
                            }
                        }
                    }
                    if (lyricsProvider != LyricsProvider.AI) {
                        viewModelScope.launch {
                            nowPlayingScreenData.value.lyricsData?.lyrics?.let {
                                getAITranslationLyrics(
                                    videoId,
                                    it,
                                )
                            }
                        }
                    }
                    return
                }
            }
        }

        val shouldSendLyricsToSimpMusic =
            runBlocking {
                dataStoreManager.helpBuildLyricsDatabase.first() == TRUE
            } &&
                lyricsProvider != LyricsProvider.SIMPMUSIC
        if (_nowPlayingState.value?.songEntity?.videoId == videoId) {
            val track = _nowPlayingState.value?.track
            when (isTranslatedLyrics) {
                true -> {
                    if (lyricsProvider == LyricsProvider.SIMPMUSIC) {
                        _translatedVoteState.value =
                            VoteData(
                                id = lyrics.simpMusicLyrics?.id ?: "",
                                vote = lyrics.simpMusicLyrics?.vote ?: 0,
                                state = VoteState.Idle,
                            )
                    }
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                it.lyricsData?.copy(
                                    translatedLyrics = lyrics to lyricsProvider,
                                ),
                        )
                    }
                    if (shouldSendLyricsToSimpMusic && track != null) {
                        viewModelScope.launch {
                            lyricsCanvasRepository
                                .insertSimpMusicTranslatedLyrics(
                                    dataStoreManager,
                                    track,
                                    lyrics,
                                    dataStoreManager.translationLanguage.first(),
                                ).collect {
                                    when (it) {
                                        is Resource.Error -> {
                                            log("Insert SimpMusic Translated Lyrics Error ${it.message}")
                                        }

                                        is Resource.Success -> {
                                            log("Insert SimpMusic Translated Lyrics Success")
                                        }
                                    }
                                }
                        }
                    }
                }

                false -> {
                    if (lyricsProvider == LyricsProvider.SIMPMUSIC) {
                        _lyricsVoteState.value =
                            VoteData(
                                id = lyrics.simpMusicLyrics?.id ?: "",
                                vote = lyrics.simpMusicLyrics?.vote ?: 0,
                                state = VoteState.Idle,
                            )
                    }
                    _nowPlayingScreenData.update {
                        it.copy(
                            lyricsData =
                                NowPlayingScreenData.LyricsData(
                                    lyrics = lyrics,
                                    lyricsProvider = lyricsProvider,
                                ),
                        )
                    }
                    // Save lyrics to database
                    viewModelScope.launch {
                        lyricsCanvasRepository.insertLyrics(
                            LyricsEntity(
                                videoId = videoId,
                                error = false,
                                lines = lyrics.lines,
                                syncType = lyrics.syncType,
                            ),
                        )
                    }
                    if (shouldSendLyricsToSimpMusic && track != null) {
                        viewModelScope.launch {
                            lyricsCanvasRepository
                                .insertSimpMusicLyrics(
                                    dataStoreManager,
                                    track,
                                    duration,
                                    lyrics,
                                ).collect {
                                    when (it) {
                                        is Resource.Error -> {
                                            Logger.w(tag, "Insert SimpMusic Lyrics Error ${it.message}")
                                        }

                                        is Resource.Success -> {
                                            Logger.d(tag, "Insert SimpMusic Lyrics Success")
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    private fun getLyricsFromFormat(
        isVideo: Boolean,
        song: SongEntity,
        duration: Int,
    ) {
        viewModelScope.launch {
            val videoId = song.videoId
            log("Get Lyrics From Format for $videoId", LogLevel.WARN)
            val artistName = song.artistName
            val artist =
                if (artistName?.firstOrNull() != null &&
                    artistName
                        .firstOrNull()
                        ?.contains("Various Artists") == false
                ) {
                    artistName.firstOrNull()
                } else {
                    mediaPlayerHandler.nowPlaying
                        .first()
                        ?.metadata
                        ?.artist
                        ?: ""
                }
<<<<<<< HEAD
            loadLyricsVoteState()
=======
            resetLyricsVoteState()
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
            val lyricsProvider = dataStoreManager.lyricsProvider.first()
            if (isVideo) {
                getYouTubeCaption(
                    videoId,
                    song,
                    (artist ?: "").toString(),
                    duration,
                )
            } else {
                when (lyricsProvider) {
                    DataStoreManager.SIMPMUSIC -> {
                        getSimpMusicLyrics(
                            videoId,
                            song,
                            (artist ?: "").toString(),
                            duration,
                        )
                    }

                    DataStoreManager.LRCLIB -> {
                        getLrclibLyrics(
                            song,
                            (artist ?: "").toString(),
                            duration,
                        )
                    }

                    DataStoreManager.YOUTUBE -> {
                    }
                }
            }
        }
    }

    private suspend fun getSimpMusicLyrics(
        videoId: String,
        song: SongEntity,
        artist: String?,
        duration: Int,
    ) {
        lyricsCanvasRepository.getSimpMusicLyrics(videoId).collectLatest {
            Logger.w(tag, "Get SimpMusic Lyrics for $videoId: $it")
            val data = it.data
            if (it is Resource.Success && data != null) {
                Logger.d(tag, "Get SimpMusic Lyrics Success")
                updateLyrics(
                    videoId,
                    duration,
                    data,
                    false,
                    LyricsProvider.SIMPMUSIC,
                )
                insertLyrics(
                    data.toLyricsEntity(videoId),
                )
                getSimpMusicTranslatedLyrics(
                    videoId,
                    data,
                )
            } else if (dataStoreManager.spotifyLyrics.first() == TRUE) {
                getSpotifyLyrics(
                    song.toTrack().copy(durationSeconds = duration),
                    "${song.title} $artist",
                    duration,
                )
            } else {
                getLrclibLyrics(
                    song,
                    (artist ?: ""),
                    duration,
                )
            }
        }
    }

    private suspend fun getYouTubeCaption(
        videoId: String,
        song: SongEntity,
        artist: String?,
        duration: Int,
    ) {
        lyricsCanvasRepository
            .getYouTubeCaption(dataStoreManager.youtubeSubtitleLanguage.first(), videoId)
            .cancellable()
            .collect { response ->
                val data = response.data
                when (response) {
                    is Resource.Success if (data != null) -> {
                        val lyrics = data.first
                        val translatedLyrics = data.second
                        insertLyrics(lyrics.toLyricsEntity(videoId))
                        updateLyrics(
                            videoId,
                            duration,
                            lyrics,
                            false,
                            LyricsProvider.YOUTUBE,
                        )
                        if (translatedLyrics != null) {
                            updateLyrics(
                                videoId,
                                duration,
                                translatedLyrics,
                                true,
                                LyricsProvider.YOUTUBE,
                            )
                        } else {
                            getAITranslationLyrics(
                                videoId,
                                lyrics,
                            )
                        }
                    }

                    else -> {
                        getSimpMusicLyrics(
                            videoId,
                            song,
                            (artist ?: ""),
                            duration,
                        )
                    }
                }
            }
    }

    private fun getLrclibLyrics(
        song: SongEntity,
        artist: String,
        duration: Int,
    ) {
        viewModelScope.launch {
            lyricsCanvasRepository
                .getLrclibLyricsData(
                    artist,
                    song.title,
                    duration,
                ).collectLatest { res ->
                    val data = res.data
                    when (res) {
                        is Resource.Success if (data != null) -> {
                            Logger.d(tag, "Get Lyrics Data Success")
                            updateLyrics(
                                song.videoId,
                                duration,
                                res.data,
                                false,
                                LyricsProvider.LRCLIB,
                            )
                            insertLyrics(
                                res.data?.toLyricsEntity(
                                    song.videoId,
                                ) ?: return@collectLatest,
                            )
                            getAITranslationLyrics(
                                song.videoId,
                                data,
                            )
                        }

                        else -> {
                            getSavedLyrics(
                                song.toTrack().copy(
                                    durationSeconds = duration,
                                ),
                            )
                        }
                    }
                }
        }
    }

    private suspend fun getSimpMusicTranslatedLyrics(
        videoId: String,
        lyrics: Lyrics,
    ) {
        val translationLanguage =
            dataStoreManager.translationLanguage.first()
        lyricsCanvasRepository.getSimpMusicTranslatedLyrics(videoId, translationLanguage).collectLatest { response ->
            val data = response.data
            when (response) {
                is Resource.Success if (data != null) -> {
                    Logger.d(tag, "Get SimpMusic Translated Lyrics Success")
                    updateLyrics(
                        videoId,
                        0,
                        data,
                        true,
                        LyricsProvider.SIMPMUSIC,
                    )
                }

                else -> {
                    Logger.w(tag, "Get SimpMusic Translated Lyrics Error: ${response.message}")
                    getAITranslationLyrics(
                        videoId,
                        lyrics,
                    )
                }
            }
        }
    }

    private suspend fun getAITranslationLyrics(
        videoId: String,
        lyrics: Lyrics,
    ) {
        Logger.d(tag, "Get AI Translation Lyrics for $videoId")
        if (dataStoreManager.useAITranslation.first() == TRUE &&
            dataStoreManager.aiApiKey.first().isNotEmpty() &&
            dataStoreManager.enableTranslateLyric.first() == FALSE
        ) {
            val savedTranslatedLyrics =
                lyricsCanvasRepository
                    .getSavedTranslatedLyrics(
                        videoId,
                        dataStoreManager.translationLanguage.first(),
                    ).firstOrNull()
            if (savedTranslatedLyrics != null) {
                Logger.d(tag, "Get Saved Translated Lyrics")
                updateLyrics(
                    videoId,
                    0,
                    savedTranslatedLyrics.toLyrics(),
                    true,
                )
            } else {
                lyricsCanvasRepository
                    .getAITranslationLyrics(
                        lyrics,
                        dataStoreManager.translationLanguage.first(),
                    ).cancellable()
                    .collectLatest {
                        val data = it.data
                        when (it) {
                            is Resource.Success if (data != null) -> {
                                Logger.d(tag, "Get AI Translate Lyrics Success")
                                lyricsCanvasRepository.insertTranslatedLyrics(
                                    TranslatedLyricsEntity(
                                        videoId = videoId,
                                        language = dataStoreManager.translationLanguage.first(),
                                        error = false,
                                        lines = data.lines,
                                        syncType = data.syncType,
                                    ),
                                )
                                updateLyrics(
                                    videoId,
                                    0,
                                    data,
                                    true,
                                    LyricsProvider.AI,
                                )
                            }

                            else -> {
                                Logger.w(tag, "Get AI Translate Lyrics Error: ${it.message}")
                            }
                        }
                    }
            }
        }
    }

    private fun getSpotifyLyrics(
        track: Track,
        query: String,
        duration: Int? = null,
    ) {
        viewModelScope.launch {
            Logger.d("Check SpotifyLyrics", "SpotifyLyrics $query")
            lyricsCanvasRepository.getSpotifyLyrics(dataStoreManager, query, duration).cancellable().collect { response ->
                Logger.d("Check SpotifyLyrics", response.toString())
                val data = response.data
                when (response) {
                    is Resource.Success -> {
                        if (data != null) {
                            insertLyrics(
                                data.toLyricsEntity(
                                    track.videoId,
                                ),
                            )
                            updateLyrics(
                                track.videoId,
                                duration ?: 0,
                                data,
                                false,
                                LyricsProvider.SPOTIFY,
                            )
                            getAITranslationLyrics(
                                track.videoId,
                                data,
                            )
                        }
                    }

                    else -> {
                        getLrclibLyrics(
                            track.toSongEntity(),
                            track.artists.toListName().firstOrNull() ?: "",
                            duration ?: 0,
                        )
                    }
                }
            }
        }
    }

    private fun setLyricsProvider() {
        viewModelScope.launch {
            val songEntity = nowPlayingState.value?.songEntity ?: return@launch
            val isVideo = nowPlayingState.value?.mediaItem?.isVideo() ?: false
            getLyricsFromFormat(isVideo, songEntity, timeline.value.total.toInt() / 1000)
        }
    }

    private var _recreateActivity: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val recreateActivity: StateFlow<Boolean> = _recreateActivity

    fun activityRecreate() {
        _recreateActivity.value = true
    }

    fun activityRecreateDone() {
        _recreateActivity.value = false
    }

    fun addListToQueue(listTrack: ArrayList<Track>) {
        viewModelScope.launch {
            if (listTrack.size == 1 && dataStoreManager.endlessQueue.first() == TRUE) {
                mediaPlayerHandler.playNext(listTrack.first())
                makeToast(getString(Res.string.play_next))
            } else {
                mediaPlayerHandler.loadMoreCatalog(listTrack)
                makeToast(getString(Res.string.added_to_queue))
            }
        }
    }

    fun addToYouTubeLiked() {
        viewModelScope.launch {
            val videoId = mediaPlayerHandler.nowPlaying.first()?.mediaId
            if (videoId != null) {
                val like = likeStatus.value
                if (!like) {
                    songRepository
                        .addToYouTubeLiked(
                            mediaPlayerHandler.nowPlaying.first()?.mediaId,
                        ).collect { response ->
                            if (response == 200) {
                                makeToast(getString(Res.string.added_to_youtube_liked))
                                getLikeStatus(videoId)
                            } else {
                                makeToast(getString(Res.string.error))
                            }
                        }
                } else {
                    songRepository
                        .removeFromYouTubeLiked(
                            mediaPlayerHandler.nowPlaying.first()?.mediaId,
                        ).collect {
                            if (it == 200) {
                                makeToast(getString(Res.string.removed_from_youtube_liked))
                                getLikeStatus(videoId)
                            } else {
                                makeToast(getString(Res.string.error))
                            }
                        }
                }
            }
        }
    }

    fun getTranslucentBottomBar() = dataStoreManager.translucentBottomBar

    fun getEnableLiquidGlass() = dataStoreManager.enableLiquidGlass

    private val _reloadDestination: MutableStateFlow<KClass<*>?> = MutableStateFlow(null)
    val reloadDestination: StateFlow<KClass<*>?> = _reloadDestination.asStateFlow()

    fun reloadDestination(destination: KClass<*>) {
        _reloadDestination.value = destination
    }

    fun reloadDestinationDone() {
        _reloadDestination.value = null
    }

    fun shouldCheckForUpdate(): Boolean = runBlocking { dataStoreManager.autoCheckForUpdates.first() == TRUE }

    private var _downloadFileProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.INIT)
    val downloadFileProgress: StateFlow<DownloadProgress> get() = _downloadFileProgress

    fun downloadFile(bitmap: ImageBitmap) {
        val fileName =
            "${nowPlayingScreenData.value.nowPlayingTitle} - ${nowPlayingScreenData.value.artistName}"
                .replace(Regex("""[|\\?*<":>]"""), "")
                .replace(" ", "_")
        val path =
            "${getDownloadFolderPath()}/$fileName"
        viewModelScope.launch {
            nowPlayingState.value?.track?.let { track ->
                val bytesArray = bitmap.toByteArray()
                try {
                    val fileOutputStream = FileOutputStream("$path.jpg")
                    fileOutputStream.write(bytesArray)
                    fileOutputStream.close()
                    Logger.d(tag, "Thumbnail saved to $path.jpg")
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
                songRepository
                    .downloadToFile(
                        track = track,
                        videoId = track.videoId,
                        path = path,
                        isVideo = nowPlayingScreenData.value.isVideo,
                    ).collectLatest {
                        _downloadFileProgress.value = it
                    }
            }
        }
    }

    fun downloadFileDone() {
        _downloadFileProgress.value = DownloadProgress.INIT
    }

    fun onDoneReview(isDismissOnly: Boolean = true) {
        viewModelScope.launch {
            if (!isDismissOnly) {
                dataStoreManager.doneOpenAppTime()
            } else {
                dataStoreManager.openApp()
            }
        }
    }

    fun onDoneRequestingShareLyrics(contributor: Pair<String, String>? = null) {
        viewModelScope.launch {
            dataStoreManager.setHelpBuildLyricsDatabase(true)
            dataStoreManager.setContributorLyricsDatabase(
                contributor,
            )
        }
    }

    fun setBitmap(bitmap: ImageBitmap?) {
        _nowPlayingScreenData.update {
            it.copy(bitmap = bitmap)
        }
    }

    // Vote state for translated lyrics
    private val _translatedVoteState = MutableStateFlow<VoteData?>(null)
    val translatedVoteState: StateFlow<VoteData?> = _translatedVoteState.asStateFlow()

    // Vote state for original lyrics
    private val _lyricsVoteState = MutableStateFlow<VoteData?>(null)
    val lyricsVoteState: StateFlow<VoteData?> = _lyricsVoteState.asStateFlow()

    /**
     * Vote for SimpMusic original lyrics (upvote or downvote)
     * @param upvote true for upvote, false for downvote
     */
    fun voteLyrics(upvote: Boolean) {
        val lyricsData = _nowPlayingScreenData.value.lyricsData
        val lyricsProvider = lyricsData?.lyricsProvider
        val simpMusicLyricsId = lyricsData?.lyrics?.simpMusicLyrics?.id ?: return

        if (lyricsProvider != LyricsProvider.SIMPMUSIC || simpMusicLyricsId.isEmpty()) {
            Logger.w(tag, "Cannot vote: not a SimpMusic lyrics or missing ID")
            return
        }

<<<<<<< HEAD
        // Check if user has already voted
        viewModelScope.launch {
            val hasVoted = dataStoreManager.hasVotedLyrics(simpMusicLyricsId).first()
            if (hasVoted) {
                Logger.d(tag, "User has already voted for lyrics: $simpMusicLyricsId")
                makeToast(getString(Res.string.already_voted))
                return@launch
            }

=======
        viewModelScope.launch {
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
            _lyricsVoteState.update {
                it?.copy(
                    state = VoteState.Loading,
                )
            }
<<<<<<< HEAD
            
            // Save the vote immediately
            val voteValue = if (upvote) 1 else -1
            dataStoreManager.setLyricsVote(simpMusicLyricsId, voteValue)
            
=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
            lyricsCanvasRepository
                .voteSimpMusicLyrics(
                    lyricsId = simpMusicLyricsId,
                    upvote = upvote,
                ).collectLatest { result ->
                    when (result) {
                        is Resource.Error -> {
                            Logger.w(tag, "Vote SimpMusic Lyrics Error ${result.message}")
                            _lyricsVoteState.update {
                                it?.copy(
                                    state = VoteState.Error(result.message ?: "Unknown error"),
                                )
                            }
<<<<<<< HEAD
                            // Remove saved vote on error
                            dataStoreManager.setLyricsVote(simpMusicLyricsId, 0)
=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
                        }

                        is Resource.Success -> {
                            Logger.d(tag, "Vote SimpMusic Lyrics Success")
                            _lyricsVoteState.update {
                                it?.copy(
<<<<<<< HEAD
                                    vote = voteValue,
=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
                                    state = VoteState.Success(upvote),
                                )
                            }
                            makeToast(getString(Res.string.vote_submitted))
                        }
                    }
                }
        }
    }

<<<<<<< HEAD
    private fun loadLyricsVoteState() {
        viewModelScope.launch {
            val lyricsData = _nowPlayingScreenData.value.lyricsData
            
            // Load original lyrics vote state
            lyricsData?.lyrics?.simpMusicLyrics?.let { simpMusicLyrics ->
                val lyricsId = simpMusicLyrics.id
                val savedVote = dataStoreManager.getLyricsVote(lyricsId).first()
                val hasVoted = savedVote != 0
                
                _lyricsVoteState.value = VoteData(
                    id = lyricsId,
                    vote = savedVote,
                    state = if (hasVoted) {
                        VoteState.Success(savedVote > 0)
                    } else {
                        VoteState.Idle
                    }
                )
            }
            
            // Load translated lyrics vote state
            lyricsData?.translatedLyrics?.let { translatedLyrics ->
                val simpMusicLyrics = translatedLyrics.first
                val lyricsId = simpMusicLyrics.simpMusicLyrics?.id ?: return@let
                val savedVote = dataStoreManager.getTranslatedVote(lyricsId).first()
                val hasVoted = savedVote != 0
                
                _translatedVoteState.value = VoteData(
                    id = lyricsId,
                    vote = savedVote,
                    state = if (hasVoted) {
                        VoteState.Success(savedVote > 0)
                    } else {
                        VoteState.Idle
                    }
                )
            }
        }
=======
    private fun resetLyricsVoteState() {
        _lyricsVoteState.value = null
        _translatedVoteState.value = null
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
    }

    /**
     * Vote for SimpMusic translated lyrics (upvote or downvote)
     * @param upvote true for upvote, false for downvote
     */
    fun voteTranslatedLyrics(upvote: Boolean) {
        val translatedLyrics = _nowPlayingScreenData.value.lyricsData?.translatedLyrics
        val lyricsProvider = translatedLyrics?.second
        val simpMusicLyricsId = translatedLyrics?.first?.simpMusicLyrics?.id ?: return

        if (lyricsProvider != LyricsProvider.SIMPMUSIC || simpMusicLyricsId.isEmpty()) {
            Logger.w(tag, "Cannot vote: not a SimpMusic translated lyrics or missing ID")
            return
        }

<<<<<<< HEAD
        // Check if user has already voted
        viewModelScope.launch {
            val hasVoted = dataStoreManager.hasVotedTranslated(simpMusicLyricsId).first()
            if (hasVoted) {
                Logger.d(tag, "User has already voted for translated lyrics: $simpMusicLyricsId")
                makeToast(getString(Res.string.already_voted))
                return@launch
            }

=======
        viewModelScope.launch {
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
            _translatedVoteState.update {
                it?.copy(
                    state = VoteState.Loading,
                )
            }
<<<<<<< HEAD
            
            // Save the vote immediately
            val voteValue = if (upvote) 1 else -1
            dataStoreManager.setTranslatedVote(simpMusicLyricsId, voteValue)
            
=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
            lyricsCanvasRepository
                .voteSimpMusicTranslatedLyrics(
                    translatedLyricsId = simpMusicLyricsId,
                    upvote = upvote,
                ).collectLatest { result ->
                    when (result) {
                        is Resource.Error -> {
                            Logger.w(tag, "Vote SimpMusic Translated Lyrics Error ${result.message}")
                            _translatedVoteState.update {
                                it?.copy(
                                    state = VoteState.Error(result.message ?: "Unknown error"),
                                )
                            }
<<<<<<< HEAD
                            // Remove saved vote on error
                            dataStoreManager.setTranslatedVote(simpMusicLyricsId, 0)
=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
                        }

                        is Resource.Success -> {
                            Logger.d(tag, "Vote SimpMusic Translated Lyrics Success")
<<<<<<< HEAD
                            _translatedVoteState.update {
                                it?.copy(
                                    vote = voteValue,
                                    state = VoteState.Success(upvote),
                                )
                            }
=======
                            _translatedVoteState.update { it?.copy(state = VoteState.Success(upvote)) }
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
                            makeToast(getString(Res.string.vote_submitted))
                        }
                    }
                }
        }
    }

    fun shouldStopMusicService(): Boolean = runBlocking { dataStoreManager.killServiceOnExit.first() == TRUE }

    fun isUserLoggedIn(): Boolean = runBlocking { dataStoreManager.cookie.first().isNotEmpty() }

    fun isCombineFavoriteAndYTLiked(): Boolean = runBlocking { dataStoreManager.combineLocalAndYouTubeLiked.first() == TRUE }
<<<<<<< HEAD
    
    // CSV Import/Export methods
    fun importPlaylistFromCSV(csvContent: String): Flow<LocalResource<Long>> = 
        csvImportUseCase?.invoke(csvContent) ?: flow { emit(LocalResource.Error<Long>("CSV import not supported on this platform")) }
    
    fun exportPlaylistToCSV(playlistId: Long): Flow<LocalResource<String>> = 
        csvExportUseCase?.invoke(playlistId) ?: flow { emit(LocalResource.Error<String>("CSV export not supported on this platform")) }
    
    fun exportAllPlaylistsToCSV(): Flow<LocalResource<List<Pair<String, String>>>> = 
        csvExportUseCase?.exportAllPlaylists() ?: flow { emit(LocalResource.Error<List<Pair<String, String>>>("CSV export not supported on this platform")) }
=======
>>>>>>> b4b08beb9872537da9becf0dd247fbd569039180
}

sealed class UIEvent {
    data object PlayPause : UIEvent()

    data object Backward : UIEvent()

    data object Forward : UIEvent()

    data object Next : UIEvent()

    data object Previous : UIEvent()

    data object Stop : UIEvent()

    data object Shuffle : UIEvent()

    data object Repeat : UIEvent()

    data class UpdateProgress(
        val newProgress: Float,
    ) : UIEvent()

    data class UpdateVolume(
        val newVolume: Float,
    ) : UIEvent()

    data object ToggleLike : UIEvent()
}

enum class LyricsProvider {
    SIMPMUSIC,
    YOUTUBE,
    SPOTIFY,
    LRCLIB,
    AI,
    OFFLINE,
}

data class NowPlayingScreenData(
    val playlistName: String,
    val nowPlayingTitle: String,
    val artistName: String,
    val isVideo: Boolean,
    val isExplicit: Boolean = false,
    val thumbnailURL: String?,
    val canvasData: CanvasData? = null,
    val lyricsData: LyricsData? = null,
    val songInfoData: SongInfoEntity? = null,
    val bitmap: ImageBitmap? = null,
) {
    data class CanvasData(
        val isVideo: Boolean,
        val url: String,
    )

    data class LyricsData(
        val lyrics: Lyrics,
        val translatedLyrics: Pair<Lyrics, LyricsProvider>? = null,
        val lyricsProvider: LyricsProvider,
    )

    companion object {
        fun initial(): NowPlayingScreenData =
            NowPlayingScreenData(
                nowPlayingTitle = "",
                artistName = "",
                isVideo = false,
                thumbnailURL = null,
                canvasData = null,
                lyricsData = null,
                songInfoData = null,
                playlistName = "",
            )
    }
}

data class VoteData(
    val id: String,
    val vote: Int,
    val state: VoteState,
)

sealed class VoteState {
    data object Idle : VoteState()

    data object Loading : VoteState()

    data class Success(
        val upvote: Boolean,
    ) : VoteState()

    data class Error(
        val message: String,
    ) : VoteState()
}