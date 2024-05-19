package com.maxrave.simpmusic.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil.ImageLoader
import coil.request.CachePolicy
import com.maxrave.simpmusic.R
import com.skydoves.landscapist.coil.LocalCoilImageLoader

private val DarkColors =
    darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        error = md_theme_dark_error,
        errorContainer = md_theme_dark_errorContainer,
        onError = md_theme_dark_onError,
        onErrorContainer = md_theme_dark_onErrorContainer,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        outline = md_theme_dark_outline,
        inverseOnSurface = md_theme_dark_inverseOnSurface,
        inverseSurface = md_theme_dark_inverseSurface,
        inversePrimary = md_theme_dark_inversePrimary,
        surfaceTint = md_theme_dark_surfaceTint,
        outlineVariant = md_theme_dark_outlineVariant,
        scrim = md_theme_dark_scrim,
    )

@Composable
fun AppTheme(
    content:
        @Composable()
        () -> Unit,
) {
//    val colors = if (supportsDynamic()) {
//        val context = LocalContext.current
//        if (inDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//    } else {
//        DarkColors
//    }
    val context = LocalContext.current
    val imageLoader =
        ImageLoader
            .Builder(context)
            .diskCachePolicy(CachePolicy.ENABLED)
            .placeholder(R.drawable.holder)
            .build()
    val contentWithImageLoader: @Composable () -> Unit = {
        CompositionLocalProvider(LocalCoilImageLoader provides imageLoader, content)
    }

    MaterialTheme(
        colorScheme = DarkColors,
        content = {
            CompositionLocalProvider(
                LocalContentColor provides DarkColors.onSurfaceVariant, // replace this with needed color from your pallete
                contentWithImageLoader,
            )
        },
        typography = typo,
    )
}

fun supportsDynamic(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) true else false

@Composable
@Preview(
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Light Mode",
)
fun AppThemePreview() {
    AppTheme {
        Column {
            Text(text = "Hello, World!", style = typo.titleSmall)
            Button(onClick = { /*TODO*/ }) {
                Text(text = "Click me!")
            }
        }
    }
}