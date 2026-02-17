package com.maxrave.simpmusic.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = dynamicColorScheme(context) ?: ColorScheme()
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content,
    )
}
