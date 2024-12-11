package com.maxrave.simpmusic.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.findNavController
import com.maxrave.simpmusic.ui.screen.home.SettingScreen
import com.maxrave.simpmusic.ui.theme.AppTheme

@UnstableApi
class SettingsFragment : Fragment() {
    private lateinit var composeView: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).also {
            composeView = it
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        composeView.apply {
            setContent {
                AppTheme {
                    Scaffold {
                        SettingScreen(
                            it,
                            findNavController(),
                        )
                    }
                }
            }
        }
    }
}