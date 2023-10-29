package com.maxrave.simpmusic.ui.fragment.player

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.databinding.InfoFragmentBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class InfoFragment: BottomSheetDialogFragment(){
    private var _binding: InfoFragmentBinding? = null
    private val binding get() = _binding!!
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
        savedInstanceState: Bundle?
    ): View {
        _binding = InfoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (viewModel.nowPlayingMediaItem.value != null) {
            if (viewModel.simpleMediaServiceHandler != null) {
                val data = viewModel.simpleMediaServiceHandler!!.catalogMetadata[viewModel.getCurrentMediaItemIndex()]
                with(binding){
                    toolbar.title = data.title
                    artistsName.text = data.artists.toListName().connectArtists()
                    "https://www.youtube.com/watch?v=${data.videoId}".also { youtubeUrl.text = it }
                    title.text = data.title
                    albumName.text = data.album?.name
                    val format = runBlocking { viewModel.format.first() }
                    if (format != null){
                        itag.text = format.itag.toString()
                        mimeType.text = format.mimeType ?: context?.getString(androidx.media3.ui.R.string.exo_track_unknown)
                        bitrate.text = (format.bitrate ?: context?.getString(androidx.media3.ui.R.string.exo_track_unknown)).toString()
                        description.text = format.description ?: context?.getString(androidx.media3.ui.R.string.exo_track_unknown)
                    }
                }
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
    }
}