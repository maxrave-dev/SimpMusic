package com.maxrave.simpmusic.ui.fragment.other

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
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
import com.maxrave.simpmusic.adapter.artist.AlbumsAdapter
import com.maxrave.simpmusic.adapter.artist.PopularAdapter
import com.maxrave.simpmusic.adapter.artist.RelatedArtistsAdapter
import com.maxrave.simpmusic.adapter.artist.SinglesAdapter
import com.maxrave.simpmusic.data.model.browse.artist.ResultAlbum
import com.maxrave.simpmusic.data.model.browse.artist.ResultRelated
import com.maxrave.simpmusic.data.model.browse.artist.ResultSingle
import com.maxrave.simpmusic.data.model.browse.artist.ResultSong
import com.maxrave.simpmusic.databinding.FragmentArtistBinding
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.ArtistViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.Insetter
import dev.chrisbanes.insetter.applyInsetter
import dev.chrisbanes.insetter.windowInsetTypesOf

@AndroidEntryPoint
class ArtistFragment: Fragment(){
    private val viewModel by viewModels<ArtistViewModel>()
    private var _binding: FragmentArtistBinding? = null
    private val binding get() = _binding!!

    private lateinit var popularAdapter: PopularAdapter
    private lateinit var singlesAdapter: SinglesAdapter
    private lateinit var albumsAdapter: AlbumsAdapter
    private lateinit var relatedArtistsAdapter: RelatedArtistsAdapter

    private var gradientDrawable: GradientDrawable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistBinding.inflate(inflater, container, false)
//        binding.toolBar.applyInsetter {
//            type(statusBars = true) {
//                padding()
//            }
//        }
//        Insetter.builder()
//
//            // This will add the status bars insets as margin to all sides of the view,
//            // maintaining the original margins (from the layout XML, etc)`
//            .margin(windowInsetTypesOf(statusBars = true))
//
//            // This is a shortcut for view.setOnApplyWindowInsetsListener(builder.build())
//            .applyToView(binding.toolBar)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.gradientDrawable.value != null){
            gradientDrawable = viewModel.gradientDrawable.value
        }
        binding.rootFull.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE
        popularAdapter = PopularAdapter(arrayListOf())
        singlesAdapter = SinglesAdapter(arrayListOf())
        albumsAdapter = AlbumsAdapter(arrayListOf())
        relatedArtistsAdapter = RelatedArtistsAdapter(arrayListOf(), requireContext())
        binding.rvPopularSongs.apply {
            adapter = popularAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.rvSingles.apply {
            adapter = singlesAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rvAlbum.apply {
            adapter = albumsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rvRelatedArtists.apply {
            adapter = relatedArtistsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        val channelId = requireArguments().getString("channelId")
        fetchData(channelId.toString())

        binding.toolBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        relatedArtistsAdapter.setOnClickListener(object: RelatedArtistsAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val bundle = Bundle()
                bundle.putString("channelId", relatedArtistsAdapter.getItem(position).browseId)
                findNavController().navigate(R.id.action_global_artistFragment, bundle)
            }
        })
        singlesAdapter.setOnClickListener(object: SinglesAdapter.OnItemClickListener{
            override fun onItemClick(position: Int, type: String) {
                Toast.makeText(context, "${type.toString()} ${singlesAdapter.getItem(position).toString()}", Toast.LENGTH_LONG).show()
                val args = Bundle()
                args.putString("browseId", singlesAdapter.getItem(position).browseId)
                findNavController().navigate(R.id.action_global_albumFragment, args)
            }
        })
        albumsAdapter.setOnClickListener(object: AlbumsAdapter.OnItemClickListener{
            override fun onItemClick(position: Int, type: String) {
                Toast.makeText(context, "${type.toString()} ${albumsAdapter.getItem(position).toString()}", Toast.LENGTH_LONG).show()
                val args = Bundle()
                args.putString("browseId", albumsAdapter.getItem(position).browseId)
                findNavController().navigate(R.id.action_global_albumFragment, args)
            }
        })
        popularAdapter.setOnClickListener(object: PopularAdapter.OnItemClickListener{
            override fun onItemClick(position: Int, type: String) {
                Toast.makeText(context, "${type.toString()} ${popularAdapter.getItem(position).toString()}", Toast.LENGTH_LONG).show()
            }
        })

    }
    private fun fetchData(channelId: String){
        viewModel.browseArtist(channelId)
        viewModel.artistBrowse.observe(viewLifecycleOwner, Observer {response ->
            when(response){
                is Resource.Success -> {
                    response.data.let {
                        with(binding){
                            topAppBar.title = it?.name.toString()
                            if (it?.thumbnails?.size!! > 1){
                                loadImage(it.thumbnails[1].url)
                            }
                            else {
                                loadImage(it.thumbnails[0].url)
                            }
                            if (viewModel.gradientDrawable.value == null){
                                viewModel.gradientDrawable.observe(viewLifecycleOwner, Observer {gd ->
                                    binding.belowAppBarLayoutContainer.background = gd
                                    binding.aboutContainer.background = gd
                                    Log.d("Load Gradient from Image", gd.toString())
                                })
                            }
                            else {
                                Log.d("Load Gradient from Cache", gradientDrawable.toString())
                                binding.belowAppBarLayoutContainer.background = gradientDrawable
                                binding.aboutContainer.background = gradientDrawable
                            }
                            tvSubscribers.text = context?.getString(R.string.subscribers,
                                it.subscribers
                            )
                            if (it.views == null){
                                tvViews.text = ""
                            }
                            else {
                                tvViews.text = it.views.toString()
                            }
                            tvDescription.text = it.description.toString()
                            if (it.songs?.results != null) {
                                popularAdapter.updateList(it.songs.results as ArrayList<ResultSong>)
                            }
                            if (it.singles?.results != null) {
                                singlesAdapter.updateList(it.singles.results as ArrayList<ResultSingle>)
                            }
                            if (it.albums?.results != null) {
                                albumsAdapter.updateList(it.albums.results as ArrayList<ResultAlbum>)
                            }
                            if (it.related?.results != null) {
                                relatedArtistsAdapter.updateList(it.related.results as ArrayList<ResultRelated>)
                            }
                        }
                        binding.loadingLayout.visibility = View.GONE
                        binding.topAppBarLayout.visibility = View.VISIBLE
                        binding.rootLayout.visibility = View.VISIBLE
                        binding.rootFull.visibility = View.VISIBLE
                    }
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    binding.loadingLayout.visibility = View.GONE
                    Snackbar.make(binding.root, "Can't get artist data because: ${response.message}", Snackbar.LENGTH_LONG)
                        .setAction("Retry") {
                            fetchData(channelId)
                        }
                        .show()
                    findNavController().popBackStack()
                }
            }
        })
    }

    private fun loadImage(url: String) {
        binding.ivArtistImage.load(url) {
            transformations(object: Transformation {
                override val cacheKey: String
                    get() = "paletteTransformer"

                override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                    val p = Palette.from(input).generate()
                    val defaultColor = 0x000000
                    var startColor = p.getDarkVibrantColor(defaultColor)
                    Log.d("Check Start Color", "transform: $startColor")
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
//                    val centerColor = 0x6C6C6C
                    val endColor = 0x1b1a1f
                    val gd = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(startColor, endColor)
                    )
                    gd.cornerRadius = 0f
                    gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                    gd.gradientRadius = 0.5f
                    gd.alpha = 150
                    viewModel.gradientDrawable.postValue(gd)
                    return input
                }

            })
        }

    }
}