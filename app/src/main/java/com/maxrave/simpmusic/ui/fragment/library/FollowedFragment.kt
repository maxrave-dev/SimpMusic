package com.maxrave.simpmusic.ui.fragment.library

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
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.databinding.FragmentFollowedBinding
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.viewModel.FollowedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter

@AndroidEntryPoint
class FollowedFragment: Fragment() {
    private var _binding: FragmentFollowedBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<FollowedViewModel>()

    private lateinit var followedAdapter: SearchItemAdapter
    private lateinit var listFollowed: ArrayList<Any>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowedBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listFollowed = ArrayList<Any>()
        followedAdapter = SearchItemAdapter(arrayListOf(), requireContext())
        binding.rvFavorite.apply {
            adapter = followedAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.getListLikedSong()
        viewModel.listFollowedArtist.observe(viewLifecycleOwner){ followed ->
            listFollowed.clear()
            for (i in followed.size - 1 downTo 0) {
                listFollowed.add(followed[i])
            }
            followedAdapter.updateList(listFollowed)
        }


        followedAdapter.setOnClickListener(object : SearchItemAdapter.onItemClickListener {
            override fun onItemClick(position: Int, type: String) {
                if (type == "artist") {
                    val data = listFollowed[position] as ArtistEntity
                    findNavController().navigateSafe(R.id.action_global_artistFragment, Bundle().apply {
                        putString("channelId", data.channelId)
                    })
                }
            }

            override fun onOptionsClick(position: Int, type: String) {

            }

        })

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }
}