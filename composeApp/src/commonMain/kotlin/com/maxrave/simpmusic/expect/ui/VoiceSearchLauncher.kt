package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

@Composable
expect fun rememberVoiceSearchLauncher(onResult: (String) -> Unit): () -> Unit
