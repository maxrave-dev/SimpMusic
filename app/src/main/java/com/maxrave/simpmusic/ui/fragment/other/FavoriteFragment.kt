package com.maxrave.simpmusic.ui.fragment.other

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.search.SearchItemAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.FragmentFavoriteBinding
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.viewModel.FavoriteViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<FavoriteViewModel>()

    private lateinit var likedAdapter: SearchItemAdapter
    private lateinit var listLiked: ArrayList<Any>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listLiked = ArrayList<Any>()
        likedAdapter = SearchItemAdapter(arrayListOf(), requireContext())
        binding.rvFavorite.apply {
            adapter = likedAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.getListLikedSong()
        viewModel.listLikedSong.observe(viewLifecycleOwner){ liked ->
            listLiked.clear()
            listLiked.addAll(liked)
            likedAdapter.updateList(listLiked)
        }


        likedAdapter.setOnClickListener(object : SearchItemAdapter.onItemClickListener {
            override fun onItemClick(position: Int, type: String) {
                val song = listLiked[position] as SongEntity
                val args = Bundle()
                args.putString("type", Config.ALBUM_CLICK)
                args.putString("videoId", song.videoId)
                args.putString("from", "Favorite")
                args.putInt("index", position)
                Queue.clear()
                Queue.setNowPlaying(song.toTrack())
                Queue.addAll(listLiked.map { (it as SongEntity).toTrack()} as ArrayList<Track>)
                Queue.removeTrackWithIndex(position)
                findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
            }

        })

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }
}