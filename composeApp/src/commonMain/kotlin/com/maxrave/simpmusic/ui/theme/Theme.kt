package com.maxrave.simpmusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

/**
 * The accent as the user picked it, before Material turns it into a tonal scheme.
 *
 * [androidx.compose.material3.MaterialTheme.colorScheme.primary] is the generated tone, which is
 * deliberately desaturated and shifts shade between light and dark; a few places read the raw
 * accent instead because they are drawn over artwork or on a glass surface where the generated
 * primary is either invisible or the wrong brightness. It follows the stored colour so those
 * places change with the rest of the app instead of staying brand-blue.
 */
val LocalAccentColor = staticCompositionLocalOf { seed }

/** True in dark theme. Provided by [AppTheme] so platform glass (blur/tint) can adapt reliably. */
val LocalIsDarkTheme = staticCompositionLocalOf { true }

/**
 * Whether liquid-glass surfaces may actually draw glass.
 *
 * Provided by [AppTheme] from the user's setting; the default is true so previews and anything
 * composed outside [AppTheme] keep today's look. The setting row is Android-only, so Desktop always
 * provides true — its capsule player and detail-screen buttons are glass by design, with no switch.
 */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { true }

/**
 * The dark scheme to use for immersive screens while the app itself is on the light theme.
 * Provided by [AppTheme], consumed by [ForceDarkContent]; null only outside of [AppTheme].
 */
val LocalForcedDarkColorScheme = staticCompositionLocalOf<ColorScheme?> { null }

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
 * [this] as hue in degrees [0, 360), saturation and value in [0, 1].
 *
 * The picker works in HSV because that is the space where "the same colour, darker" and "the same
 * colour, greyer" are single sliders; an RGB field makes the user solve for three channels.
 */
fun Color.toHsv(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val hue =
        if (delta == 0f) {
            0f
        } else {
            val raw =
                when (max) {
                    r -> ((g - b) / delta) % 6f
                    g -> (b - r) / delta + 2f
                    else -> (r - g) / delta + 4f
                } * 60f
            if (raw < 0f) raw + 360f else raw
        }
    val saturation = if (max == 0f) 0f else delta / max
    return Triple(hue, saturation, max)
}

/** [hsvToColor] with the alpha of the current colour kept. */
fun Color.withHsv(
    hue: Float,
    saturation: Float,
    value: Float,
): Color = hsvToColor(hue, saturation, value, alpha)

/** Builds an sRGB [Color] from hue in degrees, saturation and value in [0, 1]. */
fun hsvToColor(
    hue: Float,
    saturation: Float,
    value: Float,
    alpha: Float = 1f,
): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)
    val c = v * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) =
        when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
    return Color(r + m, g + m, b + m, alpha)
}

/** [this] as an eight-digit ARGB hex string, no "#". */
fun Color.toArgbHex(): String =
    toArgb()
        .toUInt()
        .toString(16)
        .uppercase()
        .padStart(8, '0')

/**
 * Neutral surfaces for the light theme, on a pure neutral-grey ramp (R=G=B, no seed tint);
 * primary/secondary/tertiary stay seed-derived. The page background is #FAFAFA (neutral tone 98,
 * the Material 3 stance) rather than pure white: a full-bleed #FFFFFF expanse glares on a large
 * desktop window, and even Apple — who pins systemBackground to white — puts #F2F2F7 behind
 * list pages. Pure white is kept for surfaceBright/surfaceContainerLowest so cards and sheets
 * still have a brighter-than-page tier to lift onto. Dark keeps the generated neutrals (a tone-6
 * page), not AMOLED pure black.
 */
/**
 * Pure-black surfaces for OLED panels: the page and the lowest tier stop emitting light at all,
 * and every container above them steps up in small, near-black increments so cards still read as
 * raised without turning the screen grey. Primary/secondary/tertiary keep the seed-derived tones.
 */
private fun ColorScheme.withAmoledSurfaces(): ColorScheme =
    copy(
        background = Color.Black,
        onBackground = Color(0xFFE8E8E8),
        surface = Color.Black,
        onSurface = Color(0xFFE8E8E8),
        surfaceVariant = Color(0xFF141414),
        onSurfaceVariant = Color(0xFFB4B4B4),
        surfaceTint = primary,
        surfaceBright = Color(0xFF1E1E1E),
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF101010),
        surfaceContainerHigh = Color(0xFF161616),
        surfaceContainerHighest = Color(0xFF1C1C1C),
        outline = Color(0xFF5C5C5C),
        outlineVariant = Color(0xFF2A2A2A),
        inverseSurface = Color(0xFFE8E8E8),
        inverseOnSurface = Color(0xFF1B1B1B),
    )

