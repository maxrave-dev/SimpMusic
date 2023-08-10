package com.maxrave.simpmusic.ui.fragment.other

import android.content.Intent
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
import coil.load
import coil.size.Size
import coil.transform.Transformation
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.adapter.playlist.PlaylistItemAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentPlaylistBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.toArrayListTrack
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toListVideoId
import com.maxrave.simpmusic.extension.toPlaylistEntity
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

@AndroidEntryPoint
class PlaylistFragment: Fragment() {
    private val viewModel by activityViewModels<PlaylistViewModel>()
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

    @UnstableApi
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

        playlistItemAdapter = PlaylistItemAdapter(arrayListOf())
        binding.rvListSong.apply {
            adapter = playlistItemAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        var id = requireArguments().getString("id")
        val downloaded = arguments?.getInt("downloaded")
        if (id == null || id == viewModel.id.value){
            id = viewModel.id.value
            fetchDataFromViewModel()
        }
        else {
            viewModel.updateId(id)
            if (downloaded == null || downloaded == 0){
                fetchData(id)
            }
            if (downloaded == 1){
                fetchData(id, downloaded = 1)
            }
        }

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.cbLove.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked){
                viewModel.playlistEntity.value?.let { playlist -> viewModel.updatePlaylistLiked(false, playlist.id) }
            }
            else {
                viewModel.playlistEntity.value?.let { playlist -> viewModel.updatePlaylistLiked(true, playlist.id) }
            }
        }

        binding.btPlayPause.setOnClickListener {
            if (viewModel.playlistBrowse.value is Resource.Success && viewModel.playlistBrowse.value?.data != null){
                val args = Bundle()
                args.putString("type", Config.PLAYLIST_CLICK)
                args.putString("videoId", viewModel.playlistBrowse.value?.data?.tracks?.get(0)?.videoId)
                args.putString("from", "Playlist \"${viewModel.playlistBrowse.value?.data?.title}\"")
                if (viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                    args.putInt("downloaded", 1)
                }
                Queue.clear()
                Queue.setNowPlaying(viewModel.playlistBrowse.value?.data!!.tracks[0])
                if (Queue.getQueue().size > 1) {
                    Queue.addAll(viewModel.playlistBrowse.value?.data!!.tracks as ArrayList<Track>)
                    Queue.removeFirstTrackForPlaylistAndAlbum()
                }
                findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
            }
            else if (viewModel.playlistEntity.value != null && viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED){
                val args = Bundle()
                args.putString("type", Config.ALBUM_CLICK)
                args.putString("videoId", viewModel.playlistEntity.value?.tracks?.get(0))
                args.putString("from", "Album \"${viewModel.playlistEntity.value?.title}\"")
                if (viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                    args.putInt("downloaded", 1)
                }
                Queue.clear()
                Queue.setNowPlaying(viewModel.listTrack.value?.get(0)!!.toTrack())
                if (Queue.getQueue().size > 1) {
                    Queue.addAll(viewModel.listTrack.value.toArrayListTrack())
                    Queue.removeFirstTrackForPlaylistAndAlbum()
                }
                findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
            }
            else {
                Snackbar.make(requireView(),
                    getString(R.string.playlist_is_empty), Snackbar.LENGTH_SHORT).show()
            }
        }

        playlistItemAdapter.setOnClickListener(object: PlaylistItemAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                if (viewModel.playlistBrowse.value is Resource.Success && viewModel.playlistBrowse.value?.data != null){
                    val args = Bundle()
                    args.putString("type", Config.ALBUM_CLICK)
                    args.putString("videoId", viewModel.playlistBrowse.value?.data!!.tracks[position].videoId)
                    args.putString("from", "Album \"${viewModel.playlistBrowse.value?.data!!.title}\"")
                    args.putInt("index", position)
                    if (viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                        args.putInt("downloaded", 1)
                    }
                    Queue.clear()
                    Queue.setNowPlaying(viewModel.playlistBrowse.value?.data!!.tracks[position])
                    if (Queue.getQueue().size > 1) {
                        Queue.addAll(viewModel.playlistBrowse.value?.data!!.tracks as ArrayList<Track>)
                        Queue.removeTrackWithIndex(position)
                    }
                    findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
                }
                else if (viewModel.playlistEntity.value != null && viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED){
                    val args = Bundle()
                    args.putString("type", Config.ALBUM_CLICK)
                    args.putString("videoId", viewModel.playlistEntity.value?.tracks?.get(position))
                    args.putString("from", "Album \"${viewModel.playlistEntity.value?.title}\"")
                    if (viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                        args.putInt("downloaded", 1)
                    }
                    Queue.clear()
                    Queue.setNowPlaying(viewModel.listTrack.value?.get(position)!!.toTrack())
                    if (Queue.getQueue().size > 1) {
                        Queue.addAll(viewModel.listTrack.value.toArrayListTrack())
                        Queue.removeTrackWithIndex(position)
                    }
                    findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
                }
                else {
                    Snackbar.make(requireView(), getString(R.string.error), Snackbar.LENGTH_SHORT).show()
                }
            }
        })
        playlistItemAdapter.setOnOptionClickListener(object: PlaylistItemAdapter.OnOptionClickListener{
            override fun onOptionClick(position: Int) {
                val song = viewModel.playlistBrowse.value?.data!!.tracks[position]
                viewModel.getSongEntity(song.toSongEntity())
                val dialog = BottomSheetDialog(requireContext())
                val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                with(bottomSheetView) {
                    viewModel.songEntity.observe(viewLifecycleOwner) { songEntity ->
                        if (songEntity.liked) {
                            tvFavorite.text = getString(R.string.liked)
                            cbFavorite.isChecked = true
                        } else {
                            tvFavorite.text = getString(R.string.like)
                            cbFavorite.isChecked = false
                        }
                    }
                    tvSongTitle.text = song.title
                    tvSongTitle.isSelected = true
                    tvSongArtist.text = song.artists.toListName().connectArtists()
                    tvSongArtist.isSelected = true
                    ivThumbnail.load(song.thumbnails)

                    btLike.setOnClickListener {
                        if (cbFavorite.isChecked) {
                            cbFavorite.isChecked = false
                            tvFavorite.text = getString(R.string.like)
                            viewModel.updateLikeStatus(song.videoId, 0)
                        } else {
                            cbFavorite.isChecked = true
                            tvFavorite.text = getString(R.string.liked)
                            viewModel.updateLikeStatus(song.videoId, 1)
                        }
                    }
                    btSeeArtists.setOnClickListener {
                        val subDialog = BottomSheetDialog(requireContext())
                        val subBottomSheetView =
                            BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                        if (!song.artists.isNullOrEmpty()) {
                            val artistAdapter = SeeArtistOfNowPlayingAdapter(song.artists)
                            subBottomSheetView.rvArtists.apply {
                                adapter = artistAdapter
                                layoutManager = LinearLayoutManager(requireContext())
                            }
                            artistAdapter.setOnClickListener(object :
                                SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                override fun onItemClick(position: Int) {
                                    val artist = song.artists[position]
                                    if (artist.id != null) {
                                        findNavController().navigate(
                                            R.id.action_global_artistFragment,
                                            Bundle().apply {
                                                putString("channelId", artist.id)
                                            })
                                        subDialog.dismiss()
                                        dialog.dismiss()
                                    }
                                }

                            })
                        }

                        subDialog.setCancelable(true)
                        subDialog.setContentView(subBottomSheetView.root)
                        subDialog.show()
                    }
                    btDownload.visibility = View.GONE
                    btAddPlaylist.setOnClickListener {
                        viewModel.getLocalPlaylist()
                        val listLocalPlaylist: ArrayList<LocalPlaylistEntity> = arrayListOf()
                        val addPlaylistDialog = BottomSheetDialog(requireContext())
                        val viewAddPlaylist =
                            BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
                        val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
                        viewAddPlaylist.rvLocalPlaylists.apply {
                            adapter = addToAPlaylistAdapter
                            layoutManager = LinearLayoutManager(requireContext())
                        }
                        viewModel.listLocalPlaylist.observe(viewLifecycleOwner) { list ->
                            Log.d("Check Local Playlist", list.toString())
                            listLocalPlaylist.clear()
                            listLocalPlaylist.addAll(list)
                            addToAPlaylistAdapter.updateList(listLocalPlaylist)
                        }
                        addToAPlaylistAdapter.setOnItemClickListener(object :
                            AddToAPlaylistAdapter.OnItemClickListener {
                            override fun onItemClick(position: Int) {
                                val playlist = listLocalPlaylist[position]
                                val tempTrack = ArrayList<String>()
                                if (playlist.tracks != null) {
                                    tempTrack.addAll(playlist.tracks)
                                }
                                tempTrack.add(song.videoId)
                                tempTrack.removeConflicts()
                                viewModel.updateLocalPlaylistTracks(tempTrack, playlist.id)
                                addPlaylistDialog.dismiss()
                                dialog.dismiss()
                            }
                        })
                        addPlaylistDialog.setContentView(viewAddPlaylist.root)
                        addPlaylistDialog.setCancelable(true)
                        addPlaylistDialog.show()
                    }
                    btShare.setOnClickListener {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        val url = "https://youtube.com/watch?v=${song.videoId}"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                        val chooserIntent =
                            Intent.createChooser(shareIntent, getString(R.string.share_url))
                        startActivity(chooserIntent)
                    }
                    dialog.setCancelable(true)
                    dialog.setContentView(bottomSheetView.root)
                    dialog.show()
                }
            }
        })
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
        binding.btDownload.setOnClickListener {
        if (viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
//                if (!viewModel.prevPlaylistDownloading.value){
//                    viewModel.downloading()
                    for (i in viewModel.playlistBrowse.value?.data?.tracks!!){
                        viewModel.insertSong(i.toSongEntity())
                    }
                    runBlocking {
                        delay(1000)
                        viewModel.listJob.emit(arrayListOf())
                    }
                    viewModel.getListTrack(viewModel.playlistBrowse.value?.data?.tracks?.toListVideoId())
                    viewModel.listTrack.observe(viewLifecycleOwner) {listTrack->
                        if (!listTrack.isNullOrEmpty()) {
                            val listJob: ArrayList<SongEntity> = arrayListOf()
                            for (song in viewModel.listTrack.value!!) {
                                if (song.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
                                    listJob.add(song)
                                }
                            }
                            viewModel.listJob.value = listJob
                            Log.d("PlaylistFragment", "ListJob: ${viewModel.listJob.value}")
                            viewModel.updatePlaylistDownloadState(
                                id!!,
                                DownloadState.STATE_DOWNLOADING
                            )
                            listJob.forEach { job ->
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
                            viewModel.downloadFullPlaylistState(id)
                        }
                    }
//                }
//                else{
//                    Toast.makeText(requireContext(), getString(R.string.please_wait_before_playlist_downloaded), Toast.LENGTH_SHORT).show()
//                }
            }
            else if (viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                Toast.makeText(requireContext(), getString(R.string.downloaded), Toast.LENGTH_SHORT).show()
            }
            else if (viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADING) {
                Toast.makeText(requireContext(), getString(R.string.downloading), Toast.LENGTH_SHORT).show()
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.playlistDownloadState.collectLatest { playlistDownloadState ->
                        when (playlistDownloadState) {
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
            }
        }
    }
    private fun fetchDataFromViewModel(){
            val response = viewModel.playlistBrowse.value
            when (response) {
                is Resource.Success -> {
                    response.data.let {
                        with(binding){
                            topAppBar.title = it?.title
                            tvPlaylistAuthor.text = it?.author?.name
                            tvYearAndCategory.text = requireContext().getString(R.string.year_and_category, it?.year.toString(), "Playlist")
                            tvTrackCountAndDuration.text = requireContext().getString(R.string.album_length, it?.trackCount.toString(), "")
                            if (it?.description != null){
                                tvDescription.originalText = it.description
                            } else {
                                tvDescription.originalText = getString(R.string.no_description)
                            }
                            loadImage(it?.thumbnails?.last()?.url)
                            val list: ArrayList<Any> = arrayListOf()
                            list.addAll(it?.tracks as ArrayList<Track>)
                            playlistItemAdapter.updateList(list)
                            if (viewModel.gradientDrawable.value == null) {
                                viewModel.gradientDrawable.observe(viewLifecycleOwner) { gradient ->
                                    fullRootLayout.background = gradient
                                    toolbarBackground = gradient?.colors?.get(0)
                                    topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                                }
                            }
                            else {
                                fullRootLayout.background = gradientDrawable
                                topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                            }
                            binding.rootLayout.visibility = View.VISIBLE
                            binding.loadingLayout.visibility = View.GONE
                            val playlistEntity = viewModel.playlistEntity.value
                            if (playlistEntity != null){
                                viewModel.checkAllSongDownloaded(it.tracks as ArrayList<Track>)
                                viewModel.playlistEntity.observe(viewLifecycleOwner){ playlistEntity2 ->
                                    when (playlistEntity2.downloadState) {
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

    private fun fetchData(id: String, downloaded: Int = 0){
        if (downloaded == 0) {
            viewModel.clearPlaylistBrowse()
            viewModel.browsePlaylist(id)
            viewModel.playlistBrowse.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data.let {
                            with(binding){
                                if (it != null) {
                                    viewModel.insertPlaylist(it.toPlaylistEntity())
                                }
                                topAppBar.title = it?.title
                                tvPlaylistAuthor.text = it?.author?.name
                                tvYearAndCategory.text = requireContext().getString(R.string.year_and_category, it?.year.toString(), "Playlist")
                                tvTrackCountAndDuration.text = requireContext().getString(R.string.album_length, it?.trackCount.toString(), "")
                                if (it?.description != null){
                                    tvDescription.originalText = it.description
                                } else {
                                    tvDescription.originalText = getString(R.string.no_description)
                                }
                                loadImage(it?.thumbnails?.last()?.url)
                                val list: ArrayList<Any> = arrayListOf()
                                list.addAll(it?.tracks as ArrayList<Track>)
                                playlistItemAdapter.updateList(list)
                                if (viewModel.gradientDrawable.value == null) {
                                    viewModel.gradientDrawable.observe(viewLifecycleOwner)
                                        { gradient ->
                                            fullRootLayout.background = gradient
                                            toolbarBackground = gradient?.colors?.get(0)
                                            topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                                        }
                                }
                                else {
                                    fullRootLayout.background = gradientDrawable
                                    topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                                }
                                binding.rootLayout.visibility = View.VISIBLE
                                binding.loadingLayout.visibility = View.GONE
                                viewModel.playlistEntity.observe(viewLifecycleOwner) { playlistEntity2 ->
                                    if (playlistEntity2 != null) {
                                        when (playlistEntity2.downloadState) {
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
        }
        else if (downloaded == 1) {
            viewModel.getPlaylist(id)
            with(binding) {
                viewModel.playlistEntity.observe(viewLifecycleOwner) { playlistEntity ->
                    if (playlistEntity != null){
                        when (playlistEntity.downloadState) {
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
                        topAppBar.title = playlistEntity.title
                        tvPlaylistAuthor.text = playlistEntity.author
                        tvYearAndCategory.text = requireContext().getString(R.string.year_and_category, playlistEntity.year.toString(), "Playlist")
                        tvTrackCountAndDuration.text = requireContext().getString(R.string.album_length, playlistEntity.trackCount.toString(),
                            ""
                        )
                        if (playlistEntity.description != ""){
                            tvDescription.originalText = playlistEntity.description
                        } else {
                            tvDescription.originalText = "No description"
                        }
                        loadImage(playlistEntity.thumbnails)
                        if (viewModel.gradientDrawable.value == null) {
                            viewModel.gradientDrawable.observe(viewLifecycleOwner) { gradient ->
                                fullRootLayout.background = gradient
                                toolbarBackground = gradient?.colors?.get(0)
                                topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                            }
                        }
                        else {
                            fullRootLayout.background = gradientDrawable
                            topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
                        }
                        viewModel.getListTrack(playlistEntity.tracks)
                        viewModel.listTrack.observe(viewLifecycleOwner) { listTrack ->
                            val tempList = arrayListOf<Any>()
                            for (i in listTrack){
                                tempList.add(i)
                            }
                            playlistItemAdapter.updateList(tempList)
                            binding.rootLayout.visibility = View.VISIBLE
                            binding.loadingLayout.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun loadImage(url: String?) {
        if (url != null){
            binding.ivPlaylistArt.load(url){
                transformations(object : Transformation{
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