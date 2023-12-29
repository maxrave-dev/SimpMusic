package com.maxrave.simpmusic.ui.fragment.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.daimajia.swipe.SwipeLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.LYRICS_PROVIDER
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetFullscreenBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSleepTimerBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.service.RepeatState
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

@AndroidEntryPoint
class FullscreenFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()
    private val binding by lazy { BottomSheetFullscreenBinding.inflate(layoutInflater) }
    private var showJob: Job? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity?.window?.navigationBarColor = Color.TRANSPARENT
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val miniplayer = activity.findViewById<SwipeLayout>(R.id.miniplayer)
        bottom.visibility = View.GONE
        miniplayer.visibility = View.GONE
        if (!viewModel.isFullScreen) {
            hideSystemUI()
            viewModel.isFullScreen = true
        }
        if (binding.playerView.player == null) {
            binding.playerView.player = viewModel.simpleMediaServiceHandler?.player
        }

        with(binding) {
            overlayLayout.visibility = View.VISIBLE
            showJob = lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                    delay(3000)
                    overlayLayout.visibility = View.GONE
                }
            }
            if (viewModel.isSubtitle) {
                binding.btSubtitle.setImageResource(R.drawable.baseline_subtitles_24)
                binding.subtitleView.visibility = View.VISIBLE
            } else {
                binding.btSubtitle.setImageResource(R.drawable.baseline_subtitles_off_24)
                binding.subtitleView.visibility = View.GONE
            }
            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
            btOutFullScreen.setOnClickListener {
                findNavController().popBackStack()
            }
            btNext.setOnClickListener {
                viewModel.onUIEvent(UIEvent.Next)
            }
            btPrevious.setOnClickListener {
                viewModel.onUIEvent(UIEvent.Previous)
            }
            btPlayPause.setOnClickListener {
                viewModel.onUIEvent(UIEvent.PlayPause)
            }
            binding.btShuffle.setOnClickListener {
                viewModel.onUIEvent(UIEvent.Shuffle)
            }
            binding.btRepeat.setOnClickListener {
                viewModel.onUIEvent(UIEvent.Repeat)
            }
            binding.btSubtitle.setOnClickListener {
                viewModel.isSubtitle = !viewModel.isSubtitle
                if (viewModel.isSubtitle) {
                    binding.btSubtitle.setImageResource(R.drawable.baseline_subtitles_24)
                    binding.subtitleView.visibility = View.VISIBLE
                } else {
                    binding.btSubtitle.setImageResource(R.drawable.baseline_subtitles_off_24)
                    binding.subtitleView.visibility = View.GONE
                }
            }
            binding.progressSong.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {

                }

                override fun onStopTrackingTouch(slider: Slider) {
                    viewModel.onUIEvent(UIEvent.UpdateProgress(slider.value))
                }
            })
            rootLayout.setOnClickListener {
                if (overlayLayout.visibility == View.VISIBLE) {
                    showJob?.cancel()
                    overlayLayout.visibility = View.GONE
                } else {
                    val shortAnimationDuration =
                        resources.getInteger(android.R.integer.config_mediumAnimTime)
                    overlayLayout.alpha = 0f
                    overlayLayout.apply {
                        visibility = View.VISIBLE
                        animate()
                            .alpha(1f)
                            .setDuration(shortAnimationDuration.toLong())
                            .setListener(null)
                    }
                    showJob = lifecycleScope.launch {
                        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                            delay(3000)
                            overlayLayout.visibility = View.GONE
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val time = launch {
                    viewModel.progress.collect {
                        if (it in 0.0..1.0) {
                            binding.progressSong.value = it * 100
                        }
                    }
                }
                val timeString = launch {
                    viewModel.progressString.collect { prog ->
                        binding.tvCurrentTime.text = prog
                    }
                }
                val duration = launch {
                    viewModel.duration.collect { dur ->
                        if (dur < 0) {
                            binding.tvFullTime.text = getString(R.string.na_na)
                        } else {
                            binding.tvFullTime.text = viewModel.formatDuration(dur)
                        }
                    }
                }
                val isPlaying = launch {
                    viewModel.isPlaying.collect { isPlaying ->
                        if (isPlaying) {
                            binding.btPlayPause.setImageResource(R.drawable.baseline_pause_circle_24)
                        } else {
                            binding.btPlayPause.setImageResource(R.drawable.baseline_play_circle_24)
                        }
                    }
                }
                val title = launch {
                    viewModel.simpleMediaServiceHandler?.nowPlaying?.collectLatest {
                        if (it != null) {
                            binding.toolbar.title = it.mediaMetadata.title.toString()
                        }
                    }
                }
                val shuffle = launch {
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
                val repeat = launch {
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
                val job5 = launch {
                    viewModel.progressMillis.collect {
                        if (viewModel._lyrics.value?.data != null && viewModel.isSubtitle) {
//                            val temp = viewModel.getLyricsString(it)
                            val lyrics = viewModel._lyrics.value!!.data
                            val translated = viewModel.translateLyrics.value
                            val index = viewModel.getActiveLyrics(it)
                            if (index != null) {
                                if (lyrics?.lines?.get(0)?.words == "Lyrics not found") {
                                    binding.subtitleView.visibility = View.GONE
                                } else {
                                    lyrics.let { it1 ->
                                        if (viewModel.getLyricsSyncState() == Config.SyncState.LINE_SYNCED) {
                                            if (index == -1) {
                                                binding.subtitleView.visibility = View.GONE
                                            } else {
                                                binding.subtitleView.visibility = View.VISIBLE
                                                binding.tvMainSubtitle.text =
                                                    it1?.lines?.get(index)?.words
                                                if (translated != null) {
                                                    val line = translated.lines?.find { line ->
                                                        line.startTimeMs == it1?.lines?.get(index)?.startTimeMs
                                                    }
                                                    if (line != null) {
                                                        binding.tvTranslatedSubtitle.visibility =
                                                            View.VISIBLE
                                                        binding.tvTranslatedSubtitle.text =
                                                            line.words
                                                    } else {
                                                        binding.tvTranslatedSubtitle.visibility =
                                                            View.GONE
                                                    }
                                                }
                                            }
                                        } else if (viewModel.getLyricsSyncState() == Config.SyncState.UNSYNCED) {
                                            binding.subtitleView.visibility = View.GONE
                                        }
                                    }
                                }
                            } else {
                                binding.subtitleView.visibility = View.GONE
                            }
                        } else {
                            binding.subtitleView.visibility = View.GONE
                        }
                    }
                }
                job5.join()
                time.join()
                timeString.join()
                duration.join()
                isPlaying.join()
                title.join()
                repeat.join()
                job11.join()
                job12.join()
                shuffle.join()
            }
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
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
                                                        playlistId = playlist.id,
                                                        songId = song.videoId,
                                                        position = tempTrack.size,
                                                        inPlaylist = LocalDateTime.now()
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
                                    var checkedIndex =
                                        if (mainLyricsProvider == DataStoreManager.MUSIXMATCH) 0 else 1
                                    val dialogChange = MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(getString(R.string.main_lyrics_provider))
                                        .setSingleChoiceItems(
                                            LYRICS_PROVIDER.items,
                                            checkedIndex
                                        ) { _, which ->
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
                                                } else if (checkedIndex == 1) {
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

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        WindowInsetsControllerCompat(
            requireActivity().window,
            binding.root
        ).show(WindowInsetsCompat.Type.systemBars())
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    @UnstableApi
    override fun onDestroy() {
        super.onDestroy()
        binding.playerView.player = null
        if (viewModel.isFullScreen) {
            showSystemUI()
            viewModel.isFullScreen = false
        }
    }
}