package com.maxrave.simpmusic.ui.fragment.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.extension.isMyServiceRunning
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.ui.screen.login.SpotifyLoginScreen
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.viewModel.LogInViewModel
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.runBlocking
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

@UnstableApi
class SpotifyLogInFragment : Fragment() {
    private lateinit var composeView: ComposeView
    private val viewModel by viewModel<LogInViewModel>()
    private val settingsViewModel by viewModel<SettingsViewModel>()
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
                        SpotifyLoginScreen(
                            innerPadding = innerPadding,
                            navController = findNavController(),
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            hideBottomNavigation = {
                                val activity = requireActivity()
                                val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
                                val miniplayer = activity.findViewById<ComposeView>(R.id.miniplayer)
                                bottom.visibility = View.GONE
                                miniplayer.visibility = View.GONE
                            },
                            showBottomNavigation = {
                                val activity = requireActivity()
                                val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
                                bottom.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
                                bottom.visibility = View.VISIBLE
                                val miniplayer = activity.findViewById<ComposeView>(R.id.miniplayer)
                                if (requireActivity().isMyServiceRunning(SimpleMediaService::class.java)) {
                                    miniplayer.animation = AnimationUtils.loadAnimation(requireContext(), R.anim.bottom_to_top)
                                    if (runBlocking { sharedViewModel.nowPlayingState.value?.mediaItem != null }) {
                                        miniplayer.visibility = View.VISIBLE
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}