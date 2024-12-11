package com.maxrave.simpmusic.ui.fragment.player

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.databinding.InfoFragmentBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InfoFragment : BottomSheetDialogFragment() {
    private var _binding: InfoFragmentBinding? = null
    val binding get() = _binding!!
    private val viewModel by activityViewModels<SharedViewModel>()

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
        savedInstanceState: Bundle?,
    ): View {
        _binding = InfoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job1 =
                    launch {
                        viewModel.format.collect { f ->
                            if (f != null) {
                                binding.itag.text = f.itag.toString()
                                binding.mimeType.text = f.mimeType
                                    ?: context?.getString(androidx.media3.ui.R.string.exo_track_unknown)
                                binding.codec.text = f.codecs
                                    ?: context?.getString(androidx.media3.ui.R.string.exo_track_unknown)
                                binding.bitrate.text =
                                    (
                                        f.bitrate
                                            ?: context?.getString(androidx.media3.ui.R.string.exo_track_unknown)
                                    ).toString()
                            }
                        }
                    }
                val job2 =
                    launch {
                        viewModel.nowPlayingScreenData
                            .collectLatest { s ->
                                val songInfo = s.songInfoData
                                if (songInfo != null) {
                                    binding.description.text = songInfo.description
                                    binding.plays.text = songInfo.viewCount?.toString() ?: context?.getString(
                                        androidx.media3.ui.R.string.exo_track_unknown,
                                    )
                                    binding.like.text =
                                        if (songInfo.like != null && songInfo.dislike != null) {
                                            getString(R.string.like_and_dislike, songInfo.like, songInfo.dislike)
                                        } else {
                                            getString(androidx.media3.ui.R.string.exo_track_unknown)
                                        }
                                }
                            }
                    }
                val job3 =
                    launch {
                        viewModel.nowPlayingState.collectLatest {
                            val nowPlaying = it?.track
                            if (nowPlaying != null) {
                                with(binding) {
                                    toolbar.title = nowPlaying.title
                                    artistsName.text = nowPlaying.artists.toListName().connectArtists()
                                    "https://www.youtube.com/watch?v=${nowPlaying.videoId}".also { youtubeUrl.text = it }
                                    title.text = nowPlaying.title
                                    albumName.text = nowPlaying.album?.name
                                    albumName.setOnClickListener {
                                        if (!nowPlaying.album?.id.isNullOrEmpty()) {
                                            findNavController().navigateSafe(
                                                R.id.action_global_albumFragment,
                                                Bundle().apply {
                                                    putString("browseId", nowPlaying.album?.id)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                job1.join()
                job2.join()
                job3.join()
            }
        }
    }
}