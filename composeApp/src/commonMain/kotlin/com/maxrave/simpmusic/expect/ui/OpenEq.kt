package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

interface OpenEqLauncher {
    fun launch()
}

@Composable
expect fun openEqResult(audioSessionId: Int): OpenEqLauncher