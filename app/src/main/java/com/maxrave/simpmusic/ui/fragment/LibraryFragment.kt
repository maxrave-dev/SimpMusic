package com.maxrave.simpmusic.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.library.FavoritePlaylistAdapter
import com.maxrave.simpmusic.adapter.search.SearchItemAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentLibraryBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.pagination.RecentPagingAdapter
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<LibraryViewModel>()

    private lateinit var adapterItem: SearchItemAdapter
    private lateinit var listRecentlyAdded: ArrayList<Any>

    //private lateinit var pagingAdapter: RecentPagingAdapter

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
        //pagingAdapter = RecentPagingAdapter(requireContext())
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
//        lifecycleScope.launch {
//            withContext(Dispatchers.IO) {
//                viewModel.recentlyAdded.collectLatest { pagingData ->
//                    pagingAdapter.submitData(pagingData)
//                }
//            }
//        }
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
                    val videoId = songClicked.videoId
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

            override fun onOptionsClick(position: Int, type: String) {
                val song = adapterItem.getCurrentList()[position] as SongEntity
                viewModel.getSongEntity(song.videoId)
                val dialog = BottomSheetDialog(requireContext())
                val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                with(bottomSheetView) {
                    viewModel.songEntity.observe(viewLifecycleOwner) { songEntity ->
                        if (songEntity.liked) {
                            tvFavorite.text = "Liked"
                            cbFavorite.isChecked = true
                        }
                        else {
                            tvFavorite.text = "Like"
                            cbFavorite.isChecked = false
                        }
                    }
                    tvSongTitle.text = song.title
                    tvSongTitle.isSelected = true
                    tvSongArtist.text = song.artistName?.connectArtists()
                    tvSongArtist.isSelected = true
                    ivThumbnail.load(song.thumbnails)

                    btLike.setOnClickListener {
                        if (cbFavorite.isChecked){
                            cbFavorite.isChecked = false
                            tvFavorite.text = "Like"
                            viewModel.updateLikeStatus(song.videoId, 0)
                            viewModel.getRecentlyAdded()
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
                        }
                        else {
                            cbFavorite.isChecked = true
                            tvFavorite.text = "Liked"
                            viewModel.updateLikeStatus(song.videoId, 1)
                            viewModel.getRecentlyAdded()
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
                        }
                    }

                    btSeeArtists.setOnClickListener {
                        val subDialog = BottomSheetDialog(requireContext())
                        val subBottomSheetView = BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                        Log.d("FavoriteFragment", "onOptionsClick: ${song.artistId}")
                        if (!song.artistName.isNullOrEmpty()) {
                            val tempArtist = mutableListOf<Artist>()
                            for (i in 0 until song.artistName.size) {
                                tempArtist.add(Artist(name = song.artistName[i], id = song.artistId?.get(i)))
                            }
                            Log.d("FavoriteFragment", "onOptionsClick: $tempArtist")
                            val artistAdapter = SeeArtistOfNowPlayingAdapter(tempArtist)
                            subBottomSheetView.rvArtists.apply {
                                adapter = artistAdapter
                                layoutManager = LinearLayoutManager(requireContext())
                            }
                            artistAdapter.setOnClickListener(object : SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                override fun onItemClick(position: Int) {
                                    val artist = tempArtist[position]
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
                        val url = "https://youtube.com/watch?v=${song.videoId}"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                        val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ URL")
                        startActivity(chooserIntent)
                    }
                }
                dialog.setCancelable(true)
                dialog.setContentView(bottomSheetView.root)
                dialog.show()
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