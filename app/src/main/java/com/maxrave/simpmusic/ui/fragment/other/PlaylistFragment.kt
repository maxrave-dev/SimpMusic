package com.maxrave.simpmusic.ui.fragment.other

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
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
import coil.request.CachePolicy
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.adapter.playlist.AddToAPlaylistAdapter
import com.maxrave.simpmusic.adapter.playlist.PlaylistItemAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.databinding.BottomSheetAddToAPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetPlaylistMoreBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentPlaylistBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.removeConflicts
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.extension.setStatusBarsColor
import com.maxrave.simpmusic.extension.toArrayListTrack
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toListVideoId
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.viewModel.PlaylistUIState
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.random.Random

class PlaylistFragment : Fragment() {
    private val tag = "PlaylistFragment"

    private val viewModel by activityViewModels<PlaylistViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private var _binding: FragmentPlaylistBinding? = null
    val binding get() = _binding!!

    private var gradientDrawable: GradientDrawable? = null
    private var toolbarBackground: Int? = null

    private lateinit var playlistItemAdapter: PlaylistItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireArguments().clear()
        setStatusBarsColor(
            ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark),
            requireActivity(),
        )
        _binding = null
    }

    @UnstableApi
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getLocation()
        lifecycleScope.launch {
            viewModel.liked.collect { liked ->
                binding.cbLove.isChecked = liked
            }
        }
        if (viewModel.gradientDrawable.value != null) {
            gradientDrawable = viewModel.gradientDrawable.value
            toolbarBackground = gradientDrawable?.colors?.get(0)
        }

        playlistItemAdapter = PlaylistItemAdapter(arrayListOf())
        binding.rvListSong.apply {
            adapter = playlistItemAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            setHasFixedSize(false)
        }
        val id = requireArguments().getString("id")
        val downloaded = arguments?.getInt("downloaded")
        val radioId = arguments?.getString("radioId")
        val channelId = arguments?.getString("channelId")
        Log.w(tag, "radioId: $radioId")
        val videoId = arguments?.getString("videoId")
        // 3 truong hop:
        /*
        1. La Playlist
        2. La Radio
        3. Downloaded Playlist
        4. Luc lai tu ViewModel
         */
        if (id != null || radioId != null) {
            Log.w(tag, "id: $id")
            Log.w(tag, "radioId: $radioId")
            if (radioId != null ||
                id?.startsWith("RDEM") == true ||
                id?.startsWith("RDAMVM") == true
            ) {
                viewModel.updateIsRadio(true)
                if (radioId != null) {
                    fetchDataWithRadio(radioId, videoId, channelId)
                } else if (id != null) {
                    fetchDataWithRadio(id)
                }
            } else if (id != null && id.startsWith("RDAT")) {
                viewModel.updateIsRadio(true)
                fetchRDATRadio(id)
            } else if (id != null) {
                viewModel.updateIsRadio(false)
                if (!requireArguments().getBoolean("youtube")) {
                    viewModel.checkSuccess()
                }
                if (downloaded == null || downloaded == 0) {
                    fetchData(id)
                }
                if (downloaded == 1) {
                    fetchData(id, downloaded = 1)
                }
            }
        }
