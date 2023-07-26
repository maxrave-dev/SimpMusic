package com.maxrave.simpmusic.ui.fragment.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.load
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentNowPlayingBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.isMyServiceRunning
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toListTrack
import com.maxrave.simpmusic.service.RepeatState
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.service.test.source.FetchQueue
import com.maxrave.simpmusic.service.test.source.MusicSource
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import javax.inject.Inject


@UnstableApi
@AndroidEntryPoint
class NowPlayingFragment : Fragment() {

    @Inject
    lateinit var musicSource: MusicSource


    private val viewModel by activityViewModels<SharedViewModel>()
    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    private var metadataCurSong: MetadataSong? = null

    private var videoId: String? = null
    private var from: String? = null
    private var type: String? = null
    private var index: Int? = null
    private var downloaded: Int? = null

    private var gradientDrawable: GradientDrawable? = null
    private var lyricsBackground: Int? = null

    private lateinit var songChangeListener: OnNowPlayingSongChangeListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNowPlayingSongChangeListener) {
            songChangeListener = context
        } else {
            throw RuntimeException("$context must implement OnNowPlayingSongChangeListener")
        }
    }

    override fun onResume() {
        Log.d("NowPlayingFragment", "onResume")
        updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
        super.onResume()
    }

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
        binding.root.applyInsetter {
            type(navigationBars = true) {
                margin()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val card = activity.findViewById<MaterialCardView>(R.id.card)
        bottom.visibility = View.GONE
        card.visibility = View.GONE
        binding.lyricsFullLayout.visibility = View.GONE
        binding.buffered.max = 100
        Log.d("check Video ID in ViewModel", viewModel.videoId.value.toString())

        type = arguments?.getString("type")
        videoId = arguments?.getString("videoId")
        from = arguments?.getString("from")
        index = arguments?.getInt("index")
        downloaded = arguments?.getInt("downloaded")

        Log.d("check Video ID in Fragment", videoId.toString())
        when (type) {
            Config.SONG_CLICK -> {
                if (viewModel.videoId.value == videoId) {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                } else {
                    Log.i("Now Playing Fragment", "Bên trên")
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        musicSource.reset()
                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
                            requireActivity().stopService(
                                Intent(
                                    requireContext(),
                                    FetchQueue::class.java
                                )
                            )
                        }
                        viewModel.loadMediaItemFromTrack(it)
                        viewModel.songDB.observe(viewLifecycleOwner) { songDB ->
                            if (songDB.downloadState != DownloadState.STATE_DOWNLOADED) {
                                viewModel.videoId.postValue(it.videoId)
                                viewModel.from.postValue(from)
                                viewModel.resetLyrics()
                                if (it.artists.isNullOrEmpty()) {
                                    viewModel.getLyrics(it.title)
                                } else {
                                    viewModel.getLyrics(it.title + " " + it.artists.first().name)
                                }
                                updateUIfromQueueNowPlaying()
                                lifecycleScope.launch {
                                    viewModel.firstTrackAdded.collect { added ->
                                        if (added) {
                                            viewModel.changeFirstTrackAddedToFalse()
                                            getRelated(it.videoId)
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }

            Config.VIDEO_CLICK -> {
                if (viewModel.videoId.value == videoId) {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                } else {
//                if (!viewModel.songTransitions.value){
                    Log.i("Now Playing Fragment", "Bên trên")
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        musicSource.reset()
                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
                            requireActivity().stopService(
                                Intent(
                                    requireContext(),
                                    FetchQueue::class.java
                                )
                            )
                        }
                        viewModel.loadMediaItemFromTrack(it)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        viewModel.resetLyrics()
                        viewModel.getLyrics(it.title + " " + it.artists?.first()?.name)
                        updateUIfromQueueNowPlaying()
                        lifecycleScope.launch {
                            viewModel.firstTrackAdded.collect { added ->
                                if (added) {
                                    viewModel.changeFirstTrackAddedToFalse()
                                    getVideosRelated(it.videoId)
                                }
                            }
                        }
                    }
                    //}
//                viewModel.loadMediaItems(videoId!!)
                }
            }

            Config.ALBUM_CLICK -> {
                if (viewModel.videoId.value == videoId) {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                } else {
//                if (!viewModel.songTransitions.value){
                    Log.i("Now Playing Fragment", "Bên trên")
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        musicSource.reset()
                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
                            requireActivity().stopService(
                                Intent(
                                    requireContext(),
                                    FetchQueue::class.java
                                )
                            )
                        }
                        viewModel.loadMediaItemFromTrack(it)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        viewModel.resetLyrics()
                        viewModel.getLyrics(it.title + " " + it.artists?.first()?.name)
                        updateUIfromQueueNowPlaying()
                        Log.d("check index", index.toString())
                        lifecycleScope.launch {
                            viewModel.firstTrackAdded.collect { added ->
                                if (added) {
                                    viewModel.changeFirstTrackAddedToFalse()
                                    if (index == null) {
                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                                    } else {
                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                                    }
                                }
                            }
                        }
                    }
                    //}
//                viewModel.loadMediaItems(videoId!!)
                }
            }

            Config.PLAYLIST_CLICK -> {
                if (viewModel.videoId.value == videoId) {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                } else {
//                if (!viewModel.songTransitions.value){
                    Log.i("Now Playing Fragment", "Bên trên")
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        musicSource.reset()
                        if (requireContext().isMyServiceRunning(FetchQueue::class.java)) {
                            requireActivity().stopService(
                                Intent(
                                    requireContext(),
                                    FetchQueue::class.java
                                )
                            )
                        }
                        viewModel.loadMediaItemFromTrack(it)

//                        viewModel.getMetadata(it.videoId)
//                        observerMetadata()
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        viewModel.resetLyrics()
                        viewModel.getLyrics(it.title + " " + it.artists?.first()?.name)
                        updateUIfromQueueNowPlaying()
                        Log.d("check index", index.toString())
                        lifecycleScope.launch {
                            viewModel.firstTrackAdded.collect { added ->
                                if (added) {
                                    viewModel.changeFirstTrackAddedToFalse()
                                    if (index == null) {
                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                                    } else {
                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                                    }
                                }
                            }
                        }
                    }
                    //}
//                viewModel.loadMediaItems(videoId!!)
                }
            }

            Config.MINIPLAYER_CLICK -> {
                videoId = viewModel.videoId.value
                from = viewModel.from.value
                metadataCurSong = viewModel.metadata.value?.data
                gradientDrawable = viewModel.gradientDrawable.value
                lyricsBackground = viewModel.lyricsBackground.value
                updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
            }
        }

        lifecycleScope.launch {
            val job7 = launch {
                viewModel.songTransitions.collect { isChanged ->
                    if (isChanged) {
                        if (viewModel.getCurrentMediaItem() != null) {
                            Log.i("Now Playing Fragment", "Bên dưới")
                            Log.d("Song Transition", "Song Transition")
                            videoId = viewModel.videoId.value
                            binding.ivArt.visibility = View.GONE
                            binding.loadingArt.visibility = View.VISIBLE
//                            val track = viewModel.nowPlayingMediaItem.value
//                            if (track != null) {
//                                viewModel.resetLyrics()
//                                viewModel.getLyrics(track.mediaMetadata.title.toString() + " " + track.mediaMetadata.artist)
//                            }
                            Log.d("Check Lyrics", viewModel._lyrics.value?.data.toString())
                            updateUIfromCurrentMediaItem(viewModel.nowPlayingMediaItem.value)
                            musicSource.setCurrentSongIndex(viewModel.getCurrentMediaItemIndex())
                            viewModel.changeSongTransitionToFalse()
                        }
                    }
                }
            }
//            val job11 = launch {
//                viewModel.nowPlayingMediaItem.observe(viewLifecycleOwner) {
//                    if (it != null){
//                        Log.i("Now Playing Fragment", "Bên dưới")
//                        Log.d("Song Transition", "Song Transition")
//                        videoId = viewModel.videoId.value
//                        binding.ivArt.visibility = View.GONE
//                        binding.loadingArt.visibility = View.VISIBLE
//                        viewModel.resetLyrics()
//                        Log.d("Check Lyrics", viewModel._lyrics.value?.data.toString())
//                        updateUIfromCurrentMediaItem(viewModel.nowPlayingMediaItem.value)
//                        musicSource.setCurrentSongIndex(viewModel.getCurrentMediaItemIndex())
//                        viewModel.changeSongTransitionToFalse()
//                    }
//                }
//            }
            val job1 = launch {
                viewModel.progressString.observe(viewLifecycleOwner) {
                    binding.tvCurrentTime.text = it
                    if (viewModel.progress.value * 100 in 0f..100f) {
                        binding.progressSong.value = viewModel.progress.value * 100
                        songChangeListener.onUpdateProgressBar(viewModel.progress.value * 100)
                    }
                }
                viewModel.duration.collect {
                    if (viewModel.formatDuration(it).contains('-')) {
                        binding.tvFullTime.text = getString(R.string.na_na)
                    } else {
                        binding.tvFullTime.text = viewModel.formatDuration(it)
                    }
                }
            }
            val job2 = launch {
                viewModel.isPlaying.observe(viewLifecycleOwner) {
                    Log.d("Check Song Transistion", "${viewModel.songTransitions.value}")
                    if (it) {
                        //updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                        binding.btPlayPause.setImageResource(R.drawable.baseline_pause_circle_24)
                        songChangeListener.onIsPlayingChange()
                    } else {
                        binding.btPlayPause.setImageResource(R.drawable.baseline_play_circle_24)
                        songChangeListener.onIsPlayingChange()
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
                        val temp = viewModel.getLyricsString(it)
                        binding.tvSyncState.text = when (viewModel.getLyricsSyncState()) {
                            Config.SyncState.NOT_FOUND -> null
                            Config.SyncState.LINE_SYNCED -> "Line Synced"
                            Config.SyncState.UNSYNCED -> "Unsynced"
                        }
                        if (temp != null) {
                            if (temp.nowLyric == "Lyrics not found") {
                                binding.lyricsLayout.visibility = View.GONE
                                binding.lyricsTextLayout.visibility = View.GONE
                            } else {
                                if (binding.btFull.text == "Show") {
                                    binding.lyricsTextLayout.visibility = View.VISIBLE
                                }
                                binding.lyricsLayout.visibility = View.VISIBLE
                                if (temp.nowLyric != null) {
                                    binding.tvNowLyrics.visibility = View.VISIBLE
                                    binding.tvNowLyrics.text = temp.nowLyric
                                } else {
                                    binding.tvNowLyrics.visibility = View.GONE
                                }
                                if (temp.prevLyrics != null) {
                                    binding.tvPrevLyrics.visibility = View.VISIBLE
                                    if (temp.prevLyrics.size > 1) {
                                        val txt = temp.prevLyrics[0] + "\n" + temp.prevLyrics[1]
                                        binding.tvPrevLyrics.text = txt
                                    } else {
                                        binding.tvPrevLyrics.text = temp.prevLyrics[0]
                                    }
                                } else {
                                    binding.tvPrevLyrics.visibility = View.GONE
                                }
                                if (temp.nextLyric != null) {
                                    binding.tvNextLyrics.visibility = View.VISIBLE
                                    if (temp.nextLyric.size > 1) {
                                        val txt = temp.nextLyric[0] + "\n" + temp.nextLyric[1]
                                        binding.tvNextLyrics.text = txt
                                    } else {
                                        binding.tvNextLyrics.text = temp.nextLyric[0]
                                    }
                                } else {
                                    binding.tvNextLyrics.visibility = View.GONE
                                }
                            }
                        }
                    } else {
                        binding.lyricsLayout.visibility = View.GONE
                        binding.lyricsTextLayout.visibility = View.GONE
                    }
                }
            }
            val job6 = launch {
                viewModel.lyricsFull.observe(viewLifecycleOwner) {
                    if (it != null) {
                        binding.tvFullLyrics.text = it
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
                viewModel.nextTrackAvailable.collect { nextTrackAvailable ->
                    if (nextTrackAvailable) {
                        setEnabledAll(binding.btNext, true)
                    } else {
                        setEnabledAll(binding.btNext, false)
                    }
                }
            }
            val job12 = launch {
                viewModel.previousTrackAvailable.collect { previousTrackAvailable ->
                    if (previousTrackAvailable) {
                        setEnabledAll(binding.btPrevious, true)
                    } else {
                        setEnabledAll(binding.btPrevious, false)
                    }
                }
            }

            job1.join()
            job2.join()
            job3.join()
            job4.join()
            job5.join()
            job6.join()
            job7.join()
            job8.join()
            job9.join()
            job10.join()
            job11.join()
            job12.join()
        }
        binding.btFull.setOnClickListener {
            if (binding.btFull.text == "Show") {
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
            findNavController().navigate(R.id.action_nowPlayingFragment_to_queueFragment)
        }
        binding.btSongInfo.setOnClickListener {
            findNavController().navigate(R.id.action_nowPlayingFragment_to_infoFragment)
        }
        binding.cbFavorite.setOnCheckedChangeListener { cb, isChecked ->
            if (!isChecked) {
                viewModel.getCurrentMediaItem()?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(
                        nowPlayingSong.mediaId,
                        false
                    )
                }
            } else {
                viewModel.getCurrentMediaItem()?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(
                        nowPlayingSong.mediaId,
                        true
                    )
                }
            }
        }
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.now_playing_dialog_menu_item_more -> {
                    if (musicSource.catalogMetadata.isNotEmpty()) {
                        viewModel.refreshSongDB()
                        val dialog = BottomSheetDialog(requireContext())
                        val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                        with(bottomSheetView) {
                            if (viewModel.liked.value) {
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
                                    setEnabledAll(btDownload, false)
                                }

                                DownloadState.STATE_DOWNLOADED -> {
                                    tvDownload.text = getString(R.string.downloaded)
                                    ivDownload.setImageResource(R.drawable.baseline_downloaded)
                                    setEnabledAll(btDownload, true)
                                }
                            }
                            val song =
                                musicSource.catalogMetadata[viewModel.getCurrentMediaItemIndex()]
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
                            btAddPlaylist.setOnClickListener {
                                viewModel.getAllLocalPlaylist()
                                val listLocalPlaylist: ArrayList<LocalPlaylistEntity> = arrayListOf()
                                val addPlaylistDialog = BottomSheetDialog(requireContext())
                                val viewAddPlaylist = BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
                                val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
                                viewAddPlaylist.rvLocalPlaylists.apply {
                                    adapter = addToAPlaylistAdapter
                                    layoutManager = LinearLayoutManager(requireContext())
                                }
                                viewModel.localPlaylist.observe(viewLifecycleOwner) {list ->
                                    Log.d("Check Local Playlist", list.toString())
                                    listLocalPlaylist.clear()
                                    listLocalPlaylist.addAll(list)
                                    addToAPlaylistAdapter.updateList(listLocalPlaylist)
                                }
                                addToAPlaylistAdapter.setOnItemClickListener(object : AddToAPlaylistAdapter.OnItemClickListener{
                                    override fun onItemClick(position: Int) {
                                        val playlist = listLocalPlaylist[position]
                                        val tempTrack = ArrayList<String>()
                                        if (playlist.tracks != null) {
                                            tempTrack.addAll(playlist.tracks)
                                        }
                                        tempTrack.add(song.videoId)
                                        tempTrack.removeConflicts()
                                        viewModel.updateLocalPlaylistTracks(tempTrack, playlist.id)
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
                                val subBottomSheetView =
                                    BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                                if (song.artists != null) {
                                    val artistAdapter = SeeArtistOfNowPlayingAdapter(song.artists)
                                    subBottomSheetView.rvArtists.apply {
                                        adapter = artistAdapter
                                        layoutManager = LinearLayoutManager(requireContext())
                                    }
                                    artistAdapter.setOnClickListener(object :
                                        SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                        override fun onItemClick(position: Int) {
                                            val artist = song.artists[position]
                                            if (artist.id != null) {
                                                findNavController().navigate(
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
                            btShare.setOnClickListener {
                                val shareIntent = Intent(Intent.ACTION_SEND)
                                shareIntent.type = "text/plain"
                                val url = "https://youtube.com/watch?v=${song.videoId}"
                                shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                                val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ URL")
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
                                        DownloadRequest.Builder(song.videoId, song.videoId.toUri())
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
                                                        tvDownload.text = getString(R.string.downloading)
                                                        ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                                                        setEnabledAll(btDownload, false)
                                                    }

                                                    Download.STATE_FAILED -> {
                                                        viewModel.updateDownloadState(
                                                            song.videoId,
                                                            DownloadState.STATE_NOT_DOWNLOADED
                                                        )
                                                        tvDownload.text = getString(R.string.download)
                                                        ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                                        setEnabledAll(btDownload, true)
                                                        Toast.makeText(
                                                            requireContext(),
                                                            "Download failed",
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
                                                            "Download completed",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        tvDownload.text = getString(R.string.downloaded)
                                                        ivDownload.setImageResource(R.drawable.baseline_downloaded)
                                                        setEnabledAll(btDownload, true)
                                                    }

                                                    Download.STATE_QUEUED -> {
                                                        TODO()
                                                    }

                                                    Download.STATE_REMOVING -> {
                                                        TODO()
                                                    }

                                                    Download.STATE_RESTARTING -> {
                                                        TODO()
                                                    }

                                                    Download.STATE_STOPPED -> {
                                                        TODO()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else if (tvDownload.text == getString(R.string.downloaded)){
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
                                        "Removed download",
                                        Toast.LENGTH_SHORT
                                    ).show()
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

    private fun fetchSourceFromQueue(index: Int? = null, downloaded: Int = 0) {
        if (index == null) {
            if (!requireContext().isMyServiceRunning(FetchQueue::class.java)) {
                requireActivity().startService(Intent(requireContext(), FetchQueue::class.java))
            } else {
                requireActivity().stopService(Intent(requireContext(), FetchQueue::class.java))
                requireActivity().startService(Intent(requireContext(), FetchQueue::class.java))
            }
        } else {
            Log.d("fetchSourceFromQueue", "fetchSourceFromQueue: $index")
            val mIntent = Intent(requireContext(), FetchQueue::class.java)
            mIntent.putExtra("index", index)
            if (downloaded != 0) {
                mIntent.putExtra("downloaded", downloaded)
            }
            if (!requireContext().isMyServiceRunning(FetchQueue::class.java)) {
                requireActivity().startService(mIntent)
            } else {
                requireActivity().stopService(Intent(requireContext(), FetchQueue::class.java))
                requireActivity().startService(mIntent)
            }
        }
    }

    private fun getVideosRelated(videoId: String) {
        viewModel.getVideoRelated(videoId)
        viewModel.videoRelated.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    val data = response.data!!
                    val queue = data.toListTrack()
                    Queue.addAll(queue)
                    if (!requireContext().isMyServiceRunning(FetchQueue::class.java)) {
                        requireActivity().startService(
                            Intent(
                                requireContext(),
                                FetchQueue::class.java
                            )
                        )
                    } else {
                        requireActivity().stopService(
                            Intent(
                                requireContext(),
                                FetchQueue::class.java
                            )
                        )
                        requireActivity().startService(
                            Intent(
                                requireContext(),
                                FetchQueue::class.java
                            )
                        )
                    }
                }

                is Resource.Error -> {
                    Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                    Log.d("Error", "${response.message}")
                }
            }
        }
    }

    private fun getRelated(videoId: String) {
        viewModel.getRelated(videoId)
        viewModel.related.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    Queue.addAll(response.data!!)
                    if (!requireContext().isMyServiceRunning(FetchQueue::class.java)) {
                        requireActivity().startService(
                            Intent(
                                requireContext(),
                                FetchQueue::class.java
                            )
                        )
                    } else {
                        requireActivity().stopService(
                            Intent(
                                requireContext(),
                                FetchQueue::class.java
                            )
                        )
                        requireActivity().startService(
                            Intent(
                                requireContext(),
                                FetchQueue::class.java
                            )
                        )
                    }
                }

                is Resource.Error -> {
                    Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                    Log.d("Error", "${response.message}")
                }
            }
        }
    }


    private fun updateUIfromQueueNowPlaying() {
        Log.w("CHECK NOW PLAYING IN QUEUE", "updateUIfromQueueNowPlaying: ${Queue.getNowPlaying()}")
        Log.d("CHECK QUEUE", "updateUIfromQueueNowPlaying: ${Queue.getQueue()}")
        val nowPlaying = Queue.getNowPlaying()
        if (nowPlaying != null) {
            binding.ivArt.visibility = View.GONE
            binding.loadingArt.visibility = View.VISIBLE
            Log.d("Update UI", "current: ${nowPlaying.title}")
            var thumbUrl = nowPlaying.thumbnails?.last()?.url!!
            if (thumbUrl.contains("w120")) {
                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
            }
            val request = ImageRequest.Builder(requireContext())
                .data(Uri.parse(thumbUrl))
                .diskCacheKey(nowPlaying.videoId)
                .diskCachePolicy(CachePolicy.ENABLED)
                .target(
                    onStart = {
                        binding.ivArt.visibility = View.GONE
                        binding.loadingArt.visibility = View.VISIBLE
                        Log.d("Update UI", "onStart: ")
                    },
                    onSuccess = { result ->
                        binding.ivArt.visibility = View.VISIBLE
                        binding.loadingArt.visibility = View.GONE
                        binding.ivArt.setImageDrawable(result)
                        Log.d("Update UI", "onSuccess: ")
                        if (viewModel.gradientDrawable.value != null) {
                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                                binding.rootLayout.background = it
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
                        songChangeListener.onNowPlayingSongChange()
                    },
                )
                .transformations(object : Transformation {
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

                })
                .build()
            ImageLoader(requireContext()).enqueue(request)
            binding.topAppBar.subtitle = from
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
        binding.ivArt.visibility = View.GONE
        binding.loadingArt.visibility = View.VISIBLE
        Log.d("Update UI", "current: ${mediaItem?.mediaMetadata?.title}")
        val request = ImageRequest.Builder(requireContext())
            .data(mediaItem?.mediaMetadata?.artworkUri)
            .diskCacheKey(mediaItem?.mediaId)
            .diskCachePolicy(CachePolicy.ENABLED)
            .target(
                onStart = {
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    Log.d("Update UI", "onStart: ")
                },
                onSuccess = { result ->
                    binding.ivArt.visibility = View.VISIBLE
                    binding.loadingArt.visibility = View.GONE
                    binding.ivArt.setImageDrawable(result)
                    Log.d("Update UI", "onSuccess: ")
                    if (viewModel.gradientDrawable.value != null) {
                        viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                            binding.rootLayout.background = it
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
                    songChangeListener.onNowPlayingSongChange()
                },
            )
            .diskCacheKey(mediaItem?.mediaMetadata?.artworkUri.toString())
            .diskCachePolicy(CachePolicy.ENABLED)
            .transformations(object : Transformation {
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
            .build()
        ImageLoader(requireContext()).enqueue(request)
        binding.topAppBar.subtitle = from
        if (mediaItem != null) {
            binding.tvSongTitle.text = mediaItem.mediaMetadata.title
            binding.tvSongTitle.isSelected = true
            binding.tvSongArtist.text = mediaItem.mediaMetadata.artist
            binding.tvSongArtist.isSelected = true
        }
        binding.tvSongTitle.visibility = View.VISIBLE
        binding.tvSongArtist.visibility = View.VISIBLE
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

    fun updateStatusBarColor(color: Int) { // Color must be in hexadecimal format
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        val window: Window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor("99" + color.toString())
    }

    public interface OnNowPlayingSongChangeListener {
        fun onNowPlayingSongChange()
        fun onIsPlayingChange()
        fun onUpdateProgressBar(progress: Float)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val card = activity.findViewById<MaterialCardView>(R.id.card)
        bottom.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
        bottom.visibility = View.VISIBLE
        card.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
        card.visibility = View.VISIBLE
    }


}