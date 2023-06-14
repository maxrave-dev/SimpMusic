package com.maxrave.simpmusic.ui.fragment.other

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.customview.widget.ViewDragHelper
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.queue.QueueAdapter
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.QueueBottomSheetBinding
import com.maxrave.simpmusic.service.test.source.MusicSource
import com.maxrave.simpmusic.service.test.source.StateSource
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QueueFragment: BottomSheetDialogFragment() {

    @Inject
    lateinit var musicSource: MusicSource

    private val viewModel by activityViewModels<SharedViewModel>()
    private var _binding: QueueBottomSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var queueAdapter: QueueAdapter
    private var listTracks: ArrayList<Track>? = null

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
        _binding = QueueBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loadingQueue.visibility = View.VISIBLE
        binding.rvQueue.visibility = View.GONE
        binding.topAppBar.subtitle = viewModel.from.value
        binding.topAppBar.setNavigationOnClickListener {
            dismiss()
        }
        listTracks = ArrayList()
        queueAdapter = QueueAdapter(arrayListOf())
        binding.rvQueue.apply {
            adapter = queueAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        queueAdapter.setOnClickListener(object : QueueAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                Toast.makeText(requireContext(), "Clicked", Toast.LENGTH_SHORT).show()
            }

        })
        lifecycleScope.launch {
            while (musicSource.state == StateSource.STATE_INITIALIZING){
                binding.loadingQueue.visibility = View.VISIBLE
                binding.rvQueue.visibility = View.GONE
            }
            binding.loadingQueue.visibility = View.GONE
            binding.rvQueue.visibility = View.VISIBLE
        }

        when (viewModel.metadata.value){
            is Resource.Success -> {
                binding.ivThumbnail.load((viewModel.metadata.value as Resource.Success<MetadataSong>).data?.thumbnails?.last()?.url)
                binding.tvSongTitle.text = (viewModel.metadata.value as Resource.Success<MetadataSong>).data?.title
                binding.tvSongTitle.isSelected = true
                binding.tvSongArtist.text = (viewModel.metadata.value as Resource.Success<MetadataSong>).data?.artists?.first()?.name
                binding.tvSongArtist.isSelected = true
                viewModel.related.observe(viewLifecycleOwner){response ->
                    when(response){
                        is Resource.Success -> {
                            listTracks?.clear()
                            listTracks?.addAll(Queue.getQueue())
                            queueAdapter.updateList(listTracks!!)
                        }
                        is Resource.Loading -> {}
                        is Resource.Error -> {}
                        else -> {}
                    }
                }
            }
            is Resource.Loading -> {}
            is Resource.Error -> {}
            else -> {}
        }
    }
}