private fun ColorScheme.withNeutralLightSurfaces(): ColorScheme =
    copy(
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF1B1B1B),
        surface = Color(0xFFFAFAFA),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    themeMode: String = DataStoreManager.THEME_MODE_DARK,
    themeColorSource: String = DataStoreManager.THEME_COLOR_DEFAULT,
    customThemeColor: Color? = null,
    liquidGlassEnabled: Boolean = true,
    content:
        @Composable()
        () -> Unit,
) {
    val isDark = isDarkTheme(themeMode)
    val isAmoled = themeMode == DataStoreManager.THEME_MODE_AMOLED
    val wallpaperScheme =
        if (themeColorSource == DataStoreManager.THEME_COLOR_WALLPAPER) {
            platformDynamicColorScheme(isDark)
        } else {
            null
        }
    val seedColor =
        if (themeColorSource == DataStoreManager.THEME_COLOR_CUSTOM) {
            customThemeColor ?: seed
        } else {
            seed
        }
    // Light pins the neutrals to a neutral-grey ramp (the seed otherwise tints them warm/cream);
    // dark keeps the generated neutrals as they are, and AMOLED replaces them with pure black.
    val generatedScheme =
        wallpaperScheme
            ?: rememberDynamicColorScheme(
                seedColor = seedColor,
                isDark = isDark,
                style = PaletteStyle.TonalSpot,
                modifyColorScheme = { cs -> if (isDark) cs else cs.withNeutralLightSurfaces() },
            )
    val colorScheme =
        when {
            isAmoled -> generatedScheme.withAmoledSurfaces()
            else -> generatedScheme
        }
    // The raw accent those over-artwork and glass surfaces read. Wallpaper has no single picked
    // colour, so its generated primary stands in.
    val accentColor =
        if (themeColorSource == DataStoreManager.THEME_COLOR_WALLPAPER) {
            colorScheme.primary
        } else {
            seedColor
        }
    // Immersive screens stay dark even at light theme (see [ForceDarkContent]). Resolve their scheme
    // once here instead of letting every such subtree build a palette of its own.
    val forcedDarkScheme =
        if (isDark) {
            colorScheme
        } else {
            rememberDynamicColorScheme(
                seedColor = seedColor,
                isDark = true,
                style = PaletteStyle.TonalSpot,
            )
        }
    SystemBarAppearanceEffect(isDark)
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        content = {
            CompositionLocalProvider(
                LocalRippleConfiguration provides SoftRippleConfiguration,
                LocalContentColor provides colorScheme.onSurfaceVariant,
                LocalAppColors provides if (isDark) DarkAppColors else LightAppColors,
                LocalAccentColor provides accentColor,
                LocalIsDarkTheme provides isDark,
                LocalForcedDarkColorScheme provides forcedDarkScheme,
                LocalLiquidGlassEnabled provides liquidGlassEnabled,
                content = content,
            )
        },
        typography = typo(colorScheme),
    )
}

/**
 * A quieter ripple than the Material default, applied app-wide.
 *
 * Material 3's own alphas are tuned for light surfaces; on the near-black background this app
 * uses they read as a grey flash rather than a touch response. Each value is roughly 40% of
 * the default — enough to register the touch, not enough to wash the surface.
 *
 * The colour is left unspecified on purpose, so the ripple keeps deriving from LocalContentColor
 * and stays correct inside [ForceDarkContent] subtrees, which run a different scheme.
 *
 * This reaches bare `Modifier.clickable` too, not just Material components: MaterialTheme
 * already provides `material3.ripple()` as LocalIndication, and that is the one indication that
 * reads this configuration. Nothing extra has to be provided for it.
 *
 * The (color, rippleAlpha) constructor is deprecated in favour of one that also takes a `focus`,
 * but that three-argument version is internal to material3, and the two public replacements —
 * (color) and (focus, color) — drop rippleAlpha entirely. Adjusting ripple alpha therefore has
 * no non-deprecated path in this version, so the warning is suppressed rather than designed
 * around. Revisit once material3 opens up the full constructor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
private val SoftRippleConfiguration =
    RippleConfiguration(
        // Colour is deliberately left alone. Ripple derives it from LocalContentColor, which is
        // light on the dark theme and dark on the light one — always contrasting with what it
        // covers. Pinning a dark grey here instead made the ripple paint *darker* than the black
        // surface underneath, so a tap read as a sooty smudge rather than a highlight.
        // Alpha is the only lever that softens without changing which way the contrast runs.
        rippleAlpha =
            RippleAlpha(
                draggedAlpha = 0.06f,
                focusedAlpha = 0.04f,
                hoveredAlpha = 0.03f,
                pressedAlpha = 0.04f,
            ),
    )

/**
 * Wraps immersive screens — artist, album, playlist, local playlist, podcast and the players — which
 * always draw over dark artwork and must therefore render dark even on the light theme.
 *
 * [LocalForceDarkText] alone is not enough: it only recolors text through [typo]. Icons, buttons and
 * every other Material default read [LocalContentColor] and [MaterialTheme.colorScheme], which at
 * light theme resolve to dark-on-light and turn grey and unreadable over the artwork. Providing the
 * dark scheme here fixes the whole subtree in one place.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForceDarkContent(content: @Composable () -> Unit) {
    val darkScheme = LocalForcedDarkColorScheme.current ?: MaterialTheme.colorScheme
    MaterialExpressiveTheme(
        colorScheme = darkScheme,
        content = {
            CompositionLocalProvider(
                LocalForceDarkText provides true,
                LocalIsDarkTheme provides true,
                LocalContentColor provides darkScheme.onSurfaceVariant,
                LocalAppColors provides DarkAppColors,
                content = content,
            )
        },
        typography = typo(darkScheme, forceDark = true),
    )
}

/**
 * Resolves the stored theme mode to a plain boolean.
 *
 * Pulled out of [AppTheme] so chrome living *outside* it can ask the same question — the desktop
 * title bar is drawn before AppTheme is entered, so it has no MaterialTheme to read and would
 * otherwise need its own copy of this `when`.
 */
@Composable
fun isDarkTheme(themeMode: String): Boolean =
    when (themeMode) {
        DataStoreManager.THEME_MODE_LIGHT -> false
        DataStoreManager.THEME_MODE_SYSTEM -> isSystemInDarkTheme()
        DataStoreManager.THEME_MODE_DARK, DataStoreManager.THEME_MODE_AMOLED -> true
        else -> true
    }
