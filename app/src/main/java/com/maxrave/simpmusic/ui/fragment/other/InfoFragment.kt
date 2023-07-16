package com.maxrave.simpmusic.ui.fragment.other

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.maxrave.simpmusic.databinding.InfoFragmentBinding
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import com.maxrave.simpmusic.service.test.source.MusicSource
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import javax.inject.Inject

@AndroidEntryPoint
class InfoFragment: Fragment(){
    private var _binding: InfoFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<SharedViewModel>()

    @Inject
    lateinit var musicSource: MusicSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InfoFragmentBinding.inflate(inflater, container, false)
        binding.topAppBar.applyInsetter {
            type(statusBars = true){
                margin()
            }
        }
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (viewModel.nowPlayingMediaItem.value != null) {
//            binding.artistsName.text = viewModel.nowPlayingMediaItem.value?.mediaMetadata?.artist
//            binding.toolbar.title = viewModel.nowPlayingMediaItem.value?.mediaMetadata?.title
//            "https://www.youtube.com/watch?v=${viewModel.nowPlayingMediaItem.value?.mediaId}".also { binding.youtubeUrl.text = it }
            var data = musicSource.catalogMetadata[viewModel.getCurrentMediaItemIndex()]
            var downloadUrl = musicSource.downloadUrl[viewModel.getCurrentMediaItemIndex()]
            with(binding){
                toolbar.title = data?.title
                artistsName.text = data?.artists.toListName().connectArtists()
                "https://www.youtube.com/watch?v=${data.videoId}".also { youtubeUrl.text = it }
                title.text = data?.title
                albumName.text = data?.album?.name
                binding.downloadUrl .text = downloadUrl
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }
}