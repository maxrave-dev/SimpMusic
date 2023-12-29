package com.maxrave.simpmusic.ui.fragment.player

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.request.CachePolicy
import coil.size.Size
import coil.transform.RoundedCornersTransformation
import coil.transform.Transformation
import com.daimajia.swipe.SwipeLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.lyrics.LyricsAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.Config.ALBUM_CLICK
import com.maxrave.simpmusic.common.Config.MINIPLAYER_CLICK
import com.maxrave.simpmusic.common.Config.PLAYLIST_CLICK
import com.maxrave.simpmusic.common.Config.SHARE
import com.maxrave.simpmusic.common.Config.SONG_CLICK
import com.maxrave.simpmusic.common.Config.VIDEO_CLICK
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.LYRICS_PROVIDER
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.metadata.Line
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSleepTimerBinding
import com.maxrave.simpmusic.databinding.FragmentNowPlayingBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.setTextAnimation
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.RepeatState
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.utils.CenterLayoutManager
import com.maxrave.simpmusic.utils.DisableTouchEventRecyclerView
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime


@UnstableApi
@AndroidEntryPoint
class NowPlayingFragment : Fragment() {

    val viewModel by activityViewModels<SharedViewModel>()
    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    private var metadataCurSong: MetadataSong? = null

    private var videoId: String? = null
    private var from: String? = null
    private var type: String? = null
    private var index: Int? = null
    private var downloaded: Int? = null
    private var playlistId: String? = null

    private var gradientDrawable: GradientDrawable? = null
    private var lyricsBackground: Int? = null

    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var lyricsFullAdapter: LyricsAdapter
    private lateinit var disableScrolling: DisableTouchEventRecyclerView
    private var overlayJob: Job? = null

    private var isFullScreen = false

//    private lateinit var songChangeListener: OnNowPlayingSongChangeListener
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        if (context is OnNowPlayingSongChangeListener) {
//            songChangeListener = context
//        } else {
//            throw RuntimeException("$context must implement OnNowPlayingSongChangeListener")
//        }
//    }

//    override fun onResume() {
//        Log.d("NowPlayingFragment", "onResume")
//        updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
//        super.onResume()
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        binding.topAppBar.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        activity?.window?.navigationBarColor = Color.TRANSPARENT
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val miniplayer = activity.findViewById<SwipeLayout>(R.id.miniplayer)

        bottom.visibility = View.GONE
        miniplayer.visibility = View.GONE
        binding.lyricsFullLayout.visibility = View.GONE
        binding.buffered.max = 100
        Log.d("check Video ID in ViewModel", viewModel.videoId.value.toString())

        type = arguments?.getString("type")
        videoId = arguments?.getString("videoId")
        from = arguments?.getString("from") ?: viewModel.from.value
        index = arguments?.getInt("index")
        downloaded = arguments?.getInt("downloaded")
        playlistId = arguments?.getString("playlistId")


        Log.d("check Video ID in Fragment", videoId.toString())

        disableScrolling = DisableTouchEventRecyclerView()

        lyricsAdapter = LyricsAdapter(null)
        lyricsAdapter.setOnItemClickListener(object : LyricsAdapter.OnItemClickListener {
            override fun onItemClick(line: Line?) {
                //No Implementation
            }
        })
        lyricsFullAdapter = LyricsAdapter(null)
        lyricsFullAdapter.setOnItemClickListener(object : LyricsAdapter.OnItemClickListener {
            override fun onItemClick(line: Line?) {
                Log.w("Check line", line.toString())
                if (line != null) {
                    val duration = runBlocking { viewModel.duration.first() }
                    Log.w("Check duration", duration.toString())
                    if (duration > 0 && line.startTimeMs.toLong() < duration) {
                        Log.w(
                            "Check seek",
                            (line.startTimeMs.toLong().toDouble() / duration).toFloat().toString()
                        )
                        val seek =
                            ((line.startTimeMs.toLong() * 100).toDouble() / duration).toFloat()
                        viewModel.onUIEvent(UIEvent.UpdateProgress(seek))
                    }
                }
            }
        })
        binding.rvLyrics.apply {
            adapter = lyricsAdapter
            layoutManager = CenterLayoutManager(requireContext())
        }
        binding.rvFullLyrics.apply {
            adapter = lyricsFullAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        binding.playerView.player = viewModel.simpleMediaServiceHandler?.player

        when (type) {
            SONG_CLICK -> {
                viewModel.playlistId.value = null
                if (viewModel.videoId.value == videoId) {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                } else {
                    Log.i("Now Playing Fragment", "Song Click")
                    binding.ivArt.setImageResource(0)
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
//                        lifecycleScope.launch {
//                            repeatOnLifecycle(Lifecycle.State.CREATED) {
//                                viewModel.firstTrackAdded.collect { added ->
//                                    if (added && type == Config.SONG_CLICK) {
//                                        viewModel.changeFirstTrackAddedToFalse()
////                                        viewModel.getFormat(it.videoId)
//                                        getRelated(it.videoId)
//                                    }
//                                }
//                            }
//                        }
                        viewModel.simpleMediaServiceHandler?.reset()
                        viewModel.resetRelated()
//                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
//                            requireActivity().stopService(
//                                Intent(
//                                    requireContext(),
//                                    FetchQueue::class.java
//                                )
//                            )
//                        }
                        viewModel.loadMediaItemFromTrack(it, SONG_CLICK)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        updateUIfromQueueNowPlaying()
                    }
                }
            }
            SHARE -> {
                viewModel.playlistId.value = null
                viewModel.stopPlayer()
                binding.ivArt.setImageResource(0)
                binding.loadingArt.visibility = View.VISIBLE
                viewModel.gradientDrawable.postValue(null)
                viewModel.lyricsBackground.postValue(null)
                binding.tvSongTitle.visibility = View.GONE
                binding.tvSongArtist.visibility = View.GONE
                if (videoId != null){
                    Log.d("Check Video ID", videoId!!)
                    Log.d("Check videoId in ViewModel", viewModel.videoId.value.toString())
                    viewModel.getSongFull(videoId!!)
                    viewModel.songFull.observe(viewLifecycleOwner) {
                        Log.w("Check Song Full", it?.videoDetails?.title.toString())
                        if (it != null && it.videoDetails?.videoId == videoId && it.videoDetails?.videoId != null) {
                            val track = it.toTrack()
                            Queue.clear()
                            Queue.setNowPlaying(track)
                            viewModel.simpleMediaServiceHandler?.reset()
                            viewModel.resetRelated()
//                            if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
//                                requireActivity().stopService(
//                                    Intent(
//                                        requireContext(),
//                                        FetchQueue::class.java
//                                    )
//                                )
//                            }
                            viewModel.loadMediaItemFromTrack(track, SHARE)
                            viewModel.videoId.postValue(track.videoId)
                            viewModel.from.postValue(from)
                            updateUIfromQueueNowPlaying()
                            miniplayer.visibility = View.GONE
                            bottom.visibility = View.GONE
//                            lifecycleScope.launch {
//                                repeatOnLifecycle(Lifecycle.State.CREATED) {
//                                    viewModel.firstTrackAdded.collect { added ->
//                                        if (added && type == Config.SHARE) {
//                                            viewModel.changeFirstTrackAddedToFalse()
////                                            viewModel.getFormat(track.videoId)
//                                            getRelated(track.videoId)
//                                        }
//                                    }
//                                }
//                            }
                        }
                    }
                }
            }

            VIDEO_CLICK -> {
                viewModel.playlistId.value = null
                if (viewModel.videoId.value == videoId) {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                } else {
//                if (!viewModel.songTransitions.value){
                    Log.i("Now Playing Fragment", "Video Click")
                    binding.ivArt.setImageResource(0)
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        viewModel.simpleMediaServiceHandler?.reset()
                        viewModel.resetRelated()
//                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
//                            requireActivity().stopService(
//                                Intent(
//                                    requireContext(),
//                                    FetchQueue::class.java
//                                )
//                            )
//                        }
                        viewModel.loadMediaItemFromTrack(it, VIDEO_CLICK)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        updateUIfromQueueNowPlaying()
//                        lifecycleScope.launch {
//                            repeatOnLifecycle(Lifecycle.State.CREATED) {
//                                viewModel.firstTrackAdded.collect { added ->
//                                    if (added && type == Config.VIDEO_CLICK) {
//                                        viewModel.changeFirstTrackAddedToFalse()
////                                        viewModel.getFormat(it.videoId)
//                                        getRelated(it.videoId)
//                                    }
//                                }
//                            }
//                        }
                    }
                    //}
//                viewModel.loadMediaItems(videoId!!)
                }
            }

            ALBUM_CLICK -> {
                    if (playlistId != null) {
                        viewModel.playlistId.value = playlistId
                    }
//                if (!viewModel.songTransitions.value){
                Log.i("Now Playing Fragment", "Album Click")
                binding.ivArt.setImageResource(0)
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        viewModel.simpleMediaServiceHandler?.reset()
                        viewModel.resetRelated()
//                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
//                            requireActivity().stopService(
//                                Intent(
//                                    requireContext(),
//                                    FetchQueue::class.java
//                                )
//                            )
//                        }
                        viewModel.loadMediaItemFromTrack(it, ALBUM_CLICK, index)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
//                        viewModel.resetLyrics()
//                        viewModel.getLyrics(it.title + " " + it.artists?.first()?.name)
                        updateUIfromQueueNowPlaying()
//                        viewModel._lyrics.observe(viewLifecycleOwner){ resourceLyrics ->
//                            when(resourceLyrics){
//                                is Resource.Success -> {
//                                    if (resourceLyrics.data != null) {
//                                        viewModel.insertLyrics(resourceLyrics.data.toLyricsEntity(it.videoId))
//                                        viewModel.parseLyrics(resourceLyrics.data)
//                                    }
//                                }
//                                is Resource.Error -> {
//                                    viewModel.getSavedLyrics(it.videoId)
//                                }
//                            }
//                        }
                        Log.d("check index", index.toString())
//                        lifecycleScope.launch {
//                            repeatOnLifecycle(Lifecycle.State.CREATED) {
//                                viewModel.firstTrackAdded.collect { added ->
//                                    if (added && type == Config.ALBUM_CLICK) {
//                                        viewModel.changeFirstTrackAddedToFalse()
////                                        viewModel.getFormat(it.videoId)
//                                        if (index == null) {
//                                            fetchSourceFromQueue(downloaded = downloaded ?: 0)
//                                        } else {
//                                            fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
//                                        }
//                                    }
//                                }
//                            }
//                        }
                }
            }

