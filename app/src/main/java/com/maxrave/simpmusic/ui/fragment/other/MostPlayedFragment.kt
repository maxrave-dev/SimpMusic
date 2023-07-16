package com.maxrave.simpmusic.ui.fragment.other

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.search.SearchItemAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.FragmentMostPlayedBinding
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.viewModel.MostPlayedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter

@AndroidEntryPoint
class MostPlayedFragment: Fragment() {
    private var _binding: FragmentMostPlayedBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<MostPlayedViewModel>()

    private lateinit var mostPlayedAdapter: SearchItemAdapter
    private lateinit var listMostPlayed: ArrayList<Any>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMostPlayedBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listMostPlayed = ArrayList<Any>()
        mostPlayedAdapter = SearchItemAdapter(arrayListOf(), requireContext())
        binding.rvFavorite.apply {
            adapter = mostPlayedAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.getListLikedSong()
        viewModel.listMostPlayedSong.observe(viewLifecycleOwner){ most ->
            listMostPlayed.clear()
            listMostPlayed.addAll(most)
            mostPlayedAdapter.updateList(listMostPlayed)
        }


        mostPlayedAdapter.setOnClickListener(object : SearchItemAdapter.onItemClickListener {
            override fun onItemClick(position: Int, type: String) {
                if (type == Config.SONG_CLICK){
                    val songClicked = mostPlayedAdapter.getCurrentList()[position] as SongEntity
                    val videoId = (mostPlayedAdapter.getCurrentList()[position] as SongEntity).videoId
                    Queue.clear()
                    val firstQueue: Track = songClicked.toTrack()
                    Queue.setNowPlaying(firstQueue)
                    val args = Bundle()
                    args.putString("videoId", videoId)
                    args.putString("from", "Most Played")
                    args.putString("type", Config.SONG_CLICK)
                    findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
                }
            }

        })

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }
}