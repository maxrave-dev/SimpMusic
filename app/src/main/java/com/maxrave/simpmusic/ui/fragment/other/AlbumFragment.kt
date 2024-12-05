package com.maxrave.simpmusic.ui.fragment.other

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.navigation.findNavController
import com.maxrave.simpmusic.ui.screen.other.AlbumScreen
import com.maxrave.simpmusic.ui.theme.AppTheme

@UnstableApi
class AlbumFragment : Fragment() {
    private var browseId: String? = null
    private lateinit var composeView: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).also {
            composeView = it
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        browseId = arguments?.getString("browseId")
        composeView.apply {
            setContent {
                AppTheme {
                    Scaffold {
                        val id = browseId
                        if (id != null) {
                            AlbumScreen(id, findNavController())
                        } else {
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }
    }
}