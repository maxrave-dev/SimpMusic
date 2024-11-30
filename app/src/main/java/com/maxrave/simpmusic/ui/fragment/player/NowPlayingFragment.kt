package com.maxrave.simpmusic.ui.fragment.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.findNavController
import com.maxrave.simpmusic.ui.screen.player.NowPlayingScreen
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.viewModel.SharedViewModel

@UnstableApi
class NowPlayingFragment : Fragment() {
    val viewModel by activityViewModels<SharedViewModel>()
    private lateinit var composeView: ComposeView
//    private var overlayJob: Job? = null
//
//    private var canvasOverlayJob: Job? = null
//
//    private var player: ExoPlayer? = null
//
//    private var isFullScreen = false
//
// //    private lateinit var songChangeListener: OnNowPlayingSongChangeListener
// //    override fun onAttach(context: Context) {
// //        super.onAttach(context)
// //        if (context is OnNowPlayingSongChangeListener) {
// //            songChangeListener = context
// //        } else {
// //            throw RuntimeException("$context must implement OnNowPlayingSongChangeListener")
// //        }
// //    }
//
//    override fun onResume() {
//        super.onResume()
//        Log.d("NowPlayingFragment", "onResume")
//        val scs = getScreenHeight(requireActivity())
//        binding.topAppBarLayout.getDimensions { _, hAppBar, _, _ ->
//            Log.w("Screen size", scs.toString())
//            val topAppBarHeight = hAppBar
//            binding.middleLayout.getDimensions { w, h, margin, xy ->
//                Log.w("Check Margin", "w: $w, h: $h, margin: $margin, xy: $xy")
//                val layoutParamsCopy = binding.middleLayout.layoutParams as ViewGroup.MarginLayoutParams
//                var x = margin[2]
//                //                val y = binding.topAppBarLayout.layoutParams.height + 2 * x + h + binding.middleLayout.layoutParams.height
//                binding.infoControllerLayout.getDimensions { w1, h1, margin1, xy1 ->
//                    Log.w("Check Margin", "w1: $w1, h1: $h1, margin1: $margin1, xy1: $xy1")
//                    val y = topAppBarHeight + 2 * x + h + h1
//                    Log.w("Check Margin", "y: $y")
//
//                    binding.belowControllerButtonLayout.getDimensions { _, h3, _, _ ->
//                        if (y + h3 > scs) {
//                            x = (x - ((y + h3 - scs) / 2))
//                            if (x < 0) x = h3
//                            layoutParamsCopy.bottomMargin = x
//                            layoutParamsCopy.topMargin = x
//                            binding.middleLayout.layoutParams = layoutParamsCopy
//                            Log.w("Check Margin", "y: $y, scs: $scs, x: $x")
//                        } else {
//                            x = (x + ((scs - y - h3) / 2))
//                            if (x < 0) x = h3
//                            layoutParamsCopy.bottomMargin = x
//                            layoutParamsCopy.topMargin = x
//                            binding.middleLayout.layoutParams = layoutParamsCopy
//                            Log.w("Check Margin", "y: $y, scs: $scs, x: $x")
//                        }
//                        Log.w("Check Margin", "y: $y, scs: $scs")
//                        binding.smallArtistLayout.getDimensions { _, _, _, _ ->
//                            val lop = binding.smallArtistLayout.layoutParams as ViewGroup.MarginLayoutParams
//                            lop.bottomMargin = h3 + 20
//                            binding.smallArtistLayout.layoutParams = lop
//                        }
//                    }
//                }
//            }
//        }
//        val track =
//            viewModel.canvas.value
//                ?.canvases
//                ?.firstOrNull()
//        if (track != null && track.canvas_url.contains(".mp4")) {
//            player?.stop()
//            player?.release()
//            player = ExoPlayer.Builder(requireContext()).build()
//            binding.playerCanvas.player = player
//            binding.playerCanvas.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
//            player?.repeatMode = Player.REPEAT_MODE_ONE
//            player?.setMediaItem(
//                MediaItem.fromUri(track.canvas_url),
//            )
//            player?.prepare()
//            player?.play()
//        }
//    }
//
//    private fun getScreenHeight(activity: Activity): Int =
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val windowMetrics = activity.windowManager.currentWindowMetrics
//            (windowMetrics.bounds.height())
//        } else {
//            val displayMetrics = DisplayMetrics()
//            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
//            (displayMetrics.heightPixels)
//        }
//
//    override fun onStop() {
//        super.onStop()
//        player?.stop()
//        player?.release()
//        canvasOverlayJob?.cancel()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        player?.stop()
//        player?.release()
//        canvasOverlayJob?.cancel()
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
//        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
//        binding.topAppBar.applyInsetter {
//            type(statusBars = true) {
//                margin()
//            }
//        }
//        activity?.window?.navigationBarColor = Color.TRANSPARENT
//        return binding.root
        return ComposeView(requireContext()).also {
            composeView = it
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
//        val activity = requireActivity()
//        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
//        val miniplayer = activity.findViewById<ComposeView>(R.id.miniplayer)
//
//        bottom.visibility = View.GONE
//        miniplayer.visibility = View.GONE

        composeView.apply {
            setContent {
                AppTheme {
                    Scaffold { paddingValues ->
                        NowPlayingScreen(sharedViewModel = viewModel, navController = findNavController())
                    }
                }
            }
        }
    }

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        Log.w("IvArt", (binding.ivArt.visibility == View.GONE).toString())
//        Log.w("MiddleLayout", (binding.middleLayout.visibility == View.GONE).toString())
//        val scs = getScreenHeight(requireActivity())
//        binding.topAppBarLayout.getDimensions { _, hAppBar, _, _ ->
//            Log.w("Screen size", scs.toString())
//            val topAppBarHeight = hAppBar
//            binding.middleLayout.getDimensions { w, h, margin, xy ->
//                Log.w("Check Margin", "w: $w, h: $h, margin: $margin, xy: $xy")
//                val layoutParamsCopy = binding.middleLayout.layoutParams as ViewGroup.MarginLayoutParams
//                var x = margin[2]
//                //                val y = binding.topAppBarLayout.layoutParams.height + 2 * x + h + binding.middleLayout.layoutParams.height
//                binding.infoControllerLayout.getDimensions { w1, h1, margin1, xy1 ->
//                    Log.w("Check Margin", "w1: $w1, h1: $h1, margin1: $margin1, xy1: $xy1")
//                    val y = topAppBarHeight + 2 * x + h + h1
//                    Log.w("Check Margin", "y: $y")
//
//                    binding.belowControllerButtonLayout.getDimensions { _, h3, _, _ ->
//                        if (y + h3 > scs) {
//                            x = (x - ((y + h3 - scs) / 2))
//                            if (x < 0) x = h3
//                            layoutParamsCopy.bottomMargin = x
//                            layoutParamsCopy.topMargin = x
//                            binding.middleLayout.layoutParams = layoutParamsCopy
//                            Log.w("Check Margin", "y: $y, scs: $scs, x: $x")
//                        } else {
//                            x = (x + ((scs - y - h3) / 2))
//                            if (x < 0) x = h3
//                            layoutParamsCopy.bottomMargin = x
//                            layoutParamsCopy.topMargin = x
//                            binding.middleLayout.layoutParams = layoutParamsCopy
//                            Log.w("Check Margin", "y: $y, scs: $scs, x: $x")
//                        }
//                        Log.w("Check Margin", "y: $y, scs: $scs")
//                        binding.smallArtistLayout.getDimensions { _, _, _, _ ->
//                            val lop = binding.smallArtistLayout.layoutParams as ViewGroup.MarginLayoutParams
//                            lop.bottomMargin = h3 + 20
//                            binding.smallArtistLayout.layoutParams = lop
//                        }
//                    }
//                }
//            }
//        }
//    }

//    override fun onViewCreated(
//        view: View,
//        savedInstanceState: Bundle?,
//    ) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.canvasLayout.apply {
//            val layoutParamsCopy = layoutParams
//            layoutParamsCopy.height = getScreenHeight(requireActivity())
//            layoutParams = layoutParamsCopy
//            Log.w("Check Height", layoutParams.height.toString())
//        }
// //            val location = IntArray(2)
// //            binding.belowControllerButtonLayout.getLocationInWindow(location)
// //            val y = binding.belowControllerButtonLayout.top
//
//
//        val activity = requireActivity()
//        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
//        val miniplayer = activity.findViewById<ComposeView>(R.id.miniplayer)
//
//        bottom.visibility = View.GONE
//        miniplayer.visibility = View.GONE
//        binding.lyricsFullLayout.visibility = View.GONE
//        binding.buffered.max = 100
//        Log.d("check Video ID in ViewModel", viewModel.videoId.value.toString())
//
//        type = arguments?.getString("type")
//        videoId = arguments?.getString("videoId")
//        from = arguments?.getString("from") ?: viewModel.from.value
//        index = arguments?.getInt("index")
//        downloaded = arguments?.getInt("downloaded")
//        playlistId = arguments?.getString("playlistId")
//
//        Log.d("check Video ID in Fragment", videoId.toString())
//
//        disableScrolling = DisableTouchEventRecyclerView()
//
//        val onLyricsClickObject =
//            object : LyricsAdapter.OnItemClickListener {
//                override fun onItemClick(line: Line?) {
//                    Log.w("Check line", line.toString())
//                    if (line != null) {
//                        val duration = runBlocking { viewModel.duration.first() }
//                        Log.w("Check duration", duration.toString())
//                        if (duration > 0 && line.startTimeMs.toLong() < duration) {
//                            Log.w(
//                                "Check seek",
//                                (line.startTimeMs.toLong().toDouble() / duration).toFloat().toString(),
//                            )
//                            val seek =
//                                ((line.startTimeMs.toLong() * 100).toDouble() / duration).toFloat()
//                            viewModel.onUIEvent(UIEvent.UpdateProgress(seek))
//                        }
//                    }
//                }
//            }
//
//        lyricsAdapter = LyricsAdapter(null)
//        lyricsAdapter.setOnItemClickListener(
//            onLyricsClickObject,
//        )
//        lyricsFullAdapter = LyricsAdapter(null)
//        lyricsFullAdapter.setOnItemClickListener(
//            onLyricsClickObject,
//        )
//        binding.rvLyrics.apply {
//            adapter = lyricsAdapter
//            layoutManager = CenterLayoutManager(requireContext())
//        }
//        binding.rvFullLyrics.apply {
//            adapter = lyricsFullAdapter
//            layoutManager = LinearLayoutManager(requireContext())
//        }
//        binding.playerView.player = viewModel.simpleMediaServiceHandler?.player
//
// //        when (type) {
// //            SONG_CLICK -> {
// //                viewModel.playlistId.value = null
// //                if (viewModel.videoId.value == videoId) {
// //                    gradientDrawable = viewModel.gradientDrawable.value
// //                    lyricsBackground = viewModel.lyricsBackground.value
// //                    metadataCurSong = viewModel.metadata.value?.data
// //                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
// //                } else {
// //                    Log.i("Now Playing Fragment", "Song Click")
// //                    binding.ivArt.setImageResource(0)
// //                    binding.loadingArt.visibility = View.VISIBLE
// //                    viewModel.gradientDrawable.postValue(null)
// //                    viewModel.lyricsBackground.postValue(null)
// //                    binding.tvSongTitle.visibility = View.GONE
// //                    binding.tvSongArtist.visibility = View.GONE
// //                    Queue.getNowPlaying()?.let {
// ////                        lifecycleScope.launch {
// ////                            repeatOnLifecycle(Lifecycle.State.CREATED) {
// ////                                viewModel.firstTrackAdded.collect { added ->
// ////                                    if (added && type == Config.SONG_CLICK) {
// ////                                        viewModel.changeFirstTrackAddedToFalse()
// //// //                                        viewModel.getFormat(it.videoId)
// ////                                        getRelated(it.videoId)
// ////                                    }
// ////                                }
// ////                            }
// ////                        }
// //                        viewModel.simpleMediaServiceHandler?.reset()
// ////                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
// ////                            requireActivity().stopService(
// ////                                Intent(
// ////                                    requireContext(),
// ////                                    FetchQueue::class.java
// ////                                )
// ////                            )
// ////                        }
// //
// ////                        viewModel.loadMediaItemFromTrack(it, SONG_CLICK)
// //                        viewModel.videoId.postValue(it.videoId)
// //                        viewModel.from.postValue(from)
// //                        updateUIfromQueueNowPlaying()
// //                    }
// //                }
// //            }
// //
// //            SHARE -> {
// //                viewModel.playlistId.value = null
// //                viewModel.stopPlayer()
// //                binding.ivArt.setImageResource(0)
// //                binding.loadingArt.visibility = View.VISIBLE
// //                viewModel.gradientDrawable.postValue(null)
// //                viewModel.lyricsBackground.postValue(null)
// //                binding.tvSongTitle.visibility = View.GONE
// //                binding.tvSongArtist.visibility = View.GONE
// //                if (videoId != null) {
// //                    Log.d("Check Video ID", videoId!!)
// //                    Log.d("Check videoId in ViewModel", viewModel.videoId.value.toString())
// //                    viewModel.getSongFull(videoId!!)
// //                    viewModel.songFull.observe(viewLifecycleOwner) {
// //                        Log.w("Check Song Full", it?.videoDetails?.title.toString())
// //                        if (it != null && it.videoDetails?.videoId == videoId && it.videoDetails?.videoId != null) {
// //                            val track = it.toTrack()
// ////                            Queue.clear()
// //                            Queue.setNowPlaying(track)
// //                            viewModel.simpleMediaServiceHandler?.reset()
// ////                            if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
// ////                                requireActivity().stopService(
// ////                                    Intent(
// ////                                        requireContext(),
// ////                                        FetchQueue::class.java
// ////                                    )
// ////                                )
// ////                            }
// ////                            viewModel.loadMediaItemFromTrack(track, SHARE)
// //                            viewModel.videoId.postValue(track.videoId)
// //                            viewModel.from.postValue(from)
// //                            updateUIfromQueueNowPlaying()
// //                            miniplayer.visibility = View.GONE
// //                            bottom.visibility = View.GONE
// ////                            lifecycleScope.launch {
// ////                                repeatOnLifecycle(Lifecycle.State.CREATED) {
// ////                                    viewModel.firstTrackAdded.collect { added ->
// ////                                        if (added && type == Config.SHARE) {
// ////                                            viewModel.changeFirstTrackAddedToFalse()
// //// //                                            viewModel.getFormat(track.videoId)
// ////                                            getRelated(track.videoId)
// ////                                        }
// ////                                    }
// ////                                }
// ////                            }
// //                        }
// //                    }
// //                }
// //            }
// //
// //            VIDEO_CLICK -> {
// //                viewModel.playlistId.value = null
// //                if (viewModel.videoId.value == videoId) {
// //                    gradientDrawable = viewModel.gradientDrawable.value
// //                    lyricsBackground = viewModel.lyricsBackground.value
// //                    metadataCurSong = viewModel.metadata.value?.data
// //                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
// //                } else {
// ////                if (!viewModel.songTransitions.value){
// //                    Log.i("Now Playing Fragment", "Video Click")
// //                    binding.ivArt.setImageResource(0)
// //                    binding.loadingArt.visibility = View.VISIBLE
// //                    viewModel.gradientDrawable.postValue(null)
// //                    viewModel.lyricsBackground.postValue(null)
// //                    binding.tvSongTitle.visibility = View.GONE
// //                    binding.tvSongArtist.visibility = View.GONE
// //                    Queue.getNowPlaying()?.let {
// //                        viewModel.simpleMediaServiceHandler?.reset()
// ////                        viewModel.resetRelated()
// ////                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
// ////                            requireActivity().stopService(
// ////                                Intent(
// ////                                    requireContext(),
// ////                                    FetchQueue::class.java
// ////                                )
// ////                            )
// ////                        }
// ////                        viewModel.loadMediaItemFromTrack(it, VIDEO_CLICK)
// //                        viewModel.videoId.postValue(it.videoId)
// //                        viewModel.from.postValue(from)
// //                        updateUIfromQueueNowPlaying()
// ////                        lifecycleScope.launch {
// ////                            repeatOnLifecycle(Lifecycle.State.CREATED) {
// ////                                viewModel.firstTrackAdded.collect { added ->
// ////                                    if (added && type == Config.VIDEO_CLICK) {
// ////                                        viewModel.changeFirstTrackAddedToFalse()
// //// //                                        viewModel.getFormat(it.videoId)
// ////                                        getRelated(it.videoId)
// ////                                    }
// ////                                }
// ////                            }
// ////                        }
// //                    }
// //                    // }
// ////                viewModel.loadMediaItems(videoId!!)
// //                }
// //            }
// //
// //            ALBUM_CLICK -> {
// //                if (playlistId != null) {
// //                    viewModel.playlistId.value = playlistId
// //                }
// ////                if (!viewModel.songTransitions.value){
// //                Log.i("Now Playing Fragment", "Album Click")
// //                binding.ivArt.setImageResource(0)
// //                binding.loadingArt.visibility = View.VISIBLE
// //                viewModel.gradientDrawable.postValue(null)
// //                viewModel.lyricsBackground.postValue(null)
// //                binding.tvSongTitle.visibility = View.GONE
// //                binding.tvSongArtist.visibility = View.GONE
// //                Queue.getNowPlaying()?.let {
// //                    viewModel.simpleMediaServiceHandler?.reset()
// ////                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
// ////                            requireActivity().stopService(
// ////                                Intent(
// ////                                    requireContext(),
// ////                                    FetchQueue::class.java
// ////                                )
// ////                            )
// ////                        }
// ////                    viewModel.loadMediaItemFromTrack(it, ALBUM_CLICK, index)
// //                    viewModel.videoId.postValue(it.videoId)
// //                    viewModel.from.postValue(from)
// ////                        viewModel.resetLyrics()
// ////                        viewModel.getLyrics(it.title + " " + it.artists?.first()?.name)
// //                    updateUIfromQueueNowPlaying()
// ////                        viewModel._lyrics.observe(viewLifecycleOwner){ resourceLyrics ->
// ////                            when(resourceLyrics){
// ////                                is Resource.Success -> {
// ////                                    if (resourceLyrics.data != null) {
// ////                                        viewModel.insertLyrics(resourceLyrics.data.toLyricsEntity(it.videoId))
// ////                                        viewModel.parseLyrics(resourceLyrics.data)
// ////                                    }
// ////                                }
// ////                                is Resource.Error -> {
// ////                                    viewModel.getSavedLyrics(it.videoId)
// ////                                }
// ////                            }
// ////                        }
// //                    Log.d("check index", index.toString())
// ////                        lifecycleScope.launch {
// ////                            repeatOnLifecycle(Lifecycle.State.CREATED) {
// ////                                viewModel.firstTrackAdded.collect { added ->
// ////                                    if (added && type == Config.ALBUM_CLICK) {
// ////                                        viewModel.changeFirstTrackAddedToFalse()
// //// //                                        viewModel.getFormat(it.videoId)
// ////                                        if (index == null) {
// ////                                            fetchSourceFromQueue(downloaded = downloaded ?: 0)
// ////                                        } else {
// ////                                            fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
// ////                                        }
// ////                                    }
// ////                                }
// ////                            }
// ////                        }
// //                }
// //            }
// //
// //            PLAYLIST_CLICK -> {
// //                if (playlistId != null) {
// //                    viewModel.playlistId.value = playlistId
// //                }
// //                Log.i("Now Playing Fragment", "Playlist Click")
// //                binding.ivArt.setImageResource(0)
// //                binding.loadingArt.visibility = View.VISIBLE
// //                viewModel.gradientDrawable.postValue(null)
// //                viewModel.lyricsBackground.postValue(null)
// //                binding.tvSongTitle.visibility = View.GONE
// //                binding.tvSongArtist.visibility = View.GONE
// //                Queue.getNowPlaying()?.let {
// //                    viewModel.simpleMediaServiceHandler?.reset()
// ////                    viewModel.resetRelated()
// ////                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
// ////                            requireActivity().stopService(
// ////                                Intent(
// ////                                    requireContext(),
// ////                                    FetchQueue::class.java
// ////                                )
// ////                            )
// ////                        }
// //                    Log.d("check index", index.toString())
// ////                    viewModel.loadMediaItemFromTrack(it, PLAYLIST_CLICK, index)
// //                    viewModel.videoId.postValue(it.videoId)
// //                    viewModel.from.postValue(from)
// ////                        viewModel.resetLyrics()
// ////                        viewModel.getLyrics(it.title + " " + it.artists?.first()?.name)
// //                    updateUIfromQueueNowPlaying()
// ////                        viewModel._lyrics.observe(viewLifecycleOwner){ resourceLyrics ->
// ////                            when(resourceLyrics){
// ////                                is Resource.Success -> {
// ////                                    if (resourceLyrics.data != null) {
// ////                                        viewModel.insertLyrics(resourceLyrics.data.toLyricsEntity(it.videoId))
// ////                                        viewModel.parseLyrics(resourceLyrics.data)
// ////                                    }
// ////                                }
// ////                                is Resource.Error -> {
// ////                                    viewModel.getSavedLyrics(it.videoId)
// ////                                }
// ////                            }
// ////                        }
// ////                        lifecycleScope.launch {
// ////                            repeatOnLifecycle(Lifecycle.State.CREATED) {
// ////                                viewModel.firstTrackAdded.collect { added ->
// ////                                    if (added && type == Config.PLAYLIST_CLICK) {
// ////                                        viewModel.changeFirstTrackAddedToFalse()
// //// //                                        viewModel.getFormat(it.videoId)
// ////                                        if (index == null) {
// ////                                            fetchSourceFromQueue(downloaded = downloaded ?: 0)
// ////                                        } else {
// ////                                            fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
// ////                                        }
// ////                                    }
// ////                                }
// ////                            }
// ////                        }
// //                }
// //            }
// //
// //            MINIPLAYER_CLICK -> {
// //                videoId = viewModel.videoId.value
// //                from = viewModel.from.value
// //                metadataCurSong = viewModel.metadata.value?.data
// //                gradientDrawable = viewModel.gradientDrawable.value
// //                lyricsBackground = viewModel.lyricsBackground.value
// ////                if (viewModel.progress.value in 0.0..1.0) {
// ////                    binding.progressSong.value = viewModel.progress.value * 100
// ////                }
// //                if (videoId == null) {
// //                    videoId = runBlocking { viewModel.nowPlayingMediaItem.first()?.mediaId }
// //                    viewModel.videoId.postValue(videoId)
// //                }
// //                updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
// //            }
// //        }
//
//        lifecycleScope.launch {
// //            val job7 = launch {
// //                viewModel.songTransitions.collectLatest { isChanged ->
// //                    if (isChanged) {
// //                        val song = viewModel.getCurrentMediaItem()
// //                        if (song != null) {
// //                            Log.i("Now Playing Fragment", "Bên dưới")
// //                            Log.d("Song Transition", "Song Transition")
// //                            videoId = viewModel.videoId.value
// //                            binding.ivArt.setImageResource(0)
// //                            binding.loadingArt.visibility = View.VISIBLE
// //                            Log.d("Check Lyrics", viewModel._lyrics.value?.data.toString())
// //                            updateUIfromCurrentMediaItem(song)
// //                            simpleMediaServiceHandler.setCurrentSongIndex(viewModel.getCurrentMediaItemIndex())
// //                            viewModel.changeSongTransitionToFalse()
// //                        }
// //                    }
// //                }
// //            }
//            repeatOnLifecycle(Lifecycle.State.CREATED) {
//                val job7 =
//                    launch {
//                        viewModel.nowPlayingMediaItem.collectLatest { song ->
//                            if (song != null) {
// //                                viewModel.getFormat(song.mediaId)
//                                Log.i("Now Playing Fragment", "song ${song.mediaMetadata.title}")
//                                Log.w("Now Playing Fragment", "song ${song.mediaMetadata.description}")
//                                videoId = viewModel.videoId.value
//                                binding.ivArt.setImageResource(0)
//                                binding.loadingArt.visibility = View.VISIBLE
//                                Log.d(
//                                    "Check Lyrics",
//                                    viewModel._lyrics.value
//                                        ?.data
//                                        .toString(),
//                                )
//                                updateUIfromCurrentMediaItem(song)
//                                viewModel.simpleMediaServiceHandler?.setCurrentSongIndex(
//                                    viewModel.getCurrentMediaItemIndex(),
//                                )
//                                viewModel.changeSongTransitionToFalse()
//                                if (viewModel.listYouTubeLiked
//                                        .first()
//                                        ?.contains(song.mediaId) == true
//                                ) {
//                                    binding.btAddToYouTubeLiked.setImageResource(R.drawable.done)
//                                } else {
//                                    binding.btAddToYouTubeLiked.setImageResource(
//                                        R.drawable.baseline_add_24,
//                                    )
//                                }
//                                if (song.isVideo()) {
//                                    binding.playerLayout.visibility = View.VISIBLE
//                                    binding.playerView.visibility = View.VISIBLE
//                                    binding.ivArt.visibility = View.INVISIBLE
//                                    binding.loadingArt.visibility = View.GONE
//                                    Log.w(
//                                        "Format: ",
//                                        binding.playerView.player
//                                            ?.currentMediaItem
//                                            ?.mediaMetadata
//                                            ?.title
//                                            .toString(),
//                                    )
//                                    if (binding.playerView.player == null) {
//                                        binding.playerView.player = viewModel.simpleMediaServiceHandler?.player
//                                    }
//                                } else {
//                                    binding.playerLayout.visibility = View.GONE
//                                    binding.playerView.visibility = View.GONE
//                                    binding.ivArt.visibility = View.VISIBLE
//                                }
//                            }
//                        }
//                    }
//                val job13 =
//                    launch {
//                        viewModel.progress.collect {
//                            if (it in 0.0..1.0) {
//                                binding.progressSong.value = it * 100
//                            }
//                        }
//                    }
//                val job1 =
//                    launch {
//                        viewModel.progressString.collect {
//                            binding.tvCurrentTime.text = it
// //                        if (viewModel.progress.value * 100 in 0f..100f) {
// //                            binding.progressSong.value = viewModel.progress.value * 100
// // //                        songChangeListener.onUpdateProgressBar(viewModel.progress.value * 100)
// //                        }
//                        }
//                    }
//                val job2 =
//                    launch {
//                        viewModel.isPlaying.collect {
//                            Log.d("Check Song Transistion", "${viewModel.songTransitions.value}")
//                            if (it) {
//                                binding.btPlayPause.setImageResource(
//                                    R.drawable.baseline_pause_circle_24,
//                                )
// //                        songChangeListener.onIsPlayingChange()
//                            } else {
//                                binding.btPlayPause.setImageResource(
//                                    R.drawable.baseline_play_circle_24,
//                                )
// //                        songChangeListener.onIsPlayingChange()
//                            }
//                        }
//                    }
//                // Update progress bar from buffered percentage
//                val job3 =
//                    launch {
//                        viewModel.bufferedPercentage.collect {
//                            binding.buffered.progress = it
// //                    Log.d("buffered", it.toString())
//                        }
//                    }
//                // Check if song is ready to play. And make progress bar indeterminate
//                val job4 =
//                    launch {
//                        viewModel.notReady.observe(viewLifecycleOwner) {
//                            binding.buffered.isIndeterminate = it
//                        }
//                    }
//                val job5 =
//                    launch {
//                        viewModel.progressMillis.collect {
//                            if (viewModel._lyrics.value?.data != null) {
// //                            val temp = viewModel.getLyricsString(it)
//                                val lyrics = viewModel._lyrics.value!!.data
//                                binding.tvSyncState.text =
//                                    when (viewModel.getLyricsSyncState()) {
//                                        Config.SyncState.NOT_FOUND -> null
//                                        Config.SyncState.LINE_SYNCED ->
//                                            getString(
//                                                R.string.line_synced,
//                                            )
//
//                                        Config.SyncState.UNSYNCED -> getString(R.string.unsynced)
//                                    }
// //                                viewModel._lyrics.value?.data?.let {
// //
// //                                }
//                                val index = viewModel.getActiveLyrics(it)
//                                if (index != null) {
//                                    if (lyrics?.lines?.get(0)?.words == "Lyrics not found") {
//                                        if (binding.lyricsLayout.visibility != View.GONE) {
//                                            binding.lyricsLayout.visibility = View.GONE
//                                        }
//                                        if (binding.lyricsTextLayout.visibility != View.GONE) {
//                                            binding.lyricsTextLayout.visibility = View.GONE
//                                        }
//                                    } else {
//                                        viewModel._lyrics.value!!.data?.let { it1 ->
//                                            lyricsAdapter.updateOriginalLyrics(
//                                                it1,
//                                            )
//                                            lyricsFullAdapter.updateOriginalLyrics(it1)
//                                            if (viewModel.getLyricsSyncState() == Config.SyncState.LINE_SYNCED && lyricsAdapter.index != index) {
// //                                                binding.rvLyrics.addOnItemTouchListener(disableScrolling)
//                                                lyricsAdapter.setActiveLyrics(index)
//                                                lyricsFullAdapter.setActiveLyrics(index)
//                                                if (index == -1) {
//                                                    binding.rvLyrics.smoothScrollToPosition(0)
//                                                } else {
//                                                    binding.rvLyrics.smoothScrollToPosition(index)
//                                                }
//                                            } else if (viewModel.getLyricsSyncState() == Config.SyncState.UNSYNCED && lyricsAdapter.index != -1) {
//                                                lyricsAdapter.setActiveLyrics(-1)
//                                                lyricsFullAdapter.setActiveLyrics(-1)
// //                                                binding.rvLyrics.removeOnItemTouchListener(disableScrolling)
//                                            }
// //                                        it1.lines?.find { line -> line.words == temp.nowLyric }
// //                                            ?.let { it2 ->
// //                                                lyricsAdapter.setActiveLyrics(it2)
// //                                                binding.rvLyrics.smoothScrollToPosition(it1.lines.indexOf(it2))
// //                                            }
//                                        }
//
//                                        if (binding.btFull.text == getString(R.string.show)) {
//                                            if (binding.lyricsTextLayout.visibility != View.VISIBLE) {
//                                                binding.lyricsTextLayout.visibility = View.VISIBLE
//                                            }
//                                        }
//                                        if (binding.lyricsLayout.visibility != View.VISIBLE) {
//                                            binding.lyricsLayout.visibility = View.VISIBLE
//                                        }
// //                                    if (temp.nowLyric != null) {
// //                                        binding.tvNowLyrics.visibility = View.VISIBLE
// //                                        binding.tvNowLyrics.text = temp.nowLyric
// //                                    } else {
// //                                        binding.tvNowLyrics.visibility = View.GONE
// //                                    }
// //                                    if (temp.prevLyrics != null) {
// //                                        binding.tvPrevLyrics.visibility = View.VISIBLE
// //                                        if (temp.prevLyrics.size > 1) {
// //                                            val txt = temp.prevLyrics[0] + "\n" + temp.prevLyrics[1]
// //                                            binding.tvPrevLyrics.text = txt
// //                                        } else {
// //                                            binding.tvPrevLyrics.text = temp.prevLyrics[0]
// //                                        }
// //                                    } else {
// //                                        binding.tvPrevLyrics.visibility = View.GONE
// //                                    }
// //                                    if (temp.nextLyric != null) {
// //                                        binding.tvNextLyrics.visibility = View.VISIBLE
// //                                        if (temp.nextLyric.size > 1) {
// //                                            val txt = temp.nextLyric[0] + "\n" + temp.nextLyric[1]
// //                                            binding.tvNextLyrics.text = txt
// //                                        } else {
// //                                            binding.tvNextLyrics.text = temp.nextLyric[0]
// //                                        }
// //                                    } else {
// //                                        binding.tvNextLyrics.visibility = View.GONE
// //                                    }
//                                    }
//                                }
//                            } else {
//                                if (binding.lyricsLayout.visibility != View.GONE) {
//                                    binding.lyricsLayout.visibility = View.GONE
//                                }
//                                if (binding.lyricsTextLayout.visibility != View.GONE) {
//                                    binding.lyricsTextLayout.visibility = View.GONE
//                                }
//                            }
//                        }
//                    }
//                val job8 =
//                    launch {
//                        viewModel.shuffleModeEnabled.collect { shuffle ->
//                            when (shuffle) {
//                                true -> {
//                                    binding.btShuffle.setImageResource(
//                                        R.drawable.baseline_shuffle_24_enable,
//                                    )
//                                }
//
//                                false -> {
//                                    binding.btShuffle.setImageResource(
//                                        R.drawable.baseline_shuffle_24,
//                                    )
//                                }
//                            }
//                        }
//                    }
//                val job9 =
//                    launch {
//                        viewModel.repeatMode.collect { repeatMode ->
//                            when (repeatMode) {
//                                RepeatState.None -> {
//                                    binding.btRepeat.setImageResource(R.drawable.baseline_repeat_24)
//                                }
//
//                                RepeatState.One -> {
//                                    binding.btRepeat.setImageResource(
//                                        R.drawable.baseline_repeat_one_24,
//                                    )
//                                }
//
//                                RepeatState.All -> {
//                                    binding.btRepeat.setImageResource(
//                                        R.drawable.baseline_repeat_24_enable,
//                                    )
//                                }
//                            }
//                        }
//                    }
//                val job10 =
//                    launch {
//                        viewModel.liked.collectLatest { liked ->
//                            Log.w("Check Like", "Collect from main activity $liked")
//                            binding.cbFavorite.isChecked = liked
//                        }
//                    }
//                val job11 =
//                    launch {
//                        viewModel.simpleMediaServiceHandler?.controlState?.collect { state ->
//                            setEnabledAll(binding.btPrevious, state.isPreviousAvailable)
//                            setEnabledAll(binding.btNext, state.isNextAvailable)
//                        }
//                    }
//                val job14 =
//                    launch {
//                        viewModel.songInfo.collectLatest { songInfo ->
//                            if (songInfo != null) {
//                                binding.uploaderLayout.visibility = View.VISIBLE
//                                binding.infoLayout.visibility = View.VISIBLE
//                                binding.tvUploader.text = songInfo.author
//                                binding.tvSmallArtist.text = songInfo.author
//                                binding.ivSmallArtist.load(songInfo.authorThumbnail) {
//                                    diskCacheKey(songInfo.authorThumbnail)
//                                    diskCachePolicy(CachePolicy.ENABLED)
//                                    crossfade(true)
//                                    placeholder(R.drawable.holder)
//                                }
//                                binding.ivAuthor.load(songInfo.authorThumbnail) {
//                                    diskCacheKey(songInfo.authorThumbnail)
//                                    diskCachePolicy(CachePolicy.ENABLED)
//                                    crossfade(true)
//                                    placeholder(R.drawable.holder_video)
//                                }
//                                binding.tvSubCount.text = songInfo.subscribers
//                                binding.tvPublishAt.text =
//                                    getString(R.string.published_at, songInfo.uploadDate)
//                                binding.tvPlayCount.text =
//                                    getString(
//                                        R.string.view_count,
//                                        String.format("%,d", songInfo.viewCount),
//                                    )
//                                binding.tvLikeCount.text =
//                                    getString(
//                                        R.string.like_and_dislike,
//                                        songInfo.like,
//                                        songInfo.dislike,
//                                    )
//                                binding.tvDescription.text =
//                                    songInfo.description ?: getString(R.string.no_description)
//                                InteractiveTextMaker
//                                    .of(
//                                        binding.tvDescription,
//                                    ).setOnTextClickListener {
//                                        Log.d("NowPlayingFragment", "Text Clicked $it")
//                                        val timestamp = parseTimestampToMilliseconds(it)
//                                        if (timestamp != 0.0 && timestamp < runBlocking { viewModel.duration.first() }) {
//                                            viewModel.onUIEvent(
//                                                UIEvent.UpdateProgress(
//                                                    ((timestamp * 100) / runBlocking { viewModel.duration.first() }).toFloat(),
//                                                ),
//                                            )
//                                        }
//                                    }.setSpecialTextColorRes(R.color.light_blue_A400)
//                                    .initialize()
//                            } else {
//                                binding.uploaderLayout.visibility = View.GONE
//                                binding.infoLayout.visibility = View.GONE
//                            }
//                        }
//                    }
// //                val job16 = launch {
// //                    viewModel.firstTrackAdded.collectLatest { added ->
// //                        if (added) {
// //                            when(type) {
// //                                Config.SONG_CLICK -> {
// //                                    viewModel.nowPLaying.first()?.let { getRelated(it.mediaId) }
// //                                    viewModel.changeFirstTrackAddedToFalse()
// //                                }
// //                                Config.SHARE -> {
// //                                    viewModel.nowPLaying.first()?.let { getRelated(it.mediaId) }
// //                                    viewModel.changeFirstTrackAddedToFalse()
// //                                }
// //                                Config.VIDEO_CLICK -> {
// //
// // //                                        viewModel.getFormat(it.videoId)
// //                                    viewModel.nowPLaying.first()?.let { getRelated(it.mediaId) }
// //                                    viewModel.changeFirstTrackAddedToFalse()
// //                                }
// //                                Config.ALBUM_CLICK -> {
// //                                    if (index == null) {
// // //                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
// //                                        viewModel.loadPlaylistOrAlbum(downloaded = downloaded ?: 0)
// //                                    } else {
// // //                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
// //                                        viewModel.loadPlaylistOrAlbum(downloaded = downloaded ?: 0, index = index)
// //                                    }
// //
// //                                    viewModel.changeFirstTrackAddedToFalse()
// //                                }
// //                                Config.PLAYLIST_CLICK -> {
// //                                    if (index == null) {
// // //                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
// //                                        viewModel.loadPlaylistOrAlbum(downloaded = downloaded ?: 0)
// //                                    } else {
// // //                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
// //                                        viewModel.loadPlaylistOrAlbum(downloaded = downloaded ?: 0, index = index)
// //                                    }
// //                                    viewModel.changeFirstTrackAddedToFalse()
// //                                }
// //                            }
// //                        }
// //                    }
// //                }
//                val job16 =
//                    launch {
//                        viewModel.translateLyrics.collect {
//                            lyricsAdapter.updateTranslatedLyrics(it)
//                            lyricsFullAdapter.updateTranslatedLyrics(it)
//                        }
//                    }
//                val job17 =
//                    launch {
//                        viewModel.lyricsProvider.collect {
//                            when (it) {
//                                LyricsProvider.SPOTIFY -> {
//                                    binding.tvLyricsProvider.text =
//                                        getString(R.string.spotify_lyrics_provider)
//                                }
//
//                                LyricsProvider.MUSIXMATCH -> {
//                                    binding.tvLyricsProvider.text =
//                                        getString(
//                                            R.string.lyrics_provider,
//                                        )
//                                }
//
//                                LyricsProvider.YOUTUBE -> {
//                                    binding.tvLyricsProvider.text =
//                                        getString(R.string.lyrics_provider_youtube)
//                                }
//
//                                else -> {
//                                    binding.tvLyricsProvider.text = getString(R.string.offline_mode)
//                                }
//                            }
//                        }
//                    }
//                val job18 =
//                    launch {
//                        viewModel.duration.collect {
//                            if (viewModel.formatDuration(it).contains('-')) {
//                                binding.tvFullTime.text = getString(R.string.na_na)
//                            } else {
//                                binding.tvFullTime.text = viewModel.formatDuration(it)
//                            }
//                        }
//                    }
//                val job20 =
//                    launch {
//                        viewModel.canvas.collect { canvas ->
//                            val canva = canvas?.canvases?.firstOrNull()
//                            if (canva != null) {
//                                if (canva.canvas_url.contains(".mp4")) {
//                                    player?.stop()
//                                    player?.release()
//                                    player = ExoPlayer.Builder(requireContext()).build()
//                                    binding.playerCanvas.visibility = View.VISIBLE
//                                    binding.ivCanvas.visibility = View.GONE
//                                    player?.setMediaItem(
//                                        MediaItem
//                                            .Builder()
//                                            .setUri(
//                                                canva.canvas_url.toUri(),
//                                            ).build(),
//                                    )
//                                    binding.playerCanvas.player = player
//                                    binding.playerCanvas.resizeMode =
//                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
//                                    player?.prepare()
//                                    player?.play()
//                                    player?.repeatMode = Player.REPEAT_MODE_ONE
//                                } else {
//                                    binding.playerCanvas.visibility = View.GONE
//                                    binding.ivCanvas.visibility = View.VISIBLE
//                                    binding.ivCanvas.load(canva.canvas_url) {
//                                        diskCachePolicy(CachePolicy.ENABLED)
//                                        diskCacheKey(canva.canvas_url)
//                                        crossfade(true)
//                                    }
//                                }
//
//                                binding.middleLayout.visibility = View.INVISIBLE
//                                binding.canvasLayout.visibility = View.VISIBLE
//                                binding.playerView.visibility = View.INVISIBLE
//                                binding.rootLayout.background =
//                                    ColorDrawable(
//                                        resources.getColor(
//                                            R.color.md_theme_dark_background,
//                                            null,
//                                        ),
//                                    )
//                                binding.overlayCanvas.visibility = View.VISIBLE
//                                binding.smallArtistLayout.visibility = View.GONE
//                                val shortAnimationDuration =
//                                    resources.getInteger(android.R.integer.config_mediumAnimTime)
//                                canvasOverlayJob?.cancel()
//                                if (binding.root.scrollY == 0 && binding.root.scrollX == 0) {
//                                    canvasOverlayJob =
//                                        lifecycleScope.launch {
//                                            repeatOnLifecycle(Lifecycle.State.CREATED) {
//                                                delay(5000)
//                                                if (binding.root.scrollY == 0) {
//                                                    binding.overlayCanvas.visibility = View.GONE
//                                                    binding.infoControllerLayout.visibility = View.INVISIBLE
//                                                    binding.smallArtistLayout.visibility = View.VISIBLE
//                                                }
//                                            }
//                                        }
//                                }
//                                binding.root.setOnScrollChangeListener {
//                                        v,
//                                        scrollX,
//                                        scrollY,
//                                        oldScrollX,
//                                        oldScrollY,
//                                    ->
//                                    if (scrollY > 0 && binding.overlayCanvas.visibility == View.GONE) {
//                                        canvasOverlayJob?.cancel()
//                                        binding.overlayCanvas.alpha = 0f
//                                        binding.overlayCanvas.apply {
//                                            visibility = View.VISIBLE
//                                            animate()
//                                                .alpha(1f)
//                                                .setDuration(shortAnimationDuration.toLong())
//                                                .setListener(null)
//                                        }
//                                        binding.infoControllerLayout.alpha = 0f
//                                        binding.infoControllerLayout.apply {
//                                            visibility = View.VISIBLE
//                                            animate()
//                                                .alpha(1f)
//                                                .setDuration(shortAnimationDuration.toLong())
//                                                .setListener(null)
//                                        }
//                                        binding.smallArtistLayout.visibility = View.GONE
//                                    } else if (scrollY == 0 && scrollX == 0 && binding.overlayCanvas.visibility == View.VISIBLE) {
//                                        canvasOverlayJob =
//                                            lifecycleScope.launch {
//                                                repeatOnLifecycle(Lifecycle.State.CREATED) {
//                                                    delay(5000)
//                                                    if (binding.root.scrollY == 0) {
//                                                        binding.overlayCanvas.visibility = View.GONE
//                                                        binding.infoControllerLayout.visibility =
//                                                            View.INVISIBLE
//                                                        binding.smallArtistLayout.visibility = View.VISIBLE
//                                                    }
//                                                }
//                                            }
//                                    }
//                                }
//                                binding.helpMeBro.setOnClickListener {
//                                    if (binding.root.scrollY == 0 && binding.root.scrollX == 0) {
//                                        if (binding.overlayCanvas.visibility == View.VISIBLE) {
//                                            canvasOverlayJob?.cancel()
//                                            binding.overlayCanvas.visibility = View.GONE
//                                            binding.infoControllerLayout.visibility = View.INVISIBLE
//                                            binding.smallArtistLayout.visibility = View.VISIBLE
//                                        } else {
//                                            canvasOverlayJob?.cancel()
//                                            binding.overlayCanvas.alpha = 0f
//                                            binding.overlayCanvas.apply {
//                                                visibility = View.VISIBLE
//                                                animate()
//                                                    .alpha(1f)
//                                                    .setDuration(shortAnimationDuration.toLong())
//                                                    .setListener(null)
//                                            }
//                                            binding.infoControllerLayout.alpha = 0f
//                                            binding.infoControllerLayout.apply {
//                                                visibility = View.VISIBLE
//                                                animate()
//                                                    .alpha(1f)
//                                                    .setDuration(shortAnimationDuration.toLong())
//                                                    .setListener(null)
//                                            }
//                                            binding.smallArtistLayout.visibility = View.GONE
//                                            canvasOverlayJob =
//                                                lifecycleScope.launch {
//                                                    repeatOnLifecycle(Lifecycle.State.CREATED) {
//                                                        delay(5000)
//                                                        if (binding.root.scrollY == 0) {
//                                                            binding.overlayCanvas.visibility = View.GONE
//                                                            binding.infoControllerLayout.visibility =
//                                                                View.INVISIBLE
//                                                            binding.smallArtistLayout.visibility =
//                                                                View.VISIBLE
//                                                        }
//                                                    }
//                                                }
//                                        }
//                                    }
//                                }
//                            } else {
//                                canvasOverlayJob?.cancel()
//                                player?.stop()
//                                player?.release()
//                                binding.helpMeBro.setOnClickListener {
//                                }
//                                binding.root.setOnScrollChangeListener {
//                                        v,
//                                        scrollX,
//                                        scrollY,
//                                        oldScrollX,
//                                        oldScrollY,
//                                    ->
//                                }
//                                binding.overlayCanvas.visibility = View.GONE
//                                binding.infoControllerLayout.visibility = View.VISIBLE
//                                binding.canvasLayout.visibility = View.INVISIBLE
//                                binding.middleLayout.visibility = View.VISIBLE
//                                viewModel.gradientDrawable.value?.let {
//                                    binding.rootLayout.background = it
//                                }
//                            }
//                        }
//                    }
//                val job21 =
//                    launch {
//                        viewModel.logInToYouTube().distinctUntilChanged().collect {
//                            if (it == DataStoreManager.TRUE) {
//                                setEnabledAll(binding.btAddToYouTubeLiked, true)
//                                if (viewModel.isFirstLiked) {
//                                    val balloon =
//                                        Balloon
//                                            .Builder(requireContext())
//                                            .setWidthRatio(0.5f)
//                                            .setHeight(BalloonSizeSpec.WRAP)
//                                            .setText(getString(R.string.guide_liked_title))
//                                            .setTextColorResource(R.color.md_theme_dark_onSurface)
//                                            .setTextSize(11f)
//                                            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
//                                            .setArrowSize(10)
//                                            .setArrowPosition(0.5f)
//                                            .setAutoDismissDuration(5000L)
//                                            .setPadding(12)
//                                            .setCornerRadius(8f)
//                                            .setBackgroundColorResource(
//                                                R.color.md_theme_dark_onSecondary,
//                                            ).setBalloonAnimation(BalloonAnimation.ELASTIC)
//                                            .setLifecycleOwner(viewLifecycleOwner)
//                                            .build()
//                                    balloon.showAlignTop(binding.btAddToYouTubeLiked)
//                                    viewModel.putString("liked_guide", STATUS_DONE)
//                                    viewModel.isFirstSuggestions = false
//                                }
//                            } else {
//                                setEnabledAll(binding.btAddToYouTubeLiked, false)
//                            }
//                        }
//                    }
//                val job22 =
//                    launch {
//                        viewModel.listYouTubeLiked.collect {
//                            if (it?.contains(
//                                    viewModel.simpleMediaServiceHandler
//                                        ?.nowPlaying
//                                        ?.first()
//                                        ?.mediaId,
//                                ) == true
//                            ) {
//                                binding.btAddToYouTubeLiked.setImageResource(R.drawable.done)
//                            } else {
//                                binding.btAddToYouTubeLiked.setImageResource(
//                                    R.drawable.baseline_add_24,
//                                )
//                            }
//                        }
//                    }
//                job1.join()
//                job2.join()
//                job3.join()
//                job4.join()
//                job5.join()
//                job7.join()
//                job8.join()
//                job9.join()
//                job10.join()
//                job13.join()
//                job11.join()
//                job14.join()
//                job16.join()
//                job17.join()
//                job18.join()
//                job20.join()
//                job21.join()
//                job22.join()
//            }
//        }
//        binding.btAddToYouTubeLiked.setOnClickListener {
//            viewModel.addToYouTubeLiked()
//        }
//        binding.tvMore.setOnClickListener {
//            if (binding.tvDescription.maxLines == 2) {
//                val animation =
//                    ObjectAnimator.ofInt(
//                        binding.tvDescription,
//                        "maxLines",
//                        1000,
//                    )
//                animation.setDuration(1000)
//                animation.start()
//                binding.tvMore.setText(R.string.less)
//            } else {
//                val animation =
//                    ObjectAnimator.ofInt(
//                        binding.tvDescription,
//                        "maxLines",
//                        2,
//                    )
//                animation.setDuration(200)
//                animation.start()
//                binding.tvMore.setText(R.string.more)
//            }
//        }
//        binding.btFull.setOnClickListener {
//            if (binding.btFull.text == getString(R.string.show)) {
//                binding.btFull.text = getString(R.string.hide)
//                binding.lyricsTextLayout.visibility = View.GONE
//                binding.lyricsFullLayout.visibility = View.VISIBLE
//            } else {
//                binding.btFull.text = getString(R.string.show)
//                binding.lyricsTextLayout.visibility = View.VISIBLE
//                binding.lyricsFullLayout.visibility = View.GONE
//            }
//        }
//
//        binding.progressSong.addOnSliderTouchListener(
//            object : Slider.OnSliderTouchListener {
//                override fun onStartTrackingTouch(slider: Slider) {
//                }
//
//                override fun onStopTrackingTouch(slider: Slider) {
//                    viewModel.onUIEvent(UIEvent.UpdateProgress(slider.value))
//                }
//            },
//        )
//        binding.btFullscreen.setOnClickListener {
//            binding.playerView.player = null
//            isFullScreen = true
//            findNavController().navigateSafe(R.id.action_global_fullscreenFragment)
//
// //                requireActivity().requestedOrientation =
// //                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
// //                val parent = fullscreen.root.parent as View
// //                parent.fitsSystemWindows = true
// //                val params =
// //                    parent.layoutParams as CoordinatorLayout.LayoutParams
// //                val behavior = params.behavior
// //                if (behavior != null && behavior is BottomSheetBehavior<*>) {
// //                    behavior.peekHeight = binding.root.height
// //                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
// //                    behavior.addBottomSheetCallback(object : BottomSheetCallback() {
// //                        override fun onStateChanged( bottomSheet: View, newState: Int) {
// //                            if (newState == BottomSheetBehavior.STATE_DRAGGING) {
// //                                behavior.state = BottomSheetBehavior.STATE_EXPANDED
// //                            }
// //                        }
// //
// //                        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
// //                    })
// //                }
//
// //            }
//        }
//        binding.playerLayout.setOnClickListener {
//            val shortAnimationDuration =
//                resources.getInteger(android.R.integer.config_mediumAnimTime)
//            if (binding.overlay.visibility == View.VISIBLE) {
//                binding.overlay.visibility = View.GONE
//                overlayJob?.cancel()
//            } else {
//                binding.overlay.alpha = 0f
//                binding.overlay.apply {
//                    visibility = View.VISIBLE
//                    animate()
//                        .alpha(1f)
//                        .setDuration(shortAnimationDuration.toLong())
//                        .setListener(null)
//                }
//                overlayJob?.cancel()
//                overlayJob =
//                    lifecycleScope.launch {
//                        repeatOnLifecycle(Lifecycle.State.CREATED) {
//                            delay(3000)
//                            binding.overlay.visibility = View.GONE
//                        }
//                    }
//            }
//        }
//
//        binding.btPlayPause.setOnClickListener {
//            viewModel.onUIEvent(UIEvent.PlayPause)
//        }
//        binding.btNext.setOnClickListener {
//            viewModel.onUIEvent(UIEvent.Next)
//        }
//        binding.btPrevious.setOnClickListener {
//            viewModel.onUIEvent(UIEvent.Previous)
//        }
//        binding.btShuffle.setOnClickListener {
//            viewModel.onUIEvent(UIEvent.Shuffle)
//        }
//        binding.btRepeat.setOnClickListener {
//            viewModel.onUIEvent(UIEvent.Repeat)
//        }
//
//        binding.topAppBar.setNavigationOnClickListener {
//            findNavController().popBackStack()
//        }
//        binding.btQueue.setOnClickListener {
//            findNavController().navigateSafe(R.id.action_global_queueFragment)
//        }
//        binding.btSongInfo.setOnClickListener {
//            findNavController().navigateSafe(R.id.action_global_infoFragment)
//        }
//        binding.cbFavorite.setOnClickListener {
//            runBlocking { viewModel.nowPlayingMediaItem.first() }?.let { nowPlayingSong ->
//                viewModel.updateLikeStatus(
//                    nowPlayingSong.mediaId,
//                    !runBlocking { viewModel.liked.first() },
//                )
//            }
//        }
//        binding.uploaderLayout.setOnClickListener {
//            val browseId =
//                if (!viewModel.songDB.value
//                        ?.artistId
//                        .isNullOrEmpty()
//                ) {
//                    viewModel.songDB.value
//                        ?.artistId
//                        ?.firstOrNull()
//                } else {
//                    runBlocking { viewModel.songInfo.first()?.authorId }
//                }
//            findNavController().navigateSafe(
//                R.id.action_global_artistFragment,
//                Bundle().apply {
//                    putString("channelId", browseId)
//                },
//            )
//        }
//        binding.smallArtistLayout.setOnClickListener {
//            val browseId =
//                if (!viewModel.songDB.value
//                        ?.artistId
//                        .isNullOrEmpty()
//                ) {
//                    viewModel.songDB.value
//                        ?.artistId
//                        ?.firstOrNull()
//                } else {
//                    runBlocking { viewModel.songInfo.first()?.authorId }
//                }
//            findNavController().navigateSafe(
//                R.id.action_global_artistFragment,
//                Bundle().apply {
//                    putString("channelId", browseId)
//                },
//            )
//        }
//        binding.tvSongArtist.setOnClickListener {
//            val queue = runBlocking { viewModel.simpleMediaServiceHandler?.queueData?.firstOrNull()?.listTracks }
//            if (!queue.isNullOrEmpty()) {
//                val song =
//                    queue[viewModel.getCurrentMediaItemIndex()]
//                if (song.artists?.firstOrNull()?.id != null) {
//                    findNavController().navigateSafe(
//                        R.id.action_global_artistFragment,
//                        Bundle().apply {
//                            putString("channelId", song.artists.firstOrNull()?.id)
//                        },
//                    )
//                }
//            }
//        }
//        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.now_playing_dialog_menu_item_more -> {
//                    val queue = runBlocking { viewModel.simpleMediaServiceHandler?.queueData?.firstOrNull()?.listTracks }
//                    if (!queue.isNullOrEmpty()) {
//                        viewModel.refreshSongDB()
//                        val dialog = BottomSheetDialog(requireContext())
//                        dialog.apply {
//                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                        }
//                        val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
//                        with(bottomSheetView) {
//                            lifecycleScope.launch {
//                                viewModel.simpleMediaServiceHandler?.sleepMinutes?.collect { min ->
//                                    if (min > 0) {
//                                        tvSleepTimer.text =
//                                            getString(R.string.sleep_timer, min.toString())
//                                        ivSleepTimer.setImageResource(R.drawable.alarm_enable)
//                                    } else {
//                                        tvSleepTimer.text = getString(R.string.sleep_timer_off)
//                                        ivSleepTimer.setImageResource(
//                                            R.drawable.baseline_access_alarm_24,
//                                        )
//                                    }
//                                }
//                            }
//                            btAddQueue.visibility = View.GONE
//                            if (runBlocking { viewModel.liked.first() }) {
//                                tvFavorite.text = getString(R.string.liked)
//                                cbFavorite.isChecked = true
//                            } else {
//                                tvFavorite.text = getString(R.string.like)
//                                cbFavorite.isChecked = false
//                            }
//                            when (viewModel.songDB.value?.downloadState) {
//                                DownloadState.STATE_PREPARING -> {
//                                    tvDownload.text = getString(R.string.preparing)
//                                    ivDownload.setImageResource(
//                                        R.drawable.outline_download_for_offline_24,
//                                    )
//                                    setEnabledAll(btDownload, true)
//                                }
//
//                                DownloadState.STATE_NOT_DOWNLOADED -> {
//                                    tvDownload.text = getString(R.string.download)
//                                    ivDownload.setImageResource(
//                                        R.drawable.outline_download_for_offline_24,
//                                    )
//                                    setEnabledAll(btDownload, true)
//                                }
//
//                                DownloadState.STATE_DOWNLOADING -> {
//                                    tvDownload.text = getString(R.string.downloading)
//                                    ivDownload.setImageResource(
//                                        R.drawable.baseline_downloading_white,
//                                    )
//                                    setEnabledAll(btDownload, true)
//                                }
//
//                                DownloadState.STATE_DOWNLOADED -> {
//                                    tvDownload.text = getString(R.string.downloaded)
//                                    ivDownload.setImageResource(R.drawable.baseline_downloaded)
//                                    setEnabledAll(btDownload, true)
//                                }
//                            }
//                            if (queue.isNotEmpty()) {
//                                val song =
//                                    queue[viewModel.getCurrentMediaItemIndex()]
//                                tvSongTitle.text = song.title
//                                tvSongTitle.isSelected = true
//                                tvSongArtist.text = song.artists.toListName().connectArtists()
//                                tvSongArtist.isSelected = true
//                                ivThumbnail.load(song.thumbnails?.last()?.url) {
//                                    diskCachePolicy(CachePolicy.ENABLED)
//                                    diskCacheKey(song.thumbnails?.last()?.url)
//                                    crossfade(true)
//                                    placeholder(R.drawable.holder)
//                                }
//                                if (song.album != null) {
//                                    setEnabledAll(btAlbum, true)
//                                    tvAlbum.text = song.album.name
//                                } else {
//                                    tvAlbum.text = getString(R.string.no_album)
//                                    setEnabledAll(btAlbum, false)
//                                }
//                                btAlbum.setOnClickListener {
//                                    val albumId = song.album?.id
//                                    if (albumId != null) {
//                                        findNavController().navigateSafe(
//                                            R.id.action_global_albumFragment,
//                                            Bundle().apply {
//                                                putString("browseId", albumId)
//                                            },
//                                        )
//                                        dialog.dismiss()
//                                    } else {
//                                        Toast
//                                            .makeText(
//                                                requireContext(),
//                                                getString(R.string.no_album),
//                                                Toast.LENGTH_SHORT,
//                                            ).show()
//                                    }
//                                }
//
//                                btLike.setOnClickListener {
//                                    if (cbFavorite.isChecked) {
//                                        cbFavorite.isChecked = false
//                                        tvFavorite.text = getString(R.string.like)
//                                        viewModel.updateLikeStatus(song.videoId, false)
//                                    } else {
//                                        cbFavorite.isChecked = true
//                                        tvFavorite.text = getString(R.string.liked)
//                                        viewModel.updateLikeStatus(song.videoId, true)
//                                    }
//                                }
//                                btPlayNext.visibility = View.GONE
//                                btRadio.setOnClickListener {
//                                    val args = Bundle()
//                                    args.putString("radioId", "RDAMVM${song.videoId}")
//                                    args.putString(
//                                        "videoId",
//                                        song.videoId,
//                                    )
//                                    dialog.dismiss()
//                                    findNavController().navigateSafe(
//                                        R.id.action_global_playlistFragment,
//                                        args,
//                                    )
//                                }
//                                btSleepTimer.setOnClickListener {
//                                    Log.w("Sleep Timer", "onClick")
//                                    if (viewModel.sleepTimerRunning.value == true) {
//                                        MaterialAlertDialogBuilder(requireContext())
//                                            .setTitle(getString(R.string.warning))
//                                            .setMessage(getString(R.string.sleep_timer_warning))
//                                            .setPositiveButton(getString(R.string.yes)) { d, _ ->
//                                                viewModel.stopSleepTimer()
//                                                Toast
//                                                    .makeText(
//                                                        requireContext(),
//                                                        getString(R.string.sleep_timer_off_done),
//                                                        Toast.LENGTH_SHORT,
//                                                    ).show()
//                                                d.dismiss()
//                                            }.setNegativeButton(getString(R.string.cancel)) { d, _ ->
//                                                d.dismiss()
//                                            }.show()
//                                    } else {
//                                        val d = BottomSheetDialog(requireContext())
//                                        d.apply {
//                                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                                        }
//                                        val v = BottomSheetSleepTimerBinding.inflate(layoutInflater)
//                                        v.btSet.setOnClickListener {
//                                            val min =
//                                                v.etTime.editText
//                                                    ?.text
//                                                    .toString()
//                                            if (min.isNotBlank() && min.toInt() > 0) {
//                                                viewModel.setSleepTimer(min.toInt())
//                                                d.dismiss()
//                                            } else {
//                                                Toast
//                                                    .makeText(
//                                                        requireContext(),
//                                                        getString(R.string.sleep_timer_set_error),
//                                                        Toast.LENGTH_SHORT,
//                                                    ).show()
//                                            }
//                                        }
//                                        d.setContentView(v.root)
//                                        d.setCancelable(true)
//                                        d.show()
//                                    }
//                                }
//                                btAddPlaylist.setOnClickListener {
//                                    viewModel.getAllLocalPlaylist()
//                                    val listLocalPlaylist: ArrayList<LocalPlaylistEntity> =
//                                        arrayListOf()
//                                    val addPlaylistDialog = BottomSheetDialog(requireContext())
//                                    addPlaylistDialog.apply {
//                                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                                    }
//                                    val viewAddPlaylist =
//                                        BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
//                                    val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
//                                    addToAPlaylistAdapter.setVideoId(song.videoId)
//                                    viewAddPlaylist.rvLocalPlaylists.apply {
//                                        adapter = addToAPlaylistAdapter
//                                        layoutManager = LinearLayoutManager(requireContext())
//                                    }
//                                    lifecycleScope.launch {
//                                        repeatOnLifecycle(Lifecycle.State.CREATED) {
//                                            launch {
//                                                viewModel.localPlaylist.collect { list ->
//                                                    Log.d("Check Local Playlist", list.toString())
//                                                    listLocalPlaylist.clear()
//                                                    listLocalPlaylist.addAll(list)
//                                                    addToAPlaylistAdapter.updateList(listLocalPlaylist)
//                                                }
//                                            }.join()
//                                        }
//                                    }
//                                    addToAPlaylistAdapter.setOnItemClickListener(
//                                        object :
//                                            AddToAPlaylistAdapter.OnItemClickListener {
//                                            override fun onItemClick(position: Int) {
//                                                val playlist = listLocalPlaylist[position]
//                                                viewModel.updateInLibrary(song.videoId)
//                                                val tempTrack = ArrayList<String>()
//                                                if (playlist.tracks != null) {
//                                                    tempTrack.addAll(playlist.tracks)
//                                                }
//                                                if (!tempTrack.contains(
//                                                        song.videoId,
//                                                    ) &&
//                                                    playlist.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced &&
//                                                    playlist.youtubePlaylistId != null
//                                                ) {
//                                                    viewModel.addToYouTubePlaylist(
//                                                        playlist.id,
//                                                        playlist.youtubePlaylistId,
//                                                        song.videoId,
//                                                    )
//                                                }
//                                                if (!tempTrack.contains(song.videoId)) {
//                                                    viewModel.insertPairSongLocalPlaylist(
//                                                        PairSongLocalPlaylist(
//                                                            playlistId = playlist.id,
//                                                            songId = song.videoId,
//                                                            position = playlist.tracks?.size ?: 0,
//                                                            inPlaylist = LocalDateTime.now(),
//                                                        ),
//                                                    )
//                                                    tempTrack.add(song.videoId)
//                                                }
//                                                viewModel.updateLocalPlaylistTracks(
//                                                    tempTrack.removeConflicts(),
//                                                    playlist.id,
//                                                )
//                                                addPlaylistDialog.dismiss()
//                                                dialog.dismiss()
//                                            }
//                                        },
//                                    )
//                                    addPlaylistDialog.setContentView(viewAddPlaylist.root)
//                                    addPlaylistDialog.setCancelable(true)
//                                    addPlaylistDialog.show()
//                                }
//
//                                btSeeArtists.setOnClickListener {
//                                    val subDialog = BottomSheetDialog(requireContext())
//                                    subDialog.apply {
//                                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                                    }
//                                    val subBottomSheetView =
//                                        BottomSheetSeeArtistOfNowPlayingBinding.inflate(
//                                            layoutInflater,
//                                        )
//                                    if (song.artists != null) {
//                                        val artistAdapter =
//                                            SeeArtistOfNowPlayingAdapter(song.artists)
//                                        subBottomSheetView.rvArtists.apply {
//                                            adapter = artistAdapter
//                                            layoutManager = LinearLayoutManager(requireContext())
//                                        }
//                                        artistAdapter.setOnClickListener(
//                                            object :
//                                                SeeArtistOfNowPlayingAdapter.OnItemClickListener {
//                                                override fun onItemClick(position: Int) {
//                                                    val artist = song.artists[position]
//                                                    if (artist.id != null) {
//                                                        findNavController().navigateSafe(
//                                                            R.id.action_global_artistFragment,
//                                                            Bundle().apply {
//                                                                putString("channelId", artist.id)
//                                                            },
//                                                        )
//                                                        subDialog.dismiss()
//                                                        dialog.dismiss()
//                                                    }
//                                                }
//                                            },
//                                        )
//                                    }
//
//                                    subDialog.setCancelable(true)
//                                    subDialog.setContentView(subBottomSheetView.root)
//                                    subDialog.show()
//                                }
//                                btChangeLyricsProvider.setOnClickListener {
//                                    val mainLyricsProvider = viewModel.getLyricsProvier()
//                                    var checkedIndex =
//                                        if (mainLyricsProvider == DataStoreManager.MUSIXMATCH) 0 else 1
//                                    val dialogChange =
//                                        MaterialAlertDialogBuilder(requireContext())
//                                            .setTitle(getString(R.string.main_lyrics_provider))
//                                            .setSingleChoiceItems(
//                                                LYRICS_PROVIDER.items,
//                                                checkedIndex,
//                                            ) { _, which ->
//                                                checkedIndex = which
//                                            }.setNegativeButton(
//                                                getString(R.string.cancel),
//                                            ) { dialog, _ ->
//                                                dialog.dismiss()
//                                            }.setPositiveButton(
//                                                getString(R.string.change),
//                                            ) { dialog, _ ->
//                                                if (checkedIndex != -1) {
//                                                    if (checkedIndex == 0) {
//                                                        if (mainLyricsProvider != DataStoreManager.MUSIXMATCH) {
//                                                            viewModel.setLyricsProvider(
//                                                                DataStoreManager.MUSIXMATCH,
//                                                            )
//                                                        }
//                                                    } else if (checkedIndex == 1) {
//                                                        if (mainLyricsProvider != DataStoreManager.YOUTUBE) {
//                                                            viewModel.setLyricsProvider(
//                                                                DataStoreManager.YOUTUBE,
//                                                            )
//                                                        }
//                                                    }
//                                                }
//                                                dialog.dismiss()
//                                            }
//                                    dialogChange.show()
//                                }
//                                btShare.setOnClickListener {
//                                    val shareIntent = Intent(Intent.ACTION_SEND)
//                                    shareIntent.type = "text/plain"
//                                    val url = "https://youtube.com/watch?v=${song.videoId}"
//                                    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
//                                    val chooserIntent =
//                                        Intent.createChooser(
//                                            shareIntent,
//                                            getString(R.string.share_url),
//                                        )
//                                    startActivity(chooserIntent)
//                                }
//                                btDownload.setOnClickListener {
//                                    if (tvDownload.text == getString(R.string.download)) {
//                                        Log.d("Download", "onClick: ${song.videoId}")
//                                        viewModel.updateDownloadState(
//                                            song.videoId,
//                                            DownloadState.STATE_PREPARING,
//                                        )
//                                        val downloadRequest =
//                                            DownloadRequest
//                                                .Builder(
//                                                    song.videoId,
//                                                    song.videoId.toUri(),
//                                                ).setData(song.title.toByteArray())
//                                                .setCustomCacheKey(song.videoId)
//                                                .build()
//                                        viewModel.updateDownloadState(
//                                            song.videoId,
//                                            DownloadState.STATE_DOWNLOADING,
//                                        )
//                                        viewModel.getDownloadStateFromService(song.videoId)
//                                        DownloadService.sendAddDownload(
//                                            requireContext(),
//                                            MusicDownloadService::class.java,
//                                            downloadRequest,
//                                            false,
//                                        )
//                                        lifecycleScope.launch {
//                                            viewModel.downloadState.collect { download ->
//                                                if (download != null) {
//                                                    when (download.state) {
//                                                        Download.STATE_DOWNLOADING -> {
//                                                            viewModel.updateDownloadState(
//                                                                song.videoId,
//                                                                DownloadState.STATE_DOWNLOADING,
//                                                            )
//                                                            tvDownload.text =
//                                                                getString(R.string.downloading)
//                                                            ivDownload.setImageResource(
//                                                                R.drawable.baseline_downloading_white,
//                                                            )
//                                                            setEnabledAll(btDownload, true)
//                                                        }
//
//                                                        Download.STATE_FAILED -> {
//                                                            viewModel.updateDownloadState(
//                                                                song.videoId,
//                                                                DownloadState.STATE_NOT_DOWNLOADED,
//                                                            )
//                                                            tvDownload.text =
//                                                                getString(R.string.download)
//                                                            ivDownload.setImageResource(
//                                                                R.drawable.outline_download_for_offline_24,
//                                                            )
//                                                            setEnabledAll(btDownload, true)
//                                                            Toast
//                                                                .makeText(
//                                                                    requireContext(),
//                                                                    getString(
//                                                                        androidx.media3.exoplayer.R.string.exo_download_failed,
//                                                                    ),
//                                                                    Toast.LENGTH_SHORT,
//                                                                ).show()
//                                                        }
//
//                                                        Download.STATE_COMPLETED -> {
//                                                            viewModel.updateDownloadState(
//                                                                song.videoId,
//                                                                DownloadState.STATE_DOWNLOADED,
//                                                            )
//                                                            Toast
//                                                                .makeText(
//                                                                    requireContext(),
//                                                                    getString(
//                                                                        androidx.media3.exoplayer.R.string.exo_download_completed,
//                                                                    ),
//                                                                    Toast.LENGTH_SHORT,
//                                                                ).show()
//                                                            tvDownload.text =
//                                                                getString(R.string.downloaded)
//                                                            ivDownload.setImageResource(
//                                                                R.drawable.baseline_downloaded,
//                                                            )
//                                                            setEnabledAll(btDownload, true)
//                                                        }
//
//                                                        else -> {
//                                                            Log.d(
//                                                                "Download",
//                                                                "onCreate: ${download.state}",
//                                                            )
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    } else if (tvDownload.text ==
//                                        getString(
//                                            R.string.downloaded,
//                                        ) ||
//                                        tvDownload.text ==
//                                        getString(
//                                            R.string.downloading,
//                                        )
//                                    ) {
//                                        DownloadService.sendRemoveDownload(
//                                            requireContext(),
//                                            MusicDownloadService::class.java,
//                                            song.videoId,
//                                            false,
//                                        )
//                                        viewModel.updateDownloadState(
//                                            song.videoId,
//                                            DownloadState.STATE_NOT_DOWNLOADED,
//                                        )
//                                        tvDownload.text = getString(R.string.download)
//                                        ivDownload.setImageResource(
//                                            R.drawable.outline_download_for_offline_24,
//                                        )
//                                        setEnabledAll(btDownload, true)
//                                        Toast
//                                            .makeText(
//                                                requireContext(),
//                                                getString(R.string.removed_download),
//                                                Toast.LENGTH_SHORT,
//                                            ).show()
//                                    }
//                                }
//                            }
//                        }
//                        dialog.setCancelable(true)
//                        dialog.setContentView(bottomSheetView.root)
//                        dialog.show()
//                    }
//                    true
//                }
//
//                else -> false
//            }
//        }
//    }

//    private fun updateUIfromQueueNowPlaying() {
//        Log.w("CHECK NOW PLAYING IN QUEUE", "updateUIfromQueueNowPlaying: ${Queue.getNowPlaying()}")
//        Log.d("CHECK QUEUE", "updateUIfromQueueNowPlaying: ${Queue.getQueue()}")
//        val nowPlaying = Queue.getNowPlaying()
//        if (nowPlaying != null) {
// //            viewModel.getFormat(nowPlaying.videoId)
//            binding.ivArt.setImageResource(0)
//            binding.loadingArt.visibility = View.VISIBLE
//            Log.d("Update UI", "current: ${nowPlaying.title}")
//            var thumbUrl = nowPlaying.thumbnails?.last()?.url!!
//            if (thumbUrl.contains("w120")) {
//                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
//            }
//            binding.ivArt.load(Uri.parse(thumbUrl)) {
//                diskCacheKey(nowPlaying.videoId)
//                diskCachePolicy(CachePolicy.ENABLED)
//                listener(
//                    onStart = {
//                        binding.ivArt.setImageResource(0)
//                        binding.loadingArt.visibility = View.VISIBLE
//                        Log.d("Update UI", "onStart: ")
//                    },
//                    onSuccess = { _, result ->
//                        binding.ivArt.visibility = View.VISIBLE
//                        binding.loadingArt.visibility = View.GONE
//                        Log.d("Update UI", "onSuccess: ")
//                        val p = Palette.from(result.drawable.toBitmap()).generate()
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
// //                    val centerColor = 0x6C6C6C
//                        val endColor = 0x1b1a1f
//                        val gd =
//                            GradientDrawable(
//                                GradientDrawable.Orientation.TL_BR,
//                                intArrayOf(startColor, endColor),
//                            )
//                        gd.cornerRadius = 0f
//                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
//                        gd.gradientRadius = 0.5f
//                        gd.alpha = 150
//                        val bg = ColorUtils.setAlphaComponent(startColor, 230)
//                        viewModel.gradientDrawable.postValue(gd)
//                        viewModel.lyricsBackground.postValue(bg)
//
//                        var start = binding.rootLayout.background
//                        if (start == null) {
//                            start = ColorDrawable(Color.BLACK)
//                        }
//                        val transition = TransitionDrawable(arrayOf(start, gd))
//                        transition.setDither(true)
//                        binding.rootLayout.background = transition
//                        transition.isCrossFadeEnabled = true
//                        transition.startTransition(500)
//
// //                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
// //                                binding.lyricsLayout.setCardBackgroundColor(color)
// //                                Log.d("Update UI", "Lyrics: $color")
// //                                updateStatusBarColor(color)
// //                            })
//                        binding.lyricsLayout.setCardBackgroundColor(
//                            bg,
//                        )
//                        binding.infoLayout.setCardBackgroundColor(bg)
// //                        songChangeListener.onNowPlayingSongChange()
//                    },
//                )
//                transformations(
//                    RoundedCornersTransformation(8f),
//                )
//            }
// //            val request = ImageRequest.Builder(requireContext())
// //                .data(Uri.parse(thumbUrl))
// //                .diskCacheKey(nowPlaying.videoId)
// //                .diskCachePolicy(CachePolicy.ENABLED)
// //                .target(
// //                    onStart = {
// //                        binding.ivArt.setImageResource(0)
// //                        binding.loadingArt.visibility = View.VISIBLE
// //                        Log.d("Update UI", "onStart: ")
// //                    },
// //                    onSuccess = { result ->
// //                        binding.ivArt.visibility = View.VISIBLE
// //                        binding.loadingArt.visibility = View.GONE
// //                        binding.ivArt.setImageDrawable(result)
// //                        Log.d("Update UI", "onSuccess: ")
// //                        if (viewModel.gradientDrawable.value != null) {
// //                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
// //                                binding.rootLayout.background = it
// // //                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
// // //                                binding.lyricsLayout.setCardBackgroundColor(color)
// // //                                Log.d("Update UI", "Lyrics: $color")
// // //                                updateStatusBarColor(color)
// // //                            })
// //                                viewModel.lyricsBackground.value?.let { it1 ->
// //                                    binding.lyricsLayout.setCardBackgroundColor(
// //                                        it1
// //                                    )
// //                                }
// //                            }
// //                            Log.d("Update UI", "updateUI: NULL")
// //                        }
// // //                        songChangeListener.onNowPlayingSongChange()
// //                    },
// //                )
// //                .transformations(object : Transformation {
// //                    override val cacheKey: String
// //                        get() = nowPlaying.videoId
// //
// //                    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
// //                        val p = Palette.from(input).generate()
// //                        val defaultColor = 0x000000
// //                        var startColor = p.getDarkVibrantColor(defaultColor)
// //                        Log.d("Check Start Color", "transform: $startColor")
// //                        if (startColor == defaultColor) {
// //                            startColor = p.getDarkMutedColor(defaultColor)
// //                            if (startColor == defaultColor) {
// //                                startColor = p.getVibrantColor(defaultColor)
// //                                if (startColor == defaultColor) {
// //                                    startColor = p.getMutedColor(defaultColor)
// //                                    if (startColor == defaultColor) {
// //                                        startColor = p.getLightVibrantColor(defaultColor)
// //                                        if (startColor == defaultColor) {
// //                                            startColor = p.getLightMutedColor(defaultColor)
// //                                        }
// //                                    }
// //                                }
// //                            }
// //                            Log.d("Check Start Color", "transform: $startColor")
// //                        }
// // //                    val centerColor = 0x6C6C6C
// //                        val endColor = 0x1b1a1f
// //                        val gd = GradientDrawable(
// //                            GradientDrawable.Orientation.TOP_BOTTOM,
// //                            intArrayOf(startColor, endColor)
// //                        )
// //                        gd.cornerRadius = 0f
// //                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
// //                        gd.gradientRadius = 0.5f
// //                        gd.alpha = 150
// //                        val bg = ColorUtils.setAlphaComponent(startColor, 230)
// //                        viewModel.gradientDrawable.postValue(gd)
// //                        viewModel.lyricsBackground.postValue(bg)
// //                        return input
// //                    }
// //
// //                })
// //                .build()
// //            ImageLoader(requireContext()).enqueue(request)
//            binding.topAppBar.subtitle = from
//            viewModel.from.postValue(from)
//            binding.tvSongTitle.text = nowPlaying.title
//            binding.tvSongTitle.isSelected = true
//            val tempArtist = mutableListOf<String>()
//            if (nowPlaying.artists != null) {
//                for (artist in nowPlaying.artists) {
//                    tempArtist.add(artist.name)
//                }
//            }
//            val artistName: String = connectArtists(tempArtist)
//            binding.tvSongArtist.text = artistName
//            binding.tvSongArtist.isSelected = true
//            binding.tvSongTitle.visibility = View.VISIBLE
//            binding.tvSongArtist.visibility = View.VISIBLE
//        }
//    }

//    private fun updateUIfromCurrentMediaItem(mediaItem: MediaItem?) {
//        if (mediaItem != null) {
//            binding.ivArt.setImageResource(0)
//            binding.loadingArt.visibility = View.VISIBLE
// //            viewModel.getFormat(mediaItem.mediaId)
//            Log.d("Update UI", "current: ${mediaItem.mediaMetadata.title}")
//            binding.tvSongTitle.visibility = View.VISIBLE
//            binding.tvSongArtist.visibility = View.VISIBLE
//            binding.topAppBar.subtitle = from
//            viewModel.from.postValue(from)
//            binding.tvSongTitle.setTextAnimation(mediaItem.mediaMetadata.title.toString())
//            binding.tvSongTitle.isSelected = true
//            binding.tvSongArtist.setTextAnimation(mediaItem.mediaMetadata.artist.toString())
//            binding.tvSongArtist.isSelected = true
//            binding.ivArt.load(mediaItem.mediaMetadata.artworkUri) {
//                diskCacheKey(mediaItem.mediaId)
//                diskCachePolicy(CachePolicy.ENABLED)
//                crossfade(true)
//                crossfade(300)
//                listener(
//                    onStart = {
//                        binding.ivArt.setImageResource(0)
//                        binding.loadingArt.visibility = View.VISIBLE
//                        Log.d("Update UI", "onStart: ")
//                    },
//                    onSuccess = { _, result ->
//                        binding.ivArt.visibility = View.VISIBLE
//                        binding.loadingArt.visibility = View.GONE
//                        val p = Palette.from(result.drawable.toBitmap()).generate()
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
// //                    val centerColor = 0x6C6C6C
//                        val endColor = 0x1b1a1f
//                        val gd =
//                            GradientDrawable(
//                                GradientDrawable.Orientation.TL_BR,
//                                intArrayOf(startColor, endColor),
//                            )
//                        gd.cornerRadius = 0f
//                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
//                        gd.gradientRadius = 0.5f
//                        gd.alpha = 150
//                        val bg = ColorUtils.setAlphaComponent(startColor, 230)
//                        viewModel.gradientDrawable.postValue(gd)
//                        viewModel.lyricsBackground.postValue(bg)
//                        Log.d("Update UI", "onSuccess: ")
//                        var start = binding.rootLayout.background
//                        if (start == null) {
//                            start = ColorDrawable(Color.BLACK)
//                        }
//                        val transition = TransitionDrawable(arrayOf(start, gd))
//                        transition.setDither(true)
//                        binding.rootLayout.background = transition
//                        transition.isCrossFadeEnabled = true
//                        transition.startTransition(500)
// //                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
// //                                binding.lyricsLayout.setCardBackgroundColor(color)
// //                                Log.d("Update UI", "Lyrics: $color")
// //                                updateStatusBarColor(color)
// //                            })
//                        binding.lyricsLayout.setCardBackgroundColor(
//                            bg
//                        )
//                        binding.infoLayout.setCardBackgroundColor(bg)
// //                        songChangeListener.onNowPlayingSongChange()
//                    },
//                )
//                transformations(
//                    RoundedCornersTransformation(8f)
//                )
//            }
// //            val request = ImageRequest.Builder(requireContext())
// //                .data(mediaItem.mediaMetadata.artworkUri)
// //                .diskCacheKey(mediaItem.mediaId)
// //                .diskCachePolicy(CachePolicy.ENABLED)
// //                .target(
// //                    onStart = {
// //                        binding.ivArt.setImageResource(0)
// //                        binding.loadingArt.visibility = View.VISIBLE
// //                        Log.d("Update UI", "onStart: ")
// //                    },
// //                    onSuccess = { result ->
// //                        binding.ivArt.visibility = View.VISIBLE
// //                        binding.loadingArt.visibility = View.GONE
// //                        binding.ivArt.setImageDrawable(result)
// //                        Log.d("Update UI", "onSuccess: ")
// //                        if (viewModel.gradientDrawable.value != null) {
// //                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
// //                                if (it != null) {
// //                                    val start = binding.rootLayout.background
// //                                    val transition = TransitionDrawable(arrayOf(start, it))
// //                                    binding.rootLayout.background = transition
// //                                    transition.isCrossFadeEnabled = true
// //                                    transition.startTransition(500)
// //                                }
// // //                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
// // //                                binding.lyricsLayout.setCardBackgroundColor(color)
// // //                                Log.d("Update UI", "Lyrics: $color")
// // //                                updateStatusBarColor(color)
// // //                            })
// //                                viewModel.lyricsBackground.value?.let { it1 ->
// //                                    binding.lyricsLayout.setCardBackgroundColor(
// //                                        it1
// //                                    )
// //                                }
// //                            }
// //                            Log.d("Update UI", "updateUI: NULL")
// //                        }
// // //                        songChangeListener.onNowPlayingSongChange()
// //                    },
// //                )
// //                .diskCacheKey(mediaItem.mediaMetadata.artworkUri.toString())
// //                .diskCachePolicy(CachePolicy.ENABLED)
// //                .transformations(object : Transformation {
// //                    override val cacheKey: String
// //                        get() = "paletteArtTransformer"
// //
// //                    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
// //                        val p = Palette.from(input).generate()
// //                        val defaultColor = 0x000000
// //                        var startColor = p.getDarkVibrantColor(defaultColor)
// //                        Log.d("Check Start Color", "transform: $startColor")
// //                        if (startColor == defaultColor) {
// //                            startColor = p.getDarkMutedColor(defaultColor)
// //                            if (startColor == defaultColor) {
// //                                startColor = p.getVibrantColor(defaultColor)
// //                                if (startColor == defaultColor) {
// //                                    startColor = p.getMutedColor(defaultColor)
// //                                    if (startColor == defaultColor) {
// //                                        startColor = p.getLightVibrantColor(defaultColor)
// //                                        if (startColor == defaultColor) {
// //                                            startColor = p.getLightMutedColor(defaultColor)
// //                                        }
// //                                    }
// //                                }
// //                            }
// //                            Log.d("Check Start Color", "transform: $startColor")
// //                        }
// // //                    val centerColor = 0x6C6C6C
// //                        val endColor = 0x1b1a1f
// //                        val gd = GradientDrawable(
// //                            GradientDrawable.Orientation.TOP_BOTTOM,
// //                            intArrayOf(startColor, endColor)
// //                        )
// //                        gd.cornerRadius = 0f
// //                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
// //                        gd.gradientRadius = 0.5f
// //                        gd.alpha = 150
// //                        val bg = ColorUtils.setAlphaComponent(startColor, 230)
// //                        viewModel.gradientDrawable.postValue(gd)
// //                        viewModel.lyricsBackground.postValue(bg)
// //                        return input
// //                    }
// //
// //                })
// //                .build()
// //            ImageLoader(requireContext()).enqueue(request)
//        }
//    }

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
//        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
//        val miniplayer = activity.findViewById<ComposeView>(R.id.miniplayer)
//        bottom.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
//        bottom.visibility = View.VISIBLE
//        miniplayer.animation =
//            AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
//        miniplayer.visibility = View.VISIBLE
//        if (!isFullScreen) {
//            bottom.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
//            bottom.visibility = View.VISIBLE
//            miniplayer.animation =
//                AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
//            miniplayer.visibility = View.VISIBLE
//            if (viewModel.isFirstMiniplayer) {
//                val balloon =
//                    Balloon
//                        .Builder(requireContext())
//                        .setWidthRatio(0.5f)
//                        .setHeight(BalloonSizeSpec.WRAP)
//                        .setText(getString(R.string.guide_miniplayer_content))
//                        .setTextColorResource(R.color.md_theme_dark_onSurface)
//                        .setTextSize(11f)
//                        .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
//                        .setAutoDismissDuration(5000L)
//                        .setArrowSize(10)
//                        .setArrowPosition(0.5f)
//                        .setPadding(8)
//                        .setCornerRadius(8f)
//                        .setBackgroundColorResource(R.color.md_theme_dark_onSecondary)
//                        .setBalloonAnimation(BalloonAnimation.ELASTIC)
//                        .setLifecycleOwner(activity)
//                        .build()
//                balloon.showAlignTop(miniplayer)
//                viewModel.putString("miniplayer_guide", STATUS_DONE)
//                viewModel.isFirstMiniplayer = false
//            }
//        }
//        isFullScreen = false
//        overlayJob?.cancel()
//        canvasOverlayJob?.cancel()
    }

    private inline fun View.getDimensions(
        crossinline onDimensionsReady: (
            w: Int,
            h: Int,
            margin: List<Int>,
            xy: Pair<Int, Int>,
        ) -> Unit,
    ) {
        lateinit var layoutListener: ViewTreeObserver.OnGlobalLayoutListener
        layoutListener =
            ViewTreeObserver.OnGlobalLayoutListener {
                viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
                onDimensionsReady(
                    width,
                    height,
                    listOf(marginStart, marginEnd, marginTop, marginBottom),
                    Pair(translationX.toInt(), translationY.toInt()),
                )
            }
        viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }
}