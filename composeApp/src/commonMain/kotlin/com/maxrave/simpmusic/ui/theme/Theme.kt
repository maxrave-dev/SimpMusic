package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.maxrave.simpmusic.expect.ui.SystemBarAppearanceEffect

internal val md_theme_dark_background = Color(0xFF1C1B1F)
internal val md_theme_dark_surface = Color(0xFF1C1B1F)
internal val md_theme_dark_onSurface = Color(0xFFFFFFFF) // Cambiado a Blanco puro
internal val transparent = Color.Transparent

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
        favorite = Color(0xFFEF5350),
        lyricActive = Color(0xFFFFEB3B),
        shimmerBackground = Color(0xFF2A2A2A),
        shimmerLine = Color(0xFF3A3A3A),
        overlay = Color(0x66000000),
        overlayHeavy = Color(0xCC000000),
    )

private val LightAppColors =
    DarkAppColors.copy(
        shimmerBackground = Color(0xFFE0E0E0),
        shimmerLine = Color(0xFFCCCCCC),
    )

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }
val LocalIsDarkTheme = staticCompositionLocalOf { true }

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

val DarkColors =
    darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        error = Color(0xFFF2B8B5),
        errorContainer = Color(0xFF8C1D18),
        onError = Color(0xFF601410),
        onErrorContainer = Color(0xFFF9DEDC),
        background = md_theme_dark_background,
        onBackground = Color(0xFFFFFFFF), // Blanco puro
        surface = md_theme_dark_surface,
        onSurface = Color(0xFFFFFFFF), // Blanco puro
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFFFFFFF), // Blanco puro para resaltar secundarios
        outline = Color(0xFF938F99),
        inverseOnSurface = Color(0xFF1C1B1F),
        inverseSurface = Color(0xFFE6E1E5),
        inversePrimary = Color(0xFF6750A4),
        surfaceTint = Color(0xFFD0BCFF),
        outlineVariant = Color(0xFF49454F),
        scrim = Color(0xFF000000),
    )

val LightColors =
    lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF625B71),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = Color(0xFF7D5260),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8E4),
        onTertiaryContainer = Color(0xFF31111D),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF79747E),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF000000),
        surface = Color(0xFFFAFAFA),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF000000),
        inverseOnSurface = Color(0xFFF4EFF4),
        inverseSurface = Color(0xFF313033),
        inversePrimary = Color(0xFFD0BCFF),
        surfaceTint = Color(0xFF6750A4),
        outlineVariant = Color(0xFFCAC4D0),
        scrim = Color(0xFF000000),
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    color: Color? = null,
    followSystemTheme: Boolean = true,
    forceLightTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = if (followSystemTheme) {
        isSystemInDarkTheme()
    } else {
        !forceLightTheme
    }

    val baseColors = if (isDark) DarkColors else LightColors
    val colors = if (color != null) baseColors.copy(primary = color) else baseColors

    SystemBarAppearanceEffect(isDark)

    MaterialExpressiveTheme(
        colorScheme = colors,
        content = {
            CompositionLocalProvider(
                LocalContentColor provides colors.onBackground,
                LocalAppColors provides if (isDark) DarkAppColors else LightAppColors,
                LocalIsDarkTheme provides isDark,
                content = content,
            )
        },
        typography = typo(),
    )
}