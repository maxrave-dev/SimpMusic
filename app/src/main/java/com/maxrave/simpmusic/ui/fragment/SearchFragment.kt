package com.maxrave.simpmusic.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.search.SearchHistoryItemAdapter
import com.maxrave.simpmusic.adapter.search.SearchItemAdapter
import com.maxrave.simpmusic.adapter.search.SuggestQueryAdapter
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.databinding.FragmentSearchBinding
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {
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
//        binding.svSearch.setQuery("", false)
//        binding.svSearch.clearFocus()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Log.d("SearchFragment", "onResume")
//        binding.svSearch.setQuery("", false)
//        binding.svSearch.clearFocus()
        binding.svSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    fetchSearchAll(query)
                    binding.svSearch.clearFocus()
                    binding.suggestList.visibility = View.GONE
                    binding.recentlyQueryView.visibility = View.GONE
                    binding.defaultLayout.visibility = View.GONE
                    binding.resultView.visibility = View.VISIBLE
                    if (!searchHistory.contains(query)){
                        searchHistory.add(query)
                        Log.d("Check History in Fragment", searchHistory.toString())
                    }
                    Log.d("Check History", searchHistory.toString())
                    viewModel.searchHistory.postValue(searchHistory)
                    searchHistoryAdapter.updateData(searchHistory)
                    binding.refreshSearch.isRefreshing = true
                    if (binding.chipAll.isChecked){
                        fetchSearchAll(query)
                    }
                    else if (binding.chipSong.isChecked){
                        fetchSearchSongs(query)
                    }
                    else if (binding.chipAlbum.isChecked){
                        fetchSearchAlbums(query)
                    }
                    else if (binding.chipArtists.isChecked){
                        fetchSearchArtists(query)
                    }
                    else if (binding.chipPlaylist.isChecked){
                        fetchSearchPlaylists(query)
                    }
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if ((newText == null) || newText.isEmpty())
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
        if (viewModel.searchHistory.value != null){
            searchHistory.addAll(viewModel.searchHistory.value as ArrayList<String>)
            searchHistoryAdapter.updateData(searchHistory)
        }
        suggestAdapter = SuggestQueryAdapter(arrayListOf())
        binding.suggestList.apply {
            adapter = suggestAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.resultList.apply {
            adapter = resultAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.searchType.value = "all"
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
                viewModel.searchHistory.observe(viewLifecycleOwner, Observer { history ->
                    searchHistoryAdapter.updateData(searchHistory)
                })
            }
        }

        resultAdapter.setOnClickListener(object : SearchItemAdapter.onItemClickListener{
            override fun onItemClick(position: Int, type: String) {
                Toast.makeText(context, resultAdapter.getCurrentList()[position].toString(), Toast.LENGTH_SHORT).show()
                if (type == "artist"){
                    val channelId = (resultAdapter.getCurrentList()[position] as ArtistsResult).browseId.toString()
                    val args = Bundle()
                    args.putString("channelId", channelId)
                    findNavController().navigate(R.id.action_bottom_navigation_item_search_to_artistFragment, args)
                }
                if (type == "album"){
                    val browseId = (resultAdapter.getCurrentList()[position] as AlbumsResult).browseId.toString()
                    val args = Bundle()
                    args.putString("browseId", browseId)
                    findNavController().navigate(R.id.action_global_albumFragment, args)
                }
                if (type == "playlist"){
                    val id = (resultAdapter.getCurrentList()[position] as PlaylistsResult).browseId.toString()
                    val args = Bundle()
                    args.putString("id", id)
                    findNavController().navigate(R.id.action_global_playlistFragment, args)
                }
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
        }
        binding.refreshSearch.setOnRefreshListener {
            if (binding.chipAll.isChecked){
                fetchSearchAll(binding.svSearch.query.toString())
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
        binding.chipGroupTypeSearch.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.contains(binding.chipSong.id))
            {
                resultList.clear()
                val temp = viewModel.songsSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
            }
            else if (checkedIds.contains(binding.chipAll.id))
            {
                resultList.clear()
                resultList.addAll(searchAllResult)
                resultAdapter.updateList(resultList)
            }
            else if (checkedIds.contains(binding.chipAlbum.id))
            {
                resultList.clear()
                val temp = viewModel.albumsSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
            }
            else if (checkedIds.contains(binding.chipArtists.id))
            {
                resultList.clear()
                val temp = viewModel.artistsSearchResult.value?.data
                for (i in temp!!){
                    resultList.add(i)
                }
                resultAdapter.updateList(resultList)
            }
            else if (checkedIds.contains(binding.chipPlaylist.id))
            {
                resultList.clear()
                val temp = viewModel.playlistSearchResult.value?.data
                for (i in temp!!){
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
    }


    //    private fun observeSuggestList() {
//        viewModel.suggestQuery.observe(viewLifecycleOwner, Observer { result ->
//            result?.let {
//                suggestList.clear()
//                for (i in result){
//                    suggestList += i
//                }
//                Log.d("SearchFragment", "observeSuggestList: $suggestList")
//                suggestAdapter.updateData(suggestList)
//            }
//        })
//    }
//
//    private fun observeSongSearchResultList() {
//        viewModel.songsSearchResult.observe(viewLifecycleOwner, Observer { result ->
//            result?.let {
//                resultList.clear()
//                for (i in result){
//                    resultList += i
//                }
//
//                Log.d("SearchFragment", "observeResultList: $resultList")
//                resultAdapter.updateList(resultList)
//                binding.refreshSearch.isRefreshing = false
//            }
//
//        })
//    }
//    private fun observeAllSearchResultList() {
//        viewModel.searchAllResult.observe(viewLifecycleOwner, Observer { result ->
//            result?.let {
//                resultList.clear()
//                for (i in result){
//                    resultList += i
//                }
//                Log.d("SearchFragment", "observeResultList: $resultList")
//                resultAdapter.updateList(resultList)
//                binding.refreshSearch.isRefreshing = false
//            }
//        })
//    }
//    private fun observeAlbumSearchResult() {
//        viewModel.albumsSearchResult.observe(viewLifecycleOwner, Observer {
//            result ->
//            result?.let {
//                resultList.clear()
//                for (i in result){
//                    resultList += i
//                }
//                Log.d("SearchFragment", "observeResultList: $resultList")
//                resultAdapter.updateList(resultList)
//                binding.refreshSearch.isRefreshing = false
//            }
//        })
//    }
//    private fun observeArtistSearchResult() {
//        viewModel.artistsSearchResult.observe(viewLifecycleOwner, Observer {
//                result ->
//            result?.let {
//                resultList.clear()
//                for (i in result){
//                    resultList += i
//                }
//                Log.d("SearchFragment", "observeResultList: $resultList")
//                resultAdapter.updateList(resultList)
//                binding.refreshSearch.isRefreshing = false
//            }
//        })
//    }
//    private fun observePlaylistSearchResult() {
//        viewModel.playlistSearchResult.observe(viewLifecycleOwner, Observer {
//                result ->
//            result?.let {
//                resultList.clear()
//                for (i in result){
//                    resultList += i
//                }
//                Log.d("SearchFragment", "observeResultList: $resultList")
//                resultAdapter.updateList(resultList)
//                binding.refreshSearch.isRefreshing = false
//            }
//        })
//    }
    private fun fetchSearchAlbums(query: String) {
        binding.refreshSearch.isRefreshing = true
        viewModel.searchAlbums(query)
        viewModel.albumsSearchResult.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        Log.d("SearchFragment", "observeResultList: $it")
                        resultList.clear()
                        if (it != null) {
                            for (i in it){
                                resultList += i
                            }
                        }
                        resultAdapter.updateList(resultList)
                        binding.refreshSearch.isRefreshing = false
                    }
                }
                is Resource.Loading -> {
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
                }
            }
        })
    }
    private fun fetchSearchPlaylists(query: String) {
        binding.refreshSearch.isRefreshing = true
        viewModel.searchPlaylists(query)
        viewModel.playlistSearchResult.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        Log.d("SearchFragment", "observeResultList: $it")
                        resultList.clear()
                        if (it != null) {
                            for (i in it){
                                resultList += i
                            }
                        }
                        resultAdapter.updateList(resultList)
                        binding.refreshSearch.isRefreshing = false
                    }
                }
                is Resource.Loading -> {
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
                }
            }
        })
    }
    private fun fetchSearchArtists(query: String) {
        binding.refreshSearch.isRefreshing = true
        viewModel.searchArtists(query)
        viewModel.artistsSearchResult.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        Log.d("SearchFragment", "observeResultList: $it")
                        resultList.clear()
                        if (it != null) {
                            for (i in it){
                                resultList += i
                            }
                        }
                        resultAdapter.updateList(resultList)
                        binding.refreshSearch.isRefreshing = false
                    }
                }
                is Resource.Loading -> {
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
                }
            }
        })
    }
    private fun fetchSearchSongs(query: String) {
        binding.refreshSearch.isRefreshing = true
        viewModel.searchSongs(query)
        viewModel.songsSearchResult.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        Log.d("SearchFragment", "observeResultList: $it")
                        resultList.clear()
                        if (it != null) {
                            for (i in it){
                                resultList += i
                            }
                        }
                        resultAdapter.updateList(resultList)
                        binding.refreshSearch.isRefreshing = false
                    }
                }
                is Resource.Loading -> {
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
                    binding.refreshSearch.isRefreshing = false
                }
            }
        })
    }
    private fun fetchSearchAll(query: String) {
        viewModel.searchAll(query)
        var song = ArrayList<SongsResult>()
        var album = ArrayList<AlbumsResult>()
        var artist = ArrayList<ArtistsResult>()
        var playlist = ArrayList<PlaylistsResult>()
        val temp: ArrayList<Any> = ArrayList()
        viewModel.songsSearchResult.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        Log.d("SearchFragment", "observeResultList: $it")
                        song = it!!
                    }
                }
                is Resource.Loading -> {
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
        })
        viewModel.albumsSearchResult.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        print(it)
                        album = it!!
                    }
                }
                is Resource.Loading -> {
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
        })
        viewModel.artistsSearchResult.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        print(it)
                        artist = it!!
                    }
                }
                is Resource.Loading -> {
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
        })
        viewModel.playlistSearchResult.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        print(it)
                        playlist = it!!
                    }
                }
                is Resource.Loading -> {
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
        })
        viewModel.loading.observe(viewLifecycleOwner, Observer { loading ->
            if (loading) {
                binding.refreshSearch.isRefreshing = true
            } else {
                try {
                    if (song.size >= 3) {
                        for (i in 0..2)
                        {
                            temp += artist[i]
                        }
                        for (i in 0 until song.size)
                        {
                            temp += song[i]
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
                resultList.clear()
                viewModel.searchAllResult.postValue(temp)
                searchAllResult.addAll(temp)
                resultList.addAll(temp)
                resultAdapter.updateList(resultList)
                binding.refreshSearch.isRefreshing = false
            }
        })
    }

    private fun fetchSuggestList(query: String) {
        viewModel.suggestQuery(query)
        viewModel.suggestQuery.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        print(it)
                        suggestList.clear()
                        suggestList.addAll(it!!)
                        suggestAdapter.updateData(suggestList)
                    }
                }
                is Resource.Loading -> {

                }
                is Resource.Error -> {

                }
            }
        })
    }
}