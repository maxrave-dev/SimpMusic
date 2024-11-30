package com.maxrave.simpmusic.ui.fragment.library

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil3.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.adapter.search.SearchItemAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.LOCAL_PLAYLIST_ID_DOWNLOADED
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentDownloadedBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.viewModel.DownloadedViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class DownloadedFragment : Fragment() {
    private var _binding: FragmentDownloadedBinding? = null
    val binding get() = _binding!!

    private val viewModel by viewModels<DownloadedViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private lateinit var downloadedAdapter: SearchItemAdapter
    private lateinit var listDownloaded: ArrayList<Any>
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDownloadedBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listDownloaded = ArrayList<Any>()
        downloadedAdapter = SearchItemAdapter(arrayListOf(), requireContext())
        binding.rvDownloaded.apply {
            adapter = downloadedAdapter
            layoutManager = LinearLayoutManager(context)
        }

        viewModel.getListDownloadedSong()
        viewModel.listDownloadedSong.observe(viewLifecycleOwner){ downloaded ->
            listDownloaded.clear()
            val tempDownloaded = mutableListOf<SongEntity>()
            for (i in downloaded.size - 1 downTo 0) {
                tempDownloaded.add(downloaded[i])
            }
            listDownloaded.addAll(tempDownloaded)
            downloadedAdapter.updateList(listDownloaded)
        }

        downloadedAdapter.setOnClickListener(object : SearchItemAdapter.onItemClickListener {
            @UnstableApi
            override fun onItemClick(position: Int, type: String) {
                val song = downloadedAdapter.getCurrentList()[position] as SongEntity
                val firstQueue = song.toTrack()
                viewModel.setQueueData(
                    QueueData(
                        listTracks = downloadedAdapter.getCurrentList().map { (it as SongEntity).toTrack()} as ArrayList<Track>,
                        firstPlayedTrack = firstQueue,
                        playlistId = LOCAL_PLAYLIST_ID_DOWNLOADED,
                        playlistName = getString(R.string.downloaded),
                        playlistType = PlaylistType.LOCAL_PLAYLIST,
                        continuation = null
                    )
                )
                viewModel.loadMediaItem(
                    firstQueue,
                    Config.PLAYLIST_CLICK,
                    position
                )
            }

            @UnstableApi
            override fun onOptionsClick(position: Int, type: String) {
                val song = listDownloaded[position] as SongEntity
                viewModel.getSongEntity(song.videoId)
                val dialog = BottomSheetDialog(requireContext())
                val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                with(bottomSheetView) {
                    btSleepTimer.visibility = View.GONE
                    viewModel.songEntity.observe(viewLifecycleOwner) { songEntity ->
                        if (songEntity != null) {
                            if (songEntity.liked) {
                                tvFavorite.text = getString(R.string.liked)
                                cbFavorite.isChecked = true
                            } else {
                                tvFavorite.text = getString(R.string.like)
                                cbFavorite.isChecked = false
                            }
                            when (song.downloadState) {
                                DownloadState.STATE_PREPARING -> {
                                    tvDownload.text = getString(R.string.preparing)
                                    ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                    setEnabledAll(btDownload, true)
                                }

                                DownloadState.STATE_NOT_DOWNLOADED -> {
                                    tvDownload.text = getString(R.string.download)
                                    ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                                    setEnabledAll(btDownload, true)
                                }

                                DownloadState.STATE_DOWNLOADING -> {
                                    tvDownload.text = getString(R.string.downloading)
                                    ivDownload.setImageResource(R.drawable.baseline_downloading_white)
                                    setEnabledAll(btDownload, true)
                                }

                                DownloadState.STATE_DOWNLOADED -> {
                                    tvDownload.text = getString(R.string.downloaded)
                                    ivDownload.setImageResource(R.drawable.baseline_downloaded)
                                    setEnabledAll(btDownload, true)
                                }
                            }
                        }
                    }
                    btChangeLyricsProvider.visibility = View.GONE
                    tvSongTitle.text = song.title
                    tvSongTitle.isSelected = true
                    tvSongArtist.text = song.artistName?.connectArtists()
                    tvSongArtist.isSelected = true
                    ivThumbnail.load(song.thumbnails)
                    if (song.albumName != null) {
                        setEnabledAll(btAlbum, true)
                        tvAlbum.text = song.albumName
                    } else {
                        tvAlbum.text = getString(R.string.no_album)
                        setEnabledAll(btAlbum, false)
                    }
                    btAlbum.setOnClickListener {
                        val albumId = song.albumId
                        if (albumId != null) {
                            findNavController().navigateSafe(
                                R.id.action_global_albumFragment,
                                Bundle().apply {
                                    putString("browseId", albumId)
                                })
                            dialog.dismiss()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.no_album),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    btAddQueue.setOnClickListener {
                        sharedViewModel.addToQueue(song.toTrack())
                    }
                    btPlayNext.setOnClickListener {
                        sharedViewModel.playNext(song.toTrack())
                    }
                    btRadio.setOnClickListener {
                        val args = Bundle()
                        args.putString("radioId", "RDAMVM${song.videoId}")
                        args.putString(
                            "videoId",
                            song.videoId
                        )
                        dialog.dismiss()
                        findNavController().navigateSafe(R.id.action_global_playlistFragment, args)
                    }
                    btLike.setOnClickListener {
                        if (cbFavorite.isChecked){
                            cbFavorite.isChecked = false
                            tvFavorite.text = getString(R.string.like)
                            viewModel.updateLikeStatus(song.videoId, 0)
                            viewModel.listDownloadedSong.observe(viewLifecycleOwner){ downloaded ->
                                listDownloaded.clear()
                                val tempDownloaded = mutableListOf<SongEntity>()
                                for (i in downloaded.size - 1 downTo 0) {
                                    tempDownloaded.add(downloaded[i])
                                }
                                listDownloaded.addAll(tempDownloaded)
                                downloadedAdapter.updateList(listDownloaded)
                            }
                        }
                        else {
                            cbFavorite.isChecked = true
                            tvFavorite.text = getString(R.string.liked)
                            viewModel.updateLikeStatus(song.videoId, 1)
                            viewModel.listDownloadedSong.observe(viewLifecycleOwner) { downloaded ->
                                listDownloaded.clear()
                                val tempDownloaded = mutableListOf<SongEntity>()
                                for (i in downloaded.size - 1 downTo 0) {
                                    tempDownloaded.add(downloaded[i])
                                }
                                listDownloaded.addAll(tempDownloaded)
                                downloadedAdapter.updateList(listDownloaded)
                            }
                        }
                    }
                    btAddPlaylist.setOnClickListener {
                        viewModel.getAllLocalPlaylist()
                        val listLocalPlaylist: ArrayList<LocalPlaylistEntity> = arrayListOf()
                        val addPlaylistDialog = BottomSheetDialog(requireContext())
                        val viewAddPlaylist = BottomSheetAddToAPlaylistBinding.inflate(layoutInflater)
                        val addToAPlaylistAdapter = AddToAPlaylistAdapter(arrayListOf())
                        addToAPlaylistAdapter.setVideoId(song.videoId)
                        viewAddPlaylist.rvLocalPlaylists.apply {
                            adapter = addToAPlaylistAdapter
                            layoutManager = LinearLayoutManager(requireContext())
                        }
                        viewModel.localPlaylist.observe(viewLifecycleOwner) {list ->
                            Log.d("Check Local Playlist", list.toString())
                            listLocalPlaylist.clear()
                            listLocalPlaylist.addAll(list)
                            addToAPlaylistAdapter.updateList(listLocalPlaylist)
                        }
                        addToAPlaylistAdapter.setOnItemClickListener(object : AddToAPlaylistAdapter.OnItemClickListener{
                            override fun onItemClick(position: Int) {
                                val playlist = listLocalPlaylist[position]
                                viewModel.updateInLibrary(song.videoId)
                                val tempTrack = ArrayList<String>()
                                if (playlist.tracks != null) {
                                    tempTrack.addAll(playlist.tracks)
                                }
                                if (!tempTrack.contains(song.videoId) &&
                                    playlist.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced &&
                                    playlist.youtubePlaylistId != null) {
                                    viewModel.addToYouTubePlaylist(playlist.id, playlist.youtubePlaylistId, song.videoId)
                                }
                                if (!tempTrack.contains(song.videoId)) {
                                    viewModel.insertPairSongLocalPlaylist(
                                        PairSongLocalPlaylist(
                                            playlistId = playlist.id,
                                            songId = song.videoId,
                                            position = playlist.tracks?.size ?: 0,
                                            inPlaylist = LocalDateTime.now()
                                        )
                                    )
                                    tempTrack.add(song.videoId)
                                }
                                viewModel.updateLocalPlaylistTracks(
                                    tempTrack.removeConflicts(),
                                    playlist.id
                                )
                                addPlaylistDialog.dismiss()
                                dialog.dismiss()
                            }
                        })
                        addPlaylistDialog.setContentView(viewAddPlaylist.root)
                        addPlaylistDialog.setCancelable(true)
                        addPlaylistDialog.show()
                    }
                    btDownload.setOnClickListener {
                        if (tvDownload.text == getString(R.string.downloaded) || tvDownload.text == getString(R.string.downloading)){
                            DownloadService.sendRemoveDownload(
                                requireContext(),
                                MusicDownloadService::class.java,
                                song.videoId,
                                false
                            )
                            viewModel.updateDownloadState(
                                song.videoId,
                                DownloadState.STATE_NOT_DOWNLOADED
                            )
                            tvDownload.text = getString(R.string.download)
                            ivDownload.setImageResource(R.drawable.outline_download_for_offline_24)
                            setEnabledAll(btDownload, true)
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.removed_download),
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.listDownloadedSong.observe(viewLifecycleOwner){ downloaded ->
                                listDownloaded.clear()
                                val tempDownloaded = mutableListOf<SongEntity>()
                                for (i in downloaded.size - 1 downTo 0) {
                                    tempDownloaded.add(downloaded[i])
                                }
                                listDownloaded.addAll(tempDownloaded)
                                downloadedAdapter.updateList(listDownloaded)
                            }
                        }
                    }

                    btSeeArtists.setOnClickListener {
                        val subDialog = BottomSheetDialog(requireContext())
                        val subBottomSheetView = BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                        Log.d("FavoriteFragment", "onOptionsClick: ${song.artistId}")
                        if (!song.artistName.isNullOrEmpty()) {
                            val tempArtist = mutableListOf<Artist>()
                            for (i in 0 until song.artistName.size) {
                                tempArtist.add(Artist(name = song.artistName[i], id = song.artistId?.get(i)))
                            }
                            Log.d("FavoriteFragment", "onOptionsClick: $tempArtist")
                            val artistAdapter = SeeArtistOfNowPlayingAdapter(tempArtist)
                            subBottomSheetView.rvArtists.apply {
                                adapter = artistAdapter
                                layoutManager = LinearLayoutManager(requireContext())
                            }
                            artistAdapter.setOnClickListener(object : SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                override fun onItemClick(position: Int) {
                                    val artist = tempArtist[position]
                                    if (artist.id != null) {
                                        findNavController().navigateSafe(R.id.action_global_artistFragment, Bundle().apply {
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
                    btShare.setOnClickListener {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        val url = "https://youtube.com/watch?v=${song.videoId}"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                        val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share_url))
                        startActivity(chooserIntent)
                    }
                }
                dialog.setCancelable(true)
                dialog.setContentView(bottomSheetView.root)
                dialog.show()
            }

        })

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val job1 = launch {
                    sharedViewModel.downloadList.collect {
                        downloadedAdapter.setDownloadedList(it)
                    }
                }
                val job2 = launch {
                    combine(sharedViewModel.nowPlayingState.distinctUntilChangedBy {
                        it?.songEntity?.videoId
                    }, sharedViewModel.controllerState.distinctUntilChangedBy {
                        it.isPlaying
                    }) { nowPlaying, controllerState ->
                        Pair(nowPlaying, controllerState)
                    }.collect {
                        val songEntity = it.first?.songEntity
                        if (songEntity != null && it.second.isPlaying) {
                            downloadedAdapter.setNowPlaying(songEntity.videoId)
                        } else {
                            downloadedAdapter.setNowPlaying(null)
                        }
                    }
                }
                job1.join()
                job2.join()
            }
        }
    }
}