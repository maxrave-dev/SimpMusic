package com.maxrave.simpmusic.ui.fragment.other

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.size.Size
import coil.transform.Transformation
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.podcast.PodcastAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.podcast.PodcastBrowse
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetPlaylistMoreBinding
import com.maxrave.simpmusic.databinding.FragmentPodcastBinding
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.toListTrack
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.PodcastViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import kotlin.random.Random

@AndroidEntryPoint
class PodcastFragment : Fragment() {

    private val viewModel by activityViewModels<PodcastViewModel>()
    private var _binding: FragmentPodcastBinding? = null
    private val binding get() = _binding!!

    private var gradientDrawable: GradientDrawable? = null
    private var toolbarBackground: Int? = null

    private lateinit var podcastAdapter: PodcastAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPodcastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.statusBarColor =
            ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.gradientDrawable.value != null) {
            gradientDrawable = viewModel.gradientDrawable.value
            toolbarBackground = gradientDrawable?.colors?.get(0)
        }
        podcastAdapter = PodcastAdapter(arrayListOf())
        binding.rvListPodcasts.apply {
            adapter = podcastAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        binding.rootLayout.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE
        var id = requireArguments().getString("id")
        if (id == null) {
            id = viewModel.id.value
            fetchDataFromViewModel()
        } else {
            viewModel.id.value = id
            fetchData(id)
        }
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.topAppBarLayout.addOnOffsetChangedListener { it, verticalOffset ->
            if (abs(it.totalScrollRange) == abs(verticalOffset)) {
                binding.topAppBar.background = viewModel.gradientDrawable.value
                binding.collapsingToolbarLayout.isTitleEnabled = true
                if (viewModel.gradientDrawable.value != null) {
                    if (viewModel.gradientDrawable.value?.colors != null) {
                        requireActivity().window.statusBarColor =
                            viewModel.gradientDrawable.value?.colors!!.first()
                    }
                }
            } else {
                binding.collapsingToolbarLayout.isTitleEnabled = false
                binding.topAppBar.background = null
                requireActivity().window.statusBarColor =
                    ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
            }
        }
        binding.btPlayPause.setOnClickListener {
            if (viewModel.podcastBrowse.value is Resource.Success && viewModel.podcastBrowse.value?.data != null) {
                val args = Bundle()
                args.putString("type", Config.PLAYLIST_CLICK)
                args.putString(
                    "videoId",
                    viewModel.podcastBrowse.value?.data?.listEpisode?.get(0)?.videoId
                )
                args.putString("from", "Podcast \"${viewModel.podcastBrowse.value?.data?.title}\"")
                args.putString("playlistId", id?.replaceFirst("VL", ""))
                Queue.clear()
                Queue.setNowPlaying(viewModel.podcastBrowse.value?.data!!.listEpisode[0].toTrack())
                Queue.addAll(viewModel.podcastBrowse.value?.data!!.listEpisode.toListTrack())
                if (Queue.getQueue().size >= 1) {
                    Queue.removeFirstTrackForPlaylistAndAlbum()
                }
                Log.d("PlaylistFragment", "Queue: ${Queue.getQueue().size}")
                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
            }
        }
        binding.btShuffle.setOnClickListener {
            if (viewModel.podcastBrowse.value is Resource.Success && viewModel.podcastBrowse.value?.data != null) {
                val args = Bundle()
                args.putString("type", Config.PLAYLIST_CLICK)
                val index =
                    Random.nextInt(0, viewModel.podcastBrowse.value?.data!!.listEpisode.size - 1)
                args.putString(
                    "videoId",
                    viewModel.podcastBrowse.value?.data?.listEpisode?.get(index)?.videoId
                )
                args.putString("from", "Podcast \"${viewModel.podcastBrowse.value?.data?.title}\"")
                args.putString("playlistId", id?.replaceFirst("VL", ""))
                Queue.clear()
                Queue.setNowPlaying(viewModel.podcastBrowse.value?.data!!.listEpisode[index].toTrack())
                val shuffleList: ArrayList<Track> = arrayListOf()
                viewModel.podcastBrowse.value?.data?.listEpisode?.let {
                    shuffleList.addAll(it.toListTrack())
                }
                shuffleList.remove(
                    viewModel.podcastBrowse.value?.data?.listEpisode?.get(index)?.toTrack()
                )
                shuffleList.shuffle()
                Queue.addAll(shuffleList)
                Log.d("PlaylistFragment", "Queue: ${Queue.getQueue().size}")
                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
            }
        }
        binding.btMore.setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val moreView = BottomSheetPlaylistMoreBinding.inflate(layoutInflater)
            moreView.ivThumbnail.load(viewModel.podcastBrowse.value?.data?.thumbnail?.lastOrNull()?.url)
            moreView.tvSongTitle.text = viewModel.podcastBrowse.value?.data?.title
            moreView.tvSongArtist.text = viewModel.podcastBrowse.value?.data?.author?.name
            moreView.btShare.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                val url = "https://youtube.com/playlist?list=${id?.replaceFirst("VL", "")}"
                shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                val chooserIntent =
                    Intent.createChooser(shareIntent, getString(R.string.share_url))
                startActivity(chooserIntent)
            }
            moreView.btSync.visibility = View.GONE
            bottomSheetDialog.setContentView(moreView.root)
            bottomSheetDialog.setCancelable(true)
            bottomSheetDialog.show()
        }
        podcastAdapter.setOnItemClickListener(object : PodcastAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                if (viewModel.podcastBrowse.value is Resource.Success && viewModel.podcastBrowse.value?.data != null) {
                    val args = Bundle()
                    args.putString("type", Config.PLAYLIST_CLICK)
                    args.putString(
                        "videoId",
                        viewModel.podcastBrowse.value?.data!!.listEpisode[position].videoId
                    )
                    args.putString(
                        "from",
                        "Podcast \"${viewModel.podcastBrowse.value?.data!!.title}\""
                    )
                    args.putString("playlistId", id?.replaceFirst("VL", ""))
                    args.putInt("index", position)
                    Queue.clear()
                    Queue.setNowPlaying(viewModel.podcastBrowse.value?.data!!.listEpisode[position].toTrack())
                    Queue.addAll(viewModel.podcastBrowse.value?.data!!.listEpisode.toListTrack())
                    if (Queue.getQueue().size >= 1) {
                        Queue.removeTrackWithIndex(position)
                    }
                    Log.d("PlaylistFragment", "Queue: ${Queue.getQueue().size}")
                    findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
                }
            }
        })
    }

    private fun fetchDataFromViewModel() {
        val response = viewModel.podcastBrowse.value
        when (response) {
            is Resource.Success -> {
                response.data?.let {
                    with(binding) {
                        collapsingToolbarLayout.title = it.title
                        tvTitle.text = it.title
                        tvTitle.isSelected = true
                        tvDescription.originalText =
                            it.description ?: getString(R.string.no_description)
                        ivPlaylistAuthor.load(it.authorThumbnail)
                        tvPlaylistAuthor.text = it.author.name
                        loadImage(it.thumbnail.lastOrNull()?.url)
                        val listEpisode: ArrayList<PodcastBrowse.EpisodeItem> = arrayListOf()
                        for (i in it.listEpisode.indices) {
                            listEpisode.add(it.listEpisode[i])
                        }
                        podcastAdapter.updateData(listEpisode)
                        if (viewModel.gradientDrawable.value == null) {
                            viewModel.gradientDrawable.observe(viewLifecycleOwner)
                            { gradient ->
                                topAppBarLayout.background = gradient
                            }
                        } else {
                            topAppBarLayout.background = gradientDrawable
                        }
                        binding.rootLayout.visibility = View.VISIBLE
                        binding.loadingLayout.visibility = View.GONE
                    }
                }
            }

            is Resource.Error -> {
                Snackbar.make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG)
                    .show()
                findNavController().popBackStack()
            }

            else -> {
                Log.w("PodcastFragment", "Resource.Loading")
            }
        }
    }

    private fun fetchData(id: String) {
        viewModel.clearPodcastBrowse()
        viewModel.getPodcastBrowse(id)
        viewModel.podcastBrowse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let {
                        with(binding) {
                            collapsingToolbarLayout.title = it.title
                            tvTitle.text = it.title
                            tvTitle.isSelected = true
                            tvDescription.originalText =
                                it.description ?: getString(R.string.no_description)
                            ivPlaylistAuthor.load(it.authorThumbnail)
                            tvPlaylistAuthor.text = it.author.name
                            loadImage(it.thumbnail.lastOrNull()?.url)
                            val listEpisode: ArrayList<PodcastBrowse.EpisodeItem> = arrayListOf()
                            for (i in it.listEpisode.indices) {
                                listEpisode.add(it.listEpisode[i])
                            }
                            podcastAdapter.updateData(listEpisode)
                            if (viewModel.gradientDrawable.value == null) {
                                viewModel.gradientDrawable.observe(viewLifecycleOwner)
                                { gradient ->
                                    if (gradient != null) {
                                        val start = topAppBarLayout.background
                                        val transition =
                                            TransitionDrawable(arrayOf(start, gradient))
                                        topAppBarLayout.background = transition
                                        transition.isCrossFadeEnabled = true
                                        transition.startTransition(500)
                                    }
                                }
                            } else {
                                topAppBarLayout.background = gradientDrawable
                            }
                            binding.rootLayout.visibility = View.VISIBLE
                            binding.loadingLayout.visibility = View.GONE
                        }
                    }
                }

                is Resource.Error -> {
                    Snackbar.make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG)
                        .show()
                    findNavController().popBackStack()
                }

                else -> {
                    Log.w("PodcastFragment", "Resource.Loading")
                }
            }
        }
    }

    private fun loadImage(url: String?) {
        if (url != null) {
            binding.ivPlaylistArt.load(url) {
                transformations(object : Transformation {
                    override val cacheKey: String
                        get() = url

                    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                        val p = Palette.from(input).generate()
                        val defaultColor = 0x000000
                        var startColor = p.getDarkVibrantColor(defaultColor)
                        if (startColor == defaultColor) {
                            startColor = p.getDarkMutedColor(defaultColor)
                            if (startColor == defaultColor) {
                                startColor = p.getVibrantColor(defaultColor)
                                if (startColor == defaultColor) {
                                    startColor = p.getMutedColor(defaultColor)
                                    if (startColor == defaultColor) {
                                        startColor = p.getLightVibrantColor(defaultColor)
                                        if (startColor == defaultColor) {
                                            startColor = p.getLightMutedColor(defaultColor)
                                        }
                                    }
                                }
                            }
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