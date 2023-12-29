package com.maxrave.simpmusic.ui.fragment

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
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
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentSearchBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SearchViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment() {

    @Inject
    lateinit var application: Application

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    //Data Saved
    private lateinit var searchHistory: ArrayList<String>
    private lateinit var resultList: ArrayList<Any>
    private lateinit var suggestList: ArrayList<String>
    private lateinit var searchAllResult: ArrayList<Any>
    private lateinit var suggestYTItemList: ArrayList<YTItem>

    private val viewModel by activityViewModels<SearchViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private lateinit var resultAdapter: SearchItemAdapter
    private lateinit var searchHistoryAdapter: SearchHistoryItemAdapter
    private lateinit var suggestAdapter: SuggestQueryAdapter
    private lateinit var suggestYTItemAdapter: SuggestYTItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
        if (viewModel.loading.value == true){
            binding.shimmerLayout.startShimmer()
            binding.shimmerLayout.visibility = View.VISIBLE
        }
        else {
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.visibility = View.GONE
        }
        Log.d("SearchFragment", "onResume")
//        binding.svSearch.setQuery("", false)
//        binding.svSearch.clearFocus()
        binding.svSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
//                    fetchSearchAll(query)
                    resultList.clear()
                    resultAdapter.updateList(resultList)
                    setEnabledAll(binding.chipGroupTypeSearch, false)
                    binding.svSearch.clearFocus()
                    binding.suggestList.visibility = View.GONE
                    binding.suggestListYtItem.visibility = View.GONE
                    binding.recentlyQueryView.visibility = View.GONE
                    binding.defaultLayout.visibility = View.GONE
                    binding.resultView.visibility = View.VISIBLE
                    viewModel.insertSearchHistory(query)
                    viewModel.getSearchHistory()
                    observeSearchHistory()
                    Log.d("Check History", searchHistory.toString())
                    viewModel.searchHistory.postValue(searchHistory)
                    searchHistoryAdapter.updateData(searchHistory)
                    viewModel.searchType.value.let {searchType ->
                        when (searchType) {
                            "all" -> {
                                resultList.clear()
                                resultAdapter.updateList(resultList)
                                fetchSearchAll(query)
                                Log.d("Check All", "All is checked")
                            }
                            "videos" -> {
                                resultList.clear()
                                resultAdapter.updateList(resultList)
                                fetchSearchVideos(query)
                                binding.chipGroupTypeSearch.isClickable = true
                                Log.d("Check Video", "Video is checked")
                            }
                            "songs" -> {
                                resultList.clear()
                                Log.d("Check ResultList", resultList.toString())
                                resultAdapter.updateList(resultList)
                                fetchSearchSongs(query)
                                binding.chipGroupTypeSearch.isClickable = true
                                Log.d("Check Song", "Song is checked")
                            }
                            "albums" -> {
                                resultList.clear()
                                resultAdapter.updateList(resultList)
                                fetchSearchAlbums(query)
                                binding.chipGroupTypeSearch.isClickable = true
                                Log.d("Check Album", "Album is checked")
                            }
                            "artists" -> {
                                resultList.clear()
                                resultAdapter.updateList(resultList)
                                fetchSearchArtists(query)
                                binding.chipGroupTypeSearch.isClickable = true
                                Log.d("Check Artist", "Artist is checked")
                            }

                            "playlists" -> {
                                resultList.clear()
                                resultAdapter.updateList(resultList)
                                fetchSearchPlaylists(query)
                                binding.chipGroupTypeSearch.isClickable = true
                                Log.d("Check Playlist", "Playlist is checked")
                            }

                            "featured_playlists" -> {
                                resultList.clear()
                                resultAdapter.updateList(resultList)
                                fetchSearchFeaturedPlaylists(query)
                                binding.chipGroupTypeSearch.isClickable = true
                                Log.d("Check Search Type", "Search Type is featured_playlists")
                            }

                            "podcasts" -> {
                                resultList.clear()
                                resultAdapter.updateList(resultList)
                                fetchSearchPodcasts(query)
                                binding.chipGroupTypeSearch.isClickable = true
                                Log.d("Check Search Type", "Search Type is featured_playlists")
                                Log.d("Check Search Type", "Search Type is podcasts")
                            }

                            else -> {
                                Log.d("Check Search Type", "Search Type is null")
                            }
                        }
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty())
                {
                    observeSearchHistory()
                    binding.suggestList.visibility = View.GONE
                    binding.suggestListYtItem.visibility = View.GONE
                    binding.recentlyQueryView.visibility = View.VISIBLE
                    binding.resultView.visibility = View.GONE
                    binding.defaultLayout.visibility = View.GONE
                }
                else {
                    binding.suggestList.visibility = View.VISIBLE
                    binding.suggestListYtItem.visibility = View.VISIBLE
                    binding.recentlyQueryView.visibility = View.GONE
                    binding.defaultLayout.visibility = View.GONE
                    binding.resultView.visibility = View.GONE
                    Log.d("Gọi suggest", "onQueryTextChange: $newText")
                    fetchSuggestList(newText)
                }
                return false
            }

        })
    }
    override fun onDestroyView() {
        super.onDestroyView()
//        binding.svSearch.setQuery("", false)
//        binding.svSearch.clearFocus()
        _binding = null
        Log.d("Xoá Fragment", "onDestroyView")
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getSearchHistory()
        Log.d("SearchFragment", "onViewCreated")
        Log.d("SearchFragment", "viewModel.searchAllResult.value: ${viewModel.searchAllResult.value}")
        resultList = ArrayList<Any>()
        searchAllResult = ArrayList<Any>()
        resultAdapter = SearchItemAdapter(arrayListOf(), requireContext())
        searchHistory = ArrayList()
        suggestList = ArrayList<String>()
        suggestYTItemList = ArrayList<YTItem>()
        searchHistoryAdapter = SearchHistoryItemAdapter(arrayListOf())
        binding.recentlyList.apply {
            adapter = searchHistoryAdapter
            layoutManager = LinearLayoutManager(context)
        }
        suggestAdapter = SuggestQueryAdapter(arrayListOf())
        suggestYTItemAdapter = SuggestYTItemAdapter(arrayListOf(), requireContext())
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

        binding.suggestList.visibility = View.GONE
        binding.suggestListYtItem.visibility = View.GONE
        if (viewModel.searchAllResult.value == null || viewModel.searchAllResult.value!!.isEmpty()){
            if (searchHistory.isEmpty()) {
                binding.recentlyQueryView.visibility = View.GONE
                binding.defaultLayout.visibility = View.VISIBLE
            }
            else {
                binding.recentlyQueryView.visibility = View.VISIBLE
                binding.defaultLayout.visibility = View.GONE
                binding.resultView.visibility = View.GONE
            }
        }
        else {
            searchAllResult.addAll(viewModel.searchAllResult.value!!)
            resultAdapter.updateList(searchAllResult)
            binding.recentlyQueryView.visibility = View.GONE
            binding.defaultLayout.visibility = View.GONE
            binding.resultView.visibility = View.VISIBLE
        }

        binding.svSearch.setOnQueryTextFocusChangeListener{ v, hasFocus ->
            if (hasFocus){
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
        suggestYTItemAdapter.setOnClickListener(object : SuggestYTItemAdapter.onItemClickListener{
            override fun onItemClick(position: Int, type: String) {
                if (type == "artist"){
                    val channelId = (suggestYTItemAdapter.getCurrentList()[position] as ArtistItem).id
                    val args = Bundle()
                    args.putString("channelId", channelId)
                    findNavController().navigateSafe(R.id.action_bottom_navigation_item_search_to_artistFragment, args)
                }
                if (type == Config.ALBUM_CLICK){
                    val browseId = (suggestYTItemAdapter.getCurrentList()[position] as AlbumItem).browseId
                    val args = Bundle()
                    args.putString("browseId", browseId)
                    findNavController().navigateSafe(R.id.action_global_albumFragment, args)
                }
                if (type == Config.PLAYLIST_CLICK){
                    val id = (suggestYTItemAdapter.getCurrentList()[position] as PlaylistItem).id
                    val args = Bundle()
                    args.putString("id", id)
                    findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
                }
                if (type == Config.SONG_CLICK){
                    val songClicked = suggestYTItemAdapter.getCurrentList()[position] as SongItem
                    val videoId = (suggestYTItemAdapter.getCurrentList()[position] as SongItem).id
                    Queue.clear()
                    val firstQueue: Track = songClicked.toTrack()
                    Queue.setNowPlaying(firstQueue)
                    val args = Bundle()
                    args.putString("videoId", videoId)
                    args.putString("from", "\"${binding.svSearch.query}\" ${getString(R.string.in_search)}")
                    args.putString("type", Config.SONG_CLICK)
                    findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
                }
                if (type == Config.VIDEO_CLICK) {
                    val videoClicked = suggestYTItemAdapter.getCurrentList()[position] as VideoItem
                    val videoId = videoClicked.id
                    Queue.clear()
                    val firstQueue = videoClicked.toTrack()
                    Queue.setNowPlaying(firstQueue)
                    val args = Bundle()
                    args.putString("videoId", videoId)
                    args.putString("from", "\"${binding.svSearch.query}\" ${getString(R.string.in_search)}")
                    args.putString("type", Config.VIDEO_CLICK)
                    findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
                }
            }
        })

        resultAdapter.setOnClickListener(object : SearchItemAdapter.onItemClickListener{
            override fun onItemClick(position: Int, type: String) {
                if (type == "artist"){
                    val channelId = (resultAdapter.getCurrentList()[position] as ArtistsResult).browseId
                    val args = Bundle()
                    args.putString("channelId", channelId)
                    findNavController().navigateSafe(R.id.action_bottom_navigation_item_search_to_artistFragment, args)
                }
                if (type == Config.ALBUM_CLICK){
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
                if (type == Config.SONG_CLICK){
                    val songClicked = resultAdapter.getCurrentList()[position] as SongsResult
                    val videoId = (resultAdapter.getCurrentList()[position] as SongsResult).videoId
                    Queue.clear()
                    val firstQueue: Track = songClicked.toTrack()
                    Queue.setNowPlaying(firstQueue)
                    val args = Bundle()
                    args.putString("videoId", videoId)
                    args.putString("from", "\"${binding.svSearch.query}\" ${getString(R.string.in_search)}")
                    args.putString("type", Config.SONG_CLICK)
                    findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
                }
                if (type == Config.VIDEO_CLICK) {
                    val videoClicked = resultAdapter.getCurrentList()[position] as VideosResult
                    val videoId = videoClicked.videoId
                    Queue.clear()
                    val firstQueue = videoClicked.toTrack()
                    Queue.setNowPlaying(firstQueue)
                    val args = Bundle()
                    args.putString("videoId", videoId)
                    args.putString("from", "\"${binding.svSearch.query}\" ${getString(R.string.in_search)}")
                    args.putString("type", Config.VIDEO_CLICK)
                    findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
                }
            }

            @UnstableApi
            override fun onOptionsClick(position: Int, type: String) {
                    val song = if (type == Config.SONG_CLICK) {
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
                        btAddQueue.setOnClickListener {
                            sharedViewModel.addToQueue(track)
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
                            if (cbFavorite.isChecked){
                                cbFavorite.isChecked = false
                                tvFavorite.text = getString(R.string.like)
                                viewModel.updateLikeStatus(track.videoId, false)
                            }
                            else {
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
                                artistAdapter.setOnClickListener(object : SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                    override fun onItemClick(position: Int) {
                                        val artist = track.artists[position]
                                        if (artist.id != null) {
                                            findNavController().navigateSafe(R.id.action_global_artistFragment, Bundle().apply {
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
                                    viewModel.updateInLibrary(track.videoId)
                                    if (playlist.tracks != null) {
                                        tempTrack.addAll(playlist.tracks)
                                    }
                                    if (!tempTrack.contains(track.videoId) && playlist.syncedWithYouTubePlaylist == 1 && playlist.youtubePlaylistId != null) {
                                        viewModel.addToYouTubePlaylist(playlist.id, playlist.youtubePlaylistId, track.videoId)
                                    }
                                    if (!tempTrack.contains(track.videoId)) {
                                        viewModel.insertPairSongLocalPlaylist(
                                            PairSongLocalPlaylist(
                                            playlistId = playlist.id, songId = track.videoId, position = tempTrack.size, inPlaylist = LocalDateTime.now()
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
                            if (tvDownload.text == getString(R.string.download)){
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
                                                    tvDownload.text = getString(R.string.downloading)
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
                            }
                            else if (tvDownload.text == getString(R.string.downloaded) || tvDownload.text == getString(R.string.downloading)){
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

        })
        searchHistoryAdapter.setOnClickListener(object: SearchHistoryItemAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                binding.svSearch.setQuery(searchHistoryAdapter.getCurrentList()[position], true)
            }
        })
        searchHistoryAdapter.setOnDeleteClickListener(object: SearchHistoryItemAdapter.onDeleteClickListener{
            override fun onDeleteClick(position: Int) {
                searchHistory.removeAt(position)
                viewModel.searchHistory.value?.removeAt(position)
                observeSearchHistory()
            }
        })
        binding.btClearSearchHistory.setOnClickListener {
            viewModel.searchHistory.value?.clear()
            searchHistory.clear()
            searchHistoryAdapter.updateData(searchHistory)
            viewModel.deleteSearchHistory()
        }
        binding.refreshSearch.setOnRefreshListener {
            resultList.clear()
            resultAdapter.updateList(resultList)
            if (binding.chipAll.isChecked){
                fetchSearchAll(binding.svSearch.query.toString())
            }
            else if (binding.chipVideo.isChecked){
                fetchSearchVideos(binding.svSearch.query.toString())
            }
            else if (binding.chipSong.isChecked){
                fetchSearchSongs(binding.svSearch.query.toString())
            }
            else if (binding.chipAlbum.isChecked){
                fetchSearchAlbums(binding.svSearch.query.toString())
            }
            else if (binding.chipArtists.isChecked){
                fetchSearchArtists(binding.svSearch.query.toString())
            }
            else if (binding.chipPlaylist.isChecked) {
                fetchSearchPlaylists(binding.svSearch.query.toString())
            } else if (binding.chipFeaturedPlaylist.isChecked) {
                fetchSearchFeaturedPlaylists(binding.svSearch.query.toString())
            } else if (binding.chipPodcast.isChecked) {
                fetchSearchPodcasts(binding.svSearch.query.toString())
            }
        }
        binding.chipGroupTypeSearch.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.contains(binding.chipSong.id))
            {
                viewModel.searchType.postValue("songs")
                resultList.clear()
                val temp = viewModel.songsSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
            }
            else if (checkedIds.contains(binding.chipVideo.id)){
                viewModel.searchType.postValue("videos")
                resultList.clear()
                val temp = viewModel.videoSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
            }
            else if (checkedIds.contains(binding.chipAll.id))
            {
                viewModel.searchType.postValue("all")
                resultList.clear()
                resultList.addAll(searchAllResult)
                resultAdapter.updateList(resultList)
            }
            else if (checkedIds.contains(binding.chipAlbum.id))
            {
                viewModel.searchType.postValue("albums")
                resultList.clear()
                val temp = viewModel.albumsSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
            }
            else if (checkedIds.contains(binding.chipArtists.id))
            {
                viewModel.searchType.postValue("artists")
                resultList.clear()
                val temp = viewModel.artistsSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
            }
            else if (checkedIds.contains(binding.chipPlaylist.id)) {
                viewModel.searchType.postValue("playlists")
                resultList.clear()
                val temp = viewModel.playlistSearchResult.value?.data
                for (i in temp!!) {
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
            } else if (checkedIds.contains(binding.chipFeaturedPlaylist.id)) {
                viewModel.searchType.postValue("featured_playlists")
                resultList.clear()
                val temp = viewModel.featuredPlaylistSearchResult.value?.data
                for (i in temp!!) {
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
            } else if (checkedIds.contains(binding.chipPodcast.id)) {
                viewModel.searchType.postValue("podcasts")
                resultList.clear()
                val temp = viewModel.podcastSearchResult.value?.data
                for (i in temp!!) {
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
            }
        }
        suggestAdapter.setOnClickListener(object: SuggestQueryAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                binding.svSearch.setQuery(suggestList[position], false)
            }
        })
        lifecycleScope.launch {
            viewModel.loading.observe(viewLifecycleOwner) { loading ->
                if (loading) {
                    if (binding.chipGroupTypeSearch.isEnabled) {
                        setEnabledAll(binding.chipGroupTypeSearch, false)
                    }
                }
                else {
                    if (!binding.chipGroupTypeSearch.isEnabled) {
                        setEnabledAll(binding.chipGroupTypeSearch, true)
                    }
                }
            }
        }
    }

    private fun observeSearchHistory() {
        viewModel.searchHistory.observe(viewLifecycleOwner){ searchHistoryList ->
            if (searchHistoryList != null){
                searchHistory.clear()
                for (i in searchHistoryList.size - 1 downTo 0){
                    searchHistory.add(searchHistoryList[i])
                }
                searchHistoryAdapter.updateData(searchHistory)
            }
        }
    }

    private fun fetchSearchVideos(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.refreshSearch.isRefreshing = true
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        viewModel.searchVideos(query)
        viewModel.loading.observe(viewLifecycleOwner){
            viewModel.videoSearchResult.observe(viewLifecycleOwner){response ->
                when (response) {
                    is Resource.Success -> {
                        response.data.let {
                            resultList.clear()
                            if (it != null) {
                                for (i in it){
                                    resultList += i
                                }
                            }
                            resultAdapter.updateList(resultList)
                            binding.refreshSearch.isRefreshing = false
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                            binding.resultList.smoothScrollToPosition(0)
                        }
                    }
                    is Resource.Error -> {
                        response.message?.let { message ->
                            Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                .setAction("Retry") {
                                    fetchSearchVideos(query)
                                }
                                .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                .setDuration(3000)
                                .show()
                        }
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        setEnabledAll(binding.chipGroupTypeSearch, true)
                    }
                }
            }
        }
    }
    private fun fetchSearchAlbums(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.refreshSearch.isRefreshing = true
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        viewModel.searchAlbums(query)
        viewModel.loading.observe(viewLifecycleOwner){
            viewModel.albumsSearchResult.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data.let {
                            Log.d("SearchFragment", "observeAlbumList: $it")
                            resultList.clear()
                            if (it != null) {
                                for (i in it){
                                    resultList += i
                                }
                            }
                            resultAdapter.updateList(resultList)
                            binding.refreshSearch.isRefreshing = false
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                            binding.resultList.smoothScrollToPosition(0)
                        }
                    }
                    is Resource.Error -> {
                        response.message?.let { message ->
                            Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.retry)) {
                                    fetchSearchAlbums(query)
                                }
                                .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                .setDuration(3000)
                                .show()
                        }
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        setEnabledAll(binding.chipGroupTypeSearch, true)
                    }
                }
            }
        }
    }

    private fun fetchSearchFeaturedPlaylists(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.refreshSearch.isRefreshing = true
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        viewModel.searchFeaturedPlaylist(query)
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.featuredPlaylistSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { playlistsResultArrayList ->
                                Log.d(
                                    "SearchFragment",
                                    "observePlaylistsList: $playlistsResultArrayList"
                                )
                                resultList.clear()
                                if (playlistsResultArrayList != null) {
                                    for (i in playlistsResultArrayList) {
                                        resultList += i
                                    }
                                }
                                resultAdapter.updateList(resultList)
                                binding.shimmerLayout.stopShimmer()
                                binding.shimmerLayout.visibility = View.GONE
                                binding.refreshSearch.isRefreshing = false
                                setEnabledAll(binding.chipGroupTypeSearch, true)
                                binding.resultList.smoothScrollToPosition(0)
                            }
                        }

                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(
                                    requireActivity().findViewById(R.id.mini_player_container),
                                    message,
                                    Snackbar.LENGTH_LONG
                                )
                                    .setAction(R.string.retry) {
                                        fetchSearchFeaturedPlaylists(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                        }
                    }
                }
            }
        }
    }

    private fun fetchSearchPodcasts(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.refreshSearch.isRefreshing = true
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        viewModel.searchPodcast(query)
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.podcastSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { playlistsResultArrayList ->
                                Log.d("SearchFragment", "podcast: $playlistsResultArrayList")
                                resultList.clear()
                                if (playlistsResultArrayList != null) {
                                    for (i in playlistsResultArrayList) {
                                        resultList += i
                                    }
                                }
                                resultAdapter.updateList(resultList)
                                binding.shimmerLayout.stopShimmer()
                                binding.shimmerLayout.visibility = View.GONE
                                binding.refreshSearch.isRefreshing = false
                                setEnabledAll(binding.chipGroupTypeSearch, true)
                                binding.resultList.smoothScrollToPosition(0)
                            }
                        }

                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(
                                    requireActivity().findViewById(R.id.mini_player_container),
                                    message,
                                    Snackbar.LENGTH_LONG
                                )
                                    .setAction(R.string.retry) {
                                        fetchSearchPodcasts(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            setEnabledAll(binding.chipGroupTypeSearch, true)
                        }
                    }
                }
            }
        }
    }

    private fun fetchSearchPlaylists(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.refreshSearch.isRefreshing = true
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        viewModel.searchPlaylists(query)
        viewModel.loading.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.playlistSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { playlistsResultArrayList ->
                                Log.d("SearchFragment", "observePlaylistsList: $playlistsResultArrayList")
                                resultList.clear()
                                if (playlistsResultArrayList != null) {
                                    for (i in playlistsResultArrayList){
                                        resultList += i
                                    }
                                }
                                resultAdapter.updateList(resultList)
                                binding.shimmerLayout.stopShimmer()
                                binding.shimmerLayout.visibility = View.GONE
                                binding.refreshSearch.isRefreshing = false
                                setEnabledAll(binding.chipGroupTypeSearch, true)
                                binding.resultList.smoothScrollToPosition(0)
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(R.string.retry) {
                                        fetchSearchPlaylists(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
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
        viewModel.loading.observe(viewLifecycleOwner){
            if (it == false){
                viewModel.artistsSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { artistsResultArrayList ->
                                Log.d("SearchFragment", "observeArtistList: $artistsResultArrayList")
                                resultList.clear()
                                if (artistsResultArrayList != null) {
                                    for (i in artistsResultArrayList){
                                        resultList += i
                                    }
                                }
                                resultAdapter.updateList(resultList)
                                binding.shimmerLayout.stopShimmer()
                                binding.shimmerLayout.visibility = View.GONE
                                binding.refreshSearch.isRefreshing = false
                                setEnabledAll(binding.chipGroupTypeSearch, true)
                                binding.resultList.smoothScrollToPosition(0)
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(R.string.retry) {
                                        fetchSearchArtists(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
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
        viewModel.loading.observe(viewLifecycleOwner){
            if (it == false){
                viewModel.songsSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { songsResultArrayList ->
                                Log.d("SearchFragment", "observeSongList: $songsResultArrayList")
                                resultList.clear()
                                if (songsResultArrayList != null) {
                                    for (i in songsResultArrayList){
                                        resultList += i
                                    }
                                }
                                resultAdapter.updateList(resultList)
                                binding.shimmerLayout.stopShimmer()
                                binding.shimmerLayout.visibility = View.GONE
                                binding.refreshSearch.isRefreshing = false
                                setEnabledAll(binding.chipGroupTypeSearch, true)
                                binding.resultList.smoothScrollToPosition(0)
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.retry)) {
                                        fetchSearchSongs(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
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
        var song = ArrayList<SongsResult>()
        val video = ArrayList<VideosResult>()
        var album = ArrayList<AlbumsResult>()
        var artist = ArrayList<ArtistsResult>()
        var playlist = ArrayList<PlaylistsResult>()
        var featuredPlaylist = ArrayList<PlaylistsResult>()
        var podcast = ArrayList<PlaylistsResult>()
        val temp: ArrayList<Any> = ArrayList()
        viewModel.loading.observe(viewLifecycleOwner) { it ->
            if (it == false) {
                viewModel.songsSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { songsResultArrayList ->
                                Log.d("SearchFragment", "observeResultList: $songsResultArrayList")
                                song = songsResultArrayList!!
                            }
                        }

                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.retry)) {
                                        fetchSearchAll(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                        }
                    }
                }
                viewModel.videoSearchResult.observe(viewLifecycleOwner){ response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { videoResultArrayList ->
                                Log.d("SearchFragment", "observeResultList: $videoResultArrayList")
                                for (i in videoResultArrayList!!){
                                    video += i
                                }
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.retry)) {
                                        fetchSearchAll(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                        }
                    }
                }
                viewModel.albumsSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let {
                                print(it)
                                album = it!!
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.retry)) {
                                        fetchSearchAll(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                        }
                    }
                }
                viewModel.artistsSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { artistsResultArrayList ->
                                print(artistsResultArrayList)
                                artist = artistsResultArrayList!!
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.retry)) {
                                        fetchSearchAll(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                        }
                    }
                }
                viewModel.playlistSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { playlistsResultArrayList ->
                                print(playlistsResultArrayList)
                                playlist = playlistsResultArrayList!!
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), message, Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.retry)) {
                                        fetchSearchAll(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                        }
                    }
                }
                viewModel.featuredPlaylistSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { playlistsResultArrayList ->
                                print(playlistsResultArrayList)
                                featuredPlaylist = playlistsResultArrayList!!
                            }
                        }

                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(
                                    requireActivity().findViewById(R.id.mini_player_container),
                                    message,
                                    Snackbar.LENGTH_LONG
                                )
                                    .setAction(getString(R.string.retry)) {
                                        fetchSearchAll(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                        }
                    }
                }
                viewModel.podcastSearchResult.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data.let { playlistsResultArrayList ->
                                print(playlistsResultArrayList)
                                podcast = playlistsResultArrayList!!
                            }
                        }

                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(
                                    requireActivity().findViewById(R.id.mini_player_container),
                                    message,
                                    Snackbar.LENGTH_LONG
                                )
                                    .setAction(getString(R.string.retry)) {
                                        fetchSearchAll(query)
                                    }
                                    .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                    .setDuration(3000)
                                    .show()
                            }
                        }
                    }
                }
                viewModel.loading.observe(viewLifecycleOwner) { loading ->
                    if (loading) {
                        binding.refreshSearch.isRefreshing = true
                    } else {
                        try {
                            if (artist.size >= 3) {
                                for (i in 0..2) {
                                    temp += artist[i]
                                }
                                for (i in 0 until song.size)
                                {
                                    temp += song[i]
                                }
                                for (i in 0 until video.size)
                                {
                                    temp += video[i]
                                }
                                for (i in 0 until album.size) {
                                    temp += album[i]
                                }
                                for (i in 0 until playlist.size) {
                                    temp += playlist[i]
                                }
                                for (i in 0 until featuredPlaylist.size) {
                                    temp += featuredPlaylist[i]
                                }
                                for (i in 0 until podcast.size) {
                                    temp += podcast[i]
                                }
                            }
                            else {
                                temp.addAll(song)
                                temp.addAll(video)
                                temp.addAll(artist)
                                temp.addAll(album)
                                temp.addAll(playlist)
                                temp.addAll(featuredPlaylist)
                                temp.addAll(podcast)
                            }
                        }
                        catch (e: Exception){
                            Snackbar.make(requireActivity().findViewById(R.id.mini_player_container), e.message.toString(), Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.retry)) {
                                    fetchSearchAll(query)
                                }
                                .setAnchorView(activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view))
                                .setDuration(3000)
                                .show()
                        }
                        resultList.clear()
                        viewModel.searchAllResult.postValue(temp)
                        searchAllResult.addAll(temp)
                        resultList.addAll(temp)
                        resultAdapter.updateList(resultList)
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        binding.refreshSearch.isRefreshing = false
                        setEnabledAll(binding.chipGroupTypeSearch, true)
                        binding.resultList.smoothScrollToPosition(0)
                    }
                }
            }
        }
    }

    private fun fetchSuggestList(query: String) {
        viewModel.suggestQuery(query)
        viewModel.suggestQuery.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        Log.d("Suggest", it.toString())
                        suggestList.clear()
                        suggestList.addAll(it!!.queries)
                        suggestYTItemList.clear()
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
}