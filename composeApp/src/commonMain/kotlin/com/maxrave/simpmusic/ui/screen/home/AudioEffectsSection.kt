package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.player.DelayEffect
import com.maxrave.domain.data.player.ReverbPreset
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.Help
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.delay_preset_dub
import simpmusic.composeapp.generated.resources.delay_preset_quarter
import simpmusic.composeapp.generated.resources.delay_preset_slapback
import simpmusic.composeapp.generated.resources.effect_feedback
import simpmusic.composeapp.generated.resources.effect_mix
import simpmusic.composeapp.generated.resources.effect_time
import simpmusic.composeapp.generated.resources.ok
import simpmusic.composeapp.generated.resources.reverb_help_cathedral
import simpmusic.composeapp.generated.resources.reverb_help_damping
import simpmusic.composeapp.generated.resources.reverb_help_damping_meaning
import simpmusic.composeapp.generated.resources.reverb_help_hall
import simpmusic.composeapp.generated.resources.reverb_help_intro
import simpmusic.composeapp.generated.resources.reverb_help_mix
import simpmusic.composeapp.generated.resources.reverb_help_plate
import simpmusic.composeapp.generated.resources.reverb_help_predelay
import simpmusic.composeapp.generated.resources.reverb_help_predelay_meaning
import simpmusic.composeapp.generated.resources.reverb_help_room
import simpmusic.composeapp.generated.resources.reverb_help_rt60
import simpmusic.composeapp.generated.resources.reverb_help_rt60_meaning
import simpmusic.composeapp.generated.resources.reverb_help_title
import simpmusic.composeapp.generated.resources.reverb_preset_cathedral
import simpmusic.composeapp.generated.resources.reverb_preset_hall
import simpmusic.composeapp.generated.resources.reverb_preset_plate
import simpmusic.composeapp.generated.resources.reverb_preset_room
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Width of the number beside each slider.
 *
 * Wider than the equalizer's own 52dp because the longest string here is `2000 ms`, not `-15 dB`.
 * Fixed rather than wrapped so the readout does not shuffle sideways as the digits change under a
 * finger that is already moving.
 */
private val EFFECT_READOUT_WIDTH = 60.dp

/**
 * Resolution the delay time is stored at.
 *
 * The slider itself stays continuous — a stepped [Slider] draws a tick per stop, and 198 of them
 * across this range is a stipple, not a scale — so the snap is applied to the value as the thumb
 * produces it. Thumb, readout and stored value therefore always agree, which they would not if the
 * rounding happened only on release.
 */
private const val DELAY_TIME_STEP_MS = 10

/** Slack allowed when reading a preset back off stored floats that have been through text. */
private const val PRESET_MATCH_TOLERANCE = 0.001f

/**
 * A named echo, in the three numbers the sliders below hold.
 *
 * Deliberately not stored anywhere: which preset is on is read back off the values, the same way
 * the equalizer names its curve. Drag any slider and the highlight drops by itself; drag it back
 * and the preset re-selects itself, with nothing having to notice.
 */
private data class DelayPresetOption(
    val label: StringResource,
    val timeMs: Int,
    val feedback: Float,
    val mix: Float,
)

/**
 * The three echoes worth offering, shortest first.
 *
 * Slapback is the doubling effect that reads as a room rather than as an echo; Quarter is the
 * musical one, a repeat roughly on the beat at common tempos; Dub is the long, feeding tail the
 * effect is named for in the genre that made it.
 */
private val DELAY_PRESETS: List<DelayPresetOption> =
    listOf(
        DelayPresetOption(label = Res.string.delay_preset_slapback, timeMs = 80, feedback = 0.3f, mix = 0.25f),
        DelayPresetOption(label = Res.string.delay_preset_quarter, timeMs = 400, feedback = 0.45f, mix = 0.3f),
        DelayPresetOption(label = Res.string.delay_preset_dub, timeMs = 600, feedback = 0.7f, mix = 0.4f),
    )

/** The preset these three values sit on, or null once any of them has been dragged off every one. */
private fun delayPresetFor(
    timeMs: Int,
    feedback: Float,
    mix: Float,
): DelayPresetOption? =
    DELAY_PRESETS.firstOrNull {
        it.timeMs == timeMs &&
            abs(it.feedback - feedback) < PRESET_MATCH_TOLERANCE &&
            abs(it.mix - mix) < PRESET_MATCH_TOLERANCE
    }

/**
 * The room's name.
 *
 * A `when` over the enum rather than a label on it: [ReverbPreset] lives in `core/domain`, which
 * has no business holding UI text, and an exhaustive `when` here is what makes a room added there
 * later fail to compile until it has been named.
 */
