package com.maxrave.simpmusic.ui.screen.home

import kotlin.math.abs

/**
 * A named curve for the ten ISO band centres the equalizer runs on, in dB per band.
 *
 * Names and order follow the list Spotify shows, because that is the set most listeners already
 * recognise. The gains are ours: neither Spotify nor Apple — whose preset list Spotify took — has
 * ever published the numbers behind those names, and both run a six-band equalizer, so there is
 * nothing to copy across to ten bands even in principle. Each curve here is written to the sense of
 * its own name and can be re-tuned freely without breaking anything that reads it.
 */
data class EqualizerPreset(
    val name: String,
    val bandsDb: List<Float>,
) {
    /**
     * The preamp this curve wants, in dB.
     *
     * Derived instead of written out per preset: it is always the tallest boost pulled back down to
     * make room, so listing it twenty-two times would only be twenty-two chances to get it wrong.
     * A preset that only cuts asks for no headroom and comes out at zero — spelled out rather than
     * negating a max, which would hand storage a negative zero for every such preset.
     */
    val preampDb: Float
        get() {
            val loudest = bandsDb.maxOrNull() ?: 0f
            return if (loudest > 0f) -loudest else 0f
        }
}

/**
 * The preset list, in the order it is shown.
 *
 * Gains run left to right over 31, 62, 125, 250, 500 Hz, 1, 2, 4, 8, 16 kHz — the centres named in
 * `EQUALIZER_BAND_LABELS`.
 */
val EQUALIZER_PRESETS: List<EqualizerPreset> =
    listOf(
        preset("Flat", 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        preset("Acoustic", 4f, 4f, 3f, 1f, 1.5f, 1.5f, 3f, 3.5f, 3f, 1.5f),
        preset("Bass booster", 6f, 5.5f, 4.5f, 2.5f, 0.5f, 0f, 0f, 0f, 0f, 0f),
        preset("Bass reducer", -6f, -5.5f, -4.5f, -2.5f, -0.5f, 0f, 0f, 0f, 0f, 0f),
        preset("Classical", 4.5f, 3.5f, 3f, 2.5f, -1.5f, -1.5f, 0f, 2f, 3f, 3.5f),
        preset("Dance", 5f, 6f, 3.5f, 0f, 2f, 3f, 4f, 4f, 3f, 0f),
        preset("Deep", 6f, 5f, 3f, 1.5f, 3f, 2f, 0.5f, -2.5f, -4f, -5f),
        preset("Electronic", 5f, 4.5f, 1.5f, 0f, -1.5f, 2f, 1f, 1.5f, 4.5f, 5f),
        preset("HipHop", 6f, 5f, 1.5f, 3f, -1f, -1f, 1.5f, -1f, 2f, 3f),
        preset("Jazz", 4f, 3f, 1.5f, 2f, -1.5f, -1.5f, 0f, 1.5f, 3f, 4f),
        preset("Latin", 5f, 3f, 0f, 0f, -1.5f, -1.5f, -1.5f, 0f, 3f, 5f),
        preset("Loudness", 6f, 5f, 0f, 0f, -2f, 0f, -1f, -5f, 5f, 1f),
        preset("Lounge", -3f, -1.5f, -0.5f, 1.5f, 4f, 2.5f, 0f, -1.5f, 2f, 1f),
        preset("Piano", 3f, 2f, 0f, 2.5f, 3f, 1f, 2f, 4f, 3f, 3f),
        preset("Pop", -1.5f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -1.5f),
        preset("RnB", 5.5f, 6f, 4.5f, 1f, -2.5f, -1f, 2.5f, 3f, 3.5f, 4f),
        preset("Rock", 5f, 4f, 3f, 1.5f, -0.5f, -1f, 0.5f, 3f, 4f, 4.5f),
        preset("Small speakers", 6f, 5f, 3.5f, 2f, 0f, -0.5f, 0f, 1.5f, 2.5f, 2f),
        preset("Spoken word", -4f, -3.5f, -2f, 0f, 3f, 4.5f, 5f, 4f, 2f, -1f),
        preset("Treble booster", 0f, 0f, 0f, 0f, 0f, 0f, 2.5f, 4.5f, 5.5f, 6f),
        preset("Treble reducer", 0f, 0f, 0f, 0f, 0f, 0f, -2.5f, -4.5f, -5.5f, -6f),
        preset("Vocal booster", -2.5f, -3f, -2.5f, 1.5f, 4f, 4f, 3.5f, 2f, 0f, -1.5f),
    )

private fun preset(
    name: String,
    vararg bandsDb: Float,
) = EqualizerPreset(name, bandsDb.toList())

/**
 * The preset [bandsDb] currently sits on, or null once the curve has been dragged off every one.
 *
 * Read back off the curve rather than stored alongside it, so there is only one thing to keep
 * right: dragging a band drops the label to Custom by itself, and a preset re-selects itself if the
 * curve is ever put back on it. The tolerance is there because the stored gains are floats that
 * have been through text.
 */
fun equalizerPresetFor(bandsDb: List<Float>): EqualizerPreset? =
    EQUALIZER_PRESETS.firstOrNull { preset ->
        preset.bandsDb.indices.all { i -> abs(preset.bandsDb[i] - bandsDb.getOrElse(i) { 0f }) < 0.01f }
    }

/**
 * The imported AutoEq profile's label for [bandsDb], or null once the curve has moved off it.
 *
 * [stored] is the `"<label>\n<gains>"` pair written at import. Read the same way a preset is —
 * by comparing against the curve actually in force — so dragging a band drops the headphone name
 * without anything having to notice and clear it.
 */
fun autoEqLabelFor(
    stored: String,
    bandsDb: List<Float>,
): String? {
    if (stored.isBlank()) return null
    val label = stored.substringBefore('\n').takeIf { it.isNotBlank() } ?: return null
    val saved =
        stored
            .substringAfter('\n', "")
            .split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
    if (saved.isEmpty()) return null
    val matches = bandsDb.indices.all { i -> abs(saved.getOrElse(i) { 0f } - bandsDb[i]) < 0.01f }
    return label.takeIf { matches }
}
