package com.maxrave.simpmusic.ui.fragment.other

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.maxrave.kotlinytmusicscraper.models.YTItem
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.MoreAlbumAdapter
import com.maxrave.simpmusic.databinding.FragmentMoreAlbumsBinding
import com.maxrave.simpmusic.viewModel.MoreAlbumsViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MoreAlbumsFragment : Fragment() {
    private var _binding: FragmentMoreAlbumsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<MoreAlbumsViewModel>()
    private var id: String? = null
    private var type: String? = null
    private lateinit var albumAdapter: MoreAlbumAdapter
    private lateinit var listAlbum: ArrayList<YTItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMoreAlbumsBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        arguments?.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        id = arguments?.getString("id")
        type = arguments?.getString("type")
        Log.w("MoreAlbumsFragment", "id: $id, type: $type")
        listAlbum = arrayListOf()
        albumAdapter = MoreAlbumAdapter(listAlbum)
        binding.rvListAlbums.apply {
            adapter = albumAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
        if (id == null) {
            fetchFromViewModel()
        }
        else {
            if (type != null) {
                when (type) {
                    "album" -> {
                        fetchAlbums(id!!)
                    }
                    "single" -> {
                        fetchSingles(id!!)
                    }
                }
            }
        }
        albumAdapter.setOnClickListener(object : MoreAlbumAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, type: String) {
                val bundle = Bundle()
                bundle.putString("browseId", listAlbum[position].id)
                findNavController().navigate(
                    R.id.action_global_albumFragment,
                    bundle
                )
            }
        })
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        lifecycleScope.launch {
            viewModel.browseResult.collect {data ->
                if (data != null) {
                    binding.topAppBar.title = data.title
                    data.items.firstOrNull()?.items?.let { listAlbum.addAll(it) }
                    Log.w("MoreAlbumsFragment", "listAlbum: $listAlbum")
                    albumAdapter.updateList(listAlbum)
                }
            }
        }
    }

    private fun fetchFromViewModel() {
        if (viewModel.browseResult.value != null) {
            viewModel.browseResult.value!!.items.firstOrNull()?.items?.let { listAlbum.addAll(it) }
            albumAdapter.updateList(listAlbum)
        }
    }

    private fun fetchSingles(id: String) {
        viewModel.getSingleMore(id)
    }

    private fun fetchAlbums(id: String) {
        viewModel.getAlbumMore(id)
    }
}