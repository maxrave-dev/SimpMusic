package com.maxrave.simpmusic.ui.fragment

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.customview.widget.ViewDragHelper
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.artist.SeeArtistOfNowPlayingAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetNowPlayingBinding
import com.maxrave.simpmusic.databinding.BottomSheetSeeArtistOfNowPlayingBinding
import com.maxrave.simpmusic.databinding.FragmentNowPlayingBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.isMyServiceRunning
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.extension.toListTrack
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.service.RepeatState
import com.maxrave.simpmusic.service.test.source.FetchQueue
import com.maxrave.simpmusic.service.test.source.MusicSource
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import javax.inject.Inject


@UnstableApi
@AndroidEntryPoint
class NowPlayingFragment: Fragment() {

    @Inject
    lateinit var musicSource: MusicSource


    private val viewModel by activityViewModels<SharedViewModel>()
    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    private var metadataCurSong: MetadataSong? = null

    private var videoId: String? = null
    private var from: String? = null
    private var type: String? = null
    private var index: Int? = null

    private var gradientDrawable: GradientDrawable? = null
    private var lyricsBackground: Int? = null

    private lateinit var songChangeListener: OnNowPlayingSongChangeListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNowPlayingSongChangeListener){
            songChangeListener = context
        }
        else{
            throw RuntimeException("$context must implement OnNowPlayingSongChangeListener")
        }
    }

    override fun onResume() {
        Log.d("NowPlayingFragment", "onResume")
        updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        binding.topAppBar.applyInsetter {
            type(statusBars = true) {
                margin()
            }
        }
        binding.root.applyInsetter {
            type(navigationBars = true) {
                margin()
            }
        }
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val card = activity.findViewById<MaterialCardView>(R.id.card)
        bottom.visibility = View.GONE
        card.visibility = View.GONE
        binding.lyricsFullLayout.visibility = View.GONE
        binding.buffered.max = 100
        Log.d("check Video ID in ViewModel", viewModel.videoId.value.toString())

        type = arguments?.getString("type")
        videoId = arguments?.getString("videoId")
        from = arguments?.getString("from")
        index = arguments?.getInt("index")

        Log.d("check Video ID in Fragment", videoId.toString())
        when(type){
            Config.SONG_CLICK -> {
                if (viewModel.videoId.value == videoId)
                {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                }
                else
                {
                    Log.i("Now Playing Fragment", "Bên trên")
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        musicSource.reset()
                        if (requireContext().isMyServiceRunning(FetchQueue:: class.java)) {
                            requireActivity().stopService(Intent(requireContext(), FetchQueue::class.java))
                        }
                        viewModel.loadMediaItemFromTrack(it)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        viewModel.resetLyrics()
                        if (it.artists.isNullOrEmpty()) {
                            viewModel.getLyrics(it.title)
                        }
                        else {
                            viewModel.getLyrics(it.title + " " + it.artists.first().name)
                        }
                        updateUIfromQueueNowPlaying()
                        lifecycleScope.launch {
                            viewModel.firstTrackAdded.collect { added ->
                                if (added) {
                                    viewModel.changeFirstTrackAddedToFalse()
                                    getRelated(it.videoId)
                                }
                            }
                        }
                    }
                }
            }
            Config.VIDEO_CLICK -> {
                if (viewModel.videoId.value == videoId)
                {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                }
                else
                {
//                if (!viewModel.songTransitions.value){
                    Log.i("Now Playing Fragment", "Bên trên")
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        musicSource.reset()
                        if (requireContext().isMyServiceRunning(FetchQueue:: class.java)) {
                            requireActivity().stopService(Intent(requireContext(), FetchQueue::class.java))
                        }
                        viewModel.loadMediaItemFromTrack(it)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        viewModel.resetLyrics()
                        viewModel.getLyrics(it.title + " " + it.artists?.first()?.name)
                        updateUIfromQueueNowPlaying()
                        lifecycleScope.launch {
                            viewModel.firstTrackAdded.collect { added ->
                                if (added) {
                                    viewModel.changeFirstTrackAddedToFalse()
                                    getVideosRelated(it.videoId)
                                }
                            }
                        }
                    }
                    //}
//                viewModel.loadMediaItems(videoId!!)
                }
            }
            Config.ALBUM_CLICK -> {
                if (viewModel.videoId.value == videoId)
                {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                }
                else
                {
//                if (!viewModel.songTransitions.value){
                    Log.i("Now Playing Fragment", "Bên trên")
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        musicSource.reset()
                        if (requireContext().isMyServiceRunning(FetchQueue:: class.java)) {
                            requireActivity().stopService(Intent(requireContext(), FetchQueue::class.java))
                        }
                        viewModel.loadMediaItemFromTrack(it)
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        viewModel.resetLyrics()
                        viewModel.getLyrics(it.title + " " + it.artists?.first()?.name)
                        updateUIfromQueueNowPlaying()
                        Log.d("check index", index.toString())
                        lifecycleScope.launch {
                            viewModel.firstTrackAdded.collect { added ->
                                if (added) {
                                    viewModel.changeFirstTrackAddedToFalse()
                                    if (index == null){
                                        fetchSourceFromQueue()
                                    }
                                    else{
                                        fetchSourceFromQueue(index!!)
                                    }
                                }
                            }
                        }
                    }
                    //}
//                viewModel.loadMediaItems(videoId!!)
                }
            }
            Config.PLAYLIST_CLICK -> {
                if (viewModel.videoId.value == videoId)
                {
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    metadataCurSong = viewModel.metadata.value?.data
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                }
                else
                {
//                if (!viewModel.songTransitions.value){
                    Log.i("Now Playing Fragment", "Bên trên")
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    viewModel.gradientDrawable.postValue(null)
                    viewModel.lyricsBackground.postValue(null)
                    binding.tvSongTitle.visibility = View.GONE
                    binding.tvSongArtist.visibility = View.GONE
                    Queue.getNowPlaying()?.let {
                        musicSource.reset()
                        if (requireContext().isMyServiceRunning(FetchQueue:: class.java)) {
                            requireActivity().stopService(Intent(requireContext(), FetchQueue::class.java))
                        }
                        viewModel.loadMediaItemFromTrack(it)

//                        viewModel.getMetadata(it.videoId)
//                        observerMetadata()
                        viewModel.videoId.postValue(it.videoId)
                        viewModel.from.postValue(from)
                        viewModel.resetLyrics()
                        viewModel.getLyrics(it.title + " " + it.artists?.first()?.name)
                        updateUIfromQueueNowPlaying()
                        Log.d("check index", index.toString())
                        lifecycleScope.launch {
                            viewModel.firstTrackAdded.collect { added ->
                                if (added) {
                                    viewModel.changeFirstTrackAddedToFalse()
                                    if (index == null){
                                        fetchSourceFromQueue()
                                    }
                                    else{
                                        fetchSourceFromQueue(index!!)
                                    }
                                }
                            }
                        }
                    }
                    //}
//                viewModel.loadMediaItems(videoId!!)
                }
            }
            Config.MINIPLAYER_CLICK -> {
                    videoId = viewModel.videoId.value
                    from = viewModel.from.value
                    metadataCurSong = viewModel.metadata.value?.data
                    gradientDrawable = viewModel.gradientDrawable.value
                    lyricsBackground = viewModel.lyricsBackground.value
                    updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
            }
        }

        lifecycleScope.launch {
            val job7 = launch {
                viewModel.songTransitions.collect{isChanged ->
                    if (isChanged){
                        if (viewModel.getCurrentMediaItem() != null){
                            Log.i("Now Playing Fragment", "Bên dưới")
                            Log.d("Song Transition", "Song Transition")
                            videoId = viewModel.videoId.value
                            binding.ivArt.visibility = View.GONE
                            binding.loadingArt.visibility = View.VISIBLE
                            val track = viewModel.nowPlayingMediaItem.value
                            if (track != null) {
                                viewModel.resetLyrics()
                                viewModel.getLyrics(track.mediaMetadata.title.toString() + " " + track.mediaMetadata.artist)
                            }
                            Log.d("Check Lyrics", viewModel._lyrics.value?.data.toString())
                            updateUIfromCurrentMediaItem(viewModel.nowPlayingMediaItem.value)
                            musicSource.setCurrentSongIndex(viewModel.getCurrentMediaItemIndex())
                            viewModel.changeSongTransitionToFalse()
                        }
                    }
                }
            }
            val job1 = launch {
                viewModel.progressString.observe(viewLifecycleOwner){
                    binding.tvCurrentTime.text = it
                    if (viewModel.progress.value * 100 in 0f..100f){
                        binding.progressSong.value = viewModel.progress.value * 100
                        songChangeListener.onUpdateProgressBar(viewModel.progress.value * 100)
                    }
                }
                viewModel.duration.collect {
                    if (viewModel.formatDuration(it).contains('-')){
                        binding.tvFullTime.text = "NA:NA"
                    }
                    else {
                        binding.tvFullTime.text = viewModel.formatDuration(it)
                    }
                }
            }
            val job2 = launch {
                viewModel.isPlaying.observe(viewLifecycleOwner){
                    Log.d("Check Song Transistion", "${viewModel.songTransitions.value}")
                    if (it){
                        //updateUIfromCurrentMediaItem(viewModel.getCurrentMediaItem())
                        binding.btPlayPause.setImageResource(R.drawable.baseline_pause_circle_24)
                        songChangeListener.onIsPlayingChange()
                    }
                    else{
                        binding.btPlayPause.setImageResource(R.drawable.baseline_play_circle_24)
                        songChangeListener.onIsPlayingChange()
                    }
                }
            }
            //Update progress bar from buffered percentage
            val job3 = launch {
                viewModel.bufferedPercentage.collect{
                    binding.buffered.progress = it
//                    Log.d("buffered", it.toString())
                }
            }
            //Check if song is ready to play. And make progress bar indeterminate
            val job4 = launch {
                viewModel.notReady.observe(viewLifecycleOwner){
                    binding.buffered.isIndeterminate = it
                }
            }
            val job5 = launch {
                    viewModel.progressMillis.collect{
                        if (viewModel._lyrics.value?.data != null){
                            val temp = viewModel.getLyricsString(it)
                            binding.tvSyncState.text = when (viewModel.getLyricsSyncState()){
                                Config.SyncState.NOT_FOUND -> null
                                Config.SyncState.LINE_SYNCED -> "Line Synced"
                                Config.SyncState.UNSYNCED -> "Unsynced"
                            }
                            if (temp != null){
                                if (temp.nowLyric == "Lyrics not found"){
                                    binding.lyricsLayout.visibility = View.GONE
                                    binding.lyricsTextLayout.visibility = View.GONE
                                }
                                else
                                {
                                    if (binding.btFull.text == "Show"){
                                        binding.lyricsTextLayout.visibility = View.VISIBLE
                                    }
                                    binding.lyricsLayout.visibility = View.VISIBLE
                                    if (temp.nowLyric != null){
                                        binding.tvNowLyrics.visibility = View.VISIBLE
                                        binding.tvNowLyrics.text = temp.nowLyric
                                    }
                                    else{
                                        binding.tvNowLyrics.visibility = View.GONE
                                    }
                                    if (temp.prevLyrics != null){
                                        binding.tvPrevLyrics.visibility = View.VISIBLE
                                        if (temp.prevLyrics.size > 1){
                                            val txt = temp.prevLyrics[0] + "\n" + temp.prevLyrics[1]
                                            binding.tvPrevLyrics.text = txt
                                        }
                                        else {
                                            binding.tvPrevLyrics.text = temp.prevLyrics[0]
                                        }
                                    }
                                    else{
                                        binding.tvPrevLyrics.visibility = View.GONE
                                    }
                                    if (temp.nextLyric != null){
                                        binding.tvNextLyrics.visibility = View.VISIBLE
                                        if (temp.nextLyric.size > 1){
                                            val txt = temp.nextLyric[0] + "\n" + temp.nextLyric[1]
                                            binding.tvNextLyrics.text = txt
                                        }
                                        else {
                                            binding.tvNextLyrics.text = temp.nextLyric[0]
                                        }
                                    }
                                    else{
                                        binding.tvNextLyrics.visibility = View.GONE
                                    }
                                }
                            }
                        }
                        else {
                            binding.lyricsLayout.visibility = View.GONE
                            binding.lyricsTextLayout.visibility = View.GONE
                        }
                }
            }
            val job6 = launch {
                viewModel.lyricsFull.observe(viewLifecycleOwner){
                    if (it != null){
                        binding.tvFullLyrics.text = it
                    }
                }
            }
            val job8 = launch {
                viewModel.shuffleModeEnabled.collect{ shuffle ->
                    when (shuffle){
                        true -> {
                            binding.btShuffle.setImageResource(R.drawable.baseline_shuffle_24_enable)
                        }
                        false -> {
                            binding.btShuffle.setImageResource(R.drawable.baseline_shuffle_24)
                        }
                    }
                }
            }
            val job9 = launch {
                viewModel.repeatMode.collect{ repeatMode ->
                    when (repeatMode) {
                        RepeatState.None -> {
                            binding.btRepeat.setImageResource(R.drawable.baseline_repeat_24)
                        }
                        RepeatState.One -> {
                            binding.btRepeat.setImageResource(R.drawable.baseline_repeat_one_24)
                        }
                        RepeatState.All -> {
                            binding.btRepeat.setImageResource(R.drawable.baseline_repeat_24_enable)
                        }
                    }
                }
            }
            val job10 = launch {
                viewModel.liked.collect { liked ->
                    binding.cbFavorite.isChecked = liked
                }
            }

            job1.join()
            job2.join()
            job3.join()
            job4.join()
            job5.join()
            job6.join()
            job7.join()
            job8.join()
            job9.join()
            job10.join()
        }
        binding.btFull.setOnClickListener {
            if (binding.btFull.text == "Show"){
                binding.btFull.text = "Hide"
                binding.lyricsTextLayout.visibility = View.GONE
                binding.lyricsFullLayout.visibility = View.VISIBLE
            }
            else{
                binding.btFull.text = "Show"
                binding.lyricsTextLayout.visibility = View.VISIBLE
                binding.lyricsFullLayout.visibility = View.GONE
            }
        }

        binding.progressSong.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {

            }

            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.onUIEvent(UIEvent.UpdateProgress(slider.value))
            }
        })

        binding.btPlayPause.setOnClickListener {
            viewModel.onUIEvent(UIEvent.PlayPause)
        }
        binding.btNext.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Next)
        }
        binding.btPrevious.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Previous)
        }
        binding.btShuffle.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Shuffle)
        }
        binding.btRepeat.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Repeat)
        }

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.btQueue.setOnClickListener {
            findNavController().navigate(R.id.action_nowPlayingFragment_to_queueFragment)
        }
        binding.btSongInfo.setOnClickListener {
            findNavController().navigate(R.id.action_global_infoFragment)
        }
        binding.cbFavorite.setOnCheckedChangeListener { cb, isChecked ->
            if (!isChecked){
                Log.d("cbFavorite", "onCheckedChanged: $isChecked")
                viewModel.getCurrentMediaItem()?.let { nowPlayingSong -> viewModel.updateLikeStatus(nowPlayingSong.mediaId, false) }
            }
            else {
                Log.d("cbFavorite", "onCheckedChanged: $isChecked")
                viewModel.getCurrentMediaItem()?.let { nowPlayingSong -> viewModel.updateLikeStatus(nowPlayingSong.mediaId, true) }
            }
        }
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.now_playing_dialog_menu_item_more -> {
                    if (musicSource.catalogMetadata.isNotEmpty()){
                        val dialog = BottomSheetDialog(requireContext())
                        val bottomSheetView = BottomSheetNowPlayingBinding.inflate(layoutInflater)
                        with(bottomSheetView) {
                            if (viewModel.liked.value){
                                tvFavorite.text = "Liked"
                                cbFavorite.isChecked = true
                            }
                            else {
                                tvFavorite.text = "Like"
                                cbFavorite.isChecked = false
                            }
                            val song = musicSource.catalogMetadata[viewModel.getCurrentMediaItemIndex()]
                            tvSongTitle.text = song.title
                            tvSongTitle.isSelected = true
                            tvSongArtist.text = song.artists.toListName().connectArtists()
                            tvSongArtist.isSelected = true
                            ivThumbnail.load(song.thumbnails?.last()?.url)

                            btLike.setOnClickListener {
                                if (cbFavorite.isChecked){
                                    cbFavorite.isChecked = false
                                    tvFavorite.text = "Like"
                                    viewModel.updateLikeStatus(song.videoId, false)
                                }
                                else {
                                    cbFavorite.isChecked = true
                                    tvFavorite.text = "Liked"
                                    viewModel.updateLikeStatus(song.videoId, true)
                                }
                            }

                            btSeeArtists.setOnClickListener {
                                val subDialog = BottomSheetDialog(requireContext())
                                val subBottomSheetView = BottomSheetSeeArtistOfNowPlayingBinding.inflate(layoutInflater)
                                if (song.artists != null) {
                                    val artistAdapter = SeeArtistOfNowPlayingAdapter(song.artists)
                                    subBottomSheetView.rvArtists.apply {
                                        adapter = artistAdapter
                                        layoutManager = LinearLayoutManager(requireContext())
                                    }
                                    artistAdapter.setOnClickListener(object : SeeArtistOfNowPlayingAdapter.OnItemClickListener {
                                        override fun onItemClick(position: Int) {
                                            val artist = song.artists[position]
                                            if (artist.id != null) {
                                                findNavController().navigate(R.id.action_global_artistFragment, Bundle().apply {
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
                                val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ URL")
                                startActivity(chooserIntent)
                            }
                        }
                        dialog.setCancelable(true)
                        dialog.setContentView(bottomSheetView.root)
                        dialog.show()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchSourceFromQueue(index: Int? = null) {
        if (index == null){
            if (!requireContext().isMyServiceRunning(FetchQueue:: class.java)){
                requireActivity().startService(Intent(requireContext(), FetchQueue::class.java))
            }
            else {
                requireActivity().stopService(Intent(requireContext(), FetchQueue::class.java))
                requireActivity().startService(Intent(requireContext(), FetchQueue::class.java))
            }
        }
        else {
            Log.d("fetchSourceFromQueue", "fetchSourceFromQueue: $index")
            val mIntent = Intent(requireContext(), FetchQueue::class.java)
            mIntent.putExtra("index", index)
            if (!requireContext().isMyServiceRunning(FetchQueue:: class.java)){
                requireActivity().startService(mIntent)
            }
            else {
                requireActivity().stopService(Intent(requireContext(), FetchQueue::class.java))
                requireActivity().startService(mIntent)
            }
        }
    }

//    private fun observerLyrics() {
//        viewModel.lyrics.observe(viewLifecycleOwner){ lyrics ->
//            if (lyrics != null){
//                when (lyrics) {
//                    is Resource.Success -> {
//                        viewModel.parseLyrics(lyrics.data)
//                        binding.lyricsFullLayout.visibility = View.VISIBLE
//                    }
//                    is Resource.Error -> {
//                        binding.lyricsFullLayout.visibility = View.GONE
//                    }
//                }
//            }
//        }
//    }

    //
//        val player = ExoPlayer.Builder(requireContext()).build()
//        if (viewModel.currentSong.value == null){
//            viewModel.init()
//        }
//        var mediaItem = MediaItem.fromUri(viewModel.getStreamUrl())
//        player.addMediaItem(mediaItem)
//        player.prepare()
//        binding.progressSong.value = viewModel.currentSongPosition.value!!
//        if (viewModel.isPlaying) {
//            binding.btPlayPause.setImageResource(R.drawable.baseline_pause_24)
//        }
//        else {
//            binding.btPlayPause.setImageResource(R.drawable.baseline_play_arrow_24)
//        }
//        player.addListener(object : Player.Listener {
//            override fun onPlaybackStateChanged(state: Int) {
//                super.onPlaybackStateChanged(state)
//                when (state) {
//                    Player.STATE_BUFFERING -> {
//
//                    }
//                    Player.STATE_READY -> {
//                        binding.tvFullTime.text = time(player.duration)
//                        binding.progressSong.addOnChangeListener { slider, value, fromUser ->
//                            if (fromUser) {
//                                player.seekTo(value.toLong() * player.duration / 100)
//                            }
//                        }
//                    }
//                    Player.STATE_ENDED -> {
//                    }
//                    Player.STATE_IDLE -> {
//                    }
//                }
//            }
//        })
//        binding.btPlayPause.setOnClickListener {
//            if (viewModel.player.playbackState == Player.STATE_READY){
//
//            }
////            {
////                val handler = Handler(Looper.getMainLooper())
////                val updateProgressAction = object : Runnable {
////                    override fun run() { // get reference to your ExoPlayer instance
////                        val duration = player.duration
////                        val position = player.currentPosition
////                        binding.tvCurrentTime.text = time(position)
////                        Log.d("TAG", "run: $position")
////                        val bufferedPosition = player.bufferedPosition
////                        val percent = if (duration == 0L) 0f else (position * 100 / duration).toFloat()
////                        Log.d("TAG", "run: $percent")
//////                binding.progressSong.value = percent
////                        viewModel.currentSongPosition.value = percent
////                        observeViewModel(duration)
////                        binding.buffered.progress = (bufferedPosition * 100 / duration).toInt()
////                        handler.postDelayed(this, UPDATE_INTERVAL_MS)
////                    }
////                }
////                fun startUpdatingProgress() {
////                    handler.post(updateProgressAction)
////                }
////
////                fun stopUpdatingProgress() {
////                    handler.removeCallbacks(updateProgressAction)
////                }
////                if (viewModel.isPlaying) {
////                    stopUpdatingProgress()
////                    player.pause()
////                    viewModel.isPlaying = false
////                    binding.btPlayPause.setImageResource(R.drawable.baseline_play_arrow_24)
////
////                } else {
////                    player.play()
////                    startUpdatingProgress()
////                    viewModel.isPlaying = true
////                    binding.btPlayPause.setImageResource(R.drawable.baseline_pause_24)
////                    startUpdatingProgress()
////                }
//            else {
//                Toast.makeText(requireContext(), "Loading", Toast.LENGTH_SHORT).show()
//            }
//        }
//        Log.d("Duration", "onViewCreated: ${player.duration}")
//
//
//        binding.topAppBar.setNavigationOnClickListener {
//            dismiss()
//        }
//    }

    private fun getVideosRelated(videoId: String){
        viewModel.getVideoRelated(videoId)
        viewModel.videoRelated.observe(viewLifecycleOwner){ response ->
            when (response) {
                is Resource.Success -> {
                    val data = response.data!!
                    val queue = data.toListTrack()
                    Queue.addAll(queue)
                    if (!requireContext().isMyServiceRunning(FetchQueue:: class.java)){
                        requireActivity().startService(Intent(requireContext(), FetchQueue::class.java))
                    }
                    else {
                        requireActivity().stopService(Intent(requireContext(), FetchQueue::class.java))
                        requireActivity().startService(Intent(requireContext(), FetchQueue::class.java))
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                    Log.d("Error", "${response.message}")
                }
            }
        }
    }
    private fun getRelated(videoId: String){
        viewModel.getRelated(videoId)
        viewModel.related.observe(viewLifecycleOwner){ response ->
            when (response) {
                is Resource.Success -> {
                    Queue.addAll(response.data!!)
                    if (!requireContext().isMyServiceRunning(FetchQueue:: class.java)){
                        requireActivity().startService(Intent(requireContext(), FetchQueue::class.java))
                    }
                    else {
                        requireActivity().stopService(Intent(requireContext(), FetchQueue::class.java))
                        requireActivity().startService(Intent(requireContext(), FetchQueue::class.java))
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                    Log.d("Error", "${response.message}")
                }
            }
        }
    }


    private fun updateUIfromQueueNowPlaying() {
        Log.w("CHECK NOW PLAYING IN QUEUE", "updateUIfromQueueNowPlaying: ${Queue.getNowPlaying()}")
        Log.d("CHECK QUEUE", "updateUIfromQueueNowPlaying: ${Queue.getQueue()}")
        val nowPlaying = Queue.getNowPlaying()
        if (nowPlaying != null) {
            binding.ivArt.visibility = View.GONE
            binding.loadingArt.visibility = View.VISIBLE
            Log.d("Update UI", "current: ${nowPlaying.title}")
            var thumbUrl = nowPlaying.thumbnails?.last()?.url!!
            if (thumbUrl.contains("w120")){
                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
            }
            val request = ImageRequest.Builder(requireContext())
                .data(Uri.parse(thumbUrl))
                .target(
                    onStart = {
                        binding.ivArt.visibility = View.GONE
                        binding.loadingArt.visibility = View.VISIBLE
                        Log.d("Update UI", "onStart: ")
                    },
                    onSuccess = { result ->
                        binding.ivArt.visibility = View.VISIBLE
                        binding.loadingArt.visibility = View.GONE
                        binding.ivArt.setImageDrawable(result)
                        Log.d("Update UI", "onSuccess: ")
                        if (viewModel.gradientDrawable.value != null){
                            viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                                binding.rootLayout.background = it
//                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
//                                binding.lyricsLayout.setCardBackgroundColor(color)
//                                Log.d("Update UI", "Lyrics: $color")
//                                updateStatusBarColor(color)
//                            })
                                viewModel.lyricsBackground.value?.let { it1 ->
                                    binding.lyricsLayout.setCardBackgroundColor(
                                        it1
                                    )
                                }
                            }
                            Log.d("Update UI", "updateUI: NULL")
                        }
                        songChangeListener.onNowPlayingSongChange()
                    },
                )
                .diskCacheKey(thumbUrl)
                .transformations(object : Transformation{
                    override val cacheKey: String
                        get() = thumbUrl

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
                        val bg = ColorUtils.setAlphaComponent(startColor, 230)
                        viewModel.gradientDrawable.postValue(gd)
                        viewModel.lyricsBackground.postValue(bg)
                        return input
                    }

                })
                .build()
            ImageLoader(requireContext()).enqueue(request)
            binding.topAppBar.subtitle = from
            binding.tvSongTitle.text = nowPlaying.title
            binding.tvSongTitle.isSelected = true
            val tempArtist = mutableListOf<String>()
            if (nowPlaying.artists != null){
                for (artist in nowPlaying.artists) {
                    tempArtist.add(artist.name)
                }
            }
            val artistName: String = connectArtists(tempArtist)
            binding.tvSongArtist.text = artistName
            binding.tvSongArtist.isSelected = true
            binding.tvSongTitle.visibility = View.VISIBLE
            binding.tvSongArtist.visibility = View.VISIBLE
        }
    }

    private fun updateUIfromCurrentMediaItem(mediaItem: MediaItem?){
        binding.ivArt.visibility = View.GONE
        binding.loadingArt.visibility = View.VISIBLE
        Log.d("Update UI", "current: ${mediaItem?.mediaMetadata?.title}")
        val request = ImageRequest.Builder(requireContext())
            .data(mediaItem?.mediaMetadata?.artworkUri)
            .target(
                onStart = {
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    Log.d("Update UI", "onStart: ")
                },
                onSuccess = { result ->
                    binding.ivArt.visibility = View.VISIBLE
                    binding.loadingArt.visibility = View.GONE
                    binding.ivArt.setImageDrawable(result)
                    Log.d("Update UI", "onSuccess: ")
                    if (viewModel.gradientDrawable.value != null){
                        viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                            binding.rootLayout.background = it
//                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
//                                binding.lyricsLayout.setCardBackgroundColor(color)
//                                Log.d("Update UI", "Lyrics: $color")
//                                updateStatusBarColor(color)
//                            })
                            viewModel.lyricsBackground.value?.let { it1 ->
                                binding.lyricsLayout.setCardBackgroundColor(
                                    it1
                                )
                            }
                        }
                        Log.d("Update UI", "updateUI: NULL")
                    }
                    songChangeListener.onNowPlayingSongChange()
                },
            )
            .diskCacheKey(mediaItem?.mediaMetadata?.artworkUri.toString())
            .transformations(object : Transformation{
                override val cacheKey: String
                    get() = "paletteArtTransformer"

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
                    val bg = ColorUtils.setAlphaComponent(startColor, 230)
                    viewModel.gradientDrawable.postValue(gd)
                    viewModel.lyricsBackground.postValue(bg)
                    return input
                }

            })
            .build()
        ImageLoader(requireContext()).enqueue(request)
        binding.topAppBar.subtitle = from
        if (mediaItem != null) {
            binding.tvSongTitle.text = mediaItem.mediaMetadata.title
            binding.tvSongTitle.isSelected = true
            binding.tvSongArtist.text = mediaItem.mediaMetadata.artist
            binding.tvSongArtist.isSelected = true
        }
        binding.tvSongTitle.visibility = View.VISIBLE
        binding.tvSongArtist.visibility = View.VISIBLE
    }
    private fun observerMetadata(){
        viewModel.metadata.observe(viewLifecycleOwner) {
            when (it){
                is Resource.Success ->{
                    metadataCurSong = it.data
                    updateUI()
                }
                is Resource.Error ->{
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun updateUI(){
        binding.ivArt.visibility = View.GONE
        binding.loadingArt.visibility = View.VISIBLE
        val request = ImageRequest.Builder(requireContext())
            .data(metadataCurSong?.thumbnails?.last()?.url)
            .target(
                onStart = {
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    Log.d("Update UI", "onStart: ")
                },
                onSuccess = { result ->
                    binding.ivArt.visibility = View.VISIBLE
                    binding.loadingArt.visibility = View.GONE
                    binding.ivArt.setImageDrawable(result)
                    Log.d("Update UI", "onSuccess: ")
                    if (viewModel.gradientDrawable.value != null){
                        viewModel.gradientDrawable.observe(viewLifecycleOwner, Observer {
                            binding.rootLayout.background = it
//                            viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer { color ->
//                                binding.lyricsLayout.setCardBackgroundColor(color)
//                                Log.d("Update UI", "Lyrics: $color")
//                                updateStatusBarColor(color)
//                            })
                            viewModel.lyricsBackground.value?.let { it1 ->
                                binding.lyricsLayout.setCardBackgroundColor(
                                    it1
                                )
                            }
                        })
                        Log.d("Update UI", "updateUI: NULL")
                    }
                    songChangeListener.onNowPlayingSongChange()
                },
            )
            .diskCacheKey(metadataCurSong?.thumbnails?.last()?.url.toString())
            .transformations(object : Transformation{
                override val cacheKey: String
                    get() = metadataCurSong?.thumbnails?.last()?.url.toString()

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
                    val bg = ColorUtils.setAlphaComponent(startColor, 230)
                    viewModel.gradientDrawable.postValue(gd)
                    viewModel.lyricsBackground.postValue(bg)
                    return input
                }

            })
            .build()
        ImageLoader(requireContext()).enqueue(request)
        Log.d("Metadata", metadataCurSong.toString())
        binding.topAppBar.subtitle = from
        binding.tvSongTitle.text = metadataCurSong?.title
        binding.tvSongTitle.isSelected = true
        val tempArtist = mutableListOf<String>()
        if (metadataCurSong?.artists != null){
            for (artist in metadataCurSong?.artists!!) {
                tempArtist.add(artist.name)
            }
        }
        val artistName: String = connectArtists(tempArtist)
        binding.tvSongArtist.text = artistName
        binding.tvSongArtist.isSelected = true
        binding.tvSongTitle.visibility = View.VISIBLE
        binding.tvSongArtist.visibility = View.VISIBLE

        Log.d("Metadata", metadataCurSong.toString())
    }
    fun connectArtists(artists: List<String>): String {
        val stringBuilder = StringBuilder()

        for ((index, artist) in artists.withIndex()) {
            stringBuilder.append(artist)

            if (index < artists.size - 1) {
                stringBuilder.append(", ")
            }
        }

        return stringBuilder.toString()
    }
    fun updateStatusBarColor(color: Int) { // Color must be in hexadecimal format
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        val window: Window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor("99"+color.toString())
    }
    public interface OnNowPlayingSongChangeListener{
        fun onNowPlayingSongChange()
        fun onIsPlayingChange()
        fun onUpdateProgressBar(progress: Float)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val activity = requireActivity()
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val card = activity.findViewById<MaterialCardView>(R.id.card)
        bottom.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
        bottom.visibility = View.VISIBLE
        card.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
        card.visibility = View.VISIBLE
    }


}