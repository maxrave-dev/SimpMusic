package com.maxrave.simpmusic.ui.fragment.other

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.maxrave.simpmusic.ui.screen.other.MoreAlbumsScreen
import com.maxrave.simpmusic.ui.theme.AppTheme

class MoreAlbumsFragment : Fragment() {
    private lateinit var composeView: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).also {
            composeView = it
        }

    @OptIn(UnstableApi::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val id = arguments?.getString("id")
        val type = arguments?.getString("type")

        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    Scaffold { innerPadding ->
                        MoreAlbumsScreen(
                            innerPadding = innerPadding,
                            navController = findNavController(),
                            id = id,
                            type = type,
                        )
                    }
                }
            }
        }
    }
}