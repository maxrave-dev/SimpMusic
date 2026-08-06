package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.common.Config
import com.maxrave.common.PODCAST_PROGRESS_KEY_PREFIX
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.EpisodeEntity
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.model.podcast.PodcastBrowse
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.extension.isPodcast
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.repository.PodcastRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.expect.shareUrl
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.share_url

// UI state cho podcast
sealed class PodcastUIState {
    object Loading : PodcastUIState()

    data class Success(
        val id: String,
        val data: PodcastBrowse,
    ) : PodcastUIState()

    data class Error(
        val message: String,
    ) : PodcastUIState()
}

sealed class PodcastUIEvent {
    data class PlayAll(
        val podcastId: String,
    ) : PodcastUIEvent()

    data class Shuffle(
        val podcastId: String,
    ) : PodcastUIEvent()

    data class EpisodeClick(
        val videoId: String,
        val podcastId: String,
    ) : PodcastUIEvent()

    data class ToggleFavorite(
        val podcastId: String,
        val isFavorite: Boolean,
    ) : PodcastUIEvent()

    data class Share(
        val podcastId: String,
    ) : PodcastUIEvent()

    data object DownloadAll : PodcastUIEvent()
}

class PodcastViewModel(
    private val podcastRepository: PodcastRepository,
    private val dataStoreManager: DataStoreManager,
    private val songRepository: SongRepository,
    private val downloadHandler: DownloadHandler,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<PodcastUIState>(PodcastUIState.Loading)
    val uiState: StateFlow<PodcastUIState> = _uiState.asStateFlow()

    private val _podcastEntity = MutableStateFlow<PodcastsEntity?>(null)
    val podcastEntity: StateFlow<PodcastsEntity?> = _podcastEntity.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _episodeProgress = MutableStateFlow<Map<String, Long>>(emptyMap())
    val episodeProgress: StateFlow<Map<String, Long>> = _episodeProgress.asStateFlow()

    private var episodeProgressJob: Job? = null
    private var lastProgressVideoId: String? = null
    private var lastProgressSecond = -1L

    init {
        viewModelScope.launch {
            combine(mediaPlayerHandler.nowPlayingState, mediaPlayerHandler.simpleMediaState) { nowPlaying, mediaState ->
                nowPlaying to mediaState
            }.collectLatest { (nowPlaying, mediaState) ->
                val isPodcast = nowPlaying.track?.isPodcast() == true || nowPlaying.songEntity?.isPodcast() == true
                if (!isPodcast) return@collectLatest
                val videoId = nowPlaying.track?.videoId ?: nowPlaying.songEntity?.videoId ?: return@collectLatest
                when (mediaState) {
                    is SimpleMediaState.Progress -> {
                        val progressSecond = mediaState.progress / 1_000L
                        if (videoId != lastProgressVideoId || progressSecond != lastProgressSecond) {
                            lastProgressVideoId = videoId
                            lastProgressSecond = progressSecond
                            _episodeProgress.update { it + (videoId to mediaState.progress) }
                        }
                    }

                    SimpleMediaState.Ended -> {
                        _episodeProgress.update { it + (videoId to 0L) }
                    }

                    else -> Unit
                }
            }
        }
    }

    fun clearPodcastBrowse() {
        _uiState.value = PodcastUIState.Loading
        _podcastEntity.value = null
        episodeProgressJob?.cancel()
        _episodeProgress.value = emptyMap()
    }

    fun getPodcastBrowse(id: String) {
        _isFavorite.value = false
        _uiState.value = PodcastUIState.Loading
        viewModelScope.launch {
            // Kiểm tra xem có PodcastEntity trong database không
            podcastRepository.getPodcast(id).collectLatest { entity ->
                _podcastEntity.value = entity
                _isFavorite.value = entity?.isFavorite == true

                // Tải dữ liệu từ API
                podcastRepository.getPodcastData(id).collectLatest { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            resource.data?.let { podcastBrowse ->
                                _uiState.value =
                                    PodcastUIState.Success(
                                        id,
                                        podcastBrowse,
                                    )
                                loadEpisodeProgress(podcastBrowse.listEpisode)

                                val podcastEntity = podcastRepository.getPodcast(id).firstOrNull()
                                if (podcastEntity == null) {
                                    // Lưu podcast vào database
                                    savePodcastToDatabase(id, podcastBrowse)
                                } else {
                                    _podcastEntity.value = podcastEntity
                                    updatePodcastInLibraryNow(id)
                                    updatePodcastEpisodes(id, podcastBrowse)
                                }
                            }
                        }

                        is Resource.Error -> {
                            // Nếu đã có dữ liệu trong database, sử dụng dữ liệu đó
                            if (_podcastEntity.value != null) {
                                // Lấy episodes từ database
                                podcastRepository.getPodcastWithEpisodes(id).first()?.let { podcastWithEpisodes ->
                                    val episodes =
                                        podcastWithEpisodes.episodes.map { episode ->
                                            PodcastBrowse.EpisodeItem(
                                                title = episode.title,
                                                author =
                                                    Artist(
                                                        id = episode.authorId,
                                                        name = episode.authorName,
                                                    ),
                                                description = episode.description,
                                                thumbnail = listOf(),
                                                createdDay = episode.createdDay,
                                                durationString = episode.durationString,
                                                videoId = episode.videoId,
                                            )
                                        }

                                    val podcastBrowse =
                                        PodcastBrowse(
                                            title = podcastWithEpisodes.podcast.title,
                                            author =
                                                Artist(
                                                    id = podcastWithEpisodes.podcast.authorId,
                                                    name = podcastWithEpisodes.podcast.authorName,
                                                ),
                                            authorThumbnail = podcastWithEpisodes.podcast.authorThumbnail,
                                            thumbnail = listOf(),
                                            description = podcastWithEpisodes.podcast.description,
                                            listEpisode = episodes,
                                        )

                                    _uiState.value =
                                        PodcastUIState.Success(
                                            id,
                                            podcastBrowse,
                                        )
                                    loadEpisodeProgress(podcastBrowse.listEpisode)
                                } ?: run {
                                    _uiState.value = PodcastUIState.Error(resource.message ?: "Unknown error")
                                }
                            } else {
                                _uiState.value = PodcastUIState.Error(resource.message ?: "Unknown error")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun savePodcastToDatabase(
        id: String,
        podcastBrowse: PodcastBrowse,
    ) {
        viewModelScope.launch {
            // Lưu podcast
            val podcastEntity =
                PodcastsEntity(
                    podcastId = id,
                    title = podcastBrowse.title,
                    authorId = podcastBrowse.author.id ?: "",
                    authorName = podcastBrowse.author.name,
                    authorThumbnail = podcastBrowse.authorThumbnail,
                    description = podcastBrowse.description,
                    thumbnail = podcastBrowse.thumbnail.lastOrNull()?.url,
                    listEpisodes =
                        podcastBrowse.listEpisode.map {
                            it.videoId
                        },
                )

            podcastRepository.insertPodcast(podcastEntity).collectLatest {
                _podcastEntity.value = podcastEntity
            }

            // Lưu episodes
            val episodes =
                podcastBrowse.listEpisode.map { episode ->
                    EpisodeEntity(
                        videoId = episode.videoId,
                        podcastId = id,
                        title = episode.title,
                        authorId = episode.author.id ?: "",
                        authorName = episode.author.name,
                        description = episode.description,
                        createdDay = episode.createdDay,
                        durationString = episode.durationString,
                        thumbnail = episode.thumbnail.lastOrNull()?.url,
                    )
                }

            podcastRepository.insertEpisodes(episodes).firstOrNull()?.let {
                // Episodes đã được lưu
            }
            _podcastEntity.value = podcastRepository.getPodcast(id).firstOrNull()
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _podcastEntity.value?.let { podcast ->
                val newFavoriteState = !podcast.isFavorite
                _isFavorite.value = newFavoriteState

                podcastRepository.favoritePodcast(podcast.podcastId, newFavoriteState).collectLatest {
                    _podcastEntity.update { it?.copy(isFavorite = newFavoriteState) }
                }
            }
        }
    }

    fun updatePodcastEpisodes(
        id: String,
        podcastBrowse: PodcastBrowse,
    ) {
        viewModelScope.launch {
            val episodes =
                podcastBrowse.listEpisode.map { episode ->
                    EpisodeEntity(
                        videoId = episode.videoId,
                        podcastId = id,
                        title = episode.title,
                        authorId = episode.author.id ?: "",
                        authorName = episode.author.name,
                        description = episode.description,
                        createdDay = episode.createdDay,
                        durationString = episode.durationString,
                        thumbnail = episode.thumbnail.lastOrNull()?.url,
                    )
                }

            podcastRepository.insertEpisodes(episodes).collectLatest {
                // Episodes đã được cập nhật
            }
        }
    }

    fun updatePodcastInLibraryNow(id: String) {
        viewModelScope.launch {
            podcastRepository.updatePodcastInLibraryNow(id).collectLatest {
                // Podcast đã được cập nhật trong thư viện
                log("Podcast $id updated in library at ${System.currentTimeMillis()}")
            }
        }
    }

    private fun loadEpisodeProgress(episodes: List<PodcastBrowse.EpisodeItem>) {
        episodeProgressJob?.cancel()
        episodeProgressJob =
            viewModelScope.launch {
                _episodeProgress.value =
                    episodes.associate { episode ->
                        val progress =
                            dataStoreManager
                                .getString("$PODCAST_PROGRESS_KEY_PREFIX${episode.videoId}")
                                .first()
                                ?.toLongOrNull()
                                ?: 0L
                        episode.videoId to progress
                    }
            }
    }

    private fun downloadAllEpisodes(podcastData: PodcastBrowse) {
        viewModelScope.launch {
            val tracks = podcastData.listEpisode.map { it.toTrack() }
            tracks.forEach { track ->
                songRepository.insertSong(track.toSongEntity()).singleOrNull()
            }
            val songs =
                songRepository
                    .getSongsByListVideoId(tracks.map { it.videoId })
                    .singleOrNull()
                    .orEmpty()
            val pending = songs.filter { it.downloadState != DownloadState.STATE_DOWNLOADED }
            if (pending.isEmpty()) {
                makeToast(getString(Res.string.downloaded))
                return@launch
            }

            makeToast(getString(Res.string.downloading))
            pending.forEach { episode ->
                downloadHandler.downloadTrack(
                    videoId = episode.videoId,
                    title = episode.title,
                    thumbnail = episode.thumbnails.orEmpty(),
                )
            }
        }
    }

    fun onUIEvent(event: PodcastUIEvent) {
        val currentState = _uiState.value
        if (currentState !is PodcastUIState.Success) return

        val podcastData = currentState.data
        when (event) {
            is PodcastUIEvent.PlayAll -> {
                if (podcastData.listEpisode.isNotEmpty()) {
                    val firstEpisode = podcastData.listEpisode.first()
                    val queueData =
                        QueueData.Data(
                            listTracks = podcastData.listEpisode.map { it.toTrack() },
                            firstPlayedTrack = firstEpisode.toTrack(),
                            playlistId = event.podcastId,
                            playlistName = "Podcast \"${podcastData.title}\"",
                            playlistType = PlaylistType.PODCAST,
                            continuation = null,
                        )
                    setQueueData(queueData)
                    loadMediaItem(
                        firstEpisode.toTrack(),
                        Config.PLAYLIST_CLICK,
                        0,
                    )
                }
            }

            is PodcastUIEvent.Shuffle -> {
                if (podcastData.listEpisode.isNotEmpty()) {
                    val index = kotlin.random.Random.nextInt(0, podcastData.listEpisode.size)
                    val shuffleList = podcastData.listEpisode.toMutableList()
                    val firstPlayItem = shuffleList.removeAt(index)
                    shuffleList.shuffle()
                    shuffleList.add(0, firstPlayItem)

                    val queueData =
                        QueueData.Data(
                            listTracks = shuffleList.map { it.toTrack() },
                            firstPlayedTrack = firstPlayItem.toTrack(),
                            playlistId = event.podcastId,
                            playlistName = "Podcast \"${podcastData.title}\"",
                            playlistType = PlaylistType.PODCAST,
                            continuation = null,
                        )
                    setQueueData(queueData)
                    loadMediaItem(
                        firstPlayItem.toTrack(),
                        Config.PLAYLIST_CLICK,
                        0,
                    )
                }
            }

            is PodcastUIEvent.EpisodeClick -> {
                val videoId = event.videoId
                val episode = podcastData.listEpisode.find { it.videoId == videoId } ?: return
                val index = podcastData.listEpisode.indexOf(episode)

                val queueData =
                    QueueData.Data(
                        listTracks = podcastData.listEpisode.map { it.toTrack() },
                        firstPlayedTrack = episode.toTrack(),
                        playlistId = event.podcastId,
                        playlistName = "Podcast \"${podcastData.title}\"",
                        playlistType = PlaylistType.PODCAST,
                        continuation = null,
                    )
                setQueueData(queueData)
                loadMediaItem(
                    episode.toTrack(),
                    Config.PLAYLIST_CLICK,
                    index,
                )
            }

            is PodcastUIEvent.ToggleFavorite -> {
                toggleFavorite()
            }

            is PodcastUIEvent.Share -> {
                val url = "https://music.youtube.com/playlist?list=${event.podcastId}"
                shareUrl(
                    title = getString(Res.string.share_url),
                    url = url,
                )
            }

            PodcastUIEvent.DownloadAll -> {
                downloadAllEpisodes(podcastData)
            }
        }
    }
}
