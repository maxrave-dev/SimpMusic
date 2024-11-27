package com.maxrave.simpmusic.ui.fragment.player

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.queue.QueueAdapter
import com.maxrave.simpmusic.adapter.queue.RecyclerRowMoveCallback
import com.maxrave.simpmusic.databinding.BottomSheetQueueTrackOptionBinding
import com.maxrave.simpmusic.databinding.QueueBottomSheetBinding
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.service.StateSource
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class QueueFragment : BottomSheetDialogFragment() {
    private val viewModel by activityViewModels<SharedViewModel>()
    private var _binding: QueueBottomSheetBinding? = null
    val binding get() = _binding!!
    private lateinit var queueAdapter: QueueAdapter

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
        _binding = QueueBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.loadingQueue.visibility = View.VISIBLE
        binding.rvQueue.visibility = View.GONE
        binding.topAppBar.subtitle = viewModel.nowPlayingScreenData.value.playlistName
        binding.topAppBar.setNavigationOnClickListener {
            dismiss()
        }
        queueAdapter = QueueAdapter(arrayListOf(), requireContext(), -1)
        binding.rvQueue.apply {
            adapter = queueAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        val callback: ItemTouchHelper.Callback = RecyclerRowMoveCallback(queueAdapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(binding.rvQueue)
        val queue =
            runBlocking {
                viewModel.getQueueData()
                    ?.listTracks
            }
        if (!queue.isNullOrEmpty()) {
            queueAdapter.updateList(queue)
        }

        lifecycleScope.launch {
            val job1 =
                launch {
                    viewModel.getHandlerStateFlow().collect { state ->
                        when (state) {
                            StateSource.STATE_INITIALIZING -> {
                                binding.loadingQueue.visibility = View.VISIBLE
                                binding.rvQueue.visibility = View.VISIBLE
                            }
                            StateSource.STATE_ERROR -> {
                                binding.loadingQueue.visibility = View.GONE
                                binding.rvQueue.visibility = View.VISIBLE
                            }
                            StateSource.STATE_INITIALIZED -> {
                                binding.loadingQueue.visibility = View.GONE
                                binding.rvQueue.visibility = View.VISIBLE
                                queueAdapter.updateList(
                                    viewModel.getQueueData()
                                        ?.listTracks ?: arrayListOf(),
                                )
                            }
                            else -> {
                                binding.loadingQueue.visibility = View.VISIBLE
                                binding.rvQueue.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            val job2 =
                launch {
                    viewModel.nowPlayingState.collect {
                        if (it != null) {
                            binding.ivThumbnail.load(it.mediaItem.mediaMetadata.artworkUri)
                            binding.tvSongTitle.text = it.mediaItem.mediaMetadata.title
                            binding.tvSongTitle.isSelected = true
                            binding.tvSongArtist.text = it.mediaItem.mediaMetadata.artist
                            binding.tvSongArtist.isSelected = true
                            if (viewModel.getHandlerStateFlow().first() == StateSource.STATE_INITIALIZED ||
                                viewModel.getHandlerStateFlow().first() == StateSource.STATE_INITIALIZING
                            ) {
                                val index = it.songEntity?.videoId?.let { it1 -> queueAdapter.getIndexOf(it1) }
                                if (index != null) {
                                    binding.rvQueue.smoothScrollToPosition(index)
                                    queueAdapter.setCurrentPlaying(index)
                                }
                            }
                        }
                    }
                }
            val job3 =
                launch {
                    viewModel.getCurrentSongIndex().collect { index ->
                        Log.d("QueueFragment", "onViewCreated: $index")
                    }
                }
//            val job4 = launch {
//                viewModel.simpleMediaServiceHandler?.added?.collect { isAdded ->
//                    Log.d("Check Added in Queue", "$isAdded")
//                    if (isAdded) {
//                        queueAdapter.updateList(viewModel.simpleMediaServiceHandler!!.catalogMetadata)
//                        viewModel.simpleMediaServiceHandler?.changeAddedState()
//                    }
//                }
//            }
//            val job5 = launch {
//                viewModel.loadingMore.collect {
//                    if (it) {
//                        binding.loadingQueue.visibility = View.VISIBLE
//                    } else {
//                        binding.loadingQueue.visibility = View.GONE
//                    }
//
//                }
//            }
            job1.join()
            job2.join()
            job3.join()
//            job4.join()
//            job5.join()
        }

        binding.tvSongTitle.isSelected = true
        binding.tvSongArtist.isSelected = true

        binding.rvQueue.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                    if (viewModel.getHandlerStateFlow().value != StateSource.STATE_INITIALIZING &&
                        layoutManager != null &&
                        layoutManager.findLastCompletelyVisibleItemPosition() == queueAdapter.itemCount - 1
                    ) {
                        viewModel.startLoadMore()
                    }
                }
            },
        )

        queueAdapter.setOnClickListener(
            object : QueueAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    viewModel.playMediaItemInMediaSource(position)
                    dismiss()
                }
            },
        )
        queueAdapter.setOnSwapListener(
            object : QueueAdapter.OnSwapListener {
                override fun onSwap(
                    from: Int,
                    to: Int,
                ) {
                    lifecycleScope.launch {
                        viewModel.swapQueue(from, to)
                        queueAdapter.updateList(
                            viewModel.getQueueData()
                                ?.listTracks ?: arrayListOf(),
                        )
                    }
                }
            },
        )
        queueAdapter.setOnOptionClickListener(
            object : QueueAdapter.OnOptionClickListener {
                override fun onOptionClick(position: Int) {
                    val dialog = BottomSheetDialog(requireContext())
                    val dialogView = BottomSheetQueueTrackOptionBinding.inflate(layoutInflater)
                    lifecycleScope.launch {
                        repeatOnLifecycle(Lifecycle.State.CREATED) {
                            viewModel.getQueueDataFlow().collect { queueData ->
                                val queueList = queueData?.listTracks ?: arrayListOf()
                                if (queueList.size > 1) {
                                    when (position) {
                                        0 -> {
                                            setEnabledAll(dialogView.btMoveUp, false)
                                            setEnabledAll(dialogView.btMoveDown, true)
                                        }

                                        queueList.size - 1 -> {
                                            setEnabledAll(dialogView.btMoveUp, true)
                                            setEnabledAll(dialogView.btMoveDown, false)
                                        }

                                        else -> {
                                            setEnabledAll(dialogView.btMoveUp, true)
                                            setEnabledAll(dialogView.btMoveDown, true)
                                        }
                                    }
                                } else {
                                    setEnabledAll(dialogView.btMoveUp, false)
                                    setEnabledAll(dialogView.btMoveDown, false)
                                    setEnabledAll(dialogView.btDelete, false)
                                }
                            }
                        }
                    }
                    with(dialogView) {
                        btMoveUp.setOnClickListener {
                            lifecycleScope.launch {
                                viewModel.moveItemUp(position)
                                queueAdapter.updateList(
                                    viewModel.getQueueData()
                                        ?.listTracks ?: arrayListOf(),
                                )
                            }
                            dialog.dismiss()
                        }
                        btMoveDown.setOnClickListener {
                            lifecycleScope.launch {
                                viewModel.moveItemDown(position)
                                queueAdapter.updateList(
                                    viewModel.getQueueData()
                                        ?.listTracks ?: arrayListOf(),
                                )
                            }
                            dialog.dismiss()
                        }
                        btDelete.setOnClickListener {
                            viewModel.removeItem(position)
                            lifecycleScope.launch {
                                queueAdapter.updateList(
                                    viewModel.getQueueData()
                                        ?.listTracks ?: arrayListOf(),
                                )
                            }
                            dialog.dismiss()
                        }
                    }
                    dialog.setCancelable(true)
                    dialog.setContentView(dialogView.root)
                    dialog.show()
                }
            },
        )
    }
}