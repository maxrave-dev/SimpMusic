package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.expect.ui.SystemBarAppearanceEffect
import com.maxrave.simpmusic.expect.ui.platformDynamicColorScheme

/**
 * Semantic colors that sit outside the Material 3 ColorScheme.
 * Read them via [LocalAppColors] inside composables.
 */
@Immutable
data class AppColors(
    val favorite: Color,
    val lyricActive: Color,
    val shimmerBackground: Color,
    val shimmerLine: Color,
    val overlay: Color,
    val overlayHeavy: Color,
)

private val DarkAppColors =
    AppColors(
        favorite = favoriteColor,
        lyricActive = lyricActiveColor,
        shimmerBackground = shimmerBackground,
        shimmerLine = shimmerLine,
        overlay = overlay,
        overlayHeavy = blackMoreOverlay,
    )

// Overlays stay dark in both themes: they cover artwork, where content is always light.
private val LightAppColors =
    DarkAppColors.copy(
        shimmerBackground = shimmerBackgroundLight,
        shimmerLine = shimmerLineLight,
    )

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/** True in dark theme. Provided by [AppTheme] so platform glass (blur/tint) can adapt reliably. */
val LocalIsDarkTheme = staticCompositionLocalOf { true }

/** Parses "RRGGBB" or "AARRGGBB" (optionally "#"-prefixed) into a [Color]; null if malformed. */
fun parseThemeColorHex(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    val argb =
        when (clean.length) {
            6 -> "FF$clean"
            8 -> clean
            else -> return null
        }
    return argb.toLongOrNull(16)?.let { Color(it) }
}

/**
 * Neutral surfaces for the light theme. Mirrors the dark theme's pure-black AMOLED base: dark pins
 * background/surface to #000000, so light pins them to #FFFFFF, with a pure neutral-grey ramp
 * (R=G=B, no seed tint) for the container tiers. Primary/secondary/tertiary stay seed-derived.
 */
private fun ColorScheme.withNeutralLightSurfaces(): ColorScheme =
    copy(
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1B1B1B),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1B1B1B),
        surfaceVariant = Color(0xFFE2E2E2),
        onSurfaceVariant = Color(0xFF474747),
        surfaceTint = primary,
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFDADADA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F7F7),
        surfaceContainer = Color(0xFFF1F1F1),
        surfaceContainerHigh = Color(0xFFECECEC),
        surfaceContainerHighest = Color(0xFFE6E6E6),
        outline = Color(0xFF777777),
        outlineVariant = Color(0xFFC7C7C7),
        inverseSurface = Color(0xFF303030),
        inverseOnSurface = Color(0xFFF1F1F1),
    )

/** Theme color presets supported by SimpMusic UI. */
object ThemeColorPreset {
    const val DEFAULT = DataStoreManager.THEME_COLOR_DEFAULT
    const val WALLPAPER = DataStoreManager.THEME_COLOR_WALLPAPER
    const val CUSTOM = DataStoreManager.THEME_COLOR_CUSTOM
    const val SAKURA = "SAKURA"
    const val CYBERPUNK = "CYBERPUNK"
    const val EMERALD = "EMERALD"
    const val SUNSET = "SUNSET"
    const val OCEAN = "OCEAN"
    const val RUBY = "RUBY"
    const val MONOCHROME = "MONOCHROME"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    themeMode: String = DataStoreManager.THEME_MODE_DARK,
    themeColorSource: String = DataStoreManager.THEME_COLOR_DEFAULT,
    customThemeColor: Color? = null,
    content:
        @Composable()
        () -> Unit,
) {
    val isDark =
        when (themeMode) {
            DataStoreManager.THEME_MODE_LIGHT -> false
            DataStoreManager.THEME_MODE_SYSTEM -> isSystemInDarkTheme()
            else -> true
        }
    val wallpaperScheme =
        if (themeColorSource == ThemeColorPreset.WALLPAPER) {
            platformDynamicColorScheme(isDark)
        } else {
            null
        }
    val seedColor =
        when (themeColorSource) {
            ThemeColorPreset.CUSTOM -> customThemeColor ?: seed
            ThemeColorPreset.SAKURA -> Color(0xFFFF80AB)
            ThemeColorPreset.CYBERPUNK -> Color(0xFFB388FF)
            ThemeColorPreset.EMERALD -> Color(0xFF00E676)
            ThemeColorPreset.SUNSET -> Color(0xFFFFAB00)
            ThemeColorPreset.OCEAN -> Color(0xFF448AFF)
            ThemeColorPreset.RUBY -> Color(0xFFFF5252)
            ThemeColorPreset.MONOCHROME -> Color(0xFF9E9E9E)
            else -> seed
        }
    val paletteStyle =
        when (themeColorSource) {
            ThemeColorPreset.MONOCHROME -> PaletteStyle.Monochrome
            ThemeColorPreset.CYBERPUNK -> PaletteStyle.Vibrant
            else -> PaletteStyle.TonalSpot
        }
    // Symmetric base: dark pins background/surface to pure black via isAmoled; light pins them to
    // pure white with a neutral-grey ramp (the seed otherwise tints the light neutrals warm/cream).
    val colorScheme =
        wallpaperScheme
            ?: rememberDynamicColorScheme(
                seedColor = seedColor,
                isDark = isDark,
                isAmoled = isDark,
                style = paletteStyle,
                modifyColorScheme = { cs -> if (isDark) cs else cs.withNeutralLightSurfaces() },
            )
    SystemBarAppearanceEffect(isDark)
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        content = {
            CompositionLocalProvider(
                LocalContentColor provides colorScheme.onSurfaceVariant,
                LocalAppColors provides if (isDark) DarkAppColors else LightAppColors,
                LocalIsDarkTheme provides isDark,
                content = content,
            )
        },
        typography = typo(colorScheme),
    )
}
