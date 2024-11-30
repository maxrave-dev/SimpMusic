package com.maxrave.simpmusic.ui.fragment.other

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import coil3.asDrawable
import coil3.imageLoader
import coil3.load
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.placeholder
import coil3.request.transformations
import coil3.size.Size
import coil3.transform.Transformation
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.album.TrackAdapter
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentAlbumBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.indexMap
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.setStatusBarsColor
import com.maxrave.simpmusic.extension.toAlbumEntity
import com.maxrave.simpmusic.extension.toArrayListTrack
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toListVideoId
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.AlbumViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.random.Random

@UnstableApi
class AlbumFragment : Fragment() {
    private val viewModel by activityViewModels<AlbumViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private var _binding: FragmentAlbumBinding? = null
    val binding get() = _binding!!

    private var gradientDrawable: GradientDrawable? = null
    private var toolbarBackground: Int? = null

    private lateinit var songsAdapter: TrackAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireArguments().clear()
        setStatusBarsColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark), requireActivity())
        _binding = null
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getLocation()
        lifecycleScope.launch {
            viewModel.liked.collect { liked ->
                binding.cbLove.isChecked = liked
            }
        }
        if (viewModel.gradientDrawable.value != null) {
            gradientDrawable = viewModel.gradientDrawable.value
            toolbarBackground = gradientDrawable?.colors?.get(0)
        }
        binding.rootLayout.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE
        // init Adapter
        songsAdapter = TrackAdapter(arrayListOf())
        // init RecyclerView
        binding.rvListSong.apply {
            adapter = songsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        var browseId = requireArguments().getString("browseId")
        val downloaded = arguments?.getInt("downloaded")
        if (browseId == null || browseId == viewModel.browseId.value) {
            browseId = viewModel.browseId.value
        }
        if (browseId != null) {
            Log.d("Check null", "onViewCreated: $downloaded")
            if (downloaded == null || downloaded == 0) {
                viewModel.updateBrowseId(browseId)
                fetchData(browseId)
            }
            if (downloaded == 1) {
                viewModel.updateBrowseId(browseId)
                fetchData(browseId, downloaded = 1)
            }
        }

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.btArtist.setOnClickListener {
            if (viewModel.albumBrowse.value?.data != null) {
                Log.d(
                    "TAG",
                    "Artist name clicked: ${viewModel.albumBrowse.value?.data?.artists?.get(0)?.id}",
                )
                val args = Bundle()
                args.putString(
                    "channelId",
                    viewModel.albumBrowse.value
                        ?.data
                        ?.artists
                        ?.get(0)
                        ?.id,
                )
                findNavController().navigateSafe(R.id.action_global_artistFragment, args)
            }
        }
        binding.cbLove.setOnCheckedChangeListener { cb, isChecked ->
            if (!isChecked) {
                viewModel.albumEntity.value?.let { album ->
                    viewModel.updateAlbumLiked(
                        false,
                        album.browseId,
                    )
                }
            } else {
                viewModel.albumEntity.value?.let { album ->
                    viewModel.updateAlbumLiked(
                        true,
                        album.browseId,
                    )
                }
            }
        }
        binding.btShuffle.setOnClickListener {
            if (viewModel.albumBrowse.value is Resource.Success && viewModel.albumBrowse.value?.data != null) {
                val index =
                    Random.nextInt(
                        viewModel.albumBrowse.value
                            ?.data!!
                            .tracks.size,
                    )
                val shuffleList: ArrayList<Track> = arrayListOf()
                shuffleList.addAll(
                    viewModel.albumBrowse.value
                        ?.data!!
                        .tracks,
                )
                val firstPlay = viewModel.albumBrowse.value
                    ?.data?.tracks?.getOrNull(index)
                shuffleList.removeAt(
                    index
                )
                shuffleList.shuffle()
                if (firstPlay != null) {
                    shuffleList.add(0, firstPlay)
                    viewModel.setQueueData(
                        QueueData(
                            listTracks = shuffleList,
                            firstPlayedTrack = firstPlay,
                            playlistId = viewModel.albumBrowse.value
                                ?.data
                                ?.audioPlaylistId
                                ?.replaceFirst("VL", "") ?: "",
                            playlistName = "Album \"${viewModel.albumBrowse.value?.data!!.title}\"",
                            playlistType = PlaylistType.PLAYLIST,
                            continuation = null,
                        )
                    )
                    viewModel.loadMediaItem(
                        firstPlay,
                        Config.ALBUM_CLICK,
                        0,
                    )
                }

            } else if (viewModel.albumEntity.value != null && viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                val index =
                    Random.nextInt(
                        viewModel.albumEntity.value
                            ?.tracks
                            ?.size!!,
                    )
                val shuffleList: ArrayList<Track> = arrayListOf()
                val firstPlay = viewModel.listTrack.value?.getOrNull(index)
                shuffleList.addAll(viewModel.listTrack.value.toArrayListTrack())
                shuffleList.removeAt(
                    index
                )
                shuffleList.shuffle()
                if (firstPlay != null) {
                    viewModel.setQueueData(
                        QueueData(
                            shuffleList,
                            firstPlay.toTrack(),
                            viewModel.albumEntity.value
                                ?.browseId
                                ?.replaceFirst("VL", "") ?: "",
                            "Album \"${viewModel.albumEntity.value?.title}\"",
                            PlaylistType.PLAYLIST,
                            null
                        )
                    )
                    viewModel.loadMediaItem(
                        firstPlay.toTrack(),
                        Config.ALBUM_CLICK,
                        0,
                    )
                }
            } else {
                Snackbar.make(requireView(), "Error", Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.btPlayPause.setOnClickListener {
            if (viewModel.albumBrowse.value is Resource.Success &&
                viewModel.albumBrowse.value?.data != null &&
                songsAdapter
                    .getList()
                    .isNotEmpty()
            ) {
                val firstPlay = songsAdapter.getList().firstOrNull()
                if (firstPlay != null) {
                    viewModel.setQueueData(
                        QueueData(
                            songsAdapter.getList(),
                            firstPlay,
                            viewModel.albumBrowse.value
                                ?.data
                                ?.audioPlaylistId
                                ?.replaceFirst("VL", "") ?: "",
                            "Album \"${viewModel.albumBrowse.value?.data!!.title}\"",
                            PlaylistType.PLAYLIST,
                            null
                        )
                    )
                    viewModel.loadMediaItem(
                        firstPlay,
                        Config.ALBUM_CLICK,
                        0,
                    )
                }
            } else if (viewModel.albumEntity.value != null && viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                val firstPlay = songsAdapter.getList().firstOrNull()
                if (firstPlay != null) {
                    viewModel.setQueueData(
                        QueueData(
                            songsAdapter.getList(),
                            firstPlay,
                            viewModel.albumEntity.value
                                ?.audioPlaylistId
                                ?.replaceFirst("VL", "") ?: "",
                            "Album \"${viewModel.albumEntity.value?.title}\"",
                            PlaylistType.PLAYLIST,
                            null
                        )
                    )
                    viewModel.loadMediaItem(
                        firstPlay,
                        Config.ALBUM_CLICK,
                        0,
                    )
                }
            } else {
                Snackbar.make(requireView(), "Error", Snackbar.LENGTH_SHORT).show()
            }
        }
        songsAdapter.setOnClickListener(
            object : TrackAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    if (viewModel.albumBrowse.value is Resource.Success && viewModel.albumBrowse.value?.data != null) {
                        val firstPlay = songsAdapter.getList().getOrNull(position)
                        if (firstPlay != null) {
                            viewModel.setQueueData(
                                QueueData(
                                    songsAdapter.getList(),
                                    firstPlay,
                                    viewModel.albumBrowse.value
                                        ?.data
                                        ?.audioPlaylistId
                                        ?.replaceFirst("VL", "") ?: "",
                                    "Album \"${viewModel.albumBrowse.value?.data!!.title}\"",
                                    PlaylistType.PLAYLIST,
                                    null
                                )
                            )
                            viewModel.loadMediaItem(
                                firstPlay,
                                Config.ALBUM_CLICK,
                                position,
                            )
                        }
                    } else if (viewModel.albumEntity.value != null && viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                        val firstPlay = songsAdapter.getList().getOrNull(position)
                        if (firstPlay != null) {
                            viewModel.setQueueData(
                                QueueData(
                                    songsAdapter.getList(),
                                    firstPlay,
                                    viewModel.albumEntity.value
                                        ?.audioPlaylistId
                                        ?.replaceFirst("VL", "") ?: "",
                                    "Album \"${viewModel.albumEntity.value?.title}\"",
                                    PlaylistType.PLAYLIST,
                                    null
                                )
                            )
                            viewModel.loadMediaItem(
                                firstPlay,
                                Config.ALBUM_CLICK,
                                position,
                            )
                        }
                    } else {
                        Snackbar.make(requireView(), "Error", Snackbar.LENGTH_SHORT).show()
                    }
                }
            },
        )
        songsAdapter.setOnOptionClickListener(
            object : TrackAdapter.OnOptionClickListener {
                override fun onOptionClick(position: Int) {
                    val song = songsAdapter.getList().get(position)
                    viewModel.getSongEntity(song.toSongEntity())
                    val dialog = BottomSheetDialog(requireContext())
                    val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                    with(bottomSheetView) {
                        btSleepTimer.visibility = View.GONE
                        viewModel.songEntity.observe(viewLifecycleOwner) { songEntity ->
                            if (songEntity != null) {
                                if (songEntity.liked) {
                                    tvFavorite.text = getString(R.string.liked)
                                    cbFavorite.isChecked = true
                                } else {
                                    tvFavorite.text = getString(R.string.like)
                                    cbFavorite.isChecked = false
                                }
                            }
                        }
                        btChangeLyricsProvider.visibility = View.GONE
                        tvSongTitle.text = song.title
                        tvSongTitle.isSelected = true
                        tvSongArtist.text = song.artists.toListName().connectArtists()
                        tvSongArtist.isSelected = true
                        if (song.album != null) {
                            setEnabledAll(btAlbum, true)
                            tvAlbum.text = song.album.name
                        } else {
                            tvAlbum.text = getString(R.string.no_album)
                            setEnabledAll(btAlbum, false)
                        }
                        btAlbum.setOnClickListener {
                            val albumId = song.album?.id
                            if (albumId != null) {
                                findNavController().navigateSafe(
                                    R.id.action_global_albumFragment,
                                    Bundle().apply {
                                        putString("browseId", albumId)
                                    },
                                )
                                dialog.dismiss()
                            } else {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        getString(R.string.no_album),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                        btAddQueue.setOnClickListener {
                            sharedViewModel.addToQueue(song)
                        }
                        btPlayNext.setOnClickListener {
                            sharedViewModel.playNext(song)
                        }
                        ivThumbnail.load(song.thumbnails?.lastOrNull()?.url)
                        btRadio.setOnClickListener {
                            val args = Bundle()
                            args.putString("radioId", "RDAMVM${song.videoId}")
                            args.putString(
                                "videoId",
                                song.videoId,
                            )
                            dialog.dismiss()
                            findNavController().navigateSafe(
                                R.id.action_global_playlistFragment,
                                args,
                            )
                        }
                        btLike.setOnClickListener {
                            if (cbFavorite.isChecked) {
                                cbFavorite.isChecked = false
                                tvFavorite.text = getString(R.string.like)
                                viewModel.updateLikeStatus(song.videoId, 0)
                            } else {
                                cbFavorite.isChecked = true
                                tvFavorite.text = getString(R.string.liked)
                                viewModel.updateLikeStatus(song.videoId, 1)
                            }
                        }
                        btSeeArtists.setOnClickListener {
                            val subDialog = BottomSheetDialog(requireContext())
                            val subBottomSheetView =
                                BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                            if (!song.artists.isNullOrEmpty()) {
                                val artistAdapter = SeeArtistOfNowPlayingAdapter(song.artists)
                                subBottomSheetView.rvArtists.apply {
                                    adapter = artistAdapter
                                    layoutManager = LinearLayoutManager(requireContext())
                                }
                                artistAdapter.setOnClickListener(
                                    object :
                                        SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                        override fun onItemClick(position: Int) {
                                            val artist = song.artists[position]
                                            if (artist.id != null) {
                                                findNavController().navigateSafe(
                                                    R.id.action_global_artistFragment,
                                                    Bundle().apply {
                                                        putString("channelId", artist.id)
                                                    },
                                                )
                                                subDialog.dismiss()
                                                dialog.dismiss()
                                            }
                                        }
                                    },
                                )
                            }

                            subDialog.setCancelable(true)
                            subDialog.setContentView(subBottomSheetView.root)
                            subDialog.show()
                        }
                        btDownload.visibility = View.GONE
                        btAddPlaylist.setOnClickListener {
                            viewModel.getLocalPlaylist()
                            val listLocalPlaylist: ArrayList<LocalPlaylistEntity> = arrayListOf()
                            val addPlaylistDialog = BottomSheetDialog(requireContext())
                            val viewAddPlaylist =
                                BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
                            val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
                            addToAPlaylistAdapter.setVideoId(song.videoId)
                            viewAddPlaylist.rvLocalPlaylists.apply {
                                adapter = addToAPlaylistAdapter
                                layoutManager = LinearLayoutManager(requireContext())
                            }
                            viewModel.listLocalPlaylist.observe(viewLifecycleOwner) { list ->
                                Log.d("Check Local Playlist", list.toString())
                                listLocalPlaylist.clear()
                                listLocalPlaylist.addAll(list)
                                addToAPlaylistAdapter.updateList(listLocalPlaylist)
                            }
                            addToAPlaylistAdapter.setOnItemClickListener(
                                object :
                                    AddToAPlaylistAdapter.OnItemClickListener {
                                    override fun onItemClick(position: Int) {
                                        val playlist = listLocalPlaylist[position]
                                        viewModel.updateInLibrary(song.videoId)
                                        val tempTrack = ArrayList<String>()
                                        if (playlist.tracks != null) {
                                            tempTrack.addAll(playlist.tracks)
                                        }
                                        if (!tempTrack.contains(
                                                song.videoId,
                                            ) &&
                                            playlist.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced &&
                                            playlist.youtubePlaylistId != null
                                        ) {
                                            viewModel.addToYouTubePlaylist(
                                                playlist.id,
                                                playlist.youtubePlaylistId,
                                                song.videoId,
                                            )
                                        }
                                        if (!tempTrack.contains(song.videoId)) {
                                            viewModel.insertPairSongLocalPlaylist(
                                                PairSongLocalPlaylist(
                                                    playlistId = playlist.id,
                                                    songId = song.videoId,
                                                    position = playlist.tracks?.size ?: 0,
                                                    inPlaylist = LocalDateTime.now(),
                                                ),
                                            )
                                            tempTrack.add(song.videoId)
                                        }

                                        viewModel.updateLocalPlaylistTracks(
                                            tempTrack.removeConflicts(),
                                            playlist.id,
                                        )
                                        addPlaylistDialog.dismiss()
                                        dialog.dismiss()
                                    }
                                },
                            )
                            addPlaylistDialog.setContentView(viewAddPlaylist.root)
                            addPlaylistDialog.setCancelable(true)
                            addPlaylistDialog.show()
                        }
                        btShare.setOnClickListener {
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "text/plain"
                            val url = "https://youtube.com/watch?v=${song.videoId}"
                            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                            val chooserIntent =
                                Intent.createChooser(shareIntent, getString(R.string.share_url))
                            startActivity(chooserIntent)
                        }
                        dialog.setCancelable(true)
                        dialog.setContentView(bottomSheetView.root)
                        dialog.show()
                    }
                }
            },
        )

        binding.btDownload.setOnClickListener {
            if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
                for (i in viewModel.albumBrowse.value
                    ?.data
                    ?.tracks!!) {
                    viewModel.insertSong(i.toSongEntity())
                }
                runBlocking {
                    delay(1000)
                    viewModel.listJob.emit(arrayListOf())
                }
                viewModel.getListTrackForDownload(
                    viewModel.albumBrowse.value
                        ?.data
                        ?.tracks
                        ?.toListVideoId(),
                )
            } else if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                Toast
                    .makeText(requireContext(), getString(R.string.downloaded), Toast.LENGTH_SHORT)
                    .show()
            } else if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADING) {
                Toast
                    .makeText(
                        requireContext(),
                        getString(R.string.downloading),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
        binding.topAppBarLayout.addOnOffsetChangedListener { it, verticalOffset ->
            if (abs(it.totalScrollRange) == abs(verticalOffset)) {
                binding.topAppBar.background = viewModel.gradientDrawable.value
                if (viewModel.gradientDrawable.value != null) {
                    if (viewModel.gradientDrawable.value?.colors != null) {
                        setStatusBarsColor(viewModel.gradientDrawable.value
                            ?.colors!!
                            .first(), requireActivity())

                    }
                }
            } else {
                binding.topAppBar.background = null
                setStatusBarsColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark),
                    requireActivity(),
                )
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val job1 =
                    launch {
                        sharedViewModel.downloadList.collect {
                            songsAdapter.setDownloadedList(it)
                        }
                    }
                val playingJob = launch {
                    combine(sharedViewModel.nowPlayingState.distinctUntilChangedBy {
                        it?.songEntity?.videoId
                    }, sharedViewModel.controllerState.distinctUntilChangedBy {
                        it.isPlaying
                    }) { nowPlaying, controllerState ->
                        Pair(nowPlaying, controllerState)
                    }.collect {
                        val songEntity = it.first?.songEntity
                        if (songEntity != null && it.second.isPlaying) {
                            songsAdapter.setNowPlaying(songEntity.videoId)
                        } else {
                            songsAdapter.setNowPlaying(null)
                        }
                    }
                }
                val job2 =
                    launch {
                        combine(
                            sharedViewModel.nowPlayingState.distinctUntilChangedBy {
                                it?.songEntity?.videoId
                            },
                            sharedViewModel.controllerState,
                        ) { nowPlaying, isPlaying ->
                            Pair(nowPlaying, isPlaying)
                        }.collect {
                            if (it.first != null && it.second.isPlaying) {
                                songsAdapter.setNowPlaying(it.first?.songEntity?.videoId)
                            } else {
                                songsAdapter.setNowPlaying(null)
                            }
                        }
                    }
                val job3 =
                    launch {
                        viewModel.albumDownloadState.collectLatest { albumDownloadState ->
                            when (albumDownloadState) {
                                DownloadState.STATE_PREPARING -> {
                                    binding.btDownload.visibility = View.GONE
                                    binding.animationDownloading.visibility = View.VISIBLE
                                }

                                DownloadState.STATE_DOWNLOADING -> {
                                    binding.btDownload.visibility = View.GONE
                                    binding.animationDownloading.visibility = View.VISIBLE
                                }

                                DownloadState.STATE_DOWNLOADED -> {
                                    binding.btDownload.visibility = View.VISIBLE
                                    binding.animationDownloading.visibility = View.GONE
                                    binding.btDownload.setImageResource(R.drawable.baseline_downloaded)
                                }

                                DownloadState.STATE_NOT_DOWNLOADED -> {
                                    binding.btDownload.visibility = View.VISIBLE
                                    binding.animationDownloading.visibility = View.GONE
                                    binding.btDownload.setImageResource(R.drawable.download_button)
                                }
                            }
                        }
                    }
                val loadingJob =
                    launch {
                        viewModel.loading.collectLatest { loading ->
                            if (loading) {
                                binding.rootLayout.visibility = View.GONE
                                binding.loadingLayout.visibility = View.VISIBLE
                            } else {
                                binding.rootLayout.visibility = View.VISIBLE
                                binding.loadingLayout.visibility = View.GONE
                            }
                        }
                    }
                val browseIdJob =
                    launch {
                        viewModel.browseId.collectLatest { browseId ->
                            if (browseId != null) {
                                fetchData(browseId, downloaded ?: 0)
                            }
                        }
                    }
                val albumBrowseJob =
                    launch {
                        viewModel.albumBrowse.collect { response ->
                            if (response != null) {
                                when (response) {
                                    is Resource.Success -> {
                                        response.data.let {
                                            if (it != null) {
                                                (viewModel.browseId.value ?: browseId)?.let { id ->
                                                    viewModel.insertAlbum(it.toAlbumEntity(id))
                                                    it.thumbnails?.lastOrNull()?.url?.let { url ->
                                                        loadImage(
                                                            url,
                                                            id,
                                                        )
                                                    }
                                                }
                                                with(binding) {
                                                    topAppBar.title = it.title
                                                    btArtist.text = it.artists[0].name
                                                    tvYearAndCategory.text =
                                                        context?.getString(
                                                            R.string.year_and_category,
                                                            it.year,
                                                            it.type,
                                                        )
                                                    tvTrackCountAndDuration.text =
                                                        context?.getString(
                                                            R.string.album_length,
                                                            it.trackCount.toString(),
                                                            it.duration,
                                                        )
                                                    if (it.description == null || it.description == "") {
                                                        tvDescription.originalText =
                                                            getString(R.string.no_description)
                                                    } else {
                                                        tvDescription.originalText =
                                                            it.description.toString()
                                                    }

                                                    val tempList = arrayListOf<Any>()
                                                    for (i in it.tracks) {
                                                        tempList.add(i)
                                                    }
                                                    songsAdapter.updateList(tempList)
                                                    binding.rootLayout.visibility = View.VISIBLE
                                                    binding.loadingLayout.visibility = View.GONE
                                                }
                                            }
                                        }
                                    }

                                    is Resource.Error -> {
                                        Snackbar
                                            .make(
                                                binding.root,
                                                response.message.toString(),
                                                Snackbar.LENGTH_LONG,
                                            ).show()
                                        findNavController().popBackStack()
                                    }
                                }
                            }
                        }
                    }
                val albumEntity =
                    launch {
                        viewModel.albumEntity.collect { albumEntity ->
                            if (albumEntity != null) {
                                with(binding) {
                                    when (albumEntity.downloadState) {
                                        DownloadState.STATE_DOWNLOADED -> {
                                            btDownload.visibility = View.VISIBLE
                                            animationDownloading.visibility = View.GONE
                                            btDownload.setImageResource(R.drawable.baseline_downloaded)
                                        }

                                        DownloadState.STATE_DOWNLOADING -> {
                                            btDownload.visibility = View.GONE
                                            animationDownloading.visibility = View.VISIBLE
                                        }

                                        DownloadState.STATE_NOT_DOWNLOADED -> {
                                            btDownload.visibility = View.VISIBLE
                                            animationDownloading.visibility = View.GONE
                                            btDownload.setImageResource(R.drawable.download_button)
                                        }
                                    }
                                    binding.cbLove.isChecked = albumEntity.liked
                                }
                                if (viewModel.albumBrowse.value?.data == null) {
                                    with(binding) {
                                        topAppBar.title = albumEntity.title
                                        btArtist.text = albumEntity.artistName?.get(0) ?: "Unknown"
                                        tvYearAndCategory.text =
                                            context?.getString(
                                                R.string.year_and_category,
                                                albumEntity.year,
                                                albumEntity.type,
                                            )
                                        tvTrackCountAndDuration.text =
                                            context?.getString(
                                                R.string.album_length,
                                                albumEntity.trackCount.toString(),
                                                albumEntity.duration,
                                            )
                                        if (albumEntity.description == "") {
                                            tvDescription.originalText =
                                                getString(R.string.no_description)
                                        } else {
                                            tvDescription.originalText = albumEntity.description
                                        }
                                        loadImage(albumEntity.thumbnails!!, albumEntity.browseId)
                                        viewModel.getListTrack(albumEntity.tracks)
                                    }
                                }
                            }
                        }
                    }
                val listTrackJob =
                    launch {
                        viewModel.listTrack.collect { listTrack ->
                            if (listTrack != null) {
                                val tempList = arrayListOf<Any>()
                                for (i in listTrack) {
                                    tempList.add(i)
                                }
                                if (viewModel.albumEntity.value != null) {
                                    tempList.sortBy {
                                        (
                                            viewModel.albumEntity.value
                                                ?.tracks
                                                ?.indexMap()
                                        )?.get(
                                            (it as SongEntity).videoId,
                                        )
                                    }
                                }
                                songsAdapter.updateList(tempList)
                            }
                        }
                    }
                val downloadJob =
                    launch {
                        viewModel.listTrackForDownload.collect { listTrack ->
                            if (!listTrack.isNullOrEmpty()) {
                                val listJob: ArrayList<SongEntity> = arrayListOf()
                                for (song in listTrack) {
                                    if (song.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
                                        listJob.add(song)
                                    }
                                }
                                viewModel.listJob.value = listJob
                                Log.d("AlbumFragment", "ListJob: ${viewModel.listJob.value}")
                                viewModel.updatePlaylistDownloadState(
                                    browseId!!,
                                    DownloadState.STATE_DOWNLOADING,
                                )
                                listJob.forEach { job ->
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(job.videoId, job.videoId.toUri())
                                            .setData(job.title.toByteArray())
                                            .setCustomCacheKey(job.videoId)
                                            .build()
                                    viewModel.updateDownloadState(
                                        job.videoId,
                                        DownloadState.STATE_DOWNLOADING,
                                    )
                                    DownloadService.sendAddDownload(
                                        requireContext(),
                                        MusicDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                    viewModel.getDownloadStateFromService(job.videoId)
                                }
                                viewModel.downloadFullAlbumState(browseId)
                            }
                        }
                    }
                job1.join()
                job2.join()
                job3.join()
                loadingJob.join()
                browseIdJob.join()
                albumBrowseJob.join()
                albumEntity.join()
                listTrackJob.join()
                downloadJob.join()
                playingJob.join()
//                launch {
//                    viewModel.listJob.collectLatest {jobs->
//                        Log.d("AlbumFragment", "ListJob: $jobs")
//                        if (jobs.isNotEmpty()){
//                            var count = 0
//                            jobs.forEach { job ->
//                                if (job.downloadState == DownloadState.STATE_DOWNLOADED) {
//                                    count++
//                                }
//                            }
//                            Log.d("AlbumFragment", "Count: $count")
//                            if (count == jobs.size) {
//                                viewModel.updatePlaylistDownloadState(
//                                    browseId!!,
//                                    DownloadState.STATE_DOWNLOADED
//                                )
//                                Toast.makeText(
//                                    requireContext(),
//                                    getString(R.string.downloaded),
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
//                        }
//                    }
//                }
            }
            // job2.join()
        }
    }
//    private fun fetchDataFromViewModel() {
//            val response = viewModel.albumBrowse.value
//            when (response){
//                is Resource.Success -> {
//                    response.data.let {
//                        with(binding){
//                            topAppBar.title = it?.title
//                            btArtist.text = it?.artists?.get(0)?.name
//                            tvYearAndCategory.text= context?.getString(R.string.year_and_category, it?.year, it?.type)
//                            tvTrackCountAndDuration.text = context?.getString(R.string.album_length, it?.trackCount.toString(), it?.duration)
//                            if (it?.description == null || it.description == ""){
//                                tvDescription.originalText = "No description"
//                            }
//                            else {
//                                tvDescription.originalText = it.description.toString()
//                            }
//                            when (it?.thumbnails?.size!!){
//                                1 -> loadImage(it.thumbnails[0].url, viewModel.browseId.value!!)
//                                2 -> loadImage(it.thumbnails[1].url, viewModel.browseId.value!!)
//                                3 -> loadImage(it.thumbnails[2].url, viewModel.browseId.value!!)
//                                4 -> loadImage(it.thumbnails[3].url, viewModel.browseId.value!!)
//                                else -> {}
//                            }
//                            val tempList = arrayListOf<Any>()
//                            for (i in it.tracks){
//                                tempList.add(i)
//                            }
//                            songsAdapter.updateList(tempList)
//                            binding.rootLayout.visibility = View.VISIBLE
//                            binding.loadingLayout.visibility = View.GONE
//                            val albumEntity = viewModel.albumEntity.value
//                            if (albumEntity != null) {
//                                viewModel.checkAllSongDownloaded(it.tracks as ArrayList<Track>)
//                                viewModel.albumEntity.observe(viewLifecycleOwner){albumEntity2 ->
//                                    if (albumEntity2 != null) {
//                                        when (albumEntity2.downloadState) {
//                                            DownloadState.STATE_DOWNLOADED -> {
//                                                btDownload.visibility = View.VISIBLE
//                                                animationDownloading.visibility = View.GONE
//                                                btDownload.setImageResource(R.drawable.baseline_downloaded)
//                                            }
//
//                                            DownloadState.STATE_DOWNLOADING -> {
//                                                btDownload.visibility = View.GONE
//                                                animationDownloading.visibility = View.VISIBLE
//                                            }
//
//                                            DownloadState.STATE_NOT_DOWNLOADED -> {
//                                                btDownload.visibility = View.VISIBLE
//                                                animationDownloading.visibility = View.GONE
//                                                btDownload.setImageResource(R.drawable.download_button)
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//                is Resource.Error -> {
//                    Snackbar.make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG).show()
//                    findNavController().popBackStack()
//                }
//
//                else -> {}
//        }
//    }

    private fun fetchData(
        browseId: String,
        downloaded: Int = 0,
    ) {
        viewModel.clearAlbumBrowse()
        if (downloaded == 0) {
            viewModel.browseAlbum(browseId)
        } else if (downloaded == 1) {
            viewModel.getAlbum(browseId)
        }

//        if (downloaded == 0) {
//            viewModel.browseAlbum(browseId)
//            viewModel.albumBrowse.observe(viewLifecycleOwner) { response ->
//                when (response){
//                    is Resource.Success -> {
//                        response.data.let {
//                            if (it != null){
//                                viewModel.insertAlbum(it.toAlbumEntity(browseId))
//                                with(binding){
//                                    topAppBar.title = it.title
//                                    btArtist.text = it.artists[0].name
//                                    tvYearAndCategory.text= context?.getString(R.string.year_and_category, it.year, it.type)
//                                    tvTrackCountAndDuration.text = context?.getString(R.string.album_length, it.trackCount.toString(), it.duration)
//                                    if (it.description == null || it.description == ""){
//                                        tvDescription.originalText = getString(R.string.no_description)
//                                    }
//                                    else {
//                                        tvDescription.originalText = it.description.toString()
//                                    }
//                                    when (it.thumbnails?.size!!){
//                                        1 -> loadImage(it.thumbnails[0].url, browseId)
//                                        2 -> loadImage(it.thumbnails[1].url, browseId)
//                                        3 -> loadImage(it.thumbnails[2].url, browseId)
//                                        4 -> loadImage(it.thumbnails[3].url, browseId)
//                                        else -> {}
//                                    }
//                                    val tempList = arrayListOf<Any>()
//                                    for (i in it.tracks){
//                                        tempList.add(i)
//                                    }
//                                    songsAdapter.updateList(tempList)
//                                    binding.rootLayout.visibility = View.VISIBLE
//                                    binding.loadingLayout.visibility = View.GONE
//                                    viewModel.albumEntity.observe(viewLifecycleOwner) {albumEntity ->
//                                        if (albumEntity != null) {
//                                            when (albumEntity.downloadState) {
//                                                DownloadState.STATE_DOWNLOADED -> {
//                                                    btDownload.visibility = View.VISIBLE
//                                                    animationDownloading.visibility = View.GONE
//                                                    btDownload.setImageResource(R.drawable.baseline_downloaded)
//                                                }
//                                                DownloadState.STATE_DOWNLOADING -> {
//                                                    btDownload.visibility = View.GONE
//                                                    animationDownloading.visibility = View.VISIBLE
//                                                }
//                                                DownloadState.STATE_NOT_DOWNLOADED -> {
//                                                    btDownload.visibility = View.VISIBLE
//                                                    animationDownloading.visibility = View.GONE
//                                                    btDownload.setImageResource(R.drawable.download_button)
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    is Resource.Error -> {
//                        Snackbar.make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG).show()
//                        findNavController().popBackStack()
//                    }
//
//                    else -> {}
//                }
//            }
//        }
//        else if (downloaded == 1){
//            viewModel.getAlbum(browseId)
//            with(binding){
//                viewModel.albumEntity.observe(viewLifecycleOwner) {albumEntity ->
//                    if (albumEntity != null) {
//                        when (albumEntity.downloadState) {
//                            DownloadState.STATE_DOWNLOADED -> {
//                                btDownload.visibility = View.VISIBLE
//                                animationDownloading.visibility = View.GONE
//                                btDownload.setImageResource(R.drawable.baseline_downloaded)
//                            }
//                            DownloadState.STATE_DOWNLOADING -> {
//                                btDownload.visibility = View.GONE
//                                animationDownloading.visibility = View.VISIBLE
//                            }
//                            DownloadState.STATE_NOT_DOWNLOADED -> {
//                                btDownload.visibility = View.VISIBLE
//                                animationDownloading.visibility = View.GONE
//                                btDownload.setImageResource(R.drawable.download_button)
//                            }
//                        }
//                    }
//                    if (albumEntity != null) {
//                        topAppBar.title = albumEntity.title
//                        btArtist.text = albumEntity.artistName?.get(0) ?: "Unknown"
//                        tvYearAndCategory.text= context?.getString(R.string.year_and_category, albumEntity.year, albumEntity.type)
//                        tvTrackCountAndDuration.text = context?.getString(R.string.album_length, albumEntity.trackCount.toString(), albumEntity.duration)
//                        if (albumEntity.description == ""){
//                            tvDescription.originalText = getString(R.string.no_description)
//                        }
//                        else {
//                            tvDescription.originalText = albumEntity.description.toString()
//                        }
//                        loadImage(albumEntity.thumbnails!!, browseId)
//                        viewModel.getListTrack(albumEntity.tracks)
//                    }
//                    viewModel.listTrack.observe(viewLifecycleOwner) { listTrack ->
//                        val tempList = arrayListOf<Any>()
//                        for (i in listTrack){
//                            tempList.add(i)
//                        }
//                        if (albumEntity != null) {
//                            tempList.sortBy { (albumEntity.tracks?.indexMap())?.get((it as SongEntity).videoId) }
//                        }
//                        songsAdapter.updateList(tempList)
//                    }
//                    binding.rootLayout.visibility = View.VISIBLE
//                    binding.loadingLayout.visibility = View.GONE
//                }
//            }
//        }
    }

    private fun loadImage(
        url: String,
        albumId: String,
    ) {
        val request =
            ImageRequest
                .Builder(requireContext())
                .placeholder(R.drawable.holder)
                .data(Uri.parse(url))
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(url)
                .target(
                    onStart = {
                    },
                    onSuccess = { result ->
                        binding.ivAlbumArt.setImageDrawable(result.asDrawable(requireContext().resources))
                        if (viewModel.gradientDrawable.value != null) {
                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                                binding.fullRootLayout.background =
                                    it.apply {
                                        setDither(true)
                                    }
                                toolbarBackground = it.colors?.get(0)
                                Log.d("TAG", "fetchData: $toolbarBackground")
                                // binding.topAppBar.background = ColorDrawable(toolbarBackground!!)
                                binding.topAppBarLayout.background =
                                    ColorDrawable(toolbarBackground!!)
                            }
                        }
                    },
                    onError = {
                        binding.ivAlbumArt.load(R.drawable.holder)
                    },
                ).transformations(
                    object : Transformation() {
                        override val cacheKey: String
                            get() = url

                        override suspend fun transform(
                            input: Bitmap,
                            size: Size,
                        ): Bitmap {
                            val p = Palette.from(input).generate()
                            val defaultColor = 0x000000
                            var startColor = p.getDarkVibrantColor(defaultColor)
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
                            startColor = ColorUtils.setAlphaComponent(startColor, 150)
                            val endColor =
                                resources.getColor(R.color.md_theme_dark_background, null)
                            val gd =
                                GradientDrawable(
                                    GradientDrawable.Orientation.TOP_BOTTOM,
                                    intArrayOf(startColor, endColor),
                                )
                            gd.cornerRadius = 0f
                            gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                            gd.gradientRadius = 0.5f
                            viewModel.gradientDrawable.postValue(gd)
                            return input
                        }
                    },
                ).build()
        requireContext().imageLoader.enqueue(request)
//        binding.ivAlbumArt.load(url) {
//            diskCachePolicy(CachePolicy.ENABLED)
//                .diskCacheKey(albumId)
//            transformations(object : Transformation{
//                override val cacheKey: String
//                    get() = albumId
//
//                override suspend fun transform(input: Bitmap, size: Size): Bitmap {
//                    val p = Palette.from(input).generate()
//                    val defaultColor = 0x000000
//                    var startColor = p.getDarkVibrantColor(defaultColor)
//                    if (startColor == defaultColor){
//                        startColor = p.getDarkMutedColor(defaultColor)
//                        if (startColor == defaultColor){
//                            startColor = p.getVibrantColor(defaultColor)
//                            if (startColor == defaultColor){
//                                startColor = p.getMutedColor(defaultColor)
//                                if (startColor == defaultColor){
//                                    startColor = p.getLightVibrantColor(defaultColor)
//                                    if (startColor == defaultColor){
//                                        startColor = p.getLightMutedColor(defaultColor)
//                                    }
//                                }
//                            }
//                        }
//                        Log.d("Check Start Color", "transform: $startColor")
//                    }
//                    startColor = ColorUtils.setAlphaComponent(startColor, 150)
//                    val endColor = resources.getColor(R.color.md_theme_dark_background, null)
//                    val gd = GradientDrawable(
//                        GradientDrawable.Orientation.TOP_BOTTOM,
//                        intArrayOf(startColor, endColor)
//                    )
//                    gd.cornerRadius = 0f
//                    gd.gradientType = GradientDrawable.LINEAR_GRADIENT
//                    gd.gradientRadius = 0.5f
//                    viewModel.gradientDrawable.postValue(gd)
//                    return input
//                }
//
//            })
//        }
    }
}