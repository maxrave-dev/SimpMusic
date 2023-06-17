package com.maxrave.simpmusic.ui.fragment.other

import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.size.Size
import coil.transform.Transformation
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.playlist.PlaylistItemAdapter
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.playlist.TrackPlaylist
import com.maxrave.simpmusic.databinding.FragmentPlaylistBinding
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class PlaylistFragment: Fragment() {
    private val viewModel by viewModels<PlaylistViewModel>()
    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private var gradientDrawable: GradientDrawable? = null
    private var toolbarBackground: Int? = null

    private lateinit var playlistItemAdapter: PlaylistItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.gradientDrawable.value != null){
            gradientDrawable = viewModel.gradientDrawable.value
            toolbarBackground = gradientDrawable?.colors?.get(0)
        }
        binding.rootLayout.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE

        playlistItemAdapter = PlaylistItemAdapter(arrayListOf())
        binding.rvListSong.apply {
            adapter = playlistItemAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        val id = requireArguments().getString("id")
        fetchData(id.toString())

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        playlistItemAdapter.setOnClickListener(object: PlaylistItemAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                Toast.makeText(requireContext(), playlistItemAdapter.getItem(position).toString(), Toast.LENGTH_SHORT).show()
            }
        })
        playlistItemAdapter.setOnOptionClickListener(object: PlaylistItemAdapter.OnOptionClickListener{
            override fun onOptionClick(position: Int) {
                Toast.makeText(requireContext(), "Option", Toast.LENGTH_SHORT).show()
            }
        })
        binding.topAppBarLayout.addOnOffsetChangedListener { it, verticalOffset ->
            Log.d("ArtistFragment", "Offset: $verticalOffset" + "Total: ${it.totalScrollRange}")
            if(abs(it.totalScrollRange) == abs(verticalOffset)) {
                binding.topAppBar.background = viewModel.gradientDrawable.value
                requireActivity().window.statusBarColor = viewModel.gradientDrawable.value?.colors!!.first()
            }
            else
            {
                binding.topAppBar.background = null
                requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
                Log.d("ArtistFragment", "Expanded")
            }
        }
    }

    private fun fetchData(id: String) {
        viewModel.browsePlaylist(id)
        viewModel.playlistBrowse.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        with(binding){
                            topAppBar.title = it?.title
                            tvPlaylistAuthor.text = it?.author?.name
                            tvYearAndCategory.text = requireContext().getString(R.string.year_and_category, it?.year.toString(), "Playlist")
                            tvTrackCountAndDuration.text = requireContext().getString(R.string.album_length, it?.trackCount.toString(), it?.duration.toString())
                            if (it?.description != null){
                                tvDescription.originalText = it.description
                            } else {
                                tvDescription.originalText = "No description"
                            }
                            loadImage(it?.thumbnails?.last()?.url)
                            playlistItemAdapter.updateList(it?.tracks as ArrayList<Track>)
                            if (viewModel.gradientDrawable.value == null) {
                                viewModel.gradientDrawable.observe(viewLifecycleOwner, Observer { gradient ->
                                    fullRootLayout.background = gradient
                                    toolbarBackground = gradient?.colors?.get(0)
                                    topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                                })
                            }
                            else {
                                fullRootLayout.background = gradientDrawable
                                topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                            }
                        }
                    }
                    binding.rootLayout.visibility = View.VISIBLE
                    binding.loadingLayout.visibility = View.GONE
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
            }
        })
    }

    private fun loadImage(url: String?) {
        if (url != null){
            binding.ivPlaylistArt.load(url){
                transformations(object : Transformation{
                    override val cacheKey: String
                        get() = "paletteTransformerForPlaylistArt"

                    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                        val p = Palette.from(input).generate()
                        val defaultColor = 0x000000
                        var startColor = p.getDarkVibrantColor(defaultColor)
                        if (startColor == defaultColor){
                            startColor = p.getDarkMutedColor(defaultColor)
                            if (startColor == defaultColor){
                                startColor = p.getVibrantColor(defaultColor)
                                if (startColor == defaultColor){
                                    startColor = p.getMutedColor(defaultColor)
                                    if (startColor == defaultColor){
                                        startColor = p.getLightVibrantColor(defaultColor)
                                        if (startColor == defaultColor){
                                            startColor = p.getLightMutedColor(defaultColor)
                                        }
                                    }
                                }
                            }
                            Log.d("Check Start Color", "transform: $startColor")
                        }
                        startColor = ColorUtils.setAlphaComponent(startColor, 150)
                        val endColor = resources.getColor(R.color.md_theme_dark_background, null)
                        val gd = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(startColor, endColor)
                        )
                        gd.cornerRadius = 0f
                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                        gd.gradientRadius = 0.5f
                        viewModel.gradientDrawable.postValue(gd)
                        return input
                    }

                })
            }
        }
    }
}