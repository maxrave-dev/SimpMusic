package com.mocharealm.accompanist.lyrics.ui.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Creates a copy of this `Color` instance, optionally overriding the
 * hue, saturation, lightness, and alpha channels.
 *
 * All HSL component values are Floats. Hue is specified in degrees (0-360),
 * while saturation and lightness are specified as a percentage (0.0-1.0).
 *
 * @param hue The new hue value in degrees (0-360).
 * @param saturation The new saturation value (0.0 - 1.0).
 * @param lightness The new lightness value (0.0 - 1.0).
 * @param alpha The new alpha value (0.0 - 1.0).
 * @return A new `Color` with the modified HSL and/or alpha values.
 */
fun Color.copyHsl(
    hue: Float? = null,
    saturation: Float? = null,
    lightness: Float? = null,
    alpha: Float? = null
): Color {
    // 1. Convert current color from RGB to HSL
    val (h, s, l) = this.toHsl()

    // 2. Use new values if provided, otherwise keep old ones.
    // Ensure hue wraps around correctly (0-360 degrees) and other values are clamped.
    val newHue = hue?.coerceIn(0f, 360f) ?: h
    val newSaturation = saturation?.coerceIn(0f, 1f) ?: s
    val newLightness = lightness?.coerceIn(0f, 1f) ?: l
    val newAlpha = alpha?.coerceIn(0f, 1f) ?: this.alpha

    // 3. Convert new HSL back to RGB and return the new Color object.
    return hslToColor(newHue, newSaturation, newLightness, newAlpha)
}

// --- Helper Functions ---

/**
 * Converts this RGB `Color` to its HSL representation.
 * @return A `FloatArray` containing the HSL values: `[hue, saturation, lightness]`.
 */
private fun Color.toHsl(): FloatArray {
    val r = this.red
    val g = this.green
    val b = this.blue

    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min

    var h = 0f
    var s = 0f
    val l = (max + min) / 2f

    if (max != min) {
        s = if (l < 0.5f) delta / (max + min) else delta / (2f - max - min)
        h = when (max) {
            r -> (g - b) / delta + (if (g < b) 6f else 0f)
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        }
        h *= 60f
    }

    return floatArrayOf(h, s, l)
}

/**
 * Converts HSL color components to an RGB `Color`.
 * @param h Hue in degrees (0-360).
 * @param s Saturation (0.0-1.0).
 * @param l Lightness (0.0-1.0).
 * @param a Alpha (0.0-1.0).
 * @return The corresponding `Color` object.
 */
private fun hslToColor(h: Float, s: Float, l: Float, a: Float = 1.0f): Color {
    if (s == 0f) {
        // If saturation is 0, it's a shade of gray.
        return Color(red = l, green = l, blue = l, alpha = a)
    }

    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q

    val hueNormalized = h / 360f
    val r = hueToRgbComponent(p, q, hueNormalized + 1f / 3f)
    val g = hueToRgbComponent(p, q, hueNormalized)
    val b = hueToRgbComponent(p, q, hueNormalized - 1f / 3f)

    return Color(red = r, green = g, blue = b, alpha = a)
}

/**
 * Helper to convert a single hue component to its RGB value.
 */
private fun hueToRgbComponent(p: Float, q: Float, t: Float): Float {
    var tempT = t
    if (tempT < 0f) tempT += 1f
    if (tempT > 1f) tempT -= 1f
    return when {
        tempT < 1f / 6f -> p + (q - p) * 6f * tempT
        tempT < 1f / 2f -> q
        tempT < 2f / 3f -> p + (q - p) * (2f / 3f - tempT) * 6f
        else -> p
    }
}