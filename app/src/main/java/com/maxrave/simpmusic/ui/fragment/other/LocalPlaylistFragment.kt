package com.maxrave.simpmusic.ui.fragment.other

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.findNavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.extension.setStatusBarsColor
import com.maxrave.simpmusic.ui.screen.library.PlaylistScreen
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel

@UnstableApi
@ExperimentalFoundationApi
class LocalPlaylistFragment : Fragment() {
    private val viewModel by activityViewModels<LocalPlaylistViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private var playlistId: Long? = null
    private lateinit var composeView: ComposeView

    @UnstableApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).also {
            composeView = it
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @UnstableApi
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = arguments?.getLong("id")
        composeView.apply {
            setContent {
                AppTheme {
                    Scaffold {
                        val id = playlistId
                        if (id != null) {
                            PlaylistScreen(
                                id = id,
                                sharedViewModel = sharedViewModel,
                                viewModel = viewModel,
                                findNavController(),
                            )
                        } else {
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }

//        binding.loadingLayout.visibility = View.VISIBLE
//        binding.rootLayout.visibility = View.GONE
//
//        listTrack = arrayListOf()
//        playlistAdapter = PlaylistItemAdapter(arrayListOf())
//
//        listSuggestTrack = arrayListOf()
//        suggestAdapter = SuggestItemAdapter(arrayListOf())
//
//        binding.rvListSong.apply {
//            adapter = playlistAdapter
//            layoutManager =
//                if (!viewModel.reverseLayout) {
//                    LinearLayoutManager(
//                        requireContext(),
//                        LinearLayoutManager.VERTICAL,
//                        false,
//                    )
//                } else {
//                    LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
//                }
//        }
//
//        binding.rvSuggest.apply {
//            adapter = suggestAdapter
//            layoutManager = LinearLayoutManager(requireContext())
//        }
//
//        if (id == null) {
//            id = viewModel.id.value
//            fetchDataFromDatabase()
//            binding.loadingLayout.visibility = View.GONE
//            binding.rootLayout.visibility = View.VISIBLE
//        } else {
//            fetchDataFromDatabase()
//            binding.loadingLayout.visibility = View.GONE
//            binding.rootLayout.visibility = View.VISIBLE
//        }
//        if (viewModel.listSuggestions.value.isNullOrEmpty()) {
//            binding.suggestLayout.visibility = View.GONE
//        } else {
//            binding.suggestLayout.visibility = View.VISIBLE
//        }
//        binding.btSort.setOnClickListener {
//            if (viewModel.filter.value == FilterState.OlderFirst) {
//                binding.btSort.setIconResource(R.drawable.baseline_arrow_drop_up_24)
//                viewModel.setFilter(FilterState.NewerFirst)
//            } else {
//                binding.btSort.setIconResource(R.drawable.baseline_arrow_drop_down_24)
//                viewModel.setFilter(FilterState.OlderFirst)
//            }
//        }
//        playlistAdapter.setOnClickListener(
//            object : PlaylistItemAdapter.OnItemClickListener {
//                override fun onItemClick(position: Int) {
//                    val listTrack = playlistAdapter.getListTrack()
//                    val args = Bundle()
//                    args.putString("type", Config.PLAYLIST_CLICK)
//                    args.putString("videoId", (listTrack[position] as SongEntity).videoId)
//                    args.putString("from", "Playlist \"${(viewModel.localPlaylist.value)?.title}\"")
//                    args.putInt("index", position)
//                    if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
//                        args.putInt("downloaded", 1)
//                    }
//                    Queue.initPlaylist(
//                        Queue.LOCAL_PLAYLIST_ID + viewModel.localPlaylist.value?.id,
//                        "Playlist \"${(viewModel.localPlaylist.value)?.title}\"",
//                        Queue.PlaylistType.PLAYLIST,
//                    )
//                    Queue.setNowPlaying((listTrack[position] as SongEntity).toTrack())
//                    val tempList: ArrayList<Track> = arrayListOf()
//                    for (i in listTrack) {
//                        tempList.add((i as SongEntity).toTrack())
//                    }
//                    Queue.addAll(tempList)
//                    if (Queue.getQueue().size >= 1) {
//                        Queue.removeTrackWithIndex(position)
//                    }
//                    findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
//                }
//            },
//        )
//        playlistAdapter.setOnOptionClickListener(
//            object :
//                PlaylistItemAdapter.OnOptionClickListener {
//                override fun onOptionClick(position: Int) {
//                    val song = playlistAdapter.getListTrack().getOrNull(position) as SongEntity
//                    val dialog = BottomSheetDialog(requireContext())
//                    val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
//                    with(bottomSheetView) {
//                        btSleepTimer.visibility = View.GONE
//                        btDelete.visibility = View.VISIBLE
//                        if (song.liked) {
//                            tvFavorite.text = getString(R.string.liked)
//                            cbFavorite.isChecked = true
//                        } else {
//                            tvFavorite.text = getString(R.string.like)
//                            cbFavorite.isChecked = false
//                        }
//                        btAddQueue.setOnClickListener {
//                            sharedViewModel.addToQueue(song.toTrack())
//                        }
//                        btPlayNext.setOnClickListener {
//                            sharedViewModel.playNext(song.toTrack())
//                        }
//                        setEnabledAll(btAlbum, true)
//                        tvAlbum.text = song.albumName
//                        btAlbum.setOnClickListener {
//                            val albumId = song.albumId
//                            findNavController().navigateSafe(
//                                R.id.action_global_albumFragment,
//                                Bundle().apply {
//                                    putString("browseId", albumId)
//                                },
//                            )
//                            dialog.dismiss()
//                        }
//                        btChangeLyricsProvider.visibility = View.GONE
//                        tvSongTitle.text = song.title
//                        tvSongTitle.isSelected = true
//                        tvSongArtist.text = song.artistName?.connectArtists()
//                        tvSongArtist.isSelected = true
//                        ivThumbnail.load(song.thumbnails)
//                        btRadio.setOnClickListener {
//                            val args = Bundle()
//                            args.putString("radioId", "RDAMVM${song.videoId}")
//                            args.putString(
//                                "videoId",
//                                song.videoId,
//                            )
//                            dialog.dismiss()
//                            findNavController().navigateSafe(
//                                R.id.action_global_playlistFragment,
//                                args,
//                            )
//                        }
//                        btLike.setOnClickListener {
//                            if (cbFavorite.isChecked) {
//                                cbFavorite.isChecked = false
//                                tvFavorite.text = getString(R.string.like)
//                                viewModel.updateLikeStatus(song.videoId, 0)
//                                playlistAdapter.setLikedTrack(position, false)
//                            } else {
//                                cbFavorite.isChecked = true
//                                tvFavorite.text = getString(R.string.liked)
//                                viewModel.updateLikeStatus(song.videoId, 1)
//                                playlistAdapter.setLikedTrack(position, true)
//                            }
//                            lifecycleScope.launch {
//                                if (sharedViewModel.simpleMediaServiceHandler?.nowPlaying?.first()?.mediaId == song.videoId) {
//                                    delay(500)
//                                    sharedViewModel.refreshSongDB()
//                                }
//                            }
//                        }
//                        btSeeArtists.setOnClickListener {
//                            val subDialog = BottomSheetDialog(requireContext())
//                            val subBottomSheetView =
//                                BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
//                            if (!song.artistName.isNullOrEmpty()) {
//                                val artistAdapter =
//                                    SeeArtistOfNowPlayingAdapter(
//                                        arrayListOf<Artist>().apply {
//                                            List(song.artistName.size) { i ->
//                                                add(
//                                                    Artist(
//                                                        song.artistId?.getOrNull(i),
//                                                        song.artistName.get(i),
//                                                    ),
//                                                )
//                                            }
//                                        },
//                                    )
//                                subBottomSheetView.rvArtists.apply {
//                                    adapter = artistAdapter
//                                    layoutManager = LinearLayoutManager(requireContext())
//                                }
//                                artistAdapter.setOnClickListener(
//                                    object :
//                                        SeeArtistOfNowPlayingAdapter.OnItemClickListener {
//                                        override fun onItemClick(position: Int) {
//                                            val artist = song.artistId?.getOrNull(position)
//                                            if (artist != null) {
//                                                findNavController().navigateSafe(
//                                                    R.id.action_global_artistFragment,
//                                                    Bundle().apply {
//                                                        putString("channelId", artist)
//                                                    },
//                                                )
//                                                subDialog.dismiss()
//                                                dialog.dismiss()
//                                            }
//                                        }
//                                    },
//                                )
//                            }
//                            subDialog.setCancelable(true)
//                            subDialog.setContentView(subBottomSheetView.root)
//                            subDialog.show()
//                        }
//                        btDownload.visibility = View.GONE
//                        btAddPlaylist.setOnClickListener {
//                            viewModel.getAllLocalPlaylist()
//                            val listLocalPlaylist: ArrayList<LocalPlaylistEntity> = arrayListOf()
//                            val addPlaylistDialog = BottomSheetDialog(requireContext())
//                            val viewAddPlaylist =
//                                BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
//                            val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
//                            addToAPlaylistAdapter.setVideoId(song.videoId)
//                            viewAddPlaylist.rvLocalPlaylists.apply {
//                                adapter = addToAPlaylistAdapter
//                                layoutManager = LinearLayoutManager(requireContext())
//                            }
//                            viewModel.listAllLocalPlaylist.observe(viewLifecycleOwner) { list ->
//                                Log.d("Check Local Playlist", list.toString())
//                                listLocalPlaylist.clear()
//                                listLocalPlaylist.addAll(list)
//                                addToAPlaylistAdapter.updateList(listLocalPlaylist)
//                            }
//                            addToAPlaylistAdapter.setOnItemClickListener(
//                                object :
//                                    AddToAPlaylistAdapter.OnItemClickListener {
//                                    override fun onItemClick(position: Int) {
//                                        val playlist = listLocalPlaylist[position]
//                                        viewModel.updateInLibrary(song.videoId)
//                                        val tempTrack = ArrayList<String>()
//                                        if (playlist.tracks != null) {
//                                            tempTrack.addAll(playlist.tracks)
//                                        }
//                                        if (!tempTrack.contains(
//                                                song.videoId,
//                                            ) && playlist.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced && playlist.youtubePlaylistId != null
//                                        ) {
//                                            viewModel.addToYouTubePlaylist(
//                                                playlist.id,
//                                                playlist.youtubePlaylistId,
//                                                song.videoId,
//                                            )
//                                        }
//                                        if (!tempTrack.contains(song.videoId)) {
//                                            viewModel.insertPairSongLocalPlaylist(
//                                                PairSongLocalPlaylist(
//                                                    playlistId = playlist.id,
//                                                    songId = song.videoId,
//                                                    position = playlist.tracks?.size ?: 0,
//                                                    inPlaylist = LocalDateTime.now(),
//                                                ),
//                                            )
//                                            tempTrack.add(song.videoId)
//                                        }
//
//                                        viewModel.updateLocalPlaylistTracks(
//                                            tempTrack.removeConflicts(),
//                                            playlist.id,
//                                        )
//                                        addPlaylistDialog.dismiss()
//                                        dialog.dismiss()
//                                    }
//                                },
//                            )
//                            addPlaylistDialog.setContentView(viewAddPlaylist.root)
//                            addPlaylistDialog.setCancelable(true)
//                            addPlaylistDialog.show()
//                        }
//                        btShare.setOnClickListener {
//                            val shareIntent = Intent(Intent.ACTION_SEND)
//                            shareIntent.type = "text/plain"
//                            val url = "https://youtube.com/watch?v=${song.videoId}"
//                            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
//                            val chooserIntent =
//                                Intent.createChooser(shareIntent, getString(R.string.share_url))
//                            startActivity(chooserIntent)
//                        }
//                        btDelete.setOnClickListener {
//                            val temp =
//                                playlistAdapter.getListTrack().getOrNull(position) as SongEntity
//                            viewModel.deleteItem(temp, id!!)
//                            if (viewModel.localPlaylist.value?.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced && viewModel.localPlaylist.value?.youtubePlaylistId != null) {
//                                val videoId = viewModel.listTrack.value?.get(position)?.videoId
//                                viewModel.removeYouTubePlaylistItem(
//                                    viewModel.localPlaylist.value?.youtubePlaylistId!!,
//                                    videoId!!,
//                                )
//                                dialog.dismiss()
//                            }
//                        }
//                        dialog.setCancelable(true)
//                        dialog.setContentView(bottomSheetView.root)
//                        dialog.show()
//                    }
//                }
//            },
//        )
//        suggestAdapter.setOnItemClickListener(
//            object : SuggestItemAdapter.OnItemClickListener {
//                override fun onItemClick(position: Int) {
//                    if (listSuggestTrack.isNotEmpty()) {
//                        val args = Bundle()
//                        args.putString("type", Config.PLAYLIST_CLICK)
//                        args.putString("videoId", listSuggestTrack[position].videoId)
//                        args.putString(
//                            "from",
//                            "${getString(
//                                R.string.playlist,
//                            )} \"${(viewModel.localPlaylist.value)?.title}\" ${
//                                getString(R.string.suggest)
//                            }",
//                        )
//                        args.putInt("index", position)
//                        Queue.initPlaylist(
//                            "RDAMVM${listSuggestTrack[position].videoId}",
//                            "${getString(
//                                R.string.playlist,
//                            )} \"${(viewModel.localPlaylist.value)?.title}\" ${
//                                getString(R.string.suggest)
//                            }",
//                            Queue.PlaylistType.RADIO,
//                        )
//                        Queue.setNowPlaying(listSuggestTrack[position])
//                        val tempList: ArrayList<Track> = arrayListOf()
//                        for (i in listSuggestTrack) {
//                            tempList.add(i)
//                        }
//                        Queue.addAll(tempList)
//                        if (Queue.getQueue().size >= 1) {
//                            Queue.removeTrackWithIndex(position)
//                        }
//                        findNavController().navigateSafe(
//                            R.id.action_global_nowPlayingFragment,
//                            args,
//                        )
//                    } else {
//                        Toast.makeText(
//                            requireContext(),
//                            getString(R.string.error),
//                            Toast.LENGTH_SHORT,
//                        )
//                            .show()
//                    }
//                }
//            },
//        )
//        suggestAdapter.setOnAddItemClickListener(
//            object :
//                SuggestItemAdapter.OnAddItemClickListener {
//                override fun onAddItemClick(position: Int) {
//                    if (listSuggestTrack.isNotEmpty()) {
//                        val song = listSuggestTrack.get(position)
//                        viewModel.insertSong(song)
//                        viewModel.updateInLibrary(song.videoId)
//                        val tempTrack = ArrayList<String>()
//                        if (viewModel.listTrack.value != null) {
//                            viewModel.listTrack.value?.forEach { track ->
//                                tempTrack.add(track.videoId)
//                            }
//                        }
//                        if (!tempTrack.contains(
//                                song.videoId,
//                            ) && viewModel.localPlaylist.value?.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced && viewModel.localPlaylist.value?.youtubePlaylistId != null
//                        ) {
//                            viewModel.addToYouTubePlaylist(
//                                viewModel.localPlaylist.value?.id!!,
//                                viewModel.localPlaylist.value?.youtubePlaylistId!!,
//                                song.videoId,
//                            )
//                        }
//                        if (!tempTrack.contains(song.videoId)) {
//                            viewModel.insertPairSongLocalPlaylist(
//                                PairSongLocalPlaylist(
//                                    playlistId = viewModel.localPlaylist.value?.id!!,
//                                    songId = song.videoId,
//                                    position = tempTrack.size,
//                                    inPlaylist = LocalDateTime.now(),
//                                ),
//                            )
//                            tempTrack.add(song.videoId)
//                        }
//
//                        viewModel.localPlaylist.value?.id?.let {
//                            viewModel.updateLocalPlaylistTracks(
//                                tempTrack.removeConflicts(),
//                                it,
//                            )
//                        }
//                        listSuggestTrack.remove(song)
//                        suggestAdapter.updateList(listSuggestTrack)
//                    } else {
//                        Toast.makeText(
//                            requireContext(),
//                            getString(R.string.error),
//                            Toast.LENGTH_SHORT,
//                        )
//                            .show()
//                    }
//                }
//            },
//        )
//
//        binding.topAppBar.setNavigationOnClickListener {
//            findNavController().popBackStack()
//        }
//        binding.topAppBarLayout.addOnOffsetChangedListener { it, verticalOffset ->
//            Log.d("Local Fragment", "Offset: $verticalOffset" + "Total: ${it.totalScrollRange}")
//            if (abs(it.totalScrollRange) == abs(verticalOffset)) {
//                binding.topAppBar.background = viewModel.gradientDrawable.value
//                if (viewModel.gradientDrawable.value != null) {
//                    if (viewModel.gradientDrawable.value?.colors != null) {
//                        requireActivity().window.statusBarColor =
//                            viewModel.gradientDrawable.value?.colors!!.first()
//                    }
//                }
//            } else {
//                requireActivity().window.statusBarColor =
//                    ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
//                Log.d("ArtistFragment", "Expanded")
//            }
//        }
//        binding.btPlayPause.setOnClickListener {
//            val listTrack = playlistAdapter.getListTrack()
//            if (listTrack.isNotEmpty()) {
//                val args = Bundle()
//                args.putString("type", Config.ALBUM_CLICK)
//                args.putString("videoId", (listTrack[0] as SongEntity).videoId)
//                args.putString("from", "Playlist \"${(viewModel.localPlaylist.value)?.title}\"")
//                if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
//                    args.putInt("downloaded", 1)
//                }
//                if (viewModel.localPlaylist.value?.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
//                    args.putString(
//                        "playlistId",
//                        viewModel.localPlaylist.value?.youtubePlaylistId?.replaceFirst("VL", ""),
//                    )
//                }
//                Queue.initPlaylist(
//                    Queue.LOCAL_PLAYLIST_ID + viewModel.localPlaylist.value?.id,
//                    "Playlist \"${(viewModel.localPlaylist.value)?.title}\"",
//                    Queue.PlaylistType.PLAYLIST,
//                )
//                Queue.setNowPlaying((listTrack[0] as SongEntity).toTrack())
//                val tempList: ArrayList<Track> = arrayListOf()
//                for (i in listTrack) {
//                    tempList.add((i as SongEntity).toTrack())
//                }
//                Queue.addAll(tempList)
//                if (Queue.getQueue().size >= 1) {
//                    Queue.removeFirstTrackForPlaylistAndAlbum()
//                }
//                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
//            } else {
//                Snackbar.make(
//                    requireView(),
//                    getString(R.string.playlist_is_empty),
//                    Snackbar.LENGTH_SHORT,
//                ).show()
//            }
//        }
//        binding.btShuffle.setOnClickListener {
//            if (listTrack.isNotEmpty()) {
//                val args = Bundle()
//                val index = Random.nextInt(listTrack.size)
//                args.putString("type", Config.ALBUM_CLICK)
//                args.putString("videoId", (listTrack[index] as SongEntity).videoId)
//                args.putString("from", "Playlist \"${(viewModel.localPlaylist.value)?.title}\"")
//                if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
//                    args.putInt("downloaded", 1)
//                }
//                if (viewModel.localPlaylist.value?.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
//                    args.putString(
//                        "playlistId",
//                        viewModel.localPlaylist.value?.youtubePlaylistId?.replaceFirst("VL", ""),
//                    )
//                }
//                Queue.initPlaylist(
//                    Queue.LOCAL_PLAYLIST_ID + viewModel.localPlaylist.value?.id,
//                    "Playlist \"${(viewModel.localPlaylist.value)?.title}\"",
//                    Queue.PlaylistType.PLAYLIST,
//                )
//                Queue.setNowPlaying((listTrack[index] as SongEntity).toTrack())
//                val tempList: ArrayList<Track> = arrayListOf()
//                for (i in listTrack) {
//                    if (i != listTrack[index]) {
//                        tempList.add((i as SongEntity).toTrack())
//                    }
//                }
//                tempList.shuffle()
//                Queue.addAll(tempList)
//                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
//            } else {
//                Snackbar.make(
//                    requireView(),
//                    getString(R.string.playlist_is_empty),
//                    Snackbar.LENGTH_SHORT,
//                ).show()
//            }
//        }
//
//        binding.btDownload.setOnClickListener {
//            if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
//                if (!viewModel.listTrack.value.isNullOrEmpty()) {
//                    val listJob: ArrayList<SongEntity> = arrayListOf()
//                    for (song in viewModel.listTrack.value!!) {
//                        if (song.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
//                            listJob.add(song)
//                        }
//                    }
//                    viewModel.listJob.value = listJob
//                    Log.d("PlaylistFragment", "ListJob: ${viewModel.listJob.value}")
//                    listJob.forEach { job ->
//                        val downloadRequest =
//                            DownloadRequest.Builder(job.videoId, job.videoId.toUri())
//                                .setData(job.title.toByteArray())
//                                .setCustomCacheKey(job.videoId)
//                                .build()
//                        viewModel.updateDownloadState(
//                            job.videoId,
//                            DownloadState.STATE_DOWNLOADING,
//                        )
//                        DownloadService.sendAddDownload(
//                            requireContext(),
//                            MusicDownloadService::class.java,
//                            downloadRequest,
//                            false,
//                        )
//                        viewModel.getDownloadStateFromService(job.videoId)
//                    }
//                    viewModel.downloadFullPlaylistState(id!!)
//                }
//            } else if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
//                Toast.makeText(requireContext(), getString(R.string.downloaded), Toast.LENGTH_SHORT)
//                    .show()
//            } else if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_DOWNLOADING) {
//                Toast.makeText(
//                    requireContext(),
//                    getString(R.string.downloading),
//                    Toast.LENGTH_SHORT,
//                ).show()
//            }
//        }
//        binding.btSuggest.setOnClickListener {
//            if (binding.suggestLayout.visibility == View.GONE) {
//                if (viewModel.localPlaylist.value?.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
//                    if (viewModel.localPlaylist.value?.youtubePlaylistId != null) {
//                        binding.suggestLayout.visibility = View.VISIBLE
//                        viewModel.getSuggestions(viewModel.localPlaylist.value?.youtubePlaylistId!!)
//                    }
//                }
//            } else {
//                binding.suggestLayout.visibility = View.GONE
//            }
//        }
//        binding.btReload.setOnClickListener {
//            viewModel.reloadSuggestion()
//        }
//        binding.btMore.setOnClickListener {
//            val moreDialog = BottomSheetDialog(requireContext())
//            val moreDialogView = BottomSheetLocalPlaylistBinding.inflate(layoutInflater)
//            moreDialogView.btEditTitle.setOnClickListener {
//                val editDialog = BottomSheetDialog(requireContext())
//                val editDialogView = BottomSheetEditPlaylistTitleBinding.inflate(layoutInflater)
//                editDialogView.etPlaylistName.editText?.setText(
//                    viewModel.localPlaylist.value?.title,
//                )
//                editDialogView.btEdit.setOnClickListener {
//                    if (editDialogView.etPlaylistName.editText?.text.isNullOrEmpty()) {
//                        Toast.makeText(
//                            requireContext(),
//                            getString(R.string.playlist_name_cannot_be_empty),
//                            Toast.LENGTH_SHORT,
//                        ).show()
//                    } else {
//                        viewModel.updatePlaylistTitle(
//                            editDialogView.etPlaylistName.editText?.text.toString(),
//                            id!!,
//                        )
//                        if (viewModel.localPlaylist.value?.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
//                            viewModel.updateYouTubePlaylistTitle(
//                                editDialogView.etPlaylistName.editText?.text.toString(),
//                                viewModel.localPlaylist.value?.youtubePlaylistId!!,
//                            )
//                        }
// //                        fetchDataFromDatabase()
//                        editDialog.dismiss()
//                        moreDialog.dismiss()
//                    }
//                }
//
//                editDialog.setCancelable(true)
//                editDialog.setContentView(editDialogView.root)
//                editDialog.show()
//            }
//            if (viewModel.localPlaylist.value?.syncedWithYouTubePlaylist == 0) {
//                moreDialogView.tvSync.text = getString(R.string.sync)
//                moreDialogView.ivSync.setImageResource(R.drawable.baseline_sync_24)
//                moreDialogView.btUpdate.visibility = View.GONE
//            } else if (viewModel.localPlaylist.value?.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
//                moreDialogView.tvSync.text = getString(R.string.synced)
//                moreDialogView.ivSync.setImageResource(R.drawable.baseline_sync_disabled_24)
//                moreDialogView.btUpdate.visibility = View.VISIBLE
//                moreDialogView.btUpdate.setOnClickListener {
//                    viewModel.updateListTrackSynced(
//                        viewModel.localPlaylist.value?.id!!,
//                        viewModel.localPlaylist.value?.tracks!!,
//                        viewModel.localPlaylist.value?.youtubePlaylistId!!,
//                    )
//                    viewModel.getSetVideoId(viewModel.localPlaylist.value?.youtubePlaylistId!!)
// //                    viewModel.localPlaylist.observe(viewLifecycleOwner) { localPlaylist ->
// //                        Log.d("Check", "fetchData: ${viewModel.localPlaylist.value}")
// //                        if (localPlaylist != null) {
// //                            if (!localPlaylist.tracks.isNullOrEmpty()) {
// //                                viewModel.getListTrack(localPlaylist.tracks)
// //                                viewModel.getPairSongLocalPlaylist(localPlaylist.id)
// //                            }
// //                        }
// //                        if (localPlaylist != null) {
// //                            binding.collapsingToolbarLayout.title = localPlaylist.title
// //                            binding.tvTitle.text = localPlaylist.title
// //                            binding.tvTitle.isSelected = true
// //                            if (localPlaylist.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced && localPlaylist.youtubePlaylistId != null) {
// //                                if (!localPlaylist.tracks.isNullOrEmpty()) {
// //                                    viewModel.getSetVideoId(localPlaylist.youtubePlaylistId)
// //                                }
// //                            }
// //                        }
// //                        binding.tvTrackCountAndTimeCreated.text =
// //                            getString(
// //                                R.string.album_length,
// //                                localPlaylist?.tracks?.size?.toString() ?: "0",
// //                                localPlaylist?.inLibrary?.format(
// //                                    DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"),
// //                                ),
// //                            )
// //                        loadImage(localPlaylist?.thumbnail)
// //                        with(binding) {
// //                            if (localPlaylist != null) {
// //                                when (localPlaylist.downloadState) {
// //                                    DownloadState.STATE_DOWNLOADED -> {
// //                                        btDownload.visibility = View.VISIBLE
// //                                        animationDownloading.visibility = View.GONE
// //                                        btDownload.setImageResource(R.drawable.baseline_downloaded)
// //                                    }
// //
// //                                    DownloadState.STATE_DOWNLOADING -> {
// //                                        btDownload.visibility = View.GONE
// //                                        animationDownloading.visibility = View.VISIBLE
// //                                    }
// //
// //                                    DownloadState.STATE_PREPARING -> {
// //                                        btDownload.visibility = View.GONE
// //                                        animationDownloading.visibility = View.VISIBLE
// //                                    }
// //
// //                                    DownloadState.STATE_NOT_DOWNLOADED -> {
// //                                        btDownload.visibility = View.VISIBLE
// //                                        animationDownloading.visibility = View.GONE
// //                                        btDownload.setImageResource(R.drawable.download_button)
// //                                    }
// //                                }
// //                            }
// //                        }
// //                    }
//                }
//            }
//            moreDialogView.btSync.setOnClickListener {
//                if (viewModel.localPlaylist.value?.syncedWithYouTubePlaylist == 0) {
//                    val alertDialog =
//                        MaterialAlertDialogBuilder(requireContext())
//                            .setTitle(getString(R.string.warning))
//                            .setMessage(getString(R.string.sync_playlist_warning))
//                            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
//                                dialog.dismiss()
//                            }
//                            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
//                                viewModel.localPlaylist.value?.let { playlist ->
//                                    Toast.makeText(
//                                        requireContext(),
//                                        getString(R.string.syncing),
//                                        Toast.LENGTH_SHORT,
//                                    ).show()
//                                    viewModel.syncPlaylistWithYouTubePlaylist(playlist)
//                                }
//                                dialog.dismiss()
//                                moreDialog.dismiss()
//                            }
//                    alertDialog.setCancelable(true)
//                    alertDialog.show()
// //                    viewModel.localPlaylist.observe(viewLifecycleOwner) { localPlaylist ->
// //                        if (localPlaylist?.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
// //                            moreDialogView.tvSync.text = getString(R.string.synced)
// //                            moreDialogView.ivSync.setImageResource(
// //                                R.drawable.baseline_sync_disabled_24,
// //                            )
// //                        }
// //                    }
//                } else if (viewModel.localPlaylist.value?.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
//                    val alertDialog =
//                        MaterialAlertDialogBuilder(requireContext())
//                            .setTitle(getString(R.string.warning))
//                            .setMessage(getString(R.string.unsync_playlist_warning))
//                            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
//                                dialog.dismiss()
//                            }
//                            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
//                                viewModel.localPlaylist.value?.let { playlist ->
//                                    Toast.makeText(
//                                        requireContext(),
//                                        getString(R.string.unsyncing),
//                                        Toast.LENGTH_SHORT,
//                                    ).show()
//                                    viewModel.unsyncPlaylistWithYouTubePlaylist(playlist)
//                                }
//                                dialog.dismiss()
//                                moreDialog.dismiss()
//                            }
//                    alertDialog.setCancelable(true)
//                    alertDialog.show()
// //                    viewModel.localPlaylist.observe(viewLifecycleOwner) { localPlaylist ->
// //                        if (localPlaylist?.syncedWithYouTubePlaylist == 0) {
// //                            moreDialogView.tvSync.text = getString(R.string.sync)
// //                            moreDialogView.ivSync.setImageResource(R.drawable.baseline_sync_24)
// //                        }
// //                    }
//                }
//            }
//            moreDialogView.btDelete.setOnClickListener {
//                Log.d("Check", "onViewCreated: ${viewModel.localPlaylist.value}")
//                Log.d("Check", "onViewCreated: $id")
//                viewModel.deletePlaylist(id!!)
//                moreDialog.dismiss()
//                Toast.makeText(requireContext(), "Playlist deleted", Toast.LENGTH_SHORT).show()
//                findNavController().popBackStack()
//            }
//
//            moreDialogView.btEditThumbnail.setOnClickListener {
//                val intent = Intent()
//                intent.type = "image/*"
//                intent.action = Intent.ACTION_OPEN_DOCUMENT
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//
//                resultLauncher.launch(intent)
//            }
//
//            moreDialogView.btAddToQueue.setOnClickListener {
//                val list = playlistAdapter.getListTrack()
//                if (list.isNotEmpty()) {
//                    sharedViewModel.addListToQueue(list.map { it as SongEntity }.toArrayListTrack())
//                } else {
//                    Snackbar.make(
//                        requireView(),
//                        getString(R.string.playlist_is_empty),
//                        Snackbar.LENGTH_SHORT,
//                    ).show()
//                }
//            }
//
//            moreDialog.setCancelable(true)
//            moreDialog.setContentView(moreDialogView.root)
//            moreDialog.show()
//        }
//        binding.topAppBarLayout.addOnOffsetChangedListener { it, verticalOffset ->
//            if (abs(it.totalScrollRange) == abs(verticalOffset)) {
//                binding.topAppBar.background = viewModel.gradientDrawable.value
//                binding.collapsingToolbarLayout.isTitleEnabled = true
//                if (viewModel.gradientDrawable.value != null) {
//                    if (viewModel.gradientDrawable.value?.colors != null) {
//                        requireActivity().window.statusBarColor =
//                            viewModel.gradientDrawable.value?.colors!!.first()
//                    }
//                }
//            } else {
//                binding.collapsingToolbarLayout.isTitleEnabled = false
//                binding.topAppBar.background = null
//                binding.topAppBarLayout.background = viewModel.gradientDrawable.value
//                requireActivity().window.statusBarColor =
//                    ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
//            }
//        }
//
//        lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
//                launch {
//                    viewModel.playlistDownloadState.collectLatest { playlistDownloadState ->
//                        when (playlistDownloadState) {
//                            DownloadState.STATE_PREPARING -> {
//                                binding.btDownload.visibility = View.GONE
//                                binding.animationDownloading.visibility = View.VISIBLE
//                            }
//
//                            DownloadState.STATE_DOWNLOADING -> {
//                                binding.btDownload.visibility = View.GONE
//                                binding.animationDownloading.visibility = View.VISIBLE
//                            }
//
//                            DownloadState.STATE_DOWNLOADED -> {
//                                binding.btDownload.visibility = View.VISIBLE
//                                binding.animationDownloading.visibility = View.GONE
//                                binding.btDownload.setImageResource(R.drawable.baseline_downloaded)
//                            }
//
//                            DownloadState.STATE_NOT_DOWNLOADED -> {
//                                binding.btDownload.visibility = View.VISIBLE
//                                binding.animationDownloading.visibility = View.GONE
//                                binding.btDownload.setImageResource(R.drawable.download_button)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
//                val job1 =
//                    launch {
//                        viewModel.listSuggestions.collectLatest { list ->
//                            if (!list.isNullOrEmpty()) {
//                                listSuggestTrack.clear()
//                                listSuggestTrack.addAll(list)
//                                suggestAdapter.updateList(listSuggestTrack)
//                                binding.rvSuggest.visibility = View.VISIBLE
//                                binding.btReload.visibility = View.VISIBLE
//                            } else {
//                                binding.rvSuggest.visibility = View.GONE
//                                binding.btReload.visibility = View.GONE
//                            }
//                        }
//                    }
//                val job2 =
//                    launch {
//                        viewModel.loading.collectLatest { loading ->
//                            if (loading) {
//                                binding.suggestLoading.visibility = View.VISIBLE
//                            } else {
//                                binding.suggestLoading.visibility = View.GONE
//                            }
//                        }
//                    }
//                val job3 =
//                    launch {
//                        combine(
//                            viewModel.listTrack,
//                            viewModel.listPair,
//                            viewModel.filter,
//                        ) { listSong, listPair, filter ->
//                            Triple(listSong, listPair, filter)
//                        }.collect {
//                            val listSong = it.first
//                            val listPair = it.second
//                            val filter = it.third
//                            Log.w("Check", "combine: $listPair")
//                            if (listPair != null && listSong != null) {
//                                listTrack.clear()
//                                listTrack.addAll(listSong)
//                                if (filter == FilterState.OlderFirst) {
//                                    binding.btSort.setIconResource(
//                                        R.drawable.baseline_arrow_drop_down_24,
//                                    )
//                                    listTrack.sortBy {
//                                        viewModel.listPair.value?.find {
//                                                pair ->
//                                            pair.songId == (it as SongEntity).videoId
//                                        }?.position
//                                    }
//                                } else if (filter == FilterState.NewerFirst) {
//                                    binding.btSort.setIconResource(
//                                        R.drawable.baseline_arrow_drop_up_24,
//                                    )
//                                    listTrack.sortByDescending {
//                                        viewModel.listPair.value?.find {
//                                                pair ->
//                                            pair.songId == (it as SongEntity).videoId
//                                        }?.position
//                                    }
//                                }
//                                playlistAdapter.updateList(listTrack)
//                            } else {
//                                listTrack.clear()
//                                playlistAdapter.updateList(arrayListOf())
//                            }
//                        }
//                    }
//                val job4 =
//                    launch {
//                        sharedViewModel.downloadList.collect {
//                            playlistAdapter.setDownloadedList(it)
//                        }
//                    }
//                val job5 =
//                    launch {
//                        combine(
//                            sharedViewModel.simpleMediaServiceHandler?.nowPlaying
//                                ?: flowOf<MediaItem?>(
//                                    null,
//                                ),
//                            sharedViewModel.isPlaying,
//                        ) { nowPlaying, isPlaying ->
//                            Pair(nowPlaying, isPlaying)
//                        }.collect {
//                            if (it.first != null && it.second) {
//                                playlistAdapter.setNowPlaying(it.first!!.mediaId)
//                            } else {
//                                playlistAdapter.setNowPlaying(null)
//                            }
//                        }
//                    }
//                job1.join()
//                job2.join()
//                job3.join()
//                job4.join()
//                job5.join()
//            }
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        setStatusBarsColor(
            ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark),
            requireActivity()
        )
    }
}