private fun reverbPresetLabel(preset: ReverbPreset): StringResource =
    when (preset) {
        ReverbPreset.ROOM -> Res.string.reverb_preset_room
        ReverbPreset.HALL -> Res.string.reverb_preset_hall
        ReverbPreset.PLATE -> Res.string.reverb_preset_plate
        ReverbPreset.CATHEDRAL -> Res.string.reverb_preset_cathedral
    }

/**
 * The delay block, embedded in the settings list under its own switch.
 *
 * Presets first, then the three numbers behind them. Most people want a shape they recognise and
 * never touch a slider; the sliders are there for the ones who do, and they read as the *contents*
 * of the preset rather than as a separate control because dragging one drops the highlight.
 */
@Composable
fun DelaySection(viewModel: SettingsViewModel = koinViewModel()) {
    val timeMs by viewModel.delayTimeMs.collectAsStateWithLifecycle()
    val feedback by viewModel.delayFeedback.collectAsStateWithLifecycle()
    val mix by viewModel.delayMix.collectAsStateWithLifecycle()

    // Starts the collectors on THIS view model instance, the same way the equalizer block does.
    // SettingsViewModel.getData() has normally already run by the time this is on screen; the
    // effect stays so the block keeps working if it is ever hosted somewhere else.
    LaunchedEffect(Unit) { viewModel.getAudioEffects() }

    val activePreset = remember(timeMs, feedback, mix) { delayPresetFor(timeMs, feedback, mix) }

    EffectCard {
        EffectPresetRow {
            DELAY_PRESETS.forEach { preset ->
                Chip(
                    isSelected = preset == activePreset,
                    text = stringResource(preset.label),
                    onClick = { viewModel.applyDelayPreset(preset.timeMs, preset.feedback, preset.mix) },
                )
            }
        }
        EffectSlider(
            label = stringResource(Res.string.effect_time),
            value = timeMs.toFloat(),
            valueRange = DelayEffect.MIN_TIME_MS.toFloat()..DelayEffect.MAX_TIME_MS.toFloat(),
            snap = { (it / DELAY_TIME_STEP_MS).roundToInt() * DELAY_TIME_STEP_MS.toFloat() },
            readout = { "${it.roundToInt()} ms" },
            onCommit = { viewModel.setDelayTimeMs(it.roundToInt()) },
            modifier = Modifier.padding(top = 12.dp),
        )
        EffectSlider(
            label = stringResource(Res.string.effect_feedback),
            value = feedback,
            // Stops short of 1, where each repeat would come back as loud as the one before it and
            // the tail would never end. The ceiling belongs to the domain type, so the slider and
            // the two backends cannot drift apart on where "too much" is.
            valueRange = 0f..DelayEffect.MAX_FEEDBACK,
            readout = { "${(it * 100).roundToInt()} %" },
            onCommit = { viewModel.setDelayFeedback(it) },
            modifier = Modifier.padding(top = 12.dp),
        )
        EffectSlider(
            label = stringResource(Res.string.effect_mix),
            value = mix,
            valueRange = 0f..1f,
            readout = { "${(it * 100).roundToInt()} %" },
            onCommit = { viewModel.setDelayMix(it) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/**
 * The reverb block: which room, how much of it, and the one-tap preset that uses it.
 *
 * There are no sliders for the room's own shape — length, pre-delay, damping are a recipe the
 * generator owns, and exposing them would mean four numbers to get wrong instead of a name to
 * recognise.
 */
@Composable
fun ReverbSection(viewModel: SettingsViewModel = koinViewModel()) {
    val preset by viewModel.reverbPreset.collectAsStateWithLifecycle()
    val mix by viewModel.reverbMix.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.getAudioEffects() }

    // Dialog-open state lives here rather than in the view model: it is a piece of this screen's
    // scaffolding, not a setting, and nothing else ever needs to know it is open.
    var showHelp by remember { mutableStateOf(false) }

    EffectCard {
        // The help button sits OUTSIDE the scrolling chip row, pinned to the right edge, so it is
        // reachable whichever chips a narrow screen has scrolled away — a button that scrolls with
        // the chips is exactly the one you cannot find when "Cathedral" is off-screen.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                EffectPresetRow {
                    ReverbPreset.entries.forEach { option ->
                        Chip(
                            isSelected = option == preset,
                            text = stringResource(reverbPresetLabel(option)),
                            onClick = { viewModel.setReverbPreset(option) },
                        )
                    }
                }
            }
            RippleIconButton(
                imageVector = SimpIcons.Help,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { showHelp = true },
            )
        }
        EffectSlider(
            label = stringResource(Res.string.effect_mix),
            value = mix,
            valueRange = 0f..1f,
            readout = { "${(it * 100).roundToInt()} %" },
            onCommit = { viewModel.setReverbMix(it) },
            modifier = Modifier.padding(top = 12.dp),
        )
    }

    if (showHelp) {
        ReverbHelpDialog(onDismiss = { showHelp = false })
    }
}

