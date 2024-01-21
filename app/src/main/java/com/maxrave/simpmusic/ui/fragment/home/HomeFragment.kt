package com.maxrave.simpmusic.ui.fragment.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.home.GenreAdapter
import com.maxrave.simpmusic.adapter.home.HomeItemAdapter
import com.maxrave.simpmusic.adapter.home.HomeItemContentAdapter
import com.maxrave.simpmusic.adapter.home.MoodsMomentAdapter
import com.maxrave.simpmusic.adapter.home.QuickPicksAdapter
import com.maxrave.simpmusic.adapter.home.chart.ArtistChartAdapter
import com.maxrave.simpmusic.adapter.home.chart.TrackChartAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.home.HomeItem
import com.maxrave.simpmusic.data.model.home.chart.toTrack
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentHomeBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.HomeViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar

@AndroidEntryPoint
@UnstableApi
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<HomeViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private lateinit var mAdapter: HomeItemAdapter
    private lateinit var moodsMomentAdapter: MoodsMomentAdapter
    private lateinit var genreAdapter: GenreAdapter
    private lateinit var trackChartAdapter: TrackChartAdapter
    private lateinit var artistChartAdapter: ArtistChartAdapter
    private lateinit var quickPicksAdapter: QuickPicksAdapter
    private lateinit var newReleaseAdapter: HomeItemAdapter
    private val onSongOrVideoLongClickListener by lazy {
        object : HomeItemContentAdapter.OnSongOrVideoLongClickListener {
            override fun onSongOrVideoLongClick(track: Track?) {
                if (track != null) {
                    viewModel.getSongEntity(track)
                    val dialog = BottomSheetDialog(requireContext())
                    val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                    with(bottomSheetView) {
                        viewModel.songEntity.observe(viewLifecycleOwner) { songEntity ->
                            if (songEntity != null) {
                                if (songEntity.liked) {
                                    tvFavorite.text = getString(R.string.liked)
                                    cbFavorite.isChecked = true
                                } else {
                                    tvFavorite.text = getString(R.string.like)
                                    cbFavorite.isChecked = false
                                }

                                when (songEntity.downloadState) {
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

                                    DownloadState.STATE_PREPARING -> {
                                        tvDownload.text = getString(R.string.preparing)
                                        ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                                        setEnabledAll(btDownload, true)
                                    }
                                }
                            }
                        }
                        btChangeLyricsProvider.visibility = View.GONE
                        tvSongTitle.text = track.title
                        tvSongTitle.isSelected = true
                        tvSongArtist.text = track.artists.toListName().connectArtists()
                        tvSongArtist.isSelected = true
                        ivThumbnail.load(track.thumbnails?.last()?.url)
                        btAddQueue.setOnClickListener {
                            sharedViewModel.addToQueue(track)
                        }
                        btPlayNext.setOnClickListener {
                            sharedViewModel.playNext(track)
                        }
                        btRadio.setOnClickListener {
                            val args = Bundle()
                            args.putString("radioId", "RDAMVM${track.videoId}")
                            args.putString(
                                "videoId",
                                track.videoId
                            )
                            dialog.dismiss()
                            findNavController().navigateSafe(
                                R.id.action_global_playlistFragment,
                                args
                            )
                        }
                        btLike.setOnClickListener {
                            if (cbFavorite.isChecked) {
                                cbFavorite.isChecked = false
                                tvFavorite.text = getString(R.string.like)
                                viewModel.updateLikeStatus(track.videoId, false)
                            } else {
                                cbFavorite.isChecked = true
                                tvFavorite.text = getString(R.string.liked)
                                viewModel.updateLikeStatus(track.videoId, true)
                            }
                        }

                        btSeeArtists.setOnClickListener {
                            val subDialog = BottomSheetDialog(requireContext())
                            val subBottomSheetView =
                                BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                            if (track.artists != null) {
                                val artistAdapter = SeeArtistOfNowPlayingAdapter(track.artists)
                                subBottomSheetView.rvArtists.apply {
                                    adapter = artistAdapter
                                    layoutManager = LinearLayoutManager(requireContext())
                                }
                                artistAdapter.setOnClickListener(object :
                                    SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                    override fun onItemClick(position: Int) {
                                        val artist = track.artists[position]
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
                        btShare.setOnClickListener {
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "text/plain"
                            val url = "https://youtube.com/watch?v=${track.videoId}"
                            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                            val chooserIntent =
                                Intent.createChooser(shareIntent, getString(R.string.share_url))
                            startActivity(chooserIntent)
                        }
                        btAddPlaylist.setOnClickListener {
                            viewModel.getAllLocalPlaylist()
                            val listLocalPlaylist: ArrayList<LocalPlaylistEntity> = arrayListOf()
                            val addPlaylistDialog = BottomSheetDialog(requireContext())
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
                                    val tempTrack = ArrayList<String>()
                                    viewModel.updateInLibrary(track.videoId)
                                    if (playlist.tracks != null) {
                                        tempTrack.addAll(playlist.tracks)
                                    }
                                    if (!tempTrack.contains(track.videoId) && playlist.syncedWithYouTubePlaylist == 1 && playlist.youtubePlaylistId != null) {
                                        viewModel.addToYouTubePlaylist(
                                            playlist.id,
                                            playlist.youtubePlaylistId,
                                            track.videoId
                                        )
                                    }
                                    if (!tempTrack.contains(track.videoId)) {
                                        viewModel.insertPairSongLocalPlaylist(
                                            PairSongLocalPlaylist(
                                                playlistId = playlist.id,
                                                songId = track.videoId,
                                                position = playlist.tracks?.size ?: 0,
                                                inPlaylist = LocalDateTime.now()
                                            )
                                        )
                                        tempTrack.add(track.videoId)
                                    }
                                    tempTrack.add(track.videoId)
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
                        btSleepTimer.visibility = View.GONE
                        btDownload.setOnClickListener {
                            if (tvDownload.text == getString(R.string.download)) {
                                Log.d("Download", "onClick: ${track.videoId}")
                                viewModel.updateDownloadState(
                                    track.videoId,
                                    DownloadState.STATE_PREPARING
                                )
                                val downloadRequest =
                                    DownloadRequest.Builder(track.videoId, track.videoId.toUri())
                                        .setData(track.title.toByteArray())
                                        .setCustomCacheKey(track.videoId)
                                        .build()
                                viewModel.updateDownloadState(
                                    track.videoId,
                                    DownloadState.STATE_DOWNLOADING
                                )
                                viewModel.getDownloadStateFromService(track.videoId)
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
                                                        track.videoId,
                                                        DownloadState.STATE_DOWNLOADING
                                                    )
                                                    tvDownload.text =
                                                        getString(R.string.downloading)
                                                    ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                                                    setEnabledAll(btDownload, true)
                                                }

                                                Download.STATE_FAILED -> {
                                                    viewModel.updateDownloadState(
                                                        track.videoId,
                                                        DownloadState.STATE_NOT_DOWNLOADED
                                                    )
                                                    tvDownload.text = getString(R.string.download)
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
                                                        track.videoId,
                                                        DownloadState.STATE_DOWNLOADED
                                                    )
                                                    Toast.makeText(
                                                        requireContext(),
                                                        getString(R.string.downloaded),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    tvDownload.text = getString(R.string.downloaded)
                                                    ivDownload.setImageResource(R.drawable.baseline_downloaded)
                                                    setEnabledAll(btDownload, true)
                                                }

                                                else -> {
                                                    Log.d("Download", "onCreate: ${download.state}")
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
                                    track.videoId,
                                    false
                                )
                                viewModel.updateDownloadState(
                                    track.videoId,
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
                    dialog.setCancelable(true)
                    dialog.setContentView(bottomSheetView.root)
                    dialog.show()
                }
            }
        }
    }

    private val items = arrayOf(
        "US",
        "ZZ",
        "AR",
        "AU",
        "AT",
        "BE",
        "BO",
        "BR",
        "CA",
        "CL",
        "CO",
        "CR",
        "CZ",
        "DK",
        "DO",
        "EC",
        "EG",
        "SV",
        "EE",
        "FI",
        "FR",
        "DE",
        "GT",
        "HN",
        "HU",
        "IS",
        "IN",
        "ID",
        "IE",
        "IL",
        "IT",
        "JP",
        "KE",
        "LU",
        "MX",
        "NL",
        "NZ",
        "NI",
        "NG",
        "NO",
        "PA",
        "PY",
        "PE",
        "PL",
        "PT",
        "RO",
        "RU",
        "SA",
        "RS",
        "ZA",
        "KR",
        "ES",
        "SE",
        "CH",
        "TZ",
        "TR",
        "UG",
        "UA",
        "AE",
        "GB",
        "UY",
        "ZW"
    )
    private val itemsData = arrayOf(
        "United States",
        "Global",
        "Argentina",
        "Australia",
        "Austria",
        "Belgium",
        "Bolivia",
        "Brazil",
        "Canada",
        "Chile",
        "Colombia",
        "Costa Rica",
        "Czech Republic",
        "Denmark",
        "Dominican Republic",
        "Ecuador",
        "Egypt",
        "El Salvador",
        "Estonia",
        "Finland",
        "France",
        "Germany",
        "Guatemala",
        "Honduras",
        "Hungary",
        "Iceland",
        "India",
        "Indonesia",
        "Ireland",
        "Israel",
        "Italy",
        "Japan",
        "Kenya",
        "Luxembourg",
        "Mexico",
        "Netherlands",
        "New Zealand",
        "Nicaragua",
        "Nigeria",
        "Norway",
        "Panama",
        "Paraguay",
        "Peru",
        "Poland",
        "Portugal",
        "Romania",
        "Russia",
        "Saudi Arabia",
        "Serbia",
        "South Africa",
        "South Korea",
        "Spain",
        "Sweden",
        "Switzerland",
        "Tanzania",
        "Turkey",
        "Uganda",
        "Ukraine",
        "United Arab Emirates",
        "United Kingdom",
        "Uruguay",
        "Zimbabwe"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.root.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAdapter = HomeItemAdapter(
            arrayListOf(),
            requireContext(),
            findNavController(),
            onSongOrVideoLongClickListener
        )
        moodsMomentAdapter = MoodsMomentAdapter(arrayListOf())
        genreAdapter = GenreAdapter(arrayListOf())
        trackChartAdapter = TrackChartAdapter(arrayListOf(), requireContext())
        artistChartAdapter = ArtistChartAdapter(arrayListOf(), requireContext())
        quickPicksAdapter = QuickPicksAdapter(arrayListOf(), requireContext(), findNavController())
        newReleaseAdapter = HomeItemAdapter(
            arrayListOf(),
            requireContext(),
            findNavController(),
            onSongOrVideoLongClickListener
        )
        initView()
        initObserver()
    }
    private fun initView() {
        val date = Calendar.getInstance().time
        val formatter = SimpleDateFormat("HH")
        when (formatter.format(date).toInt()) {
            in 6..12 -> {
                binding.topAppBar.subtitle = getString(R.string.good_morning)
            }

            in 13..17 -> {
                binding.topAppBar.subtitle = getString(R.string.good_afternoon)
            }

            in 18..23 -> {
                binding.topAppBar.subtitle = getString(R.string.good_evening)
            }

            else -> {
                binding.topAppBar.subtitle = getString(R.string.good_night)
            }
        }
        Log.d("Check", formatter.format(date))
        Log.d("Date", "onCreateView: $date")
        binding.rvHome.apply {
            adapter = mAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
        binding.rvMoodsMoment.apply {
            adapter = moodsMomentAdapter
            layoutManager = getGridLayoutHorizontal(3)
        }
        binding.rvGenre.apply {
            adapter = genreAdapter
            layoutManager = getGridLayoutHorizontal(3)
        }
        binding.rvTopTrack.apply {
            adapter = trackChartAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rvTopArtist.apply {
            adapter = artistChartAdapter
            layoutManager = getGridLayoutHorizontal(3)
        }
        binding.rvQuickPicks.apply {
            adapter = quickPicksAdapter
            layoutManager = getGridLayoutHorizontal(3)
        }
        binding.rvNewRelease.apply {
            adapter = newReleaseAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
        moodsMomentAdapter.setOnMoodsMomentClickListener(object :
            MoodsMomentAdapter.OnMoodsMomentItemClickListener {
            override fun onMoodsMomentItemClick(position: Int) {
                val args = Bundle()
                args.putString("params", moodsMomentAdapter.moodsMomentList[position].params)
                findNavController().navigateSafe(R.id.action_global_moodFragment, args)
            }
        })
        genreAdapter.setOnGenreClickListener(object : GenreAdapter.OnGenreItemClickListener {
            override fun onGenreItemClick(position: Int) {
                val args = Bundle()
                args.putString("params", genreAdapter.genreList[position].params)
                findNavController().navigateSafe(R.id.action_global_moodFragment, args)
            }
        })
        artistChartAdapter.setOnArtistClickListener(object :
            ArtistChartAdapter.OnArtistItemClickListener {
            override fun onArtistItemClick(position: Int) {
                val args = Bundle()
                args.putString("channelId", artistChartAdapter.listArtist[position].browseId)
                findNavController().navigateSafe(R.id.action_global_artistFragment, args)
            }
        })
        quickPicksAdapter.setOnClickListener(object : QuickPicksAdapter.OnClickListener {
            override fun onClick(position: Int) {
                val song = quickPicksAdapter.getData()[position]
                if (song.videoId != null) {
                    Queue.clear()
                    val firstQueue: Track = song.toTrack()
                    Queue.setNowPlaying(firstQueue)
                    val args = Bundle()
                    args.putString("videoId", song.videoId)
                    args.putString("from", "\"${song.title}\" Radio")
                    args.putString("type", Config.SONG_CLICK)
                    findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.this_song_is_not_available), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
        trackChartAdapter.setOnItemClickListener(object : TrackChartAdapter.SetOnItemClickListener {
            override fun onItemClick(position: Int) {
                trackChartAdapter.trackList.getOrNull(position)?.let { song ->
                    Queue.clear()
                    val firstQueue: Track = song.toTrack()
                    Queue.setNowPlaying(firstQueue)
                    val args = Bundle()
                    args.putString("videoId", song.videoId)
                    args.putString("from", "\"${song.title}\" ${getString(R.string.in_charts)}")
                    args.putString("type", Config.SONG_CLICK)
                    findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
                }
            }
        })
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchHomeData()
        }
        val listPopup = ListPopupWindow(
            requireContext(),
            null,
            com.google.android.material.R.attr.listPopupWindowStyle
        )
        listPopup.anchorView = binding.btRegionCode
        val codeAdapter = ArrayAdapter(requireContext(), R.layout.item_list_popup, itemsData)
        listPopup.setAdapter(codeAdapter)
        listPopup.setOnItemClickListener { _, _, position, _ ->
            binding.btRegionCode.text = itemsData[position]
            binding.chartResultLayout.visibility = View.GONE
            binding.chartLoadingLayout.visibility = View.VISIBLE
            viewModel.exploreChart(items[position])
            listPopup.dismiss()
        }
        binding.btRegionCode.setOnClickListener {
            listPopup.show()
        }
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home_fragment_menu_item_recently_played -> {
                    findNavController().navigateSafe(R.id.action_bottom_navigation_item_home_to_recentlySongsFragment)
                    true
                }

                R.id.home_fragment_menu_item_settings -> {
                    findNavController().navigateSafe(R.id.action_bottom_navigation_item_home_to_settingsFragment)
                    true
                }

                else -> false
            }
        }
    }
    private fun initObserver() {
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            if (!loading) {
                if (viewModel.regionCodeChart.value != null) {
                    for (i in 1..items.size) {
                        if (viewModel.regionCodeChart.value == items[i]) {
                            binding.btRegionCode.text = itemsData[i]
                            break
                        }
                    }
                    Log.d("Region Code", "onViewCreated: ${viewModel.regionCodeChart.value}")
                }
            } else {
                showLoading()
            }
        }

        viewModel.chart.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let { chart ->
                        trackChartAdapter.updateData(chart.videos.items)
                        artistChartAdapter.updateData(chart.artists.itemArtists)
                    }
                    binding.chartResultLayout.visibility = View.VISIBLE
                    binding.chartLoadingLayout.visibility = View.GONE
                }

                is Resource.Error -> {
                    binding.chartResultLayout.visibility = View.GONE
                    binding.chartLoadingLayout.visibility = View.GONE
                }
            }
        }
        viewModel.homeItemList.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let { homeItemList ->
                        val homeItemListWithoutQuickPicks = arrayListOf<HomeItem>()
                        val firstHomeItem = homeItemList.find { it.title == getString(R.string.quick_picks) }
                        if (firstHomeItem != null)
                        {
                            val temp = firstHomeItem.contents.filterNotNull()
                            quickPicksAdapter.updateData(temp)
                            for (i in 0 until homeItemList.size) {
                                homeItemListWithoutQuickPicks.add(homeItemList[i])
                            }
                            homeItemListWithoutQuickPicks.remove(firstHomeItem)
                        }
                        else {
                            binding.tvTitleQuickPicks.visibility = View.GONE
                            binding.tvStartWithARadio.visibility = View.GONE
                            homeItemListWithoutQuickPicks.addAll(homeItemList)
                        }
                        Log.d("Data", "onViewCreated: $homeItemList")
                        mAdapter.updateData(homeItemListWithoutQuickPicks)
                    }
                    showData()
                }

                is Resource.Error -> {
                    showData()
                }
            }
        }
        viewModel.exploreMoodItem.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let { exploreMoodItem ->
                        moodsMomentAdapter.updateData(exploreMoodItem.moodsMoments)
                        genreAdapter.updateData(exploreMoodItem.genres)
                        Log.d("Moods & Moment", "onViewCreated: ${exploreMoodItem.moodsMoments}")
                        Log.d("Genre", "onViewCreated: ${exploreMoodItem.moodsMoments}")
                    }
                }

                is Resource.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
        binding.accountLayout.visibility = View.GONE
        viewModel.accountInfo.observe(viewLifecycleOwner) {pair ->
            if (pair != null) {
                val accountName = pair.first
                val accountThumbUrl = pair.second
                if (accountName != null && accountThumbUrl != null && accountName != "" && accountThumbUrl != "") {
                    binding.accountLayout.visibility = View.VISIBLE
                    binding.tvAccountName.text = accountName
                    binding.ivAccount.load(accountThumbUrl)
                } else {
                    binding.accountLayout.visibility = View.GONE
                }
                if (viewModel.homeItemList.value?.data?.find { it.subtitle == accountName } != null) {
                    binding.accountLayout.visibility = View.GONE
                }
            } else {
                binding.accountLayout.visibility = View.GONE
            }
        }
        viewModel.newRelease.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let { newRelease ->
                        newReleaseAdapter.updateData(newRelease)
                    }
                }

                is Resource.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.showSnackBarErrorState.collect {
                    showSnackBarError(it)
                }
            }
        }
    }




    private fun showSnackBarError(message: String?) {
        if (message == null)
            return
        Snackbar.make(
            requireActivity().findViewById(R.id.mini_player_container),
            message,
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.retry)) {
            fetchHomeData()
        }
            .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
            .setDuration(5000)
            .show()

    }


    private fun showLoading() {
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.fullLayout.visibility = View.GONE

    }

    private fun showData() {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.fullLayout.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun getGridLayoutHorizontal(spanCount: Int) =
        GridLayoutManager(requireContext(), spanCount, GridLayoutManager.HORIZONTAL, false)

    private fun fetchHomeData() {
        viewModel.getHomeItemList()
    }


}