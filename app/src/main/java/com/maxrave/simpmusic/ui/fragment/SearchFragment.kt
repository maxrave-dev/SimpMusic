package com.maxrave.simpmusic.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.net.toUri
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
import coil3.load
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.models.YTItem
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.adapter.search.SearchHistoryItemAdapter
import com.maxrave.simpmusic.adapter.search.SearchItemAdapter
import com.maxrave.simpmusic.adapter.search.SuggestQueryAdapter
import com.maxrave.simpmusic.adapter.search.SuggestYTItemAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentSearchBinding
import com.maxrave.simpmusic.extension.IntermediaryMigrateApi
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SearchViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // Data Saved
    private lateinit var searchHistory: ArrayList<String>
    //private lateinit var resultList: ArrayList<Any>
    //private lateinit var suggestList: ArrayList<String>
    //private lateinit var searchAllResult: ArrayList<Any>
    //private lateinit var suggestYTItemList: ArrayList<YTItem>

    private val viewModel by activityViewModels<SearchViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private lateinit var resultAdapter: SearchItemAdapter
    private lateinit var searchHistoryAdapter: SearchHistoryItemAdapter
    private lateinit var suggestAdapter: SuggestQueryAdapter
    private lateinit var suggestYTItemAdapter: SuggestYTItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d("SearchFragment", "onCreateView")
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        binding.root.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    override fun onPause() {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        observeSearchHistory()
        if (viewModel.loading.value == true) {
            binding.shimmerLayout.startShimmer()
            binding.shimmerLayout.visibility = View.VISIBLE
        } else {
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.visibility = View.GONE
        }
        Log.d("SearchFragment", "onResume")
//        binding.svSearch.setQuery("", false)
//        binding.svSearch.clearFocus()
        binding.svSearch.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        viewModel.searchSubmitted.value = true
                        viewModel.searchText.value = it

                        viewModel.resultList.value?.clear()
                        resultAdapter.updateList(arrayListOf())

                        binding.suggestList.visibility = View.GONE
                        binding.suggestListYtItem.visibility = View.GONE
                        binding.recentlyQueryView.visibility = View.GONE
                        binding.defaultLayout.visibility = View.GONE
                        binding.resultView.visibility = View.VISIBLE

                        viewModel.insertSearchHistory(it)
                        viewModel.searchHistory.value?.add(it)
                        observeSearchHistory()
                        Log.d("Check History", searchHistory.toString())
                        fetchSearchAll(it)
                    }
                    binding.svSearch.clearFocus()
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (viewModel.searchSubmitted.value == true && newText == viewModel.searchText.value) {
                        return false
                    }

                    if (newText.isNullOrEmpty()) {
                        viewModel.searchSubmitted.value = false
                        binding.suggestList.visibility = View.GONE
                        binding.suggestListYtItem.visibility = View.GONE
                        binding.recentlyQueryView.visibility = View.VISIBLE
                        binding.resultView.visibility = View.GONE
                        binding.defaultLayout.visibility = View.GONE
                    } else {
                        viewModel.searchSubmitted.value = false
                        binding.suggestList.visibility = View.VISIBLE
                        binding.suggestListYtItem.visibility = View.VISIBLE
                        binding.recentlyQueryView.visibility = View.GONE
                        binding.defaultLayout.visibility = View.GONE
                        binding.resultView.visibility = View.GONE
                        Log.d("Gọi suggest", "onQueryTextChange: $newText")
                        fetchSuggestList(newText)
                    }
                    viewModel.searchText.value = newText
                    return false
                }
            },
        )

        if (viewModel.searchAllResult.value != null && viewModel.searchAllResult.value!!.isNotEmpty()) {
            updateResultsAdapter()
        }
    }

    private fun updateResultsAdapter() {
        val results = getResultsForCurrentType()
        resultAdapter.updateList(ArrayList(results))
    }

    private fun getResultsForCurrentType(): List<Any> {
        return when (viewModel.searchType.value) {
            "all" -> viewModel.searchAllResult.value ?: arrayListOf()
            "songs" -> viewModel.songsSearchResult.value?.data ?: arrayListOf<SongsResult>()
            "videos" -> viewModel.videoSearchResult.value?.data ?: arrayListOf<VideosResult>()
            "albums" -> viewModel.albumsSearchResult.value?.data ?: arrayListOf<AlbumsResult>()
            "artists" -> viewModel.artistsSearchResult.value?.data ?: arrayListOf<ArtistsResult>()
            "playlists" -> viewModel.playlistSearchResult.value?.data ?: arrayListOf<PlaylistsResult>()
            "featured_playlists" -> viewModel.featuredPlaylistSearchResult.value?.data ?: arrayListOf<PlaylistsResult>()
            "podcasts" -> viewModel.podcastSearchResult.value?.data ?: arrayListOf<PlaylistsResult>()
            else -> arrayListOf()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        binding.svSearch.setQuery("", false)
//        binding.svSearch.clearFocus()
        _binding = null
        Log.d("Xoá Fragment", "onDestroyView")
    }

    private fun showLoadingState() {
        binding.refreshSearch.isRefreshing = true
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
    }

    private fun hideLoadingState() {
        binding.refreshSearch.isRefreshing = false
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
    }

    private fun showSearchResults() {
        updateResultsAdapter()
        binding.resultList.smoothScrollToPosition(0)
        binding.suggestList.visibility = View.GONE
        binding.suggestListYtItem.visibility = View.GONE
        binding.recentlyQueryView.visibility = View.GONE
        binding.defaultLayout.visibility = View.GONE
        binding.resultView.visibility = View.VISIBLE
    }

    private fun showSearchSuggestions() {
        binding.suggestList.visibility = View.VISIBLE
        binding.suggestListYtItem.visibility = View.VISIBLE
        binding.recentlyQueryView.visibility = View.GONE
        binding.defaultLayout.visibility = View.GONE
        binding.resultView.visibility = View.GONE
    }

    private fun showDefaultOrHistory() {
        if (searchHistory.isEmpty()) {
            binding.recentlyQueryView.visibility = View.GONE
            binding.defaultLayout.visibility = View.VISIBLE
        } else {
            binding.recentlyQueryView.visibility = View.VISIBLE
            binding.defaultLayout.visibility = View.GONE
            binding.resultView.visibility = View.GONE
        }
        binding.suggestList.visibility = View.GONE
        binding.suggestListYtItem.visibility = View.GONE
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getSearchHistory()
        Log.d("SearchFragment", "onViewCreated")
        Log.d("SearchFragment", "viewModel.searchAllResult.value: ${viewModel.searchAllResult.value}")
        //resultList = ArrayList<Any>()
        //searchAllResult = ArrayList<Any>()
        searchHistory = ArrayList()
        //suggestList = ArrayList<String>()
        //suggestYTItemList = ArrayList<YTItem>()

        resultAdapter = SearchItemAdapter(arrayListOf(), requireContext())
        searchHistoryAdapter = SearchHistoryItemAdapter(arrayListOf())
        suggestAdapter = SuggestQueryAdapter(arrayListOf())
        suggestYTItemAdapter = SuggestYTItemAdapter(arrayListOf(), requireContext())
        
        binding.recentlyList.apply {
            adapter = searchHistoryAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.suggestList.apply {
            adapter = suggestAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.suggestListYtItem.apply {
            adapter = suggestYTItemAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.resultList.apply {
            adapter = resultAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.searchText.observe(viewLifecycleOwner) { text ->
            if (binding.svSearch.query.toString() != text) {
                binding.svSearch.setQuery(text, false)
            }
        }
        viewModel.resultList.observe(viewLifecycleOwner) { list ->
            resultAdapter.updateList(list)
        }
        viewModel.suggestList.observe(viewLifecycleOwner) { list ->
            suggestAdapter.updateData(list)
        }
        viewModel.suggestYTItemList.observe(viewLifecycleOwner) { list ->
            suggestYTItemAdapter.updateList(list)
        }
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoadingState()
            } else {
                hideLoadingState()
                when {
                    viewModel.searchSubmitted.value == true -> { showSearchResults() }
                    !viewModel.searchText.value.isNullOrEmpty() -> { showSearchSuggestions() }
                    else -> { showDefaultOrHistory() }
                }
            }
        }
        viewModel.loading.value = viewModel.loading.value
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(
                    requireActivity().findViewById(R.id.mini_player_container),
                    it,
                    Snackbar.LENGTH_LONG
                ).setAction(getString(R.string.retry)) {
                    fetchSearchAll(binding.svSearch.query.toString())
                }.setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                    .setDuration(3000)
                    .show()
            }
        }
        viewModel.searchType.observe(viewLifecycleOwner) { type ->
            when (type) {
                "all" -> binding.chipGroupTypeSearch.check(binding.chipAll.id)
                "songs" -> binding.chipGroupTypeSearch.check(binding.chipSong.id)
                "videos" -> binding.chipGroupTypeSearch.check(binding.chipVideo.id)
                "albums" -> binding.chipGroupTypeSearch.check(binding.chipAlbum.id)
                "artists" -> binding.chipGroupTypeSearch.check(binding.chipArtists.id)
                "playlists" -> binding.chipGroupTypeSearch.check(binding.chipPlaylist.id)
                "featured_playlists" -> binding.chipGroupTypeSearch.check(binding.chipFeaturedPlaylist.id)
                "podcasts" -> binding.chipGroupTypeSearch.check(binding.chipPodcast.id)
            }
        }

        binding.svSearch.setOnQueryTextFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                Log.d("Check History in ViewModel", viewModel.searchHistory.value.toString())
                if (binding.svSearch.query.isNullOrEmpty()) {
                    observeSearchHistory()
                    binding.suggestList.visibility = View.GONE
                    binding.suggestListYtItem.visibility = View.GONE
                    binding.recentlyQueryView.visibility = View.VISIBLE
                    binding.defaultLayout.visibility = View.GONE
                    binding.resultView.visibility = View.GONE
                }
            }
        }
        suggestYTItemAdapter.setOnClickListener(
            object : SuggestYTItemAdapter.onItemClickListener {
                override fun onItemClick(
                    position: Int,
                    type: String,
                ) {
                    if (type == "artist") {
                        val channelId = (suggestYTItemAdapter.getCurrentList()[position] as ArtistItem).id
                        val args = Bundle()
                        args.putString("channelId", channelId)
                        findNavController().navigateSafe(R.id.action_bottom_navigation_item_search_to_artistFragment, args)
                    }
                    if (type == Config.ALBUM_CLICK) {
                        val browseId = (suggestYTItemAdapter.getCurrentList()[position] as AlbumItem).browseId
                        val args = Bundle()
                        args.putString("browseId", browseId)
                        findNavController().navigateSafe(R.id.action_global_albumFragment, args)
                    }
                    if (type == Config.PLAYLIST_CLICK) {
                        val id = (suggestYTItemAdapter.getCurrentList()[position] as PlaylistItem).id
                        val args = Bundle()
                        args.putString("id", id)
                        findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
                    }
                    if (type == Config.SONG_CLICK) {
                        val songClicked = suggestYTItemAdapter.getCurrentList()[position] as SongItem
                        val videoId = (suggestYTItemAdapter.getCurrentList()[position] as SongItem).id
                        val firstQueue: Track = songClicked.toTrack()
                        viewModel.setQueueData(
                            QueueData(
                                listTracks = arrayListOf(firstQueue),
                                firstPlayedTrack = firstQueue,
                                playlistId = "RDAMVM$videoId",
                                playlistName = "\"${binding.svSearch.query}\" ${getString(R.string.in_search)}",
                                playlistType = PlaylistType.RADIO,
                                continuation = null,
                            ),
                        )
                        viewModel.loadMediaItem(
                            firstQueue,
                            type = Config.SONG_CLICK,
                        )
                    }
                    if (type == Config.VIDEO_CLICK) {
                        val videoClicked = suggestYTItemAdapter.getCurrentList()[position] as VideoItem
                        val videoId = videoClicked.id
                        val firstQueue = videoClicked.toTrack()
                        viewModel.setQueueData(
                            QueueData(
                                listTracks = arrayListOf(firstQueue),
                                firstPlayedTrack = firstQueue,
                                playlistId = "RDAMVM$videoId",
                                playlistName = "\"${binding.svSearch.query}\" ${getString(R.string.in_search)}",
                                playlistType = PlaylistType.RADIO,
                                continuation = null,
                            ),
                        )
                        viewModel.loadMediaItem(
                            firstQueue,
                            type = Config.VIDEO_CLICK,
                        )
                    }
                }
            },
        )

        resultAdapter.setOnClickListener(
            object : SearchItemAdapter.onItemClickListener {
                override fun onItemClick(
                    position: Int,
                    type: String,
                ) {
                    if (type == "artist") {
                        val channelId = (resultAdapter.getCurrentList()[position] as ArtistsResult).browseId
                        val args = Bundle()
                        args.putString("channelId", channelId)
                        findNavController().navigateSafe(R.id.action_bottom_navigation_item_search_to_artistFragment, args)
                    }
                    if (type == Config.ALBUM_CLICK) {
                        val browseId = (resultAdapter.getCurrentList()[position] as AlbumsResult).browseId
                        val args = Bundle()
                        args.putString("browseId", browseId)
                        findNavController().navigateSafe(R.id.action_global_albumFragment, args)
                    }
                    if (type == Config.PLAYLIST_CLICK) {
                        val data = (resultAdapter.getCurrentList()[position] as PlaylistsResult)
                        val id = (resultAdapter.getCurrentList()[position] as PlaylistsResult).browseId
                        if (data.resultType == "Podcast") {
                            val args = Bundle()
                            args.putString("id", id)
                            findNavController().navigateSafe(R.id.action_global_podcastFragment, args)
                        } else {
                            val args = Bundle()
                            args.putString("id", id)
                            findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
                        }
                    }
                    if (type == Config.SONG_CLICK) {
                        val songClicked = resultAdapter.getCurrentList()[position] as SongsResult
                        val videoId = (resultAdapter.getCurrentList()[position] as SongsResult).videoId
                        viewModel.setQueueData(
                            QueueData(
                                listTracks = arrayListOf(songClicked.toTrack()),
                                firstPlayedTrack = songClicked.toTrack(),
                                playlistId = "RDAMVM$videoId",
                                playlistName = "\"${binding.svSearch.query}\" ${getString(R.string.in_search)}",
                                playlistType = PlaylistType.RADIO,
                                continuation = null,
                            ),
                        )
                        viewModel.loadMediaItem(
                            songClicked.toTrack(),
                            type = Config.SONG_CLICK,
                        )
                    }
                    if (type == Config.VIDEO_CLICK) {
                        val videoClicked = resultAdapter.getCurrentList()[position] as VideosResult
                        val videoId = videoClicked.videoId
                        val firstQueue = videoClicked.toTrack()
                        viewModel.setQueueData(
                            QueueData(
                                listTracks = arrayListOf(firstQueue),
                                firstPlayedTrack = firstQueue,
                                playlistId = "RDAMVM$videoId",
                                playlistName = "\"${binding.svSearch.query}\" ${getString(R.string.in_search)}",
                                playlistType = PlaylistType.RADIO,
                                continuation = null,
                            ),
                        )
                        viewModel.loadMediaItem(
                            firstQueue,
                            type = Config.VIDEO_CLICK,
                        )
                    }
                }

                @UnstableApi
                override fun onOptionsClick(
                    position: Int,
                    type: String,
                ) {
                    val song =
                        if (type == Config.SONG_CLICK) {
                            resultAdapter.getCurrentList()[position] as SongsResult
                        } else {
                            resultAdapter.getCurrentList()[position] as VideosResult
                        }
                    val track = if (song is SongsResult) song.toTrack() else (song as VideosResult).toTrack()
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
                        if (track.album != null) {
                            setEnabledAll(btAlbum, true)
                            tvAlbum.text = track.album.name
                        } else {
                            tvAlbum.text = getString(R.string.no_album)
                            setEnabledAll(btAlbum, false)
                        }
                        btAlbum.setOnClickListener {
                            val albumId = track.album?.id
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
                                track.videoId,
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
                                viewModel.updateLikeStatus(track.videoId, false)
                            } else {
                                cbFavorite.isChecked = true
                                tvFavorite.text = getString(R.string.liked)
                                viewModel.updateLikeStatus(track.videoId, true)
                            }
                        }

                        btSeeArtists.setOnClickListener {
                            val subDialog = BottomSheetDialog(requireContext())
                            val subBottomSheetView = BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                            if (track.artists != null) {
                                val artistAdapter = SeeArtistOfNowPlayingAdapter(track.artists)
                                subBottomSheetView.rvArtists.apply {
                                    adapter = artistAdapter
                                    layoutManager = LinearLayoutManager(requireContext())
                                }
                                artistAdapter.setOnClickListener(
                                    object : SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                        override fun onItemClick(position: Int) {
                                            val artist = track.artists[position]
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
                        btShare.setOnClickListener {
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "text/plain"
                            val url = "https://youtube.com/watch?v=${track.videoId}"
                            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                            val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share_url))
                            startActivity(chooserIntent)
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
                            addToAPlaylistAdapter.setVideoId(track.videoId)
                            viewModel.localPlaylist.observe(viewLifecycleOwner) { list ->
                                Log.d("Check Local Playlist", list.toString())
                                listLocalPlaylist.clear()
                                listLocalPlaylist.addAll(list)
                                addToAPlaylistAdapter.updateList(listLocalPlaylist)
                            }
                            addToAPlaylistAdapter.setOnItemClickListener(
                                object : AddToAPlaylistAdapter.OnItemClickListener {
                                    override fun onItemClick(position: Int) {
                                        val playlist = listLocalPlaylist[position]
                                        val tempTrack = ArrayList<String>()
                                        viewModel.updateInLibrary(track.videoId)
                                        if (playlist.tracks != null) {
                                            tempTrack.addAll(playlist.tracks)
                                        }
                                        if (!tempTrack.contains(track.videoId) &&
                                            playlist.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced &&
                                            playlist.youtubePlaylistId != null
                                        ) {
                                            viewModel.addToYouTubePlaylist(playlist.id, playlist.youtubePlaylistId, track.videoId)
                                        }
                                        if (!tempTrack.contains(track.videoId)) {
                                            viewModel.insertPairSongLocalPlaylist(
                                                PairSongLocalPlaylist(
                                                    playlistId = playlist.id,
                                                    songId = track.videoId,
                                                    position = playlist.tracks?.size ?: 0,
                                                    inPlaylist = LocalDateTime.now(),
                                                ),
                                            )
                                            tempTrack.add(track.videoId)
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
                        btSleepTimer.visibility = View.GONE
                        btDownload.setOnClickListener {
                            if (tvDownload.text == getString(R.string.download)) {
                                Log.d("Download", "onClick: ${track.videoId}")
                                viewModel.updateDownloadState(
                                    track.videoId,
                                    DownloadState.STATE_PREPARING,
                                )
                                val downloadRequest =
                                    DownloadRequest
                                        .Builder(track.videoId, track.videoId.toUri())
                                        .setData(track.title.toByteArray())
                                        .setCustomCacheKey(track.videoId)
                                        .build()
                                viewModel.updateDownloadState(
                                    track.videoId,
                                    DownloadState.STATE_DOWNLOADING,
                                )
                                viewModel.getDownloadStateFromService(track.videoId)
                                DownloadService.sendAddDownload(
                                    requireContext(),
                                    MusicDownloadService::class.java,
                                    downloadRequest,
                                    false,
                                )
                                lifecycleScope.launch {
                                    viewModel.downloadState.collect { download ->
                                        if (download != null) {
                                            when (download.state) {
                                                Download.STATE_DOWNLOADING -> {
                                                    viewModel.updateDownloadState(
                                                        track.videoId,
                                                        DownloadState.STATE_DOWNLOADING,
                                                    )
                                                    tvDownload.text = getString(R.string.downloading)
                                                    ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                                                    setEnabledAll(btDownload, true)
                                                }

                                                Download.STATE_FAILED -> {
                                                    viewModel.updateDownloadState(
                                                        track.videoId,
                                                        DownloadState.STATE_NOT_DOWNLOADED,
                                                    )
                                                    tvDownload.text = getString(R.string.download)
                                                    ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                                    setEnabledAll(btDownload, true)
                                                    Toast
                                                        .makeText(
                                                            requireContext(),
                                                            getString(androidx.media3.exoplayer.R.string.exo_download_failed),
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                }

                                                Download.STATE_COMPLETED -> {
                                                    viewModel.updateDownloadState(
                                                        track.videoId,
                                                        DownloadState.STATE_DOWNLOADED,
                                                    )
                                                    Toast
                                                        .makeText(
                                                            requireContext(),
                                                            getString(R.string.downloaded),
                                                            Toast.LENGTH_SHORT,
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
                            } else if (tvDownload.text == getString(R.string.downloaded) || tvDownload.text == getString(R.string.downloading)) {
                                DownloadService.sendRemoveDownload(
                                    requireContext(),
                                    MusicDownloadService::class.java,
                                    track.videoId,
                                    false,
                                )
                                viewModel.updateDownloadState(
                                    track.videoId,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                                tvDownload.text = getString(R.string.download)
                                ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                setEnabledAll(btDownload, true)
                                Toast
                                    .makeText(
                                        requireContext(),
                                        getString(R.string.removed_download),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                    }
                    dialog.setCancelable(true)
                    dialog.setContentView(bottomSheetView.root)
                    dialog.show()
                }
            },
        )
        searchHistoryAdapter.setOnClickListener(
            object : SearchHistoryItemAdapter.onItemClickListener {
                override fun onItemClick(position: Int) {
                    binding.svSearch.setQuery(searchHistoryAdapter.getCurrentList()[position], true)
                }
            },
        )
        searchHistoryAdapter.setOnDeleteClickListener(
            object : SearchHistoryItemAdapter.onDeleteClickListener {
                override fun onDeleteClick(position: Int) {
                    viewModel.searchHistory.value?.let { currentHistory ->
                        val indexToRemove = currentHistory.size - 1 - position
                        if (currentHistory.size == 1) {
                            clearSearchHistory()
                            return
                        } else {
                            currentHistory.removeAt(indexToRemove)
                        }
                    }
                    observeSearchHistory()

                    viewModel.deleteSearchHistory()
                    for (i in searchHistory.size - 1 downTo 0) {
                        viewModel.insertSearchHistory(searchHistory[i])
                    }
                }
            },
        )
        binding.btClearSearchHistory.setOnClickListener {
            clearSearchHistory()
        }
        binding.refreshSearch.setOnRefreshListener {
            val query = binding.svSearch.query.toString()
            if (binding.chipAll.isChecked) {
                fetchSearchAll(query)
            } else if (binding.chipVideo.isChecked) {
                fetchSearchVideos(query)
            } else if (binding.chipSong.isChecked) {
                fetchSearchSongs(query)
            } else if (binding.chipAlbum.isChecked) {
                fetchSearchAlbums(query)
            } else if (binding.chipArtists.isChecked) {
                fetchSearchArtists(query)
            } else if (binding.chipPlaylist.isChecked) {
                fetchSearchPlaylists(query)
            } else if (binding.chipFeaturedPlaylist.isChecked) {
                fetchSearchFeaturedPlaylists(query)
            } else if (binding.chipPodcast.isChecked) {
                fetchSearchPodcasts(query)
            }
        }
        binding.chipGroupTypeSearch.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(binding.chipSong.id) -> viewModel.searchType.value = "songs"
                checkedIds.contains(binding.chipVideo.id) -> viewModel.searchType.value = "videos"
                checkedIds.contains(binding.chipAlbum.id) -> viewModel.searchType.value = "albums"
                checkedIds.contains(binding.chipArtists.id) -> viewModel.searchType.value = "artists"
                checkedIds.contains(binding.chipPlaylist.id) -> viewModel.searchType.value = "playlists"
                checkedIds.contains(binding.chipFeaturedPlaylist.id) -> viewModel.searchType.value = "featured_playlists"
                checkedIds.contains(binding.chipPodcast.id) -> viewModel.searchType.value = "podcasts"
                else -> viewModel.searchType.value = "all"
            }
            updateResultsAdapter()
        }
        suggestAdapter.setOnClickListener(
            object : SuggestQueryAdapter.onItemClickListener {
                override fun onItemClick(position: Int) {
                    binding.svSearch.setQuery(suggestAdapter.getCurrentList()[position], true)
                }
            },
        )
        suggestAdapter.setOnCopyClickListener(
            object : SuggestQueryAdapter.OnCopyClickListener {
                override fun onCopyClick(position: Int) {
                    binding.svSearch.setQuery(suggestAdapter.getCurrentList()[position], false)
                }
            },
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val job1 =
                    launch {
                        sharedViewModel.downloadList.collect {
                            resultAdapter.setDownloadedList(it)
                        }
                    }
                val job2 =
                    launch {
                        combine(
                            sharedViewModel.nowPlayingState.distinctUntilChangedBy {
                                it?.songEntity?.videoId
                            },
                            sharedViewModel.controllerState.distinctUntilChangedBy {
                                it.isPlaying
                            },
                        ) { nowPlaying, controllerState ->
                            Pair(nowPlaying, controllerState)
                        }.collect {
                            val songEntity = it.first?.songEntity
                            if (songEntity != null && it.second.isPlaying) {
                                resultAdapter.setNowPlaying(songEntity.videoId)
                            } else {
                                resultAdapter.setNowPlaying(null)
                            }
                        }
                    }
                val job3 =
                    launch {
                        viewModel.loading.observe(viewLifecycleOwner) { loading ->
                            if (loading) {
                                if (binding.chipGroupTypeSearch.isEnabled) {
                                    setEnabledAll(binding.chipGroupTypeSearch, false)
                                }
                            } else {
                                if (!binding.chipGroupTypeSearch.isEnabled) {
                                    setEnabledAll(binding.chipGroupTypeSearch, true)
                                }
                            }
                        }
                    }
                job1.join()
                job2.join()
                job3.join()
            }
        }
    }

    private fun observeSearchHistory() {
        viewModel.searchHistory.observe(viewLifecycleOwner) { searchHistoryList ->
            if (searchHistoryList != null) {
                searchHistory.clear()
                for (i in searchHistoryList.size - 1 downTo 0) {
                    searchHistory.add(searchHistoryList[i])
                }
                searchHistoryAdapter.updateData(searchHistory)
            }
        }
    }

    private fun fetchSearchVideos(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.refreshSearch.isRefreshing = true
        viewModel.searchVideos(query)
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.videoSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let {
                                val resultList : ArrayList<Any> = arrayListOf()
                                if (it != null) {
                                    for (i in it) {
                                        resultList += i
                                    }
                                }
                                if (viewModel.searchType.value == "videos") {
                                    resultAdapter.updateList(resultList)
                                    binding.refreshSearch.isRefreshing = false
                                    binding.shimmerLayout.stopShimmer()
                                    binding.shimmerLayout.visibility = View.GONE
                                    setEnabledAll(binding.chipGroupTypeSearch, true)
                                    binding.resultList.smoothScrollToPosition(0)
                                }
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar
                                    .make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction("Retry") {
                                        fetchSearchVideos(query)
                                    }.setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.refreshSearch.isRefreshing = false
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                        }
                    }
                }
            }
        }
    }

    private fun fetchSearchAlbums(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.refreshSearch.isRefreshing = true
        viewModel.searchAlbums(query)
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.albumsSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let {
                                Log.d("SearchFragment", "observeAlbumList: $it")
                                val resultList: ArrayList<Any> = arrayListOf()
                                if (it != null) {
                                    for (i in it) {
                                        resultList += i
                                    }
                                }
                                if (viewModel.searchType.value == "albums") {
                                    resultAdapter.updateList(resultList)
                                    binding.refreshSearch.isRefreshing = false
                                    binding.shimmerLayout.stopShimmer()
                                    binding.shimmerLayout.visibility = View.GONE
                                    setEnabledAll(binding.chipGroupTypeSearch, true)
                                    binding.resultList.smoothScrollToPosition(0)
                                }
                            }
                        }

                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar
                                    .make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.retry)) {
                                        fetchSearchAlbums(query)
                                    }.setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.refreshSearch.isRefreshing = false
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                        }
                    }
                }
            }
        }
    }

    private fun fetchSearchFeaturedPlaylists(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.refreshSearch.isRefreshing = true
        viewModel.searchFeaturedPlaylist(query)
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.featuredPlaylistSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { playlistsResultArrayList ->
                                Log.d(
                                    "SearchFragment",
                                    "observePlaylistsList: $playlistsResultArrayList",
                                )
                                val resultList : ArrayList<Any> = arrayListOf()
                                if (playlistsResultArrayList != null) {
                                    for (i in playlistsResultArrayList) {
                                        resultList += i
                                    }
                                }
                                if (viewModel.searchType.value == "featured_playlists") {
                                    resultAdapter.updateList(resultList)
                                    binding.refreshSearch.isRefreshing = false
                                    binding.shimmerLayout.stopShimmer()
                                    binding.shimmerLayout.visibility = View.GONE
                                    setEnabledAll(binding.chipGroupTypeSearch, true)
                                    binding.resultList.smoothScrollToPosition(0)
                                }
                            }
                        }

                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar
                                    .make(
                                        requireActivity().findViewById(R.id.mini_player_container),
                                        message,
                                        Snackbar.LENGTH_LONG,
                                    ).setAction(R.string.retry) {
                                        fetchSearchFeaturedPlaylists(query)
                                    }.setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.refreshSearch.isRefreshing = false
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                        }
                    }
                }
            }
        }
    }

    private fun fetchSearchPodcasts(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.refreshSearch.isRefreshing = true
        viewModel.searchPodcast(query)
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.podcastSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { playlistsResultArrayList ->
                                Log.d("SearchFragment", "podcast: $playlistsResultArrayList")
                                val resultList : ArrayList<Any> = arrayListOf()
                                if (playlistsResultArrayList != null) {
                                    for (i in playlistsResultArrayList) {
                                        resultList += i
                                    }
                                }
                                if (viewModel.searchType.value == "podcasts") {
                                    resultAdapter.updateList(resultList)
                                    binding.refreshSearch.isRefreshing = false
                                    binding.shimmerLayout.stopShimmer()
                                    binding.shimmerLayout.visibility = View.GONE
                                    setEnabledAll(binding.chipGroupTypeSearch, true)
                                    binding.resultList.smoothScrollToPosition(0)
                                }
                            }
                        }

                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar
                                    .make(
                                        requireActivity().findViewById(R.id.mini_player_container),
                                        message,
                                        Snackbar.LENGTH_LONG,
                                    ).setAction(R.string.retry) {
                                        fetchSearchPodcasts(query)
                                    }.setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.refreshSearch.isRefreshing = false
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                        }
                    }
                }
            }
        }
    }

    private fun fetchSearchPlaylists(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.refreshSearch.isRefreshing = true
        viewModel.searchPlaylists(query)
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.playlistSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { playlistsResultArrayList ->
                                Log.d(
                                    "SearchFragment",
                                    "observePlaylistsList: $playlistsResultArrayList",
                                )
                                val resultList : ArrayList<Any> = arrayListOf()
                                if (playlistsResultArrayList != null) {
                                    for (i in playlistsResultArrayList) {
                                        resultList += i
                                    }
                                }
                                if (viewModel.searchType.value == "playlists") {
                                    resultAdapter.updateList(resultList)
                                    binding.refreshSearch.isRefreshing = false
                                    binding.shimmerLayout.stopShimmer()
                                    binding.shimmerLayout.visibility = View.GONE
                                    setEnabledAll(binding.chipGroupTypeSearch, true)
                                    binding.resultList.smoothScrollToPosition(0)
                                }
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar
                                    .make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(R.string.retry) {
                                        fetchSearchPlaylists(query)
                                    }.setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.refreshSearch.isRefreshing = false
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                        }
                    }
                }
            }
        }
    }

    private fun fetchSearchArtists(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.refreshSearch.isRefreshing = true
        viewModel.searchArtists(query)
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.artistsSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { artistsResultArrayList ->
                                Log.d(
                                    "SearchFragment",
                                    "observeArtistList: $artistsResultArrayList",
                                )
                                val resultList : ArrayList<Any> = arrayListOf()
                                if (artistsResultArrayList != null) {
                                    for (i in artistsResultArrayList) {
                                        resultList += i
                                    }
                                }
                                if (viewModel.searchType.value == "artists") {
                                    resultAdapter.updateList(resultList)
                                    binding.refreshSearch.isRefreshing = false
                                    binding.shimmerLayout.stopShimmer()
                                    binding.shimmerLayout.visibility = View.GONE
                                    setEnabledAll(binding.chipGroupTypeSearch, true)
                                    binding.resultList.smoothScrollToPosition(0)
                                }
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar
                                    .make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(R.string.retry) {
                                        fetchSearchArtists(query)
                                    }.setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.refreshSearch.isRefreshing = false
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                        }
                    }
                }
            }
        }
    }

    private fun fetchSearchSongs(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.refreshSearch.isRefreshing = true
        viewModel.searchSongs(query)
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.songsSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { songsResultArrayList ->
                                Log.d("SearchFragment", "observeSongList: $songsResultArrayList")
                                val resultList : ArrayList<Any> = arrayListOf()
                                if (songsResultArrayList != null) {
                                    for (i in songsResultArrayList) {
                                        resultList += i
                                    }
                                }
                                if (viewModel.searchType.value == "songs") {
                                    resultAdapter.updateList(resultList)
                                    binding.refreshSearch.isRefreshing = false
                                    binding.shimmerLayout.stopShimmer()
                                    binding.shimmerLayout.visibility = View.GONE
                                    setEnabledAll(binding.chipGroupTypeSearch, true)
                                    binding.resultList.smoothScrollToPosition(0)
                                }
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar
                                    .make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.retry)) {
                                        fetchSearchSongs(query)
                                    }.setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(5000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.refreshSearch.isRefreshing = false
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                        }
                    }
                }
            }
        }
    }
    private fun fetchSearchAll(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.refreshSearch.isRefreshing = true
        viewModel.searchAll(query)
    }

    private fun fetchSuggestList(query: String) {
        viewModel.suggestQuery(query)
        viewModel.suggestQuery.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let {
                        Log.d("Suggest", it.toString())
                        viewModel.suggestList.postValue(ArrayList(it.queries))
                        viewModel.suggestYTItemList.postValue(ArrayList(it.recommendedItems))
                        val suggestList : ArrayList<String> = arrayListOf()
                        suggestList.addAll(it.queries)
                        val suggestYTItemList : ArrayList<YTItem> = arrayListOf()
                        suggestYTItemList.addAll(it.recommendedItems)
                        suggestYTItemAdapter.updateList(suggestYTItemList)
                        suggestAdapter.updateData(suggestList)
                    }
                }
                is Resource.Error -> {
                }
            }
        }
    }

    private fun clearSearchHistory() {
        viewModel.searchHistory.value?.clear()
        searchHistory.clear()
        searchHistoryAdapter.updateData(searchHistory)
        viewModel.deleteSearchHistory()
    }
}

@Composable
@IntermediaryMigrateApi
fun SearchFragmentComposable() {
    AndroidViewBinding(FragmentSearchBinding::inflate)
}