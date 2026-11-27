package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * A [Slider] the caller drives by [value], with its own [thumb] and [track]. material3 1.5 dropped that
 * overload for the [SliderState] one; this is it rebuilt the way the old one worked inside: one state per
 * [valueRange], given the caller's value on every composition, so the caller stays the source of truth.
 */
@Composable
fun ValueSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    thumb: @Composable (SliderState) -> Unit,
    track: @Composable (SliderState) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val state = remember(valueRange) { SliderState(value = value, trackRange = valueRange) }
    state.value = value
    Slider(
        state = state,
        onValueChange = onValueChange,
        modifier = modifier,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        thumb = thumb,
        track = track,
    )
}
