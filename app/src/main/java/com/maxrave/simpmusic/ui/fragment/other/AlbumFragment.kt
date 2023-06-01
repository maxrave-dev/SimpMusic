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
import com.maxrave.simpmusic.adapter.album.TrackAdapter
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.databinding.FragmentAlbumBinding
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.AlbumViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class AlbumFragment: Fragment() {
    private val viewModel by viewModels<AlbumViewModel>()
    private var _binding: FragmentAlbumBinding? = null
    private val binding get() = _binding!!

    private var gradientDrawable: GradientDrawable? = null
    private var toolbarBackground: Int? = null

    private lateinit var songsAdapter: TrackAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumBinding.inflate(inflater, container, false)
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
        //init Adapter
        songsAdapter = TrackAdapter(arrayListOf())
        //init RecyclerView
        binding.rvListSong.apply {
            adapter = songsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        val browseId = requireArguments().getString("browseId")
        fetchData(browseId.toString())

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.btArtist.setOnClickListener {
            if (viewModel.albumBrowse.value?.data != null){
                Log.d("TAG", "Artist name clicked: ${viewModel.albumBrowse.value?.data?.artists?.get(0)?.id}")
                val args = Bundle()
                args.putString("channelId", viewModel.albumBrowse.value?.data?.artists?.get(0)?.id)
                findNavController().navigate(R.id.action_global_artistFragment, args)
            }
        }
        songsAdapter.setOnClickListener(object : TrackAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                Toast.makeText(requireContext(), "Position: $position", Toast.LENGTH_SHORT).show()
            }

        })
        songsAdapter.setOnOptionClickListener(object : TrackAdapter.OnOptionClickListener{
            override fun onOptionClick(position: Int) {
                Toast.makeText(requireContext(), "Position: $position", Toast.LENGTH_SHORT).show()
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

    private fun fetchData(browseId: String) {
        viewModel.browseAlbum(browseId)
        viewModel.albumBrowse.observe(viewLifecycleOwner, Observer { response ->
            when (response){
                is Resource.Success -> {
                    response.data.let {
                        with(binding){
                            topAppBar.title = it?.title
                            btArtist.text = it?.artists?.get(0)?.name
                            tvYearAndCategory.text= context?.getString(R.string.year_and_category, it?.year, it?.type)
                            tvTrackCountAndDuration.text = context?.getString(R.string.album_length, it?.trackCount.toString(), it?.duration)
                            tvDescription.originalText = it?.description.toString()
                            if (it?.description == null){
                                tvDescription.originalText = "No description"
                            }
                            else {
                                tvDescription.originalText = it.description.toString()
                            }
//                            if (it?.thumbnails?.size!! > 3){
//                                ivAlbumArt.load(it.thumbnails[4].url)
//                                }
//                            else {
//                                ivAlbumArt.load(it.thumbnails[0].url)
//                            }
                            when (it?.thumbnails?.size!!){
                                1 -> loadImage(it.thumbnails[0].url)
                                2 -> loadImage(it.thumbnails[1].url)
                                3 -> loadImage(it.thumbnails[2].url)
                                4 -> loadImage(it.thumbnails[3].url)
                                else -> {}
                            }
                            songsAdapter.updateList(it.tracks as ArrayList<Track>)
                            if (viewModel.gradientDrawable.value == null){
                                viewModel.gradientDrawable.observe(viewLifecycleOwner, Observer { gradient ->
                                    binding.fullRootLayout.background = gradient
                                    toolbarBackground = gradient.colors?.get(0)
                                    Log.d("TAG", "fetchData: $toolbarBackground")
                                    //binding.topAppBar.background = ColorDrawable(toolbarBackground!!)
                                    binding.topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                                })
                            }
                            else {
                                binding.fullRootLayout.background = gradientDrawable
                                //binding.topAppBar.background = ColorDrawable(toolbarBackground!!)
                                binding.topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                            }
                        }
                    }
                    binding.rootLayout.visibility = View.VISIBLE
                    binding.loadingLayout.visibility = View.GONE
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    Snackbar.make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
            }
        })
    }
    private fun loadImage(url: String){
        binding.ivAlbumArt.load(url) {
            transformations(object : Transformation{
                override val cacheKey: String
                    get() = "paletteTransformerForAlbumArt"

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