            PLAYLIST_CLICK -> {
                    if (playlistId != null) {
                        viewModel.playlistId.value = playlistId
                    }
                Log.i("Now Playing Fragment", "Playlist Click")
                binding.ivArt.setImageResource(0)
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        viewModel.simpleMediaServiceHandler?.reset()
                        viewModel.resetRelated()
//                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
//                            requireActivity().stopService(
//                                Intent(
//                                    requireContext(),
//                                    FetchQueue::class.java
//                                )
//                            )
//                        }
                        Log.d("check index", index.toString())
                        viewModel.loadMediaItemFromTrack(it, PLAYLIST_CLICK, index)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
//                        viewModel.resetLyrics()
//                        viewModel.getLyrics(it.title + " " + it.artists?.first()?.name)
                        updateUIfromQueueNowPlaying()
//                        viewModel._lyrics.observe(viewLifecycleOwner){ resourceLyrics ->
//                            when(resourceLyrics){
//                                is Resource.Success -> {
//                                    if (resourceLyrics.data != null) {
//                                        viewModel.insertLyrics(resourceLyrics.data.toLyricsEntity(it.videoId))
//                                        viewModel.parseLyrics(resourceLyrics.data)
//                                    }
//                                }
//                                is Resource.Error -> {
//                                    viewModel.getSavedLyrics(it.videoId)
//                                }
//                            }
//                        }
//                        lifecycleScope.launch {
//                            repeatOnLifecycle(Lifecycle.State.CREATED) {
//                                viewModel.firstTrackAdded.collect { added ->
//                                    if (added && type == Config.PLAYLIST_CLICK) {
//                                        viewModel.changeFirstTrackAddedToFalse()
////                                        viewModel.getFormat(it.videoId)
//                                        if (index == null) {
//                                            fetchSourceFromQueue(downloaded = downloaded ?: 0)
//                                        } else {
//                                            fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
//                                        }
//                                    }
//                                }
//                            }
//                        }
                }
            }

            MINIPLAYER_CLICK -> {
                videoId = viewModel.videoId.value
                from = viewModel.from.value
                metadataCurSong = viewModel.metadata.value?.data
                gradientDrawable = viewModel.gradientDrawable.value
                lyricsBackground = viewModel.lyricsBackground.value
//                if (viewModel.progress.value in 0.0..1.0) {
//                    binding.progressSong.value = viewModel.progress.value * 100
//                }
                if (videoId == null) {
                    videoId = viewModel.nowPlayingMediaItem.value?.mediaId
                    viewModel.videoId.postValue(videoId)
                }
                updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
            }
        }

        lifecycleScope.launch {
//            val job7 = launch {
//                viewModel.songTransitions.collectLatest { isChanged ->
//                    if (isChanged) {
//                        val song = viewModel.getCurrentMediaItem()
//                        if (song != null) {
//                            Log.i("Now Playing Fragment", "Bên dưới")
//                            Log.d("Song Transition", "Song Transition")
//                            videoId = viewModel.videoId.value
//                            binding.ivArt.setImageResource(0)
//                            binding.loadingArt.visibility = View.VISIBLE
//                            Log.d("Check Lyrics", viewModel._lyrics.value?.data.toString())
//                            updateUIfromCurrentMediaItem(song)
//                            simpleMediaServiceHandler.setCurrentSongIndex(viewModel.getCurrentMediaItemIndex())
//                            viewModel.changeSongTransitionToFalse()
//                        }
//                    }
//                }
//            }
            repeatOnLifecycle(Lifecycle.State.CREATED) {

                val job7 =
                    launch {
                        viewModel.simpleMediaServiceHandler?.nowPlaying?.collectLatest { song ->
                            if (song != null) {
//                                viewModel.getFormat(song.mediaId)
                                Log.i("Now Playing Fragment", "song ${song.mediaMetadata.title}")
                                videoId = viewModel.videoId.value
                                binding.ivArt.setImageResource(0)
                                binding.loadingArt.visibility = View.VISIBLE
                                Log.d("Check Lyrics", viewModel._lyrics.value?.data.toString())
                                updateUIfromCurrentMediaItem(song)
                                viewModel.simpleMediaServiceHandler?.setCurrentSongIndex(viewModel.getCurrentMediaItemIndex())
                                viewModel.changeSongTransitionToFalse()
                            }
                        }
                    }
                val job13 = launch {
                    viewModel.progress.collect {
                        if (it in 0.0..1.0) {
                            binding.progressSong.value = it * 100
                        }
                    }
                }
                val job1 = launch {
                    viewModel.progressString.collect {
                        binding.tvCurrentTime.text = it
//                        if (viewModel.progress.value * 100 in 0f..100f) {
//                            binding.progressSong.value = viewModel.progress.value * 100
////                        songChangeListener.onUpdateProgressBar(viewModel.progress.value * 100)
//                        }
                    }
                }
                val job2 = launch {
                    viewModel.isPlaying.collect {
                        Log.d("Check Song Transistion", "${viewModel.songTransitions.value}")
                        if (it) {
                            binding.btPlayPause.setImageResource(R.drawable.baseline_pause_circle_24)
//                        songChangeListener.onIsPlayingChange()
                        } else {
                            binding.btPlayPause.setImageResource(R.drawable.baseline_play_circle_24)
//                        songChangeListener.onIsPlayingChange()
                        }
                    }
                }
                //Update progress bar from buffered percentage
                val job3 = launch {
                    viewModel.bufferedPercentage.collect {
                        binding.buffered.progress = it
//                    Log.d("buffered", it.toString())
                    }
                }
                //Check if song is ready to play. And make progress bar indeterminate
                val job4 = launch {
                    viewModel.notReady.observe(viewLifecycleOwner) {
                        binding.buffered.isIndeterminate = it
                    }
                }
                val job5 = launch {
                    viewModel.progressMillis.collect {
                        if (viewModel._lyrics.value?.data != null) {
//                            val temp = viewModel.getLyricsString(it)
                            val lyrics = viewModel._lyrics.value!!.data
                            binding.tvSyncState.text = when (viewModel.getLyricsSyncState()) {
                                Config.SyncState.NOT_FOUND -> null
                                Config.SyncState.LINE_SYNCED -> getString(R.string.line_synced)
                                Config.SyncState.UNSYNCED -> getString(R.string.unsynced)
                            }
                            viewModel._lyrics.value?.data?.let {
                                lyricsFullAdapter.updateOriginalLyrics(it)
                                lyricsFullAdapter.setActiveLyrics(-1)
                            }
                            val index = viewModel.getActiveLyrics(it)
                            if (index != null) {
                                if (lyrics?.lines?.get(0)?.words == "Lyrics not found") {
                                    binding.lyricsLayout.visibility = View.GONE
                                    binding.lyricsTextLayout.visibility = View.GONE
                                } else {
                                    viewModel._lyrics.value!!.data?.let { it1 ->
                                        lyricsAdapter.updateOriginalLyrics(
                                            it1
                                        )
                                        if (viewModel.getLyricsSyncState() == Config.SyncState.LINE_SYNCED) {
                                            binding.rvLyrics.addOnItemTouchListener(disableScrolling)
                                            lyricsAdapter.setActiveLyrics(index)
                                            lyricsFullAdapter.setActiveLyrics(index)
                                            if (index == -1) {
                                                binding.rvLyrics.smoothScrollToPosition(0)
                                            }
                                            else {
                                                binding.rvLyrics.smoothScrollToPosition(index)
                                            }
                                        }
                                        else if (viewModel.getLyricsSyncState() == Config.SyncState.UNSYNCED) {
                                            lyricsAdapter.setActiveLyrics(-1)
                                            binding.rvLyrics.removeOnItemTouchListener(disableScrolling)
                                        }
//                                        it1.lines?.find { line -> line.words == temp.nowLyric }
//                                            ?.let { it2 ->
//                                                lyricsAdapter.setActiveLyrics(it2)
//                                                binding.rvLyrics.smoothScrollToPosition(it1.lines.indexOf(it2))
//                                            }
                                    }

                                    if (binding.btFull.text == getString(R.string.show)) {
                                        binding.lyricsTextLayout.visibility = View.VISIBLE
                                    }
                                    binding.lyricsLayout.visibility = View.VISIBLE
//                                    if (temp.nowLyric != null) {
//                                        binding.tvNowLyrics.visibility = View.VISIBLE
//                                        binding.tvNowLyrics.text = temp.nowLyric
//                                    } else {
//                                        binding.tvNowLyrics.visibility = View.GONE
//                                    }
//                                    if (temp.prevLyrics != null) {
//                                        binding.tvPrevLyrics.visibility = View.VISIBLE
//                                        if (temp.prevLyrics.size > 1) {
//                                            val txt = temp.prevLyrics[0] + "\n" + temp.prevLyrics[1]
//                                            binding.tvPrevLyrics.text = txt
//                                        } else {
//                                            binding.tvPrevLyrics.text = temp.prevLyrics[0]
//                                        }
//                                    } else {
//                                        binding.tvPrevLyrics.visibility = View.GONE
//                                    }
//                                    if (temp.nextLyric != null) {
//                                        binding.tvNextLyrics.visibility = View.VISIBLE
//                                        if (temp.nextLyric.size > 1) {
//                                            val txt = temp.nextLyric[0] + "\n" + temp.nextLyric[1]
//                                            binding.tvNextLyrics.text = txt
//                                        } else {
//                                            binding.tvNextLyrics.text = temp.nextLyric[0]
//                                        }
//                                    } else {
//                                        binding.tvNextLyrics.visibility = View.GONE
//                                    }
                                }
                            }
                        } else {
                            binding.lyricsLayout.visibility = View.GONE
                            binding.lyricsTextLayout.visibility = View.GONE
                        }
                    }
                }
                val job8 = launch {
                    viewModel.shuffleModeEnabled.collect { shuffle ->
                        when (shuffle) {
                            true -> {
                                binding.btShuffle.setImageResource(R.drawable.baseline_shuffle_24_enable)
                            }

                            false -> {
                                binding.btShuffle.setImageResource(R.drawable.baseline_shuffle_24)
                            }
                        }
                    }
                }
                val job9 = launch {
                    viewModel.repeatMode.collect { repeatMode ->
                        when (repeatMode) {
                            RepeatState.None -> {
                                binding.btRepeat.setImageResource(R.drawable.baseline_repeat_24)
                            }

                            RepeatState.One -> {
                                binding.btRepeat.setImageResource(R.drawable.baseline_repeat_one_24)
                            }

                            RepeatState.All -> {
                                binding.btRepeat.setImageResource(R.drawable.baseline_repeat_24_enable)
                            }
                        }
                    }
                }
                val job10 = launch {
                    viewModel.liked.collect { liked ->
                        binding.cbFavorite.isChecked = liked
                    }
                }
                val job11 = launch {
                    viewModel.simpleMediaServiceHandler?.previousTrackAvailable?.collect { available ->
                        setEnabledAll(binding.btPrevious, available)
                    }
                }
                val job12 = launch {
                    viewModel.simpleMediaServiceHandler?.nextTrackAvailable?.collect { available ->
                        setEnabledAll(binding.btNext, available)
                    }
                }
                val job14 = launch {
                    viewModel.format.collectLatest { format ->
                        if (format != null) {
                            binding.uploaderLayout.visibility = View.VISIBLE
                            binding.tvUploader.text = format.uploader
                            binding.ivAuthor.load(format.uploaderThumbnail)
                            binding.tvSubCount.text = format.uploaderSubCount
                            if (format.itag == 22 || format.itag == 18) {
                                binding.playerLayout.visibility = View.VISIBLE
                                binding.ivArt.visibility = View.INVISIBLE
                                binding.loadingArt.visibility = View.GONE
                                viewModel.updateSubtitle(format.youtubeCaptionsUrl)
                            } else {
                                binding.playerLayout.visibility = View.GONE
                                binding.ivArt.visibility = View.VISIBLE
                            }
                        }
                        else {
                            binding.uploaderLayout.visibility = View.GONE
                            binding.playerLayout.visibility = View.GONE
                            binding.ivArt.visibility = View.VISIBLE
                        }
                    }
                }
                val job15 = launch {
                    viewModel.related.collectLatest { response ->
                        if (response != null) {
                            when (response) {
                                is Resource.Success -> {
                                    val data = response.data!!
                                    data.add(Queue.getNowPlaying()!!)
                                    val listWithoutDuplicateElements: ArrayList<Track> = ArrayList()
                                    for (element in data) {
                                        // Check if element not exist in list, perform add element to list
                                        if (!listWithoutDuplicateElements.contains(element)) {
                                            listWithoutDuplicateElements.add(element)
                                        }
                                    }
                                    Log.d("Queue", "getRelated: ${listWithoutDuplicateElements.size}")
                                    Queue.clear()
                                    Queue.addAll(listWithoutDuplicateElements)
                                    Log.d("Queue", "getRelated: ${Queue.getQueue().size}")
                                    viewModel.addQueueToPlayer()
//                                if (!requireContext().isMyServiceRunning(FetchQueue::class.java)) {
//                                    requireActivity().startService(
//                                        Intent(
//                                            requireContext(),
//                                            FetchQueue::class.java
//                                        )
//                                    )
//                                } else {
//                                    requireActivity().stopService(
//                                        Intent(
//                                            requireContext(),
//                                            FetchQueue::class.java
//                                        )
//                                    )
//                                    requireActivity().startService(
//                                        Intent(
//                                            requireContext(),
//                                            FetchQueue::class.java
//                                        )
//                                    )
//                                }
                                }

                                is Resource.Error -> {
                                    if (response.message != "null") {
                                        Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                                        Log.d("Error", "${response.message}")
                                    }
                                }
                            }
                        }
                    }
                }
//                val job16 = launch {
//                    viewModel.firstTrackAdded.collectLatest { added ->
//                        if (added) {
//                            when(type) {
//                                Config.SONG_CLICK -> {
//                                    viewModel.nowPLaying.first()?.let { getRelated(it.mediaId) }
//                                    viewModel.changeFirstTrackAddedToFalse()
//                                }
//                                Config.SHARE -> {
//                                    viewModel.nowPLaying.first()?.let { getRelated(it.mediaId) }
//                                    viewModel.changeFirstTrackAddedToFalse()
//                                }
//                                Config.VIDEO_CLICK -> {
//
////                                        viewModel.getFormat(it.videoId)
//                                    viewModel.nowPLaying.first()?.let { getRelated(it.mediaId) }
//                                    viewModel.changeFirstTrackAddedToFalse()
//                                }
//                                Config.ALBUM_CLICK -> {
//                                    if (index == null) {
////                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
//                                        viewModel.loadPlaylistOrAlbum(downloaded = downloaded ?: 0)
//                                    } else {
////                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
//                                        viewModel.loadPlaylistOrAlbum(downloaded = downloaded ?: 0, index = index)
//                                    }
//
//                                    viewModel.changeFirstTrackAddedToFalse()
//                                }
//                                Config.PLAYLIST_CLICK -> {
//                                    if (index == null) {
////                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
//                                        viewModel.loadPlaylistOrAlbum(downloaded = downloaded ?: 0)
//                                    } else {
////                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
//                                        viewModel.loadPlaylistOrAlbum(downloaded = downloaded ?: 0, index = index)
//                                    }
//                                    viewModel.changeFirstTrackAddedToFalse()
//                                }
//                            }
//                        }
//                    }
//                }
                val job16 = launch {
                    viewModel.translateLyrics.collect {
                        lyricsAdapter.updateTranslatedLyrics(it)
                        lyricsFullAdapter.updateTranslatedLyrics(it)
                    }
                }
                val job17 = launch {
                    viewModel._lyrics.collectLatest { lyrics ->
                        if (lyrics != null && lyrics is Resource.Success) {
                            if (viewModel.getLyricsProvier() == DataStoreManager.MUSIXMATCH) {
                                binding.tvLyricsProvider.text = getString(R.string.lyrics_provider)
                            }
                            else if (viewModel.getLyricsProvier() == DataStoreManager.YOUTUBE) {
                                binding.tvLyricsProvider.text = getString(R.string.lyrics_provider_youtube)
                            }
                        }
                    }
                }
                val job18 = launch {
                    viewModel.duration.collect {
                        if (viewModel.formatDuration(it).contains('-')) {
                            binding.tvFullTime.text = getString(R.string.na_na)
                        } else {
                            binding.tvFullTime.text = viewModel.formatDuration(it)
                        }
                    }
                }
                job1.join()
                job2.join()
                job3.join()
                job4.join()
                job5.join()
                job7.join()
                job8.join()
                job9.join()
                job10.join()
                job13.join()
                job11.join()
                job12.join()
                job14.join()
                job15.join()
                job16.join()
                job17.join()
                job18.join()
            }
        }
        binding.btFull.setOnClickListener {
            if (binding.btFull.text == getString(R.string.show)) {
                binding.btFull.text = getString(R.string.hide)
                binding.lyricsTextLayout.visibility = View.GONE
                binding.lyricsFullLayout.visibility = View.VISIBLE
            } else {
                binding.btFull.text = getString(R.string.show)
                binding.lyricsTextLayout.visibility = View.VISIBLE
                binding.lyricsFullLayout.visibility = View.GONE
            }
        }

        binding.progressSong.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {

            }

            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.onUIEvent(UIEvent.UpdateProgress(slider.value))
            }
        })
        binding.btFullscreen.setOnClickListener {
            binding.playerView.player = null
            isFullScreen = true
            findNavController().navigateSafe(R.id.action_global_fullscreenFragment)

//                requireActivity().requestedOrientation =
//                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
//                val parent = fullscreen.root.parent as View
//                parent.fitsSystemWindows = true
//                val params =
//                    parent.layoutParams as CoordinatorLayout.LayoutParams
//                val behavior = params.behavior
//                if (behavior != null && behavior is BottomSheetBehavior<*>) {
//                    behavior.peekHeight = binding.root.height
//                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                    behavior.addBottomSheetCallback(object : BottomSheetCallback() {
//                        override fun onStateChanged( bottomSheet: View, newState: Int) {
//                            if (newState == BottomSheetBehavior.STATE_DRAGGING) {
//                                behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                            }
//                        }
//
//                        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
//                    })
//                }


//            }
        }
        binding.playerLayout.setOnClickListener {
            val shortAnimationDuration =
                resources.getInteger(android.R.integer.config_mediumAnimTime)
            if (binding.overlay.visibility == View.VISIBLE) {
                binding.overlay.visibility = View.GONE
                overlayJob?.cancel()
            } else {
                binding.overlay.alpha = 0f
                binding.overlay.apply {
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .setDuration(shortAnimationDuration.toLong())
                        .setListener(null)
                }
                overlayJob?.cancel()
                overlayJob = lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.CREATED) {
                        delay(3000)
                        binding.overlay.visibility = View.GONE
                    }
                }
            }
        }

        binding.btPlayPause.setOnClickListener {
            viewModel.onUIEvent(UIEvent.PlayPause)
        }
        binding.btNext.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Next)
        }
        binding.btPrevious.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Previous)
        }
        binding.btShuffle.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Shuffle)
        }
        binding.btRepeat.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Repeat)
        }

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.btQueue.setOnClickListener {
            findNavController().navigateSafe(R.id.action_global_queueFragment)
        }
        binding.btSongInfo.setOnClickListener {
            findNavController().navigateSafe(R.id.action_global_infoFragment)
        }
        binding.cbFavorite.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                viewModel.getCurrentMediaItem()?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(
                        nowPlayingSong.mediaId,
                        false
                    )
                    viewModel.updateLikeInNotification(false)
                }
            } else {
                viewModel.getCurrentMediaItem()?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(
                        nowPlayingSong.mediaId,
                        true
                    )
                    viewModel.updateLikeInNotification(true)
                }
            }
        }
        binding.uploaderLayout.setOnClickListener {
            findNavController().navigateSafe(
                R.id.action_global_artistFragment,
                Bundle().apply {
                    putString("channelId", runBlocking { viewModel.format.first()?.uploaderId })
                })
        }
        binding.tvSongArtist.setOnClickListener {
            if (!viewModel.simpleMediaServiceHandler?.catalogMetadata.isNullOrEmpty()) {
                val song = viewModel.simpleMediaServiceHandler!!.catalogMetadata[viewModel.getCurrentMediaItemIndex()]
                if (song.artists?.firstOrNull()?.id != null) {
                    findNavController().navigateSafe(
                        R.id.action_global_artistFragment,
                        Bundle().apply {
                            putString("channelId", song.artists.firstOrNull()?.id)
                        })
                }
            }
        }
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.now_playing_dialog_menu_item_more -> {
                    if (!viewModel.simpleMediaServiceHandler?.catalogMetadata.isNullOrEmpty()) {
                        viewModel.refreshSongDB()
                        val dialog = BottomSheetDialog(requireContext())
                        dialog.apply {
                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                        val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                        with(bottomSheetView) {
                            lifecycleScope.launch {
                                viewModel.simpleMediaServiceHandler?.sleepMinutes?.collect { min ->
                                    if (min > 0) {
                                        tvSleepTimer.text =
                                            getString(R.string.sleep_timer, min.toString())
                                        ivSleepTimer.setImageResource(R.drawable.alarm_enable)
                                    } else {
                                        tvSleepTimer.text = getString(R.string.sleep_timer_off)
                                        ivSleepTimer.setImageResource(R.drawable.baseline_access_alarm_24)
                                    }
                                }
                            }
                            btAddQueue.visibility = View.GONE
                            if (runBlocking { viewModel.liked.first() }) {
                                tvFavorite.text = getString(R.string.liked)
                                cbFavorite.isChecked = true
                            } else {
                                tvFavorite.text = getString(R.string.like)
                                cbFavorite.isChecked = false
                            }
                            when (viewModel.songDB.value?.downloadState) {
                                DownloadState.STATE_PREPARING -> {
                                    tvDownload.text = getString(R.string.preparing)
                                    ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                    setEnabledAll(btDownload, true)
                                }

                                DownloadState.STATE_NOT_DOWNLOADED -> {
                                    tvDownload.text = getString(R.string.download)
                                    ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                    setEnabledAll(btDownload, true)
                                }

                                DownloadState.STATE_DOWNLOADING -> {
                                    tvDownload.text = getString(R.string.downloading)
                                    ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                                    setEnabledAll(btDownload, true)
                                }

                                DownloadState.STATE_DOWNLOADED -> {
                                    tvDownload.text = getString(R.string.downloaded)
                                    ivDownload.setImageResource(R.drawable.baseline_downloaded)
                                    setEnabledAll(btDownload, true)
                                }
                            }
                            if (!viewModel.simpleMediaServiceHandler?.catalogMetadata.isNullOrEmpty()) {
                                val song =
                                    viewModel.simpleMediaServiceHandler!!.catalogMetadata[viewModel.getCurrentMediaItemIndex()]
                                tvSongTitle.text = song.title
                                tvSongTitle.isSelected = true
                                tvSongArtist.text = song.artists.toListName().connectArtists()
                                tvSongArtist.isSelected = true
                                ivThumbnail.load(song.thumbnails?.last()?.url)

                                btLike.setOnClickListener {
                                    if (cbFavorite.isChecked) {
                                        cbFavorite.isChecked = false
                                        tvFavorite.text = getString(R.string.like)
                                        viewModel.updateLikeStatus(song.videoId, false)
                                    } else {
                                        cbFavorite.isChecked = true
                                        tvFavorite.text = getString(R.string.liked)
                                        viewModel.updateLikeStatus(song.videoId, true)
                                    }
                                }
                                btRadio.setOnClickListener {
                                    val args = Bundle()
                                    args.putString("radioId", "RDAMVM${song.videoId}")
                                    args.putString(
                                        "videoId",
                                        song.videoId
                                    )
                                    dialog.dismiss()
                                    findNavController().navigateSafe(
                                        R.id.action_global_playlistFragment,
                                        args
                                    )
                                }
                                btSleepTimer.setOnClickListener {
                                    Log.w("Sleep Timer", "onClick")
                                    if (viewModel.sleepTimerRunning.value == true) {
                                        MaterialAlertDialogBuilder(requireContext())
                                            .setTitle(getString(R.string.warning))
                                            .setMessage(getString(R.string.sleep_timer_warning))
                                            .setPositiveButton(getString(R.string.yes)) { d, _ ->
                                                viewModel.stopSleepTimer()
                                                Toast.makeText(
                                                    requireContext(),
                                                    getString(R.string.sleep_timer_off_done),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                d.dismiss()
                                            }
                                            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                                                d.dismiss()
                                            }
                                            .show()
                                    } else {
                                        val d = BottomSheetDialog(requireContext())
                                        d.apply {
                                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
                                        }
                                        val v = BottomSheetSleepTimerBinding.inflate(layoutInflater)
                                        v.btSet.setOnClickListener {
                                            val min = v.etTime.editText?.text.toString()
                                            if (min.isNotBlank() && min.toInt() > 0) {
                                                viewModel.setSleepTimer(min.toInt())
                                                d.dismiss()
                                            } else {
                                                Toast.makeText(
                                                    requireContext(),
                                                    getString(R.string.sleep_timer_set_error),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        d.setContentView(v.root)
                                        d.setCancelable(true)
                                        d.show()
                                    }
                                }
                                btAddPlaylist.setOnClickListener {
                                    viewModel.getAllLocalPlaylist()
                                    val listLocalPlaylist: ArrayList<LocalPlaylistEntity> =
                                        arrayListOf()
                                    val addPlaylistDialog = BottomSheetDialog(requireContext())
                                    addPlaylistDialog.apply {
                                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                                    }
                                    val viewAddPlaylist =
                                        BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
                                    val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
                                    viewAddPlaylist.rvLocalPlaylists.apply {
                                        adapter = addToAPlaylistAdapter
                                        layoutManager = LinearLayoutManager(requireContext())
                                    }
                                    viewModel.localPlaylist.observe(viewLifecycleOwner) { list ->
                                        Log.d("Check Local Playlist", list.toString())
                                        listLocalPlaylist.clear()
                                        listLocalPlaylist.addAll(list)
                                        addToAPlaylistAdapter.updateList(listLocalPlaylist)
                                    }
                                    addToAPlaylistAdapter.setOnItemClickListener(object :
                                        AddToAPlaylistAdapter.OnItemClickListener {
                                        override fun onItemClick(position: Int) {
                                            val playlist = listLocalPlaylist[position]
                                            viewModel.updateInLibrary(song.videoId)
                                            val tempTrack = ArrayList<String>()
                                            if (playlist.tracks != null) {
                                                tempTrack.addAll(playlist.tracks)
                                            }
                                            if (!tempTrack.contains(song.videoId) && playlist.syncedWithYouTubePlaylist == 1 && playlist.youtubePlaylistId != null) {
                                                viewModel.addToYouTubePlaylist(
                                                    playlist.id,
                                                    playlist.youtubePlaylistId,
                                                    song.videoId
                                                )
                                            }
                                            if (!tempTrack.contains(song.videoId)) {
                                                viewModel.insertPairSongLocalPlaylist(
                                                    PairSongLocalPlaylist(
                                                        playlistId = playlist.id, songId = song.videoId, position = tempTrack.size, inPlaylist = LocalDateTime.now()
                                                    )
                                                )
                                                tempTrack.add(song.videoId)
                                            }
                                            tempTrack.add(song.videoId)
                                            tempTrack.removeConflicts()
                                            viewModel.updateLocalPlaylistTracks(
                                                tempTrack,
                                                playlist.id
                                            )
                                            addPlaylistDialog.dismiss()
                                            dialog.dismiss()
                                        }
                                    })
                                    addPlaylistDialog.setContentView(viewAddPlaylist.root)
                                    addPlaylistDialog.setCancelable(true)
                                    addPlaylistDialog.show()
                                }

                                btSeeArtists.setOnClickListener {
                                    val subDialog = BottomSheetDialog(requireContext())
                                    subDialog.apply {
                                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                                    }
                                    val subBottomSheetView =
                                        BottomSheetSeeArtistOfNowPlayingBinding.inflate(
                                            layoutInflater
                                        )
                                    if (song.artists != null) {
                                        val artistAdapter =
                                            SeeArtistOfNowPlayingAdapter(song.artists)
                                        subBottomSheetView.rvArtists.apply {
                                            adapter = artistAdapter
                                            layoutManager = LinearLayoutManager(requireContext())
                                        }
                                        artistAdapter.setOnClickListener(object :
                                            SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                            override fun onItemClick(position: Int) {
                                                val artist = song.artists[position]
                                                if (artist.id != null) {
                                                    findNavController().navigateSafe(
                                                        R.id.action_global_artistFragment,
                                                        Bundle().apply {
                                                            putString("channelId", artist.id)
                                                        })
                                                    subDialog.dismiss()
                                                    dialog.dismiss()
                                                }
                                            }

                                        })
                                    }

                                    subDialog.setCancelable(true)
                                    subDialog.setContentView(subBottomSheetView.root)
                                    subDialog.show()
                                }
                                btChangeLyricsProvider.setOnClickListener {
                                    var mainLyricsProvider = viewModel.getLyricsProvier()
                                    var checkedIndex = if (mainLyricsProvider == DataStoreManager.MUSIXMATCH) 0 else 1
                                    val dialogChange = MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(getString(R.string.main_lyrics_provider))
                                        .setSingleChoiceItems(LYRICS_PROVIDER.items, checkedIndex) { _, which ->
                                            checkedIndex = which
                                        }
                                        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                                            if (checkedIndex != -1) {
                                                if (checkedIndex == 0) {
                                                    if (mainLyricsProvider != DataStoreManager.MUSIXMATCH) {
                                                        viewModel.setLyricsProvider(DataStoreManager.MUSIXMATCH)
                                                    }
                                                } else if (checkedIndex == 1){
                                                    if (mainLyricsProvider != DataStoreManager.YOUTUBE) {
                                                        viewModel.setLyricsProvider(DataStoreManager.YOUTUBE)
                                                    }
                                                }
                                            }
                                            dialog.dismiss()
                                        }
                                    dialogChange.show()
                                }
                                btShare.setOnClickListener {
                                    val shareIntent = Intent(Intent.ACTION_SEND)
                                    shareIntent.type = "text/plain"
                                    val url = "https://youtube.com/watch?v=${song.videoId}"
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                                    val chooserIntent = Intent.createChooser(
                                        shareIntent,
                                        getString(R.string.share_url)
                                    )
                                    startActivity(chooserIntent)
                                }
                                btDownload.setOnClickListener {
                                    if (tvDownload.text == getString(R.string.download)) {
                                        Log.d("Download", "onClick: ${song.videoId}")
                                        viewModel.updateDownloadState(
                                            song.videoId,
                                            DownloadState.STATE_PREPARING
                                        )
                                        val downloadRequest =
                                            DownloadRequest.Builder(
                                                song.videoId,
                                                song.videoId.toUri()
                                            )
                                                .setData(song.title.toByteArray())
                                                .setCustomCacheKey(song.videoId)
                                                .build()
                                        viewModel.updateDownloadState(
                                            song.videoId,
                                            DownloadState.STATE_DOWNLOADING
                                        )
                                        viewModel.getDownloadStateFromService(song.videoId)
                                        DownloadService.sendAddDownload(
                                            requireContext(),
                                            MusicDownloadService::class.java,
                                            downloadRequest,
                                            false
                                        )
                                        lifecycleScope.launch {
                                            viewModel.downloadState.collect { download ->
                                                if (download != null) {
                                                    when (download.state) {
                                                        Download.STATE_DOWNLOADING -> {
                                                            viewModel.updateDownloadState(
                                                                song.videoId,
                                                                DownloadState.STATE_DOWNLOADING
                                                            )
                                                            tvDownload.text =
                                                                getString(R.string.downloading)
                                                            ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                                                            setEnabledAll(btDownload, true)
                                                        }

                                                        Download.STATE_FAILED -> {
                                                            viewModel.updateDownloadState(
                                                                song.videoId,
                                                                DownloadState.STATE_NOT_DOWNLOADED
                                                            )
                                                            tvDownload.text =
                                                                getString(R.string.download)
                                                            ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                                            setEnabledAll(btDownload, true)
                                                            Toast.makeText(
                                                                requireContext(),
                                                                getString(androidx.media3.exoplayer.R.string.exo_download_failed),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }

                                                        Download.STATE_COMPLETED -> {
                                                            viewModel.updateDownloadState(
                                                                song.videoId,
                                                                DownloadState.STATE_DOWNLOADED
                                                            )
                                                            Toast.makeText(
                                                                requireContext(),
                                                                getString(androidx.media3.exoplayer.R.string.exo_download_completed),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            tvDownload.text =
                                                                getString(R.string.downloaded)
                                                            ivDownload.setImageResource(R.drawable.baseline_downloaded)
                                                            setEnabledAll(btDownload, true)
                                                        }

                                                        else -> {
                                                            Log.d(
                                                                "Download",
                                                                "onCreate: ${download.state}"
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (tvDownload.text == getString(R.string.downloaded) || tvDownload.text == getString(
                                            R.string.downloading
                                        )
                                    ) {
                                        DownloadService.sendRemoveDownload(
                                            requireContext(),
                                            MusicDownloadService::class.java,
                                            song.videoId,
                                            false
                                        )
                                        viewModel.updateDownloadState(
                                            song.videoId,
                                            DownloadState.STATE_NOT_DOWNLOADED
                                        )
                                        tvDownload.text = getString(R.string.download)
                                        ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                        setEnabledAll(btDownload, true)
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.removed_download),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                        dialog.setCancelable(true)
                        dialog.setContentView(bottomSheetView.root)
                        dialog.show()
                    }
                    true
                }

                else -> false
            }
        }
    }


    private fun updateUIfromQueueNowPlaying() {
        Log.w("CHECK NOW PLAYING IN QUEUE", "updateUIfromQueueNowPlaying: ${Queue.getNowPlaying()}")
        Log.d("CHECK QUEUE", "updateUIfromQueueNowPlaying: ${Queue.getQueue()}")
        val nowPlaying = Queue.getNowPlaying()
        if (nowPlaying != null) {
//            viewModel.getFormat(nowPlaying.videoId)
            binding.ivArt.setImageResource(0)
            binding.loadingArt.visibility = View.VISIBLE
            Log.d("Update UI", "current: ${nowPlaying.title}")
            var thumbUrl = nowPlaying.thumbnails?.last()?.url!!
            if (thumbUrl.contains("w120")) {
                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
            }
            binding.ivArt.load(Uri.parse(thumbUrl)) {
                diskCacheKey(nowPlaying.videoId)
                diskCachePolicy(CachePolicy.ENABLED)
                listener(
                    onStart = {
                        binding.ivArt.setImageResource(0)
                        binding.loadingArt.visibility = View.VISIBLE
                        Log.d("Update UI", "onStart: ")
                    },
                    onSuccess = { _, _ ->
                        binding.ivArt.visibility = View.VISIBLE
                        binding.loadingArt.visibility = View.GONE
                        Log.d("Update UI", "onSuccess: ")
                        if (viewModel.gradientDrawable.value != null) {
                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                                if (it != null) {
                                    var start = binding.rootLayout.background
                                    if (start == null) {
                                        start = ColorDrawable(Color.BLACK)
                                    }
                                    val transition = TransitionDrawable(arrayOf(start, it))
                                    binding.rootLayout.background = transition
                                    transition.isCrossFadeEnabled = true
                                    transition.startTransition(500)
                                }
//                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
//                                binding.lyricsLayout.setCardBackgroundColor(color)
//                                Log.d("Update UI", "Lyrics: $color")
//                                updateStatusBarColor(color)
//                            })
                                viewModel.lyricsBackground.value?.let { it1 ->
                                    binding.lyricsLayout.setCardBackgroundColor(
                                        it1
                                    )
                                }
                            }
                            Log.d("Update UI", "updateUI: NULL")
                        }
//                        songChangeListener.onNowPlayingSongChange()
                    },
                )
                transformations(
                    object : Transformation {
                        override val cacheKey: String
                            get() = nowPlaying.videoId

                        override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                            val p = Palette.from(input).generate()
                            val defaultColor = 0x000000
                            var startColor = p.getDarkVibrantColor(defaultColor)
                            Log.d("Check Start Color", "transform: $startColor")
                            if (startColor == defaultColor) {
                                startColor = p.getDarkMutedColor(defaultColor)
                                if (startColor == defaultColor) {
                                    startColor = p.getVibrantColor(defaultColor)
                                    if (startColor == defaultColor) {
                                        startColor = p.getMutedColor(defaultColor)
                                        if (startColor == defaultColor) {
                                            startColor = p.getLightVibrantColor(defaultColor)
                                            if (startColor == defaultColor) {
                                                startColor = p.getLightMutedColor(defaultColor)
                                            }
                                        }
                                    }
                                }
                                Log.d("Check Start Color", "transform: $startColor")
                            }
//                    val centerColor = 0x6C6C6C
                            val endColor = 0x1b1a1f
                            val gd = GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                intArrayOf(startColor, endColor)
                            )
                            gd.cornerRadius = 0f
                            gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                            gd.gradientRadius = 0.5f
                            gd.alpha = 150
                            val bg = ColorUtils.setAlphaComponent(startColor, 230)
                            viewModel.gradientDrawable.postValue(gd)
                            viewModel.lyricsBackground.postValue(bg)
                            return input
                        }

                    },
                    RoundedCornersTransformation(8f)
                )
            }
//            val request = ImageRequest.Builder(requireContext())
//                .data(Uri.parse(thumbUrl))
//                .diskCacheKey(nowPlaying.videoId)
//                .diskCachePolicy(CachePolicy.ENABLED)
//                .target(
//                    onStart = {
//                        binding.ivArt.setImageResource(0)
//                        binding.loadingArt.visibility = View.VISIBLE
//                        Log.d("Update UI", "onStart: ")
//                    },
//                    onSuccess = { result ->
//                        binding.ivArt.visibility = View.VISIBLE
//                        binding.loadingArt.visibility = View.GONE
//                        binding.ivArt.setImageDrawable(result)
//                        Log.d("Update UI", "onSuccess: ")
//                        if (viewModel.gradientDrawable.value != null) {
//                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
//                                binding.rootLayout.background = it
////                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
////                                binding.lyricsLayout.setCardBackgroundColor(color)
////                                Log.d("Update UI", "Lyrics: $color")
////                                updateStatusBarColor(color)
////                            })
//                                viewModel.lyricsBackground.value?.let { it1 ->
//                                    binding.lyricsLayout.setCardBackgroundColor(
//                                        it1
//                                    )
//                                }
//                            }
//                            Log.d("Update UI", "updateUI: NULL")
//                        }
////                        songChangeListener.onNowPlayingSongChange()
//                    },
//                )
//                .transformations(object : Transformation {
//                    override val cacheKey: String
//                        get() = nowPlaying.videoId
//
//                    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
//                        val p = Palette.from(input).generate()
//                        val defaultColor = 0x000000
//                        var startColor = p.getDarkVibrantColor(defaultColor)
//                        Log.d("Check Start Color", "transform: $startColor")
//                        if (startColor == defaultColor) {
//                            startColor = p.getDarkMutedColor(defaultColor)
//                            if (startColor == defaultColor) {
//                                startColor = p.getVibrantColor(defaultColor)
//                                if (startColor == defaultColor) {
//                                    startColor = p.getMutedColor(defaultColor)
//                                    if (startColor == defaultColor) {
//                                        startColor = p.getLightVibrantColor(defaultColor)
//                                        if (startColor == defaultColor) {
//                                            startColor = p.getLightMutedColor(defaultColor)
//                                        }
//                                    }
//                                }
//                            }
//                            Log.d("Check Start Color", "transform: $startColor")
//                        }
////                    val centerColor = 0x6C6C6C
//                        val endColor = 0x1b1a1f
//                        val gd = GradientDrawable(
//                            GradientDrawable.Orientation.TOP_BOTTOM,
//                            intArrayOf(startColor, endColor)
//                        )
//                        gd.cornerRadius = 0f
//                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
//                        gd.gradientRadius = 0.5f
//                        gd.alpha = 150
//                        val bg = ColorUtils.setAlphaComponent(startColor, 230)
//                        viewModel.gradientDrawable.postValue(gd)
//                        viewModel.lyricsBackground.postValue(bg)
//                        return input
//                    }
//
//                })
//                .build()
//            ImageLoader(requireContext()).enqueue(request)
            binding.topAppBar.subtitle = from
            viewModel.from.postValue(from)
            binding.tvSongTitle.text = nowPlaying.title
            binding.tvSongTitle.isSelected = true
            val tempArtist = mutableListOf<String>()
            if (nowPlaying.artists != null) {
                for (artist in nowPlaying.artists) {
                    tempArtist.add(artist.name)
                }
            }
            val artistName: String = connectArtists(tempArtist)
            binding.tvSongArtist.text = artistName
            binding.tvSongArtist.isSelected = true
            binding.tvSongTitle.visibility = View.VISIBLE
            binding.tvSongArtist.visibility = View.VISIBLE

        }
    }

    private fun updateUIfromCurrentMediaItem(mediaItem: MediaItem?) {
        if (mediaItem != null) {
            binding.ivArt.setImageResource(0)
            binding.loadingArt.visibility = View.VISIBLE
//            viewModel.getFormat(mediaItem.mediaId)
            Log.d("Update UI", "current: ${mediaItem.mediaMetadata.title}")
            binding.tvSongTitle.visibility = View.VISIBLE
            binding.tvSongArtist.visibility = View.VISIBLE
            binding.topAppBar.subtitle = from
            viewModel.from.postValue(from)
            binding.tvSongTitle.setTextAnimation(mediaItem.mediaMetadata.title.toString())
            binding.tvSongTitle.isSelected = true
            binding.tvSongArtist.setTextAnimation(mediaItem.mediaMetadata.artist.toString())
            binding.tvSongArtist.isSelected = true
            binding.ivArt.load(mediaItem.mediaMetadata.artworkUri) {
                diskCacheKey(mediaItem.mediaId)
                diskCachePolicy(CachePolicy.ENABLED)
                crossfade(true)
                crossfade(300)
                listener(
                    onStart = {
                        binding.ivArt.setImageResource(0)
                        binding.loadingArt.visibility = View.VISIBLE
                        Log.d("Update UI", "onStart: ")
                    },
                    onSuccess = { _, _ ->
                        binding.ivArt.visibility = View.VISIBLE
                        binding.loadingArt.visibility = View.GONE
                        Log.d("Update UI", "onSuccess: ")
                        if (viewModel.gradientDrawable.value != null) {
                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                                if (it != null) {
                                    var start = binding.rootLayout.background
                                    if (start == null) {
                                        start = ColorDrawable(Color.BLACK)
                                    }
                                    val transition = TransitionDrawable(arrayOf(start, it))
                                    binding.rootLayout.background = transition
                                    transition.isCrossFadeEnabled = true
                                    transition.startTransition(500)
                                }
//                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
//                                binding.lyricsLayout.setCardBackgroundColor(color)
//                                Log.d("Update UI", "Lyrics: $color")
//                                updateStatusBarColor(color)
//                            })
                                viewModel.lyricsBackground.value?.let { it1 ->
                                    binding.lyricsLayout.setCardBackgroundColor(
                                        it1
                                    )
                                }
                            }
                            Log.d("Update UI", "updateUI: NULL")
                        }
//                        songChangeListener.onNowPlayingSongChange()
                    },
                )
                transformations(object : Transformation {
                    override val cacheKey: String
                        get() = "paletteArtTransformer"

                    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                        val p = Palette.from(input).generate()
                        val defaultColor = 0x000000
                        var startColor = p.getDarkVibrantColor(defaultColor)
                        Log.d("Check Start Color", "transform: $startColor")
                        if (startColor == defaultColor) {
                            startColor = p.getDarkMutedColor(defaultColor)
                            if (startColor == defaultColor) {
                                startColor = p.getVibrantColor(defaultColor)
                                if (startColor == defaultColor) {
                                    startColor = p.getMutedColor(defaultColor)
                                    if (startColor == defaultColor) {
                                        startColor = p.getLightVibrantColor(defaultColor)
                                        if (startColor == defaultColor) {
                                            startColor = p.getLightMutedColor(defaultColor)
                                        }
                                    }
                                }
                            }
                            Log.d("Check Start Color", "transform: $startColor")
                        }
//                    val centerColor = 0x6C6C6C
                        val endColor = 0x1b1a1f
                        val gd = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(startColor, endColor)
                        )
                        gd.cornerRadius = 0f
                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                        gd.gradientRadius = 0.5f
                        gd.alpha = 150
                        val bg = ColorUtils.setAlphaComponent(startColor, 230)
                        viewModel.gradientDrawable.postValue(gd)
                        viewModel.lyricsBackground.postValue(bg)
                        return input
                    }
                })
            }
//            val request = ImageRequest.Builder(requireContext())
//                .data(mediaItem.mediaMetadata.artworkUri)
//                .diskCacheKey(mediaItem.mediaId)
//                .diskCachePolicy(CachePolicy.ENABLED)
//                .target(
//                    onStart = {
//                        binding.ivArt.setImageResource(0)
//                        binding.loadingArt.visibility = View.VISIBLE
//                        Log.d("Update UI", "onStart: ")
//                    },
//                    onSuccess = { result ->
//                        binding.ivArt.visibility = View.VISIBLE
//                        binding.loadingArt.visibility = View.GONE
//                        binding.ivArt.setImageDrawable(result)
//                        Log.d("Update UI", "onSuccess: ")
//                        if (viewModel.gradientDrawable.value != null) {
//                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
//                                if (it != null) {
//                                    val start = binding.rootLayout.background
//                                    val transition = TransitionDrawable(arrayOf(start, it))
//                                    binding.rootLayout.background = transition
//                                    transition.isCrossFadeEnabled = true
//                                    transition.startTransition(500)
//                                }
////                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
////                                binding.lyricsLayout.setCardBackgroundColor(color)
////                                Log.d("Update UI", "Lyrics: $color")
////                                updateStatusBarColor(color)
////                            })
//                                viewModel.lyricsBackground.value?.let { it1 ->
//                                    binding.lyricsLayout.setCardBackgroundColor(
//                                        it1
//                                    )
//                                }
//                            }
//                            Log.d("Update UI", "updateUI: NULL")
//                        }
////                        songChangeListener.onNowPlayingSongChange()
//                    },
//                )
//                .diskCacheKey(mediaItem.mediaMetadata.artworkUri.toString())
//                .diskCachePolicy(CachePolicy.ENABLED)
//                .transformations(object : Transformation {
//                    override val cacheKey: String
//                        get() = "paletteArtTransformer"
//
//                    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
//                        val p = Palette.from(input).generate()
//                        val defaultColor = 0x000000
//                        var startColor = p.getDarkVibrantColor(defaultColor)
//                        Log.d("Check Start Color", "transform: $startColor")
//                        if (startColor == defaultColor) {
//                            startColor = p.getDarkMutedColor(defaultColor)
//                            if (startColor == defaultColor) {
//                                startColor = p.getVibrantColor(defaultColor)
//                                if (startColor == defaultColor) {
//                                    startColor = p.getMutedColor(defaultColor)
//                                    if (startColor == defaultColor) {
//                                        startColor = p.getLightVibrantColor(defaultColor)
//                                        if (startColor == defaultColor) {
//                                            startColor = p.getLightMutedColor(defaultColor)
//                                        }
//                                    }
//                                }
//                            }
//                            Log.d("Check Start Color", "transform: $startColor")
//                        }
////                    val centerColor = 0x6C6C6C
//                        val endColor = 0x1b1a1f
//                        val gd = GradientDrawable(
//                            GradientDrawable.Orientation.TOP_BOTTOM,
//                            intArrayOf(startColor, endColor)
//                        )
//                        gd.cornerRadius = 0f
//                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
//                        gd.gradientRadius = 0.5f
//                        gd.alpha = 150
//                        val bg = ColorUtils.setAlphaComponent(startColor, 230)
//                        viewModel.gradientDrawable.postValue(gd)
//                        viewModel.lyricsBackground.postValue(bg)
//                        return input
//                    }
//
//                })
//                .build()
//            ImageLoader(requireContext()).enqueue(request)
        }
    }

    fun connectArtists(artists: List<String>): String {
        val stringBuilder = StringBuilder()

        for ((index, artist) in artists.withIndex()) {
            stringBuilder.append(artist)

            if (index < artists.size - 1) {
                stringBuilder.append(", ")
            }
        }

        return stringBuilder.toString()
    }

//    public interface OnNowPlayingSongChangeListener {
//        fun onNowPlayingSongChange()
//        fun onIsPlayingChange()
//        fun onUpdateProgressBar(progress: Float)
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        arguments?.putString("type", null)
        arguments?.putString("videoId", null)
        val activity = requireActivity()
        activity.window.navigationBarColor = Color.parseColor("#CB0B0A0A")
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val miniplayer = activity.findViewById<SwipeLayout>(R.id.miniplayer)
        if (!isFullScreen) {
            bottom.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
            bottom.visibility = View.VISIBLE
            miniplayer.animation =
                AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
            miniplayer.visibility = View.VISIBLE
        }
        isFullScreen = false
    }
}