//        if (id == null && radioId == null || id == viewModel.id.value && radioId == null || id == null && radioId == viewModel.id.value) {
//            id = viewModel.id.value
//            if (id?.startsWith("RDEM") == true || id?.startsWith("RDAMVM") == true || id?.startsWith("RDAT") == true) {
//                viewModel.updateIsRadio(true)
//            } else {
//                viewModel.updateIsRadio(false)
//            }
//            if (!requireArguments().getBoolean("youtube")) {
//                viewModel.checkSuccess()
//            } else {
//                if (id != null) {
//                    if (downloaded == null || downloaded == 0) {
//                        fetchData(id)
//                    }
//                    if (downloaded == 1) {
//                        fetchData(id, downloaded = 1)
//                    }
//                }
//            }
//        } else if (radioId != null && id == null) {
//            viewModel.clearPlaylistBrowse()
//            viewModel.updateIsRadio(true)
//            if (videoId != null) {
//                fetchDataWithRadio(radioId, videoId)
//            } else if (channelId != null) {
//                fetchDataWithRadio(radioId, null, channelId)
//            }
//        } else if (id != null && id.startsWith("RDEM") || id != null && id.startsWith("RDAMVM") || id?.startsWith("RDAT") == true) {
//            viewModel.clearPlaylistEntity()
//            viewModel.clearPlaylistBrowse()
//            fetchData(id)
//        } else if (id != null) {
//            viewModel.updateId(id)
//            if (downloaded == null || downloaded == 0) {
//                fetchData(id)
//            }
//            if (downloaded == 1) {
//                fetchData(id, downloaded = 1)
//            }
//        }

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.cbLove.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                viewModel.playlistEntity.value?.let { playlist ->
                    viewModel.updatePlaylistLiked(
                        false,
                        playlist.id,
                    )
                }
            } else {
                viewModel.playlistEntity.value?.let { playlist ->
                    viewModel.updatePlaylistLiked(
                        true,
                        playlist.id,
                    )
                }
            }
        }
        binding.btMore.setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val moreView = BottomSheetPlaylistMoreBinding.inflate(layoutInflater)
            if (viewModel.isRadio.value == false) {
                moreView.ivThumbnail.load(viewModel.playlistEntity.value?.thumbnails)
                moreView.tvSongTitle.text = viewModel.playlistEntity.value?.title
                moreView.tvSongArtist.text = viewModel.playlistEntity.value?.author
                moreView.btShare.setOnClickListener {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    val url = "https://youtube.com/playlist?list=${
                        viewModel.playlistEntity.value?.id?.replaceFirst(
                            "VL",
                            "",
                        )
                    }"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                    val chooserIntent =
                        Intent.createChooser(shareIntent, getString(R.string.share_url))
                    startActivity(chooserIntent)
                }
            } else {
                moreView.ivThumbnail.load(
                    viewModel.playlistBrowse.value
                        ?.thumbnails
                        ?.lastOrNull()
                        ?.url,
                )
                moreView.tvSongTitle.text =
                    viewModel.playlistBrowse.value
                        ?.title
                moreView.tvSongArtist.text =
                    viewModel.playlistBrowse.value
                        ?.author
                        ?.name
                moreView.btShare.setOnClickListener {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    val url = "https://youtube.com/playlist?list=${
                        viewModel.playlistBrowse.value?.id?.replaceFirst(
                            "VL",
                            "",
                        )
                    }"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                    val chooserIntent =
                        Intent.createChooser(shareIntent, getString(R.string.share_url))
                    startActivity(chooserIntent)
                }
            }
            if (requireArguments().getBoolean("youtube")) {
                Log.w(tag, "id check: $id")
                moreView.btSync.visibility = View.VISIBLE
                viewModel.checkSyncedPlaylist(id)
                lifecycleScope.launch {
                    viewModel.localPlaylistIfYouTubePlaylist.collectLatest { ytPlaylist ->
                        Log.w(tag, "ytPlaylist: ${ytPlaylist?.youtubePlaylistId}")
                        Log.w(tag, "id: $id")
                        if (ytPlaylist != null) {
                            val tempId = ytPlaylist.youtubePlaylistId
                            if (tempId == id) {
                                moreView.tvSync.text =
                                    context?.getString(R.string.saved_to_local_playlist)
                                setEnabledAll(moreView.btSync, false)
                            }
                        } else {
                            moreView.tvSync.text =
                                context?.getString(R.string.save_to_local_playlist)
                        }
                    }
                }
                moreView.btSync.setOnClickListener {
                    if (moreView.tvSync.text == context?.getString(R.string.save_to_local_playlist)) {
                        val playlist = viewModel.playlistBrowse.value
                        if (playlist != null) {
                            val localPlaylistEntity =
                                LocalPlaylistEntity(
                                    title = playlist.title,
                                    thumbnail = playlist.thumbnails.lastOrNull()?.url,
                                    youtubePlaylistId = playlist.id,
                                    syncedWithYouTubePlaylist = 1,
                                    tracks = playlist.tracks.toListVideoId(),
                                    downloadState = DownloadState.STATE_NOT_DOWNLOADED,
                                    syncState = LocalPlaylistEntity.YouTubeSyncState.Synced,
                                )
                            viewModel.insertLocalPlaylist(localPlaylistEntity, playlist.tracks)
                            moreView.tvSync.text =
                                context?.getString(R.string.saved_to_local_playlist)
                        }
                    } else {
                        Snackbar
                            .make(
                                requireView(),
                                getString(R.string.if_you_want_to_unsync_this_playlist_please_go_to_local_playlist),
                                Snackbar.LENGTH_SHORT,
                            ).show()
                    }
                }
            } else {
                moreView.btSync.visibility = View.GONE
            }

            moreView.btAddToQueue.setOnClickListener {
                if (viewModel.playlistBrowse.value != null) {
                    val list =
                        viewModel.playlistBrowse.value
                            ?.tracks as ArrayList<Track>
                    sharedViewModel.addListToQueue(list)
                } else if (viewModel.playlistEntity.value != null &&
                    viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED
                ) {
                    val list = viewModel.listTrack.value.toArrayListTrack()
                    sharedViewModel.addListToQueue(list)
                } else {
                    Snackbar
                        .make(
                            requireView(),
                            getString(R.string.playlist_is_empty),
                            Snackbar.LENGTH_SHORT,
                        ).show()
                }
            }

            bottomSheetDialog.setContentView(moreView.root)
            bottomSheetDialog.setCancelable(true)
            bottomSheetDialog.show()
        }

        binding.btPlayPause.setOnClickListener {
            if (!viewModel.isRadio.value) {
                if (viewModel.playlistBrowse.value != null) {
                    sharedViewModel.simpleMediaServiceHandler?.setQueueData(
                        QueueData(
                            listTracks = (viewModel.playlistBrowse.value?.tracks ?: arrayListOf()) as ArrayList<Track>,
                            firstPlayedTrack =
                                viewModel.playlistBrowse.value
                                    ?.tracks
                                    ?.get(0),
                            playlistId =
                                viewModel.playlistBrowse.value
                                    ?.id
                                    ?.replaceFirst("VL", "") ?: "",
                            playlistName = "Playlist \"${viewModel.playlistBrowse.value?.title}\"",
                            playlistType = PlaylistType.PLAYLIST,
                            continuation = null,
                        ),
                    )
                    viewModel.playlistBrowse.value
                        ?.tracks
                        ?.get(0)
                        ?.let {
                            sharedViewModel.loadMediaItemFromTrack(
                                it,
                                type = Config.PLAYLIST_CLICK,
                                index = 0,
                            )
                        }
                } else if (viewModel.playlistEntity.value != null &&
                    viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED
                ) {
                    sharedViewModel.simpleMediaServiceHandler?.setQueueData(
                        QueueData(
                            listTracks = viewModel.listTrack.value.toArrayListTrack(),
                            firstPlayedTrack =
                                viewModel.listTrack.value
                                    .firstOrNull()
                                    ?.toTrack(),
                            playlistId =
                                viewModel.playlistEntity.value
                                    ?.id
                                    ?.replaceFirst("VL", "") ?: "",
                            playlistName = "Playlist \"${viewModel.playlistEntity.value?.title}\"",
                            playlistType = PlaylistType.PLAYLIST,
                            continuation = null,
                        ),
                    )
                    viewModel.listTrack.value.firstOrNull()?.let {
                        sharedViewModel.loadMediaItemFromTrack(
                            it.toTrack(),
                            type = Config.PLAYLIST_CLICK,
                            index = 0,
                        )
                    }
                } else {
                    Snackbar
                        .make(
                            requireView(),
                            getString(R.string.playlist_is_empty),
                            Snackbar.LENGTH_SHORT,
                        ).show()
                }
            } else {
                if (viewModel.playlistBrowse.value != null) {
                    sharedViewModel.simpleMediaServiceHandler?.setQueueData(
                        QueueData(
                            listTracks = (viewModel.playlistBrowse.value?.tracks ?: arrayListOf()) as ArrayList<Track>,
                            firstPlayedTrack =
                                viewModel.playlistBrowse.value
                                    ?.tracks
                                    ?.get(0),
                            playlistId =
                                viewModel.playlistBrowse.value
                                    ?.id
                                    ?.replaceFirst("VL", "") ?: "",
                            playlistName = "${viewModel.playlistBrowse.value?.title}",
                            playlistType = PlaylistType.RADIO,
                            continuation =
                                viewModel.radioContinuation.value?.let {
                                    if (it.first == viewModel.playlistBrowse.value?.id) it.second else null
                                },
                        ),
                    )
                    viewModel.playlistBrowse.value?.tracks?.firstOrNull()?.let {
                        sharedViewModel.loadMediaItemFromTrack(
                            it,
                            type = Config.PLAYLIST_CLICK,
                            index = 0,
                        )
                    }
                } else if (viewModel.playlistEntity.value != null &&
                    viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED
                ) {
                    sharedViewModel.simpleMediaServiceHandler?.setQueueData(
                        QueueData(
                            listTracks = viewModel.listTrack.value.toArrayListTrack(),
                            firstPlayedTrack =
                                viewModel.listTrack.value
                                    .firstOrNull()
                                    ?.toTrack(),
                            playlistId =
                                viewModel.playlistBrowse.value
                                    ?.id
                                    ?.replaceFirst("VL", ""),
                            playlistName = "${viewModel.playlistBrowse.value?.title}",
                            playlistType = PlaylistType.RADIO,
                            continuation =
                                viewModel.radioContinuation.value?.let {
                                    if (it.first == viewModel.playlistBrowse.value?.id) it.second else null
                                },
                        ),
                    )
                    viewModel.listTrack.value.firstOrNull()?.let {
                        sharedViewModel.loadMediaItemFromTrack(
                            it.toTrack(),
                            type = Config.PLAYLIST_CLICK,
                            index = 0,
                        )
                    }
                } else {
                    Snackbar
                        .make(
                            requireView(),
                            getString(R.string.playlist_is_empty),
                            Snackbar.LENGTH_SHORT,
                        ).show()
                }
            }
        }

        playlistItemAdapter.setOnClickListener(
            object : PlaylistItemAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    if (!viewModel.isRadio.value) {
                        if (viewModel.playlistBrowse.value != null) {
                            sharedViewModel.simpleMediaServiceHandler?.setQueueData(
                                QueueData(
                                    listTracks = (viewModel.playlistBrowse.value?.tracks ?: arrayListOf()) as ArrayList<Track>,
                                    firstPlayedTrack =
                                        viewModel.playlistBrowse.value
                                            ?.tracks
                                            ?.get(position),
                                    playlistId =
                                        viewModel.playlistBrowse.value
                                            ?.id
                                            ?.replaceFirst("VL", "") ?: "",
                                    playlistName = "Playlist \"${viewModel.playlistBrowse.value?.title}\"",
                                    playlistType = PlaylistType.PLAYLIST,
                                    continuation = null,
                                ),
                            )
                            viewModel.playlistBrowse.value?.tracks?.get(position)?.let {
                                Log.w(tag, "track: $it")
                                sharedViewModel.loadMediaItemFromTrack(
                                    it,
                                    type = Config.PLAYLIST_CLICK,
                                    index = position,
                                )
                            }
                        } else if (viewModel.playlistEntity.value != null &&
                            viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED
                        ) {
                            sharedViewModel.simpleMediaServiceHandler?.setQueueData(
                                QueueData(
                                    listTracks = viewModel.listTrack.value.toArrayListTrack(),
                                    firstPlayedTrack =
                                        viewModel.listTrack.value
                                            .getOrNull(position)
                                            ?.toTrack(),
                                    playlistId =
                                        viewModel.playlistEntity.value
                                            ?.id
                                            ?.replaceFirst("VL", "") ?: "",
                                    playlistName = "Playlist \"${viewModel.playlistEntity.value?.title}\"",
                                    playlistType = PlaylistType.PLAYLIST,
                                    continuation = null,
                                ),
                            )
                            viewModel.listTrack.value.getOrNull(position)?.let {
                                Log.w(tag, "track: $it")
                                sharedViewModel.loadMediaItemFromTrack(
                                    it.toTrack(),
                                    type = Config.PLAYLIST_CLICK,
                                    index = position,
                                )
                            }
                        } else {
                            Snackbar
                                .make(
                                    requireView(),
                                    getString(R.string.error),
                                    Snackbar.LENGTH_SHORT,
                                ).show()
                        }
                    } else {
                        if (viewModel.playlistBrowse.value != null) {
                            sharedViewModel.simpleMediaServiceHandler?.setQueueData(
                                QueueData(
                                    listTracks = (viewModel.playlistBrowse.value?.tracks ?: arrayListOf()) as ArrayList<Track>,
                                    firstPlayedTrack =
                                        viewModel.playlistBrowse.value
                                            ?.tracks
                                            ?.get(position),
                                    playlistId =
                                        viewModel.playlistBrowse.value
                                            ?.id
                                            ?.replaceFirst("VL", "") ?: "",
                                    playlistName = "${viewModel.playlistBrowse.value?.title}",
                                    playlistType = PlaylistType.RADIO,
                                    continuation =
                                        viewModel.radioContinuation.value?.let {
                                            if (it.first == viewModel.playlistBrowse.value?.id) it.second else null
                                        },
                                ),
                            )
                            viewModel.playlistBrowse.value?.tracks?.get(position)?.let {
                                Log.w(tag, "track: $it")
                                sharedViewModel.loadMediaItemFromTrack(
                                    it,
                                    type = Config.PLAYLIST_CLICK,
                                    index = position,
                                )
                            }
                        } else if (viewModel.playlistEntity.value != null &&
                            viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED
                        ) {
                            sharedViewModel.simpleMediaServiceHandler?.setQueueData(
                                QueueData(
                                    listTracks = viewModel.listTrack.value.toArrayListTrack(),
                                    firstPlayedTrack =
                                        viewModel.listTrack.value
                                            .getOrNull(position)
                                            ?.toTrack(),
                                    playlistId =
                                        viewModel.playlistBrowse.value
                                            ?.id
                                            ?.replaceFirst("VL", ""),
                                    playlistName = "${viewModel.playlistBrowse.value?.title}",
                                    playlistType = PlaylistType.RADIO,
                                    continuation =
                                        viewModel.radioContinuation.value?.let {
                                            if (it.first == viewModel.playlistBrowse.value?.id) it.second else null
                                        },
                                ),
                            )
                            viewModel.listTrack.value.getOrNull(position)?.let {
                                Log.w(tag, "track: $it")
                                sharedViewModel.loadMediaItemFromTrack(
                                    it.toTrack(),
                                    type = Config.PLAYLIST_CLICK,
                                    index = position,
                                )
                            }
                        } else {
                            Snackbar
                                .make(
                                    requireView(),
                                    getString(R.string.error),
                                    Snackbar.LENGTH_SHORT,
                                ).show()
                        }
                    }
                }
            },
        )
        playlistItemAdapter.setOnOptionClickListener(
            object :
                PlaylistItemAdapter.OnOptionClickListener {
                override fun onOptionClick(position: Int) {
                    val song =
                        viewModel.playlistBrowse.value!!.tracks[position]
                    viewModel.getSongEntity(song.toSongEntity())
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
                            }
                        }
                        btChangeLyricsProvider.visibility = View.GONE
                        tvSongTitle.text = song.title
                        tvSongTitle.isSelected = true
                        tvSongArtist.text = song.artists.toListName().connectArtists()
                        tvSongArtist.isSelected = true
                        if (song.album != null) {
                            setEnabledAll(btAlbum, true)
                            tvAlbum.text = song.album.name
                        } else {
                            tvAlbum.text = getString(R.string.no_album)
                            setEnabledAll(btAlbum, false)
                        }
                        btAlbum.setOnClickListener {
                            val albumId = song.album?.id
                            if (albumId != null) {
                                findNavController().navigateSafe(
                                    R.id.action_global_albumFragment,
                                    Bundle().apply {
                                        putString("browseId", albumId)
                                    },
                                )
                                dialog.dismiss()
                            } else {
                                Toast
                                    .makeText(
                                        requireContext(),
                                        getString(R.string.no_album),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                        btAddQueue.setOnClickListener {
                            sharedViewModel.addToQueue(song)
                        }
                        btPlayNext.setOnClickListener {
                            sharedViewModel.playNext(song)
                        }
                        ivThumbnail.load(song.thumbnails?.lastOrNull()?.url)
                        btRadio.setOnClickListener {
                            val args = Bundle()
                            args.putString("radioId", "RDAMVM${song.videoId}")
                            args.putString(
                                "videoId",
                                song.videoId,
                            )
                            dialog.dismiss()
                            findNavController().navigateSafe(
                                R.id.action_global_playlistFragment,
                                args,
                            )
                        }
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
                                artistAdapter.setOnClickListener(
                                    object :
                                        SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                        override fun onItemClick(position: Int) {
                                            val artist = song.artists[position]
                                            if (artist.id != null) {
                                                findNavController().navigateSafe(
                                                    R.id.action_global_artistFragment,
                                                    Bundle().apply {
                                                        putString("channelId", artist.id)
                                                    },
                                                )
                                                subDialog.dismiss()
                                                dialog.dismiss()
                                            }
                                        }
                                    },
                                )
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
                            addToAPlaylistAdapter.setVideoId(song.videoId)
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
                            addToAPlaylistAdapter.setOnItemClickListener(
                                object :
                                    AddToAPlaylistAdapter.OnItemClickListener {
                                    override fun onItemClick(position: Int) {
                                        val playlist = listLocalPlaylist[position]
                                        viewModel.updateInLibrary(song.videoId)
                                        val tempTrack = ArrayList<String>()
                                        if (playlist.tracks != null) {
                                            tempTrack.addAll(playlist.tracks)
                                        }
                                        if (!tempTrack.contains(
                                                song.videoId,
                                            ) &&
                                            playlist.syncedWithYouTubePlaylist == 1 &&
                                            playlist.youtubePlaylistId != null
                                        ) {
                                            viewModel.addToYouTubePlaylist(
                                                playlist.id,
                                                playlist.youtubePlaylistId,
                                                song.videoId,
                                            )
                                        }
                                        if (!tempTrack.contains(song.videoId)) {
                                            viewModel.insertPairSongLocalPlaylist(
                                                PairSongLocalPlaylist(
                                                    playlistId = playlist.id,
                                                    songId = song.videoId,
                                                    position = playlist.tracks?.size ?: 0,
                                                    inPlaylist = LocalDateTime.now(),
                                                ),
                                            )
                                            tempTrack.add(song.videoId)
                                        }

                                        viewModel.updateLocalPlaylistTracks(
                                            tempTrack.removeConflicts(),
                                            playlist.id,
                                        )
                                        addPlaylistDialog.dismiss()
                                        dialog.dismiss()
                                    }
                                },
                            )
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
            },
        )
        binding.topAppBarLayout.addOnOffsetChangedListener { it, verticalOffset ->
            if (abs(it.totalScrollRange) == abs(verticalOffset)) {
                binding.topAppBar.background = viewModel.gradientDrawable.value
                binding.collapsingToolbarLayout.isTitleEnabled = true
                if (viewModel.gradientDrawable.value != null) {
                    if (viewModel.gradientDrawable.value?.colors != null) {
                        setStatusBarsColor(
                            viewModel.gradientDrawable.value
                                ?.colors!!
                                .first(),
                            requireActivity(),
                        )
                    }
                }
            } else {
                binding.collapsingToolbarLayout.isTitleEnabled = false
                binding.topAppBar.background = null
                binding.topAppBarLayout.background = viewModel.gradientDrawable.value
                setStatusBarsColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark),
                    requireActivity(),
                )
            }
        }
        binding.btShuffle.setOnClickListener {
            if (viewModel.playlistBrowse.value != null) {
                val index =
                    Random.nextInt(
                        0,
                        viewModel.playlistBrowse.value!!
                            .tracks.size - 1,
                    )
                val shuffleList: ArrayList<Track> = arrayListOf()
                viewModel.playlistBrowse.value?.tracks?.let {
                    shuffleList.addAll(it)
                }
                shuffleList.shuffle()
                val afterShuffleIndex =
                    shuffleList.indexOf(
                        viewModel.playlistBrowse.value
                            ?.tracks
                            ?.get(index),
                    )
                sharedViewModel.simpleMediaServiceHandler?.setQueueData(
                    QueueData(
                        listTracks = shuffleList,
                        firstPlayedTrack =
                            viewModel.playlistBrowse.value
                                ?.tracks
                                ?.get(index),
                        playlistId =
                            viewModel.playlistBrowse.value
                                ?.id
                                ?.replaceFirst("VL", "") ?: "",
                        playlistName = "Playlist \"${viewModel.playlistBrowse.value?.title}\"",
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = null,
                    ),
                )
                viewModel.playlistBrowse.value?.tracks?.get(index)?.let {
                    sharedViewModel.loadMediaItemFromTrack(
                        it,
                        type = Config.PLAYLIST_CLICK,
                        index = afterShuffleIndex,
                    )
                }
            } else if (viewModel.playlistEntity.value != null && viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                val index =
                    Random.nextInt(
                        0,
                        viewModel.playlistEntity.value
                            ?.tracks
                            ?.size
                            ?.minus(1) ?: 0,
                    )
                val shuffleList: ArrayList<Track> = arrayListOf()
                viewModel.listTrack.value
                    .toArrayListTrack()
                    .let { it1 -> shuffleList.addAll(it1) }
                viewModel.listTrack.value
                    .getOrNull(index)
                    ?.let { shuffleList.remove(it.toTrack()) }
                shuffleList.shuffle()
                val afterShuffleIndex =
                    shuffleList.indexOf(
                        viewModel.listTrack.value
                            .getOrNull(index)
                            ?.toTrack(),
                    )
                sharedViewModel.simpleMediaServiceHandler?.setQueueData(
                    QueueData(
                        listTracks = shuffleList,
                        firstPlayedTrack =
                            viewModel.listTrack.value
                                .getOrNull(index)
                                ?.toTrack(),
                        playlistId =
                            viewModel.playlistEntity.value
                                ?.id
                                ?.replaceFirst("VL", "") ?: "",
                        playlistName = "Playlist \"${viewModel.playlistEntity.value?.title}\"",
                        playlistType = PlaylistType.PLAYLIST,
                        continuation = null,
                    ),
                )
                viewModel.listTrack.value
                    .getOrNull(index)
                    ?.let {
                        sharedViewModel.loadMediaItemFromTrack(
                            it.toTrack(),
                            type = Config.PLAYLIST_CLICK,
                            index = afterShuffleIndex,
                        )
                    }
            } else {
                Snackbar
                    .make(
                        requireView(),
                        getString(R.string.playlist_is_empty),
                        Snackbar.LENGTH_SHORT,
                    ).show()
            }
        }
        binding.btDownload.setOnClickListener {
            if (viewModel.playlistDownloadState.value == DownloadState.STATE_NOT_DOWNLOADED) {
//                if (!viewModel.prevPlaylistDownloading.value){
//                    viewModel.downloading()
                if (viewModel.playlistBrowse.value
                        ?.tracks
                        ?.size != viewModel.listTrack.value.size &&
                    viewModel.listTrack.value.isNotEmpty()
                ) {
                    for (i in viewModel.playlistBrowse.value?.tracks!!) {
                        viewModel.insertSong(i.toSongEntity())
                    }
                    runBlocking {
                        delay(1000)
                        viewModel.listJob.emit(arrayListOf())
                    }
                    viewModel.getListTrack(
                        viewModel.playlistBrowse.value
                            ?.tracks
                            ?.toListVideoId(),
                    )
                }
                viewModel.updatePlaylistDownloadState(
                    id!!,
                    DownloadState.STATE_PREPARING,
                )
//                }
//                else{
//                    Toast.makeText(requireContext(), getString(R.string.please_wait_before_playlist_downloaded), Toast.LENGTH_SHORT).show()
//                }
            } else if (viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                Toast
                    .makeText(requireContext(), getString(R.string.downloaded), Toast.LENGTH_SHORT)
                    .show()
            } else if (viewModel.playlistEntity.value?.downloadState == DownloadState.STATE_DOWNLOADING) {
                Toast
                    .makeText(
                        requireContext(),
                        getString(R.string.downloading),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
        collectUIState()
        collectPlaylist()
        collectListTrack()
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val job2 =
                    launch {
                        combine(
                            sharedViewModel.nowPlayingState.distinctUntilChangedBy {
                                it?.songEntity?.videoId
                            },
                            sharedViewModel.controllerState.map { it.isPlaying }.distinctUntilChanged(),
                        ) { nowPlaying, isPlaying ->
                            Pair(nowPlaying, isPlaying)
                        }.collect {
                            if (it.first != null && it.second) {
                                playlistItemAdapter.setNowPlaying(it.first?.songEntity?.videoId)
                            } else {
                                playlistItemAdapter.setNowPlaying(null)
                            }
                        }
                    }
                val job3 =
                    launch {
                        combine(viewModel.playlistDownloadState, viewModel.isRadio) { downloadState, isRadio ->
                            Pair(downloadState, isRadio)
                        }.collectLatest { pair ->
                            val playlistDownloadState = pair.first
                            val isRadio = pair.second
                            if (!isRadio) {
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
                                if (viewModel.isRadio.value) {
                                    binding.btDownload.visibility = View.GONE
                                    binding.animationDownloading.visibility = View.GONE
                                }
                            } else {
                                binding.btDownload.visibility = View.GONE
                                binding.animationDownloading.visibility = View.GONE
                            }
                        }
                    }
                job2.join()
                job3.join()
            }
        }
    }

