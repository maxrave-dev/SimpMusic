package com.maxrave.simpmusic.ui.fragment

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.customview.widget.ViewDragHelper
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.viewModel.NowPlayingDialogViewModel
import com.maxrave.simpmusic.databinding.FragmentNowPlayingBinding
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.log

@UnstableApi
@AndroidEntryPoint
class NowPlayingFragment: BottomSheetDialogFragment() {
    private val UPDATE_INTERVAL_MS: Long = 1000
    private val viewModel by activityViewModels<SharedViewModel>()
    private lateinit var dragHelper: ViewDragHelper
    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    private var metadataCurSong: MetadataSong? = null

    private var videoId: String? = null
    private var from: String? = null

    private var gradientDrawable: GradientDrawable? = null
    private var lyricsBackground: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogTheme)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.DialogAnimation)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {

            val bottomSheetDialog = it as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { bottom ->
                val behaviour = BottomSheetBehavior.from(bottom)
                behaviour.isDraggable = false
                setupFullHeight(bottom)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("check Video ID in ViewModel", viewModel.videoId.value.toString())
        videoId = arguments?.getString("videoId")
        from = arguments?.getString("from")
        Log.d("check Video ID in Fragment", videoId.toString())
        if (videoId != null) {
            if (viewModel.videoId.value == videoId)
            {
                gradientDrawable = viewModel.gradientDrawable.value
                lyricsBackground = viewModel.lyricsBackground.value
                metadataCurSong = viewModel.metadata.value?.data
                updateUI()
            }
            else
            {
                viewModel.gradientDrawable.postValue(null)
                viewModel.lyricsBackground.postValue(null)
                binding.tvSongTitle.visibility = View.GONE
                binding.tvSongArtist.visibility = View.GONE
                binding.ivArt.visibility = View.GONE
                binding.loadingArt.visibility = View.VISIBLE
                viewModel.getMetadata(videoId!!)
                observerMetadata()
            }
        }
        else {
            videoId = viewModel.videoId.value
            from = viewModel.from.value
            metadataCurSong = viewModel.metadata.value?.data
            gradientDrawable = viewModel.gradientDrawable.value
            lyricsBackground = viewModel.lyricsBackground.value
            updateUI()
        }





        binding.btPlayPause.setOnClickListener {
            viewModel.onUIEvent(UIEvent.PlayPause)
        }

        binding.topAppBar.setNavigationOnClickListener {
            dismiss()
        }
    }
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
    private fun removeTrailingComma(sentence: String): String {
        val trimmed = sentence.trimEnd()
        return if (trimmed.endsWith(", ")) {
            trimmed.dropLast(2)
        } else {
            trimmed
        }
    }


    private fun removeComma(string: String): String {
        return if (string.endsWith(',')) {
            string.substring(0, string.length - 1)
        } else {
            string
        }
    }
    private fun observerMetadata(){
        viewModel.loadMediaItems(videoId!!)
        viewModel.videoId.postValue(videoId)
        viewModel.from.postValue(from)
        viewModel.metadata.observe(viewLifecycleOwner, Observer {
            when (it){
                is Resource.Success ->{
                    metadataCurSong = it.data
                    updateUI()
                }
                is Resource.Error ->{
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading ->{

                }
            }
        })
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
                    Log.d("Metadata", "onStart: ")
                },
                onSuccess = { result ->
                    binding.ivArt.visibility = View.VISIBLE
                    binding.loadingArt.visibility = View.GONE
                    binding.ivArt.setImageDrawable(result)
                    Log.d("Metadata", "onSuccess: ")
                    if (viewModel.gradientDrawable.value != null){
                        viewModel.gradientDrawable.observe(viewLifecycleOwner, Observer {
                            binding.rootLayout.background = it
                        })
                        viewModel.lyricsBackground.observe(viewLifecycleOwner, Observer {
                            binding.lyricsLayout.setCardBackgroundColor(it)
                        })
                        Log.d("Metadata", "updateUI: NULL")
                    }
                },
                onError = { error ->
                    binding.ivArt.visibility = View.GONE
                    binding.loadingArt.visibility = View.VISIBLE
                    Log.d("Metadata", "onError: "+ error.toString())
                }
            )
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
                    viewModel.gradientDrawable.postValue(gd)

                    viewModel.lyricsBackground.postValue(startColor)
                    return input
                }

            })
            .build()
        ImageLoader(requireContext()).enqueue(request)
        Log.d("Metadata", metadataCurSong.toString())
        binding.topAppBar.subtitle = from
        binding.tvSongTitle.text = metadataCurSong?.title
        var artistName = ""
        if (metadataCurSong?.artists != null) {
            for (artist in metadataCurSong!!.artists) {
                artistName += artist.name + ", "
            }
        }
        artistName = removeTrailingComma(artistName)
        artistName = removeComma(artistName)
        binding.tvSongArtist.text = artistName
        binding.tvSongTitle.visibility = View.VISIBLE
        binding.tvSongArtist.visibility = View.VISIBLE
        Log.d("Metadata", metadataCurSong.toString())
    }

}