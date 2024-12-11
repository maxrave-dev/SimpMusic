package com.maxrave.simpmusic.ui.fragment.library

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.findNavController
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistScreen
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.AppTheme

class FavoriteFragment : Fragment() {
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
    @ExperimentalMaterial3Api
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val type =
            when (arguments?.getString("type")) {
                "favorite" -> LibraryDynamicPlaylistType.Favorite
                "followed" -> LibraryDynamicPlaylistType.Followed
                "most_played" -> LibraryDynamicPlaylistType.MostPlayed
                "downloaded" -> LibraryDynamicPlaylistType.Downloaded
                else -> LibraryDynamicPlaylistType.Favorite
            }
        Log.w("FavoriteFragment", "type: $type")
        composeView.apply {
            setContent {
                AppTheme {
                    Scaffold { paddingValue ->
                        LibraryDynamicPlaylistScreen(paddingValue, findNavController(), type)
                    }
                }
            }
        }
    }
}