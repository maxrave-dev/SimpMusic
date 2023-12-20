package com.maxrave.simpmusic.ui.fragment.other

import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.load
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.album.TrackAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.FragmentAlbumBinding
import com.maxrave.simpmusic.extension.indexMap
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.toAlbumEntity
import com.maxrave.simpmusic.extension.toArrayListTrack
import com.maxrave.simpmusic.extension.toListVideoId
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.AlbumViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.random.Random

@AndroidEntryPoint
@UnstableApi
class AlbumFragment: Fragment() {
    private val viewModel by activityViewModels<AlbumViewModel>()

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
        viewModel.getLocation()
        lifecycleScope.launch {
            viewModel.liked.collect { liked ->
                binding.cbLove.isChecked = liked
            }
        }
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
        var browseId = requireArguments().getString("browseId")
        val downloaded = arguments?.getInt("downloaded")
        if (browseId == null || browseId == viewModel.browseId.value){
            browseId = viewModel.browseId.value
            fetchDataFromViewModel()
        }
        if (browseId != null){
            Log.d("Check null", "onViewCreated: $downloaded")
            if (downloaded == null || downloaded == 0){
                viewModel.updateBrowseId(browseId)
                fetchData(browseId)
            }
            if (downloaded == 1){
                viewModel.updateBrowseId(browseId)
                fetchData(browseId, downloaded = 1)
            }
        }


        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.btArtist.setOnClickListener {
            if (viewModel.albumBrowse.value?.data != null){
                Log.d("TAG", "Artist name clicked: ${viewModel.albumBrowse.value?.data?.artists?.get(0)?.id}")
                val args = Bundle()
                args.putString("channelId", viewModel.albumBrowse.value?.data?.artists?.get(0)?.id)
                findNavController().navigateSafe(R.id.action_global_artistFragment, args)
            }
        }
        binding.cbLove.setOnCheckedChangeListener { cb, isChecked ->
            if (!isChecked){
                viewModel.albumEntity.value?.let { album -> viewModel.updateAlbumLiked(false, album.browseId) }
            }
            else {
                viewModel.albumEntity.value?.let { album -> viewModel.updateAlbumLiked(true, album.browseId) }
            }
        }
        binding.btShuffle.setOnClickListener {
            if (viewModel.albumBrowse.value is Resource.Success && viewModel.albumBrowse.value?.data != null){
                val args = Bundle()
                val index = Random.nextInt(viewModel.albumBrowse.value?.data!!.tracks.size)
                args.putString("type", Config.ALBUM_CLICK)
                args.putString("videoId", viewModel.albumBrowse.value?.data!!.tracks[index].videoId)
                args.putString("from", "Album \"${viewModel.albumBrowse.value?.data!!.title}\"")
                if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                    args.putInt("downloaded", 1)
                }
                args.putString("playlistId", viewModel.albumBrowse.value?.data?.audioPlaylistId?.replaceFirst("VL", ""))
                Queue.clear()
                Queue.setNowPlaying(viewModel.albumBrowse.value?.data!!.tracks[index])
                val shuffleList: ArrayList<Track> = arrayListOf()
                shuffleList.addAll(viewModel.albumBrowse.value?.data!!.tracks)
                shuffleList.remove(viewModel.albumBrowse.value?.data!!.tracks[index])
                shuffleList.shuffle()
                Queue.addAll(shuffleList)
                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
            }
            else if (viewModel.albumEntity.value != null && viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED){
                val args = Bundle()
                val index = Random.nextInt(viewModel.albumEntity.value?.tracks?.size!!)
                args.putString("type", Config.ALBUM_CLICK)
                args.putString("videoId", viewModel.albumEntity.value?.tracks?.get(index))
                args.putString("from", "Album \"${viewModel.albumEntity.value?.title}\"")
                if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                    args.putInt("downloaded", 1)
                }
                args.putString("playlistId", viewModel.albumEntity.value?.browseId?.replaceFirst("VL", ""))
                Queue.clear()
                Queue.setNowPlaying(viewModel.listTrack.value?.get(index)!!.toTrack())
                val shuffleList: ArrayList<Track> = arrayListOf()
                shuffleList.addAll(viewModel.listTrack.value.toArrayListTrack())
                shuffleList.remove(viewModel.listTrack.value?.get(index)!!.toTrack())
                shuffleList.shuffle()
                Queue.addAll(shuffleList)
                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
            }
            else {
                Snackbar.make(requireView(), "Error", Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.btPlayPause.setOnClickListener {
            if (viewModel.albumBrowse.value is Resource.Success && viewModel.albumBrowse.value?.data != null){
                val args = Bundle()
                args.putString("type", Config.ALBUM_CLICK)
                args.putString("videoId", viewModel.albumBrowse.value?.data!!.tracks[0].videoId)
                args.putString("from", "Album \"${viewModel.albumBrowse.value?.data!!.title}\"")
                if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                    args.putInt("downloaded", 1)
                }
                args.putString("playlistId", viewModel.albumBrowse.value?.data?.audioPlaylistId?.replaceFirst("VL", ""))
                Queue.clear()
                Queue.setNowPlaying(viewModel.albumBrowse.value?.data!!.tracks[0])
                Queue.addAll(viewModel.albumBrowse.value?.data!!.tracks as ArrayList<Track>)
                if (Queue.getQueue().size >= 1) {
                    Queue.removeFirstTrackForPlaylistAndAlbum()
                }
                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
            }
            else if (viewModel.albumEntity.value != null && viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED){
                val args = Bundle()
                args.putString("type", Config.ALBUM_CLICK)
                args.putString("videoId", viewModel.albumEntity.value?.tracks?.get(0))
                args.putString("from", "Album \"${viewModel.albumEntity.value?.title}\"")
                if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                    args.putInt("downloaded", 1)
                }
                args.putString("playlistId", viewModel.albumEntity.value?.browseId?.replaceFirst("VL", ""))
                Queue.clear()
                Queue.setNowPlaying(viewModel.listTrack.value?.get(0)!!.toTrack())
                Queue.addAll(viewModel.listTrack.value.toArrayListTrack())
                if (Queue.getQueue().size >= 1) {
                    Queue.removeFirstTrackForPlaylistAndAlbum()
                }
                findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
            }
            else {
                Snackbar.make(requireView(), "Error", Snackbar.LENGTH_SHORT).show()
            }
        }
        songsAdapter.setOnClickListener(object : TrackAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                if (viewModel.albumBrowse.value is Resource.Success && viewModel.albumBrowse.value?.data != null){
                    val args = Bundle()
                    args.putString("type", Config.ALBUM_CLICK)
                    args.putString("videoId", viewModel.albumBrowse.value?.data!!.tracks[position].videoId)
                    args.putString("from", "Album \"${viewModel.albumBrowse.value?.data!!.title}\"")
                    args.putInt("index", position)
                    if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                        args.putInt("downloaded", 1)
                    }
                    args.putString("playlistId", viewModel.albumBrowse.value?.data?.audioPlaylistId?.replaceFirst("VL", ""))
                    Queue.clear()
                    Queue.setNowPlaying(viewModel.albumBrowse.value?.data!!.tracks[position])
                    Queue.addAll(viewModel.albumBrowse.value?.data!!.tracks as ArrayList<Track>)
                    if (Queue.getQueue().size >= 1) {
                        Queue.removeTrackWithIndex(position)
                    }
                    findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
                }
                else if (viewModel.albumEntity.value != null && viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED){
                    val args = Bundle()
                    args.putString("type", Config.ALBUM_CLICK)
                    args.putString("videoId", viewModel.albumEntity.value?.tracks?.get(position))
                    args.putString("from", "Album \"${viewModel.albumEntity.value?.title}\"")
                    args.putInt("index", position)
                    if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                        args.putInt("downloaded", 1)
                    }
                    args.putString("playlistId", viewModel.albumEntity.value?.browseId?.replaceFirst("VL", ""))
                    Queue.clear()
                    Queue.setNowPlaying(viewModel.listTrack.value?.get(position)!!.toTrack())
                    Queue.addAll(viewModel.listTrack.value.toArrayListTrack())
                    if (Queue.getQueue().size >= 1) {
                        Queue.removeTrackWithIndex(position)
                    }
                    findNavController().navigateSafe(R.id.action_global_nowPlayingFragment, args)
                }
                else {
                    Snackbar.make(requireView(), "Error", Snackbar.LENGTH_SHORT).show()
                }
            }

        })

        binding.btDownload.setOnClickListener {
            if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
                for (i in viewModel.albumBrowse.value?.data?.tracks!!){
                    viewModel.insertSong(i.toSongEntity())
                }
                runBlocking {
                    delay(1000)
                    viewModel.listJob.emit(arrayListOf())
                }
                viewModel.getListTrack(viewModel.albumBrowse.value?.data?.tracks?.toListVideoId())
                viewModel.listTrack.observe(viewLifecycleOwner) {listTrack->
                    if (!listTrack.isNullOrEmpty()) {
                        val listJob: ArrayList<SongEntity> = arrayListOf()
                        for (song in listTrack){
                            if (song.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
                                listJob.add(song)
                            }
                        }
                        viewModel.listJob.value = listJob
                        Log.d("AlbumFragment", "ListJob: ${viewModel.listJob.value}")
                        viewModel.updatePlaylistDownloadState(
                            browseId!!,
                            DownloadState.STATE_DOWNLOADING
                        )
                        listJob.forEach {job ->
                            val downloadRequest =
                                DownloadRequest.Builder(job.videoId, job.videoId.toUri())
                                    .setData(job.title.toByteArray())
                                    .setCustomCacheKey(job.videoId)
                                    .build()
                            viewModel.updateDownloadState(
                                job.videoId,
                                DownloadState.STATE_DOWNLOADING
                            )
                            DownloadService.sendAddDownload(
                                requireContext(),
                                MusicDownloadService::class.java,
                                downloadRequest,
                                false
                            )
                            viewModel.getDownloadStateFromService(job.videoId)
                        }
                        viewModel.downloadFullAlbumState(browseId)
                    }
                }

            }
            else if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                Toast.makeText(requireContext(), getString(R.string.downloaded), Toast.LENGTH_SHORT).show()
            }
            else if (viewModel.albumEntity.value?.downloadState == DownloadState.STATE_DOWNLOADING) {
                Toast.makeText(requireContext(), getString(R.string.downloading), Toast.LENGTH_SHORT).show()
            }
        }
        binding.topAppBarLayout.addOnOffsetChangedListener { it, verticalOffset ->
            if(abs(it.totalScrollRange) == abs(verticalOffset)) {
                binding.topAppBar.background = viewModel.gradientDrawable.value
                if (viewModel.gradientDrawable.value != null ){
                    if (viewModel.gradientDrawable.value?.colors != null){
                        requireActivity().window.statusBarColor = viewModel.gradientDrawable.value?.colors!!.first()
                    }
                }
            }
            else
            {
                binding.topAppBar.background = null
                requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.albumDownloadState.collectLatest { albumDownloadState ->
                        when (albumDownloadState) {
                            DownloadState.STATE_PREPARING -> {
                                binding.btDownload.visibility = View.GONE
                                binding.animationDownloading.visibility = View.VISIBLE
                            }
                            DownloadState.STATE_DOWNLOADING -> {
                                binding.btDownload.visibility = View.GONE
                                binding.animationDownloading.visibility = View.VISIBLE
                            }
                            DownloadState.STATE_DOWNLOADED -> {
                                binding.btDownload.visibility = View.VISIBLE
                                binding.animationDownloading.visibility = View.GONE
                                binding.btDownload.setImageResource(R.drawable.baseline_downloaded)
                            }
                            DownloadState.STATE_NOT_DOWNLOADED -> {
                                binding.btDownload.visibility = View.VISIBLE
                                binding.animationDownloading.visibility = View.GONE
                                binding.btDownload.setImageResource(R.drawable.download_button)
                            }
                        }
                    }
                }
//                launch {
//                    viewModel.listJob.collectLatest {jobs->
//                        Log.d("AlbumFragment", "ListJob: $jobs")
//                        if (jobs.isNotEmpty()){
//                            var count = 0
//                            jobs.forEach { job ->
//                                if (job.downloadState == DownloadState.STATE_DOWNLOADED) {
//                                    count++
//                                }
//                            }
//                            Log.d("AlbumFragment", "Count: $count")
//                            if (count == jobs.size) {
//                                viewModel.updatePlaylistDownloadState(
//                                    browseId!!,
//                                    DownloadState.STATE_DOWNLOADED
//                                )
//                                Toast.makeText(
//                                    requireContext(),
//                                    getString(R.string.downloaded),
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
//                        }
//                    }
//                }
            }
            //job2.join()
        }
    }
    private fun fetchDataFromViewModel() {
            val response = viewModel.albumBrowse.value
            when (response){
                is Resource.Success -> {
                    response.data.let {
                        with(binding){
                            topAppBar.title = it?.title
                            btArtist.text = it?.artists?.get(0)?.name
                            tvYearAndCategory.text= context?.getString(R.string.year_and_category, it?.year, it?.type)
                            tvTrackCountAndDuration.text = context?.getString(R.string.album_length, it?.trackCount.toString(), it?.duration)
                            if (it?.description == null || it.description == ""){
                                tvDescription.originalText = "No description"
                            }
                            else {
                                tvDescription.originalText = it.description.toString()
                            }
                            when (it?.thumbnails?.size!!){
                                1 -> loadImage(it.thumbnails[0].url, viewModel.browseId.value!!)
                                2 -> loadImage(it.thumbnails[1].url, viewModel.browseId.value!!)
                                3 -> loadImage(it.thumbnails[2].url, viewModel.browseId.value!!)
                                4 -> loadImage(it.thumbnails[3].url, viewModel.browseId.value!!)
                                else -> {}
                            }
                            val tempList = arrayListOf<Any>()
                            for (i in it.tracks){
                                tempList.add(i)
                            }
                            songsAdapter.updateList(tempList)
                            binding.rootLayout.visibility = View.VISIBLE
                            binding.loadingLayout.visibility = View.GONE
                            val albumEntity = viewModel.albumEntity.value
                            if (albumEntity != null) {
                                viewModel.checkAllSongDownloaded(it.tracks as ArrayList<Track>)
                                viewModel.albumEntity.observe(viewLifecycleOwner){albumEntity2 ->
                                    when (albumEntity2.downloadState) {
                                        DownloadState.STATE_DOWNLOADED -> {
                                            btDownload.visibility = View.VISIBLE
                                            animationDownloading.visibility = View.GONE
                                            btDownload.setImageResource(R.drawable.baseline_downloaded)
                                        }
                                        DownloadState.STATE_DOWNLOADING -> {
                                            btDownload.visibility = View.GONE
                                            animationDownloading.visibility = View.VISIBLE
                                        }
                                        DownloadState.STATE_NOT_DOWNLOADED -> {
                                            btDownload.visibility = View.VISIBLE
                                            animationDownloading.visibility = View.GONE
                                            btDownload.setImageResource(R.drawable.download_button)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }

                else -> {}
        }
    }

    private fun fetchData(browseId: String, downloaded: Int = 0) {
        if (downloaded == 0) {
            viewModel.clearAlbumBrowse()
            viewModel.browseAlbum(browseId)
            viewModel.albumBrowse.observe(viewLifecycleOwner) { response ->
                when (response){
                    is Resource.Success -> {
                        response.data.let {
                            if (it != null){
                                viewModel.insertAlbum(it.toAlbumEntity(browseId))
                                with(binding){
                                    topAppBar.title = it.title
                                    btArtist.text = it.artists[0].name
                                    tvYearAndCategory.text= context?.getString(R.string.year_and_category, it.year, it.type)
                                    tvTrackCountAndDuration.text = context?.getString(R.string.album_length, it.trackCount.toString(), it.duration)
                                    if (it.description == null || it.description == ""){
                                        tvDescription.originalText = getString(R.string.no_description)
                                    }
                                    else {
                                        tvDescription.originalText = it.description.toString()
                                    }
                                    when (it.thumbnails?.size!!){
                                        1 -> loadImage(it.thumbnails[0].url, browseId)
                                        2 -> loadImage(it.thumbnails[1].url, browseId)
                                        3 -> loadImage(it.thumbnails[2].url, browseId)
                                        4 -> loadImage(it.thumbnails[3].url, browseId)
                                        else -> {}
                                    }
                                    val tempList = arrayListOf<Any>()
                                    for (i in it.tracks){
                                        tempList.add(i)
                                    }
                                    songsAdapter.updateList(tempList)
                                    binding.rootLayout.visibility = View.VISIBLE
                                    binding.loadingLayout.visibility = View.GONE
                                    viewModel.albumEntity.observe(viewLifecycleOwner) {albumEntity ->
                                        if (albumEntity != null) {
                                            when (albumEntity.downloadState) {
                                                DownloadState.STATE_DOWNLOADED -> {
                                                    btDownload.visibility = View.VISIBLE
                                                    animationDownloading.visibility = View.GONE
                                                    btDownload.setImageResource(R.drawable.baseline_downloaded)
                                                }
                                                DownloadState.STATE_DOWNLOADING -> {
                                                    btDownload.visibility = View.GONE
                                                    animationDownloading.visibility = View.VISIBLE
                                                }
                                                DownloadState.STATE_NOT_DOWNLOADED -> {
                                                    btDownload.visibility = View.VISIBLE
                                                    animationDownloading.visibility = View.GONE
                                                    btDownload.setImageResource(R.drawable.download_button)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        Snackbar.make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    }

                    else -> {}
                }
            }
        }
        else if (downloaded == 1){
            viewModel.getAlbum(browseId)
            with(binding){
                viewModel.albumEntity.observe(viewLifecycleOwner) {albumEntity ->
                    if (albumEntity != null) {
                        when (albumEntity.downloadState) {
                            DownloadState.STATE_DOWNLOADED -> {
                                btDownload.visibility = View.VISIBLE
                                animationDownloading.visibility = View.GONE
                                btDownload.setImageResource(R.drawable.baseline_downloaded)
                            }
                            DownloadState.STATE_DOWNLOADING -> {
                                btDownload.visibility = View.GONE
                                animationDownloading.visibility = View.VISIBLE
                            }
                            DownloadState.STATE_NOT_DOWNLOADED -> {
                                btDownload.visibility = View.VISIBLE
                                animationDownloading.visibility = View.GONE
                                btDownload.setImageResource(R.drawable.download_button)
                            }
                        }
                    }
                    topAppBar.title = albumEntity.title
                    btArtist.text = albumEntity.artistName?.get(0) ?: "Unknown"
                    tvYearAndCategory.text= context?.getString(R.string.year_and_category, albumEntity.year, albumEntity.type)
                    tvTrackCountAndDuration.text = context?.getString(R.string.album_length, albumEntity.trackCount.toString(), albumEntity.duration)
                    if (albumEntity.description == ""){
                        tvDescription.originalText = getString(R.string.no_description)
                    }
                    else {
                        tvDescription.originalText = albumEntity.description.toString()
                    }
                    loadImage(albumEntity.thumbnails!!, browseId)
                    viewModel.getListTrack(albumEntity.tracks)
                    viewModel.listTrack.observe(viewLifecycleOwner) { listTrack ->
                        val tempList = arrayListOf<Any>()
                        for (i in listTrack){
                            tempList.add(i)
                        }
                        tempList.sortBy { (albumEntity.tracks?.indexMap())?.get((it as SongEntity).videoId) }
                        songsAdapter.updateList(tempList)
                    }
                    binding.rootLayout.visibility = View.VISIBLE
                    binding.loadingLayout.visibility = View.GONE
                }
            }
        }
    }
    private fun loadImage(url: String, albumId: String){
        val request = ImageRequest.Builder(requireContext())
            .placeholder(R.drawable.holder)
            .data(Uri.parse(url))
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCacheKey(url)
            .target(
                onStart = {

                },
                onSuccess = { result ->
                    binding.ivAlbumArt.setImageDrawable(result)
                    if (viewModel.gradientDrawable.value != null) {
                        viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                            binding.fullRootLayout.background = it
                            toolbarBackground = it.colors?.get(0)
                            Log.d("TAG", "fetchData: $toolbarBackground")
                            //binding.topAppBar.background = ColorDrawable(toolbarBackground!!)
                            binding.topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                        }
                    }
                },
                onError = {
                    binding.ivAlbumArt.load(R.drawable.holder)
                }
            )
            .transformations(object : Transformation{
                override val cacheKey: String
                    get() = url

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

            }).build()
        ImageLoader(requireContext()).enqueue(request)
//        binding.ivAlbumArt.load(url) {
//            diskCachePolicy(CachePolicy.ENABLED)
//                .diskCacheKey(albumId)
//            transformations(object : Transformation{
//                override val cacheKey: String
//                    get() = albumId
//
//                override suspend fun transform(input: Bitmap, size: Size): Bitmap {
//                    val p = Palette.from(input).generate()
//                    val defaultColor = 0x000000
//                    var startColor = p.getDarkVibrantColor(defaultColor)
//                    if (startColor == defaultColor){
//                        startColor = p.getDarkMutedColor(defaultColor)
//                        if (startColor == defaultColor){
//                            startColor = p.getVibrantColor(defaultColor)
//                            if (startColor == defaultColor){
//                                startColor = p.getMutedColor(defaultColor)
//                                if (startColor == defaultColor){
//                                    startColor = p.getLightVibrantColor(defaultColor)
//                                    if (startColor == defaultColor){
//                                        startColor = p.getLightMutedColor(defaultColor)
//                                    }
//                                }
//                            }
//                        }
//                        Log.d("Check Start Color", "transform: $startColor")
//                    }
//                    startColor = ColorUtils.setAlphaComponent(startColor, 150)
//                    val endColor = resources.getColor(R.color.md_theme_dark_background, null)
//                    val gd = GradientDrawable(
//                        GradientDrawable.Orientation.TOP_BOTTOM,
//                        intArrayOf(startColor, endColor)
//                    )
//                    gd.cornerRadius = 0f
//                    gd.gradientType = GradientDrawable.LINEAR_GRADIENT
//                    gd.gradientRadius = 0.5f
//                    viewModel.gradientDrawable.postValue(gd)
//                    return input
//                }
//
//            })
//        }
    }
}