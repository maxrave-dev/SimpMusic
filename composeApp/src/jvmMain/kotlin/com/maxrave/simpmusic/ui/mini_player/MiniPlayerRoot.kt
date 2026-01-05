package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.maxrave.simpmusic.viewModel.SharedViewModel

/**
 * Root composable for the mini player window content.
 * This will contain the adaptive layout logic based on window size.
 */
@Composable
fun MiniPlayerRoot(sharedViewModel: SharedViewModel) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Placeholder content - will be replaced with adaptive layouts
        Text("Mini Player")
    }
}
