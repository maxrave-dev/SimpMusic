package com.maxrave.simpmusic.ui.fragment.other

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.findNavController
import com.maxrave.simpmusic.ui.screen.other.PlaylistScreen
import com.maxrave.simpmusic.ui.theme.AppTheme

@UnstableApi
class PlaylistFragment : Fragment() {
    private lateinit var composeView: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).also {
            composeView = it
        }

    @UnstableApi
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val playlistId = arguments?.getString("id")
        if (playlistId == null) {
            view.findNavController().popBackStack()
            return
        }
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    Scaffold { contentPadding ->
                        PlaylistScreen(
                            playlistId = playlistId,
                            isYourYouTubePlaylist = arguments?.getBoolean("youtube") == true,
                            navController = findNavController(),
                        )
                    }
                }
            }
        }
    }
}