/**
 * What the four rooms and the mix slider actually do, in the user's language.
 *
 * The three figures under each preset are read off [ReverbPreset] itself rather than typed into
 * the strings, for the same reason the equalizer derives its band labels from the filter's own
 * centre list: a number that lives in two places drifts, and nothing catches a help text that
 * describes a room the generator no longer builds. Only the prose is a resource.
 */
@Composable
private fun ReverbHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.reverb_help_title),
                style = typo().titleSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = stringResource(Res.string.reverb_help_intro), style = typo().bodySmall)
                ReverbPreset.entries.forEach { preset ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(reverbPresetLabel(preset)),
                            style = typo().labelMedium,
                        )
                        Text(
                            text =
                                stringResource(Res.string.reverb_help_rt60, formatSeconds(preset.rt60Ms)) + " · " +
                                    stringResource(Res.string.reverb_help_predelay, "${preset.preDelayMs} ms") + " · " +
                                    stringResource(Res.string.reverb_help_damping, formatKiloHertz(preset.dampingHz)),
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(text = stringResource(reverbPresetHelp(preset)), style = typo().bodySmall)
                    }
                }
                Text(text = stringResource(Res.string.reverb_help_rt60_meaning), style = typo().bodySmall)
                Text(text = stringResource(Res.string.reverb_help_predelay_meaning), style = typo().bodySmall)
                Text(text = stringResource(Res.string.reverb_help_damping_meaning), style = typo().bodySmall)
                Text(text = stringResource(Res.string.reverb_help_mix), style = typo().bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.ok))
            }
        },
    )
}

private fun reverbPresetHelp(preset: ReverbPreset): StringResource =
    when (preset) {
        ReverbPreset.ROOM -> Res.string.reverb_help_room
        ReverbPreset.HALL -> Res.string.reverb_help_hall
        ReverbPreset.PLATE -> Res.string.reverb_help_plate
        ReverbPreset.CATHEDRAL -> Res.string.reverb_help_cathedral
    }

/**
 * `600` → `0.6 s`, `2200` → `2.2 s`, `4000` → `4.0 s`.
 *
 * Integer arithmetic on tenths rather than a format string: common code has no `String.format`,
 * and the one-decimal shape is the whole point — `4 s` next to `2.2 s` reads as two different
 * kinds of number.
 */
private fun formatSeconds(ms: Int): String {
    val tenths = (ms + 50) / 100
    return "${tenths / 10}.${tenths % 10} s"
}

/** `6000` → `6 kHz`, `4500` → `4.5 kHz`; the decimal only appears when it carries information. */
private fun formatKiloHertz(hz: Int): String {
    val tenths = (hz + 50) / 100
    return if (tenths % 10 == 0) "${tenths / 10} kHz" else "${tenths / 10}.${tenths % 10} kHz"
}

/**
 * The surface both blocks sit on — the equalizer card's treatment, kept identical on purpose.
 *
 * Each block is one control made of several widgets, and without a card behind it the chips, the
 * sliders and the button read as unrelated rows in the settings list.
 */
@Composable
private fun EffectCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            content = content,
        )
    }
}

/**
 * A row of preset chips.
 *
 * Scrolls sideways rather than wrapping: four rooms including "Cathedral" do not fit across a phone
 * in every language, and a row that reflows to two lines moves every chip under the finger that is
 * reaching for one. Carries no label — a filled chip among empty ones is already legible as a
 * choice, and the block's own switch above says what is being chosen.
 */
@Composable
private fun EffectPresetRow(chips: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips()
    }
}

/**
 * One labelled slider with its number beside it.
 *
 * [value] is the stored setting; the thumb follows a local draft while a finger is down and only
 * [onCommit] reaches storage, on release. Every write lands in the preferences file and from there
 * in the audio chain — which on desktop drains and rebuilds mpv's whole filter graph — so applying
 * per frame would rebuild it on every pixel of a drag.
 *
 * The draft is handed back to the stored value by the [LaunchedEffect], not at release: clearing it
 * in [Slider.onValueChangeFinished] would show the pre-drag number for the length of that round
 * trip through storage.
 */
@Composable
private fun EffectSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    readout: (Float) -> String,
    onCommit: (Float) -> Unit,
    modifier: Modifier = Modifier,
    snap: (Float) -> Float = { it },
) {
    var draft by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(value) { draft = null }
    val shown = draft ?: value

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Slider(
                value = shown,
                onValueChange = { draft = snap(it).coerceIn(valueRange.start, valueRange.endInclusive) },
                onValueChangeFinished = { draft?.let(onCommit) },
                valueRange = valueRange,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = readout(shown),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(EFFECT_READOUT_WIDTH),
            )
        }
    }
}
