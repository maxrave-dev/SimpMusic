package com.maxrave.simpmusic.ui.fragment.library

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.findNavController
import com.maxrave.simpmusic.ui.screen.library.LibraryScreen
import com.maxrave.simpmusic.ui.theme.AppTheme

@UnstableApi
class LibraryFragment : Fragment() {
    private lateinit var composeView: ComposeView

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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        composeView.apply {
            setContent {
                AppTheme {
                    Scaffold { paddingValue ->
                        LibraryScreen(navController = findNavController())
                    }
                }
            }
        }
    }
//
//    override fun onViewCreated(
//        view: View,
//        savedInstanceState: Bundle?,
//    ) {
//        super.onViewCreated(view, savedInstanceState)
//        listRecentlyAdded = ArrayList()
//        adapterItem = SearchItemAdapter(arrayListOf(), requireContext())
//        // pagingAdapter = RecentPagingAdapter(requireContext())
//        listPlaylist = ArrayList()
//        adapterPlaylist = FavoritePlaylistAdapter(arrayListOf(), requireContext())
//
//        listDownloaded = ArrayList()
//        adapterDownloaded = FavoritePlaylistAdapter(arrayListOf(), requireContext())
//
//        listLocalPlaylist = ArrayList()
//        adapterLocalPlaylist = FavoritePlaylistAdapter(arrayListOf(), requireContext())
//
//        listYouTubePlaylist = ArrayList()
//        adapterYouTubePlaylist = FavoritePlaylistAdapter(arrayListOf(), requireContext())
//
//        binding.rvRecentlyAdded.apply {
//            adapter = adapterItem
//            layoutManager = LinearLayoutManager(requireContext())
//        }
//
//        binding.rvFavoritePlaylists.apply {
//            adapter = adapterPlaylist
//            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        }
//
//        binding.rvDownloadedPlaylists.apply {
//            adapter = adapterDownloaded
//            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        }
//
//        binding.rvYourPlaylists.apply {
//            adapter = adapterLocalPlaylist
//            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        }
//
//        binding.rvYouTubePlaylists.apply {
//            adapter = adapterYouTubePlaylist
//            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        }
//        viewModel.getYouTubePlaylist()
//        viewModel.getRecentlyAdded()
//        viewModel.getPlaylistFavorite()
//        viewModel.getLocalPlaylist()
//        viewModel.getDownloadedPlaylist()
//        viewModel.listPlaylistFavorite.observe(viewLifecycleOwner) { listFavorite ->
//            val temp = ArrayList<Any>()
//            for (i in listFavorite.size - 1 downTo 0) {
//                temp.add(listFavorite[i])
//            }
//            listPlaylist.clear()
//            listPlaylist.addAll(temp)
//            adapterPlaylist.updateList(listPlaylist)
//            if (listPlaylist.isEmpty()) {
//                binding.tvFavoritePlaylistsStatus.visibility = View.VISIBLE
//            } else {
//                binding.tvFavoritePlaylistsStatus.visibility = View.GONE
//            }
//        }
//        viewModel.listRecentlyAdded.observe(viewLifecycleOwner) { list ->
//            Log.d("LibraryFragment", "onViewCreated: $list")
//            val temp = ArrayList<Any>()
//            for (i in list.size - 1 downTo 0) {
//                temp.add(list[i])
//            }
//            listRecentlyAdded.clear()
//            listRecentlyAdded.addAll(temp)
//            adapterItem.updateList(listRecentlyAdded)
//            if (listRecentlyAdded.isEmpty()) {
//                binding.tvRecentlyAdded.visibility = View.GONE
//            } else {
//                binding.tvRecentlyAdded.visibility = View.VISIBLE
//            }
//        }
//        viewModel.listLocalPlaylist.observe(viewLifecycleOwner) { list ->
//            val temp = ArrayList<Any>()
//            for (i in list.size - 1 downTo 0) {
//                temp.add(list[i])
//            }
//            listLocalPlaylist.clear()
//            listLocalPlaylist.addAll(temp)
//            adapterLocalPlaylist.updateList(listLocalPlaylist)
//            if (listLocalPlaylist.isEmpty()) {
//                binding.tvYourPlaylistsStatus.visibility = View.VISIBLE
//            } else {
//                binding.tvYourPlaylistsStatus.visibility = View.GONE
//            }
//        }
//        viewModel.listDownloadedPlaylist.observe(viewLifecycleOwner) { list ->
//            val temp = ArrayList<Any>()
//            for (i in list.size - 1 downTo 0) {
//                temp.add(list[i])
//            }
//            listDownloaded.clear()
//            listDownloaded.addAll(temp)
//            adapterDownloaded.updateList(listDownloaded)
//            if (listDownloaded.isEmpty()) {
//                binding.tvDownloadedPlaylistsStatus.visibility = View.VISIBLE
//            } else {
//                binding.tvDownloadedPlaylistsStatus.visibility = View.GONE
//            }
//        }
//        if (!viewModel.getYouTubeLoggedIn()) {
//            binding.tvYouTubePlaylistsStatus.visibility = View.VISIBLE
//            binding.tvYouTubePlaylistsStatus.text = getString(R.string.log_in_to_get_YouTube_playlist)
//        }
//        viewModel.listYouTubePlaylist.observe(viewLifecycleOwner) { list ->
//            Log.w("LibraryFragment", "onViewCreated: $list")
//            if (!list.isNullOrEmpty() && viewModel.getYouTubeLoggedIn()) {
//                val temp = ArrayList<Any>()
//                for (i in list.size - 1 downTo 0) {
//                    temp.add(list[i])
//                }
//                listYouTubePlaylist.clear()
//                listYouTubePlaylist.addAll(temp)
//                adapterYouTubePlaylist.updateList(listYouTubePlaylist)
//                if (listYouTubePlaylist.isEmpty()) {
//                    binding.tvYouTubePlaylistsStatus.visibility = View.VISIBLE
//                    binding.tvYouTubePlaylistsStatus.text = getString(R.string.no_YouTube_playlists)
//                } else {
//                    binding.tvYouTubePlaylistsStatus.visibility = View.GONE
//                    binding.tvYouTubePlaylistsStatus.text = getString(R.string.no_YouTube_playlists)
//                }
//            } else {
//                binding.tvYouTubePlaylistsStatus.visibility = View.VISIBLE
//                binding.tvYouTubePlaylistsStatus.text = getString(R.string.log_in_to_get_YouTube_playlist)
//            }
//        }
//        adapterItem.setOnClickListener(
//            object : SearchItemAdapter.onItemClickListener {
//                @UnstableApi
//                override fun onItemClick(
//                    position: Int,
//                    type: String,
//                ) {
//                    if (type == "artist") {
//                        val channelId = (adapterItem.getCurrentList()[position] as ArtistEntity).channelId
//                        val args = Bundle()
//                        args.putString("channelId", channelId)
//                        findNavController().navigateSafe(R.id.action_global_artistFragment, args)
//                    }
//                    if (type == Config.ALBUM_CLICK) {
//                        val browseId = (adapterItem.getCurrentList()[position] as AlbumEntity).browseId
//                        val args = Bundle()
//                        args.putString("browseId", browseId)
//                        findNavController().navigateSafe(R.id.action_global_albumFragment, args)
//                    }
//                    if (type == Config.PLAYLIST_CLICK) {
//                        val id = (adapterItem.getCurrentList()[position] as PlaylistEntity).id
//                        val args = Bundle()
//                        args.putString("id", id)
//                        findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
//                    }
//                    if (type == Config.SONG_CLICK) {
//                        val songClicked = adapterItem.getCurrentList()[position] as SongEntity
//                        val videoId = songClicked.videoId
//                        val firstQueue: Track = songClicked.toTrack()
//                        viewModel.setQueueData(
//                            QueueData(
//                                listTracks = arrayListOf(firstQueue),
//                                firstPlayedTrack = firstQueue,
//                                playlistId = "RDAMVM$videoId",
//                                playlistName = getString(R.string.recently_added),
//                                playlistType = PlaylistType.RADIO,
//                                continuation = null,
//                            ),
//                        )
//                        viewModel.loadMediaItem(
//                            firstQueue,
//                            Config.SONG_CLICK,
//                            position,
//                        )
//                    }
//                }
//
//                @UnstableApi
//                override fun onOptionsClick(
//                    position: Int,
//                    type: String,
//                ) {
//                    val song = adapterItem.getCurrentList()[position] as SongEntity
//                    viewModel.getSongEntity(song.videoId)
//                    val dialog = BottomSheetDialog(requireContext())
//                    val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
//                    with(bottomSheetView) {
//                        btSleepTimer.visibility = View.GONE
//                        viewModel.songEntity.observe(viewLifecycleOwner) { songEntity ->
//                            if (songEntity != null) {
//                                if (songEntity.liked) {
//                                    tvFavorite.text = getString(R.string.liked)
//                                    cbFavorite.isChecked = true
//                                } else {
//                                    tvFavorite.text = getString(R.string.like)
//                                    cbFavorite.isChecked = false
//                                }
//                            }
//                        }
//                        btAddQueue.setOnClickListener {
//                            sharedViewModel.addToQueue(song.toTrack())
//                        }
//                        btPlayNext.setOnClickListener {
//                            sharedViewModel.playNext(song.toTrack())
//                        }
//                        btChangeLyricsProvider.visibility = View.GONE
//                        tvSongTitle.text = song.title
//                        tvSongTitle.isSelected = true
//                        tvSongArtist.text = song.artistName?.connectArtists()
//                        tvSongArtist.isSelected = true
//                        ivThumbnail.load(song.thumbnails)
//                        if (song.albumName != null) {
//                            setEnabledAll(btAlbum, true)
//                            tvAlbum.text = song.albumName
//                        } else {
//                            tvAlbum.text = getString(R.string.no_album)
//                            setEnabledAll(btAlbum, false)
//                        }
//                        btAlbum.setOnClickListener {
//                            val albumId = song.albumId
//                            if (albumId != null) {
//                                findNavController().navigateSafe(
//                                    R.id.action_global_albumFragment,
//                                    Bundle().apply {
//                                        putString("browseId", albumId)
//                                    },
//                                )
//                                dialog.dismiss()
//                            } else {
//                                Toast.makeText(
//                                    requireContext(),
//                                    getString(R.string.no_album),
//                                    Toast.LENGTH_SHORT,
//                                ).show()
//                            }
//                        }
//                        btRadio.setOnClickListener {
//                            val args = Bundle()
//                            args.putString("radioId", "RDAMVM${song.videoId}")
//                            args.putString(
//                                "videoId",
//                                song.videoId,
//                            )
//                            dialog.dismiss()
//                            findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
//                        }
//                        btLike.setOnClickListener {
//                            if (cbFavorite.isChecked) {
//                                cbFavorite.isChecked = false
//                                tvFavorite.text = getString(R.string.like)
//                                viewModel.updateLikeStatus(song.videoId, 0)
//                                viewModel.getRecentlyAdded()
//                                viewModel.listRecentlyAdded.observe(viewLifecycleOwner) { list ->
//                                    Log.d("LibraryFragment", "onViewCreated: $list")
//                                    val temp = ArrayList<Any>()
//                                    for (i in list.size - 1 downTo 0) {
//                                        temp.add(list[i])
//                                    }
//                                    listRecentlyAdded.clear()
//                                    listRecentlyAdded.addAll(temp)
//                                    adapterItem.updateList(listRecentlyAdded)
//                                }
//                            } else {
//                                cbFavorite.isChecked = true
//                                tvFavorite.text = getString(R.string.liked)
//                                viewModel.updateLikeStatus(song.videoId, 1)
//                                viewModel.getRecentlyAdded()
//                                viewModel.listRecentlyAdded.observe(viewLifecycleOwner) { list ->
//                                    Log.d("LibraryFragment", "onViewCreated: $list")
//                                    val temp = ArrayList<Any>()
//                                    for (i in list.size - 1 downTo 0) {
//                                        temp.add(list[i])
//                                    }
//                                    listRecentlyAdded.clear()
//                                    listRecentlyAdded.addAll(temp)
//                                    adapterItem.updateList(listRecentlyAdded)
//                                }
//                            }
//                        }
//
//                        btSeeArtists.setOnClickListener {
//                            val subDialog = BottomSheetDialog(requireContext())
//                            val subBottomSheetView = BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
//                            Log.d("FavoriteFragment", "onOptionsClick: ${song.artistId}")
//                            if (!song.artistName.isNullOrEmpty()) {
//                                val tempArtist = mutableListOf<Artist>()
//                                for (i in 0 until song.artistName.size) {
//                                    tempArtist.add(Artist(name = song.artistName[i], id = song.artistId?.get(i)))
//                                }
//                                Log.d("FavoriteFragment", "onOptionsClick: $tempArtist")
//                                val artistAdapter = SeeArtistOfNowPlayingAdapter(tempArtist)
//                                subBottomSheetView.rvArtists.apply {
//                                    adapter = artistAdapter
//                                    layoutManager = LinearLayoutManager(requireContext())
//                                }
//                                artistAdapter.setOnClickListener(
//                                    object : SeeArtistOfNowPlayingAdapter.OnItemClickListener {
//                                        override fun onItemClick(position: Int) {
//                                            val artist = tempArtist[position]
//                                            if (artist.id != null) {
//                                                findNavController().navigateSafe(
//                                                    R.id.action_global_artistFragment,
//                                                    Bundle().apply {
//                                                        putString("channelId", artist.id)
//                                                    },
//                                                )
//                                                subDialog.dismiss()
//                                                dialog.dismiss()
//                                            }
//                                        }
//                                    },
//                                )
//                            }
//
//                            subDialog.setCancelable(true)
//                            subDialog.setContentView(subBottomSheetView.root)
//                            subDialog.show()
//                        }
//                        btDelete.visibility = View.VISIBLE
//                        tvDelete.setText(R.string.delete)
//                        btDelete.setOnClickListener {
//                            viewModel.deleteSong(song.videoId)
//                            dialog.dismiss()
//                        }
//                        btDownload.visibility = View.GONE
//                        btAddPlaylist.setOnClickListener {
//                            viewModel.getLocalPlaylist()
//                            val listLocalPlaylist: ArrayList<LocalPlaylistEntity> = arrayListOf()
//                            val addPlaylistDialog = BottomSheetDialog(requireContext())
//                            val viewAddPlaylist = BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
//                            val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
//                            addToAPlaylistAdapter.setVideoId(song.videoId)
//                            viewAddPlaylist.rvLocalPlaylists.apply {
//                                adapter = addToAPlaylistAdapter
//                                layoutManager = LinearLayoutManager(requireContext())
//                            }
//                            viewModel.listLocalPlaylist.observe(viewLifecycleOwner) { list ->
//                                Log.d("Check Local Playlist", list.toString())
//                                listLocalPlaylist.clear()
//                                listLocalPlaylist.addAll(list)
//                                addToAPlaylistAdapter.updateList(listLocalPlaylist)
//                            }
//                            addToAPlaylistAdapter.setOnItemClickListener(
//                                object : AddToAPlaylistAdapter.OnItemClickListener {
//                                    override fun onItemClick(position: Int) {
//                                        val playlist = listLocalPlaylist[position]
//                                        val tempTrack = ArrayList<String>()
//                                        viewModel.updateInLibrary(song.videoId)
//                                        if (playlist.tracks != null) {
//                                            tempTrack.addAll(playlist.tracks)
//                                        }
//                                        if (!tempTrack.contains(
//                                                song.videoId,
//                                            ) && playlist.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced && playlist.youtubePlaylistId != null
//                                        ) {
//                                            viewModel.addToYouTubePlaylist(playlist.id, playlist.youtubePlaylistId, song.videoId)
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
//                            val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share_url))
//                            startActivity(chooserIntent)
//                        }
//                    }
//                    dialog.setCancelable(true)
//                    dialog.setContentView(bottomSheetView.root)
//                    dialog.show()
//                }
//            },
//        )
//        adapterPlaylist.setOnClickListener(
//            object : FavoritePlaylistAdapter.OnItemClickListener {
//                override fun onItemClick(
//                    position: Int,
//                    type: String,
//                ) {
//                    when (type) {
//                        "playlist" -> {
//                            val id = (listPlaylist[position] as PlaylistEntity).id
//                            val args = Bundle()
//                            args.putString("id", id)
//                            findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
//                        }
//                        "album" -> {
//                            val browseId = (listPlaylist[position] as AlbumEntity).browseId
//                            val args = Bundle()
//                            args.putString("browseId", browseId)
//                            findNavController().navigateSafe(R.id.action_global_albumFragment, args)
//                        }
//                    }
//                }
//            },
//        )
//        adapterYouTubePlaylist.setOnClickListener(
//            object : FavoritePlaylistAdapter.OnItemClickListener {
//                override fun onItemClick(
//                    position: Int,
//                    type: String,
//                ) {
//                    val args = Bundle()
//                    args.putString("id", (listYouTubePlaylist.get(position) as PlaylistsResult).browseId)
//                    args.putBoolean("youtube", true)
//                    findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
//                }
//            },
//        )
//        adapterDownloaded.setOnClickListener(
//            object : FavoritePlaylistAdapter.OnItemClickListener {
//                override fun onItemClick(
//                    position: Int,
//                    type: String,
//                ) {
//                    when (type) {
//                        "playlist" -> {
//                            val id = (listDownloaded[position] as PlaylistEntity).id
//                            val args = Bundle()
//                            args.putString("id", id)
//                            args.putInt("downloaded", 1)
//                            findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
//                        }
//                        "album" -> {
//                            val browseId = (listDownloaded[position] as AlbumEntity).browseId
//                            val args = Bundle()
//                            args.putString("browseId", browseId)
//                            args.putInt("downloaded", 1)
//                            findNavController().navigateSafe(R.id.action_global_albumFragment, args)
//                        }
//                    }
//                }
//            },
//        )
//        adapterLocalPlaylist.setOnClickListener(
//            object : FavoritePlaylistAdapter.OnItemClickListener {
//                override fun onItemClick(
//                    position: Int,
//                    type: String,
//                ) {
//                    val playlist = listLocalPlaylist[position] as LocalPlaylistEntity
//                    val args = Bundle()
//                    args.putLong("id", playlist.id)
//                    findNavController().navigateSafe(R.id.action_bottom_navigation_item_library_to_localPlaylistFragment, args)
//                }
//            },
//        )
//        binding.btFavorite.setOnClickListener {
//            findNavController().navigateSafe(R.id.action_bottom_navigation_item_library_to_favoriteFragment)
//        }
//        binding.btFollowed.setOnClickListener {
//            findNavController().navigateSafe(R.id.action_bottom_navigation_item_library_to_followedFragment)
//        }
//        binding.btTrending.setOnClickListener {
//            findNavController().navigateSafe(R.id.action_bottom_navigation_item_library_to_mostPlayedFragment)
//        }
//        binding.btDownloaded.setOnClickListener {
//            findNavController().navigateSafe(R.id.action_bottom_navigation_item_library_to_downloadedFragment)
//        }
//        binding.btAddLocalPlaylist.setOnClickListener {
//            val dialog = BottomSheetDialog(requireContext())
//            val viewDialog = BottomSheetAddPlaylistBinding.inflate(layoutInflater)
//            viewDialog.btCreatePlaylist.setOnClickListener {
//                val title = viewDialog.etPlaylistName.editText?.text.toString()
//                if (title.isNotEmpty()) {
//                    viewModel.createPlaylist(title)
//                    viewModel.listLocalPlaylist.observe(viewLifecycleOwner) { list ->
//                        val temp: ArrayList<Any> = arrayListOf()
//                        for (i in list.size - 1 downTo 0) {
//                            temp.add(list[i])
//                        }
//                        listLocalPlaylist.clear()
//                        listLocalPlaylist.addAll(temp)
//                        adapterLocalPlaylist.updateList(temp)
//                        if (listLocalPlaylist.isEmpty()) {
//                            binding.tvYourPlaylistsStatus.visibility = View.VISIBLE
//                        } else {
//                            binding.tvYourPlaylistsStatus.visibility = View.GONE
//                        }
//                    }
//                    dialog.dismiss()
//                }
//            }
//            dialog.setCancelable(true)
//            dialog.setContentView(viewDialog.root)
//            dialog.show()
//        }
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.CREATED) {
//                val job1 =
//                    launch {
//                        sharedViewModel.downloadList.collect {
//                            adapterItem.setDownloadedList(it)
//                        }
//                    }
//                val job2 = launch {
//                    combine(sharedViewModel.nowPlayingState.distinctUntilChangedBy {
//                        it?.songEntity?.videoId
//                    }, sharedViewModel.controllerState.distinctUntilChangedBy {
//                        it.isPlaying
//                    }) { nowPlaying, controllerState ->
//                        Pair(nowPlaying, controllerState)
//                    }.collect {
//                        val songEntity = it.first?.songEntity
//                        if (songEntity != null && it.second.isPlaying) {
//                            adapterItem.setNowPlaying(songEntity.videoId)
//                        } else {
//                            adapterItem.setNowPlaying(null)
//                        }
//                    }
//                }
//                job1.join()
//                job2.join()
//            }
//        }
////        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
////            when (menuItem.itemId) {
////                R.id.library_fragment_menu_item_add -> {
////                    val dialog = BottomSheetDialog(requireContext())
////                    val viewDialog = BottomSheetAddPlaylistBinding.inflate(layoutInflater)
////                    viewDialog.btCreatePlaylist.setOnClickListener {
////                        val title = viewDialog.etPlaylistName.editText?.text.toString()
////                        if (title.isNotEmpty()){
////                            viewModel.createPlaylist(title)
////                            viewModel.listLocalPlaylist.observe(viewLifecycleOwner) { list ->
////                                val temp: ArrayList<Any> = arrayListOf()
////                                for (i in list.size - 1 downTo 0) {
////                                    temp.add(list[i])
////                                }
////                                listLocalPlaylist.clear()
////                                listLocalPlaylist.addAll(temp)
////                                adapterLocalPlaylist.updateList(temp)
////                                if (listLocalPlaylist.isEmpty()) {
////                                    binding.tvYourPlaylistsStatus.visibility = View.VISIBLE
////                                }
////                                else {
////                                    binding.tvYourPlaylistsStatus.visibility = View.GONE
////                                }
////                            }
////                            dialog.dismiss()
////                        }
////                    }
////                    dialog.setCancelable(true)
////                    dialog.setContentView(viewDialog.root)
////                    dialog.show()
////                    true
////                }
////                else -> {
////                    false
////                }
////            }
////        }
//    }
}
