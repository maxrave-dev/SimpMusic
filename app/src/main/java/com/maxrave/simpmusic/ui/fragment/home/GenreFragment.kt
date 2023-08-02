package com.maxrave.simpmusic.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.adapter.moodandgenre.genre.GenreItemAdapter
import com.maxrave.simpmusic.data.model.explore.mood.genre.ItemsPlaylist
import com.maxrave.simpmusic.databinding.MoodMomentDialogBinding
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.GenreViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter

@AndroidEntryPoint
class GenreFragment: Fragment(){
    private val viewModel by viewModels<GenreViewModel>()
    private var _binding: MoodMomentDialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var genreList: ArrayList<ItemsPlaylist>
    private lateinit var genreItemAdapter: GenreItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MoodMomentDialogBinding.inflate(inflater, container, false)
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.contentLayout.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE
        genreList = ArrayList()
        genreItemAdapter = GenreItemAdapter(arrayListOf(), requireContext(), findNavController())
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvListPlaylist.apply {
            adapter = genreItemAdapter
            layoutManager = linearLayoutManager
        }
        val params = requireArguments().getString("params")
        if (viewModel.genreObject.value != null){
            binding.contentLayout.visibility = View.VISIBLE
            binding.loadingLayout.visibility = View.GONE
            genreList.addAll(viewModel.genreObject.value?.data?.itemsPlaylist as ArrayList<ItemsPlaylist>)
            genreItemAdapter.updateData(genreList)
        }
        else{
            fetchData(params)
        }
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun fetchData(params: String?) {
        if (params != null) {
            viewModel.getGenre(params)
        }
        viewModel.genreObject.observe(viewLifecycleOwner, Observer { response ->
            when(response){
                is Resource.Success -> {
                    response.data.let {
                        binding.topAppBar.title = it?.header
                        genreList.addAll(it?.itemsPlaylist as ArrayList<ItemsPlaylist>)
                        genreItemAdapter.updateData(genreList)
                        binding.contentLayout.visibility = View.VISIBLE
                        binding.loadingLayout.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.loadingLayout.visibility = View.GONE
                    response.message?.let { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}