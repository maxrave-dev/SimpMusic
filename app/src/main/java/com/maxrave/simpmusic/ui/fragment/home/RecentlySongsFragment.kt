package com.maxrave.simpmusic.ui.fragment.home

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
import androidx.navigation.fragment.findNavController
import com.maxrave.simpmusic.ui.screen.home.RecentlySongsScreen
import com.maxrave.simpmusic.ui.theme.AppTheme
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel

@UnstableApi
class RecentlySongsFragment : Fragment() {
    private lateinit var composeView: ComposeView
    private val sharedViewModel by activityViewModel<SharedViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).also {
            composeView = it
        }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    Scaffold { innerPadding ->
                        RecentlySongsScreen(
                            innerPadding = innerPadding,
                            navController = findNavController(),
                            sharedViewModel = sharedViewModel
                        )
                    }
                }
            }
        }
    }
}