package com.maxrave.simpmusic.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.library.FavoritePlaylistAdapter
import com.maxrave.simpmusic.adapter.search.SearchItemAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.FragmentLibraryBinding
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import java.time.LocalDateTime

@AndroidEntryPoint
class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<LibraryViewModel>()

    private lateinit var adapterItem: SearchItemAdapter
    private lateinit var listRecentlyAdded: ArrayList<Any>

    private lateinit var adapterPlaylist: FavoritePlaylistAdapter
    private lateinit var listPlaylist: ArrayList<Any>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true){
                margin()
            }
        }
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listRecentlyAdded = ArrayList()
        adapterItem = SearchItemAdapter(arrayListOf(), requireContext())
        listPlaylist = ArrayList()
        adapterPlaylist = FavoritePlaylistAdapter(arrayListOf())
        binding.rvRecentlyAdded.apply {
            adapter = adapterItem
            layoutManager = LinearLayoutManager(requireContext())
        }
        binding.rvFavoritePlaylists.apply {
            adapter = adapterPlaylist
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        viewModel.getRecentlyAdded()
        viewModel.getPlaylistFavorite()
        viewModel.listPlaylistFavorite.observe(viewLifecycleOwner) { listFavorite ->
            val temp = ArrayList<Any>()
            for (i in listFavorite.size - 1 downTo 0) {
                temp.add(listFavorite[i])
            }
            listPlaylist.clear()
            listPlaylist.addAll(temp)
            adapterPlaylist.updateList(listPlaylist)
        }
        viewModel.listRecentlyAdded.observe(viewLifecycleOwner){list ->
            Log.d("LibraryFragment", "onViewCreated: $list")
            val temp = ArrayList<Any>()
            for (i in list.size - 1 downTo 0) {
                temp.add(list[i])
            }
            listRecentlyAdded.clear()
            listRecentlyAdded.addAll(temp)
            adapterItem.updateList(listRecentlyAdded)
        }
        adapterItem.setOnClickListener(object : SearchItemAdapter.onItemClickListener {
            override fun onItemClick(position: Int, type: String) {
                if (type == "artist"){
                    val channelId = (adapterItem.getCurrentList()[position] as ArtistEntity).channelId
                    val args = Bundle()
                    args.putString("channelId", channelId)
                    findNavController().navigate(R.id.action_global_artistFragment, args)
                }
                if (type == Config.ALBUM_CLICK){
                    val browseId = (adapterItem.getCurrentList()[position] as AlbumEntity).browseId
                    val args = Bundle()
                    args.putString("browseId", browseId)
                    findNavController().navigate(R.id.action_global_albumFragment, args)
                }
                if (type == Config.PLAYLIST_CLICK){
                    val id = (adapterItem.getCurrentList()[position] as PlaylistEntity).id
                    val args = Bundle()
                    args.putString("id", id)
                    findNavController().navigate(R.id.action_global_playlistFragment, args)
                }
                if (type == Config.SONG_CLICK){
                    val songClicked = adapterItem.getCurrentList()[position] as SongEntity
                    val videoId = (adapterItem.getCurrentList()[position] as SongEntity).videoId
                    Queue.clear()
                    val firstQueue: Track = songClicked.toTrack()
                    Queue.setNowPlaying(firstQueue)
                    val args = Bundle()
                    args.putString("videoId", videoId)
                    args.putString("from", "Recently Added")
                    args.putString("type", Config.SONG_CLICK)
                    findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
                }
            }
        })
        adapterPlaylist.setOnClickListener(object : FavoritePlaylistAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, type: String) {
                when (type) {
                    "playlist" -> {
                        val id = (listPlaylist[position] as PlaylistEntity).id
                        val args = Bundle()
                        args.putString("id", id)
                        findNavController().navigate(R.id.action_global_playlistFragment, args)
                    }
                    "album" -> {
                        val browseId = (listPlaylist[position] as AlbumEntity).browseId
                        val args = Bundle()
                        args.putString("browseId", browseId)
                        findNavController().navigate(R.id.action_global_albumFragment, args)
                    }
                }
            }

        })
        binding.btFavorite.setOnClickListener{
            findNavController().navigate(R.id.action_bottom_navigation_item_library_to_favoriteFragment)
        }
        binding.btFollowed.setOnClickListener {
            findNavController().navigate(R.id.action_bottom_navigation_item_library_to_followedFragment)
        }
        binding.btTrending.setOnClickListener {
            findNavController().navigate(R.id.action_bottom_navigation_item_library_to_mostPlayedFragment)
        }
    }

}