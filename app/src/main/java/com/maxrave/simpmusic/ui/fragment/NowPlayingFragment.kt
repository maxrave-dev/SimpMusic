package com.maxrave.simpmusic.ui.fragment

import android.app.Dialog
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.viewModel.NowPlayingDialogViewModel
import com.maxrave.simpmusic.databinding.FragmentNowPlayingBinding
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlin.math.log


class NowPlayingFragment: BottomSheetDialogFragment() {
    private val UPDATE_INTERVAL_MS: Long = 1000
    private lateinit var dragHelper: ViewDragHelper
    private val viewModel: SharedViewModel by activityViewModels()
    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
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
        if (!viewModel.initedPlayer) {
            viewModel.initializePlayer(requireContext())
            viewModel.init()
            viewModel.setMediaSource()
            viewModel.initedPlayer = true
            viewModel.isPlaying.value = false
            viewModel.isLoading.value = true
        }
        viewModel.isPlaying.observe(this, Observer {isPlaying ->
            if (isPlaying) {
                binding.btPlayPause.setImageResource(R.drawable.baseline_pause_24)
                viewModel.updatePosition()
            } else {
                binding.btPlayPause.setImageResource(R.drawable.baseline_play_arrow_24)
            }
        })
        viewModel.currentSongBuffered.observe(this, Observer {buffered ->
            binding.buffered.progress = (buffered*100/viewModel.currentSongDuration.value!!).toInt()
        })
        binding.btPlayPause.setOnClickListener {
            if (viewModel.player.playbackState == Player.STATE_READY) {
                if (viewModel.isPlaying.value!!) {
                    viewModel.player.pause()
                    viewModel.isPlaying.value = false
                } else {
                    viewModel.player.play()
                    viewModel.isPlaying.value = true
                }
            } else {
                Toast.makeText(requireContext(), "Loading...", Toast.LENGTH_SHORT).show()
            }
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


}