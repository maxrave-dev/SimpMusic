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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.adapter.search.SearchHistoryItemAdapter
import com.maxrave.simpmusic.adapter.search.SearchItemAdapter
import com.maxrave.simpmusic.adapter.search.SuggestQueryAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
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
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.service.test.source.MusicSource
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment : Fragment() {
    @Inject
    lateinit var musicSource: MusicSource

    @Inject
    lateinit var application: Application

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    //Data Saved
    private lateinit var searchHistory: ArrayList<String>
    private lateinit var resultList: ArrayList<Any>
    private lateinit var suggestList: ArrayList<String>
    private lateinit var searchAllResult: ArrayList<Any>

    private val viewModel by viewModels<SearchViewModel>()
    private lateinit var resultAdapter: SearchItemAdapter
    private lateinit var searchHistoryAdapter: SearchHistoryItemAdapter
    private lateinit var suggestAdapter: SuggestQueryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d("SearchFragment", "onCreateView")
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        binding.svSearch.applyInsetter {
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
                    setEnabledAll(binding.chipGroupTypeSearch, false)
                    binding.svSearch.clearFocus()
                    binding.suggestList.visibility = View.GONE
                    binding.recentlyQueryView.visibility = View.GONE
                    binding.defaultLayout.visibility = View.GONE
                    binding.resultView.visibility = View.VISIBLE
//                    if (!searchHistory.contains(query)){
//                        viewModel.insertSearchHistory(query)
//                        viewModel.getSearchHistory()
//                        observeSearchHistory()
//                    }
                    viewModel.insertSearchHistory(query)
                    viewModel.getSearchHistory()
                    observeSearchHistory()
                    Log.d("Check History", searchHistory.toString())
                    viewModel.searchHistory.postValue(searchHistory)
                    searchHistoryAdapter.updateData(searchHistory)
                    when (viewModel.searchType.value) {
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
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty())
                {
                    binding.suggestList.visibility = View.GONE
                    binding.recentlyQueryView.visibility = View.VISIBLE
                    binding.resultView.visibility = View.GONE
                    binding.defaultLayout.visibility = View.GONE
                }
                else {
                    binding.suggestList.visibility = View.VISIBLE
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
        searchHistoryAdapter = SearchHistoryItemAdapter(arrayListOf())
        binding.recentlyList.apply {
            adapter = searchHistoryAdapter
            layoutManager = LinearLayoutManager(context)
        }
        observeSearchHistory()
        suggestAdapter = SuggestQueryAdapter(arrayListOf())
        binding.suggestList.apply {
            adapter = suggestAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.resultList.apply {
            adapter = resultAdapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.suggestList.visibility = View.GONE
        if (viewModel.searchAllResult.value == null || viewModel.searchAllResult.value!!.isEmpty()){
            Log.d("SearchFragment Dòng 92", "viewModel.searchAllResult.value == null")
            if (searchHistory.isEmpty()) {
                Log.d("SearchFragment Dòng 94", "searchHistory.isEmpty()")
                binding.recentlyQueryView.visibility = View.GONE
                binding.defaultLayout.visibility = View.VISIBLE
            }
            else {
                Log.d("SearchFragment Dòng 98", "searchHistory.isNotEmpty()")
                binding.recentlyQueryView.visibility = View.VISIBLE
                binding.defaultLayout.visibility = View.GONE
                binding.resultView.visibility = View.GONE
            }
        }
        else {
            searchAllResult.addAll(viewModel.searchAllResult.value!!)
            Log.d("SearchFragment", "searchAllResult: $searchAllResult")
            resultAdapter.updateList(searchAllResult)
            binding.recentlyQueryView.visibility = View.GONE
            binding.defaultLayout.visibility = View.GONE
            binding.resultView.visibility = View.VISIBLE
        }

        binding.svSearch.setOnQueryTextFocusChangeListener{ v, hasFocus ->
            if (hasFocus){
                Log.d("Check History in ViewModel", viewModel.searchHistory.value.toString())
                viewModel.searchHistory.observe(viewLifecycleOwner){ history ->
                    searchHistoryAdapter.updateData(searchHistory)
                }
            }
        }

        resultAdapter.setOnClickListener(object : SearchItemAdapter.onItemClickListener{
            override fun onItemClick(position: Int, type: String) {
                if (type == "artist"){
                    val channelId = (resultAdapter.getCurrentList()[position] as ArtistsResult).browseId
                    val args = Bundle()
                    args.putString("channelId", channelId)
                    findNavController().navigate(R.id.action_bottom_navigation_item_search_to_artistFragment, args)
                }
                if (type == Config.ALBUM_CLICK){
                    val browseId = (resultAdapter.getCurrentList()[position] as AlbumsResult).browseId
                    val args = Bundle()
                    args.putString("browseId", browseId)
                    findNavController().navigate(R.id.action_global_albumFragment, args)
                }
                if (type == Config.PLAYLIST_CLICK){
                    val id = (resultAdapter.getCurrentList()[position] as PlaylistsResult).browseId
                    val args = Bundle()
                    args.putString("id", id)
                    findNavController().navigate(R.id.action_global_playlistFragment, args)
                }
                if (type == Config.SONG_CLICK){
                    val songClicked = resultAdapter.getCurrentList()[position] as SongsResult
                    val videoId = (resultAdapter.getCurrentList()[position] as SongsResult).videoId
                    Queue.clear()
                    val firstQueue: Track = songClicked.toTrack()
                    Queue.setNowPlaying(firstQueue)
                    val args = Bundle()
                    args.putString("videoId", videoId)
                    args.putString("from", "\"${binding.svSearch.query}\" in Search")
                    args.putString("type", Config.SONG_CLICK)
                    findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
                }
                if (type == Config.VIDEO_CLICK) {
                    val videoClicked = resultAdapter.getCurrentList()[position] as VideosResult
                    val videoId = videoClicked.videoId
                    Queue.clear()
                    val firstQueue = videoClicked.toTrack()
                    Queue.setNowPlaying(firstQueue)
                    val args = Bundle()
                    args.putString("videoId", videoId)
                    args.putString("from", "\"${binding.svSearch.query}\" in Search")
                    args.putString("type", Config.VIDEO_CLICK)
                    findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
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
                            if (songEntity.liked){
                                tvFavorite.text = getString(R.string.liked)
                                cbFavorite.isChecked = true
                            }
                            else {
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
                                    setEnabledAll(btDownload, false)
                                }
                                DownloadState.STATE_DOWNLOADED -> {
                                    tvDownload.text = getString(R.string.downloaded)
                                    ivDownload.setImageResource(R.drawable.baseline_downloaded)
                                    setEnabledAll(btDownload, true)
                                }
                                DownloadState.STATE_PREPARING -> {
                                    tvDownload.text = getString(R.string.preparing)
                                    ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                                    setEnabledAll(btDownload, false)
                                }
                            }
                        }
                        tvSongTitle.text = track.title
                        tvSongTitle.isSelected = true
                        tvSongArtist.text = track.artists.toListName().connectArtists()
                        tvSongArtist.isSelected = true
                        ivThumbnail.load(track.thumbnails?.last()?.url)

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
                                            findNavController().navigate(R.id.action_global_artistFragment, Bundle().apply {
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
                            val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ URL")
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
                                    if (playlist.tracks != null) {
                                        tempTrack.addAll(playlist.tracks)
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
                                                    setEnabledAll(btDownload, false)
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
                                                        "Download failed",
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
                                                        "Download completed",
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
                            else if (tvDownload.text == getString(R.string.downloaded)){
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
                searchHistoryAdapter.updateData(searchHistory)
            }
        })
        binding.btClearSearchHistory.setOnClickListener {
            viewModel.searchHistory.value?.clear()
            searchHistory.clear()
            searchHistoryAdapter.updateData(searchHistory)
            viewModel.deleteSearchHistory()
        }
        binding.refreshSearch.setOnRefreshListener {
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
            else if (binding.chipPlaylist.isChecked){
                fetchSearchPlaylists(binding.svSearch.query.toString())
            }
        }
        binding.chipGroupTypeSearch.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.contains(binding.chipSong.id))
            {
                resultList.clear()
                val temp = viewModel.songsSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
                viewModel.searchType.postValue("songs")
            }
            else if (checkedIds.contains(binding.chipVideo.id)){
                resultList.clear()
                val temp = viewModel.videoSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
                viewModel.searchType.postValue("videos")
            }
            else if (checkedIds.contains(binding.chipAll.id))
            {
                resultList.clear()
                resultList.addAll(searchAllResult)
                resultAdapter.updateList(resultList)
                viewModel.searchType.postValue("all")
            }
            else if (checkedIds.contains(binding.chipAlbum.id))
            {
                resultList.clear()
                val temp = viewModel.albumsSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
                viewModel.searchType.postValue("albums")
            }
            else if (checkedIds.contains(binding.chipArtists.id))
            {
                resultList.clear()
                val temp = viewModel.artistsSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
                viewModel.searchType.postValue("artists")
            }
            else if (checkedIds.contains(binding.chipPlaylist.id))
            {
                resultList.clear()
                val temp = viewModel.playlistSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
                viewModel.searchType.postValue("playlists")
            }
        }
        suggestAdapter.setOnClickListener(object: SuggestQueryAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                binding.svSearch.setQuery(suggestList[position], false)
            }
        })
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
                        }
                    }
                    is Resource.Error -> {
                        response.message?.let { message ->
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                .setAction("Retry") {
                                    fetchSearchVideos(query)
                                }
                                .setDuration(5000)
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
                        }
                    }
                    is Resource.Error -> {
                        response.message?.let { message ->
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                .setAction("Retry") {
                                    fetchSearchAlbums(query)
                                }
                                .setDuration(5000)
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
    private fun fetchSearchPlaylists(query: String) {
        setEnabledAll(binding.chipGroupTypeSearch, false)
        binding.refreshSearch.isRefreshing = true
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        viewModel.searchPlaylists(query)
        viewModel.loading.observe(viewLifecycleOwner){
            if (it == false){
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
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                    .setAction("Retry") {
                                        fetchSearchPlaylists(query)
                                    }
                                    .setDuration(5000)
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
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                    .setAction("Retry") {
                                        fetchSearchArtists(query)
                                    }
                                    .setDuration(5000)
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
                            }
                        }
                        is Resource.Error -> {
                            response.message?.let { message ->
                                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                    .setAction("Retry") {
                                        fetchSearchSongs(query)
                                    }
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
        val temp: ArrayList<Any> = ArrayList()
        viewModel.loading.observe(viewLifecycleOwner){ it ->
            if (it == false){
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
                                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                    .setAction("Retry") {
                                        fetchSearchAll(query)
                                    }
                                    .setDuration(5000)
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
                                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                    .setAction("Retry") {
                                        fetchSearchAll(query)
                                    }
                                    .setDuration(5000)
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
                                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                    .setAction("Retry") {
                                        fetchSearchAll(query)
                                    }
                                    .setDuration(5000)
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
                                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                    .setAction("Retry") {
                                        fetchSearchAll(query)
                                    }
                                    .setDuration(5000)
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
                                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                    .setAction("Retry") {
                                        fetchSearchAll(query)
                                    }
                                    .setDuration(5000)
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
                                for (i in 0..2)
                                {
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
                                for (i in 0 until album.size)
                                {
                                    temp += album[i]
                                }
                                for (i in 0 until playlist.size)
                                {
                                    temp += playlist[i]
                                }
                            }
                            else {
                                temp.addAll(song)
                                temp.addAll(video)
                                temp.addAll(artist)
                                temp.addAll(album)
                                temp.addAll(playlist)
                            }
                        }
                        catch (e: Exception){
                            Snackbar.make(binding.root, e.message.toString(), Snackbar.LENGTH_LONG)
                                .setAction("Retry") {
                                    fetchSearchAll(query)
                                }
                                .setDuration(5000)
                                .show()
                        }
//                        val sortedList = temp.sortedWith(compareByDescending<Any>
//                            {compareQuery ->
//                                    when (compareQuery){
//                                    is SongsResult -> compareQuery.title?.lowercase()?.contains(binding.svSearch.query.toString().lowercase())
//                                    is VideosResult -> compareQuery.title.lowercase().contains(binding.svSearch.query.toString().lowercase())
//                                    is AlbumsResult -> compareQuery.title.lowercase().contains(binding.svSearch.query.toString().lowercase())
//                                    is ArtistsResult -> compareQuery.artist.lowercase().contains(binding.svSearch.query.toString().lowercase())
//                                    is PlaylistsResult -> compareQuery.title.lowercase().contains(binding.svSearch.query.toString().lowercase())
//                                    else -> ""
//                                }
//                            }
//                            .thenBy {compareQuery ->
//                                when (compareQuery){
//                                    is SongsResult -> compareQuery.title?.length
//                                    is VideosResult -> compareQuery.title.length
//                                    is AlbumsResult -> compareQuery.title.length
//                                    is ArtistsResult -> compareQuery.artist.length
//                                    is PlaylistsResult -> compareQuery.title.length
//                                    else -> ""
//                                }  }
//                            .thenBy { compareQuery ->
//                                when (compareQuery){
//                                    is SongsResult -> compareQuery.title
//                                    is VideosResult -> compareQuery.title
//                                    is AlbumsResult -> compareQuery.title
//                                    is ArtistsResult -> compareQuery.artist
//                                    is PlaylistsResult -> compareQuery.title
//                                    else -> ""
//                                }
//                            }
//                        )
//                        temp.clear()
//                        temp.addAll(sortedList)
                        resultList.clear()
                        viewModel.searchAllResult.postValue(temp)
                        searchAllResult.addAll(temp)
                        resultList.addAll(temp)
                        resultAdapter.updateList(resultList)
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        binding.refreshSearch.isRefreshing = false
                        setEnabledAll(binding.chipGroupTypeSearch, true)
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
                        print(it)
                        suggestList.clear()
                        suggestList.addAll(it!!)
                        suggestAdapter.updateData(suggestList)
                    }
                }
                is Resource.Error -> {

                }
            }
        }
    }
}