//    private fun fetchDataFromViewModel() {
//        val response = viewModel.playlistBrowse.value
//        when (response) {
//            is Resource.Success -> {
//                response.data.let {
//                    with(binding) {
//                        if (it?.id?.startsWith("RDEM") == true || it?.id?.startsWith("RDAMVM") == true) {
//                            btDownload.visibility = View.GONE
//                        }
//                        collapsingToolbarLayout.title = it?.title
//                        tvTitle.text = it?.title
//                        tvTitle.isSelected = true
//                        tvPlaylistAuthor.text = it?.author?.name
//                        if (it?.year != "") {
//                            tvYearAndCategory.text =
//                                requireContext().getString(
//                                    R.string.year_and_category,
//                                    it?.year.toString(),
//                                    "Playlist",
//                                )
//                        } else {
//                            tvYearAndCategory.text = requireContext().getString(R.string.playlist)
//                        }
//                        tvTrackCountAndDuration.text =
//                            requireContext().getString(
//                                R.string.album_length,
//                                it?.trackCount.toString(),
//                                "",
//                            )
//                        if (it?.description != null && it.description != "") {
//                            tvDescription.originalText = it.description
//                        } else {
//                            tvDescription.originalText = getString(R.string.no_description)
//                        }
//                        loadImage(it?.thumbnails?.last()?.url)
//                        val list: ArrayList<Any> = arrayListOf()
//                        list.addAll(it?.tracks as ArrayList<Track>)
//                        playlistItemAdapter.updateList(list)
//                        if (viewModel.gradientDrawable.value == null) {
//                            viewModel.gradientDrawable.observe(viewLifecycleOwner) { gradient ->
//                                // fullRootLayout.background = gradient
// //                                    toolbarBackground = gradient?.colors?.get(0)
//                                if (gradient != null) {
//                                    val start =
//                                        topAppBarLayout.background ?: ColorDrawable(
//                                            Color.TRANSPARENT,
//                                        )
//                                    val transition =
//                                        TransitionDrawable(arrayOf(start, gradient))
//                                    topAppBarLayout.background = transition
//                                    transition.isCrossFadeEnabled = true
//                                    transition.startTransition(500)
//                                }
//                            }
//                        } else {
// //                                fullRootLayout.background = gradientDrawable
// //                                topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
//                            topAppBarLayout.background = gradientDrawable
//                        }
//                        binding.rootLayout.visibility = View.VISIBLE
//                        binding.loadingLayout.visibility = View.GONE
//                        if (viewModel.isRadio.value == false) {
//                            val playlistEntity = viewModel.playlistEntity.value
//                            if (playlistEntity != null) {
//                                viewModel.checkAllSongDownloaded(it.tracks as ArrayList<Track>)
//                                viewModel.playlistEntity.observe(viewLifecycleOwner) { playlistEntity2 ->
//                                    if (playlistEntity2 != null) {
//                                        when (playlistEntity2.downloadState) {
//                                            DownloadState.STATE_DOWNLOADED -> {
//                                                btDownload.visibility = View.VISIBLE
//                                                animationDownloading.visibility = View.GONE
//                                                btDownload.setImageResource(R.drawable.baseline_downloaded)
//                                            }
//
//                                            DownloadState.STATE_DOWNLOADING -> {
//                                                btDownload.visibility = View.GONE
//                                                animationDownloading.visibility = View.VISIBLE
//                                            }
//
//                                            DownloadState.STATE_NOT_DOWNLOADED -> {
//                                                btDownload.visibility = View.VISIBLE
//                                                animationDownloading.visibility = View.GONE
//                                                btDownload.setImageResource(R.drawable.download_button)
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        } else {
//                            btDownload.visibility = View.GONE
//                            cbLove.visibility = View.GONE
//                        }
//                    }
//                }
//            }
//
//            is Resource.Error -> {
//                Snackbar
//                    .make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG)
//                    .show()
//                findNavController().popBackStack()
//            }
//
//            else -> {}
//        }
//    }

    private fun collectUIState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val uiStateJob =
                    launch {
                        viewModel.uiState.collectLatest { state ->
                            when (state) {
                                is PlaylistUIState.Loading -> {
                                    binding.rootLayout.visibility = View.GONE
                                    binding.loadingLayout.visibility = View.VISIBLE
                                }

                                is PlaylistUIState.Error -> {
                                    Snackbar.make(binding.root, state.message ?: getString(R.string.error), Snackbar.LENGTH_LONG).show()
                                }

                                is PlaylistUIState.Success -> {
                                    binding.rootLayout.visibility = View.VISIBLE
                                    binding.loadingLayout.visibility = View.GONE
                                }
                            }
                        }
                    }
                val bgJob =
                    launch {
                        viewModel.gradientDrawable.collectLatest { gd ->
                            if (gd != null) {
                                with(binding) {
                                    val start =
                                        topAppBarLayout.background ?: ColorDrawable(
                                            Color.TRANSPARENT,
                                        )
                                    val transition =
                                        TransitionDrawable(arrayOf(start, gd))
                                    transition.setDither(true)
                                    topAppBarLayout.background = transition
                                    transition.isCrossFadeEnabled = true
                                    transition.startTransition(500)
                                }
                            }
                        }
                    }
                uiStateJob.join()
                bgJob.join()
            }
        }
    }

    @UnstableApi
    private fun collectListTrack() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val job1 =
                    launch {
                        combine(
                            viewModel.listTrack,
                            viewModel.playlistDownloadState,
                        ) { listTrack, downloadState ->
                            Pair(listTrack, downloadState)
                        }.collectLatest { pair ->
                            val listTrack = pair.first
                            val downloadState = pair.second
                            val tempList = arrayListOf<Any>()
                            for (i in listTrack) {
                                tempList.add(i)
                            }
                            listTrack.let {
                                viewModel.checkAllSongDownloaded(it.toArrayListTrack())
                            }
                            if (listTrack.isNotEmpty() && downloadState == DownloadState.STATE_PREPARING) {
                                val listJob: ArrayList<SongEntity> = arrayListOf()
                                for (song in listTrack) {
                                    if (song.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
                                        listJob.add(song)
                                    }
                                }
                                viewModel.listJob.value = listJob
                                listJob.forEach { job ->
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(job.videoId, job.videoId.toUri())
                                            .setData(job.title.toByteArray())
                                            .setCustomCacheKey(job.videoId)
                                            .build()
                                    viewModel.updateDownloadState(
                                        job.videoId,
                                        DownloadState.STATE_DOWNLOADING,
                                    )
                                    DownloadService.sendAddDownload(
                                        requireContext(),
                                        MusicDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                }
                                Log.d("PlaylistFragment", "ListJob: ${viewModel.listJob.value}")
                                viewModel.updatePlaylistDownloadState(
                                    viewModel.id.value!!,
                                    DownloadState.STATE_DOWNLOADING,
                                )
                            }
                        }
                    }
                val job2 =
                    launch {
                        combine(viewModel.downloadedList, viewModel.listTrack) { downloadedList, listTrack ->
                            Pair(downloadedList, listTrack)
                        }.collectLatest { pair ->
                            val list = pair.second
                            val downloadList = pair.first
                            val temp =
                                list.map { it.videoId }.toMutableSet().apply {
                                    removeAll(downloadList.toSet())
                                }
                            Log.w(tag, "DownloadList: $downloadList")
                            Log.w(tag, "Downloading and not download: $temp")
                            Log.w(tag, "DownloadList size: ${downloadList.size}")
                            Log.w(tag, "Downloading and not download size: ${temp.size}")
                            Log.w(tag, "List size: ${list.size}")
                            playlistItemAdapter.setDownloadedList(downloadList)
                            if (list.isNotEmpty()) {
                                if (downloadList.containsAll(
                                        list.map {
                                            it.videoId
                                        },
                                    ) &&
                                    downloadList.isNotEmpty()
                                ) {
                                    viewModel.updatePlaylistDownloadState(
                                        viewModel.id.value!!,
                                        DownloadState.STATE_DOWNLOADED,
                                    )
                                    Log.w(tag, "All downloaded")
                                } else if (viewModel.downloadUtils.downloadingVideoIds.value
                                        .containsAll(temp) &&
                                    temp.isNotEmpty()
                                ) {
                                    viewModel.updatePlaylistDownloadState(
                                        viewModel.id.value!!,
                                        DownloadState.STATE_DOWNLOADING,
                                    )
                                    Log.w(tag, "Downloading")
                                } else {
                                    viewModel.updatePlaylistDownloadState(
                                        viewModel.id.value!!,
                                        DownloadState.STATE_NOT_DOWNLOADED,
                                    )
                                    Log.w(tag, "Not downloaded")
                                }
                            } else {
                                viewModel.updatePlaylistDownloadState(
                                    viewModel.id.value!!,
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                )
                                Log.w(tag, "Not downloaded")
                            }
                        }
                    }
                job1.join()
                job2.join()
            }
        }
    }

    private fun collectPlaylist() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val job1 =
                    launch {
                        combine(viewModel.playlistBrowse, viewModel.playlistEntity) { playlistBrowse, playlistEntity ->
                            Pair(playlistBrowse, playlistEntity)
                        }.collectLatest { pair ->
                            val playlistBrowse = pair.first
                            val playlistEntity = pair.second
                            if (playlistBrowse != null && playlistEntity != null) {
                                viewModel.checkAllSongDownloaded(playlistBrowse.tracks as ArrayList<Track>)
                                with(binding) {
                                    if (playlistBrowse.id.startsWith("RDEM") || playlistBrowse.id.startsWith("RDAMVM")) {
                                        btDownload.visibility = View.GONE
                                        cbLove.visibility = View.GONE
                                    }
                                    collapsingToolbarLayout.title = playlistBrowse.title
                                    tvTitle.text = playlistBrowse.title
                                    tvTitle.isSelected = true
                                    tvPlaylistAuthor.text = playlistBrowse.author.name
                                    if (playlistBrowse.year != "") {
                                        tvYearAndCategory.text =
                                            requireContext().getString(
                                                R.string.year_and_category,
                                                playlistBrowse.year,
                                                "Playlist",
                                            )
                                    } else {
                                        tvYearAndCategory.text =
                                            requireContext().getString(R.string.playlist)
                                    }
                                    tvTrackCountAndDuration.text =
                                        requireContext().getString(
                                            R.string.album_length,
                                            playlistBrowse.trackCount.toString(),
                                            "",
                                        )
                                    if (playlistBrowse.description != null && playlistBrowse.description != "") {
                                        tvDescription.originalText = playlistBrowse.description
                                    } else {
                                        tvDescription.originalText = getString(R.string.no_description)
                                    }
                                    loadImage(playlistBrowse.thumbnails.lastOrNull()?.url)
                                    val list: ArrayList<Any> = arrayListOf()
                                    list.addAll(playlistBrowse.tracks)
                                    playlistItemAdapter.updateList(list)
                                }
                            } else if (playlistBrowse == null && playlistEntity != null) {
                                with(binding) {
                                    collapsingToolbarLayout.title = playlistEntity.title
                                    tvTitle.text = playlistEntity.title
                                    tvTitle.isSelected = true
                                    tvPlaylistAuthor.text = playlistEntity.author
                                    tvYearAndCategory.text =
                                        requireContext().getString(
                                            R.string.year_and_category,
                                            playlistEntity.year.toString(),
                                            "Playlist",
                                        )
                                    tvTrackCountAndDuration.text =
                                        requireContext().getString(
                                            R.string.album_length,
                                            playlistEntity.trackCount.toString(),
                                            "",
                                        )
                                    if (playlistEntity.description != "") {
                                        tvDescription.originalText = playlistEntity.description
                                    } else {
                                        tvDescription.originalText = getString(R.string.no_description)
                                    }
                                    loadImage(playlistEntity.thumbnails)
                                }
                            }
                        }
                    }
                val job2 =
                    launch {
                        combine(viewModel.playlistBrowse, viewModel.listTrack) { playlistBrowse, listTrack ->
                            Pair(playlistBrowse, listTrack)
                        }.collectLatest { pair ->
                            val playlistBrowse = pair.first
                            val listTrack = pair.second
                            if (playlistBrowse == null && listTrack.isNotEmpty()) {
                                val tempList = arrayListOf<Any>()
                                for (i in listTrack) {
                                    tempList.add(i)
                                }
                                listTrack.let {
                                    viewModel.checkAllSongDownloaded(it.toArrayListTrack())
                                }
                                playlistItemAdapter.updateList(tempList)
                            }
                        }
                    }
                job1.join()
                job2.join()
            }
        }
    }

    private fun fetchRDATRadio(radioId: String) {
        viewModel.clearPlaylistBrowse()
        viewModel.clearPlaylistEntity()
        viewModel.updateId(radioId)
        viewModel.getRadio(radioId)
    }

    private fun fetchDataWithRadio(
        radioId: String,
        videoId: String? = null,
        channelId: String? = null,
    ) {
        viewModel.clearPlaylistBrowse()
        viewModel.clearPlaylistEntity()
        viewModel.updateId(radioId)
        viewModel.getRadio(radioId, videoId, channelId)
//        viewModel.playlistBrowse.observe(viewLifecycleOwner) { response ->
//            when (response) {
//                is Resource.Success -> {
//                    response.data.let {
//                        with(binding) {
//                            if (it != null) {
//                                viewModel.insertRadioPlaylist(it.toPlaylistEntity())
//                            }
//                            collapsingToolbarLayout.title = it?.title
//                            tvTitle.text = it?.title
//                            tvTitle.isSelected = true
//                            tvPlaylistAuthor.text = it?.author?.name
//                            if (it?.year != "") {
//                                tvYearAndCategory.text =
//                                    requireContext().getString(
//                                        R.string.year_and_category,
//                                        it?.year.toString(),
//                                        "Playlist",
//                                    )
//                            } else {
//                                tvYearAndCategory.text =
//                                    requireContext().getString(R.string.playlist)
//                            }
//                            tvTrackCountAndDuration.text =
//                                requireContext().getString(
//                                    R.string.album_length,
//                                    it?.trackCount.toString(),
//                                    "",
//                                )
//                            if (it?.description != null && it.description != "") {
//                                tvDescription.originalText = it.description
//                            } else {
//                                tvDescription.originalText = getString(R.string.no_description)
//                            }
//                            loadImage(it?.thumbnails?.last()?.url)
//                            val list: ArrayList<Any> = arrayListOf()
//                            list.addAll(it?.tracks as ArrayList<Track>)
//                            playlistItemAdapter.updateList(list)
//                            if (viewModel.gradientDrawable.value == null) {
//                                viewModel.gradientDrawable.observe(viewLifecycleOwner) { gradient ->
// //                                    fullRootLayout.background = gradient
// //                                    toolbarBackground = gradient?.colors?.get(0)
//                                    if (gradient != null) {
//                                        val start =
//                                            topAppBarLayout.background ?: ColorDrawable(
//                                                Color.TRANSPARENT,
//                                            )
//                                        val transition =
//                                            TransitionDrawable(arrayOf(start, gradient))
//                                        transition.setDither(true)
//                                        topAppBarLayout.background = transition
//                                        transition.isCrossFadeEnabled = true
//                                        transition.startTransition(500)
//                                    }
//                                }
//                            } else {
// //                                fullRootLayout.background = gradientDrawable
// //                                topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
//                                topAppBarLayout.background =
//                                    gradientDrawable?.apply {
//                                        setDither(true)
//                                    }
//                            }
//                            binding.rootLayout.visibility = View.VISIBLE
//                            binding.loadingLayout.visibility = View.GONE
//                            btDownload.visibility = View.GONE
//                            cbLove.visibility = View.GONE
//                        }
//                    }
//                }
//
//                is Resource.Error -> {
//                    Snackbar
//                        .make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG)
//                        .show()
//                    findNavController().popBackStack()
//                }
//
//                else -> {}
//            }
//        }
    }

    private fun fetchData(
        id: String,
        downloaded: Int = 0,
    ) {
        viewModel.updateId(id)
        if (downloaded == 0) {
            viewModel.clearPlaylistBrowse()
            viewModel.clearPlaylistEntity()
            viewModel.browsePlaylist(id)
//            viewModel.playlistBrowse.observe(viewLifecycleOwner) { response ->
//                when (response) {
//                    is Resource.Success -> {
//                        response.data.let {
//                            with(binding) {
//                                if (it != null) {
//                                    viewModel.insertPlaylist(it.toPlaylistEntity())
//                                }
//                                collapsingToolbarLayout.title = it?.title
//                                tvTitle.text = it?.title
//                                tvTitle.isSelected = true
//                                tvPlaylistAuthor.text = it?.author?.name
//                                if (it?.year != "") {
//                                    tvYearAndCategory.text =
//                                        requireContext().getString(
//                                            R.string.year_and_category,
//                                            it?.year.toString(),
//                                            "Playlist",
//                                        )
//                                } else {
//                                    tvYearAndCategory.text =
//                                        requireContext().getString(R.string.playlist)
//                                }
//                                tvTrackCountAndDuration.text =
//                                    requireContext().getString(
//                                        R.string.album_length,
//                                        it?.trackCount.toString(),
//                                        "",
//                                    )
//                                if (it?.description != null && it.description != "") {
//                                    tvDescription.originalText = it.description
//                                } else {
//                                    tvDescription.originalText = getString(R.string.no_description)
//                                }
//                                loadImage(it?.thumbnails?.last()?.url)
//                                val list: ArrayList<Any> = arrayListOf()
//                                list.addAll(it?.tracks as ArrayList<Track>)
//                                playlistItemAdapter.updateList(list)
//                                if (viewModel.gradientDrawable.value == null) {
//                                    viewModel.gradientDrawable.observe(viewLifecycleOwner) { gradient ->
// //                                            fullRootLayout.background = gradient
// //                                            toolbarBackground = gradient?.colors?.get(0)
// //                                            topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
//                                        if (gradient != null) {
//                                            val start =
//                                                topAppBarLayout.background ?: ColorDrawable(
//                                                    Color.TRANSPARENT,
//                                                )
//                                            val transition =
//                                                TransitionDrawable(arrayOf(start, gradient))
//                                            transition.setDither(true)
//                                            topAppBarLayout.background = transition
//                                            transition.isCrossFadeEnabled = true
//                                            transition.startTransition(500)
//                                        }
//                                    }
//                                } else {
// //                                    fullRootLayout.background = gradientDrawable
// //                                    topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
//                                    topAppBarLayout.background =
//                                        gradientDrawable?.apply {
//                                            setDither(true)
//                                        }
//                                }
//                                binding.rootLayout.visibility = View.VISIBLE
//                                binding.loadingLayout.visibility = View.GONE
//                                viewModel.playlistEntity.observe(viewLifecycleOwner) { playlistEntity2 ->
//                                    if (playlistEntity2 != null) {
//                                        when (playlistEntity2.downloadState) {
//                                            DownloadState.STATE_DOWNLOADED -> {
//                                                btDownload.visibility = View.VISIBLE
//                                                animationDownloading.visibility = View.GONE
//                                                btDownload.setImageResource(R.drawable.baseline_downloaded)
//                                            }
//
//                                            DownloadState.STATE_DOWNLOADING -> {
//                                                btDownload.visibility = View.GONE
//                                                animationDownloading.visibility = View.VISIBLE
//                                            }
//
//                                            DownloadState.STATE_NOT_DOWNLOADED -> {
//                                                btDownload.visibility = View.VISIBLE
//                                                animationDownloading.visibility = View.GONE
//                                                btDownload.setImageResource(R.drawable.download_button)
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                    is Resource.Error -> {
//                        Snackbar
//                            .make(
//                                binding.root,
//                                response.message.toString(),
//                                Snackbar.LENGTH_LONG,
//                            ).show()
//                        findNavController().popBackStack()
//                    }
//
//                    else -> {}
//                }
//            }
        } else if (downloaded == 1) {
            viewModel.clearPlaylistBrowse()
            viewModel.clearPlaylistEntity()
            viewModel.getPlaylist(id, null, null)
//            with(binding) {
//                viewModel.playlistEntity.observe(viewLifecycleOwner) { playlistEntity ->
//                    if (playlistEntity != null) {
//                        when (playlistEntity.downloadState) {
//                            DownloadState.STATE_DOWNLOADED -> {
//                                btDownload.visibility = View.VISIBLE
//                                animationDownloading.visibility = View.GONE
//                                btDownload.setImageResource(R.drawable.baseline_downloaded)
//                            }
//
//                            DownloadState.STATE_DOWNLOADING -> {
//                                btDownload.visibility = View.GONE
//                                animationDownloading.visibility = View.VISIBLE
//                            }
//
//                            DownloadState.STATE_NOT_DOWNLOADED -> {
//                                btDownload.visibility = View.VISIBLE
//                                animationDownloading.visibility = View.GONE
//                                btDownload.setImageResource(R.drawable.download_button)
//                            }
//                        }
//                        collapsingToolbarLayout.title = playlistEntity.title
//                        tvTitle.text = playlistEntity.title
//                        tvTitle.isSelected = true
//                        tvPlaylistAuthor.text = playlistEntity.author
//                        tvYearAndCategory.text =
//                            requireContext().getString(
//                                R.string.year_and_category,
//                                playlistEntity.year.toString(),
//                                "Playlist",
//                            )
//                        tvTrackCountAndDuration.text =
//                            requireContext().getString(
//                                R.string.album_length,
//                                playlistEntity.trackCount.toString(),
//                                "",
//                            )
//                        if (playlistEntity.description != "") {
//                            tvDescription.originalText = playlistEntity.description
//                        } else {
//                            tvDescription.originalText = getString(R.string.no_description)
//                        }
//                        loadImage(playlistEntity.thumbnails)
//                        if (viewModel.gradientDrawable.value == null) {
//                            viewModel.gradientDrawable.observe(viewLifecycleOwner) { gradient ->
// //                                fullRootLayout.background = gradient
// //                                toolbarBackground = gradient?.colors?.get(0)
// //                                topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
//                                if (gradient != null) {
//                                    val start =
//                                        topAppBarLayout.background
//                                            ?: ColorDrawable(Color.TRANSPARENT)
//                                    val transition = TransitionDrawable(arrayOf(start, gradient))
//                                    transition.setDither(true)
//                                    topAppBarLayout.background = transition
//                                    transition.isCrossFadeEnabled = true
//                                    transition.startTransition(500)
//                                }
//                            }
//                        } else {
// //                            fullRootLayout.background = gradientDrawable
// //                            topAppBarLayout.background = ColorDrawable(toolbarBackground!!)
//                            topAppBarLayout.background =
//                                gradientDrawable?.apply {
//                                    setDither(true)
//                                }
//                        }
//                        viewModel.getListTrack(playlistEntity.tracks)
//                        viewModel.listTrack.observe(viewLifecycleOwner) { listTrack ->
//                            val tempList = arrayListOf<Any>()
//                            for (i in listTrack) {
//                                tempList.add(i)
//                            }
//                            playlistItemAdapter.updateList(tempList)
//                        }
//                    }
//                }
//            }
        }
    }

    private fun loadImage(url: String?) {
        if (url != null) {
            binding.ivPlaylistArt.visibility = View.VISIBLE
            binding.ivPlaylistArt.background = ColorDrawable(Color.WHITE)
            binding.ivPlaylistArt.load(url) {
                allowHardware(false)
                placeholder(R.drawable.holder)
                diskCachePolicy(CachePolicy.ENABLED)
                crossfade(true)
                crossfade(300)
                listener(
                    onError = { _, er ->
                        Log.w(tag, "Load Image Error ${er.throwable.message}")
                    },
                    onSuccess = { _, result ->
                        binding.ivPlaylistArt.setImageDrawable(result.drawable)
                        val p = Palette.from(result.drawable.toBitmap()).generate()
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
                        val endColor =
                            resources.getColor(R.color.md_theme_dark_background, null)
                        val gd =
                            GradientDrawable(
                                GradientDrawable.Orientation.TOP_BOTTOM,
                                intArrayOf(startColor, endColor),
                            )
                        gd.cornerRadius = 0f
                        gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                        gd.gradientRadius = 0.5f
                        viewModel.setGradientDrawable(gd)
                    },
                )
            }
        }
    }
}