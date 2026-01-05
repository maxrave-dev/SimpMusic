package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.runtime.Composable
import com.maxrave.simpmusic.viewModel.SharedViewModel

/**
 * Adaptive layout components for the mini player.
 * Different layouts will be used based on window width:
 * - Compact (< 260dp): Controls only
 * - Medium (260-360dp): Artwork + controls
 * - Expanded (> 360dp): Full mini player with all info
 */

@Composable
fun CompactMiniLayout(sharedViewModel: SharedViewModel) {
    // TODO: Implement compact layout with controls only
}

@Composable
fun MediumMiniLayout(sharedViewModel: SharedViewModel) {
    // TODO: Implement medium layout with artwork + controls
}

@Composable
fun ExpandedMiniLayout(sharedViewModel: SharedViewModel) {
    // TODO: Implement expanded layout with full information
}
